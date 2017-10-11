package fortunedog.mail.proxy;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

import javax.mail.Message;
import javax.mail.MessagingException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.kxml2.io.KXmlSerializer;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.kxml2.wap.WbxmlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import fortunedog.mail.proxy.MailClient.ClientRequestData;
import fortunedog.mail.proxy.MailClient.ConnectionData;
import fortunedog.mail.proxy.checker.MailChecker;
import fortunedog.mail.proxy.exchange.ActiveSyncManager;
import fortunedog.mail.proxy.exchange.Base64;
import fortunedog.mail.proxy.exchange.ExchangeAttachmentPart;
import fortunedog.mail.proxy.exchange.ExchangeException;
import fortunedog.mail.proxy.exchange.ExchangeMessage;
import fortunedog.mail.proxy.exchange.codepage.AirNotifyCodePage;
import fortunedog.mail.proxy.exchange.codepage.AirSyncBaseCodePage;
import fortunedog.mail.proxy.exchange.codepage.AirSyncCodePage;
import fortunedog.mail.proxy.exchange.codepage.CalendarCodePage;
import fortunedog.mail.proxy.exchange.codepage.CodePage;
import fortunedog.mail.proxy.exchange.codepage.ComposeMailCodePage;
import fortunedog.mail.proxy.exchange.codepage.Contacts2CodePage;
import fortunedog.mail.proxy.exchange.codepage.ContactsCodePage;
import fortunedog.mail.proxy.exchange.codepage.DocumentLibraryCodePage;
import fortunedog.mail.proxy.exchange.codepage.Email2CodePage;
import fortunedog.mail.proxy.exchange.codepage.EmailCodePage;
import fortunedog.mail.proxy.exchange.codepage.FolderHierarchyCodePage;
import fortunedog.mail.proxy.exchange.codepage.GALCodePage;
import fortunedog.mail.proxy.exchange.codepage.ItemEstimateCodePage;
import fortunedog.mail.proxy.exchange.codepage.ItemOperationsCodePage;
import fortunedog.mail.proxy.exchange.codepage.MeetingResponseCodePage;
import fortunedog.mail.proxy.exchange.codepage.MoveCodePage;
import fortunedog.mail.proxy.exchange.codepage.NotesCodePage;
import fortunedog.mail.proxy.exchange.codepage.PingCodePage;
import fortunedog.mail.proxy.exchange.codepage.ProvisionCodePage;
import fortunedog.mail.proxy.exchange.codepage.ResolveRecipientsCodePage;
import fortunedog.mail.proxy.exchange.codepage.RightsManagementCodePage;
import fortunedog.mail.proxy.exchange.codepage.SearchCodePage;
import fortunedog.mail.proxy.exchange.codepage.SettingsCodePage;
import fortunedog.mail.proxy.exchange.codepage.TasksCodePage;
import fortunedog.mail.proxy.exchange.codepage.ValidateCertCodePage;
import fortunedog.mail.proxy.net.MailSummary;
import fortunedog.mail.proxy.net.Result;
import fortunedog.util.DbHelper;
import fortunedog.util.NoElementException;
import fortunedog.util.Utils;
import fortunedog.util.XmlDomHelper;
import fortunedog.util.XmlPullHelper;
import static fortunedog.mail.proxy.exchange.FolderSyncType.*;
public class ExchangeProtocol extends MailProtocol
{
	static Logger log = LoggerFactory.getLogger(ExchangeProtocol.class);
	ActiveSyncManager asManager = null;
	private String currentFolder;
	String currentFolderId = null;
	public ExchangeProtocol(MailClient mailClient2)
	{
		super(mailClient2);
		
	}

	@Override
	protected void syncupMail(ClientRequestData reqData)
	{
		Connection mysqlConn = null;
		
		try
		{
			mysqlConn = DbHelper.getConnection();
			String syncKey = null;
			try(PreparedStatement st = mysqlConn.prepareStatement("select serverId, syncKey from folders where displayname=? and accountid="+reqData.client.connData.accountId);
					PreparedStatement updateFolderSyncKey = mysqlConn.prepareStatement("update folders set syncKey=? where serverId=? and accountId="+reqData.client.connData.accountId))
			{
				st.setString(1, reqData.folderName);
				ResultSet rst = st.executeQuery();
				if(!rst.next())
					throw new Exception("Folder not found in DB:"+reqData.folderName);
				String folderId = rst.getString(1);
				syncKey = rst.getString(2);
				HttpResponse rsp = asManager.sync(folderId, syncKey);
				Document xml = asManager.getParser(rsp, "MailSync", "C:\\temp\\mailsync.xml");
				if(xml == null)
					return;
				Element root = xml.getRootElement();
				if("0".equals(syncKey))
				{
					//this is first time do sync, need get an initial synckey
					Element csE = root.getElement(null, "Collections");
					Element cE = csE.getElement(null, "Collection");
					syncKey = XmlDomHelper.getChildAsString(cE, "SyncKey");
					updateFolderSyncKey.setString(1, syncKey);
					updateFolderSyncKey.setString(2, folderId);
					updateFolderSyncKey.execute();
					rsp = asManager.sync(folderId, syncKey);
					xml = asManager.getParser(rsp, "MailSync", "C:\\temp\\mailsync.xml");
				}
				parseMailSyncResponse(reqData, xml,folderId, reqData.folderName, updateFolderSyncKey);
			}
			
		}
		catch(Exception ex)
		{
			
			log.error( "Fail to syncup mail:{} ", reqData.client.connData.accountName, ex);
			
		}
		finally
		{
			DbHelper.close(mysqlConn);
			
		}

	}

	private int parseMailSyncResponse(ClientRequestData reqData, Document doc, String folderId, String folderName, PreparedStatement updateSynckeyStatement) throws SQLException, NumberFormatException, XmlPullParserException, IOException, NoElementException
	{
		/*
		 * a response is an xml like bellow:
				<?xml version='1.0' encoding='utf-8' ?>
				<Sync xmlns="AirSync">
				  <Collections>
		(1)		    <Collection>
				      <Class>Email</Class>
		(2)		      <SyncKey>1371609885</SyncKey>
				      <CollectionId>1</CollectionId>
				      <Status>1</Status>
		(3)		      <Commands>
				        <Add>
				          <ServerId>ZC1212-TtnCiBxesAuzb08eYBXXa36</ServerId>
				          <ApplicationData>
				            <To>"乐乐" &lt;549164183&#64;qq.com&gt;,</To>
				            <From>"洋洋" &lt;1146885880&#64;qq.com&gt;</From>
				            <Subject>洋洋 寄来的贺卡 《一起过端午》</Subject>
				            <DateReceived>2013-06-12T09:40:43.000Z</DateReceived>
				            <Importance>0</Importance>
				            <Read>1</Read>
				            <MessageClass>IPM.Note</MessageClass>
				            <Body>
				              <Type>1</Type>
				              <EstimatedDataSize>1887</EstimatedDataSize>
				              <Truncated>true</Truncated>
				              <Data>洋洋 寄来的贺卡 《一起过端午》　   如果您无法查看贺卡，点击此处查看 。棕子香，香厨房。艾叶香，香满堂。这也东阳  那也东阳 处处东阳大家端午节快乐！</Data>
				            </Body>
				          </ApplicationData>
				        </Add>
				        <Change>
				          <ServerId>ZC1212-TtnCiBxesAuzb08eYBXXa36</ServerId>
				          <ApplicationData>
				            <Read>1</Read>
				          </ApplicationData>
				        </Change>
				      </Commands>
				    </Collection>
				  </Collections>
				</Sync>



			if has attachment
			<?xml version='1.0' encoding='utf-8' ?>
<Sync xmlns="AirSync">
  <Collections>
    <Collection>
      <Class>Email</Class>
      <SyncKey>1371658010</SyncKey>
      <CollectionId>1</CollectionId>
      <Status>1</Status>
      <Commands>
        <Add>
          <ServerId>ZC0519-ZvGEvir52GOiBCR12lC7R36</ServerId>
          <ApplicationData>
            <To>"549164183"&lt;549164183&#64;qq.com&gt;,</To>
            <CC>"dliu"&lt;dliu&#64;cloudymail.mobi&gt;,</CC>
            <From>"LiuLele"&lt;dliu&#64;glorycloud.com.cn&gt;</From>
            <Subject>Atestwithattachment</Subject>
            <DateReceived>2013-06-19T15:51:17.000Z</DateReceived>
            <Importance>0</Importance>
            <Read>0</Read>
            <MessageClass>IPM.Note</MessageClass>
            <Attachments>
              <Attachment>
                <DisplayName>彩云荣光招聘信息.txt</DisplayName>
                <FileReference>ZC0519-ZvGEvir52GOiBCR12lC7R36%3A%2445429dfdf8aa81c516322716e73c8dd1</FileReference>
                <Method>1</Method>
                <EstimatedDataSize>1215</EstimatedDataSize>
                <IsInline>0</IsInline>
              </Attachment>
            </Attachments>
            <Body>
              <Type>1</Type>
              <EstimatedDataSize>1887</EstimatedDataSize>
              <Truncated>true</Truncated>
              <Data>asimpleTextinHTML</Data>
            </Body>
          </ApplicationData>
        </Add>
      </Commands>
    </Collection>
  </Collections>
</Sync>
		 */
		Element root =doc.getRootElement();
		Element ce = root.getElement(null, "Collections");
		Element collectionE = null;
		if(ce == null || (collectionE = ce.getElement(null, "Collection" )) == null)
			return Result.FAIL;
		String folderSyncKey = XmlDomHelper.getChildAsString(collectionE, "SyncKey"); //get (2)
		Element cmdsE = collectionE.getElement(null, "Commands");
		if(cmdsE == null)	//get (3)
			return Result.FAIL;
		
		Connection conn = null;
		PreparedStatement addStatement = null;
		PreparedStatement delStatement = null;
		PreparedStatement updateWithParentStatement = null;
		PreparedStatement insertWithParentStatement = null;
		//PreparedStatement updateSynckeyStatement = null;
		int uidxSeed = 0;
		int currUidx=0;
		boolean doAdd = false;
		boolean doDel = false;
		try
		{
			ArrayList<MailSummary> changedMails = new ArrayList<>();
			currUidx=uidxSeed=DbHelper.executScalar("select mailIndexCounter from account where ID="+mailClient.connData.accountId);
			conn = DbHelper.getConnection(mailClient.connData.accountId);
			conn.setAutoCommit(false);
			int count = cmdsE.getChildCount();
			for(int i=0;i<count;i++)
			{
				Element action = cmdsE.getElement(i);
				switch(action.getName())
				{
				case "Add":
					String serverId=XmlDomHelper.getChildAsString(action, "ServerId");
					Element data = action.getElement(null, "ApplicationData");
					
					if(addStatement == null)
					{
						addStatement=conn.prepareStatement("insert into mails(uid,subject,`date`, `from`, state,uidx,`to`,cc,attachmentFlag,previewContent,foldername)"
										+" values(?,?,?,?,?,?,?,?,?,?,?)");

					}
					//String uid,subject,date,from,state,uidx,to,cc,previewContent;
					MailSummary s = new MailSummary(serverId);
					s.accountId = mailClient.connData.accountId;
					s.to=XmlDomHelper.getChildAsString(data, "To", null);
					s.cc=XmlDomHelper.getChildAsString(data, "Cc", null);
					s.date=javax.xml.bind.DatatypeConverter.parseDateTime(XmlDomHelper.getChildAsString(data, "DateReceived")).getTime(); //need convert from UTC to CST
					s.from=XmlDomHelper.getChildAsString(data, "From",null);
					s.subject=XmlDomHelper.getChildAsString(data, "Subject",null);
					s.folderName = folderName;
					
					
					Element attachsE = data.getElement(null, "Attachments");
					if(attachsE != null)
					{
						int ct = attachsE.getChildCount();
						for(int j=0;j<ct;j++)
						
						{
							Element attE = attachsE.getElement(j);
							if(!"Attachment".equals(attE.getName()))
								continue;
							
							String attachName = XmlDomHelper.getChildAsString(attE, "DisplayName");
							if(attachName.endsWith(".ics"))
								s.attachmentFlag |= MailSummary.ATF_CALENDAR_ATTACH;
							else
								s.attachmentFlag |= MailSummary.ATF_NORMAL_ATTACH;
						}
					}
					Element bodyE = data.getElement(null, "Body");
					s.previewContent = XmlDomHelper.getChildAsString(bodyE, "Data", null);

					addStatement.setString(1, s.uid);
					addStatement.setString(2, StringUtils.abbreviate(s.subject, MailClient.DB_COLUMN_WIDTH_SUBJECT));
					addStatement.setTimestamp(3, new Timestamp(s.date.getTime()));
					addStatement.setString(4, StringUtils.abbreviate(s.from, MailClient.DB_COLUMN_WIDTH_FROM));
					addStatement.setInt(5, s.state );
					addStatement.setInt(6, ++currUidx);
					addStatement.setString(7, StringUtils.abbreviate(s.to, MailClient.DB_COLUMN_WIDTH_TO));
					addStatement.setString(8, StringUtils.abbreviate(s.cc, MailClient.DB_COLUMN_WIDTH_CC));
					addStatement.setInt(9, s.attachmentFlag );
					addStatement.setString(10, s.previewContent);
					addStatement.setString(11, s.folderName);
					addStatement.execute();
					changedMails.add(s);
					doAdd=true;
					break;
				case "Delete":
				case "SoftDelete":
					if(delStatement == null)
					{
						delStatement = conn.prepareStatement("update mails set uidx=null, `index`=-1, state=" + MailStatus.MAIL_TO_DEL 
								+ " where state!=" + MailStatus.MAIL_TO_DEL
								+ " and uid=?"
								+ " and foldername='"+folderName+"'");
						serverId=XmlDomHelper.getChildAsString(action, "ServerId");
						delStatement.setString(1, serverId);
						delStatement.execute();
						s = new MailSummary(serverId);
						s.state = MailStatus.MAIL_TO_DEL;
						changedMails.add(s);
						doDel=true;
					}
				}
			}
			if(doAdd)
			{
				//updateSynckeyStatement=conn.prepareStatement("update account set syncKey=? where id="+mailClient.connData.accountId);
				updateSynckeyStatement.setString(1, folderSyncKey);
				updateSynckeyStatement.setString(2, folderId);
				updateSynckeyStatement.execute();
			}
			MailSummary[] a =  changedMails.toArray(new MailSummary[0]);
			reqData.syncState.offer(a);
			conn.commit();
			if(doDel)
				reassignMailUidx(mailClient.connData.accountId, folderName);
		}
		finally
		{
			DbHelper.close(delStatement);
			DbHelper.close(updateWithParentStatement);
			DbHelper.close(addStatement);
			DbHelper.close(insertWithParentStatement);
			DbHelper.close(conn);
			if(uidxSeed != currUidx)
			try(Connection conn1=DbHelper.getConnection();Statement updateUidx=conn1.createStatement())
			{
				String sql = "update  account set mailIndexCounter="+currUidx+" WHERE ID="+ mailClient.connData.accountId ;
				updateUidx.execute(sql);
			}
			catch(Exception e)
			{
				log.error("Fail update mailIndexCounter, for account:", mailClient.connData.accountName, e);
			}
		}
		return Result.SUCCESSED;		
	}

	@Override
	Message retrieveRawMail(String uid) throws MessagingException
	{
		return null;
	}
	public ExchangeMessage retrieveExchangeMail(String uid) throws MessagingException
	{
		try
		{
			HttpResponse rsp = asManager.retrieveMail(currentFolderId, uid);
			Document doc = asManager.getParser(rsp, "RetrieveMail", "C:\\temp\\retrieveMail.xml");
			return parseRetrieveMailResponse(currentFolderId, uid, doc);
		}
		catch (Exception e)
		{
			throw new MessagingException("Exchange retrieveMail fail", e);
		}
	}

	@Override
	boolean checkOpen() throws MessagingException
	{
		return asManager.checkOpen();
	}

	@Override
	int connect(boolean forcePop3Resync, String folder)
	{
		
		if(asManager == null)
		{
			ConnectionData d = mailClient.connData;
			asManager = new ActiveSyncManager(d.accountName, d.mailServer, "", d.loginName, d.password, d.useSSL, true, "0",
										"12.1", 1234);
			asManager.Initialize();
			HttpResponse rsp;
			// send autodiscover
			// rsp = mgr.autoDiscover(); //may fail, server (ex.qq.com) return
			// not implemented
			// dump("autoDiscover", rsp);

			try
			{
				rsp = asManager.getOptions();
				
				if(!ActiveSyncManager.isSuccessed(rsp))
				{
					
					return Result.AUTH_FAIL;
				}
				//dump("getOption", rsp, "c:\\temp\\getOption.xml");
			}
			catch (IOException e)
			{
				log.error("Fail getOption", e);
				return Result.FAIL;
			}

			try
			{
				asManager.provisionDevice();
			}
			catch ( IOException | ExchangeException e)
			{
				log.error("Fail getOption", e);
				return Result.FAIL;
			}
			listForders("/");
		}
		if(Utils.isEmpty(folder))
		{
			currentFolder = folder;
			currentFolderId = null;
		}
		else
		{
			if(!folder.equals(currentFolder))
			{
				this.currentFolder = folder;
				try
				{
					currentFolderId = DbHelper.executScalarString("select serverId from folders where displayName=? and accountId="+mailClient.connData.accountId, folder);
				}
				catch (SQLException e)
				{
					log.info("Fail get folder serverId", e);
					return Result.FAIL;
				}
			}
		}
		return Result.SUCCESSED;
	}


	@Override
	void close()
	{
		

	}

	@Override
	int listForders(String root)
	{
		// send FolderSync command
		HttpResponse rsp;
		try
		{
			rsp = asManager.folderSync(mailClient.connData.syncKey);
			Document d = asManager.getParser(rsp, "FolderSync", "c:\\temp\\folderSync.xml");
			
			if(d != null)
			{
				parseFolderSyncResponse(d);
			}
			return Result.SUCCESSED;
		}
		catch ( Exception e)
		{
			log.error("listFolder fail", e);
			return Result.FAIL;
		}
		
	}

	private int parseFolderSyncResponse(Document xml) throws NumberFormatException, XmlPullParserException, IOException, NoElementException, SQLException
	{
		Element root = xml.getRootElement();
		if(XmlDomHelper.getChildAsInt(root, "Status") != ActiveSyncManager.EXCH_STATUS_OK)
			return Result.FAIL;
		String folderSyncKey = XmlDomHelper.getChildAsString(root, "SyncKey");
		Element chgE = root.getElement(null, "Changes");
		if(chgE == null)
			return Result.SUCCESSED;
		int count =0;
		if((count = XmlDomHelper.getChildAsInt(chgE, "Count")) == 0)
			return Result.SUCCESSED;
		Connection conn = null;
		PreparedStatement addStatement = null;
		PreparedStatement updateStatement = null;
		PreparedStatement delStatement = null;
		PreparedStatement updateWithParentStatement = null;
		PreparedStatement insertWithParentStatement = null;
		PreparedStatement updateSynckeyStatement = null;
		try
		{
			conn = DbHelper.getConnection();
			conn.setAutoCommit(false);
			
			
			for(int i=0;i<count;i++)
			{
				Element actionE = chgE.getElement(i);
				String serverId=null;
				String parentId=null;
				String displayName=null;
				int type=0;
				switch(actionE.getName())
				{
				case "Add":
					type=XmlDomHelper.getChildAsInt(actionE, "Type");
					if(isMailInbox(type))
					{
						serverId=XmlDomHelper.getChildAsString(actionE, "ServerId");
						parentId=XmlDomHelper.getChildAsString(actionE, "ParentId");
						displayName=XmlDomHelper.getChildAsString(actionE, "DisplayName");
						if(displayName.equals("收件箱"))
							displayName="INBOX";
						if("0".equals(parentId ) )
						{
							if(addStatement == null)
							{
								addStatement=conn.prepareStatement("insert into folders(accountid,foldername,displayname, serverId, parentId) values("
										+ mailClient.connData.accountId + ",?,?,?,?)");

							}
							addStatement.setString(1, displayName);
							addStatement.setString(2, displayName);
							addStatement.setString(3, serverId);
							addStatement.setString(4, parentId);
							addStatement.execute();
						}
						else
						{
							if(insertWithParentStatement == null)
							{
								insertWithParentStatement = conn.prepareStatement("insert into folders (accountid, foldername,displayname,serverId, parentId) "
                                        +"select "+mailClient.connData.accountId+", ?, concat(displayname, '/',  ?) ?, ? where serverId=? and accountid="+mailClient.connData.accountId);

							}
							insertWithParentStatement.setString(1, displayName);//displayName as folder name
							insertWithParentStatement.setString(2, displayName);
							insertWithParentStatement.setString(3, serverId);
							insertWithParentStatement.setString(4, parentId);
							insertWithParentStatement.setString(5, parentId);
							insertWithParentStatement.execute();
						}
					}
					break;
				case "Update":
					type=XmlDomHelper.getChildAsInt(actionE, "Type");
					if(isMailInbox(type))
					{
						serverId=XmlDomHelper.getChildAsString(actionE, "ServerId");
						parentId=XmlDomHelper.getChildAsString(actionE, "ParentId");
						displayName=XmlDomHelper.getChildAsString(actionE, "DisplayName");
						if("0".equals(parentId ) )
						{
							if(updateStatement == null)
								updateStatement=conn.prepareStatement("update folders set parentId=?, displayName=?, folderName=? where serverId=? and accountid="+mailClient.connData.accountId);

							updateStatement.setString(1, parentId);
							updateStatement.setString(2, displayName);
							updateStatement.setString(3, displayName);
							updateStatement.setString(4, serverId);
							updateStatement.execute();
						}
						else
						{
							if(updateWithParentStatement == null)
								updateWithParentStatement=conn.prepareStatement(" update folders t1, (select concat(displayName,'/',?) dispname from folders where serverId=? and accountId="+mailClient.connData.accountId+") newdata set t1.parentId=?, t1.foldername=?, t1.displayname=newdata.dispname where   serverid=? and accountid="+mailClient.connData.accountId);

							updateWithParentStatement.setString(1, displayName);
							updateWithParentStatement.setString(2, parentId);
							updateWithParentStatement.setString(3, parentId);
							updateWithParentStatement.setString(4, displayName);
							updateWithParentStatement.setString(3, serverId);
							updateWithParentStatement.execute();
						}
					}
					break;
				case "Delete":
					serverId=XmlDomHelper.getChildAsString(actionE, "ServerId");
					if(delStatement == null)
						delStatement=conn.prepareStatement("delete from folders where serverId=? and accountid="+mailClient.connData.accountId);

					delStatement.setString(1, serverId);
					delStatement.execute();
					break;
				}
			}
			
			updateSynckeyStatement=conn.prepareStatement("update account set syncKey=? where id="+mailClient.connData.accountId);
			updateSynckeyStatement.setString(1, folderSyncKey);
			updateSynckeyStatement.execute();
			conn.commit();
		}
		finally
		{
			DbHelper.close(delStatement);
			DbHelper.close(updateStatement);
			DbHelper.close(updateWithParentStatement);
			DbHelper.close(addStatement);
			DbHelper.close(insertWithParentStatement);
			DbHelper.close(updateStatement);
			DbHelper.close(conn);
		}
		return Result.SUCCESSED;
	}
	
	public byte[] retrieveAttachment(String folderId, String mailUid, String attachFileReference) throws SQLException, ExchangeException, IOException, IllegalStateException, XmlPullParserException, MessagingException
	{
		checkOpen();
		HttpResponse rsp = asManager.retrieveAttachment(folderId, mailUid, attachFileReference);
		Document d=asManager.getParser(rsp, "GetAttachment", "C:\\temp\\getattachment.xml");
		Element root = d.getRootElement();
		Element data = XmlDomHelper.searchElement(root, "Response", "Fetch", "Properties", "Data");
		String base64String = data.getText();
		
//		XmlPullParser parser = getXmlPullParser(rsp);
//		XmlPullHelper xml = new XmlPullHelper(parser);
//		xml.nextElement("Data");
		
		return org.apache.commons.codec.binary.Base64.decodeBase64(base64String);
		
	}
	@Override
	boolean waitingNewMail() throws MessagingException, InterruptedException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	void interruptWaiting() throws MessagingException, InterruptedException
	{
		// TODO Auto-generated method stub

	}
	

	
	
	
	public static boolean isMailInbox(int type)
	{
		return type == INBOX || type == USER_CREATED_MAIL;
	}
	
	ExchangeMessage parseRetrieveMailResponse(String folderId, String serverId, Document doc) throws NoElementException, MessagingException, IOException
	{
		/*
		<ItemOperations xmlns="ItemOperations">
		  <Status>1</Status>
		  <Response>
		    <Fetch>
		      <ServerId>ZC0519-ZvGEvir52GOiBCR12lC7R36</ServerId>
		      <Status>1</Status>
		      <Properties>
		        <To>"549164183"&lt;549164183&#64;qq.com&gt;,</To>
		        <CC>"dliu"&lt;dliu&#64;cloudymail.mobi&gt;,</CC>
		        <From>"LiuLele"&lt;dliu&#64;glorycloud.com.cn&gt;</From>
		        <Subject>Atestwithattachment</Subject>
		        <DateReceived>2013-06-19T15:51:17.000Z</DateReceived>
		        <Importance>1325465656</Importance>
		        <Read>0</Read>
		        <MessageClass>IPM.Note</MessageClass>
		        <Attachments>
		          <Attachment>
		            <DisplayName>彩云荣光招聘信息.txt</DisplayName>
		            <FileReference>ZC0519-ZvGEvir52GOiBCR12lC7R36%3A%2445429dfdf8aa81c516322716e73c8dd1</FileReference>
		            <Method>1</Method>
		            <EstimatedDataSize>1215</EstimatedDataSize>
		            <IsInline>0</IsInline>
		          </Attachment>
		        </Attachments>
		        <Body>
		          <Type>2</Type>
		          <Data>&lt;html&gt;
		&lt;head&gt;

		&lt;metahttp-equiv="content-type"content="text/html;charset=ISO-8859-1"&gt;
		&lt;/head&gt;
		&lt;bodytext="#000000"bgcolor="#FFFFFF"&gt;
		asim&lt;fontcolor="#ff0000"&gt;pleTextinH&lt;/font&gt;TML&lt;br&gt;
		&lt;/body&gt;
		&lt;/html&gt;</Data>
		        </Body>
		      </Properties>
		    </Fetch>
		  </Response>
		</ItemOperations>
	*/
		Vector<MailPart> parts = new Vector<>();
		ExchangeMessage msg = new ExchangeMessage(folderId, serverId, parts);
		Element root = doc.getRootElement();
		Element data = XmlDomHelper.searchElement(root, "Response", "Fetch", "Properties", "Body", "Data");
		String html = data.getText();
		byte[] content = html.getBytes("UTF-8");
		MailPart p = new MailPart(null, content, null, "text/html", null, null, content.length);
		parts.add(p);
		
		Element asE = XmlDomHelper.searchElement(root, "Response", "Fetch", "Properties", "Attachments");
		if(asE != null)
		{
			int ct = asE.getChildCount();
			for(int j=0;j<ct;j++)
			
			{
				Element attE = asE.getElement(j);
				if(!"Attachment".equals(attE.getName()))
					continue;
				
				String attachName = XmlDomHelper.getChildAsString(attE, "DisplayName");
				
				String contentId=XmlDomHelper.getChildAsString(attE, "FileReference");
				String disposition =null;
				int size=XmlDomHelper.getChildAsInt(attE, "EstimatedDataSize");
				String inline = XmlDomHelper.getChildAsString(attE, "IsInline", "0");
				if("0".equals(inline))
					disposition = "attachment";
				else
					disposition = "inline";
				ExchangeAttachmentPart p2 = new ExchangeAttachmentPart(this, msg, new byte[0], contentId, "application/octet-stream", attachName, disposition, size);
				parts.add(p2);
			}
		}
		
		return msg;
	}
}

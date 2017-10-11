package fortunedog.mail.proxy;

import java.security.Security;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;

import javax.mail.AuthenticationFailedException;
import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.StringUtils;
import org.apache.geronimo.javamail.store.imap.IMAPFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fortunedog.mail.proxy.MailClient.ClientRequestData;
import fortunedog.mail.proxy.MailClient.ConnectionData;
import fortunedog.mail.proxy.checker.MailCheckerManager;
import fortunedog.mail.proxy.net.MailSummary;
import fortunedog.mail.proxy.net.Result;
import fortunedog.util.DbHelper;

class ImapProtocol extends MailProtocol
{
	static Logger log = LoggerFactory.getLogger(ImapProtocol.class);
	private Session session;
	private Store store ;
	private IMAPFolder folder;
//	final String tempTableName = "tempUid"+(tempTableId++);
	private String currentFolderName="";
	private HashMap<String, Integer> folderMsgCount = new HashMap<String, Integer>();
	public ImapProtocol(MailClient mailClient2)
	{
		super(mailClient2);
	}
	/**
	 * syncup的思路:
	 * 基本思路：
	 * 用户请求一定数量的邮件，通过HTTP请求上面的mc参数，一般是30，20，10等。syncup就只同步指定数量的邮件。
	 * 邮件范围的确定： 1）检查最新邮件，就是从sequence number最大开始，同步mc封
	 * 				   2） 获取更多邮件，此时用户会发送uid ceil，首先找到uid ceil对应的sequence number，然后
	 * 					  从该sequence number开始，向下同步mc封
	 * 之所以要转成sequence number，因为IMAP的UID是不连续的，且变化范围很大，区间[uid, uid+30)里面的邮件数量并
	 * 不一定是30。sequence number是连续的，可以较容易确定30封邮件的范围。
	 * IMAP没有指定起始的uid, 和想要获取的邮件数量，就能返回UID列表的方法。
	 * 
	 * 扩展：
	 * 为了指定30封邮件的后面还有没有更多邮件，每次获取要试图多获取一定数量的邮件，比如获取35封，这样就能知道是
	 * 有更多邮件。
	 * 
	 * 同步服务器上已经删除了的邮件，需要对服务器上已经同步过的邮件进行再同步。因为并不知道哪些邮件被删除了，同步
	 * 范围无法确定。
	 * 客户端同步邮件的时候，会发送已收取的最大的邮件的uidx，和最小的uidx，只需要同步这个范围内的邮件即可。
	 * 如果这个范围仍然太大，就要采用一些启发式的策略，可以只同步max uidx向下的100封
	 */

	private  Vector<Long> updateMailUid(Connection conn, Statement st, PreparedStatement pst,
	                    				IMAPFolder uidfolder, long uidCeil, int count, 
	                    				boolean isSeqNum,/*uidCeil is a sequence number, otherwise, a uid*/
	                    				ClientRequestData reqData)
	                    				throws MessagingException, SQLException
	{
		int index = 1;
		if(isSeqNum)
			index = (int) uidCeil;
		else
		{
			if(uidCeil == Long.MAX_VALUE)
			{
				
				index = uidfolder.getMessageCount();// don't touuch, it's value of uidfolder.getMessageCount()	
			}
			else
			{
				try {
					index = uidfolder.getSequenceNum(uidCeil);
					
				}
				catch(MessagingException ex)
				{//requested UID may not exist . How to make a test to cover this condition?
					index = uidfolder.getMessageCount();
				}
			}
		}
		int updatedCount = 0;
		Vector<Long> v = new Vector<Long>(count);
		do
		{
			Vector<Long> tmpV = new Vector<Long>(count);
			int low = index - count+1;
			if(low <=0)
				low = 1;
			if(index < low) //we have encountered error : 1a18 UID SEARCH 1:-1
				break;
			long[] uids = uidfolder.searchUidBySequenceRange((int)low, (int)index);
			for(long uid:uids)
			{
				if(isSeqNum )
				{
					pst.setLong(1, uid);
					pst.addBatch();
					updatedCount++;
					tmpV.add(uid);
				}
				else
				{
					if(uid <= uidCeil) 
					{
						pst.setLong(1, uid);
						pst.addBatch();
						updatedCount++;
						tmpV.add(uid);
					}
					else
						break; //the following UID will be more big
				}
			}
			if(tmpV.size() > 0)
			{
				tmpV.addAll(v);
				v = tmpV;
			}
			index = low -1;
			if(index <= 0 )
				break;
		}while(updatedCount < count);
		conn.setAutoCommit(false);
		try
		{
			pst.executeBatch();
		}
		finally
		{
			conn.commit(); //commit to DB any way, so individual record fail will not block other mails
		}
		String sql1 = null;
		if(DbHelper.useSqlite)
		{
			sql1 = "insert into mails (uid, `index`, state, foldername) select uid, 0, state,'"
					+ reqData.folderName
					+ "' as foldername"
					+ " from tempUid where not exists ( select uid from mails where uid=tempUid.uid and foldername='"
					+ reqData.folderName + "')";
		}
		else
		{
			assert false;
			sql1 = "insert into mails (uid, `index`, state, accountId,foldername) select uid, 0, state, "
					+ mailClient.connData.accountId
					+ " as accountId, '"
					+ reqData.folderName
					+ "' as foldername"
					+ " from tempUid where not exists ( select uid from mails where uid=tempUid.uid and accountId="
					+ mailClient.connData.accountId
					+ " and foldername='"
					+ reqData.folderName + "')";
		}
		DbHelper.doTransaction(conn, st, sql1);
		DbHelper.checkState(conn);			
		
		///sqlite_refactor
		String sql2 = null;
		if(DbHelper.useSqlite)
		{
			sql2 = "update mails set uidx=null, state="
						+ MailStatus.MAIL_TO_DEL + " where state!="
							+ MailStatus.MAIL_TO_DEL
							+ " and not exists (select uid from tempUid where "
							+ "tempUid.uid=mails.uid) and cast(uid as unsigned)<=" + v.lastElement()
						+ " and cast(uid as unsigned)>=" + v.firstElement() + " and foldername='"
						+ reqData.folderName + "'";
			//System.out.println("Update delete mail, for account:" + mailClient.connData.accountId);
			DbHelper.doTransaction(conn, st, sql2);
			DbHelper.checkState(conn);
			reassignMailUidx(mailClient.connData.accountId, reqData.folderName);
			DbHelper.checkState(conn);
		}
		else
		{
			assert false;
		}
		return v;
	}	
	private void updateMailHeader(Connection conn, PreparedStatement pst,
			IMAPFolder folder, FetchProfile fp, long[] uids,
			ClientRequestData reqData)
			throws MessagingException
	{
		Message[] msgs = folder.getMessagesByUID(uids);
		
		ArrayList<Message> al = new ArrayList<Message>(msgs.length);
		
		for(int i=0;i<msgs.length; i++)
		{
			if(msgs[i] == null)
			{
				log.info(mailClient.connData.accountName + " mail not exist:" + uids[i]);
			}
			else
				al.add(msgs[i]);
		}
		
		if(al.size() != msgs.length)
		{
			msgs = new Message[al.size()];
			al.toArray(msgs);
		}
		try
		{
			folder.fetch(msgs, fp);
		}
		catch(org.apache.geronimo.javamail.util.ResponseFormatException fe)
		{ //if there's exception, try to fetch message one by one, so a bad message will not prevent other messages
			fe.printStackTrace();
			//bodystructure may be parsed failed, so we only obtain UID and envelope here. refer to bug 157
			FetchProfile fp1 = new FetchProfile();
			fp1.add(UIDFolder.FetchProfileItem.UID);
			fp1.add(FetchProfile.Item.ENVELOPE);
			Message[] m1 = new Message[1];
			
			for(Message m : msgs)
			{
			
				m1[0] = m;
				try 
				{
					folder.fetch(m1, fp1);
				}
				catch(org.apache.geronimo.javamail.util.ResponseFormatException fe2)
				{
					fe2.printStackTrace();
				}
			}
			
		}
		Vector<MailSummary> summarys = new Vector<MailSummary>(msgs.length);
//		MailSummary[] summarys = new MailSummary[msgs.length];
		for(int i=0;i<msgs.length;i++)
		{
			try
			{
			MailSummary summary=new MailSummary(mailClient.connData.accountId, folder.getUID(msgs[i])+"",msgs[i],0);
			summary.uidx=Integer.parseInt(summary.uid);
			summary.state=MailStatus.MAIL_NEW;
			summary.folderName=reqData.folderName;
//			summarys[i]=summary;
			summarys.add(summary);
			}
			catch(Exception e)
			{
				log.warn("Fail to update mail:{}", msgs[i], e);
			}
		}
		MailSummary[] array = new MailSummary[summarys.size()];
		summarys.toArray(array);
		reqData.syncState.offer(array);
		
		
		for(int i=0;i<array.length;i++)
		{
			
			try
			{
				MailSummary s = array[i];

				pst.setString(1, StringUtils.abbreviate(s.subject, MailClient.DB_COLUMN_WIDTH_SUBJECT));
				pst.setTimestamp(2, new java.sql.Timestamp(s.date.getTime()));
				pst.setString(3, StringUtils.abbreviate(s.from, MailClient.DB_COLUMN_WIDTH_FROM));
				pst.setString(4, StringUtils.abbreviate(s.to, MailClient.DB_COLUMN_WIDTH_TO));
				pst.setString(5, StringUtils.abbreviate(s.cc, MailClient.DB_COLUMN_WIDTH_CC));
				pst.setInt(6, s.attachmentFlag);
				pst.setString(7, s.uid);
				pst.addBatch();
			}
			catch (Exception e)
			{
				log.error("Update Mail Fail", e);
			}
				
		}
		try
		{		
			conn.setAutoCommit(false);
			pst.executeBatch();
		}
		catch (Exception e)
		{
			log.error("execute Update Mail Fail", e);
		}
		finally
		{
			try
			{
				conn.commit(); //commit anyway,
			}
			catch (SQLException e)
			{
				log.error( "commit Update Mail Fail", e);
			}
		}
		
		DbHelper.checkState(conn);
		
	}

	@Override
	public Message retrieveRawMail(String uid) throws MessagingException
	{
//		folder.updateMailboxStatus();
		MimeMessage imapMsg =  (MimeMessage) folder.getMessageByUID(Long.parseLong(uid));
//		MimeMessage cmsg = new MimeMessage(imapMsg);
//		return cmsg;
		return imapMsg;
	}
	public boolean checkOpen() throws MessagingException
	{
		if(folder == null)
			return connect(false,null) == Result.SUCCESSED;
		if(folder != null && !folder.isOpen())
		{
			try
			{
				folder.open(Folder.READ_WRITE);
			}
			catch(MessagingException e)
			{
				return connect(false,null) == Result.SUCCESSED;
			}
		}

		return true;
	}
	
	@Override
	public synchronized int connect(boolean forcePop3Resync,String folderName)
	{
//		log.info( "Entering ImapProtocol.connect");
		ConnectionData connData = mailClient.connData;
		if(folder != null &&folderName!=null&& folderName.equals(folder.getFullName()))
		{//this is just folder we want
			try
			{
				folder.updateMailboxStatus();
				return Result.SUCCESSED;
			}
			catch (MessagingException e)
			{
				log.debug( "folder set to null", e);
				folder = null;
			}
		}
		// Get a Store object
		try
		{
			if(folder == null || !folder.isOpen() || !store.isConnected())
			{

				Properties props = new Properties();
				props.putAll(System.getProperties());
				//Properties props = System.getProperties();
				if(connData.useSSL)
				{// SSL 连接需要(开始)

					Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());  
					final String SSL_FACTORY = "fortunedog.mail.proxy.DummySSLSocketFactory";  
					
					//props.setProperty("mail.imap.sasl.enable", "true"); //NullPointException for some mail server. e.g. 163
					props.setProperty("mail.imap.ssl.enable", "true");
					props.setProperty("mail.imap.ssl.trust", "*");
					props.setProperty("mail.imaps.ssl.trust", "*");
					props.setProperty("mail.imaps.ssl.socketFactory.class", SSL_FACTORY);
					props.setProperty("mail.imaps.socketFactory.class", SSL_FACTORY);
					props.setProperty("mail.imap.ssl.socketFactory.class", SSL_FACTORY);
					props.setProperty("mail.imap.ssl.socketFactory.fallback", "false");
//					props.setProperty("mail.imaps.separatestoreconnection", "true");
				}// SSL连接需要(结束)
				else
				{
//					props.setProperty("mail.imap.separatestoreconnection", "true");
				}
				//SMTP authentication
//				properties.setProperty("mail.smtp.submitter", authenticator.getPasswordAuthentication().getUserName());
				props.setProperty("mail.imap.fetchsize", "65536");
//				props.setProperty("mail.smtp.host", smtpServer);
//				props.setProperty("mail.smtp.port", smtpPort+"");
//				session = Session.getDefaultInstance(props, new Authenticator());
				

				session = Session.getInstance(props, null);
				session.setDebug(MailClient.verbose || connData.accountName.startsWith("dliu@cloudy"));
				if(store == null)
					store = session.getStore(connData.protocol); 

				if( !store.isConnected())
				{
					
					store.connect(connData.mailServer, connData.mailPort, connData.loginName, connData.password);
					folder = (IMAPFolder) store.getDefaultFolder();
					currentFolderName = folder.getFullName();
					if (folder == null) {
						//System.out.println("No default folder");
						//store.close(); //found error after a long time of waiting mails
						log.error( Thread.currentThread().getName()+" Fail to open default folder");
						return Result.FAIL;
					}
					System.out.println("#########################");
					listForders("/");
					System.out.println("#########################");
				}
			
				// uidfolder = (UIDFolder)folder;
			}
			if(folderName!=null)
			{
				try
				{ 
					if(!folderName.equals(currentFolderName) || folder == null)
					{
						folder = (IMAPFolder) store.getFolder(folderName);
						currentFolderName=folder.getFullName();
					}
					if (folder == null) {
						//System.out.println("Invalid folder");
						//store.close();
						log.error( Thread.currentThread().getName()+" Fail to open folder: "+folderName);
						return Result.FAIL;
					}
					if(!folder.isOpen())
					{
						try
						{
							folder.open(Folder.READ_WRITE);
	
						}
						catch (MessagingException e)
						{
							folder.open(Folder.READ_ONLY);
						}
					}
				}
				catch (MessagingException e1)
				{
					log.error("Fail connect", e1);
				}
			}

		}

		catch (AuthenticationFailedException ae)
		{
			log.debug("Fail connect", ae);
			return Result.AUTH_FAIL;

		}
		catch (NoSuchProviderException e)
		{
			// folder.close(false);
			// store.close();
			log.error( "Fail to connect mail server:"+connData.accountName, e);
			return Result.FAIL;
		}
		catch (Exception e)
		{
			// folder.close(false);
			// store.close();
			log.error( "Fail to connect mail server:"+connData.accountName, e);
			return Result.FAIL;
		}
		finally
		{
//			log.info( "Levae MailClient.connect" );
		}
		return Result.SUCCESSED;
	}

	protected Folder getCurrentFolder() throws MessagingException
	{
		checkOpen();
		return folder;
	}


	/**
	 * 
	 * 将指定参数下的邮件夹名称，邮件夹全名，accountid放入forders表中
	 * 
	 * @param 
	 * 		root
	 *   	mailboxname(such as super) or mailboxfullname(such as super/sub) or "/"
	 * @exception
	 * 		MessagingException e :参数错误(格式或邮箱名不存在)
	 * @exception
	 * 		SQLException e		 :主键冲突
	 */
	@Override
	public int listForders(String root)
	{
		Connection conn = null;
		PreparedStatement preparedStatementst = null;
		Statement statement=null;
		IMAPFolder f = null;
		Folder[] folders = null;
		if (root == null)
		{
			return Result.FAIL;
		}
		try
		{
			if ("/".equals(root))
			{
//				f = (IMAPFolder) store.getDefaultFolder();
				folders = folder.list("*");
			}
			else
			{
				f = (IMAPFolder) store.getFolder(root);
				//当root错误时此句报异常
				folders = f.list(root);
			}
			conn = DbHelper.getConnection();
			statement=conn.createStatement();
			statement.execute("delete from  folders where accountid="+mailClient.connData.accountId);
			preparedStatementst = conn
					.prepareStatement("insert into folders(accountid,foldername,displayname) values("
										+ mailClient.connData.accountId + ",?,?)");
			ArrayList<Folder> folderQueue = new ArrayList<Folder>(Arrays.asList(folders));
			for (Folder folder : folderQueue)
			{
				IMAPFolder imapFolder = (IMAPFolder) folder;
//				if(imapFolder.hasChild())
//				{
//					Folder[] children = folder.list("*");
//					
//					folderQueue.addAll(Arrays.asList(children));
//					
//					continue;
//				}
				if(imapFolder.isNoSelectable())
					continue;
				preparedStatementst.setString(1, imapFolder.getFullName());
				preparedStatementst.setString(2, imapFolder.getFullName());
				preparedStatementst.addBatch();

				System.out.println(imapFolder.getName());
				System.out.println(imapFolder.getFullName());
			}
			preparedStatementst.executeBatch();
		}

		catch (MessagingException e)
		{
			log.error( "Fail to list folder", e);
			return Result.FAIL;
		}
		catch (SQLException e)
		{
			log.error( "Fail to list folder", e);
			return Result.FAIL;
		}
		finally
		{
			DbHelper.close(statement);
			DbHelper.close(preparedStatementst);
			DbHelper.close(conn);
		}
		return Result.SUCCESSED;
	}

	public void close()
	{
		try
		{
			if (folder != null)
			{
				try
				{
					if (folder.isOpen())
						folder.close(true);
				}
				catch (Exception e)
				{
				}
				folder = null;
				currentFolderName = "";
			}
			if (store != null)
			{
				try
				{
//					if(store.isConnected()) //isConnect will try send command to server to check it's alive or not, since we are 
					// going to close it, checking is unnecessar
					store.close();
				}
				catch (Exception e)
				{
					// e.printStackTrace();
				}
				store = null;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/////////sqlite_refactor
	@Override
	public void syncupMail(ClientRequestData reqData)
	{
		Connection conn = null;
		Statement st = null;
		PreparedStatement pst = null;
		PreparedStatement pst2 = null;
		int accountId = mailClient.connData.accountId;
		try
		{
			IMAPFolder f = (IMAPFolder) getCurrentFolder();
			FetchProfile fp = new FetchProfile();
			fp.add(UIDFolder.FetchProfileItem.UID);
			fp.add(FetchProfile.Item.ENVELOPE);
			fp.add(FetchProfile.Item.CONTENT_INFO);
			
			conn = DbHelper.getConnection(accountId);
			st = conn.createStatement();
			pst = conn.prepareStatement("UPDATE mails SET `subject`=?, `date`=?, `from`=?, `to`=?, cc=?, `state`=" + MailStatus.MAIL_NEW +", uidx=uid, attachmentFlag=? WHERE uid=? AND foldername='"+reqData.folderName+"'");
			//we need a set to store new fetched mails replace of previous temporary table.
			st.execute("DROP TABLE IF EXISTS tempUid");
			st.execute("CREATE TEMPORARY TABLE tempUid (uid VARCHAR(70), state tinyint,  PRIMARY KEY (`uid`))");
			pst2 = conn.prepareStatement("insert into tempUid values( ? ," + MailStatus.MAIL_NEW +")");

			
			int msgCount = folder.getMessageCount();
			log.info(  "MailClient.mailCount=" + mailClient.mailCount + "IMAPFolder.getMessageCount="+msgCount);
			mailClient.mailCount = msgCount;
			if(msgCount == 0)
			{
				//no message on server, delete all mails in db
				String sql2 = "update mails set uidx=null, state=" + MailStatus.MAIL_TO_DEL
						+ " where state!="
						+ MailStatus.MAIL_TO_DEL
						+ " and foldername='"+reqData.folderName+"'";
				st.executeUpdate(sql2);
				reassignMailUidx(accountId, reqData.folderName);
				DbHelper.checkState(conn);
				return;
			}
			int requestedMailCount = reqData.requestedMailCount;
			long requestedUidxCeil  = reqData.requestedUidxCeil;
			long clientUidxMax = reqData.uidxMax;
			String folderName=reqData.folderName;
			StringBuilder sqlWhere = new StringBuilder( " where `uidx` > ").append( clientUidxMax  );
			if(requestedUidxCeil != Long.MAX_VALUE)
			{
				sqlWhere.append(" and `uidx` <= ").append(requestedUidxCeil);
			}
			sqlWhere.append( " and state !=").append( MailStatus.MAIL_TO_DEL )
				.append(  " and foldername='"+folderName+"'");
			
			String sql = "select count(*) from mails " + sqlWhere;
			
			long lastSyncedUidx = requestedUidxCeil;
			long updatedMinUid = 0;
			long lastSyncedSeq = 0;
			
			//keep alive to server before receiving mails, "noop" command will cause updates.
			f.sendSimpleCommand("NOOP");
			for(int j=0;;j++)
			{
				reqData.syncState.maxUidInSyncing = lastSyncedUidx;
				//determine first index to start sync
				Vector<Long> uids = null;
				if(j == 0)
					uids = updateMailUid(conn, st, pst2, folder, lastSyncedUidx, requestedMailCount+5, false,reqData);
				else
					uids = updateMailUid(conn, st, pst2, folder, lastSyncedSeq-1, requestedMailCount+5, true,reqData);
				
				Vector<Long> newMails = new Vector<Long>();
				ResultSet rst2 = null;
				try 
				{
					rst2 = st.executeQuery("select `index`, cast(uid as unsigned) as uid_long from mails where uidx is null and foldername='"+folderName+"' and cast(uid as unsigned)>="+uids.firstElement()+" and cast(uid as unsigned)<="+uids.lastElement()+" and state=1 order by uid_long desc");
					while(rst2.next())
					{
						newMails.add( rst2.getLong(2));
					}
				}
				finally
				{
					DbHelper.close(rst2);
					rst2 = null;
				}
				
				long[] newMailUids_ = new long[newMails.size()];
				for(int i=0;i<newMails.size();i++)
					newMailUids_[i] = newMails.get(i);
				updateMailHeader(conn, pst, folder, fp, newMailUids_, reqData); //may fail to update headers
				
				updatedMinUid = uids.firstElement();
				if(lastSyncedUidx == updatedMinUid || updatedMinUid<=1 || updatedMinUid <= clientUidxMax)
					break; //smallest uid has encounted, stop
				lastSyncedUidx = uids.firstElement();
				lastSyncedSeq = folder.getSequenceNum(uids.firstElement());
				if(lastSyncedSeq <= 1)
					break; //reach the minimum sequence number, no more mails to sync
				ResultSet r = null;
				try
				{
					r = st.executeQuery(sql);
					
					if(r.next() && r.getInt(1) >= requestedMailCount)
						break; //OK, there's enough mail for client to retrieve
				}
				finally
				{
					DbHelper.close(r);
				}
			}
			
			//syncup deleted mails, in a certain range
			int seqHigh = folder.getSequenceNum(updatedMinUid) -1;
			try
			{
				int clientSeqHigh = folder.getSequenceNum(clientUidxMax);
				if(clientSeqHigh < seqHigh)
					seqHigh = clientSeqHigh;
			}
			catch(Exception ex)
			{}
			long maxUidOnImapServer = folder.getMessageUid(msgCount);
			String sql2 = "update mails set uidx=null, state=" + MailStatus.MAIL_TO_DEL
					+ " where state!="
					+ MailStatus.MAIL_TO_DEL
					+ " and cast(uid as unsigned)>" + maxUidOnImapServer
					+ " and foldername='"+folderName+"'";
			st.executeUpdate(sql2);
			conn.commit();
			reassignMailUidx(accountId, folderName);
			DbHelper.checkState(conn);
			if(seqHigh < 1)
			{
				sql2 = "update mails set uidx=null, state=" + MailStatus.MAIL_TO_DEL
						+ " where state!="+ MailStatus.MAIL_TO_DEL
						+ " and cast(uid as unsigned)<" + updatedMinUid
						+ " and foldername='"+folderName+"'";
				st.executeUpdate(sql2);
				reassignMailUidx(accountId, folderName);
				DbHelper.checkState(conn);
				return;
			}
			updateMailUid(conn, st, pst2, folder, seqHigh, 100, true,reqData);
			
		}
		catch(Exception ex)
		{
			try
			{
				conn.rollback();
			}
			catch (SQLException e)
			{
			}
			log.error( "Fail to syncup mail:"+reqData.client.connData.accountName, ex);
			
		}
		finally
		{
			DbHelper.close(st);
			DbHelper.close(pst);
			DbHelper.close(pst2);
			if(!DbHelper.shareSqliteConnection)
				DbHelper.close(conn);//do not close the connection until mailclient expired.
		}
	}
	
	private boolean toStop = false;
	private boolean idleWaiting = false;
	//private AtomicBoolean waitingMail = new AtomicBoolean(false);
		
	@Override
	public boolean waitingNewMail() throws InterruptedException, MessagingException
	{
		toStop = false;
		try
		{
//			waitingMail.set(true);
			String folderName = "INBOX";
			
			 
			connect(false, folderName);
			int count;
			if(folderMsgCount.containsKey(folderName))
				count= folderMsgCount.get(folderName);
			else
			{
				count = folder.getMessageCount();
				folderMsgCount.put(folderName, count);
			}
			
			log.info("count after connect is " + count);
			boolean idleAble = folder.hasCapability("IDLE");
			while(true)
			{
				if(toStop)
				{
					log.info("Stop checker, for toStop is true "+mailClient.connData.accountName);

					return false;
				}
				if(idleAble)
				{
					
					try
					{
						idleWaiting = true;
						int newMsgCount = folder.getMessageCount();
						if(count != newMsgCount)
						{
							count= newMsgCount;
							folderMsgCount.put(folderName, count);
							log.info("IDLE find New mail! "+mailClient.connData.accountName);
							return true;
							
						}
						folder.sendIdle();
					
						if(toStop)
							return false;
						log.info( "snedIdle completed, to call getMessageCount");
						newMsgCount = folder.getMessageCount();
						if(count != newMsgCount)
						{
							count= newMsgCount;
							folderMsgCount.put(folderName, count);
							log.info("IDLE find New mail! "+mailClient.connData.accountName);
							return true;
							
						}
					}
					finally
					{
						idleWaiting = false;
						
					}
					
				}
				else
				{
					folder.sendSimpleCommand("NOOP");
					int newMsgCount = folder.getMessageCount();
					//System.out.println("the new count is " + newMsgCount );
					if(count != newMsgCount)
					{
						count= newMsgCount;
						folderMsgCount.put(folderName, count);
//						folder.refreshStatus(true);
//						if(uidNext != folder.getUidNext()) //126.com will not return UIDNEXT
						log.info( "NOOP find New mail! "+mailClient.connData.accountName);
						return true;
						
					}
					
					
					Thread.sleep(MailCheckerManager.POLL_INTERVAL*1000);
					
				}
			}
		}
		finally
		{
//			synchronized(this)
//			{
//				waitingMail.set(false);
//				//log.info("SendIdel finish, to notifyAll");
//				this.notifyAll();
//			}
		}
	}
	
	@Override
	public  void  interruptWaiting() throws MessagingException, InterruptedException
	{
		
		
			toStop = true;
			if(idleWaiting)
			{
				folder.interruptIdle();
			}
	}
	

}

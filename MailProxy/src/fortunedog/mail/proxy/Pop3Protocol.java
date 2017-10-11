package fortunedog.mail.proxy;

import java.security.Security;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;

import javax.mail.AuthenticationFailedException;
import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;

import org.apache.commons.lang3.StringUtils;
import org.apache.geronimo.javamail.store.pop3.POP3Folder;

import fortunedog.mail.proxy.MailClient.ClientRequestData;
import fortunedog.mail.proxy.MailClient.ConnectionData;
import fortunedog.mail.proxy.checker.MailCheckerManager;
import fortunedog.mail.proxy.net.MailSummary;
import fortunedog.mail.proxy.net.Result;
import fortunedog.util.DbHelper;
import fortunedog.util.Utils;

class Pop3Protocol extends MailProtocol
{
	private Session session;
	private Store store ;
	POP3Folder folder;
	private Message[] allPop3Mails; //only used when server is POP3 
	//static final boolean debug= true;

	public Pop3Protocol(MailClient mailClient2)
	{
		super(mailClient2);
	}

	private boolean syncupUid()
	{
		Connection conn = null;
		Statement st = null;
		PreparedStatement pst = null;
		ResultSet rst = null;
		String folderName=null;
		ConnectionData connData = mailClient.connData;
		int accountId = connData.accountId;
		try
		{

			conn = DbHelper.getConnection(accountId);
			conn.setAutoCommit(false); //improve performance dramatically
			st = conn.createStatement();

			allPop3Mails = folder.getMessages();
			folderName=folder.getFullName();
			//way 2
			st.execute("DROP TABLE IF EXISTS `tempUid`");
			st.execute("CREATE TEMPORARY TABLE tempUid (uid VARCHAR(70), state tinyint, `index` int, PRIMARY KEY (`uid`))");
			
			pst = conn.prepareStatement("insert into tempUid values( ? ," + MailStatus.MAIL_NEW +", ?)");
			HashSet<String> set = new HashSet<String> ((int)(allPop3Mails.length*1.618));
			for(int i=0; i< allPop3Mails.length; i++)
			{
				String uid = folder.getUID( allPop3Mails[i]);
				/**
				 * For some POP3 server, known so far, 139 and 21cn, may return duplicated UIDs. For example, in account 13509855970@139.com
				 *	1380 1tbiARH2pEfpLG36+wAAms
				 *	1381 1tbiARH2pEfpLG36+wAAms
				 * was found
				 */
				{
					if(!set.add(uid))
						continue;
				}
				pst.setString(1, uid);
				pst.setInt(2, i);
				pst.addBatch();
			}		
			try
			{
				pst.executeBatch(); //get java.sql.SQLException: column uid is not unique occasionally, found in log 2013-04-16 00:06:20.953
			}
			finally
			{
				conn.commit(); //commit to DB any way, so individual record fail will not block other mails
			}		
			
			//what a pity that sqlite can not support such sql.
	//		String sql2 = "update mails set mails.index=tempUid.index where mails.uid=tempUid.uid and mails.foldername='"+folderName+"'";
			String sql2 = "update mails set `index`=(select `index` from tempUid"
				+" where mails.uid=tempUid.uid and mails.folderName='"+folderName+"')";
			//optimize this sql statement, replace "not in" with "not exists"
			String sql3 = "insert into mails (uid, `index`, state, foldername) select uid, `index`, state,'"+folderName+"' as foldername"
				+  " from tempUid where not exists ( select uid from mails where uid=tempUid.uid)";
			System.out.println(sql3);
			DbHelper.doTransaction(conn, st, sql2,sql3);
			DbHelper.checkState(conn);
			//end way2

			String sql1 = "update mails set uidx=null, `index`=-1, state=" + MailStatus.MAIL_TO_DEL 
					+ " where state!=" + MailStatus.MAIL_TO_DEL
					+ " and uid not in (select uid from tempUid)"
					+ " and foldername='"+folderName+"'";
			DbHelper.doTransaction(conn, st, sql1);
			reassignMailUidx(accountId, folderName);
			DbHelper.checkState(conn);
			return true;			
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
			log.error( "POP3 doSyncupUid2 fail:"+connData.accountName, ex);
			return false;
		}
		finally
		{
			DbHelper.close(rst);
			DbHelper.close(st);
			DbHelper.close(pst);
			if(!DbHelper.shareSqliteConnection)
				DbHelper.close(conn);
		}
	}



	@Override
	public Message retrieveRawMail(String uid) throws MessagingException
	{
		Connection conn = null;
		Statement st = null;
		ResultSet rs_t = null;
		Message msg = null;
		ConnectionData connData = mailClient.connData;
		try
		{
			if(DbHelper.useSqlite)
				conn = DbHelper.getConnection(connData.accountId);
			else
				conn = DbHelper.getConnection();
			st = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			int loopCount = 0;
			while(loopCount < 2)
			{
				loopCount++;
				if(DbHelper.useSqlite)
					rs_t = st.executeQuery("select `index` from mails where state!="+MailStatus.MAIL_TO_DEL + " and uid='"+uid +"'");
				else
				rs_t = st.executeQuery("select `index` from mails where state!="+MailStatus.MAIL_TO_DEL + " and uid='"+uid +"' and accountId=" + connData.accountId);
				int index = -1;
				boolean b = rs_t.next();
				if(b)
				{
					index = rs_t.getInt(1);
					if(index < 0)
						return null;
				}
				else
				{
					return null;
				}
				msg = allPop3Mails[index];
				if( !uid.equals(folder.getUID(msg)))
				{
					folder.close(false);
					folder = null;
					connect(true, null);
					continue;
				}
				break;
			}
		}
		catch(Exception ex)
		{
			log.error("retrieveRawMail fail", ex);
			return null;
		}
		finally
		{
			DbHelper.close(rs_t);
			DbHelper.close(st);
			if(!DbHelper.useSqlite || !DbHelper.shareSqliteConnection)
				DbHelper.close(conn);
		}
		return msg;
	}
	
	@Override
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
	public synchronized int connect(boolean forcePop3Resync,String folderName)
	{
		log.info( "Entering Pop3Protocol.connect");
		
		ConnectionData connData = mailClient.connData;

		// Get a Store object
		try
		{
			if(folder == null || ensureNewMail || !folder.isOpen() || !store.isConnected())
			
			{
				if(store != null)
				{
					try 
					{
						store.close();
					}
					catch(Throwable t)
					{
						
					}
				}

				Properties props = new Properties();
				props.putAll(System.getProperties());
				//Properties props = System.getProperties();
				if(connData.useSSL)
				{// SSL 连接需要(开始)

					Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());  
					final String SSL_FACTORY = "fortunedog.mail.proxy.DummySSLSocketFactory";  
					props.setProperty("mail.pop3s.socketFactory.class", SSL_FACTORY);  
					props.setProperty("mail.pop3.socketFactory.fallback", "false");  
					props.setProperty("mail.pop3.port", connData.mailPort+"");  
					props.setProperty("mail.pop3.socketFactory.port", connData.mailPort+"");  			// Get a Session object
				}// SSL连接需要(结束)
				//props.setProperty("mail.pop3.apop.enable", "true");  //Found NullPointException in GetMailConfig servlet, So, will this cause user's login fail?
				

				session = Session.getInstance(props, null);
				session.setDebug(MailClient.verbose/* || connData.accountName.startsWith("dliu@") || connData.accountName.startsWith("13509855970@139.com")*/);
				store = session.getStore(connData.protocol);

				if(!store.isConnected())
				{
					store.connect(connData.mailServer, connData.mailPort, connData.loginName, connData.password);
					folder = (POP3Folder) store.getDefaultFolder();
					if (folder == null) {
						//System.out.println("No default folder");
						store.close();
						log.error( "folder == null,return Result.FAIL");
						return Result.FAIL;
					}
					//System.out.println("#########################");
					listForders("/");
					//System.out.println("#########################");
				}
				
				folder = (POP3Folder) folder.getFolder(mbox);
				if (folder == null)
				{
					// System.out.println("Invalid folder");
					store.close();
					log.error( "folder == null,return Result.FAIL");
					return Result.FAIL;
				}
				try
				{
					folder.open(Folder.READ_WRITE);

				}
				catch (MessagingException e)
				{
					folder.open(Folder.READ_ONLY);
				}
				syncupUid();
			}
			else if(forcePop3Resync)
			{
				syncupUid();
			}
		}
		catch(AuthenticationFailedException ae)
		{
			return Result.AUTH_FAIL;
			
		}
		catch (NoSuchProviderException e)
		{
			log.error( "POP3 connect fail:"+connData.accountName, e);
			return Result.FAIL;
		}
		catch (Exception e)
		{
			log.error( "POP3 connect fail:"+connData.accountName, e);
			return  Result.FAIL;
		}
		finally
		{
			log.info( "Levae MailClient.connect" );
		}
		return  Result.SUCCESSED;
	}
	
	protected Folder getCurrentFolder() throws MessagingException
	{
		checkOpen();
		return folder;
	}
	
	@Override
	public int listForders(String root)
	{
		Connection conn = null;
		Statement statement=null;
		POP3Folder pop3Folder = null;
		Folder[] folders = null;
		if (root == null||root!="/")
		{
			return Result.FAIL;
		}
		try
		{
			folders = folder.list();
			conn = DbHelper.getConnection();
			statement = conn.createStatement();
			for (Folder f : folders)
			{			
				pop3Folder=(POP3Folder) f;
				statement.execute("replace into folders(accountid,foldername,displayname) values("+mailClient.connData.accountId+",'"+pop3Folder.getFullName()+"','"+pop3Folder.getName()+"')");
				//System.out.println(pop3Folder.getName());
				//System.out.println(pop3Folder.getFullName());
			}
		}
		catch (Exception e)
		{
			log.warn("listForders fail", e);
			return Result.FAIL;
		}
		
		finally
		{
			DbHelper.close(statement);
			DbHelper.close(conn);
		}
		return Result.SUCCESSED;
	}

	public void close()
	{
		try
		{
			if(folder != null)
			{
				try
				{
					if(folder.isOpen())
						folder.close(true);
				}
				catch (Exception e)
				{
				}
				folder = null;
			}
			if(store != null)
			{
				try
				{
//					if(store.isConnected()) //isConnect will try send command to server to check it's alive or not, since we are 
                                            //going to close it, checking is unnecessar
						store.close();
				}
				catch (Exception e)
				{
//					e.printStackTrace();
				}
				store = null;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	protected void syncupMail(ClientRequestData reqData)
	{
		Connection conn = null;
		Statement st = null;
		Statement st1 = null;//for mysql table
		Connection conn1 = null;//for mysql
		PreparedStatement pst = null;
		int accountId = mailClient.connData.accountId;
		try
		{
			Folder f =  getCurrentFolder();	
			conn = DbHelper.getConnection(accountId);
			st = conn.createStatement();
			conn1 = DbHelper.getConnection();
			st1 = conn1.createStatement();
			FetchProfile fp = new FetchProfile();
			fp.add(FetchProfile.Item.ENVELOPE);			
			
			conn.setAutoCommit(false);
			ResultSet rst1 = st.executeQuery("select `index`, uid from mails where uidx is null and state!=" + MailStatus.MAIL_TO_DEL +" and foldername='"+reqData.folderName+"'");
			java.util.List<MailSummary> mailList = new ArrayList<MailSummary>(100);
			try
			{
				while(rst1.next())
				{
					int index = rst1.getInt(1);
					String uid = rst1.getString(2);
					Message msg ;
					msg = allPop3Mails[index];
					Utils.ASSERT(msg != null);
					if(!uid.equals(folder.getUID(msg)))
					{
						log.warn("skip duplicated uid:{}", uid);
						continue; //something wrong, not the same message
					}
					try
					{
						MailSummary s = null;
						s = new MailSummary(accountId, uid, msg, index);
						mailList.add(s);
					}
					catch (Exception e)
					{
						log.warn("Skip message {}, because Error:"+uid, e);
					}
				}
			}
			finally
			{
				DbHelper.close(rst1);
			}
			if(mailList.size() == 0)
				return;
			MailSummary[] array = new MailSummary[mailList.size()];
			mailList.toArray(array);
			Arrays.sort(array, new java.util.Comparator<MailSummary>() 
					{
						public int compare(MailSummary o1, MailSummary o2)
						{
							int r = o1.date.compareTo(o2.date);
							if(r == 0)
							{
								return o1.uid.compareTo(o2.uid);
							}
							return -r;
						}
						public boolean equals(Object obj)
						{
							return super.equals(obj);
						}
					});
			String sql = "SELECT mailIndexCounter FROM account WHERE ID="+ mailClient.connData.accountId + " FOR UPDATE";
			
			int uidxSeed = 0;
			ResultSet rst = st1.executeQuery(sql);
			try
			{
				rst.next();
				uidxSeed = rst.getInt(1);
			}
			finally
			{
				DbHelper.close(rst);
			}
			sql = "UPDATE account SET mailIndexCounter=" + (uidxSeed + array.length) + " WHERE ID=" + mailClient.connData.accountId;
			st1.execute(sql);
			
			uidxSeed ++;
			
			
			for(int i=array.length - 1;i>=0;i--)
			{
				array[i].state = MailStatus.MAIL_NEW;
				array[i].uidx=uidxSeed+array.length-1-i;
				array[i].folderName=reqData.folderName;
			}
			reqData.syncState.offer(array);

			pst = conn.prepareStatement("UPDATE mails SET `subject`=?, `date`=?, `from`=?, `to`=?, cc=?, `state`=" + MailStatus.MAIL_NEW +", uidx=?, attachmentFlag=? WHERE uid=? AND foldername='"+reqData.folderName+"'");
			for(int i=array.length - 1;i>=0;i--)
			{
				MailSummary s = array[i];
				//updateMailSummary(pst, array[i]);
				pst.setString(1, StringUtils.abbreviate(s.subject, MailClient.DB_COLUMN_WIDTH_SUBJECT));
				pst.setTimestamp(2, new java.sql.Timestamp(s.date.getTime()));
				pst.setString(3, StringUtils.abbreviate(s.from, MailClient.DB_COLUMN_WIDTH_FROM));
				pst.setString(4, StringUtils.abbreviate(s.to, MailClient.DB_COLUMN_WIDTH_TO));
				pst.setString(5, StringUtils.abbreviate(s.cc, MailClient.DB_COLUMN_WIDTH_CC));
				pst.setInt(6, s.uidx);
				pst.setInt(7, s.attachmentFlag);
				pst.setString(8, s.uid);
				pst.addBatch();
				
			}
			conn.setAutoCommit(false);
			try
			{
				pst.executeBatch();
			}
			finally
			{
				conn.commit(); //commit to DB any way, so individual record fail will not block other mails
			}
			DbHelper.checkState(conn);
		}
		catch(Exception e)
		{
			DbHelper.checkState(conn);
			log.error("doSyncupMail2 ", e);
		}
		finally
		{
			DbHelper.close(pst);
			DbHelper.close(st);
			DbHelper.close(conn1);
			if(!DbHelper.shareSqliteConnection)
				DbHelper.close(conn);
		}
	}
	private boolean toStop = false;
//	private boolean waitingMail = false;
	private boolean ensureNewMail;
	@Override
	boolean waitingNewMail() throws MessagingException, InterruptedException
	{
		toStop = false;
		try
		{
			String folderName = "INBOX";
//			synchronized(this)
//			{
//				waitingMail = true;
//			}
			connect(false, folderName);
		
			while(true)
			{
				if(toStop)
					return false;
				int msgCount = folder.getMessageCount();
				folder.refreshMessageCount();
				if(msgCount != folder.getMessageCount())
				{
					ensureNewMail = true;
					return true;
				}
				
				Thread.sleep(MailCheckerManager.POLL_INTERVAL*1000);
				
			}
		}
		finally
		{
//			synchronized(this)
//			{
//				waitingMail = false;
//				this.notifyAll();
//			}
		}
	}
	@Override
	public void interruptWaiting() throws MessagingException, InterruptedException
	{
		
		toStop = true;
		
	}

	
}

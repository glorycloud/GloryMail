package fortunedog.mail.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.activation.DataHandler;
import javax.activation.MimetypesFileTypeMap;
import javax.mail.Address;
import javax.mail.AuthenticationFailedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimePartDataSource;
import javax.mail.internet.MimeUtility;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fortunedog.mail.proxy.MailClient.ClientRequestData.SyncState;
import fortunedog.mail.proxy.checker.MailCheckerManager;
import fortunedog.mail.proxy.exchange.ExchangeMessage;
import fortunedog.mail.proxy.net.DataPacket;
import fortunedog.mail.proxy.net.MailSummary;
import fortunedog.mail.proxy.net.Result;
import fortunedog.mail.proxy.servlet.AttachmentInfo;
import fortunedog.mail.storage.MailCache;
import fortunedog.util.DbHelper;
import fortunedog.util.Pair;
import fortunedog.util.Utils;

public class MailClient implements MailStatus 
{
	public static final int TYPE_POP3 = 1;
	public static final int TYPE_IMAP = 2;
	public static final int TYPE_EXCHANGE = 3;
	public static final String MAILER_NAME = "SmallMail";
	
	public static final int DB_COLUMN_WIDTH_CC = 512; //column width of CC in DB
	public static final int DB_COLUMN_WIDTH_SUBJECT = 512;
	public static final int DB_COLUMN_WIDTH_FROM = 100;
	public static final int DB_COLUMN_WIDTH_TO = 512;
	public static final String PLAINTEXT_CONTENT_TYPE = "text/plain; charset=UTF-8"; 
	public static final String HTML_CONTENT_TYPE = "text/html; charset=UTF-8";
	static Logger log = LoggerFactory.getLogger(MailClient.class);
	public static class ConnectionData{
		/* (non-Javadoc)
		 * @see java.lang.Object#clone()
		 */
		@Override
		public ConnectionData clone() throws CloneNotSupportedException
		{
			// TODO Auto-generated method stub
			return new ConnectionData(protocol, smtpServer, mailServer, loginName, password, accountName, mailPort, smtpPort, serverType, useSSL, accountId);
		}
		private ConnectionData(String protocol, String smtpServer, String mailServer,
				String loginName, String password, String accountName, int mailPort, int smtpPort,
				int serverType, boolean useSSL, int accountId)
		{
			super();
			this.protocol = protocol;
			this.smtpServer = smtpServer;
			this.mailServer = mailServer;
			this.loginName = loginName;
			this.password = password;
			this.accountName = accountName;
			this.mailPort = mailPort;
			this.smtpPort = smtpPort;
			this.serverType = serverType;
			this.useSSL = useSSL;
			this.accountId = accountId;
		}
		public ConnectionData()
		{
			
		}
		public String protocol;
		public String smtpServer = null;
		public String mailServer = null;
		public String loginName = null;
		public String password = null;
		public String accountName = null; //the full account name, liu_lele@126.com
		
		//port for receiver server, 110 for pop3, 992 for POP3 over SSL, 143 for imap, 993 for IMAP over
		//SSL or MMP IMAP Proxy over SSL
		public int mailPort = 110; 
		public int smtpPort = 25;
		public int serverType = TYPE_POP3; //pop3 or imap
		public boolean useSSL = false;
		public int accountId = 0;
		public String syncKey = "0"; //used by exchange active Sync, to syncup folder
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((accountName == null) ? 0 : accountName.hashCode());
			result = prime * result + ((loginName == null) ? 0 : loginName.hashCode());
			result = prime * result + mailPort;
			result = prime * result + ((mailServer == null) ? 0 : mailServer.hashCode());
			result = prime * result + ((password == null) ? 0 : password.hashCode());
			result = prime * result + ((protocol == null) ? 0 : protocol.hashCode());
			result = prime * result + serverType;
			result = prime * result + smtpPort;
			result = prime * result + ((smtpServer == null) ? 0 : smtpServer.hashCode());
			result = prime * result + (useSSL ? 1231 : 1237);
			result = (int) (prime * result + syncKey.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ConnectionData other = (ConnectionData) obj;
			if (accountName == null)
			{
				if (other.accountName != null)
					return false;
			}
			else if (!accountName.equals(other.accountName))
				return false;
			if (loginName == null)
			{
				if (other.loginName != null)
					return false;
			}
			else if (!loginName.equals(other.loginName))
				return false;
			if (mailPort != other.mailPort)
				return false;
			if (mailServer == null)
			{
				if (other.mailServer != null)
					return false;
			}
			else if (!mailServer.equals(other.mailServer))
				return false;
			if (password == null)
			{
				if (other.password != null)
					return false;
			}
			else if (!password.equals(other.password))
				return false;
			if (protocol == null)
			{
				if (other.protocol != null)
					return false;
			}
			else if (!protocol.equals(other.protocol))
				return false;
			if (serverType != other.serverType)
				return false;
			if (smtpPort != other.smtpPort)
				return false;
			if (smtpServer == null)
			{
				if (other.smtpServer != null)
					return false;
			}
			else if (!smtpServer.equals(other.smtpServer))
				return false;
			if (useSSL != other.useSSL)
				return false;
			if (syncKey != other.syncKey)
				return false;
			return true;
		}
		
	}
	public ConnectionData connData = new ConnectionData();
	public static class ClientRequestData 
	{
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + requestedMailCount;
			result = prime * result + (int) (requestedUidxCeil ^ (requestedUidxCeil >>> 32));
			result = prime * result + (int) (uidxMax ^ (uidxMax >>> 32));
			if(client!=null)
			{
				int id = client.connData.accountId;
				result = prime * result + (int)(id ^ (id >>> 32));
			}
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ClientRequestData other = (ClientRequestData) obj;
			if (requestedMailCount != other.requestedMailCount)
				return false;
			if (requestedUidxCeil != other.requestedUidxCeil)
				return false;
			if (uidxMax != other.uidxMax)
				return false;
			if(client != other.client)
				return false;
			return true;
		}

		public long uidxMax;


		public long requestedUidxCeil = Long.MAX_VALUE;

		public int requestedMailCount;
		
		public String folderName;
		MailClient client = null;
		
		
		
		public SyncState syncState;
		public static class SyncState
		{
			public  final Object event = new Object();
			volatile long  maxUidInSyncing = 0; //the separator, mail's UID great than this value has been synchronized, less than this
												//this value is to be synchronized
			volatile AtomicBoolean syncFinished = new AtomicBoolean(false);
			private final Vector<MailSummary> newMails = new Vector<MailSummary>(100);
			public void offer(MailSummary[] s)
			{
				for(MailSummary i:s)
					newMails.add(i);
				synchronized(event)
				{
					event.notifyAll();
				}
			}
			
			public Iterator<MailSummary> getSyncedMails()
			{
				return new Iterator<MailSummary>() {
					int index = 0;
					@Override
					public void remove()
					{
						return;
					}
					
					@Override
					public MailSummary next()
					{
						return newMails.get(index++);
					}
					
					/**
					 * return true if there's more mails available.
					 * false if sync is finished, and all mails has been iterated 
					 * This function block until mails are available.
					 */
					@Override
					public boolean hasNext()
					{
						for(int i=0;i<6;i++) //wait 30 seconds totally, 6*5
						{
							if( index < newMails.size())
								return true;
							if(syncFinished.get())
								return false;
							synchronized(event)
							{
								try
								{
									event.wait(5*1000);//wait 5 seconds
								}
								catch (InterruptedException e)
								{
									e.printStackTrace();
								}
							}
						}
						log.error("SyncState hasNext retun abnormal on timeout");
						return false;
					}
				};
			}
		}
	}
//	private ClientRequestData clientRequestData;
	int mailCount = 0; //count of mails in INBOX
	
	/**
	 * a counter to indicate MailClient object usage state. 0 means this object is free and ok for any use.
	 * positive number indicates this object is used by normal user operation.
	 * negative number indicates this object is used by IDLE mail checker.
	 */
	private int state = 0; 
	MailProtocol mailProtocol;
	
	
	private static HashMap<ConnectionData, MailClient> pool = new HashMap<MailClient.ConnectionData, MailClient>();
	
	/**
	 * contains client request those are waiting for syncup
	 */
	private static ArrayDeque<ClientRequestData> waitingQueue = new  ArrayDeque<ClientRequestData>();
	/**
	 * contains client request those are waiting for syncup, request data is moved to this queue if another request 
	 * has same account ID is in syning
	 */
	private static ArrayDeque<ClientRequestData> waitingQueue2 = new  ArrayDeque<ClientRequestData>();
	
	/**
	 * contains client request those are being syncup
	 */
	private static HashSet<ClientRequestData> syncingQueue = new HashSet<ClientRequestData>();
	
	private static final Semaphore updateSemaphore = new Semaphore(0, false);
	private static final int MAX_WORK_THREAD_COUNT = 10;
	private static ThreadGroup updateThreadGroup = new ThreadGroup("MailSynchronizeGroup");
	public static boolean verbose = false;
	static
	{
		System.setProperty("mail.mime.encodeeol.strict", "false");
		
		Context env;
		try
		{
			env = (Context) new InitialContext().lookup("java:comp/env");
			verbose = (Boolean)env.lookup("verbose");
		}
		catch (NamingException e)
		{
			
		}

			
	}
	
	private MailClient(ConnectionData data)
	{
		this.connData = data;
		switch(data.serverType)
		{
		case TYPE_POP3:
			mailProtocol = new Pop3Protocol(this);
			break;
		case TYPE_IMAP:
			mailProtocol = new ImapProtocol(this);
			break;
		case TYPE_EXCHANGE:
			mailProtocol = new ExchangeProtocol(this);
		default:
			log.info( "Invalid serverType code:"+data.serverType);
		}
	}
	
	/**
	 * add this sync request to queue, return a request data. 
	 * The returned request data may be the same as reqData passed in this method, 
	 *  if this request has not already exists in queue.
	 * The returned request data may different with reqData passed in, if there's already
	 *  a same request exists in queue.
	 * In any case, assertion ' returnObject.equals(reqData)' always be true, but may not true for returnObject==reqData
	 * @param client
	 * @param reqData
	 * @return
	 *
	 */
	protected static synchronized ClientRequestData addToSyncQueue(MailClient client, ClientRequestData reqData)
	{
		log.trace( "Add account to syncing queue:"+client.connData.accountName);
//		if(syncingQueue.containsKey(client.connData.accountId))
//			return false;
		
		reqData.client = client;
		
		@SuppressWarnings("unchecked")
		Iterator<ClientRequestData>[] allReqs = new Iterator[]{syncingQueue.iterator(), waitingQueue.iterator(), waitingQueue2.iterator() };
		
		for(Iterator<ClientRequestData> it : allReqs)
		{
			while(it.hasNext())
			{
				ClientRequestData old = it.next();
				if(old.equals(reqData))
				{
					log.trace( "Account already in queue:"+client.connData.accountName);
					reqData.syncState = old.syncState;
					return old;
				}
			}
		}
		
		
		reqData.syncState = new SyncState();
		reqData.syncState.maxUidInSyncing = Long.MAX_VALUE;
		waitingQueue.add(reqData);
//		syncingQueue.add(reqData);
		updateSemaphore.release();
		
		if(updateSemaphore.getQueueLength() == 0 && waitingQueue.size() != 0 && updateThreadGroup.activeCount() < MAX_WORK_THREAD_COUNT)
		{
			Thread t = new SyncUpThread(updateThreadGroup, "MailSyncThread_"+updateThreadGroup.activeCount());
			t.start();
		}
		return reqData;
	}
	
	//called in work thread, block until a syncup request is added
	private static  ClientRequestData getOneForSync()
	{
		while(true){
			try
			{
				updateSemaphore.acquire();
			}
			catch (InterruptedException e)
			{
				
				log.error( "Fail in getOneForSync", e);
				return null;
			}
			synchronized(MailClient.class)
			{
	//			waitingQueue.
				while(!waitingQueue.isEmpty())
				{
					ClientRequestData req = waitingQueue.poll();
					if(!inSyncing(req.client.connData.accountId, 0))
					{
						syncingQueue.add(req);
						return req;
					}
					waitingQueue2.add(req);
				}
				
			}
		}
	}
	
	private static synchronized void removeFromSync(ClientRequestData req)
	{
		waitingQueue.remove(req);
		syncingQueue.remove(req);
		
		waitingQueue.addAll(waitingQueue2);
		updateSemaphore.release(waitingQueue2.size());
		waitingQueue2.clear();
		
		log.trace(  "Remove account from syncing queue:"+req.client.connData.accountId);
	}
	
	private static synchronized boolean inSyncing(int accountId, long uid)
	{
		log.trace( "Check is in syncing for account:"+accountId);
		for(ClientRequestData req : syncingQueue)
		{
			MailClient c =req.client;
			if(c.connData.accountId == accountId)
				if(req.syncState.maxUidInSyncing > uid)
					return true;
		}
		
		return false;
	}
	
	
	public static MailClient getClientInstance(String accountName, String password, boolean forcePop3Resync) throws SQLException, InterruptedException
	{
		Connection conn = null;
		PreparedStatement st = null;
		ResultSet rst = null;
		ConnectionData data = new ConnectionData();
		data.accountName = accountName;
		data.password = password;
		try
		{
			conn = DbHelper.getConnection();
			st = conn.prepareStatement("select * from account where name=?");
			st.setString(1, accountName);
			rst = st.executeQuery();
			if(rst.next())
			{
				data.accountId = rst.getInt("ID");
				data.mailServer = rst.getString("mailServer");
				data.smtpServer = rst.getString("smtpServer");
				data.useSSL = rst.getInt("useSSL") != 0;
				String serverType = rst.getString("serverType");
				if("imap".equals(serverType))
				{
					data.protocol =  data.useSSL?"imaps":"imap";
					data.serverType = TYPE_IMAP;
				}
				else if("pop3".equals(serverType))
				{
					data.protocol = data.useSSL?"pop3s":"pop3";
					data.serverType = TYPE_POP3;
				}
				else if("exchange".equals(serverType))
				{
					data.protocol = "exchange";
					data.serverType = TYPE_EXCHANGE;
				}
				data.mailPort = rst.getInt("mailPort");
				data.smtpPort = rst.getInt("smtpPort");
				data.loginName = rst.getString("loginName");
				data.syncKey = rst.getString("syncKey");
				if(data.syncKey == null)
					data.syncKey = "0";
			}
		}
		finally
		{
			DbHelper.close(rst);
			DbHelper.close(st);
			DbHelper.close(conn);
		}
		
		MailClient client = null;
		synchronized(pool)
		{
			client = pool.get(data);
			
			if(client == null)
			{
				client = new MailClient(data);
				pool.put(data, client);
			}
			client.addRef();
		}
		
		client.enterUserState();
		try
		{
			if(client.mailProtocol.connect(forcePop3Resync,null) == Result.SUCCESSED)
			{
				return client;
			}
			else
			{
				client.decRef();
			}
		}
		finally
		{
			client.quiteUserState();
		}
		return null;
		
		
		
	}
	
	/**
	 * initiate a check new mail request. 
	 * block current thread until check new completed.
	 * @param reqData
	 * @return true if check new was performed, otherwise false
	 * @throws MessagingException
	 * @throws InterruptedException 
	 *
	 */
	public int checkNewMails(ClientRequestData reqData) throws MessagingException, InterruptedException
	{
		enterUserState();
		try
		{
			return mailProtocol.checkNewMails(reqData);
		}
		finally
		{
			quiteUserState();
		}
	}
		
	void dumpMessage(Message msg, PrintStream out) throws MessagingException
	{
		StringBuilder sb = new StringBuilder("<html><title>");
		sb.append(msg.getSubject());
		sb.append("</title>\n<body>\n<p>");
		sb.append(msg.isMimeType("text/plain"));
	}
	

	
	@Override
	protected void finalize() throws Throwable
	{
		mailProtocol.close();
		super.finalize();
	}

	public void close()
	{
		mailProtocol.close();
	}
	
	public StructurizedMail getMail(HttpSession session, String uid,String folderName)
	{//reserve old style for calling this interface
		return getMail(session,uid,folderName,false);
	}
	
	@SuppressWarnings("unchecked")
	public StructurizedMail getMail(HttpSession session, String uid, String folderName, boolean multiPage)
	{
		Pair<String, StructurizedMail> p =  null;
		if(session != null)
		{//try load from session
			p=(Pair<String, StructurizedMail>) session.getAttribute("currentMailPager");
			if (p != null && p.key.equals(uid+folderName))
				return p.value;
		}
		MailCache cache =null;
		StructurizedMail mail =null;
		try
		{//try load from db cache
			cache = new MailCache(connData.accountId);
			mail = cache.get(folderName, uid);
			
			
		}
		catch(Throwable t)
		{
			log.warn("MailCache fail. ", t);
		}
		if (mail == null)
		{//load from mail server
			if(mailProtocol instanceof ExchangeProtocol)
			{
				if (mailProtocol.connect(false, folderName) != Result.SUCCESSED)
					return null;
				try
				{
					ExchangeMessage msg = ((ExchangeProtocol)mailProtocol).retrieveExchangeMail(uid);
					if (msg == null)
						return null;
					MailSummary s = MailSummary.createFromDb(uid, this.connData.accountId, folderName);
					mail = new StructurizedMail(msg, s);
				}
				catch (Exception e)
				{
					log.error("Fail create StructurizedMail", e);
					return null;
				}
				
			}
			else
			{
				Message msg = null;
				try
				{
					if(msg == null)
					{//not found in cache, get from network
						if (mailProtocol.connect(false, folderName) != Result.SUCCESSED)
							return null;
						msg = mailProtocol.retrieveRawMail(uid);
					}
					
					
					
				}
				catch (Throwable e)
				{// we can get OutOfMemory
					log.error("Fail getMail", e);
				}
				if (msg == null)
					return null;
				try
				{
					MailSummary s = MailSummary.createFromDb(uid, this.connData.accountId, folderName);
					mail = new StructurizedMail(msg, s, multiPage);
				}
				catch (Exception e)
				{
					log.error("Fail create StructurizedMail", e);
					return null;
				}
			}
			if(mail != null && cache != null)
			{//save to cache
				try
				{
					cache.put(folderName, uid, mail);
				}
				catch(Throwable t)
				{
					log.warn("MailCache fail. ", t);
				}
			}

		}
		if(session != null)
		{
			p = new Pair<String, StructurizedMail>(uid+folderName, mail);
			session.setAttribute("currentMailPager", p);
		}
		return mail;
	}
	

	public boolean checkOpen() 
	{
		try
		{
			return mailProtocol.checkOpen();
		}
		catch (MessagingException e)
		{
			e.printStackTrace();
		}
		return false;
	}

	
	
	static final class SyncUpThread extends Thread
	{
		private MailClient mailClient;
		private ClientRequestData reqData;
		SyncUpThread(ThreadGroup group, String name)
		{
			super(group, name);
			setPriority(Thread.MIN_PRIORITY);
		}

		public void run()
		{
			try {
				
				//Logger log = Logger.getLogger(getClass().getName());
				while(true)
				{
					String accountName = null;
					reqData= getOneForSync();
					mailClient = reqData.client;
					this.setName("MailSyncThread_"+mailClient.connData.accountName);
					try
					{
						reqData.client.enterUserState();
					}
					catch (InterruptedException e)
					{
						continue;
					}
					if(mailClient == null)
						continue;
					try
					{
						synchronized (mailClient)
						{
							reqData.syncState.maxUidInSyncing = Long.MAX_VALUE;
							accountName = mailClient.connData.accountName;
							log.info( "Start sync [MailServer => CloudServer]");
							if(!mailClient.mailProtocol.checkOpen())
							{
								continue;
							}
							try
							{
								mailClient.mailProtocol.syncupMail(reqData);
							}
							finally
							{
								synchronized(reqData.syncState.event)
								{
									reqData.syncState.syncFinished.set(true);
									reqData.syncState.event.notifyAll();
								}
							}
						}
						mailClient.cacheMails();
	
					}
					catch(Exception e2)
					{
						log.warn( "Mail Syhchronization error:" , e2);
					}
					finally
					{
						log.info( "Finish sync [MailServer => CloudServer]");
						reqData.client.quiteUserState();
						removeFromSync(reqData);
					}
				}
			}
			finally
			{
				
			}
			
		}
	}
	
	//save mails to cache
	void cacheMails() throws InterruptedException
	{
		enterUserState();
		try
		{
			MailCache c = new MailCache(this.connData.accountId);
			c.cacheMails(this);
		}
		finally
		{
			quiteUserState();
		}
	}
	public Result sendMail(HttpServletRequest req, HttpSession httpSession, DataPacket msgData, Map<String, FileItem> newAttaches)
	{
		log.info( "Entering MailClient.sendMail");
		SmtpTransport transport = new SmtpTransport(connData);
		try
		{
			ResourceBundle rb = Utils.getResourceBundle(req);
			MimeMessage msg ;
			msg = transport.createEmptyMessage();
			String bodyContentType = HTML_CONTENT_TYPE;
			StructurizedMail oldMail = null;
			//Message oldMsg = null;
			MailSummary oldSummary = null;
			if(!Utils.isEmpty(msgData.refMailId))
			{
				oldMail = getMail(httpSession, msgData.refMailId,msgData.refMailFolder);
				oldSummary = oldMail.getSummary();
			}

			if(msgData.packetType.equals(DataPacket.NEWMAIL_TYPE))
			{
			//	msg.setText(msgData.bodyText);
				bodyContentType = PLAINTEXT_CONTENT_TYPE;
			}
			else if(msgData.packetType.equals(DataPacket.REPLYMAIL_TYPE))
			{
				String oldMsgId = oldSummary.getMessageId();
				if(!Utils.isEmpty(oldMsgId))
				{
					msg.setHeader("In-Reply-To", oldMsgId);
					msg.setHeader("References", oldMsgId);
				}
				if(msgData.quoteOld)
				{
					StringBuilder newBody = new StringBuilder("<p>");
					newBody.append(msgData.bodyText.replaceAll("(\\r\\n)|\\r|\\n", "<br>"));
					String addrs = oldSummary.getFrom();
					if (!Utils.isEmpty(addrs))
					{
						newBody.append("<br>");
						newBody.append(addrs);
						Date sendDate = oldSummary.getSentDate();
						if(sendDate != null)
						{
							newBody.append(rb.getString("at"));
							SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							newBody.append(sdf.format(sendDate));
						}
						newBody.append(rb.getString("wrote"));
					}
	
					newBody.append("<blockquote\r\n");
					if(!Utils.isEmpty(oldMsgId))
					{
						newBody.append(" cite=\"mid:").append(oldMsgId).append("\"\r\n");
					}
					newBody.append(" type=\"cite\">").append(oldMail.getRawMailBody()).append(" </blockquote></p>");
								
//					msg.setDataHandler(new DataHandler(
//							new ByteArrayDataSource(newBody.toString(), "text/html")));
					msgData.bodyText = newBody.toString();
					//msg.setContent(newBody.toString(), "text/html; charset=UTF-8");
				}
				else
				{
					bodyContentType = PLAINTEXT_CONTENT_TYPE;
					//msg.setText(msgData.bodyText);
				}
			}
			else if(msgData.packetType.equals(DataPacket.FORWARDMAIL_TYPE))
			{
				String oldMsgId = oldSummary.getMessageId();
				if(!Utils.isEmpty(oldMsgId))
				{
					msg.setHeader("In-Reply-To", oldMsgId);
					msg.setHeader("References", oldMsgId);
				}
				
				StringBuilder newBody = new StringBuilder("<p>");
				newBody.append(msgData.bodyText.replaceAll("(\\r\\n)|\\r|\\n", "<br>"));
				newBody.append(rb.getString("forwardedMail"));
				newBody.append(rb.getString("forwardedSubject")).append(oldSummary.getSubject()); //should call MimeUtility.decodeText ?
				newBody.append(rb.getString("fwdDate"));
				Date sendDate = oldSummary.getSentDate();
				if(sendDate != null)
				{
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					newBody.append(sdf.format(sendDate));
				}
				newBody.append(rb.getString("fwdFrom"));
				String addrs = oldSummary.getFrom();
				if (!Utils.isEmpty(addrs))
				{
					newBody.append(addrs);
				}
				newBody.append(rb.getString("fwdTo"));
				String tos = oldSummary.getTo();
				
				newBody.append(tos);
				
				String ccs = oldSummary.getCc();
				if(!Utils.isEmpty(ccs))
				{
					newBody.append(rb.getString("fwdCc"));
				
					newBody.append(ccs);
				}

				newBody.append("<br>").append(oldMail.getRawMailBody()).append("</p>");

				msgData.bodyText = newBody.toString();
			}
			
			boolean isMixedMsg = false;
			if(msgData.attachments.size() > 0)
			{
				MimeMultipart mp = new MimeMultipart("mixed");
				MimeBodyPart mbp = new MimeBodyPart();
				mbp.setContent(msgData.bodyText, bodyContentType);
				mp.addBodyPart(mbp);
				for(AttachmentInfo info : msgData.attachments)
				{
					if(info.index == AttachmentInfo.ALL_REFATTACH_INDEX)
					{
						if(!msgData.forwardAttach)
							continue; //don't forward original attachment
						for(MailPart p : oldMail.attachmentParts)
						{
							MimePartDataSource ds = new MimePartDataSource((MimePart)p);
							DataHandler dh = new DataHandler(ds);
							MimeBodyPart bp = new ForwardBodyPart();
							bp.setDataHandler(dh);
							bp.setDisposition(p.getDisposition());
							mp.addBodyPart(bp);
							isMixedMsg = true;
						}
					}
					else if(info.index >= 0)
					{
						if(!msgData.forwardAttach)
							continue; //don't forward original attachment
						MimePart p = (MimePart) oldMail.attachmentParts.get(info.index);
						MimePartDataSource ds = new MimePartDataSource((MimePart)p);
						DataHandler dh = new DataHandler(ds);
						MimeBodyPart bp = new ForwardBodyPart();
						if(!Utils.isEmpty(info.fileName))
							bp.setFileName(javax.mail.internet.MimeUtility.encodeText(info.fileName));
						bp.setDataHandler(dh);
						bp.setDisposition(p.getDisposition());
						mp.addBodyPart(bp);
						isMixedMsg = true;
					}
					else if(!Utils.isEmpty(info.fileName) )
					{
						FileItem item = newAttaches.get(info.fileName);
						
						
						FileItemDataSource ds = new FileItemDataSource(item);
						
						DataHandler dh = new DataHandler(ds);
						
						MimeBodyPart bp = new ForwardBodyPart();
						
						bp.setFileName(javax.mail.internet.MimeUtility.encodeText(ds.getName()));
						bp.setDataHandler(dh);
						mp.addBodyPart(bp);
						isMixedMsg = true;
					}
				}
				msg.setContent(mp);
				
			}
			
			if(!isMixedMsg)
				msg.setContent(msgData.bodyText, bodyContentType);
			
				
			msg.setFrom(new InternetAddress(connData.accountName));
			if(!Utils.isEmpty(msgData.toList))
			{
				msg.setRecipients(Message.RecipientType.TO, parseAddress(msgData.toList.replace(" ", "")));
			}
			if(!Utils.isEmpty(msgData.ccList))
			{
				msg.setRecipients(Message.RecipientType.CC, parseAddress(msgData.ccList.replace(" ", "")));
			}
			if(!Utils.isEmpty(msgData.bccList))
			{
				msg.setRecipients(Message.RecipientType.BCC, parseAddress(msgData.bccList.replace(" ", "")));
			}
			msg.setSubject(msgData.subject);
//		    msg.setHeader("X-Mailer", MAILER_NAME);//this cause hotmail to refuse send
		    msg.setSentDate(new Date());
		    if(connData.smtpServer.equalsIgnoreCase("smtp.live.com"))
		    {
		    	connData.useSSL=false;
		    }
		    msg.saveChanges();
		    transport.sendMessage(msg);
			
		}
		catch(SendFailedException f)
		{
			f.printStackTrace();
			return new Result(Result.MSGSEND_FAIL, f.getMessage());
		}
		catch (MessagingException e)
		{
			e.printStackTrace();
			return new Result(Result.FAIL, e.getMessage());
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return new Result(Result.FAIL, e.getMessage());
		}
		log.info( "Leave MailClient.sendMail");
		return new Result();
	}
	

	/**
	 * check is any mails in rang (maxUid:*) still in syncing 
	 * @param maxUid
	 * @return
	 */
	public boolean  inSynching(long maxUid)
	{
		boolean result =  inSyncing(this.connData.accountId, maxUid);
		log.debug( "syncing return "+(result?"true":"false")+",means syncing "+(result?"not ":"")+"finished.");
		return result;
	}
	
	public static Address[] parseAddress(String addrs) throws AddressException
	{
		String [] ad = addrs.split(";");
		Vector<Address> v = new Vector<Address>(ad.length);
		for(String s : ad)
		{
			if(Utils.isEmpty(s))
				continue;
			
			if(s.contains("<"))
			{
				int i=s.indexOf('<');
			
				try
				{
					v.add(InternetAddress.parse(MimeUtility.encodeText(s.substring(0,i))+s.substring(i), false)[0]);
				}
				catch (UnsupportedEncodingException e)
				{
					e.printStackTrace();
				}
			
			}
			else 
			{
				v.add(InternetAddress.parse(s, false)[0]);
			}
				
			
			

		}
		return v.toArray(new Address[v.size()]);
	}
	
	private int refCount = 0;
	public synchronized void addRef()
	{
		refCount ++;
	}
	
	public synchronized void decRef()
	{
		refCount --;
		if(refCount == 0)
		{
			pool.remove(connData);
			mailProtocol.close();
		}
	}

//	public synchronized void setClientRequestData(ClientRequestData reqData)
//	{
//		this.clientRequestData = reqData;
//		
//	}
//	
//	public synchronized ClientRequestData getClientRequestData()
//	{
//		return clientRequestData;
//	}
	
	public boolean isImap()
	{
		return mailProtocol instanceof ImapProtocol;
	}
	/**
	 * return true on new mail reached.
	 * return false if interrupted by calling interrupt waiting
	 * throw Authentication
	 */
	public boolean waitingNewMail() throws AuthenticationFailedException, MessagingException, InterruptedException
	{
		
		Utils.ASSERT(state < 0);
		return mailProtocol.waitingNewMail();
		
	}
	
	int userWaiting = 0;
	public synchronized void enterUserState() throws InterruptedException 
	{
		
			userWaiting ++;
			try
			{
				while(state < 0)
				{
					MailCheckerManager.stopPush(connData.accountName);
					this.wait();//wait quit waiting state to notify
				}
				state ++;
				log.info( "After Enter UserState,  state:"+state);
			}
			finally
			{
				userWaiting --;
			}
	}

	
	public  synchronized  void quiteUserState()
	{
		state --;
		log.info( "After Quit UserState, state:"+state);
		if(state == 0)
			this.notifyAll();
	}
	
	public synchronized void enterWaitingState() throws InterruptedException 
	{
		while(state != 0 || userWaiting != 0)
		{
			this.wait(); //wait quitUserState to notify
		}
		state --;
		log.info( "After Enter Waiting State,  state:"+state);
		
	}
	public  synchronized void quitWaitingState()
	{
		state ++;
		log.info( "After Quit Waiting State,  state:"+state);
		if(state == 0)
			this.notifyAll();
	}
	
	public void interruptWaiting() throws InterruptedException
	{
		
		try
		{
			mailProtocol.interruptWaiting();
		}
		catch (MessagingException e)
		{
			log.error( "interruptWaiting fail", e);
		}
		
	}

}

class ForwardBodyPart extends MimeBodyPart
{
	@Override
	public String getContentType()throws MessagingException
	{
		return getDataHandler().getContentType();
	}
}

class FileItemDataSource implements javax.activation.DataSource
{
	FileItem item;
	public FileItemDataSource(FileItem i)
	{
		item = i;
	}
	
	@Override
	public String getContentType()
	{
		return MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(item.getName());
	}

	@Override
	public InputStream getInputStream() throws IOException
	{
		return item.getInputStream();
	}

	@Override
	public String getName()
	{
		String name = item.getName();
		int lastPos = name.lastIndexOf('/');
		if(lastPos == -1)
			name.lastIndexOf('\\');
		String fileNameWithoutPath = name.substring(lastPos+1);
		return fileNameWithoutPath;
	}

	@Override
	public OutputStream getOutputStream() throws IOException
	{
		Utils.ASSERT(false);
		return null;
	}
	
}

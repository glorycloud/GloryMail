package fortunedog.mail.proxy.servlet;

import java.io.IOException;
import java.net.URLDecoder;
import java.security.Security;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;

import javax.mail.AuthenticationFailedException;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.servlet.ServletException;

import org.apache.geronimo.javamail.store.imap.IMAPFolder;
import org.apache.geronimo.javamail.store.pop3.POP3Folder;

import fortunedog.mail.proxy.MailClient;
import fortunedog.mail.proxy.MailClient.ConnectionData;
import fortunedog.mail.proxy.net.MailConfigResult;
import fortunedog.mail.proxy.net.Result;
import fortunedog.util.Utils;


public class GetMailConfig extends DatabaseServlet
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 9180043710300091568L;
	private static final int imapPortUseSSL=993;
	private static final int smtpPortUseSSL=465;
	private static final int popPortUseSSL=995;
	private static final int imapPort=143;
	private static final int smtpPort=25;
	private static final int popPort=110;
	private static final String CONNECTION_TIMEOUT="30000";//30 seconds
	private class IMAPPOPConfigValidate extends Thread
	{
		public ConnectionData connData;
		public int result=Result.FAIL;
		Object waitingLock = null;
		
		public IMAPPOPConfigValidate(ConnectionData connData, Object waitingLock)
		{
			this.connData=connData;
			this.waitingLock = waitingLock;
			this.setName("ConfigValidate:"+connData.accountName+connData.protocol+connData.mailServer+connData.mailPort);
		}
		@Override
		public void run()
		{
			try
			{
				this.setName("TryMailServer:"+connData.mailServer+(connData.useSSL ? "SSL":"NO_SSL"));
				if(connData.serverType == MailClient.TYPE_IMAP)
				{
					if(testImapConnect(connData)==Result.SUCCESSED)
					{
						result=Result.SUCCESSED;
					}
					
				}
				else
				{
					if(testPopConnect(connData)==Result.SUCCESSED)
					{
						result=Result.SUCCESSED;
					}
				}
			}
			finally
			{
				synchronized(waitingLock)
				{
					waitingLock.notify();
				}
			}
			
		}

	}
	@Override
	Result dbService(ServiceData d) throws ServletException, IOException, SQLException,
			NeedLoginException
	{
		String account = URLDecoder.decode(d.request.getParameter("account"),"UTF-8");
		int pos = account.indexOf('@');
		if(pos <= 0)
		{
			return new Result(Result.FAIL, "Unknown mail server.");
		}
		String domainName = account.substring(pos+1);
		if(Utils.isEmpty(domainName))
		{
			return new Result(Result.FAIL, "Unknown mail server.");
		}
		
		d.dbRst = d.dbStat.executeQuery("select * from knownServers where domainName='"+domainName+"' and enterprise='0'");
		if(d.dbRst.next())
		{
			String loginName = d.dbRst.getString("loginName");
			return new MailConfigResult(account, loginName, d.dbRst.getString("mailServer"), 
			                            d.dbRst.getString("smtpServer"), d.dbRst.getString("serverType"), d.dbRst.getString("smtpPort"), 
			                            d.dbRst.getString("useSSL"), d.dbRst.getString("mailPort"));
		}
		
		//not recognized domain name, use nslookup
		String newDomainName = doLookup(domainName);
		if(!Utils.isEmpty(newDomainName))
		{
			d.dbRst = d.dbStat.executeQuery("select * from knownServers where '" + newDomainName
											+ "' like concat('%',`domainName`)"
											+ " and enterprise='1'");
			if (d.dbRst.next())
			{
				String loginName = d.dbRst.getString("loginName");
				loginName = loginName.replace("$DOMAIN", domainName);
				return new MailConfigResult(account, loginName, d.dbRst.getString("mailServer"),
											d.dbRst.getString("smtpServer"),
											d.dbRst.getString("serverType"),
											d.dbRst.getString("smtpPort"),
											d.dbRst.getString("useSSL"),
											d.dbRst.getString("mailPort"));
			}
		}
		String rawPass = d.request.getParameter("password");
		if(Utils.isEmpty(rawPass))
			return new Result(Result.FAIL, "Unknown mail server");
		String password= URLDecoder.decode(rawPass,"UTF-8");

		try
		{
			return guessMailConfig(account, password, pos, domainName, newDomainName);
		}
		catch (InterruptedException e)
		{
			return new Result(Result.FAIL, "Unknown mail server");
		}
		
//		log.info( "can not find server for account:"+account+",the domain name is "+newDomainName);
//		return new Result(Result.FAIL, "Unknown mail server");
	}
	private Result guessMailConfig(String account, String password, int pos, String domainName, String newDomainName) throws InterruptedException
	{
		
		final String imapServer="imap."+domainName;
		final String popServer="pop."+domainName;
		final String pop3Server="pop3."+domainName;
		final String smtpServer="smtp."+domainName;
		Object waitingLock = new Object();
		ArrayList<IMAPPOPConfigValidate> workers = new ArrayList<GetMailConfig.IMAPPOPConfigValidate>();
		//connData.accountId未赋值
		try
		{
			String[] loginNames=new String[] {account, account.substring(0,pos)};
			for(int i=0;i<loginNames.length;i++)
			{
				MailClient.ConnectionData connData=new MailClient.ConnectionData();
				connData.accountName=account;
				connData.loginName=loginNames[i];
				connData.password=password;
				
				
				
				connData.serverType=MailClient.TYPE_IMAP;
				connData.protocol="imap";
				connData.mailServer=imapServer;
				connData.mailPort=imapPortUseSSL;
				connData.smtpServer=smtpServer;
				connData.smtpPort=smtpPortUseSSL;
				connData.useSSL=true;
				workers.add(new IMAPPOPConfigValidate(connData, waitingLock));
	
				MailClient.ConnectionData connData1=connData.clone();
				connData1.mailPort=imapPort;
				connData1.smtpPort=smtpPort;
				connData1.useSSL=false;
				workers.add(new IMAPPOPConfigValidate(connData1, waitingLock));
				
				MailClient.ConnectionData connData2=connData1.clone();
				connData2.serverType=MailClient.TYPE_POP3;
				connData2.protocol="pop3";
				connData2.mailServer=popServer;
				connData2.mailPort=popPortUseSSL;
				connData2.smtpServer=smtpServer;
				connData2.smtpPort=smtpPortUseSSL;
				connData2.useSSL=true;
				workers.add(new IMAPPOPConfigValidate(connData2, waitingLock));
				
				MailClient.ConnectionData connData3=connData2.clone();
				connData3.mailPort=popPort;
				connData3.smtpPort=smtpPort;
				connData3.useSSL=false;
				workers.add(new IMAPPOPConfigValidate(connData3, waitingLock));
				
				MailClient.ConnectionData connData4 = connData3.clone();
				
				connData4.mailServer=pop3Server;
				connData4.mailPort=popPortUseSSL;
				connData4.smtpPort=smtpPortUseSSL;
				connData4.useSSL=true;
				workers.add(new IMAPPOPConfigValidate(connData4, waitingLock));
				
				MailClient.ConnectionData connData5=connData4.clone();
				connData5.mailPort=popPort;
				connData5.smtpPort=smtpPort;
				connData5.useSSL=false;
				workers.add(new IMAPPOPConfigValidate(connData5, waitingLock));
				
				if(!Utils.isEmpty(newDomainName))
				{
					MailClient.ConnectionData connData6=connData5.clone();
					connData6.serverType=MailClient.TYPE_IMAP;
					connData6.protocol="imap";
					connData6.mailServer=newDomainName;
					connData6.smtpServer=newDomainName;
					connData6.mailPort=imapPortUseSSL;
					connData6.smtpPort=smtpPortUseSSL;
					connData6.useSSL=true;
					workers.add(new IMAPPOPConfigValidate(connData6, waitingLock));
					
					MailClient.ConnectionData connData7=connData6.clone();
					connData7.mailPort=imapPort;
					connData7.smtpPort=smtpPort;
					connData7.useSSL=false;
					workers.add(new IMAPPOPConfigValidate(connData7, waitingLock));
					
					MailClient.ConnectionData connData8=connData7.clone();
					connData8.serverType=MailClient.TYPE_POP3;
					connData8.protocol="pop3";
					connData8.mailPort=popPortUseSSL;
					connData8.smtpPort=smtpPortUseSSL;
					connData8.useSSL=true;
					workers.add(new IMAPPOPConfigValidate(connData8, waitingLock));
					
					MailClient.ConnectionData connData9=connData8.clone();
					connData9.mailPort=popPort;
					connData9.smtpPort=smtpPort;
					connData9.useSSL=false;
					workers.add(new IMAPPOPConfigValidate(connData9, waitingLock));
				}
			}
		}
		catch (CloneNotSupportedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(IMAPPOPConfigValidate worker:workers)
		{
			worker.start();
		}
		for(int _j=0;_j<8;_j++) //try 8 times, 5 seconds each time, totally 40 seconds. Mail connection will timeout in 30 seconds
		{
			synchronized (waitingLock)
			{
				waitingLock.wait(5000); //wait 5 seconds for ConfigValidater to complete
			}
			for(int i=workers.size()-1;i>=0;i--)
			{
				IMAPPOPConfigValidate worker=workers.get(i);
				if(!worker.isAlive())
				{
					if(worker.result==Result.SUCCESSED)
					{
						for(IMAPPOPConfigValidate w :workers)
						{
							w.interrupt();
						}
//								for(IMAPPOPConfigValidate t:workers)
						return new MailConfigResult(worker.connData.accountName, worker.connData.loginName, worker.connData.mailServer, 
						                            worker.connData.smtpServer, worker.connData.protocol, worker.connData.smtpPort+"", 
						                            worker.connData.useSSL?"true":"false", worker.connData.mailPort+"");
					}
					else
					{
						workers.remove(i);
					}
				}
			}
			if(workers.size()==0)
			{
				return new Result(Result.FAIL, "Unknown mail server");
			}
		}
		return new Result(Result.FAIL, "Unknown mail server");
	}
	int testPopConnect(ConnectionData connData)
	{
		Session session=null;
		Store store =null;
		POP3Folder folder=null;
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
					props.setProperty("mail.pop3s.socketFactory.class", SSL_FACTORY);  
					props.setProperty("mail.pop3.socketFactory.fallback", "false");  
					props.setProperty("mail.pop3.port", connData.mailPort+"");  
					props.setProperty("mail.pop3.socketFactory.port", connData.mailPort+"");  			// Get a Session object
					props.setProperty("mail.pop3s.timeout", CONNECTION_TIMEOUT);
				}// SSL连接需要(结束)
				props.setProperty("mail.pop3.apop.enable", "false");
				props.setProperty("mail.pop3.timeout", CONNECTION_TIMEOUT);
				session = Session.getInstance(props, null);
				session.setDebug(MailClient.verbose);
				store = session.getStore(connData.protocol);

				if(!store.isConnected())
				{
					store.connect(connData.mailServer, connData.mailPort, connData.loginName, connData.password);
					folder = (POP3Folder) store.getDefaultFolder();
					if (folder == null) {
						//System.out.println("No default folder");
						store.close();
						return Result.FAIL;
					}
				}
				
				folder = (POP3Folder) folder.getFolder("INBOX");
				if (folder == null)
				{
					// System.out.println("Invalid folder");
					store.close();
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
			}
		}
		catch(AuthenticationFailedException ae)
		{
//			ae.printStackTrace();
			return Result.AUTH_FAIL;
			
		}
		catch (NoSuchProviderException e)
		{
//			e.printStackTrace();
			return Result.FAIL;
		}
		catch (Exception e)
		{
//			folder.close(false);
//			store.close();
//			Utils.logException(e);
			return  Result.FAIL;
		}
		finally
		{
			try
			{
				store.close();
			}
			catch (MessagingException e)
			{
				// TODO Auto-generated catch block
//				e.printStackTrace();
			}
			log.info( "Leave testPopConnect" );
		}
		return  Result.SUCCESSED;
	}
	int testImapConnect(ConnectionData connData)
	{
		Session session=null;
		Store store =null;
		IMAPFolder folder=null;
		try
		{
			if (folder == null || !folder.isOpen() || !store.isConnected())
			{

				Properties props = new Properties();
				props.putAll(System.getProperties());
				// Properties props = System.getProperties();
				if(connData.useSSL)
				{// SSL 连接需要(开始)

					Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());  
					final String SSL_FACTORY = "fortunedog.mail.proxy.DummySSLSocketFactory";  
					
					//props.setProperty("mail.imap.sasl.enable", "true");
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
					// props.setProperty("mail.imap.separatestoreconnection",
					// "true");
				}
				// SMTP authentication
				// properties.setProperty("mail.smtp.submitter",
				// authenticator.getPasswordAuthentication().getUserName());
				props.setProperty("mail.imap.fetchsize", "65536");
				props.setProperty("mail.imap.timeout", CONNECTION_TIMEOUT);
				props.setProperty("mail.imaps.timeout", CONNECTION_TIMEOUT);
				// props.setProperty("mail.smtp.host", smtpServer);
				// props.setProperty("mail.smtp.port", smtpPort+"");
				// session = Session.getDefaultInstance(props, new
				// Authenticator());

				session = Session.getInstance(props, null);
				session.setDebug(MailClient.verbose);
				if (store == null)
					store = session.getStore(connData.protocol);

				if (!store.isConnected())
				{

					store.connect(	connData.mailServer, connData.mailPort, connData.loginName,
									connData.password);
					folder = (IMAPFolder) store.getDefaultFolder();
					if (folder == null)
					{
						// System.out.println("No default folder");
						store.close();
						return Result.FAIL;
					}
				}
			}
		}

		catch (AuthenticationFailedException ae)
		{
//			ae.printStackTrace();
			return Result.AUTH_FAIL;

		}
		
		catch (NoSuchProviderException e)
		{
			// folder.close(false);
			// store.close();
//			e.printStackTrace();
			return Result.FAIL;
		}
		catch (Exception e)
		{
			// folder.close(false);
			// store.close();
//			Utils.logException(e);
			return Result.FAIL;
		}
		finally
		{
			try
			{
				store.close();
			}
			catch (MessagingException e)
			{
				// TODO Auto-generated catch block
//				e.printStackTrace();
			}
			log.info( "Leave testImapConnect" );
		}
		return Result.SUCCESSED;
	}
	String doLookup( String hostName )  
	{
		try
		{
		Hashtable<String,String> env = new Hashtable<String,String>();
        env.put("java.naming.factory.initial",
                "com.sun.jndi.dns.DnsContextFactory");
        DirContext ictx = new InitialDirContext( env );
        Attributes attrs = 
           ictx.getAttributes( hostName, new String[] { "MX" });
        Attribute attr = attrs.get( "MX" );
        if( attr == null ) 
        	return null;
        String dnsStr = attr.get(0).toString();
        String []tmpList = dnsStr.split(" ");
        if(tmpList.length>0)
        {
        //	"aspmx.l.google.com." will be changed to "aspmx.l.google.com"
        	String reStr = tmpList[tmpList.length-1];
        	if(reStr.endsWith("."))
        		reStr = reStr.substring(0,reStr.length()-1);
        	return reStr;
        }
        else
        	return null;
      }
	catch(NamingException excpt)
	{
		excpt.printStackTrace();
		return null;
	}
	}

}

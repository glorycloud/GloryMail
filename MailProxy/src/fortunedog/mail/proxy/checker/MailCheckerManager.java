package fortunedog.mail.proxy.checker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fortunedog.mail.proxy.MailClient;
import fortunedog.mail.proxy.servlet.SessionListener;
import fortunedog.util.Utils;

/**
 * Problem to solve:
 * 1. how to keep connection with Client, UDP like QQ, or TCP like IDLE?
 * 2. how to track server's response
 *    for IMAP with IDLE, use NIO channel 
 *    for IMAP without IDLE, send NOOP command periodically 
 *    for POP3 server, send UIDL? or other more efficient method
 * 3. How to share server connections with normal mail access?
 * 
 * What a user request can be made?
 * @author Daniel
 *
 */
public class MailCheckerManager extends Thread
{
	public static final int PACKET_TYPE_PUSHREQUEST = 1; //PUSHREQUEST for Cloud Mail Version 1.3
	public static final int PACKET_TYPE_PUSHREQUESTV14 = 2; //PUSHREQUEST for Cloud Mail Version 1.4
	public static final int PACKET_TYPE_PUSHREQUESTV142 = com.glorycloud.pushserver.MailCheckerManager.PACKET_TYPE_PUSHREQUESTV15; //PUSHREQUEST for Cloud Mail Version 1.4.2
	
	public static final int POLL_INTERVAL = 10;//interval, in seconds
	
	public static final int PUSH_LISTEN_PORT = 10099;
	public static final int SESSION_RENEW_TIME = 30; //minutes, a session will be renewed for this time
	public static HashMap<String, MailChecker> checkers = new HashMap<String, MailChecker>(); 
	public static MailCheckerManager instance = new MailCheckerManager();
	static Logger log = LoggerFactory.getLogger(MailCheckerManager.class);
	private ServerSocket tcpServer = null;
	private boolean shutdown = false;
	private Thread cleanThread = null;
	
	@Override
	public void run()
	{
		setName("MailCheckerManager");
		log.info("start MailCheckerManager ");
		try
		{
			tcpServer = new ServerSocket(PUSH_LISTEN_PORT,100);
			tcpServer.setReuseAddress(true) ;
		}
		catch (IOException e1)
		{
			log.error("MailCheckerManager fail to start", e1);
			return;
		}
		
		cleanThread = new Thread(new Runnable() {

			@Override
			public void run()
			{
				try
				{
					while(true)
					{
						if(interrupted())
							return;
						sleep(17*60*1000);//sleep 17 minutes
						long time = System.currentTimeMillis();
						
						for(Entry<String, MailChecker>  entry : checkers.entrySet())
						{
							if(interrupted())
								return;
							MailChecker chk = entry.getValue();
							if(time - chk.lastActive > 30*60*1000) //more than 30 minutes, not used
							{
								chk.interrupt();
								checkers.remove(entry.getValue());
							}
						}
					}
				}
				catch (InterruptedException e)
				{
					return;
				} 
				
			}
			
		});
		cleanThread.setName("CheckerCleaner");
		cleanThread.start();
		while(!shutdown)
		{
			try {
				
				final Socket client = tcpServer.accept();
				
				//use a thread to wait client, to avoid spiteful connection block other client
				Thread checkInitThread = new Thread(new Runnable() {
					
					byte[] buf = new byte[4096];
					@Override
					public void run()
					{
						try {
							int bytesWant = 1;
							client.setSoTimeout(30*1000);//wait 30 seconds for packet header
//							client.setSoTimeout(10000);
							InputStream is = client.getInputStream();
							int offset=0;
							int len = readBytes(is, buf, 0, bytesWant);
							if(len < bytesWant)
							{
								log.trace("Fail read head, close_socket");
								client.close();
								return;
							}
							offset+=len;
							int pushType = buf[0];
							if(pushType >= PACKET_TYPE_PUSHREQUESTV142)
							{
								bytesWant = 3;
							}
							else
								bytesWant =2;
							len = readBytes(is, buf, offset, bytesWant);
							if(len < bytesWant)
							{
								log.trace("Fail read head, close_socket");
								client.close();
								return;
							}
							offset+=len;
							
							bytesWant = intFromBytes(buf[offset-1], buf[offset-2]);
							if(bytesWant <= 0)
							{
								log.trace("No client data, close_socket");
								client.close();
								return;
							}
							client.setSoTimeout(30*1000);//wait 30 seconds for data
							int n = readBytes(is, buf, 0, bytesWant);
							if(n < bytesWant)
							{
								log.trace("Invalid client data, close_socket");
								client.close();
								return;
							}
							String sids = new String(buf, 0, n, "UTF-8");
							String[] sidList = sids.split(" ");
							
							for(int i=0; i<sidList.length; i++)
							{
								if(Utils.isEmpty(sidList[i]))
									continue;
								if(!startMailChecker(client, pushType, sidList[i], i))
									break;
							}
						}
						catch(Throwable t)
						{
							log.error("Fail startMailChecker", t);
						}
					}

					private boolean startMailChecker(final Socket client,
							int pushType, String sid, int index) {
						HttpSession session = SessionListener.getSession(sid);
						
						if(session != null )
						{
							MailClient mailClient = null;
							try
							{
								mailClient =SessionListener.getStoredMailClient(session);
							}
							catch(Exception ex)
							{//may get exception: java.lang.IllegalStateException : getAttribute: Session already invalidated
								log.warn( sid+" session IllegalStateException, checker not start", ex);
								//log this exception, send fail to client in next
								SessionListener.removeStoredMailClient(session);
							}
							if(mailClient != null )
							{
								Date now = new Date();
								
								//renew this session, so it will not expire 
								session.setMaxInactiveInterval((int)((now.getTime() - session.getLastAccessedTime())/1000 + SESSION_RENEW_TIME*60));
								log.info("session renewed, "+mailClient.connData.accountName);
								String accountName = mailClient.connData.accountName;
								MailChecker checker = checkers.get(accountName);
								Thread.currentThread().setName("CheckerClientInit "+accountName);
								if(checker != null)
								{
									if(checker.isAlive())
									{
										InetAddress ad = client.getInetAddress();
										log.debug( mailClient.connData.accountName+", Push client changed, to :" + ad.getHostAddress()+":"+client.getPort());
										try {
											checker.setClientSocket(client);
										}
										catch(IOException se)
										{
											log.error("re-setClientSocket", se);
											checker.interrupt();
											checkers.remove(accountName);
											return false;
										}
										
//										mailClient.enterUserState();//to terminate a IDLE and restart a push check
//										mailClient.quiteUserState();
										try {
											mailClient.interruptWaiting();
										} catch (InterruptedException e) {
											return false;
										}
										return true;
									}
								}
								
								log.debug(mailClient.connData.accountName+", Create new push client, from:" + client.getInetAddress().getHostAddress()+":"+client.getPort());
								checker = new MailChecker( mailClient, pushType);
								try {
									checker.setClientSocket(client);
								} catch (IOException e) {
									log.error("setClientSocket", e);
									return false;
								}
								checkers.put(accountName, checker);
								checker.start();
								
								return true;
							}
						}
						
						if(pushType >= PACKET_TYPE_PUSHREQUESTV14)
						{
							byte[] replyBuf = (MailChecker.NEED_LOGIN_REPLY+" "+sid).getBytes();
							try {
								MailChecker.sendReply(client.getOutputStream(), pushType, replyBuf);
							} catch (IOException e) {
								log.warn("Send_reply failed", e);
							}
							
						}
						else
						{
							//Mar.10 2013, Client version should send pushType as PACKET_TYPE_PUSHREQUESTV14, but for a client bug, old push type
							//code is send. So server can't distinguish ClientV1.4 and earlier.
							//we want to do is to ask client issue a relogin. We can send back a new_mail_found, client will try to syncup mail 
							//and do relogin.
							//But, we now have Session-ID only, client need an account name
							

						}
						try {
							log.warn( "Session not found, Server close_socket." + sid);
							client.close();
						} catch (IOException e) {
							log.warn("Close socket Fail" + e.getMessage());
						}
						return false;
					}
				}, "CheckerClientInit ");
				checkInitThread.start();
				
//				is.close();
	//			DatagramPacket p = new DatagramPacket(buf, buf.length);
	//			udp.receive(p );
	//			String sid = new String(buf,0, p.getLength(),"UTF-8");
	//			HttpSession session = SessionListener.getSession(sid);
	//			if(session != null)
	//			{
	//				MailChecker checker = new MailChecker(sid, session, p.getSocketAddress());
	//				
	//			}
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static void stopPush(HttpSession session)
	{
		log.info( "To stop push for session:"+session.getId());
		MailClient client = SessionListener.getStoredMailClient(session);
		if(client != null)
		{
			MailChecker checker = checkers.remove(client.connData.accountName);
			if(checker != null)
				checker.interrupt();
			
		}
		else
		{
			log.error("Fail to stopPush for mailClient is NULL, for session:"+session.getId());
		}
	}
	public static void stopPush(String accountName)
	{
		
		MailChecker checker = checkers.remove(accountName);
		if(checker != null)
			checker.interrupt();
			
		
	}
	synchronized int readBytes(InputStream inputStream, byte[] buf, int offset, int lenRequested) throws IOException
    {
        int cnt = 0;
        boolean readTimeout = false;

        //serialPort.setTimeOut(tiemoutSeconds*1000);
    	try
    	{
	    	while (cnt < lenRequested)
	        {
	    		
	    		int r = inputStream.read(buf, offset, lenRequested - cnt);
	    		
	    		offset += r;
	        	cnt += r;
	        }
	        return cnt;
    	}
    	finally
    	{
    		
    	}
    }
	
	public void shutdown()
	{
		log.info("Shutdonw mail pusher server");
		shutdown=true;
		if(tcpServer != null)
			try
			{
				tcpServer.close();
			}
			catch (IOException e)
			{
				log.error( "Fail to shutdonw", e);
			};
	}
	public static int intFromBytes(byte h, byte l)
	{
		return (((h&0x000000ff) << 8) | (l&0x000000ff))&0xffff;
	}

	public static MailCheckerManager getInstance()
	{
		return instance;
	}
}

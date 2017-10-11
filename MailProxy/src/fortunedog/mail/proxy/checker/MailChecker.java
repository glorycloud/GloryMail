package fortunedog.mail.proxy.checker;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import javax.mail.AuthenticationFailedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fortunedog.mail.proxy.MailClient;

public class MailChecker extends Thread
{
	static Logger log = LoggerFactory.getLogger(MailChecker.class);
	private Socket clientSocket;
	private MailClient mailClient;
	int protocolVersion = 0;
	private static final String OK_REPLY = "OK";
	public static final String NEED_LOGIN_REPLY = "NEED_LOGIN";

	public long lastActive = System.currentTimeMillis();
	private OutputStream outputStream;
	public MailChecker(MailClient mailClient, int protocolVersion)
	{
		this.mailClient = mailClient;
		this.protocolVersion = protocolVersion;
		setName("MailChecker for:"+mailClient.connData.accountName);
	}
	
	@Override
	public void run()
	{
		log.info( "checker thread start, account:" + mailClient.connData.accountName);
		
		while(true)
		{
			if(isInterrupted())
				return;
			
			try
			{
				mailClient.enterWaitingState();
			}
			catch (InterruptedException e1)
			{
				return;
			}
			
			
			boolean hasNewMail = false;
			try
			{
				hasNewMail = mailClient.waitingNewMail();
				if(isInterrupted())
					return;
				if(hasNewMail)
				{
					
					log.info("Send_reply to client:" + mailClient.connData.accountName+" to addr:" + clientSocket.getInetAddress().getHostAddress()+":"+clientSocket.getPort());
					byte[] buf = (OK_REPLY+" "+mailClient.connData.accountName).getBytes();
					//DatagramPacket p = new DatagramPacket(buf, 0, buf.length, clientAddress);
					//socket.send(p);
					
					
					sendReply(outputStream, protocolVersion, buf);
				}

			}
			catch (AuthenticationFailedException e)
			{
				log.warn("AuthenticationFailedException, Checker terminate for:"+Thread.currentThread().getName(), e);
				return;
			}
			catch(InterruptedException ei)
			{
				log.trace("InterruptedException, Checker terminate for:"+Thread.currentThread().getName(), ei);
				return;
			}
			catch (Exception e)
			{
				log.error("Unknown Exception, Checker terminate for:"+Thread.currentThread().getName(), e);
				return;
			}
			finally
			{
				mailClient.quitWaitingState();
				log.trace("Mail checker finish");
			}
			
			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e)
			{
				
			} //sleep a moment, give chance to other thread to use MailClient
		}
	}

	public static void sendReply(OutputStream outputStream, int protocolVersion, byte[] buf) {
		byte[] lenBuf = null;
		if(protocolVersion <= MailCheckerManager.PACKET_TYPE_PUSHREQUESTV14)
		{
			lenBuf = new byte[3];
			lenBuf[1] = (byte)(buf.length&0xff);
			lenBuf[2] = (byte)((buf.length >> 8)&0xff);
		}
		else
		{
			lenBuf = new byte[4];
			lenBuf[0] = (byte)protocolVersion;
			lenBuf[2] = (byte)(buf.length&0xff);
			lenBuf[3] = (byte)((buf.length >> 8)&0xff);
		}
		
		try
		{
			outputStream.write(lenBuf);
			outputStream.write(buf);
			outputStream.flush();
		
		}
		catch(Exception ex)
		{
			log.trace( "Send_reply Fail" , ex);
		}
	}

	public void setClientSocket(Socket clientSocket) throws IOException
	{
		lastActive = System.currentTimeMillis();
		if(this.clientSocket != clientSocket)
		{
			try
			{
				if(this.clientSocket != null)
				{
					log.trace("client changed, close_socket");
					this.clientSocket.close();
				}
			}
			catch (IOException e)
			{
				log.info("close clientSocket", e);
			}
		
			this.clientSocket = clientSocket;
			outputStream = this.clientSocket.getOutputStream();
			
		}
	}
	
	@Override
	public void interrupt()
	{
		super.interrupt();
		
		try
		{
			if(clientSocket != null)
			{
				log.trace( "interrupt checker, close_socket");
				clientSocket.close();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		clientSocket = null;
		try {
			mailClient.interruptWaiting();
		} catch (InterruptedException e1) {
			
		}
	}
}

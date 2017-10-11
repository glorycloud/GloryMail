package fortunedog.mail.proxy;

import java.security.Security;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import fortunedog.mail.proxy.MailClient.ConnectionData;

public class SmtpTransport implements MailTransport
{

	private Session session;
	private ConnectionData connData;

	public SmtpTransport(ConnectionData connData)
	{
		this.connData  = connData;
	}
	@Override
	public MimeMessage createEmptyMessage()
	{
		return new MimeMessage(getSession());
	}

	@Override
	public void sendMessage(MimeMessage msg) throws MessagingException
	{
		Transport trans = session.getTransport(connData.useSSL?"smtps":"smtp");
		if(connData.smtpServer.equalsIgnoreCase("smtp.live.com"))
		{
			((org.apache.geronimo.javamail.transport.smtp.SMTPTransport)trans).setStartTLS(true);
		}
		try
		{
			trans.connect(connData.smtpServer, connData.smtpPort, connData.loginName, connData.password);
			trans.sendMessage(msg, msg.getAllRecipients());
		}
		finally
		{
			trans.close();
		}

	}

	private Session getSession()
	{
		if(session != null)
			return session;
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
			props.setProperty("mail.smtps.auth", "true");
			props.setProperty("mail.smtp.auth", "true");
			props.setProperty("mail.smtps.socketFactory.class", SSL_FACTORY);  
			props.setProperty("mail.smtp.socketFactory.fallback", "false"); 
			props.setProperty("mail.imap.sasl.enable", "true");
			props.setProperty("mail.imap.ssl.enable", "true");
			props.setProperty("mail.imap.ssl.trust", "*");
			props.setProperty("mail.imaps.ssl.trust", "*");
			props.setProperty("mail.imaps.ssl.socketFactory.class", SSL_FACTORY);
			props.setProperty("mail.imaps.socketFactory.class", SSL_FACTORY);
			props.setProperty("mail.imap.ssl.socketFactory.class", SSL_FACTORY);
			props.setProperty("mail.imap.ssl.socketFactory.fallback", "false");
			props.setProperty("mail.imaps.separatestoreconnection", "true");
		}// SSL连接需要(结束)
		else
		{
			props.setProperty("mail.imap.separatestoreconnection", "true");
			props.setProperty("mail.smtp.auth", "true");
		}
		//SMTP authentication
//		properties.setProperty("mail.smtp.submitter", authenticator.getPasswordAuthentication().getUserName());
		props.setProperty("mail.pop3.apop.enable", "true");
		props.setProperty("mail.imap.fetchsize", "65536");
//		props.setProperty("mail.smtp.host", smtpServer);
//		props.setProperty("mail.smtp.port", smtpPort+"");
//		session = Session.getDefaultInstance(props, new Authenticator());
		

		session = Session.getInstance(props, null);
		session.setDebug(MailClient.verbose);
		return session;
	}
}

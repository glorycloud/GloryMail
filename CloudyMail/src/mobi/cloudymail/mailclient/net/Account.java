package mobi.cloudymail.mailclient.net;

import java.io.Serializable;

import org.simpleframework.xml.Attribute;

public class Account implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -8600595852655158891L;
	public static final int pop3DefaultPort = 110;
	public static final int imapDefaultPort = 143;
	public static final int smtpDefaultPort = 25;
	
	//[SoapIgnore]
	public int id = 0;
	/// <summary>
	/// the complete email name of this account, like 'liu_lele@126.com'
	/// </summary>
	@Attribute
	public String name;

	/// <summary>
	/// login name used by authentication. for example, liu_lele is the 
	/// loginName for above email
	/// </summary>
	@Attribute
	public String loginName;

	/// <summary>
	/// address of receiver server, can be domain name or IP of pop3 or imap
	/// </summary>
	@Attribute
	public String mailServer;
	@Attribute
	public String smtpServer;
	@Attribute
	public String serverType = "pop3"; //pop3 or imap

	//TODO: make sure _mailPort is serialized as getMailPort()'s return value
	@Attribute(name="mailPort")
	private int _mailPort = 0;
//	[XmlAttribute(AttributeName = "mailPort")]
	public int getMailPort()
	{
			if(_mailPort == 0)
			{
				if(serverType == "pop3")
				{
					return pop3DefaultPort;
				}
				else if(serverType == "imap")
				{
					return imapDefaultPort;
				}
			}
			return _mailPort;
	}
	
	public void setMailPort(int value)
	{
		_mailPort = value;
	}
	
	@Attribute
	public int smtpPort = 25;
	@Attribute
	public boolean useSSL;
	@Attribute
	public String password;
	
	public boolean promptForPOPIMAP = false;
	
	public boolean isPromptForPOPIMAP()
	{
		return promptForPOPIMAP;
	}

	public void setPromptForPOPIMAP(boolean promptForPOPIMAP)
	{
		this.promptForPOPIMAP = promptForPOPIMAP;
	}

	public String toString() 
	{
		return name;
	}
	
	public String getHostName()
	{
		if(name == null)
			return "";
		int pos = name.indexOf('@');
		if(pos == -1)
			return "";
		return name.substring(pos+1);
	}
	
	
	public boolean isValid()
	{
		if(name == null || this.loginName == null || this.mailServer == null || this.serverType == null ||
				this.mailServer == null || this.smtpServer == null )
			return false;
		if(name.equals("") || loginName.equals("") || mailServer.equals("") || (!serverType.equals("pop3")&&!serverType.equals("imap")&&!serverType.equals("exchange")) ||
				mailServer.equals("") || smtpServer.equals(""))
			return false;
		if(!isPortValid(smtpPort))
			return false;
		if(_mailPort != 0 && !isPortValid(_mailPort))
			return false;
		return true;
	}
	
	public static boolean isPortValid(int portNum)
	{
		if(portNum <= 0 || portNum > 65535)
			return false;
		return true;
	}
	
	public void clear()
	{
		name = null;
		loginName = null;
		mailServer = null;
		mailServer = null;
		smtpServer = null;
	}
	
	public String toXmlString()
	{
		StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n").append("<account ");
		sb.append("name='");
		sb.append("' password='").append(password);
		sb.append("' loginName='").append(loginName);
		sb.append("' mailServer='").append(mailServer);
		sb.append("' smtpServer='").append(smtpServer);
		sb.append("' serverType='").append(serverType);
		sb.append("' smtpPort='").append(smtpPort);
		sb.append("' useSSL='").append(useSSL);
		sb.append("' mailPort='").append(getMailPort());
		sb.append("'/>");
		return sb.toString();
	}
	
	public void setDefault()
	{
		smtpPort = smtpDefaultPort;
		_mailPort = pop3DefaultPort;
		useSSL = false;
		if(name == null)
			return;
		int pos = name.indexOf("@");
		if(pos<1)
			return;
		serverType = "pop3";
		String domain = name.substring(pos+1);
		loginName = name.substring(0, pos);		
		mailServer = "pop3."+domain;
		smtpServer = "smtp."+domain;		
	}

	public int getIdleRefreshMinutes()
	{
		return 20;
	}
	
	public boolean isImap()
	{
		return "imap".equalsIgnoreCase(serverType);
	}
}


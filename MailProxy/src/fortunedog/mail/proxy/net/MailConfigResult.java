package fortunedog.mail.proxy.net;

import org.jdom.Element;

public class MailConfigResult extends Result
{
	public MailConfigResult(String name, String loginName, String mailServer,
			String smtpServer, String serverType, String smtpPort,
			String useSSL, String mailPort)
	{
		accountElement.setAttribute("name", name);
		accountElement.setAttribute("loginName", loginName);
		accountElement.setAttribute("mailServer", mailServer);
		accountElement.setAttribute("smtpServer", smtpServer);
		accountElement.setAttribute("serverType", serverType);
		accountElement.setAttribute("smtpPort", smtpPort);
		accountElement.setAttribute("useSSL", useSSL);
		accountElement.setAttribute("mailPort", mailPort);

	}

	protected void fillContent(Element content)
	{
		content.addContent(accountElement);

	}

	private Element accountElement = new Element("account");
}

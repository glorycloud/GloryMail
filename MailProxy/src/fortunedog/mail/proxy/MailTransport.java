package fortunedog.mail.proxy;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

public interface MailTransport
{
	MimeMessage createEmptyMessage();
	void sendMessage(MimeMessage msg) throws  MessagingException;
}

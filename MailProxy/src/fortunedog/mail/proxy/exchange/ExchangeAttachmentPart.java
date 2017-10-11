package fortunedog.mail.proxy.exchange;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.Part;

import org.apache.commons.io.IOUtils;

import fortunedog.mail.proxy.ExchangeProtocol;
import fortunedog.mail.proxy.MailPart;
import fortunedog.mail.proxy.StructurizedMail;

public class ExchangeAttachmentPart extends MailPart
{
	ExchangeProtocol exchangeProtocol;
	ExchangeMessage rawMsg;
	public ExchangeAttachmentPart(ExchangeProtocol exchangeProtocol, ExchangeMessage rawMsg, byte[] content, String contentId, String contentType, String fileName, String disposition,  int size) throws MessagingException, IOException
	{
		super(null, content, contentId, contentType, fileName, disposition, size);
		this.exchangeProtocol = exchangeProtocol;
		this.rawMsg = rawMsg;
	}
	protected byte[] getContentFromMail() throws IOException, MessagingException
	{
		if(_content.length == 0 && size > 0)
		{
			try
			{
				_content = exchangeProtocol.retrieveAttachment(rawMsg.folderId, rawMsg.serverId, contentId);
			}
			catch(Exception e)
			{
				throw new MessagingException("Fail get attachment body", e);
			}
//			_content = IOUtils.toByteArray(_part.getInputStream());
			size = _content.length;
		}
		return _content;
	}

	
}

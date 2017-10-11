package fortunedog.mail.proxy.exchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;

import fortunedog.mail.proxy.MailPart;

public class ExchangeMessage
{
	static public class NotImplementedException extends RuntimeException
	{}
	String folderId;
	String serverId;
	Vector<MailPart> parts;
	public ExchangeMessage(String folderId, String serverId, Vector<MailPart> parts) throws MessagingException
	{
		this.folderId = folderId;
		this.serverId=serverId;
		this.parts = parts;
		
	}
	
	public Vector<MailPart> getAllParts()
	{
		return parts;
	}
	
	public void addPart(MailPart p)
	{
		parts.add(p);
	}
//	@Override
//	public void addHeader(String arg0, String arg1) throws MessagingException
//	{
//		throw new NotImplementedException();
//	}
//
//	@Override
//	public Enumeration getAllHeaders() throws MessagingException
//	{
//		throw new NotImplementedException();
//	}
//
//	@Override
//	public Object getContent() throws IOException, MessagingException
//	{
//		return parts;
//	}
//
//	@Override
//	public String getContentType() throws MessagingException
//	{
//		return "text/html";
//	}
//
//	@Override
//	public DataHandler getDataHandler() throws MessagingException
//	{
//		throw new NotImplementedException();
//	}
//
//	@Override
//	public String getDescription() throws MessagingException
//	{
//		throw new NotImplementedException();
//	}
//
//	@Override
//	public String getDisposition() throws MessagingException
//	{
//		throw new NotImplementedException();
//	}
//
//	@Override
//	public String getFileName() throws MessagingException
//	{
//		throw new NotImplementedException();
//	}
//
//	@Override
//	public String[] getHeader(String arg0) throws MessagingException
//	{
//		throw new NotImplementedException();
//	}
//
//	@Override
//	public InputStream getInputStream() throws IOException, MessagingException
//	{
//		throw new NotImplementedException();
//	}
//
//	@Override
//	public int getLineCount() throws MessagingException
//	{
//		throw new NotImplementedException();
//	}
//
//	@Override
//	public Enumeration getMatchingHeaders(String[] arg0) throws MessagingException
//	{
//		throw new NotImplementedException();
//	}
//
//	@Override
//	public Enumeration getNonMatchingHeaders(String[] arg0) throws MessagingException
//	{
//		throw new NotImplementedException();
//	}
//
//	@Override
//	public int getSize() throws MessagingException
//	{
//		throw new NotImplementedException();
//	}
//
//	@Override
//	public boolean isMimeType(String arg0) throws MessagingException
//	{
//		throw new NotImplementedException();
//	}
//
//	@Override
//	public void removeHeader(String arg0) throws MessagingException
//	{
//		throw new NotImplementedException();
//
//	}
//
//	@Override
//	public void setContent(Multipart arg0) throws MessagingException
//	{
//		throw new NotImplementedException();
//
//	}
//
//	@Override
//	public void setContent(Object arg0, String arg1) throws MessagingException
//	{
//		throw new NotImplementedException();
//
//	}
//
//	@Override
//	public void setDataHandler(DataHandler arg0) throws MessagingException
//	{
//		throw new NotImplementedException();
//
//	}
//
//	@Override
//	public void setDescription(String arg0) throws MessagingException
//	{
//		throw new NotImplementedException();
//
//	}
//
//	@Override
//	public void setDisposition(String arg0) throws MessagingException
//	{
//		throw new NotImplementedException();
//
//	}
//
//	@Override
//	public void setFileName(String arg0) throws MessagingException
//	{
//		throw new NotImplementedException();
//
//	}
//
//	@Override
//	public void setHeader(String arg0, String arg1) throws MessagingException
//	{
//		throw new NotImplementedException();
//
//	}
//
//	@Override
//	public void setText(String arg0) throws MessagingException
//	{
//		throw new NotImplementedException();
//
//	}
//
//	@Override
//	public void writeTo(OutputStream arg0) throws IOException, MessagingException
//	{
//		throw new NotImplementedException();
//
//	}
//
//	@Override
//	public void addFrom(Address[] arg0) throws MessagingException
//	{
//		throw new NotImplementedException();
//
//	}
//
//
//
//	@Override
//	public Flags getFlags() throws MessagingException
//	{
//		throw new NotImplementedException();
//	}
//
//	@Override
//	public Address[] getFrom() throws MessagingException
//	{
//		throw new NotImplementedException();
//	}
//
//	@Override
//	public Date getReceivedDate() throws MessagingException
//	{
//		throw new NotImplementedException();
//	}
//
//
//	@Override
//	public Date getSentDate() throws MessagingException
//	{
//		throw new NotImplementedException();
//	}
//
//	@Override
//	public String getSubject() throws MessagingException
//	{
//		throw new NotImplementedException();
//	}
//
//	@Override
//	public Message reply(boolean arg0) throws MessagingException
//	{
//		throw new NotImplementedException();
//	}
//
//	@Override
//	public void saveChanges() throws MessagingException
//	{
//		throw new NotImplementedException();
//
//	}
//
//	@Override
//	public void setFlags(Flags arg0, boolean arg1) throws MessagingException
//	{
//		throw new NotImplementedException();
//
//	}
//
//	@Override
//	public void setFrom() throws MessagingException
//	{
//		throw new NotImplementedException();
//
//	}
//
//	@Override
//	public void setFrom(Address arg0) throws MessagingException
//	{
//		throw new NotImplementedException();
//
//	}
//
//
//
//	@Override
//	public void setSentDate(Date arg0) throws MessagingException
//	{
//		throw new NotImplementedException();
//
//	}
//
//	@Override
//	public void setSubject(String arg0) throws MessagingException
//	{
//		throw new NotImplementedException();
//
//	}

}

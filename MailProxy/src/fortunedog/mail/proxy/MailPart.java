package fortunedog.mail.proxy;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.mail.MessagingException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.geronimo.javamail.store.imap.IMAPMimeBodyPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fortunedog.util.DbHelper;


public class MailPart
{
	static Logger log = LoggerFactory.getLogger(MailPart.class);
	public MailPart(StructurizedMail mail, ResultSet record) throws SQLException
	{
		super();
		this.mail = mail;
		this.partRowId=record.getLong("ROWID");
		this.fileName = record.getString("fileName");
		this.contentType = record.getString("contentType");
		this.contentId = record.getString("contentId");
		this.size = record.getLong("size");
		this.disposition = record.getString("disposition");
	}
	public MailPart(StructurizedMail mail, byte[] content, String contentId, String contentType,  String fileName, 
			 String disposition, int size)
	{
		super();
		this.contentId = contentId;
		this.fileName = fileName;
		this.contentType = contentType;
		this.mail = mail;
		this._content = content;
		this.disposition = disposition;
		this.size = size;
	}
	public MailPart(StructurizedMail mail, javax.mail.Part part) throws MessagingException, IOException
	{
		this.mail = mail;
		
		if( part instanceof IMAPMimeBodyPart)
		{/*  fix bug that mail content can't show
			java.lang.ClassCastException: javax.mail.internet.MimeBodyPart cannot be cast to org.apache.geronimo.javamail.store.imap.IMAPMimeBodyPart
			at fortunedog.mail.proxy.StructurizedMail.addSimplePart(StructurizedMail.java:580)*/
			IMAPMimeBodyPart imapPart=(IMAPMimeBodyPart)part;  
		
			contentId = imapPart.bodyStructure.contentID;
		}
		else if(part instanceof javax.mail.internet.MimeBodyPart)
		{
			javax.mail.internet.MimeBodyPart mimePart = (javax.mail.internet.MimeBodyPart)part;
			contentId=mimePart.getContentID();
		}

		fileName = part.getFileName();
		if(fileName != null)
			fileName = javax.mail.internet.MimeUtility.decodeText(fileName);
		contentType =part.getContentType();
		_part = part;
		//content = IOUtils.toByteArray(part.getInputStream());
		
		//size = content.length;
		disposition = part.getDisposition();
	}
	
	public void saveToDb(Connection conn) throws SQLException, IOException, MessagingException
	{
		PreparedStatement st = null;
		ResultSet keys = null;
		try
		{
			st = conn.prepareStatement("insert into cache(mailROWID, fileName, contentType, contentId, disposition, content, size) values(?,?,?,?,?,?,?)");
			st.setLong(1, mail.getRowId());
			st.setString(2, fileName);
			st.setString(3, contentType);
			st.setString(4, contentId);
			st.setString(5, disposition);
			
			
			st.setBytes(6, getContentFromMail());
			st.setLong(7, size);
			st.execute();
			keys = st.getGeneratedKeys();
			if(keys.next())
				partRowId = keys.getLong(1);
		}
		finally
		{
			DbHelper.close(st);
			DbHelper.close(keys);
		}
	}
	protected byte[] getContentFromMail() throws IOException, MessagingException
	{
		if(_part != null && _content == null)
		{
			_content = IOUtils.toByteArray(_part.getInputStream());
			size = _content.length;
		}
		return _content;
	}
	protected String contentId;
	String fileName;
	String contentType;
	protected long size;
	StructurizedMail mail;
	long partRowId;
	//InputStream contentStream;
	protected byte[] _content;
	String disposition;
	javax.mail.Part _part = null;
	public InputStream getInputStream() throws SQLException, IOException
	{
		if(_content == null)
		{
			if(_part != null)
			{
				try
				{
					getContentFromMail();
				}
				catch (MessagingException e)
				{
					throw new IOException(e);
				}
			}
			else
				loadContentFromDb();
		}
		return new ByteArrayInputStream(_content);
	}
	
	private void loadContentFromDb() throws SQLException
	{
		Connection conn = null;
		PreparedStatement st = null;
		ResultSet rst = null;
		try
		{
			conn = DbHelper.getConnection(mail.getAccountId());
			st = conn.prepareStatement("select content from cache where ROWID=?");
			st.setLong(1, partRowId);
			rst = st.executeQuery();
			_content = rst.getBytes(1);
		}
		finally
		{
			DbHelper.close(conn);
		}
	}
	public String getContentType()
	{
		return contentType;
	}
	public boolean isMimeType(String type)
	{
		MimeType m;
		try
		{
			m = new MimeType(contentType);
			return m.match(type);
		}
		catch (MimeTypeParseException e)
		{
			log.warn("Fail to match MIME {}:{}", contentType, type, e);
		}
		return false;
	}
	public String getFileName()
	{
		return fileName;
	}
	public String getContentId()
	{
		return contentId;
	}
	public String getDisposition()
	{
		return disposition;
	}
	public void saveFile(File tempPdfFile) throws IOException, SQLException, MessagingException
	{
		FileUtils.copyInputStreamToFile(getInputStream(), tempPdfFile);
		
	}
	public long getSize()
	{
		return size;
	}
}

package fortunedog.mail.proxy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.mozilla.universalchardet.UniversalDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fortunedog.mail.proxy.exchange.ExchangeAttachmentPart;
import fortunedog.mail.proxy.exchange.ExchangeMessage;
import fortunedog.mail.proxy.net.MailSummary;
import fortunedog.mail.proxy.servlet.SetCharacterEncodingFilter;
import fortunedog.mail.reflow.ContentPager;
import fortunedog.mail.reflow.FullFormatHtmlPager;
import fortunedog.mail.reflow.HtmlPager;
import fortunedog.mail.reflow.PagerFactory;
import fortunedog.mail.reflow.PlainTextPager;
import fortunedog.mail.storage.MailCache;
import fortunedog.mail.storage.MailCache.MailNotCachedException;
import fortunedog.util.DbHelper;
import fortunedog.util.Utils;

public class StructurizedMail
{
	static Logger log = LoggerFactory.getLogger(StructurizedMail.class);
	// members here
	MailSummary mailSummary;
	protected int pageCount = 0;
	private  boolean _multiPage = false;
	public Vector<MailPart> attachmentParts = new Vector<MailPart>();
//	protected Vector<MailPart> inlineParts = new Vector<MailPart>();
	protected Vector<MailPart> bodyParts = new Vector<MailPart>();
	protected Vector<ContentPager> bodyPagers = new Vector<ContentPager>();
	public Map<Integer, ContentPager> attachPagers = new HashMap<Integer, ContentPager>();
	
	protected Map<String, MailPart> cidToPartMap = new TreeMap<String, MailPart>();
	
	public StructurizedMail(ExchangeMessage message,  MailSummary summary)
	{
		this.mailSummary = summary;
		try
		{
			Vector<MailPart> parts = message.getAllParts();
			for(int i=0;i<parts.size();i++)
			{
				MailPart p = parts.get(i);
				p.mail = this;
				addSimplePart(p);
			}
		}
		catch (Exception e)
		{
			log.error("Fail construct StructurizedMail:", e);
		}
		return;
		
	}
	public StructurizedMail(javax.mail.Message message, MailSummary summary, boolean multiPage)
	{
		this.mailSummary = summary;
		javax.mail.Message msg = message;
		_multiPage = multiPage;
		try
		{
			
			if (msg.isMimeType("text/*"))
			{
				bodyParts.add(new MailPart(this, msg));
			}
			else if (msg.isMimeType("multipart/*"))
			{
				
				scanMsgMultiPart(msg);
		    }
			else
			{
				addSimplePart(new MailPart(this, msg));
			}

			//createBodyPager();
			
		}
		catch (Exception e)
		{
			log.error("Fail constructe StructurizedMail:", e);
		}
	}

	private void createBodyPager() 
	{
		for (MailPart p : bodyParts)
		{
			try
			{
				ContentPager pager = null;
				String charsetType = getPartCharType(p);
				if (p.isMimeType("text/html"))
				{
					pager = new HtmlPager();
//			System.err.println((String)p.getContent());
					InputStream in = p.getInputStream();
					
					byte[] buf = IOUtils.toByteArray(in);
					
					if(charsetType!=null && Charset.isSupported(charsetType))
					{
						((HtmlPager)pager).init(new String(buf,charsetType),_multiPage);
					}
					else
					{
						UniversalDetector detector = new UniversalDetector(null);
						detector.handleData(buf, 0, buf.length);
						detector.dataEnd();
						String encoding = detector.getDetectedCharset();
						in = new ByteArrayInputStream(buf);

						InputStreamReader reader;
						if (encoding != null)
							reader = new InputStreamReader(in, encoding);
						else
							reader = new InputStreamReader(in);

						((HtmlPager) pager).init( reader, _multiPage);
						reader.close();
					}
					in.close();			
				}
				else
				{
					InputStream in = p.getInputStream();
					/*ClassLoader cloader = in.getClass().getClassLoader();
					String str1 = cloader.getResource("javax/mail/internet/MimeBodyPart.class").toString();
					System.err.println(str1+"\n");*/

					if(p.isMimeType("application/octet-stream") || p.isMimeType("text/*"))
					{
				//		String bodyStr = (String)p.getContent();
//				System.err.println(bodyStr);
				//		pager = new PlainTextPager(in ,_multiPage);
						if(charsetType!=null && Charset.isSupported(charsetType))
						{
				//			int avlen = in.available();
				//			System.err.println("Body part's size is:"+avlen);
							byte[] buf = IOUtils.toByteArray(in);
							pager = new PlainTextPager(new String(buf,charsetType));
						}
						else
							pager = new PlainTextPager(in ,_multiPage);
						
					}
					else
					{
						log.warn( "Unexpected content type for message body:" + p.getContentType());
						pager = new PlainTextPager(Utils.getResourceBundle(SetCharacterEncodingFilter.getCurrentRequest()).getString("unsupportedContentType") + p.getContentType(),_multiPage);
					}
				}
				bodyPagers.add(pager);
				pageCount += pager.getPageCount();
			}
			catch (Exception e)
			{
				log.error("Create body pager fail", e);
				continue;
			}
		}
	}

	public StructurizedMail(Connection conn, MailSummary summary) throws MessagingException, IOException, SQLException, MailNotCachedException
	{
		this.mailSummary = summary;
		PreparedStatement st = null;
		ResultSet rst = null;
		try
		{
			st=conn.prepareStatement("select ROWID,fileName,contentType, contentId, size, disposition from cache where mailROWID=?");
			st.setLong(1, summary.getRowId());
			rst = st.executeQuery();
			boolean found = false;
			while(rst.next())
			{
				found=true;
				MailPart p = new MailPart(this, rst);
				if (p.isMimeType("text/*"))
				{
					bodyParts.add(p);
				}
				
				else
				{
					addSimplePart(p);
				}
			}
			if(!found)
			{
				throw new MailCache.MailNotCachedException();
			}
		}
		
		finally
		{
			DbHelper.close(st);
			DbHelper.close(rst);
		}
	}
	private String getPartCharType(MailPart p)
	{
		try
		{
			String contentType = p.getContentType();
			// System.err.println("Content Type:"+contentType);
			String charsetType = null;
			String[] subs = contentType.split(";");
			for (int i = 0; i < subs.length; i++)
			{
				String[] expr = subs[i].split("=");
				if (expr.length != 2 || !expr[0].trim().equals("charset"))
					continue;
				charsetType = expr[1].trim();
				// remove the quotation marks.
				charsetType = charsetType.replace("\"", "");
				return charsetType;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public ContentPager getAttachmentPager(int attachIndex, HttpSession session) throws IOException, MessagingException
	{
		ContentPager p = attachPagers.get(attachIndex);
		if(p!=null)
			return p;
		MailPart part = null;
		part = this.attachmentParts.get(attachIndex);

		if (part == null)
			return null;
		p = PagerFactory.getPager(part.getFileName(), session);
		String contentType = part.getContentType();
		String charset = null;
		if(!Utils.isEmpty(contentType))
		{
			String[] strs = contentType.split(";");
			for(String s : strs)
			{
				String s2 = s.trim();
				if(s2.startsWith("charset="))
				{
					charset = s2.substring(8);
					break;
				}
			}
		}
		if(p == null)
			return null; //unsupported format
	//	p.init(is, charset);
		p.rawInit(part, charset);
		attachPagers.put(attachIndex, p);
		return p;
	}
	
	//before invoke this function, make sure the pager has been created before.
	public ContentPager getAttachmentPagerDirectly(int attachIndex)
	{
		return attachPagers.get(attachIndex);
	}
	
	public MailPart getMimePart(String cid)
	{
		return cidToPartMap.get(cid);
	}
	
	// there's a good explain about MIME type on http://en.wikipedia.org/wiki/MIME
	private void scanMsgMultiPart(javax.mail.Part part) throws MessagingException, IOException
	{
		// subtype of multiipart may be:  alternative, mixed,
		// digest, related,report, signed, encrypted, form data,
		// mixed-replace, byteranges, parallel
		if (part.isMimeType("multipart/alternative")) 
		{
			/*
			 * Alternative
			 * 
			 * The multipart/alternative subtype indicates that each part is
			 * an "alternative" version of the same (or similar) content,
			 * each in a different format denoted by its "Content-Type"
			 * header. The formats are ordered by how faithful they are to
			 * the original, with the least faithful first and the most
			 * faithful last. Systems can then choose the "best"
			 * representation they are capable of processing; in general,
			 * this will be the last part that the system can understand,
			 * although other factors may affect this.
			 * 
			 * Since a client is unlikely to want to send a version that is
			 * less faithful than the plain text version, this structure
			 * places the plain text version (if present) first. This makes
			 * life easier for users of clients that do not understand
			 * multipart messages.
			 * 
			 * Most commonly, multipart/alternative is used for e-mail with
			 * two parts, one plain text (text/plain) and one HTML
			 * (text/html). The plain text part provides backwards
			 * compatibility while the HTML part allows use of formatting
			 * and hyperlinks. Most e-mail clients offer a user option to
			 * prefer plain text over HTML; this is an example of how local
			 * factors may affect how an application chooses which "best"
			 * part of the message to display.
			 * 
			 * While it is intended that each part of the message represent
			 * the same content, the standard does not require this to be
			 * enforced in any way. At one time, anti-spam filters would
			 * only examine the text/plain part of a message,[citation
			 * needed] because it is easier to parse than the text/html
			 * part. But spammers eventually took advantage of this,
			 * creating messages with an innocuous-looking text/plain part
			 * and advertising in the text/html part. Anti-spam software
			 * eventually caught up on this trick, penalizing messages with
			 * very different text in a multipart/alternative
			 * message.[citation needed]
			 * 
			 * Defined in RFC 2046, Section 5.1.4
			 */
			processMixed(part);
		}
		else if(part.isMimeType("multipart/related"))
		{
			/*
			 * Related
			 * 
			 * A multipart/related is used to indicate that message parts should
			 * not be considered individually but rather as parts of an
			 * aggregate whole. The message consists of a root part (by default,
			 * the first) which reference other parts inline, which may in turn
			 * reference other parts. Message parts are commonly referenced by
			 * the "Content-ID" part header. The syntax of a reference is
			 * unspecified and is instead dictated by the encoding or protocol
			 * used in the part.
			 * 
			 * One common usage of this subtype is to send a web page complete
			 * with images in a single message. The root part would contain the
			 * HTML document, and use image tags to reference images stored in
			 * the latter parts.
			 * 
			 * Defined in RFC 2387
			 */
			processMixed(part);

		}
		else if(part.isMimeType("multipart/encrypted")  )
		{
			/*
			 * Encrypted
			 * 
			 * A multipart/encrypted message has two parts. The first part has
			 * control information that is needed to decrypt the
			 * application/octet-stream second part. Similar to signed messages,
			 * there are different implementations which are identified by their
			 * separate content types for the control part. The most common
			 * types are "application/pgp-encrypted" (RFC 3156) and
			 * "application/pkcs7-mime" (S/MIME).
			 * 
			 * Defined in RFC 1847, Section 2.2 
			 * 
			 * Digest
			 * 
			 * Multipart/digest is a simple way to send multiple text messages.
			 * The default content-type for each part is "message/rfc822".
			 * 
			 * Defined in RFC 2046, Section 5.1.5
			 */
			Multipart mp = (Multipart) part.getContent();
			if(mp.getCount() > 1)
			{
				BodyPart p = mp.getBodyPart(1);
				if(p.isMimeType("application/octet-stream"))
				{
					bodyParts.add(new MailPart(this,p));
				}
			}
		
		}
		else if (part.isMimeType("multipart/signed") || part.isMimeType("multipart/digest"))
		{
			/*
			 * Signed
			 * 
			 * A multipart/signed message is used to attach a digital
			 * signature to a message. It has two parts, a body part and a
			 * signature part. The whole of the body part, including mime
			 * headers, is used to create the signature part. Many signature
			 * types are possible, like application/pgp-signature (RFC 3156)
			 * and application/x-pkcs7-signature (S/MIME).
			 * 
			 * Defined in RFC 1847, Section 2.1
			 */
			processMixed(part);
		}
		else if(part.isMimeType("multipart/mixed"))
		{
			/*
			 * Mixed
			 * 
			 * Multipart/mixed is used for sending files with different
			 * "Content-Type" headers inline (or as attachments). If sending
			 * pictures or other easily readable files, most mail clients will
			 * display them inline (unless otherwise specified with the
			 * "Content-disposition" header). Otherwise it will offer them as
			 * attachments. The default content-type for each part is
			 * "text/plain".
			 * 
			 * Defined in RFC 2046, Section 5.1.3
			 */
			processMixed(part);

		}

		else if(part.isMimeType("multipart/report"))
		{
			/*
			 * Report
			 * 
			 * Multipart/report is a message type that contains data formatted
			 * for a mail server to read. It is split between a text/plain (or
			 * some other content/type easily readable) and a
			 * message/delivery-status, which contains the data formatted for
			 * the mail server to read.
			 * 
			 * Defined in RFC 3462
			 */
			processMixed(part);
		}
		else if(part.isMimeType("multipart/byteranges"))
		{
			/*
			 * Byteranges
			 * 
			 * The multipart/byteranges is used to represent noncontiguous byte
			 * ranges of a single message. It is used by HTTP when a server
			 * returns multiple byte ranges and is defined in RFC 2616.
			 */
			processMixed(part);
		}
		else  //may be message/rfc822 or others like application/*, image/*, viedo/*, audio/*, x-world/*
			processMixed(part);
	}

	private void processMixed(javax.mail.Part part) throws IOException,MessagingException
	{
		Multipart mp = (Multipart) part.getContent();
		int count = mp.getCount();
		for (int i = 0; i < count; i++)
		{
			BodyPart p = mp.getBodyPart(i);
			if(p.isMimeType("multipart/*"))
				scanMsgMultiPart(p);
			else
			{
				addSimplePart(new MailPart(this, p));
			}
		}
	}


	/**
	 * get total page number of this message
	 * 
	 * @return total page number
	 */
	public int getPageCount()
	{
		if(bodyPagers.size() == 0 && bodyParts.size() != 0)	
			createBodyPager();
		if(bodyPagers.size() == 0)
			return 0;
		return getPager().getPageCount();
	}

	/**
	 * render a specified page in HTML. Implementation of this function should
	 * append generated HMTL code to the StringBuilder passed in. returned
	 * string is then rounded with a <body> </body> tag by caller.
	 * 
	 * @param pageNo
	 *            specify which page to render
	 */
	public String renderPage(int pageNo)
	{
		if (pageNo < 0 || pageNo >= pageCount)
		{
			return "&nbsp;";
		}
		return getPager().renderPage(pageNo);
	}

	public ContentPager getPager()
	{
		if(bodyPagers.size() == 0 && bodyParts.size() != 0)	
			createBodyPager();
			
		if(bodyPagers.size() > 0)
			return bodyPagers.lastElement();
		
		return null;
	}

	public MailSummary getSummary()
	{
		return mailSummary;
	}

	public boolean hasImage()
	{	ContentPager pager = getPager();
		if(pager == null)
			return false;
		return pager.hasImage();
	}
	public boolean hasAttachment()
	{
		return attachmentParts.size() > 0;
	}
	
	private void addSimplePart(MailPart p) throws MessagingException, IOException
	{
		assert !p.isMimeType("multipart/*");
//		Enumeration hs = p.getAllHeaders();
//		while(hs.hasMoreElements())
//		{
//			Header h = (Header) hs.nextElement();
//			System.out.println(h.getName() + "=" + h.getValue());
//		}
		String disposition = null;
		try
		{
			disposition = p.getDisposition();
		}
		catch(Exception e)
		{
			
		}
		
		if(Part.ATTACHMENT.equalsIgnoreCase(disposition))
		{
			attachmentParts.add(p);
		}
		else if(p.getFileName() != null) //fix bug 266, add only part with file name //---------10
		{
			attachmentParts.add(p); //fix bug223, add inline part as attachment also.
			
		}
		else if (p.isMimeType("text/*"))  //先判断文件名是否存在，存在则一般情况下为附件，防止判断类型时将
		{                                 //txt附件显示为邮件正文 ，fix bug398
 			bodyParts.add(p);
		}
		if( !Utils.isEmpty(p.getContentId()))
			cidToPartMap.put(p.getContentId(), p);
	}
		
	public boolean isHmtl() throws MessagingException
	{
		for(int i= bodyParts.size() - 1; i>=0; i--)
		{
			MailPart p = bodyParts.get(i);
			if( p.isMimeType("text/html"))
				return true;
		}
		return false;
	}
	
	public String getRawMailBody()
	{
		if(bodyParts.size() == 0)
			return "";
		
		MailPart p = null;
		for(int i= bodyParts.size() - 1; i>=0; i--)
		{
			
			if( bodyParts.get(i).isMimeType("text/html"))
			{
				p = bodyParts.get(i);
				break;
			}
		}
		if(p == null)
		{
			p = bodyParts.get(0);
		}
		StringWriter sw = new StringWriter(100*1024);
		
		InputStreamReader reader;
		try
		{
			reader = createPartReader(p);
			IOUtils.copy(reader, sw );
		}
		catch (Exception e)
		{
			log.error("Fail read mail body", e);
			return "";
		}
		String s = sw.toString();
		return s;
		
	}
	public String getFullMailBody()
	{
		if(bodyParts.size() == 0)
			return "";
		
		try
		{
			for(int i= bodyParts.size() - 1; i>=0; i--)
			{
				MailPart p = bodyParts.get(i);
				if( p.isMimeType("text/html"))
				{
					FullFormatHtmlPager pager = new FullFormatHtmlPager();
					InputStreamReader reader = createPartReader(p);

					pager.init(mailSummary.uid, mailSummary.folderName, reader, _multiPage);
					reader.close();
					return pager.renderPage(0);

				}
			}
			
			StringWriter sw = new StringWriter(100*1024);
			
			InputStreamReader reader = createPartReader(bodyParts.get(0));
			IOUtils.copy(reader, sw );
			String s = sw.toString();
			if(bodyParts.get(0).isMimeType("text/plain"))
			{
				return "<pre>"+ s +"</pre>";
			}
			return s;
		}
		catch (Exception e)
		{
			log.error("Mail server error:",e);
			return "<pre>Error:" + e.getMessage() +"</pre>";
		}


	}

	private InputStreamReader createPartReader(MailPart p) throws SQLException, IOException,
			UnsupportedEncodingException, MessagingException
	{
		InputStream in = p.getInputStream();
		byte[] buf = new byte[in.available()];
		in.read(buf);
		
		String charsetType = getPartCharType(p);
		String encoding = null;
		if (charsetType != null && Charset.isSupported(charsetType))
			encoding = charsetType;
		else
		{
			UniversalDetector detector = new UniversalDetector(null);
			detector.handleData(buf, 0, buf.length);
			detector.dataEnd();
			encoding = detector.getDetectedCharset();
		}
		in = new ByteArrayInputStream(buf);

		InputStreamReader reader;
		if (encoding != null)
			reader = new InputStreamReader(in, encoding);
		else
			reader = new InputStreamReader(in);
		return reader;
	}
	public int getAccountId()
	{
		return mailSummary.accountId;
	}

	public long getRowId()
	{
		return mailSummary.getRowId();
	}

	public void store(Connection conn) throws SQLException, IOException, MessagingException
	{
		for(MailPart p : bodyParts)
		{
			p.saveToDb(conn);
		}
		for(MailPart p  : attachmentParts)
		{
			p.saveToDb(conn);
		}
	}


}


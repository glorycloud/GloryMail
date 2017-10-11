package fortunedog.mail.proxy.servlet;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fortunedog.mail.proxy.MailClient;
import fortunedog.mail.proxy.MailPart;
import fortunedog.mail.proxy.StructurizedMail;
/**
 * get a certain part of a mail. 
 * Request to this svevlet should have query string like:
 * http://localhost:8080/MailProxy/MailRender?uid=1tbirQOOYEX9eQlIXgAAse&contentId=<part2.03040405.02000206@agatelogic.com>
 * Note, the value for content-ID should be escaped to put in an URL.
 * This servlet will return the requested content in content type as it specified in mail
 * 
 * This will download the whole part as its original mime type. This servelet used to show embedded
 * picture of a mail.
 * @see ViewPart, DownloadPart
 * @author Daniel
 *
 */
public class GetMailPart extends HttpServlet
{
	private static final long serialVersionUID = -6392988726643317056L;
	static Logger log = LoggerFactory.getLogger(GetMailPart.class);

	protected void service(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
	{
		String uid = request.getParameter("uid");
		String folderName=new String(request.getParameter("folderName").getBytes("ISO8859-1"),"UTF-8");
		String cid = request.getParameter("cid");
		String indexStr = request.getParameter("index");
		int attachIndex = -1;
		if(indexStr != null)
		{
			attachIndex = Integer.parseInt(indexStr);
		}
		ServletOutputStream os = response.getOutputStream();
		HttpSession session = request.getSession(false);
		if(session == null)
		{
			return;
		}
		MailClient client =SessionListener.getStoredMailClient(session);
		if(client == null)
		{
			return;
		}
		
		try
		{
			client.enterUserState();
		}
		catch (InterruptedException e)
		{
			log.warn("Fail enterUserState", e);
			return;
		}
		try
		{
			StructurizedMail pager = client.getMail(session, uid,folderName);
			MailPart part = null;
			if(cid != null)
				part = pager.getMimePart("<"+cid+">");
			else if(attachIndex >= 0)
				part = pager.attachmentParts.get(attachIndex);
			
			if(part == null)
				return;
			response.setContentType(part.getContentType());
			InputStream is = part.getInputStream();
			byte[] buf = new byte[is.available()];
			int actualLen  = is.read(buf);
			os.write(buf, 0, actualLen);
			
		}
		catch(Exception ex)
		{
			log.warn("Fail GetMailPart", ex);
		}
		finally
		{
			client.quiteUserState();
		}
	}
}

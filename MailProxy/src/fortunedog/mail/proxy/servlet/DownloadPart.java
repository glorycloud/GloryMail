package fortunedog.mail.proxy.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.sql.SQLException;

import javax.mail.MessagingException;
import javax.servlet.ServletException;

import fortunedog.mail.proxy.MailPart;
import fortunedog.mail.proxy.StructurizedMail;
import fortunedog.mail.proxy.net.Result;
import fortunedog.mail.reflow.RarZipPager;
import fortunedog.util.Utils;


/**
 * get a certain part of a mail. 
 * Request to this svevlet should have query string like:
 * http://localhost:8080/MailProxy/MailRender?uid=1tbirQOOYEX9eQlIXgAAse&contentId=<part2.03040405.02000206@agatelogic.com>
 * Note, the value for content-ID should be escaped to put in an URL.
 * 
 * This servlet used to download an attachment to user's local storage. All part are transfered
 * as content type "application/octet-stream", and file name is also specified as in mail 
 * @see GetPart ViewPart
 * @author Daniel
 *
 */
public class DownloadPart extends DatabaseServlet
{
	private static final long serialVersionUID = 8179581509221105142L;

	@Override
	Result dbService(ServiceData d)
	throws ServletException, IOException, SQLException, NeedLoginException
	{
		String uid = d.request.getParameter("uid");
		String folderName=new String(d.request.getParameter("folderName").getBytes("ISO8859-1"),"UTF-8");
		String cid = d.request.getParameter("cid");
		String indexStr = d.request.getParameter("index");
		String internalPath = d.request.getParameter("internalPath");
		int attachIndex = -1;
		if (indexStr != null)
		{
			attachIndex = Integer.parseInt(indexStr);
		}
		checkSession(d,true);
		checkMailClient(d);
		StructurizedMail pager = d.mailClient.getMail(d.session, uid,folderName);
		try
		{
			InputStream is = null;
			String fileName = "";
			if(internalPath == null || internalPath.equals(""))
			{
				MailPart part = null;
				if (cid != null)
					part = pager.getMimePart("<" + cid + ">");
				else if (attachIndex >= 0)
					part = pager.attachmentParts.get(attachIndex);

				if (part == null)
					throw new ServletException(Utils.getResourceBundle(d.request).getString("attachNotFound"));
				fileName = part.getFileName();
				is = part.getInputStream();
			}
			else //internal file in rar/zip file
			{
				//pager has been prepared before.
				RarZipPager rzPager = (RarZipPager)pager.getAttachmentPagerDirectly(attachIndex);
				String filePath = rzPager.getFilePath(internalPath);
				File tmpFile = new File(filePath);
				fileName = new File(filePath).getName();
				is = new FileInputStream(tmpFile);
			}
			
			d.response.setContentType("application/octet-stream");//;charset=UTF-8");
			d.response.addHeader(	"Content-Disposition",
									"attachment;filename="+URLEncoder.encode(fileName,"UTF-8"));
			byte[] buffer = new byte[1024*1024];
			int len;
			while( (len = is.read(buffer)) >= 0)
			{
				d.stream.write(buffer, 0, len);
			}
			is.close();
			
		}
		finally
		{
			
		}
		return null;
	}
}

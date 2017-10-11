package fortunedog.mail.proxy.servlet;

import java.io.File;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import fortunedog.mail.proxy.MailClient;
import fortunedog.mail.proxy.StructurizedMail;
import fortunedog.util.Pair;

public class DumpAssistData
{
	public HttpServletRequest request;
	public HttpServletResponse response;
	public HttpSession session;
	
	public String getAccount()
	{
		session = request.getSession(false);
		if(session == null)
			return "NO_ACCOUNT";
		MailClient mailClient = SessionListener.getStoredMailClient(session);
		if(mailClient == null)
			return "NO_ACCOUNT";
		return mailClient.connData.accountName;

	}
	public void dump(PrintWriter out)
	{
		try
		{
			out.println("=============BEGIN EXECUTION CONTEXT:");
			out.println("Request:"+request.getRequestURI() + "/" + request.getQueryString());
			
			
			session = request.getSession(false);
			out.println("Current Session:" + (session == null ? "NULL" : session.getId()));
			MailClient mailClient =SessionListener.getStoredMailClient(session);
			out.println("MailClient:" + (mailClient == null ? "NULL" : ("account ID:" + mailClient.connData.accountId +"," + mailClient.connData.accountName)));
			Pair<String, StructurizedMail> p = (Pair)session.getAttribute("currentMailPager");
			if(p == null)
			{
				out.println("Current Mail: NULL");
			}
			else
			{
				out.println("Current Mail: UID:"+p.key);
				String dirName = session.getServletContext().getRealPath("/") + "WEB-INF/logs/";
				File dir = new File(dirName);
				if(!dir.exists())
				{
					dir.mkdirs();
				}
				
				String stem = mailClient.connData.accountId + "_" + p.key;
				
				File mailFile = new File(dirName+stem+".eml");
				int i = 0;
				while(mailFile.exists())
				{
					mailFile = new File(dirName+stem+ "_"+i+".eml");
					i++;
				}
//				out.println("Mail dumped to:"+mailFile.getCanonicalPath());
				out.println("=============END EXECUTION CONTEXT=================");
				
//				InputStream is = p.value.getRawMessage().getInputStream();
//				OutputStream os = new FileOutputStream(mailFile);
//				IOUtils.copy(is, os);
//				IOUtils.closeQuietly(is);
//				IOUtils.closeQuietly(os);
				
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}

	}
}

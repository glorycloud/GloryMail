package fortunedog.mail.proxy.servlet;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import fortunedog.mail.proxy.MailClient;
import fortunedog.mail.proxy.StructurizedMail;
import fortunedog.util.Utils;

/**
 * render a mail in html format
 * Request to this svevelet should have qurey string like:
 * http://localhost:8080/MailProxy/MailRender?uid=1tbirQOOYEX9eQlIXgAAse&pageNo=0
 * @author Daniel
 *
 */
public class MailRender extends HttpServlet
{
	private static final long serialVersionUID = 6922572326491120701L;

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException
	{
		Thread.currentThread().setName("Thread_"+this.getClass().getSimpleName());
		String folerName=new String(request.getParameter("folderName").getBytes("ISO8859-1"),"UTF-8");
		response.setContentType("text/html; charset=UTF-8");
		String uid = request.getParameter("uid");
		String sid = request.getParameter("sid");
		String multiPage = request.getParameter("multipage");
		boolean isMultiPage = false;
		if(multiPage != null && !multiPage.equals(""))
			isMultiPage = Boolean.parseBoolean(multiPage);
		java.io.PrintWriter out = response.getWriter();
		HttpSession session = null;
		if(sid != null)
			session = SessionListener.getSession(sid);
		else
			session = request.getSession(false);
		if(session == null)
		{
			//response.sendRedirect("Relogin.jsp");
			response.setStatus(HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION);
			RequestDispatcher disp = request.getRequestDispatcher("/Relogin.jsp");
			disp.forward(request, response);
			//out.write("<html><body><h1>´íÎó:µÇÂ¼Ê§°Ü</h1></body></html>");
			return;
		}
		MailClient client = SessionListener.getStoredMailClient(session);
		if(client == null)
		{
			//response.sendRedirect("Relogin.jsp");
			response.setStatus(HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION);
			RequestDispatcher disp = request.getRequestDispatcher("/Relogin.jsp");
			disp.forward(request, response);
			//out.write("<html><body><h1>´íÎó:µÇÂ¼Ê§°Ü</h1></body></html>");
			return;
		}
		try {
			client.enterUserState();
		} catch (InterruptedException e) {
			throw new ServletException(e);
		}
		try 
		{
	//		int pageNo = Integer.parseInt(request.getParameter("pageNo"));
			if(!client.checkOpen())
			{
				response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
				out.print(Utils.getResourceBundle(request).getString("cannotConnectMailServer"));
				return;
			}
			
			StructurizedMail pager = client.getMail(session, uid,folerName,isMultiPage);
			if(pager == null)
			{
				response.setStatus(HttpServletResponse.SC_GONE);
//				out.write("<html><body><h1>´íÎó:ÎÞ·¨»ñµÃÓÊ¼þ£¬¿ÉÄÜÒÑ¾­±»É¾³ý</h1></body></html>");
				RequestDispatcher disp = request.getRequestDispatcher("/MailNotFound.jsp");
				disp.forward(request, response);
				return;
			}
			request.setAttribute("askedMail", pager);
			request.setAttribute("session", session);
			request.setAttribute("folderName",folerName);
			RequestDispatcher disp = request.getRequestDispatcher("/MailRender.jsp");
			disp.forward(request, response);
		}
		finally
		{
			client.quiteUserState();
		}
			
	}

}

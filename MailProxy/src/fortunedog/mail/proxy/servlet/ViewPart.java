package fortunedog.mail.proxy.servlet;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import fortunedog.mail.proxy.MailClient;
import fortunedog.mail.proxy.StructurizedMail;
import fortunedog.mail.reflow.ContentPager;
import fortunedog.mail.reflow.PageVisitor;
import fortunedog.util.Utils;

/**
 * get a certain part of a mail. Request to this svevlet should have query
 * string like:
 * http://localhost:8080/MailProxy/ViewPart?uid=1tbirQOOYEX9eQlIXgAAse&index=0
 * Note, the value for content-ID should be escaped to put in an URL.
 * 
 * This servlet used to view attachment online. doc, pdf will be convert to html
 * firstly, then paged to view page by page.
 * 
 * @see GetPart DownloadPart
 * @author Daniel
 * 
 */
public class ViewPart extends HttpServlet
{
	private static final long serialVersionUID = -3350469840518248383L;

	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException
	{
		String uid = request.getParameter("uid");
		String folderName = new String(request.getParameter("folderName").getBytes("ISO8859-1"),
										"UTF-8");
		String indexStr = request.getParameter("index");
		String internalPathStr = request.getParameter("internalPath");
		if (internalPathStr == null || internalPathStr.equals(""))
			internalPathStr = null;
		int pageNo = Integer.parseInt(request.getParameter("pageNo"));
		int attachIndex = -1;
		if (indexStr != null)
		{
			attachIndex = Integer.parseInt(indexStr);
		}
		String sid = request.getParameter("sid");
		java.io.PrintWriter out = response.getWriter();
		HttpSession session = null;
		if (sid != null)
			session = SessionListener.getSession(sid);
		if (session == null)
			session = request.getSession(false);
		if (session == null)
		{
			// response.sendRedirect("Relogin.jsp");
			response.setStatus(HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION);
			RequestDispatcher disp = request.getRequestDispatcher("/Relogin.jsp");
			disp.forward(request, response);
			// out.write("<html><body><h1>错误:登录失败</h1></body></html>");
			return;
		}
		MailClient mailClient = SessionListener.getStoredMailClient(session);
		if (mailClient == null)
		{
			// response.sendRedirect("Relogin.jsp");
			response.setStatus(HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION);
			RequestDispatcher disp = request.getRequestDispatcher("/Relogin.jsp");
			disp.forward(request, response);
			// out.write("<html><body><h1>错误:登录失败</h1></body></html>");
			return;
		}
		try {
			mailClient.enterUserState();
		} catch (InterruptedException e) {
			throw new ServletException(e);
		}
		try
		{
			StructurizedMail sMail = mailClient.getMail(session, uid, folderName);
			response.setContentType("text/html");
			ContentPager p = null;
			// if(internalPathStr == null)
			p = sMail.getAttachmentPager(attachIndex, session);
			// else {
			// p = sMail.getAttachmentPagerDirectly(attachIndex);
			// }
			if (p == null)
			{
				response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
				out.write(Utils.getResourceBundle(request).getString("previewNotSupported"));
				return;
			}
			try
			{
				PageVisitor visitor = new PageVisitor(uid, folderName, attachIndex, internalPathStr);
				p.visit(visitor);

				int totalPage = p.getPageCount();
				
				int clientVersionCode=Utils.getClientVersionCode(session);
				if (totalPage > 1)
				{
					if(clientVersionCode<4)
					{
						p.setNavBar(getPageNavBar(	uid, folderName, indexStr, internalPathStr, pageNo,
												totalPage, Utils.getClientLocale(request).getLanguage()));
					}
				}
				String html = null;
				if (p == null || (html = p.renderPage(pageNo)) == null)
				{
					throw new RuntimeException();
					/*
					 * response.setStatus(HttpServletResponse.
					 * SC_INTERNAL_SERVER_ERROR);
					 * out.write("<html><body><font>无法打开该附件。</font></body></html>"
					 * ); return;
					 */
				}
				
				
				
				if(clientVersionCode>=4)
				{
					request.setAttribute("pageNumber", pageNo);
					request.setAttribute("attachBody", html);
					request.setAttribute("totalPageCount", totalPage);
					RequestDispatcher disp = request.getRequestDispatcher("/AttachCache.jsp");
					disp.forward(request, response);
				}
				
				else {
					 out.write(html);
				}
				
			}
			catch (Exception excpt)
			{
				excpt.printStackTrace();

				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				out.write(Utils.getResourceBundle(request).getString("downloadToOpen"));
				return;
			}

		}
		catch (MessagingException e)
		{
			throw new ServletException(e);
		}
		finally
		{
			mailClient.quiteUserState();
		}

	}

	private String getPageNavBar(String uid, String folderName, String indexStr,
			String internalPath, int pageNo, int totalPage, String clientLang)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<div style=\"position:relative\"><table><tr><td bgcolor=\"#CCCCCC\">");
		String commonLinkStr;

		commonLinkStr = "<a href=\"/MailProxy2/ViewPart?uid="
						+ Utils.encodeUrlParam(uid)
						+ "&folderName="
						+ Utils.encodeUrlParam(folderName)
						+ "&index="
						+ indexStr
						+ "&lang=" + clientLang
						+ (internalPath == null ? "" : "&internalPath="
														+ Utils.encodeUrlParam(internalPath))
						+ "&pageNo=";

		if (pageNo != 0)
		{
			sb.append(commonLinkStr + (pageNo - 1) + "\" >上一页</a>");//don't translate to English, since this is called on old version only
		}

		int startPage = pageNo - 5;
		int endPage = pageNo + 5;
		if (startPage < 0)
		{
			startPage = 0;
			endPage = Math.min(10, totalPage);
		}
		if (endPage > totalPage)
		{
			endPage = totalPage;
			startPage = Math.max(0, endPage - 10);
		}

		for (int i = startPage; i < endPage; i++)
		{
			if (i == pageNo)
			{
				sb.append(i + 1);
			}
			else
			{
				sb.append(commonLinkStr + i + "\" >" + (i + 1) + "</a>");
			}
			sb.append("&nbsp;");
		}
		if (pageNo != totalPage - 1)
		{
			sb.append(commonLinkStr + (pageNo + 1) + "\" >下一页</a>");
		}
		sb.append("</td></tr></table></div>");

		return sb.toString();
	}
}
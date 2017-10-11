/*
 * Generated by the Jasper component of Apache Tomcat
 * Version: Apache Tomcat/7.0.41
 * Generated at: 2013-06-21 09:35:21 UTC
 * Note: The last modified time of this file was set to
 *       the last modified time of the source file after
 *       generation to assist with modification tracking.
 */
package org.apache.jsp;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import fortunedog.mail.reflow.PagerFactory;
import fortunedog.mail.reflow.ContentPager;
import fortunedog.util.*;
import fortunedog.mail.proxy.*;
import java.text.SimpleDateFormat;
import java.net.URLEncoder;
import java.util.*;
import java.util.Map.Entry;

public final class MailRender_jsp extends org.apache.jasper.runtime.HttpJspBase
    implements org.apache.jasper.runtime.JspSourceDependent {

public String formatAddr(String addr, boolean nameOnly)
	{
		if(Utils.isEmpty(addr))
			return "";
		
		String[] list = addr.split(";");
		
		//may exists repeating account in address list, so we store it at first.
		Map<String,String> addrMap = new HashMap<String,String>();
		for(String s : list)
		{
			EmailAddress a = new EmailAddress(s);
			String aname = a.getName();
			if(aname==null || addrMap.containsKey(aname))
				continue;
			addrMap.put(aname, a.getAddress());
		}
		StringBuilder sb =new StringBuilder(1024);
		Iterator<Entry<String,String>> itr=addrMap.entrySet().iterator();
		while (itr.hasNext()) 
		{
			Entry<String,String> entry=itr.next();
			sb.append(entry.getKey());
			if(!nameOnly)
				sb.append("&lt;<font style=\"color:blue\">").append(entry.getValue()).append("</font>&gt;");
			sb.append(';');
		}
		if(sb.length() == 0)
			return "";
		return sb.toString().substring(0, sb.length()-1);
		
	}
  private static final javax.servlet.jsp.JspFactory _jspxFactory =
          javax.servlet.jsp.JspFactory.getDefaultFactory();

  private static java.util.Map<java.lang.String,java.lang.Long> _jspx_dependants;

  private javax.el.ExpressionFactory _el_expressionfactory;
  private org.apache.tomcat.InstanceManager _jsp_instancemanager;

  public java.util.Map<java.lang.String,java.lang.Long> getDependants() {
    return _jspx_dependants;
  }

  public void _jspInit() {
    _el_expressionfactory = _jspxFactory.getJspApplicationContext(getServletConfig().getServletContext()).getExpressionFactory();
    _jsp_instancemanager = org.apache.jasper.runtime.InstanceManagerFactory.getInstanceManager(getServletConfig());
  }

  public void _jspDestroy() {
  }

  public void _jspService(final javax.servlet.http.HttpServletRequest request, final javax.servlet.http.HttpServletResponse response)
        throws java.io.IOException, javax.servlet.ServletException {

    final javax.servlet.jsp.PageContext pageContext;
    final javax.servlet.ServletContext application;
    final javax.servlet.ServletConfig config;
    javax.servlet.jsp.JspWriter out = null;
    final java.lang.Object page = this;
    javax.servlet.jsp.JspWriter _jspx_out = null;
    javax.servlet.jsp.PageContext _jspx_page_context = null;


    try {
      response.setContentType("text/html; charset=UTF-8");
      pageContext = _jspxFactory.getPageContext(this, request, response,
      			null, false, 8192, true);
      _jspx_page_context = pageContext;
      application = pageContext.getServletContext();
      config = pageContext.getServletConfig();
      out = pageContext.getOut();
      _jspx_out = out;

      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write(" \r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");

	String uid = request.getParameter("uid");
    String folderName=request.getParameter("folderName");
	int pageNo = Integer.parseInt(request.getParameter("pageNo"));
	fortunedog.mail.proxy.StructurizedMail structurizedMail = (fortunedog.mail.proxy.StructurizedMail) request
			.getAttribute("askedMail");
	javax.servlet.http.HttpSession session = (javax.servlet.http.HttpSession) request
			.getAttribute("session");
	fortunedog.mail.proxy.MailClient client = (fortunedog.mail.proxy.MailClient) session
			.getAttribute("mailClient");
	fortunedog.mail.proxy.net.MailSummary summary = structurizedMail.getSummary();		
	fortunedog.util.EmailAddress sender = new fortunedog.util.EmailAddress(summary.getFrom());
	ResourceBundle rb = Utils.getResourceBundle(request);
	Locale clientLocale = Utils.getClientLocale(request);
	boolean inChinese = clientLocale == Locale.CHINESE ;
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", clientLocale);
	SimpleDateFormat sdf2 = new SimpleDateFormat(rb.getString("shortDatetimeFmt"),clientLocale); //shortDateTimeFmt
	SimpleDateFormat sdf3 = new SimpleDateFormat(rb.getString("timeFmt"),clientLocale);

      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("<html>\r\n");
      out.write("<head>\r\n");
      out.write("\r\n");
      out.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\r\n");
      out.write("<meta\r\n");
      out.write("    name=\"viewport\"\r\n");
      out.write("\tcontent=\"width=100%; \r\n");
      out.write("\tinitial-scale=1;\r\n");
      out.write("\tmaximum-scale=1;\r\n");
      out.write("\tminimum-scale=1; \r\n");
      out.write("\tuser-scalable=no;\"\r\n");
      out.write("    />\r\n");
      out.write("<link rel=\"stylesheet\" type=\"text/css\" href=\"android_res/styles.css\" />\r\n");
      out.write("<script type=\"text/javascript\" src=\"android_res/scripts.js\"> </script>\r\n");
      out.write("\r\n");
      out.write("<script type=\"text/javascript\" >\r\n");
      out.write("function docload(){\r\n");
      out.write("\t\r\n");
      out.write("    subjectHeight = subjectDiv.scrollHeight;\r\n");
      out.write("    ");
if (request.getHeader("User-Agent").toLowerCase()
					.contains("iphone")) {
      out.write("\r\n");
      out.write("    \twindow.location = \"cmail://save\";\r\n");
      out.write("    ");
} else {
      out.write("\r\n");
      out.write("    \twindow.cmail.showHTML(document.getElementsByTagName('html')[0].innerHTML);\r\n");
      out.write("    \twindow.cmail.setMailBody(mailBody.innerHTML);\r\n");
      out.write("    ");
}
      out.write("\r\n");
      out.write("    \r\n");
      out.write("    \r\n");
      out.write("  ");
  for(int attachIndex=0;attachIndex<structurizedMail.attachmentParts.size();attachIndex++)
    {
		MailPart attach = structurizedMail.attachmentParts.get(attachIndex);
		String attachFileName = attach.getFileName();
				try
				{
					String attachFileSize= String.valueOf(attach.getSize());
					String attachmentFileSize = "";
					
					attachFileName = attachFileName.replaceAll("[:\'/\\\r\n]", "");
					
					attachmentFileSize = ContentPager.getHumanReadableSize(Integer.parseInt(attachFileSize));
					int startIndex=attachFileName.lastIndexOf(".");
				    String attachFormat=attachFileName.substring(startIndex+1,attachFileName.length());
				    boolean previewFlag = PagerFactory.canPreview(attachFileName);
						//window.cmail.addAttachInfo(1, "B.doc", "100KB");
	
      out.write("\r\n");
      out.write("    window.cmail.addAttachInfo(");
      out.print(attachIndex);
      out.write(',');
      out.write(' ');
      out.write('\'');
      out.print(attachFileName);
      out.write("', '");
      out.print(attachmentFileSize);
      out.write('\'');
      out.write(',');
      out.write('\'');
      out.print(attachFormat);
      out.write('\'');
      out.write(',');
      out.print(previewFlag);
      out.write(");\r\n");
      out.write("   ");

				}
				catch(Exception ex)
				{
					System.out.println("Decode atachname:"+attachFileName);
					ex.printStackTrace();
				}
	}
      out.write("\r\n");
      out.write("}\r\n");

if (structurizedMail.hasImage())
{

      out.write("\r\n");
      out.write("function showImage(ele,cid)\r\n");
      out.write("{\r\n");
      out.write("\tele.parentNode.innerHTML=\"<img src=\\\"GetMailPart?uid=");
      out.print(URLEncoder.encode(uid, "UTF-8"));
      out.write("&folderName=");
      out.print(URLEncoder.encode(folderName));
      out.write("&cid=\" \r\n");
      out.write("\t\t\t+  cid \r\n");
      out.write("\t\t\t+ \"\\\">\";\r\n");
      out.write("\treturn false;\r\n");
      out.write("\t\r\n");
      out.write("}\r\n");
}
      out.write("\r\n");
      out.write("\r\n");
      out.write("function reply(){\r\n");
if (request.getHeader("User-Agent").toLowerCase().contains("iphone")) {
      out.write("\r\n");
      out.write("\twindow.location = \"cmail://reply\";\r\n");
} else {
      out.write("\r\n");
      out.write("\twindow.cmail.reply();\r\n");
}
      out.write("\r\n");
      out.write("}\r\n");
      out.write("\r\n");
      out.write("function replyAll(){\r\n");
if (request.getHeader("User-Agent").toLowerCase().contains("iphone")) {
      out.write("\r\n");
      out.write("\t\twindow.location = \"cmail://replyAll\";\r\n");
} else {
      out.write("\r\n");
      out.write("\t\twindow.cmail.replyAll();\r\n");
}
      out.write("\r\n");
      out.write("}\r\n");
      out.write("\r\n");
      out.write("function forward(){\r\n");
if (request.getHeader("User-Agent").toLowerCase().contains("iphone")) {
      out.write("\r\n");
      out.write("\twindow.location = \"cmail://forward\";\r\n");
} else {
      out.write("\r\n");
      out.write("\twindow.cmail.forward();\r\n");
}
      out.write("\r\n");
      out.write("}\r\n");
      out.write("\r\n");
      out.write("function downloadAttachment(index, flag) {\r\n");
if (request.getHeader("User-Agent").toLowerCase().contains("iphone")) {
      out.write("\r\n");
      out.write("\twindow.location = \"cmail://download/?index=\"+index+\"&flag=\"+flag;\r\n");
} else {
      out.write("\r\n");
      out.write("\twindow.cmail.downloadAttachment(index, flag);\r\n");
}
      out.write("\t\r\n");
      out.write("}\r\n");
      out.write("\r\n");
      out.write("function openAttachment(index, name) {\r\n");
if (request.getHeader("User-Agent").toLowerCase().contains("iphone")) {
      out.write("\r\n");
      out.write("\twindow.location = \"cmail://open?index=\"+index+\"&name=\"+name;\r\n");
} else {
      out.write("\r\n");
      out.write("\twindow.cmail.openAttachment(index, name);\r\n");
}
      out.write("\r\n");
      out.write("}\r\n");
      out.write("</script>\r\n");

	
	String subjectText = summary.getSubject();
	if(fortunedog.util.Utils.isEmpty(subjectText))
		subjectText = "";
	else
		subjectText = javax.mail.internet.MimeUtility.decodeText(subjectText);

      out.write("\r\n");
      out.write("<title>");
      out.print(subjectText);
      out.write("</title>\r\n");
      out.write("</head>\r\n");
      out.write("<body onLoad=\"docload()\"  onscroll=\"scrollHandler()\" id=\"body\" class=\"bodyStyle\">\r\n");
      out.write("\t\t<div class=\"conversationHeaderDiv\" id=\"subjectDiv\" class=\"bodyStyle\">\r\n");
      out.write("\t\t\t<font>");
      out.print(subjectText);
      out.write("</font>\r\n");
      out.write("\t\t</div>\r\n");
      out.write("\t\t<div class=\"headerDivStyle\" id=\"headerDiv\" >\r\n");
      out.write("\t\t\t<table cellpadding=\"0\" cellspacing=\"1\" class=\"bodyStyle\"><tr>\r\n");
      out.write("\t\t\t\t<td>\r\n");
      out.write("\t\t\t\t\t<div class=\"presenceBgImg\"  >\r\n");
      out.write("\t\t\t\t\t\t<div><img   src=\"android_res/presence_invisible.png\"></div>\r\n");
      out.write("\t\t\t\t\t</div>\r\n");
      out.write("\t\t\t\t</td>\r\n");
      out.write("\t\t\t\t<td align='left' width='100%'>\r\n");
      out.write("\t\t\t\t\t<div class=\"fromDiv\" > <span style=\"color:#990000\" onclick=\"return false;\">");
      out.print((sender != null ? sender.getName() : ""));
      out.write("</span></div> <div class=\"addressDiv\"><span style=\"color:#999999\" onclick=\"return false;\">");
      out.print((sender != null ? sender.getAddress() : ""));
      out.write("</span></div>\r\n");
      out.write("\t\t\t\t</td>\r\n");
      out.write("\t\t\t</tr></table>\r\n");
      out.write("\t\t\t<div class=\"messageFooterDiv2\" >\r\n");
      out.write("\t\t\t\t<table cellpadding=\"0\" cellspacing=\"0\"  class=\"toolbarTableStyle\">\r\n");
      out.write("\t\t\t\t\t<tr align='right' >\r\n");
      out.write("\t\t\t\t\t\t<td>\r\n");
      out.write("\t\t\t\t\t\t\t<button class=\"footerButton\" onClick=\"toggoleStar()\" width=\"25\" ><img class=\"footerIconImg\" src=\"android_res/btn_star_big_buttonless_off.png\" id=\"starImg\"></button>\t\t\t\t\t\t\t\t\t\t\r\n");
      out.write("\t\t\t\t\t\t</td>\r\n");
      out.write("\t\t\t\t\t\t<td class=\"buttonTd\" style=\"display:none\" id=\"replyTd\">\r\n");
      out.write("\t\t\t\t\t\t\t\r\n");
      out.write("\t\t\t\t\t\t\t<button class=\"footerButton\" onClick=\"reply()\">\r\n");
      out.write("\t\t\t\t\t\t\t\t<div><img src=\"android_res/reply.png\" class=\"footerIconImg\"></div>\r\n");
      out.write("\t\t\t\t\t\t\t\t<div class=\"toolbarTextDivStyle\" id=\"textReply\">");
      out.print(rb.getString("reply"));
      out.write("</div>\r\n");
      out.write("\t\t\t\t\t\t\t</button>\r\n");
      out.write("\t\t\t\t\t  \t\t\r\n");
      out.write("\t\t\t\t\t  \t</td>\r\n");
      out.write("\t\t\t\t\t  \t<td class=\"buttonTd\">\r\n");
      out.write("\t\t\t\t\t  \t\t\r\n");
      out.write("\t\t\t\t\t\t\t<button class=\"footerButton\" onClick=\"replyAll()\">\r\n");
      out.write("\t\t\t\t\t\t\t\t<div><img src=\"android_res/replyAll.png\" class=\"footerIconImg\"></div>\r\n");
      out.write("\t\t\t\t\t\t\t\t<div class=\"toolbarTextDivStyle\" id=\"textReplyall\">");
      out.print(rb.getString("replyAll"));
      out.write("</div>\r\n");
      out.write("\t\t\t\t\t\t\t</button>\t\t\t\t\r\n");
      out.write("\t\t\t\t\t\t\t\r\n");
      out.write("\t\t\t\t\t\t</td>\r\n");
      out.write("\r\n");
      out.write("\t\t\t\t\t\t<td class=\"buttonTd\" style=\"display:none\" id=\"fwdTd\">\r\n");
      out.write("\t\t\t\t\t\t\t<button class=\"footerButton\" onClick=\"forward()\">\r\n");
      out.write("\t\t\t\t\t\t\t\t<div><img class=\"footerIconImg\" src=\"android_res/forward.png\"></div>\r\n");
      out.write("\t\t\t\t\t\t\t\t<div class=\"toolbarTextDivStyle\" id=\"textFwd\">");
      out.print(rb.getString("forward"));
      out.write("</div>\r\n");
      out.write("\t\t\t\t\t\t\t</button>\t\t\t\t\t\t\t\t\t\t\r\n");
      out.write("\t\t\t\t\t\t</td>\r\n");
      out.write("\t\t\t\t\t\t<td class=\"buttonTd\">\r\n");
      out.write("\t\t\t\t\t\t\t<button class=\"footerButton\" onClick=\"showAllButton()\" ><div style=\"width:30\"><img class=\"footerIconImg\" src=\"android_res/expand.png\" id=\"arrowBtn\"></div></button>\t\t\t\t\t\t\t\t\t\t\r\n");
      out.write("\t\t\t\t\t\t</td>\r\n");
      out.write("\t\t\t\t\t</tr>\r\n");
      out.write("\t\t\t\t</table>\r\n");
      out.write("\t\t\t</div>\t\t\r\n");
      out.write("\t\t</div>\r\n");
      out.write("\t\t<div id=\"scrollDiv\" >\r\n");
      out.write("\t\t<div class=\"headerDivStyle3\"  >\r\n");
      out.write("\t\t\t<table    class=\"recvDivStyle\" onclick=\"toggleRecipt()\" onfocus=\"this.blur();\"  id=\"reciptBriefTbl\">\r\n");
      out.write("\t\t\t\t<tr><td width=\"60\">");
      out.print(rb.getString("to"));
      out.write(":</td><td style=\"text-overflow: ellipsis;white-space:pre-wrap;\">");
      out.print(formatAddr(summary.to, true));
      out.write("</td><td>");
      out.print((summary.date == null ? "" : sdf2.format(summary.date)));
      out.write("</td></tr>\r\n");
      out.write("\t\t\t\t");

					String cc = formatAddr(summary.cc, true);
					if (!fortunedog.util.Utils.isEmpty(cc))
					{
						out.print("<tr><td width=\"60\" >"+rb.getString("cc")+":</td><td  colspan=\"2\">");
						out.print(cc);
						out.print("</td></tr>");
					}
				
      out.write("\r\n");
      out.write("\t\t\t\t<tr><td width=\"100\" style=\"font-size: 12\" colspan=\"2\">");
      out.print(rb.getString("showDetail"));
      out.write("</td></tr>\r\n");
      out.write("\t\t\t</table>\r\n");
      out.write("\t\t\t<table  width=\"100%\" class=\"recvDivStyle\" style=\"display:none\" onclick=\"toggleRecipt()\" id=\"reciptDetailTbl\">\r\n");
      out.write("\t\t\t\t<tr><td width=\"60\">");
      out.print(rb.getString("date"));
      out.write(":</td><td>");
      out.print((summary.date==null?"":sdf2.format(summary.date)));
      out.write("</td></tr>\r\n");
      out.write("\t\t\t\t<tr><td width=\"60\">");
      out.print(rb.getString("to"));
      out.write(":</td><td onclick=\"return false\">");
      out.print(formatAddr(summary.to, false));
      out.write("</td></tr>\r\n");
      out.write("\t\t\t\t");

					cc = formatAddr(summary.cc, false);
					if (!fortunedog.util.Utils.isEmpty(cc))
					{
						out.print("<tr><td width=\"60\">"+rb.getString("cc")+":</td><td>");
						out.print(cc);
						out.print("</td></tr>");
					}
				
      out.write("\r\n");
      out.write("\t\t\t\t<tr width=\"100%\"><td width=\"100\" style=\"font-size: 12\" colspan=\"2\">");
      out.print(rb.getString("hideDetail"));
      out.write("</td></tr>\r\n");
      out.write("\t\t\t</table>\r\n");
      out.write("\t\t</div>\r\n");
      out.write("\t\t");

			if (structurizedMail.hasImage() && !"1".equals(request.getParameter("full")))
			{
		
      out.write("\r\n");
      out.write("\t\t<div align='right'>\r\n");
      out.write("\t\t\t<button  style=\"height:50\"><a href=\"");
      out.print("/MailProxy2/MailRender?uid=" + Utils.encodeUrlParam(uid)
								+ "&folderName="+Utils.encodeUrlParam(folderName)
								+ "&pageNo=" + pageNo 
								+ "&full=1&lang="+Utils.getClientLocale(session).getLanguage());
      out.write('"');
      out.write('>');
      out.print(rb.getString("showAllImg"));
      out.write("</a></button>\r\n");
      out.write("\t\t</div>\r\n");
      out.write("\t\t");

			}
		
      out.write("\r\n");
      out.write("\t\t<div class=\"bodyCell\" id=\"bodyCell\" >\r\n");
      out.write("\t\t\t\r\n");
      out.write("<table cellspacing=\"0\"  bordercolor=\"#000000\" border=\"0\" >\r\n");
      out.write("\r\n");
      out.write("<tr height=\"100%\"><td><div id=\"mailBody\">");
      out.print(("1".equals(request.getParameter("full")) ? structurizedMail.getFullMailBody() : structurizedMail.renderPage(pageNo)));
      out.write("</div></td></tr>  \r\n");

	if (structurizedMail.getPageCount() > 1)
	{

      out.write("\r\n");
      out.write("<tr><td bgcolor=\"#CCCCCC\">\r\n");

	if (pageNo != 0)
		{
			out.print("<a href=" + Utils.urlBase + "/MailRender?uid="
						+ Utils.encodeUrlParam(uid) + "&folerName="+Utils.encodeUrlParam(folderName)+"&pageNo=" + (pageNo - 1)
						+ "&lang=" + Utils.getClientLocale(session).getLanguage() +">"+rb.getString("prev")+"</a>");
		}

		for (int i = 0; i < structurizedMail.getPageCount(); i++)
		{
			if (i == pageNo)
			{
				out.print(i + 1);
			}
			else
			{
				out.print("<a href=" + Utils.urlBase + "/MailRender?uid="
							+ Utils.encodeUrlParam(uid) +"&folerName="+Utils.encodeUrlParam(folderName)+"&pageNo=" + i + "&lang=" + Utils.getClientLocale(session).getLanguage() +">"
							+ (i + 1) + "</a>");
			}
			out.print("&nbsp;");
		}
		if (pageNo != structurizedMail.getPageCount() - 1)
		{
			out.print("<a href=" + Utils.urlBase + "/MailRender?uid="
						+ Utils.encodeUrlParam(uid) +"&folerName="+Utils.encodeUrlParam(folderName)+"&pageNo=" + (pageNo + 1)
						+ "&lang=" + Utils.getClientLocale(session).getLanguage() +">"+rb.getString("next")+"</a>");
		}

      out.write("\r\n");
      out.write(" </td></tr>\r\n");
      out.write(" ");

 	}
 
      out.write("\r\n");
      out.write("</table>\r\n");
      out.write("</div>\r\n");
      out.write("</div>\r\n");
      out.write("\r\n");
      out.write('\r');
      out.write('\n');

if (structurizedMail.hasAttachment()) { 
      out.write("\r\n");
      out.write("<div style=\"position:relative\">\r\n");
      out.write("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" class=\"attachTable2\">\r\n");
      out.write("\r\n");
      out.write("\t<tr>\r\n");
      out.write("   <td><img src=\"android_res/attach_table_bg_r1_c1.png\" width=\"19\" height=\"23\"/></td>\r\n");
      out.write("   <td width=\"100%\"><img src=\"android_res/attach_table_bg_r1_c2.png\" width=\"100%\" height=\"23\"/></td>\r\n");
      out.write("   <td><img src=\"android_res/attach_table_bg_r1_c3.png\" width=\"17\" height=\"23\"/></td>\r\n");
      out.write("  </tr>\r\n");
      out.write("  ");

	for(int attachIndex=0;attachIndex<structurizedMail.attachmentParts.size();attachIndex++)
	{
		MailPart attach = structurizedMail.attachmentParts.get(attachIndex);
		String attachFileName = attach.getFileName();
		
		try
		{
			String attachFileSize= String.valueOf(attach.getSize());
			String attachmentFileSize = "";
			attachFileName = javax.mail.internet.MimeUtility.decodeText(attachFileName);
			attachFileName = attachFileName.replaceAll("[:\'/\\\r\n]", "");
			attachFileSize= javax.mail.internet.MimeUtility.decodeText(attachFileSize);
			attachmentFileSize = ContentPager.getHumanReadableSize(Integer.parseInt(attachFileSize));
			int startIndex=attachFileName.lastIndexOf(".");
		    String attachFormat=attachFileName.substring(startIndex+1,attachFileName.length());
	
      out.write("\r\n");
      out.write(" <tr>\r\n");
      out.write("   <td><img src=\"android_res/attach_table_bg_r2_c1.png\"  height=\"58\"/></td>\r\n");
      out.write("   <td>\r\n");
      out.write("   \t\t<div>\r\n");
      out.write("\t   \t \t<img  src=\"android_res/attach_table_bg_r2_c2.png\" width=\"100%\" height=\"58\"  style=\"attachTable2\"/>\r\n");
      out.write("\t  \t\t<div style=\"position:relative;top:-58px\" height=\"58\" >\r\n");
      out.write("\t\t\t\t<table class=\"attachTable2\"><tr><img  class=\"attachTable2\" src=\"android_res/attach_tr_bg.png\" width=\"100%\" height=\"61\" /></tr></table>\r\n");
      out.write("\t\t\t\t<table  class=\"attachTable2\"  style=\"left:5px\">\r\n");
      out.write("\t\t\t\t\t<tr><td rowspan=\"2\" width=\"50px\" height=\"50\"><img  src=\"android_res/attach_icon_bg.png\"  /><img  src=\"android_res/");
      out.print(ContentPager.getFileImage(attachFormat));
      out.write("\" width=\"40\" height=\"40\" style=\"position:relative;top:-45;left:7\"/></td><td align=\"left\"><h1>");
      out.print(attachFileName);
      out.write('(');
      out.print(attachmentFileSize);
      out.write(")</h1></td></tr>\r\n");
      out.write("\t\t\t\t\t<tr><td >\r\n");
      out.write("\t\t\t\t\t\t<button class=\"attachBtn\" onClick=\"downloadAttachment(");
      out.print(attachIndex);
      out.write(", 'false')\" ><img  src=\"");
      out.print(inChinese ? "android_res/btn_download.png" : "android_res/btn_download_en.png");
      out.write("\"/></button>\r\n");
 if(PagerFactory.canPreview(attachFileName))
					{

      out.write("\t\t\t\t\t<button class=\"attachBtn\" onClick=\"openAttachment(");
      out.print(attachIndex);
      out.write(',');
      out.write('\'');
      out.print(attachFileName);
      out.write("')\" ><img  src=\"");
      out.print(inChinese ? "android_res/btn_preview.png" : "android_res/btn_preview_en.png");
      out.write("\"/></button>\r\n");
}
      out.write('\r');
      out.write('\n');
 if(attachFileName.endsWith(".ics"))
					{

      out.write("\t\t\t\t\t<button class=\"attachBtn\" onClick=\"window.cmail.importCalendarAttachment(");
      out.print(attachIndex);
      out.write(',');
      out.write('\'');
      out.print(attachFileName);
      out.write("')\"><img  src=\"");
      out.print(inChinese ? "android_res/btn_import.png" : "android_res/btn_import_en.png");
      out.write("\" /></button>\r\n");
}
      out.write("\t\r\n");
      out.write("\t\t\t\t\t</td> </tr>\r\n");
      out.write("\t\t\t\t</table>\r\n");
      out.write("\t   \t \t</div>\r\n");
      out.write("\t   \t \t\r\n");
      out.write("   \t \t </div>\r\n");
      out.write("   \t</td>\r\n");
      out.write("   <td><img  src=\"android_res/attach_table_bg_r2_c3.png\" height=\"58\"  /></td>\r\n");
      out.write("  </tr>\r\n");
      out.write("  ");
    
		}
		catch(Exception ex)
		{
			System.out.println("Decode atachname:"+attachFileName);
			ex.printStackTrace();
		}
} /*end for*/ 
      out.write("\r\n");
      out.write("  <tr>\r\n");
      out.write("   <td><img src=\"android_res/attach_table_bg_r3_c1.png\" height=\"25\" /></td>\r\n");
      out.write("   <td><img src=\"android_res/attach_table_bg_r3_c2.png\" width=\"100%\" height=\"25\"  /></td>\r\n");
      out.write("   <td><img  src=\"android_res/attach_table_bg_r3_c3.png\"  height=\"25\"  /></td>\r\n");
      out.write("  </tr></table>\r\n");
      out.write("</div>\r\n");
} /*end attachemnts list*/ 
      out.write("\r\n");
      out.write("</body>\r\n");
      out.write("</html>");
    } catch (java.lang.Throwable t) {
      if (!(t instanceof javax.servlet.jsp.SkipPageException)){
        out = _jspx_out;
        if (out != null && out.getBufferSize() != 0)
          try { out.clearBuffer(); } catch (java.io.IOException e) {}
        if (_jspx_page_context != null) _jspx_page_context.handlePageException(t);
        else throw new ServletException(t);
      }
    } finally {
      _jspxFactory.releasePageContext(_jspx_page_context);
    }
  }
}

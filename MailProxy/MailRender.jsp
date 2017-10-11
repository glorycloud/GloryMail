<%@page import="fortunedog.mail.reflow.PagerFactory"%>
<%@page import="fortunedog.mail.reflow.ContentPager"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page session="false"%> 
<%@page import="fortunedog.util.*"%>
<%@page import="fortunedog.mail.proxy.*"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="java.util.*" %>
<%@page import="java.util.Map.Entry" %>
<%
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
%>

<%!public String formatAddr(String addr, boolean nameOnly)
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
		
	}%>

<html>
<head>

<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta
    name="viewport"
	content="width=100%; 
	initial-scale=1;
	maximum-scale=1;
	minimum-scale=1; 
	user-scalable=no;"
    />
<link rel="stylesheet" type="text/css" href="android_res/styles.css" />
<script type="text/javascript" src="android_res/scripts.js"> </script>

<script type="text/javascript" >
function docload(){
	
    subjectHeight = subjectDiv.scrollHeight;
    <%if (request.getHeader("User-Agent").toLowerCase()
					.contains("iphone")) {%>
    	window.location = "cmail://save";
    <%} else {%>
    	window.cmail.showHTML(document.getElementsByTagName('html')[0].innerHTML);
    	window.cmail.setMailBody(mailBody.innerHTML);
    <%}%>
    
    
  <%  for(int attachIndex=0;attachIndex<structurizedMail.attachmentParts.size();attachIndex++)
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
	%>
    window.cmail.addAttachInfo(<%=attachIndex%>, '<%=attachFileName%>', '<%=attachmentFileSize%>','<%=attachFormat%>',<%=previewFlag%>);
   <%
				}
				catch(Exception ex)
				{
					System.out.println("Decode atachname:"+attachFileName);
					ex.printStackTrace();
				}
	}%>
}
<%
if (structurizedMail.hasImage())
{
%>
function showImage(ele,cid)
{
	ele.parentNode.innerHTML="<img src=\"GetMailPart?uid=<%=URLEncoder.encode(uid, "UTF-8")%>&folderName=<%=URLEncoder.encode(folderName)%>&cid=" 
			+  cid 
			+ "\">";
	return false;
	
}
<%}%>

function reply(){
<%if (request.getHeader("User-Agent").toLowerCase().contains("iphone")) {%>
	window.location = "cmail://reply";
<%} else {%>
	window.cmail.reply();
<%}%>
}

function replyAll(){
<%if (request.getHeader("User-Agent").toLowerCase().contains("iphone")) {%>
		window.location = "cmail://replyAll";
<%} else {%>
		window.cmail.replyAll();
<%}%>
}

function forward(){
<%if (request.getHeader("User-Agent").toLowerCase().contains("iphone")) {%>
	window.location = "cmail://forward";
<%} else {%>
	window.cmail.forward();
<%}%>
}

function downloadAttachment(index, flag) {
<%if (request.getHeader("User-Agent").toLowerCase().contains("iphone")) {%>
	window.location = "cmail://download/?index="+index+"&flag="+flag;
<%} else {%>
	window.cmail.downloadAttachment(index, flag);
<%}%>	
}

function openAttachment(index, name) {
<%if (request.getHeader("User-Agent").toLowerCase().contains("iphone")) {%>
	window.location = "cmail://open?index="+index+"&name="+name;
<%} else {%>
	window.cmail.openAttachment(index, name);
<%}%>
}
</script>
<%
	
	String subjectText = summary.getSubject();
	if(fortunedog.util.Utils.isEmpty(subjectText))
		subjectText = "";
	else
		subjectText = javax.mail.internet.MimeUtility.decodeText(subjectText);
%>
<title><%=subjectText%></title>
</head>
<body onLoad="docload()"  onscroll="scrollHandler()" id="body" class="bodyStyle">
		<div class="conversationHeaderDiv" id="subjectDiv" class="bodyStyle">
			<font><%=subjectText%></font>
		</div>
		<div class="headerDivStyle" id="headerDiv" >
			<table cellpadding="0" cellspacing="1" class="bodyStyle"><tr>
				<td>
					<div class="presenceBgImg"  >
						<div><img   src="android_res/presence_invisible.png"></div>
					</div>
				</td>
				<td align='left' width='100%'>
					<div class="fromDiv" > <span style="color:#990000" onclick="return false;"><%=(sender != null ? sender.getName() : "")%></span></div> <div class="addressDiv"><span style="color:#999999" onclick="return false;"><%=(sender != null ? sender.getAddress() : "")%></span></div>
				</td>
			</tr></table>
			<div class="messageFooterDiv2" >
				<table cellpadding="0" cellspacing="0"  class="toolbarTableStyle">
					<tr align='right' >
						<td>
							<button class="footerButton" onClick="toggoleStar()" width="25" ><img class="footerIconImg" src="android_res/btn_star_big_buttonless_off.png" id="starImg"></button>										
						</td>
						<td class="buttonTd" style="display:none" id="replyTd">
							
							<button class="footerButton" onClick="reply()">
								<div><img src="android_res/reply.png" class="footerIconImg"></div>
								<div class="toolbarTextDivStyle" id="textReply"><%=rb.getString("reply")%></div>
							</button>
					  		
					  	</td>
					  	<td class="buttonTd">
					  		
							<button class="footerButton" onClick="replyAll()">
								<div><img src="android_res/replyAll.png" class="footerIconImg"></div>
								<div class="toolbarTextDivStyle" id="textReplyall"><%=rb.getString("replyAll")%></div>
							</button>				
							
						</td>

						<td class="buttonTd" style="display:none" id="fwdTd">
							<button class="footerButton" onClick="forward()">
								<div><img class="footerIconImg" src="android_res/forward.png"></div>
								<div class="toolbarTextDivStyle" id="textFwd"><%=rb.getString("forward")%></div>
							</button>										
						</td>
						<td class="buttonTd">
							<button class="footerButton" onClick="showAllButton()" ><div style="width:30"><img class="footerIconImg" src="android_res/expand.png" id="arrowBtn"></div></button>										
						</td>
					</tr>
				</table>
			</div>		
		</div>
		<div id="scrollDiv" >
		<div class="headerDivStyle3"  >
			<table    class="recvDivStyle" onclick="toggleRecipt()" onfocus="this.blur();"  id="reciptBriefTbl">
				<tr><td width="60"><%=rb.getString("to")%>:</td><td style="text-overflow: ellipsis;white-space:pre-wrap;"><%=formatAddr(summary.to, true)%></td><td><%=(summary.date == null ? "" : sdf2.format(summary.date))%></td></tr>
				<%
					String cc = formatAddr(summary.cc, true);
					if (!fortunedog.util.Utils.isEmpty(cc))
					{
						out.print("<tr><td width=\"60\" >"+rb.getString("cc")+":</td><td  colspan=\"2\">");
						out.print(cc);
						out.print("</td></tr>");
					}
				%>
				<tr><td width="100" style="font-size: 12" colspan="2"><%=rb.getString("showDetail")%></td></tr>
			</table>
			<table  width="100%" class="recvDivStyle" style="display:none" onclick="toggleRecipt()" id="reciptDetailTbl">
				<tr><td width="60"><%=rb.getString("date")%>:</td><td><%=(summary.date==null?"":sdf2.format(summary.date))%></td></tr>
				<tr><td width="60"><%=rb.getString("to")%>:</td><td onclick="return false"><%=formatAddr(summary.to, false)%></td></tr>
				<%
					cc = formatAddr(summary.cc, false);
					if (!fortunedog.util.Utils.isEmpty(cc))
					{
						out.print("<tr><td width=\"60\">"+rb.getString("cc")+":</td><td>");
						out.print(cc);
						out.print("</td></tr>");
					}
				%>
				<tr width="100%"><td width="100" style="font-size: 12" colspan="2"><%=rb.getString("hideDetail")%></td></tr>
			</table>
		</div>
		<%
			if (structurizedMail.hasImage() && !"1".equals(request.getParameter("full")))
			{
		%>
		<div align='right'>
			<button  style="height:50"><a href="<%="/MailProxy2/MailRender?uid=" + Utils.encodeUrlParam(uid)
								+ "&folderName="+Utils.encodeUrlParam(folderName)
								+ "&pageNo=" + pageNo 
								+ "&full=1&lang="+Utils.getClientLocale(session).getLanguage()%>"><%=rb.getString("showAllImg")%></a></button>
		</div>
		<%
			}
		%>
		<div class="bodyCell" id="bodyCell" >
			
<table cellspacing="0"  bordercolor="#000000" border="0" >

<tr height="100%"><td><div id="mailBody"><%=("1".equals(request.getParameter("full")) ? structurizedMail.getFullMailBody() : structurizedMail.renderPage(pageNo))%></div></td></tr>  
<%
	if (structurizedMail.getPageCount() > 1)
	{
%>
<tr><td bgcolor="#CCCCCC">
<%
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
%>
 </td></tr>
 <%
 	}
 %>
</table>
</div>
</div>

<%-- div for attachments list --%>
<%
if (structurizedMail.hasAttachment()) { %>
<div style="position:relative">
<table border="0" cellpadding="0" cellspacing="0" width="100%" class="attachTable2">

	<tr>
   <td><img src="android_res/attach_table_bg_r1_c1.png" width="19" height="23"/></td>
   <td width="100%"><img src="android_res/attach_table_bg_r1_c2.png" width="100%" height="23"/></td>
   <td><img src="android_res/attach_table_bg_r1_c3.png" width="17" height="23"/></td>
  </tr>
  <%
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
	%>
 <tr>
   <td><img src="android_res/attach_table_bg_r2_c1.png"  height="58"/></td>
   <td>
   		<div>
	   	 	<img  src="android_res/attach_table_bg_r2_c2.png" width="100%" height="58"  style="attachTable2"/>
	  		<div style="position:relative;top:-58px" height="58" >
				<table class="attachTable2"><tr><img  class="attachTable2" src="android_res/attach_tr_bg.png" width="100%" height="61" /></tr></table>
				<table  class="attachTable2"  style="left:5px">
					<tr><td rowspan="2" width="50px" height="50"><img  src="android_res/attach_icon_bg.png"  /><img  src="android_res/<%=ContentPager.getFileImage(attachFormat)%>" width="40" height="40" style="position:relative;top:-45;left:7"/></td><td align="left"><h1><%=attachFileName%>(<%=attachmentFileSize%>)</h1></td></tr>
					<tr><td >
						<button class="attachBtn" onClick="downloadAttachment(<%=attachIndex%>, 'false')" ><img  src="<%=inChinese ? "android_res/btn_download.png" : "android_res/btn_download_en.png"%>"/></button>
<% if(PagerFactory.canPreview(attachFileName))
					{
%>					<button class="attachBtn" onClick="openAttachment(<%=attachIndex%>,'<%=attachFileName%>')" ><img  src="<%=inChinese ? "android_res/btn_preview.png" : "android_res/btn_preview_en.png"%>"/></button>
<%}%>
<% if(attachFileName.endsWith(".ics"))
					{
%>					<button class="attachBtn" onClick="window.cmail.importCalendarAttachment(<%=attachIndex%>,'<%=attachFileName%>')"><img  src="<%=inChinese ? "android_res/btn_import.png" : "android_res/btn_import_en.png"%>" /></button>
<%}%>	
					</td> </tr>
				</table>
	   	 	</div>
	   	 	
   	 	 </div>
   	</td>
   <td><img  src="android_res/attach_table_bg_r2_c3.png" height="58"  /></td>
  </tr>
  <%    
		}
		catch(Exception ex)
		{
			System.out.println("Decode atachname:"+attachFileName);
			ex.printStackTrace();
		}
} /*end for*/ %>
  <tr>
   <td><img src="android_res/attach_table_bg_r3_c1.png" height="25" /></td>
   <td><img src="android_res/attach_table_bg_r3_c2.png" width="100%" height="25"  /></td>
   <td><img  src="android_res/attach_table_bg_r3_c3.png"  height="25"  /></td>
  </tr></table>
</div>
<%} /*end attachemnts list*/ %>
</body>
</html>
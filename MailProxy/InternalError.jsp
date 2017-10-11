<%@page contentType="text/html;charset=UTF8" isErrorPage="true"%>
<%@ page session="false"%> 
<%@page import="fortunedog.util.*"%>
<%@page import="java.util.*" %>
<html>
<%
	ResourceBundle rb = Utils.getResourceBundle(request);
%>
<head><title><%=rb.getString("errTitle")%></title>
<meta http-equiv="cache-control" content="max-age=0" />
<meta http-equiv="cache-control" content="no-cache" />
<meta http-equiv="expires" content="0" />
<meta http-equiv="expires" content="Tue, 01 Jan 1980 1:00:00 GMT" />
<meta http-equiv="pragma" content="no-cache" />
</head>
<body>
	<p><%=rb.getString("internalError")%></p>
	<%
	java.util.logging.Logger.getAnonymousLogger().log(java.util.logging.Level.SEVERE, "Internal error, code:"
				+request.getAttribute("javax.servlet.error.status_code")
				+ "Info:"+request.getAttribute("javax.servlet.error.message")
				+ "Exception:"+ request.getAttribute("javax.servlet.error.exception_type"));
	exception.printStackTrace();
	%>
</html>
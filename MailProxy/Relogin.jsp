<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page session="false"%> 
<%@page import="fortunedog.util.*"%>
<%@page import="java.util.*" %>
<%response.setStatus(javax.servlet.http.HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION);%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<%
	ResourceBundle rb = Utils.getResourceBundle(request);
%>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title><%=rb.getString("loginTitle")%></title>
<script type="text/javascript" >
 
function docLoad()
{
<%if (request.getHeader("User-Agent").toLowerCase()
			.contains("iphone")) {%>
window.location = 'about:relogin';
<%} else {%>
window.cmail.relogin();
<%}%>

//	location='about:relogin';
}
</script>

</head>
<body  onload="docLoad()">
<p><%=rb.getString("logining")%><br><br><%=rb.getString("manualLogin")%></p>
</body>
</html>
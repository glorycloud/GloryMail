<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@page import="fortunedog.util.*"%>
<%@page import="java.util.*" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<meta
    name="viewport"
	content="width=100%; 
	initial-scale=1;
	maximum-scale=1;
	minimum-scale=1; 
	user-scalable=no;"
    />
<title>Insert title here</title>
</head>
<body style="left:0px; right:0px; margin: 0 0 0 0;">
   <img src='<%=Utils.getClientLocale(request)== Locale.CHINESE ? "android_res/MailNotFound.png" : "android_res/MailNotFound_en.png"%>' width='100%' height='100%' >
</body>
</html>
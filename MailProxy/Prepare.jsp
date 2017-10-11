<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page session="false"%> 
<%@page import="java.io.*"%>
<%@page import="java.sql.*"%>
<%@page import="java.util.logging.*"%>
<%@page import="javax.servlet.*"%>
<%@page import="javax.servlet.http.*"%>
<%@page import="fortunedog.util.*"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>test</title>

</head>
<body  >
<%
			Connection dbConn = null;
			Statement dbStat= null;
//			ServletOutputStream stream;
//			OutputStreamWriter out;
			try
			{
				
				dbConn = DbHelper.getConnection();
				dbConn.setAutoCommit(false);
				dbStat = dbConn.createStatement();
//				stream = resp.getOutputStream();
//				out = new OutputStreamWriter(stream, "UTF-8");
				java.util.logging.Logger log = Logger.getLogger(this.getClass().getName());
				
				dbStat.addBatch(" delete from mails where accountid=(select id from account where name='unittest2012@163.com')");
				dbStat.addBatch("delete from account where name='unittest2012@163.com'");
				
				dbStat.executeBatch();
				dbConn.commit();
			}
			catch(Exception ex)
			{
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
			finally
			{
				DbHelper.close(dbStat);
				DbHelper.close(dbConn);
				
//				IOUtils.closeQuietly(out);
//				IOUtils.closeQuietly(stream);
			}
		%>
</body>
</html>
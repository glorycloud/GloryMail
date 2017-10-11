package fortunedog.mail.test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fortunedog.util.DbHelper;

public class Prepare extends HttpServlet
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 8498493108392109242L;

	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
		{
			
			Connection dbConn = null;
			Statement dbStat= null;
//			ServletOutputStream stream;
//			OutputStreamWriter out;
			try
			{
				String mode = req.getParameter("DEVTESTMODE");
				if(mode != null)
					System.setProperty("DEVTESTMODE", mode);
				dbConn = DbHelper.getConnection();
				dbConn.setAutoCommit(false);
				dbStat = dbConn.createStatement();
//				stream = resp.getOutputStream();
//				out = new OutputStreamWriter(stream, "UTF-8");
				
				dbStat.addBatch(" delete from mails where accountid=(select id from account where name='unittest2012@163.com')");
				dbStat.addBatch("delete from account where name='unittest2012@163.com'");
				
				dbStat.executeBatch();
				dbConn.commit();
			}
			catch(Exception ex)
			{
				resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
			finally
			{
				DbHelper.close(dbStat);
				DbHelper.close(dbConn);
				
//				IOUtils.closeQuietly(out);
//				IOUtils.closeQuietly(stream);
			}
		}
}

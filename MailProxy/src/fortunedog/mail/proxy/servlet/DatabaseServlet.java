package fortunedog.mail.proxy.servlet;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fortunedog.mail.proxy.MailClient;
import fortunedog.mail.proxy.net.Result;
import fortunedog.util.DbHelper;
import fortunedog.util.Utils;

public abstract class DatabaseServlet extends HttpServlet
{
	static Logger log = LoggerFactory.getLogger(DatabaseServlet.class);
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static class ServiceData
	{
		protected Connection dbConn = null;
		protected Statement dbStat = null;
		protected ResultSet dbRst = null;
		protected HttpServletRequest request;
		protected HttpServletResponse response;
		protected HttpSession session;
		protected MailClient mailClient;
		///sqlite_refactor
		protected Statement sqliteStat = null;
		protected Connection sqliteConn = null;
		
		ServletOutputStream stream = null;
		java.io.OutputStreamWriter out = null;
	};
	protected void service(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException
	{
		Thread.currentThread().setName("Thread_"+this.getClass().getSimpleName());
		ServiceData d = new ServiceData();
		try
		{
			
			d.request = req;
			d.response = resp;
			d.dbConn = DbHelper.getConnection();
			d.dbStat = d.dbConn.createStatement();
			d.stream = resp.getOutputStream();
			d.out = new OutputStreamWriter(d.stream, "UTF-8");
			if(d.out == null)
			{
				log.info( "object output is null! ");
			}
			Result rst = dbService(d);
			if(rst != null)
			{
				rst.serialize(d.out);
				d.out.flush();
				d.stream.flush();
			}
		}
		catch(NeedLoginException ex)
		{
			d.response.setContentType("text/xml");
			d.response.setStatus(HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION);
			new Result(Result.NEEDLOGIN_FAIL, "Need Login").serialize(d.out);
		}
		catch(Throwable ex)
		{
			log.error("Server Error", ex);
			d.response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			new Result(Result.FAIL, "Server Error").serialize(d.out);
		}
		finally
		{
			DbHelper.close(d.dbRst);
			DbHelper.close(d.dbStat);
			DbHelper.close(d.dbConn);
			if(!DbHelper.shareSqliteConnection)
				DbHelper.close(d.sqliteConn);
			IOUtils.closeQuietly(d.out);
			IOUtils.closeQuietly(d.stream);
		}
	}
	
	protected boolean checkSession(ServiceData d, boolean useSid) throws NeedLoginException
	{
		
		d.session = Utils.getSession(d.request, useSid);
		
			

//		session = request.getSession(false); //possible error
		if(d.session == null)
		{
			throw new NeedLoginException("Need login");
		}
		return true;
	}

	protected boolean checkMailClient(ServiceData d) throws NeedLoginException
	{
		d.mailClient = SessionListener.getStoredMailClient(d.session);
		if(d.mailClient == null)
		{
			throw new NeedLoginException("Need login");
		}
		//sqlite_refactor
		if(d.sqliteStat == null )
		{
			try
			{
				d.sqliteConn= DbHelper.getConnection(d.mailClient.connData.accountId);
				d.sqliteStat = d.sqliteConn.createStatement();
			}
			catch(Exception e)
			{
				e.printStackTrace();
				d.sqliteStat = null;
			}
		}
		return true;
	}
	
	abstract Result dbService(ServiceData d)
	throws ServletException, IOException, SQLException, NeedLoginException;
	
}

class NeedLoginException extends Exception
{
	private static final long serialVersionUID = 8750770635832028997L;

	public NeedLoginException(String msg)
	{
		super(msg);
	}
}
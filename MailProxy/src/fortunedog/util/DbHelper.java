package fortunedog.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Random;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbHelper
{
	static Logger log = LoggerFactory.getLogger(DbHelper.class);
	public static final boolean shareSqliteConnection = false; //if set to true, will happen error java.sql.SQLException: SQL logic error or missing database
	//map for storing sqlite connections, key is account id.
	static private HashMap<Integer,Connection> _sqliteConnPool = new HashMap<Integer,Connection>();
	static final String DbFormatPath = "%s\\mail_%d.db";
	static String mailDataDir;
	static final String createTable = "CREATE TABLE `mails` ("+
	  "`uid` varchar(70) NOT NULL,"+
	  "`subject` varchar(512) DEFAULT NULL,"+
	  "`date` datetime DEFAULT NULL,"+
	  "`from` varchar(100) DEFAULT NULL,"+
	  "`state` tinyint(1) DEFAULT NULL,"+
	  "`index` int(11) DEFAULT NULL,"+
	  "`uidx` int(10) DEFAULT NULL,"+
	  "`to` varchar(512) DEFAULT NULL,"+
	  "`cc` varchar(512) DEFAULT NULL,"+
	  "`attachmentFlag` tinyint(1) DEFAULT 0,"+
	  "`previewContent` varchar(512) DEFAULT NULL,"+
	  "`foldername` varchar(50) NOT NULL DEFAULT 'INBOX',"+
	  "PRIMARY KEY (`uid`,`foldername`));";
	static final String createCacheTable = "create table cache( mailROWID unsign int, contentType varchar(64), fileName varchar(256), contentId varchar(64), disposition varchar(128), size long int, content blob);";
//				+"CREATE INDEX [indx] ON [cache] ([partId]);";
	static public boolean useSqlite = true; 
	
	
	static private DataSource dataSource ;
	static {
		try
		{
			
			Context initContext = new InitialContext();
			Context envContext = null;
			try
			{
				envContext = (Context)initContext.lookup("java:/comp/env");
				if(envContext != null)
				{//we are using Tomcat
					dataSource= (DataSource)envContext.lookup("jdbc/TestDB"); //这里就是连接池的名称
					mailDataDir = (String)envContext.lookup("mailDataDir");
				}
				else
				{//we are using Geronimo
					dataSource = (DataSource)initContext.lookup("java:comp/env/jdbc/MyDataSource");
				}
			}
			catch(Exception ex2)
			{
				
			}
			
				
			
			
		}
		catch (NamingException e)
		{
			log.error( "Fail to init context for DB connection pool", e);
		}
	
	}
	public static Connection getConnection() throws SQLException
	{
	    Connection conn = null;
		if(dataSource == null)
		{//has failed in searching in JNDI
		    String url = "jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=utf8&characterSetResults=utf8&connectionCollation=utf8";
		    String driver = "com.mysql.jdbc.Driver";
		    String userName = "root"; 
		    String password = "root";
		    try 
		    {
		      Class.forName(driver).newInstance();
		      conn = DriverManager.getConnection(url,userName,password);
		    }
		    catch (Exception e) 
		    {
		      e.printStackTrace();
		      return null;
		    }
		}
		else
			conn = dataSource.getConnection(); 

		Statement st = null;
		try
		{
			st = conn.createStatement();
			st.execute("set names utf8");
		}
		catch(Exception e)
		{
			close(st);
		}
		finally
		{
			DbHelper.close(st);
		}
		return conn;
	}
	
	public static Connection getConnection(int accountId)
	{
		
		synchronized (_sqliteConnPool)
		{
			Connection conn = _sqliteConnPool.get(accountId);
			if(shareSqliteConnection)
			{
				if(conn != null)
				{
					try {
						if(!conn.isClosed())
							return conn;
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			String dbFile = String.format(DbFormatPath, mailDataDir, accountId);
			boolean existed = new File(dbFile).exists();
			try
			{
				if(_sqliteConnPool.isEmpty())
					Class.forName("org.sqlite.JDBC");
				conn = DriverManager.getConnection("jdbc:sqlite:"+dbFile);
				// if it's created right now, create table.
				if (!existed)
				{
					Statement stat = conn.createStatement();
					stat.executeUpdate(createTable);
					stat.executeUpdate(createCacheTable);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				return null;
			}
			_sqliteConnPool.put(accountId, conn);
			cleanState(conn);
			return conn;
		}
	}
	
	public static void close(java.sql.Statement st)
    {

		try
		{
            if(st != null)
				st.close();
		}
		catch (Throwable ex)
		{
            ex.printStackTrace();
		}
    }

    public static void close(java.sql.ResultSet rst)
    {

        try
        {
            if(rst != null)
                rst.close();
        }
        catch (Throwable ex)
        {
        	ex.printStackTrace();
        }
    }
    
	public static void close(java.sql.Connection st)
    {

		try
		{
            if(st != null)
				st.close();
		}
		catch (Throwable ex)
		{
            ex.printStackTrace();
		}
    }
	

	public static int executScalar(String sql) throws SQLException
	{
		Connection conn = null;
		Statement st = null;
		ResultSet rst = null;
		try
		{
			conn = getConnection();
			st = conn.createStatement();
			rst = st.executeQuery(sql);
			rst.next();
			return rst.getInt(1);
		}
		finally
		{
			DbHelper.close(rst);
			DbHelper.close(st);
			DbHelper.close(conn);
		}

	}
	
	public static String executScalarString(String sql, String... args) throws SQLException
	{
		Connection conn = null;
		PreparedStatement st = null;
		ResultSet rst = null;
		try
		{
			conn = getConnection();
			st = conn.prepareStatement(sql);
			for(int i=0;i<args.length;i++)
				st.setString(i+1, args[i]);
			rst = st.executeQuery();
			rst.next();
			return rst.getString(1);
		}
		finally
		{
			DbHelper.close(rst);
			DbHelper.close(st);
			DbHelper.close(conn);
		}

	}
	/**
	 * This function will try to run a statement 3 times if there's SQLException. 
	 * A transaction may fail because of deadlock, retry after a random time later, the deadlock may has gone.
	 * 
	 * As MySQL official document, http://dev.mysql.com/doc/refman/5.0/en/innodb-deadlocks.html
	 * You can cope with deadlocks and reduce the likelihood of their occurrence with the following techniques:
	 * Use SHOW ENGINE INNODB STATUS to determine the cause of the latest deadlock. That can help you to tune 
	 *     your application to avoid deadlocks.
	 * Always be prepared to re-issue a transaction if it fails due to deadlock. Deadlocks are not dangerous. 
	 *     Just try again.
	 * @param conn
	 * @param st
	 * @return
	 * @throws SQLException
	 */
	public static void doTransaction(Connection conn, Statement st , String ... sqls) throws SQLException
	{
		SQLException ex = null;
		for(int i=0;i<3;i++)
		{
			try
			{
				for(String sql : sqls)
				{
					st.execute(sql);
				}
				
				conn.commit();
				return;
			}
			catch(SQLException e)
			{
				e.printStackTrace();
				conn.rollback();
				ex = e;
			}
			Random rd = new Random();
			try
			{
				Thread.sleep(rd.nextInt(200)+1); //sleep a moment to avoid deadlock
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		throw ex;
	}

	public static void closeConnectionForAccount(int accountId) {
		Connection conn = _sqliteConnPool.remove(accountId);
		try {
			conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void checkState(Connection conn)
	{
		if(conn == null)
			return;//some times, this is called during exception handling, and conn has not been created
		Statement st = null;
		ResultSet rst = null;
		try
		{
			String sql = "select count(*) from mails  where date is null and uidx is not null";
			st = conn.createStatement();
			rst = st.executeQuery(sql);
			rst.next();
			int ct =  rst.getInt(1);
			if(ct != 0)
			{
				Exception e = new Exception("State ERROR detected");
				log.debug("Error mail count:"+ct, e);
				
			}
		} catch (SQLException e) {
			log.debug("State check fail:", e);
		}
		finally
		{
			DbHelper.close(rst);
			DbHelper.close(st);
		}
	}
	
	public static void cleanState(Connection conn)
	{
		Statement st = null;
		try
		{
			String sql = "delete  from mails  where date is null and uidx is not null";
			st = conn.createStatement();
			st.executeUpdate(sql);
			
		} catch (SQLException e) {
			log.debug("cleanState fail:", e);
		}
		finally
		{
			DbHelper.close(st);
		}
	}
}

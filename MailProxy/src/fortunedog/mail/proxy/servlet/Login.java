package fortunedog.mail.proxy.servlet;

import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fortunedog.mail.proxy.MailClient;
import fortunedog.mail.proxy.MailClient.ConnectionData;
import fortunedog.mail.proxy.net.Result;
import fortunedog.util.DbHelper;
import fortunedog.util.Utils;

/**
 * this servlet accept the following argument from request
 * accountName : full name of email account, e.g. liu_lele@126.com
 * password  : password to login to mail server
 * Request to this svevelet should have qurey string like:
 * http://.../Login?account=liu_lele%40126.com&password=xxxxx
 * 
 * @author dliu
 *
 */
public class Login extends HttpServlet
{
	public static final String HEADER_MAIL_CLIENT = "MailClient"; // this header
	public static final String HEADER_OS_VERSION = "OSVer"; //this header is Android system version
	static Logger log = LoggerFactory.getLogger(Login.class);
	/**
	 * ²âÊÔ£ºÃÜÂë´íÎóÊ±ÄÜ·ñ·µ»Ø´íÎó
	 */
	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException
	{
//		byte[] buf = Utils.readPostData(request);
		
		org.jdom.input.SAXBuilder xmlBuilder = new SAXBuilder();
		Connection conn = null;
		Statement st = null;
		ResultSet rst = null;
		ResultSet rst1=null;
		ResultSet rst2=null;
		log.info("MailClient:" + request.getHeader("MailClient"));
		log.info( "Entering Login servelet");
		response.setHeader("Cache-Control", "no-cache");
		try
		{
			String account;
			String password;
			conn = DbHelper.getConnection();
			st = conn.createStatement();
			if("POST".equals(request.getMethod()))
			{
				ServletInputStream s = request.getInputStream();
				InputStreamReader ireader = new InputStreamReader(s, "UTF-8");
//				StringBuilder sb2 = new StringBuilder();
//				int c;
//				while((c = ireader.read()) > 0)
//					sb2.append((char)c);
				
				org.jdom.Document doc = xmlBuilder.build(ireader);
				Element root = doc.getRootElement();
				account = root.getAttributeValue("name");
				password = root.getAttributeValue("password");
				String loginName = root.getAttributeValue("loginName");
				String mailServer = root.getAttributeValue("mailServer");
				String smtpServer = root.getAttributeValue("smtpServer");
				String serverType = root.getAttributeValue("serverType");				
				String smtpPort = root.getAttributeValue("smtpPort");
				String useSSL = root.getAttributeValue("useSSL");
				String mailPort = root.getAttributeValue("mailPort");
				if(account.endsWith("189.cn") && mailServer.equals("pop.189.cn"))//fix bug 202
					serverType = "pop3";
				
				rst = st.executeQuery("select count(*) from account where name='"+account +"'");
				rst.next();
				if(rst.getInt(1) > 0)
				{
					rst2=st.executeQuery("select serverType,ID from account where name='"+account+"'");
					if(rst2.next()&&!serverType.equals(rst2.getString(1)))
					{
						/////////sqlite_refactor,here we only need to remove the corresponding sqlite db file
						if (DbHelper.useSqlite)
						{
							Connection sqliteConn = null;
							try
							{
								sqliteConn = DbHelper.getConnection(rst2.getInt(2));
								Statement sqliteSt = sqliteConn.createStatement();
								sqliteSt.execute("delete from mails");
								DbHelper.close(sqliteSt);
							}
							finally
							{
								if(!DbHelper.shareSqliteConnection)
								{//close connection here, to avoid database lock error
									DbHelper.close(sqliteConn);
									sqliteConn = null;
								}
							}
						}
						else
						{
							st.execute("delete from mails where accountId in (select ID from account where name='"
										+ account + "')");
							st.execute("delete from folders where accountid in (select ID from account where name='"
										+ account + "')");
						}
					}
					StringBuilder sb = new StringBuilder("update account set mailServer='")	.append(mailServer)
									.append("', smtpServer='").append(smtpServer)
									.append("', serverType='").append(serverType)
									.append("', smtpPort=").append(smtpPort)
									.append(", mailPort=").append(mailPort)
									.append(", useSSL=").append(useSSL)
									.append(", loginName='").append(loginName)
									.append("' where name='").append(account)
									.append("'");
					st.execute(sb.toString());
				}
				else
				{
					StringBuilder sb = new StringBuilder("insert into account (name, mailServer, smtpServer, serverType,smtpPort,mailPort,useSSL,loginName) values('")
									.append(account)
									.append("','").append(mailServer)
									.append("','").append(smtpServer)
									.append("', '").append(serverType)
									.append("',").append(smtpPort)
									.append(",").append(mailPort)
									.append(",").append(useSSL)
									.append(",'").append(loginName)
									.append("')");
					
					st.execute(sb.toString());
				}
			}
			else
			{
				account = request.getParameter("account");
				password = request.getParameter("password");
			}
			String agentId = request.getHeader(HEADER_MAIL_CLIENT);
			Thread.currentThread().setName("Login_"+account);
			response.setContentType("text/xml");
			HttpSession existingSession = request.getSession(false);
			Result result= null;
			MailClient mailClient = null;
			if(existingSession != null)
			{
				log.info("Existing session found in Login!!!");
				MailClient old = SessionListener.getStoredMailClient(existingSession);
				if(old != null)
				{ //Close old, to avoid exception: java.lang.IllegalStateException: Folder is Open
				  //also, we can synchronize mails again
					old.enterUserState();
					try
					{
						if(old.inSynching(0) || old.checkOpen())
						{
							//the old connection is ok for use
							result = new Result();
						}
						else
							old.close();
					}
					finally
					{
						old.quiteUserState();
					}
					mailClient = old;
				}
			}
			if(result == null)
			{
				mailClient = MailClient.getClientInstance(account, password, false);
				boolean loginSuccess = true;
				if( mailClient != null)
				{
					HttpSession session = request.getSession(true);
					session.setAttribute(SessionListener.ATTR_MAIL_CLIENT, mailClient);
					String lang = request.getHeader("lang");
					if(lang != null && lang.startsWith("zh"))
						session.setAttribute("lang", Locale.CHINESE);
					else
						session.setAttribute("lang", Locale.ENGLISH);
					String l = request.getLocale().getLanguage();
					log.info("Session created for account:" + account + " " + session.getId());
					Integer versionCode = 1;
					if(!Utils.isEmpty(agentId) && agentId.startsWith("CloudMail"))
					{
						String version = agentId.substring(9);
						if(version != null)
						{
							if(version.startsWith("1.2"))
							{
								versionCode = 2;
							}
							else if(version.startsWith("1.3"))
							{
								versionCode = 3;
							}
							else if(version.startsWith("1.4"))
							{
								versionCode = 4;
							}
						}
					}
					session.setAttribute("clientVersion", versionCode);
					//by now, we write to client directly, 
					//login successfully, if the account setting not existing in 
					//knownservers, then add it.
					addKnownServer(mailClient.connData);
					StringBuffer buffer = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
					buffer.append("<result class=\"fortunedog.mail.proxy.net.Result\">\n");
					buffer.append("<status code=\"" + Result.SUCCESSED + "\"/>\n");
					buffer.append("<content>\n");
					rst1 = st.executeQuery("select * from folders where accountid="+mailClient.connData.accountId);
					String forderName=null;
					String displayName=null;
					while (rst1.next())
					{
						forderName=rst1.getString(3);
						displayName=rst1.getString(4);
						buffer.append("<folder foldername=\""+Utils.escapeStr(forderName)+"\" displayname=\""+Utils.escapeStr(displayName)+"\" />\n");
					}
					buffer.append("</content></result>");
					response.getWriter().write(buffer.toString());	
					response.getWriter().flush();
					
				}
				else
				{
					response.setStatus(HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION);
					result = new Result(Result.AUTH_FAIL, "Fail to connect server, passord or server setting incorrect");
					result.serialize(response.getWriter());
					loginSuccess = false;
				}
				StringBuilder sb = new StringBuilder("update account set ")
				.append(loginSuccess?"lastActive":"lastFailedLogin")
				.append("=Now() where name='")
				.append(account).append("'");
				st.execute(sb.toString());
			}
			else
			{
				result.serialize(response.getWriter());
			}
			response.getWriter().close();
		}
		catch (Exception e)
		{
			log.error("Login fail", e);
			response.setStatus(HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION);
			Result result = new Result(Result.FAIL, e.getMessage());
			result.serialize(response.getWriter());
			response.getWriter().close();
		}
		finally
		{
			DbHelper.close(rst);
			DbHelper.close(rst1);
			DbHelper.close(rst2);
			DbHelper.close(st);
			DbHelper.close(conn);
		}
		log.info( "Leave Login servelet");
	}
	
	private void addKnownServer(ConnectionData connData)
	{
		String account = connData.accountName;
		int pos = account.indexOf('@');
		if(pos <= 0)
			return;
		String domainName = account.substring(pos+1);
		if(Utils.isEmpty(domainName))
			return;
		Connection conn = null;
		Statement st = null;
		ResultSet rst = null;
		PreparedStatement pst  = null;
		try
		{
			conn = DbHelper.getConnection();
			st = conn.createStatement();
			rst = st.executeQuery("select count(*) from knownServers where domainName='"+domainName+"' and enterprise='0'");
			rst.next();
			if(rst.getInt(1)>0)//added before
			{
				return;
			}
			//otherwise we add it to table knownservers.
			pst = conn.prepareStatement(
			 "insert into `knownservers` (`domainName`, `loginName`, `mailServer`, `smtpServer`, `serverType`, `smtpPort`, `useSSL`, `mailPort`) values(?,?,?,?,?,?,?,?)");
			pst.setString(1, domainName);
			if(connData.loginName.equals(account))
				pst.setString(2,"$USER@"+domainName);
			else
				pst.setString(2, "$USER");
			pst.setString(3, connData.mailServer);
			pst.setString(4, connData.smtpServer);
			pst.setString(5,connData.serverType==MailClient.TYPE_POP3?"pop3":"imap");
			pst.setString(6, connData.smtpPort+"");
			pst.setString(7, connData.useSSL?"true":"false");
			pst.setString(8, connData.mailPort+"");
			pst.execute();
			
		}
		catch(Exception expt)
		{
			expt.printStackTrace();
		}
		finally
		{
			DbHelper.close(rst);
			DbHelper.close(st);
			DbHelper.close(pst);
			DbHelper.close(conn);
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -6343219513855263503L;
	
}

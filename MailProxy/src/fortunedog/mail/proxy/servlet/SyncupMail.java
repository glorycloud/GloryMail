package fortunedog.mail.proxy.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

import javax.mail.MessagingException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fortunedog.mail.proxy.MailClient;
import fortunedog.mail.proxy.MailStatus;
import fortunedog.mail.proxy.net.MailSummary;
import fortunedog.mail.proxy.net.Result;
import fortunedog.util.DbHelper;

public class SyncupMail extends DatabaseServlet
{
	private static int MAIL_COUNT_PER_PACKET = 10;
	static Logger log = LoggerFactory.getLogger(SyncupMail.class);

	@Override
	Result dbService(ServiceData d) throws ServletException, IOException, SQLException,
			NeedLoginException
	{
		// log.log(Level.INFO,
		// "Servlet Version:"+this.getServletContext().getMajorVersion()+"."+this.getServletContext().getMinorVersion());
		// Servelet version 2.5 in tomcat6
		log.info( "Entering SyncupMail servelet");
		checkSession(d, true);
		checkMailClient(d);
		int normalMaxIdx = Integer.parseInt(d.request.getParameter("nm"));
		int deleteMaxIdx = Integer.parseInt(d.request.getParameter("dm"));
		int mailCount = MAIL_COUNT_PER_PACKET;
		if (d.request.getParameter("mc") != null)
		{
			mailCount = Integer.parseInt(d.request.getParameter("mc"));
		}
		long LE = Long.MAX_VALUE;
		if (d.request.getParameter("LE") != null)
		{
			LE = Integer.parseInt(d.request.getParameter("LE"));
		}
		String checkNew = d.request.getParameter("cn");
		MailClient.ClientRequestData reqData = new MailClient.ClientRequestData();
		reqData.uidxMax = normalMaxIdx;
		reqData.requestedMailCount = mailCount;
		reqData.requestedUidxCeil = LE;
		if(d.request.getParameter("foldername")==null)
			reqData.folderName="INBOX";
		else
			reqData.folderName=new String(d.request.getParameter("foldername").getBytes("ISO8859-1"), "UTF-8");
//		d.mailClient.setClientRequestData(reqData);
		Thread.currentThread().setName("Syncup_"+d.mailClient.connData.accountName);
		try
		{
			d.mailClient.enterUserState();
		}
		catch (InterruptedException e1)
		{
			return null;
		}
		try
		{
		
			boolean checkNewInitated = false;
			if ("1".equals(checkNew)/* User ask to check new mail */|| d.mailClient.isImap() )
			{
				try
				{
					int r = d.mailClient.checkNewMails(reqData);
					if (r != Result.SUCCESSED)
					{
						if (r == Result.AUTH_FAIL)
							throw new NeedLoginException("Login Failed");
						else
							throw new ServletException("Failed to check new mail");
					}
					checkNewInitated = true;
					
				}
				catch (MessagingException e)
				{
					e.printStackTrace();
					throw new ServletException("Failed to check new mail:" + e.getMessage());
				}
	
			}
	
			TreeSet<MailSummary> waitingMail=new TreeSet<MailSummary>(new Comparator<MailSummary>()
			{
				@Override
				public int compare(MailSummary o1, MailSummary o2)
				{
					int r = o1.uidx>(o2.uidx)?-1:(o1.uidx==(o2.uidx)?0:1);
					return r;
				}
			});
			StringBuilder sqlWhere = new StringBuilder(" where `uidx` > ").append(normalMaxIdx);
			if (LE != Long.MAX_VALUE)
			{
				sqlWhere.append(" and `uidx` <= ").append(LE);
			}
		/////sqlite_refactor
			sqlWhere.append(" and state !=").append(MailStatus.MAIL_TO_DEL)
					.append(" and foldername='" + reqData.folderName + "'");
			Statement varSt = d.sqliteStat;
			if(!DbHelper.useSqlite)
			{
				sqlWhere.append(" and accountId="+d.mailClient.connData.accountId);
				varSt = d.dbStat;
			}
			String sql = "select * from mails "	+ sqlWhere.append(" order by uidx DESC limit ").append(mailCount + 1);
	//		System.err.println(sql);
			d.dbRst = varSt.executeQuery(sql.toString());
			while (d.dbRst.next())
			{
				//log.info( "Select mail:");
				MailSummary s = new MailSummary(d.dbRst.getString("uid"));
				s.date = d.dbRst.getTimestamp("date");
				if(s.date==null||s.date.toString()=="")
					break;
				s.from = d.dbRst.getString("from");
				s.to = d.dbRst.getString("to");
				s.cc = d.dbRst.getString("cc");
				s.subject = d.dbRst.getString("subject");
				s.uidx =d.dbRst.getInt("uidx");
				s.index = d.dbRst.getInt("index");
				s.state = d.dbRst.getInt("state");
				s.attachmentFlag = d.dbRst.getInt("attachmentFlag");
				s.folderName = d.dbRst.getString("foldername");
				waitingMail.add(s);
				//log.info( "\t\tSubject:"+s.subject);
			}
			DbHelper.close(d.dbRst);
			
			//get all deleted mails, also obey the limitation 
			sqlWhere = new StringBuilder(" where `uidx` > ").append(deleteMaxIdx);
			
		///sqlite_refactor
			sqlWhere.append(" and state=").append(MailStatus.MAIL_TO_DEL).append(" and foldername='" + reqData.folderName + "'");
			if(!DbHelper.useSqlite)
				sqlWhere.append(" and accountId="+d.mailClient.connData.accountId);
			sql = "select * from mails " + sqlWhere.append(" order by uidx DESC limit ").append(mailCount + 1);
			System.err.println(sql);
			d.dbRst = varSt.executeQuery(sql.toString());
			while (d.dbRst.next())
			{
				MailSummary s = new MailSummary(d.dbRst.getString("uid"));
				s.date = d.dbRst.getTimestamp("date");
				if(s.date==null||s.date.toString()=="")
					break;
				s.from = d.dbRst.getString("from");
				s.to = d.dbRst.getString("to");
				s.cc = d.dbRst.getString("cc");
				s.subject = d.dbRst.getString("subject");
				s.uidx =d.dbRst.getInt("uidx");
				s.index = d.dbRst.getInt("index");
				s.state = d.dbRst.getInt("state");
				s.attachmentFlag = d.dbRst.getInt("attachmentFlag");
				s.folderName = d.dbRst.getString("foldername");
				waitingMail.add(s);
			}
	
			d.response.setContentType("text/xml");
			int mailTransfered = 0;
			
			d.out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			d.out.write("<result class=\"fortunedog.mail.proxy.net.MailListResult\">\n");
			d.out.write("<status code=\"" + Result.SUCCESSED + "\"/>\n");
			d.out.write("<content>\n");
			if(!DbHelper.shareSqliteConnection)
			{
				DbHelper.close(d.sqliteStat);
				DbHelper.close(d.sqliteConn);
				d.sqliteStat=null;
				d.sqliteConn = null;
			}
			if (checkNewInitated )
			{
				Iterator<MailSummary> syncedMails = reqData.syncState.getSyncedMails();
				
	iterateNewMails:
				while(syncedMails.hasNext())
				{
					MailSummary mailSummary = syncedMails.next();
					//log.info( "synced new mail:" + mailSummary.subject);
					waitingMail.add(mailSummary);
					do{
						MailSummary s = waitingMail.pollFirst();
						if(s.state == MailStatus.MAIL_TO_DEL)
						{
							d.out.write(s.toXml());
							continue;
						}
						if(mailTransfered==mailCount)
						{
							s.state |= MailStatus.FLAG_HAS_MORE_PLACEHOLD;
							d.out.write(s.toXml());
							mailTransfered++;
							break iterateNewMails;
						}
						else 
						{
							d.out.write(s.toXml());
						}
						mailTransfered++;
					
					}while(!waitingMail.isEmpty() && waitingMail.first().uidx >= mailSummary.uidx);
				}
			}
			Iterator<MailSummary> iterator=waitingMail.iterator();
			while(iterator.hasNext() && mailTransfered<=mailCount)
			{
				MailSummary mailSummary=iterator.next();
				//log.info( "waiting mail:" + mailSummary.subject);
				if(mailSummary.state == MailStatus.MAIL_TO_DEL)
				{
					d.out.write(mailSummary.toXml());
					continue;
				}
				if(mailTransfered==mailCount)
				{
					mailSummary.state |= MailStatus.FLAG_HAS_MORE_PLACEHOLD;
					d.out.write(mailSummary.toXml());
					break;
				}
				d.out.write(mailSummary.toXml());
				mailTransfered++;
			}
			d.out.write("</content></result>");
			
	//		System.out.println(buffer.toString());
			d.out.flush();
			d.stream.flush();

		}
		catch(Throwable t)
		{
			if(d.session != null)
				SessionListener.removeStoredMailClient(d.session);
			log.error( "Fail syncup mail" ,t );

		}
		finally
		{
			d.mailClient.quiteUserState();
		}
		return null;
	}

	@Override
	public void init() throws ServletException
	{

		Context env;
		try
		{
			env = (Context) new InitialContext().lookup("java:comp/env");
			MAIL_COUNT_PER_PACKET = (Integer) env.lookup("maxMailCountPerPacket");
		}
		catch (NamingException e)
		{
			e.printStackTrace();
		}
	}

}

package fortunedog.mail.storage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import javax.mail.MessagingException;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fortunedog.mail.proxy.MailClient;
import fortunedog.mail.proxy.MailStatus;
import fortunedog.mail.proxy.StructurizedMail;
import fortunedog.mail.proxy.net.MailSummary;
import fortunedog.util.DbHelper;

public class MailCache
{
	static Logger log = LoggerFactory.getLogger(MailClient.class);
	
	int accountId;

	public static class MailNotCachedException extends Exception
	{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
	}
	public MailCache(int accountId)
	{
		this.accountId = accountId;
		
	}

	public void put(String folderName, String uid, StructurizedMail mail)
	{
		Connection conn = null;
		try
		{
			conn = DbHelper.getConnection(accountId);
			mail.store(conn);
		}
		catch (SQLException e)
		{
			log.warn("Fail to store mail", e);
		}
		catch (IOException e)
		{
			log.warn("Fail to store mail", e);
		}
		catch (MessagingException e)
		{
			log.warn("Fail to store mail", e);
		}
		finally
		{
			DbHelper.close(conn);
		}
	}
	
	public StructurizedMail get(String folderName, String uid)
	{
		Connection conn = null;
		try
		{
			MailSummary s = MailSummary.createFromDb(uid, accountId, folderName);
			conn = DbHelper.getConnection(accountId);
			StructurizedMail m = new StructurizedMail(conn, s);
			return m;
		}
		catch (SQLException e)
		{
			log.warn("Fail to store mail", e);
		}
		catch (IOException e)
		{
			log.warn("Fail to store mail", e);
		}
		catch (MessagingException e)
		{
			log.warn("Fail to store mail", e);
		}
		catch (MailNotCachedException e)
		{
			
		}
		finally
		{
			DbHelper.close(conn);
		}
		return null;
	}

	public void cacheMails(MailClient mailClient)
	{
		Connection conn = null;
		PreparedStatement st = null;
		ResultSet rst  = null;
		ArrayList<String > uids = new ArrayList<String>(50);
		ArrayList<String > folders = new ArrayList<String>(50);
		try
		{
			conn = DbHelper.getConnection(accountId);
			st = conn.prepareStatement("select uid, foldername from mails where date>? and state!="+MailStatus.MAIL_TO_DEL+" and not exists (select * from cache where mailROWID=mails.ROWID)");
			Date d = DateUtils.addDays(new Date(), -3);
			st.setDate(1, new java.sql.Date(d.getTime()));
			rst = st.executeQuery();
			while(rst.next())
			{
				String uid = rst.getString(1);
				String f = rst.getString(2);
				uids.add(uid);
				folders.add(f);
			}
			

		}
		catch (SQLException e)
		{
			log.warn("Fail to store mail", e);
		}
		
		finally
		{
			DbHelper.close(rst);
			DbHelper.close(st);
			DbHelper.close(conn);
		}
		for(int i=0;i<uids.size(); i++)
		{
			mailClient.getMail(null, uids.get(i), folders.get(i));
		}
		
	}
}

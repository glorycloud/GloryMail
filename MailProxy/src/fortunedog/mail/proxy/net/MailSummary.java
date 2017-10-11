package fortunedog.mail.proxy.net;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.ResourceBundle;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import org.apache.geronimo.javamail.store.imap.IMAPMessage;
import org.apache.geronimo.javamail.store.pop3.POP3Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import fortunedog.mail.proxy.MailClient;
import fortunedog.mail.proxy.MailStatus;
import fortunedog.mail.proxy.servlet.SetCharacterEncodingFilter;
import fortunedog.util.DbHelper;
import fortunedog.util.Utils;

/**
 * bug: 当subject或其他邮件内容，是编码和非编码格式混合时，如："=?gb2312?Q?=D6=B0=B3=A1=BE=AB=D3=A2=D4=C2=BF=AFNo.15 =CC=F8=B2=DB=A3=AC=C4=E3=D7=BC=B1=B8=BA=C3=C1=CB=C2=F0=A3=BF?="
 * MimeUtility.decodeText()函数无法正确解码
 * @author dliu
 *
 */
public class MailSummary
{
	public static final int ATF_NO_ATTACH = 0;
	public static final int ATF_NORMAL_ATTACH = 1;
	public static final int ATF_CALENDAR_ATTACH = ATF_NORMAL_ATTACH<<1;
	static Logger log = LoggerFactory.getLogger(MailSummary.class);
	
	public MailSummary(int accountId, String uidl, javax.mail.Message msg, int index)
	{
		this.accountId = accountId;
		this.uid = uidl;
		this.index = index;
		try
		{
			subject = msg.getSubject();
			if(!Utils.isEmpty(subject))
				subject = Utils.ensureStringValidate(javax.mail.internet.MimeUtility.decodeText(subject));
			else
				subject = "";
			
		}
		catch (Exception e)
		{
			if(e instanceof UnsupportedEncodingException || e instanceof MessagingException)
			{
				String[] s;
				try
				{
					s = msg.getHeader("Subject");
					if(s != null && s.length > 1)
						subject=s[0];
				}
				catch (Exception e1)
				{
					subject = "";
					
				}
			}
			else 
				throw (RuntimeException)e;
		}
		
		try
		{
			date = msg.getReceivedDate();
			if(date == null)
				date = msg.getSentDate();
			if(date == null)
				date = new Date();
		}
		catch (Exception e1)
		{
			date = new Date();
		}
		
		
		Address[] a;
		try
		{
			
			// FROM
			if ( (a= msg.getFrom()) != null)
			{
				try
				{
					from = "";
					for (int j = 0; j < a.length; j++)
					{
						if(a[j] != null)
							from += javax.mail.internet.MimeUtility.decodeText(a[j].toString()) + " ";
					}
				}
				catch (Exception e)
				{
					
				}
			}
		}
		catch(Exception e)
		{
			String[] fs;
			try
			{
				fs = msg.getHeader("From");
				if(fs != null)
					from = fs[0];
			}
			catch (Exception e1)
			{
			}
		}
		
		if(Utils.isEmpty(from))
		{
			ResourceBundle rb = Utils.getResourceBundle(SetCharacterEncodingFilter.getCurrentRequest());
			from = rb.getString("anonymous");
		}
		try
		{
			 // TO
	        if ((a = msg.getRecipients(javax.mail.Message.RecipientType.TO)) != null) 
	        {
	        	to = "";
	            for (int j = 0; j < a.length; j++)
	            {
	                InternetAddress ia = (InternetAddress)a[j];
	                to += javax.mail.internet.MimeUtility.decodeText(ia.toString())+";";
	                //don't extract group members.
	      //          if (ia.isGroup()) {
	     //               InternetAddress[] aa = ia.getGroup(false);
	    //            }
	            }
	        }
		}
		catch(Exception e)
		{
			String[] tos;
			try
			{
				tos = msg.getHeader("To");
				if(tos != null)
					to = tos[0];
			}	
			catch (Exception e1)
			{
				//some times, we got null 'tos' object, and get null point exception
			}
		}
		
 
		try
		{
			if ((a = msg.getRecipients(javax.mail.Message.RecipientType.CC)) != null) 
	        {
	        	cc = "";
	            for (int j = 0; j < a.length; j++)
	            {
	                InternetAddress ia = (InternetAddress)a[j];
	                cc += javax.mail.internet.MimeUtility.decodeText(ia.toString())+";";
	            }
	        }
		}
		catch(Exception e)
		{
			String[] ccs;
			try
			{
				ccs = msg.getHeader("CC");
				if(cc != null)
					cc = ccs[0];
			}
			catch (Exception e1)
			{
			}
		}
		
		
		try
		{
			if(msg instanceof POP3Message)
				attachmentFlag = getAttachFlag(((POP3Message)msg).getAttachmentNames());
			else if(msg instanceof IMAPMessage)
				attachmentFlag = getAttachFlag(((IMAPMessage)msg).getAttachmentNames());
		}
		catch (Exception e)
		{
			log.warn("fail get attachmentFlag", e);
		}
		
		try
		{
			String[] oldMsgId = msg.getHeader("Message-ID");
			if(oldMsgId.length > 0)
			{
				messageId = oldMsgId[0];
			}
		}
		catch (MessagingException e)
		{
			log.warn("fail get Message-ID", e);
		}
		
	}
	
	static public int getAttachFlag(LinkedList<String> attachNames)
	{
		int attachFlag = ATF_NO_ATTACH;
		for(String attachName:attachNames)
		{
			if(attachName.endsWith(".ics"))
				attachFlag |= ATF_CALENDAR_ATTACH;
			else
				attachFlag |= ATF_NORMAL_ATTACH;
		}
		return attachFlag;
	}
	
	public MailSummary(String uid)
	{
		this.uid = uid;
		subject = "";
		from = "";
		date = new Date();
		to = "";
		cc = "";
		messageId = "";
	}
	
	static public MailSummary createFromDb(String uid, int accountId, String folder)
	{
		Connection conn = null;
		PreparedStatement dbStat = null;
		ResultSet dbRst = null;
		try
		{
			String sql = "select rowid,* from mails where uid=? and folderName=?";
			if(DbHelper.useSqlite)
			{
				conn = DbHelper.getConnection(accountId);
			}
			else
			{
				sql += " and accountId="+accountId;
				conn = DbHelper.getConnection();
			}
			
			dbStat = conn.prepareStatement(sql);
			dbStat.setString(1, uid);
			dbStat.setString(2, folder);
			dbRst = dbStat.executeQuery();
			if(dbRst.next())
			{
				MailSummary s = new MailSummary(dbRst.getString("uid"));
				s.rowId = dbRst.getLong("ROWID");
				s.date = dbRst.getTimestamp("date");
				s.from = dbRst.getString("from");
				s.to = dbRst.getString("to");
				s.cc = dbRst.getString("cc");
				s.subject = dbRst.getString("subject");
				s.uidx = dbRst.getInt("uidx");
				s.index = dbRst.getInt("index");
				s.state = dbRst.getInt("state");
				s.attachmentFlag = dbRst.getInt("attachmentFlag");
				s.previewContent = dbRst.getString("previewContent");
				s.accountId = accountId;
				s.folderName = dbRst.getString("foldername");
				return s;
			}
			log.debug("Mail not found in db, account:{},uid:{}", accountId, uid);
			return null;
		}
		catch(SQLException ex)
		{
			log.error("fail create MailSummary from DB", ex);
			return null;
		}
		finally
		{
			DbHelper.close(dbRst);
			DbHelper.close(dbStat);
			if(!DbHelper.useSqlite)
				DbHelper.close(conn);
		}
	}
	
	/**
	 * return a string like
	 *   <mail uid="xxxx" uidx="xxx" 
	 * @return
	 *
	 */
	public String toXml()
	{
		this.subject =this.subject==null?"":this.subject;
		this.from=this.from==null?"":this.from;
		this.to=this.to==null?"":this.to;
		this.cc=this.cc==null?"":this.cc;
		String dateString=this.date==null?"":Utils.netDateFormater.format(this.date);
		return "<mail uid=\""+Utils.escapeStr(this.uid)+"\" subject=\""+Utils.escapeStr(this.subject)+"\" date=\""+dateString+"\" from=\""+Utils.escapeStr(this.from)+"\" index=\""+this.index+"\" uidx=\""+this.uidx+"\" state=\""+this.state+"\" to=\""+Utils.escapeStr(this.to)+"\" cc=\""+Utils.escapeStr(this.cc)+"\" attachmentFlag=\""+this.attachmentFlag+"\" foldername=\""+folderName+"\" />\n";
		
	}
	public int accountId;
	public String uid;
	public String subject;
	public Date date;
	public String from;
	public String to;//if more than one recipients, separated by ";"
	public String cc;
	public int index;
	public int uidx;
	public int state = MailStatus.MAIL_NEW;
	public int attachmentFlag = 0;
	public String folderName;
	public String previewContent;
	public String messageId ; 
	private long rowId;//row id in db

	public String getFrom()
	{
		return from;
	}

	public String getTo()
	{
		return to;
	}

	public String getCc()
	{
		return cc;
	}

	public String getSubject()
	{
		return subject;
	}

	public String getMessageId()
	{
		return messageId;
	}

	public Date getSentDate()
	{
		return date;
	}
	
	public long getRowId()
	{
		Utils.ASSERT(rowId > 0);
		return rowId;
	}
}

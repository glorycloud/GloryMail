package fortunedog.mail.proxy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.mail.Message;
import javax.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fortunedog.mail.proxy.MailClient.ClientRequestData;
import fortunedog.mail.proxy.net.Result;
import fortunedog.util.DbHelper;

abstract public class MailProtocol
{
	final static String mbox = "INBOX";
	static Logger log = LoggerFactory.getLogger(MailProtocol.class);
	protected MailClient mailClient;

	public MailProtocol(MailClient mailClient2)
	{
		mailClient = mailClient2;
	}

	/**
	 * called by syncup thread, syncup mail's between mail server and cloud server
	 */
	protected abstract void syncupMail(ClientRequestData reqData);
		
	/**
	 * get a raw mail by uid (uid for IMAP and POP3)
	 * @param uid
	 * @return
	 * @throws MessagingException
	 */
	abstract Message retrieveRawMail(String uid) throws MessagingException;
	
	/**
	 * check whether mail server is connected. and try to connect if not.
	 * @return true if mail server is connected, false if server not connected and failed to reconnect
	 * @throws MessagingException
	 */
	abstract boolean checkOpen() throws MessagingException;
	
	/**
	 * make a connect to mail server. This method will do nothing if it has already connected.
	 * after connecting, current folder will changed to folder
	 * @param forcePop3Resync
	 * @param folder the folder to set as current folder
	 * @return
	 */
	abstract int connect(boolean forcePop3Resync,String folder);
	
	/**
	 * close this mail connection
	 */
	abstract void close();
	
	/**
	 * ask to check new mails. This will initiate a syncup on necessary.
	 * @param reqData 
	 * @return
	 * @throws MessagingException
	 */
	/**
	 * 
	 */
	abstract int listForders(String root);
	
	public int checkNewMails(ClientRequestData reqData) throws MessagingException
	{
		int r = connect(true,reqData.folderName);
		if(r != Result.SUCCESSED) //syncUidWithDb has called in connect, for POP3 server, force to resync POP3, so we can get latest mail
		{
			return r;
		}
		ClientRequestData workingData = MailClient.addToSyncQueue(mailClient, reqData);
		return Result.SUCCESSED;
	}

	abstract boolean waitingNewMail() throws MessagingException, InterruptedException;
	
	/////sqlite_refactor, replacement of mysql function reassignMailUidx2
	protected void reassignMailUidx(int accountId,String folderName)
	{
		Statement st = null;
		PreparedStatement upSt = null;
		Statement st1 = null;
		Connection conn = null;
		Connection mailConn = null;
		try
		{
			conn =DbHelper.getConnection();
			int maxUidx = DbHelper.executScalar("select mailIndexCounter from account where ID="+accountId);
			mailConn = DbHelper.getConnection(accountId);			
			st = mailConn.createStatement();
			ResultSet rset = st.executeQuery("select uid from mails where uidx is null and folderName='"+folderName+"' and state="+MailStatus.MAIL_TO_DEL);
			upSt = mailConn.prepareStatement("update mails set uidx=? where uid=? and folderName='"+folderName+"' and state="+MailStatus.MAIL_TO_DEL);
			while(rset.next())
			{
				upSt.setInt(1, ++maxUidx);
				upSt.setString(2, rset.getString(1));
				upSt.addBatch();
			}
			mailConn.setAutoCommit(false);
			try
			{
				upSt.executeBatch();
			}
			finally
			{
				mailConn.commit();
				mailConn.setAutoCommit(true);
			}
			DbHelper.checkState(mailConn);
			st1 = conn.createStatement();
			st1.execute("update account set mailIndexCounter="+maxUidx+" where ID="+accountId);
		}
		catch(SQLException e)
		{
			log.warn("Failed to reassign mail uidx for account "+accountId, e);
		}
		finally
		{
			DbHelper.close(st);
			DbHelper.close(upSt);
			DbHelper.close(st1);
			DbHelper.close(conn);
			if(!DbHelper.shareSqliteConnection)
				DbHelper.close(mailConn);
		}
	}

	abstract void interruptWaiting() throws MessagingException, InterruptedException;

	
}

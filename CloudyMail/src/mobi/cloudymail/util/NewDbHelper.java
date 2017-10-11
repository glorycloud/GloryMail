package mobi.cloudymail.util;

import static mobi.cloudymail.util.Utils.LOGTAG;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import mobi.cloudymail.data.FolderInfo;
import mobi.cloudymail.data.InMailInfo;
import mobi.cloudymail.data.MailInfo;
import mobi.cloudymail.data.MailStatus;
import mobi.cloudymail.data.OutMailInfo;
import mobi.cloudymail.mailclient.AddressBook;
import mobi.cloudymail.mailclient.EmailAddress;
import mobi.cloudymail.mailclient.R;
import mobi.cloudymail.mailclient.net.Account;
import mobi.cloudymail.mailclient.net.AttachmentInfo;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;



public class NewDbHelper extends SQLiteOpenHelper
{
	
	private final static int DATABASE_VERSION_V12 = 4; //version of CloudyMail 1.2
	private final static int DATABASE_VERSION_V13 = 5; //version of CloudyMail 1.3
	private final static int DATABASE_VERSION_V14 = 6; //version of CloudyMail 1.4
	private final static int DATABASE_VERSION_V142 = 7; //version of CloudyMail 1.4.2
	private final static int DATABASE_VERSION_V15 = 8; //
	private final static int DATABASE_VERSION_CURRENT = DATABASE_VERSION_V15;
	private final static String DATABASE_NAME = "client.db";
	private static String _dbFile;
	private static NewDbHelper _instance = null;
	private static NewDbHelper _UiCriticalInstance = null; //to used for UI update, and not blocked by background thread
	public static final int ORDER_BY_DATE = 1;
	public static final int ORDER_BY_SUBJECT = 2;
	public static final int ORDER_BY_FROM = 3;
	public static final int ORDER_BY_UNREAD = 4;
	public static final int ORDER_BY_READED = 5;
	public static final int ORDER_BY_READEDSTATE = 6;
	public static final int ORDER_BY_ASTERISK = 7;
    
    
	/* finish */
	public static NewDbHelper getInstance()
	{
		
		if (_instance == null)
		{
			ApplicationInfo info = MyApp.instance()
					.getApplicationInfo();
			_dbFile = info.dataDir + "/databases/" + DATABASE_NAME;
			dbFileObj = new File(_dbFile);
			if (!dbFileObj.exists())
			{
				try
				{
					{
						File dir = new File(info.dataDir + "/databases");
						boolean rst;
						if (!dir.exists())
						{
							rst = dir.mkdirs();
						}
						InputStream s = MyApp.instance().getAssets()
								.open(DATABASE_NAME);
						FileOutputStream fo = new FileOutputStream(_dbFile);
						IOUtils.copy(s, fo);
						s.close();
						fo.close();
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			_instance = new NewDbHelper(MyApp.instance());
			
			int version = _instance.executScalar("select value from sysconfig where name='clientVersion'", null);
			_instance.upgradeToV15(version);
		}
		return _instance;
	}
	
	/**
	 * get a instance to used in circumstance of UI, this instance should used in UI thread only, and not blocked by backgroud thread
	 * @return
	 */
	public static NewDbHelper getUiCriticalInstance()
	{
		if(_UiCriticalInstance == null)
		{
			getInstance();
			_UiCriticalInstance = new NewDbHelper(MyApp.instance());
		}
		return _UiCriticalInstance;
	}
	
	public long getModificationTime()
	{
		return dbFileObj.lastModified();
	}
	@Override
	public void onCreate(SQLiteDatabase db)
	{
		// if database not existed, this function will be invoked.
		Log.d(LOGTAG,"onCreate"+ "database file not exists");
	}

	private NewDbHelper(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION_CURRENT);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		// if version is higher, this function will be called.

		/*
		 * String sql = "drop table if exists [t_contacts]"; db.execSQL(sql); //
		 * 濠殿喗绺块崐鏇㈡晸閼恒儲鍎熼柨鐔告灮閹风兘鏁撻弬銈嗗闁跨喐鏋婚幏鐑芥晸缁插湢闁荤姴娴勯幏鐑芥晸閿熺氮ql = "CREATE TABLE [t_contacts] (" + "[id] AUTOINC," +
		 * "[name] VARCHAR(20) NOT NULL ON CONFLICT FAIL," +
		 * "[telephone] VARCHAR(20) NOT NULL ON CONFLICT FAIL," +
		 * "[email] VARCHAR(20)," + "[photo] BINARY, " +
		 * "CONSTRAINT [sqlite_autoindex_t_contacts_1] PRIMARY KEY ([id]))";
		 * db.execSQL(sql);
		 */

//		upgradeToV12(db);
	}

	
	
	// execute insert,update,delete statement
	public void execSQL(String sql, Object[] args)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL(sql, args);
	}

	// execute batch of sql statement
	/*
	 * public void execSQLBatch(String []sql, Object[] args) { SQLiteDatabase db
	 * = this.getWritableDatabase(); db.beginTransaction(); for(int i = 0; i <
	 * 100; i++) db.execSQL(sql, args); db.setTransactionSuccessful();
	 * db.endTransaction(); }
	 */

	// select statement.
	public Cursor query(String sql, String[] args)
	{
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(sql, args);
		return cursor;
	}

	public int executScalar(String sql, String[] args)
	{
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(sql, args);
		int result = 0;
		try
		{
			if (cursor.moveToFirst())
				result = cursor.getInt(0);
		}
		finally
		{
			cursor.close();
		}
		return result;
	}

	// convenience method for loading accounts.


	// return error string if failed.
	public String addAccount(Account a)
	{
		SQLiteDatabase db = this.getReadableDatabase();
		String queryTxt = "select count(id) from account where name='" + a.name
							+ "'";
		Cursor cursor = db.rawQuery(queryTxt, null);
		if (cursor == null)
			return "Cursor is null";
		String errStr = "";
		cursor.moveToFirst();
		if (cursor.getInt(0) > 0)
		{
			errStr = MyApp.instance().getResources()
					.getString(R.string.err_accountExist);
			cursor.close();
			return errStr;
		}
//		String[] argsStrings = { a.name, a.loginName, a.mailServer,
//								a.getMailPort() + "", a.smtpServer,
//								a.smtpPort + "", a.serverType,
//								(a.useSSL ? 1 : 0) + "" };
//		String CommandText = "insert into account (name, loginName, mailServer, "
//								+ "mailPort, smtpServer, smtpPort, serverType, useSSL) "
//								+ "values(?,?,?,?,?,?,?,?)";
		//encryption
		String encryptPassword = null;
		try
		{
			 encryptPassword = SimpleCrypto.encrypt("&cloudyServices@cloudymail.mobi&",a.password);
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[] argsStrings = { a.name, a.loginName, a.mailServer,
								a.getMailPort() + "", a.smtpServer,
								a.smtpPort + "", a.serverType,
								(a.useSSL ? 1 : 0) + "", encryptPassword};
//		(a.useSSL ? 1 : 0) + "", a.password };
		
		
		String CommandText = "insert into account (name, loginName, mailServer, "
								+ "mailPort, smtpServer, smtpPort, serverType, useSSL, password) "
								+ "values(?,?,?,?,?,?,?,?,?)";
		this.getWritableDatabase().execSQL(CommandText, argsStrings);

		CommandText = "select id from account where rowid=last_insert_rowid()";
		cursor.close();
		cursor = db.rawQuery(CommandText, null);
		try
		{
			if (cursor == null)
				return "Cursor is null";
			cursor.moveToFirst();
			a.id = cursor.getInt(0);
		}
		finally
		{
			cursor.close();
		}
		
		return errStr;
	}

	public String deleteAccount(Account a)
	{
		String errStr = "";
		SQLiteDatabase db = getWritableDatabase();
		// try {
		String cmdTxt = "delete from account where name=?";
		String[] args = { a.name };
		db.beginTransaction();
		db.execSQL(cmdTxt, args);

		// the mails belonged to this account will also be deleted.
		cmdTxt = "delete from mail where accountId=" + a.id;
		db.execSQL(cmdTxt);

		cmdTxt= "delete from folders where accountid="+a.id;
		db.execSQL(cmdTxt);
		
		cmdTxt= "delete from attachmentInfo where accountid="+a.id;
		db.execSQL(cmdTxt);
		cmdTxt= "delete from mailGroup where accountid="+a.id;
		db.execSQL(cmdTxt);
		
		db.setTransactionSuccessful();
		db.endTransaction();
		// } catch (SQLException e) {
		// errStr = e.getMessage();
		// }
		return errStr;
	}
	
	public void deleteAccountMails(int accountId)
	{
		SQLiteDatabase db = getWritableDatabase();
		String cmdTxt = "delete from mail where accountId=" + accountId;
		db.execSQL(cmdTxt);
	}

	public String updateAccount(Account a, String oldAccountName)
	{
		String errStr = "";
		try
		{
			// check whether the old account name existed.
			String queryTxt = "select count(id) from account where name=?";
			String[] args = { oldAccountName };
			SQLiteDatabase db = this.getReadableDatabase();
			Cursor cursor = db.rawQuery(queryTxt, args);
			if (cursor == null)
				return "Cursor is null";
			cursor.moveToFirst();
			if (cursor.getInt(0) < 1)
			{
				errStr = String
						.format(MyApp
										.instance()
										.getResources()
										.getString(	R.string.err_oldAccountNotExist),
								oldAccountName);
				cursor.close();
				return errStr;
			}
			// check the new account name has been existed.
			if (!a.name.equals(oldAccountName))
			{
				queryTxt = "select count(id) from account where name='"
							+ a.name + "'";
				cursor.close();
				cursor = db.rawQuery(queryTxt, null);
				cursor.moveToFirst();
				if (cursor.getInt(0) > 0)
				{
					errStr = MyApp.instance().getResources()
							.getString(R.string.err_accountExist);
					cursor.close();
					return errStr;
				}
			}
			String encryptPassword = SimpleCrypto.encrypt("&cloudyServices@cloudymail.mobi&", a.password);
			String CommandText = "update account set name='" + a.name
									+ "',loginName='" + a.loginName
									+ "',password='" + encryptPassword
									+ "',mailServer='" + a.mailServer
									+ "',mailPort=" + a.getMailPort()
									+ ",smtpServer='" + a.smtpServer
									+ "',smtpPort=" + a.smtpPort
									+ ",serverType='" + a.serverType
									+ "',useSSL=" + (a.useSSL ? 1 : 0)
									+ " where name='" + oldAccountName + "'";
			getWritableDatabase().execSQL(CommandText);
			cursor.close();
			return errStr;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			errStr = e.getMessage();
			return errStr;
		}
	}

	public String updateAccountPassword(String passwd, int id)
	{
		String encryptPassword = null;
	    try
		{
			encryptPassword = SimpleCrypto.encrypt("&cloudyServices@cloudymail.mobi&",passwd);
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String sql = "update account set password='" + encryptPassword + "' where id='"
						+ id + "'";
		getWritableDatabase().execSQL(sql);
		return "";
	}

	// //////////////////////////////////////
	public String loadAddressBook()
	{
		Cursor cursor = query("select * from addressbook", null);
		AddressBook.addressArry.clear();
		if (cursor == null)
			return "Cursor is null";
		int nameIdx = cursor.getColumnIndex("name");
		int emailIdx = cursor.getColumnIndex("email");
		try
		{
			if (cursor.moveToFirst())
			{
				do
				{
					String name = cursor.getString(nameIdx);
					String address = cursor.getString(emailIdx);
					AddressBook.addressArry.add(new EmailAddress(name, address));
				} while (cursor.moveToNext());
			}
		}
		finally
		{
			cursor.close();
		}
		return "";
	}

	public String addEmailAddress(EmailAddress ea)
	{
		String errStr = "";
		String queryTxt = "select count(name) from addressbook where email=?";
		String[] args = { ea.getAddress() };
		int count = executScalar(queryTxt, args);
		if (count > 0)
		{
			errStr = MyApp.instance().getResources()
					.getString(R.string.err_accountExist);
			return errStr;
		}

		String CommandText = "insert into addressbook (name, email) values(?,?)";
		String[] argsString = { ea.getName(), ea.getAddress() };
		execSQL(CommandText, argsString);
		return errStr;
	}

	public String deleteEmailAddress(EmailAddress ea)
	{
		String cmdTxt = "delete from addressbook where email='"
						+ ea.getAddress() + "'";
		execSQL(cmdTxt, new Object[] {});
		return "";
	}

	public String updateEmailAddress(EmailAddress ea, String oldAddress)
	{
		String errStr = "";
		// check whether the old address.
		String queryTxt = "select count(name) from addressbook where email=?";
		String[] args = { oldAddress };
		int count = executScalar(queryTxt, args);
		if (count < 1)
		{
			errStr = String.format(MyApp.instance().getResources()
					.getString(R.string.err_oldAccountNotExist), oldAddress);
			return errStr;
		}
		// check the new address has been existed.
		if (!ea.getAddress().equals(oldAddress))
		{
			queryTxt = "select count(name) from addressbook where email=?";
			String[] tmpArgs = { ea.getAddress() };
			count = executScalar(queryTxt, tmpArgs);
			if (count > 0)
			{
				errStr = MyApp.instance().getResources()
						.getString(R.string.err_accountExist);
				return errStr;
			}
		}
		String CommandText = "update addressbook set name=? ,email=? where email=?";
		String[] args1 = { ea.getName(), ea.getAddress(), oldAddress };
		execSQL(CommandText, args1);
		return errStr;
	}
	
	public void saveOutMail(OutMailInfo mailInfo)
	{
		ContentValues cv = new ContentValues();
		cv.put("accountId", mailInfo.getAccountId());
		cv.put("`to`", mailInfo.getTo());
		cv.put("cc", mailInfo.getCc());
		cv.put("bc", mailInfo.getBc());
		cv.put("body", mailInfo.getBody());
		cv.put("refUid", mailInfo.getUid());
		cv.put("subject", mailInfo.getSubject());
		cv.put("refBody", mailInfo.getRefBodyFlag());
		cv.put("mailType", mailInfo.getMailType());
		
		StringBuffer attacheStr = new StringBuffer();
		List<AttachmentInfo> attaches = mailInfo.getAttachments();
		for (AttachmentInfo info : attaches)
		{
			if(attacheStr.length() > 0)
				attacheStr.append(";");
			if(info.index < 0)//local attachment
				attacheStr.append(info.index+":"+info.fullFilePath);
			else
				attacheStr.append(info.index + ":" + info.fileName+":"+info.size);
		}
		
		cv.put("attachments",attacheStr.toString());
		cv.put("folder", mailInfo.getFolder());
		
		SQLiteDatabase db = getWritableDatabase();
		int mailId = mailInfo.getUidx();
		if (mailId >= 0)//if it's an existing mail.
		{
			String[] args = { mailId+"" };
			db.update("outBox", cv, "ID=?", args);
		}
		else
		{
			mailInfo.setUidx((int)db.insert("outBox", null, cv));
		}
		}
/*	
	public int saveOutMail(int mailId, int accountId, DataPacket dp,String folderName)
	{
		ContentValues cv = new ContentValues();
		cv.put("accountId", accountId);
		cv.put("`to`", dp.toList);
		cv.put("cc", dp.ccList);
		cv.put("body", dp.bodyText);
		cv.put("refUid", dp.refMailId);
		cv.put("subject", dp.subject);
		
		StringBuffer attaches = new StringBuffer();
		for (AttachmentInfo info : dp.attachments)
		{
			if(attaches.length() > 0)
				attaches.append(";");
			if(info.index < 0)//local attachment
				attaches.append(info.index+":"+info.fullFilePath);
			else
				attaches.append(info.index + ":" + info.fileName+":"+info.size);
		}
		
		cv.put("attachments",attaches.toString());
		cv.put("folder", folderName);
		
		SQLiteDatabase db = getWritableDatabase();
		if (mailId >= 0)//if it's an existing mail.
		{
			String[] args = { mailId+"" };
			int updateCount = db.update("outBox", cv, "ID=?", args);
			if(updateCount > 0)
				return mailId;
		}
		return (int)db.insert("outBox", null, cv);
	}*/

	// ////////////////////////////////////////////////
	//if mailId is not valid, then update the mail in outBox
/*	public String saveOutMail(int accountId, DataPacket dp,String folderName)
	{
		// 婵烇絽娴勯幏鐑芥晸閺傘倖瀚归柨鐔诲Г椤愪粙鏁撻挊澶婎暢闁跨喓鏅幉鎾晸閺傘倖瀚??
		// <attaches> = <attach> [; <attaches> ]
		// <attach> = <RemoteAttach> | <LocalAttach>
		// <RemoteAttach> = <index>:<Positive number>:<size>
		// <LocalAttach> = <index>:<full_file_path>
		String comTxt;
		comTxt = "insert into outBox (accountId,`to`,cc,"
					+ "body,refUid,subject,attachments,folder) "
					+ "values(?,?,?,?,?,?,?,?)";
		StringBuffer attaches = new StringBuffer();
		for (AttachmentInfo info : dp.attachments)
		{
			if(attaches.length() > 0)
				attaches.append(";");
			if(info.index < 0)//local attachment
				attaches.append(info.index+":"+info.fullFilePath);
			else
				attaches.append(info.index + ":" + info.fileName+":"+info.size);
		}

		String[] args = { accountId + "", dp.toList, dp.ccList, dp.bodyText,
							dp.refMailId, dp.subject, attaches.toString(),
							folderName};

		execSQL(comTxt, args);
		return "";
	}*/

	// ////////////////////////////////////////
	public int getCurrentAccountId()
	{
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.rawQuery("select accountId from outBox", null);
		int curId = -1;
		try
		{
			if (cursor.moveToFirst())
				curId = cursor.getInt(0);
		}
		finally
		{
			cursor.close();
		}
		return curId;
	}

	/***************************************/
/*	public Cursor fetchData(String folderName)
	{
		String str_sql = "select count(folder) from mail where folder=";
		if (folderName.equals(FolderManager.FOLDER_INBOX))
		{
			str_sql += "`FolderManager.FOLDER_INBOX`";
		}
		else if (folderName.equals(FolderManager.FOLDER_DRAFT))
		{
			str_sql += "`FolderManager.FOLDER_DRAFT`";
		}
		else if (folderName.equals(FolderManager.FOLDER_SEND))
		{
			str_sql += "`FolderManager.FOLDER_SENT`";
		}
        
		Cursor folder_Cursor = query(str_sql, null);

		return folder_Cursor;

	}*/

	public Cursor getOutMails(int accountId, int length, int offset,
			int orderType, String folderName)
	{
		return getOutMails(accountId,length,offset,orderType,folderName,null);
	}
	
	public Cursor getOutMails(int accountId, int length, int offset,
			int orderType, String folderName,String searchWord)
	{
		String sql = "select * from outBox"+
					getMailWhereSql(accountId, folderName, 
					                new int[]{MailStatus.MAIL_NEW, MailStatus.MAIL_READED},
					                false, searchWord);
		sql += getOrderSql(orderType);
		sql += " limit " + length + " offset " + offset;
		return query(sql, null);
	}
	
	public Cursor getDelOutMails(int accountId, int length, int offset,String searchWord)
	{
		String sql = "select * from outBox"+
					getMailWhereSql(accountId, "", 
					                new int[]{MailStatus.MAIL_LOCAL_DELETED},
					                false, searchWord);
		sql += " limit " + length + " offset " + offset;
		return query(sql, null);
	}

	public Cursor getInMails(int accountId, int length, int offset,
			int orderType, String folderName,
			boolean excludeHasMore,String searchWord)
	{
		String sql = "select * from mail"+
					getMailWhereSql(accountId, folderName, 
					                new int[]{MailStatus.MAIL_NEW, MailStatus.MAIL_READED},
					                excludeHasMore, searchWord);
		sql += getOrderSql(orderType);
		sql += " limit " + length + " offset " + offset;
		Log.d(LOGTAG,"getInMails"+ sql);
		return query(sql, null);
	}
	
	public Cursor getDelInMails(int accountId, int length, int offset,String searchWord)
	{
		String sql = "select * from mail"+
					getMailWhereSql(accountId, "", 
					                new int[]{MailStatus.MAIL_LOCAL_DELETED},
					                true, searchWord);
		sql += " limit " + length + " offset " + offset;
		return query(sql, null);
	}
	
	private String getOrderSql(int orderType)
	{
		String sqlOrder = " order by ";
		if (orderType == ORDER_BY_DATE)
		{
			sqlOrder += "date desc";
		}
		else if (orderType == ORDER_BY_SUBJECT)
		{
			sqlOrder += "subject asc, date desc";
		}
		else if (orderType == ORDER_BY_FROM)
		{
			sqlOrder += "`from` asc, date desc";
		}
		else if (orderType == ORDER_BY_READEDSTATE)
		{
			sqlOrder += "state asc, date desc";
		}
		else if (orderType == ORDER_BY_ASTERISK)
		{
			sqlOrder += "asterisk desc, date desc";
		}
		
		return sqlOrder;
	}
	private String getGroupOrderSql(int orderType)
	{
		String sqlOrder = " order by ";
		if (orderType == ORDER_BY_DATE)
		{
			sqlOrder += "date desc";
		}
		else if (orderType == ORDER_BY_SUBJECT)
		{
			sqlOrder += "suffix asc, date desc";
		}
		else if (orderType == ORDER_BY_FROM)
		{
			sqlOrder += "`from` asc, date desc";
		}
		else if (orderType == ORDER_BY_READEDSTATE)
		{
			sqlOrder += " date desc";
		}
		else if (orderType == ORDER_BY_ASTERISK)
		{
			sqlOrder += " date desc";
		}
		
		return sqlOrder;
	}
	public String updateOutMailStatus(Vector<Integer> accountIds, Vector<Integer> ids, int state)
	{
		SQLiteDatabase db = getWritableDatabase();
		String cmdTxt;
		if(state == MailStatus.MAIL_DELETE_FOREVER)
			cmdTxt = "delete from outBox where ID=? and accountId=?";
		else
			cmdTxt = "update outBox set state=" + state
						+ " where ID=? and accountId=?";

		db.beginTransaction();
		for (int i = 0; i < ids.size(); i++)// too slow.
		{
			int id = ids.get(i);
			if (id < 0)
				continue;
			String[] args = { id+"",accountIds.get(i)+""};
			db.execSQL(cmdTxt, args);
		}
		db.setTransactionSuccessful();
		db.endTransaction();
		return "";
	}
	

	public void updateMailBody(InMailInfo mailInfo)
	{
		SQLiteDatabase db = getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put("body", mailInfo.getBody());
		String[] args = { mailInfo.getUid(),mailInfo.getAccountId()+"",mailInfo.getFolder()};
		db.update("mail", cv, "uid=? and accountId=? and folder=?",args);
		/*String cmdTxt = "update mail set body='"+mailInfo.getBody()
						+"' where uid=? and accountId=?";
		String[] args = { mailInfo.getUid(),mailInfo.getAccountId()+""};
		db.execSQL(cmdTxt, args);*/
	}
	public String updateInMailStatus(Vector<Integer> accountIds, Vector<Integer> uidxs, 
								int state,Vector<String> folders)
	{
		SQLiteDatabase db = getWritableDatabase();
		String cmdTxt;
//		if(state == MailStatus.MAIL_DELETE_FOREVER)
//			cmdTxt = "delete from mail where uidx=? and accountId=?";
//		else
//			cmdTxt = "update mail set state=" + state
//						+ " where uidx=? and accountId=?";
		cmdTxt = "update mail set state=" + state + " where uidx=? and accountId=? and folder=?";
		db.beginTransaction();
		for (int i = 0; i < uidxs.size(); i++)// too slow.
		{
			int uidx = uidxs.get(i);
			if (uidx < 0)
				continue;
			String[] args = { uidxs.get(i)+"", accountIds.get(i)+"",folders.get(i)};
			db.execSQL(cmdTxt, args);
		}
		db.setTransactionSuccessful();
		db.endTransaction();
		return "";
	}
	
	private String getMailWhereSql(int accountId, 
			String folderName, 
			int[] states,
			boolean excludeHasMore, 
			String searchWord)
	{
		StringBuffer sqlBuf = new StringBuffer();//("select count(*) from mail");
		if(!folderName.equals(""))
			sqlBuf.append(" folder='"+folderName+"'");
//		if(!folderName.equals(FolderNames.FOLDER_DELETE)&&state != MailStatus.MAIL_INVALID_STATE)
//		{
//				if(sqlBuf.length() > 0)
//					sqlBuf.append(" and");
//				sqlBuf.append(" state").append(stateExcludeFlag?"!=":"=");
//				sqlBuf.append(state+"");
//		}
		
		if(states != null && states.length > 0)
		{
			if(sqlBuf.length() > 0)
				sqlBuf.append(" and");
			sqlBuf.append(" state in (");
			for(int i=0;i<states.length;i++)
			{
				if(i>0)
					sqlBuf.append(",");
				sqlBuf.append(states[i]);
			}
			if(!excludeHasMore)
			{
				for(int i=0;i<states.length;i++)
				{
					sqlBuf.append(",");
					sqlBuf.append(states[i] | MailStatus.FLAG_HAS_MORE_PLACEHOLD);
				}
			}
			sqlBuf.append(") ");
			
		}
		
		if(accountId > 0)
		{
			if(sqlBuf.length() > 0)
				sqlBuf.append(" and");
			sqlBuf.append(" accountId="+accountId);
		}

		if(searchWord!=null && !searchWord.equals(""))
        {
			if (sqlBuf.length() > 0)
				sqlBuf.append(" and");
//        	sqlBuf.append(" subject like '%"+ searchWord +"%'");	dd
//			(subject like '%"+ floatingSearch +"%' or `from` like '%"+floatingSearch+"%')
        	sqlBuf.append(" (subject like '%"+ searchWord +"%' or `from` like '%"+searchWord+"%')");
        }
		if(sqlBuf.length() > 0)
			return " where "+sqlBuf.toString();
		else
			return "";
	}
	
	public int getInMailCount(int accountId, String folderName, int[] states,
			 boolean excludeHasMore)
	{
		return getInMailCount(accountId,folderName,states,
		                       excludeHasMore,null);
	}
	
	public int getInMailCount(int accountId, String folderName, int[] states,
			boolean excludeHasMore, String searchWord)
	{
		String sql = "select count(*) from mail" 
			+ getMailWhereSql(accountId,folderName,states,
			                  excludeHasMore,searchWord);
		return executScalar(sql, null);
	}
	
	public int getOutMailCount(int accountId, String folderName, int[] states)
	{
		return getOutMailCount(accountId,folderName,states,null);
	}
	
	public int getOutMailCount(int accountId, String folderName, int[] states,
			String searchWord)
	{
		String sql = "select count(*) from outBox"
			+ getMailWhereSql(accountId,folderName,states,
			                  false,searchWord);
		return executScalar(sql, null);
	}

	public void setMailAttachment(String uid,String folderName, int attachFlag)
	{
		String cmdTxt = "update mail set hasAttach="+attachFlag+" where uid=? and folder='"+folderName+"'";
		String[] args = { uid };
		execSQL(cmdTxt, args);
	}
//	public String updateAccountPassword(String passwd, int id)
//	{
//		String sql = "update account set password='" + passwd + "' where id='"
//						+ id + "'";
//		getWritableDatabase().execSQL(sql);
//		return "";
//	}
//	public String updateInMailStatus(int[] accountIds, int[] uidxs, 
//			int state)
//{
//SQLiteDatabase db = getWritableDatabase();
//String cmdTxt;
//if(state == MailStatus.MAIL_DELETE_FOREVER)
//cmdTxt = "delete from mail where uidx=? and accountId=?";
//else
//cmdTxt = "update mail set state=" + state
//	+ " where uid=? and accountId=?";
//
//db.beginTransaction();
//for (int i = 0; i < uidxs.length; i++)// too slow.
//{
//int uidx = uidxs[i];
//if (uidx < 0)
//continue;
//String[] args = { uidxs[i]+"", accountIds[i]+""};
//db.execSQL(cmdTxt, args);
//}
//db.setTransactionSuccessful();
//db.endTransaction();
//return "";
//}
	// "update mail set  subject=?, date=?, 'from'=?, 'to'=?, cc=?, uidx=?, state=? folder="+MailInfo.FOLDER_INBOX+"where uid=? and accountId="
	// + accountId;
    public void updateInAsteriskstatu(int accountId,int asteriskValue,Integer uidx, String folderName)
    {
    	SQLiteDatabase db=getWritableDatabase();
    
    	String comTxt="update mail set asterisk='"+asteriskValue+"' where uidx='"+uidx+"' and folder='"+folderName +"' and accountId='"+accountId+"'" ;
//    	String comTxt="update mail set asterisk=1 where uidx=85 and folder=inbox and accountId=3 ;
//    	String comTxt="update mail set asterisk=?,"
    	db.execSQL(comTxt);
    	
    }
    public void updateOutAsteriskstatu(int accountId,int asteriskValue,Integer uidx)
    {
    	SQLiteDatabase db=getWritableDatabase();
    	String comTxt="update outBox set asterisk='"+asteriskValue+"' where ID='"+uidx+"' and accountId='"+accountId+"'" ;
//    	Integer [] args={asteriskValue};
    	db.execSQL(comTxt);
    }
//    public int getCurrentAccountId()
//	{
//		SQLiteDatabase db = getReadableDatabase();
//		Cursor cursor = db.rawQuery("select accountId from outBox", null);
//		int curId = -1;
//		if (cursor.moveToFirst())
//			curId = cursor.getInt(0);
//		cursor.close();
//		return curId;
//	}
    public int getAsterisk()
    {
       SQLiteDatabase db=getReadableDatabase();
       Cursor cursor=db.rawQuery("select asterisk from mail", null);
       int curId=-1;
       if(cursor.moveToFirst())
    	   curId=cursor.getInt(0);
    	return curId;

    }
	public void  insertMailsToDb(MailInfo info, int accountId)
	{
		try
		{
			String folderName=info.getFolder();
			String cmd2 = "insert into mail (uid, subject, date, 'from', 'to', cc, uidx, state, hasAttach,accountId,folder,groupId) values(?,?,?,?,?,?,?,?,?, "
							+ accountId
							+ ","
							+ "'"
							+ folderName + "',?)";
			
			String uid =info.getUid();
			int ct = executScalar(	"select count(*) from mail where uid=? and accountId=? and folder='"+folderName+"'",
									new String[] { uid, accountId + "" });

			int state = info.getState();
			int attachementFlag =info.getAttachmentFlag();
			long groupId = updateMailGroup(info);
			info.setGroupId(groupId);
			if (ct == 0)
			{
				String[] args = { 
									uid,
									info.getSubject(),
									Utils.netDateFormater.format(info.getDate()),
									
									info.getFrom(),
									info.getTo(),
									info.getCc(),
									info.getUidx()+"", 
									state + "",
									attachementFlag+"",
				                    String.valueOf(groupId)};
				execSQL(cmd2, args);
			}
			else
			{
				String cmd3="select state from mail where uid=? and accountId= " + accountId+" and folder='"+ folderName + "'";
				Cursor cursor=query(cmd3, new String[]{uid});
				try
				{
					if(cursor.moveToFirst())
					{
						int currentStateInDB=cursor.getInt(0);
						if(state==MailStatus.MAIL_DELETED&&(currentStateInDB & MailStatus.FLAG_HAS_MORE_PLACEHOLD)!=0)
							return;
					}
				}
				finally
				{
					cursor.close();
				}
				cmd3 = "update mail set  subject=?, date=?, 'from'=?, 'to'=?, cc=?, uidx=?, state=?,hasAttach=?,groupId=?,folder='"
						+ folderName
						+ "' where uid=? and accountId= " + accountId+" and folder='"+ folderName + "'";
				String[] args = { info.getSubject(),
									Utils.netDateFormater.format(info.getDate()),
									info.getFrom(),
									info.getTo(),
									info.getCc(),
									info.getUidx()+"", 
									state + "",
									attachementFlag+"",
									groupId+"",
									uid };
				execSQL(cmd3, args);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public int insertMailsToDb(Element xmlReader, int accountId)
	{
		int mailCount = 0;
		try
		{
			NodeList mailElements = xmlReader.getElementsByTagName("mail");
			for (int i = 0; i < mailElements.getLength(); i++)
			{
				Element xr = (Element) mailElements.item(i);
				if(Utils.isEmpty(xr.getAttribute("date")))
					break;
				MailInfo mailInfo=new InMailInfo(accountId, xr);
				
				insertMailsToDb(mailInfo, accountId);
				if(mailInfo.getState()==MailStatus.MAIL_NEW)
				{
					MyApp.instance().addNewMailToMap(accountId, mailInfo);
					mailCount++;
				}
					
			}
			
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		return mailCount;
	}
	/*
	 * 邮件组更新的规则：
	 * 1. 对于普通邮件，首先找到这个邮件对应的分组，如果不存在就新建分组。返回MailGroup的ID.这个ID被记录到mail表中。
	 * 2. 对于HAS_MORE_PLACEHOLD，一定要在MailGroup中新建一个分组。
	 * 3. 对于普通邮件，如果找到这个邮件相同UID, account id的HAS_MORE_PLACEHOLD，也就是说，这个邮件在上次接收时被当作占位符，
	 *    本次接收中才被更新成为普通邮件。那么，首先删除之前的占位符，然后进行步骤1.并将这个邮件对应的group id更新。
	 */
	private long updateMailGroup(MailInfo mailInfo) {
		SQLiteDatabase db = getWritableDatabase();
		if((mailInfo.getState() & MailStatus.FLAG_HAS_MORE_PLACEHOLD) != 0)
		{//rule 2
		    ContentValues initialValues = new ContentValues();
		    initialValues.put("accountId", mailInfo.getAccountId());
		    initialValues.put("suffix", mailInfo.getSubject());
		    initialValues.put("'from'", mailInfo.getFrom());
		    initialValues.put("'date'", Utils.netDateFormater.format(mailInfo.getDate()));
		    initialValues.put("'accountId'", mailInfo.getAccountId());
		    initialValues.put("'folderName'", mailInfo.getFolder());
		    initialValues.put("state", mailInfo.getState());
		    return db.insert("mailGroup", null, initialValues);

		}
		
		//rule 3
		Cursor cursor = _instance.query("select groupId from mail where uid=? and accountid=? and state >=?", new String[] {mailInfo.getUid(), mailInfo.getAccountId()+"", MailStatus.FLAG_HAS_MORE_PLACEHOLD+""});
		try
		{
			if(cursor.moveToFirst())
			{
				long groupid = cursor.getInt(0);
				_instance.execSQL("delete from mailGroup where id=?", new Object[] {groupid});
			}
		}
		finally
		{
			cursor.close();
		}
		String suffixStr = findSuffix(mailInfo.getSubject());
		
		String cmd="select id from mailGroup where suffix=? and accountId=? and folderName=?";
		String [] arg={suffixStr, mailInfo.getAccountId()+"", mailInfo.getFolder()};
		long groupId = _instance.executScalar(cmd, arg);
		
		if(groupId==0)
		{
		    ContentValues initialValues = new ContentValues();
		    initialValues.put("accountId", mailInfo.getAccountId());
		    initialValues.put("suffix", suffixStr);
		    initialValues.put("'from'", mailInfo.getFrom());
		    initialValues.put("'date'", Utils.netDateFormater.format(mailInfo.getDate()));
		    initialValues.put("'accountId'", mailInfo.getAccountId());
		    initialValues.put("'folderName'", mailInfo.getFolder());
		    initialValues.put("state", mailInfo.getState());
		    groupId = db.insert("mailGroup", null, initialValues);
		}
		 else
		 {
			
			String updateGroupCmd="update mailGroup set 'from'=?,date=? where date<? and id=?"; 
			String dateStr = Utils.netDateFormater.format(mailInfo.getDate());
		    String []args={ 
		            		mailInfo.getFrom(),
		            		dateStr,
		            		dateStr,
							groupId+""
		                  };
		    db.execSQL(updateGroupCmd, args);
		 }
		return groupId;
	}

	private  String findSuffix(String subject) 
	{
		String[] prefixes = new String[] {"Re:","Fwd:","回复:","转发:","答复:",
		                                  "Re：","Fwd：","回复：","转发：","答复："};
		if(subject!=null)
		{
			subject = subject.trim();
			for(String prefix:prefixes)
			{
				if(subject.startsWith(prefix))
					return findSuffix(subject.substring(prefix.length()));
			}
		}
		return subject;
	}
	public ArrayList<FolderInfo> getFolders(int accountId)
	{
		String sqlString="select foldername,displayname from folders where accountid="+accountId;
		Cursor cursor=query(sqlString, null);
		ArrayList<FolderInfo> folders=new ArrayList<FolderInfo>();
		try
		{
			while(cursor.moveToNext())
			{	
				FolderInfo info=new FolderInfo(accountId,cursor.getString(0), cursor.getString(1));
				folders.add(info);
			}
		}
		finally
		{
			cursor.close();
		}
		return folders;
	}
	/**
	 * @param xmlReader
	 * @param id
	 * 
	 */

	public void insertFoldersToDb(Element xmlReader, int accountId)
	{
		execSQL("delete from folders where accountid=?",new String[]{accountId+""});
		String insert="insert into folders values("+accountId+",?,?)";
		NodeList folderElements=xmlReader.getElementsByTagName("folder");
		for (int i = 0; i < folderElements.getLength(); i++)
		{
			Element folder=(Element) folderElements.item(i);
			String folderName=folder.getAttribute("foldername");
			//System.out.println("accountid=="+accountId+"foldername=="+folderName);
			int c=executScalar("select count(*) from folders where accountid=? and foldername=?", new String[]{accountId+"",folderName});
			if(c==0)
			{
				String displayName=folder.getAttribute("displayname");
				execSQL(insert, new String[]{folderName,displayName});		
			}
		}
		
	}
	static SimpleDateFormat dateFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
	private static File dbFileObj;

//	public static String date2str(java.util.Date d)
//	{
//		return dateFormat.format(d);
//	}

//	public static Date str2date(String s) throws ParseException
//	{
//		return dateFormat.parse(s);
//	}
//	public int getMailCount()throws SQLException{
//		if(currentAccount!=null){
//			
//		}
//	}
	private int upgradeToV12(int fromVersion)
	{
		if(fromVersion >= DATABASE_VERSION_V12)
			return fromVersion;
		SQLiteDatabase db = this.getWritableDatabase();
		try
		{
			db.execSQL("drop table attachmentInfo");
		}
		catch (Exception e)
		{
			// TODO: handle exception
		}
		try {
			
			db.execSQL("CREATE TABLE [attachmentInfo] (" + 
				"  [mailUid] VARCHAR NOT NULL, " + 
				"  [mailFolder] VARCHAR NOT NULL, " +
				"  [attachIdx] INTEGER NOT NULL, " + 
				"  [fileName] VARCHAR, " + 
				"  [size] VARCHAR, " + 
				"  [fileType] VARCHAR, " + 
				"  [accountId] INTEGER NOT NULL," +
				"  [canPreview] tinyint(1) DEFAULT '0'," +
				"  [filePath] VARCHAR DEFAULT NULL," +
				"  PRIMARY KEY ([mailUid], [mailFolder], [attachIdx], [accountId]))",new Object[0] );
		//		"  CONSTRAINT [sqlite_autoindex_attachmentInfo_1] PRIMARY KEY ([uidx], [attachIdx]))", new Object[0] );
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		try {
			db.execSQL("CREATE TABLE \"folders\"" + 
				"(accountid integer not null,\n" + 
				"foldername varchar(50) not null,\n" + 
				" displayname varchar(50) not null,\n" + 
				"primary key(accountid,foldername)\n" + 
				")", new Object[0]);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		try
		{
			db.execSQL("CREATE TABLE \"mail2\"(" +
					"uid varchar(70) NOT NULL,\n"+
					"subject varchar(512),\n"+
					"date DATETIME DEFAULT (CURRENT_TIMESTAMP),\n"+
					"\"from\" varchar(100),\n"+
					"state INTEGER DEFAULT (0),\n"+
					"hasAttach INTEGER DEFAULT (0),\n"+
					"accountId INTEGER DEFAULT (0),\n"+
					"uidx INTEGER NOT NULL,\n"+
					"\"to\" varchar(1024),\n"+
					"cc varchar(1024),\n"+
					"folder VARCHAR(20) NOT NULL DEFAULT ('INBOX'),\n"+
					"body TEXT,\n"+
					"asterisk INTEGER DEFAULT (0),\n"+
					"CONSTRAINT sqlite_autoindex_mail_1 PRIMARY KEY (uid,accountId,folder)\n" +
					");");
					db.execSQL("insert into mail2 select * from mail");
					db.execSQL("drop table mail");
					db.execSQL("alter table mail2 rename to mail");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		db.execSQL("update mail set uidx=0 where state="+MailStatus.MAIL_DELETED);
		db.execSQL("update outBox set state="+MailStatus.MAIL_LOCAL_DELETED+" where state="+MailStatus.MAIL_DELETED);
		db.execSQL("insert or replace into sysconfig (name, value) values('clientVersion', " + DATABASE_VERSION_V12+")", new Object[0]); //select value from sysconfig where name='clientVersion'
		return DATABASE_VERSION_V12;
		//		int currentVersion = db.getVersion();
//		if(currentVersion>=12)
//			return; //its already higher than 12;
//		Utils.ASSERT(currentVersion == 3);//DB for cloudymail 1.1.0 and 1.1.1 is 3

	}

	private int upgradeToV13(int fromVersion)
	{
		if(fromVersion >= DATABASE_VERSION_V13)
			return fromVersion;
		//1. call upgradeToV12 first, if db is too earlier
		//2. upgrade data tables
		//3. clear user mail cache. since MailRender.jsp is changed for calendar support
		fromVersion = upgradeToV12(fromVersion);
		SQLiteDatabase db = this.getWritableDatabase();
		try
		{
			db.execSQL("CREATE TABLE [mailGroup] (\n" + 
					"  [id] INTEGER PRIMARY KEY AUTOINCREMENT,\n" + 
					"  [accountId] INTEGER NOT NULL,\n" + 
					"  [suffix] VARCHAR NOT NULL,\n" + 
					"  [from] varchar(100),\n" + 
					"  [date] DATETIME DEFAULT (CURRENT_TIMESTAMP),\n" + 
					"  [folderName] VARCHAR(20) NOT NULL DEFAULT ('INBOX'),\n" + 
					"  [count] INTEGER DEFAULT (0));");
					
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}	
		db.execSQL("alter table mail add column [groupId] INTEGER");
		
		String sql = "select * from mail";
	
		Cursor cursor = query(sql, null);
		try
		{
			if (cursor.moveToFirst())
			{
				do
				{

					MailInfo info = new InMailInfo(cursor);
					long groupId=updateMailGroup(info);
					db.execSQL("update mail set groupId='"+groupId+"' where accountId="+info.getAccountId()+" and uid="+info.getUid()+" and folder='"+info.getFolder()+"'");
				} while (cursor.moveToNext());
			}
			else
				Log.d(LOGTAG,"There's no mail to upgrade");
		}
		finally
		{
			cursor.close();
		}
		
		updateMailGroupState();
		db.execSQL("insert or replace into sysconfig (name, value) values('clientVersion', " + DATABASE_VERSION_V13+")", new Object[0]); //select value from sysconfig where name='clientVersion'
		return DATABASE_VERSION_V13;
	}

	
	private int upgradeToV14(int fromVersion)
	{
		if(fromVersion >= DATABASE_VERSION_V14)
			return fromVersion;
		//1. call upgradeToV12 first, if db is too earlier
		//2. upgrade data tables
		//3. clear user mail cache. since MailRender.jsp is changed for calendar support
		fromVersion = upgradeToV13(fromVersion);
		SQLiteDatabase db = this.getWritableDatabase();
		
		db.execSQL("alter table attachmentInfo add column [totalPageCount] INT");
		db.execSQL("alter table attachPreviewCache add column [folder] varchar(20) not null DEFAULT ('INBOX')");
		
		db.execSQL("insert or replace into sysconfig (name, value) values('clientVersion', " + DATABASE_VERSION_V14+")", new Object[0]); //select value from sysconfig where name='clientVersion'
		return DATABASE_VERSION_V14;
	}
	
	private int upgradeToV142(int fromVersion)
	{
		int newVer = DATABASE_VERSION_V142;
		if(fromVersion >= newVer)
			return fromVersion;
		
		fromVersion = upgradeToV14(fromVersion);
		SQLiteDatabase db = this.getWritableDatabase();
		
		db.execSQL("drop table attachPreviewCache");
		db.execSQL(" CREATE TABLE [attachPreviewCache] ("
				+ "[accountId] INTEGER NOT NULL,"
				+ "[uidx] INTEGER NOT NULL,"
				+ "[attachIdx] INTEGER NOT NULL,"
				+ "[attachPage] INTEGER NOT NULL,"
				+ "[body] TEXT,"
				+ "[folder] VARCHAR(20) NOT NULL DEFAULT ('INBOX'),"
				+ "CONSTRAINT PK PRIMARY KEY (accountId, uidx, attachIdx, attachPage, folder) )");
		
		db.execSQL("insert or replace into sysconfig (name, value) values('clientVersion', " + newVer+")", new Object[0]); //select value from sysconfig where name='clientVersion'
		db.execSQL("alter table account add column  [sessionID] VARCHAR(128)");
		return newVer;
	}
	
	
	//version 1.5, support exchange, fix performance problem on large count of mails
	private int upgradeToV15(int fromVersion)
	{
		int newVer = DATABASE_VERSION_V15;
		if(fromVersion >= newVer)
			return fromVersion;
		
		fromVersion = upgradeToV142(fromVersion);
		SQLiteDatabase db = this.getWritableDatabase();
		
		
		db.execSQL("alter table mailgroup add column  [state] INTEGER DEFAULT (0)");
		db.execSQL("update mailgroup set state=(select state from mail where groupid=mailgroup.id limit 1)");
		db.execSQL("insert or replace into sysconfig (name, value) values('clientVersion', " + newVer+")", new Object[0]); //select value from sysconfig where name='clientVersion'
		return newVer;
	}
	
	/**
	 *  insert attachment into attachmentInfo table
	 */
	public void insertAttachInfo(AttachmentInfo attaInfo, int accountId) 
	{
		String selectSql="select * from  attachmentInfo where mailUid='"+attaInfo.getMailUid()+"' and mailFolder='"+attaInfo.getFolder()+"' and attachIdx="+attaInfo.getAttachIndx()+" and accountId="+attaInfo.getAccountId()+"";		
		Cursor cursor = getReadableDatabase().rawQuery(selectSql,null);
		try
		{

				if(cursor ==null||!cursor.moveToFirst())
			    {
					String insertCommand="replace into attachmentInfo (mailUid,mailFolder,attachIdx,fileName,size,fileType, accountId,canPreview,filePath,totalPageCount) values(?,?,?,?,?,?,?,?,?,?)"; 		
			      	this.getWritableDatabase().execSQL(insertCommand, 
			      	                                   new Object[]{attaInfo.getMailUid(),attaInfo.getMailInfo().getFolder(), attaInfo.index,
			      	                                              attaInfo.fileName,attaInfo.size, attaInfo.fileType,
			      	                                            accountId,attaInfo.canPreview?1:0,attaInfo.fullFilePath,attaInfo.getTotalPageCount()});
			    }
		}
		finally
		{
			cursor.close();
		}
		
	}
	public int getAttachmentPageCount(AttachmentInfo attach)
	{
		SQLiteDatabase db = getReadableDatabase();
		String sqlString="select totalPageCount from attachmentInfo where mailUid='"+attach.getMailUid()+"' and mailFolder='"+attach.getFolder()+"' and attachIdx="+attach.getAttachIndx()+" and accountId="+attach.getAccountId()+"";
//		String[] args = { attach.getMailUid()+"",attach.getFolder()+"",attach.getAttachIndx()+"",attach.getAccountId()+""}; 
//		Cursor cursor = db.rawQuery("select totalPageCount from attachmentInfo " +
//				                    "where mailUid=? and mailFolder=? and attachIdx=? and accountId=?",args); 
		Cursor cursor = db.rawQuery(sqlString,null);
		int totalPageCount=0;
		try
		{

				if(cursor !=null&&cursor.moveToFirst())
			    {
					totalPageCount=cursor.getInt(0);
//					int colIndex = cursor.getColumnIndex("totalPageCount");
//					totalPageCount = cursor.getInt(colIndex);
			    }
		}
		finally
		{
			cursor.close();
		}
		//db.close();  
		return totalPageCount;
		
	}
	public void updateAttachTotalPageCount(AttachmentInfo attach)
	{
		String updateCommand="update attachmentInfo set totalPageCount="+attach.getTotalPageCount()+" where mailUid='"+attach.getMailUid()+"' and mailFolder='"+attach.getFolder()+"' and attachIdx="+attach.getAttachIndx()+" and accountId="+attach.getAccountId()+"";		
      	this.getWritableDatabase().execSQL(updateCommand);
	}
	//update file path on sd card after download attachment finished.
	public boolean updateAttachFilePath(int accountId,String mailUid,String mailFolder, int attachIdx,String filePath)
	{
		String upCmd = "update attachmentInfo set filePath='"+filePath+
						"' where accountId='"+accountId+"' and mailUid='"+mailUid+
						"' and attachIdx='"+attachIdx+"'"+" and mailFolder='"+mailFolder+"'";
		try
		{
			getWritableDatabase().execSQL(upCmd);
			return true;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			//do nothing.
		}
		return false;
	}
	
	public File getAttachFilePath(int accountId, String mailUid, String mailFolder, int attachIdx)
	{
		String findSql= "select filePath from attachmentInfo" + 
						" where attachmentInfo.mailUid='" + mailUid + 
						"' and attachmentInfo.accountId='" + accountId +
						"' and attachmentInfo.attachIdx='" + attachIdx + "'";
		
		Cursor rawQuery = null;
		try
		{
			rawQuery = this.getReadableDatabase().rawQuery(findSql, null);
			if(rawQuery!=null)
			{
				int filePathIndex = rawQuery.getColumnIndex("filePath");
			    if(rawQuery.moveToFirst()) 
			    {
			    	String filePath = rawQuery.getString(filePathIndex);
			    	if(filePath == null)
			    	{
			    		return null;
			    	}
			    	return new File(filePath);
			    	
			    }
		     }
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
		}
		return null;
	}

	public List<AttachmentInfo> getAttachmentInfo(String fileType) 
	{
		Hashtable<String, MailInfo> cache = new Hashtable<String, MailInfo> (137);
		
		List<AttachmentInfo> attachments=new ArrayList<AttachmentInfo>();

       String queryCmd="select mail.date, mail.folder, attachmentInfo.mailUid, "+
    	   "attachmentInfo.attachIdx, attachmentInfo.size,attachmentInfo.fileName, "+
    	   "attachmentInfo.fileType, attachmentInfo.accountId,attachmentInfo.canPreview,"+
    	   "attachmentInfo.filePath from attachmentInfo,mail "+
    	   "where attachmentInfo.mailUid=mail.uid and mail.accountId=attachmentInfo.accountId and mail.folder=attachmentInfo.mailFolder and mail.state in ("+MailStatus.MAIL_READED +"," + MailStatus.MAIL_NEW+")";
       if(!Utils.isEmpty(fileType))
    	   queryCmd += " and fileType='"+fileType+"'";
       queryCmd += " order by mail.date DESC";
//    	   		"attachName like '%."+fileType +"'";
       
       
       Cursor rawQuery = null;
       try
       {
    	   rawQuery = this.getReadableDatabase().rawQuery(queryCmd, null);
	       if(rawQuery!=null)
	       {
				int uidIndex = rawQuery.getColumnIndex("mailUid");
				int folderIndex=rawQuery.getColumnIndex("folder");
				int attachidx = rawQuery.getColumnIndex("attachIdx");
				int attSizeIndex = rawQuery.getColumnIndex("size");
				int attNameIndex = rawQuery.getColumnIndex("fileName");
				int attFileType=rawQuery.getColumnIndex("fileType");
				int accountIdIndex = rawQuery.getColumnIndex("accountId");
				int previewIndex = rawQuery.getColumnIndex("canPreview");
				int filePathIndex = rawQuery.getColumnIndex("filePath");
	            if(rawQuery.moveToFirst())
		        {
		    	   do
		    	   {
		    		   
		    		   int accountId=rawQuery.getInt(accountIdIndex);
		    		   String folderName=rawQuery.getString(folderIndex);
		    		   String uid = rawQuery.getString(uidIndex);
		    		   String key = uid+accountId;
		    		   MailInfo info = cache.get(key);
		    		   if(info == null)
		    		   {
		    			   Cursor query = null;
		    			   try
		    			   {
		    				   query = this.getReadableDatabase().rawQuery("select * from mail where accountid=? and uid=? and folder=?", new String[]{accountId+"", uid,folderName});
			    			   query.moveToFirst();
			    			   info = new InMailInfo(query);
			    			   cache.put(key, info);
		    			   }
		    			   finally
		    			   {
		    				   query.close();
		    			   }
		    		   }
		    		   AttachmentInfo attach=new AttachmentInfo(info);
		    		   info.addAttachInfo(attach);
		    		   attach.index=rawQuery.getInt(attachidx);
		    		   attach.fileName=rawQuery.getString(attNameIndex);
		    		   attach.fileType=rawQuery.getString(attFileType);
		    		   attach.size = rawQuery.getString(attSizeIndex);
		    		   attach.canPreview = (rawQuery.getInt(previewIndex)!=0);
		    		   attach.fullFilePath = rawQuery.getString(filePathIndex);
		    		   
		    		   attachments.add(attach);
		    	   }while(rawQuery.moveToNext());
		         }
	           
	     	}
       }
       finally
       {
    	   rawQuery.close();
       }
     
	   return attachments;
	}
	public int getAttachmentCount() {
        String cmd="select count(*) as num from attachmentInfo";
        int attCount = this.executScalar(cmd, null);
        return attCount; 
        
	}
	public int getAttachCountAsType(String fileType)
	{
		String cmd="select count(*) as num from attachmentInfo where fileType='"+fileType+"'";
		int attCount=this.executScalar(cmd, null);
		return attCount;
	}
	public String deleteAccountInfo(Account a)
	{

		String errStr = "";
		SQLiteDatabase db = getWritableDatabase();
		// try {
		String cmdTxt = "";
		// the mails belonged to this account will also be deleted.
		cmdTxt = "delete from mail where accountId=" + a.id;
		db.execSQL(cmdTxt);

		cmdTxt= "delete from folders where accountid="+a.id;
		db.execSQL(cmdTxt);
		
		cmdTxt= "delete from attachmentInfo where accountid="+a.id;
		db.execSQL(cmdTxt);
		cmdTxt= "delete from mailGroup where accountid="+a.id;
		db.execSQL(cmdTxt);
		
		// } catch (SQLException e) {
		// errStr = e.getMessage();
		// }
		return errStr;
	}
	public int getGroupCount(int accountId, String folderName, boolean excludeHasMore){
		String cmd=null;
		if(excludeHasMore)
			cmd = "select count(*) from mailGroup where mailGroup.count>0 and mailGroup.folderName=? and not exists (select state from mail where mail.groupId=mailGroup.id and mail.state >="+MailStatus.FLAG_HAS_MORE_PLACEHOLD + ") ";
		else
			cmd = "select count(*) from mailGroup where count>0 and folderName=? ";
		Vector<String> args = new Vector<String>(2);
		args.add(folderName);
		if(accountId > 0)
		{
			cmd += " and mailGroup.accountId=?";
			args.add(accountId+"");
		}
		int executScalar = executScalar(cmd,args.toArray(new String[1]));
		return executScalar;
	}
	
	public  Cursor getInGroups(int accountId, int length, int offset, String folderName, int orderType, boolean excludeHasMore) 
	{
		String cmd = null;
		
		if(excludeHasMore)
		{
			cmd = "select * from mailGroup where mailGroup.count>0 and mailGroup.folderName=? and not exists (select state from mail where mail.groupId=mailGroup.id and mail.state >="+MailStatus.FLAG_HAS_MORE_PLACEHOLD + ") "; 
		}
		else
			cmd="select * from mailGroup where  mailGroup.count>0 and  mailGroup.folderName=? "; 
		Vector<String> args = new Vector<String>(2);
		args.add(folderName);
		if(accountId > 0)
		{
			cmd += " and  mailGroup.accountId=? ";
			args.add(accountId+"");
		}
		cmd += getGroupOrderSql(orderType);
		cmd+=" limit " + length +" offset " +offset;
		return query(cmd,args.toArray(new String[1]));
	}


	public Vector<MailInfo> queryMailByGroup(int groupId) {
		
		long curTime = System.currentTimeMillis();
		//state 3,4,5 equals to MailStatus.MAIL_DELETED MAIL_DELETE_FOREVER MAIL_LOCAL_DELETED
		String cmd="select * from mail where groupId=? and state not in (3,4,5) order by date desc";	
		String [] args={String.valueOf(groupId)};
		Cursor cursor=null;
		Vector<MailInfo> vector=new Vector<MailInfo>(); 
		cursor=query(cmd,args);
		try {
			if(cursor!=null)
			{
			   if(cursor.moveToFirst())
			   {
				   do{
					 MailInfo mailInfo =new InMailInfo(cursor);
					 vector.add(mailInfo);
				   }while(cursor.moveToNext());
			   }
			}
		  }
          finally
          {
        	  cursor.close();
          }
		long spend = System.currentTimeMillis() - curTime;
		if(spend > 10000)
			Log.w(Utils.LOGTAG, "getMailByGroup:"+groupId+" spend(ms):"+spend);
		return vector ;
		
	}
	
	public void updateMailGroupState()
	{
		long start = System.currentTimeMillis();
		String sql = "update mailGroup set count=(select count(*) from mail where mail.groupId=mailGroup.id and (mail.state !="+MailStatus.MAIL_DELETED +" and mail.state!="+MailStatus.MAIL_DELETE_FOREVER+" and mail.state!="+MailStatus.MAIL_LOCAL_DELETED+"))";
		execSQL(sql, new Object[] {});
		long spend = System.currentTimeMillis()-start;
		if(spend > 10000)
		{
			Log.w(Utils.LOGTAG, "updateMailGroupState spend to long time(ms):" + spend);
		}
	}
	
	public long insertAttachBody(AttachmentInfo attachInfo,int pageNo)
	{
		SQLiteDatabase db = getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put("body", attachInfo.getBody());
		cv.put("uidx", attachInfo.getUidx());
		cv.put("accountId",attachInfo.getAccountId());
		cv.put("folder",attachInfo.getFolder());
		cv.put("attachIdx",attachInfo.getAttachIndx());
		cv.put("attachPage", pageNo);
		//String[] args = { attachInfo.getUidx()+"",attachInfo.getAccountId()+"",attachInfo.getFolder(),attachInfo.getAttachIndx()+"",pageNo+""};
//		db.update("attachPreviewCache", cv, "uidx=? and accountId=? and folder=? and attachIdx=? and attachPage=?",args);
//	    db.insert("attachPreviewCache", null, cv);//
	    long i = db.replace("attachPreviewCache", null, cv);
	    return i;
	}
	public String getAttachBody(AttachmentInfo attachInfo,int pageNo)
	{
		SQLiteDatabase db = getReadableDatabase();
		String[] args = { attachInfo.getUidx()+"",attachInfo.getAccountId()+"",attachInfo.getFolder(),attachInfo.getAttachIndx()+"",pageNo+""}; 
		Cursor cursor = db.rawQuery("select body from attachPreviewCache " +
				                    "where uidx=? and accountId=? and folder=? and attachIdx=? and attachPage=?",args);  
		String attachBody = null;
		try
		{
			if(cursor !=null)
			{
				if(cursor.moveToFirst())
			    {
			       attachBody = cursor.getString(0);
			    }
			}
		}
		finally
		{
			cursor.close();
		}
		//db.close();  
		return attachBody;
	}
	
	public String getSessionId(Account a)
	{
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery("select sessionID from account where ID=?", new String[] {a.id+""});
		String result = null;
		try
		{
			if (cursor.moveToFirst())
				result = cursor.getString(0);
		}
		finally
		{
			cursor.close();
		}
		return result;
	}
	public void saveSessionId(Account a, String sessionId)
	{
		String sql = "update account set sessionId=? where ID=?";
		execSQL(sql, new Object[] {sessionId, a.id});
	}
	
	
};

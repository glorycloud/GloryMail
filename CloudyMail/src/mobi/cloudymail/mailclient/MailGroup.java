package mobi.cloudymail.mailclient;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import mobi.cloudymail.data.MailInfo;
import mobi.cloudymail.data.MailStatus;
import mobi.cloudymail.util.NewDbHelper;
import mobi.cloudymail.util.Utils;
import android.database.Cursor;
import android.util.Log;

public class MailGroup 
{
	private int id=0;
	private int accountId=0;
	private String suffix="";
	private Date date;
	private String from="";
	private int count;
	private int state;
    Vector<MailInfo> _groupVector=null;
	
	public MailGroup(Cursor cursor)//throws SQLException
	{
		setId(cursor.getInt(cursor.getColumnIndex("id")));
		setSuffix(cursor.getString(cursor.getColumnIndex("suffix")));
		setAccountId(cursor.getInt(cursor.getColumnIndex("accountId")));
		setFrom(cursor.getString(cursor.getColumnIndex("from")));
		String dateStr = cursor.getString(cursor.getColumnIndex("date"));
		state = cursor.getInt(cursor.getColumnIndex("state"));
		count = cursor.getInt(cursor.getColumnIndex("count"));
		//get mailInfo
		
        
		try 
		{
			setDate(Utils.netDateFormater.parse(dateStr));
		} catch (ParseException e) {
			Log.d(Utils.LOGTAG, "",e);
		}
	}
	
	private Vector<MailInfo> getMails()
	{
		if(_groupVector == null)
			_groupVector= NewDbHelper.getUiCriticalInstance().queryMailByGroup(id);
		return _groupVector;
	}
	public int getAccountId() {
		return accountId;
	}

	public void setAccountId(int accountId) {
		this.accountId = accountId;
	}

	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public int getId() {
		return id;
	}

	public void setId(int groupId) {
		this.id = groupId;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}


//	private List<MailInfo> ins = new ArrayList<MailInfo>();
//	public int id = 0;

	public MailInfo getMailInfo(int i)
	{
		Vector<MailInfo> groupVector = getMails();
		if(groupVector.size()>1)
		{ 
			return groupVector.elementAt(i);
		}
		if(groupVector.size()>0)
		{
			return groupVector.elementAt(0);
		}
		else
			return null;
	}
	
	public int getGroupSize()
	{
		return count;
	}

	public boolean isReaded()
	{
		Iterator<MailInfo> it = getMails().iterator();
		while(it.hasNext())
		{
			if(it.next().getState() == MailStatus.MAIL_NEW)
				return false;
		}
		return true;
	}
	
	public int getState()
	{
		return state;
	}
}



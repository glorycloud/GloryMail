package mobi.cloudymail.data;

import java.io.File;
import java.io.Serializable;

import mobi.cloudymail.mailclient.AccountManager;
import mobi.cloudymail.mailclient.Composer;
import mobi.cloudymail.mailclient.net.Account;
import mobi.cloudymail.mailclient.net.AttachmentInfo;
import mobi.cloudymail.util.Utils;
import android.database.Cursor;

public class OutMailInfo extends MailInfo implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 8385446331316177045L;
	public static final int REFMAIL_NO=0;
	public static final int REFMAIL_YES=1;
	public static final int REFMAIL_RESPOND_INLINE=2;
	private String bc="";
	private int mailType=Composer.COMPOSER_NEWMAIL;//new, reply, reply all, or forward.
	private int refBodyFlag=REFMAIL_NO;
	private String refFolder="";
	/**
	 * @return the refFolder
	 */
	public String getRefFolder()
	{
		return refFolder;
	}
	/**
	 * @param refFolder the refFolder to set
	 */
	public void setRefFolder(String refFolder)
	{
		this.refFolder = refFolder;
	}
	public OutMailInfo()
	{
		
	}
	//uidx is the column id, uid is the column refUid.
	public OutMailInfo(Cursor cursor)// throws SQLException
	{
		super(cursor);
		// TODO Auto-generated constructor stub
		Account a = AccountManager.getAccount(getAccountId());
		if(a != null)
			setFrom(a.name);
		setBc(cursor.getString(cursor.getColumnIndex("bc")));
		setUidx(cursor.getInt(cursor.getColumnIndex("ID")));
		setUid(cursor.getString(cursor.getColumnIndex("refUid")));
		setMailType(cursor.getInt(cursor.getColumnIndex("mailType")));
		setRefBodyFlag(cursor.getInt(cursor.getColumnIndex("refBody")));
		
		
		String attachFiles = cursor.getString(cursor.getColumnIndex("attachments"));
		if(attachFiles == null || attachFiles.equals(""))
		{
//			_hasAttachment = false;
//			_hasNormalAttachment = false;
//			_hasCalendarAttachment = false;
			_attachments.clear();
		}
		else
		{
			//The attachments in column attachments are saved as:
			//index:fileName:size for referenced mail's attachment index.
			//index:fullFilePath for local attachment.
			//each attachment is seperated by ";", attach:attach:attach
			String[] attStrList = attachFiles.split(";");
			for(String attaStr:attStrList)
			{
				String[] tmpList = attaStr.split(":");
				int attaIdx = Integer.parseInt(tmpList[0]);
				AttachmentInfo attaInfo;
				if(attaIdx < 0 )//local attachment
				{//index,fullFilePath
					if(tmpList.length < 2)
						continue;
					String fullFilePath = tmpList[1];
					File attFile = new File(fullFilePath);
					if (!attFile.exists())
						continue;
					attaInfo = new AttachmentInfo(this);
					attaInfo.fileName = attFile.getName();
					// Log.d(fileName,
					// "File.length()="+attFile.length()+"; FileInputStream.available():"+fi.available());
					attaInfo.size = Utils.getReadableSize(attFile.length());// fi.available());
					attaInfo.fullFilePath = fullFilePath;
					_attachments.add(attaInfo);
				}
				else //ref mail's attachment.
				{//index,fileName,size
					if(tmpList.length < 3)
						continue;
					attaInfo = new AttachmentInfo(this,attaIdx);
					attaInfo.fileName = tmpList[1];
					attaInfo.size = tmpList[2];
					_attachments.add(attaInfo);
				}		
				
				if(!hasNormalAttachment() || !hasCalendarAttachment()) 
				{
					boolean is_calendar_attach = attaInfo.fileName.endsWith(".ics");
					if(is_calendar_attach) 
					{
						attachFlag |=ATF_CALENDAR_ATTACH;
					} 
					else
					{
						attachFlag |= ATF_NORMAL_ATTACH;
					}
				}
				
			}
			
		}
	}

	public void setAttachmentFlag(int attachFlag)
	{
		this.attachFlag=attachFlag;
	}
	
	public void setBc(String bc)
	{
		this.bc = bc;
	}

	public String getBc()
	{
		return bc;
	}

	public void setMailType(int mailType)
	{
		this.mailType = mailType;
	}

	public int getMailType()
	{
		return mailType;
	}

	public void setRefBodyFlag(int refBodyFlag)
	{
		this.refBodyFlag = refBodyFlag;
	}

	public int getRefBodyFlag()
	{
		return refBodyFlag;
	}

}

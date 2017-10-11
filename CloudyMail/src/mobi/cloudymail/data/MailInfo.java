package mobi.cloudymail.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import mobi.cloudymail.mailclient.net.AttachmentInfo;
import mobi.cloudymail.util.Utils;
import android.database.Cursor;



	public class MailInfo implements Serializable
	{
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((_folder == null) ? 0 : _folder.hashCode());
			result = prime * result + accountId;
			result = prime * result + ((uid == null) ? 0 : uid.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MailInfo other = (MailInfo) obj;
			if (_folder == null)
			{
				if (other._folder != null)
					return false;
			}
			else if (!_folder.equals(other._folder))
				return false;
			if (accountId != other.accountId)
				return false;
			if (uid == null)
			{
				if (other.uid != null)
					return false;
			}
			else if (!uid.equals(other.uid))
				return false;
			return true;
		}

		private static final long serialVersionUID = 4941273778839405189L;
		private String subject="";
		private String from="";//------------------
		private String to="";
		private String cc="";
		private String uid="";//in OutMailInfo, it's the refId.
		private Date date;
		private int accountId;//belonged account's id.
		private String dateString; //date in string format
		private int state = MailStatus.MAIL_NEW;
		private int asterisk=0;
		protected int attachFlag = 0;
//		protected boolean _hasAttachment = false;
//		protected boolean _hasNormalAttachment = false;
//		protected boolean _hasCalendarAttachment = false;
		protected List<AttachmentInfo> _attachments = new ArrayList<AttachmentInfo>();
		private String _folder="";
		private int uidx=-1;
		private String body="";
		private long groupId=0;
		
//		private String suffix="";
		public static final int ATF_NO_ATTACH = 0;
		public static final int ATF_NORMAL_ATTACH = 1;
		public static final int ATF_CALENDAR_ATTACH = ATF_NORMAL_ATTACH << 1;

		public static final int REPLY=ATF_CALENDAR_ATTACH<<1;
		public static final int FORWARD=REPLY<<1;
		public MailInfo()
		{
			
		}
		
		public MailInfo(Cursor cursor) //throws SQLException
		{
			setSubject(cursor.getString(cursor.getColumnIndex("subject")));
//			setFrom(cursor.getString(cursor.getColumnIndex("from")));
			setTo(cursor.getString(cursor.getColumnIndex("to")));
			setCc(cursor.getString(cursor.getColumnIndex("cc")));
			setBody(cursor.getString(cursor.getColumnIndex("body")));
			setAsterisk(cursor.getInt(cursor.getColumnIndex("asterisk")));
			try
			{
				String dateStr = cursor.getString(cursor.getColumnIndex("date"));
				setDate(Utils.netDateFormater.parse(dateStr));
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			setFolder(cursor.getString(cursor.getColumnIndex("folder")));
			setAccountId(cursor.getInt(cursor.getColumnIndex("accountId")));
			setState(cursor.getInt(cursor.getColumnIndex("state")));
		}

		public boolean hasNormalAttachment()
		{
			//return _hasNormalAttachment;
			return (attachFlag&ATF_NORMAL_ATTACH)>0;
		}
		
		public long getGroupId() {
			return groupId;
		}
		public boolean hasCalendarAttachment()
		{
			return (attachFlag&ATF_CALENDAR_ATTACH)>0;
		}

//		public String getSuffix() {
//			return suffix;
//		}

//		public void setSuffix(String suffix) {
//			this.suffix = suffix;
//		}

		public void setGroupId(long groupId) {
			this.groupId = groupId;
		}
		public boolean hasAttachment()
		{
			return hasNormalAttachment() || hasCalendarAttachment();
		}
		
		public int getAttachmentFlag()
		{
			return this.attachFlag;
		}
		
		public void setAttachmentFlag(int attachmentFlag)
		{
			this.attachFlag = attachmentFlag;
		}
		
		public void setHasAttachment(boolean hasAttach, boolean useDb)
		{
				this.attachFlag |= ATF_NORMAL_ATTACH;
			
		}
		
		public AttachmentInfo getAttachment(int index)
		{
			if(index < 0 || !hasAttachment() || _attachments.isEmpty() || index >= _attachments.size())
				return null;
			return _attachments.get(index);
		}

		public List<AttachmentInfo> getAttachments()
		{
			return _attachments;
		}
		
		public void setAttachments(List<AttachmentInfo> attaches)
		{
			_attachments = new ArrayList<AttachmentInfo>(attaches);
//			setHasAttachment( !attaches.isEmpty(), true);
		}
		


		//this property simply return attachments raw information, the difference with property attachments
		//is, if has not download attachments from server, and this mail has attachment, a special
		//AttachmentInfo will included in returned list, this special AttachmentInfo object has index -2
		public List<AttachmentInfo> getRawAttachments()
		{
			
			return _attachments;
			
		}

		public void setSubject(String subject)
		{
			this.subject = subject;
		}

		public String getSubject()
		{
			return subject;
		}

		public void setFrom(String from)
		{
			this.from = from;
		}

		public String getFrom()
		{
			return from;
		}

		public void setTo(String to)
		{
			this.to = to;
		}

		public String getTo()
		{
			return to;
		}

		public void setCc(String cc)
		{
			this.cc = cc;
		}

		public String getCc()
		{
			return cc;
		}

		public void setUid(String uid)
		{
			this.uid = uid;
		}

		public String getUid()
		{
			return uid;
		}
        
		public int getAsterisk()
		{
			return asterisk;
		}

		public void setAsterisk(int asterisk)
		{
			this.asterisk = asterisk;
		}

		public void setDate(Date date)
		{
			this.date = date;
		}

		public Date getDate()
		{
			return date;
		}

		public void setAccountId(int accountId)
		{
			this.accountId = accountId;
		}

		public int getAccountId()
		{
			return accountId;
		}

		public void setState(int state)
		{
			this.state = state;
		}

		public int getState()
		{
			return state;
		}

//		public void setDateString(String dateString)
//		{
//			this.dateString = dateString;
//		}

		public String getDateString()
		{
			if(dateString == null && date != null)
			{
				Date now = new Date();
			
				if (date.getYear() != now.getYear())
				{
					dateString = Utils.earlierFormat.format(date);
				}
				else
				{
					dateString = Utils.nearFormat.format(date);
				}
			}
			return dateString;
		}

		public void setFolder(String folder)
		{
			this._folder = folder;
		}

		public String getFolder()
		{
			return _folder;
		}

		public void setUidx(int uidx)
		{
			this.uidx = uidx;
		}

		public int getUidx()
		{
			return uidx;
		}
		
		public void setBody(String body)
		{
			this.body = body;
		}

		public String getBody()
		{
			return body;
		}

		public void addAttachInfo(AttachmentInfo attach){
			_attachments.add(attach);
			if(attach.fileName.endsWith(".ics"))
				this.attachFlag |= ATF_CALENDAR_ATTACH;
			else {
				this.attachFlag |= ATF_NORMAL_ATTACH;
			}
		}


		public boolean hasReply()
		{
			// TODO Auto-generated method stub
			return (attachFlag & REPLY)!=0;
		}

		public boolean hasForward()
		{
			// TODO Auto-generated method stub
			return (attachFlag & FORWARD)!=0;
		}
        
	}

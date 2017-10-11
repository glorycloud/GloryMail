package mobi.cloudymail.mailclient.net;

import java.io.File;
import java.io.Serializable;
import java.util.Date;

import mobi.cloudymail.data.MailInfo;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Transient;

import android.os.Parcel;
import android.os.Parcelable;


	public class AttachmentInfo implements Serializable, Parcelable
	{
		/**
		 * 
		 */
		@Transient
		private static final long serialVersionUID = -3505454732504893305L;
		@Transient
		public static final int LOCAL_ATTACH_INDEX = -1;
		@Transient
		public static final int ALL_REFATTACH_INDEX = -2;
		
		@Element
		public String fileName="";
		
		
		//only for local attachment.
		@Transient
		public String fullFilePath;
		@Element
		public int index = -1; //index of this attachment in its container mail, 
							   //-1 means its from local
							   //-2 means all original attachments from referenced mail
								
		@Element
	//	public byte[] body; //body of the attachment, used to upload local created attachment to server
		public String body="";
		//		@Attribute(required=false)
		@Attribute
		public String size; //size in string, may 100Bytes, 10K, 1M, etc
		@Attribute(required=false)
		public boolean canPreview;
		@Transient
		public String fileType;
		
		@Transient
		MailInfo mailInfo;
		
		@Transient
		public String getFileType() {
			return fileType;
		}
		@Transient
		public void setFileType(String fileType) {
			this.fileType = fileType;
		}
		@Transient
		
		
		public AttachmentInfo(MailInfo mail)
		{
			mailInfo = mail;
		}
		
		public AttachmentInfo(MailInfo mail, int index)
		{
			mailInfo = mail;
			this.index = index;
		}
		
				
		@Transient
		public String getAttachSize() {
			return size;
		}
		@Transient
		public void setAttachSize(String attachSize) {
			this.size = attachSize;
		}
		@Transient
		public int getAttachIndx() {
			return index;
		}
		@Transient
		public void setAttachIndx(int attachIndx) {
			this.index = attachIndx;
		}
		@Transient
		public int getUidx()
		{
			return mailInfo.getUidx();
		}
		@Transient
		public String getMailUid()
		{
			return mailInfo.getUid();
		}
		@Transient
		public int getAccountId()
		{
			return mailInfo.getAccountId();
		}
		@Override
		public int describeContents() {
			return 0;
		}
		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeSerializable(this);
			
		}
		
		public static final Parcelable.Creator<AttachmentInfo> CREATOR = new Parcelable.Creator<AttachmentInfo>() {
		    public AttachmentInfo createFromParcel(Parcel in) {
		    	return (AttachmentInfo) in.readSerializable();
		    }
		
		    public AttachmentInfo[] newArray(int size) {
		        return new AttachmentInfo[size];
		    }
};
		@Transient
		public Date getMailDate() {
			// TODO Auto-generated method stub
			return mailInfo.getDate();
		}
		@Transient
		public MailInfo getMailInfo() {
			// TODO Auto-generated method stub
			return mailInfo;
		}
		
		public boolean isFilePathValid()
		{
			if(fullFilePath == null || fullFilePath.equals(""))
				return false;
			return new File(fullFilePath).exists();
		}
		//·ÂÕÕMailInfoÖÐµÄsetBodyºÍgetBody
		public void setBody(String body)
		{
			this.body = body;
		}

		public String getBody()
		{
			return body;
		}
		int totalPageCount=0;
		public void setTotalPageCount(int pageCount)
		{
			totalPageCount=pageCount;
		}
		public int getTotalPageCount()
		{
			return totalPageCount;
		}
//		public void setFolder(String folder)
//		{
//			this._folder = folder;
//		}

		public String getFolder()
		{
			return mailInfo.getFolder();
		}
	}

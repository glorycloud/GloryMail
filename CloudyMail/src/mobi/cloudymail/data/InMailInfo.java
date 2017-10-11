package mobi.cloudymail.data;

import java.io.Serializable;

import mobi.cloudymail.mailclient.net.AttachmentInfo;
import mobi.cloudymail.util.NewDbHelper;
import mobi.cloudymail.util.Utils;
import android.database.Cursor;
public class InMailInfo extends MailInfo implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 4941273778839405159L;

	public InMailInfo(Cursor cursor) //throws SQLException
	{
		super(cursor);
		setFrom(cursor.getString(cursor.getColumnIndex("from")));
		setUid(cursor.getString(cursor.getColumnIndex("uid")));
		setUidx(cursor.getInt(cursor.getColumnIndex("uidx")));
		setAttachmentFlag(cursor.getInt(cursor.getColumnIndex("hasAttach")));
	}
	
	public InMailInfo(int accountId, org.w3c.dom.Element xr) //throws SQLException
	{
		setSubject( xr.getAttribute("subject"));
		setTo( xr.getAttribute("to"));
		setCc( xr.getAttribute("cc"));
		try
		{
			String dateStr =  xr.getAttribute("date");
			setDate(Utils.netDateFormater.parse(dateStr));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		setFolder( xr.getAttribute("foldername"));
		setAccountId(accountId);
		setState(Integer.parseInt( xr.getAttribute("state")));
		
		setFrom(xr.getAttribute("from"));
		setUid(xr.getAttribute("uid"));
		setUidx(Integer.parseInt(xr.getAttribute("uidx")));
		setAttachmentFlag(Integer.parseInt(xr.getAttribute("attachmentFlag")));
	}
	
	@Override
	public void addAttachInfo(AttachmentInfo attach) {
		int oldFlag = attachFlag;
		super.addAttachInfo(attach);
		if(oldFlag != attachFlag)
			NewDbHelper.getInstance().setMailAttachment(getUid(),getFolder(),attachFlag);
	}

	
	@Override
	public void setHasAttachment(boolean hasAttach,boolean updateDb)
	{
		if(hasAttach&&!hasAttachment())
		{
			_attachments.clear();
			attachFlag |= ATF_NORMAL_ATTACH;
//			_attachments.add(new AttachmentInfo(AttachmentInfo.ALL_REFATTACH_INDEX));
		}
		else if(!hasAttach){
			attachFlag &= ~ATF_NORMAL_ATTACH;
		}
		if (hasAttach == true && hasAttachment() == false)
		{
//			_attachments.clear();
//			_hasAttachment = true;
//			_attachments.add(new AttachmentInfo(AttachmentInfo.ALL_REFATTACH_INDEX));
			if(updateDb)
				NewDbHelper.getInstance().setMailAttachment(getUid(),getFolder(),attachFlag);
		}
		else if(hasAttach == false && hasAttachment() == true)
		{
			_attachments.clear();
//			_hasAttachment = false;
			if(updateDb)
				NewDbHelper.getInstance().setMailAttachment(getUid(),getFolder(),attachFlag);
		}
	}
}

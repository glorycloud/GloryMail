package mobi.cloudymail.mailclient.net;

import static mobi.cloudymail.util.Utils.LOGTAG;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Iterator;

import mobi.cloudymail.mailclient.AccountManager;
import mobi.cloudymail.mailclient.R;
import mobi.cloudymail.util.DialogUtils;
import mobi.cloudymail.util.MyApp;
import mobi.cloudymail.util.NewDbHelper;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.parameter.Value;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;


public class DownloadAttaTask extends AsyncTask<String, Integer, String>
{
	private Resources res = MyApp.instance().getResources();
	private ProgressDialog prgDialog = null;//new ProgressDialog(MailViewer.this);
	private File storeFile;
	private int attachIndex;
	//file in rar/zip will have this property.
	private String internalPath=null;
	private String sid = null;
	private String mailUid = null;
	private String mailFolder=null;
	private int accountId=0;
	private Handler handler = null;
	private AttachmentInfo attachInfo;
	private boolean _isCalendarImport; 
	
	private static String calendarURIBase = "";
	
	static {
		if (Integer.parseInt(Build.VERSION.SDK) >= 8) {
			calendarURIBase = "content://com.android.calendar";
		} else {
			calendarURIBase = "content://calendar";
		}
	}
	
	public DownloadAttaTask(AttachmentInfo attachInfo,String internalPath, boolean isCalendarImport)
	{
		this.attachInfo = attachInfo;
		this.attachIndex = attachInfo.getAttachIndx();
		prgDialog = new ProgressDialog(MyApp.getCurrentActivity());
		this.internalPath = internalPath;
		this.mailFolder=attachInfo.getMailInfo().getFolder();
		this.mailUid = attachInfo.getMailUid();
		this.accountId = attachInfo.getAccountId();
		this._isCalendarImport = isCalendarImport;
	}
	
	public void setHandler(Handler hd)
	{
		handler = hd;
	}
	
	@Override
	protected String doInBackground(String... fileNames)
	{
		if(sid == null)
			return res.getString(R.string.failLogin);
		// int count = attaInfos.length;
//		AttachmentInfo attInfo = _attachments.get(selectedIdx);// MailClient.curMailInfo.getAttachment(selectedIdx);
		String fileName = fileNames[0];
		String errStr = "";
		HttpGet req = null;

		try
		{
			String reqUrl = ServerAgent.getUrlBase()
							+ "/DownloadPart?uid="
							+ java.net.URLEncoder.encode(mailUid)
							+ "&folderName="
							+ java.net.URLEncoder.encode(mailFolder)
							+"&index=" + attachIndex+"&sid="+java.net.URLEncoder.encode(sid);
			if(internalPath!=null && !internalPath.equals(""))
				reqUrl += "&internalPath="+java.net.URLEncoder.encode(internalPath);
			req = new HttpGet(reqUrl);
     //			File sdCardDir = Environment.getExternalStorageDirectory();
			
			storeFile = new File(fileName);
	//		Log.d(LOGTAG, "AsyncTask:get attachment:" + reqUrl);
	//		Log.d(LOGTAG, "AsyncTask:save to:" + storeFile.toString());
			HttpResponse rsp = ServerAgent.execute(req);
			if (HttpStatus.SC_OK == rsp.getStatusLine().getStatusCode())
			{
				HttpEntity entity = rsp.getEntity();
				if (entity == null)
					return "No content downloaded";
				Log.d(LOGTAG, "attach:" + fileName + " "
								+ entity.getContentType().getName());
				Log.d(	LOGTAG,
						"attach:" + fileName + " "
								+ String.valueOf(entity.isStreaming()));
				String state = Environment.getExternalStorageState();
				if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
				{
					return res.getString(R.string.atta_sdcardReadOnly);
				}
				else if (!Environment.MEDIA_MOUNTED.equals(state))
				{
					return res.getString(R.string.atta_sdcardNotAvailable);
				}

				FileOutputStream outStream = new FileOutputStream(storeFile);
				InputStream input = entity.getContent();
				byte b[] = new byte[1024];
				int j = 0;
				while ((j = input.read(b)) != -1)
				{
					outStream.write(b, 0, j);
				}
				outStream.flush();
				outStream.close();
				if (entity != null)
				{
					entity.consumeContent();
				}
			}
			// publishProgress(result);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return e.getMessage();
		}
		return errStr;
	}

	@Override
	protected void onPreExecute()
	{
		Account a = AccountManager.getAccount(accountId);
		sid=MyApp.getAgent(a).getSessionId(true, true, true);
		if(sid == null)
			return;
		prgDialog.setMessage(res.getString(R.string.atta_downloading));
		prgDialog.show();
	}

	@Override
	protected void onProgressUpdate(Integer... progress)
	{
		// setProgressPercent(progress[0]);
	}

	@Override
	protected void onPostExecute(String result)
	{
		if(sid == null)
			return;
		prgDialog.dismiss();
		if(result == null)
		{
			result = MyApp.instance().getResources().getString(R.string.failedDownloadAttach);
		}
		if (result.equals(""))
		{
			String path = "";
			try
			{
				path = storeFile.getCanonicalPath();
			}
			catch (Exception e)
			{
				path = storeFile.getAbsolutePath();
			}
			//if it's a internal file in rar/zip attachment,ignore it
			if (internalPath == null || internalPath.equals(""))
			{
				// update attachmentInfo table.
				if (NewDbHelper.getInstance()
						.updateAttachFilePath(accountId, mailUid,mailFolder, attachIndex,path))
					attachInfo.fullFilePath = path;
			}
			Toast.makeText(MyApp.getCurrentActivity(),
							res.getString(R.string.atta_fileSaved)+path,
							Toast.LENGTH_SHORT).show();
			//send sessage to attached activity
			if(handler!=null)
				handler.sendEmptyMessage(1);
			
			if(this._isCalendarImport) {
				try {
					importCalendarEvents(storeFile);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		else
		{
			Log.d("download failed:", result);
			// MessageBox.show(AttachmentList.this,result,
			// getResources().getString(R.string.error));
			DialogUtils.showMsgBox(	MyApp.getCurrentActivity(), result,
			                       	MyApp.getCurrentActivity().getResources()
											.getString(R.string.error));
		}
	}
	
	public static void importCalendarEvents(File calendarFile) throws ParseException 
	{
		FileInputStream fin = null;
		try {
			fin = new FileInputStream(calendarFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		CalendarBuilder builder = new CalendarBuilder();
		
		Calendar calendar = null;
		try {
			calendar = builder.build(fin);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (Iterator i = calendar.getComponents().iterator(); i.hasNext();) {
			Component component = (Component) i.next();
			System.out.println("Component [" + component.getName() + "]");
				
			ContentValues event = new ContentValues(); 
			Property property = null;
			String value = null;
			
			//insert description
			property = component.getProperty(Property.SUMMARY);
			if(property!=null) {
				value = property.getValue();
				event.put("description", value); 
			}
			
			//insert event time zone
			property = component.getProperty(Property.TZNAME);
			if(property!=null) {
				value = property.getValue();
				event.put("eventTimezone", value); 
			}
			
			//insert event location
			property = component.getProperty(Property.LOCATION);
			if(property!=null) {
				value = property.getValue();
				event.put("eventLocation", value); 
			}
			
			//insert event title
			property = component.getProperty(Property.NAME);
			if(property!=null) {
				value = property.getValue();
				event.put("title", value);
			}
			
			//insert event rrule
			property = component.getProperty(Property.RRULE);
			if(property!=null) {
				value = property.getValue();
				event.put("rrule", value);
			}
			
			//insert start time
			property = component.getProperty(Property.DTSTART);
			if(property!=null) {
				value = property.getValue();
	
				if(property.getParameter("VALUE") == Value.DATE)
				{
					Date dt = new Date(value);
					event.put("allDay", "1");
					long beginTime = dt.getTime();
					event.put("dtstart", beginTime);
				}
				else
				{
					DateTime dt = new DateTime(value);
					event.put("allDay", "0");
					long beginTime = dt.getTime();
					event.put("dtstart", beginTime);
				}
			}
			
			//insert end time
			property = component.getProperty(Property.DTEND);
			if(property!=null) {
				value = property.getValue();
	
				if(property.getParameter("VALUE") == Value.DATE)
				{
					Date dte = new Date(value);
					long endTime = dte.getTime();
					event.put("dtend", endTime);
				}
				else
				{
					DateTime dte = new DateTime(value);
					long endTime = dte.getTime();
					event.put("dtend", endTime);
				}
			}			
						
			event.put("selfAttendeeStatus", 1); 
			event.put("eventStatus", 1);
			event.put("visibility", 0);
			event.put("transparency", 0);
			event.put("hasAlarm", 1); // 0 for false, 1 for true 
			
			event.put("calendar_id", 0);
		    Uri eventsUri = Uri.parse(calendarURIBase + "/events"); 
		    Uri insert_event = MyApp.instance().getContentResolver().insert(eventsUri, event);  
		    
		    String eventID = insert_event.getLastPathSegment(); 
		    ContentValues values = new ContentValues(); 
			values.put("event_id", eventID); 
	        values.put("method", 1); 
	        values.put("minutes", 20);
	        Uri REMINDERS_URI = Uri.parse(calendarURIBase + "/reminders"); 
	        MyApp.instance().getContentResolver().insert(REMINDERS_URI, values);  	        
		}		
		Toast.makeText(MyApp.getCurrentActivity(), calendarFile.getName() + MyApp.instance().getResources().getString(R.string.atta_import), Toast.LENGTH_SHORT).show();
	}
}

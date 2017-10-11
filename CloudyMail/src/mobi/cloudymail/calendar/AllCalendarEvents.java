package mobi.cloudymail.calendar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mobi.cloudymail.mailclient.Composer;
import mobi.cloudymail.mailclient.R;
import mobi.cloudymail.util.Utils;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Name;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.TzName;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.util.HostInfo;
import net.fortuna.ical4j.util.UidGenerator;

import org.apache.commons.lang3.time.DateUtils;

import android.app.Activity;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class AllCalendarEvents extends ListActivity implements CompoundButton.OnCheckedChangeListener
{
	public static int year, month, day;
	
	private java.util.Calendar cal_begin = java.util.Calendar.getInstance();
	private java.util.Calendar cal_end = java.util.Calendar.getInstance();
	private java.util.Calendar helper_calendar = java.util.Calendar.getInstance();
		
    //private Button miNewRecord1;
	private Button miDeleteRecord1; 
	private Button miSendSelectedRecord1;
	private LinearLayout layout;
	private static String calendarURIBase = "";
	private List< Map<String, String> > eventList;
	private  Set<Integer> _checkedEvents = new HashSet<Integer>();
	private EventlistAdapter eventAdapter;
	
	static {
		if (Integer.parseInt(Build.VERSION.SDK) >= 8) {
			calendarURIBase = "content://com.android.calendar";
		} else {
			calendarURIBase = "content://calendar";
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.calendar_event_list);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.calendar_titlebar);
		//miNewRecord1=(Button)findViewById(R.id.addEventBtn);
		miDeleteRecord1=(Button)findViewById(R.id.delEventBtn);
		miSendSelectedRecord1=(Button)findViewById(R.id.SendEventBtn);
		layout=(LinearLayout)findViewById(R.id.selectEventLayout);
		updateWindowTitle();
		Button bt = (Button)findViewById(R.id.add_event);
		bt.setOnClickListener(new OnClickListener() {
		
			@Override
			public void onClick(View v)
			{
				 NewRecord();
				
			}
		});
//		setTitle(sdf.format(helper_calendar.getTime()));
//		miNewRecord1.setOnClickListener(new OnClickListener() {
//			
//			@Override
//			public void onClick(View v)
//			{
//				 NewRecord();
//				
//			}
//		});
		miDeleteRecord1.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v)
			{
				DelRecord();
			}
		});
		miSendSelectedRecord1.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v)
			{
				
				SendSelectRecord();
			}
		});
		if (eventList == null)
			eventList = new ArrayList<Map<String, String>>();
		else 
			eventList.clear();
		
		getEventList();
		ListView lv=getListView();
		if(eventList.size()==0)
		{
			lv.setBackgroundResource(R.drawable.noevent);
		}
		
		if(eventAdapter == null) 
				eventAdapter = new EventlistAdapter(eventList, this);

		setListAdapter(eventAdapter);
	}

	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
		updateWindowTitle();
		eventList.clear();
		_checkedEvents.clear();
		getEventList();
		eventAdapter.setEvents(eventList);
	}

	
	private void updateWindowTitle()
	{
		year = getIntent().getExtras().getInt("year");
		month = getIntent().getExtras().getInt("month");
		day = getIntent().getExtras().getInt("day");
		SimpleDateFormat sdf = Utils.dateFmt;
		helper_calendar.set(year, month, day);
		TextView txtView=(TextView)findViewById(R.id.calendartitle);
		txtView.setText(sdf.format(helper_calendar.getTime()));
		
	}

	private void getEventList()
	{
		// first; find out which calendars exist	
		ContentResolver contentResolver = getContentResolver(); 
		/*
		 * Fetching all events, and particular event is done by specifying range
	     */
		Uri.Builder builder = Uri.parse(calendarURIBase + "/instances/when").buildUpon(); 
		cal_begin.set(year, month, day, 0, 0, 0);
		ContentUris.appendId(builder, cal_begin.getTimeInMillis()); //now - DateUtils.DAY_IN_MILLIS * 10000); 
		cal_end.set(year, month, day, 23, 59, 59);
		ContentUris.appendId(builder, cal_end.getTimeInMillis());//now + DateUtils.DAY_IN_MILLIS * 10000); 

		Cursor eventCursor = contentResolver.query(builder.build(), 
		                			               new String[] {"event_id", "title", "dtstart", "dtend", "allDay", "description", 
												   "eventLocation", "eventTimezone", "selfAttendeeStatus", "rrule"}, 
												   null, null, "startDay ASC, startMinute ASC");
		
/*		
		String names[];
		names = envetCursor1.getColumnNames();
*/		
		while (eventCursor.moveToNext())
		{
		//	String selfAS = eventCursor.getString(eventCursor.getColumnIndex("selfAttendeeStatus"));
		//	String eventStatus = eventCursor.getString(eventCursor.getColumnIndex("eventStatus"));		
			long t = Long.valueOf(eventCursor.getString(2));
			Date beginTime = new Date(t);
			helper_calendar.setTimeInMillis(t);
			int beginMonth = helper_calendar.get(Calendar.MONTH) + 1;
			int beginDay = helper_calendar.get(Calendar.DATE);
			int beginHour = helper_calendar.get(Calendar.HOUR_OF_DAY);
			int beginMin = helper_calendar.get(Calendar.MINUTE);
				
			long t2=Long.valueOf(eventCursor.getString(3));
			java.util.Date endTime = new Date(t2);
			helper_calendar.setTimeInMillis(t2);
			int endMonth = helper_calendar.get(Calendar.MONTH) + 1;
			int endDay = helper_calendar.get(Calendar.DATE);
			int endHour = helper_calendar.get(Calendar.HOUR_OF_DAY);
			int endMin = helper_calendar.get(Calendar.MINUTE);
					
			Map<String, String> map = new HashMap<String, String>();
			map.put("event_id", eventCursor.getString(0));
			map.put("title", eventCursor.getString(1));
			map.put("beginTime", eventCursor.getString(2));
			map.put("endTime", eventCursor.getString(3));
			map.put("allDay", eventCursor.getString(4));
			map.put("description", eventCursor.getString(5));
			map.put("location", eventCursor.getString(6));
			map.put("timezone", eventCursor.getString(7));
			map.put("selfAttendeeStatus", eventCursor.getString(8));
			map.put("rrule", eventCursor.getString(9));
			if(eventCursor.getString(4).equals("1")) 
			{
				if(endTime.getDate() == day)
				{
					continue;
				}
				helper_calendar.setTimeInMillis(t2 - DateUtils.MILLIS_PER_DAY);
				endMonth = helper_calendar.get(Calendar.MONTH) + 1;
				endDay = helper_calendar.get(Calendar.DATE);
				endHour = helper_calendar.get(Calendar.HOUR_OF_DAY);
				endMin = helper_calendar.get(Calendar.MINUTE);
				endTime=helper_calendar.getTime();
				map.put("content", eventCursor.getString(1) + "\r\n" +Utils.dateFmtNoYear.format(beginTime) + " - " + Utils.dateFmtNoYear.format(helper_calendar.getTime()));
			}
			else
			{
				if((beginTime.getMonth()==endTime.getMonth()) && (beginTime.getDate()== endTime.getDate()))
				{
					map.put("content", eventCursor.getString(1) + "\r\n" + beginHour + ":" + beginMin + " - " + endHour + ":" + endMin);
				}
				else
				{					
					map.put("content", eventCursor.getString(1) + "\r\n" + Utils.nearFormat.format(beginTime) + " - " 
				                                                         + Utils.nearFormat.format(endTime));		
				}
			}
			eventList.add(map);
		}
		eventCursor.close();
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		super.onListItemClick(l, v, position, id);
		Intent intent = new Intent(Intent.ACTION_VIEW); 
		intent.setData(Uri.parse(calendarURIBase + "/events/" + eventList.get(position).get("event_id")));  
		
	    intent.putExtra("beginTime", Long.valueOf(eventList.get(position).get("beginTime")));
	    intent.putExtra("endTime", Long.valueOf(eventList.get(position).get("endTime")));
		startActivity(intent); 
	}
	
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu)
//	{
//		miNewRecord = menu.add(0, 1, 1, "Ìí¼Ó");
//		miDeleteRecord = menu.add(0, 2, 2, "É¾³ý");
//		miSendSelectedRecord = menu.add(0, 5, 5, "·¢ËÍ");
//		
//		miNewRecord.setOnMenuItemClickListener(new OnAddRecordMenuItemClick(this));
//		miDeleteRecord.setOnMenuItemClickListener(new OnDeleteRecordMenuItemClick(this));
//		miSendSelectedRecord.setOnMenuItemClickListener(new OnSendSelectedRecordMenuItemClick(this));
//		return true;
//	}
	
	class MenuItemClickParent
	{
		protected Activity activity;

		public MenuItemClickParent(Activity activity)
		{
			this.activity = activity;
		}
	}
	
	class OnAddRecordMenuItemClick extends MenuItemClickParent implements OnMenuItemClickListener
	{

		@Override
		public boolean onMenuItemClick(MenuItem item)
		{
			NewRecord();
			return true;
		}

		public OnAddRecordMenuItemClick(Activity activity)
		{
			super(activity);
		}
	}
	
	class LocalHostInfo implements HostInfo
	{

		@Override
		public String getHostName() {
			return "127.0.0.1";
		}
		
	}
	
	class OnSendSelectedRecordMenuItemClick extends MenuItemClickParent implements OnMenuItemClickListener
	{
		private Activity _activity;
		public OnSendSelectedRecordMenuItemClick(Activity activity)
		{
			super(activity);
			_activity = activity;

		}

		@Override
		public boolean onMenuItemClick(MenuItem item)
		{
			SendSelectRecord();
			
			return true;
		}
	}
	
	class OnDeleteRecordMenuItemClick extends MenuItemClickParent implements OnMenuItemClickListener
	{
		public OnDeleteRecordMenuItemClick(Activity activity)
		{
			super(activity);
			// TODO Auto-generated constructor stub
		}

		@Override
		public boolean onMenuItemClick(MenuItem item)
		{
			DelRecord();
			
			return true;
		}
		
	}
	
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		int position = (Integer) buttonView.getTag(); // <-- get the position
		if (isChecked)
			_checkedEvents.add(position);
		else
		{
			_checkedEvents.remove(position);
		}	
		layout.setVisibility(_checkedEvents.isEmpty()?View.GONE:View.VISIBLE);
		
	}
	
	private class EventlistAdapter extends BaseAdapter {
		private List< Map<String, String> > _eventList;
		private AllCalendarEvents _ace;
		
		public EventlistAdapter(List<Map<String, String>> eventList, AllCalendarEvents ace) {
			_ace = ace;
			_eventList = eventList;
		}
		
		public void setEvents(List< Map<String, String> >  eventList) {
			_eventList = eventList;
			notifyDataSetChanged();
		}
		
		@Override
		public int getCount() {
			return _eventList.size();
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			if(convertView == null) {
				convertView = _ace.getLayoutInflater().inflate(R.layout.event_item, null);
				holder = new ViewHolder();
				holder.eventBox = (CheckBox) convertView.findViewById(R.id.calendarEventCtx);
				holder.eventBox.setOnCheckedChangeListener(_ace);
				holder.ctText = (TextView) convertView.findViewById(R.id.eventContent);	
				convertView.setTag(holder);
			}
			else
			{
				// Get the ViewHolder back to get fast access to the TextView
				// and the CheckBox.
				holder = (ViewHolder) convertView.getTag();
			}
			holder.eventBox.setTag(position);
			holder.ctText.setText(_eventList.get(position).get("content"));
			holder.eventBox
			.setChecked(_ace._checkedEvents.contains(position));
			return convertView;
		}

		@Override
		public Object getItem(int arg0) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	
	private class ViewHolder {
		CheckBox eventBox;
		TextView ctText;
	}

    public void NewRecord(){
    	java.util.Calendar cal = java.util.Calendar.getInstance();
		int Hour = cal.get(Calendar.HOUR_OF_DAY);
		int Minute = cal.get(Calendar.MINUTE);
		cal_begin.set(year, month, day, Hour, Minute);
		cal_end.set(year, month, day, Hour + 1, Minute);
		Intent intent = new Intent(Intent.ACTION_EDIT); 
	    intent.setType("vnd.android.cursor.item/event"); 
	    intent.putExtra("beginTime", cal_begin.getTimeInMillis());
	    intent.putExtra("endTime", cal_end.getTimeInMillis());
	    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK 
	            | Intent.FLAG_ACTIVITY_SINGLE_TOP 
	            | Intent.FLAG_ACTIVITY_CLEAR_TOP 
	            | Intent.FLAG_ACTIVITY_NO_HISTORY 
	            | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET); 
	    startActivity(intent); 
    }
    
    public void DelRecord()  {

    	if(_checkedEvents.isEmpty())
		{
    		return;
		}
		Iterator<Integer> itr = _checkedEvents.iterator();
		//	int checkedSize = _checkedEvents.size();
	
		while (itr.hasNext())
		{
			Map<String, String> eventItem = eventList.get(itr.next());
			int event_id = Integer.valueOf(eventItem.get("event_id"));	
			DeleteCalendarEntry(event_id);
		}
		onRestart();
    	if(eventList.isEmpty())
			layout.setVisibility(View.INVISIBLE);
    }
	
	private void DeleteCalendarEntry(int event_id) 
	{  
		Uri eventUri = Uri.parse(calendarURIBase + "/events/" + event_id);  
		getContentResolver().delete(eventUri, null, null);     
    } 
    
    public void SendSelectRecord(){


		net.fortuna.ical4j.model.Calendar ical = new net.fortuna.ical4j.model.Calendar();

		ical.getProperties().add(new ProdId("-//Ben Fortuna//iCal4j 1.0//EN"));
		ical.getProperties().add(Version.VERSION_2_0);
		ical.getProperties().add(CalScale.GREGORIAN);

		Iterator<Integer> itr = _checkedEvents.iterator();
		// int checkedSize = _checkedEvents.size();

		LocalHostInfo localInfo = new LocalHostInfo();
		UidGenerator ug = new UidGenerator(localInfo, "1");
		String[] icaFilePaths = new String[_checkedEvents.size()];
		int i = 0;
		String fileName = null;
		while (itr.hasNext())
			try
			{
				Map<String, String> eventItem = eventList.get(itr.next());

				String name = eventItem.get("title");
				String beginTime = eventItem.get("beginTime");
				String endTime = eventItem.get("endTime");
				String allDay = eventItem.get("allDay");
				String location = eventItem.get("location");
				String summary = eventItem.get("description");
				String timezone = eventItem.get("timezone");
				String rrule = eventItem.get("rrule");

				fileName = name;

				VEvent new_event = null;
				if (allDay.equals("1"))
				{
					new_event = new VEvent(new Date(new java.util.Date(Long
							.valueOf(beginTime))), new Date(new java.util.Date(Long
							.valueOf(endTime))), summary);
					// Parameter a =
					// new_event.getProperties().getProperty(Property.DTSTART).getParameter("VALUE");
				}
				else
				{
					new_event = new VEvent(new DateTime(Long.valueOf(beginTime)),
											new DateTime(Long.valueOf(endTime)), summary);
				}
				new_event.getProperties().add(ug.generateUid());
				new_event.getProperties().add(new Location(location));
				new_event.getProperties().add(new Name(name));
				new_event.getProperties().add(new TzName(timezone));
				try
				{
					if (rrule != null)
					{
						new_event.getProperties().add(new RRule(rrule));
					}
				}
				catch (ParseException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				ical.getComponents().add(new_event);
				try
				{
					File file = new File(Environment.getExternalStorageDirectory(),
											fileName + ".ics");
					file.createNewFile();
					FileOutputStream fout = new FileOutputStream(file);

					CalendarOutputter outputter = new CalendarOutputter();
					outputter.output(ical, fout);
					fout.close();

					icaFilePaths[i++] = Environment.getExternalStorageDirectory() + "/"
										+ fileName + ".ics";

				}
				catch (FileNotFoundException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (ValidationException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			catch (NumberFormatException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		Intent intent = new Intent(AllCalendarEvents.this, Composer.class);
		intent.putExtra("icaFilePaths", icaFilePaths);
		startActivity(intent);
		// if(fileName == null)
		// {
		// return;
		// }
		//
		
	
    }
}
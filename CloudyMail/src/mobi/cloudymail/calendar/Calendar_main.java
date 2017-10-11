package mobi.cloudymail.calendar;

import java.text.SimpleDateFormat;

import mobi.cloudymail.mailclient.R;
import mobi.cloudymail.util.Utils;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.LinearLayout;
import android.widget.TextView;


public class Calendar_main extends Activity
{
	private java.util.Calendar cal_begin = java.util.Calendar.getInstance();
	private java.util.Calendar cal_end = java.util.Calendar.getInstance();
	
	public CalendarView calendarView;	
	private AlertDialog.Builder builder;
	private AlertDialog adMyDate;
	public static Activity activity;

	public static MediaPlayer mediaPlayer;
	public static Vibrator vibrator;
	public int screenDensity;



	public ScrollLayout root=null;
	
	public static final String FIRST_INTENT_TAG = "first";
	public static final String SECOND_INTENT_TAG = "second";
	public static final String THIRD_INTENT_TAG = "third";
	public static final int FIRST_VIEW = 0;
	public static final int SECOND_VIEW = 1;
	public static final int THIRD_VIEW = 2;
	public CalendarView mFirstView, mSecondView, mThirdView;
	
	    
	    public Handler mHandler = new Handler(){
	    	@Override
	    	public void handleMessage(Message msg) {
	    		
	    		super.handleMessage(msg);
	    		switch (msg.what) {
				case FIRST_VIEW:
					break;
				case SECOND_VIEW:
					break;
					
				case THIRD_VIEW:
					break;
				default:
					break;
				}
	    	}
	    };
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if (activity == null)
		{
			activity = this;
		}
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
//		RelativeLayout mainLayout = (RelativeLayout) getLayoutInflater().inflate(
//				R.layout.calendar_main, null);
//		
//		setContentView(mainLayout);
		setContentView(R.layout.calendar_main);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.calendar_titlebar);
//		calendarView = new CalendarView(this);
//		mainLayout.addView(calendarView);
      
		root =(ScrollLayout) findViewById(R.id.ScrollLayout);
		mFirstView=new CalendarView(this);
		mSecondView=new CalendarView(this);
		
		mThirdView=new CalendarView(this);
//		mFirstView.draw_which=ScrollLayout.DRAW_PRE;
//		mThirdView.draw_which=ScrollLayout.DRAW_NEXT;
		mFirstView.setTag(FIRST_INTENT_TAG);
		mSecondView.setTag(SECOND_INTENT_TAG);
		mThirdView.setTag(THIRD_INTENT_TAG);
		root.addView(mFirstView);
		root.addView(mSecondView);
		root.addView(mThirdView);
		
		CalendarView view=(CalendarView)root.getCurScreen();
		java.util.Calendar calendar = java.util.Calendar.getInstance();

		view.ce.grid.currentYear = calendar.get(java.util.Calendar.YEAR);
		view.ce.grid.currentMonth = calendar.get(java.util.Calendar.MONTH);
		view.ce.grid.currentDay = calendar.get(java.util.Calendar.DATE);
		
		root.reDrawChildView(view);
		calendarView.invalidate();
		Button bt = (Button)findViewById(R.id.add_event);
		bt.setOnClickListener(new OnAddRecordButtonClick());
		updateWindowTitle();

		//ª≠±≥æ∞Õºcalendar1_02
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		screenDensity=metrics.densityDpi;
		                                                
				
	}
	@Override
	protected void onResume()
	{
		updateWindowTitle();
		CalendarView view=(CalendarView)root.getCurScreen();
		view.ce.grid.currentYear=calendarView.ce.grid.currentYear;
		view.ce.grid.currentMonth=calendarView.ce.grid.currentMonth;
		view.ce.grid.currentDay=calendarView.ce.grid.currentDay;
		root.reDrawChildView(view);
		calendarView.invalidate();
		super.onResume();
	}

	class MenuItemClickParent
	{
		protected Activity activity;

		public MenuItemClickParent(Activity activity)
		{
			this.activity = activity;
		}
	}

	class OnEventMenuItemClick extends MenuItemClickParent implements
			OnMenuItemClickListener
	{

		public OnEventMenuItemClick(Activity activity)
		{
			super(activity);

		}

		@Override
		public boolean onMenuItemClick(MenuItem item)
		{
			Intent intent = new Intent(activity, AllCalendarEvents.class);
			intent.putExtra("year", calendarView.ce.grid.currentYear);
			intent.putExtra("month", calendarView.ce.grid.currentMonth);
			intent.putExtra("day", calendarView.ce.grid.currentDay1);
			activity.startActivity(intent);
			return true;
		}

	}
	
	class OnAddRecordButtonClick implements android.view.View.OnClickListener
	{
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			java.util.Calendar cal = java.util.Calendar.getInstance();
			int Hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
			int Minute = cal.get(java.util.Calendar.MINUTE);
			cal_begin.set(calendarView.ce.grid.currentYear,
					      calendarView.ce.grid.currentMonth, 
					      calendarView.ce.grid.currentDay1,
					      Hour, Minute);
			cal_end.set(calendarView.ce.grid.currentYear,
						calendarView.ce.grid.currentMonth, 
						calendarView.ce.grid.currentDay, 
						Hour + 1, Minute);
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
	}
	class OnTodayMenuItemClick extends MenuItemClickParent implements
			OnMenuItemClickListener
	{

		public OnTodayMenuItemClick(Activity activity)
		{
			super(activity);

		}

		@Override
		public boolean onMenuItemClick(MenuItem item)
		{
			CalendarView view=(CalendarView)root.getCurScreen();
			java.util.Calendar calendar = java.util.Calendar.getInstance();

			view.ce.grid.currentYear = calendar.get(java.util.Calendar.YEAR);
			view.ce.grid.currentMonth = calendar.get(java.util.Calendar.MONTH);
			view.ce.grid.currentDay = calendar.get(java.util.Calendar.DATE);
			
			root.reDrawChildView(view);
			calendarView.invalidate();

			return true;
		}

	}

	class OnMyDateMenuItemClick extends MenuItemClickParent implements
			OnMenuItemClickListener, OnClickListener, OnDateChangedListener
	{
		private DatePicker dpSelectDate;
		private LinearLayout myDateLayout;
		private TextView tvDate;
		private TextView tvLunarDate;

		public OnMyDateMenuItemClick(Activity activity)
		{
			super(activity);
			myDateLayout = (LinearLayout) getLayoutInflater().inflate(
					R.layout.mydate, null);
			dpSelectDate = (DatePicker) myDateLayout
					.findViewById(R.id.dpSelectDate);

		}

		@Override
		public void onDateChanged(DatePicker view, int year, int monthOfYear,
				int dayOfMonth)
		{

			SimpleDateFormat sdf = Utils.dateFmt;// new SimpleDateFormat("yyyyƒÍM‘¬d»’");
			java.util.Calendar calendar = java.util.Calendar.getInstance();
			calendar.set(year, monthOfYear, dayOfMonth);
			if (tvDate != null)
				tvDate.setText(sdf.format(calendar.getTime()));
			else
				adMyDate.setTitle(sdf.format(calendar.getTime()));

			java.util.Calendar calendar1 = java.util.Calendar.getInstance();
			if (calendar1.get(java.util.Calendar.YEAR) == year
					&& calendar1.get(java.util.Calendar.MONTH) == monthOfYear
					&& calendar1.get(java.util.Calendar.DATE) == dayOfMonth)
			{
				String today = getResources().getString(R.string.today);
				if (tvDate != null)
					tvDate.setText(tvDate.getText() + today);
				else
					adMyDate.setTitle(sdf.format(calendar.getTime()) + today);
			}

			if (tvLunarDate == null)
				return;
			
		}

		@Override
		public void onClick(DialogInterface dialog, int which)
		{
			CalendarView view=(CalendarView)root.getCurScreen();
			view.ce.grid.currentYear = dpSelectDate.getYear();
			view.ce.grid.currentMonth = dpSelectDate.getMonth();
			view.ce.grid.currentDay = dpSelectDate.getDayOfMonth();
			root.reDrawChildView(view);
			calendarView.invalidate();

		}

		@Override
		public boolean onMenuItemClick(MenuItem item)
		{
			Resources res = getResources();
			// Create a builder
			builder = new AlertDialog.Builder(activity);
			builder.setTitle(res.getString(R.string.assign_date));

			myDateLayout = (LinearLayout) getLayoutInflater().inflate(
					R.layout.mydate, null);
			dpSelectDate = (DatePicker) myDateLayout
					.findViewById(R.id.dpSelectDate);
			tvDate = (TextView) myDateLayout.findViewById(R.id.tvDate);
			tvLunarDate = (TextView) myDateLayout
					.findViewById(R.id.tvLunarDate);

			dpSelectDate.init(calendarView.ce.grid.currentYear,
					calendarView.ce.grid.currentMonth,
					calendarView.ce.grid.currentDay, this);

			builder.setView(myDateLayout);

			builder.setPositiveButton(res.getString(R.string.ok), this);
			builder.setNegativeButton(res.getString(R.string.cancel), null);
			builder.setIcon(R.drawable.calendar_small);
			adMyDate = builder.create();
			onDateChanged(dpSelectDate, dpSelectDate.getYear(), dpSelectDate
					.getMonth(), dpSelectDate.getDayOfMonth());
			adMyDate.show();

			return true;
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		
        
		MenuItem miToday = menu.add(0, 1, 0, R.string.today);
		MenuItem miMyDate = menu.add(0, 2, 0, R.string.assign_date);
		MenuItem miEvent = menu.add(0, 3, 0, R.string.check_event);	
		
		miToday.setIcon(R.drawable.clock);
		miToday.setOnMenuItemClickListener(new OnTodayMenuItemClick(this));
		miMyDate.setIcon(R.drawable.calendar_small);
		miMyDate.setOnMenuItemClickListener(new OnMyDateMenuItemClick(this));
		miEvent.setIcon(R.drawable.diary);
		miEvent.setOnMenuItemClickListener(new OnEventMenuItemClick(this));

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		calendarView.onKeyDown(keyCode, event);
		return super.onKeyDown(keyCode, event);
	}
	private void updateWindowTitle()
	{
		TextView txtView=(TextView)findViewById(R.id.calendartitle);
		txtView.setText(R.string.app_name);
	}
}
package mobi.cloudymail.calendar;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import mobi.cloudymail.mailclient.R;
import mobi.cloudymail.util.MyApp;
import mobi.cloudymail.util.Utils;

import org.apache.commons.lang3.time.DateUtils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.view.View;

public class Grid extends CalendarParent implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String[] days = new String[42];
	private String[] Chinesedays = new String[42];
	// true表示有记录，false表示没有记录
	private boolean[] recordDays = new boolean[42];
	private int dayColor;
	private int innerGridColor;
	private int prevNextMonthDayColor;
	private int gregorianDayColor;
	private int sundaySaturdayPrevNextMonthDayColor;
	private int eventSymbolColor;
	private float currentDaySize;
	private float cellX = -1, cellY = -1;
	ChineseCalendarGB chineseCalendar=new ChineseCalendarGB();
	ChineseCalendarGB calendarGB=new ChineseCalendarGB();

	// 从0开始
	private int currentRow, currentCol;
	private boolean redrawForKeyDown = false;

	// 当前年和月
	public int currentYear, currentMonth;
	// 上月或下月选中的天
	public int currentDay = -1, currentDay1 = -1, currentDayIndex = -1;
	private java.util.Calendar calendar = java.util.Calendar.getInstance();

	private java.util.Calendar cal_begin = java.util.Calendar.getInstance();
	private java.util.Calendar cal_end = java.util.Calendar.getInstance();
	
	private static String calendarURIBase = "";
	
	
	
	static{
		if (Integer.parseInt(Build.VERSION.SDK) >= 8) {
			calendarURIBase = "content://com.android.calendar";
		} else {
			calendarURIBase = "content://calendar";
		}
	}
	
	public void setCurrentRow(int currentRow)
	{
		if (currentRow < 0)
		{
			currentMonth--;
			if (currentMonth == -1)
			{
				currentMonth = 11;
				currentYear--;
			}
			currentDay = getMonthDays(currentYear, currentMonth) + currentDay
					- 7;
			currentDay1 = currentDay;
			cellX = -1;
			cellX = -1;
			view.invalidate();
			return;

		}
		else if (currentRow > 5)
		{
			int n = 0;
			for (int i = 35; i < days.length; i++)
			{
				if (!days[i].startsWith("*"))
					n++;
				else
					break;
			}
			currentDay = 7 - n + currentCol + 1;
			currentDay1 = currentDay;
			currentMonth++;
			if (currentMonth == 12)
			{
				currentMonth = 0;
				currentYear++;
			}
			cellX = -1;
			cellX = -1;
			view.invalidate();
			return;
		}
		this.currentRow = currentRow;
		redrawForKeyDown = true;
		view.invalidate();
	}

	private void getRecordDays()
	{
		int beginDayIndex = 0;
		for (int i = 0; i < recordDays.length; i++)
			recordDays[i] = false;
		for (int i = 0; i < days.length; i++)
		{
			if (!days[i].startsWith("*"))
			{
		
				beginDayIndex = i;
				break;
			}
		}
		
		ContentResolver contentResolver = MyApp.instance().getContentResolver();
		Uri.Builder builderCurrent = Uri.parse(calendarURIBase + "/instances/when").buildUpon();
			
		cal_begin.set(currentYear, currentMonth, 1, 0, 0, 0);
		long begin = cal_begin.getTimeInMillis();
		ContentUris.appendId(builderCurrent, begin); // now -
														// DateUtils.DAY_IN_MILLIS
														// * 10000);

		cal_end.set(currentYear, currentMonth, getMonthDays(currentYear, currentMonth), 23, 59, 59);
		long end = cal_end.getTimeInMillis();
		ContentUris.appendId(builderCurrent, end);// now +
													// DateUtils.DAY_IN_MILLIS *
													// 10000);
		Cursor eventCursor = contentResolver
				.query(	builderCurrent.build(), new String[] { "event_id", "title", "dtstart",
																"dtend", "allDay", "duration" },
						null, null, "startDay ASC, startMinute ASC");

		java.util.Calendar temp = java.util.Calendar.getInstance();
		while (eventCursor.moveToNext())
		{
			Long beginTime = Long.valueOf(eventCursor.getString(2));
			temp.setTimeInMillis(beginTime);
			int begin_day = temp.get(Calendar.DAY_OF_MONTH);
			
			Long endTime = null;
			if (eventCursor.getString(3) == null)
			{
				String during = eventCursor.getString(5).substring(1, eventCursor.getString(5).length() - 2);
				endTime = Long.valueOf(during) * 1000 + beginTime;
			}
			else
			{
				endTime = Long.valueOf(eventCursor.getString(3));
			}
			if (eventCursor.getString(4).equals("1"))
			{
				endTime = endTime - DateUtils.MILLIS_PER_DAY;
			}
			temp.setTimeInMillis(endTime);
			int end_day = temp.get(Calendar.DAY_OF_MONTH);
			int end_hour = temp.get(Calendar.HOUR_OF_DAY);
			int end_min = temp.get(Calendar.MINUTE);
			if ((end_hour == 0) && (end_min == 0))
			{
				end_day--;
			}
			for (int day = begin_day; day <= end_day; day++)
			{
				recordDays[beginDayIndex + day - 1] = true;
			}
		}
		closeEventCursor(eventCursor);
		if (days[0].startsWith("*"))
		{
			int prevYear = currentYear, prevMonth = currentMonth - 1;
			if (prevMonth == -1)
			{
				prevMonth = 11;
				prevYear--;
			}
			int minDay = Integer.parseInt(days[0].substring(1));

			Uri.Builder builderPre = Uri.parse(calendarURIBase + "/instances/when").buildUpon();
			cal_begin.set(prevYear, prevMonth, minDay, 0, 0, 0);
			begin = cal_begin.getTimeInMillis();
			ContentUris.appendId(builderPre, begin); // now -
														// DateUtils.DAY_IN_MILLIS
														// * 10000);

			cal_end.set(prevYear, prevMonth, getMonthDays(prevYear, prevMonth), 23, 59, 59);
			end = cal_end.getTimeInMillis();
			ContentUris.appendId(builderPre, end);// now +
													// DateUtils.DAY_IN_MILLIS *
													// 10000);

			eventCursor = contentResolver.query(builderPre.build(), new String[] { "event_id",
																					"title",
																					"dtstart",
																					"dtend",
																					"allDay" },
												null, null, "startDay ASC, startMinute ASC");

			temp = java.util.Calendar.getInstance();
			while (eventCursor.moveToNext())
			{
				Long beginTime = Long.valueOf(eventCursor.getString(2));
				temp.setTimeInMillis(beginTime);
				int begin_day = temp.get(Calendar.DAY_OF_MONTH);

				Long endTime = Long.valueOf(eventCursor.getString(3));
				if (eventCursor.getString(4).equals("1"))
				{
					endTime = endTime - DateUtils.MILLIS_PER_DAY;
				}
				temp.setTimeInMillis(endTime);
				int end_day = temp.get(Calendar.DAY_OF_MONTH);

				for (int day = begin_day; day <= end_day; day++)
				{
					recordDays[day - minDay] = true;
				}
			}
			
		}
		closeEventCursor(eventCursor);
		if (days[days.length - 1].startsWith("*"))
		{
			int nextYear = currentYear, nextMonth = currentMonth + 1;
			if (nextMonth == 12)
			{
				nextMonth = 0;
				nextYear++;
			}
			
			int maxDay = Integer.parseInt(days[days.length - 1].substring(1));
		
			Uri.Builder builderNext = Uri.parse(calendarURIBase + "/instances/when").buildUpon();
			cal_begin.set(nextYear, nextMonth, 1, 0, 0, 0);
			begin = cal_begin.getTimeInMillis();
			ContentUris.appendId(builderNext, begin); // now -
														// DateUtils.DAY_IN_MILLIS
														// * 10000);

			cal_end.set(nextYear, nextMonth, maxDay, 23, 59, 59);
			end = cal_end.getTimeInMillis();
			ContentUris.appendId(builderNext, end);// now +
													// DateUtils.DAY_IN_MILLIS *
													// 10000);

			eventCursor = contentResolver.query(builderNext.build(), 
			                			               new String[] { "event_id", "title", "dtstart", "dtend", "allDay"}, null, 
			                			               null, "startDay ASC, startMinute ASC");  
		
			temp = java.util.Calendar.getInstance();
			while (eventCursor.moveToNext())
			{
				Long beginTime = Long.valueOf(eventCursor.getString(2));
				temp.setTimeInMillis(beginTime);
				int begin_day = temp.get(Calendar.DAY_OF_MONTH);
				
				Long endTime = Long.valueOf(eventCursor.getString(3));
				if (eventCursor.getString(4).equals("1"))
				{
					endTime = endTime - DateUtils.MILLIS_PER_DAY;
				}
				temp.setTimeInMillis(endTime);
				int end_day = temp.get(Calendar.DAY_OF_MONTH);

				for (int day = begin_day; day <= end_day; day++)
				{
					recordDays[days.length - (maxDay - day) - 1] = true;
				}
			}
		}
		closeEventCursor(eventCursor);
	}
	private void closeEventCursor(Cursor cursor)
	{
		if(cursor!=null&&!cursor.isClosed())
			cursor.close();
	}
	public void setCurrentCol(int currentCol)
	{
		if (currentCol < 0)
		{
			if (currentRow == 0)
			{

				currentMonth--;

				if (currentMonth == -1)
				{
					currentMonth = 11;
					currentYear--;
				}
				currentDay = getMonthDays(currentYear, currentMonth);
				currentDay1 = currentDay;
				cellX = -1;
				cellX = -1;
				view.invalidate();
				return;
			}

			else
			{
				currentCol = 6;
				setCurrentRow(--currentRow);

			}
		}
		else if (currentCol > 6)
		{
			currentCol = 0;
			setCurrentRow(++currentRow);

		}
		this.currentCol = currentCol;
		redrawForKeyDown = true;
		view.invalidate();
	}

	public int getCurrentRow()
	{
		return currentRow;
	}

	public int getCurrentCol()
	{
		return currentCol;
	}

	public void setCellX(float cellX)
	{

		this.cellX = cellX;
	}

	public void setCellY(float cellY)
	{

		this.cellY = cellY;
	}

	private int getMonthDays(int year, int month)
	{
		month++;
		switch (month)
		{
			case 1:
			case 3:
			case 5:
			case 7:
			case 8:
			case 10:
			case 12:
			{
				return 31;
			}
			case 4:
			case 6:
			case 9:
			case 11:
			{
				return 30;
			}
			case 2:
			{
				if (((year % 4 == 0) && (year % 100 != 0)) || (year % 400 == 0))
					return 29;
				else
					return 28;
			}
		}
		return 0;
	}

	private void calculateDays()
	{
		calendar.set(currentYear, currentMonth, 1);

		int week = calendar.get(java.util.Calendar.DAY_OF_WEEK);
		int monthDays = 0;
		int prevMonthDays = 0;
		int year=0;
		int month=0;
		int year2=0;
		int month2=0;
		monthDays = getMonthDays(currentYear, currentMonth);
	
		if (currentMonth == 0)//一月
		{
			prevMonthDays = getMonthDays(currentYear - 1, 11);
			year=currentYear - 1;
			month=12;
		}
			
		else
		{
			prevMonthDays = getMonthDays(currentYear, currentMonth-1);
			year=currentYear;
			month=currentMonth;
		}
		if (currentMonth == 11)//12月
		{
//			nextMonthDays = getMonthDays(currentYear +1, 0);
			year2=currentYear + 1;
			month2=1;
		}
			
		else
		{
//			nextMonthDays = getMonthDays(currentYear, currentMonth);
			year2=currentYear;
			month2=currentMonth+2;
		}

		for (int i = week, day = prevMonthDays; i > 1; i--, day--)
		{
			days[i - 2] = "*" + String.valueOf(day);
			chineseCalendar.setGregorian(year, month, day);
		    chineseCalendar.computeChineseFields();
			Chinesedays[i-2]=chineseCalendar.getChineseMonth()+","+chineseCalendar.getChineseDate()+"";
		}
		for (int day = 1, i = week - 1; day <= monthDays; day++, i++)
		{
			days[i] = String.valueOf(day);
			chineseCalendar.setGregorian(currentYear, currentMonth+1, day);
		    chineseCalendar.computeChineseFields();
			Chinesedays[i]=chineseCalendar.getChineseMonth()+","+chineseCalendar.getChineseDate()+"";
			if (day == currentDay)
			{
				currentDayIndex = i;

			}
		}
		for (int i = week + monthDays - 1, day = 1; i < days.length; i++, day++)
		{
			days[i] = "*" + String.valueOf(day);
			
			chineseCalendar.setGregorian(year2, month2, day);
		    chineseCalendar.computeChineseFields();
			Chinesedays[i]=chineseCalendar.getChineseMonth()+","+chineseCalendar.getChineseDate()+"";
			
		}

	}

	public Grid(Activity activity, View view)
	{
		super(activity, view);

//		tvMsg1 = (TextView) activity.findViewById(R.id.tvMsg1);
//		tvMsg2 = (TextView) activity.findViewById(R.id.tvMsg2);
		dayColor = activity.getResources().getColor(R.color.day_color);
		gregorianDayColor=activity.getResources().getColor(R.color.gregorian_color);
		innerGridColor = activity.getResources().getColor(
				R.color.inner_grid_color);
		prevNextMonthDayColor = activity.getResources().getColor(
				R.color.prev_next_month_day_color);
		sundaySaturdayPrevNextMonthDayColor = activity.getResources().getColor(
				R.color.sunday_saturday_prev_next_month_day_color);
		eventSymbolColor=activity.getResources().getColor(R.color.event_symbol_color);
	//	daySize = activity.getResources().getDimension(R.dimen.day_size);
	//	dayTopOffset = activity.getResources().getDimension(
	//			R.dimen.day_top_offset);
		currentDaySize = activity.getResources().getDimension(
				R.dimen.current_day_size);
		paint.setColor(activity.getResources().getColor(R.color.border_color));

		currentYear = calendar.get(java.util.Calendar.YEAR);
		currentMonth = calendar.get(java.util.Calendar.MONTH);
	}

	private boolean isCurrentDay(int dayIndex, int currentDayIndex,
			Rect cellRect)
	{
		boolean result = false;
		if (redrawForKeyDown == true)
		{
			result = dayIndex == (7 * ((currentRow > 0) ? currentRow : 0) + currentCol);
			if (result)
				redrawForKeyDown = false;

		}
		else if (cellX != -1 && cellY != -1)
		{
			if (cellX >= cellRect.left && cellX <= cellRect.right
					&& cellY >= cellRect.top && cellY <= cellRect.bottom)
			{
				result = true;
			}
			else
			{
				result = false;
			}
		}
		else
		{
			result = (dayIndex == currentDayIndex);

		}
		if (result)
		{
			if (currentRow > 0 && currentRow < 6)
			{
				currentDay1 = currentDay;

			}
			currentDayIndex = -1;
			cellX = -1;
			cellY = -1;

		}
		return result;
	}

	// 更新当前日期的信息
	private String updateMsg(boolean today)
	{
		String dateString = "";
		SimpleDateFormat sdf = Utils.dateFmt;//new SimpleDateFormat("yyyy年M月d日");
		java.util.Calendar calendar = java.util.Calendar.getInstance();
		calendar.set(currentYear, currentMonth, currentDay);

		dateString = sdf.format(calendar.getTime());
		if (today)
			dateString += "("+(Utils.isInChinese()? "今天" : "Today") + ")";
		
//		tvMsg2.setText(dateString);
		return dateString;

	}

	public boolean inBoundary()
	{
		if (cellX < borderMargin
				|| cellX > (view.getMeasuredWidth() - borderMargin)
				|| cellY < top
				|| cellY > (view.getMeasuredHeight() - borderMargin))
		{
			return false;
		}
		else
		{
			return true;
		}
	}

	float top, left;

	@Override
	public void draw(Canvas canvas, double scale)
	{
		  
	    left = (int)(borderMargin*2*scale);
//		top = borderMargin + weekNameSize+borderMargin*3;
	    top =(int)((borderMargin*(3/2)+dayTopOffset+daySize+gregorianDaySize+borderMargin*5)*scale); 
		float calendarWidth = view.getMeasuredWidth() - left * 2;    
		float calendarHeight =(float)((6*(dayTopOffset+daySize+gregorianDaySize+borderMargin/2)+borderMargin/3)*scale); 
		float cellWidth = calendarWidth / 7;   
		float cellHeight = calendarHeight / 6 ;   
		int viewWidth = view.getWidth();
		paint.setColor(innerGridColor);
		
		// 画日期
		calculateDays();
		
		java.util.Calendar calendar = java.util.Calendar.getInstance();
		int day = calendar.get(java.util.Calendar .DATE);
		int myYear = calendar.get(java.util.Calendar.YEAR);
		int myMonth = calendar.get(java.util.Calendar.MONTH);
		
		calendar.set(myYear,myMonth,1);
		int week = calendar.get(java.util.Calendar.DAY_OF_WEEK);
		int todayIndex = week + day - 2;
		boolean today = false;
		if (currentDayIndex == -1)
		{
			currentDayIndex = todayIndex;

		}
		boolean flag = false;
		try
		{
			getRecordDays();
		}
		catch (Exception e)
		{
			// TODO: handle exception
		}
		
		for (int i = 0; i < days.length; i++)
		{
			today = false;
			int row = i / 7;
			int col = i % 7;
			String text = String.valueOf(days[i]);
			if ((i % 7 == 0 || (i - 6) % 7 == 0) && text.startsWith("*"))
			{
				paint.setColor(sundaySaturdayPrevNextMonthDayColor);
			}
			else if (i % 7 == 0 || (i - 6) % 7 == 0)
			{
				paint.setColor(sundaySaturdayColor);
			}
			else if (text.startsWith("*"))
			{
				paint.setColor(prevNextMonthDayColor);
			}
			else
			{
				paint.setColor(dayColor);
			}
			text = text.startsWith("*") ? text.substring(1) : text;
			
		    Rect dst = new Rect();
//			if(viewWidth > 480)
//			{
//				dst.left = (int) (left + cellWidth * col) ;
//				dst.top = (int) (top + cellHeight * row);
//				dst.bottom = (int) (dst.top + cellHeight + 1);
//				dst.right = (int) (dst.left + cellWidth )+2;
//			}
			
			
			dst.left = (int) (left + cellWidth * col) ;
			dst.top = (int) (top + cellHeight * row);
			dst.bottom = (int) (dst.top + cellHeight + 1);
			dst.right = (int) (dst.left + cellWidth )-1;
			
			String myText = text;
			
			paint.setTextSize((int)(daySize*scale));
			float textLeft = left + cellWidth * col
					+ (cellWidth - paint.measureText(myText)) / 2;
			float textTop = top + cellHeight * row
					+ (cellHeight - paint.getTextSize()) / 2 + dayTopOffset;
			if (myYear == currentYear && myMonth == currentMonth
					&& i == todayIndex)
			{
				today = true;
			}

			if (isCurrentDay(i, currentDayIndex, dst) && flag == false)
			{
				if (days[i].startsWith("*"))
				{
					// 下月
					if (i > 20)
					{
						currentMonth++;
						if (currentMonth == 12)
						{
							currentMonth = 0;
							currentYear++;
						}
						
						view.invalidate();

					}
					// 上月
					else
					{
						currentMonth--;
						if (currentMonth == -1)
						{
							currentMonth = 11;
							currentYear--;
						}
						view.invalidate();

					}
					currentDay = Integer.parseInt(text);
					currentDay1 = currentDay;
					cellX = -1;
					cellY = -1;
					break;

				}
				else
				{
					paint.setTextSize((int)(currentDaySize*scale));
					flag = true;
					Bitmap bitmap = BitmapFactory.decodeResource(activity
							.getResources(), R.drawable.day);
					Rect src = new Rect();
					src.left = 0;
					src.top = 0;
					src.right = bitmap.getWidth();
					src.bottom = bitmap.getHeight();
					canvas.drawBitmap(bitmap, src, dst, paint);
//					paint.setColor(currentDayColor);
					currentCol = col;
					currentRow = row;
					currentDay = Integer.parseInt(text);
					currentDay1 = currentDay;
			

				}
			}
//			paint.setTypeface(Typeface.SANS_SERIF);
			paint.setAntiAlias(true);
			canvas.drawText(myText, textLeft, textTop, paint);
			
		    paint.setColor(gregorianDayColor);
		    paint.setTextSize((int)(gregorianDaySize*scale));
		    if(Utils.isInChinese())
		    {
		    	String chineseDay=chineseCalendar.getGregorianReturnData(Chinesedays[i]);
			    float posX=left + cellWidth * col
						+ (cellWidth - paint.measureText(chineseDay)) / 2;
			   
				float posY = top+(float)(borderMargin*(3/2)*scale)+ cellHeight * row
						+ (cellHeight - paint.getTextSize()) / 2 + (int)(dayTopOffset*scale);
				canvas.drawText(chineseDay,posX , posY, paint);
		    }
		    
			if (recordDays[i])
			{
//				myText = "*" + myText;
		        Paint symbolPaint = new Paint();
		        symbolPaint.setColor(eventSymbolColor);
		        symbolPaint.setStyle(Paint.Style.FILL); 
		        Path path = new Path();
		        path.moveTo((float)(dst.left+cellWidth*4.0/5), (float)dst.top);
		        path.lineTo((float)dst.right-borderMargin/10,(float)(dst.top+(cellWidth*1.0/5)));
		        path.lineTo((float)dst.right-borderMargin/10,(float)dst.top);
		        path.close(); 
		        canvas.drawPath(path, symbolPaint); 
			}
			if(today)
			{
				Bitmap bitmap = BitmapFactory.decodeResource(activity
				                 							.getResources(), R.drawable.circule);
				Rect src = new Rect();
				src.left = 0;
				src.top = 0;
				src.right = bitmap.getWidth();
			    src.bottom = bitmap.getHeight();
				canvas.drawBitmap(bitmap, src, dst, paint);
//				Paint todayPaint=new Paint();
//				todayPaint.setTextSize(currentDaySize);
//				dst.left += 1;
//				dst.top += 1;
//				todayPaint.setColor(Color.RED);
//				todayPaint.setStyle(Paint.Style.STROKE); 
//				todayPaint.setAntiAlias(true);
//				todayPaint.setStrokeWidth(2.0f);
//				canvas.drawCircle(dst.left+cellWidth/2, dst.top+cellHeight/2, cellWidth/3, todayPaint);
			}
			

		}
		
				
		paint.reset();
		String tvMsgString=updateMsg(today);
		paint.setTextSize((int)(daySize*scale));
		paint.setAntiAlias(true);
		paint.setColor(activity.getResources().getColor(R.color.text_color));
		canvas.drawText(tvMsgString,(view.getMeasuredWidth()-paint.measureText(tvMsgString))/2, (float)(borderMargin*5*scale), paint);
		
//		paint.setColor(activity.getResources().getColor(R.color.border_color));
//		canvas.drawLine(left, top, left + view.getMeasuredWidth()
//				- borderMargin * 2, top, paint);
//		// 画横线
//		for (int i = 1; i < 6; i++)
//		{
//			 canvas.drawLine(left, top + (cellHeight) * i, left +
//			 calendarWidth,
//			 top + (cellHeight) * i, paint);
//		   }
//			// 画竖线
//		    for (int i = 1; i < 7; i++)
//		   {
//			 canvas.drawLine(left + cellWidth * i, top, left + cellWidth * i,
//					 top+calendarHeight, paint);
//		   }
	}
	public String getNextYearMonth(int year,int month)
	{
			if (month == 11)
			{
				year=currentYear + 1;
				month=0;
			}
				
			else
			{
				month=month+1;
			}
		return year+","+month;
	}
	public String getPreYearMonth(int year,int month)
	{
		if (month == 0)
		{
			year=year - 1;
			month=11;
		}
			
		else
		{
			month=month-1;
		}
		return  year+","+month;
	}
	
}

package mobi.cloudymail.calendar;


import mobi.cloudymail.calendar.interfaces.CalendarElement;
import mobi.cloudymail.mailclient.R;
import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

public class CalendarParent implements CalendarElement
{
	protected Activity activity;
	protected View view;
	protected Paint paint = new Paint();
	protected float borderMargin;		
	protected float weekNameMargin;
	protected float weekNameSize;
	protected float gregorianDaySize;
    protected float daySize;
	protected int sundaySaturdayColor;
	protected float dayTopOffset;

    public CalendarParent(Activity activity, View view)
    {    	
    	this.activity = activity;
    	this.view = view;
		borderMargin = activity.getResources().getDimension(
				R.dimen.calendar_border_margin);
        weekNameMargin = activity.getResources().getDimension(R.dimen.weekname_margin);
        weekNameSize=activity.getResources().getDimension(R.dimen.weekname_size);
        sundaySaturdayColor = activity.getResources().getColor(R.color.sunday_saturday_color);
        gregorianDaySize=activity.getResources().getDimension(R.dimen.gregorian_day_size);
        daySize=activity.getResources().getDimension(R.dimen.day_size);
        dayTopOffset=activity.getResources().getDimension(R.dimen.day_top_offset);
    }
    @Override
	public void draw(Canvas canvas,double scale)
	{		
		
	}

}

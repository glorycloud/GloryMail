package mobi.cloudymail.calendar;

import mobi.cloudymail.mailclient.R;
import android.app.Activity;
import android.graphics.Canvas;
import android.view.View;

public class Week extends CalendarParent
{
	private String[] weekNames;
	private int weekNameColor;

	public Week(Activity activity, View view)
	{
		super(activity, view);
		weekNameColor = activity.getResources().getColor(R.color.weekname_color);
		weekNames = activity.getResources().getStringArray(R.array.week_name);
		paint.setTextSize(weekNameSize);
	}

	@Override
	public void draw(Canvas canvas,double scale)
	{

		float left = borderMargin;
		float top = borderMargin*3;
		float everyWeekWidth = (view.getMeasuredWidth() -  borderMargin * 2)/ 7;
	//	float everyWeekHeight = everyWeekWidth;
		
//		paint.setFakeBoldText(true);
		for (int i = 0; i < weekNames.length; i++)
		{
//			if(i == 0 || i == weekNames.length - 1)
//				paint.setColor(sundaySaturdayColor);
//			else
			paint.setColor(weekNameColor);

			left = borderMargin + everyWeekWidth * i
					+ (everyWeekWidth - paint.measureText(weekNames[i])) / 2;
			canvas.drawText(weekNames[i], left, top + paint.getTextSize()+weekNameMargin, paint);
		}

	}

}

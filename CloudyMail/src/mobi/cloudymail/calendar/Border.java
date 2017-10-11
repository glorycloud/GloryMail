package mobi.cloudymail.calendar;

import mobi.cloudymail.mailclient.R;
import android.app.Activity;
import android.graphics.Canvas;
import android.view.View;

public class Border extends CalendarParent
{

	public Border(Activity activity, View view)
	{
		super(activity, view);
		paint.setColor(activity.getResources().getColor(R.color.border_color));
	}

	@Override
	public void draw(Canvas canvas,double scale)
	{
		float left = borderMargin;
		float top = borderMargin;
		float right = view.getMeasuredWidth() - left;
		float bottom=borderMargin + weekNameSize+borderMargin*3+6*(dayTopOffset+daySize+gregorianDaySize+borderMargin/2);
		canvas.drawLine(left, top, right, top, paint);
		canvas.drawLine(right, top, right, bottom, paint);
		canvas.drawLine(right, bottom, left, bottom, paint);
		canvas.drawLine(left, bottom, left, top, paint);

	}

}

package mobi.cloudymail.calendar;

import java.util.ArrayList;

import mobi.cloudymail.calendar.interfaces.CalendarElement;
import android.app.Activity;
import android.graphics.Canvas;
import android.view.View;

public class Calendar extends CalendarParent 
{
	private ArrayList<CalendarElement> elements = new ArrayList<CalendarElement>();
    public Grid grid;
	public Calendar(Activity activity, View view)
	{	
		super(activity,view);
//		elements.add(new Border(activity, view));
//		elements.add(new Week(activity, view));
		grid = new Grid(activity, view);
		elements.add(grid);
	}

	@Override
	public void draw(Canvas canvas,double scale)
	{
		for (CalendarElement ce : elements)
			ce.draw(canvas, scale);
	}

}

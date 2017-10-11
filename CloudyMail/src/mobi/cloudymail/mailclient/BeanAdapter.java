package mobi.cloudymail.mailclient;

import java.util.ArrayList;
import java.util.List;

import mobi.cloudymail.util.Utils;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

abstract class BeanAdapter implements ListAdapter
{
	private List<DataSetObserver> dataObservers = new ArrayList<DataSetObserver>(2);
	private int[] toIds;
	private String[] fromNames;
	/**
	 * 
	 * @param from A list of column names that will be added to the Map associated with each item.
	 * @param to The views that should display column in the "from" parameter. These should all be TextViews. The first N views in this list are given the values of the first N columns in the from parameter. 
	 * @param itemView view to show a list item
	 */
	public BeanAdapter(String[] from, int[] to)
	{
		fromNames = from;
		toIds = to;
	}
	@Override
	public void registerDataSetObserver(DataSetObserver dataSetObserver)
	{
		dataObservers.add(dataSetObserver);
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver dataSetObserver)
	{
		dataObservers.remove(dataSetObserver);
	}

	@Override
	public abstract int getCount();

	@Override
	public abstract Object getItem(int position);

	@Override
	public long getItemId(int pos)
	{
		return pos;
	}

	@Override
	public boolean hasStableIds()
	{
		return true;
	}
	
	public abstract View createItemView(ViewGroup parent);
	public abstract int getViewBackGround(int position);
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		if(convertView == null)
		{
			convertView = createItemView(parent);
			//convertView = activity.getLayoutInflater().inflate(R.layout.mail_item, parent);
		}
		Utils.ASSERT( convertView.getId() == R.layout.mail_item);
		Object m = getItem(position);
		for(int i=0;i<fromNames.length;i++)
		{
			((TextView)convertView.findViewById(toIds[i])).setText(getPropertyValue(m, fromNames[i]));
		}
		
		convertView.setBackgroundColor(getViewBackGround(position));
		return convertView;
	}

	public abstract String getPropertyValue(Object o, String name);
	
	@Override
	public int getItemViewType(int paramInt)
	{
		return 0;
	}

	@Override
	public int getViewTypeCount()
	{
		return 1;
	}

	@Override
	public boolean isEmpty()
	{
		return getCount() == 0;
	}

	@Override
	public boolean areAllItemsEnabled()
	{
		return true;
	}

	@Override
	public boolean isEnabled(int paramInt)
	{
		return true;
	}
	
}
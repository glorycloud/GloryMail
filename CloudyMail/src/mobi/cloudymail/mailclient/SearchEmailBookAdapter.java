package mobi.cloudymail.mailclient;
import static mobi.cloudymail.util.Utils.LOGTAG;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

public class SearchEmailBookAdapter<T> extends BaseAdapter implements
		Filterable

{

	private static final String tag = "SearchEmailBookAdapter";
	private List<T> mObjects;
	private final Object mLock = new Object();
	private int mResource;
	private int mDropDownResource;
	private int mFieldId = 0;
	private boolean mNotifyOnChange = true;
	private Context mContext;
	private ArrayList<T> mOriginalValues;
	private EmailBookArrayFilter mFilter;
	private LayoutInflater mInflater;

	public SearchEmailBookAdapter(Context context, int textViewResourceId)
	{
		init(context, textViewResourceId, 0, new ArrayList<T>());
	}



	public void add(T object)
	{
		if (mOriginalValues != null)
		{
			synchronized (mLock)
			{
				mOriginalValues.add(object);
				if (mNotifyOnChange)
					notifyDataSetChanged();
			}
		}
		else
		{
			mObjects.add(object);
			if (mNotifyOnChange)
				notifyDataSetChanged();
		}
	}

	public void addAll(Collection<? extends T> collection)
	{
		if (mOriginalValues != null)
		{
			synchronized (mLock)
			{
				mOriginalValues.addAll(collection);
				if (mNotifyOnChange)
					notifyDataSetChanged();
			}
		}
		else
		{
			mObjects.addAll(collection);
			if (mNotifyOnChange)
				notifyDataSetChanged();
		}
	}

	public void addAll(T... items)
	{
		if (mOriginalValues != null)
		{
			synchronized (mLock)
			{
				for (T item : items)
				{
					mOriginalValues.add(item);
				}
				if (mNotifyOnChange)
					notifyDataSetChanged();
			}
		}
		else
		{
			for (T item : items)
			{
				mObjects.add(item);
			}
			if (mNotifyOnChange)
				notifyDataSetChanged();
		}
	}

	public void insert(T object, int index)
	{
		if (mOriginalValues != null)
		{
			synchronized (mLock)
			{
				mOriginalValues.add(index, object);
				if (mNotifyOnChange)
					notifyDataSetChanged();
			}
		}
		else
		{
			mObjects.add(index, object);
			if (mNotifyOnChange)
				notifyDataSetChanged();
		}
	}

	public void remove(T object)
	{
		if (mOriginalValues != null)
		{
			synchronized (mLock)
			{
				mOriginalValues.remove(object);
			}
		}
		else
		{
			mObjects.remove(object);
		}
		if (mNotifyOnChange)
			notifyDataSetChanged();
	}

	public void clear()
	{
		if (mOriginalValues != null)
		{
			synchronized (mLock)
			{
				mOriginalValues.clear();
			}
		}
		else
		{
			mObjects.clear();
		}
		if (mNotifyOnChange)
			notifyDataSetChanged();
	}

	public void sort(Comparator<? super T> comparator)
	{
		Collections.sort(mObjects, comparator);
		if (mNotifyOnChange)
			notifyDataSetChanged();
	}

	@Override
	public void notifyDataSetChanged()
	{
		// TODO Auto-generated method stub
		super.notifyDataSetChanged();
		mNotifyOnChange = true;
	}

	public void setNotifyOnChange(boolean notifyOnChange)
	{
		mNotifyOnChange = notifyOnChange;
	}

	private void init(Context context, int resource, int textViewResourceId,
			List<T> objects)
	{
		mContext = context;
		mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mResource = mDropDownResource = resource;
		mObjects = objects;
		mFieldId = textViewResourceId;
		for (T t : objects)
		{
			Log.i(LOGTAG, ">>>>>>>>>>>>> t = " + t);
		}
	}

	public Context getContext()
	{
		return mContext;
	}

	@Override
	public int getCount()
	{
		return mObjects.size();
	}

	@Override
	public Object getItem(int position)
	{
		return mObjects.get(position);
	}

	public int getPosition(T item)
	{
		return mObjects.indexOf(item);
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		return createViewFromResource(position, convertView, parent, mResource);
	}

	private View createViewFromResource(int position, View convertView,
			ViewGroup parent, int resource)
	{
		View view;
		TextView text;
		if (convertView == null)
		{
			view = mInflater.inflate(resource, parent, false);
		}
		else
		{
			view = convertView;
		}
		try
		{
			if (mFieldId == 0)
			{
				// If no custom field is assigned, assume the whole resource is
				// a TextView
				text = (TextView) view;
			}
			else
			{
				// Otherwise, find the TextView field within the layout
				text = (TextView) view.findViewById(mFieldId);
			}
		}
		catch (ClassCastException e)
		{
			Log.e(LOGTAG,"SearchCityAdapter"+
					"You must supply a resource ID for a TextView");
			throw new IllegalStateException(
											"SearchCityAdapter requires the resource ID to be a TextView",
											e);
		}
		T item = (T) getItem(position);
		Log.i(LOGTAG, ">>>>>>>>>>>>>> position = " + position + " item = " + item);
		if (item instanceof CharSequence)
		{
			text.setText((CharSequence) item);
		}
		else
		{
			text.setText(item.toString());
		}
		return view;
	}

	public void setDropDownViewResource(int resource)
	{
		this.mDropDownResource = resource;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent)
	{
		return createViewFromResource(	position, convertView, parent,
										mDropDownResource);
	}

	@Override
	public Filter getFilter()
	{
		if (mFilter == null)
		{
			mFilter = new EmailBookArrayFilter();
		}
		return mFilter;
	}

	private class EmailBookArrayFilter extends Filter
	{
		/**
		 * prefix:监听用户输入的字符
		 */
		@Override
		protected FilterResults performFiltering(CharSequence prefix)
		{
			Log.i(LOGTAG, "MailArrayFilter : performFiltering >>>>>>> prefix="
						+ prefix);
			FilterResults results = new FilterResults();
			// if (mOriginalValues == null)
			// {
			// synchronized (mLock)
			// {
			// mOriginalValues = new ArrayList<T>(mObjects);
			// }
			// }
			// if (prefix == null || prefix.length() == 0)
			// {
			// synchronized (mLock)
			// {
			// ArrayList<T> list = new ArrayList<T>(mOriginalValues);
			// results.values = list;
			// results.count = list.size();
			// }
			// }
			// else
			// {


			final ArrayList<T> newValues = new ArrayList<T>();
			if (prefix != null)
			{
				
				
				String prefixString = prefix.toString().toLowerCase();
				ArrayList<EmailAddress> addArry = AddressBook
						.getEmailAddresses();

				String[] array = new String[addArry.size()];
				for (int i = 0; i < addArry.size(); i++)
				{
					array[i] = addArry.get(i).toString();
					String[] split = array[i].split("<");
					if ((split[0]).contains(prefixString)||(split[1]).contains(prefixString))
					{
						newValues.add((T) array[i]);
                       
					}
					else if ((array[i]).startsWith(prefixString))
					{
						newValues.add((T) array[i]);
					}

				}
				// }
				// String key;
				// String[] keyPart;
				//
				// while(enumeration.hasMoreElements()) {
				// key = (String)enumeration.nextElement();
				// keyPart = key.split("-");
				// Log.i(tag,
				// ">>>>>>>>>>>>>>>>>>> prefixString = "+prefixString+" key = "+key);
				// if(prefixString.length() == 1 &&
				// keyPart[1].startsWith(prefixString)) {
				// newValues.add((T)Composer.ht.get(key));
				// } else if (prefixString.length() == 2) {
				// if(keyPart[2].equals(prefixString)) {
				// newValues.add((T)Composer.ht.get(key));
				// }
				// if(newValues.size() == 0 &&
				// keyPart[1].startsWith(prefixString)) {
				// newValues.add((T)Composer.ht.get(key));
				// }
				// } else if (prefixString.length() == 3) {
				// if(keyPart[0].equals(prefixString)) {
				// newValues.add((T)Composer.ht.get(key));
				// }
				// if(newValues.size() == 0 &&
				// keyPart[1].startsWith(prefixString)) {
				// newValues.add((T)Composer.ht.get(key));
				// }
				// }
				// }
				// for (int i = 0; i < count; i++) {
				// final T value = values.get(i);
				// final String valueText = value.toString().toLowerCase();
				// // First match against the whole, non-splitted value
				// if (valueText.startsWith(prefixString)) {
				// newValues.add(value);
				// }else {
				// final String[] words = valueText.split(" ");
				// final int wordCount = words.length;
				// for (int k = 0; k < wordCount; k++) {
				// if (words[k].startsWith(prefixString)) {
				// newValues.add(value);
				// break;
				// }
				// }
				// }
				// }
			}
			results.values = newValues;
			results.count = newValues.size();
			return results;
		}

		@Override
		protected void publishResults(CharSequence constraint,
				FilterResults results)
		{
			Log.i(	LOGTAG,
					"CityArrayFilter : publishResults >>>>>>>>>>> results.values = "
							+ results.values);
			mObjects = (List<T>) results.values;
			if (results.count > 0)
			{
				notifyDataSetChanged();
			}
			else
			{
				notifyDataSetInvalidated();
			}
		}

	}
}

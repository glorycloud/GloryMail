package mobi.cloudymail.mailclient;

import java.util.ArrayList;
import java.util.regex.Pattern;

import mobi.cloudymail.util.DialogUtils;
import mobi.cloudymail.util.MyApp;
import mobi.cloudymail.util.NewDbHelper;
import mobi.cloudymail.util.Utils;
import android.R.integer;
import android.app.Dialog;
import android.content.ContentResolver;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;


interface OnEmailAddressDialogListner
{
	public abstract void onDialogReturned(EmailAddress oldAddress, EmailAddress newAddress);
}

public class AddressBook extends BaseActivity implements OnClickListener,OnEmailAddressDialogListner
{
	private ListView _addressListView;
	public static ArrayList<EmailAddress> addressArry = new ArrayList<EmailAddress>();
//	public static ArrayList<EmailAddress> addressFromPhone = new ArrayList<EmailAddress>();
	private int selectIdx = -1;
	private Button _addButton = null;
	private Button _editButton = null;
	private Button _delButton = null;
	
	private EmailAddressDialog _dialog = null;
	private  static Handler myHandler;
	final static String[] PHONENUMBER_PREFIX = { "130", "131", "132", "145", "155", "156", "185",
												"186", "134", "135", "136", "137", "138",

												"139", "147", "150", "151", "152", "157", "158",
												"159", "182", "183", "187", "188", "133", "153",
												"189", "180" };
	protected static final int SUCCESS = 0;
	private AddressListAdapter _adapter;

/**
	 * 匹配手机号码
	 * <p>
	 * 新联通</br>
	 * （中国联通+中国网通）手机号码开头数字 130,131,132,145,155,156,185,186</br>
	 * 新移动</br>
	  * 　（中国移动+中国铁通）手机号码开头数字</br>
	 * 134,135,136,137,138,139,147,150,151,152,157,158,159,182,183,187,188</br>
	 * 新电信</br>
	  * 　（中国电信+中国卫通）手机号码开头数字 133,153,189,180</br>
	 * </p>
	 * @param 手机号码
	 * @return 参数为null和不合法时返回false，否则返回true
*/

	public static boolean patternPhoneNumber(String number)
	{
		int len = PHONENUMBER_PREFIX.length;
		if (number != null)
		{
			for (int i = 0; i < len; i++)
			{
				Pattern p = Pattern.compile(PHONENUMBER_PREFIX[i] + "\\d{8}");
				if (p.matcher(number).matches())
				{
					return true;
				}
			}
		}

		return false;
	}

	public static ArrayList<EmailAddress> getEmailAddresses()
	{
		if(addressArry.isEmpty())
		{
			NewDbHelper.getInstance().loadAddressBook();
//			myHandler.post(new Runnable()
//				{
//					public void run()
//					{
//						addressArry.addAll(getAddressFromPhone());
//						
//					}
//				});
			
			Thread t = new Thread(
				new Runnable()
				{
					public void run()
					{
						addressArry.addAll(getAddressFromPhone());
						if(myHandler != null)
							myHandler.sendEmptyMessage(SUCCESS);
					}
				}
			);
			t.start();

			
		}
		return addressArry;
	}
	public static  ArrayList<EmailAddress> getAddressFromPhone()
	{
		ArrayList<EmailAddress> phoneAddress = new ArrayList<EmailAddress>();
//		ContentResolver resolver=MyApp.instance().getContentResolver();
//		String[] contractMsg=new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Email.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER};
//		Cursor cursor=resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, contractMsg, null, null, null);
//		while(cursor.moveToNext())
//		{
//			EmailAddress address=null;
//			String contractName=cursor.getString(0);
//			String contractEmail=cursor.getString(1);
//			String contractNumber=cursor.getString(2);
//			if(!Utils.isEmpty(contractName))
//			{
//				if(!Utils.isEmpty(contractEmail))
//				{
//					address=new EmailAddress(contractName, contractEmail);
//					addressFromPhone.add(address);
//				}
//				else if(!Utils.isEmpty(contractEmail))
//				{
//					address=new EmailAddress(contractName, contractNumber);
//					addressFromPhone.add(address);
//				}
//			}
//		}
	    Uri uri = ContactsContract.Contacts.CONTENT_URI; // 通讯录的uri,这是推荐的常量 
	    ContentResolver resolver=MyApp.instance().getContentResolver();
        Cursor cursor = resolver.query(uri, null, null, null, null); 
        // 取得通讯录里的内容 
        while (cursor.moveToNext()) { 
        	EmailAddress address=null;
//	            StringBuffer buf = new StringBuffer(); // 实例化一个可变字符串StringBuffer 
            // 取得联系人id,每个条目都有一个唯一的id(主键) 
            String contactId = cursor.getString(cursor 
                    .getColumnIndex(ContactsContract.Contacts._ID)); 
            // 取得联系人的显示名称 
            String name = cursor.getString(cursor 
                    .getColumnIndex(ContactsContract.Data.DISPLAY_NAME)); 
//	            buf.append("id:" + contactId).append(",name:" + name); 
            if(!Utils.isEmpty(name))
            {
	            // 取得联系人的email 
	            Cursor email = resolver.query( 
	                    ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, 
	                    ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = "
	                            + contactId, null, null); 
	            boolean hasEmail=false;
	            while (email.moveToNext()) { 
	            	hasEmail=true;
	                String mail = email 
	                        .getString(email 
	                                .getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA)); 
//		                buf.append(",mail:" + mail); 
	                address=new EmailAddress(name, mail);
	                phoneAddress.add(address);
	            } 
	            email.close(); 
	            if(!hasEmail)
	            {
		            // 取得联系人的号码 
		            Cursor phone = resolver.query( 
		                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, 
		                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = "
		                            + contactId, null, null); 
		            while (phone.moveToNext()) { 
		                String phonum = phone 
		                        .getString(phone 
		                                .getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)); 
//			                buf.append(phonum); 
		                address=new EmailAddress(name, phonum);
		                phoneAddress.add(address);
		            } 
		            phone.close(); 
	            }
            }
        } 
        cursor.close(); 
        return phoneAddress;
	}
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.address_book);
		//win.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.cloudymail);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.address_book_titlebar);
		
		
		_addressListView = (ListView)findViewById(R.id.addressBookListView);
		_addButton = (Button)findViewById(R.id.ab_newBtn);
		_editButton = (Button)findViewById(R.id.ab_editBtn);
		_delButton = (Button)findViewById(R.id.ab_deleteBtn);
		
		_addButton.setOnClickListener(this);
		_editButton.setOnClickListener(this);
		_delButton.setOnClickListener(this);
		myHandler=new Handler() {
			public void handleMessage(Message msg)
			{
				if(msg.what==SUCCESS)
				{
					_adapter.notifyDataSetChanged();
				}
			}

		};
		
		
		/*for(int i = 0; i < 3; i++)
		{
			addressArry.add(new EmailAddress("Tim Liu<mendynew@126.com>"));
		}*/
		
		
		
		getEmailAddresses();//load on demand;
		
		_adapter = new AddressListAdapter(this, addressArry);
		_addressListView.setAdapter(_adapter);
		_addressListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> paramAdapterView,
					View paramView, int paramInt, long paramLong)
			{
				if(paramInt == 0)
					return;
				clearSelection();
				selectIdx =paramInt;
//				paramAdapterView.getChildAt(0).setBackgroundColor(Color.BLACK);
				int selectedColor = getResources().getColor(R.color.gray);
//				int backColor = getResources().getColor(R.color.white);
//				int childCount=paramAdapterView.getChildCount();
				paramView.setBackgroundColor(selectedColor);
//				for(int i=0;i<paramAdapterView.getCount();i++)
//				{
////		            View v=paramAdapterView.getChildAt(i);
////			        if (paramInt == i) {
////			        	v.setBackgroundColor(selectedColor);
////			        } else {
////			        	v.setBackgroundColor(backColor);
////			        }
//					
//					}
//				}
				enableButtons(true);
			}

		});
		
		enableButtons(false);
		
		_dialog = new EmailAddressDialog(this);
		_dialog.setListner(this);
	}
	
	private EmailAddress getSelectedAddress()
	{
		if(selectIdx < 1 || selectIdx >addressArry.size())
			return null;
		return addressArry.get(selectIdx-1);
	}
	
	@Override
	public void onDialogReturned(EmailAddress oldAddress, EmailAddress newAddress)
	{
		if(oldAddress == null)
		{
			if(newAddress == null)//cancel button clicked;
				return;
			//add new address;
			String errMsg = NewDbHelper.getInstance().addEmailAddress(newAddress);
			if(!errMsg.equals(""))
			{
				DialogUtils.showMsgBox(this,errMsg, getResources().getString(R.string.error));
				return;
			}
			addressArry.add(newAddress);
			updateListView(true);
		}
		else
		{//edit address;
			if(newAddress == null)
				return;
			String errMsg = NewDbHelper.getInstance().updateEmailAddress(newAddress, oldAddress.getAddress());
			if(!errMsg.equals(""))
			{
				DialogUtils.showMsgBox(this,errMsg, getResources().getString(R.string.error));
				return;
			}
			EmailAddress curAddress = getSelectedAddress();
			curAddress.setName(newAddress.getName());
			curAddress.setAddress(newAddress.getAddress());	
			updateListView(true);
		}
	}
	
	private void updateListView(boolean clearValue)
	{
		AddressListAdapter sAdapter = (AddressListAdapter)_addressListView.getAdapter();
    	sAdapter.notifyDataSetChanged();
    	if(clearValue)
    		clearSelection();
	}
	private void clearSelection()
    {
    	selectIdx = -1;
    	int childCount = _addressListView.getChildCount();
    	int backColor = getResources().getColor(R.color.white);
    	for(int i = 1; i < childCount; i++)
    		_addressListView.getChildAt(i).setBackgroundColor(backColor);
    	enableButtons(false);
    }
	
	private void enableButtons(Boolean value)
	{
		_editButton.setEnabled(value);
		_delButton.setEnabled(value);
	}
	
	@Override
    public void onClick(View v)
	{
    	switch(v.getId())
    	{
    	case R.id.ab_newBtn:
    		_dialog.setEmailAddress(null);
    		_dialog.show();
    		break;
    	case R.id.ab_editBtn:
    	{
    		_dialog.setEmailAddress(getSelectedAddress());
    		_dialog.show();
    		break;
    	}
    	case R.id.ab_deleteBtn:
    	{
    		if(selectIdx < 1)
    			return;
    		String errMsg = NewDbHelper.getInstance().deleteEmailAddress(getSelectedAddress());
    		if(!errMsg.equals(""))
			{
				DialogUtils.showMsgBox(this,errMsg, getResources().getString(R.string.error));
				return;
			}
    		addressArry.remove(selectIdx-1);
    		updateListView(true);
    		break;
    	}
    	default:
    		break;
    	}
	}
	
	private class EmailAddressDialog extends Dialog implements android.view.View.OnClickListener
	{
		private EditText nameEditText = null;
		private EditText addressEditText = null;
		private Button okButton = null;
		private Button cancelButton = null;
		
		EmailAddress eAddress = null;
		OnEmailAddressDialogListner l = null;
		
		public EmailAddressDialog(Context ctx)
		{
			super(ctx);
			setContentView(R.layout.email_address_dialog);
			
			nameEditText = (EditText)findViewById(R.id.ead_nameTxt);
			addressEditText = (EditText)findViewById(R.id.ead_addressTxt);
			okButton = (Button)findViewById(R.id.ead_okBtn);
			cancelButton = (Button)findViewById(R.id.ead_cancelBtn);
			
			okButton.setOnClickListener(this);
			cancelButton.setOnClickListener(this);
		}
		
		public void setEmailAddress(EmailAddress address)
		{
			eAddress = address;
			if(address != null)
			{
				nameEditText.setText(address.getName());
				addressEditText.setText(address.getAddress());
			}
			else {
				nameEditText.setText("");
				addressEditText.setText("");
			}
		}
		
		public void setListner(OnEmailAddressDialogListner l)
		{
			this.l = l;
		}
		
		@Override
	    public void onClick(View v)
		{
			if(l == null)
				return;
	    	switch(v.getId())
	    	{
	    	case R.id.ead_okBtn:
	    	{
	    		String name = nameEditText.getText().toString();
	    		String address = addressEditText.getText().toString();
	    		if(name.equals(""))
	    		{
	    			Resources res = getContext().getResources();
	    			DialogUtils.showMsgBox(getContext(),res.getString(R.string.ab_empty_name), res.getString(R.string.error));
	    			return;
	    		}
	    		if(!AccountWizard.isMailAddressValid(address, getContext()))
	    			return;
	    			
	    		this.hide();
	    		if(eAddress != null)//if not changed, ignore
	    		{
	    			if(eAddress.getName().equals(name) && eAddress.getAddress().equals(address))
	    				return;
	    		}
	    		l.onDialogReturned(eAddress, new EmailAddress(name, address));
	    		break;
	    	}
	    	case R.id.ead_cancelBtn:
	    		this.hide();
	    		break;
	    	}    	
	    }
	}
	
	private class AddressListAdapter extends BaseAdapter
	{
		private Context context;
		private LayoutInflater layoutInflater;
		private ArrayList<EmailAddress> addresses;
		class ViewHolder{
			TextView nameView=null;
			TextView addressView=null;
		}
		public AddressListAdapter(Context context, ArrayList<EmailAddress> addresses)
		{
			this.context = context;
			layoutInflater = (LayoutInflater) this.context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			this.addresses = addresses;
		}

		@Override
		public int getCount()
		{
			return addresses.size()+1;
		}

		@Override
		public Object getItem(int position)
		{
			if(position==0)
				return null;
			return addresses.get(position-1);
		}

		@Override
		public long getItemId(int position)
		{
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View view = layoutInflater.inflate(R.layout.address_book_item, null);
			// view.setBackgroundDrawable(getResources().getDrawable(R.drawable.tab_selected_two));
			TextView nameView = (TextView) view.findViewById(R.id.ab_name);
			TextView addressView = (TextView) view.findViewById(R.id.ab_address);

			if (position == 0)
			{
				// view.setBackgroundColor(Color.BLACK);
				// view.setBackgroundDrawable(getResources().getDrawable(R.drawable.tab_selected_two));
				nameView.setBackgroundDrawable(getResources()
						.getDrawable(R.drawable.tab_selected_two));
				addressView.setBackgroundDrawable(getResources()
						.getDrawable(R.drawable.tab_selected_two));
				// nameView.setBackgroundColor(Color.rgb(0, 128, 255));
				// nameView.setTextColor(Color.YELLOW);
				// addressView.setBackgroundColor(Color.rgb(0, 128, 255));
				// addressView.setTextColor(Color.YELLOW);
				nameView.setText(R.string.ab_name);
				addressView.setText(R.string.ab_address);
			}
			else
			{
				EmailAddress addr = addresses.get(position - 1);
				nameView.setText(addr.getName());
				addressView.setText(addr.getAddress());
			}
			return view;

			// ViewHolder holder = null;
			// if (convertView == null)
			// {
			// holder = new ViewHolder();
			// convertView = layoutInflater.inflate(R.layout.address_book_item,
			// null);
			// holder.nameView = (TextView)
			// convertView.findViewById(R.id.ab_name);
			// holder.addressView = (TextView)
			// convertView.findViewById(R.id.ab_address);
			// convertView.setTag(holder);
			// if (position == 0)
			// {
			// holder.nameView.setBackgroundDrawable(getResources()
			// .getDrawable(R.drawable.tab_selected_two));
			// holder.addressView.setBackgroundDrawable(getResources()
			// .getDrawable(R.drawable.tab_selected_two));
			// holder.nameView.setText(R.string.ab_name);
			// holder.addressView.setText(R.string.ab_address);
			// return convertView;
			// }
			//
			//
			// }
			// else
			// {
			// holder = (ViewHolder) convertView.getTag();
			// }
			//
			// EmailAddress addr = addresses.get(position - 1);//有问题
			// holder.nameView.setText(addr.getName());
			// holder.addressView.setText(addr.getAddress());
			// return convertView;
		}
	}
}

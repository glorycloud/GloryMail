package mobi.cloudymail.mailclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import mobi.cloudymail.mailclient.FolderActivity.FolderItemData;
import mobi.cloudymail.mailclient.net.Account;
import android.app.Activity;
import android.content.Intent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class FolderAdapter extends BaseExpandableListAdapter
{
	private Activity _mContext = null;
	private FolderActivity _folderActivity=null;
	public String[] children = null;
	private List<String> groupList = null;
	public List<List<FolderItemData>> itemList = null;

	//private  Map<Integer,FolderItemData> _folderMap = null;accounts.add(a)
	public Map<Integer,ArrayList<FolderItemData>> folderMap = null;
	public boolean isTextClicked = false;
	private OnClickListener addAccountItemListener;
	private OnClickListener editAccountListener;
	
	public void init(){
		
		children = new String[]{_mContext.getResources().getString(R.string.folder_inbox), 
		                        _mContext.getResources().getString(R.string.folder_draft), 
		                        _mContext.getResources().getString(R.string.folder_notsentbox), 
		                        _mContext.getResources().getString(R.string.folder_sentbox), 
		                        _mContext.getResources().getString(R.string.folder_delete)};
		groupList.clear();
		itemList.clear();
		for(int i=0; i<AccountManager.getCount(); i++){
			Account acct = AccountManager.getByIndex(i);
			groupList.add(acct.name);
			List<FolderItemData> ll =folderMap.get(acct.id);
			itemList.add(ll);
		}
	}
	
	public FolderAdapter(FolderActivity context){
		this._folderActivity=((FolderActivity)context);
		this._mContext = context;
		this.folderMap = ((FolderActivity)context).folderMap;
		groupList = new ArrayList<String>();
		itemList = new ArrayList<List<FolderItemData>>();
		init();
	}

	@Override
	public Object getChild(int groupPosition, int childPosition)
	{
		return itemList.get(groupPosition).get(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition)
	{
		return childPosition;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent)
	{
		if(convertView == null){
			convertView = _mContext.getLayoutInflater().inflate(R.layout.folder_child, null);
		}
		convertView.setBackgroundResource(R.drawable.mail_folder_child_bg);
		TextView text = (TextView)convertView.findViewById(R.id.childText);
		ImageView image = (ImageView)convertView.findViewById(R.id.groupImage);
		String name = itemList.get(groupPosition).get(childPosition).folderDes+ "    "  + "[";
		if(itemList.get(groupPosition).get(childPosition).unreadCount >= 0)
			name += itemList.get(groupPosition).get(childPosition).unreadCount+"/";
		name += itemList.get(groupPosition).get(childPosition).totalCount+"]";
		text.setText(name);
		image.setImageResource(itemList.get(groupPosition).get(childPosition).image);
		

		return convertView;
	}
	@Override
	public int getChildrenCount(int groupPosition)
	{
		if(groupPosition == getGroupCount() - 1)
			return 0;
		return itemList.get(groupPosition).size();
	}

	@Override
	public Object getGroup(int groupPosition)
	{
		return itemList.get(groupPosition);
	}

	@Override
	public int getGroupCount()
	{
		return groupList.size()+1;
	}

	@Override
	public long getGroupId(int groupPosition)
	{
		return groupPosition;
	}

	@Override
	public int getGroupType(int groupPosition)
	{
		if (groupPosition == getGroupCount() - 1)
		{
			return 1;
		}
		return 0;
	}

	@Override
	public int getGroupTypeCount()
	{
		return 2;
	}

	@Override
	public View getGroupView(final int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent)
	{
		

		
		if (groupPosition == getGroupCount() - 1)
		{
			return getAddingAccountView(convertView);
		}
			
		if(convertView == null)
		{
			convertView = _mContext.getLayoutInflater().inflate(R.layout.folder_group, null);
		}
		
		convertView.setBackgroundResource(R.drawable.mail_folder_father_bg);
		ImageButton editAccountBtn=(ImageButton)convertView.findViewById(R.id.folder_edit_btn);
		ImageButton delAccountBtn=(ImageButton)convertView.findViewById(R.id.folder_del_btn);
		if(FolderActivity.showFlag==true)
		{
			editAccountBtn.setVisibility(View.VISIBLE);
			delAccountBtn.setVisibility(View.VISIBLE);
			FolderActivity.showFlag=!FolderActivity.showFlag;
		}
		else
		{
			editAccountBtn.setVisibility(View.GONE);
			delAccountBtn.setVisibility(View.GONE);
			FolderActivity.showFlag=!FolderActivity.showFlag;
		}
		FolderActivity.showFlag=!FolderActivity.showFlag;
		
		editAccountListener=new OnClickListener() {
			
			@Override
			public void onClick(View v)
			{
				
				_folderActivity.selectedIdx = groupPosition;
				_folderActivity.editAccount(groupPosition);
			}
		};
		editAccountBtn.setOnClickListener(editAccountListener);
		
		delAccountBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v)
			{
				_folderActivity.deleteAccount(groupPosition);
			}
		});
		
		
		TextView text = (TextView)convertView.findViewById(R.id.groupText);
		ImageView image = (ImageView)convertView.findViewById(R.id.groupImage);
		//show editBtn and delBtn
		

		text.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1)
			{
				FolderAdapter.this.isTextClicked = true;
				return false;
			}
		});
		
		image.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				// TODO Auto-generated method stub
				FolderAdapter.this.isTextClicked = false;
				return false;
			}
		});
		String name = (String) groupList.get(groupPosition);
		text.setText(name);
		image.setImageDrawable(_mContext.getResources().getDrawable(R.drawable.normal));
		if(isExpanded)
			image.setImageDrawable(_mContext.getResources().getDrawable(R.drawable.open));
		
		return convertView;
	}


	private View getAddingAccountView(View convertView)
	{
		View addAccountItem = convertView;
         //ImageView image;
       if (addAccountItem == null) {
        	 addAccountItem = View.inflate(_mContext, R.layout.add_account_item, null);
        	 
      	 if(addAccountItemListener==null)
  	       {
  	    	   addAccountItemListener=new View.OnClickListener() {
  				
  				@Override
  				public void onClick(View v)
  				{

  					Account a = new Account();
         				Intent intent = new Intent(_folderActivity, AccountWizard.class);
         				intent.putExtra("account", a);
         				intent.putExtra("isNew",true);
         				_folderActivity.startActivityForResult(intent, R.layout.account_wizard);	
  				}
  			};
  	       }
          addAccountItem.setOnClickListener(addAccountItemListener);
         }
         
	     
       	return addAccountItem;
	}

	@Override
	public boolean hasStableIds()
	{
		return false;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition)
	{
		return true;
	}

	@Override
	public boolean isEmpty()
	{
		return false;
	}

	@Override
	public void onGroupCollapsed(int groupPosition)
	{
	}
	
}

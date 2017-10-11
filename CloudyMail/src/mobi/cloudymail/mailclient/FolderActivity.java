package mobi.cloudymail.mailclient;

import static mobi.cloudymail.mailclient.FolderNames.FOLDER_DELETE;
import static mobi.cloudymail.mailclient.FolderNames.FOLDER_INBOX;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import mobi.cloudymail.data.FolderInfo;
import mobi.cloudymail.data.MailStatus;
import mobi.cloudymail.mailclient.net.Account;
import mobi.cloudymail.util.DialogResult;
import mobi.cloudymail.util.DialogUtils;
import mobi.cloudymail.util.DialogUtils.ButtonFlags;
import mobi.cloudymail.util.MyApp;
import mobi.cloudymail.util.NewDbHelper;
import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ExpandableListView.OnGroupExpandListener;

public class FolderActivity extends ExpandableListActivity implements View.OnClickListener
{

	private ExpandableListView _elv = null;
	private FolderAdapter fa = null;

	// public Map<Integer,FolderItemData> _folderMap = null;
	public Map<Integer, ArrayList<FolderItemData>> folderMap = null;
	private Account oldAccount = null;
	int selectedIdx = -1;
	// private Activity context=null;
	public static boolean showFlag;
	// public FolderAdapter2 _folderAdapter=null;

	public static final int INDEX_GLOBAL = 0;
	public static final int INDEX_ACCOUNT = 1;
	public static final int INDEX_INBOX = 2;
	public static final int INDEX_DRAFT = 3;
	public static final int INDEX_NOTSENT = 4;
	public static final int INDEX_SENT = 5;
	public static final int INDEX_WRITTEN_MAIL = 7;
	public static final int INDEX_DELETE = 6;
	public static final int INDEX_SERVER = -1;
	// public static final List<Account> accounts = new ArrayList<Account>();
	public static final String DAT_FOLDER_NAME = "folderName";
	public static final String DAT_FOLDER_DESCRIPTION = "folderDes";

	class FolderItemData
	{
		public int image;
		public String folderDes;
		public int totalCount;
		public int unreadCount = -1;
		public String folderName;
		public Class<?> activity = null;
		public int id = 0;
	}

	// public static int getAccountIndex(Account a)
	// {
	// int selectedIdx = accounts.indexOf(a);
	// return selectedIdx;
	// }
	//

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.folder);
		//win.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.cloudymail);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.folder_titlebar_layout);
		initEditWidget();
		folderMap = new HashMap<Integer, ArrayList<FolderItemData>>();
		_elv = this.getExpandableListView();
		_elv.setGroupIndicator(null);
		_elv.setOnGroupExpandListener(new OnGroupExpandListener() {

			@Override
			public void onGroupExpand(int groupPosition)
			{
				// TODO Auto-generated method stub
				for(int i=0; i<FolderActivity.this.getExpandableListAdapter().getGroupCount(); i++){
					// _elv.setGroupIndicator(FolderActivity.this.getResources().getDrawable(R.drawable.delete));
					if(groupPosition!=i){
						_elv.collapseGroup(i);
						// _elv.setGroupIndicator(FolderActivity.this.getResources().getDrawable(R.drawable.expand));
					}
				}
			}
		});

		_elv.setOnGroupClickListener(new OnGroupClickListener() {
			@Override
			public boolean onGroupClick(ExpandableListView arg0, View arg1, int groupPosition,
					long arg3)
			{
				// TODO Auto-generated method stub
				boolean currentAccountChanged=false;
				if(MyApp.currentAccount!=AccountManager.getByIndex(groupPosition))
				{
					MyApp.currentAccount = AccountManager.getByIndex(groupPosition);
					currentAccountChanged=true;
				}
				if (fa.isTextClicked){
					// int item_index = INDEX_INBOX;
					// if(folderMap.get(MyApp.currentAccount.id).containsKey(item_index))
					// {
					// FolderItemData fid =folderMap.get(MyApp.currentAccount.id).get(item_index));
					Intent intent = new Intent(FolderActivity.this, InBoxActivity.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					intent.putExtra(DAT_FOLDER_NAME, "INBOX");
					intent.putExtra("currentAccountChanged", currentAccountChanged);
					intent.putExtra(DAT_FOLDER_DESCRIPTION,getResources().getString(R.string.folder_inbox));
					startActivity(intent);
					// }
					return true;
				}
				return false;
			}
		});

		_elv.setOnChildClickListener(new OnChildClickListener() {
			
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id)
			{
				// TODO Auto-generated method stub
				//int item_index = childPosition;
				//if(_folderMap.containsKey(item_index))
				if(fa.itemList.get(groupPosition)!=null && fa.itemList.get(groupPosition).size()!=0)
				{
					FolderItemData fid = fa.itemList.get(groupPosition).get(childPosition);
					//FolderItemData fid = folderMap.get(AccountManager.accounts.get(groupPosition).name).get(childPosition);
					Intent intent = new Intent(FolderActivity.this,fid.activity);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					intent.putExtra(DAT_FOLDER_NAME, fid.folderName);
					intent.putExtra(DAT_FOLDER_DESCRIPTION, fid.folderDes);
				//	intent.putExtra(DATA_FOLDER_NAME, selectedFolderName);
					startActivity(intent);
				}
				return false;
			}
		});	
		MyApp.addActivity(this);
		if(AccountManager.isEmpty())
		{
			createNewAccount();
		}

	}

	// @Override
	// protected void onActivityResult(int requestCode, int resultCode, Intent data)
	// {
	// switch(requestCode)
	// {
	// case R.id.titleLeftBtn:
	// if(getIntent().getBooleanExtra("accountFolder", false)==true)
	// {
	// itemExpandInit();
	// }
	// break;
	//
	// }
	//
	// super.onActivityResult(requestCode, resultCode, data);
	// }

	private void initEditWidget()
	{
		Button editTitleBtn = (Button) findViewById(R.id.editfolderTitlebtn);
		editTitleBtn.setOnClickListener(this);
	}

	public void expandAccount()
	{
		// TODO Auto-generated method stub
		int groupPos = AccountManager.getAccountIndex(MyApp.currentAccount);
		_elv.expandGroup(groupPos);

	}

	public void collapseAccount()
	{
		int groupPos = AccountManager.getAccountIndex(MyApp.currentAccount);
		_elv.collapseGroup(groupPos);
	}

	private void updateMailCount(Account account)
	{
		//Account curAccount = MyApp.currentAccount;
		if(account == null)
			return;
		Account curAccount = account;		
		NewDbHelper dbHelper = NewDbHelper.getInstance();
		ArrayList<FolderItemData> list=folderMap.get(curAccount.id);
		for (FolderItemData data:list)
		{
			switch (data.id)
			{
			case INDEX_GLOBAL:
				data.totalCount=dbHelper.getInMailCount(-1,FOLDER_INBOX,new int[]{MailStatus.MAIL_NEW,MailStatus.MAIL_READED, MailStatus.FLAG_HAS_MORE_PLACEHOLD},true);
				data.unreadCount= dbHelper.getInMailCount(-1,FOLDER_INBOX,new int[]{MailStatus.MAIL_NEW},false);
				break;
			case INDEX_INBOX:
				data.totalCount= dbHelper.getInMailCount(curAccount.id, FOLDER_INBOX,new int[]{MailStatus.MAIL_NEW,MailStatus.MAIL_READED, MailStatus.FLAG_HAS_MORE_PLACEHOLD},true);
				data.unreadCount = dbHelper.getInMailCount(curAccount.id, FOLDER_INBOX,new int[]{MailStatus.MAIL_NEW},true);
				break;
			case INDEX_WRITTEN_MAIL:
				data.totalCount = dbHelper.getOutMailCount(curAccount.id,"",new int[]{MailStatus.MAIL_NEW,MailStatus.MAIL_READED, MailStatus.FLAG_HAS_MORE_PLACEHOLD});
				break;
//			case INDEX_SENT:
//				data.totalCount = dbHelper.getOutMailCount(curAccount.id,FOLDER_SENT,new int[]{MailStatus.MAIL_NEW,MailStatus.MAIL_READED, MailStatus.FLAG_HAS_MORE_PLACEHOLD});
//				break;
//			case INDEX_NOTSENT:
//				data.totalCount = dbHelper.getOutMailCount(curAccount.id,FOLDER_NOTSENT,new int[]{MailStatus.MAIL_NEW,MailStatus.MAIL_READED, MailStatus.FLAG_HAS_MORE_PLACEHOLD});
//				break;
//			case INDEX_DRAFT:
//				data.totalCount = dbHelper.getOutMailCount(curAccount.id,FOLDER_DRAFT,new int[]{MailStatus.MAIL_NEW,MailStatus.MAIL_READED, MailStatus.FLAG_HAS_MORE_PLACEHOLD});
//				break;
			case INDEX_DELETE:
				data.totalCount = DeleteBoxActivity.getDelMailCount(curAccount.id);
				break;
			default:
				data.totalCount=dbHelper.getInMailCount(curAccount.id,data.folderName,new int[]{MailStatus.MAIL_NEW,MailStatus.MAIL_READED},true);
				break;
			}
		}
//		Map<Integer,FolderItemData> accountMap = folderMap.get(curAccount.id);
//		accountMap.get(INDEX_GLOBAL).totalCount = dbHelper.getInMailCount(-1,FOLDER_INBOX,MailStatus.MAIL_DELETED,true,true);
//		accountMap.get(INDEX_GLOBAL).unreadCount = dbHelper.getInMailCount(-1,FOLDER_INBOX,MailStatus.MAIL_NEW,false,false);
//		accountMap.get(INDEX_INBOX).totalCount = dbHelper.getInMailCount(curAccount.id, FOLDER_INBOX, MailStatus.MAIL_DELETED, true,false);
//		accountMap.get(INDEX_INBOX).unreadCount = dbHelper.getInMailCount(curAccount.id, FOLDER_INBOX, MailStatus.MAIL_NEW, false,false);
//		accountMap.get(INDEX_SENT).totalCount = dbHelper.getOutMailCount(curAccount.id,FOLDER_SENT,MailStatus.MAIL_DELETED, true);
//		accountMap.get(INDEX_NOTSENT).totalCount = dbHelper.getOutMailCount(curAccount.id,FOLDER_NOTSENT,MailStatus.MAIL_DELETED, true);
//		accountMap.get(INDEX_DRAFT).totalCount = dbHelper.getOutMailCount(curAccount.id,FOLDER_DRAFT,MailStatus.MAIL_DELETED, true);
//		
//		accountMap.get(INDEX_DELETE).totalCount = DeleteBoxActivity.getDelMailCount(curAccount.id);
	}

	@Override
	protected void onResume()
	{
		// TODO Auto-generated method stub
		super.onResume();
		MyApp.setCurrentActivity(this);
		ArrayList<FolderItemData> itemDataList=null;
		ArrayList<FolderInfo> folders = null;
		Resources res = getResources();
		NewDbHelper dbHelper = NewDbHelper.getInstance();
		for (int i = 0; i < AccountManager.getCount(); i++)
		{
			itemDataList=new ArrayList<FolderItemData>();
//		    FolderItemData fid = new FolderItemData();
//		    fid.image = R.drawable.webtext;
//			fid.folderName = FOLDER_INBOX;
//		    fid.folderDes = res.getString(R.string.folder_global);
//			fid.activity = GlobalInBoxActivity.class;
//			fid.id=INDEX_GLOBAL;
//		    fid.totalCount = dbHelper.getInMailCount(-1, FOLDER_INBOX,
//		    MailStatus.MAIL_DELETED, true, true);
//			fid.unreadCount = dbHelper.getInMailCount(-1, FOLDER_INBOX,
//			MailStatus.MAIL_NEW, false, false);
//			itemDataList.add(fid);
						
			Account acct = AccountManager.getByIndex(i);
			FolderItemData fid = new FolderItemData();
			fid.image = R.drawable.inbox;
			fid.folderName = FOLDER_INBOX;
			fid.folderDes = res.getString(R.string.folder_inbox);
		    fid.activity = InBoxActivity.class;
		    fid.id=INDEX_INBOX;
		    fid.totalCount = dbHelper.getInMailCount(acct.id, FOLDER_INBOX,new int[]{MailStatus.MAIL_NEW,MailStatus.MAIL_READED}, false);
			fid.unreadCount = dbHelper.getInMailCount(acct.id, FOLDER_INBOX,new int[]{MailStatus.MAIL_NEW}, false);
			itemDataList.add(fid);
			
			 fid = new FolderItemData();
			 fid.image = R.drawable.draftbox;
			 fid.folderName = "";
			 fid.id=INDEX_WRITTEN_MAIL;
			 fid.folderDes = res.getString(R.string.folder_write);
			 fid.activity = OutBoxActivity.class;
			 fid.totalCount = dbHelper.getOutMailCount(acct.id,"",new int[]{MailStatus.MAIL_NEW,MailStatus.MAIL_READED});
			 itemDataList.add(fid);
			
//			 fid = new FolderItemData();
//			 fid.image = R.drawable.draftbox;
//			 fid.folderName = FOLDER_DRAFT;
//			 fid.id=INDEX_DRAFT;
//			 fid.folderDes = res.getString(R.string.folder_draft);
//			 fid.activity = OutBoxActivity.class;
//			 fid.totalCount = dbHelper.getOutMailCount(acct.id, FOLDER_DRAFT,new int[]{MailStatus.MAIL_NEW,MailStatus.MAIL_READED, MailStatus.FLAG_HAS_MORE_PLACEHOLD});
//			 itemDataList.add(fid);
//			 
//			 fid = new FolderItemData();
//			 fid.image = R.drawable.outbox;
//			 fid.folderName = FOLDER_NOTSENT;
//			 fid.id=INDEX_NOTSENT;
//			 fid.folderDes = res.getString(R.string.folder_notsentbox);
//			 fid.activity = OutBoxActivity.class;
//			 fid.totalCount = dbHelper.getOutMailCount(acct.id, FOLDER_NOTSENT,new int[]{MailStatus.MAIL_NEW,MailStatus.MAIL_READED, MailStatus.FLAG_HAS_MORE_PLACEHOLD});
//			 itemDataList.add(fid);		
//			 
//			 fid = new FolderItemData();
//			 fid.image = R.drawable.sentbox;
//			 fid.folderName = FOLDER_SENT;
//			 fid.id=INDEX_SENT;
//			 fid.folderDes = res.getString(R.string.folder_sentbox);
//			 fid.activity = OutBoxActivity.class;
//			 fid.totalCount = dbHelper.getOutMailCount(acct.id, FOLDER_SENT,new int[]{MailStatus.MAIL_NEW,MailStatus.MAIL_READED, MailStatus.FLAG_HAS_MORE_PLACEHOLD});
//			 itemDataList.add(fid);
				
			 fid = new FolderItemData();
			 fid.image = R.drawable.deletebox;
			 fid.id=INDEX_DELETE;
			 fid.folderName = FOLDER_DELETE;// FOLDER_INBOX;
			 fid.folderDes = res.getString(R.string.folder_delete);
			 fid.activity = DeleteBoxActivity.class;
			 itemDataList.add(fid);			
			folders = dbHelper.getFolders(acct.id);					
			if (folders.size() != 0)
			{
				for (int j = 0; j < folders.size(); j++)
				{
					fid = new FolderItemData();
					FolderInfo info = folders.get(j);
					if (info.folderName.equalsIgnoreCase("INBOX"))
					{
						continue;
					}
					
					
					fid.image = R.drawable.inbox;
					fid.folderName = info.folderName;
					fid.folderDes = info.displayName;
					fid.activity = InBoxActivity.class;
					fid.id = INDEX_SERVER;
					itemDataList.add(fid);

				}	
			}
			folderMap.put(acct.id, itemDataList);	
		}
		fa = new FolderAdapter(this);
		setListAdapter(fa);

		boolean doExpand = getIntent().getBooleanExtra("expandCurrentAccount", false);
		if (doExpand == true)
		{
			expandAccount();
		}
		else
			collapseAccount();

		updateMailCount(MyApp.currentAccount);
		fa.init();
		showFlag = false;
		fa.notifyDataSetChanged();
	}

	protected void onNewIntent(Intent intent)
	{
		fa.notifyDataSetChanged();
		setIntent(intent);
	}

	public void createNewAccount()
	{
		Account a = new Account();
		Intent intent = new Intent(this, AccountWizard.class);
		intent.putExtra("account", a);
		intent.putExtra("isNew", true);
		startActivityForResult(intent, R.layout.account_wizard);
	}

	public void deleteAccount(int groupPosition)
	{
		
		EnumSet<ButtonFlags> buttons = EnumSet.noneOf(ButtonFlags.class);
		buttons.add(ButtonFlags.Yes);
		buttons.add(ButtonFlags.No);
		Resources res = getResources();
		int checkFlag = DialogUtils.showModalMsgBox(
				this,
				String.format(
						res.getString(R.string.deleteAccountQuery), AccountManager.getByIndex(groupPosition).name),
						 res.getString(R.string.deleteAccountTitle), buttons);
		if (checkFlag == DialogResult.NO)
			return;
	    
//		int checkFlag = DialogUtils.showModalMsgBox(
//				this,
//				String.format(
//						res.getString(R.string.deleteAccountQuery), AccountManager.getByIndex(groupPosition).name),
//						 res.getString(R.string.deleteAccountTitle), buttons);
//		if (checkFlag == DialogResult.NO)
//			return;
		

		// update current account;
		Account toDel = AccountManager.deleteAccount(groupPosition);
		Account curAccount = MyApp.currentAccount;
		if (curAccount != null && curAccount.id == toDel.id) 
		{//current account is deleted
			if (!AccountManager.isEmpty())
				MyApp.currentAccount = AccountManager.getByIndex(0);
			else
				MyApp.currentAccount = null;
		}
		// update list view.
		showFlag = false;
		updateListView();
	}

	public void editAccount(int groupPosition)
	{
		Intent intent = new Intent(this, AccountWizard.class);

		Account a = AccountManager.getByIndex(groupPosition);
		oldAccount = a;
		intent.putExtra("account", a);
		intent.putExtra("isNew", false);
		startActivityForResult(intent, R.string.editStr);
	}

	@Override
	public void onActivityResult(int reqCode, int rstCode, Intent intent)
	{
		MyApp.setCurrentActivity(this);
		switch (reqCode)
		{
		case R.layout.account_wizard:// account wizard
		{
			if(rstCode == AccountWizard.RESULT_CANCELED)
			{
				return ;
			}
			Account a = (Account) intent.getSerializableExtra("account");
			// String errStr = DbHelper.getInstance().addAccount(a);
			AccountManager.addAccount(a);
			if(rstCode == AccountWizard.ACTRST_CHECKNEWMAIL){
					MyApp.currentAccount = a;
					MyApp.userSetting.currentAccountId = a.id;
					Intent intent1 = new Intent(this, InBoxActivity.class);
					intent1.putExtra("CheckMail", true);
					intent1.putExtra("accountID", a.id);
					startActivity(intent1);
					finish();

				break;
			}
			else if(rstCode == AccountWizard.ACTRST_NORMAL){
				// if it's the first account, return to mail box.
				if(MyApp.currentAccount == null){
					MyApp.currentAccount = a;
				}
				if (getIntent().getBooleanExtra(AccountManager.FirstAccount, false))
				{
					setResult(Dialog.BUTTON_POSITIVE, getIntent());
					finish();
				}
				showFlag = false;
				fa.notifyDataSetChanged();
				break;
			}
		}
		case R.string.editStr: // edit account
		{
			if (rstCode != Dialog.BUTTON_POSITIVE)
				return;
			Account a = (Account) intent.getSerializableExtra("account");
			// String errStr = DbHelper.getInstance().updateAccount(a,oldAccountName);
			AccountManager.updateAccount(oldAccount, a);
			showFlag = false;
			fa.notifyDataSetChanged();
			break;
		}
		}
	}

	public void onClick(View v)
	{
		switch (v.getId())
		{
		case R.id.editfolderTitlebtn:
			showFlag = !showFlag;
			updateListView();
			break;
		}
	}

	// }

	private void updateListView()
	{
		fa.init();
		fa.notifyDataSetChanged();
	}

}

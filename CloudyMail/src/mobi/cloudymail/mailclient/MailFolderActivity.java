package mobi.cloudymail.mailclient;

import static mobi.cloudymail.util.Utils.LOGTAG;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import mobi.cloudymail.calendar.Calendar_main;
import mobi.cloudymail.data.MailInfo;
import mobi.cloudymail.data.MailStatus;
import mobi.cloudymail.mailclient.net.Account;
import mobi.cloudymail.util.DialogResult;
import mobi.cloudymail.util.DialogUtils;
import mobi.cloudymail.util.MyApp;
import mobi.cloudymail.util.NewDbHelper;
import mobi.cloudymail.util.SoftwareUpdate;
import mobi.cloudymail.util.Utils;
import android.app.ExpandableListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.gesture.Gesture;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.gesture.Prediction;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public abstract class MailFolderActivity extends ExpandableListActivity implements
		android.view.View.OnClickListener,
		CompoundButton.OnCheckedChangeListener, OnGesturePerformedListener,
		OnGestureListener, OnTouchListener
{  
   public static Intent _pushIntent = null;

	// use global variable for mail viewer.
	// public static MailInfo curMailInfo = null;

	protected final int ACCOUNT_GRP_ID = 10;

	// region model data
	public int totalMailCount = 0;
	public int totalGroupCount=0;
	public MailInfo[] mails = null;
    private  Vector<MailGroup> groupVec=null;
	protected int mailCountPerScreen = 10;

	// current selected mail index in mail list view;
	protected int _currentMailIndex = -1;
	protected int _currentGroupIndex = -1;

	// store the account menu for dynamic operation.
	
	protected Button _delMailBtn = null;
	protected Button _markReadBtn = null;
	protected Button _markUnreadBtn = null;
	private Button _selectAllbtn = null;
	protected LinearLayout _selectMailLayout = null;
	protected MailSelection _checkedMails = new MailSelection();
	private ProgressBar _progressBar=null;
	
	// Define a variable is used to control the flow, give up parameters
	// transfer
	protected int orderType = NewDbHelper.ORDER_BY_DATE;

//	protected BaseAdapter _mailAdpt = null;
//	protected MailListAdapter _mailAdpt = null;
	protected MailGroupAdapter _mailAdpt = null;

	// for open mode;
	public static final String CLIENT_OPEN_MODE = "lient_open_type";
	public static final int OPEN_NEW = 0;
	
	public static final int OPEN_FROM_STATUS_BAR = 2;
	public static final int OPEN_FROM_GLOBAL_BOX = 3;
	public static final int OPEN_FROM_SEARCH = 4;

	protected int _openType = OPEN_NEW;
	protected String folderName = "";// FolderManager.FOLDER_INBOX;
	protected String floatingSearch;

	protected String _baseWinTitle = null;
	protected Button _titleLeftBtn;
//    public static Handler myHandler;
	public static GestureLibrary gestureLib;

	public int gesturePosition = -1;

	private int calendarDate = 0;
	private Drawable calendarIcon = null;
	
	private long updateTime = 0;// the time update mail was executed
	class MailSelection 
	{
		private boolean allSelected = false;
		private Set<MailInfo> _checkedMails = new HashSet<MailInfo>();
		public void selectAll()
		{
			allSelected = true;
			_checkedMails.clear();
		}
		public void clearSelection()
		{
			allSelected = false;
			_checkedMails.clear();
		}
		public boolean isEmpty()
		{
			return allSelected == false && _checkedMails.isEmpty();
		}
		public void select(MailInfo mail)
		{
			_checkedMails.add(mail);
		}
		public void deselect(MailInfo mail)
		{
			_checkedMails.remove(mail);
		}

		public Iterator<MailInfo> iterator()
		{
			if(allSelected)
			{
				return new Iterator<MailInfo>() {
					int pos = 0;
					@Override
					public boolean hasNext()
					{
						return pos < totalMailCount;
					}

					@Override
					public MailInfo next()
					{
						return getMail(pos++);	
					}

					@Override
					public void remove()
					{
						
						
					}
					
				};
			}
			return _checkedMails.iterator();
		}
		public boolean isSelected(MailInfo mailInfo)
		{
			return allSelected || _checkedMails.contains(mailInfo);
		}
	}

	//	public static String boxName=null;
//    }
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		MyApp.setCurrentActivity(this);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE); 
//		GestureOverlayView gestureOverLayView = new GestureOverlayView(this);
//		
//		
//		View inflate = getLayoutInflater().inflate(R.layout.main, null);
//		gestureOverLayView.addView(inflate);
//		gestureOverLayView.addOnGesturePerformedListener(this);
//		gestureLib = GestureLibraries.fromRawResource(this, R.raw.gestures);
//		if (!gestureLib.load())
//		{
//			finish();
//		}
//		gestureOverLayView.setGestureVisible(false);
//		setContentView(gestureOverLayView);
		setContentView(R.layout.main);
		_delMailBtn = (Button) findViewById(R.id.delMailBtn);
		_markReadBtn = (Button) findViewById(R.id.markReadBtn);
		_markUnreadBtn = (Button) findViewById(R.id.markUnreadBtn);
		_selectAllbtn = (Button) findViewById(R.id.selectAllBtn);
		// _moveBtn = (Button) findViewById(R.id.moveMailBtn);
		_selectMailLayout = (LinearLayout) findViewById(R.id.selectMailLayout);
		_delMailBtn.setOnClickListener(this);
		_markReadBtn.setOnClickListener(this);
		_markUnreadBtn.setOnClickListener(this);
		_selectAllbtn.setOnClickListener(this);
		// _moveBtn.setOnClickListener(this);

		_openType = getIntent().getIntExtra(CLIENT_OPEN_MODE, OPEN_NEW);

		// create data base file
		// ApplicationInfo info = getApplicationInfo();
		// settingFile = info.dataDir + "/setting.txt";


		MyApp.userSetting.loadSetting(this);

		ExpandableListView  lv = getExpandableListView();
		Resources resource = getBaseContext().getResources();
        Drawable divDrawable = resource.getDrawable(R.drawable.white);
//		lv.setDivider(divDrawable);
		lv.setGroupIndicator(null);
        lv.setDividerHeight(2);
		lv.setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState)
			{
				switch (scrollState)
				{
				case OnScrollListener.SCROLL_STATE_IDLE:
					gesturePosition = -1;
					if(_checkedMails.isEmpty())
					{
					updateMailView();
					}
					break;
				case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
					gesturePosition = -1;
					if(_checkedMails.isEmpty())
					{
					updateMailView();
					}
					break;
				case OnScrollListener.SCROLL_STATE_FLING:
					gesturePosition = -1;
					if(_checkedMails.isEmpty())
					{
					updateMailView();
					}
					break;
				}

			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount)
			{
				// TODO Auto-generated method stub

			}

		});
		lv.setOnGroupClickListener(new OnGroupClickListener() {
			@Override
			public boolean onGroupClick(ExpandableListView arg0, View itemView, int groupPosition,
					long arg3)
			{
				
				if(inGroupMode())
				{
					MailGroup g = getGroup(groupPosition);
					
					if(g.getGroupSize() == 1)
					{
						_currentGroupIndex = groupPosition;
						_currentMailIndex = 0;
						openMail(g.getMailInfo(0));
						return true;
					}
				} 
				else 
				{
					_currentMailIndex = groupPosition;
					openMail(getMail(groupPosition));
					return true;
				}
				return false;
			}
		});

		lv.setOnChildClickListener(new OnChildClickListener() {
			
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id)
			{
				_currentGroupIndex = groupPosition;
				_currentMailIndex = childPosition;
				MailGroup g = getGroup(groupPosition);
				openMail(g.getMailInfo(childPosition));
				
				
				TextView expandMarkView=(TextView)v.findViewById(R.id.expand_mark_child);
				expandMarkView.setVisibility(View.VISIBLE);
				expandMarkView.setHeight(v.getHeight());
				return true;
			}
		});	
		
//		win.setFeatureDrawableResource(	Window.FEATURE_LEFT_ICON,
//										R.drawable.cloudymail);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.titlebar);

		if (_pushIntent == null)
		{
			_pushIntent = new Intent(this, ReceiveMailService.class);
			// _pushIntent.putExtra(ReceiveMailService.PUSH_STRING,
			// userSetting.getPushFrequency());
			
			startService(_pushIntent);
			MyApp.instance().setPollInterval(MyApp.userSetting.getPushFrequency());
			PushMailClient.getInstance().startPush(this);
		}
		initBaseTitle();
		MyApp.addActivity(this);
		

		try
		{
			updateMail();
		}
		catch (SQLException e)
		{
			Log.d(Utils.LOGTAG, "",e);
		}

//		((PullToRefreshListView) getExpandableListView()).measureHeader();
//		((PullToRefreshListView) getExpandableListView()).measureHeader();
		
		
	}


    
	@Override
	protected void onStart()
	{
		super.onStart();
	}
	protected abstract boolean inReceiving();
	@Override
	protected void onResume()
	{
		super.onResume();
		MyApp.setCurrentActivity(this);
		initBaseTitle();
		updateWindowTitle();
		if(!inReceiving()&&getIntent().getBooleanExtra("currentAccountChanged", false))
		{
			setProgressBarVisible(false);
			((PullToRefreshListView)getExpandableListView()).onRefreshComplete(true);

		}
		try
		{
			updateMail();
		}
		catch (SQLException e)
		{
			Log.e(Utils.LOGTAG, "updateMail fail", e);
		}
		if (gesturePosition != -1)
		{
			gesturePosition = -1;
			updateMailView();
		}
		
//		((PullToRefreshListView) getListView()).scrollBy(0, 50);

	}
	abstract protected void initBaseTitle();

	protected Account currentAccount()
	{
		return MyApp.currentAccount;
	}

	protected void setCurrentAccount(Account a)
	{
		MyApp.currentAccount = a;
	}

	private void saveStatus()
	{
		/*
		 * try { FileOutputStream fos = new FileOutputStream(settingFile);
		 * ObjectOutputStream out = new ObjectOutputStream(fos);
		 * out.writeObject(userSetting); } catch (Exception e) { // TODO: handle
		 * exception e.printStackTrace(); }
		 */
	}
    
	@Override
	protected void onStop()
	{
		super.onStop();
	}

	@Override
	public void onDestroy()
	{
		saveStatus();
		super.onDestroy();
		// DbHelper.getInstance().disconnect();
		// AccountManager.accounts.clear();
	}

	abstract protected int getUnreadMailcount(boolean query);

	abstract protected int getTotalMailCount(boolean query);
	abstract protected int getTotalGroupCount(boolean query);

	protected boolean isLocalInBoxActivity() 
	{
		return false;
	}
	public void updateMail() throws SQLException
	{
//		if(updateTime >= NewDbHelper.getInstance().getModificationTime())
//		{
//			return;
//		}
//		updateTime = NewDbHelper.getInstance().getModificationTime();
		if (MyApp.currentAccount != null )
		{
			{
				/*
				 * NewDbHelper dbHelper = NewDbHelper.getInstance();
				 * totalMailCount = dbHelper .executScalar(
				 * "select count(*) from mail where state!=? and accountId=? and folder=?"
				 * , new String[] { MailStatus.MAIL_DELETED + "",
				 * MyApp.currentAccount.id + "", folderName + "" });
				 */
				totalMailCount = getTotalMailCount(true);
                totalGroupCount=getTotalGroupCount(true);
				mails = newMailInfoArray(totalMailCount);// new
				groupVec= newMailGroupArray(totalGroupCount);										// MailInfo[totalMailCount];
				// setTitle(getWindowTitle());
				updateTitle();
				/*
				 * if(unreadedMailCount > 0)
				 * setTitle(getResources().getString(R.string.appName)
				 * +" "+"["+unreadedMailCount+"/"+ totalMailCount + "]"); else
				 * setTitle(getResources().getString(R.string.appName)
				 * +" "+"["+totalMailCount + "]");
				 */
			}

		}
		else
		{
			totalMailCount = 0;
			mails = newMailInfoArray(totalMailCount);// new
			groupVec=newMailGroupArray(totalMailCount);											// MailInfo[totalMailCount];
			setTitle(getResources().getString(R.string.appName));
		}
		if(groupVec!=null)
		{
			if (_mailAdpt == null)
			 {
				_mailAdpt = new MailGroupAdapter(groupVec,this);
				((PullToRefreshListView) getExpandableListView()).setAdapter(_mailAdpt);
		    }
			else
			{
				_mailAdpt.setGroups(groupVec);
				_checkedMails.clearSelection();
			}
		}

		Set<Long> groupIdSet = MyApp.getNewMailGroups();
		Iterator<Long> it = groupIdSet.iterator();
		while(it.hasNext())
		{
			expandGroup(it.next());
		}

	}
	private Button titleLeftBtn;

	private Button titleRightBtn;


	public void updateMailView()
	{
		if (_mailAdpt == null)
		{
			_mailAdpt = new MailGroupAdapter(groupVec,this);
//			((PullToRefreshListView) getExpandableListView()).setAdapter(_mailAdpt);
			 getExpandableListView().setAdapter(_mailAdpt);
		}
		else
			_mailAdpt.setGroups(groupVec);
		_checkedMails.clearSelection();
//		((PullToRefreshListView) getExpandableListView().measureHeader();
	}

	/**
	 * 
	 * This interface query from database and return the cursor. Subclass needs
	 * to implement this interface;
	 * 
	 * @param index
	 * @return
	 */
	protected abstract Cursor doQueryMail(int index);
	protected abstract Cursor doQueryGroup(int index);

	protected abstract MailInfo newMailInfo(Cursor cursor);
//	protected abstract MailGroup newMailGroup(Cursor cursor);
	protected abstract MailInfo[] newMailInfoArray(int count);
	protected abstract Vector<MailGroup> newMailGroupArray(int count);
	protected abstract MailGroup newMailGroup(Cursor cursor);

	protected int getCurrentAccountId()
	{
		return MyApp.currentAccount.id;
	}

	MailGroup getGroup(int index)
	{
		Utils.ASSERT(index >= 0 && index < totalGroupCount);
			if (groupVec.get(index) == null)
			{
				long curTime = System.currentTimeMillis();
				Cursor cursor = null;
				try
				{
					cursor = doQueryGroup(index);
					int offset = 0;
					if (cursor.moveToFirst())
					{
						do
						{
					/*
					* 
					  for(int i=0;i<groupVec.size();i++)
		              {
		            	  if(info.getDate().before(groupVec.get(i).getDate()))
		            	  groupVec.insertElementAt(info,i);
		            	  break;
		              }
					 * */
	//						
							MailGroup info = newMailGroup(cursor);
							groupVec.set(index+offset, info);
							offset++;
						} while (cursor.moveToNext());
	//					Vector<MailGroup> moreGroupVec = getMoreGroup(index,MailStatus.FLAG_HAS_MORE_PLACEHOLD);
	//					insertToGroupVec(groupVec,moreGroupVec);
						
					}
					else
						Log.d(LOGTAG,"MailFolderActivity"+
								"Fail to query mail, got 0 mail");
					cursor.close();
				}
				catch (Exception e)
				{
					Log.d(Utils.LOGTAG, "",e);
				}
				finally
				{
					if(cursor != null)
						cursor.close();
				}
				
				long cost = System.currentTimeMillis() - curTime;
				Log.i(Utils.LOGTAG, "getGroup spend time(ms):"+cost);
			}
		
		return groupVec.get(index);
		
	}
//	MailGroup getGroup(int index)
//	{
//		
//		Utils.ASSERT(index >= 0 && index < totalGroupCount);
//		if (groupVec.get(index) == null)
//		{
//			try
//			{
//				Cursor cursor = doQueryGroup(index);
//				int offset = 0;
//				if (cursor.moveToFirst())
//				{
//					do
//					{
//						/*
//						 * 
//				  for(int i=0;i<groupVec.size();i++)
//	              {
//	            	  if(info.getDate().before(groupVec.get(i).getDate()))
//	            	  groupVec.insertElementAt(info,i);
//	            	  break;
//	              }
//						 * */
//						MailGroup info = newMailGroup(cursor);
//						groupVec.set(index+offset, info);
//						offset++;
//					} while (cursor.moveToNext());
//				}
//				else
//					Log.d(LOGTAG,"MailFolderActivity"+
//					"Fail to query mail, got 0 mail");
//				cursor.close();
//			}
//			catch (Exception e)
//			{
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		return groupVec.get(index);
//	}
	
	MailInfo getMail(int index)
	{
		Utils.ASSERT(index >= 0 && index < totalMailCount);
		if (mails[index] == null)
		{
			try
			{
				// rst = DbHelper.getInstance().getMails(currentAccount.id,
				// Math.min(mailCountPerScreen, mails.length), index);
				Cursor cursor = doQueryMail(index);

				int offset = 0;
				// while (rst.next())
				if (cursor.moveToFirst())
				{
					do
					{

						MailInfo info = newMailInfo(cursor);
						//Add into the HashMap
//						MailInfo suffix = findSuffix(info);
//						MailGroup grp = groupMap.get(suffix);
//						if(grp==null)
//						{
//							grp=new MailGroup(suffix);
//							groupMap.put(suffix, grp);
//						}
//						grp.addChild(info);				    
						
						mails[index + offset] = info;

						offset++;
					} while (cursor.moveToNext());
				}
				else
					Log.d(LOGTAG,"MailFolderActivity"+
							"Fail to query mail, got 0 mail");
				cursor.close();
			}
			catch (Exception e)
			{
				Log.d(Utils.LOGTAG, "",e);
			}
		}
		return mails[index];
	}

//	@Override
//	public void onBackPressed()
//	{
//		super.onBackPressed();
//	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		Date d = new Date();
		if(d.getDate() != calendarDate)
		{
			calendarDate=d.getDate();
			BitmapFactory.Options opt = new BitmapFactory.Options();
			DisplayMetrics metrics = getResources().getDisplayMetrics();
			opt.inDensity = 160;
			opt.inTargetDensity = metrics.densityDpi;
			Bitmap bmp = BitmapFactory.decodeResource(MyApp.instance().getResources(), R.drawable.calendar, opt).copy(Bitmap.Config.ARGB_8888, true);
			
			int daySizeBase=0;
			int dayXEdgeBase=0;
			int dayYEdgeBase=0;
			int weekSizeBase=0;
			int weekYEdgeBase=0;
			
			
			if(metrics.densityDpi==DisplayMetrics.DENSITY_HIGH) {
				daySizeBase=40;
				dayXEdgeBase=50;
				dayYEdgeBase=60;
				weekSizeBase=18;
				weekYEdgeBase=20;
			}
			else if(metrics.densityDpi==DisplayMetrics.DENSITY_XHIGH)
			{
				daySizeBase=48;
				dayXEdgeBase=36;
				dayYEdgeBase=75;
				weekSizeBase=24;
				weekYEdgeBase=28;
			}
			else {
				daySizeBase=25;
				dayXEdgeBase=36;
				dayYEdgeBase=38;
				weekSizeBase=12;
				weekYEdgeBase=14;
			}
			Canvas c = new Canvas(bmp);
			Typeface font = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
			Paint mp = new Paint(Paint.ANTI_ALIAS_FLAG);
			mp.setTypeface(font);
			Log.d(Utils.LOGTAG, "Canvas size"+c.getWidth() +" bmp size:"+bmp.getWidth());
			double scale = bmp.getDensity()/160.0;
			mp.setTextSize((int)(daySizeBase*scale));
			mp.setARGB(0xff, 0x3f, 0x3f, 0x3f);
			String str = calendarDate+"";
//			float wOfst = mp.measureText(str)/2;
//			c.drawText(str, (int)(dayXEdgeBase*scale)-wOfst, (int)(dayYEdgeBase*scale), mp);
			float wOfst= (bmp.getWidth()-mp.measureText(str))/2;
			c.drawText(str, wOfst, (int)(dayYEdgeBase*scale), mp);
			
			mp.setTextSize((int)(weekSizeBase*scale));
			mp.setColor(Color.WHITE);
			String week=Utils.getWeekOfDate(d);
			float wOfst1 = (bmp.getWidth()-mp.measureText(week))/2;
			c.drawText(week,wOfst1, (int)(weekYEdgeBase*scale), mp);
			calendarIcon = new BitmapDrawable(bmp);
			
//			calendarIcon = getResources().getDrawable(R.drawable.calendar); //works as expected
			//calendarIcon = new BitmapDrawable(BitmapFactory.decodeResource(MyApp.instance().getResources(), R.drawable.calendar, opt));
		}
		MenuItem calendMenu = menu.getItem(4);
		Log.d(Utils.LOGTAG, "Icon width:"+calendarIcon.getBounds().width());
		calendMenu.setIcon(calendarIcon);
		//calendMenu.setIcon(R.drawable.calendar);
		return doCreateOptionsMenu(menu);
	}

	protected boolean doCreateOptionsMenu(Menu menu)
	{
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		// case R.id.accountManagerMenu:
		// {
		// Intent intent = new Intent(this, AccountManager.class);
		// startActivityForResult(intent, R.layout.account_manager);
		// break;
		//
		// }
		
		case R.id.calendarMenu:
		{
			Intent intent = new Intent(this, Calendar_main.class);
			startActivity(intent);
			break;
		}
		case R.id.composeMailMenu:
		{
			if(!AccountManager.checkAccountAndCreate(this))
				break;
			Intent intent = new Intent(this, Composer.class);
			intent.putExtra("composer_type", Composer.COMPOSER_NEWMAIL);
			startActivityForResult(intent, R.layout.composer);
			break;
		}
		case R.id.addressBookItem:
		{
			Intent intent = new Intent(this, AddressBook.class);
			startActivity(intent);
			break;
		}
//		case R.id.accountMenu:
//		{
//			Intent intent = new Intent(this, AccountManager.class);
//			startActivity(intent);
//			// updateAccountMenu(item.getSubMenu());
//			break;
//		}
		case R.id.settingMenu:
		{
			Intent intent = new Intent(this, SettingPage.class);
			startActivity(intent);
			break;
		}
		case R.id.aboutMenu:
		{
			Resources res = getResources();
			String titleStr = res.getString(R.string.about_title);
			String msg = res.getString(R.string.appName) + " "
							+ res.getString(R.string.about_version);
			msg += "\n\n" + res.getString(R.string.about_copyright);
			DialogUtils.showMsgBox(this, msg, titleStr);
		
			return super.onOptionsItemSelected(item);
		}
		case R.id.updateMenu:
		{
			SoftwareUpdate.getInstance().checkUpdate(false);
			return super.onOptionsItemSelected(item);
		}
		case R.id.exitApp:
			try
			{
				Resources res = getResources();
				if (DialogUtils.showModalMsgBox(this, res
						.getString(R.string.exitQuestion), res
						.getString(R.string.exit), EnumSet
						.of(DialogUtils.ButtonFlags.Yes,
							DialogUtils.ButtonFlags.No)) == DialogResult.OK)
				{
//					Debug.stopMethodTracing();
					saveStatus();
					if(ReceiveMailService._instance != null && ReceiveMailService._instance != null)
					{
						if (!stopService(_pushIntent))
							Log.d(LOGTAG,	"MailClient ReceiveMailService is not stopped!");
					}
				}
			}
			catch (Exception e)
			{
				Log.d(Utils.LOGTAG, "",e);
			}
			break;
		case R.id.searchMenu:
//			updateMailView();
			onSearchRequested();
			break;
		case R.id.feedbackMenu:
			 Intent intent=new Intent(this,FeedbackActivity.class);
			 startActivity(intent);
			break;
		case R.id.attachManager:
			{
			//	Intent attManagerIntent=new Intent(this,AttachManager.class);
				Intent attManagerIntent=new Intent(this,NewAttachmentManager.class);
				startActivity(attManagerIntent);
			}
		break;
		default:
			break;
		}
		/*
		 * if (item.getGroupId() == ACCOUNT_GRP_ID) { // item.setChecked(true);
		 * String actName = item.getTitle().toString(); // Toast.makeText(this,
		 * actName, Toast.LENGTH_SHORT).show(); if
		 * (!actName.equals(MyApp.currentAccount.name))// not the same account;
		 * { Account a = AccountManager.getAccount(actName); if (a == null)
		 * return super.onOptionsItemSelected(item); Account oldCurAcct =
		 * MyApp.currentAccount; MyApp.currentAccount = a;
		 * MyApp.userSetting.currentAccountId = a.id; try { updateMail();
		 * item.setChecked(true); } catch (SQLException exp) { // restore;
		 * MyApp.currentAccount = oldCurAcct; MyApp.userSetting.currentAccountId
		 * = MyApp.currentAccount.id; DialogUtils.showMsgBox( this,
		 * "Failed to change account: " + exp.getMessage(), getResources()
		 * .getString(R.string.error)); }
		 * 
		 * } } else
		 */
		if (item.getGroupId() == R.id.sort_menu_group)
		{
		int newOrder = orderType;
		switch (item.getItemId())
		{
		// case R.id.sort:
		// selectSortMode(item);
		// break;
		case R.id.sortDateItem:
			newOrder = NewDbHelper.ORDER_BY_DATE;
			break;
		case R.id.sortFromItem:
			newOrder = NewDbHelper.ORDER_BY_FROM;
			break;
		case R.id.sortSubjectItem:
			newOrder = NewDbHelper.ORDER_BY_SUBJECT;
			break;
		case R.id.sortReadedItem:
			newOrder = NewDbHelper.ORDER_BY_READEDSTATE;
			break;
		case R.id.sortAsteriskItem:
			newOrder =NewDbHelper.ORDER_BY_ASTERISK;
			break;
		}
		if (newOrder != orderType)
		{
			orderType = newOrder;
			item.setChecked(true);
			try
			{
				updateMail();
			}
			catch (SQLException e)
			{
				Log.d(Utils.LOGTAG, "",e);
			}
		}
	}
		return super.onOptionsItemSelected(item);
	}



	abstract protected void openMail(MailInfo mailInfo);

	private void switchSelectallBtn(boolean selectAllFlag)
	{
		String txt;
		if (selectAllFlag)
			txt = getResources().getString(R.string.op_selectAll);
		else
			txt = getResources().getString(R.string.op_disselectAll);
		_selectAllbtn.setText(txt);
	}

	/**
	 * Monitor the back button events
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		//搜索界面执行正常返回功能
		if (floatingSearch == null || floatingSearch.equals(""))
		{
			return super.onKeyDown(keyCode, event);
		}
		else if (keyCode == KeyEvent.KEYCODE_BACK
					&& event.getRepeatCount() == 0)
		{
			floatingSearch = "";
			try
			{
				updateMail();
				updateSearchNotification();
			}
			catch (SQLException e)
			{
				Log.d(Utils.LOGTAG, "",e);
			}
			return true;
		}
		return false;

	}

	private void updateSearchNotification() {
		TextView searchText=(TextView) findViewById(R.id.titleTextView);
	    if(searchText.isShown()==true)
	    {
	    	searchText.setVisibility(View.INVISIBLE);
	    	titleLeftBtn.setVisibility(View.VISIBLE);
			titleRightBtn.setVisibility(View.VISIBLE);
	    }
	    else
	    {
	    	titleLeftBtn.setVisibility(View.INVISIBLE);
			titleRightBtn.setVisibility(View.INVISIBLE);
	    }
	    
	}



	public void onClick(View v)
	{
		MailInfo mailInfo = null;
		switch (v.getId())
		{
		case R.id.delMailBtn: //the delete button in the bottom of screen
			updateMailStatus(MailStatus.MAIL_LOCAL_DELETED);
			
			break;
		case R.id.delMail: //the red cross image when finger gesture finished
			mailInfo = (MailInfo) v.getTag();

			HashSet<MailInfo> toDel = new HashSet<MailInfo>();
			toDel.add(mailInfo);
			updateMailStatus(toDel.iterator(), MailStatus.MAIL_LOCAL_DELETED);
			
			gesturePosition = -1;
			try
			{
				updateMail();
			}
			catch (SQLException e)
			{
				Log.d(Utils.LOGTAG, "",e);
			}			
			break;
		case R.id.starTargetBtn:
			 
			MailInfo m = (MailInfo) v.getTag();
			 
			if(m.getAsterisk() == 0)
			{
				v.setBackgroundResource(R.drawable.btn_star_big_buttonless_on);
				doUpdateAsteriskStates(m, 1);
			}
			else
			{
				v.setBackgroundResource(R.drawable.btn_star_big_buttonless_off);
				doUpdateAsteriskStates(m, 0);
			}
			break;
		case R.id.selectAllBtn:
		{
//			if (_checkedMails.size() == mails.length)// then do deselect all
			if(_checkedMails.allSelected)
			{
				_checkedMails.clearSelection();
				switchSelectallBtn(true);
				_selectMailLayout.setVisibility(View.GONE);
			}
			else
			{
				_checkedMails.selectAll();
				updateMarkButton();
				switchSelectallBtn(false);
			}
			_mailAdpt.notifyDataSetChanged();
		}
			break;
		case R.id.markReadBtn:
			updateMailStatus(MailStatus.MAIL_READED);
			break;
		case R.id.markUnreadBtn:
			updateMailStatus(MailStatus.MAIL_NEW);
			break;
		case R.id.titleLeftBtn:
			if(MyApp.currentAccount!=null)
			{
			Intent accountIntent = new Intent();
			accountIntent.setClass(this, FolderActivity.class);
			accountIntent.putExtra("expandCurrentAccount", true);
			accountIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(accountIntent);
			}
			break;
		case R.id.titleRightBtn:
			Intent folderIntent=new Intent(this,FolderActivity.class);
			startActivity(folderIntent);
			break;
		default:
			break;
		}
	}

	abstract protected String doUpdateMailStates(Iterator<MailInfo> mails,
			int state);
	
	abstract protected  void doUpdateAsteriskStates(MailInfo mail,
			int asteriskState);
	


	protected boolean shouldRebuildMailList(int state)
	{
		return state == MailStatus.MAIL_LOCAL_DELETED;
	}

	protected void updateMailStatus(Iterator<MailInfo> mails,  int state)
	{
		String errStr = doUpdateMailStates(mails, state);
		/*
		 * String errStr = NewDbHelper.getInstance()
		 * .updateMailStatus(accountIds, uids, state);
		 */
		if (!errStr.equals(""))
		{
			DialogUtils.showMsgBox(	this, errStr,
									getResources().getString(R.string.error));
			return;
		}
		NewDbHelper.getInstance().updateMailGroupState();
		// update list view;

		if (shouldRebuildMailList(state))
		{
			try
			{
				updateMail();
			}
			catch (Exception e)
			{
				Log.d(Utils.LOGTAG, "",e);
			}
		}
		else
			_mailAdpt.notifyDataSetChanged();
	}
	protected void updateMailStatus(int state)
	{
		
		if (_checkedMails.isEmpty())
			return;
		// update status in database
		String errStr = doUpdateMailStates(_checkedMails.iterator(), state);
		/*
		 * String errStr = NewDbHelper.getInstance()
		 * .updateMailStatus(accountIds, uids, state);
		 */
		if (!errStr.equals(""))
		{
			DialogUtils.showMsgBox(	this, errStr,
									getResources().getString(R.string.error));
			return;
		}
		NewDbHelper.getInstance().updateMailGroupState();
		// update list view;
		_selectMailLayout.setVisibility(View.GONE);
		_checkedMails.clearSelection();
		if (shouldRebuildMailList(state))
		{
			try
			{
				updateMail();
			}
			catch (Exception e)
			{
				Log.d(Utils.LOGTAG, "",e);
			}
		}
		else
			_mailAdpt.notifyDataSetChanged();
	}

	protected void updateMarkButton()
	{
	}

	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
	{
		if (gesturePosition != -1)
		{
			gesturePosition = -1;
			updateMailView();
		}
		MailInfo mailInfo = (MailInfo) buttonView.getTag(); // <-- get the position
		// Toast.makeText(this, "position is "+position,
		// Toast.LENGTH_SHORT).show();
		if (isChecked)
			_checkedMails.select(mailInfo);
		else
		{
			_checkedMails.deselect(mailInfo);
		}
		updateMarkButton();
		switchSelectallBtn(!_checkedMails.allSelected);
		_selectMailLayout.setVisibility(_checkedMails.isEmpty()	? View.GONE
																: View.VISIBLE);
	}

	public static class ViewHolder
	{
			Button starBtn;
			Button mailDelBtn;
			View calendarFlagView;
			View normalAttachmentFlagView;
			View replyFlagView;
			View forwardFlagView;
			TextView dateTxt;
			TextView fromTxt;
			TextView draftTxt;
			TextView subjectTxt;
			CheckBox mailItemCtx;
			TextView groupFlag;
			
			Button newMailBtn;
		}

		@Override
	public boolean onSearchRequested()
	{
     
		// String text=etdata.getText().toString();
		Bundle bundle = new Bundle();
		bundle.putString("data", "text");

		/**
		 * 打开浮动搜索框（第一个参数默认添加到搜索框的值） bundle为传递的数据
		 */
		startSearch("", false, bundle, false);
		/**
		 * 这个地方一定要返回真 如果只是super.onSearchRequested方法不但
		 * onSearchRequested（搜索框默认值）无法添加到搜索框中,bundle也无法传递出去
		 */
		return true;
	}

	/**
	 * @param mailInfo
	 * @param holder
	 * @param convertView
	 * 
	 */
	protected void updateMailItemView(MailInfo mailInfo, ViewHolder holder, View convertView)
	{
		// TODO Auto-generated method stub
		
	}



	/**
	 * 
	 @Override public void onNewIntent(Intent intent){
	 *           super.onNewIntent(intent); //获得搜索框里值 String
	 *           query=intent.getStringExtra(SearchManager.QUERY);
	 *           tvquery.setText(query); //保存搜索记录 SearchRecentSuggestions
	 *           suggestions=new SearchRecentSuggestions(this,
	 *           SearchSuggestionSampleProvider.AUTHORITY,
	 *           SearchSuggestionSampleProvider.MODE);
	 *           suggestions.saveRecentQuery(query, null);
	 *           if(Intent.ACTION_SEARCH.equals(intent.getAction())){ //获取传递的数据
	 *           Bundle bundled=intent.getBundleExtra(SearchManager.APP_DATA);
	 *           if(bundled!=null){ String ttdata=bundled.getString("data");
	 *           tvdata.setText(ttdata); }else{ tvdata.setText("no data"); } } }
	 */
	public static void clearSearchHistory()
	{
		SearchRecentSuggestions suggestions = new SearchRecentSuggestions(
																			MyApp.instance(),
																			SearchSuggestionSampleProvider.AUTHORITY,
																			SearchSuggestionSampleProvider.MODE);
		suggestions.clearHistory();
	}

	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		setIntent(intent);
		if (Intent.ACTION_SEARCH.equals(intent.getAction()))
		{
           
			String query = intent.getStringExtra(SearchManager.QUERY);
			/***** API ****/
			/**
			 * SearchSuggestions suggestions = new SearchSuggestions(this,
			 * MySuggestionsProvider.AUTHORITY, MySuggestionsProvider.MODE);
			 * suggestions.saveRecentQuery(queryString, null);
			 */
			SearchRecentSuggestions suggestions = new SearchRecentSuggestions(
																				this,
																				SearchSuggestionSampleProvider.AUTHORITY,
																				SearchSuggestionSampleProvider.MODE);
			suggestions.saveRecentQuery(query, null);

			floatingSearch = intent.getStringExtra(SearchManager.QUERY);
			if(inGroupMode()==false)
				try
				{
					updateTitle();
				}
				catch (Exception e)
				{
					Log.d(Utils.LOGTAG, "",e);
				}
				/**
				 * String searchValue = intent.getStringExtra(SearchManager.QUERY);
				 * SearchRecentSuggestions suggestions = new
				 * SearchRecentSuggestions( this,
				 * SearchSuggestionSampleProvider.AUTHORITY,
				 * SearchSuggestionSampleProvider.MODE);
				 * suggestions.saveRecentQuery(searchValue, null);
				 * 
				 * Bundle bundled = intent.getBundleExtra(SearchManager.APP_DATA);
				 * if (bundled != null) { String query = bundled.getString("data");
				 * try { updateMail(); } catch (SQLException e) {
				 * e.printStackTrace(); } }
				 **/
			}
	}

	@Override
	public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture)
	{
		ArrayList<Prediction> predictions = MailFolderActivity.gestureLib
				.recognize(gesture);
		if (predictions.size() > 0 && predictions.get(0).score > 1.0)
		{
			String action = predictions.get(0).name;
			// if ("action_refreshMail".equals(action))
			// {
			// Toast.makeText(this, "Adding a contact", Toast.LENGTH_SHORT)
			// .show();
			// }
			if ("action_deleteMail".equals(action)
				|| "action_replyMail".equals(action))
			{ 
				/* 得到手势的矩形区域 */
				RectF gestureBounding = gesture.getBoundingBox();
//				float bottom = gestureBounding.bottom;
//				float left = gestureBounding.left;
//				float right = gestureBounding.right;
//				float top = gestureBounding.top;

				int centerX = (int) gestureBounding.centerX(); 
				int centerY = (int) gestureBounding.centerY();

				if (centerX < 5 && centerY < 5)
				{
					return;
				}
				else
				{
//					final String TAG = "gesturePostion";
//					Log.d(LOGTAG, "debug" + gestureBounding.centerX() + "botton"
//								+ " " + bottom + " " + "left" + " " + left
//								+ " " + "right" + " " + " " + right + " "
//								+ "top" + " " + top);

					ListView lv = getExpandableListView();
					gesturePosition = lv.pointToPosition(centerX, centerY)-1;

					updateMailView();
				}

			}
			// else if ("action_replyMail".equals(action))
			// {
			// Toast.makeText(this, "Reloading contacts", Toast.LENGTH_SHORT)
			// .show();
			// }
		}

	}


	// public static boolean delBtnIsVisable = false;

	/**
	 * The method onGestureListener onTouchListener rewrite
	 * 
	 * @param v
	 * @param event
	 * @param velocityX
	 *            ：On the X axis movement speed, pixel/SEC
	 * @param velocityY
	 *            ：On the Y axis movement speed, pixel/SEC
	 * @return
	 */
	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		return false;
	}

	@Override
	public boolean onDown(MotionEvent e)
	{
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY)
	{
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e)
	{
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY)
	{
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e)
	{
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e)
	{
		return false;
	}
	
	protected void updateTitle()
	{
		if(!Utils.isEmpty(floatingSearch))
		{
			String titleSearch=getResources().getString(R.string.searchprompt)+" "+":"+" "+floatingSearch;
//			setTitle(titleSearch);
			TextView textView=(TextView) findViewById(R.id.titleTextView);
			textView.setVisibility(View.VISIBLE);
			textView.setText(titleSearch);
			titleLeftBtn.setVisibility(View.INVISIBLE);
			titleRightBtn.setVisibility(View.INVISIBLE);
		}
		else
		{
			int unreadedMailCount = getUnreadMailcount(true);
														// MailInfo[totalMailCount];
			// setTitle(getWindowTitle());
			StringBuffer winTitle = new StringBuffer(_baseWinTitle);
			winTitle.append(" [");
			if (unreadedMailCount >= 0)
				winTitle.append(unreadedMailCount + "/");
			winTitle.append(totalMailCount + "] ");
			if (getCurrentAccountId() >= 0)
				winTitle.append(currentAccount().name);
			setTitle(winTitle);
		}
		updateWindowTitle();
	}
	
	protected void updateWindowTitle()
	{
		_progressBar = (ProgressBar)findViewById(R.id.progressBar);
		 titleLeftBtn=(Button)findViewById(R.id.titleLeftBtn);
		 titleRightBtn=(Button)findViewById(R.id.titleRightBtn);
		String boxName = _baseWinTitle;
		if(MyApp.currentAccount != null)
		{
			
//			View titleBarInflate=getLayoutInflater().inflate(R.layout.titlebar, null);
			int displayNumber = 0;
			
			if(isLocalInBoxActivity()) {
			    displayNumber = NewDbHelper.getInstance().getInMailCount(getCurrentAccountId(), folderName, 
			    		                                                 new int[]{MailStatus.MAIL_NEW,MailStatus.MAIL_READED},
						                                                 true, floatingSearch);
			}
			else 
			{
				displayNumber = getTotalMailCount(true);
			}
		    titleLeftBtn.setText(boxName+"("+String.valueOf(displayNumber+")"));
			titleRightBtn.setText(MyApp.currentAccount.toString());
			Log.d(LOGTAG, MyApp.currentAccount.toString());
		}
		else
		{
			titleLeftBtn.setText(boxName);
			titleRightBtn.setText(R.string.add_account_titleRight);
		}
		titleLeftBtn.setOnClickListener(this);
		titleRightBtn.setOnClickListener(this);
		
		
	}
	
	protected void setProgressBarVisible(boolean value)
	{
		_progressBar = (ProgressBar)findViewById(R.id.progressBar);
		if(_progressBar==null)
			return;
		_progressBar.setVisibility(value?View.VISIBLE:View.GONE);
	}
	
	protected boolean inGroupMode()
	{
		if(!Utils.isEmpty(floatingSearch))
		{
			return false;
		}
		return orderType == NewDbHelper.ORDER_BY_DATE || orderType == NewDbHelper.ORDER_BY_SUBJECT;
	}


	public void expandGroup(long groupId)
	{
		ExpandableListView  lv = getExpandableListView();
		for(int i=0;i<groupVec.size() ;i++) //need know real mail coun
		{
			if(i>10)
				Log.w(Utils.LOGTAG, "search too long in mail group");
			if(getGroup(i).getId() == groupId)
			{
				lv.expandGroup(i);
				return;
			}

		}
	}

	
}

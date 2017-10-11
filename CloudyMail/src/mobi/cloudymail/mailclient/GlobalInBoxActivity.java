package mobi.cloudymail.mailclient;

import static mobi.cloudymail.util.Utils.LOGTAG;
import mobi.cloudymail.data.MailInfo;
import mobi.cloudymail.mailclient.net.Account;
import mobi.cloudymail.util.MyApp;
import mobi.cloudymail.util.NewDbHelper;
import mobi.cloudymail.util.SoftwareUpdate;
import mobi.cloudymail.util.Utils;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.widget.Toast;

public class GlobalInBoxActivity extends InBoxActivity
{
	private long mExitTime;
	private static boolean firstLoad = true;
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
//		Debug.startMethodTracing("cloudmail", 50*1024*1024);
		_excludeHasMoreMail = true;
		Log.d(LOGTAG,getClass().getName()+ "onCreate()");
		if(firstLoad)
		{
			firstLoad = false;
			SoftwareUpdate.getInstance().checkUpdate(true);
		}
		doReceiveMails(0, true);
	}
	
	protected boolean isLocalInBoxActivity() 
	{
		return false;
	}
	
	@Override
	protected void initBaseTitle()
	{
		_baseWinTitle = getResources().getString(R.string.folder_global);
		
	}
	@Override
	protected void beforeOpenMail(MailInfo mailInfo)
	{
		MyApp.setCurrentMailInfo(mailInfo);
	}
//	
//	@Override
//	protected boolean doCreateOptionsMenu(Menu menu)
//	{
//		return true;
//	}
	
	@Override
	protected int getCurrentAccountId()
	{
		return -1;
	}
	/**
	 * 
	 * @param mailUidxCeil
	 *            the maximux uidx of mail want to receive. -1 to indicate no
	 *            limit
	 */
	protected void doReceiveMails(int mailUidxCeil, boolean beQuiet)
	{
		Utils.ASSERT(MyApp.currentAccount != null);
		((PullToRefreshListView)getExpandableListView()).enforceToRefreshState();
//		((PullToRefreshListView)getExpandableListView()).enforceToRefreshState();
		recvTask = new ReceiveMailTask(mailUidxCeil,this,true, beQuiet, true);
		recvTask.execute(true);
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getGroupId() == ACCOUNT_GRP_ID)
		{
			String actName = item.getTitle().toString();
			Account a = AccountManager.getAccount(actName);
			if (a == null)
				return super.onOptionsItemSelected(item);
			MyApp.currentAccount = a;
			MyApp.userSetting.currentAccountId = a.id;
			//start InBoxActivity
			Intent intent = new Intent(this,InBoxActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.putExtra(CLIENT_OPEN_MODE, OPEN_FROM_GLOBAL_BOX);
			startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
//	@Override
//	public void onClick(View v) {
//		onSearchRequested();
//	}
//	
//	@Override
//	public boolean onSearchRequested(){
//		startSearch("", false, null, false);
////		doSearchQuery();
//		return true;
//	}
//	public void doSearchQuery(){
//		final Intent intent = getIntent();
//		//获得搜索框里值
//		String query=intent.getStringExtra(SearchManager.QUERY);
////		tvquery.setText(query);
//		//保存搜索记录
//		SearchRecentSuggestions suggestions=new SearchRecentSuggestions(this,
//		SearchSuggestionSampleProvider.AUTHORITY, SearchSuggestionSampleProvider.MODE);
//		suggestions.saveRecentQuery(query, null);
//		if(Intent.ACTION_SEARCH.equals(intent.getAction())){
//		//获取传递的数据
//		Bundle bundled=intent.getBundleExtra(SearchManager.APP_DATA);
//		if(bundled!=null){
//		String ttdata=bundled.getString("data");
////		tvdata.setText(ttdata);
//		}else{
////		tvdata.setText("no data");
//		}
//		}
//		}
	@Override
	protected void onNewIntent (Intent intent)
	{
		super.onNewIntent(intent);
		int newType = intent.getIntExtra(CLIENT_OPEN_MODE, 0);
		if(newType == OPEN_FROM_STATUS_BAR)
		{
			Log.d(LOGTAG,getClass().getName()+ "onNewIntent called, open from status bar");
			try
			{
				updateMail();
			}
			catch (Exception e)
			{
				Log.d(Utils.LOGTAG, "",e);
			}
		}
		
//		else if(Intent.ACTION_SEARCH.equals(intent.getAction()))
//		{
////		else if(searchExtra==intent.getStringExtra(SearchManager.QUERY))
////		{
//			 floatingSearch = intent.getStringExtra(SearchManager.QUERY);
//			try
//			{
//				updateMail();
//			}
//			catch (SQLException e)
//			{
//				e.printStackTrace();
//			}
//		}
	}
	
	@Override
	protected void onResume ()
	{
		super.onResume();
		Log.d(LOGTAG,getClass().getName()+ "onResume()");
		try
		{
			updateMail();
		}
		catch (Exception e)
		{
			Log.d(Utils.LOGTAG, "",e);
		}
	}
	
	@Override
	protected boolean inReceiving()
	{
		for(int i=0;i<AccountManager.getCount();i++)
		{
			if(MyApp.getAgent(AccountManager.getByIndex(i)).isReceiving())
				return true;
		}
		return false;
	}
	@Override
	protected Cursor doQueryGroup(int index) {
		return NewDbHelper.getUiCriticalInstance().getInGroups(getCurrentAccountId(),mailCountPerScreen,index,folderName, orderType,true);
		
	}
	@Override
	protected int getTotalGroupCount(boolean query) {
		totalGroupCount=NewDbHelper.getInstance().getGroupCount(getCurrentAccountId(), folderName, true);
		return totalGroupCount;
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(inReceiving() || (floatingSearch!=null && !floatingSearch.equals("")))
		{
			return super.onKeyDown(keyCode, event);
		}
		if (keyCode == KeyEvent.KEYCODE_BACK) {

			if (((System.currentTimeMillis() - mExitTime) > 1000)) {

				// Object mHelperUtils;

				Toast.makeText(this, R.string.exitprogram, Toast.LENGTH_SHORT)
						.show();
				mExitTime = System.currentTimeMillis();

			} else {
				MyApp.finishAllActivities();
				System.exit(0);
			}

			return true;

		}
		return false;

	}

}
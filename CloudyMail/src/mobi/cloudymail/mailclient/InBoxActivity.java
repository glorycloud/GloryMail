package mobi.cloudymail.mailclient;

import static mobi.cloudymail.util.Utils.LOGTAG;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import mobi.cloudymail.data.InMailInfo;
import mobi.cloudymail.data.MailInfo;
import mobi.cloudymail.data.MailStatus;
import mobi.cloudymail.mailclient.PullToRefreshListView.OnRefreshListener;
import mobi.cloudymail.mailclient.net.Account;
import mobi.cloudymail.mailclient.net.Result;
import mobi.cloudymail.mailclient.net.ServerAgent;
import mobi.cloudymail.util.DialogUtils;
import mobi.cloudymail.util.MyApp;
import mobi.cloudymail.util.NewDbHelper;
import mobi.cloudymail.util.Utils;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class InBoxActivity extends MailFolderActivity
{
//	private boolean receiveMailAfterCreateAccount = false;
	protected int unreadedMailCount = 0;
	protected boolean _excludeHasMoreMail = false;
	boolean stoppedByUser = false;
	HashSet<String> receivingItems = new HashSet<String>();
	ReceiveMailTask recvTask;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		String folderNameFromIntent=getIntent().getStringExtra(FolderActivity.DAT_FOLDER_NAME);
		folderName =folderNameFromIntent==null? FolderNames.FOLDER_INBOX:folderNameFromIntent;
		try
		{
			if (AccountManager.isEmpty())
			{// ask user to create a new account if there's no any account
				MyApp.userSetting.currentAccountId = -1;
				AccountManager.checkAccountAndCreate(this);
			}
			else if(MyApp.currentAccount==null)
				initAccount();
		
			Log.d(LOGTAG,getClass().getName()+ "getTaskId():"+getTaskId());
			if(getIntent().getBooleanExtra("CheckMail", false)){
				MyApp.userSetting.currentAccountId = getIntent().getIntExtra("accountID", 0);
				Utils.ASSERT(MyApp.userSetting.currentAccountId == MyApp.currentAccount.id);
				recvMailMenu_Click();
			}
			else
				updateMail();
			
	        ((PullToRefreshListView) getExpandableListView()).setOnRefreshListener(new OnRefreshListener() {
	            @Override
	            public void onRefresh() {
	                // Do work to refresh the list here.
	            	
	            	recvMailMenu_Click();
	            }
	        });

		}
		catch (Exception e)
		{
			Log.d(Utils.LOGTAG, "",e);
		}
	}
	
	protected boolean isLocalInBoxActivity() 
	{
		return true;
	}
	
	protected void initBaseTitle()
	{
		Intent intent = getIntent();
		_baseWinTitle = intent.getStringExtra(FolderNames.DAT_FOLDER_DESCRIPTION);
		folderName = intent.getStringExtra(FolderNames.DAT_FOLDER_NAME);
		if(_baseWinTitle==null||folderName==null)
		{
			_baseWinTitle = getResources().getString(R.string.folder_inbox);
			folderName=FolderNames.FOLDER_INBOX;
		}
	}
	
	
	
	private void initAccount()
	{

		if (AccountManager.isEmpty()) // user canceled creating
													// account
		{
			return;
		}

		int lastSentAccountId = NewDbHelper.getInstance().getCurrentAccountId();
		if (lastSentAccountId >= 0)
			MyApp.userSetting.currentAccountId = lastSentAccountId;
		if (MyApp.userSetting.currentAccountId < 0)
		{// for the first time run this
			// application
			MyApp.userSetting.currentAccountId = AccountManager.getByIndex(0).id;
			MyApp.currentAccount = AccountManager.getByIndex(0);

		}
		else
		{
			for (int i=0;i<AccountManager.getCount();i++)
			{// find the saved account
				Account a = AccountManager.getByIndex(i);
				if (a.id == MyApp.userSetting.currentAccountId)
				{
					MyApp.currentAccount = a;
					break;
				}
			}
			if (MyApp.currentAccount == null)
			{// fail to find saved account, may
				// because of a previous crash
				MyApp.userSetting.currentAccountId = AccountManager.getByIndex(0).id;
				MyApp.currentAccount = AccountManager.getByIndex(0);
			}

		}
		
		updateWindowTitle();
	}
	
	@Override
	protected void onNewIntent (Intent intent)
	{
		super.onNewIntent(intent);
		int newType = intent.getIntExtra(CLIENT_OPEN_MODE, 0);
		if(newType == OPEN_FROM_STATUS_BAR || newType == OPEN_FROM_GLOBAL_BOX)
		{
			Log.d(LOGTAG,"MailClient"+ "onNewIntent called, open from status bar");
			try
			{
				updateMail();
			}
			catch (Exception e)
			{
				Log.d(Utils.LOGTAG, "",e);
			}
		}
			
	}
	
	@Override
	protected void updateMarkButton()
	{
		if (_checkedMails.isEmpty())
			return;
		boolean hasUnreadMail = false;
		Iterator<MailInfo> itr = _checkedMails.iterator();
		while (itr.hasNext())
		{
			MailInfo info = itr.next();
			if (info.getState() == MailStatus.MAIL_NEW)
			{
				hasUnreadMail = true;
				break;
			}
		}
		if (hasUnreadMail)
		{
			_markReadBtn.setVisibility(View.VISIBLE);
			_markUnreadBtn.setVisibility(View.GONE);
		}
		else
		{
			_markReadBtn.setVisibility(View.GONE);
			_markUnreadBtn.setVisibility(View.VISIBLE);
		}
	}
	
	@Override
	protected void onActivityResult(int reqCode, int rstCode, Intent intent)
	{
		MyApp.setCurrentActivity(this);
		switch (reqCode)
		{
		case R.layout.account_manager:// create account for first time run
		{
			if (rstCode == Dialog.BUTTON_POSITIVE)// when add the first account
													// finished.
			{
				// DialogUtils.showMsgBox(this, "from account manager", "info");
				initAccount();
//				if (receiveMailAfterCreateAccount)
//				{
//					doReceiveMails(-1);
//					receiveMailAfterCreateAccount = false;
//					updateAccountMenu();
//				}
				
			}
			else if(rstCode == Dialog.BUTTON_NEGATIVE){//check mail after creating a new account
				initAccount();
				recvMailMenu_Click();
			}
			else
			{
				// check whether the current account is deleted.
				if (MyApp.currentAccount != null
					&& AccountManager.getAccount(MyApp.currentAccount.name) == null)
				{
					// current account is deleted. delete the mail of this
					// account from database.
					// then reset account.
					if (!AccountManager.isEmpty())
					{
						MyApp.userSetting.currentAccountId = AccountManager.getByIndex(0).id;
						MyApp.currentAccount = AccountManager.getByIndex(0);
					}
					else
					{
						MyApp.userSetting.currentAccountId = -1;
						MyApp.currentAccount = null;
					}
				}
				try
				{
					updateMail();
				}
				catch (Exception e)
				{
					Log.d(Utils.LOGTAG, "",e);
				}
				
			}
		}
		break;
		case R.layout.mail_viewer:
		{
			if (MyApp.curMailInfo.getState() == MailStatus.MAIL_NEW)
			{
				String cmd = "update mail set state=? where accountId=? and uid=? and folder='"+folderName+"'";
				String[] args = { MailStatus.MAIL_READED + "",
				                  MyApp.currentAccount.id + "", MyApp.curMailInfo.getUid() };
				NewDbHelper.getInstance().execSQL(cmd, args);
				MyApp.curMailInfo.setState(MailStatus.MAIL_READED);
				unreadedMailCount--;
//				MyApp.curMailInfo = null;
//				MyApp.setCurrentMailInfo(null);
				_mailAdpt.notifyDataSetInvalidated();
			}
			if (rstCode == R.id.last)// show previous mail
			{
				if(inGroupMode())
				{
					if(_currentMailIndex <= 0&&_currentGroupIndex<=0)
					{
						Toast.makeText(this, this.getString(R.string.already_first_mail), Toast.LENGTH_SHORT).show();
						return;
					}
					if(_currentMailIndex==0&&_currentGroupIndex>0)
					{
						MailGroup g1 = getGroup(--_currentGroupIndex);
						_currentMailIndex=g1.getGroupSize()-1;
						openMail(g1.getMailInfo(_currentMailIndex));
						return;
					}
					MailGroup g = getGroup(_currentGroupIndex);
					openMail(g.getMailInfo(--_currentMailIndex));
				}
				else
				{
					if (_currentMailIndex <= 0)
					{
						Toast.makeText(this, this.getString(R.string.already_first_mail), Toast.LENGTH_SHORT).show();
						return;
					}
					openMail(getMail(--_currentMailIndex));
				}
			}
			else if (rstCode == R.id.next)// show next mail
			{
				if(inGroupMode())
				{
					MailGroup g0 = getGroup(_currentGroupIndex);
					if(_currentGroupIndex>=getTotalGroupCount(true)-1&&_currentMailIndex>=g0.getGroupSize()-1)
					{
						Toast.makeText(this, this.getString(R.string.already_last_mail), Toast.LENGTH_SHORT).show();
						return;
					}
					if(_currentGroupIndex<getTotalGroupCount(true)-1&&_currentMailIndex>=g0.getGroupSize()-1)
					{
						MailGroup g1 = getGroup(++_currentGroupIndex);
						_currentMailIndex=0;
						openMail(g1.getMailInfo(_currentMailIndex));
						return;
					}
					MailGroup g2 = getGroup(_currentGroupIndex);
					openMail(g2.getMailInfo(++_currentMailIndex));
				}
				else
				{
					if (_currentMailIndex >= mails.length - 1)
					{
						Toast.makeText(this, this.getString(R.string.already_last_mail), Toast.LENGTH_SHORT).show();
						return;
					}
					openMail(getMail(++_currentMailIndex));
				}
			}
			else if(rstCode == R.id.delMail)//delete current mail
			{
				HashSet<MailInfo> toDel = new HashSet<MailInfo>();
				// if (MyApp.curMailInfo!=null)
				toDel.add(MyApp.curMailInfo);
				updateMailStatus(toDel.iterator(), MailStatus.MAIL_LOCAL_DELETED);
				
			}
		}
		break;
	/*	case R.layout.folder_list:
			if( rstCode == RESULT_OK)
			{
				String newFolderName = intent.getStringExtra(FolderManager.DATA_FOLDER_NAME);
				if(!folderName.equals(newFolderName)) 
				{
					folderName = newFolderName;
					try
					{
						updateMail();
					}
					catch (SQLException e)
					{
						e.printStackTrace();
					}
				}
			}
//			else
//				System.err.println("Unexpected result code:"+rstCode);*/
		}
	}

	@Override
	protected int getUnreadMailcount(boolean query)
	{
		if(query)
		{
			unreadedMailCount = NewDbHelper.getInstance()
					.getInMailCount(getCurrentAccountId(),
									FolderNames.FOLDER_INBOX,
									new int[]{MailStatus.MAIL_NEW},
									_excludeHasMoreMail, floatingSearch);
		}
		return unreadedMailCount;
	}
	@Override
	protected int getTotalGroupCount(boolean query) {
		totalGroupCount=NewDbHelper.getInstance().getGroupCount(getCurrentAccountId(), folderName, false);
		return totalGroupCount;
	}
	@Override
	protected int getTotalMailCount(boolean query)
	{
		if (query)
		{
			totalMailCount = NewDbHelper.getInstance()
					.getInMailCount(getCurrentAccountId(),
									folderName,
									new int[]{MailStatus.MAIL_NEW,MailStatus.MAIL_READED},
									_excludeHasMoreMail, floatingSearch);
		}
		return totalMailCount;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if(item.getItemId() == R.id.recvMailMenu)
		{
			recvMailMenu_Click();
			return true;
		}
		if (item.getGroupId() == ACCOUNT_GRP_ID)
		{
			// item.setChecked(true);
			String actName = item.getTitle().toString();
			// Toast.makeText(this, actName, Toast.LENGTH_SHORT).show();
			if (!actName.equals(MyApp.currentAccount.name))// not the same account;
			{
				Account a = AccountManager.getAccount(actName);
				if (a == null)
					return super.onOptionsItemSelected(item);
				Account oldCurAcct = MyApp.currentAccount;
				MyApp.currentAccount = a;
				MyApp.userSetting.currentAccountId = a.id;
				try
				{
					updateMail();
					item.setChecked(true);
				}
				catch (SQLException exp)
				{
					// restore;
					MyApp.currentAccount = oldCurAcct;
					MyApp.userSetting.currentAccountId = MyApp.currentAccount.id;
					DialogUtils.showMsgBox(	this,
											"Failed to change account: "
													+ exp.getMessage(),
											getResources()
												.getString(R.string.error));
				}
			}
			return true;
		}
		else
			return super.onOptionsItemSelected(item);
	}
	
//	@Override
//	public void onBackPressed()
//	{
//		if (inReceiving())
//		{
//			if (stoppedByUser)
//				return;
//			stoppedByUser = true;
//			recvTask.abort();
//			recvTask = null;
//			return;
//		}
//		super.onBackPressed();
//	}
	@Override 
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0 )
		{
			if (inReceiving())
				{
					if (stoppedByUser)
						return true;
					stoppedByUser = true;
					recvTask.abort();
					recvTask = null;
					return true;
				}
			
		}
		return super.onKeyDown(keyCode, event);
//		return false;
	}
	
	@Override
	protected Cursor doQueryMail(int index)
	{
		return NewDbHelper.getInstance()
		.getInMails(getCurrentAccountId(),
					Math.min(mailCountPerScreen, mails.length),
					index, orderType,folderName,_excludeHasMoreMail,floatingSearch);
	}
	
	private void recvMailMenu_Click()
	{
		if (inReceiving())
		{
			if (stoppedByUser)
				return;
			stoppedByUser = true;
			recvTask.abort();
			recvTask = null;
			return;
		}

		try
		{
/*			if (MyApp.currentAccount == null)
			{
				if (checkAccountSetting())
					receiveMailAfterCreateAccount = true;
				return;
			}*/
			if(!AccountManager.checkAccountAndCreate(this))
				return;
		}
		catch (Exception e)
		{
			Log.d(Utils.LOGTAG, "",e);
			return;
		}

		doReceiveMails(0, false);
	}
	
	/**
	 * 
	 * @param mailUidxCeil
	 *            the maximux uidx of mail want to receive. -1 to indicate no
	 *            limit
	 */
	protected void doReceiveMails(int mailUidxCeil, boolean quiet)
	{
		Utils.ASSERT(MyApp.currentAccount != null);
		((PullToRefreshListView)getExpandableListView()).enforceToRefreshState();
		recvTask = new ReceiveMailTask( mailUidxCeil, this, false, quiet, true);
		recvTask.execute(true);
	}
	
	//do update things before open mail.
	protected void beforeOpenMail(MailInfo mailInfo)
	{
//		MyApp.curMailInfo = getMail(selectedIdx);
		MyApp.setCurrentMailInfo(mailInfo);
//		MyApp.setCurrentMailInfo(getGroup(selectedIdx));
	}
	
	public boolean isItemInReceivingState(String uid)
	{
		return receivingItems.contains(uid);
	}
	@Override
	protected void openMail(MailInfo mailInfo)
	{

		beforeOpenMail(mailInfo);
//		MyApp.curMailInfo = getMail(selectedIdx);
		if (MyApp.curMailInfo == null)
			return;
		if ((MyApp.curMailInfo.getState() & MailStatus.FLAG_HAS_MORE_PLACEHOLD) != 0)
		{
			int uidx; 
			uidx = NewDbHelper
					.getInstance()
					.executScalar(	"select uidx from mail where uid=? and accountId=? and folder='"+folderName+"'",
									new String[] { MyApp.curMailInfo.getUid(),
									               MyApp.currentAccount.id + "" });
			final String uid=MyApp.curMailInfo.getUid();
			final int id= MyApp.currentAccount.id;
			Utils.ASSERT(MyApp.currentAccount != null);
			receivingItems.add(MyApp.curMailInfo.getUid());
			recvTask = new ReceiveMailTask( uidx, this, false, false, false, new ReceiveMailTask.ReceiveMailCallback() {
				
				@Override
				public void  receiveFinished(int status)
				{
					receivingItems.remove(MyApp.curMailInfo.getUid());
					if(status==Result.SUCCESSED)
					{
						runOnUiThread(new Runnable() {
							
							@Override
							public void run()
							{
								NewDbHelper
										.getInstance()
										.executScalar(	" delete from mail  where uid=? and accountId=? and folder='"+folderName+"' and state>32768",
														new String[] { uid, id + "" });
								getExpandableListView().invalidateViews();
								
							}
						});
					}
				}
			});
			recvTask.execute(false);
			getExpandableListView().invalidateViews();
			return;
		}
		
		//if it's the first time to read this mail,then need login at first.
		if (MyApp.curMailInfo.getState() != MailStatus.MAIL_READED)
		{
			String sid = MyApp.getAgent().getSessionId(true,true,true); //Is Bug? current account may not the account this mail
															  //No, in GlobalInBoxActivity.beforeOpenMail, currentAccount has been switched
			if (sid == null)
			{
				Log.d(LOGTAG,"openMail:"+ "session id is null");
				return;
			}
			if(MyApp.instance().containsMailInMap(mailInfo))
			{
				MyApp.instance().clearNewMailMap();
				ReceiveMailService._instance.mNM.cancel(R.string.ntf_new_mail_name);
			}
		}
		Intent intent = new Intent(this, MailViewer.class);
//		intent.putExtra(MailViewer.PREVIOUS_MAIL,
//						_currentMailIndex > 0
//								&& _currentMailIndex < mails.length);
//		intent.putExtra(MailViewer.NEXT_MAIL,
//						_currentMailIndex < (mails.length - 1)
//								&& _currentMailIndex >= -1);
		intent.putExtra(MailViewer.PREVIOUS_MAIL,true);
		intent.putExtra(MailViewer.NEXT_MAIL,true);
		startActivityForResult(intent, R.layout.mail_viewer);
	}
	@Override
	public boolean  onPrepareOptionsMenu(Menu menu)
	{
		MenuItem rcv = menu.getItem(0);
		if(inReceiving())
		{
			rcv.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
			String title = MyApp.instance().getString(R.string.stop);
			rcv.setTitle(title);
			rcv.setTitleCondensed(title);
		}
		else
		{
			rcv.setIcon(R.drawable.ic_menu_refresh);
			rcv.setTitle(R.string.receiveMail);
			String title = MyApp.instance().getString(R.string.receiveMail);
			rcv.setTitleCondensed(title);
		}
		return true;
	}
	@Override
	protected boolean doCreateOptionsMenu(Menu menu)
	{
		MenuItem receiveItem = menu.findItem(R.id.recvMailMenu);
		receiveItem.setVisible(true);
		return true;
	}
	
	@Override
	protected MailInfo newMailInfo(Cursor cursor)
	{
		return new InMailInfo(cursor);
	}

	@Override
	protected MailInfo[] newMailInfoArray(int count)
	{
		return new InMailInfo[count];
	}

	
	@Override
	protected void doUpdateAsteriskStates(MailInfo mailInfo, int asteriskState)
	{
		// update mail status
//		Iterator<Integer> itr = mailPositions.iterator();
		
		if ((mailInfo.getState() & MailStatus.FLAG_HAS_MORE_PLACEHOLD) == 0)
		{
			
			mailInfo.setAsterisk(asteriskState);
			
			NewDbHelper.getInstance().updateInAsteriskstatu(mailInfo.getAccountId(), asteriskState, mailInfo.getUidx(), mailInfo.getFolder());
		}
		
	}

	@Override
	protected String doUpdateMailStates(Iterator<MailInfo> mails, int state)
	{
		// update mail status
		
		Vector<Integer> ids = new Vector<Integer>();
		Vector<Integer> accountIds = new Vector<Integer>();
		Vector<String> folders= new Vector<String>();
		int i = 0;
		while (mails.hasNext())
		{
			MailInfo mailInfo = mails.next();
			if ((mailInfo.getState() & MailStatus.FLAG_HAS_MORE_PLACEHOLD)==0)
			{
				mailInfo.setState(state);
				ids.add(mailInfo.getUidx());
				accountIds.add(mailInfo.getAccountId());
				folders.add(mailInfo.getFolder());
				
			}
		}
		
		return  NewDbHelper.getInstance().updateInMailStatus(accountIds, ids, state,folders);
	}


	protected boolean inReceiving()
	{
		ServerAgent agent = MyApp.getAgent();
		if(agent==null)
			return false;
		return agent.isReceiving();
	}

	@Override
	protected Cursor doQueryGroup(int index) {
		return NewDbHelper.getUiCriticalInstance().getInGroups(getCurrentAccountId(),mailCountPerScreen,index,folderName, orderType,false);
		
	}


	

	@Override
	protected MailGroup newMailGroup(Cursor cursor) {
        
		return new MailGroup(cursor);
	}

	@Override
	protected Vector<MailGroup> newMailGroupArray(int count) {
		
		Vector<MailGroup> vector=new Vector<MailGroup>(count); 
		for(int i=0;i<count;i++)
		{
			vector.add(null);
		}
	    return vector;
	}


}

package mobi.cloudymail.mailclient;

import java.util.Iterator;
import java.util.Vector;

import mobi.cloudymail.data.MailInfo;
import mobi.cloudymail.data.MailStatus;
import mobi.cloudymail.data.OutMailInfo;
import mobi.cloudymail.util.MyApp;
import mobi.cloudymail.util.NewDbHelper;
import mobi.cloudymail.util.Utils;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;


public  class OutBoxActivity extends MailFolderActivity
{
//	public static  boolean contentState;
	public static boolean contentState;
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		//This activity can only be started from folder manager.
		super.onCreate(savedInstanceState);
		
		_markUnreadBtn.setVisibility(View.GONE);
		_markReadBtn.setVisibility(View.GONE);
		try
		{
			updateMail();
		}
		catch (Exception e)
		{
			Log.d(Utils.LOGTAG, "",e);
		}
		
		contentState=false;
		updateMailView();
	}
	
	@Override
	protected void initBaseTitle()
	{
		Intent intent = getIntent();
		_baseWinTitle = intent.getStringExtra(FolderNames.DAT_FOLDER_DESCRIPTION);
		this.folderName = intent.getStringExtra(FolderNames.DAT_FOLDER_NAME);
	}
	
	@Override
	protected void openMail(MailInfo mailInfo)
	{//open composer
//		MyApp.curMailInfo = getMail(selectedIdx+1);
		MyApp.setCurrentMailInfo(mailInfo);
		Intent intent = new Intent(this,Composer.class);
		intent.putExtra("composer_type", Composer.COMPOSER_EDIT_MAIL);
		startActivity(intent);
	}

	@Override
	protected int getUnreadMailcount(boolean query)
	{
		return -1;
	}
	
	@Override
	protected int getTotalMailCount(boolean query)
	{
		if (query)
		{
			totalMailCount = NewDbHelper.getInstance()
					.getOutMailCount(getCurrentAccountId(),
					                 folderName,
					                 new int[]{MailStatus.MAIL_NEW,MailStatus.MAIL_READED});
		}
		return totalMailCount;
	}
	
	@Override
	protected Cursor doQueryMail(int index)
	{
		return NewDbHelper.getInstance().getOutMails(getCurrentAccountId(),
		                                             Math.min(mailCountPerScreen, mails.length),
		                                             index,orderType,folderName);
	}
	
//	@Override
//	protected boolean doCreateOptionsMenu(Menu menu)
//	{
//		//hide account manager.
//		MenuItem accountItem = menu.findItem(R.id.accountMenu);
//		accountItem.setVisible(false);
//		return true;
//	}

	@Override
	protected MailInfo newMailInfo(Cursor cursor)
	{
		// TODO Auto-generated method stub
		return new OutMailInfo(cursor);
	}

	@Override
	protected MailInfo[] newMailInfoArray(int count)
	{
		// TODO Auto-generated method stub
		return new OutMailInfo[count];
	}

	protected void doUpdateAsteriskStates(MailInfo mailInfo,
			int asteriskState)
	{
		
			
			
			  NewDbHelper.getInstance().updateOutAsteriskstatu(mailInfo.getAccountId(), asteriskState, mailInfo.getUidx());
		
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
		return  NewDbHelper.getInstance().updateOutMailStatus(accountIds, ids, state);
	}

	
	
	protected void updateMailItemView(MailInfo mailInfo, ViewHolder holder, View convertView)
	{
		holder.draftTxt.setVisibility(FolderNames.FOLDER_SENT.equals(mailInfo.getFolder())  ? View.GONE : View.VISIBLE);
		
	}

	@Override
	protected int getTotalGroupCount(boolean query) {
		return 0;
	}

	@Override
	protected Cursor doQueryGroup(int index) {
		return null;
	}

	@Override
	protected MailGroup newMailGroup(Cursor cursor) {
		return new MailGroup(cursor);
	}

	@Override
	protected Vector<MailGroup> newMailGroupArray(int count) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	protected boolean inGroupMode()
	{
		return false;
	}

	/* (non-Javadoc)
	 * @see mobi.cloudymail.mailclient.MailFolderActivity#inReceiving()
	 */
	@Override
	protected boolean inReceiving()
	{
		// TODO Auto-generated method stub
		return false;
	}
}
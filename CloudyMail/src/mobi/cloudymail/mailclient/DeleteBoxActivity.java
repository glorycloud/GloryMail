package mobi.cloudymail.mailclient;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import mobi.cloudymail.data.InMailInfo;
import mobi.cloudymail.data.MailInfo;
import mobi.cloudymail.data.MailStatus;
import mobi.cloudymail.data.OutMailInfo;
import mobi.cloudymail.util.NewDbHelper;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class DeleteBoxActivity extends OutBoxActivity
{
	private int _inMaildelNum=0;
	private int _outMailDelNum = 0;
	private Button _recoverMailBtn=null;
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Resources res = getResources();
		_delMailBtn.setText(res.getString(R.string.deleteForever));
		_recoverMailBtn = (Button)findViewById(R.id.recoverMailBtn);
		_recoverMailBtn.setVisibility(View.VISIBLE);
		_recoverMailBtn.setOnClickListener(this);
		updateMailView();
	}
	
	@Override
	public void onClick(View v)
	{
		if (v.getId() == R.id.recoverMailBtn)
		{
			//set back to state NEW
			updateMailStatus(MailStatus.MAIL_NEW);
		}
		else
			super.onClick(v);
	}
	
	@Override
	protected void openMail(MailInfo mailInfo)
	{//do nothing for deleted mail.
	}
	
	@Override
	protected boolean doCreateOptionsMenu(Menu menu)
	{
		//hide sort menu item, we do not want to sort the deleted mails.
		//we sort it fixedly, 'in' mails first and 'out' mails below.
		MenuItem sortItem = menu.findItem(R.id.sort);
		sortItem.setVisible(false);
		return super.doCreateOptionsMenu(menu);
	}
	
	//get deleted mails from 'mail' and 'outBox'
	public static int getDelMailCount(int accountId)
	{
		int count= getInDelMailCount(accountId);
		count += getOutDelMailCount(accountId);
		return count;
	}
	
	private static int getInDelMailCount(int accountId)
	{
		return NewDbHelper.getInstance().getInMailCount(accountId,"",new int[]{MailStatus.MAIL_LOCAL_DELETED},false);
	}
	
	private static int getOutDelMailCount(int accountId)
	{
		return NewDbHelper.getInstance().getOutMailCount(accountId,"", new int[]{MailStatus.MAIL_LOCAL_DELETED});
	}
	
	@Override
	protected int getTotalMailCount(boolean query)
	{
		if (query)
		{
			int curAccountId = getCurrentAccountId();
			_inMaildelNum= getInDelMailCount(curAccountId);
			_outMailDelNum = getOutDelMailCount(curAccountId);
			totalMailCount = _inMaildelNum+_outMailDelNum;
		}
		return totalMailCount;
	}
	
	@Override
	protected Cursor doQueryMail(int index)
	{
		NewDbHelper db = NewDbHelper.getInstance();
		if(index < _inMaildelNum)//'in' mail
		{
			return db.getDelInMails(getCurrentAccountId(),
						Math.min(mailCountPerScreen, _inMaildelNum),
						index,floatingSearch);
		}
		else
			return db.getDelOutMails(getCurrentAccountId(),
		                                             Math.min(mailCountPerScreen, _outMailDelNum),
		                                             index-_inMaildelNum,floatingSearch);
	}

	@Override
	protected MailInfo newMailInfo(Cursor cursor)
	{
		if(cursor.getColumnIndex("uid") < 0)//not exists 'uid',it's out mail.
			return new OutMailInfo(cursor);
		else
			return new InMailInfo(cursor);
	}

	@Override
	protected MailInfo[] newMailInfoArray(int count)
	{
		// TODO Auto-generated method stub
		return new MailInfo[count];
	}

	@Override
	protected boolean shouldRebuildMailList(int state)
	{
		//always update mail list.
		return true;
	}
	
	@Override
	protected String doUpdateMailStates(Iterator<MailInfo> mails, int state)
	{
		// update mail status
		int idx = 0;
		List<MailInfo> inMails = new LinkedList<MailInfo>();
		List<MailInfo> outMails = new LinkedList<MailInfo>();
		Vector<Integer> inAcctIds = new Vector<Integer>();
		Vector<Integer> inUidxs = new Vector<Integer>();
		Vector<String> inFolders=new Vector<String>();
		Vector<Integer> outAcctIds = new Vector<Integer>();
		Vector<Integer> outUidxs = new Vector<Integer>();
		Vector<String> outFolders=new Vector<String>();

		while (mails.hasNext())
		{
			MailInfo mailInfo = mails.next();
			if(idx < _inMaildelNum)
			{
				inMails.add(mailInfo);
				inAcctIds.add(mailInfo.getAccountId());
				inUidxs.add(mailInfo.getUidx());
				inFolders.add(mailInfo.getFolder());
			}
			else
			{
				outMails.add(mailInfo);
				outAcctIds.add(mailInfo.getAccountId());
				outUidxs.add(mailInfo.getUidx());
				outFolders.add(mailInfo.getFolder());
			}
			idx ++;
		}
		//if delete forever
		if (state == MailStatus.MAIL_LOCAL_DELETED)
			state = MailStatus.MAIL_DELETE_FOREVER;
		NewDbHelper db = NewDbHelper.getInstance();
		// in box mail
		int inMailNum = inMails.size();
		if (inMailNum > 0)
		{
			String result = db.updateInMailStatus(inAcctIds, inUidxs, state,inFolders);
			if (!result.equals(""))
				return result;
		}

		int outMailNum = outMails.size();
		if (outMailNum > 0)
		{
			String result = db.updateOutMailStatus(outAcctIds, outUidxs, state);
			if (!result.equals(""))
				return result;
		}	
		return "";
	}
	
	protected void updateMailItemView(MailInfo mailInfo, ViewHolder holder, View convertView)
	{
		
		
	}
	
	@Override
	protected boolean inGroupMode()
	{
		return false;
	}
}

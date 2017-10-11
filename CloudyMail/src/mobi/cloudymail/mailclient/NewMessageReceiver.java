package mobi.cloudymail.mailclient;

import java.util.Vector;

import mobi.cloudymail.mailclient.ReceiveMailService.SyncRequest;
import mobi.cloudymail.mailclient.net.Account;
import mobi.cloudymail.util.Utils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.glorycloud.push.CallbackInfo;

public class NewMessageReceiver extends BroadcastReceiver
{
	private static final String OK_REPLY = "OK ";
	public static final String NEED_LOGIN_REPLY = "NEED_LOGIN ";
	public NewMessageReceiver()
	{
		//this.ui = ui;
	}
	@Override
	public void onReceive(Context context, Intent intent)
	{
		String account = intent.getStringExtra(CallbackInfo.KEY_REPLY_MSG);
		
		if(!account.startsWith("OK "))
			return;
		account=account.substring(3);
		Utils.log("new mail pushed for:"+account);
		Account a = AccountManager.getAccount(account);
		if(a == null)
			return;
		
    	Intent i = new Intent(context, ReceiveMailService.class);
        context.startService(i);

		SyncRequest r = new SyncRequest();
		Vector<Account> v = new Vector<Account>();
		v.add(a);
		r.setAccountsToSync(v);
		ReceiveMailService.addToSyncQueue(r);
	}
	

}

package mobi.cloudymail.mailclient;

import mobi.cloudymail.mailclient.net.ServerAgent;
import mobi.cloudymail.util.MyApp;
import mobi.cloudymail.util.Utils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;


public class AlarmReceiver extends BroadcastReceiver { 
	
    @Override  
    public void onReceive(Context context, Intent intent) 
    {   
		Utils.log("Alarm onReceive");
 		Bundle bundle= intent.getExtras();
		if(bundle == null)
		{
			Utils.log("bundle is null");
			return;
		}
		int pollInterval =  Integer.parseInt(bundle.getString("minutes"));
	
		if (MyApp.userSetting.getPushFrequency() <= 0)
		{
			return;
		}

//		if(ReceiveMailService.wakeLock != null)
//		{
//			ReceiveMailService.wakeLock.acquire(5000);//acquire for 5s
//			Log.d(Utils.LOGTAG, "acquire lock for 5s");
//		}
		PowerManager pm = (PowerManager) MyApp.instance().getSystemService(Context.POWER_SERVICE);
//		wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "CloudyMailLock");
		WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CloudyMailLock");
		if(ServerAgent.hasNetworkConnection())
		{
			ConnectivityManager cm = (ConnectivityManager) MyApp.instance()
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			
			Utils.log("Net_connection OK:"+cm.getActiveNetworkInfo().getTypeName());
			wakeLock.acquire(500);//wait 0.5s only, to give time for ReceiveMailService to create it's own lock
		}
		else
		{
			Utils.log("No Net_connection");
			wakeLock.acquire(5000);//wait 5 seconds, give enough time for system to restore network connection
			//if a network connection is restored, CONNECTIVE_CHANGE event will happen, and 
			//push service will be restarted then
			return;
		}
		ReceiveMailService.SyncRequest r = new ReceiveMailService.SyncRequest();
		r.setAccountsToSync(AccountManager.getAccounts());
		ReceiveMailService.addToSyncQueue(r);			
		
    }   
}  


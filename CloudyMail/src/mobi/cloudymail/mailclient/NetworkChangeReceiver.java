package mobi.cloudymail.mailclient;

import mobi.cloudymail.mailclient.net.ServerAgent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

public class NetworkChangeReceiver extends BroadcastReceiver {
	public static final String CONNECTIVITY_ACTION=ConnectivityManager.CONNECTIVITY_ACTION;
	static private java.util.Date networkLostTime;
    @Override
    public void onReceive(Context context, Intent intent) {
        // 在这里干你想干的事（启动一个Service，Activity等），本例是启动一个定时调度程序，每30分钟启动一个Service去更新数据
        final String action = intent.getAction();
        Log.d("Connection", "Receive broadcast:"+action);
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action))
        {
    		ServerAgent.onConnectionChanged();
        }
       
    }
}
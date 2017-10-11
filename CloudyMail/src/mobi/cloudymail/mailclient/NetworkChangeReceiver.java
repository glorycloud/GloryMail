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
        // �����������ɵ��£�����һ��Service��Activity�ȣ�������������һ����ʱ���ȳ���ÿ30��������һ��Serviceȥ��������
        final String action = intent.getAction();
        Log.d("Connection", "Receive broadcast:"+action);
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action))
        {
    		ServerAgent.onConnectionChanged();
        }
       
    }
}
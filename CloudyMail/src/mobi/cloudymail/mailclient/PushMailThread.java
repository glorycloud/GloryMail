package mobi.cloudymail.mailclient;

import java.util.HashMap;

import mobi.cloudymail.mailclient.net.Account;
import mobi.cloudymail.mailclient.net.ServerAgent;
import mobi.cloudymail.util.MyApp;

import com.glorycloud.push.PushService;

public class PushMailThread extends com.glorycloud.push.PushThread
{
	public static final String TAG="CloudMailPush";
	public static final int PACKET_TYPE_PUSHREQUEST = 1; //PUSHREQUEST for Cloud Mail Version 1.3
	public static final int PACKET_TYPE_PUSHREQUESTV14 = 2; //PUSHREQUEST for Cloud Mail Version 1.4

	HashMap<String, Account> idToAccount = new HashMap<String, Account>();
	public PushMailThread(com.glorycloud.push.PushService service) {
		super(service);
		MyApp.userSetting.loadSetting(service);
	}
	
	@Override
	protected String getPushMessage()
	{
		idToAccount.clear();
		StringBuilder sb  = new StringBuilder(512);
		for(Account a:AccountManager.getAccountsFromDb())
		{
			String sid = MyApp.getAgent(a).getSessionId(false,false, false);
			if(sid != null)
			{
				sb.append(sid).append(" ");
				idToAccount.put(sid,  a);
			}
		}
		return sb.toString();
	}

	@Override
	protected void handleUserMessage(PushService service, String result)
	{
		String account = result;
		if(account.startsWith(NewMessageReceiver.NEED_LOGIN_REPLY))
		{
			Account a = idToAccount.get(account.substring(NewMessageReceiver.NEED_LOGIN_REPLY.length()));
			ServerAgent ag = MyApp.getAgent(a);
			ag.clearSessionId(); //clear session ID, and start a sync operation to force relogin
			return;
		}
		super.handleUserMessage(service, result);
	}


	@Override
	protected String getServerAddr()
	{
		return MyApp.userSetting._serverAddText;
	}
}

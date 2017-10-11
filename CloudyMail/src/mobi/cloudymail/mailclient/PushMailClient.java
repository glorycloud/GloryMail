package mobi.cloudymail.mailclient;

import android.content.Context;

import com.glorycloud.push.CallbackInfo;
import com.glorycloud.push.PushClient;

public class PushMailClient {
	PushClient pushClient;
	static final String companyId="GloryCloud";
	static PushMailClient instance;
	public static PushMailClient getInstance()
	{
		if(instance== null)
			instance = new PushMailClient();
		return instance;
	}
	
	PushClient getClient(Context ctx)
	{
		if(pushClient == null)
			pushClient = new PushClient(ctx, companyId);
		return pushClient;
	}
	public void startPush(Context ctx)
	{
		String pkgName = "mobi.cloudymail"; //you package name
		String action = "mobi.cloudymail.mailclient.NewMessage"; //your receiver action
		getClient(ctx).startPush("__clientIdNotUsed__", CallbackInfo.INTENT_BROADCAST, pkgName, action, 0);

	}
	
}

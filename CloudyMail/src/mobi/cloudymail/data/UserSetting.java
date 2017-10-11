package mobi.cloudymail.data;

import java.io.Serializable;

import mobi.cloudymail.mailclient.R;
import mobi.cloudymail.util.MyApp;
import mobi.cloudymail.util.Utils;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;

public class UserSetting implements Serializable
{
	/**
	 * 
	 */
	private boolean loadFlag = false;
	private static final long serialVersionUID = 6622949657960384526L;
	public int currentAccountId = -1;
	
	public boolean showMultipage = false;
	public int countPerReception = 10;
	private String _signature;
	public String _serverAddText="glorycloud.com.cn";
    public boolean muteEnabled=true;
    public String muteTimeValue="22:30-7:30-true";
    
	public String getMuteTimeValue()
	{
		return muteTimeValue;
	}
	public void setMuteTimeValue(String muteTimeValue)
	{
		this.muteTimeValue = muteTimeValue;
	}
	public String getSignature()
	{
		return _signature;
	}
	public void setSignatrue(String signatureStr)
	{
		_signature = signatureStr;
	}
	//settings for push mail.
	//frequency, the unit is minute, 0 for receive mail in real time,
	//-1 for disable push mail.
	private int _pushFrequency = 5;
	public void setPushFrequency(int minute)
	{
		_pushFrequency = minute; 
	}
	public int getPushFrequency()
	{
		return _pushFrequency;
	}
	public boolean _ledFlag = true;
	public boolean _vibrateFlag = false;
//	public String _ringtone = "";
	public boolean _sondFlag=true;
	
	
	
	public void loadSetting(Context ctx)
	{
		if(loadFlag)
			return;
		Resources res = ctx.getResources();
		_signature= "\n" + "\n" + "\n" + "-------------------->"
		+ "\n"
		+ res.getString(R.string.automatic_signature);
		MyApp.userSetting.setSignatrue(_signature);
		SharedPreferences pref = ctx.getSharedPreferences(MyApp.SHARED_SETTING, Context.MODE_WORLD_READABLE);
		Log.d(Utils.LOGTAG, "To load preference");
		if(pref != null)
		{
			
			_ledFlag = pref.getBoolean(res.getString(R.string.key_new_mail_led), _ledFlag);
			_vibrateFlag = pref.getBoolean(res.getString(R.string.key_new_mail_vibrate), _vibrateFlag);
			_sondFlag = pref.getBoolean(res.getString(R.string.key_new_mail_sond), _sondFlag);
//			showMultipage = pref.getBoolean(res.getString(R.string.key_multipage_mail), showMultipage);
//			_ringtone = pref.getString(res.getString(R.string.key_new_mail_ringtone), _ringtone);
			_pushFrequency = Integer.parseInt(pref.getString(res.getString(R.string.key_sync_frequency), ""+_pushFrequency));
			_signature = pref.getString(res.getString(R.string.key_mail_signature), _signature);
			countPerReception = Integer.parseInt(pref.getString(res.getString(R.string.key_mail_number),""+countPerReception));
			_serverAddText = pref.getString(res.getString(R.string.key_server_address), _serverAddText);
			Log.d(Utils.LOGTAG, "Load _serverAddText:"+_serverAddText);
			loadFlag = true;
			String storedString = pref.getString(res.getString(R.string.key_mute_time), muteTimeValue);
			String[] muteValue = storedString.split("-");
			muteEnabled=Boolean.parseBoolean(muteValue[2]);
			muteTimeValue = storedString;
		}
	}
}

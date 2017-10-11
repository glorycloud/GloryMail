package mobi.cloudymail.util;

import static mobi.cloudymail.util.Utils.LOGTAG;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mobi.cloudymail.data.MailInfo;
import mobi.cloudymail.data.UserSetting;
import mobi.cloudymail.mailclient.AccountManager;
import mobi.cloudymail.mailclient.AlarmReceiver;
import mobi.cloudymail.mailclient.net.Account;
import mobi.cloudymail.mailclient.net.ServerAgent;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;


public class MyApp extends Application {
	//debug flags
	public static boolean enablePush = true;
	public static boolean enableService = true;
	public static boolean isDebug=false;
	public static PendingIntent pendIntent;
	private static MyApp _instance;
	
	// use global variable for mail viewer.
	public static MailInfo curMailInfo = null;
	
	public static String SHARED_SETTING="setting";
	public static UserSetting userSetting = new UserSetting();
	public static Account currentAccount=null;
	// private static ServerAgent agent = null;
	private static Map<String, ServerAgent> _agents = new HashMap<String, ServerAgent>();
	
	private static List<Activity> _activities = new LinkedList<Activity>();
	
	private static Activity curActivity = null;
	
	private static Map<Integer, ArrayList<MailInfo>> newMailMap=new HashMap<Integer, ArrayList<MailInfo>>();
	//public static ArrayList<MailInfo> list = new ArrayList<MailInfo>();
	public boolean newMailCome=false; 
	public static Set<Long> getNewMailGroups()
	{
		Set<Long> s = new HashSet<Long>();
		Iterator<ArrayList<MailInfo>> v = newMailMap.values().iterator();
		while(v.hasNext())
		{
			Iterator<MailInfo> it = v.next().iterator();
			while(it.hasNext())
				s.add(it.next().getGroupId());
		}
		return s;
	}
	public boolean containsMailInMap(MailInfo mailInfo)
	{
		int accountId=mailInfo.getAccountId();
		if(newMailMap.containsKey(accountId))
		{
			List<MailInfo> list=newMailMap.get(accountId);
			for(int i=0;i<list.size();i++)
			{
				MailInfo mailInfo2=list.get(i);
				if(accountId==mailInfo2.getAccountId()&&mailInfo2.getUidx()==mailInfo.getUidx()&&mailInfo2.getFolder().equals(mailInfo.getFolder()))
				{
					return true;
				}
				
			}
		}
		return false;
	}
	public void addNewMailToMap(int accountId,MailInfo mailInfo)
	{
		if(!newMailMap.containsKey(accountId))
		{
			newMailMap.put(accountId, new ArrayList<MailInfo>());
		}
		List<MailInfo> list=newMailMap.get(accountId);
		list.add(mailInfo);
	}
	public int getNewMailTotalCount()
	{
		int count=0;
		Set<Integer> keySet=newMailMap.keySet();
		Iterator<Integer> iterator=keySet.iterator();
		while (iterator.hasNext())
		{
			int accountId=iterator.next();
			ArrayList<MailInfo> list=newMailMap.get(accountId);
			count+=list.size();
		}
		return count;
	}
	public void clearNewMailMap()
	{
		Set<Integer> keySet=newMailMap.keySet();
		Iterator<Integer> iterator=keySet.iterator();
		while (iterator.hasNext())
		{
			int accountId=iterator.next();
			newMailMap.get(accountId).clear();
		}
	}
	public static MyApp instance() 
	{
		return _instance;
	}
	public static void setCurrentMailInfo(MailInfo info)
	{
		curMailInfo = info;
		if(info != null)
			currentAccount = AccountManager.getAccount(info.getAccountId());
	}
	
	int timerCount;
	@Override
	public void onCreate()
	{
		super.onCreate();
		_instance = this;
		
		//for debug purpose
	}
	
	public static void addActivity(Activity a)
	{
		_activities.add(a);
	}
	
	public static void setCurrentActivity(Activity act)
	{
		curActivity = act;
	}
	
	public static Activity getCurrentActivity()
	{
		return curActivity;
	}
	
	public static void finishAllActivities()
	{
		for(Activity a:_activities)
		{
			a.finish();
		}
		_activities.clear();
	}
	
	public String getAppSdcardPath()
	{
		File sdCardDir = Environment.getExternalStorageDirectory();
		File appDir = new File(sdCardDir.toString()+"/CloudyMail");
		if(!appDir.exists())
		{
			if(!appDir.mkdir())
				Log.d(LOGTAG,"Create directory failed!"+ appDir.toString());
		}
		return appDir.toString();
	}
	public String getImageSdcardPath()
	{
		File imagesDir = new File(getAppSdcardPath()+"/Images");
		if(!imagesDir.exists())
		{
			if(!imagesDir.mkdir())
			{
				Log.d(LOGTAG,"Create directory failed!"+ imagesDir.toString());
			}
		}
		return imagesDir.toString();
	}
	public static ServerAgent getAgent()
	{
		if(currentAccount==null)
			return null;
		return getAgent(currentAccount);
		/*
		 * if(agent == null || agent.account != currentAccount) { agent = new
		 * ServerAgent(currentAccount); } return agent;
		 */
	}
	public static ServerAgent getAgent(Account a)
	{
		if (!_agents.containsKey(a.name))
			_agents.put(a.name, new ServerAgent(a));
		ServerAgent agt = _agents.get(a.name);
		agt.setAccount(a);
		return agt;
	}

	public static void removeAgent(Account a){
		if(_agents.containsKey(a.name))
			_agents.remove(a.name);
	}
	
	//set interval of poll mail. If poll service is not started, start it.
	public void setPollInterval(int minutes)
	{
		String strMin = minutes+"";
		AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);   
		Intent intent = new Intent(this, AlarmReceiver.class);
		intent.putExtra("minutes",strMin);
		int requestCode = 0;  
		if(pendIntent!=null)
		{
			pendIntent.cancel();
		}
		pendIntent = PendingIntent.getBroadcast(getApplicationContext(),   
		        requestCode++, intent, PendingIntent.FLAG_UPDATE_CURRENT);   
		// 5秒后发送广播，然后每个10秒重复发广播。广播都是直接发到AlarmReceiver的   
		int triggerAtTime = (int) (SystemClock.elapsedRealtime() + 2 * 1000);  //启动时间 
//		Log.i(Utils.LOGTAG, "Wakeup phone every minutes of:"+minutes);
		int interval = minutes*60*1000;   //发送间隔
//		alarmMgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, interval, pendIntent);  
		alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, interval, pendIntent);
	}
}

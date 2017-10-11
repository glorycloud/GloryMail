package mobi.cloudymail.mailclient;

import static mobi.cloudymail.util.Utils.LOGTAG;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import mobi.cloudymail.data.MailStatus;
import mobi.cloudymail.data.UserSetting;
import mobi.cloudymail.mailclient.net.Account;
import mobi.cloudymail.mailclient.net.Result;
import mobi.cloudymail.mailclient.net.ServerAgent;
import mobi.cloudymail.util.MyApp;
import mobi.cloudymail.util.NewDbHelper;
import mobi.cloudymail.util.Utils;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

public class ReceiveMailService extends Service
{
	public static final String PUSH_STRING = "PushFrequency";

	// key is account id, value is session id.
	private Map<Integer, SessionTime> idMap = new HashMap<Integer, SessionTime>();
	private Thread _thread = null;
	private Handler _handler = null;
	public static NotificationManager mNM = null;
	private Notification _notification = null;
	private PendingIntent _ntfIntent = null;
	public final static int HOLDON_MINUTES = 30;
	public static ReceiveMailService _instance = null;
	private static PowerManager.WakeLock wakeLock;
	
	public static class SyncRequest
	{
		private Iterable<Account> accountsToSync;
		public void setAccountsToSync(Iterable<Account> accounts)
		{
			accountsToSync = accounts;
		}
	}
	private static LinkedBlockingQueue<SyncRequest> accountToSync = new LinkedBlockingQueue<SyncRequest>();
	
	private class SessionTime
	{
		public String _sessionId = null;
		public long _loginTime = 0;
	}

	private long getHoldonTime()// in ms unit
	{
		return HOLDON_MINUTES * 60000;
	}


	@Override
	public IBinder onBind(Intent arg0)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate()
	{
		Log.d(LOGTAG, "ReceiveMailService" + "onCreate");
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		// showNotification(1);
		super.onCreate();
		_instance = this;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		Utils.log("ReceiveMailService onStartCommand");
		if(!MyApp.enableService)
			return START_NOT_STICKY ;
		if(wakeLock == null)
		{
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
//			wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "CloudyMailLock");
			wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CloudyMailLock");
		}
	

		idMap.clear();

		
		// start check new mail in background task.
		startBackgroundTask();
		return START_REDELIVER_INTENT;
	}

//	private void sendUpdataData()
//	{
//		Intent intent = new Intent();
//		intent.setAction("mobi.cloudymail.mailclient.ReceiveMailService");
//		Log.d(LOGTAG, "<<<<<<<start sendBroadCast>>>>>>");
//		sendBroadcast(intent);
//
//	}

	@Override
	public void onDestroy()
	{
		// _handler.removeCallbacks(_runnable);
		Log.d(LOGTAG, "ReceiveMailService" + "try to cancel notification");
		idMap.clear();
		if (_thread != null)
		{
			_thread.interrupt();
		}
		mNM.cancel(R.string.ntf_new_mail_name);
		super.onDestroy();
		_instance = null;
		MyApp.finishAllActivities();
		// android.os.Process.killProcess(android.os.Process.myPid());
		System.exit(0);
	}

	private static class MyHandler extends Handler
	{
		public void handleMessage(Message msg)
		{
			ReceiveMailService._instance.showNotification(msg.what);
			Activity  a = MyApp.getCurrentActivity();
			if(a instanceof MailFolderActivity)
			{
				try {
					((MailFolderActivity)a).updateMail();
				} catch (SQLException e) {
					Log.e(Utils.LOGTAG, "updateMail failed", e);
				}
			}
			super.handleMessage(msg);
		}
	}

	private void startBackgroundTask()
	{
		if (_thread == null || !_thread.isAlive())
		{
			_handler = new MyHandler();
			_thread = new Thread() {

				@Override
				public void run()
				{
					try
					{

						while(true)
						{
							SyncRequest req = accountToSync.take();
							wakeLock.acquire();
							try
							{
								if (!ServerAgent.hasNetworkConnection())
								{
									Utils.log( "ReceiveMailService" + "network is not available");
									continue;
								}

								
								
								// receive mails for all accounts;
								DefaultHttpClient sClient = ServerAgent.getHttpClient();
								int mailCount = 0;
								
								
								for (mobi.cloudymail.mailclient.net.Account a : req.accountsToSync)
								{
									if (a.password == null || a.password.equals(""))
										continue;
	
									mailCount += syncMail(sClient, a);
								}// for
	
								// if has new mail, notify user.
								if (mailCount > 0)
								{
									Utils.log( "New mails got, notify user");
									//sendUpdataData();
									
									_handler.sendEmptyMessage(mailCount);
								}
							}
							finally
							{
								wakeLock.release();
							}
						}// while
					}
					catch (InterruptedException e)
					{
						Utils.log("ReceiveMailService" + "receive InterruptedException");
					}
					finally
					{
						Utils.log("ReceiveMailService" + "Exite thread");
					}
				}

				private int syncMail(DefaultHttpClient sClient,  Account a)
				{
					int mailCount = 0;
					try
					{
						ServerAgent ag = MyApp.getAgent(a);
						HttpResponse rsp = null;
						
						for(int i=0;i<2;i++)
						{
							String sessionId = ag.getSessionId(false, false, false);
							String sql = "select max(uidx) from mail where accountId="
											+ a.id + " and state!="
											+ MailStatus.MAIL_DELETED;
							int normalMaxIdx = NewDbHelper.getInstance()
									.executScalar(sql, null);
							int deleteMaxIdx = NewDbHelper
									.getInstance()
									.executScalar(	"select max(uidx) from mail where accountId="
															+ a.id
															+ " and state="
															+ MailStatus.MAIL_DELETED, null);
							HttpGet req = new HttpGet(
														ServerAgent.getUrlBase()
																+ "/SyncupMail?nm="
																+ normalMaxIdx
																+ "&dm="
																+ deleteMaxIdx
																+ "&cn=1"
																+ (MyApp.userSetting.countPerReception > 0	? ("&mc=" + MyApp.userSetting.countPerReception)
																											: "")
																+ "&sid="
																+ java.net.URLEncoder
																		.encode(sessionId));
							rsp = ServerAgent.execute(req);
							Result rst = new Result(rsp.getEntity().getContent());
							
							if (!rst.isSuccessed())
							{
								if(rst.status == Result.NEEDLOGIN_FAIL)
								{
									ag.clearSessionId();
									continue;
								}
								Utils.log("Check new mail failed:"+rst.failReason);
								return mailCount;
							}
							mailCount = NewDbHelper.getInstance()
									.insertMailsToDb(rst.xmlReader, a.id);
							NewDbHelper.getInstance().updateMailGroupState();
							Utils.log( "ReceiveMailServer" +"accountId="+a.id+ " receive " + mailCount
											+ " mails");
							break;
						}
					}
					catch (Exception ex)
					{
						Log.e(Utils.LOGTAG, "ReceiveMailServer login/receive mail "
										+ a.name , ex);
					}
					return mailCount;
				}
			};
			try
			{
				_thread.start();
			}
			catch (IllegalThreadStateException ex)
			{
				Log.d(LOGTAG, "ReceiveMailService Failed to start receive mail thread", ex);
			}
		}
		
	}

	/**
	 * Show a notification while this service is running.
	 */
	private void showNotification(int newMailCount)
	{

		Log.d(LOGTAG, "ReceiveMailServer" + "now notify user in status bar");
		// Set the icon, scrolling text and timestamp
		Resources res = getResources();
		if (_notification == null)
		{
			_notification = new Notification(R.drawable.cloudymail,
												res.getString(R.string.ntf_new_mail_name),
												System.currentTimeMillis());
			// The PendingIntent to launch our activity if the user selects this
			// notification
			Intent intent = new Intent(this, GlobalInBoxActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.putExtra(MailFolderActivity.CLIENT_OPEN_MODE,
							MailFolderActivity.OPEN_FROM_STATUS_BAR);
			_ntfIntent = PendingIntent.getActivity(this, 0, intent, 0);
		}
		else
			_notification.when = System.currentTimeMillis();
		_notification.number = MyApp.instance().getNewMailTotalCount();

		// reset flag;
		_notification.flags = Notification.FLAG_AUTO_CANCEL;
		_notification.defaults = 0;
		if (MyApp.userSetting._ledFlag)
		{
			// _notification.defaults |= Notification.DEFAULT_LIGHTS;
			Log.d(LOGTAG, "ReceiveMailService" + "led flag on");
			// _notification.flags |= Notification.FLAG_SHOW_LIGHTS;

			_notification.ledARGB = 0x00FF00;// Color.RED;
			_notification.ledOffMS = 100;
			_notification.ledOnMS = 100;
			_notification.flags |= Notification.FLAG_SHOW_LIGHTS;

		}
		if (MyApp.userSetting._vibrateFlag)
		{
			_notification.defaults |= Notification.DEFAULT_VIBRATE;
			Log.d(LOGTAG, "ReceiveMailService" + "vibrate on");
		}
		if(MyApp.userSetting._sondFlag)
		{
			AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE); 
			_notification.audioStreamType=AudioManager.STREAM_NOTIFICATION;
			if(!isSilentTime())
			{
				_notification.defaults |= Notification.DEFAULT_SOUND; //添加声音
				//Log.d(LOGTAG, "ReceiveMailService" + "sond on");
			}
			
		}
		// if(!MailClient.userSetting._ringtone.equals(""))
		
//		_notification.sound = Uri.parse(MyApp.userSetting._ringtone);
//		Log.d(LOGTAG, "ReceiveMailService" + "ringtone:" + MyApp.userSetting._ringtone);

		// _notification.flags |= Notification.FLAG_AUTO_CANCEL;

		// Set the info for the views that show in the notification panel.
		_notification.setLatestEventInfo(this, res.getString(R.string.appName), 
		                                 res.getString(R.string.ntf_new_mail_title, _notification.number),
		                                 _ntfIntent);
		// Send the notification.
		// We use a layout id because it is a unique number. We use it later to
		// cancel.
		mNM.notify(R.string.ntf_new_mail_name, _notification);
	}

	public static void addToSyncQueue(SyncRequest r)
	{
		accountToSync.offer(r);
	}
	
	public boolean isSilentTime()
	{
		Calendar calendar = Calendar.getInstance();
		Date currentTime = calendar.getTime();
		UserSetting setting = MyApp.userSetting;

		// 2011-12-14
		SimpleDateFormat mateFormat = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");
		String tempTime = mateFormat.format(currentTime);

		String[] split2 = tempTime.split(" ");
		String[] split3 = split2[0].split("-");
		int year = Integer.parseInt(split3[0]);
		int month = Integer.parseInt(split3[1]);
		int day = Integer.parseInt(split3[2]);

		String muteTimeValue = MyApp.userSetting.getMuteTimeValue();
		String[] split = muteTimeValue.split("-");
		String[] splitStartTime = muteTimeValue.split("-");
		String[] startTime = splitStartTime[0].split(":");
		int startHour = Integer.parseInt(startTime[0]);
		int startMinute = Integer.parseInt(startTime[1]);
		int seconds = calendar.get(Calendar.SECOND);
		Date startMateTime = new Date(year - 1901, month + 11, day, startHour,
				startMinute, seconds);

		String[] endTime = splitStartTime[1].split(":");
		int endHour = Integer.parseInt(endTime[0]);
		int endMinute = Integer.parseInt(endTime[1]);
		Date endMuteTime = null;
		if (endHour < startHour) 
		{
//			endMuteTime = new Date(year - 1901, month + 11, day + 1, endHour,
//					endMinute, seconds);
			//currentTime.getHours()小于 startHour，则结束时间为当天，开始时间为昨天
			if (currentTime.getHours() < startHour)
			{
				endMuteTime = new Date(year - 1901, month + 11, day, endHour,
						endMinute, seconds);
				startMateTime = new Date(year - 1901, month + 11, day-1, startHour,
						startMinute, seconds);
			}
			//currentTime.getHours() 大于 startHour，则结束时间为明天，开始时间为今天
			else if(currentTime.getHours() >= startHour)
			{
				endMuteTime = new Date(year - 1901, month + 11, day+1, endHour,
						endMinute, seconds);
				
			}
			
		}
		else 
		{
			endMuteTime = new Date(year - 1901, month + 11, day, endHour,
					endMinute, seconds);
		}

		if (MyApp.userSetting.muteEnabled && currentTime.before(endMuteTime)
				&& currentTime.after(startMateTime))
			return true; // it's in a silient time
		else 
			return false;
	}
}

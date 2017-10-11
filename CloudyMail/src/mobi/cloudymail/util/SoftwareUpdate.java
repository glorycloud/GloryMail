package mobi.cloudymail.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.EnumSet;

import mobi.cloudymail.mailclient.R;
import mobi.cloudymail.mailclient.net.ServerAgent;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

public class SoftwareUpdate
{
	String _oldVersion = null;
	int _oldVersionCode = -1;
	String _newVersion = null;
	int _newVersionCode = -1;
	String _newFeature = "";
	long apkDownloadId = 0;
	DownloadCompleteReceiver receiver;  
	final static String APK_NAME = "CloudyMail.apk";
	final static String VERSION_JSON = "version.json";
	final static String VERSION_JSON_EN = "version_en.json";
	private Handler handler = new Handler();
	private ProgressDialog pBar=null;
//	private Context ctx;
	private boolean _doSilent = false;
	
	private static SoftwareUpdate _inst=null;
	
	public static SoftwareUpdate getInstance()
	{
		if(_inst == null)
			_inst = new SoftwareUpdate();
		return _inst;
	}
	class DownloadCompleteReceiver extends BroadcastReceiver {  
		 
        @Override  
        public void onReceive(Context context, Intent intent) {  
            if(intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)){  
                long downId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);  
                if(downId == apkDownloadId)
                {
                	DownloadManager dm = (DownloadManager)MyApp.instance().getSystemService(Context.DOWNLOAD_SERVICE);
                	
                	down("");
                	if(receiver != null)MyApp.instance().unregisterReceiver(receiver);  
                }
                //Toast.makeText(context, intent.getAction()+"id : "+downId, Toast.LENGTH_SHORT).show();  
            }  
        }  
    }  
	private SoftwareUpdate()
	{
		_oldVersion = getVerName();
		_oldVersionCode = getVerCode();
		_newVersion = _oldVersion;
		_newVersionCode = _oldVersionCode;
	}
	
	public static int getVerCode() {  
        int verCode = -1;  
        try {  
            verCode = MyApp.instance().getPackageManager().getPackageInfo(  
                    "mobi.cloudymail.mailclient", 0).versionCode;  
        } catch (NameNotFoundException e) {  
        	Log.d(Utils.LOGTAG, "",e);
        }  
        return verCode;  
    } 
	
	public static String getVerName() {  
        return MyApp.instance().getResources().getString(R.string.about_version);   
        }
	
	private boolean getServerVer() { 
		try {
			String versionFilePath =null;
			if (Utils.isInChinese())
			{
				versionFilePath = downloadVersionFile(	ServerAgent.getUrlBase() + "/"
																+ VERSION_JSON, VERSION_JSON);
			}
			else
			{
				versionFilePath = downloadVersionFile(ServerAgent.getUrlBase() + "/"
														+ VERSION_JSON_EN, VERSION_JSON_EN);
			}
			if (versionFilePath.equals(""))
				return false;
			// read json file;
			BufferedReader reader = null;
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(versionFilePath),"gb2312"));//new FileReader(versionFilePath));
			StringBuffer buffer = new StringBuffer();
			String line = reader.readLine(); // 读取第一行
			while (line != null) { // 如果 line 为空说明读完了
				buffer.append(line); // 将读到的内容添加到 buffer 中
				line = reader.readLine(); // 读取下一行
			}
			JSONArray jary = new JSONArray(buffer.toString()); 
			JSONObject obj = jary.getJSONObject(0);
			_newVersionCode = Integer.parseInt(obj.getString("verCode"));
			_newVersion = obj.getString("verName");
			if(obj.has("newFeature"))
				_newFeature = obj.getString("newFeature");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			_newVersionCode = -1;
			_newVersion = "";
			return false;
		}  	
  //the version file looks like
  //[{"appName":"CloudyMail","apkname":"CloudyMail.apk","verName":1.0.0,"verCode":2}]
    }
	
	//return downloaded json file path.
	private String downloadVersionFile(String url, String fileName)
	{
		 HttpGet get = new HttpGet(url);  
         HttpResponse response;  
         try {  
             response = ServerAgent.execute(get);  
             HttpEntity entity = response.getEntity();  
             long length = entity.getContentLength();  
             InputStream is = entity.getContent();  
             FileOutputStream fileOutputStream = null;  
             String filePath="";
             if (is != null) {  
            	 filePath = MyApp.instance().getAppSdcardPath()+"/"+fileName;
                 File file = new File(MyApp.instance().getAppSdcardPath(),fileName); 
                 fileOutputStream = new FileOutputStream(file);  
                 byte[] buf = new byte[1024];  
                 int ch = -1;  
                 int count = 0;  
                 while ((ch = is.read(buf)) != -1) {  
                     fileOutputStream.write(buf, 0, ch);  
                     count += ch;  
                 }  
             }    
             if (fileOutputStream != null) 
             {
            	 fileOutputStream.flush();
                 fileOutputStream.close();  
             }  
             return filePath;
         } catch (ClientProtocolException e) {  
             e.printStackTrace(); 
             return "";
         } catch (IOException e) {  
             e.printStackTrace(); 
             return "";
         }  
	}
	
	private void onGetServerVerFinished(final boolean value)
	{
		handler.post(new Runnable() {
			public void run()
			{
				if (value)
				{
					if (_newVersionCode > _oldVersionCode)
					{
						doNewVersionUpdate(); // 更新新版本
					}
					else if(!_doSilent)
					{
						notNewVersionShow(); // 提示当前为最新版本
					}
				}
				else if(!_doSilent)
				{
					Activity curAct = MyApp.getCurrentActivity();
					DialogUtils.showMsgBox(curAct,
					                       curAct.getResources().getString(R.string.err_get_latest_version),
					                       curAct.getResources()
												.getString(R.string.error));
				}
			}
		});
	}
	
	public void checkUpdate(boolean silence)
	{
//		this.ctx = ctx;
		_doSilent = silence;
		Activity curAct = MyApp.getCurrentActivity();
		if(!ServerAgent.hasNetworkConnection())
		{
			if(!silence)
			{
				Resources res = curAct.getResources();
				MessageBox.show(curAct, res.getString(R.string.err_notConnected),
								res.getString(R.string.error));
			}
			return;
		}
		
		Log.d(Utils.LOGTAG, "check version update");
		new Thread() {  
	        public void run() {
/*	        	try
	        	{
	        	this.sleep(1000);
	        	}catch(Exception excp)
	        	{
	        		
	        	}*/
	        	
	        	boolean value = getServerVer();
	        	onGetServerVerFinished(value);
	        }  
	    }.start();
	    
	    
/*		if (getServerVer()) {  
	         if (_newVersionCode > _oldVersionCode) {  
	             doNewVersionUpdate(); // 更新新版本  
	         } else {  
	             notNewVersionShow(); // 提示当前为最新版本  
	         }  
	     } 
		else {
			DialogUtils.showMsgBox(ctx, "Failed to get latest version number!",
					ctx.getResources().getString(R.string.error));
		}*/
	}
	
	private void notNewVersionShow() {    
		Activity curAct = MyApp.getCurrentActivity();
	    Resources res = curAct.getResources();
	    String contentStr = String.format(res.getString(R.string.no_need_update_str),_oldVersion);
	    DialogUtils.showMsgBox(curAct, contentStr, res.getString(R.string.update_software)); 
	}  
	
	private void doNewVersionUpdate() {  
		Activity curAct = MyApp.getCurrentActivity();
	    Resources res = curAct.getResources();
	    String contentStr = String.format(res.getString(R.string.whether_update_str),_oldVersion,_newVersion,_newFeature);  
		try {
			if (DialogUtils.showModalMsgBox(curAct, contentStr, res.getString(R.string.update_software),
					EnumSet.of(DialogUtils.ButtonFlags.Yes,
							DialogUtils.ButtonFlags.No)) == DialogResult.OK) {

				downFile(ServerAgent.getUrlBase() + "/"+APK_NAME,APK_NAME);
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	private void downFile(final String url, final String fileName) {  
		DownloadManager dm = (DownloadManager)MyApp.instance().getSystemService(Context.DOWNLOAD_SERVICE);
		Request req = new DownloadManager.Request( Uri.parse(url));
		req.setShowRunningNotification(true);
		File file = new File(MyApp.instance().getAppSdcardPath(),APK_NAME); 
		if(file.exists())
			file.delete();
		req.setDestinationUri(Uri.fromFile(file));
		receiver = new DownloadCompleteReceiver();  
		MyApp.instance().registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));  
		apkDownloadId = dm.enqueue(req);
	      
	} 
	
	private void down(final String msg) {  
        handler.post(new Runnable() {  
            public void run() {  
                if(msg.equals(""))
                	update(); 
                else {
                	Activity curAct = MyApp.getCurrentActivity();
                	Resources res = curAct.getResources();
					DialogUtils.showMsgBox(curAct, res.getString(R.string.err_update_failed)+msg,
							res.getString(R.string.error));
				}
            }  
        });  
	}
	
	private void update() {  
	    Intent intent = new Intent(Intent.ACTION_VIEW);  
	    intent.setDataAndType(Uri.fromFile(new File(MyApp.instance().getAppSdcardPath(), 
	    		APK_NAME)),  
	            "application/vnd.android.package-archive");  
	    MyApp.getCurrentActivity().startActivity(intent);  
	} 	
}
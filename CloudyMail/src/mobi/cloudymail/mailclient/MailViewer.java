package mobi.cloudymail.mailclient;

import static mobi.cloudymail.util.Utils.LOGTAG;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import mobi.cloudymail.data.InMailInfo;
import mobi.cloudymail.data.MailInfo;
import mobi.cloudymail.mailclient.net.AttachmentInfo;
import mobi.cloudymail.mailclient.net.DownloadAttaTask;
import mobi.cloudymail.mailclient.net.ServerAgent;
import mobi.cloudymail.util.DialogUtils;
import mobi.cloudymail.util.MessageBox;
import mobi.cloudymail.util.MyApp;
import mobi.cloudymail.util.NewDbHelper;
import mobi.cloudymail.util.Utils;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.RenderPriority;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

public class MailViewer extends BaseActivity 
{
	public static MailViewer instance = null;

	public static final String PREVIOUS_MAIL = "preMail";
	public static final String NEXT_MAIL = "nextMail";
	public static final String DEL_MAIL = "delMail";

	private MyWebView _htmlView = null;
	private ProgressBar _progresBar = null;
	private String _currentUrl = "";
	// private Button _attachBtn = null;
	private String refMailBody = null;
	
	private boolean hasAttachFlag;
	
	private Resources res = MyApp.instance().getResources();
	
	private final class MailWebViewClient extends WebViewClient
	{
		boolean timeout = true;
		private MailWebViewClient(Activity activity)
		{
			
		}

		@Override //never called ? 
		public boolean shouldOverrideUrlLoading(WebView view, String url)
		{
			
			Uri uri = Uri.parse(url);
			URL currentUrl;
			try
			{
				currentUrl = new URL(view.getUrl());
				//Judge whether the current mailView, if is open mailView, or is the external browser
				if (!currentUrl.getHost().equalsIgnoreCase(uri.getHost()))
				{
					Intent intent = new Intent(Intent.ACTION_VIEW, uri);
					startActivity(intent);
				}
				else
				{
					view.loadUrl(url);
				}
			}
			catch (MalformedURLException e)
			{
				Log.d(Utils.LOGTAG, "",e);
			}

			// Log.d("shouldOverrideUrlLoading", url);
			return true;

		}

		@Override
		public void onPageStarted(WebView view, String url,
				android.graphics.Bitmap favicon)
		{
			
			showProgressBar(true);
			timeout = true;
			final Handler myHandler = new Handler();
			Runnable run = new Runnable() {
                 public void run() {

                     if(timeout) {
                         // do what you want
 						Resources res = MailViewer.this.getResources();
 						DialogUtils.showMsgBox(MailViewer.this, res.getString(R.string.timeout), res.getString(R.string.timeout_title), 
 								new Runnable() {
 									@Override
 									public void run()
 									{
 										MailViewer.this.finish();
 									}
 							});
 						
                     }
                 }
             };
             myHandler.postDelayed(run, 30000);
		}

		@Override
		public void onPageFinished(WebView view, String url)
		{
			timeout = false;
			//Log.d(LOGTAG, "onPageFinished" + url);
			if (url.equals("about:relogin"))
			{
				view.stopLoading();
				if (MyApp.getAgent().interactiveLogin(false,true,true))
				{
					view.stopLoading();
					String tmpUrl = _currentUrl;
					int pos = tmpUrl.indexOf("sid=");
					if (pos > 0)
						tmpUrl = tmpUrl.substring(0, pos)
									+ "sid="
									+ java.net.URLEncoder.encode(MyApp
											.getAgent().getSessionId(false,true,true));
					else
						tmpUrl += "&sid="
									+ java.net.URLEncoder.encode(MyApp
											.getAgent().getSessionId(false,true,true));
					view.loadUrl(tmpUrl);
				}
				else
					finish(); // since login failed or canceled, back to
								// previous step
				return;
			}
			// MyApp.curMailInfo will be null if MailViewer activity finished
			// before HTML load complete
			if (MyApp.curMailInfo == null)
				return;
			if (MyApp.curMailInfo.getAsterisk() != 0)
			{
				final Timer t = new Timer();
				t.schedule(new TimerTask() {

					@Override
					public void run()
					{
						_htmlView.loadUrl("javascript:setStarOn();");
						t.cancel();
					}
				}, 100);
			}

			_currentUrl = url;
			showProgressBar(false);
			// _htmlView.loadUrl("javascript:window.HTMLOUT.showHTML(document.getElementsByTagName('html')[0].innerHTML);");
		}


		@Override
		public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
		{
			view.loadUrl("about:blank");
			Log.e(Utils.LOGTAG, "onReceivedError, code="+errorCode+" DESC:"+description +" URL:"+failingUrl);
			DialogUtils.showMsgBox(MailViewer.this, res.getString(R.string.timeout), res.getString(R.string.timeout_title), 
						new Runnable() {
							@Override
							public void run()
							{
								MailViewer.this.finish();
							}
					});
		}
	}

	private final class MailChromeClient extends WebChromeClient
	{
		@Override
		public void onProgressChanged(WebView view, int progress)
		{
			_progresBar.setProgress(progress);
		}
		// @Override
		// public boolean onJsAlert(WebView view, String url, String message,
		// android.webkit.JsResult result)
		// {
		// Log.d(LOGTAG, "JSAlert:"+message);
		// result.confirm();
		// return true;
		// }
	}
	
	static public boolean isSdCardAvailable(Activity activity)
	{
		Resources res = MyApp.instance().getResources();
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
		{
			MessageBox.show(activity, res.getString(R.string.atta_sdcardReadOnly),
							res.getString(R.string.error));
			return false;
		}
		else if (!Environment.MEDIA_MOUNTED.equals(state))
		{
			MessageBox.show(activity,
							res.getString(R.string.atta_sdcardNotAvailable),
							res.getString(R.string.error));
			return false;
		}
		return true;
	}
	
	static public String getAvailableFilePathToSave(String fileName)
	{
		File sdCardDir = Environment.getExternalStorageDirectory();
		File storeFile = new File(sdCardDir,fileName);
		int i = 1;
		while (storeFile.exists())
		{
			String attInfoFileName = fileName;
			String attInfoFileFormat = "";
			int pointIndex = fileName.lastIndexOf(".");
			if(pointIndex >=0)
			{
				attInfoFileName = fileName.substring(0, pointIndex);
				attInfoFileFormat = fileName.substring(pointIndex, fileName.length());
			}
			String storeFileName = attInfoFileName + "(" + i + ")"+attInfoFileFormat;
			
			storeFile = new File(sdCardDir, storeFileName);
			i++;
		}
		return storeFile.getAbsolutePath();
	}

	class CmailScriptInterface 
	{
		WebView webView;
		public CmailScriptInterface(WebView webView)
		{
			this.webView = webView;
		}
        public void addAttachInfo(int attachIndex,String fileName,
        							String size,String fileType,boolean previewFlag)
        {
        	//if loaded before, do not insert into database.
        	if(MyApp.curMailInfo.getAttachment(attachIndex) != null)
        		return;
        	AttachmentInfo attach = new AttachmentInfo(MyApp.curMailInfo);
        	attach.index = attachIndex;
        	attach.fileName = fileName;
        	attach.size = size;
        	attach.canPreview = previewFlag;
        	attach.fileType = fileType;
        	MyApp.curMailInfo.addAttachInfo(attach);
        	
        	NewDbHelper.getInstance().insertAttachInfo(attach,MyApp.curMailInfo.getAccountId());
        	/**Data stored in the Attach*/
//        	NewDbHelper.getInstance().getAttachmentInfo();
        }
        public void setHasAttachment(boolean attach)
        {
        }
		public void reply()
		{
			MailViewer.this.openComposer(Composer.COMPOSER_REPLY);
		}

		public void importCalendarAttachment(final int attachmentIndex, final String fileName)
		{
			try {
				downloadAttachment(attachmentIndex, true);
			}
			catch (Exception e1) {
				Log.e(Utils.LOGTAG, "Fail importCalendarAttachment", e1);
			}
		}
		
		public void downloadAttachment(final int attachmentIndex, final boolean isCalendarImport)
		{		
			if(!isSdCardAvailable())
			{
				MessageBox.show(MyApp.getCurrentActivity(), res.getString(R.string.atta_sdcardNotAvailable), res.getString(R.string.atta_sdcardTitle));
				return; 
			}
				 
			AttachmentInfo attach = MyApp.curMailInfo.getAttachment(attachmentIndex);
				        	
			File sdCardDir=Environment.getExternalStorageDirectory();
			File storeFile=new File(sdCardDir,attach.fileName);
			int i=1;
			while(storeFile.exists())
			{
				String attInfoFileName=attach.fileName;
				String attInfoFileFormat="";
				int pointIndex=attach.fileName.lastIndexOf(".");
				if(pointIndex>=0)
				{
					attInfoFileName=attach.fileName.substring(0,pointIndex);
				    attInfoFileFormat=attach.fileName.substring(pointIndex, attach.fileName.length());
				}
				String storeFileName=attInfoFileName+"("+i+")"+attInfoFileFormat;
				storeFile=new File(sdCardDir,storeFileName);
				i++;
			}
			new DownloadAttaTask(attach, null, isCalendarImport).execute(storeFile.getAbsolutePath());
		}
		
		public void openAttachment(final int attachmentIndex,final String attachmentName)
		{
			runOnUiThread(new Runnable() {

				@Override
				public void run()
				{
					showAttachmentView(attachmentIndex,attachmentName);
				}

			});
		}

		private boolean isSdCardAvailable() {
			Resources res=MyApp.instance().getResources();
			String state=Environment.getExternalStorageState();
			if(Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
			{
				MessageBox.show(MyApp.getCurrentActivity(), res.getString(R.string.atta_sdcardReadOnly),res.getString(R.string.error));
				return false;
			}
			else if(!Environment.MEDIA_MOUNTED.equals(state))
			{
				MessageBox.show(MyApp.getCurrentActivity(), res.getString(R.string.atta_sdcardNotAvailable), 
						        MyApp.getCurrentActivity().getString(R.string.error));
			    return false;
			}
			return true;
		}
		

		public void replyAll()
		{
			MailViewer.this.openComposer(Composer.COMPOSER_REPLYALL);
		}

		public void forward()
		{
			MailViewer.this.openComposer(Composer.COMPOSER_FORWARDMAIL);
		}

		/**
		 * 
		 * @param state
		 *            "1" or "0" for has star or not
		 */
		public void setStar(String state)
		{
			MailInfo mailInfo = MyApp.curMailInfo;
			
			int asteriskState = Integer.parseInt(state);
			
			NewDbHelper.getInstance().updateInAsteriskstatu(mailInfo.getAccountId(), asteriskState, mailInfo.getUidx(), mailInfo.getFolder());

		}

		public void setMailBody(String mail)
		{
			refMailBody = mail;
		}

		public void showHTML(String html)
		{
			/*
			 * new AlertDialog.Builder(MailViewer.this) .setTitle("HTML")
			 * .setMessage(html) .setPositiveButton(android.R.string.ok, null)
			 * .setCancelable(false) .create() .show();
			 */
			MailInfo curMailInfo = MyApp.curMailInfo;
			// only update the mail content if mail is the first time to read.
			if (curMailInfo.getBody() != null
				&& (!curMailInfo.getBody().equals("") || html.equals("")))
				return;
			curMailInfo.setBody(html);
			NewDbHelper.getInstance().updateMailBody((InMailInfo) curMailInfo);
		}
		
		
		public void relogin()
		{
			runOnUiThread(new Runnable() {

				@Override
				public void run()
				{
					
					webView.stopLoading();
					//showProgressBar(true);
					if (MyApp.getAgent().interactiveLogin(true,true,true))
					{
						//showProgressBar(false);
						webView.stopLoading();
						String tmpUrl = _currentUrl;
						int pos = tmpUrl.indexOf("sid=");
						if (pos > 0)
							tmpUrl = tmpUrl.substring(0, pos)
										+ "sid="
										+ java.net.URLEncoder.encode(MyApp
												.getAgent().getSessionId(false,true,true));
						else
							tmpUrl += "&sid="
										+ java.net.URLEncoder.encode(MyApp
												.getAgent().getSessionId(false,true,true));
						webView.loadUrl(tmpUrl);
					}
					else
						finish(); // since login failed or canceled, back to
									// previous step
				}

			});

		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		//DialogUtils.showMsgBox(MyApp.getCurrentActivity(),"test", "test");
		instance = MailViewer.this;
		// Window win = getWindow();
		// win.requestFeature(Window.FEATURE_LEFT_ICON);
		// win.requestFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.mail_viewer);
		// win.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
		// R.drawable.cloudymail);

		_htmlView = (MyWebView) findViewById(R.id.htmlMailView);
		// _htmlView = new MyWebView(this);
		_progresBar = (ProgressBar) findViewById(R.id.maiViewProgressBar);


		MailInfo curMailInfo = MyApp.curMailInfo;
		// _subTextView.setText(curMailInfo.subject);
		// Let's display the progress in the activity title bar, like the
		// browser app does.
		WebSettings setting = _htmlView.getSettings();
		Log.d(Utils.LOGTAG, "Cache Mode:" + setting.getCacheMode());
		
		//_htmlView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK); //this cause error page cached, and not reload from server. 
																					//prevent recovery from errors
		_htmlView.getSettings().setJavaScriptEnabled(true);
		_htmlView.getSettings().setBuiltInZoomControls(true);
		_htmlView.getSettings().setRenderPriority(RenderPriority.HIGH);
		_htmlView.getSettings().setLightTouchEnabled(true);
		_htmlView.addJavascriptInterface(new CmailScriptInterface(_htmlView), "cmail");
		final Activity activity = this;
		_htmlView.setWebChromeClient(new MailChromeClient());
		
		// try
		// {
		// Log.d(LOGTAG,
		// "Cache dir:"+CacheManager.getCacheFileBaseDir().getCanonicalPath());
		// }
		// catch (IOException e1)
		// {
		//
		// }
		_htmlView.setWebViewClient(new MailWebViewClient(activity));
		// prgDialog.show();
		// lstView = (ListView)findViewById(R.id.mvVirtualListView);
		// lstView.setAdapter(new VirtualListAdapter(this,
		// _htmlView,MyApp.curMailInfo));
		showProgressBar(true);
		String mbody = curMailInfo.getBody();
		if (mbody == null || mbody.equals(""))
		{
			String sid = MyApp.getAgent().getSessionId(true,true,true);
			if (sid == null)
			{
				finish();
				return;
			}
			String urlStr = ServerAgent.getUrlBase()
							+ "/MailRender?uid="
							+ java.net.URLEncoder.encode(curMailInfo.getUid())
							+ "&folderName="
							+ java.net.URLEncoder.encode(MyApp.curMailInfo.getFolder())
							+ "&pageNo=0&sid="
							+ java.net.URLEncoder.encode(sid)
							+ "&lang="+Locale.getDefault().getLanguage();
			//Log.d(LOGTAG, "Opening mail:" + urlStr);
			_htmlView.loadUrl(urlStr);
		}
		else
		{
			_htmlView.loadDataWithBaseURL(	ServerAgent.getUrlBase() + "/",
											curMailInfo.getBody(), "text/html",
											"utf-8", ServerAgent.getUrlBase());
			showProgressBar(false);
		}
	}

	private void showProgressBar(boolean visible)
	{
		_progresBar.setVisibility(visible ? View.VISIBLE : View.GONE);
		
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		_htmlView.stopLoading();
	}

	public void openComposer(int type)
	{
		Intent intent = new Intent(this, Composer.class);
		intent.putExtra("composer_type", type);
		if (type == Composer.COMPOSER_REPLYALL)
			intent.putExtra("ccList", MyApp.curMailInfo.getCc());
		intent.putExtra("refMailBody", refMailBody);
		intent.putExtra("hasAttachment", hasAttachFlag);
		startActivity(intent);
	}

	private void showAttachmentView(int attachmentIndex, String attachmentName)
	{
		MailInfo curMailInfo = MyApp.curMailInfo;
		if (curMailInfo == null || !curMailInfo.hasAttachment())
			return;
		
		Intent intent = new Intent(this, AttachmentViewer.class);
		intent.putExtra(AttachmentViewer.ATTACHMENT_INFO, (Parcelable)curMailInfo.getAttachment(attachmentIndex));
		startActivity(intent);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_MENU)
		{
			Intent intent = this.getIntent();
			Intent newIntent = new Intent(MailViewer.this,TranslucentButton.class);
			newIntent.putExtra(MailViewer.PREVIOUS_MAIL,intent.getBooleanExtra(PREVIOUS_MAIL, true));
			newIntent.putExtra(MailViewer.NEXT_MAIL,intent.getBooleanExtra(NEXT_MAIL, true));
			newIntent.putExtra(MailViewer.DEL_MAIL,intent.getBooleanExtra(DEL_MAIL, true));
			startActivityForResult(newIntent, 1);
			overridePendingTransition(R.anim.fade, R.anim.hold);
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	protected void onActivityResult(int reqCode, int rstCode, Intent intent)
	{
		MyApp.setCurrentActivity(this);
		if (reqCode == 1) // override menu bar activity finished
		{
			switch (rstCode)
			{
			case R.id.last:
				setResult(R.id.last);
				finish();
				break;
			case R.id.next:
				setResult(R.id.next);
				finish();
				break;
			case R.id.delMail:
				setResult(R.id.delMail);
				finish();
				break;
			case R.id.replyMail:
				openComposer(Composer.COMPOSER_REPLYALL);
				break;
			}
		}
	}
}

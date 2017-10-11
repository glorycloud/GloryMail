package mobi.cloudymail.mailclient;

import java.lang.reflect.Method;
import java.util.Locale;

import mobi.cloudymail.mailclient.net.AttachmentInfo;
import mobi.cloudymail.mailclient.net.DownloadAttaTask;
import mobi.cloudymail.mailclient.net.ServerAgent;
import mobi.cloudymail.util.DialogUtils;
import mobi.cloudymail.util.MyApp;
import mobi.cloudymail.util.NewDbHelper;
import mobi.cloudymail.util.Utils;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebHistoryItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class AttachmentViewer extends BaseActivity 
{
	public static final String ATTACHMENT_INFO = "attachment_info";
	private long mExitTime;

	AttachmentInfo attach = null;
	private String internalPath = null;
	int pageNumber = 0;
	private int totalPageCount=0;
	private String pageNums[];
	CmailScriptInterface cmailScriptInterface=new CmailScriptInterface();
	ImageButton imgBtnLast;
	ImageButton imgBtnNext;
	ImageButton imgBtnJump;
	Button pageBtnFlag;
	boolean enableImgBtnClick=false;
	String _currentUrl = null;
	public static void setBrowserOverviewMode(WebView view, boolean mode)
	{
		if(android.os.Build.VERSION.SDK_INT >= 7) //version 2.1 or higher
		{
			Class<WebSettings> cls = WebSettings.class;
			try
			{
				Method method = cls.getMethod("setLoadWithOverviewMode", boolean.class);
				method.invoke(view.getSettings(), mode);
			}
			catch (Exception e)
			{
				Log.d(Utils.LOGTAG, "Can't setLoadWithOverviewMode ",e);
				
			}
		}

	}
	class CmailScriptInterface
	{
		public void fitView()
		{
			AttachmentViewer.this.runOnUiThread(new Runnable() {
				
				@Override
				public void run()
				{
					setBrowserOverviewMode(_previewViewer, true);
					_previewViewer.getSettings().setUseWideViewPort(true);
				}
			});

//			int h ;
//			h  = _previewViewer.getContentHeight();
//			if(h <= 0)
//			{
//				Timer t = new Timer();
//				t.schedule(new TimerTask() {
//					
//					@Override
//					public void run()
//					{
//						int h2  = _previewViewer.getContentHeight();
//						while((h2  = _previewViewer.getContentHeight()) > _previewViewer.getHeight())
//							_previewViewer.zoomOut();
//						
//					}
//				},1000);
//			}
//			while((h  = _previewViewer.getContentHeight()) > _previewViewer.getHeight())
//				_previewViewer.zoomOut();
		}
		
		//download file from extracted rar/zip package.
		public void downloadAttachment(final int attachmentIndex,final String fileName,final String internalPath)
		{
			if (!MailViewer.isSdCardAvailable(AttachmentViewer.this))
				return;
			runOnUiThread(new Runnable() {

				@Override
				public void run()
				{
					DownloadAttaTask downTask = new DownloadAttaTask(attach, internalPath, false);
					downTask.execute(MailViewer.getAvailableFilePathToSave(fileName));
				}
			});	
		}
		
		public void openAttachment(final int attachmentIndex,final String attachmentName, final String internalPath)
		{
			AttachmentViewer.this.internalPath = internalPath;
			runOnUiThread(new Runnable() {

				@Override
				public void run()
				{
					previewAttachment(attachmentIndex,attachmentName,internalPath);
				}

			});
		}
		//仿照MailViewer中的showHTML
		public void saveAttach(String html,int pageNo,int totalPageCount) 
		{
			if(internalPath != null) //压缩包里面的文件预览时暂时不进行保存
				return;
			String attachBody = NewDbHelper.getInstance().getAttachBody(attach,pageNo);
			pageNumber = pageNo;
			if (attachBody != null && (!attachBody.equals("") || html.equals("")) )
				return;
			attach.setBody(html);
			NewDbHelper.getInstance().insertAttachBody(attach,pageNo);
			
			if(AttachmentViewer.this.totalPageCount==0)
			{
				AttachmentViewer.this.totalPageCount=totalPageCount;
				pageNums=new String[AttachmentViewer.this.totalPageCount];
				for(int i=0;i<AttachmentViewer.this.totalPageCount;i++)
				{
					pageNums[i]=String.valueOf(i+1);
				}
				attach.setTotalPageCount(totalPageCount);
				NewDbHelper.getInstance().updateAttachTotalPageCount(attach);
			}
			
			
//			System.out.println("savaAttach");
		}
		public void gotoPage(int pageNo)	
		{
			String attachUrl;
			String attachBody = NewDbHelper.getInstance().getAttachBody(attach,pageNo);
			if (attachBody != null && (!attachBody.equals("")) )
			{
				_previewViewer.loadDataWithBaseURL(	ServerAgent.getUrlBase() + "/",
                    attachBody, "text/html",
					"utf-8", "local:"+pageNo);
				 dumpWebHistory(_previewViewer, "gotoPage(146)after Loading " + "local:" + pageNo);
			}
			else
			{   //loadUrl
				String sid = MyApp.getAgent(AccountManager.getAccount(attach.getAccountId())).getSessionId(true,true,true);
				//makePreviewUrl
				if(sid == null)
				{
				    attachUrl = null;
				}
				else
				{
					attachUrl = makePreviewUrl(attach.getAttachIndx(), attach.getMailUid(), null, pageNo);
				}
				
				if(attachUrl == null)
				{
					finish();
				}
				else
				{
					String upperFileName = attach.fileName.toUpperCase();
					boolean isPPT = upperFileName.endsWith(".PPT") || upperFileName.endsWith(".PPTX");
					setBrowserOverviewMode(_previewViewer, isPPT);
				
					_previewViewer.getSettings().setUseWideViewPort(true);
					_previewViewer.loadUrl(attachUrl);
					 dumpWebHistory(_previewViewer, "gotoPage(173) after Loading " + attachUrl);
				}
				
				
			}
			AttachmentViewer.this.pageBtnFlag.setText((pageNumber+1)+"");
			
		}
		public void relogin()
		{
			runOnUiThread(new Runnable() {

				@Override
				public void run()
				{
					
					_previewViewer.stopLoading();
					//showProgressBar(true);
					if (MyApp.getAgent().interactiveLogin(true,true,true))
					{
						//showProgressBar(false);
						_previewViewer.stopLoading();
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
						_previewViewer.loadUrl(tmpUrl);
					}
					else
						finish(); // since login failed or canceled, back to
									// previous step
				}

			});

		}
	}
	private static final int MODE_ATTACHLIST = 1; // read attachment for mail
													// viewer
	private static final int MODE_PREVIEW = 2; // preview attachment
	private static final int MODE_PREVIEW_PROGRESS = 3;// proviewing progress
														// bar
	private static final int MODE_ATTACHLIST_WRITE = 4;// add attachment view
														// for composer

	
	private WebView _previewViewer = null;
	private RelativeLayout _progressBarLayout = null;
	private ProgressBar _progressBar = null;

	private int _currentMode = MODE_ATTACHLIST;

	private void dumpWebHistory(WebView view, String text)
	{
		WebBackForwardList hist = view.copyBackForwardList();
		Log.d("WebHistory", text+" History length:" + hist.getSize());
		
		for(int i=0;i<hist.getSize();i++)
		{
			WebHistoryItem item = hist.getItemAtIndex(i);
			Log.d("WebHistory", "\t"+item.getUrl());
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Window win = getWindow();
		win.requestFeature(Window.FEATURE_LEFT_ICON);
		setContentView(R.layout.attachment_viewer);
		win.setFeatureDrawableResource(	Window.FEATURE_LEFT_ICON,
										R.drawable.cloudymail);
//		_attachPreviewProgressLayout=findViewById(R.id.attachPreviewProgressLayout);
		_previewViewer = (WebView) findViewById(R.id.previewView);

		_progressBar = (ProgressBar) findViewById(R.id.attachPreviewProgressBar);
		_progressBarLayout = (RelativeLayout) findViewById(R.id.attachPreviewProgressLayout);

		_previewViewer.getSettings().setJavaScriptEnabled(true);
		_previewViewer.getSettings().setUseWideViewPort(true);
		_previewViewer.getSettings().setLightTouchEnabled(true);
		_previewViewer.setWebChromeClient(new WebChromeClient() {
			@Override
			public void onProgressChanged(WebView view, int progress)
			{
				// Activities and WebViews measure progress with different
				// scales.
				// The progress meter will automatically disappear when we reach
				// 100%
				// activity.setProgress(progress * 1000);
				_progressBar.setProgress(progress);
			}
		});
		
		_previewViewer.setWebViewClient(new WebViewClient() {
			boolean timeout = true;
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url)
			{
				view.loadUrl(url);
				// Log.d("shouldOverrideUrlLoading", url);
				//dumpWebHistory(view, "shouldOverrideUrlLoading");
				return true;
			}
            
			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon)
			{
				AttachmentViewer.this.setCurrentMode(MODE_PREVIEW_PROGRESS);
				enableImgBtnClick=false;
				super.onPageStarted(view, url, favicon);
				timeout = true;
				final Handler myHandler = new Handler();
				Runnable run = new Runnable() {
	                 public void run() {

	                     if(timeout) {
	                         // do what you want
	 						Resources res = AttachmentViewer.this.getResources();
	 						DialogUtils.showMsgBox(AttachmentViewer.this, res.getString(R.string.timeout), res.getString(R.string.timeout_title), 
	 								new Runnable() {
	 									@Override
	 									public void run()
	 									{
	 										AttachmentViewer.this.finish();
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
				if (url.equals("about:relogin"))
				{
					view.stopLoading();
					if (MyApp.getAgent(AccountManager.getAccount(attach.getAccountId())).interactiveLogin(false,true,true))
					{
						//dumpWebHistory(view, "onPageFinished before Loading " + url);
						view.loadUrl(makePreviewUrl(attach.getAttachIndx(), attach.getMailUid()+"", internalPath, 0));
						//dumpWebHistory(view, "onPageFinished after Loading " + url);
					}
					return;
				} 
				else
				{
					AttachmentViewer.this.setCurrentMode(MODE_PREVIEW);
					_currentUrl = url;
				}
				enableImgBtnClick=true;
			}
			@Override
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
			{
				view.loadUrl("about:blank");
				Log.e(Utils.LOGTAG, "onReceivedError, code="+errorCode+" DESC:"+description +" URL:"+failingUrl);
				Resources res = AttachmentViewer.this.getResources();
				DialogUtils.showMsgBox(AttachmentViewer.this, res.getString(R.string.timeout), res.getString(R.string.timeout_title), 
							new Runnable() {
								@Override
								public void run()
								{
									AttachmentViewer.this.finish();
								}
						});
			}
		});
		_previewViewer.addJavascriptInterface(new CmailScriptInterface(), "cmail");
		_previewViewer.getSettings().setBuiltInZoomControls(true);
		Intent intent = getIntent();
		attach = intent.getParcelableExtra(AttachmentViewer.ATTACHMENT_INFO);
		if(attach != null)
		{
			previewAttachment(attach.index, attach.fileName, null);
		}
		
		
		imgBtnLast=(ImageButton)findViewById(R.id.backImgBtn);
		imgBtnJump=(ImageButton)findViewById(R.id.jumpImgBtn);
		imgBtnNext=(ImageButton)findViewById(R.id.fowardImgBtn);
		pageBtnFlag=(Button)findViewById(R.id.pageFlagBtn);
		imgBtnLast.setOnClickListener(imgBtnListener);
		imgBtnJump.setOnClickListener(imgBtnListener);
		imgBtnNext.setOnClickListener(imgBtnListener);
		
	}
	OnClickListener imgBtnListener=new OnClickListener() {
		
		@Override
		public void onClick(View v)
		{
			if(!enableImgBtnClick)
				return;
			switch (v.getId())
			{
			case R.id.backImgBtn:
				if(pageNumber==0)
				{
					Toast.makeText(AttachmentViewer.this, getResources().getString(R.string.already_first_page), Toast.LENGTH_SHORT).show();
					return;
				}
				pageNumber--;
				cmailScriptInterface.gotoPage(pageNumber);
				break;
			case R.id.jumpImgBtn:
				new AlertDialog.Builder(AttachmentViewer.this).setTitle(getResources().getString(R.string.choosePage))
					.setSingleChoiceItems(pageNums, pageNumber, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which)
					{
						pageNumber = which;
						dialog.dismiss();
						cmailScriptInterface.gotoPage(pageNumber);
					}
				}).show();
				break;
			case R.id.fowardImgBtn:
				if(pageNumber==totalPageCount-1)
				{
					Toast.makeText(AttachmentViewer.this, getResources().getString(R.string.already_last_page), Toast.LENGTH_SHORT).show();
					return;
				}
				pageNumber++;
				cmailScriptInterface.gotoPage(pageNumber);
				break;
			}
		}
	};
	@Override
	//Double click backkey to exit the preview
	 public boolean onKeyDown(int keyCode, KeyEvent event)
	 {
		if ((keyCode == KeyEvent.KEYCODE_BACK)
				&&_previewViewer.getVisibility() == View.VISIBLE
				&& _previewViewer.canGoBack()) 
		{ 
//			String url=getWebHistoryUrl(_previewViewer);
//			if(url.startsWith("local"))
//				{
//					int  pageNo = Integer.parseInt(url.substring(6));
//					if(pageNo>=0)
//					{
//						String attachBody = NewDbHelper.getInstance().getAttachBody(attach,pageNo);
//						if (attachBody != null && (!attachBody.equals("")) )
//						{
//							_previewViewer.loadDataWithBaseURL(	ServerAgent.getUrlBase() + "/",
//			                    attachBody, "text/html",
//								"utf-8", "local:"+pageNo);
//						}
//						if ((System.currentTimeMillis() - mExitTime) > 1000)
//				        {
//				        	Toast.makeText(this, R.string.exitpreview, Toast.LENGTH_SHORT).show();
//				        	mExitTime = System.currentTimeMillis();
//				        }
//				        else 
//				        {
//				        	finish();
//				        }
//					}
//					else if (pageNo<0)
//					{
//						finish();
//					}
//				}
//			else
			//双击返回可退出附件预览，单击无回应
			 if ((System.currentTimeMillis() - mExitTime) > 1000)
		        {
		        	Toast.makeText(this, R.string.exitpreview, Toast.LENGTH_SHORT).show();
		        	mExitTime = System.currentTimeMillis();
		        }
		        else 
		        {
		        	finish();
		        }
				
            return true; 
	    } 
		return super.onKeyDown(keyCode, event);
	 }


	interface OnFileExistDialogListner
	{
		public abstract void onDialogReturned(int selectedId, String newFileName);
	}



	void setCurrentMode(int mode)
	{
		_currentMode = mode;
		switch (_currentMode)
		{
		case MODE_ATTACHLIST:
		case MODE_ATTACHLIST_WRITE:
		{
			_previewViewer.setVisibility(View.GONE);
			_progressBarLayout.setVisibility(View.VISIBLE);
		}
			break;
		case MODE_PREVIEW_PROGRESS:
			_previewViewer.setVisibility(View.VISIBLE);
			_progressBar.setVisibility(View.VISIBLE);
			break;
		case MODE_PREVIEW:
//			_attachListLayout.setVisibility(View.VISIBLE);
			_progressBarLayout.setVisibility(View.VISIBLE);
			_previewViewer.setVisibility(View.VISIBLE);
			_progressBar.setVisibility(View.GONE);
			break;
			
		default:
			break;
		}
	}
	
	private void previewAttachment(int attachmentIndex,String attachmentName,String internalPath)
	{
		/*
		 * _attachListLayout.setVisibility(View.GONE);
		 * _previewViewer.setVisibility(View.VISIBLE); _currentMode =
		 * MODE_PREVIEW_PROGRESS;
		 */
		String attachBody = null;
		setCurrentMode(MODE_PREVIEW_PROGRESS);
		
		_previewViewer.clearView();
		
		//String attachBody = attach.getBody();
		if(internalPath == null)
		{
			attachBody = NewDbHelper.getInstance().getAttachBody(attach,pageNumber);
		}
		if (attachBody == null || attachBody.equals(""))
		{
			String attachUrl = makePreviewUrl(attachmentIndex, attach.getMailUid()+"",internalPath, 0);
			if(attachUrl == null)
			{
				finish();
				return;
			}
			String upperFileName = attachmentName.toUpperCase();
			boolean isPPT = upperFileName.endsWith(".PPT") || upperFileName.endsWith(".PPTX");
			setBrowserOverviewMode(_previewViewer, isPPT);
			//_previewViewer.getSettings().setLoadWithOverviewMode(isPPT);
			_previewViewer.getSettings().setUseWideViewPort(true);
			_previewViewer.loadUrl(attachUrl);
			dumpWebHistory(_previewViewer, "previewAttachment if() after Loading " + attachUrl);
		}
		else
		{
			_previewViewer.getSettings().setUseWideViewPort(true);
			_previewViewer.loadDataWithBaseURL(	ServerAgent.getUrlBase() + "/",
					                        attachBody, "text/html",
											"utf-8","local:0");
			dumpWebHistory(_previewViewer, "previewAttachment(415) after Loading " + "local:0");
//			_previewViewer.getSettings().setDefaultTextEncodingName("utf-8");
//			_previewViewer.loadData(attachBody, "text/html", "utf-8");
		}
		
		
	}

//	private String makePreviewUrl(int attachmentIndex,String mailUid)
//	{
//		return makePreviewUrl(attachmentIndex, mailUid,null);
//	}
	
	private String makePreviewUrl(int attachmentIndex,
			String mailUid,String internalPath, int pageNo)
	{
		String sid = MyApp.getAgent(AccountManager.getAccount(attach.getAccountId())).getSessionId(true,true,true);
		if(sid == null)
			return null;
		String attachUrl = ServerAgent.getUrlBase()
							+ "/ViewPart?uid=" + java.net.URLEncoder.encode(mailUid)
							+ "&folderName=" + java.net.URLEncoder.encode(attach.getMailInfo().getFolder())
							+ "&index=" + attachmentIndex
							+ "&pageNo=" +pageNo
							+ "&lang="+Locale.getDefault().getLanguage();
		if(internalPath!=null && !internalPath.equals(""))
			attachUrl += "&internalPath="+java.net.URLEncoder.encode(internalPath);
		
		attachUrl += "&sid="+
						java.net.URLEncoder.encode(sid);
		return attachUrl;
	}
	
	@Override
	protected void onNewIntent(Intent intent)
	{
		setIntent(intent);
	}
	@Override
	protected void onResume() 
	{
		super.onResume();
		totalPageCount=NewDbHelper.getInstance().getAttachmentPageCount(attach);
		if(totalPageCount!=0)
		{
			pageNums=new String[totalPageCount];
			for(int i=0;i<totalPageCount;i++)
			{
				pageNums[i]=String.valueOf(i+1);
			}
		}
	};
}

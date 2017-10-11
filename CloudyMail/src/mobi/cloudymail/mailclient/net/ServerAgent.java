package mobi.cloudymail.mailclient.net;

import static mobi.cloudymail.util.Utils.LOGTAG;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import mobi.cloudymail.mailclient.PasswordDialog;
import mobi.cloudymail.mailclient.R;
import mobi.cloudymail.util.BreakbleMessageLoop;
import mobi.cloudymail.util.DialogResult;
import mobi.cloudymail.util.MessageBox;
import mobi.cloudymail.util.MyApp;
import mobi.cloudymail.util.NewDbHelper;
import mobi.cloudymail.util.Utils;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRoute;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.CookieSpecFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.impl.cookie.BrowserCompatSpec;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/*
 * 
 *CookieSpecFactory csf = new CookieSpecFactory() {
 public CookieSpec newInstance(HttpParams params) {
 return new BrowserCompatSpec() {   
 @Override
 public void validate(Cookie cookie, CookieOrigin origin)
 throws MalformedCookieException {
 // Oh, I am easy
 }
 };
 }
 };

 DefaultHttpClient httpclient = new DefaultHttpClient();
 httpclient.getCookieSpecs().register("easy", csf);
 httpclient.getParams().setParameter(
 ClientPNames.COOKIE_POLICY, "easy");
 */
public class ServerAgent
{
	public static final String HEADER_MAIL_CLIENT = "MailClient"; // this header
	public static final String HEADER_OS_VERSION = "OSVer"; //this header is Android system version

	public static final String AGENT_ID = "CloudMail1.4.3";
	public static final String OS_VERSION = android.os.Build.VERSION.RELEASE;
	

//	public static final String urlBase = "http://"+_serverAddText+":8088/MailProxy";
//	public static final String loginUrlBase = "http://"+SettingPage._serverAddText+":8088/MailProxy";
//	public static final String urlBase = "http://cloudymail.mobi:8088/MailProxy";
//	public static final String loginUrlBase = "http://cloudymail.mobi:8088/MailProxy";//"https://cloudymail.mobi:8443/MailProxy";
	// public static final String urlBase = SettingPage._serverAddText;
	// public static final String loginUrlBase=SettingPage._serverAddText;
	private LoginSemaphore loginSemaphore = new LoginSemaphore();
	public Account account = null;
	private int loginStatus = LOGIN_FAIL;

	private String cookie = null;
	//private String sessionId = null;
	// private int accountIdOfCurrentSession = -1;

	private static Timer timer = new Timer();

	private int loginFailCode = 0;
	private boolean receiving;
	
	private final static int LOGIN_OK = 1;
	private final static int LOGIN_FAIL = 2;
	private final static int LOGIN_CANCELED = 3;
	private final static int LOGIN_NETWORK_FAIL = 4;
	private static DefaultHttpClient sClient = null;
	private HttpPost loginRequest;
	private static interface LoginCallback
	{
		void loginFinished(boolean successed);
	}
	private static class LoginSemaphore{
		public Vector<LoginCallback> loginListener = new Vector<LoginCallback>();
		private Semaphore sema = null;
		private boolean loginGoing = false;
		public synchronized void addListener(LoginCallback callback)
		{
			loginListener.add(callback);
		}
//		public void acquire() throws InterruptedException
//		{
//			sema.acquire();
//		}
		public synchronized void release(boolean loginOK)
		{
			sema.release(Integer.MAX_VALUE);
			loginGoing = false;
			for(LoginCallback cbk:loginListener)
			{
				try
				{
					cbk.loginFinished(loginOK);
				}
				catch(Throwable t)
				{
					t.printStackTrace();
				}
			}
			loginListener.clear();
		}
	}
	static
	{


	}

	public static void onConnectionChanged()
	{
		sClient = null;
	}
	public static DefaultHttpClient getHttpClient()
	{
		if(sClient != null)
			return sClient;
		// Set basic data
		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, "UTF-8");
		HttpProtocolParams.setUseExpectContinue(params, true);
		HttpProtocolParams.setUserAgent(params, AGENT_ID);

		// Make pool
		ConnPerRoute connPerRoute = new ConnPerRouteBean(12);
		ConnManagerParams.setMaxConnectionsPerRoute(params, connPerRoute);
		ConnManagerParams.setMaxTotalConnections(params, 20);
		
		// Set timeout
		HttpConnectionParams.setStaleCheckingEnabled(params, false);
		HttpConnectionParams.setConnectionTimeout(params, 60 * 1000);
		HttpConnectionParams.setSoTimeout(params, 60 * 1000);
		HttpConnectionParams.setSocketBufferSize(params, 8192);

		// Some client params
		HttpClientParams.setRedirecting(params, true); //tomcat may returns 302, need redirection

		// Register http/s shemas!
		SchemeRegistry schReg = new SchemeRegistry();
		schReg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		try
		{
			schReg.register(new Scheme("https", TrustAllSSLSocketFactory.getDefault(), 443));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		ClientConnectionManager conMgr = new ThreadSafeClientConnManager(params, schReg);
		sClient = new DefaultHttpClient(conMgr, params);

		// method 1, for cookie enable
		// CookieHandler h = CookieHandler.getDefault();
		// sClient.getParams().setParameter(
		// ClientPNames.COOKIE_POLICY, CookiePolicy.RFC_2965);
		// sClient.getCookieSpecs().register("def", new RFC2965SpecFactory());

		// method 2
		CookieSpecFactory csf = new CookieSpecFactory() {
			@Override
			public CookieSpec newInstance(HttpParams params)
			{
				return new BrowserCompatSpec() {
					@Override
					public boolean match(Cookie cookie, org.apache.http.cookie.CookieOrigin origin)
					{
						boolean b = super.match(cookie, origin);
						b = cookie.getDomain().equals(origin.getHost());
						return b;
					}
				};
			}
		};

		sClient.getCookieSpecs().register("easy", csf);
		sClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, "easy");
		return sClient;
	}

	public static String getUrlBase()
	{
		return "http://" + MyApp.userSetting._serverAddText + ":8088/MailProxy2";
	}

	public static String getLoginUrlBase()
	{
		return "https://" + MyApp.userSetting._serverAddText + ":8443/MailProxy2";
	}


	public static Result doHttpPost(String url,String accountName,String password,Context context, String progressBarTitle)
	{
		final HttpPost httpPost=new HttpPost(url);
		ArrayList<NameValuePair> params =new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("account",accountName));
		params.add(new BasicNameValuePair("password",password));
		try
		{
			httpPost.setEntity((HttpEntity) new  UrlEncodedFormEntity(params, HTTP.UTF_8));
		}
		catch (UnsupportedEncodingException e)
		{
			Utils.logException(e);
		}  
		final BreakbleMessageLoop myloop = new BreakbleMessageLoop();
		final Result[] result = new Result[] { null };
		Thread newThread = new Thread() {

			@Override
			public void run()
			{

				try
				{

					HttpResponse rsp = getHttpClient().execute(httpPost);
					result[0] = new Result(rsp.getEntity().getContent()); //so we will skip set session ID on error

				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}

				myloop.breakLoop();
			}
		};
		ProgressDialog prgDialog = new ProgressDialog(context);
		prgDialog.setMessage(progressBarTitle);
		prgDialog.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface arg0)
			{
				httpPost.abort();
			}
		});
		prgDialog.show();

		newThread.start();

		myloop.loop();
		prgDialog.dismiss();
		return result[0];
	
		
	}
	
	/**
	 * A general HTTP post method.
	 * This method run in block mode, i.e. function not return to caller untill network post complete.
	 * Server may reutnr result as 1) standard XML, which can be represented in a Result object
	 * 2) General stream
	 * 
	 * For caller, should process result differently for this two situation. for example
	 *       Result r = MyApp.getAgent().doHttpPost( .... );
	 *       if(r.isSuccessed())
	 *       {
	 *       	if(r.isXMLConent())
	 *      	 {
	 *        	  //deal with XML
	 *     		  }
	 *      	 else
	 *       	{
	 *       		is = r.getInputStream();
	 *          	//deal with input stream
	 *       	}
	 *       }
	 * @param url
	 * @param parameters
	 * @param progressBarTitle
	 * @param showProgressBar
	 * @param askPassword
	 * @param promptOnFail
	 * @return
	 */
	
	public Result doHttpPost(String url, HashMap<String, String> parameters, String progressBarTitle, final boolean showProgressBar, final boolean askPassword, final boolean promptOnFail)
	{
		return doHttpRequest(url, true, parameters, progressBarTitle, showProgressBar, askPassword,  promptOnFail);
	}
	private Result doHttpRequest(String url, boolean doPost, HashMap<String, String> parameters, String progressBarTitle, final boolean showProgressBar, final boolean askPassword, final boolean promptOnFail )
	{
		Result r= new Result();
		if(getSessionId(showProgressBar, askPassword, promptOnFail) == null)
		{
			r.status = Result.AUTH_FAIL;
			return r;
		}
		HttpRequestBase httpReq=null;
		if(doPost)
		{
			httpReq = new HttpPost(url);
			ArrayList<NameValuePair> params =new ArrayList<NameValuePair>();
			for( Map.Entry<String,String> e : parameters.entrySet())
			{
				params.add(new BasicNameValuePair(e.getKey(),e.getValue()));
			}
			
			try
			{
				((HttpPost)httpReq).setEntity((HttpEntity) new  UrlEncodedFormEntity(params, HTTP.UTF_8));
			}
			catch (UnsupportedEncodingException e)
			{
				Utils.logException(e);
			}  
		}
		else
		{
			httpReq = new HttpGet(url);

		}
		final BreakbleMessageLoop myloop = new BreakbleMessageLoop();
		final Result[] result = new Result[] { null };
		final HttpRequestBase httpPost = httpReq;
		Thread newThread = new Thread() {

			@Override
			public void run()
			{

				try
				{

					HttpResponse rsp = getHttpClient().execute(httpPost);
					//Though we have call getSessionId previously, that session may be a cached and has been invalid.
					//if this true, we will get NEED_LOGIN error
					if(rsp.getStatusLine().getStatusCode()== HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION)
					{
						//clear cached session first
						
						//re login
						if(MyApp.getAgent().getSessionId(showProgressBar, askPassword, promptOnFail) == null) //try do login again
						{
							return;
						}
						rsp = getHttpClient().execute(httpPost);
					}
					
					Header[] headers = rsp.getHeaders("Content-Type");
					String contentType = null;
					if(headers != null && headers.length == 0  )
						contentType = headers[0].getValue();
					if(contentType != null && contentType.startsWith("text/xml"))
					{
						result[0] = new Result(rsp.getEntity().getContent());
					}
					else
					{
						result[0] = new Result(rsp.getEntity().getContent(), true);
						
					}
					result[0].setContentType(contentType);
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
					result[0] = new Result(); 
					result[0].status=Result.FAIL;
				}

				myloop.breakLoop();
			}
		};
		ProgressDialog prgDialog = new ProgressDialog(MyApp.getCurrentActivity());
		prgDialog.setMessage(progressBarTitle);
		prgDialog.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface arg0)
			{
				httpPost.abort();
			}
		});
		prgDialog.show();

		newThread.start();

		myloop.loop();
		prgDialog.dismiss();
		return result[0];
	
		
	}
	

	public static HttpResponse execute(HttpGet get) throws IOException
	{
		get.setHeader(HEADER_MAIL_CLIENT, AGENT_ID);
		get.setHeader(HEADER_OS_VERSION, OS_VERSION);
		return getHttpClient().execute(get);
	}

	public static HttpResponse execute(HttpPost post) throws IOException
	{
		post.setHeader(HEADER_MAIL_CLIENT, AGENT_ID);
		post.setHeader(HEADER_OS_VERSION, OS_VERSION);
		return getHttpClient().execute(post);
	}

	public static synchronized CookieStore getCookieStore()
	{
		return getHttpClient().getCookieStore();
	}

	public ServerAgent(Account account)
	{
		this.account = account;
	}

	public static void dumpResponseHeads(HttpResponse rsp)
	{
		Header[] headers = rsp.getAllHeaders();
		for (int tmpI = 0; tmpI < headers.length; ++tmpI)
		{
			Header header = headers[tmpI];
			Log.d(LOGTAG, "login's reponse" + header.toString());
		}
	}


	/**
	 * do login. ask user password and show progress bar according input parameters
	 * this function can be called in either UI thread or background thread
	 * @param showProgressBar
	 * @param askPassword
	 * @param promptOnFail
	 * @return
	 */
	public boolean interactiveLogin(final boolean showProgressBar, final boolean askPassword, final boolean promptOnFail)
	{
		boolean promptFail = promptOnFail;
		synchronized(this)
		{
			
			if(!loginSemaphore.loginGoing)
			{//if no login process, start one
				loginSemaphore.sema = new Semaphore(-1); 
				loginSemaphore.loginListener.clear();
				loginSemaphore.loginGoing = true;
				Thread backThread = new Thread()
				{
					@Override
					public void run()
					{
						interactiveLoginInBackground(showProgressBar, askPassword, promptOnFail);;
					}
				};
				backThread.setName("Login");
				backThread.start();
				//promptFail = false; //to avoid prompt error twice when this is called from UI thread
			}
		}
		//wait login to complete. use two different waiting method for UI thread and background thread
		waitingLogin(showProgressBar, promptFail);
		return loginStatus == LOGIN_OK;
			
	}
	// ask user to input password if user has not inputted, or password wrong
	private boolean interactiveLoginInBackground(boolean showProgressBar, boolean askPassword, boolean promptOnFail)
	{
		
		Resources res = MyApp.instance().getResources();
		try
		{
tryLogin:
			for (int i = 0; i < 3; i++)
			{// try 3 times for user to input password and login
				getCurrentPassword(askPassword);
				if (!Utils.isEmpty(account.password))
				{
					// int returnStatus = LOGIN_OK;
					loginStatus = LOGIN_FAIL;
					loginRequest = new HttpPost(getLoginUrlBase() + "/Login?account="
																+ java.net.URLEncoder.encode(account.name));

					
					blockingLogin(loginRequest);
							// 登录可以以四种状态结束
							// 1. 成功登录 2. 密码错误 3. 网络错误 4. 用户取消
					switch (loginStatus)
					{
					case LOGIN_OK:
						return true;
					case LOGIN_CANCELED:
						return false;
					case LOGIN_NETWORK_FAIL:
						break tryLogin;
					default:
						if (loginFailCode == Result.AUTH_FAIL)
						{ // failed because of wrong password
							clearPassword();
						}
						else
							break tryLogin; // some other reason fail, don't
											// retry
					}
				}
				else
				{// user has cancled input password, break;
					return false;
				}
			}
		
			
		}
		catch (Exception ex)
		{
			if(promptOnFail)
			{
				MessageBox.show(MyApp.getCurrentActivity(), ex.getMessage(), res.getString(R.string.failLogin));
			}
			Log.d(Utils.LOGTAG, "FailLogin", ex);
		}
		finally
		{
			synchronized(this)
			{
				loginSemaphore.release(loginStatus == LOGIN_OK);
			}
		}
		return false;
	}
	public void cancelLogin()
	{
		loginRequest.abort();
		loginStatus = LOGIN_CANCELED ;
	}
	//suppose we are running in background thread
	private boolean waitingLogin(boolean showProgressBar, boolean promptOnFail)
	{
		
		
		
		final ProgressDialog[] dlgRef = new ProgressDialog[1];
		final AtomicBoolean userCancel = new AtomicBoolean(false);
		if(showProgressBar)
		{
			
			MyApp.getCurrentActivity().runOnUiThread(new Runnable() {
				
				@Override
				public void run()
				{
					ProgressDialog prgDialog = dlgRef[0] = new ProgressDialog(MyApp.getCurrentActivity());
					Resources res = MyApp.instance().getResources();
					prgDialog.setMessage(res.getString(R.string.logining));
					prgDialog.setOnCancelListener(new OnCancelListener() {

						@Override
						public void onCancel(DialogInterface arg0)
						{
							userCancel.set(true);
							cancelLogin();
						}
					});
					prgDialog.show();
					
				}
			});
			

		}
		
		if(Utils.inUiThread())
		{
			
			final BreakbleMessageLoop myloop = new BreakbleMessageLoop();
			if(!loginSemaphore.sema.tryAcquire())
			{
				loginSemaphore.addListener(new LoginCallback() {
					
					@Override
					public void loginFinished(boolean successed)
					{
						myloop.breakLoop();
						
					}
				});
				myloop.loop();
			}
		}
		else
			try
			{
				loginSemaphore.sema.acquire();
			}
			catch (InterruptedException e)
			{
				return false;
			}
		
		
		if(showProgressBar)
		{
			Utils.runOnUiThreadAndBlock(new Runnable() {
				
				@Override
				public void run()
				{
					dlgRef[0].dismiss();
				}
			});
			
		}
		
		if(userCancel.get())
			return false;
		if(promptOnFail && loginStatus != LOGIN_OK )
		{
			Utils.runOnUiThreadAndBlock(new Runnable() {
				
				@Override
				public void run()
				{
					MyApp res = MyApp.instance();
					if (loginFailCode == Result.AUTH_FAIL)
					{
						MessageBox.show(MyApp.getCurrentActivity(), res.getString(R.string.fl_checkSetting),
										res.getString(R.string.failLogin));
					}
					else
						MessageBox.show(MyApp.getCurrentActivity(), 
								res.getString(R.string.fl_cannotConnectServer), 
								res.getString(R.string.failLogin));

				}
			});
		}
		return loginStatus == LOGIN_OK;
	}

	private void clearPassword()
	{
		account.password = null;
		Log.d(LOGTAG, "Clear password" + "account name = " + account.name);
		// DbHelper.getInstance().updateAccountPassword("", account.id);
		NewDbHelper.getInstance().updateAccountPassword("", account.id);
	}

	public String getCurrentPassword(boolean askInputOnNeed)
	{
		if (account != null)
		{
			if (Utils.isEmpty(account.password) && askInputOnNeed)
			{
				Utils.runOnUiThreadAndBlock(new Runnable() {
					
					@Override
					public void run()
					{
						PasswordDialog dlg = new PasswordDialog(MyApp.getCurrentActivity(), account.name,
																loginFailCode == Result.AUTH_FAIL);
						if (dlg.showDialog() == DialogResult.OK)
						{
							account.password = dlg.getPassword();
							if ("".equals(account.password))
								account.password = null;
							if (dlg.needSavePassword())
								NewDbHelper.getInstance().updateAccountPassword(account.password,
																				account.id);
						}
						else
							account.password = null;
					}
				});
				

			}
			return account.password;
		}
		return null;
	}



	public static boolean hasNetworkConnection()
	{
		ConnectivityManager cm = (ConnectivityManager) MyApp.instance()
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo[] netInfo = cm.getAllNetworkInfo();
		for (NetworkInfo ni : netInfo)
		{
			if (ni.isConnected())
				return true;
		}
		return false;
	}

	public String getSessionId(boolean showProgressBar, boolean askPassword, boolean promptOnFail)
	{
		// first check whether phone connects with network.
		// ConnectivityManager cwjManager = (ConnectivityManager) ctx
		// .getSystemService(Context.CONNECTIVITY_SERVICE);
		// NetworkInfo nwInfo = cwjManager.getActiveNetworkInfo();
		// if (nwInfo == null || !nwInfo.isAvailable())
		if (!hasNetworkConnection())
		{
			if( promptOnFail)
			{
				Utils.runOnUiThreadAndBlock(new Runnable() {
					
					@Override
					public void run()
					{
						Resources res = MyApp.getCurrentActivity().getResources();
						MessageBox.show(MyApp.getCurrentActivity(), res.getString(R.string.err_notConnected),
										res.getString(R.string.error));
	
						
					}
				});
			}
			return null;
		}

		// since one account has its own ServerAgent object, there's no need to
		// store the
		// accountIdOfCurrentSession
		String sid = NewDbHelper.getInstance().getSessionId(this.account);
		if (Utils.isEmpty(sid) )
		{
			if (interactiveLogin( showProgressBar,  askPassword,  promptOnFail))
			{
				sid = NewDbHelper.getInstance().getSessionId(this.account);
			}

		}
		return sid;
	}

	private void blockingLogin(final HttpPost loginRequest)
	{
		Log.d(LOGTAG, "Login" + "Start Login");
		loginFailCode = 0;
		String password = account.password;
		if (password == null)
		{
			Utils.ASSERT(false);
			loginStatus = LOGIN_CANCELED;
		}
		HttpResponse rsp = null;

		// MailClient.mainWindow.showStatus("建立网络连接...");
		// MessageBox.Show("正在登录。\n如需要中止，请点击“确定”。", "请稍候",
		// MessageBoxButtons.OK, MessageBoxIcon.None,
		// MessageBoxDefaultButton.Button1);

		try
		{
			getHttpClient().getCookieStore().clear();
			Serializer serializer = new Persister();
			ByteArrayOutputStream out = new ByteArrayOutputStream(512);
			serializer.write(account, out);
			loginRequest.setEntity(new ByteArrayEntity(out.toByteArray()));
			loginRequest.setHeader("Cache-Control", "no-cache");
			Log.d(LOGTAG, "Login" + "Start HTTP execute");
			loginRequest.addHeader("lang", Locale.getDefault().getLanguage());
			rsp = execute(loginRequest);
			Log.d(LOGTAG, "Login" + "End HTTP execute");
			// dumpResponseHeads(rsp);


			Result rst = new Result(rsp.getEntity().getContent()); //so we will skip set session ID on error
			

			if (!rst.isSuccessed())
			{
				loginFailCode = rst.status;
				loginStatus = LOGIN_FAIL;
				NewDbHelper.getInstance().saveSessionId(account, null);
				cookie = null;
			}
			else
			{
				loginStatus = LOGIN_OK;
				if (rst.xmlReader != null)
				{
					NewDbHelper.getInstance().insertFoldersToDb(rst.xmlReader, account.id);
				}
				if (rsp.getFirstHeader("Set-Cookie") != null)
				{
					cookie = rsp.getFirstHeader("Set-Cookie").getValue();
					int semiPos = cookie.indexOf(';');
					if (semiPos > 0)
						cookie = cookie.substring(0, semiPos);
					int equPos = cookie.indexOf('=');
					String sid = cookie.substring(equPos + 1);
					NewDbHelper.getInstance().saveSessionId(account, sid);
					// accountIdOfCurrentSession =
					// MailClient.currentAccount.id;
				}

			}

		}
		catch (java.net.UnknownHostException ex1)
		{
			Log.d(Utils.LOGTAG, "LOGIN_FAIL", ex1);
			loginStatus = LOGIN_NETWORK_FAIL;// LOGIN_FAIL;
		}
		catch (java.net.SocketTimeoutException ex2)
		{
			Log.d(Utils.LOGTAG, "LOGIN_NETWORK_FAIL", ex2);
			loginStatus = LOGIN_NETWORK_FAIL;// LOGIN_FAIL;
		}
		catch (Exception ex)
		{
			if (loginStatus != LOGIN_CANCELED)
			{
				Log.d(Utils.LOGTAG, "LOGIN_FAIL", ex);
				loginStatus = LOGIN_FAIL;
			}
		}
		// 登录可以以四种状态结束
		// 1. 成功登录 2. 密码错误 3. 网络错误 4. 用户取消
	}

	public boolean isReceiving()
	{
		
		return receiving;
	}
	public void setReceiving(boolean receiving)
	{
		this.receiving = receiving;
	}
	public void clearSessionId()
	{
		NewDbHelper.getInstance().saveSessionId(account, null);
	}
	public void setAccount(Account a) {
		account = a;
	}
}

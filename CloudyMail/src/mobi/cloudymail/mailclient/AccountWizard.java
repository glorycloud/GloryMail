package mobi.cloudymail.mailclient;

import static mobi.cloudymail.util.Utils.LOGTAG;

import java.util.EnumSet;

import mobi.cloudymail.mailclient.net.Account;
import mobi.cloudymail.mailclient.net.Result;
import mobi.cloudymail.mailclient.net.ServerAgent;
import mobi.cloudymail.util.DialogResult;
import mobi.cloudymail.util.DialogUtils;
import mobi.cloudymail.util.DialogUtils.ButtonFlags;
import mobi.cloudymail.util.MessageBox;
import mobi.cloudymail.util.MyApp;
import mobi.cloudymail.util.NewDbHelper;
import mobi.cloudymail.util.Utils;

import org.apache.commons.lang.ArrayUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;
import android.widget.Toast;
public class AccountWizard extends BaseActivity implements OnClickListener, OnTabChangeListener 
{
	public static final int ACTRST_CHECKNEWMAIL = RESULT_FIRST_USER;
	public static final int ACTRST_NORMAL = RESULT_OK;
	
	//awk   '{print "{\"" $1 "\",\t\"" $2  "\",\t\"" $3  "\",\t\"" $4  "\",\t\"" $5 "\",\t\"" $6  "\",\t\"" $7   "\",\t\"" $8 "\"}," }'  /d/work/workspace/MailProxy/knownServer.csv
	static final String[][] knownHostsString = new String[][]
	     //name,	loginName,	mailServer,	smtpServer,	serverType,	smtpPort,useSSL,	mailPort
	    { 
		//{"domainName",  "loginName",    "mailServer",   "smtpServer",   "serverType",   "smtpPort",     "useSSL",       "mailPort" ,"promptForPOPIMAP"},
		{"126.com",     "$USER",        "imap.126.com", "smtp.126.com", "imap", "465",   "true",        "993" ,"false"},
		{"139.com",     "$USER",        "pop.139.com",  "smtp.139.com", "pop3", "25",   "false",        "110" ,"false"},
		//{"139.com",     "$USER",        "imap.139.com",  "smtp.139.com", "imap", "25",   "false",        "143"},
		{"163.com",     "$USER",        "imap.163.com",  "smtp.163.com", "imap", "465",   "true",        "993" ,"false"},
		{"yeah.net",     "$USER",        "imap.yeah.net",  "smtp.yeah.net", "imap", "465",   "true",        "993","false"},
		{"188.com",     "$USER",        "pop.188.com",  "smtp.188.com", "pop3", "25",   "false",        "110","false"},
		{"21cn.com",    "$USER",        "pop.21cn.com", "smtp.21cn.com",        "pop3", "25",   "false",       "110","false"},
		{"21cn.net",    "$USER",        "pop.21cn.net", "smtp.21cn.net",        "pop3", "25",   "false",       "110","false"},
		{"263.net",     "$USER",        "263.net",      "smtp.263.net", "pop3", "25",   "false",        "110","false"},
		{"263.net.cn",  "$USER",        "263.net.cn",   "263.net.cn",   "pop3", "25",   "false",        "110","false"},
		{"263xmail.com", "$USER",        "pop.263xmail.com",     "smtp.263xmail.com",    "pop3", "25",  "false", "110","false"},
		{"agatelogic.com",      "$USER@agatelogic.com", "imap.gmail.com",       "smtp.gmail.com",       "imap","465",   "true", "993","false"},
		{"cloudymail.mobi",      "$USER@cloudymail.mobi", "imap.gmail.com",       "smtp.gmail.com",       "imap","465",   "true", "993","false"},
		{"china.com",   "$USER@china.com",      "pop.china.com",        "smtp.china.com",       "pop3", "25",  "false", "110","false"},
		{"ee.buaa.edu.cn",      "$USER@ee.buaa.edu.cn", "pop3.buaa.edu.cn",     "smtp.buaa.edu.cn",     "pop3","25",    "false",        "110","false"},
		{"eyou.com",    "$USER@eyou.com",       "pop3.eyou.com",        "mx.eyou.com",  "pop3", "25",   "false","110","false"},
		{"foxmail.com", "$USER@foxmail.com",    "imap.qq.com",   "smtp.qq.com",  "imap", "465",   "true",       "993","true"},
		{"gmail.com",   "$USER@gmail.com",      "imap.gmail.com",       "smtp.gmail.com",       "imap", "465", "true",  "993","false"},
		{"hotmail.com", "$USER@hotmail.com",    "pop3.live.com",        "smtp.live.com",        "pop3", "587", "true",  "995","false"},
		{"msn.com", "$USER@msn.com",    "pop3.live.com",        "smtp.live.com",        "pop3", "587", "true",  "995","false"},
		{"netease.com", "$USER",        "pop.netease.com",      "smtp.netease.com",     "pop3", "25",   "false","110","false"},


		{"qq.com",      "$USER",        "ex.qq.com",   "ex.qq.com",  "exchange", "443",   "true",        "443","true"}, //IMAP with SSL, works well
		{"vip.qq.com",  "$USER@vip.qq.com",        "imap.qq.com",   "smtp.qq.com",  "imap", "465",   "true",        "993","true"}, //IMAP with SSL, works well
		{"sina.cn",     "$USER",        "pop.sina.com", "smtp.sina.com",        "pop3", "25",   "false",       "110","true"},
		{"sina.com",    "$USER",        "pop.sina.com", "smtp.sina.com",        "pop3", "25",   "false",       "110","true"},
		{"sohu.com",    "$USER",        "pop3.sohu.com",        "smtp.sohu.com",        "pop3", "25",   "false","110","false"},
		{"tom.com",     "$USER",        "pop.tom.com",  		"smtp.tom.com", "pop3", "25",   "false",        "110","false"},
		{"vip.163.com", "$USER",        "imap.vip.163.com",		"smtp.vip.163.com",     "imap", "465",   "true","993","false"},
		{"vip.126.com", "$USER",        "imap.vip.126.com", 	"smtp.vip.126.com",     "imap", "465",   "true","993","false"},
		{"188.com", 	"$USER",        "imap.188.com",     	"smtp.188.com",     "imap", "465",   "true","993","false"},
    	{"vip.sina.cn", "$USER",        "pop3.vip.sina.com",    "smtp.vip.sina.com",    "pop3", "25",   "false","110","true"},
		{"vip.sohu.com",        "$USER",        "pop3.vip.sohu.com",    "smtp.vip.sohu.com",    "pop3", "25",  "false", "110","false"},
		{"x263.net",    "$USER",        "pop.x263.net", "smtp.x263.net",        "pop3", "25",   "false",       "110","false"},
		{"yahoo.cn",    "$USER@yahoo.cn",       "imap.mail.yahoo.cn",    "smtp.mail.yahoo.cn",   "imap", "465", "true",  "993","true"},
		{"yahoo.com",   "$USER@yahoo.com",      "imap.mail.yahoo.com",  "smtp.mail.yahoo.com",  "imap", "465", "true",  "993","true"},
		{"yahoo.com.cn",        "$USER@yahoo.com.cn",   "imap.mail.yahoo.com.cn",        "smtp.mail.yahoo.cn",  "imap",  "465",  "true", "993","true"},
		{"yeah.net",    "$USER",        "pop.yeah.net", "smtp.yeah.net",        "pop3", "25",   "false",       "110","false"},
		{"cloudymail.mobi",      "$USER@cloudymail.mobi", "imap.gmail.com",       "smtp.gmail.com",       "imap","465",   "true", "993","false"},
		{"wo.com.cn",     "$USER",        "pop.wo.com.cn",      "smtp.wo.com.cn", "pop3", "25",   "false",        "110","false"},
		{"189.cn",     "$USER",        "pop.189.cn",  "smtp.189.cn", "pop3", "25",   "false",        "110","false"},
		{"hichina.com",    "$USER@hichina.com",        "pop3.hichina.com",        "smtp.hichina.com",        "pop3", "25",   "false","110","false"},

	     };
	static final Account[] knownHosts =new Account[knownHostsString.length];
	
	static {
		for(int i=0; i<knownHostsString.length; i++)
		{
			Account a = new Account();
			a.name = knownHostsString[i][0];
			a.loginName = knownHostsString[i][1];
			a.mailServer = knownHostsString[i][2];
			a.smtpServer = knownHostsString[i][3];
			a.serverType = knownHostsString[i][4];
			a.smtpPort = Integer.parseInt(knownHostsString[i][5]);
			a.useSSL = Boolean.parseBoolean(knownHostsString[i][6]);
			a.setMailPort(Integer.parseInt(knownHostsString[i][7]));
		}
	}
	
	private Account account = null;
	private boolean isNew = true;
	private Spinner serverTypeCmb = null;
	//private EditText accountNameTxt = null;
	private EditText mailboxTxt = null;
	private EditText passwordTxt = null;
	private EditText loginNameTxt = null;
	private EditText mailPortTxt = null;
	private TextView mailPortLabel = null;
	private EditText smtpPortTxt = null;
	private EditText mailServerTxt = null;
	private TextView mailServerLabel = null;
	private EditText smtpServerTxt = null;
	private CheckBox useSSLChk = null;
	private Button finishBtn = null;
	private String serverType="";
	private LinearLayout advanceLayout=null;
	private LinearLayout settingLayout=null;
    
	@Override
	protected void onResume ()
	{
		super.onResume();
		MyApp.setCurrentActivity(this);
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Window win = getWindow();
		win.requestFeature(Window.FEATURE_LEFT_ICON);
		setContentView(R.layout.account_wizard);
		win.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.cloudymail);
	
		findViewById(R.id.finishBtn).setOnClickListener(this);
		findViewById(R.id.cancelBtn).setOnClickListener(this);
		findViewById(R.id.advancedBtn).setOnClickListener(this);
		findViewById(R.id.lastBtn).setOnClickListener(this);
		findViewById(R.id.nextBtn).setOnClickListener(this);
		
		Intent intent = getIntent();
		account = (Account) intent.getSerializableExtra("account");
		isNew = intent.getBooleanExtra("isNew", true);
		serverTypeCmb = (Spinner) findViewById(R.id.serverTypeCmb);
		//accountNameTxt = (EditText) findViewById(R.id.accountNameTxt);
		passwordTxt = (EditText) findViewById(R.id.passwordTxt);
		        
		mailboxTxt = (EditText) findViewById(R.id.mailboxTxt);
		loginNameTxt = (EditText) findViewById(R.id.loginNameTxt);
		mailPortTxt = (EditText) findViewById(R.id.mailPortTxt);
		smtpPortTxt = (EditText) findViewById(R.id.smtpPortTxt);
		mailServerTxt = (EditText) findViewById(R.id.mailServerTxt);
		smtpServerTxt = (EditText) findViewById(R.id.smtpServerTxt);
		mailServerTxt.addTextChangedListener(new  TextWatcher() {
			
			

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,int after)
			{
				// TODO Auto-generated method stub
			}
			
			@Override
			public void afterTextChanged(Editable s)
			{
				// TODO Auto-generated method stub
				String str = s.toString();
				if(str.endsWith("."))
				{
					String server = str.substring(0,str.indexOf('.'));
					Log.d(LOGTAG, server);
					if("pop3".equalsIgnoreCase(server))
						serverTypeCmb.setSelection(0);
					else if("imap".equalsIgnoreCase(server))
						serverTypeCmb.setSelection(1);
					else if("exchange".equalsIgnoreCase(server))
						serverTypeCmb.setSelection(2);
				}
			}
		});
		useSSLChk = (CheckBox) findViewById(R.id.useSSLChk);
		finishBtn = (Button) findViewById(R.id.finishBtn);
		mailPortLabel = (TextView) findViewById(R.id.mailPortLbl);
		mailServerLabel = (TextView) findViewById(R.id.mailServerLbl);
	    
		serverTypeCmb.setOnItemSelectedListener(new ServerTypeSelectedListener());
		
		advanceLayout = (LinearLayout)findViewById(R.id.advancedSettingLayout);
		settingLayout = (LinearLayout)findViewById(R.id.accountSettingLayout);
		
		
		initWidgetFromAccount();

		findViewById(R.id.showPasswordChk).setOnClickListener(new android.view.View.OnClickListener() 
		{
			@Override
			public void onClick(View paramView)
			{
				if(((CheckBox)paramView).isChecked())
				{
					passwordTxt.setTransformationMethod(null);
				}
				else
					passwordTxt.setTransformationMethod(PasswordTransformationMethod.getInstance());
			}
		});
	    
	}
	
	private void initWidgetFromAccount()
	{
		if(account == null)
		{
			account=MyApp.currentAccount;
			return;
		}
	    serverTypeCmb.setSelection(ArrayUtils.indexOf(getResources().getStringArray(R.array.serverType), account.serverType));
	 
	    if(account.isValid())
	    {
	    	advanceLayout.setVisibility(View.VISIBLE);
			settingLayout.setVisibility(View.GONE);
	    }
	    else
	    {
	    	advanceLayout.setVisibility(View.GONE);
	    	settingLayout.setVisibility(View.VISIBLE);
	    	account.setDefault();
	    }
	    mailboxTxt.setText(account.name);
	    passwordTxt.setText(account.password);
	    loginNameTxt.setText(account.loginName);
	    mailPortTxt.setText(account.getMailPort()+"");
	    smtpPortTxt.setText(account.smtpPort+"");
	    mailServerTxt.setText(account.mailServer);
	    smtpServerTxt.setText(account.smtpServer);
	    useSSLChk.setChecked(account.useSSL);
	    serverType = account.serverType;   
	}
	
	private void setAccountFromWidget()
	{
		account.name = mailboxTxt.getText().toString();
		account.password = passwordTxt.getText().toString();
		account.loginName = loginNameTxt.getText().toString();
		account.setMailPort(Integer.valueOf(mailPortTxt.getText().toString()));
		account.serverType = serverType;
		account.smtpPort = Integer.valueOf(smtpPortTxt.getText().toString());
		account.smtpServer = smtpServerTxt.getText().toString();
		account.mailServer = mailServerTxt.getText().toString();
		account.useSSL = useSSLChk.isChecked();
	}
	
	private Account isKnownServer(String name,String password)
	{
		int atPos = name.indexOf('@');
		String host = name.substring(atPos + 1).toLowerCase();
		String loginName = name.substring(0,atPos);
		for(int i=0; i<knownHostsString.length; i++)
		{
			if (knownHostsString[i][0].equals(host))
			{
				Account a = new Account();
				a.name = name;
				a.loginName = knownHostsString[i][1];
				a.loginName = a.loginName.replace("$USER", loginName);
				a.mailServer = knownHostsString[i][2];
				a.smtpServer = knownHostsString[i][3];
				a.serverType = knownHostsString[i][4];
				a.smtpPort = Integer.parseInt(knownHostsString[i][5]);
				a.useSSL = Boolean.parseBoolean(knownHostsString[i][6]);
				a.setMailPort(Integer.parseInt(knownHostsString[i][7]));
				a.promptForPOPIMAP=Boolean.parseBoolean(knownHostsString[i][8]);
				return a;
			}
		}
		Result result = null;
		try
		{
			result = ServerAgent.doHttpPost( ServerAgent.getLoginUrlBase()
					                          + "/GetMailConfig",
					                         java.net.URLEncoder.encode(name),java.net.URLEncoder.encode(password), 
					                          this,getResources().getString(R.string.search_mail_config));
			if (result != null && result.isSuccessed())
			{
				Element e = null;
				Element contents = result.xmlReader;
				NodeList nl = contents.getElementsByTagName("account");
				
				e = (Element)nl.item(0);//返回content只有一个的情况
				Account a = new Account();
				a.name = name;
				a.loginName = e.getAttribute("loginName");
				a.loginName = a.loginName.replace("$USER", loginName);
				a.mailServer = e.getAttribute("mailServer");
				a.smtpServer = e.getAttribute("smtpServer");
				a.serverType = e.getAttribute("serverType");
				a.smtpPort = Integer.parseInt(e.getAttribute("smtpPort"));
				a.setMailPort(Integer.parseInt(e.getAttribute("mailPort")));
				a.useSSL = Boolean.parseBoolean(e.getAttribute("useSSL"));
				
				return a;	
			}
			else
			{
				Toast.makeText(this,getResources().getString(R.string.fail_to_get_mail_config),Toast.LENGTH_LONG).show();
			}
			return null;
		}
		catch (Exception ex)
		{			
			Log.e(LOGTAG, "Fail get config:", ex);
			return null;
		}
		
	}
	
	public void onClick(View v)
	{
		View pageView1 = findViewById(R.id.pageView1);
		View pageView2 = findViewById(R.id.pageView2);
		if(v.getId() == R.id.cancelBtn)
		{
			try
			{
				setResult(RESULT_CANCELED);
		        finish();
			}
			catch (Exception e)
			{
				this.finish();
			}
		}
		else if(v.getId() == R.id.nextBtn)  //next button is pressed
		{
			
			String addressStr = mailboxTxt.getText().toString();
			String password = passwordTxt.getText().toString();
			Resources res = this.getResources();
			if (isMailAddressValid(addressStr, this)) {
				if (password.equals("")) {
					DialogUtils.showMsgBox(this,res.getString(R.string.err_emptyPassword),res.getString(R.string.error));
					passwordTxt.requestFocus();
					return;
				}
				// if the account name changed, then should check whether the
				// host changed.
				int atPos = addressStr.indexOf('@');
				String host = addressStr.substring(atPos + 1).toLowerCase();
				if (!host.equals(account.getHostName()))
				{
					Account tmpAccount = isKnownServer(addressStr, password);
					if (tmpAccount != null)
					{
						account = tmpAccount;
						account.password = passwordTxt.getText().toString();
					} else
					{
						// update account
						account.clear();
						account.name = mailboxTxt.getText().toString();
						account.password = passwordTxt.getText().toString();
					}
					initWidgetFromAccount();
				} else if (!addressStr.equals(account.name))
				{
					String userName = addressStr.substring(0, atPos);

					if (!Utils.isEmpty(account.loginName))
                       {
						int atIndex = account.loginName.indexOf('@');
						if (atIndex < 0) {
							account.loginName = userName;
						} else {
							/* Replace @ the string before */
							account.loginName = userName
									+ account.loginName.substring(atIndex);
						}
						account.name = mailboxTxt.getText().toString();
						loginNameTxt.setText(account.loginName);
					}
				}
				
				pageView1.setVisibility(View.GONE);
				pageView2.setVisibility(View.VISIBLE);
			}
		}
			
			else if (v.getId() == R.id.advancedBtn) // 高级
			{
				advanceLayout.setVisibility(View.GONE);
				settingLayout.setVisibility(View.VISIBLE);
			}
			else if (v.getId() == R.id.lastBtn)
			  {
				pageView1.setVisibility(View.VISIBLE);
				pageView2.setVisibility(View.GONE);
			  }
			else if (v.getId() == R.id.finishBtn)
			   {
				if (isAccountSettingValid()) {
					if (serverType != null&& !serverType.equals(account.serverType))
					{
						EnumSet<ButtonFlags> buttons = EnumSet.noneOf(ButtonFlags.class);
						buttons.add(ButtonFlags.Yes);
						buttons.add(ButtonFlags.No);
						int checkFlag = DialogUtils.showModalMsgBox(this,getResources().getString(R.string.accountWizard_Warn),getResources().getString(R.string.warning),buttons);
						if (checkFlag == DialogResult.NO)
						{
							return;
						} else 
						{
							NewDbHelper.getInstance().deleteAccountInfo(account);
						}
					}

					setAccountFromWidget();

					if (account.isPromptForPOPIMAP())
					{
						MessageBox.show(this, getString(R.string.PromptForPOPIMAP), getString(R.string.PromptAccountSetting));
					}
					/**/
					getIntent().putExtra("account", account);
					if(isNew && DialogUtils.showModalMsgBox(this,
										                 	getResources().getString(R.string.whether_syncup_mail_immediately),
										                 	getResources().getString(R.string.whether_syncup_mail_immediately_title),
															EnumSet.of(	DialogUtils.ButtonFlags.Yes, DialogUtils.ButtonFlags.No)) == DialogResult.YES)
					{
						setResult(ACTRST_CHECKNEWMAIL, getIntent());
					} else
						setResult(ACTRST_NORMAL, getIntent());
					finish();
				}
			}
		}
	

	@Override
	public void onTabChanged(String tabId)
	{
		if(tabId.equals("page1"))
		{
			finishBtn.setText(R.string.next);
		}
		else if(tabId.equals("page2"))
		{
			//if(!isMailAddressValid(accountNameTxt.getText().toString(),this))
			if(!isMailAddressValid(mailboxTxt.getText().toString(),this))
			{
//				getTabHost().setCurrentTab(0);
				return;
			}
			finishBtn.setText(R.string.finish);
		}
	}
	
	static public boolean isMailAddressValid(String name, Context ctx)
	{
		Resources res = ctx.getResources();
		String errMsg="";
		if(name.equals(""))
			errMsg = res.getString(R.string.err_emptyAddress);
		else
		{
			int flagIdx = name.indexOf("@");
			if(flagIdx == -1)
				errMsg = res.getString(R.string.err_invalidAddress);
		}
		if(!errMsg.equals(""))
		{
			DialogUtils.showMsgBox(ctx,errMsg, res.getString(R.string.error));
			return false;
		}
		else
			return true;
	}
	
	private boolean isAccountSettingValid()
	{
		Resources res = getResources();
		String err = res.getString(R.string.error);
		String loginName = this.loginNameTxt.getText().toString();
		if(loginName.equals(""))
		{
			DialogUtils.showMsgBox(this, res.getString(R.string.err_invalidLoginName), err);
			return false;
		}
		String serverAddress = this.mailServerTxt.getText().toString();
		if(serverAddress.equals(""))
		{
			DialogUtils.showMsgBox(this,res.getString(R.string.err_invalidServerAddress),err);
			return false;
		}
		boolean succeedFlag = true;
		try
		{
			int serverPort = Integer.valueOf(this.mailPortTxt.getText().toString());
			succeedFlag = Account.isPortValid(serverPort);
		}catch(NumberFormatException except)
		{
			succeedFlag = false;
		}finally
		{
			if(!succeedFlag)
			{
				DialogUtils.showMsgBox(this, res.getString(R.string.err_invalidServerPort), err);
				return false;
			}
		}
		String smtpServer = this.smtpServerTxt.getText().toString();
		if(smtpServer.equals(""))
		{
			DialogUtils.showMsgBox(this, res.getString(R.string.err_invalidSmtpServer), err);
			return false;
		}
		try
		{
			int smtpPort = Integer.valueOf(this.smtpPortTxt.getText().toString());
			succeedFlag = Account.isPortValid(smtpPort);
		}catch(NumberFormatException except)
		{
			succeedFlag = false;
		}finally
		{
			if(!succeedFlag)
			{
				DialogUtils.showMsgBox(this, res.getString(R.string.err_invalidSmtpPort), err);
				return false;
			}
		}
		return true;
	}
	
	
	 class ServerTypeSelectedListener implements OnItemSelectedListener
	 {  
		 @Override
	        public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
	        {  
			 	Resources res = getResources();
	        	serverType = res.getStringArray(R.array.serverType)[position];
	        	if(serverType.equals(res.getString(R.string.exchange)))
	        	{
	    	    	mailServerLabel.setText(res.getString(R.string.exchangeServer));
	    	    	mailPortLabel.setText(res.getString(R.string.exchangePort));
	    	    	mailPortLabel.setVisibility(View.INVISIBLE);
	    	    	mailPortTxt.setVisibility(View.INVISIBLE);
	    	    	smtpServerTxt.setVisibility(View.INVISIBLE);
	    	    	smtpPortTxt.setVisibility(View.INVISIBLE);
	    	    	useSSLChk.setVisibility(View.INVISIBLE);
	    	    	findViewById(R.id.smtpPortLbl).setVisibility(View.INVISIBLE);
	    	    	findViewById(R.id.smtpServerLbl).setVisibility(View.INVISIBLE);
	        	}
    	    	else
    	    	{
		        	if(serverType.equals(res.getString(R.string.pop3)))
		    	    {
		    	    	mailServerLabel.setText(res.getString(R.string.pop3Server));
		    	    	mailPortLabel.setText(res.getString(R.string.pop3Port));
		    	    }
		    	    else if(serverType.equals(res.getString(R.string.imap)))
		    	    {
		    	    	mailServerLabel.setText(res.getString(R.string.imapServer));
		    	    	mailPortLabel.setText(res.getString(R.string.imapPort));
		    	    }
	    	    	mailPortLabel.setVisibility(View.VISIBLE);
	    	    	mailPortTxt.setVisibility(View.VISIBLE);
	    	    	smtpServerTxt.setVisibility(View.VISIBLE);
	    	    	smtpPortTxt.setVisibility(View.VISIBLE);
	    	    	useSSLChk.setVisibility(View.VISIBLE);
	    	    	findViewById(R.id.smtpPortLbl).setVisibility(View.VISIBLE);
	    	    	findViewById(R.id.smtpServerLbl).setVisibility(View.VISIBLE);
    	    	}
	    	   
	    	    	
	        	
	        } 
		 
		 @Override
		 public void onNothingSelected(AdapterView<?> parent)
		 {
			 
		 }
	 }
	
	 
}



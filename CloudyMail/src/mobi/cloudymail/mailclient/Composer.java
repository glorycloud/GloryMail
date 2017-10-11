package mobi.cloudymail.mailclient;

import static mobi.cloudymail.util.Utils.LOGTAG;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import mobi.cloudymail.data.MailInfo;
import mobi.cloudymail.data.OutMailInfo;
import mobi.cloudymail.mailclient.net.Account;
import mobi.cloudymail.mailclient.net.AttachmentInfo;
import mobi.cloudymail.mailclient.net.DataPacket;
import mobi.cloudymail.mailclient.net.Result;
import mobi.cloudymail.mailclient.net.ServerAgent;
import mobi.cloudymail.mailclient.net.TrustAllSSLSocketFactory;
import mobi.cloudymail.mms.MMSInfo;
import mobi.cloudymail.mms.MMSSender;
import mobi.cloudymail.util.DialogResult;
import mobi.cloudymail.util.DialogUtils;
import mobi.cloudymail.util.MessageBox;
import mobi.cloudymail.util.MyApp;
import mobi.cloudymail.util.NewDbHelper;
import mobi.cloudymail.util.Utils;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpPost;
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
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.impl.cookie.BrowserCompatSpec;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.RemoteViews;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class Composer extends BaseActivity implements OnClickListener
{
	public static final int COMPOSER_NEWMAIL = 1;
	public static final int COMPOSER_REPLY = 2;
	public static final int COMPOSER_REPLYALL = 3;
	public static final int COMPOSER_FORWARDMAIL = 4;
	public static final int COMPOSER_EDIT_MAIL = 5;
	public static final int COMPOSER_NEW_FROM_SENDTO = 6;

	private static final int REQ_TAKE_PICTURE = 10;
	private static final int REQ_SOUND_RECORDER = 20;
	private static final int REQ_CROP = 2;
	private static final int REQ_FILE_BROWSER = 30;
	private String cameraFileName;
	
	
	
	private Spinner _fromSpinner = null;
	private Button _toButton = null;
	private EditText _toEditText = null;
	private Button _ccButton = null;
	private EditText _ccEditText = null;
	private Button _bccButton = null;
	private EditText _bccEditText;
	private CheckBox _addRefBox = null;
	private CheckBox _includeAttaBox = null;
	private EditText _contentText = null;
	private Button _sendButton = null;
	private Button _saveButton = null;
	private Button _cancelButton = null;
	private EditText _subjectText = null;
	private Button _respondInlineBtn = null;
	private WebView _refOldMailView = null;
	private LinearLayout _refMailLayout = null;
	boolean needSendMail=false;
	private String[] addressBookArray = null;
	// private MailInfo curEditMailInfo = null;
	private OutMailInfo curMailInfo = null;
//	public List<AttachmentInfo> _attachments = new ArrayList<AttachmentInfo>();

	private ListView lv = null;

	private int clickedButton = -1;

	// open type diff edit from reply/forwar/replay all.
	// it's used to initialize the widget.
	private int _openType = COMPOSER_NEWMAIL;
	// mail type is current edited mail's type, can be new/reply/forward;
	// it also controlls the wiget _addRefBox,_includeAttaBox,
	// private int _mailType = COMPOSER_NEWMAIL;
	// private int _mailId = -1;///current edited out mail's id.

	private DataPacket dp = null;

	private String _refBody;
	private boolean contentStateModif = false;

	private final static int CUSTOM_NOTIFICATION_VIEW_ID = 1;
	private final static int FINISH_NOTIFI_ID = 2;
	private static final int REQ_SEND_MMS = 0;

	
	private NotificationManager mNotificationManager;
	private ListView _attachListView;
	private int selectedIdx = -1;
	private boolean _hasAttachFlag;
	private boolean _fromEditable = true;
	Handler myHandler=new Handler();
	
	ArrayList<String> phoneNumbers=new ArrayList<String>();
	ArrayList<String> phoneInEditText=new ArrayList<String>();
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.composer);
		//win.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.cloudymail);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.composer_titlebar);
		
		contentStateModif = false;
	   
		_attachListView=(ListView)findViewById(R.id.attachmentListView);
		
		_fromSpinner = (Spinner) findViewById(R.id.addresserCmb);
		_fromSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				if(_fromEditable)
					((TextView)parentView.getChildAt(0)).setTextColor(Color.BLACK);
				else
					((TextView)parentView.getChildAt(0)).setTextColor(Color.GRAY);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{
				// TODO Auto-generated method stub
				
			}});
		_toButton = (Button) findViewById(R.id.toBtn);
		_toEditText = (EditText) findViewById(R.id.autoEditText);
		_ccButton = (Button) findViewById(R.id.ccBtn);
		_ccEditText = (EditText) findViewById(R.id.ccEditText);
		_bccButton = (Button) findViewById(R.id.bccBtn);
		_bccEditText = (EditText) findViewById(R.id.bccEditText);
		_addRefBox = (CheckBox) findViewById(R.id.addRefCb);

		_includeAttaBox = (CheckBox) findViewById(R.id.includeAttachmentCb);
		_contentText = (EditText) findViewById(R.id.mainBody);
		_sendButton = (Button) findViewById(R.id.cp_sendBtn);
		_saveButton = (Button) findViewById(R.id.cp_saveBtn);
		_cancelButton = (Button) findViewById(R.id.cp_cancelBtn);
		_subjectText = (EditText) findViewById(R.id.cp_subjectText);
		_refOldMailView = (WebView) findViewById(R.id.refOldMailwView);
		_respondInlineBtn = (Button) findViewById(R.id.cp_respondInlineBtn);
		_refMailLayout = (LinearLayout) findViewById(R.id.cp_addRefLayout);

		_toButton.setOnClickListener(this);
		_ccButton.setOnClickListener(this);
		_bccButton.setOnClickListener(this);
		_sendButton.setOnClickListener(this);
		_saveButton.setOnClickListener(this);
		_cancelButton.setOnClickListener(this);
		_respondInlineBtn.setOnClickListener(this);     
		_refOldMailView.setBackgroundColor(0x00000000);
		
		_addRefBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				int vbt = isChecked ? View.VISIBLE : View.GONE;
				_respondInlineBtn.setVisibility(vbt);
				_refOldMailView.setVisibility(vbt);
			}
		});

		_ccButton.setVisibility(View.GONE);
		_ccEditText.setVisibility(View.GONE);
		_bccButton.setVisibility(View.GONE);
		_bccEditText.setVisibility(View.GONE);

		

		SearchEmailBookAdapter toAdapter = new SearchEmailBookAdapter(this, R.layout.list_item);
		// ArrayAdapter<String> toAdapter=new ArrayAdapter<String>(this,
		// android.R.layout.simple_dropdown_item_1line,array);
		// AutoCompleteTextView mutiAutoCompleteTextView =
		// (AutoCompleteTextView) findViewById(R.id.autoEditText);
		MultiAutoCompleteTextView mutiAutoCompleteTextView = (MultiAutoCompleteTextView) findViewById(R.id.autoEditText);
		mutiAutoCompleteTextView.setAdapter(toAdapter);
		mutiAutoCompleteTextView.setTokenizer(new SemicolonTokenizer());
		
		MultiAutoCompleteTextView ccMutiAutoCompleteTextView = (MultiAutoCompleteTextView) findViewById(R.id.ccEditText);
		ccMutiAutoCompleteTextView.setAdapter(toAdapter);
		ccMutiAutoCompleteTextView.setTokenizer(new SemicolonTokenizer());
		
		MultiAutoCompleteTextView bccMutiAutoCompleteTextView = (MultiAutoCompleteTextView) findViewById(R.id.bccEditText);
		bccMutiAutoCompleteTextView.setAdapter(toAdapter);
		bccMutiAutoCompleteTextView.setTokenizer(new SemicolonTokenizer());
	
		/* method_1 */
		// UserSetting userObj = MailClient.userSetting;
		// _contentText.setText(userObj.getNewSignature().toString());
		/* method_2 */
		// String newSignature = MyApp.userSetting.getSignature();
		// _contentText.setText(newSignature);
		Intent intent = getIntent();
		String action = intent.getAction();
		// if this is from the share menu
		if (Intent.ACTION_SEND.equals(action))
		{
			initMailInformation();
			loadAccountsToSpinner();
			
		}
		else
		{
			/*
			 * Someone has clicked a mailto: link. The address is in the URI.
			 */
			if (Intent.ACTION_SENDTO.equals(action) || Intent.ACTION_VIEW.equals(action))
			{
				_openType = COMPOSER_NEW_FROM_SENDTO;
			}
			else // if from reply,reply all, forward, the mail info should be set.
				_openType = getIntent().getIntExtra("composer_type", _openType);
			initMailInformation();
			loadAccountsToSpinner();
		}
		// If editText change, whether display save dialog box
		_toEditText.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
				if (_toEditText.getText().toString() != null && !_toEditText.getText().toString().equals(""))
				{
					contentStateModif = true;
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{
			
			}

			@Override
			public void afterTextChanged(Editable s)
			{
				
			}
		});
		_ccEditText.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
				if (_ccEditText.getText().toString() != null && !_ccEditText.getText().toString().equals(""))
				{
					contentStateModif = true;
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{

			}

			@Override
			public void afterTextChanged(Editable s)
			{

			}
		});
		_bccEditText.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
				if (_bccEditText.getText().toString() != null && !_bccEditText.getText().toString().equals(""))
				{
					contentStateModif = true;
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{
			}

			@Override
			public void afterTextChanged(Editable s)
			{
			}
		});
		_contentText.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
				if (_contentText.getText().toString() != null && !_contentText.getText().toString().equals(""))
				{
					contentStateModif = true;
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{
			}

			@Override
			public void afterTextChanged(Editable s)
			{
			}
		});
		_subjectText.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
				if (_subjectText.getText().toString() != null && !_subjectText.getText().toString().equals(""))
				{
					contentStateModif = true;
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{
			}

			@Override
			public void afterTextChanged(Editable s)
			{
			}
		});

	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater infalter = getMenuInflater();

		infalter.inflate(R.menu.composer_menu, menu);
//		for (int idx = 0; idx < menu.size(); idx++)
//		{
//			MenuItem mItem = menu.getItem(idx);
//		}
		return true;

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.sendMailMenu:
			onSendBtnClicked();
			break;
		case R.id.addToCcMailMenu:
			_ccButton.setVisibility(View.VISIBLE);
			_ccEditText.setVisibility(View.VISIBLE);
			_bccButton.setVisibility(View.VISIBLE);
			_bccEditText.setVisibility(View.VISIBLE);
			break;
		case R.id.addAttacmentMenu:
		{
			selectViewMode();
		}
			break;
		case R.id.cancleMenu:
			onCancelBtnClicked();
			break;

		}
		return super.onOptionsItemSelected(item);
	}

	private void initMailInformation()
	{

		if (_openType == COMPOSER_EDIT_MAIL)
		{
			this.curMailInfo = (OutMailInfo) (MyApp.curMailInfo);
			curMailInfo.setMailType(_openType);
		}
		else if (_openType == COMPOSER_NEWMAIL)
		{
			curMailInfo = new OutMailInfo();
			curMailInfo.setMailType(_openType);
			Bundle extras = getIntent().getExtras();
			String icaFiles[] = extras.getStringArray("icaFilePaths");
			if(icaFiles!=null)
			for(int i=0;i<icaFiles.length;i++)
				addAttachment(icaFiles[i]);
			
			// loadAccountsToSpinner();
			// get the shared image file, add it to attachment list.
			if (extras.containsKey(Intent.EXTRA_STREAM))
			{
				try
				{
					// Get resource path from intent callee
					Uri uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
					String intentType = getIntent().getType();
					if (uri != null)
					{
						ContentResolver cr = getContentResolver();
						String fileName = null;
						String fullPath = null;
						int fileSize = 0;
						Cursor metadataCursor = cr.query(uri, new String[] { MediaStore.Images.Media.DATA,
																			OpenableColumns.DISPLAY_NAME,
																			OpenableColumns.SIZE }, null, null, null);

						if (metadataCursor != null)
						{
							try
							{
								if (metadataCursor.moveToFirst())
								{
									fullPath = metadataCursor.getString(0);
									fileName = metadataCursor.getString(1);
									fileSize = metadataCursor.getInt(2);
								}
							}
							finally
							{
								metadataCursor.close();
							}
						}
						if (fileName == null)
						{
							fileName = uri.getLastPathSegment();
						}

						if ((intentType == null) || (intentType.indexOf('*') != -1))
						{
							intentType = cr.getType(uri);
						}

						if (fileSize <= 0)
						{
							String uriString = uri.toString();
							if (uriString.startsWith("file://"))
							{
								fullPath=uriString.substring("file://".length());
								File f = new File(fullPath);
								fileSize = (int) f.length();
							}
							else
							{
								Log.d(LOGTAG, "Not a file" + "Not a file: " + uriString);
							}
						}
						Log.d(LOGTAG, "Composer" + "new attachment.size: " + fileSize);
						Log.d(LOGTAG, "Composer" + "new attachment.fileName: " + fileName);
						Log.d(LOGTAG, "Composer" + "new attachment.file path: " + fullPath);

						AttachmentInfo attaInfo = new AttachmentInfo(curMailInfo);
						attaInfo.fileName = fileName;
						// Log.d(fileName,
						// "File.length()="+attFile.length()+"; FileInputStream.available():"+fi.available());
						attaInfo.size = Utils.getReadableSize(fileSize);// fi.available());
						attaInfo.fullFilePath = fullPath;
						curMailInfo.addAttachInfo(attaInfo);
						
					}

					// Query gallery for camera picture via
					// Android ContentResolver interface
					// ContentResolver cr = getContentResolver();
					// InputStream is = cr.openInputStream(uri);

//					return;
				}
				catch (Exception e)
				{
					Log.e(LOGTAG, this.getClass().getName() + e.toString());
				}
			}
		}
		else if (_openType == COMPOSER_NEW_FROM_SENDTO)
		{
			curMailInfo = new OutMailInfo();
			_openType = COMPOSER_NEWMAIL;
			curMailInfo.setMailType(_openType);
			Intent intent = getIntent();
			if (intent.getData() != null)
			{
				Uri uri = intent.getData();
				if ("mailto".equals(uri.getScheme()))
				{
					String schemaSpecific = uri.getSchemeSpecificPart();
					int end = schemaSpecific.indexOf('?');
					if (end == -1)
					{
						end = schemaSpecific.length();
					}

					// Extract the recipient's email address from the mailto URI
					// if there's one.
					String recipient = Uri.decode(schemaSpecific.substring(0, end));
					curMailInfo.setTo(recipient);
				}
			}
		}
		else
		{
			MailInfo orgMi = MyApp.curMailInfo;
			curMailInfo = new OutMailInfo();
			curMailInfo.setAccountId(orgMi.getAccountId());
			curMailInfo.setMailType(_openType);
			curMailInfo.setRefBodyFlag(OutMailInfo.REFMAIL_YES);
			curMailInfo.setUid(orgMi.getUid());// set refered Uid;
			curMailInfo.setRefFolder(orgMi.getFolder());
			Resources res = getResources();
			switch (_openType)
			{
			case COMPOSER_REPLYALL:
				curMailInfo.setMailType(COMPOSER_REPLY);
				// add others in original mail's tolist
				Account orgAcct = AccountManager.getAccount(orgMi.getAccountId());
				String orgToOthers = EmailAddress.filterMailAddress(orgMi.getTo(), orgAcct.name);
				String orgCC = EmailAddress.filterMailAddress(orgMi.getCc(), orgAcct.name);
				if (!Utils.isEmpty(orgCC) && !Utils.isEmpty(orgToOthers))
				{
					curMailInfo.setCc(orgToOthers + ";" + orgCC);
				}
				else
				{

					curMailInfo.setCc((orgToOthers == null ? "" : orgToOthers) + (orgCC == null ? "" : orgCC));
				}
				// go on.
			case COMPOSER_REPLY:
				curMailInfo.setTo(orgMi.getFrom());
				curMailInfo.setSubject(res.getString(R.string.reply) + ":" + orgMi.getSubject());
				break;
			case COMPOSER_FORWARDMAIL:
				if (orgMi.hasAttachment())
				{
					// if not login yet, the return result will be empty.
					// Account acct =
					// AccountManager.getAccount(orgMi.getAccountId());
					// if(!MyApp.getAgent(acct).isLogin())
					//curMailInfo.addAttachment(new AttachmentInfo(AttachmentInfo.ALL_REFATTACH_INDEX));
					// else
					curMailInfo.setAttachmentFlag(orgMi.getAttachmentFlag());
					curMailInfo.setAttachments(orgMi.getAttachments());
				}
				curMailInfo.setSubject(res.getString(R.string.forward) + ":" + orgMi.getSubject());
				break;
			}
		}
		// then initialize widget based on mail info.
		_toEditText.setText(curMailInfo.getTo());
		String receiver = curMailInfo.getCc();
		_ccEditText.setText(receiver);
		if(!Utils.isEmpty(receiver))
		{
			_ccButton.setVisibility(View.VISIBLE);
			_ccEditText.setVisibility(View.VISIBLE);
		}
		receiver = curMailInfo.getBc();
		_bccEditText.setText(receiver);
		if(!Utils.isEmpty(receiver))
		{
			_bccButton.setVisibility(View.VISIBLE);
			_bccEditText.setVisibility(View.VISIBLE);
		}		
		_subjectText.setText(curMailInfo.getSubject());

		_includeAttaBox.setVisibility(View.GONE);
		_addRefBox.setVisibility(View.GONE);
		this._refOldMailView.setVisibility(View.GONE);
		this._respondInlineBtn.setVisibility(View.GONE);
		_refMailLayout.setVisibility(View.GONE);

		boolean isShowSignature = false;
		switch (curMailInfo.getMailType())
		{
		case COMPOSER_NEWMAIL:
			isShowSignature = true;
			break;
		case COMPOSER_FORWARDMAIL:
			// must refer old mail.
			_addRefBox.setEnabled(false);
			if (curMailInfo.hasAttachment())
			{
				_includeAttaBox.setChecked(true);
				_includeAttaBox.setVisibility(View.VISIBLE);
				_fromSpinner.setEnabled(false);
				isShowSignature = true;
			}
			// go on.
		case COMPOSER_REPLY:
			_refMailLayout.setVisibility(View.VISIBLE);
			_addRefBox.setChecked(true);
			_addRefBox.setVisibility(View.VISIBLE);
			_respondInlineBtn.setVisibility(View.VISIBLE);
			_refOldMailView.setVisibility(View.VISIBLE);
			isShowSignature = true;
			break;
		case COMPOSER_EDIT_MAIL:
			isShowSignature = false;
			break;
		case COMPOSER_NEW_FROM_SENDTO:
			// isShowSignature=true;
			break;
		}
		String newSignature = MyApp.userSetting.getSignature();
		if (!Utils.isEmpty(newSignature) && isShowSignature == true)
		{
			_contentText.setText(curMailInfo.getBody() + "\n" + newSignature);
		}
		else
		{
			_contentText.setText(curMailInfo.getBody());
		}
		String refUid = curMailInfo.getUid();
		if (!refUid.equals(""))
		{
			Intent intent = getIntent();
			_refBody = intent.getStringExtra("refMailBody");
			_refOldMailView.loadDataWithBaseURL("", _refBody, "text/html", "utf-8", null);
		}
		
		if(curMailInfo!=null)
		{
		_hasAttachFlag=curMailInfo.hasAttachment();
			if(_hasAttachFlag)
			{
				
	//			_refAttachIndex=intent.getIntExtra("refAttachIndex", 0);
	//			_refAttachName=intent.getStringExtra("refMailAttathName");
	//			
	//			AttachmentInfo attInfo=new AttachmentInfo();
	//			attInfo.index=_refAttachIndex;
	//			attInfo.fileName=_refAttachName;
	//			_attachments.add(attInfo);
				_attachListView.setAdapter(new AttachmentAdapter(this, curMailInfo.getAttachments()));
				_attachListView.setVisibility(View.VISIBLE);
				setAttachmentHeight();
			}
		}
	}
    private void setAttachmentHeight()
    {
    	AttachmentAdapter listAdapter = (AttachmentAdapter)_attachListView.getAdapter();   
        if (listAdapter == null) {  
            return;  
        }  
  
        int totalHeight = 0;  
        for (int i = 0; i < listAdapter.getCount(); i++) {  
            View listItem = listAdapter.getView(i, null, _attachListView);  
            listItem.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            listItem.measure(0, 0);  
            totalHeight += listItem.getMeasuredHeight();  
        }  
  
        ViewGroup.LayoutParams params = _attachListView.getLayoutParams();  
        params.height = totalHeight + (_attachListView.getDividerHeight() * (listAdapter.getCount() - 1));  
        ((ViewGroup.MarginLayoutParams)params).setMargins(10, 10, 10, 10);
        _attachListView.setLayoutParams(params);  
    }
	private String html2text(String html)
	{
		// remove the head tag. the js will always included in this tag.
		int headBegin = html.indexOf("<head>");
		int headEnd = html.indexOf("</head>");
		String tmpHtm = html;
		if (headBegin >= 0 && headEnd >= 0)// matched
		{
			String first = html.substring(0, headBegin);
			String end = html.substring(headEnd + 7);
			tmpHtm = first + end;
		}
		// String tmpHtm = html.replaceAll("<head>.*</head>", "");
		String plainTxt = Html.fromHtml(tmpHtm).toString();
		return plainTxt;
	}

	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
		case R.id.toBtn:
			clickedButton = 1;
			onToButtonClicked();
			break;
		case R.id.ccBtn:
			clickedButton = 2;
			onToButtonClicked();
			break;
		case R.id.bccBtn:
			clickedButton = 3;
			onToButtonClicked();
			break;
		case R.id.cp_sendBtn:
			curMailInfo.setFolder(FolderNames.FOLDER_SENT);
			onSendBtnClicked();
			break;
		case R.id.cp_saveBtn:
		{
			contentStateModif = false;
			updateMailInfo();
			curMailInfo.setFolder(FolderNames.FOLDER_DRAFT);
			NewDbHelper.getInstance().saveOutMail(curMailInfo);
			Resources res = getResources();
			if (curMailInfo.getUidx() < 0)
			{
				Log.d(LOGTAG, "Save mail failed" + "");
				DialogUtils.showMsgBox(this, res.getString(R.string.cp_saveMailFail), res.getString(R.string.error));
			}
			else
			{
				
				Log.d(LOGTAG, "Save mail sucessfully" + "");
				Toast.makeText(this, res.getString(R.string.cp_saveMailSuccess), Toast.LENGTH_LONG).show();
			}
			break;
		}
		case R.id.cp_cancelBtn:
			onCancelBtnClicked();
			break;
		case R.id.cp_respondInlineBtn:
		{
			// append referent body text to _contentText,then hide the related
			// widgets.
			_contentText.append(html2text(_refBody));
			_addRefBox.setChecked(false);
			_refMailLayout.setVisibility(View.GONE);
			_refOldMailView.setVisibility(View.GONE);
		}
			break;
		default:
			break;
		}
	}
	public void selectViewMode()
	{
		android.content.DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which)
			{
				switch (which)
				{
				case 0:
				{
					cameraClieded();
				}
					break;
				case 1:
					recordClicked();
					break;
				case 2:
					addAttachmentBtnClicked();
					break;
				}

			}

		};
        Resources res=getResources();
		String[] menu = {res.getString(R.string.atta_Photo_Mode) , res.getString(R.string.atta_Record_Mode) , res.getString(R.string.atta_file_Mode)};
		new AlertDialog.Builder(Composer.this).setTitle(res.getString(R.string.atta_Choose_Mode))
				.setItems(menu, listener).show();
	}
	
	private void addAttachment(String fileName)// including file path
	{
		if (fileName == null || fileName.equals(""))
			return;
		File attFile = new File(fileName);
		if (!attFile.exists())
		{
			Toast.makeText(this, fileName + " not exists.", Toast.LENGTH_SHORT)
					.show();
			return;
		}
		try
		{
			// FileInputStream fi = new FileInputStream(attFile);
			AttachmentInfo attaInfo = new AttachmentInfo(curMailInfo);
			attaInfo.fileName = attFile.getName();
			// Log.d(fileName,
			// "File.length()="+attFile.length()+"; FileInputStream.available():"+fi.available());
			attaInfo.size = getHumanSize(attFile.length());// fi.available());
			attaInfo.fullFilePath = fileName;
			curMailInfo.addAttachInfo(attaInfo);
			updateListView();
		}
		catch (Exception e)
		{
			Log.d(Utils.LOGTAG, "",e);
		}
	}
	static public String getHumanSize(long value)
	{
		double sizeF = value;
		String unit = "B";
		if (value > (1 << 30))
		{
			sizeF = sizeF / (1 << 30);
			unit = "GB";
		}
		else if (sizeF > (1 << 20))
		{
			sizeF = sizeF / (1 << 20);
			unit = "MB";
		}
		else if (sizeF > 1024)
		{
			sizeF = sizeF / (1024.0);
			unit = "KB";
		}
		NumberFormat nFormat = NumberFormat.getInstance();
		nFormat.setMaximumFractionDigits(1);
		return (nFormat.format(sizeF) + unit);
	}
	private void updateListView()
	{
		
		_attachListView.setAdapter(new AttachmentAdapter(this, curMailInfo.getAttachments()));
		((AttachmentAdapter) _attachListView.getAdapter())
				.notifyDataSetChanged();
		_attachListView.setVisibility(View.VISIBLE);
		// clear selectionattachListView
		selectedIdx = -1;
		int childCount = _attachListView.getChildCount();
		int backColor = getResources().getColor(R.color.white);
		for (int i = 1; i < childCount; i++)
			_attachListView.getChildAt(i).setBackgroundColor(backColor);
		setAttachmentHeight();
		if(curMailInfo.getAttachments().size()<=0)
		{
			_attachListView.setVisibility(View.GONE);
		}
	}
	public void delAttachment(int attachItemIndex)
	{
		selectedIdx=attachItemIndex;
		if(selectedIdx<0)
			return;
		curMailInfo.getAttachments().remove(selectedIdx);
		updateListView();
	}
	private boolean isSdCardAvailable()
	{
		Resources res = getResources();
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
		{
			MessageBox.show(this, res.getString(R.string.atta_sdcardReadOnly),
							res.getString(R.string.error));
			return false;
		}
		else if (!Environment.MEDIA_MOUNTED.equals(state))
		{
			MessageBox.show(this,
							res.getString(R.string.atta_sdcardNotAvailable),
							res.getString(R.string.error));
			return false;
		}
		return true;
	}

	private void addAttachmentBtnClicked()
	{
		if (!isSdCardAvailable())
			return;
		Intent intent = new Intent(this, FileBrowser.class);
		startActivityForResult(intent, REQ_FILE_BROWSER);
	}
	private void cameraClieded()
	{


		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		cameraFileName = createUniqeFile();

		/****************/
		Uri uri = Uri.fromFile(new File(cameraFileName));
		intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
		startActivityForResult(intent, REQ_TAKE_PICTURE);
	}


	private String createUniqeFile() {
		Calendar cal = Calendar.getInstance();
		Date curDate = cal.getTime();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String stem = sdf.format(curDate);
		
		String fileName = stem;
		File out = new File(MyApp.instance().getImageSdcardPath(),
				fileName + ".png");
		/*****************/
		int i=1;
		while (out.exists())
		{
			fileName = stem + "(" + i + ")";
			out = new File(MyApp.instance().getImageSdcardPath(),
					fileName + ".png");
			i++;

		}
		try {
			return out.getCanonicalPath();
		} catch (IOException e) {
			
			Log.d(Utils.LOGTAG, "",e);
		}
		return null;
	}

	private void recordClicked()
	{
		Intent intent = new Intent("android.provider.MediaStore.RECORD_SOUND");

//		 recordFileName = System.currentTimeMillis() + ".3gpp";
//		 File recordOut = new File(Environment.getExternalStorageDirectory(),
//		 recordFileName);
//		 Uri uri = Uri.fromFile(recordOut);
//		 intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);

		startActivityForResult(intent, REQ_SOUND_RECORDER);
	}
	/*
	 * @Override public void onActivityResult(int reqCode, int rstCode, Intent
	 * intent) { switch (reqCode) { case R.layout.attachment_viewer:// account
	 * wizard { // DialogUtils.showMsgBox(this,"activity result", "test"); // if
	 * (rstCode != Dialog.BUTTON_POSITIVE) // return; } break; default: break; }
	 * }
	 */

	// @Override
	// public boolean onMenuItemSelected(int featureId, MenuItem item)
	// {
	// switch(item.getItemId())
	// {
	// case R.id.sendMailMenu:
	// toBtnClicked = true;
	// onToButtonClicked();
	// break;
	// case R.id.addToCcMailMenu:
	// toBtnClicked = false;
	// onToButtonClicked();
	// break;
	// case R.id.addAttacmentMenu:
	// {
	// Intent intent = new Intent(this, AttachmentList.class);
	// intent.putExtra("isComposer", true);
	// startActivity(intent);
	// }
	// break;
	// case R.id.cancleMenu:
	// onCancelBtnClicked();
	// break;
	// default:
	// break;
	// }
	// return super.onMenuItemSelected(featureId, item);
	// }

	
	private void onCancelBtnClicked()
	{
		Resources res = getResources();
		try
		{
			if (_contentText.getText().toString().equals("")
				|| DialogUtils.showModalMsgBox(	this, res.getString(R.string.cp_discardMailMsg),
												res.getString(R.string.cp_discardMail),
												EnumSet.of(DialogUtils.ButtonFlags.Yes, DialogUtils.ButtonFlags.No)) == DialogResult.OK)
			{
				finish();
			}
		}
		catch (Exception e)
		{
			Log.d(Utils.LOGTAG, "",e);
		}
	}

	private DataPacket createDataPacket()
	{
		if (dp == null)
			dp = new DataPacket();
		// dp.packetType = _mailType;
		// in server there's no reply all.

		// based on OutMailInfo
		switch (curMailInfo.getMailType())
		{
		case COMPOSER_REPLY:
			dp.packetType = DataPacket.REPLYMAIL_TYPE;
			break;
		case COMPOSER_FORWARDMAIL:
			dp.packetType = DataPacket.FORWARDMAIL_TYPE;
			break;
		default:
			dp.packetType = DataPacket.NEWMAIL_TYPE;
			break;
		}
		dp.quoteOld = (curMailInfo.getRefBodyFlag() == OutMailInfo.REFMAIL_YES);
		dp.refMailId = curMailInfo.getUid();
		dp.refMailFolder = curMailInfo.getRefFolder();
		dp.forwardAttach = _includeAttaBox.isChecked();
		dp.toList = curMailInfo.getTo();
		dp.ccList = curMailInfo.getCc();
		dp.bccList = curMailInfo.getBc();
		dp.subject = curMailInfo.getSubject();
		dp.bodyText = curMailInfo.getBody();
		dp.attachments = curMailInfo.getAttachments();
		return dp;
	}

	private void updateMailInfo()// based on widgets
	{
		if (!_addRefBox.isChecked())
			curMailInfo.setRefBodyFlag(OutMailInfo.REFMAIL_NO);
		else if (_addRefBox.isShown())//
			curMailInfo.setRefBodyFlag(OutMailInfo.REFMAIL_YES);
		else
			curMailInfo.setRefBodyFlag(OutMailInfo.REFMAIL_RESPOND_INLINE);

		String[] toList = _toEditText.getText().toString().split(";");
		String tos = toList[0];
		for (int i = 1; i < toList.length; i++)
			tos += ";" + toList[i];
		curMailInfo.setTo(tos);

		String[] ccList = _ccEditText.getText().toString().split(";");
		String ccs = ccList[0];
		for (int i = 1; i < ccList.length; i++)
			ccs += ";" + ccList[i];
		curMailInfo.setCc(ccs);

		String[] bccList = _bccEditText.getText().toString().split(";");
		String bccs = bccList[0];
		for (int i = 1; i < bccList.length; i++)
			bccs += ";" + bccList[i];
		curMailInfo.setBc(bccs);

		curMailInfo.setBody(_contentText.getText().toString());
		curMailInfo.setSubject(_subjectText.getText().toString());

		int fromIdx = _fromSpinner.getSelectedItemPosition();
		curMailInfo.setAccountId(AccountManager.getByIndex(fromIdx).id);
	}

	private void onSendBtnClicked()
	{
		if (!checkAddressValid())
			return;
		/* start add */
		String toEditEmailString = _toEditText.getText().toString() + ";" + _ccEditText.getText().toString() + ";"
									+ _bccEditText.getText().toString();

		ArrayList<EmailAddress> emailAddBookArray = AddressBook.getEmailAddresses();
		needSendMail=false;
		phoneNumbers.clear();
		phoneInEditText.clear();
		String[] toAddList = toEditEmailString.split(";");
		if (!toEditEmailString.equals(""))
		{
			doReturn: for (int i = 0; i < toAddList.length; i++)
			{
				String toList = toAddList[i];
				if (Utils.isEmpty(toList.trim()))
					continue;
				EmailAddress newMailAddr = new EmailAddress(toList);
				if(newMailAddr!=null)
				{
					for (int j = 0; j < emailAddBookArray.size(); j++)
					{
						EmailAddress emailAddress = emailAddBookArray.get(j);
	
						if (emailAddress.equals(newMailAddr))
						{
							needSendMail=true;
							continue doReturn;
						}
					}
				}
				String phoneNumber=getPhoneNumber(toList);
				if(phoneNumber!=null)
				{
					phoneInEditText.add(toList);
					phoneNumbers.add(phoneNumber);
					
				}
				else
				{
					needSendMail=true;
					emailAddBookArray.add(newMailAddr);
					NewDbHelper.getInstance().addEmailAddress(newMailAddr);
				}
			}
		}

		String title = _subjectText.getText().toString();

		Resources res = getResources();
		if (title.equals(""))
		{
			try
			{
				if (DialogUtils.showModalMsgBox(this, res.getString(R.string.cp_emptySubjectMsg),
												res.getString(R.string.cp_emptySubject),
												EnumSet.of(DialogUtils.ButtonFlags.Yes, DialogUtils.ButtonFlags.No)) == DialogResult.CANCEL)
					return;
			}
			catch (Exception e)
			{
				Log.d(Utils.LOGTAG, "",e);
			}
		}
		if(phoneNumbers.size()!=0)
		{
			if (DialogUtils.showModalMsgBox(this, getResources().getString(R.string.mms),
											getResources().getString(R.string.mms_info), EnumSet
													.of(DialogUtils.ButtonFlags.Yes,
														DialogUtils.ButtonFlags.No)) == DialogResult.YES)
			{
				if(dataDisconnected())
				{
					if (DialogUtils
							.showModalMsgBox(	this,
												getResources().getString(R.string.open_setting),
												getResources().getString(R.string.setting_info),
												EnumSet.of(	DialogUtils.ButtonFlags.Yes,
															DialogUtils.ButtonFlags.No)) == DialogResult.YES)
					{
						Intent intent = new Intent("/");
						ComponentName cm = new ComponentName("com.android.settings",
																"com.android.settings.WirelessSettings");
						intent.setComponent(cm);
						intent.setAction("android.intent.action.VIEW");
						startActivityForResult(intent,REQ_SEND_MMS);
					}
				}
				else
				{
					sendMMS();
					sendMail();
					finish();
				}
			}
		}
		else
		{
			sendMail();
			finish();
		}
		
	}
	private void sendMail()
	{
		if (needSendMail)
		{
			// send mail;
			updateMailInfo();
			createDataPacket();
			// save draft before send the mail;
			// now only support current account id.
			int curIdx = _fromSpinner.getSelectedItemPosition();
			Account account = AccountManager.getByIndex(curIdx);
	
			
			
			// notification.defaults = Notification.DEFAULT_ALL;
			try
			{
				String sid = MyApp.getAgent(account).getSessionId(true, true, true);
				if (Utils.isEmpty(sid))
					return;
				/*
				 * Notice on
				 */
				Notification notification = null;
				RemoteViews contentView = null;
				mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
				notification = new Notification(R.anim.notification_fram_anim, getResources().getString(R.string.wait_sending), System.currentTimeMillis());
	//			contentView = new RemoteViews(getPackageName(), R.layout.custom_notification);
				contentView = new RemoteViews(getPackageName(), R.layout.sent_notification);
				
				// contentView.setProgressBar(R.id.noti_progressView, 10, 0, true);
				contentView.setImageViewResource(R.id.custom_notifi_image, R.drawable.sign_up_icon);
				contentView.setTextViewText(R.id.custom_notifi_title, getResources().getString(R.string.wait_sending));
				notification.contentView = contentView;
				notification.flags |= Notification.FLAG_AUTO_CANCEL;
				Intent notificationIntent = new Intent(Intent.ACTION_MAIN);
				notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
				notificationIntent.setClass(MyApp.instance(), GlobalInBoxActivity.class);
				PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
				notification.contentIntent = contentIntent;
				notification.contentView.setProgressBar(R.id.noti_progressView, 100, 0, true);
				mNotificationManager.notify(CUSTOM_NOTIFICATION_VIEW_ID, notification);
				dp.toList=getAddrs(_toEditText.getText().toString());
				dp.ccList=getAddrs(_ccEditText.getText().toString());
				dp.bccList=getAddrs(_bccEditText.getText().toString());
				new SendMailTask(account, sid).execute(dp);
				
			}
			catch (Exception e)
			{
				Log.d(Utils.LOGTAG, "",e);
			}
		}
		
	}

	class SendMailTask extends AsyncTask<DataPacket, Integer, String>
	{

		private Resources res = MyApp.instance().getResources();
		// private ProgressDialog prgDialog = new ProgressDialog(Composer.this);
		private String sid;

		private MailInfo refMailInfo=MyApp.curMailInfo;
		public SendMailTask(Account a, String sessionId)
		{
			sid = sessionId;
		}

		@Override
		protected String doInBackground(DataPacket... dPackets)
		{
			DataPacket dp = dPackets[0];
			try
			{
				MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.STRICT, null,
																Charset.forName("UTF-8"));

				// load attachment information
				// int attaCount = _attachments.size();
				// for (int i = 0; i < attaCount; i++)
				// {
				// AttachmentInfo att = _attachments.get(i);
				// if (att.index != AttachmentInfo.LOCAL_ATTACH_INDEX)
				// continue;
				// File inputFile = new File(att.fullFilePath);
				// FileInputStream finput = new FileInputStream(inputFile);
				// byte[] barry = new byte[(int) inputFile.length()];
				// finput.read(barry);
				//
				// byte[] temp = Base64.encodeBase64(barry, true);
				// att.body = new String(temp);
				// // Log.d("Encoding result",
				// // "Length:"+att.body.length()+"  contents:"+new
				// // String(att.body));
				// finput.close();
				// }
				Serializer serializer = new Persister();
				// Persister serializer = new Persister();
				ByteArrayOutputStream out = new ByteArrayOutputStream(100000);
				// FileOutputStream xmlFile = new
				// FileOutputStream("/mnt/sdcard/test.xml");
				// serializer.write(dp, xmlFile,"UTF-8");
				serializer.write(dp, out);// ,"UTF-8");
				byte[] outBytes = out.toByteArray();
                   
				ByteArrayBody bin = new ByteArrayBody(outBytes, "text/_xml_", "__thebody.__");
				reqEntity.addPart("bin", bin);
				int attaCount = curMailInfo.getAttachments().size();
				for (int i = 0; i < attaCount; i++)
				{
					AttachmentInfo att = curMailInfo.getAttachment(i);
					if (att.index != AttachmentInfo.LOCAL_ATTACH_INDEX)
						continue;
					File inputFile = new File(att.fullFilePath);
					FileBody body = new FileBody(inputFile);
					reqEntity.addPart(att.fullFilePath, body);
				}

				HttpPost p = new HttpPost(ServerAgent.getUrlBase() + "/SendMail?sid=" + java.net.URLEncoder.encode(sid));

				Log.d(LOGTAG, "byte output" + "out.size():" + out.size() + ";;;out.toByteArray:" + outBytes.length);

				p.setEntity(reqEntity);

				HttpParams params = new BasicHttpParams();
				HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
				HttpProtocolParams.setContentCharset(params, "UTF-8");
				HttpProtocolParams.setUseExpectContinue(params, true);
				HttpProtocolParams.setUserAgent(params, ServerAgent.AGENT_ID);

				// Make pool
				ConnPerRoute connPerRoute = new ConnPerRouteBean(12);
				ConnManagerParams.setMaxConnectionsPerRoute(params, connPerRoute);
				ConnManagerParams.setMaxTotalConnections(params, 20);

				// Set timeout
				HttpConnectionParams.setStaleCheckingEnabled(params, false);
				HttpConnectionParams.setConnectionTimeout(params, 20 * 1000); // 20
																				// seconds
																				// to
																				// wait
																				// connection
				HttpConnectionParams.setSoTimeout(params, 10 * 60 * 1000); // 10
																			// minutes
																			// to
																			// wait
																			// response
																			// data
				HttpConnectionParams.setSocketBufferSize(params, 8192);

				// Some client params
				HttpClientParams.setRedirecting(params, false);

				// Register http/s shemas!
				SchemeRegistry schReg = new SchemeRegistry();
				schReg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
				try
				{
					schReg.register(new Scheme("https", TrustAllSSLSocketFactory.getDefault(), 443));
				}
				catch (Exception e)
				{
					Log.d(Utils.LOGTAG, "",e);
				}

				ClientConnectionManager conMgr = new ThreadSafeClientConnManager(params, schReg);
				DefaultHttpClient client = new DefaultHttpClient(conMgr, params);

				// method 1, for cookie enable
				// CookieHandler h = CookieHandler.getDefault();
				// sClient.getParams().setParameter(
				// ClientPNames.COOKIE_POLICY, CookiePolicy.RFC_2965);
				// sClient.getCookieSpecs().register("def", new
				// RFC2965SpecFactory());

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

				client.getCookieSpecs().register("easy", csf);
				client.getParams().setParameter(ClientPNames.COOKIE_POLICY, "easy");
				HttpResponse rsp = client.execute(p);
				Result rst = new Result(rsp.getEntity().getContent());
				if (rst.isSuccessed())
					return "";
				else
					return rst.failReason;
			}
			catch (Exception e)
			{
				Log.d(Utils.LOGTAG, "",e);
				return e.getMessage();
			}
		}

		@Override
		protected void onPreExecute()
		{
			// save the mail, so user can restore it if there's error during
			// sending
			curMailInfo.setFolder(FolderNames.FOLDER_NOTSENT);
			NewDbHelper.getInstance().saveOutMail(curMailInfo);
			String msg = res.getString(R.string.inSending) + "\n" + curMailInfo.getSubject();
			Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
			// prgDialog.setMessage(res.getString(R.string.cp_sendingMail));
			// prgDialog.show();
		}

		@Override
		protected void onProgressUpdate(Integer... progress)
		{
			// setProgressPercent(progress[0]);
		}

		@Override
		protected void onPostExecute(String result)
		{
			// prgDialog.hide();
			// prgDialog.dismiss();
			String msg = null;
			String notifiMsg=null;
			mNotificationManager.cancel(CUSTOM_NOTIFICATION_VIEW_ID);
			Log.d(LOGTAG, "Cancel the notification statue bar");
			if ("".equals(result))
			{
				Log.d(LOGTAG, "Send mail sucessfully" + "");
				msg = "\""+curMailInfo.getSubject()+"\" "+res.getString(R.string.cp_sendSuccess);
				notifiMsg=getResources().getString(R.string.cp_sendSuccess);
				try
				{
					boolean updataMailView=false;
					if((_openType==COMPOSER_REPLYALL||_openType==COMPOSER_REPLY)&&!refMailInfo.hasReply())
					{
						
						refMailInfo.setAttachmentFlag(MailInfo.REPLY|refMailInfo.getAttachmentFlag());
						NewDbHelper.getInstance().setMailAttachment(refMailInfo.getUid(), refMailInfo.getFolder(), refMailInfo.getAttachmentFlag());
						updataMailView=true;
					}
					else if(_openType==COMPOSER_FORWARDMAIL&&!refMailInfo.hasForward())
					{
						refMailInfo.setAttachmentFlag(MailInfo.FORWARD|refMailInfo.getAttachmentFlag());
						NewDbHelper.getInstance().setMailAttachment(refMailInfo.getUid(), refMailInfo.getFolder(), refMailInfo.getAttachmentFlag());
						updataMailView=true;
					}
					if(updataMailView&&MyApp.getCurrentActivity()instanceof InBoxActivity)
					{
						InBoxActivity activity=(InBoxActivity)MyApp.getCurrentActivity();
						activity.updateMail();
					}
				}
				catch (Exception e)
				{
					Utils.logException(e);
				}
				// save the mail to outBox;
				curMailInfo.setFolder(FolderNames.FOLDER_SENT);
				NewDbHelper.getInstance().saveOutMail(curMailInfo);
				Activity activity = MyApp.getCurrentActivity();
				if(activity instanceof OutBoxActivity)
				{
					try
					{
						((OutBoxActivity) activity).updateMail();
					}
					catch (SQLException e)
					{
						Log.d(Utils.LOGTAG, "",e);
					}
				}
			}
			else
			{
				Log.d(LOGTAG, "Send mail failed:" + result);
				msg = "\""+curMailInfo.getSubject()+"\" "+res.getString(R.string.cp_sendFailure);
				notifiMsg=getResources().getString(R.string.cp_sendFailure);
				curMailInfo.setFolder(FolderNames.FOLDER_NOTSENT);
				NewDbHelper.getInstance().saveOutMail(curMailInfo);
			}

			Notification finishNotification = null;
			RemoteViews finishContentView = null;
			
			finishNotification = new Notification(R.drawable.sign_up_4, curMailInfo.getSubject() + notifiMsg,
													System.currentTimeMillis());
			finishContentView = new RemoteViews(getPackageName(), R.layout.sending_notification);
			// finishContentView.setProgressBar(R.id.noti_progressView, 100,
			// 100, false);
			finishContentView.setImageViewResource(R.id.custom_notifi_image, R.drawable.sign_up_icon);
			finishContentView.setTextViewText(R.id.custom_notifi_title, res.getString(R.string.appName));
			finishContentView.setTextViewText(R.id.custom_notifi_text,msg);
			finishNotification.contentView = finishContentView;

			Intent endNotificationIntent = new Intent(Intent.ACTION_MAIN);
			endNotificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
			endNotificationIntent.setClass(MyApp.instance(), GlobalInBoxActivity.class);

			PendingIntent endcontentIntent = PendingIntent.getActivity(	MyApp.instance(), 0, endNotificationIntent,
																		0);
			finishNotification.contentIntent = endcontentIntent;
			finishNotification.flags |= Notification.FLAG_AUTO_CANCEL;
			mNotificationManager.notify(FINISH_NOTIFI_ID, finishNotification);
			Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
		}
	}

	private void onToButtonClicked()
	{
		initAddressBookArray();
		/* test */
		final EditText activeTextBox = clickedButton == 1 ? _toEditText : (clickedButton == 2	? _ccEditText
																								: _bccEditText);
		Utils.ASSERT(activeTextBox != null);
		int m = AddressBook.addressArry.size();
		String toMailAddress = activeTextBox.getText().toString();
		String[] split = toMailAddress.split(";");
		boolean[] checkedItems = new boolean[m];
		final ArrayList<EmailAddress> allAddr = AddressBook.getEmailAddresses();
		for (int i = 0; i < split.length; i++)
		{
			if (split[i].trim().length() == 0)
				continue;

			String addr=split[i].trim();
				
			for (int k = 0; k < checkedItems.length; k++)
			{
				String addressInAddrBook=allAddr.get(k).toString();
				if (addr.equals(addressInAddrBook))
				{
					checkedItems[k] = true;
					break;
				}
			}
		}
		AlertDialog ad = new AlertDialog.Builder(this)
						.setMultiChoiceItems(addressBookArray, checkedItems,
												new DialogInterface.OnMultiChoiceClickListener() {
													// public abstract void
													// onClick
													// (DialogInterface dialog,
													// int
													// which, boolean isChecked)
													public void onClick(DialogInterface dialog, int whichButton,
																	boolean isChecked)
													{
													}
												}

						).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton)
							{
								// String toString = "";
								String toString="";
								String oldValueString ="";
								SparseBooleanArray checkedState = lv.getCheckedItemPositions();
								for (int i = 0; i < addressBookArray.length; i++)
								{
									
									if (checkedState.get(i))// selected
									{
										if (oldValueString.contains(allAddr.get(i).getAddress()))
											continue;
										toString = lv.getItemAtPosition(i).toString() + ";";
										if (oldValueString.lastIndexOf(";") == oldValueString.length() - 1)
										{
											oldValueString += toString;
										}
										else
										{
											oldValueString += ";" + toString;
										}
										
									}
								}
								activeTextBox.setText(oldValueString);
							}

						}).setNegativeButton(R.string.cancel, null).create();

		ad.setTitle(getResources().getString(	clickedButton == 1	? R.string.cp_selectToAddr
														: (clickedButton == 2	? R.string.cp_selectCcAddr
																				: R.string.cp_selectBccAddr)));
		lv = ad.getListView();

		Builder ad2 = new AlertDialog.Builder(this).setTitle(R.string.cp_whetherAddAddressBook)
						.setIcon(android.R.drawable.ic_dialog_info)
						.setPositiveButton(R.string.cp_immediatelyAdd, new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which)
							{

								Intent intent = new Intent(Composer.this, AddressBook.class);
								startActivity(intent);

							}
						}).setNegativeButton(R.string.cp_laterSay, new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which)
							{
								return;
							}
						});
		if (allAddr.size() == 0)
		{

			ad2.show();
		}
		else
		{
			ad.show();
		}
	}

	private void initAddressBookArray()
	{
		ArrayList<EmailAddress> addressArry = AddressBook.getEmailAddresses();
		// NewDbHelper.getInstance().loadAddressBook();
		if (addressArry.isEmpty())
			return;
		if (addressBookArray != null && addressBookArray.length == addressArry.size())
		{
			return;
		}

		int len = addressArry.size();
		addressBookArray = new String[len];
		for (int i = 0; i < len; i++)
		{
			EmailAddress ea = addressArry.get(i);
			addressBookArray[i] = ea.toString();
		}
	}

	private void loadAccountsToSpinner()
	{
		int acctCount = AccountManager.getCount();
		// if no account exists, then need create an account at first.
		if (acctCount < 1)
		{
			/*
			 * Intent intent = new Intent(this, MailClient.class);
			 * intent.putExtra(MailClient.CLIENT_OPEN_MODE,
			 * MailClient.OPEN_FROM_COMPOSE); startActivityForResult(intent,
			 * R.layout.main); return;
			 */
			if (AccountManager.getCount() == 0)
			{
				finish();
				return;
			}
		}

		String[] accoutNames = new String[acctCount];
		int curIdx = 0;
		Account curAccount = null;
		if (this.curMailInfo != null && curMailInfo.getAccountId() >= 0)
			curAccount = AccountManager.getAccount(curMailInfo.getAccountId());
		if (curAccount == null)
			curAccount = MyApp.currentAccount;
		if (curAccount == null)
			curAccount = AccountManager.getByIndex(0);
		for (int idx = 0; idx < acctCount; ++idx)
		{
			Account a = AccountManager.getByIndex(idx);
			accoutNames[idx] = a.name;
			if (curAccount.name.equals(a.name))
				curIdx = idx;
		}
		ArrayAdapter<String> spinAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
																	accoutNames);
		spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		_fromSpinner.setAdapter(spinAdapter);
		_fromSpinner.setSelection(curIdx);
	}

	@Override
	public void onActivityResult(int reqCode, int rstCode, Intent intent)
	{
		MyApp.setCurrentActivity(this);
		// create account after MailClient returned.
	  switch(reqCode){
	  case REQ_SEND_MMS:
//	  		if(!dataDisconnected())
//	  		{
//	  			sendMMS();
//	  			sendMail();
//	  		}
	  		break;
	  case R.layout.main:
		{
			if (AccountManager.getCount() == 0)// do not add any accounts,
													// finish this activity.
			{
				finish();
				return;
			}
			initMailInformation();
			loadAccountsToSpinner();
		}
	case REQ_FILE_BROWSER:
	{
		if (rstCode != Dialog.BUTTON_POSITIVE)
			return;
		addAttachment(intent.getStringExtra("selectedFile"));
	}
		break;
	case REQ_TAKE_PICTURE:

		if (rstCode == RESULT_OK)
		{
			// int i=1;
			// Calendar cal = Calendar.getInstance();
			// Date curDate = cal.getTime();
			BitmapFactory.Options options=new BitmapFactory.Options();
			options.inSampleSize=2;
			Bitmap bitmap=BitmapFactory.decodeFile(cameraFileName, options);
			FileOutputStream fos = null;    
	          try {    
	              fos = new FileOutputStream(cameraFileName);    
	              if (null != fos)    
	              {    
	            	  bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);    
	                  fos.flush();    
	                  fos.close();  
	                  bitmap.recycle();
	              }    
	          } catch (Exception e) {    
	        	  Log.d(Utils.LOGTAG, "",e);  
	          }    
			Intent cj1 = new Intent("com.android.camera.action.CROP");

			try
			{

				cj1.setData(Uri.parse(android.provider.MediaStore.Images.Media.
						insertImage(getContentResolver(),cameraFileName, null, null)));

			}
			catch (FileNotFoundException e)
			{

				Log.d(Utils.LOGTAG, "",e);

			}

			cj1.putExtra("crop", "true");

			cj1.putExtra("outputX", 384);

			cj1.putExtra("outputY", 256);
			
			cj1.putExtra("aspectX",3);
			cj1.putExtra("aspectY",2);

			cj1.putExtra("return-data", true);

			startActivityForResult(cj1, REQ_CROP);

			break;

		}

		break;
	case REQ_CROP:
		if(rstCode==RESULT_OK)
		{
			Bundle extras = intent.getExtras();  
			if(extras != null ) {  
			    Bitmap photo = extras.getParcelable("data");  
			    FileOutputStream stream;
				try {
					stream = new FileOutputStream(cameraFileName);
				photo.compress(Bitmap.CompressFormat.JPEG, 75, stream);
				} catch (FileNotFoundException e) {
					Log.d(Utils.LOGTAG, "",e);
				}  
			      
			        
			}  
			addAttachment(cameraFileName );
		}
		break;
	case REQ_SOUND_RECORDER:
		if (rstCode == RESULT_OK)
		{
			// recordFilePath = "/adcard/audio/note/";
			// folderScan(recordFilePath);
			Cursor audioCursor = managedQuery(	intent.getData(), null,
												null,
												null, null);
			audioCursor.moveToFirst();
			String audioFileName = audioCursor.getString(1);
			audioCursor.close();
			addAttachment(audioFileName);
		}
		break;
	default:
		break;
	  }
	
	}
    @Override
    protected void onNewIntent(Intent intent) 
    {
    	setIntent(intent);
    };

    private boolean checkAddressValid()
	{
		Resources res = getResources();
		String toString = _toEditText.getText().toString();
		String ccString = _ccEditText.getText().toString();
		String bccString = _bccEditText.getText().toString();
		if (Utils.isEmpty(toString) && Utils.isEmpty(ccString) && Utils.isEmpty(bccString))
		{
			DialogUtils.showMsgBox(this, res.getString(R.string.cp_specifyAddr), res.getString(R.string.error));
			return false;
		}
		String[] lists = new String[] { toString, ccString, bccString };
		for (String s : lists)
		{
			if (!Utils.isEmpty(s))
			{
				String[] toList = s.split(";");
				for (int i = 0; i < toList.length; i++)
				{
					String addr = toList[i].trim();
					if (Utils.isEmpty(addr))
						continue;
					if (!doCheckAddressValid(addr)&&getPhoneNumber(addr)==null)
					{
						DialogUtils.showMsgBox(	this, res.getString(R.string.cp_invalidAddr) + toList[i],
												res.getString(R.string.error));
						return false;
					}
				}
			}

		}
		return true;
	}
    /**
     * 
     * @param address
     * @return 
     *
     */
    private String getPhoneNumber(String address)
    {
    	if(address!=null)
    	{
	    	int posLT = address.indexOf('<');
			int posGT = address.indexOf('>');
			if (posLT > 0 && posGT > 0)
			{
				address = address.substring(posLT + 1, posGT).trim();
			}
			else
			{
			    address=address.trim();
			}
			if(AddressBook.patternPhoneNumber(address))
				return address;
    	}
		return null;
    }

	private boolean doCheckAddressValid(String addr)
	{

		EmailAddress address = new EmailAddress(addr);
		return address.isValid();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			Builder ad = new AlertDialog.Builder(this).setTitle(R.string.cp_isSaveDrafts)
							.setIcon(android.R.drawable.ic_dialog_alert)
							.setPositiveButton(R.string.cp_saveAndExit, new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which)
								{

									updateMailInfo();
									curMailInfo.setFolder(FolderNames.FOLDER_DRAFT);
									NewDbHelper.getInstance().saveOutMail(curMailInfo);
									Resources res = getResources();
									if (curMailInfo.getUidx() < 0)
									{
										Log.d(LOGTAG, "Save mail failed" + "");
										DialogUtils.showMsgBox(	Composer.this, res.getString(R.string.cp_saveMailFail),
																res.getString(R.string.error));
									}
									else
									{
										Log.d(LOGTAG, "Save mail sucessfully" + "");
										Toast.makeText(Composer.this, res.getString(R.string.cp_saveMailSuccess),
														Toast.LENGTH_LONG).show();
									}
									finish();
								}
							}).setNeutralButton(R.string.cp_ReturnTo, new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which)
								{
									return;
								}
							});
			// contentStateModif=((OutBoxActivity)
			// outBoxActivity).getContentModif();

			if (contentStateModif == false)
			{
				finish();
			}
			else
			{
				ad.show();
			}
			return true;

		}
		return super.onKeyDown(keyCode, event);
	}
    
	@Override
	protected void onResume() {
		super.onResume();
		if(curMailInfo!=null)
		{
			if (curMailInfo instanceof OutMailInfo && curMailInfo.getUid() != null
				&& !curMailInfo.getUid().trim().equals(""))
			{
				
				_fromSpinner.setEnabled(false);
				_fromSpinner.setBackgroundResource(R.drawable.spinner_sytle_serverset_bg_disabled);
				_fromEditable = false;
			}
			else
			{
				_fromSpinner.setEnabled(true);
				_fromSpinner.setBackgroundResource(R.drawable.spinner_sytle_bg);
				_fromEditable = true;
			}
		}	
		
	}
    
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
//		if(_attachments!=null)
//		{
//			for(int i=0;i<curMailInfo._attachments.size();i++)
//			{
//				curMailInfo._attachments.remove(i);
//			}
//		}
		setAttachmentHeight();
	
	}

	private void sendMMS(MMSInfo mms, final MMSSender sender)
	{
		final MMSInfo mmsInfo = mms;
		final List<String> list = sender.getSimMNC(Composer.this);
		final Handler myHandler = new Handler();
		Thread t = new Thread(new Runnable() {
			@Override
			public void run()
			{
				// 因为在切换apn过程中需要一定时间，所以需要加上一个重试操作
				int retry = 0;
				do
				{
					Log.d("发送彩信", "第" + (retry + 1)+"次发彩信");
					try
					{
						if (sender.sendMMS(list, Composer.this, mmsInfo.getMMSBytes()))
						{
							myHandler.post(new Runnable() {

								@Override
								public void run()
								{
									Toast.makeText(getApplicationContext(), "彩信发送成功！", Toast.LENGTH_LONG)
											.show();
								}
							});
							return;
						}
						retry++;
						Thread.sleep(2000);
					}
					catch (Exception e)
					{
						Log.d(Utils.LOGTAG, "",e);
					}
				} while (retry < 5);
				myHandler.post(new Runnable() {
					@Override
					public void run()
					{
						Toast.makeText(getApplicationContext(), "彩信发送失败！", Toast.LENGTH_LONG).show();
					}
				});
			}
		});
		t.start();
	}
    private void deletePhoneInfo(ArrayList<String> phoneInEditText)
    {
    	// 删除edittext中的电话号码信息
		for (int i = 0; i < phoneInEditText.size(); i++)
		{
			String phone = phoneInEditText.get(i);
			if (!Utils.isEmpty(_toEditText.getText().toString()))
			{
				String old_toString = _toEditText.getText().toString();
				if (old_toString.contains(phone))
				{

					int startPos = old_toString.indexOf(phone);
					int endPos = startPos + phone.length();
					if (endPos < old_toString.length()
						&& old_toString.charAt(endPos) == ';')
						endPos += 1;
					StringBuffer old = new StringBuffer(old_toString);
					StringBuffer to = old.delete(startPos, endPos);
					_toEditText.setText(to);
				}
			}
			if (!Utils.isEmpty(_ccEditText.getText().toString()))
			{
				String old_ccString = _ccEditText.getText().toString();
				if (old_ccString.contains(phone))
				{
					int startPos = old_ccString.indexOf(phone);
					int endPos = startPos + phone.length();
					if (endPos < old_ccString.length()
						&& old_ccString.charAt(endPos) == ';')
						endPos += 1;
					StringBuffer old = new StringBuffer(old_ccString);
					StringBuffer to = old.delete(startPos, endPos);
					_ccEditText.setText(to);
				}
			}
			if (!Utils.isEmpty(_bccEditText.getText().toString()))
			{
				String old_bccString = _bccEditText.getText().toString();
				if (old_bccString.contains(phone))
				{
					int startPos = old_bccString.indexOf(phone);
					int endPos = startPos + phone.length();
					if (endPos < old_bccString.length()
						&& old_bccString.charAt(endPos) == ';')
						endPos += 1;
					StringBuffer old = new StringBuffer(old_bccString);
					StringBuffer to = old.delete(startPos, endPos);
					_bccEditText.setText(to);
				}
			}
		}
	}
   private void sendMMS()
   {
	   MMSSender sender = new MMSSender();
	   sender.shouldChangeApn(Composer.this);
	   ConnectivityManager conManager = (ConnectivityManager) this
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = conManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		String currentAPN = info.getExtraInfo();
		conManager.startUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE, "mms");
		currentAPN = info.getExtraInfo();
		// 只有CMWAP才能发送彩信
		if ("cmwap".equals(currentAPN) || "3gwap".equals(currentAPN)
			|| "uniwap".equals(currentAPN) || "ctwap".equals(currentAPN))
		{
			String bodyText = _contentText.getText().toString();
			StringBuffer phone = new StringBuffer();
			for (String phoneNum : phoneNumbers)
			{

				phone.append(phoneNum + ";");

			}
			if (phone.length() != 0)
				phone.deleteCharAt(phone.lastIndexOf(";"));
			MMSInfo mms = new MMSInfo(this, phone.toString());// 发送的手机号
			mms.setSubject(_subjectText.getText().toString());
			mms.addPart("text", bodyText);
			if (curMailInfo.hasNormalAttachment())
			{
				for (int i = 0; i < curMailInfo.getAttachments().size(); i++)
				{
					AttachmentInfo attach = curMailInfo.getAttachments().get(i);
					if (attach.fileName.endsWith(".png")
						|| attach.fileName.endsWith(".jpg"))
						mms.addPart("image", attach.fullFilePath);
					else if (attach.fileName.endsWith(".amr"))
						mms.addPart("audio", attach.fullFilePath);
				}
			}
			sendMMS(mms,sender);
		}
   }
   private boolean dataDisconnected()
   {
	   TelephonyManager telephonyManager = (TelephonyManager) this
				.getSystemService(Context.TELEPHONY_SERVICE);
	   return telephonyManager.getDataState() == TelephonyManager.DATA_DISCONNECTED;
   }
	private String getAddrs(String toEditTextString)
	{
		String[] toAddrs = toEditTextString.split(";");
		StringBuffer toAddrString =new StringBuffer();
		for (int i = 0; i < toAddrs.length; i++)
			if (doCheckAddressValid(toAddrs[i]))
				toAddrString.append(toAddrs[i]+";");
		if(toAddrString.length()!=0)
			toAddrString.deleteCharAt(toAddrString.lastIndexOf(";"));
		return toAddrString.toString();
		
	}
}

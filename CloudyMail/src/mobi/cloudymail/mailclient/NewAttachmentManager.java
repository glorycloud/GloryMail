package mobi.cloudymail.mailclient;

import static mobi.cloudymail.util.Utils.LOGTAG;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import mobi.cloudymail.data.MailInfo;
import mobi.cloudymail.data.MailStatus;
import mobi.cloudymail.mailclient.net.Account;
import mobi.cloudymail.mailclient.net.AttachmentInfo;
import mobi.cloudymail.mailclient.net.DownloadAttaTask;
import mobi.cloudymail.util.MessageBox;
import mobi.cloudymail.util.MyApp;
import mobi.cloudymail.util.NewDbHelper;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

public class NewAttachmentManager extends ListActivity 
					implements android.view.View.OnClickListener,
					CompoundButton.OnCheckedChangeListener,
					OnItemClickListener
{
	private ListView _attListView=null;
	private List<AttachmentInfo> _attachInfos=new ArrayList<AttachmentInfo>();
	private AttachmentListAdapter _adapter=null;
	private LinearLayout _botBtnLayout=null;
	private Button _openBtn=null;
	private Button _downloadBtn = null;
	private Button _openMailBtn = null;
	private int _curSelectedPos = -1;
	
	private final String[][] MIME_MapTable={ 
	                                        //{suffix£¬MIME type} 
	                                        {".3gp",    "video/3gpp"}, 
	                                        {".apk",    "application/vnd.android.package-archive"}, 
	                                        {".asf",    "video/x-ms-asf"}, 
	                                        {".avi",    "video/x-msvideo"}, 
	                                        {".bin",    "application/octet-stream"}, 
	                                        {".bmp",    "image/bmp"}, 
	                                        {".c",  "text/plain"}, 
	                                        {".class",  "application/octet-stream"}, 
	                                        {".conf",   "text/plain"}, 
	                                        {".cpp",    "text/plain"}, 
	                                        {".doc",    "application/msword"}, 
	                                        {".docx",   "application/vnd.openxmlformats-officedocument.wordprocessingml.document"}, 
	                                        {".xls",    "application/vnd.ms-excel"},  
	                                        {".xlsx",   "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"}, 
	                                        {".exe",    "application/octet-stream"}, 
	                                        {".gif",    "image/gif"}, 
	                                        {".gtar",   "application/x-gtar"}, 
	                                        {".gz", "application/x-gzip"}, 
	                                        {".h",  "text/plain"}, 
	                                        {".htm",    "text/html"}, 
	                                        {".html",   "text/html"}, 
	                                        {".jar",    "application/java-archive"}, 
	                                        {".java",   "text/plain"}, 
	                                        {".jpeg",   "image/jpeg"}, 
	                                        {".jpg",    "image/jpeg"}, 
	                                        {".js", "application/x-javascript"}, 
	                                        {".log",    "text/plain"}, 
	                                        {".m3u",    "audio/x-mpegurl"}, 
	                                        {".m4a",    "audio/mp4a-latm"}, 
	                                        {".m4b",    "audio/mp4a-latm"}, 
	                                        {".m4p",    "audio/mp4a-latm"}, 
	                                        {".m4u",    "video/vnd.mpegurl"}, 
	                                        {".m4v",    "video/x-m4v"},  
	                                        {".mov",    "video/quicktime"}, 
	                                        {".mp2",    "audio/x-mpeg"}, 
	                                        {".mp3",    "audio/x-mpeg"}, 
	                                        {".mp4",    "video/mp4"}, 
	                                        {".mpc",    "application/vnd.mpohun.certificate"},        
	                                        {".mpe",    "video/mpeg"},   
	                                        {".mpeg",   "video/mpeg"},   
	                                        {".mpg",    "video/mpeg"},   
	                                        {".mpg4",   "video/mp4"},    
	                                        {".mpga",   "audio/mpeg"}, 
	                                        {".msg",    "application/vnd.ms-outlook"}, 
	                                        {".ogg",    "audio/ogg"}, 
	                                        {".pdf",    "application/pdf"}, 
	                                        {".png",    "image/png"}, 
	                                        {".pps",    "application/vnd.ms-powerpoint"}, 
	                                        {".ppt",    "application/vnd.ms-powerpoint"}, 
	                                        {".pptx",   "application/vnd.openxmlformats-officedocument.presentationml.presentation"}, 
	                                        {".prop",   "text/plain"}, 
	                                        {".rc", "text/plain"}, 
	                                        {".rmvb",   "audio/x-pn-realaudio"}, 
	                                        {".rtf",    "application/rtf"}, 
	                                        {".sh", "text/plain"}, 
	                                        {".tar",    "application/x-tar"},    
	                                        {".tgz",    "application/x-compressed"},  
	                                        {".txt",    "text/plain"}, 
	                                        {".wav",    "audio/x-wav"}, 
	                                        {".wma",    "audio/x-ms-wma"}, 
	                                        {".wmv",    "audio/x-ms-wmv"}, 
	                                        {".wps",    "application/vnd.ms-works"}, 
	                                        {".xml",    "text/plain"}, 
	                                        {".z",  "application/x-compress"}, 
	                                        {".zip",    "application/x-zip-compressed"}, 
	                                        {".ics",	"text/plain"},
	                                        {"",        "*/*"}   
	                                    };
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		MyApp.setCurrentActivity(this);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.new_attachment_list);
		//win.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.cloudymail);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.attachment_manager_titlebar);
		
		_attListView = getListView();
		_botBtnLayout = (LinearLayout)findViewById(R.id.newAttachOpLayout);
		_openBtn = (Button)findViewById(R.id.newOpenAttachBtn);
		_downloadBtn = (Button)findViewById(R.id.newDownladAttachBtn);
		_openMailBtn = (Button)findViewById(R.id.openMailBtn);
		_openBtn.setOnClickListener(this);
		_downloadBtn.setOnClickListener(this);
		_openMailBtn.setOnClickListener(this);
		_botBtnLayout.setVisibility(View.GONE);
		_attListView.setOnItemClickListener(this);
	}
	
	@Override
	protected void onStart()
	{
		super.onStart();
		updateAttachList();
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		MyApp.setCurrentActivity(this);
	}
	
	private void updateAttachList()
	{
		_attachInfos = NewDbHelper.getInstance().getAttachmentInfo("");
		if(_adapter == null)
		{
			_adapter = new AttachmentListAdapter(_attachInfos,this);
			_attListView.setAdapter(_adapter);
		}
		else
			_adapter.setAttachList(_attachInfos);
	}
	
	
	static class AttachmentListAdapter extends BaseAdapter
	{
		private List<AttachmentInfo> _attachInfos;
	//	private MailFolderActivity _ctx;
		private NewAttachmentManager _ctx;
		private List<RadioButton> _radBtns = new LinkedList<RadioButton>();
		private int curPos=-1;

		static class ViewHolder
		{
			TextView fromTextView;
			TextView dateTextView;
			TextView attachTextView;
			TextView filePathView;
			RadioButton attaRadioBtn;
//			Button downloadBtn;
//			Button openBtn;
//			Button previewBtn;
		}

		public AttachmentListAdapter(List<AttachmentInfo> attachInfos,
				NewAttachmentManager ctx)
		{
			_attachInfos = attachInfos;
			_ctx = ctx;
		}

		public void setAttachList(List<AttachmentInfo> attachInfos)
		{
			_attachInfos = attachInfos;
			notifyDataSetChanged();
		}

		@Override
		public int getCount()
		{
			return _attachInfos.size();
		}

		@Override
		public Object getItem(int position)
		{
			return _attachInfos.get(position);
		}

		@Override
		public long getItemId(int position)
		{
			return position;
		}

		@Override
		public int getItemViewType(int position)
		{
			/*MailInfo mail = _ctx.getMail(position);
			if ((mail.getState() & MailStatus.FLAG_HAS_MORE_PLACEHOLD) != 0)
			{
				return 1; // this is a place holder for display more mails
			}*/
			return 0;
		}

		@Override
		public int getViewTypeCount()
		{
			return 1;//2;
		}

		@Override
		public View getView(final int position, View convertView,
				ViewGroup parent)
		{
			AttachmentInfo attaInfo = _attachInfos.get(position);

			// else, this is a normal mail

			// A ViewHolder keeps references to children views to avoid
			// unnecessary calls to findViewById() on each row.
			final ViewHolder holder;

			if (convertView == null)
			{
				convertView = _ctx.getLayoutInflater()
						.inflate(R.layout.new_attachment_list_item, null);
				holder = new ViewHolder();
				holder.attachTextView = (TextView) convertView
						.findViewById(R.id.newAttachInfoText);
				holder.filePathView = (TextView) convertView
						.findViewById(R.id.newAttachPath);
				holder.fromTextView = (TextView) convertView
						.findViewById(R.id.newAttachFromTxt);
				holder.dateTextView = (TextView) convertView
						.findViewById(R.id.newAttachdateTxt);
				holder.attaRadioBtn = (RadioButton) convertView
						.findViewById(R.id.newAttachRadio);
				_radBtns.add(holder.attaRadioBtn);
				holder.attaRadioBtn.setOnCheckedChangeListener(_ctx);
				convertView.setPadding(0, 2, 0, 2);
				convertView.setTag(holder);
				convertView.setBackgroundResource(R.drawable.mail_unread_bg);	
			}
			else
			{
				// Get the ViewHolder back to get fast access to the TextView
				// and the ImageView.
				holder = (ViewHolder) convertView.getTag();
			}
			holder.attachTextView.setText(attaInfo.fileName+"("+attaInfo.size+")");
			if(attaInfo.isFilePathValid())
			{
				holder.filePathView.setText(attaInfo.fullFilePath);
				holder.filePathView.setVisibility(View.VISIBLE);
			}
			else
			{
				holder.filePathView.setText("");
				holder.filePathView.setVisibility(View.GONE);
			}
			holder.fromTextView.setText(new EmailAddress(attaInfo.getMailInfo().getFrom()).getName());
			holder.dateTextView.setText(attaInfo.getMailDate().toLocaleString());
			holder.attaRadioBtn.setTag(position);
			holder.attaRadioBtn.setOnCheckedChangeListener(null);
			holder.attaRadioBtn.setChecked(position==curPos);
			holder.attaRadioBtn.setOnCheckedChangeListener(_ctx);
			return convertView;
		}
		
		public void updateRadBtns(int selectedPos)
		{
			curPos = selectedPos;
			for ( RadioButton btn: _radBtns)
			{
				int pos = (Integer)btn.getTag();
				btn.setChecked(pos==selectedPos);
			}
		}
	}


	@Override
	public void onClick(View view)
	{
		switch(view.getId())
		{
		case R.id.newOpenAttachBtn:
			openAttachment();
			break;
		case R.id.newDownladAttachBtn:
			downloadAttachment();
			break;
		case R.id.openMailBtn:
			openMail();
			break;
		}	
	}
	
	private void previewAttachment(int position)
	{
		if (position < 0 || position >= _attachInfos.size())
			return;
		AttachmentInfo attach = _attachInfos.get(position);
	    if(!attach.canPreview)
	    {
	    	MessageBox.show(this,getString(R.string.previewInfo) , getString(R.string.previewTitle));
	    	return;
	    }
//		MyApp.curMailInfo = attach.getMailInfo();
		MyApp.setCurrentMailInfo(attach.getMailInfo());
		if (MyApp.getAgent(	AccountManager.getAccount(MyApp.curMailInfo
									.getAccountId())).getSessionId(true,true,true) == null)
		{
			MessageBox.show(this, "Internal error, need login", "Error");
			return;
		}		
		Intent intent = new Intent(this, AttachmentViewer.class);
		intent.putExtra(AttachmentViewer.ATTACHMENT_INFO, (Parcelable)attach);
		startActivity(intent);
	}
	
	private void openAttachment()
	{
		if (_curSelectedPos < 0 || _curSelectedPos >= _attachInfos.size())
			return;
		AttachmentInfo attach = _attachInfos.get(_curSelectedPos);
		if (!attach.isFilePathValid())
			return;
		File file = new File(attach.fullFilePath);
		Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setAction(android.content.Intent.ACTION_VIEW);

		String type = getMIMEType(file);
		intent.setDataAndType(Uri.fromFile(file), type);
		try
		{
			startActivity(intent);
		}
		catch(ActivityNotFoundException ex)
		{
			type = "*/*";
			intent.setDataAndType(Uri.fromFile(file), type);
			startActivity(intent);
		}
	}
	
	private void downloadAttachment()
	{
		if (!MailViewer.isSdCardAvailable(this))
			return;
		System.out.println("start downloadAttachment()");
		if (_curSelectedPos < 0 || _curSelectedPos >= _attachInfos.size())
			return;
		AttachmentInfo atta = _attachInfos.get(_curSelectedPos);
		// update current mailinfo
//		MyApp.curMailInfo = atta.getMailInfo();
		MyApp.setCurrentMailInfo(atta.getMailInfo());
		String sdcFilePath = MailViewer
				.getAvailableFilePathToSave(atta.fileName);
		
		Handler mHandler = new Handler(){
			public void handleMessage(Message msg) 
			{ 
		//		System.out.println("handleMessage "+msg.what);
				_adapter.notifyDataSetChanged();
				doOnCheckedChanged();
			}
		};
		DownloadAttaTask downloadTask = new DownloadAttaTask(atta, null, false);
		downloadTask.setHandler(mHandler);
		downloadTask.execute(sdcFilePath);
	}

	private String getMIMEType(File f)
	{
		String type = "*/*";
		String fName = f.getName();
		int idx = fName.lastIndexOf(".");
		if(idx < 1)
			return type;
		String end = fName.substring(idx, fName.length()).toLowerCase();

		if(end.equals(""))
			return type;
		//get the mime type based on file suffix
	    for(int i=0;i<MIME_MapTable.length;i++)
	    { 
	        if(end.equals(MIME_MapTable[i][0]))
	            return MIME_MapTable[i][1];
	    }        
	    return type;
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
	{
		if(!isChecked)
			return;
		_curSelectedPos = (Integer) buttonView.getTag();
		doOnCheckedChanged();
	}
	
	private void doOnCheckedChanged()
	{
		//update the layout of bottom layout.
		AttachmentInfo attach = _attachInfos.get(_curSelectedPos);
		//check whether this attachment has been download before.
		if(attach.isFilePathValid())
		{
			_downloadBtn.setVisibility(View.GONE);
			_openBtn.setVisibility(View.VISIBLE);
		}
		else
		{
			_downloadBtn.setVisibility(View.VISIBLE);
			_openBtn.setVisibility(View.GONE);
		}
		//_openMailBtn.setVisibility(attach.canPreview?View.VISIBLE:View.GONE);
		_botBtnLayout.setVisibility(View.VISIBLE);
		//update selection status of radio button.
		_adapter.updateRadBtns(_curSelectedPos);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		previewAttachment(position);
		
	}
	private void openMail()
	{
		// TODO Auto-generated method stub
		if(_curSelectedPos < 0 || _curSelectedPos >= _attachInfos.size())
			return;
		//update current mail info.
		MailInfo mailInfo = _attachInfos.get(_curSelectedPos).getMailInfo();
		Account acct = AccountManager.getAccount(mailInfo.getAccountId());
		if (mailInfo.getState() != MailStatus.MAIL_READED)
		{
			String sid = MyApp.getAgent(acct).getSessionId(true,true,true); //Is Bug? current account may not the account this mail
															  //No, in GlobalInBoxActivity.beforeOpenMail, currentAccount has been switched
			if (sid == null)
			{
				Log.d(LOGTAG,"openMail:"+ "session id is null");
				return;
			}
		}
//		MyApp.curMailInfo = mailInfo;
		MyApp.setCurrentMailInfo(mailInfo);
		Intent intent = new Intent(this, MailViewer.class);
		intent.putExtra(MailViewer.PREVIOUS_MAIL,false);
		intent.putExtra(MailViewer.NEXT_MAIL,false);
		intent.putExtra(MailViewer.DEL_MAIL,false);
		startActivityForResult(intent, R.layout.mail_viewer);
	}
	
	@Override
	protected void onActivityResult(int reqCode, int rstCode, Intent intent)
	{
		MyApp.setCurrentActivity(this);
		switch (reqCode)
		{
		case R.layout.mail_viewer:
		{
			if (MyApp.curMailInfo.getState() == MailStatus.MAIL_NEW)
			{
				String cmd = "update mail set state=? where accountId=? and uid=? and folder='"+MyApp.curMailInfo.getFolder()+"'";
				String[] args = { MailStatus.MAIL_READED + "",
									MyApp.currentAccount.id + "",
									MyApp.curMailInfo.getUid() };
				NewDbHelper.getInstance().execSQL(cmd, args);
				MyApp.curMailInfo.setState(MailStatus.MAIL_READED);
//				MyApp.curMailInfo = null;
				MyApp.setCurrentMailInfo(null);
			}
		}
			break;
		default:
			break;
		}
	}
}

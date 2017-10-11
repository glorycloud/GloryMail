package mobi.cloudymail.mailclient;

import java.io.IOException;
import java.util.Locale;

import mobi.cloudymail.mailclient.net.ServerAgent;
import mobi.cloudymail.util.MessageBox;
import mobi.cloudymail.util.Utils;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

public class FeedbackActivity extends BaseActivity
{

	private String softwareVersion;
	private String sdkVersion;
	private String phoneModel;
	private String netType;
	private String languageEnvironment;
	private OnClickListener confimBtnListener;
	private OnClickListener cancelBtnListener;
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.feedback_account);
		//win.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.cloudymail);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.feedback_titlebar);
		
		
	    final EditText feedbackInfoEdit = (EditText) findViewById(R.id.feedbackInfo);
	    Button   confimBtn=(Button) findViewById(R.id.confirmFeedbackBtn);
	    Button   cancelBtn=(Button) findViewById(R.id.cancleFeedbackBtn);
	    
	    //set focus
	    feedbackInfoEdit.requestFocus();
	    softwareVersion=getString(R.string.about_version);
	    sdkVersion=android.os.Build.VERSION.RELEASE;
	    phoneModel=android.os.Build.MODEL.toString();
	    
	    if(this.getNetType()==null)
	    {
	    	netType=getString(R.string.no_network_connection);
	    }
	    else
	    {
	    	netType=this.getNetType().toString();
	    }
	    
	    languageEnvironment=Locale.getDefault().getCountry()+Locale.getDefault().getLanguage();
		String configurationInfor = getString(R.string.feedback_software_version)+" " + softwareVersion + "\n"+
		                            getString(R.string.feedback_sdk_version)+" " + sdkVersion + "\n"+
		                            getString(R.string.feedback_phone_Model)+" " + phoneModel + "\n"+
		                            getString(R.string.feedback_network_type)+" " + netType + "\n"+
		                            getString(R.string.feedback_language_Environment)+" " + languageEnvironment+"\n"+"\n"+
		                            getString(R.string.feedback_my_recommend)+"\n";
		feedbackInfoEdit.setText(configurationInfor);
		//Set initial cursor position
	    feedbackInfoEdit.setSelection(feedbackInfoEdit.getText().length());
	    
	    if(confimBtnListener==null)
	    	
	    	confimBtnListener=new View.OnClickListener() {
				@Override
				public void onClick(View v)
				{
		           Thread feedbackThread=new Thread()
		           {
		        	  @Override
		        	  public void run()
		        	  {
		        		  
		        		try
						{
							HttpPost feedbackRequest=new HttpPost(ServerAgent.getUrlBase()+"/FeedBack");
							StringEntity ent = new StringEntity(feedbackInfoEdit.getText().toString(), "UTF-8");
							feedbackRequest.setEntity(ent );
							try
							{
								ServerAgent.execute(feedbackRequest);
							}
							catch (IOException e)
							{
								Log.d(Utils.LOGTAG, "",e);
							}
						}
						catch (Exception e)
						{
							Log.d(Utils.LOGTAG, "",e);
						}
		        		  
		        	  }
		           };
		           MessageBox.show(FeedbackActivity.this, getString(R.string.feedback_msg), getString(R.string.feedback_msg_title));
                   feedbackThread.start();	
                   finish();
				}
				
			};
	    
	    confimBtn.setOnClickListener(confimBtnListener);
	    
	    if(cancelBtnListener==null)
	    {
	    	cancelBtnListener=new View.OnClickListener() {
				
				@Override
				public void onClick(View v)
				{
                   finish();					
				}
			};
	    }
	    cancelBtn.setOnClickListener(cancelBtnListener);
	}
	/**
	 * Get network type
	 * @return netWorkType
	 */
	public String getNetType()
	{
	    ConnectivityManager cm=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
	    //nullPointException
	    if(activeNetworkInfo==null)
	    {
	    	return null;
	    }
	    else
	    {
	    	return activeNetworkInfo.getTypeName()+"["+activeNetworkInfo.getSubtypeName()+"]";
	    }
	}
}

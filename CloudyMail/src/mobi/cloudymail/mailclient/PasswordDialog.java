package mobi.cloudymail.mailclient;

import mobi.cloudymail.util.DialogResult;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class PasswordDialog extends Dialog
{
	int dialogResult;
	Handler mHandler ;
	//public static PasswordDialog instence=null;
	public PasswordDialog(Context context, String mailName, boolean retry)
	{
		
		super(context);
//		setOwnerActivity(context);
		onCreate();
		TextView promptLbl = (TextView) findViewById(R.id.promptLbl);
		if(retry)
			promptLbl.setText(context.getResources().getString(R.string.reinputPassword)+ mailName);
		else
			promptLbl.setText(context.getResources().getString(R.string.inputPassword)+ mailName);

	}

	public String getPassword()
	{
		return ((EditText)findViewById(R.id.passwordTxt)).getText().toString();
	}
	
	public boolean needSavePassword()
	{
		return ((CheckBox)findViewById(R.id.savePasswordChk)).isChecked();
	}
	public int getDialogResult()
	{
		return dialogResult;
	}
	public void setDialogResult(int dialogResult)
	{
		this.dialogResult = dialogResult;
	}
	/** Called when the activity is first created. */
	
	public void onCreate() {
		setContentView(R.layout.password_dialog);
		setOnKeyListener(new OnKeyListener(){
	          @Override
	          public boolean onKey(DialogInterface arg0, int arg1, KeyEvent arg2) {
	           if ((arg1 == KeyEvent.KEYCODE_ENTER && arg2.getAction() == KeyEvent.ACTION_DOWN ))
	           {  
	        	   endDialog(DialogResult.OK);
	           }
	           return false;
	          }
	          
	         });
		
		findViewById(R.id.cancelBtn).setOnClickListener(new android.view.View.OnClickListener() {
			
			@Override
			public void onClick(View paramView)
			{
				endDialog(DialogResult.CANCEL);
			}

			});
		findViewById(R.id.okBtn).setOnClickListener(new android.view.View.OnClickListener() {
			
			@Override
			public void onClick(View paramView)
			{
				endDialog(DialogResult.OK);
			}

			});
		findViewById(R.id.showPasswordChk).setOnClickListener(new android.view.View.OnClickListener() {
			
			@Override
			public void onClick(View paramView)
			{
				if(				((CheckBox)paramView).isChecked())
				{
					((EditText)findViewById(R.id.passwordTxt)).setTransformationMethod(null);
				}
				else
					((EditText)findViewById(R.id.passwordTxt)).setTransformationMethod(PasswordTransformationMethod.getInstance());
				
			}
		});
	}
	
	public void endDialog(int result)
	{
		setDialogResult(result);
		Message m = mHandler.obtainMessage();
		mHandler.sendMessage(m);
		dismiss();
	}
	
	public int showDialog()
	{
		mHandler = new Handler(Looper.myLooper()) {
			@Override
			public void handleMessage(Message mesg) {
				throw new RuntimeException();
              }
		};
		super.show();
		try {
			Looper.loop();
		}
		catch(Throwable e2)
		{
		}
		return dialogResult;
	}
	
}

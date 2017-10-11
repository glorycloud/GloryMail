package mobi.cloudymail.mailclient;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

public class TranslucentButton extends Activity {
	int m_nSreenHeight = 0;
	ImageButton m_menu1;
	private String refMailBody = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.translucent_button);
        
        Intent intent = getIntent();
        m_menu1 = (ImageButton)findViewById(R.id.last);
        boolean enableFlag = intent.getBooleanExtra(MailViewer.PREVIOUS_MAIL, true);
  //      m_menu1.setEnabled(enableFlag);
  //     m_menu1.setClickable(enableFlag);
        m_menu1.setVisibility(enableFlag?View.VISIBLE:View.GONE);
        ImageButton menu2 = (ImageButton)findViewById(R.id.replyMail);
        ImageButton menu3 = (ImageButton)findViewById(R.id.dseleteMail);
        enableFlag = intent.getBooleanExtra(MailViewer.DEL_MAIL, true);
  //      menu3.setEnabled(enableFlag);
  //      menu3.setClickable(enableFlag);
        menu3.setVisibility(enableFlag?View.VISIBLE:View.GONE);
       ImageButton menu4 = (ImageButton)findViewById(R.id.next);
       enableFlag = intent.getBooleanExtra(MailViewer.NEXT_MAIL, true);
  //     menu4.setEnabled(enableFlag);
  //     menu4.setClickable(enableFlag);
        menu4.setVisibility(enableFlag?View.VISIBLE:View.GONE);
        
        m_menu1.setOnClickListener(lastClickListener);
        menu2.setOnClickListener(replyMailClickListener);
        menu3.setOnClickListener(delMailClickListener);
        menu4.setOnClickListener(nextClickListener);
        
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        m_nSreenHeight = dm.heightPixels;
        
    }
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_MENU) {
    		this.finish();
    	}
    	return super.onKeyUp(keyCode, event);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	if (event.getX() < m_nSreenHeight - m_menu1.getHeight()) {
    		finish();
    	}
        return false;
    }
 /*   public void openComposer(int type)
	{
		Intent intent = new Intent(this,Composer.class);
		intent.putExtra("composer_type", type);
		if(type == Composer.COMPOSER_REPLYALL)
			intent.putExtra("ccList", MyApp.curMailInfo.getCc());
		//_htmlView.loadUrl("javascript:window.cmail.setMailBody(mailBody.innerHTML);");
		intent.putExtra("refMailBody", refMailBody);
		startActivity(intent);
	}*/
    private OnClickListener lastClickListener = new OnClickListener() {
    	public void onClick(View v) {
    		setResult(R.id.last);
    		finish();
    	}
    };
    private OnClickListener replyMailClickListener = new OnClickListener() {
    	public void onClick(View v) {
    		setResult(R.id.replyMail);
    		finish();
    	}
    };
    private OnClickListener delMailClickListener = new OnClickListener() {
    	public void onClick(View v) {
    		setResult(R.id.delMail);
    		finish();
    	}
    };
    private OnClickListener nextClickListener = new OnClickListener() {
    	public void onClick(View v) {
    	     setResult(R.id.next);
    	     finish();
    	}
    };
}
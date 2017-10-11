package mobi.cloudymail.mailclient;

import mobi.cloudymail.util.MyApp;
import mobi.cloudymail.util.Utils;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class WellComeActicity extends Activity implements OnViewChangeListener{
	
	//member for splash
	private ImageView startLogo;
	int alpha=255;
	int b=0;
	//end member of splash
	private MyScrollLayout mScrollLayout;
	private ImageView[] imgs;
	private int count;
	private int currentItem;
	private Button startBtn;
	private RelativeLayout mainRLayout;
	private LinearLayout pointLLayout;
	private LinearLayout bglayout;
	private LinearLayout leftLayout;
	private LinearLayout rightLayout;
	private LinearLayout animLayout;
	private final int SHOWANIMI=0;
	Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			
			if(msg.what==SHOWANIMI)
				startAnim();
			super.handleMessage(msg);
		}
	};
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences preferences=this.getSharedPreferences(MyApp.SHARED_SETTING, Context.MODE_APPEND);
        Editor edit=preferences.edit();
        boolean firstRun=preferences.getBoolean("firstRun", true);
        if(firstRun && Utils.isInChinese())
        {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.wellcome);
            initView();
        	
        }
        else
        	initAsSplash();
    	if(firstRun)
        	edit.putBoolean("firstRun", false).commit();
        
        
    }
    
	private void initView() {
		mScrollLayout  = (MyScrollLayout) findViewById(R.id.ScrollLayout);
		pointLLayout = (LinearLayout) findViewById(R.id.llayout);
		bglayout=(LinearLayout) findViewById(R.id.bglayout);
		mainRLayout = (RelativeLayout) findViewById(R.id.mainRLayout);
		startBtn = (Button) findViewById(R.id.startBtn);
		startBtn.setOnClickListener(onClick);
		animLayout = (LinearLayout) findViewById(R.id.animLayout);
		leftLayout  = (LinearLayout) findViewById(R.id.leftLayout);
		rightLayout  = (LinearLayout) findViewById(R.id.rightLayout);
		count = mScrollLayout.getChildCount();
		imgs = new ImageView[count];
		for(int i = 0; i< count;i++) {
			imgs[i] = (ImageView) pointLLayout.getChildAt(i);
			imgs[i].setEnabled(true);
			imgs[i].setTag(i);
		}
		currentItem = 0;
		imgs[currentItem].setEnabled(false);
		mScrollLayout.SetOnViewChangeListener(this);
	}
	
	private View.OnClickListener onClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.startBtn:
				mScrollLayout.setVisibility(View.GONE);
				pointLLayout.setVisibility(View.GONE);
				bglayout.setVisibility(View.VISIBLE);
//				imageBg.invalidate();
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							Thread.sleep(500);
							Message msg=new Message();
							msg.what=SHOWANIMI;
							mHandler.sendMessage(msg);
						} catch (InterruptedException e) {
							Log.d(Utils.LOGTAG, "",e);
						}

					}
				}).start();
				
				
				break;
			}
		}
	};
	private void startAnim() {
		bglayout.setVisibility(View.GONE);
		animLayout.setVisibility(View.VISIBLE);
//		mainRLayout.setBackgroundResource(R.drawable.whatsnew_bg);
		Animation leftOutAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.translate_left);
		Animation rightOutAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.translate_right);
//		Animation leftOutAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fadedout_to_left_down);
//		Animation rightOutAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fadedout_to_right_down);
		leftLayout.setAnimation(leftOutAnimation);
		rightLayout.setAnimation(rightOutAnimation);
		leftOutAnimation.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
//				mainRLayout.setBackgroundColor(R.color.black);
			}
			@Override
			public void onAnimationRepeat(Animation animation) {
			}
			@Override
			public void onAnimationEnd(Animation animation) {
				leftLayout.setVisibility(View.GONE);
				rightLayout.setVisibility(View.GONE);
				Intent intent = new Intent(WellComeActicity.this,GlobalInBoxActivity.class);
				WellComeActicity.this.startActivity(intent);
				WellComeActicity.this.finish();
				overridePendingTransition(R.anim.zoom_out_enter, R.anim.zoom_out_exit);
			}
		});
		
	}
	@Override
	public void OnViewChange(int position) {
		setcurrentPoint(position);
	}

	private void setcurrentPoint(int position) {
		if(position < 0 || position > count -1 || currentItem == position) {
			return;
		}
		imgs[currentItem].setEnabled(true);
		imgs[position].setEnabled(false);
		currentItem = position;
	}
	
	private void initAsSplash()
	{
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		     				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		     		requestWindowFeature(Window.FEATURE_NO_TITLE);
		             setContentView(R.layout.splash);
		     		startLogo = (ImageView) findViewById(R.id.startLogo);
		     		if(!Utils.isInChinese())
		     		{
		     			startLogo.setBackgroundResource(R.drawable.splash_en);
		     		}
		     		startLogo.setAlpha(alpha);

		     		new Thread(new Runnable() {

		     			@Override
		     			public void run() {
		     				while (b < 2) {
		     					try {
		     						if (b == 0) {
		     							Thread.sleep(200);
		     							b = 1;
		     						} else {
		     							Thread.sleep(35);
		     						}
		     						updateApp();
		     					} catch (Exception e) {
		     						Log.e(Utils.LOGTAG, "Error:", e);
		     					}
		     				}

		     			}

		     		}).start();

	}
	private void updateApp() {
		alpha -= 50;
		if (alpha <= 0) {
			b = 2;
			Intent in = new Intent(this, GlobalInBoxActivity.class);
			startActivity(in);
			WellComeActicity.this.finish();
		}
//		mHandler.sendMessage(mHandler.obtainMessage());
	}
}
package mobi.cloudymail.mailclient;

import mobi.cloudymail.util.Utils;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

public class SplashActivity extends Activity {
   private Handler mHandler=new Handler();
   private ImageView startLogo;
   int alpha=255;
   int b=0;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
						Log.d(Utils.LOGTAG, "",e);
					}
				}

			}

		}).start();

		mHandler = new Handler() {
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				startLogo.setAlpha(alpha);
				startLogo.invalidate();
			}
		};
	}

	private void updateApp() {
		alpha -= 50;
		if (alpha <= 0) {
			b = 2;
			Intent in = new Intent(this, GlobalInBoxActivity.class);
			startActivity(in);
			SplashActivity.this.finish();
		}
//		mHandler.sendMessage(mHandler.obtainMessage());
	}
}

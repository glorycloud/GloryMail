package mobi.cloudymail.mailclient;

import mobi.cloudymail.util.MyApp;
import android.app.Activity;
import android.os.Bundle;

public class BaseActivity extends Activity
{
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
		MyApp.setCurrentActivity(this);
	}
}

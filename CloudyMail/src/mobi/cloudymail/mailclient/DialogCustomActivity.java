package mobi.cloudymail.mailclient;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;

public class DialogCustomActivity extends AlertDialog {
    Context context;
	public DialogCustomActivity(Context context) {
		super(context);
        this.context=context;
	}
    public DialogCustomActivity(Context context,int theme)
    {
    	super(context,theme);
    	this.context=context;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_style);
    }
}

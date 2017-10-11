package mobi.cloudymail.mailclient;


import android.app.Dialog;
import android.os.Bundle;
import android.view.Window;

interface OnFileBrowserListener {
	public void onFileItemClick(String filename);
	public void onDirItemClick(String path);
}

public class FileBrowser extends BaseActivity implements OnFileBrowserListener {

	@Override
	public void onFileItemClick(String filename) {
	//	setTitle(filename);
		getIntent().putExtra("selectedFile", filename);
		setResult(Dialog.BUTTON_POSITIVE,getIntent());
		finish();
	}

	@Override
	public void onDirItemClick(String path) {
//		setTitle(path);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		Window win = getWindow();
		win.requestFeature(Window.FEATURE_LEFT_ICON);
		setContentView(R.layout.file_browser);
		FileBrowserView fileBrowser = (FileBrowserView)findViewById(R.id.filebrowser);
		fileBrowser.setOnFileBrowserListener(this);
		win.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.cloudymail);
	}
}
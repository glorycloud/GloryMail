package mobi.cloudymail.util;

import java.util.EnumSet;

import mobi.cloudymail.mailclient.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.view.KeyEvent;

public class DialogUtils
{
	public static enum ButtonFlags
	{
		OK, Cancel, Yes, No, Ignore
	};

	private static int result = 0;

	// show message box in blocking modal dialog
	public static int showModalMsgBox(final Context ctx, final String msg,
			final String title, final EnumSet<ButtonFlags> buttons)
			
	{
		final BreakbleMessageLoop myloop=new BreakbleMessageLoop(); ;

		Resources res = ctx.getResources();
		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setMessage(msg).setCancelable(true).setTitle(title);
		if (buttons.contains(ButtonFlags.OK))
			builder.setPositiveButton(	res.getString(R.string.ok),
										new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int id)
											{
												DialogUtils.result = DialogResult.OK;
												myloop.breakLoop();
												dialog.dismiss();
											}
										});
		if (buttons.contains(ButtonFlags.Cancel))
			builder.setNegativeButton(	res.getString(R.string.cancel),
										new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int id)
											{
												DialogUtils.result = DialogResult.CANCEL;
												myloop.breakLoop();
												dialog.dismiss();
											}
										});
		if (buttons.contains(ButtonFlags.Yes))
			builder.setPositiveButton(	res.getString(R.string.yes),
										new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int id)
											{
												DialogUtils.result = DialogResult.OK;
												myloop.breakLoop(); 
												//dialog.dismiss();
											}
										});
		if (buttons.contains(ButtonFlags.No))
			builder.setNegativeButton(	res.getString(R.string.no),
										new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int id)
											{
												DialogUtils.result = DialogResult.CANCEL;
												myloop.breakLoop();
												dialog.dismiss();
											}
										});
		builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
			
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event)
			{
				if(keyCode == KeyEvent.KEYCODE_BACK)
				{
					dialog.dismiss();
					DialogUtils.result = DialogResult.CANCEL;
					myloop.breakLoop();
					return true;
				}
				return false;
			}
		});
		AlertDialog alert = builder.create();
		alert.show();

		myloop.loop();
		return result;
	}

	// show message box in non-blocking modal dialog

	/**
	 * DialogUtils.showMsgBox( this,
	 *  "Failed to change account: "+exp.getMessage(),
	 * getResources().getString(R.string.error)); }
	 */

	public static void showMsgBox(final Context ctx, String msg, String title)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(
																ctx != null	? ctx
																			: MyApp.instance());
		builder.setMessage(msg)
				.setCancelable(true)
				.setTitle(title)
				.setPositiveButton(ctx.getResources().getString(R.string.ok),
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int id)
										{
											// returnStatus = LOGIN_FAIL;
										}
									});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	public static void showMsgBox(final Context ctx, String msg, String title, final Runnable callback)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(
																ctx != null	? ctx
																			: MyApp.instance());
		builder.setMessage(msg)
				.setCancelable(true)
				.setTitle(title)
				.setPositiveButton(ctx.getResources().getString(R.string.ok),
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int id)
										{
											callback.run();
										}
									});
		AlertDialog alert = builder.create();
		alert.show();
	}
	// show sort message box

}

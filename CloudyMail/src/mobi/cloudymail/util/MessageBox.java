package mobi.cloudymail.util;

import java.util.EnumSet;

import mobi.cloudymail.util.DialogUtils.ButtonFlags;
import android.content.Context;

public class MessageBox
{
	public static void show(Context ctx,String msg, String title)
	{
		try
		{
			DialogUtils.showModalMsgBox(ctx, msg, title, EnumSet.of(ButtonFlags.OK));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
}

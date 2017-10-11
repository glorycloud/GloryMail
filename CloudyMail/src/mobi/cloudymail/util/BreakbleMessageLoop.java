package mobi.cloudymail.util;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class BreakbleMessageLoop
{
	//public static Stack<BreakbleMessageLoop> loopStack = new Stack<BreakbleMessageLoop>();
	boolean breaked = false;
	static class LoopBreakException extends RuntimeException
	{
		public Message message;

		public LoopBreakException(Message msg)
		{
			this.message = msg;
		}
	}
	Handler mHandler = null;
	Runnable postRun = null;
	public void loop()
	{
		if(breaked == true)
			return;
		mHandler = new Handler(Looper.myLooper()) {
			@Override
			public void handleMessage(Message mesg)
			{
				// process incoming messages here
				// super.handleMessage(msg);
				throw new LoopBreakException(mesg);
			}
		};
		
		boolean rerun=false;
		do
		{
			rerun=false;
			try
			{
				Looper.loop();
			}
			catch (RuntimeException e2)
			{
				
			}
		}while(rerun);
		
	}
	
	public void breakLoop()
	{
		breaked = true;
		if(mHandler == null)
			return;
		Message msg = Message.obtain();
		
		msg.setTarget(mHandler);
//		int index = loopStack.indexOf(this);
//		if(index != loopStack.size()-1)
//		{
//			BreakbleMessageLoop innerLoop = loopStack.get(index+1);
//			innerLoop.postRun = new Runnable() {
//				
//				@Override
//				public void run()
//				{
//					
//					
//				}
//			};
//		}
		mHandler.sendMessage(msg);
	}
	
	
}

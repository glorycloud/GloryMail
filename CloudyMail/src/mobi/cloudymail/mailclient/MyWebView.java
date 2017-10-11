package mobi.cloudymail.mailclient;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;


public class MyWebView extends WebView 
{
	public MyWebView(Context ctx)
	{
		super(ctx);
	}
	
	public MyWebView(Context ctx, AttributeSet attrs)
	{
		super(ctx,attrs);
	}
	
	public MyWebView(Context ctx, AttributeSet attrs, int defStyle)
	{
		super(ctx,attrs,defStyle);
	}
	
	protected int computeVerticalScrollExtent ()
	{
		 float density = getResources().getDisplayMetrics().density;
		 return super.computeVerticalScrollExtent()+(int)(60*density);
	}
	
	protected int computeVerticalScrollRange() {
        float density = getResources().getDisplayMetrics().density;
        return super.computeVerticalScrollRange()+(int)(60*density);
   }
	
	public void	 onScrollChanged(int l, int t, int oldl, int oldt)
	{
		super.onScrollChanged(l, t, oldl, oldt);
		//Log.d(LOGTAG,"++++++++scroll changed+++++++"+ "horizon:"+l+", Vertical:"+t);
	}
}

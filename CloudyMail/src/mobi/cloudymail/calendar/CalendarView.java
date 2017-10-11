package mobi.cloudymail.calendar;

import mobi.cloudymail.mailclient.R;
import mobi.cloudymail.util.Utils;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;
import android.view.Window;
public class CalendarView extends View
{
	public Calendar ce;
	static Bitmap bitmap=null;
	static Rect initDst = new Rect();
	static Rect initSrc = new Rect();
	Paint paint = new Paint();
    Calendar_main _calActivity;
	@Override
	protected void onDraw(Canvas canvas)
	{
		double scale = 0;
		View v = _calActivity.getWindow().findViewById(Window.ID_ANDROID_CONTENT);// 获得根视图
		if (bitmap == null) 
		{
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inDensity = 240;
			bitmap = BitmapFactory.decodeResource(_calActivity.getResources(),
					(Utils.isInChinese() ? R.drawable.calendar1_02 : R.drawable.calendar1_02_en), options); // options must be added
			

			// the screen's density equals the picture's(using BitmapFactory method)
			int screenDensity = _calActivity.screenDensity;
			float viewHeightInch = (float) v.getHeight() / (float) screenDensity;
			float viewWidthInch = (float) v.getWidth() / (float) screenDensity;
			// get the picture's attributes then use inch express them
			int picDensity = bitmap.getDensity();
			int picWidth = bitmap.getWidth();
			int picHeight = bitmap.getHeight();
			float picHeightInch = (float) picHeight / (float) picDensity;
			float picWidthInch = (float) picWidth / (float) picDensity;

		    if(picWidthInch >= viewWidthInch )
		    {
		    	
//				initDst.left = picWidthInch >= viewWidthInch ? 0 : ((int) (picDensity
//						* (viewWidthInch - picWidthInch) / 2));
//				
//				initDst.top = 0;
//				initDst.bottom = picHeightInch >= viewHeightInch ? v.getHeight()
//						: (initDst.top + picHeight);
//				initDst.right = picWidthInch >= viewWidthInch ? v.getWidth()
//						: (initDst.left + picWidth);
//
//				
//				initSrc.left = picWidthInch >= viewWidthInch ? (int) (picDensity
//						* (picWidthInch - viewWidthInch) / 2) : 0;
//				initSrc.top = 0;
//				initSrc.right = picWidthInch >= viewWidthInch ? (initSrc.left + (int) (viewWidthInch * picDensity))
//						: picWidth;
//				
//				initSrc.bottom = (int) ((picHeightInch >= viewHeightInch ? viewHeightInch
//						: picHeightInch) * picDensity);	
		    	
		    	
		    	//As the picture of the actual size is greater than the screen, then capture the picture,
				//in order to adapt to the date's display
		    	
		    	//set the screen's size which will be drawed 
		    	initDst.left = 0;
		    	initDst.top = 0;
		    	initDst.right =  v.getWidth();
		    	//图片高度>=屏幕高度，取屏幕高度；否则取图片高度（防止图片高度被拉伸）
		    	initDst.bottom = picHeightInch >= viewHeightInch ? v.getHeight(): (initDst.top + picHeight);
		    	//capture the picture
		    	initSrc.left = (int) (picDensity* (picWidthInch - viewWidthInch) / 2);
		    	initSrc.top = 0;
		    	initSrc.right = initSrc.left + (int) (viewWidthInch * picDensity);
		    	initSrc.bottom = (int) ((picHeightInch >= viewHeightInch ? viewHeightInch: picHeightInch) * picDensity);	

		    }
		    else 
		    {
		    	//When the screen's size is less than picture's, then amplify the picture according to 
		    	//the width of the screen, amplify the picture's height with the proportion of the width.
                //If after  amplifing the picture's height is still less than the screen's, do nothing; 
		    	//If after  amplifing the picture' height is greater than the screen's,capture the picture.
		    	
		        //capture the picture
		    	initSrc.left = 0;
		        initSrc.top = 0;
		        initSrc.right  = picWidth ;
		        initSrc.bottom = picHeight;
		    	//set the screen's size which will be drawed 
		        initDst.left = 0;
		        initDst.top  = 0;
		        initDst.right = v.getWidth();
		        initDst.bottom =(int) ((v.getWidth()/(double)picWidth)*picHeight);
		    }
		    
		}
		scale = v.getWidth()>480?(v.getWidth()/(double)bitmap.getWidth()):1.0;
		canvas.drawBitmap(bitmap, initSrc, initDst, paint);
		 ce.draw(canvas,scale);
		

	}
	
	
 
	public CalendarView(Calendar_main activity)
	{
		super(activity);
		_calActivity=activity;
		ce = new Calendar(activity, this);
	}
//	@Override
//	public boolean onTouchEvent(MotionEvent motion)
//	{
//
//		ce.grid.setCellX(motion.getX());
//		ce.grid.setCellY(motion.getY());
//		if (ce.grid.inBoundary())
//		{
//			this.invalidate(); //liupan
//		}
//		return true;
//	}

//	@Override
//	public boolean onKeyDown(int keyCode, KeyEvent event)
//	{
//
//		switch (keyCode)
//		{
//
//			case KeyEvent.KEYCODE_DPAD_UP:
//			{
//
//				ce.grid.setCurrentRow(ce.grid.getCurrentRow() - 1);
//				break;
//			}
//			case KeyEvent.KEYCODE_DPAD_DOWN:
//			{
//				ce.grid.setCurrentRow(ce.grid.getCurrentRow() + 1);
//				break;
//			}
//			case KeyEvent.KEYCODE_DPAD_LEFT:
//			{
//				ce.grid.setCurrentCol(ce.grid.getCurrentCol() - 1);
//				break;
//			}
//			case KeyEvent.KEYCODE_DPAD_RIGHT:
//			{
//				ce.grid.setCurrentCol(ce.grid.getCurrentCol() + 1);
//				break;
//			}
//		
//		}
//		
//		return true;
//	}
}

package mobi.cloudymail.calendar;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;


/**
 * 
 * 仿Launcher中的WorkSapce，可以左右滑动切换屏幕的类
 * 
 * @author Yao.GUET
 * 
 *         blog: http://blog.csdn.net/Yao_GUET
 * 
 *         date: 2011-05-04
 */

public class ScrollLayout extends ViewGroup {
	// 当前的屏幕视图
	private int mCurScreen = 1;
	// 滑动的速度
	private static final int SNAP_VELOCITY = 200;

	private static final String TAG = "ScrollLayout";
	// 无事件的状态
	private static final int TOUCH_STATE_REST = 0;
	// 处于拖动的状态
	private static final int TOUCH_STATE_SCROLLING = 1;
	private static final int TOUCH_STATE_DEFAULT = -1;
	public static final String DRAW_PRE = "DRAW_PRE";
	public static final String DRAW_NEXT = "DRAW_NEXT";

	private Calendar_main mContext;

	private float mLastMotionX;
	// 用于滑动的类
	private Scroller mScroller;

	private int mTouchSlop;

	private int mTouchState = TOUCH_STATE_DEFAULT;
	// 用来跟踪触摸速度的类
	private VelocityTracker mVelocityTracker;

	private int mWidth;
	// 旋转的角度，可以进行修改来观察效果
//	private float angle = 90;

	public ScrollLayout(Context context, AttributeSet attrs) {

		this(context, attrs, 0);

	}

	// 在构造器中初始化
	public ScrollLayout(Context context, AttributeSet attrs, int defStyle) {

		super(context, attrs, defStyle);

		mContext = (Calendar_main)context;

		mScroller = new Scroller(context);

		mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

	}

	@Override
	public void addView(View child) {

		super.addView(child);
	}

	@Override
	protected void attachViewToParent(View child, int index, LayoutParams params) {

		super.attachViewToParent(child, index, params);
	}

	@Override
	public void computeScroll() {

		if (mScroller.computeScrollOffset()) {
			scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
			postInvalidate();
		}

	}

	/*
	 * 当进行View滑动时，会导致当前的View无效，该函数的作用是对View进行重新绘制 调用drawScreen函数
	 */
	@Override
	protected void dispatchDraw(Canvas canvas) {
		// super.dispatchDraw(canvas);
		final long drawingTime = getDrawingTime();
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			drawScreen(canvas, i, drawingTime);
		}
	}

	/*
	 * 处理循环 立体效果的实现函数 ,screen为哪一个子View
	 */
	public void drawScreen(Canvas canvas, int screen, long drawingTime) {
		// 得到当前子View的宽度
		final int width = getWidth();
		final int scrollWidth = screen * width;
		System.out.println("scrollWidth--->" + scrollWidth);
		final int scrollX = this.getScrollX();
		System.out.println("scrollX---->" + scrollX);
		// 偏移量不足的时候直接
		if (scrollWidth > scrollX + width || scrollWidth + width < scrollX) {
			return;
		}
		final View child = getChildAt(screen);
//		final int faceIndex = screen;
//		final float currentDegree = getScrollX() * (angle / getMeasuredWidth());
//		System.out.println("getMeasuredWidth---->" + getMeasuredWidth()
//				+ "getScrollX()----->" + getScrollX() + "currentDegree--->"
//				+ currentDegree);
//		final float faceDegree = currentDegree - faceIndex * angle;
//		System.out.println("faceDegree--->" + faceDegree);
//		if (faceDegree > 90 || faceDegree < -90) {
//			return;
//		}
		canvas.save();
		drawChild(canvas, child, drawingTime);
		canvas.restore();
	}

	/**
	 * @param screen
	 * 
	 */
	public void reDrawChildView(CalendarView view)
	{
		mContext.calendarView = (CalendarView)view;
		int curYear = mContext.calendarView.ce.grid.currentYear;
		int curMonth = mContext.calendarView.ce.grid.currentMonth;
		String nextYearMonth = mContext.calendarView.ce.grid.getNextYearMonth(curYear, curMonth);
		String preYearMonth = mContext.calendarView.ce.grid.getPreYearMonth(curYear, curMonth);
		String[] nYearMonth = nextYearMonth.split(",");
		String[] pYearMonth = preYearMonth.split(",");
		if (view == mContext.mFirstView)
		{
			mContext.mSecondView.ce.grid.currentYear = Integer.parseInt(nYearMonth[0]);
			mContext.mSecondView.ce.grid.currentMonth = Integer.parseInt(nYearMonth[1]);
			mContext.mSecondView.ce.grid.currentDay = 1;
			mContext.mThirdView.ce.grid.currentYear = Integer.parseInt(pYearMonth[0]);
			mContext.mThirdView.ce.grid.currentMonth = Integer.parseInt(pYearMonth[1]);
			mContext.mThirdView.ce.grid.currentDay = 1;
//			mContext.mThirdView.postInvalidate();
//			mContext.mSecondView.postInvalidate();

		}

		else if (view == mContext.mSecondView)
		{
			mContext.mThirdView.ce.grid.currentYear = Integer.parseInt(nYearMonth[0]);
			mContext.mThirdView.ce.grid.currentMonth = Integer.parseInt(nYearMonth[1]);
			mContext.mThirdView.ce.grid.currentDay = 1;
			mContext.mFirstView.ce.grid.currentYear = Integer.parseInt(pYearMonth[0]);
			mContext.mFirstView.ce.grid.currentMonth = Integer.parseInt(pYearMonth[1]);
			mContext.mFirstView.ce.grid.currentDay = 1;
//			mContext.mFirstView.postInvalidate();
//			mContext.mThirdView.postInvalidate();
		}

		else if (view == mContext.mThirdView)
		{
			mContext.mFirstView.ce.grid.currentYear = Integer.parseInt(nYearMonth[0]);
			mContext.mFirstView.ce.grid.currentMonth = Integer.parseInt(nYearMonth[1]);
			mContext.mFirstView.ce.grid.currentDay = 1;
			mContext.mSecondView.ce.grid.currentYear = Integer.parseInt(pYearMonth[0]);
			mContext.mSecondView.ce.grid.currentMonth = Integer.parseInt(pYearMonth[1]);
			mContext.mSecondView.ce.grid.currentDay = 1;
//			mContext.mFirstView.postInvalidate();
//			mContext.mSecondView.postInvalidate();
		}

	}

	@Override
	public void dispatchWindowFocusChanged(boolean hasFocus) {

		Log.d("Windows",
				"dispatchWindowFocusChanged -- >"
						+ getChildAt(mCurScreen).toString());
		super.dispatchWindowFocusChanged(hasFocus);
	}

	@Override
	public void dispatchWindowVisibilityChanged(int visibility) {
		Log.d("Windows",
				"dispatchWindowVisibilityChanged -- >"
						+ getChildAt(mCurScreen).toString());

		super.dispatchWindowVisibilityChanged(visibility);
	}

	public View getCurScreen() {

		return this.getChildAt(mCurScreen);

	}

	@Override
	protected void onAttachedToWindow() {

		Log.d("Windows", "onAttachedToWindow -- >"
				+ getChildAt(mCurScreen).toString());

		startCurrentView();
		super.onAttachedToWindow();
	}

	@Override
	protected void onDetachedFromWindow() {

		Log.d("Windows", "onDetachedFromWindow -- >"
				+ getChildAt(mCurScreen).toString());

		super.onDetachedFromWindow();
	}

//	@Override
//	public boolean onInterceptTouchEvent(MotionEvent ev) {
//
//		Log.d(TAG, "onInterceptTouchEvent-slop:" + mTouchSlop);
//
//		final int action = ev.getAction();
//		if ((action == MotionEvent.ACTION_MOVE)
//				&& (mTouchState == TOUCH_STATE_SCROLLING)) {
//			return true;
//		}
//		final float x = ev.getX();
//		switch (action) {
//		case MotionEvent.ACTION_MOVE:
//			final int xDiff = (int) Math.abs(mLastMotionX - x);
//			if (xDiff > mTouchSlop) {
//				mTouchState = TOUCH_STATE_SCROLLING;
//			}
//			break;
//			
//		case MotionEvent.ACTION_DOWN:
//			mLastMotionX = x;
//			mTouchState = mScroller.isFinished() ? TOUCH_STATE_DEFAULT
//					: TOUCH_STATE_SCROLLING;
//			break;
//
//		case MotionEvent.ACTION_CANCEL:
//
//		case MotionEvent.ACTION_UP:
//			
//			if(mTouchState == TOUCH_STATE_DEFAULT)
//			{
//				return false;
//			}
//			mTouchState=TOUCH_STATE_REST;
//			break;
//
//		}
//
//		return true;
//
//	}

	/*
	 * 
	 * 为子View指定位置
	 */
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {

		int childLeft = 0;
		final int childCount = getChildCount();
		for (int i = 0; i < childCount; i++) {
			final View childView = getChildAt(i);
			if (childView.getVisibility() != View.GONE) {
				final int childWidth = childView.getMeasuredWidth();
				childView.layout(childLeft, 0, childLeft + childWidth,
						childView.getMeasuredHeight());
				childLeft += childWidth;
			}

		}
	}

	// 重写此方法用来计算高度和宽度
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		final int width = MeasureSpec.getSize(widthMeasureSpec);
		final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		Log.e(TAG, "onMeasure width = " + width);
		// Exactly：width代表的是精确的尺寸
		// AT_MOST：width代表的是最大可获得的空间
		if (widthMode != MeasureSpec.EXACTLY) {
			throw new IllegalStateException(
					"ScrollLayout only canmCurScreen run at EXACTLY mode!");
		}

		final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		if (heightMode != MeasureSpec.EXACTLY) {
			throw new IllegalStateException(
					"ScrollLayout only can run at EXACTLY mode!");
		}

		// The children are given the same width and height as the scrollLayout
		// 得到多少页(子View)并设置他们的宽和高
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
		}

		// Log.e(TAG, "moving to screen "+mCurScreen);

		scrollTo(mCurScreen * width, 0);

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		CalendarView view=(CalendarView)getCurScreen();
		if (mVelocityTracker == null) {
			// 使用obtain方法得到VelocityTracker的一个对象
			mVelocityTracker = VelocityTracker.obtain();
		}
		// 将当前的触摸事件传递给VelocityTracker对象
		mVelocityTracker.addMovement(event);
		// 得到触摸事件的类型
		final int action = event.getAction();
		final float x = event.getX();

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			Log.d(TAG, "event down!");
			if (!mScroller.isFinished()) {
				mScroller.abortAnimation();
			}
			mLastMotionX = x;
			mTouchState=TOUCH_STATE_DEFAULT;
			reDrawChildView(view);
			break;

		case MotionEvent.ACTION_MOVE:
			
			int deltaX = (int) (mLastMotionX - x);
			if(Math.abs(deltaX)>10)
			{
				mTouchState=TOUCH_STATE_SCROLLING;
				mLastMotionX = x;
				scrollBy(deltaX, 0);
			}
			

			break;

		case MotionEvent.ACTION_UP:

			// if (mTouchState == TOUCH_STATE_SCROLLING) {
			
			final VelocityTracker velocityTracker = mVelocityTracker;
			// 计算当前的速度
			velocityTracker.computeCurrentVelocity(1000);
			// 获得当前的速度
			int velocityX = (int) velocityTracker.getXVelocity();
			Log.d(TAG, "velocityX:" + velocityX + "; event : up");
			if (velocityX > SNAP_VELOCITY && mCurScreen > 0) {
				// Fling enough to move left
				Log.d(TAG, "snap left");
				snapToScreen(mCurScreen - 1);
			} else if (velocityX < -SNAP_VELOCITY
			&& mCurScreen < getChildCount() - 1) {
				// Fling enough to move right
				Log.d(TAG, "snap right");
				snapToScreen(mCurScreen + 1);
			} else {
				snapToDestination();
			}
			if (mVelocityTracker != null) {
				mVelocityTracker.recycle();
				mVelocityTracker = null;
			}
			
			if(mTouchState ==TOUCH_STATE_DEFAULT)
			{
				view.ce.grid.setCellX(event.getX());
				view.ce.grid.setCellY(event.getY());
				if (view.ce.grid.inBoundary())
				{
					mContext.calendarView.invalidate();
				}
			}
			mTouchState = TOUCH_STATE_REST;
			break;
		case MotionEvent.ACTION_CANCEL:
			mTouchState = TOUCH_STATE_REST;
			
			break;
		}

		return true;

	}

	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {

		Log.d("Windows", "onWindowFocusChanged -- >"
				+ getChildAt(mCurScreen).toString());

		super.onWindowFocusChanged(hasWindowFocus);
	}

	@Override
	protected void onWindowVisibilityChanged(int visibility) {

		Log.d("Windows",
				"onWindowVisibilityChanged -- >"
						+ getChildAt(mCurScreen).toString());

		super.onWindowVisibilityChanged(visibility);
	}

	@Override
	public void requestChildFocus(View child, View focused) {

		Log.d("requestChildFocus", "child = " + child);

		super.requestChildFocus(child, focused);
	}

	private void setMWidth() {
		if (mWidth == 0) {
			mWidth = getWidth();
		}
	}

	private void setNext() {
		int count = this.getChildCount();
		View view = getChildAt(count - 1);
		removeViewAt(count - 1);
		addView(view, 0);
	}

	private void setPre() {
		int count = this.getChildCount();
		View view = getChildAt(0);
		removeViewAt(0);
		addView(view, count - 1);
	}

	public void setToScreen(int whichScreen) {
		whichScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));
		scrollTo(whichScreen * mWidth, 0);
		if (whichScreen > mCurScreen) {
			setPre();
		} else if (whichScreen < mCurScreen) {
			setNext();
		}

	}

	/**
	 * 
	 * According to the position of current layout
	 * 
	 * scroll to the destination page.
	 */

	public void snapToDestination() {
		setMWidth();
		// 根据View的宽度以及滑动的值来判断是哪个View
		final int destScreen = (getScrollX() + mWidth / 2) / mWidth;
		snapToScreen(destScreen);

	}

	public void snapToScreen(int whichScreen) {
		// get the valid layout page
		whichScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));
		setMWidth();
		int scrollX = getScrollX();
		int startWidth = whichScreen * mWidth;

		if (scrollX != startWidth) {

			int delta = 0;
			int startX = 0;

			if (whichScreen > mCurScreen) {
				setPre();
				delta = startWidth - scrollX;
				startX = mWidth - startWidth + scrollX;

			} else if (whichScreen < mCurScreen) {
				setNext();
				delta = -scrollX;
				startX = scrollX + mWidth;
			} else {
				startX = scrollX;
				delta = startWidth - scrollX;

			}

			mScroller.startScroll(startX, 0, delta, 0, Math.abs(delta) * 2);

			invalidate(); // Redraw the layout

		}

		startCurrentView();

	}

	private void startCurrentView() {

		String viewTag = (String) getChildAt(mCurScreen).getTag();

		Message message = new Message();

		if (TextUtils.equals(viewTag, Calendar_main.FIRST_INTENT_TAG)) {
			message.what = Calendar_main.FIRST_VIEW;
		} else if (TextUtils.equals(viewTag, Calendar_main.SECOND_INTENT_TAG)) {
			message.what = Calendar_main.SECOND_VIEW;
		} else {
			message.what = Calendar_main.THIRD_VIEW;
		}

		mContext.mHandler.sendMessage(message);
	}

}

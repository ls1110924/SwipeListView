package com.cqu.swipelistview;

import com.cqu.swipelistview.extend.ScrollState;
import com.cqu.swipelistview.extend.SwipeAction;
import com.cqu.swipelistview.extend.SwipeMode;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;


/**
 *	继承自ListView实现的可针对子item进行左右滑动的ListView
 * @author A Shuai
 *
 */
public class SwipeListView extends ListView implements OnScrollListener{

	/**
	 *	分别表征滑动事件模式，向左滑动操作，向右滑动操作和ListView的滚动状态
	 */
	private SwipeMode mSwipeMode = SwipeMode.NONE;
	private SwipeAction mLeftSwipeAction = SwipeAction.NONE;
	private SwipeAction mRightSwipeAction = SwipeAction.NONE;
	private ScrollState mScrollState = ScrollState.NONE;
	
	
	
	
	private int mListWidth;
	
	private VelocityTracker mVelocityTracker = null;
	
	public SwipeListView(Context context) {
		this(context, null);
	}
	
	public SwipeListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public SwipeListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	private void init(){
		mVelocityTracker = VelocityTracker.obtain();
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		mListWidth = w;
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		return super.dispatchTouchEvent(ev);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		// TODO 自动生成的方法存根
		return super.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		// TODO 自动生成的方法存根
		return super.onTouchEvent(ev);
	}
	
	protected void recyleVelocityTracker(){
		mVelocityTracker.recycle();
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		switch( scrollState ){
		case SCROLL_STATE_IDLE:
			break;
		case SCROLL_STATE_TOUCH_SCROLL:
		case SCROLL_STATE_FLING:
			break;
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {  }

}

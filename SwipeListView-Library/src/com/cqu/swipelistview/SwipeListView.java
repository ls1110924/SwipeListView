package com.cqu.swipelistview;

import java.util.ArrayList;
import java.util.List;

import com.cqu.swipelistview.extend.ScrollState;
import com.cqu.swipelistview.extend.SwipeAction;
import com.cqu.swipelistview.extend.SwipeMode;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListAdapter;
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
	
	//当前的动作类型
	private SwipeAction mCurrentAction = SwipeAction.NONE;
	
	//是否长按触发向左滑动事件，ListView滑动时是否关闭所有的items
	private boolean mSwipeOpenOnLongPress = false;
	private boolean mCloseAllItemsWhenMove = true;
	
	//滑动动画事件
	private int mSwipeAnimationTime;
	
	//向左滑动最后剩余的偏移量，向右滑动后的剩余偏移量
	private float mLeftSwipeRemainOffset = 0;
	private float mRightSwipeRemainOffset = 0;
	
	//子视图中上层视图的根视图资源ID，下层视图的根视图的资源ID
	private int mFrontViewResID = 0;
	private int mBackViewResID = 0;
	
	/**
	 *	两种判断滑动的滚动距离阈值
	 *	第一种是子view处理了Down事件，但是继续滑动的阈值足够长，也认为是滑动，可拦截子视图事件
	 *	第二种是子view为处理所有的事件，只要距离足够即可滑动
	 */
	private int mPagingTouchSlop;
	private int mTouchSlop;
	
	/**
	 *	最小滑动速度与最大滑动速度，用于判断手指快速滑动并释放后，速度在此阈值之间即可触发事件
	 */
	private int mMinFlingVelocity;
	private int mMaxFlingVelocity;
	private int mConfigAnimationTime;
	
	/**
	 *	分别指向触发事件的子视图的根视图，子视图中的上层视图，子视图中的下层视图
	 */
	private View mParentView;
	private View mFrontView;
	private View mBackView;
	
	/**
	 *	管理所有使用动画关闭的items，以便动画执行完毕后能恢复至正常情况
	 */
	private List<PenddingDismissItemView> mDimissItems = null;
	/**
	 *	管理所有的item的状态，表征
	 */
	private List<OpenState> mItemState = null;
	
	//Down事件触发时的初始X，Y坐标，以及触摸事件的ID，以判断是否为同一个系列的触摸事件，解决多点触摸时的混乱
	private float mStartX, mStartY;
	private int mActivePointerId;
	//触发动作事件时所处的X，Y坐标
	private float mActionX, mActionY;
	
	
	
	//ListView的宽度
	private int mListWidth;
	
	private ListAdapter mAdapter = null;
	private AdapterDataSetObserver mAdapterObserver = null;
	
	private VelocityTracker mVelocityTracker = null;
	
	public SwipeListView(Context context, int mFrontViewResID, int mBackViewResID) {
		this(context, null);
		this.mFrontViewResID = mFrontViewResID;
		this.mBackViewResID = mBackViewResID;
		
		if( mFrontViewResID == 0 || mBackViewResID == 0 ){
			throw new RuntimeException("you must appoint the resouce id of front view and back view!");
		}
	}
	
	public SwipeListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
		initAttr( context.obtainStyledAttributes(attrs, R.styleable.SwipeListView) );
	}
	
	public SwipeListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
		initAttr( context.obtainStyledAttributes(attrs, R.styleable.SwipeListView) );
	}
	
	/**
	 *	参数初始化
	 */
	private void init(){
		mVelocityTracker = VelocityTracker.obtain();
		
		final ViewConfiguration mConfiguration = ViewConfiguration.get(getContext());
		mPagingTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(mConfiguration);
		mTouchSlop = mConfiguration.getScaledTouchSlop();
		mMinFlingVelocity = mConfiguration.getScaledMinimumFlingVelocity();
		mMaxFlingVelocity = mConfiguration.getScaledMaximumFlingVelocity();
		mConfigAnimationTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
		mSwipeAnimationTime = mConfigAnimationTime;
		
		setOnScrollListener(this);
		
		mDimissItems = new ArrayList<PenddingDismissItemView>();
		mItemState = new ArrayList<OpenState>();
		
		mAdapterObserver = new AdapterDataSetObserver();
	}
	
	private void initAttr( TypedArray a ){
		if( a == null ){
			return;
		}
		
		mSwipeMode = SwipeMode.ofSwipeMode( a.getInt(R.styleable.SwipeListView_swipeMode, 0) );
		mLeftSwipeAction = SwipeAction.ofSwipeAction( a.getInt(R.styleable.SwipeListView_swipeActionLeft, 0) );
		mRightSwipeAction = SwipeAction.ofSwipeAction( a.getInt(R.styleable.SwipeListView_swipeActionRight, 0) );
		
		mSwipeOpenOnLongPress = a.getBoolean(R.styleable.SwipeListView_swipeOpenOnLongPress, false);
		mCloseAllItemsWhenMove = a.getBoolean(R.styleable.SwipeListView_swipeCloseAllItemsWhenMove, true);
		
		mSwipeAnimationTime = a.getInteger(R.styleable.SwipeListView_swipeAnimationTime, mConfigAnimationTime);
		
		mLeftSwipeRemainOffset = a.getDimension(R.styleable.SwipeListView_swipeLeftRemainOffset, 0);
		mRightSwipeRemainOffset = a.getDimension(R.styleable.SwipeListView_swipeRightRemainOffset, 0);
		
		mFrontViewResID = a.getResourceId(R.styleable.SwipeListView_swipeFrontView, 0);
		mBackViewResID = a.getResourceId(R.styleable.SwipeListView_swipeBackView, 0);
		
		a.recycle();
		
		if( mFrontViewResID == 0 || mBackViewResID == 0 ){
			throw new RuntimeException("you must appoint the resouce id of front view and back view!");
		}
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
		//如果不可用或者滑动模式为空则不监听事件
		if( !isEnabled() || mSwipeMode == SwipeMode.NONE ){
			return super.onInterceptTouchEvent(ev);
		}
		
		if( mScrollState != ScrollState.NONE ){
			requestDisallowInterceptTouchEvent(true);
			return true;
		}
		
		switch( MotionEventCompat.getActionMasked(ev) ){
			case MotionEvent.ACTION_DOWN:{
				mStartX = ev.getX();
				mStartY = ev.getY();
				mActionX = mActionY = 0;
				mScrollState = ScrollState.NONE;
				mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
				break;
			}
			case MotionEvent.ACTION_MOVE:{
				final int pointerIndex = ev.findPointerIndex(mActivePointerId);
				final float x = MotionEventCompat.getX(ev, pointerIndex);
				final float y = MotionEventCompat.getY(ev, pointerIndex);
				
				if( checkTriggeActionEvent(x, y, mPagingTouchSlop) ){
					requestDisallowInterceptTouchEvent(true);
					return true;
				}
				break;
			}
			case MotionEvent.ACTION_POINTER_UP:{
				final int pointerIndex = MotionEventCompat.getActionIndex(ev);
				final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);

				if(pointerId != mActivePointerId){
					break;
				}
			}
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:{
				mStartX = mStartY = mActionX = mActionY = 0;
				mScrollState = ScrollState.NONE;
				break;
			}
		}
		return super.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		//如果不可用或者滑动模式为空则不监听事件
		if( !isEnabled() || mSwipeMode == SwipeMode.NONE ){
			return super.onTouchEvent(ev);
		}
		
		switch( MotionEventCompat.getActionMasked(ev) ){
			case MotionEvent.ACTION_DOWN:{
				//既然子视图不处理Down事件，就阻止事件向子视图传递
				requestDisallowInterceptTouchEvent(true);
				break;
			}
			case MotionEvent.ACTION_POINTER_UP:
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:{
				mScrollState = ScrollState.NONE;
				break;
			}
		}
		return super.onTouchEvent(ev);
	}

	@Override
	public void setAdapter(ListAdapter adapter) {
		super.setAdapter(adapter);
		if( mAdapter != null ){
			mAdapter.unregisterDataSetObserver( mAdapterObserver );
		}
		mAdapter = adapter;
		if( mAdapter != null ){
			mAdapter.registerDataSetObserver(mAdapterObserver);
		}
		resetAllItems();
	}

	/**
	 *	重置速度跟踪器
	 */
	protected void recyleVelocityTracker(){
		mVelocityTracker.recycle();
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		switch( scrollState ){
		case SCROLL_STATE_IDLE:
			mScrollState = ScrollState.NONE;
			break;
		case SCROLL_STATE_TOUCH_SCROLL:
			closeAllOpenedItems();
		case SCROLL_STATE_FLING:
			mScrollState = ScrollState.SCROLLING_Y;
			break;
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {  }
	
	/**
	 *	设置滑动动画持续时长
	 * @param mSwipeAnimationTime	应当大于100毫秒
	 */
	public void setSwipeAnimationTime( int mSwipeAnimationTime ){
		if( mSwipeAnimationTime >= 100 ){
			this.mSwipeAnimationTime = mSwipeAnimationTime;
		} else {
			this.mSwipeAnimationTime = mConfigAnimationTime;
		}
	}
	
	/**
	 *	adapter数据源发生变化，需要重置所有的item状态
	 */
	private void resetAllItems(){
		mItemState.clear();
		if( mAdapter == null ){
			return;
		}
		for( int i = 0, size = mAdapter.getCount(); i < size; i++ ){
			mItemState.add(OpenState.NORMAL);
		}
	}
	
	/**
	 *	关闭所有已打开的子items
	 */
	private void closeAllOpenedItems(){
		
	}
	
	/**
	 *	检查是否超过了移动阈值，超过了移动阈值，则认为触发了滑动事件，则进行必要的处理
	 * @param x
	 * @param y
	 * @param threshold
	 */
	private boolean checkTriggeActionEvent( float x, float y, int threshold ){
		if( Math.abs( x - mStartX ) >= threshold ){
			mScrollState = ScrollState.SCROLLING_X;
			mActionX = x;
			mActionY = y;
			return true;
		}
		return false;
	}
	
	private class AdapterDataSetObserver extends DataSetObserver{

		@Override
		public void onChanged() {
			resetAllItems();
		}

		@Override
		public void onInvalidated() {
			resetAllItems();
		}
		
	}
	
	/**
	 *	子item所处的状态
	 * @author A Shuai
	 *
	 */
	private static enum OpenState{
		/**
		 *	正常
		 */
		NORMAL,
		
		/**
		 *	向左打开
		 */
		LEFT,
		
		/**
		 *	向右打开
		 */
		RIGHT;
	}
	
	private static class PenddingDismissItemView implements Comparable<PenddingDismissItemView>{

		public int mPosition;
		public View mView;
		public int mViewHeight;
		
		public PenddingDismissItemView( int mPosition, View mView ){
			this.mPosition = mPosition;
			this.mView = mView;
			mViewHeight = mView == null ? 0 : mView.getHeight();
		}
		
		@Override
		public int compareTo(PenddingDismissItemView other) {
			return other.mPosition - mPosition;
		}
		
	}

}

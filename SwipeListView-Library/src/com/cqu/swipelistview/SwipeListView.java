package com.cqu.swipelistview;

import java.util.ArrayList;
import java.util.List;

import com.cqu.swipelistview.extend.ScrollState;
import com.cqu.swipelistview.extend.SwipeAction;
import com.cqu.swipelistview.extend.SwipeMode;
import com.nineoldandroids.view.ViewHelper;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
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
public class SwipeListView extends ListView implements OnScrollListener, OnClickListener, OnLongClickListener{

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
	//能进行滑动的最大偏移量
	private float mLeftOffset;
	private float mRightOffset;
	
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
	private ArrayList<OpenState> mItemState = null;
	
	//Down事件触发时的初始X，Y坐标，以及触摸事件的ID，以判断是否为同一个系列的触摸事件，解决多点触摸时的混乱
	private float mStartX, mStartY;
	private int mActivePointerId;
	//触发动作事件时所处的X，Y坐标
	private float mActionX, mActionY;
	
	//标记触发事件的item view位置索引
	private int mDownPosition;
	
	private boolean isSwing;
	
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
		mLeftOffset = mListWidth - mLeftSwipeRemainOffset;
		mRightOffset = mListWidth - mRightSwipeRemainOffset;
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
				
				mCurrentAction = SwipeAction.NONE;
				
				mDownPosition = pointToPosition((int)mStartX, (int)mStartY);
				if( mDownPosition != INVALID_POSITION && mAdapter.isEnabled(mDownPosition) && mAdapter.getItemViewType(mDownPosition) >= 0 ){
					setViews( getChildAt(mDownPosition) );
					
					if( mSwipeOpenOnLongPress ){
						mFrontView.setLongClickable( mItemState.get(mDownPosition) == OpenState.NORMAL );
					}
					
					mVelocityTracker.clear();
					mVelocityTracker.addMovement(ev);
				}
				isSwing = false;
				
				break;
			}
			case MotionEvent.ACTION_MOVE:{
				if( mDownPosition == INVALID_POSITION ){
					break;
				}
				final int pointerIndex = ev.findPointerIndex(mActivePointerId);
				final float x = MotionEventCompat.getX(ev, pointerIndex);
				final float y = MotionEventCompat.getY(ev, pointerIndex);
				
				if( checkTriggeActionEvent(x, y, mPagingTouchSlop, ev) ){
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
				mVelocityTracker.clear();
				
				resetViews();
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
		
		if( mScrollState == ScrollState.SCROLLING_Y ){
			return super.onTouchEvent(ev);
		}
		
		switch( MotionEventCompat.getActionMasked(ev) ){
			case MotionEvent.ACTION_DOWN:{
				//既然子视图不处理Down事件，就阻止事件向子视图传递
				requestDisallowInterceptTouchEvent(true);
				mCurrentAction = SwipeAction.NONE;
				final int mDownX = (int) ev.getX();
				final int mDownY = (int) ev.getY();
				mDownPosition = pointToPosition(mDownX, mDownY);
				if( mDownPosition != INVALID_POSITION && mAdapter.isEnabled(mDownPosition) && mAdapter.getItemViewType(mDownPosition) >= 0 ){
					setViews( getChildAt(mDownPosition - getFirstVisiblePosition()) );
					
					if( mSwipeOpenOnLongPress ){
						mFrontView.setLongClickable( mItemState.get(mDownPosition) == OpenState.NORMAL );
					}
					
					mVelocityTracker.clear();
					mVelocityTracker.addMovement(ev);
				}
				isSwing = false;
				
				super.onTouchEvent(ev);
				//务必保证返回true
				return true;
			}
			case MotionEvent.ACTION_MOVE:{
				if( mDownPosition == INVALID_POSITION ){
					break;
				}
				mVelocityTracker.addMovement(ev);
				mVelocityTracker.computeCurrentVelocity(1000);
				float mVelocityX = Math.abs(mVelocityTracker.getXVelocity());
				float mVelocityY = Math.abs(mVelocityTracker.getYVelocity());
				
				float mDeltaX = ev.getX() - mStartX;
				
				if( mCurrentAction == SwipeAction.NONE ){
					switch( mItemState.get(mDownPosition) ){
						case NORMAL:
							if( mSwipeMode == SwipeMode.LEFT && mDeltaX > 0 ){
								mDeltaX = 0;
								mActionX = ev.getX();
								mActionY = ev.getY();
							}
							if( mSwipeMode == SwipeMode.RIGHT && mDeltaX < 0 ){
								mDeltaX = 0;
								mActionX = ev.getX();
								mActionY = ev.getY();
							}
							break;
						case LEFT:
							if( mDeltaX < 0 ){
								mDeltaX = 0;
								mActionX = ev.getX();
								mActionY = ev.getY();
							}
							break;
						case RIGHT:
							if( mDeltaX > 0 ){
								mDeltaX = 0;
								mActionX = ev.getX();
								mActionY = ev.getY();
							}
							break;
					}
					
					if( mVelocityX > mVelocityY && mDeltaX != 0 && checkTriggeActionEvent(ev.getX(), ev.getY(), mTouchSlop, ev) ){
						isSwing = true;
						mScrollState = ScrollState.SCROLLING_X;
						switch( mItemState.get(mDownPosition) ){
							case NORMAL:
								mCurrentAction = mDeltaX > 0 ? mRightSwipeAction : mLeftSwipeAction;
								break;
							default:
								mCurrentAction = SwipeAction.REVEAL;
								break;
						}
						return true;
					}
				}
				
				if( mCurrentAction == SwipeAction.NONE ){
					break;
				}
				final float mXOffset = ev.getX() - mActionX;
				
				moveToOffset(mXOffset);
				
				break;
			}
			case MotionEvent.ACTION_POINTER_UP:{
				final int pointerIndex = MotionEventCompat.getActionIndex(ev);
				final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);

				if(pointerId != mActivePointerId){
					break;
				}
			}
			case MotionEvent.ACTION_UP:{
				if( mDownPosition == INVALID_POSITION || !isSwing || mScrollState != ScrollState.SCROLLING_X ){
					break;
				}
				final int pointerIndex = MotionEventCompat.getActionIndex(ev);
				final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);

				if(pointerId != mActivePointerId){
					break;
				}
				
				float mXOffset = ev.getX() - mActionX;
				mVelocityTracker.addMovement(ev);
				mVelocityTracker.computeCurrentVelocity(1000);
				float mVelocityX = mVelocityTracker.getXVelocity();
				float mVelocityXAbs = Math.abs( mVelocityX );
				float mVelocityY = mVelocityTracker.getYVelocity();
				float mVelocityYAbs = Math.abs( mVelocityY );
				switch( mItemState.get(mDownPosition) ){
					case NORMAL:
						if( mVelocityX > 0 && mSwipeMode == SwipeMode.LEFT ){
							mVelocityXAbs = 0;
							mXOffset = 0;
						} else if (  mVelocityX < 0 && mSwipeMode == SwipeMode.RIGHT  ){
							mVelocityXAbs = 0;
							mXOffset = 0;
						}
						break;
					case LEFT:
						if( mVelocityX < 0 ){
							mVelocityXAbs = 0;
							mXOffset = 0;
						}
						break;
					case RIGHT:
						if( mVelocityX > 0 ){
							mVelocityXAbs = 0;
							mXOffset = 0;
						}
						break;
				}
				
				boolean mSwap = false;
				boolean mToRight = false;
				if( mVelocityXAbs >= mMinFlingVelocity && mVelocityXAbs <= mMaxFlingVelocity && mVelocityXAbs > 2*mVelocityYAbs ){
					mToRight = mVelocityX > 0;
					switch( mItemState.get(mDownPosition) ){
						case NORMAL:
							mSwap = true;
							break;
						case LEFT:
							mSwap = false;
							break;
						case RIGHT:
							mSwap = false;
							break;
					}
				} else if ( Math.abs( mXOffset ) >= mListWidth / 2 ){
					mSwap = true;
					mToRight = mXOffset > 0;
				}
				
				generateAnimation(mSwap, mToRight, mDownPosition);
				
				mVelocityTracker.recycle();
				mDownPosition = INVALID_POSITION;
				isSwing = false;
				
				mScrollState = ScrollState.NONE;
				break;
			}
			case MotionEvent.ACTION_CANCEL:{
				isSwing = false;
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
	
	@Override
	public boolean onLongClick(View v) {
		return false;
	}

	@Override
	public void onClick(View v) {
		
	}
	
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
	 *	
	 * @param mView
	 */
	private void setViews( View mView ){
		mParentView = mView;
		mFrontView = mParentView.findViewById( mFrontViewResID );
		if( mSwipeOpenOnLongPress ){
			mFrontView.setOnLongClickListener(this);
		}
		mBackView = mParentView.findViewById( mBackViewResID );
	}
	
	private void resetViews(){
		
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
	private boolean checkTriggeActionEvent( float x, float y, int threshold, MotionEvent ev ){
		if( Math.abs( x - mStartX ) >= threshold ){
			mScrollState = ScrollState.SCROLLING_X;
			mActionX = x;
			mActionY = y;
			
			MotionEvent mEvent = MotionEvent.obtain(ev);
			mEvent.setAction(MotionEvent.ACTION_CANCEL |
					(MotionEventCompat.getActionIndex(ev) << MotionEventCompat.ACTION_POINTER_INDEX_SHIFT));
			super.onTouchEvent(mEvent);
			mEvent.recycle();
			return true;
		}
		return false;
	}
	
	/**
	 *	移动指定偏移量
	 * @param mDeltaX
	 */
	private void moveToOffset( float mDeltaX ){
		
		float mLastTranslateX;
		switch( mCurrentAction ){
			case DISMISS:
				mLastTranslateX = ViewHelper.getTranslationX(mParentView);
				if( mLastTranslateX < 0 && mDeltaX > 0 ){
					ViewHelper.setTranslationX(mParentView, 0);
					ViewHelper.setAlpha(mParentView, 1);
					mCurrentAction = mRightSwipeAction;
				} else if ( mLastTranslateX > 0 && mDeltaX < 0 ){
					ViewHelper.setTranslationX(mParentView, 0);
					ViewHelper.setAlpha(mParentView, 1);
					mCurrentAction = mLeftSwipeAction;
				}
				break;
			case REVEAL:
				mLastTranslateX = ViewHelper.getTranslationX(mFrontView);
				if( mLastTranslateX < 0 && mDeltaX > 0 ){
					ViewHelper.setTranslationX(mFrontView, 0);
					mCurrentAction = mRightSwipeAction;
				} else if ( mLastTranslateX > 0 && mDeltaX < 0 ){
					ViewHelper.setTranslationX(mFrontView, 0);
					mCurrentAction = mLeftSwipeAction;
				}
				break;
			/* 不会出现这种情况 */
			default:
				return;
		}
		
		switch( mCurrentAction ){
			case DISMISS:
				ViewHelper.setTranslationX(mParentView, mDeltaX);
				ViewHelper.setAlpha(mParentView, Math.max(0f, Math.min(1f, 1f - 2f * Math.abs(mDeltaX) / mListWidth)));
				break;
			case REVEAL:
				ViewHelper.setTranslationX(mFrontView, mDeltaX);
				break;
			default:
				return;
		}
		
	}
	
	/**
	 *	手势结束以后生成一个收尾动画
	 * @param isSwap	是否触发了交换事件，为false的话则表明回归原位
	 * @param isToRight	如果触发了交换事件，即需要向左或者向右
	 * @param mPosition
	 */
	private void generateAnimation( boolean isSwap, boolean isToRight, int mPosition ){
		
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

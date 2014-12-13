package com.cqu.swipelistview.extend;

/**
 *	滑动模式
 * @author A Shuai
 *
 */
public enum SwipeMode {

	/**
	 *	不支持滑动
	 */
	NONE(0),
	
	/**
	 *	向左滑动
	 */
	LEFT(1),
	
	/**
	 *	向右滑动
	 */
	RIGHT(2),
	
	/**
	 *	双向均可滑动
	 */
	BOTH(3);
	
	
	private int mType;
	
	private SwipeMode( int mType ){
		this.mType = mType;
	}
	
	public int getType(){
		return mType;
	}
	
}

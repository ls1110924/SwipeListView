package com.cqu.swipelistview.extend;

/**
 *	表征ListView的滚动状态
 * @author A Shuai
 *
 */
public enum ScrollState {

	/**
	 *	无滚动
	 */
	NONE(0),
	
	/**
	 *	X轴方向上的滚动
	 */
	SCROLLING_X(1),
	
	/**
	 *	Y轴方向上的滚动
	 */
	SCROLLING_Y(2);
	
	private int mType;
	
	private ScrollState( int mType ){
		this.mType = mType;
	}
	
	public int getType(){
		return mType;
	}
	
}

package com.cqu.swipelistview.extend;

public enum SwipeAction {

	/**
	 *	无动作
	 */
	NONE(0),
	
	/**
	 *	显示
	 */
	REVEAL(1),
	
	/**
	 *	隐藏
	 */
	DISMISS(2);
	
	private int mType;
	
	private SwipeAction( int mType ){
		this.mType = mType;
	}
	
	public int getType(){
		return mType;
	}
	
}

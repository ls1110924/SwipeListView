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
	
	/**
	 *	将指定索引值转换为对应的枚举值
	 * @param mIndex
	 * @return
	 */
	public static SwipeAction ofSwipeAction( int mIndex ){
		SwipeAction mResult = NONE;
		
		for( SwipeAction action : values() ){
			if( action.getType() == mIndex ){
				mResult = action;
				break;
			}
		}
		
		return mResult;
	}
	
}

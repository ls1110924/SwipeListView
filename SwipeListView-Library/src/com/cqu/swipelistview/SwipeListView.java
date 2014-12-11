package com.cqu.swipelistview;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

/**
 *	继承自ListView实现的可针对子item进行左右滑动的ListView
 * @author A Shuai
 *
 */
public class SwipeListView extends ListView{

	public SwipeListView(Context context) {
		this(context, null);
	}
	
	public SwipeListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public SwipeListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	
	
	

}

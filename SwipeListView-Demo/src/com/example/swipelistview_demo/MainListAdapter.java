package com.example.swipelistview_demo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class MainListAdapter extends BaseAdapter{

	protected Context mContext;
	protected LayoutInflater mInflater = null;
	
	public MainListAdapter( Context mContext ){
		this.mContext = mContext;
		mInflater = LayoutInflater.from(this.mContext);
	}
	
	@Override
	public int getCount() {
		return 20;
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if( convertView == null ){
			convertView = mInflater.inflate(R.layout.item_main_listview, parent, false);
		}
		return convertView;
	}

}

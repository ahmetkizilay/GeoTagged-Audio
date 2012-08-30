package com.ahmetkizilay.audio.geotag.settings;

import java.util.List;

import android.R;
import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class CustomSpinnerAdapter extends BaseAdapter{

	private final List<SpinnerItem> itemList;
	private Activity parentActivity;
	
	public CustomSpinnerAdapter(Activity parentActivity, List<SpinnerItem> itemList) {
		this.itemList = itemList;
		this.parentActivity = parentActivity;
	}
	
	public int getCount() {
		return this.itemList.size();
	}

	public SpinnerItem getItem(int position) {
		return itemList.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		TextView textView = new TextView(parentActivity);
		textView.setText(itemList.get(position).getDisplay());
		textView.setTextAppearance(parentActivity, R.style.TextAppearance_Medium_Inverse);
		textView.setHeight(30);
		textView.setGravity(Gravity.CENTER_VERTICAL);
		return textView;
	}
	
	public int getItemPosition(int itemValue) {
		for(int i = 0; i < itemList.size(); i++) {
			if(itemList.get(i).getValue() == itemValue) {
				return i;
			}
		}
		return 0;
	}

}

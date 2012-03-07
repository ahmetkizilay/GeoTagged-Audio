package com.ahmetkizilay.audio.geotag;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.google.android.maps.OverlayItem;

public class MarkerOverlay extends com.google.android.maps.ItemizedOverlay<OverlayItem> {
	private List<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	@SuppressWarnings("unused")
	private Context mContext;

	public MarkerOverlay(Drawable marker) {
		
		super(boundCenterBottom(marker));
	}

	public MarkerOverlay(Drawable marker, Context context) {
		super(marker);
		this.mContext = context;
	}

	public void addOverlay(OverlayItem overlay) {
		mOverlays.add(overlay);
		populate();
	}
	
	public void clearOverlays() {
		mOverlays.clear();
	}

	@Override
	protected OverlayItem createItem(int i) {
		return mOverlays.get(i);
	}

	@Override
	public int size() {
		return mOverlays.size();
	}
}

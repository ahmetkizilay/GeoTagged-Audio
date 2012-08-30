package com.ahmetkizilay.audio.geotag.settings;

public class SpinnerItem {
	private String display;
	private int value;
	
	public SpinnerItem(String display, int value) {
		this.display = display;
		this.value = value;
	}
	
	public SpinnerItem() {
		
	}

	public String getDisplay() {
		return display;
	}

	public void setDisplay(String display) {
		this.display = display;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}
}

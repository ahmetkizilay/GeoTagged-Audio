package com.ahmetkizilay.audio.geotag;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class CoordinateInfo {
	private int index;
	private int latitude;
	private int longitude;
	private long time;
	
	
	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public int getLatitude() {
		return latitude;
	}

	public void setLatitude(int latitude) {
		this.latitude = latitude;
	}

	public int getLongitude() {
		return longitude;
	}

	public void setLongitude(int longitude) {
		this.longitude = longitude;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public CoordinateInfo() {
		
	}
	
	public CoordinateInfo(int index, int latitude, int longitude, long time) {
		this.index = index;
		this.longitude = longitude;
		this.latitude = latitude;
		this.time = time;
	}
	
	public static List<CoordinateInfo> createCoordinateInfoListFromFile(String filePath) {
		List<CoordinateInfo> result = new ArrayList<CoordinateInfo>();
		try {
			FileReader fr = new FileReader(new File(filePath));
			BufferedReader br = new BufferedReader(fr);
			String currentLine = "";
			int index = 1;
			while((currentLine = br.readLine()) != null) {
				String[] parts = currentLine.split(" ");
				result.add(new CoordinateInfo(index, Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Long.parseLong(parts[2].trim())));
				index++;
			}
			return result;
		}
		catch(Exception exp) {
			return null;
		}
	}
	
	public static void main(String[] args) {
		List<CoordinateInfo> coordinates = CoordinateInfo.createCoordinateInfoListFromFile("D:\\work\\temp\\test.txt");
		for(int i = 0; i < coordinates.size(); i++) {
			CoordinateInfo thisCoordinate = coordinates.get(i);
			System.out.println("Index: " + thisCoordinate.getIndex());
			System.out.println("Latitude: " + thisCoordinate.getLatitude());
			System.out.println("Longitude: " + thisCoordinate.getLongitude());
			System.out.println("Time: " + thisCoordinate.getTime());
		}
	}
}

package com.ahmetkizilay.audio.geotag;

import java.util.List;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class RouteOverlay extends Overlay{
	
	private List<CoordinateInfo> coordinates;
	public RouteOverlay(List<CoordinateInfo> coordinates)
	{
		this.coordinates = coordinates;
	}
	
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		super.draw(canvas, mapView, shadow);
		
		if(coordinates == null || coordinates.size() < 2) 
			return;
		
		Paint mPaint = new Paint();
		 mPaint.setDither(true);
        mPaint.setColor(Color.argb(52, 17, 52, 191));
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(4);
        
        Path path = new Path();
        Projection projection = mapView.getProjection();
        
        for(int i = 0; i < coordinates.size() - 1; i++) {
        	CoordinateInfo thisCoordinate = coordinates.get(i);
        	CoordinateInfo nextCoordinate = coordinates.get(i + 1);
        	
            GeoPoint thisGeoPoint = new GeoPoint(thisCoordinate.getLatitude(), thisCoordinate.getLongitude());
            GeoPoint nextGeoPoint = new GeoPoint(nextCoordinate.getLatitude(), nextCoordinate.getLongitude());
            
            Point thisPoint = new Point();
            Point nextPoint = new Point();
            
            projection.toPixels(thisGeoPoint, thisPoint);
            projection.toPixels(nextGeoPoint, nextPoint);

            path.moveTo(thisPoint.x, thisPoint.y);
            path.lineTo(nextPoint.x, nextPoint.y);

        }
        
        canvas.drawPath(path, mPaint);
        
	}	
}

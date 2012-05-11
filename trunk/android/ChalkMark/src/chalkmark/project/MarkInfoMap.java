package chalkmark.project;

import java.util.ArrayList;
import java.util.List;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class MarkInfoMap extends MapActivity 
{
	public static final String EXTRA_LATITUDE="chalkmark.project.EXTRA_LATITUDE";
	public static final String EXTRA_LONGITUDE="chalkmark.project.EXTRA_LONGITUDE";
	public static final String EXTRA_NAME="chalkmark.project.EXTRA_NAME";
	public static final String EXTRA_SUBJECT="chalkmark.project.EXTRA_SUBJECT";
	
	private MapView map=null;
	
	List<Overlay> mapOverlays;
	MyItemizedOverlay ChalkmarkOverlay;
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map);
		
		double lat=getIntent().getDoubleExtra(EXTRA_LATITUDE, 0);
		double lon=getIntent().getDoubleExtra(EXTRA_LONGITUDE, 0);

		map = (MapView) findViewById(R.id.map);		
		map.getController().setZoom(16);
		map.setBuiltInZoomControls(true);
		mapOverlays = map.getOverlays();
		
		Drawable drawable = getResources().getDrawable(R.drawable.marker_flag);
		ChalkmarkOverlay = new MyItemizedOverlay(drawable, map);
		
		String from = "From: " + getIntent().getStringExtra(EXTRA_NAME);
		String subject = getIntent().getStringExtra(EXTRA_SUBJECT);
		
		GeoPoint markPoint = new GeoPoint((int)(lat*1000000.0),(int)(lon*1000000.0));
		OverlayItem overlay = new OverlayItem(markPoint, from, subject);
        ChalkmarkOverlay.addOverlay(overlay);
        mapOverlays.add(ChalkmarkOverlay);
        		
		map.getController().setCenter(markPoint);		
	}
	
    class MyItemizedOverlay extends AvailableMarksBalloonItemizedOverlay<OverlayItem> {

    	private ArrayList<OverlayItem> m_overlays = new ArrayList<OverlayItem>();
    	
    	
    	public MyItemizedOverlay(Drawable defaultMarker, MapView mapView) {
    		super(boundCenter(defaultMarker), mapView);
    	}

    	public void addOverlay(OverlayItem overlay) {
    	    m_overlays.add(overlay);
    	    populate();
    	}

    	@Override
    	protected OverlayItem createItem(int i) {
    		return m_overlays.get(i);
    	}


    	@Override
    	public int size() {
    		return m_overlays.size();
    	}

    	@Override
    	protected boolean onBalloonTap(int index, OverlayItem item) 
    	{
    		Log.v("CHALKMARK","Tapping the Balloon");
    		return true;
    	}
    	
    }
	
 	@Override
	protected boolean isRouteDisplayed() 
 	{
		return(false);
	}
}
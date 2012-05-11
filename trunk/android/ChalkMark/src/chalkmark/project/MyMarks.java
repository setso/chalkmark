/* File: MyMarks.java
 * 
 * Main MyMarks Activity. This activity corresponds to a separate tab in the
 * main ChalkMark application.
 * 
 * The MyMarks activity displays the eggs that are available to me for pickup.
 * In other words, these are the eggs that other people have left for me. This
 * is preferably done using a MapView.
 * 
 * TODO:
 *  - Add a button to allow the user to go back to their location
 */

// marksJSON = "{\"marks\":"+ 
//	"["+
//		"{\"lat\": \"33.62141418457031\", \"from_name\": \"Setso Metodi\", \"lon\": \"-117.82872009277344\", \"expiration\": \"5\", \"subject\": \"hello\"},"+ 
//		"{\"lat\": \"33.7410774230957\", \"from_name\": \"Setso Metodi\", \"lon\": \"-117.834716796875\", \"expiration\": \"5\", \"subject\": \"hello+setso\"}"+
//	"]}";

package chalkmark.project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;


public class MyMarks extends MapActivity implements LocationListener
{
	String TAG = "CHALKMARK";
	public final static String ID_EXTRA = "chalkmark.project._ID";	
	Button dropMarkButton; 
	Button myLocButton;	
	private SharedPreferences mPreferences;
	LinearLayout linearLayout;	
	private MapView myMapView;
	private MyLocationOverlay myLocOverlay;		
	private GeoPoint mylocGeoPoint = null;	
	List<Overlay> mapOverlays;	
	
	// the three overlays
	DropMessageOverlay droppingMessageOverlay;
	AvailableMarksItemizedOverlay ChalkmarksOverlay;
	MyMarksItemizedOverlay MyMarksOverlay;
	
	private String myuid = null;
	
	Drawable dragmarker = null;
	ImageView dragImage=null;
    private int xDragImageOffset=0;
    private int yDragImageOffset=0;
    private int xDragTouchOffset=0;
    private int yDragTouchOffset=0;
	
    MarksDataBaseHelper dbhelper;     
    MyMarksDataBaseHelper mymarksdb; 
    
    private ProgressDialog progress_dialog;

	
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mymarks);
        
        dbhelper = new MarksDataBaseHelper(this);
        mymarksdb = new MyMarksDataBaseHelper(this);
        
        mPreferences = this.getSharedPreferences("CurrentUser", MODE_PRIVATE);
        myuid = mPreferences.getString("UserName","");
   		
        // Initialize the Map View
        myMapView = (MapView) findViewById(R.id.mapview);
        mapOverlays = myMapView.getOverlays();
        myMapView.setBuiltInZoomControls(true);
        
        myLocOverlay = new MyLocationOverlay(this,myMapView);
        mapOverlays.add(myLocOverlay);
        
           
        ImageView me = (ImageView) findViewById(R.id.my_location);
		me.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				gotoMyLocation();
			}
		});
		
		ImageView refreshImage = (ImageView) findViewById(R.id.refresh_map);
		refreshImage.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				getAvailableChalkmarks();
			}
		});
		
		ImageView dropImage = (ImageView) findViewById(R.id.make_mark);
		dropImage.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				SharedPreferences.Editor editor = mPreferences.edit();
				editor.putString("SavedSubject", "");
				editor.putString("SavedBody", "");
				editor.putString("DestinationContacts", "");
				editor.commit();
				placeDropMarkOverlayOnMap();
			}
		});
        
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putBoolean("SavedImageFlag", false);	
		
		dragmarker = getResources().getDrawable(R.drawable.marker_cross);
		dragImage=(ImageView)findViewById(R.id.drag);
	    xDragImageOffset=dragImage.getDrawable().getIntrinsicWidth()/2;
	    yDragImageOffset=dragImage.getDrawable().getIntrinsicHeight();
	    
	}
       
    
    @Override
    protected void onResume() 
    {
    	super.onResume();
        //mapOverlays.add(myOverlay); 	
    	myMapView.getController().setZoom(15);
        myLocOverlay.enableMyLocation();
        getAvailableChalkmarks();
    }
    
    
    @Override
	protected void onPause() 
	{ 
		super.onPause();
        myLocOverlay.disableMyLocation();
	}
    
    
    @Override
	protected void onDestroy() 
	{ 
		super.onDestroy();
		mymarksdb.close();
		dbhelper.close();
	}
           
    
    private void gotoMyLocation()
    {
    	GeoPoint myGeoPoint = myLocOverlay.getMyLocation();
    	if (myGeoPoint != null) {
    		myMapView.getController().setCenter(myGeoPoint);
    		myMapView.getController().setZoom(15);
    	} else {
    		Float mylat = mPreferences.getFloat("Lat", (float) 0.0);
    		Float mylon = mPreferences.getFloat("Lon", (float) 0.0);
    		mylocGeoPoint = new GeoPoint((int)(mylat*1E6),(int)(mylon*1E6)); 
    		myMapView.getController().setCenter(mylocGeoPoint);
    		myMapView.getController().setZoom(15);
    	}
    	mapOverlays.add(myLocOverlay);
    	myMapView.invalidate();
    }
    
    
    public void getAvailableChalkmarks()
    {	
    	if (mapOverlays.contains(droppingMessageOverlay)) mapOverlays.remove(droppingMessageOverlay);    	
    	
		GeoPoint center = myMapView.getMapCenter();
		Integer latInt = center.getLatitudeE6();
		Integer lonInt = center.getLongitudeE6();
    	Float lat = (float) (latInt / 1E6);
    	Float lon = (float) (lonInt / 1E6);		
    	
    	List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        nameValuePairs.add(new BasicNameValuePair("username",(String) myuid));
        nameValuePairs.add(new BasicNameValuePair("lat",String.valueOf(lat)));
        nameValuePairs.add(new BasicNameValuePair("lon",String.valueOf(lon)));   
        
        executeSendForAvailableChalkmarks(nameValuePairs); 	
    }
    
        
 
    
	/* ========================================================================================================== 
	 * MyMarksItemizedOverlay: Marks tha I have dropped
	 * ==========================================================================================================
	 */

    class MyMarksItemizedOverlay extends MyMarksBalloonItemizedOverlay<OverlayItem> 
    {

    	private ArrayList<OverlayItem> m_overlays = new ArrayList<OverlayItem>();
    	
    	public MyMarksItemizedOverlay(Drawable defaultMarker, MapView mapView) {
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
    }

    
    
    
    
    
    
    
	/* ========================================================================================================== 
	 * MyItemizedOverlay: Used for marks waiting for me
	 * ==========================================================================================================
	 */

    class AvailableMarksItemizedOverlay extends AvailableMarksBalloonItemizedOverlay<OverlayItem> 
    {

    	private ArrayList<OverlayItem> m_overlays = new ArrayList<OverlayItem>();
    	
    	public AvailableMarksItemizedOverlay(Drawable defaultMarker, MapView mapView) {
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
    		// force the server to update the location by starting and stopping the locservice
    		
    		GeoPoint myloc = myLocOverlay.getMyLocation();
			GeoPoint thisloc = m_overlays.get(index).getPoint();    
			float mylat;
			float mylon;
			
			Location locationA = new Location("myloc");  
    		if (myloc != null) {
	    		mylat = (float) (myloc.getLatitudeE6() / 1E6);
	    		mylon = (float) (myloc.getLongitudeE6()/ 1E6); 
    		}
	    	else {
	    		mylat = mPreferences.getFloat("Lat", (float) 0.0);
	    		mylon = mPreferences.getFloat("Lon", (float) 0.0);
	    	}
    		locationA.setLatitude(mylat);  
    		locationA.setLongitude(mylon);
    		
    		Location locationB = new Location("thisloc");  
    		locationB.setLatitude((float) (thisloc.getLatitudeE6() / 1E6));  
    		locationB.setLongitude((float) (thisloc.getLongitudeE6() / 1E6));  
	    		
    		double distance_meters = locationA.distanceTo(locationB);
    		double distance_miles = ((distance_meters / 1E3) / 1.609344);
    		double distance_yards = distance_miles * 1760;
    		
    		
    		if (distance_yards > 1000) { // still too far to collect
    			
    			Toast.makeText(getBaseContext(), "Distance from Me: " + String.valueOf(distance_miles) + " miles", Toast.LENGTH_SHORT).show();
    			
    		} else { // pick up the chalkmark
    			String myuid = mPreferences.getString("UserName","");
    	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
    	        nameValuePairs.add(new BasicNameValuePair("username",(String) myuid));
    	        nameValuePairs.add(new BasicNameValuePair("lat",String.valueOf(mylat)));
    	        nameValuePairs.add(new BasicNameValuePair("lon",String.valueOf(mylon)));
    	        nameValuePairs.add(new BasicNameValuePair("acc",String.valueOf(0.0)));
    	        nameValuePairs.add(new BasicNameValuePair("bat",String.valueOf(0.0)));
    	        executeSendLocation(nameValuePairs);
    		}    		
    		return true;
    	}    	
    }
   

  
    
    /* ========================================================================================================== 
     * CollectChalkmarkTask: Background task that collects the available Chalkmark from the server
     * ==========================================================================================================
     */
    
    @SuppressWarnings("unchecked")
    public void executeSendLocation(List<NameValuePair> mylist) {
    	 new CollectChalkmarkTask().execute(mylist);
    }
    
    private class CollectChalkmarkTask extends AsyncTask<List<NameValuePair>, Void, InputStream> 
    {
        protected InputStream doInBackground(List<NameValuePair>...nameValuePairs) 
        {
        	 for (List<NameValuePair> nameValuePair : nameValuePairs) { 
                HttpClient http_client = new DefaultHttpClient();
                HttpResponse response = null;
                HttpPost httppost = new HttpPost("http://eggdroplabs.com:9002/egg_loc/");
                InputStream markInputStream = null;
                
                try 
                {       // Execute HTTP Post Request
                        httppost.setEntity(new UrlEncodedFormEntity(nameValuePair));
                        response = http_client.execute(httppost);                            
                } catch (ClientProtocolException e) { return null;    					
				} catch (IOException e) { return null; }                    
                int http_status = response.getStatusLine().getStatusCode();
                if (http_status !=  HttpStatus.SC_OK) {return null;} 
                
                try { 
                	markInputStream = response.getEntity().getContent();                    
                } catch (IllegalStateException e) { return null;                        
                } catch (IOException e) { return null; }
                return markInputStream;
             }
             return null;
        }
        
        private String getDateTime() 
        {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd - HH:mm");
            Date date = new Date();
            return dateFormat.format(date);
        }
        
        protected void onPostExecute(InputStream result) 
        { 
             if (result != null) 
             { 
            	String ChalkmarksJSON = "";
                BufferedReader reader = new BufferedReader(new InputStreamReader(result));
                String s = "";
    	        try { while ((s = reader.readLine()) != null) { ChalkmarksJSON += s; }
    	        } catch (IOException e) { e.printStackTrace();}
   	        
                if (ChalkmarksJSON.length() > 0)
                {  
                	JSONObject jObject;
                    try  { 
                		jObject = new JSONObject(ChalkmarksJSON);
                		JSONArray menuitemArray = jObject.getJSONArray("marks");
        				int N = menuitemArray.length();       	
       	                for (int i = 0; i < N; i++) 
       	                {
       	                	String myuid = mPreferences.getString("UserName","");
       	                	String markStatus = "unread";
       	                 	String from_uid = menuitemArray.getJSONObject(i).getString("from_uid").toString();
       						String from_name = menuitemArray.getJSONObject(i).getString("from_name").toString();
       						String from_image = menuitemArray.getJSONObject(i).getString("from_image").toString();
       						String subject = menuitemArray.getJSONObject(i).getString("subject").toString();
       						String body = menuitemArray.getJSONObject(i).getString("body").toString();
       						String imageURL = menuitemArray.getJSONObject(i).getString("imageurl").toString();
       						Float lat = Float.valueOf(menuitemArray.getJSONObject(i).getString("lat").toString());
       						Float lon = Float.valueOf(menuitemArray.getJSONObject(i).getString("lon").toString());
       						String realAddress = menuitemArray.getJSONObject(i).getString("real_address").toString();
       						int expiration = Integer.parseInt(menuitemArray.getJSONObject(i).getString("expiration").toString()); // days
       						String timedropped = menuitemArray.getJSONObject(i).getString("timedropped").toString(); // currently th unix time 
       						String imagepair = menuitemArray.getJSONObject(i).getString("imagepair").toString(); 
       						
       						long received_msec = System.currentTimeMillis();
       						double received_sec = (double) (received_msec / 1000.0);           						
       						String datereceived = getDateTime();
       						
       						dbhelper.insert(myuid, 
       	                    		from_uid, from_name, from_image, 
       	                    		subject, body, imageURL, markStatus, 
       	                    		(double) lat, (double) lon, realAddress, 
       	                    		expiration, timedropped, datereceived, received_sec, 
       	                    		imagepair);	   
       	                }           	                
           				
   	             	} catch (JSONException e1) { e1.printStackTrace(); }
                } // End parsing of received mark: i.e., ends the "if (marksJSON.length() > 0)" statement
                SharedPreferences.Editor editor = mPreferences.edit();
                editor.putInt("DefaultTab", 1);
                editor.commit();
   				Intent i=new Intent(MyMarks.this, ChalkMarkTabHost.class);
   				startActivity(i);
            }
             
        } // end "onPostExecute()"
    }


    
    
	/* ========================================================================================================== 
	 * DropMessageOverlay: Overlay for the draggable cross for leaving a mark
	 * ==========================================================================================================
	 */
    
    class DropMessageOverlay extends DropBalloonItemizedOverlay<OverlayItem> 
    {

    	private ArrayList<OverlayItem> overlay_items = new ArrayList<OverlayItem>();
    	private OverlayItem inDrag=null;
    	
    	public DropMessageOverlay(Drawable defaultMarker, MapView mapView) {
    		super(boundCenter(defaultMarker), mapView);
    	}

    	public void addOverlay(OverlayItem overlay) 
    	{
    		overlay_items.add(overlay);
    	    populate();
    	    displayBaloon(size()-1);  
    	}

    	@Override
    	protected OverlayItem createItem(int i) 
    	{
    		return overlay_items.get(i);
    	}

    	@Override
    	public int size() {
    		return overlay_items.size();
    	}

    	@Override
    	protected boolean onBalloonTap(int index, OverlayItem item) 
    	{
    		if (mapOverlays.contains(droppingMessageOverlay)) mapOverlays.remove(droppingMessageOverlay);
    	    Intent i=new Intent(MyMarks.this, MakeMark.class);
        	startActivity(i);
    		return true;
    	}
    	
        @Override
        public boolean onTouchEvent(MotionEvent event, MapView mapView) 
        {
    		  final int action=event.getAction();
    		  final int x=(int)event.getX();
    		  final int y=(int)event.getY();
    		  boolean result=false;
    		  
    		  if (action==MotionEvent.ACTION_DOWN) 
    		  {
    			  droppingMessageOverlay.hideBalloon();
    			  for (OverlayItem item : overlay_items) 
    			  {
	    		      Point p=new Point(0,0);    		      
	    		      myMapView.getProjection().toPixels(item.getPoint(), p);
	    		      if (hitTest(item, dragmarker, x-p.x, y-p.y)) 
	    		      {
		    		        result=true;
		    		        inDrag=item;
		    		        overlay_items.remove(inDrag);
		    		        populate(); 
		    		        xDragTouchOffset=0;
		    		        yDragTouchOffset=0;    		        
		    		        setDragImagePosition(p.x, p.y);
		    		        dragImage.setVisibility(View.VISIBLE);    		
		    		        xDragTouchOffset=x-p.x;
		    		        yDragTouchOffset=y-p.y;    		        
		    		        break;
	    		      }
    			  }
    		  }
    		  else if (action==MotionEvent.ACTION_MOVE && inDrag!=null) {
    		    setDragImagePosition(x, y);
    		    result=true;
    		  }
    		  
    		  else if (action==MotionEvent.ACTION_UP && inDrag!=null) 
    		  {  	  
    		    dragImage.setVisibility(View.GONE);
    		    GeoPoint pt = myMapView.getProjection().fromPixels(x-xDragTouchOffset,y-yDragTouchOffset);
    		    myMapView.getController().animateTo(pt);
    		    recordDropMarkLocation(pt);    			
    		    OverlayItem toDrop = new OverlayItem(pt, inDrag.getTitle(), inDrag.getSnippet());       
    		    overlay_items.add(toDrop);    		    
    		    populate();     		    
    		    displayBaloon(size()-1);    		    
    		    inDrag=null;
    		    result=true;    		    
    		  }    		  
    		  return(result || super.onTouchEvent(event, mapView));
        }
        
        private void setDragImagePosition(int x, int y) 
        {
            TableLayout.LayoutParams lp = (TableLayout.LayoutParams) dragImage.getLayoutParams();                  
            lp.setMargins(x-xDragImageOffset-xDragTouchOffset,y-yDragImageOffset-yDragTouchOffset, 0, 0);
            dragImage.setLayoutParams(lp);
        }
    }
    

    
    private void recordDropMarkLocation(GeoPoint pt)
    {
	    Float lat = (float) (pt.getLatitudeE6() / 1E6);
		Float lon = (float) (pt.getLongitudeE6()/ 1E6);
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putFloat("MarkLat", lat);
		editor.putFloat("MarkLon", lon);
		editor.commit(); 
    }
    
       
    public void dropMarkOnPoint(GeoPoint point)
    {
    	recordDropMarkLocation(point);
    	if (mapOverlays.contains(droppingMessageOverlay)) mapOverlays.remove(droppingMessageOverlay);
	    Intent i=new Intent(MyMarks.this, MakeMark.class);
    	startActivity(i);
    }
    
     
    public void placeDropMarkOverlayOnMap()
    {   
    	for (Overlay overlay : mapOverlays) {
			if (overlay instanceof DropBalloonItemizedOverlay<?>) {
				((DropBalloonItemizedOverlay<?>) overlay).hideBalloon();
			}
		}    	
    	if (mapOverlays.contains(droppingMessageOverlay)) mapOverlays.remove(droppingMessageOverlay);
    	Drawable drawable = getResources().getDrawable(R.drawable.marker_cross);
    	droppingMessageOverlay = new DropMessageOverlay(drawable, myMapView);
    	OverlayItem overlay;    	
    	GeoPoint center = myMapView.getMapCenter();
    	recordDropMarkLocation(center);
		overlay = new OverlayItem(center, "Click to Drop at This Location", "or drag marker to a different location");
		droppingMessageOverlay.addOverlay(overlay);
        mapOverlays.add(droppingMessageOverlay);
	    myMapView.invalidate();
    }

    
	
	/* ========================================================================================================== 
	 * SendForChalkmarksTask: Background task that populates the map with available chalkmarks
	 * ==========================================================================================================
	 */    
    
    private class SendForChalkmarksTask extends AsyncTask<List<NameValuePair>, Void, InputStream> 
    {
            protected InputStream doInBackground(List<NameValuePair>...nameValuePairs) 
            {
            	 for (List<NameValuePair> nameValuePair : nameValuePairs) 
                 { 
                    HttpClient http_client = new DefaultHttpClient();
                    HttpResponse response = null;
                    HttpPost httppost = new HttpPost("http://eggdroplabs.com:9002/available_marks/");
                    InputStream markInputStream = null;                    
                    try {
                            httppost.setEntity(new UrlEncodedFormEntity(nameValuePair));
                            response = http_client.execute(httppost);                            
                    } catch (ClientProtocolException e) { return null;     					
    				} catch (IOException e) { return null; }                     
                    int http_status = response.getStatusLine().getStatusCode();
                    if (http_status !=  HttpStatus.SC_OK) { return null; }                     
                    try { markInputStream = response.getEntity().getContent();                    	
                    } catch (IllegalStateException e) { return null;                            
                    } catch (IOException e) { return null;}
                    return markInputStream;
                 }
                 return null;
            }
            
            @Override
    		protected void onPreExecute() 
            { 
            	progress_dialog = new ProgressDialog(MyMarks.this);
    	        progress_dialog.setMessage("Refreshing Chalkmark Flags!");
    	        progress_dialog.setIndeterminate(true);
    	        progress_dialog.setCancelable(false);
    	        progress_dialog.show();
            }
            
            @Override
    		protected void onPostExecute(InputStream result) 
            { 
            	progress_dialog.dismiss();
            	if (result != null) { 
                	String marksJSON = ""; String s = "";
                	BufferedReader reader = new BufferedReader(new InputStreamReader(result));
        	        try { while ((s = reader.readLine()) != null) { marksJSON += s; }
        	        } catch (IOException e) { e.printStackTrace();}        	        
        	        if (mapOverlays.contains(ChalkmarksOverlay)) mapOverlays.remove(ChalkmarksOverlay);
        	        if (marksJSON.length() > 0) { extractAvailableMarksFromJsonString(marksJSON);
                    } else { displayNoMarksMessage(); }
                 }                      
            } // end "onPostExecute()"
    }
    
    @SuppressWarnings("unchecked")
    public void executeSendForAvailableChalkmarks(List<NameValuePair> mylist) {
    	 new SendForChalkmarksTask().execute(mylist);
    }
    
    
    public void extractAvailableMarksFromJsonString(String marksJSON)
    {
    	Drawable drawable = getResources().getDrawable(R.drawable.marker_flag);
		ChalkmarksOverlay = new AvailableMarksItemizedOverlay(drawable, myMapView);		
		JSONObject jObject;		
		try  { 
			jObject = new JSONObject(marksJSON);
			JSONArray menuitemArray = jObject.getJSONArray("marks");
			int N = menuitemArray.length();		          	                 
			GeoPoint point;
		   	OverlayItem overlay;
			
           	for (int i = 0; i < N; i++) {
				String from = menuitemArray.getJSONObject(i).getString("from_name").toString();
				String subject = menuitemArray.getJSONObject(i).getString("subject").toString();
				Float lat = Float.valueOf(menuitemArray.getJSONObject(i).getString("lat").toString());
				Float lon = Float.valueOf(menuitemArray.getJSONObject(i).getString("lon").toString());
				String expiration = menuitemArray.getJSONObject(i).getString("expiration").toString(); 		
				String radius_yards = menuitemArray.getJSONObject(i).getString("radius").toString();
				
	            from = "From: " + from;
	            if (subject.equals("")) {
	            	subject = "Expires in "+expiration+" Days\n";
	            	subject = subject + "No Message Summary Provided"+
	            	"\n\nPick-Up Chalkmark for More Stuff";
	            } else {
	            	subject = "Expires in " + expiration + " Days\n"+
	            	"Pick-up radius: " + radius_yards + " yards\n" +
	            	"Message Summary:  "+ subject +
	            	"\n\nPick-Up Chalkmark for More Stuff";
	            }	            
	            point = new GeoPoint((int)(lat*1E6),(int)(lon*1E6));
	            overlay = new OverlayItem(point, from, subject);
	            ChalkmarksOverlay.addOverlay(overlay);
		   	}
           	mapOverlays.add(ChalkmarksOverlay);
           	myMapView.refreshDrawableState();
    	    myMapView.invalidate();           
           	String mymsg = N+" available Chalkmarks.\nZoom out to see on map";
       		Toast.makeText(getBaseContext(), mymsg, Toast.LENGTH_SHORT).show();			           
	    } catch (JSONException e1) {  }
	}
    
    public void displayNoMarksMessage() {
    	String mymsg = "There are no Chalkmarks Available.";
    	Toast.makeText(getBaseContext(), mymsg, Toast.LENGTH_LONG).show();
    }

    
    
	/* ========================================================================================================== 
	 * Menu Functions and Buttons
	 * ==========================================================================================================
	 */
	
	public void logMeOut()
	{
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putString("UserName", "UserIsLoggedOut");
		editor.putInt("DefaultTab", 0);
		editor.putString("DestinationContacts", "");
		editor.commit();
		stopService(new Intent(MyMarks.this, LocService.class));
		Intent intent = new Intent().setClass(MyMarks.this, ChalkMark.class);
		startActivity(intent);
		finish();  
	}
	
	public void onLocationChanged(Location location) { }
            
    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }
	
    @Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		new MenuInflater(this).inflate(R.menu.my_marks_options, menu);
		return(super.onCreateOptionsMenu(menu));
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		if (item.getItemId()==R.id.sign_off) 
		{
			AlertDialog alertDialog = new AlertDialog.Builder(this).create(); 
			alertDialog.setTitle("Logging Out"); 			
			alertDialog.setMessage("Are you sure want to exit?");
			
			alertDialog.setButton("Yes", new DialogInterface.OnClickListener() 
			{  
			      public void onClick(DialogInterface dialog, int which) {  
						logMeOut();
			      }
			});   
			alertDialog.setButton2("No", new DialogInterface.OnClickListener() 
			{  
			      public void onClick(DialogInterface dialog, int which) {
			    	  // don't do anyting
			      }
			});  
			alertDialog.show(); 
			
		}
		
		else if (item.getItemId()==R.id.span) {
			if (mapOverlays.contains(ChalkmarksOverlay)) 
			{
				myMapView.getController().zoomToSpan(ChalkmarksOverlay.getLatSpanE6(), ChalkmarksOverlay.getLonSpanE6());
				//myMapView.getController().setCenter(ChalkmarksOverlay.getCenter());
				myMapView.getController().animateTo(ChalkmarksOverlay.getCenter());
			} else {
				String mymsg = "There are no Chalkmarks Available.";
		    	Toast.makeText(getBaseContext(), mymsg, Toast.LENGTH_LONG).show();
			}
		}
		
		else if (item.getItemId()==R.id.here) {
			// drop message at my location
			GeoPoint myGeoPoint = myLocOverlay.getMyLocation();
	    	if (myGeoPoint == null) {
	    		Float mylat = mPreferences.getFloat("Lat", (float) 0.0);
	    		Float mylon = mPreferences.getFloat("Lon", (float) 0.0);
	    		myGeoPoint = new GeoPoint((int)(mylat*1E6),(int)(mylon*1E6)); 
	    	}
	    	dropMarkOnPoint(myGeoPoint);
		}
		
		else if (item.getItemId()==R.id.mine) 
		{
			Cursor myMarksCursor = mymarksdb.getAllMarks("myuid");
			myMarksCursor.moveToFirst();
			int rows = myMarksCursor.getCount();
			if (rows == 0) {
				Toast.makeText(getBaseContext(), "0 Chalkmarks Dropped By Me", Toast.LENGTH_SHORT).show();
			} 
			else 
			{
	    	    Toast.makeText(getBaseContext(), rows + " Chalkmarks Dropped By Me", Toast.LENGTH_SHORT).show();
				Drawable drawable = getResources().getDrawable(R.drawable.marker_mymark);
				MyMarksOverlay = new MyMarksItemizedOverlay(drawable, myMapView);
				GeoPoint point;
				OverlayItem overlay;
				while (myMarksCursor.isAfterLast() == false) 
				{
					String imageurl = mymarksdb.getImage(myMarksCursor);
					String subject = mymarksdb.getSubject(myMarksCursor);
					String body = mymarksdb.getBody(myMarksCursor);
					int radius_yards = mymarksdb.getRadius(myMarksCursor);
					if (imageurl.equals("")) subject = "Image: (no image)\nSummary: "+subject;
					else subject = "Image: Yes\n\nSummary: " + subject;
					String bodytext = "\nRadius: " + radius_yards + " yards";
					bodytext = bodytext + "\nText: " + body; 
					point = new GeoPoint((int)(mymarksdb.getLat(myMarksCursor)*1E6),(int)(mymarksdb.getLon(myMarksCursor)*1E6));
					overlay = new OverlayItem(point, "My Chalkmark", subject + bodytext);            	            
					MyMarksOverlay.addOverlay(overlay);
					myMarksCursor.moveToNext();
				}
	           	mapOverlays.add(MyMarksOverlay);
	           	myMapView.refreshDrawableState();
	    	    myMapView.invalidate();  
		    }
		}
		
		else if (item.getItemId()==R.id.hidemine) 
		{	
			for (Overlay overlay : mapOverlays) {
				if (overlay instanceof MyMarksBalloonItemizedOverlay<?>) {
					((MyMarksBalloonItemizedOverlay<?>) overlay).hideBalloon();
				}
			} 
			if (mapOverlays.contains(MyMarksOverlay)) mapOverlays.remove(MyMarksOverlay);
			myMapView.refreshDrawableState();
		    myMapView.invalidate();	
		}
		
		return(super.onOptionsItemSelected(item));
	}
	
	@Override
	public void onBackPressed() {
	   return;
	}


	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub	
	}
}


//public void getMyChalkmarks()
//{	
//	if (mapOverlays.contains(droppingMessageOverlay)) mapOverlays.remove(droppingMessageOverlay);    	
//	
//	GeoPoint center = myMapView.getMapCenter();
//	Integer latInt = center.getLatitudeE6();
//	Integer lonInt = center.getLongitudeE6();
//	Float lat = (float) (latInt / 1E6);
//	Float lon = (float) (lonInt / 1E6);		
//	
//	List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
//  nameValuePairs.add(new BasicNameValuePair("username",(String) myuid));
//  nameValuePairs.add(new BasicNameValuePair("lat",String.valueOf(lat)));
//  nameValuePairs.add(new BasicNameValuePair("lon",String.valueOf(lon)));   
//  
//  executeSendForMyChalkmarks(nameValuePairs); 	
//}
//
//
//
///* ========================================================================================================== 
//* SendForMyChalkmarksTask: Background task that populates the map with my chalkmarks
//* ==========================================================================================================
//*/    
//
//private class SendForMyChalkmarksTask extends AsyncTask<List<NameValuePair>, Void, InputStream> 
//{
//      protected InputStream doInBackground(List<NameValuePair>...nameValuePairs) 
//      {
//      	 for (List<NameValuePair> nameValuePair : nameValuePairs) 
//           { 
//              HttpClient http_client = new DefaultHttpClient();
//              HttpResponse response = null;
//              HttpPost httppost = new HttpPost("http://eggdroplabs.com:9002/my_marks/");
//              InputStream markInputStream = null;                    
//              try {
//                      httppost.setEntity(new UrlEncodedFormEntity(nameValuePair));
//                      response = http_client.execute(httppost);                            
//              } catch (ClientProtocolException e) { return null;     					
//				} catch (IOException e) { return null; }                     
//              int http_status = response.getStatusLine().getStatusCode();
//              if (http_status !=  HttpStatus.SC_OK) { return null; }                     
//              try { markInputStream = response.getEntity().getContent();                    	
//              } catch (IllegalStateException e) { return null;                            
//              } catch (IOException e) { return null;}
//              return markInputStream;
//           }
//           return null;
//      }
//      
//      @Override
//		protected void onPreExecute() 
//      { 
//      	progress_dialog = new ProgressDialog(MyMarks.this);
//	        progress_dialog.setMessage("Getting My Chalkmarks!");
//	        progress_dialog.setIndeterminate(true);
//	        progress_dialog.setCancelable(false);
//	        progress_dialog.show();
//      }
//      
//      @Override
//		protected void onPostExecute(InputStream result) 
//      { 
//      	progress_dialog.dismiss();
//      	if (result != null) 
//      	{ 
//          	String marksJSON = ""; String s = "";
//          	BufferedReader reader = new BufferedReader(new InputStreamReader(result));
//  	        try { while ((s = reader.readLine()) != null) { marksJSON += s; }
//  	        } catch (IOException e) { e.printStackTrace();}        	        
//  	        if (mapOverlays.contains(MyMarksOverlay)) mapOverlays.remove(MyMarksOverlay);
//  	        if (marksJSON.length() > 0) { extractMyMarksFromJsonString(marksJSON);
//              } else { displayNoMarksMessage(); }
//           }                      
//      }
//}
//
//@SuppressWarnings("unchecked")
//public void executeSendForMyChalkmarks(List<NameValuePair> mylist) {
//	 new SendForMyChalkmarksTask().execute(mylist);
//}
//
//
//public void extractMyMarksFromJsonString(String marksJSON)
//{
//	Drawable drawable = getResources().getDrawable(R.drawable.marker_mymark);
//	MyMarksOverlay = new MyMarksItemizedOverlay(drawable, myMapView);		
//	JSONObject jObject;		
//	try  { 
//		jObject = new JSONObject(marksJSON);
//		JSONArray menuitemArray = jObject.getJSONArray("marks");
//		int N = menuitemArray.length();		          	                 
//		GeoPoint point;
//	   	OverlayItem overlay;
//		
//     	for (int i = 0; i < N; i++) 
//     	{
//			String subject = menuitemArray.getJSONObject(i).getString("subject").toString();
//			String body = menuitemArray.getJSONObject(i).getString("body").toString();
//			Float lat = Float.valueOf(menuitemArray.getJSONObject(i).getString("lat").toString());
//			Float lon = Float.valueOf(menuitemArray.getJSONObject(i).getString("lon").toString());
//			String expiration = menuitemArray.getJSONObject(i).getString("expiration").toString(); 		
//			String radius_yards = menuitemArray.getJSONObject(i).getString("radius").toString();
//			String imageurl = menuitemArray.getJSONObject(i).getString("imageurl").toString();
//			// String msgid = menuitemArray.getJSONObject(i).getString("msgid").toString();
//			
//	    	if (imageurl.equals("")) subject = "Image: (no image)\nSummary: "+subject;
//	    	else subject = "Image: Yes\n\nSummary: "+subject;
//	    	String bodytext = "\nExpiration: "+expiration+ " days";
//	    	bodytext = bodytext + "\nRadius: " + radius_yards + " yards";
//	    	bodytext = bodytext + "\nText: " + body; 
//	    	point = new GeoPoint((int)(lat*1E6),(int)(lon*1E6));
//          overlay = new OverlayItem(point, "My Chalkmark", subject + bodytext);            	            
//          MyMarksOverlay.addOverlay(overlay);	            
//	   	}
//     	mapOverlays.add(MyMarksOverlay);
//     	myMapView.refreshDrawableState();
//	    myMapView.invalidate();           
//     	String mymsg = N+" Chalkmarks Belonging to Me.\nZoom out to see on map";
// 		Toast.makeText(getBaseContext(), mymsg, Toast.LENGTH_SHORT).show();			           
//  } catch (JSONException e1) { 
//  }
//}


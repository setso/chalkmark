
                
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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;


public class  LocService extends Service implements LocationListener
{
        private static final int NOTIFY_ME_ID = 1234;
        String TAG = "CHALKMARK";
        TextView txtInfo;
        
        LocationManager myLocManager;
        
        Location previouslocation = null;
        static double lat=0.0;
        static double lon=0.0;
        int locationheld_count = 0;
        
        int noOfFixes = 0;
        long countloc = 0;
        long countmark = 0;
        float accuracy =0;
        float speed =0; 
        
        String myuid = "";
                
        private SharedPreferences mPreferences;
        
        InputStream GlobalMarkInputStream = null;
        private NotificationManager notif_mgr=null;
        private Notification ChalkmarkNotification;
        
        List<NameValuePair> nameValuePairs = null;	// used to hold the info sent to the server when checking for a Chalkmark
        
        private boolean sendingLocationToServer = false;
        MarksDataBaseHelper dbhelper; 
        volatile float battery_level=0; 
        
        @Override
        public IBinder onBind(Intent intent) {
                return null;
        }
        
        
/*  Note (from AndGuide Tutorial, pg 462):
 * 
        Some services will be missed by the user if they mysteriously vanish. 
        For example, the default music player application that ships with Android uses a 
        service for the actual music playback. That way, the user can listen to music while 
        continuing to use their phone for other purposes. The service only stops when the 
        user goes in and presses the stop button in the music player activity. If that 
        service were to be shut down unexpectedly, the user might wonder what is wrong.
        
        Services like this can declare themselves as being part of the "foreground". This 
        will cause their priority to rise and make them less likely to be bumped out of memory. 
        The trade-off is that the service has to maintain a Notification, so the user knows 
        that this service is claiming part of the foreground. And, ideally, that Notification 
        provides an easy path back to some activity where the user can stop the service.
        
        To do this, on onCreate() of your service (or wherever else in the service's life it 
        would make sense), call startForeground(). This takes a Notification and a 
        locally-unique  integer, just like the notify() method on NotificationManager. It 
        causes the Notification to appear and moves the service into foreground priority. 
        Later on, you can call stopForeground() to return to normal priority.
*/
        
        @Override
        public void onCreate() 
        {
            super.onCreate();
            
            locationheld_count = 0;
            
            dbhelper = new MarksDataBaseHelper(this);   
            myLocManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            notif_mgr=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            
            ChalkmarkNotification = new Notification(R.drawable.fried_egg,"New Mark!",System.currentTimeMillis());
            ChalkmarkNotification.defaults |= Notification.DEFAULT_SOUND;
            ChalkmarkNotification.defaults |= Notification.DEFAULT_LIGHTS;
            ChalkmarkNotification.flags |= Notification.FLAG_AUTO_CANCEL;
            
            mPreferences = getSharedPreferences("CurrentUser", MODE_PRIVATE);
            myuid = mPreferences.getString("UserName","");
            
            batteryLevel(); 
            
            // Declare this is service as part of the foreground. Read above
            Intent i = new Intent(this, ChalkMark.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
            Notification foregroundNote = new Notification(R.drawable.ic_launcher, "ChalkMark", System.currentTimeMillis());
            foregroundNote.setLatestEventInfo(this, "ChalkMark", "Place Messages Anywhere", pi);
            foregroundNote.flags|=Notification.FLAG_NO_CLEAR;
            startForeground(9041979, foregroundNote);
        }
       
        
        private void batteryLevel() {
            BroadcastReceiver batteryLevelReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    context.unregisterReceiver(this);
                    int rawlevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    if (rawlevel >= 0 && scale > 0) {
                        battery_level = (float)((rawlevel * 100) / scale);
                    }
                }
            };
            IntentFilter batteryLevelFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            registerReceiver(batteryLevelReceiver, batteryLevelFilter);
        }
        
    
        public int onStartCommand(Intent intent, int flags, int start_id)
        {
        	locationheld_count = 0;
            myLocManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);  
            //myLocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 180000, 500, this);  
            previouslocation = myLocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            return START_STICKY; 
        }
        
        
        public int onStopCommand(Intent intent, int flags, int start_id)
        {
            myLocManager.removeUpdates(this);
            dbhelper.close();
            return 0; 
        }
                
                
        @Override
        public void onDestroy() 
        {
            myLocManager.removeUpdates(this);
            super.onDestroy();
        }
        
        
        public void onLocationChanged(Location newlocation) 
        {                   
            if (newlocation == null) return; // no location set yet
                        
            // Check if the new location is better than the old location
            if (!isBetterLocation(newlocation, previouslocation)) return;
          
            // The new location is likely better than the old location.
            // If we have already collected the location at least once before and the distance
            // of the new location and the previous location is < 200 meters then we don't
            // need to send the location to the server again and poll for new messages. 
            if ((previouslocation != null) && (locationheld_count < 5)) 
            {     
            	int accuracyDelta = (int) (newlocation.getAccuracy() - previouslocation.getAccuracy());
                boolean isLessAccurate = accuracyDelta > 0; // is the new location less accurate?
            	if ((previouslocation.distanceTo(newlocation) <= 200) && (isLessAccurate))
            	{            		
            		locationheld_count += 1;
            		return;
            	}
            }
            
            lat = newlocation.getLatitude();
            lon = newlocation.getLongitude();
            accuracy = newlocation.getAccuracy();
            locationheld_count = 0;            
            SharedPreferences.Editor editor = mPreferences.edit();
            editor.putFloat("Lat", (float) lat);
            editor.putFloat("Lon", (float) lon);
            editor.commit(); 
            
            previouslocation = myLocManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);     
            
            if (sendingLocationToServer == true) return;
			getAvailableChalkmarks();			
        }
        
        
        /** Determines whether one Location reading is better than the current Location fix
         * @param location  The new Location that you want to evaluate
         * @param currentBestLocation  The current Location fix, to which you want to compare the new one
         */
       protected boolean isBetterLocation(Location location, Location currentBestLocation) 
       {
    	   int TWO_MINUTES = 1000 * 60 * 2;
    	   
           if (currentBestLocation == null) {
               // A new location is always better than no location
               return true;
           }

           // Check whether the new location fix is newer or older
           long timeDelta = location.getTime() - currentBestLocation.getTime();
           boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
           boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
           boolean isNewer = timeDelta > 0;

           // If it's been more than two minutes since the current location, use the new location
           // because the user has likely moved
           if (isSignificantlyNewer) {
               return true;
           // If the new location is more than two minutes older, it must be worse
           } else if (isSignificantlyOlder) {
               return false;
           }

           // Check whether the new location fix is more or less accurate
           int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
           boolean isLessAccurate = accuracyDelta > 0;
           boolean isMoreAccurate = accuracyDelta < 0;
           boolean isSignificantlyLessAccurate = accuracyDelta > 200;


           // Determine location quality using a combination of timeliness and accuracy
           if (isMoreAccurate) { return true;
           } else if (isNewer && !isLessAccurate) { return true;
           } else if (isNewer && !isSignificantlyLessAccurate) { return true; } 
           return false;
       }

        

        public void getAvailableChalkmarks()
        {
            // holds the data sent to the server when polling for a Chalkmark
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("username",(String) myuid));
            nameValuePairs.add(new BasicNameValuePair("lat",String.valueOf(lat)));
            nameValuePairs.add(new BasicNameValuePair("lon",String.valueOf(lon)));
            
            // additional info that we should send
            nameValuePairs.add(new BasicNameValuePair("acc",String.valueOf(accuracy)));
            nameValuePairs.add(new BasicNameValuePair("bat",String.valueOf(battery_level)));

            // Send the location to the server and Pickup the "Marks" if there are any at this location
            sendingLocationToServer = true;
            executeSendLocation(nameValuePairs);
        }
                
        
        public void onProviderDisabled(String provider) 
        { /* this is called if/when the GPS is disabled in settings */
                // Log.i(TAG, "[LocService:onProviderDisabled]: GPS Disabled");
                // Toast.makeText(getBaseContext(), "GPS is Disabled", Toast.LENGTH_LONG).show();
        }

        
        public void onProviderEnabled(String provider) { }
        
        
        public void onStatusChanged(String provider, int status, Bundle extras) 
        {
                /* This is called when the GPS status alters */
                switch (status) {
                        case LocationProvider.OUT_OF_SERVICE: break;
                        case LocationProvider.TEMPORARILY_UNAVAILABLE: break;
                        case LocationProvider.AVAILABLE: break;
                }
        }
        
        
        private class SendLocationTask extends AsyncTask<List<NameValuePair>, Void, InputStream> 
        {
                protected InputStream doInBackground(List<NameValuePair>...nameValuePairs) 
                {
                	 for (List<NameValuePair> nameValuePair : nameValuePairs) 
                	 { 
                        HttpClient http_client = new DefaultHttpClient();
                        HttpResponse response = null;
                        HttpPost httppost = new HttpPost("http://eggdroplabs.com:9002/egg_loc/");
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
	                    } catch (IOException e) { return null; }
                        
	                    return markInputStream;	                    
                     }
                     return null;
                }
                
        		@Override
        		protected void onPreExecute()
        		{
        			sendingLocationToServer = true;
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
                            ChalkmarkNotification.number = mPreferences.getInt("ChalkmarksNumber", 0);
                            
                        	try  
                        	{ 
                        		jObject = new JSONObject(ChalkmarksJSON);
                        		JSONArray menuitemArray = jObject.getJSONArray("marks");
                				int N = menuitemArray.length();
           	
	           	                for (int i = 0; i < N; i++) 
	           	                {
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
	       
	           						ChalkmarkNotification.number = ChalkmarkNotification.number + 1;    
	           	                   
	           	                    dbhelper.insert(myuid, 
	           	                    		from_uid, from_name, from_image, 
	           	                    		subject, body, imageURL, markStatus, 
	           	                    		(double) lat, (double) lon, realAddress, 
	           	                    		expiration, timedropped, datereceived, received_sec,
	           	                    		imagepair);	   
	           	                }
	           	                
	           	                // Update the number of received Chalkmarks in the notification display
	           	                SharedPreferences.Editor editor = mPreferences.edit();
	           	        		editor.putInt("ChalkmarksNumber", ChalkmarkNotification.number);
	           	        		editor.commit();
	           	        		
	           	        		// Raise a New Chalkmarks notification
	           	        		String notificationTitle = String.valueOf(ChalkmarkNotification.number) + " New Geo Marks Received";
        	                    String notificationText  = "Click to View in ChalkMark";        	                    
        	                    generateNewChalkmarkNotification(notificationTitle, notificationText);
        	                    
           	             	} catch (JSONException e1) {  }
                        } // End parsing of received mark: i.e., ends the "if (marksJSON.length() > 0)" statement
                     } 
                     sendingLocationToServer = false;      
                } // end "onPostExecute()"
        }
        
        @SuppressWarnings("unchecked")
        public void executeSendLocation(List<NameValuePair> mylist) 
        {
        	 Log.v("CHALKMARK","Sending Location");
        	 new SendLocationTask().execute(mylist);
        }

        
        private void generateNewChalkmarkNotification(String notificationTitle, String notificationText)
        {
        	// Setting the DefaultTab to 1, redirects the user to the ChalkBoard
            SharedPreferences.Editor editor = mPreferences.edit();
            editor.putInt("DefaultTab", 1);
            editor.commit();
            
        	Context context = getApplicationContext();
        	Intent MarkNotifyIntent = new Intent(context, ChalkMarkTabHost.class);
            PendingIntent markNoteIntent = PendingIntent.getActivity(this, 0, MarkNotifyIntent, android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            ChalkmarkNotification.setLatestEventInfo(context, notificationTitle, notificationText, markNoteIntent);
            notif_mgr.notify(NOTIFY_ME_ID, ChalkmarkNotification);
        }
        
        
	    private String getDateTime() 
	    {
	        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd - HH:mm");
	        Date date = new Date();
	        return dateFormat.format(date);
	    }
    
}
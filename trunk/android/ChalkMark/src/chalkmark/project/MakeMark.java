/* File: MakeMark.java
 * 
 * Main Activity for dropping an Mark. This activity corresponds to a separate tab in the
 * main ChalkMark application.
 * 
 */

package chalkmark.project;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


public class MakeMark extends Activity implements SeekBar.OnSeekBarChangeListener
{
	double current_lat; 
	double current_lon;
	String mySubject;
	String myBody;
	String location = null;
	String myuid = null;
	
	private Button btnRecipients = null;
	private Button dropMarkButton = null;
	private ImageView cameraButton = null;
	private Button cancelButton = null;
	EditText subjectView = null;
	EditText bodyView = null;
	private SharedPreferences mPreferences;

	byte[] buffer;
	
	SeekBar expirationSeekBar;
    TextView mProgressTextView;    
    int expiration_days = 25;
    
	SeekBar radiusSeekBar;
    TextView radiusTextView;    
    int radius_yards = 1000;
    
    MyMarksDataBaseHelper mymarksdb; 
    private int PICTURE_ACTIVITY = 1;
    private ProgressDialog progress_dialog;
    
    
    public void onCreate(Bundle savedInstanceState) 
    {    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.make_mark);
        
        progress_dialog = new ProgressDialog(MakeMark.this);
    	progress_dialog.setMessage("Making Chalkmark!");
    	progress_dialog.setIndeterminate(true);
    	progress_dialog.setCancelable(false);
        
		mymarksdb = new MyMarksDataBaseHelper(this); 
		mPreferences = getSharedPreferences("CurrentUser", MODE_PRIVATE);         	    
        myuid = mPreferences.getString("UserName", null);
		
        subjectView = (EditText) findViewById(R.id.subject);
		bodyView = (EditText) findViewById(R.id.body);
		
		cameraButton = (ImageView) findViewById(R.id.camera);
		
		cameraButton.setOnClickListener(new OnClickListener() 
		{
			public void onClick(View v) 
			{
				Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); 
				startActivityForResult(cameraIntent, PICTURE_ACTIVITY);
			}
		});
		
		mProgressTextView = (TextView)findViewById(R.id.progress);
		mProgressTextView.setText("Expiration 25 days");
		expirationSeekBar = (SeekBar) findViewById(R.id.seekbar);
		expirationSeekBar.setOnSeekBarChangeListener(this);
		
		radiusTextView = (TextView)findViewById(R.id.radius);
		radiusTextView.setText("Pick-Up Radius: 1000 yards");
		radiusSeekBar = (SeekBar) findViewById(R.id.seekbar2);
		radiusSeekBar.setOnSeekBarChangeListener(this);
		
		// get handles on the buttons so that we can handler events
		dropMarkButton = (Button)findViewById(R.id.drop);	
		btnRecipients = (Button) findViewById(R.id.people);
		cancelButton = (Button) findViewById(R.id.cancel); 
		
		// Choose a handler for each button
		dropMarkButton.setOnClickListener(onDrop);
		btnRecipients.setOnClickListener(onSelectRecipients);
		cancelButton.setOnClickListener(onCancelMark);
    }

    
    @Override
    protected void onResume() 
    {
    	super.onResume();
    	   
    	String contactUids = mPreferences.getString("DestinationContacts", "");
    	String[] uids = contactUids.split(",");
    	
		if (uids[0].length() == 0) 
			Toast.makeText(getBaseContext(), 
					"No Contacts Selected", Toast.LENGTH_SHORT).show();
		else
			Toast.makeText(getBaseContext(), 
				"Selected " + uids.length + " Contacts: " + mPreferences.getString("SelectedContacts", ""), 
				Toast.LENGTH_SHORT).show(); 
			
    	
    	String savedSubject = mPreferences.getString("SavedSubject", "");
    	String savedBody = mPreferences.getString("SavedBody", "");
    	expiration_days = mPreferences.getInt("SavedExpiration", 25);
    	radius_yards = mPreferences.getInt("SavedRadius", 1000);
    	boolean imageFlag = mPreferences.getBoolean("SavedImageFlag", false);
    	
    	subjectView.setText(savedSubject);
		bodyView.setText(savedBody);

    	if (imageFlag == true) {
			cameraButton.setImageResource(R.drawable.button_change_photo);
			
		} else {
			cameraButton.setImageResource(R.drawable.button_include_photo);
		}
		
		mProgressTextView.setText("Expiration " + String.valueOf(expiration_days) + " days");
		expirationSeekBar.setProgress(expiration_days);
    }
  
    
    @Override
    protected void onPause() 
    {
    	super.onPause(); 
    	

		progress_dialog.dismiss();
		
		// Save the current state. This is needed to avoid loosing the message info
		String mySubject 	 = subjectView.getText().toString();
		String myBody_db	 = bodyView.getText().toString();
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putString("SavedSubject", mySubject);
		editor.putString("SavedBody", myBody_db);
		editor.putInt("SavedExpiration",expiration_days);
		editor.putInt("SavedRadius",radius_yards);
		editor.commit();
    }
    
    
	private View.OnClickListener onCancelMark=new View.OnClickListener() 
	{
		public void onClick(View v)
		{
			cancelMark();
		}
	};
	
	
	
    // Track the expiration date seekbar
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) 
    {
    	if (seekBar.equals(expirationSeekBar)) 
    	{
	    	if (progress < 51) {
		        mProgressTextView.setText("Expiration " + String.valueOf(progress) + " days");
		        expiration_days = progress;
	    	}
	    	else {
	    		mProgressTextView.setText("Mark will never expire!");
	    		expiration_days = 1000000;
	    	}
    	} 
    	else {
    		if (progress < 350) {
		        radiusTextView.setText("Min Pick-Up Radius is 350 yards");
		        radius_yards = 350;
	    	} 
    		else if ((progress >= 350) && (progress < 1760)) {
    			radiusTextView.setText("Pick-Up Radius: " + String.valueOf(progress) + " yards");
		        radius_yards = progress;
	    	}
	    	else if (progress > 1750) {
	    		radiusTextView.setText("Pick-Up Radius: 1 mile");
	    		radius_yards = progress;
	    	}
    	}
    }
	public void onStartTrackingTouch(SeekBar seekBar) { }
	public void onStopTrackingTouch(SeekBar seekBar) { }
	
    
	
	private View.OnClickListener onSelectRecipients=new View.OnClickListener() 
	{
		public void onClick(View v) 
		{ 
			// extract the mark subject and body from the edit boxes if any have been typed
			String mySubject 	 = subjectView.getText().toString();
			String myBody_db	 = bodyView.getText().toString();
			
			// Save the current state. This is also done in the onPause() function
			SharedPreferences.Editor editor = mPreferences.edit();
			editor.putString("SavedSubject", mySubject);
			editor.putString("SavedBody", myBody_db);
			editor.putInt("SavedExpiration",expiration_days);
			editor.putInt("SavedRadius",radius_yards);
			editor.commit();
			
			Intent i=new Intent(MakeMark.this, Contacts.class);
			startActivity(i);
		}
	};
	

	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) 
	{
		super.onActivityResult(requestCode, resultCode, intent);
		
		if (requestCode == PICTURE_ACTIVITY)
			
            if (resultCode == Activity.RESULT_OK) 
            {
            	Bundle b = intent.getExtras();
                Bitmap pic = (Bitmap) b.get("data");
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                pic.compress(CompressFormat.JPEG, 75, bos); // Make the image JPEG and compress it to 75% of its original size
                
                cameraButton.setImageResource(R.drawable.button_change_photo);
                
                // Generate a random string that will tie an image taken with the message text
        		Random randomGenerator = new Random();
        		int randomInt = randomGenerator.nextInt(1000000000);
        		String randomString = myuid + "-" + String.valueOf(randomInt);
        		SharedPreferences.Editor editor = mPreferences.edit();
    			editor.putString("RandomString", randomString);
    			editor.putBoolean("SavedImageFlag", true);
    			editor.commit();
    			
                try { executeMultipartPost(bos);
				} catch (Exception e) { }
                
            }
            else if (resultCode == Activity.RESULT_CANCELED) {
            	Toast.makeText(getBaseContext(), "Canceled Image Attachment", Toast.LENGTH_SHORT).show();
            	SharedPreferences.Editor editor = mPreferences.edit();
    			editor.putBoolean("SavedImageFlag", false);
    			editor.commit();
            }
	}
	
	
	public void executeMultipartPost(ByteArrayOutputStream bos) throws Exception 
	{
		byte[] data = bos.toByteArray();			
		HttpClient httpClient = new DefaultHttpClient();
		HttpPost postRequest = new HttpPost("http://www.eggdroplabs.com:9002/upload_img/");			
		ByteArrayBody bab = new ByteArrayBody(data, "mypic.jpg");
		MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
		reqEntity.addPart("uploaded", bab);			
		String randomString = mPreferences.getString("RandomString", "");
		reqEntity.addPart("photoCaption", new StringBody(randomString));			
		postRequest.setEntity(reqEntity);
		HttpResponse response = httpClient.execute(postRequest);
		response.getEntity().getContent().close();
		BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));	
		String sResponse;
		StringBuilder s = new StringBuilder();
		while ((sResponse = reader.readLine()) != null) {
			s = s.append(sResponse);
		}
	}
	
	private View.OnClickListener onDrop=new View.OnClickListener() 
	{
		public void onClick(View v) 
		{ 
		  	makeMark();
		}
    };
     
    public void makeMark()
    {
	  	// Pull the contacts string from shared preferences
		String destinationContacts = mPreferences.getString("DestinationContacts", "");
		
		if (destinationContacts.length() > 0) 
		{
			// Set the Mark Object subject/body/type by calling the XML widgets or other functions
			mySubject  = subjectView.getText().toString();
			myBody 	  = bodyView.getText().toString();
							
		    // Get the current lat/lon info from the shared preferences store
			// the Lat/Lon info is updated in the sharedPreferences store in the LocService activity
			Float lat = mPreferences.getFloat("MarkLat", (float) 0.0);
			Float lon = mPreferences.getFloat("MarkLon", (float) 0.0);
			current_lat = (double) lat;
			current_lon = (double) lon;

			String randomString = mPreferences.getString("RandomString", "");
			
			// Make a list that represents the mark sent to the server
		    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair("myUserIdentifier", myuid));
		    nameValuePairs.add(new BasicNameValuePair("subject", mySubject));
		    nameValuePairs.add(new BasicNameValuePair("body", myBody));
		    nameValuePairs.add(new BasicNameValuePair("lat", String.valueOf(current_lat)));
		    nameValuePairs.add(new BasicNameValuePair("lon", String.valueOf(current_lon)));	
		    nameValuePairs.add(new BasicNameValuePair("expiration",String.valueOf(expiration_days)));
		    nameValuePairs.add(new BasicNameValuePair("friends", destinationContacts));
		    nameValuePairs.add(new BasicNameValuePair("imagepair", randomString));
		    nameValuePairs.add(new BasicNameValuePair("radius", String.valueOf(radius_yards)));
		    
		    mymarksdb.insert(myuid, destinationContacts, "", 
			    	mySubject, myBody, randomString, current_lat, current_lon, "", expiration_days, 
			    	System.currentTimeMillis(), radius_yards, randomString, "");		
			
		    // Send the mark to the server
		    executeDropMark(nameValuePairs);
		} 
		else {
			Toast.makeText(getBaseContext(), "No Contacts Have Been Selected", Toast.LENGTH_LONG).show();
		}
    }
    
    // AsynTask: input, type of post execute, return value, 
	private class DropMarkTask extends AsyncTask<List<NameValuePair>, Void, InputStream> 
	{
		@Override
		protected InputStream doInBackground(List<NameValuePair>...nameValuePairs) 
		{
			for (List<NameValuePair> nameValuePair : nameValuePairs) 
			{
				HttpClient http_client = new DefaultHttpClient();
				HttpPost httppost = new HttpPost("http://eggdroplabs.com:9002/drop_egg/");
				HttpResponse response = null;
				InputStream markInputStream = null;  
				
				try { 	
					httppost.setEntity(new UrlEncodedFormEntity(nameValuePair));
					response = http_client.execute(httppost);
				} catch (ClientProtocolException e) { return null;         					
				} catch (IOException e) { return null; }
					
				int status = response.getStatusLine().getStatusCode();
				if (status !=  HttpStatus.SC_OK) { return null; }
				
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
        	progress_dialog.show();
		}
		
		@Override
		protected void onPostExecute(InputStream in) 
		{ 
			progress_dialog.dismiss();
			
	        // Add the Mark to the MyMarks database
//	        boolean imageFlag = mPreferences.getBoolean("SavedImageFlag", false);
//		    String image = "";
//		    if (imageFlag == true) image = "image";
//		    String from_image = "";
//		    String real_address = "";
//			String randomString = mPreferences.getString("RandomString", "");
//			String destinationContacts = mPreferences.getString("DestinationContacts", "");
			

			
			
			clearMarkData();
	        setDefaultTab(0);
	        
	        // End this activity and send user to their home page (the map)
			Intent i=new Intent(MakeMark.this, ChalkMarkTabHost.class);
			startActivity(i);
			finish();
			
//			if (in != null)
//			{
//				String msgid = "BLAH-BLAH";
//                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
//                String s = "";
//    	        try { while ((s = reader.readLine()) != null) { msgid += s; }
//    	        } catch (IOException e) { }
//
//    	        // Add the Mark to the database
//    	        boolean imageFlag = mPreferences.getBoolean("SavedImageFlag", false);
//    		    String image = "";
//    		    if (imageFlag == true) image = "image";
//    		    String from_image = "";
//    		    String real_address = "";
//    			String randomString = mPreferences.getString("RandomString", "");
//    			String destinationContacts = mPreferences.getString("DestinationContacts", "");
//    			
//    		    mymarksdb.insert(myuid, destinationContacts, from_image, 
//    		    	mySubject, myBody, image, current_lat, current_lon, 
//    		    	real_address, expiration_days, 
//    		    	System.currentTimeMillis(), radius_yards, randomString, msgid);				
//			}

		}
	}
	
	private void clearMarkData()
	{
		subjectView.setText("");
		bodyView.setText("");
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putString("SelectedContacts", "");
		editor.putString("DestinationContacts", "");
		editor.putString("SavedSubject", "");
		editor.putString("SavedBody", "");
		editor.putBoolean("SavedImageFlag", false);
		editor.putString("RandomString", "");
		editor.putInt("SavedExpiration", 25);
		editor.putInt("SavedRadius", 1000);
		editor.commit();
	}
	
	private void setDefaultTab(int tab)
	{
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putInt("DefaultTab", 0);
		editor.commit();
	}
	
	
	@SuppressWarnings("unchecked")
	public void executeDropMark(List<NameValuePair> mylist) 
	{
		new DropMarkTask().execute(mylist);
	}

    
     
	@Override
	public void onBackPressed() 
	{
		return;
	}
	
	public void cancelMark() 
	{
		AlertDialog alertDialog = new AlertDialog.Builder(this).create(); 
		alertDialog.setTitle("Canceling Message"); 			
		alertDialog.setMessage("Are you sure want to cancel? This will destroy all message information.");
		
		alertDialog.setButton("Yes", new DialogInterface.OnClickListener() 
		{  
		      public void onClick(DialogInterface dialog, int which) 
		      {  
		    	clearMarkData();
		    	setDefaultTab(0);
				
				Intent i=new Intent(MakeMark.this, ChalkMarkTabHost.class);
				startActivity(i);
				finish();
		      }
		});   
		alertDialog.setButton2("No", new DialogInterface.OnClickListener() 
		{  
		      public void onClick(DialogInterface dialog, int which) {}
		});  
		alertDialog.show(); 
	}
}

/* (c) COPYRIGHT 2011 EGG-DROP LABS, LLC. ALL RIGHTS RESERVED
 * 
 * File:	ChalkMark.java
 * Date:	Mar 20, 2011
 * Author:	N. Desai, T. Metodi <eggdrop@eggdroplabs.com>
 * 
 * Main ChalkMark activity
 *
 * ===================================================================================================
 * SharedPreferences Keys and Default Values:
 * 
 *  DefaultTab		 : 0          // sets which tab to display when ChalkMark comes to foreground
 *  DeubgMode   	 : false      // turn debug mode on/off
 *  UserName    	 : uid        // set to "null" if user is not logged in
 *  SelectedContacts : ""		  // The selected set of contacts to receive a message
 *  SavedUserName    : ""         // Saved uid on this Android device
 *  TempMarkSubject  : ""		  // Temporary subject of an Mark (set when dropping a message)
 *  TempMarkBody     : ""         // Temporary message body (set when dropping a message)
 * 
 */

package chalkmark.project;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.janrain.android.engage.JREngage;
import com.janrain.android.engage.JREngageDelegate;
import com.janrain.android.engage.JREngageError;
import com.janrain.android.engage.net.async.HttpResponseHeaders;
import com.janrain.android.engage.types.JRActivityObject;
import com.janrain.android.engage.types.JRDictionary;


public class ChalkMark extends Activity implements JREngageDelegate
{
	Button loginButtonFacebook; 
	EditText uidEditTextBox;
	EditText passwordEditTextBox; 
	
	HttpParams httpParameters = new BasicHttpParams();
	HttpClient http_client = new DefaultHttpClient(); 
	HttpPost httppost = new HttpPost("http://eggdroplabs.com:9002/login/");
	String TAG = "CHALKMARK";
	
	private SharedPreferences mPreferences;		// used to check if user is logged-in or not
	
	private static String ENGAGE_APP_ID = "cddhelikoajgajnhomdb";
	private static String ENGAGE_TOKEN_URL = "http://eggdroplabs.com:9002/token/";
	private JREngage mEngage;
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
		
		mPreferences = getSharedPreferences("CurrentUser", MODE_PRIVATE);
		
		// When starting ChalkMark, always start with the Chalk Board (the map)
		SharedPreferences.Editor editor1 = mPreferences.edit();
        editor1.putInt("DefaultTab", 0);	
        editor1.putBoolean("SavedImageFlag", false);
        editor1.commit();
		
		loginButtonFacebook = (Button)findViewById(R.id.facebook);
		
		Button infoButton = (Button)findViewById(R.id.info);
        infoButton.setOnClickListener(new OnClickListener() 
		{       
			public void onClick(View v) {
				Intent i=new Intent(ChalkMark.this, ChalkmarkInfo.class);
				startActivity(i);
			}
		});
		
		// We need to check if the user is already. The following call sets loggedin to
		// true if the user is already logged in, false otherwise.
		boolean loggedin = false;
		
		// loggedin is set to "FALSE" if the user is not logged in.
		// If the user is not logged the "UserName" in SharedPreferences is set to "UserIsLoggedOut",
		// which is the default user name. When the "Log Out" button is pressed in the "MyMarks" activity
		// the "UserName" in SharedPreferences is set to "UserIsLoggedOut" and the ChalkMark app
		// is restarted, prompting the "checkLoginInfo()" function to return false and redirect
		// the app into the login screen.
		
		loggedin = checkLoginInfo();
		
		if (loggedin == true) 
		{			
	        // Startup the Locator Service
			Intent MarkLocI = new Intent(getApplicationContext(),LocService.class);
			startService(MarkLocI);
	
			// Start the rest of the Activity		
			Intent i=new Intent(ChalkMark.this, ChalkMarkTabHost.class);
			startActivity(i);
			finish();
		}
		else 
		{
			// Using the JanRain engage libraries to authenicate/validate user
			// using Facebook. Currently this is the only option we offer. We do not
			// have the option for ChalkMark-only user accounts.
			
			loginButtonFacebook.setOnClickListener(new OnClickListener() 
			{       
				public void onClick(View v) { spawnJanrainEngageActivity(); }
			});
		}
	}
	
	
	public void spawnJanrainEngageActivity()
	{
		// Spawns the Janrain engage activity that allows the user to login through Facebook
		// if the Facebook login is successful, a token is passed to the eggroplabs.com servers indicating
		// that the user has been validated.  That token is then used to gather additional
		// information.  The user then is added to the edl (eggdrop labs)
		// user database and the MyMarks activity is started. 
		try {
			mEngage = JREngage.initInstance(this, ENGAGE_APP_ID, ENGAGE_TOKEN_URL, this);
			mEngage.showAuthenticationDialog();
		} catch (NullPointerException e ) {
			Toast.makeText(this, "Cannot Reach Facebook", Toast.LENGTH_LONG).show();
		}
	}

	public void jrEngageDialogDidFailToShowWithError(JREngageError error) {
	}

	public void jrAuthenticationDidNotComplete() {
		 Toast.makeText(this, "Authentication did not complete", Toast.LENGTH_SHORT).show();	
	}

	public void jrAuthenticationDidSucceedForUser(JRDictionary auth_info, String provider) 
	{
		    JRDictionary profile = (auth_info == null) ? null : auth_info.getAsDictionary("profile");
	       
	        String raw_id = (profile == null)? null :profile.getAsString("identifier");
	        String verified_email = (profile == null)? null :profile.getAsString("verifiedEmail");
	        
	        // extract the unique identifying number from the raw ID string and make that the user name
	        String[] raw_id_list = raw_id.split("\\?id=");
	        for (int i = raw_id_list.length - 1; i >= 0; i--) {
	        	raw_id_list[i] = raw_id_list[i].trim();
			}
	        
	        // Initialize All Shared Preferences used in the App
	        SharedPreferences.Editor editor = mPreferences.edit();
            editor.putString("UserName", raw_id_list[1]);			// The unique user number assigned by Facebook
            editor.putString("VerifiedEmail", verified_email);	    // The user's email address  
            editor.putInt("ChalkmarksNumber", 0);					// number of new messages received (displayed in the notification)
            editor.putBoolean("SavedImageFlag", false);				// indicates if there is an image when placing a message
            editor.putInt("DefaultTab", 0);							// The Tab that opens when starting ChalkMarkTabHost activity
            editor.putString("SelectedContacts", "");				// Used to save selected contacts when user refreshes contacts list
            editor.putString("DestinationContacts", "");			// The UIDs for the contacts that the message is sent to
            editor.putFloat("Lat", (float) 0.0);					// My current latitude as computed by LocService: OnChangeLocation
            editor.putFloat("Lon", (float) 0.0);					// My current longitude as computed by LocService: OnChangeLocation
    		editor.putString("SavedSubject", "");					// The saved subject for a new Chalkmark to be loaded when user returns to MakeMark activity from the Camera or from the Contacts 
    		editor.putString("SavedBody", "");						// The saved body for a new Chalkmark to be loaded when user returns to MakeMark activity from the Camera or from the Contacts 
    		editor.putInt("SavedExpiration", 25);					// The saved expiration of a newly placed chalkmark 
    		editor.putInt("SavedRadius", 350);						// default pickup radius in yards (min=350, max=1mile)
    		editor.putString("RandomString", "");					// The random "imagepair" string generated that matches up an image with a message
    		editor.putFloat("MarkLat", (float) 0.0);				// The latitude set by MyMarks.java for the mark that is being dropped
    		editor.putFloat("MarkLon", (float) 0.0);				// The longitude set by MyMarks.java for the mark that is being dropped
            editor.commit();
			
	        // Startup the Locator Service
			Intent MarkLocI = new Intent(getApplicationContext(),LocService.class);
			startService(MarkLocI);
	
			// Start the rest of the Activity		
			Intent i=new Intent(ChalkMark.this, ChalkMarkTabHost.class);
			startActivity(i);
			finish();
		
	}

	public void jrAuthenticationDidFailWithError(JREngageError error, String provider) { }

	public void jrAuthenticationDidReachTokenUrl(String tokenUrl,
			HttpResponseHeaders response, String tokenUrlPayload,
			String provider) { }

	public void jrAuthenticationCallToTokenUrlDidFail(String tokenUrl,
			JREngageError error, String provider) { }

	public void jrSocialDidNotCompletePublishing() { }

	public void jrSocialDidCompletePublishing() { }

	public void jrSocialDidPublishJRActivity(JRActivityObject activity,
			String provider) { }

	public void jrSocialPublishJRActivityDidFail(JRActivityObject activity,
			JREngageError error, String provider) { }
	
	
	private boolean checkLoginInfo() 
	{	
		boolean uid_set = mPreferences.contains("UserName");
		if (!uid_set) { return false; }
		String uid = mPreferences.getString("UserName", "UserIsLoggedOut");
		if (uid.compareToIgnoreCase("UserIsLoggedOut") == 0) { return false; 
		} else { return true; }
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) 
	{
	    if ((keyCode == KeyEvent.KEYCODE_BACK)) { 
	    	// exit the app if the back button is pressed while in the login screen
	        finish();
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
}

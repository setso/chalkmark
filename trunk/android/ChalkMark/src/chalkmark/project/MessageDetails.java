package chalkmark.project;

// http://www.ibiblio.org/wm/paint/auth/gogh/gogh.starry-night-rhone.jpg

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.TextView;


public class MessageDetails extends Activity 
{
	String TAG = "CHALKMARK";
	private Cursor c 				= null; 
	MarksDataBaseHelper dbhelper	= null;
	String ChalkmarkID				= null;
	private SharedPreferences mPreferences;
	private boolean imageflag;
	
	// Since the body is displayed as a WebView, we must supply the HTML that the body field
	// displays to the user.
	String imageHeader = "<html><body>";
	String imageTail     = "</body></html>";
	String MIMEType = "text/html";
	String Encoding = "UTF-8";
	
	
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);

        mPreferences = getSharedPreferences("CurrentUser", MODE_PRIVATE);
        
        ChalkmarkID = getIntent().getStringExtra(ChalkBoard.ID_EXTRA);                
        
        dbhelper=new MarksDataBaseHelper(this);
        c = dbhelper.getByID(ChalkmarkID);
    	c.moveToFirst();
        
    	String imageURL = dbhelper.getImage(c);
    	
    	String[] temp;
    	temp = imageURL.split("=");
    	imageflag = false;
    	
    	if (temp.length > 1) 
    	{
    		imageflag = true;
    		setContentView(R.layout.mark_details_image);
    		imageURL = imageHeader + "<p><img src=\"" + imageURL + "\" width=\"300\"/>" + imageTail;
    		WebView imageView=(WebView)findViewById(R.id.webkit); 
    		imageView.getSettings().setSupportZoom(true);         // Zoom Control on web (You don't need this     
    		imageView.getSettings().setBuiltInZoomControls(true); // if ROM supports Multi-Touch
    		imageView.loadData(imageURL, MIMEType, Encoding);
    	}     	
    	else 
    	{
    		setContentView(R.layout.mark_details);
    	}
    	
    	String from_name = dbhelper.getFromName(c);
    	
        TextView bodyView = (TextView)findViewById(R.id.body);
        bodyView.setMovementMethod(new ScrollingMovementMethod());
        TextView subjectView = (TextView)findViewById(R.id.subject);
        subjectView.setText(" " + dbhelper.getSubject(c));
        String bodytext = "Date Dropped: " + dbhelper.getTimeDropped(c);
        bodytext = bodytext + "\nFrom: " + from_name;
        bodytext = bodytext + "\n\n" + dbhelper.getBody(c); 
        bodyView.setText(bodytext);
        dbhelper.updateStatus(ChalkmarkID, "read");    
    }
   
	@Override
	public void onDestroy() 
	{
		super.onDestroy();	
		dbhelper.close();
	}
    
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		new MenuInflater(this).inflate(R.menu.mark_detail_options, menu);
		return(super.onCreateOptionsMenu(menu));
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		if (item.getItemId() == R.id.delete) 
		{
			dbhelper.deleteMarkByID(ChalkmarkID);
			Context context = getApplicationContext();
			AlertDialog.Builder alertbox = new AlertDialog.Builder(context);
			alertbox.setMessage("The Mark Was Deleted");			
			SharedPreferences.Editor editor = mPreferences.edit();
			editor.putInt("DefaultTab", 1);
			editor.commit();
			Intent intent = new Intent().setClass(MessageDetails.this, ChalkMarkTabHost.class);
			startActivity(intent);
			finish();
		}
		
		else if (item.getItemId() == R.id.map) 
		{
			Intent i=new Intent(MessageDetails.this, MarkInfoMap.class);
			
			c = dbhelper.getByID(ChalkmarkID);
	    	c.moveToFirst();  
	    	
	    	Double latitude = dbhelper.getLat(c);
	    	Double longitude = dbhelper.getLon(c);
			
			i.putExtra(MarkInfoMap.EXTRA_LATITUDE, latitude);
			i.putExtra(MarkInfoMap.EXTRA_LONGITUDE, longitude);
			i.putExtra(MarkInfoMap.EXTRA_NAME, dbhelper.getFromName(c));
			i.putExtra(MarkInfoMap.EXTRA_SUBJECT, dbhelper.getSubject(c));			
			
			dbhelper.close();
			startActivity(i);
		}
		
		
		else if (item.getItemId() == R.id.unread) 
		{ // Make the Chalkmark unread
			c = dbhelper.getByID(ChalkmarkID);
	    	c.moveToFirst(); 
	    	dbhelper.updateStatus(ChalkmarkID, "unread");
		}
		
		else if (item.getItemId() == R.id.unread) 
		{ // Make the Chalkmark unread
			c = dbhelper.getByID(ChalkmarkID);
	    	c.moveToFirst(); 
	    	dbhelper.updateStatus(ChalkmarkID, "unread");
		}
		
		else if (item.getItemId() == R.id.repost) 
		{
			c = dbhelper.getByID(ChalkmarkID);
			c.moveToFirst();
			
			SharedPreferences.Editor editor = mPreferences.edit();
			editor.putString("SavedSubject", dbhelper.getSubject(c));
			editor.putString("SavedBody", dbhelper.getBody(c));
			editor.putInt("SavedExpiration", 25);
			editor.putInt("SavedRadius", 1000);
			editor.putBoolean("SavedImageFlag", imageflag);
			editor.putString("RandomString", dbhelper.getImagePair(c));
			editor.putFloat("MarkLat", (float) dbhelper.getLat(c));
			editor.putFloat("MarkLon", (float) dbhelper.getLon(c));
			editor.commit();
			
			Intent i=new Intent(MessageDetails.this, MakeMark.class);
        	startActivity(i);
        	finish();
		}
		
		
		return(super.onOptionsItemSelected(item));
	}
	
}
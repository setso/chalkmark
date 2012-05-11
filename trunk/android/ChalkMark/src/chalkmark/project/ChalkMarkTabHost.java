package chalkmark.project;


import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;


public class ChalkMarkTabHost extends TabActivity 
{
		
	public TabHost tabHost = null;
	private SharedPreferences mPreferences;		// used to check if user is logged-in or not
	int tabdisplay = 0;
	
	
	public void onCreate(Bundle savedInstanceState) 
	{
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.main);
	    
	    
	    mPreferences = getSharedPreferences("CurrentUser", MODE_PRIVATE);
	    
	    String userid = mPreferences.getString("UserName", "UserIsLoggedOut");
	    if (userid.equals("UserIsLoggedOut")) 
	    {
	    	Context context = getApplicationContext();
	    	Intent i = new Intent(context, ChalkMark.class);
			startActivity(i);
			finish();
	    }
	    
	    int tabdisplay = mPreferences.getInt("DefaultTab", 0);
   
	    Resources res = getResources(); 	// Resource object to get Drawables
	    TabHost tabHost = getTabHost();  	// The activity TabHost
	    TabHost.TabSpec spec;  				// Reusable TabSpec for each tab
	    Intent intent;  					// Reusable Intent for each tab

	    
	    
	    // Create an Intent to launch an Activity for the "MyMarks" tab and initialize a
	    // tab spec and add the tab spec to the TabHost
	    intent = new Intent().setClass(this, MyMarks.class);
	    spec = tabHost.newTabSpec("mymarks")
	    			.setIndicator("", res.getDrawable(R.drawable.ic_tab_chalkboard))
	    			.setContent(intent);
	    tabHost.addTab(spec);

	    // Create an Intent to launch an Activity for the "ChalkBoard" tab and initialize a
	    // tab spec and add the tab spec to the TabHost
	    intent = new Intent().setClass(this, ChalkBoard.class);
	    spec = tabHost.newTabSpec("chalkboard")
	    			.setIndicator("",res.getDrawable(R.drawable.ic_tab_mymarks))
	                .setContent(intent);
	    tabHost.addTab(spec);

/*
	    // Create an Intent to launch an Activity for the "Contacts" tab and initialize a
	    // tab spec and add the tab spec to the TabHost
	    intent = new Intent().setClass(this, Contacts.class);
	    spec = tabHost.newTabSpec("contacts")
	    			.setIndicator("",res.getDrawable(R.drawable.ic_tab_contacts))
	    			.setContent(intent);
	    tabHost.addTab(spec);
*/

	    tabHost.setCurrentTab(tabdisplay);
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
	}
	
}



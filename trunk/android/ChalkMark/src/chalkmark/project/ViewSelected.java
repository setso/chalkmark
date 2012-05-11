package chalkmark.project;

// http://www.ibiblio.org/wm/paint/auth/gogh/gogh.starry-night-rhone.jpg

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;


public class ViewSelected extends Activity 
{
	String TAG = "CHALKMARK";

    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.selected_names);

        String names = getIntent().getStringExtra(Contacts.NAMES);                
        
        TextView bodyView = (TextView)findViewById(R.id.body);
        TextView subjectView = (TextView)findViewById(R.id.subject);
        subjectView.setText("Selected Contacts");
        bodyView.setText(names);
    }
   
	@Override
	public void onDestroy() 
	{
		super.onDestroy();	
	}
    
}
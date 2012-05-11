/* File: ChalkBoard.java
 * 
 * Main ChalkBoard Activity. This activity corresponds to a separate tab in the
 * main ChalkMark application.
 * 
 */

package chalkmark.project;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;



public class ChalkBoard extends ListActivity 
{
	String TAG = "CHALKMARK";
	
	public final static String ID_EXTRA = "chalkmark.project._ID";
	
	Cursor marksCursor				= null;
	MarkAdapter 	adapter			= null;
	String username				= null;
	String auth_string			= null;
	private SharedPreferences mPreferences;
	
	MarksDataBaseHelper dbhelper;
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chalkboard);		
    	mPreferences = getSharedPreferences("CurrentUser", MODE_PRIVATE);
    	username = mPreferences.getString("UserName", null);					
		dbhelper = new MarksDataBaseHelper(this);
		
		if (username.equals("UserIsLoggedOut")) {
			Intent i=new Intent(ChalkBoard.this, ChalkMark.class);
			startActivity(i);
			finish();
		}
	}
	
	
    @Override
    protected void onDestroy() 
    {
    	super.onDestroy();
    	dbhelper.close();
    }
    
    
    @Override
    protected void onResume() 
    {
    	super.onResume();
    	
		// Reset the number of received Chalkmarks to Zero
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putInt("ChalkmarksNumber", 0);
		editor.commit();
    	
    	marksCursor = dbhelper.getMarksFilteredBy("myuid", username, "received_sec");
    	startManagingCursor(marksCursor);
		adapter=new MarkAdapter(marksCursor);
		setListAdapter(adapter);
    }
    
	
	@Override
	public void onListItemClick(ListView list, View view, int position, long id) 
	{
		Intent i = new Intent(ChalkBoard.this, MessageDetails.class);
		i.putExtra(ID_EXTRA, String.valueOf(id));
		startActivity(i);
	}

	
	class MarkAdapter extends CursorAdapter 
	{
		MarkAdapter(Cursor c) 
		{
			super(ChalkBoard.this, c);
		}
		
		@Override
		public void bindView(View row, Context ctxt, Cursor c) 
		{
			MarksHolder holder=(MarksHolder)row.getTag();
			holder.populateFrom(c, dbhelper);
		}
		
		@Override
		public View newView(Context ctxt, Cursor c, ViewGroup parent) 
		{
			LayoutInflater inflater=getLayoutInflater();
			View row=inflater.inflate(R.layout.row, parent, false);
			MarksHolder holder=new MarksHolder(row);
			row.setTag(holder);
			return(row);
		}
	}
	
	
	static class MarksHolder
	{
		private TextView fromView=null;
		private TextView bodyView=null;
		private ImageView icon=null;
		private ImageView icon2=null;
		
		MarksHolder(View row) 
		{
			fromView=(TextView)row.findViewById(R.id.subject);
			bodyView=(TextView)row.findViewById(R.id.body);
			icon=(ImageView)row.findViewById(R.id.icon);
			icon2=(ImageView)row.findViewById(R.id.icon2);
		}
		
		void populateFrom(Cursor c, MarksDataBaseHelper dbhelper) 
		{
			String subject_text = dbhelper.getSubject(c);
			String from_name = dbhelper.getFromName(c);
			String body_text = dbhelper.getBody(c);
			String date_received = dbhelper.getDateReceived(c);
			
			if (subject_text.length() == 0) {
				fromView.setText(date_received + " - (no subject)");
			} else {
				fromView.setText(date_received + " - " + subject_text);
			}
			
			bodyView.setText("From: "+ from_name + " - " + body_text);
			
			String myStatus = dbhelper.getStatus(c);
			if (myStatus.equals("unread")) { icon2.setImageResource(R.drawable.ic_status_unread); }
			else if (myStatus.equals("read")) { icon2.setImageResource(R.drawable.ic_status_read); }
			else { icon2.setImageResource(R.drawable.fried_egg); }
			String imageURL = dbhelper.getImage(c);
			String[] temp;
	    	temp = imageURL.split("=");
	    	if (temp.length > 1) { icon.setImageResource(R.drawable.ic_camera); }
	    	else { icon.setImageResource(R.drawable.ic_no_image); }
		}
	}
	
	@Override
	public void onBackPressed() {
	   return;
	}
	
	
}
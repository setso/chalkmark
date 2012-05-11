package chalkmark.project;

/* File: Contacts.java
 * 
 * Main Contacts Activity. This activity corresponds to a separate tab in the
 * main ChalkMark application.
 * 
 * The Contacts activity allows the user to add new contacts and manage existing
 * contacts.
 * 
 * We have the following information about each contact:
 *  - Name
 *  - email (mandatory)
 *  - type (business or personal (Check box))
 *  
 */

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class Contacts extends ListActivity 
{
	public final static String NAMES = "chalkmark.project._ID";
	
	private ArrayAdapter<String> mainContactsAdapter = null;
	private ListView myContactsListView = null;
	private ArrayList<String> namesList;
	private SharedPreferences mPreferences;
	private ProgressDialog progress_dialog;
	
	private ArrayList<String> selectedNamesList;
	
	private String myuid = null;
	
	ContactsDataBaseHelper contactsdb;
	
	private TextView selectedContactsView = null;
	private EditText filterText = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contacts);

        progress_dialog = new ProgressDialog(Contacts.this);
        progress_dialog.setIndeterminate(true);
        progress_dialog.setCancelable(false);
		
		mPreferences = getSharedPreferences("CurrentUser", MODE_PRIVATE);
		myuid = mPreferences.getString("UserName", "");
		
		selectedNamesList = new ArrayList<String>();

		myContactsListView = getListView();
		
	    namesList = new ArrayList<String>();
		namesList.add("Myself");
		
		contactsdb = new ContactsDataBaseHelper(this);
		Cursor contactsdbCursor = contactsdb.getAllContacts("name");
		int rows = contactsdbCursor.getCount();
		
		if (rows == 0) { 
			refreshContactsFromServer(); 
		}
		else {
			contactsdbCursor = contactsdb.getAllContacts("name");
			contactsdbCursor.moveToFirst();
		    while (contactsdbCursor.isAfterLast() == false) {
		    	namesList.add(contactsdb.getName(contactsdbCursor));
			    contactsdbCursor.moveToNext();
		    }	
		}
		
		mainContactsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, namesList);
		myContactsListView.setAdapter(mainContactsAdapter);
		
	    selectedContactsView = (TextView) findViewById(R.id.summary);
		
		myContactsListView.setOnItemClickListener(new OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> arg0, View v, int position, long id) 
			{
				Object list_item = myContactsListView.getItemAtPosition(position);
				String name = ""+list_item;
				if (selectedNamesList.contains(name)) handleNameExists(name);
				else handleAddingName(name);
			}
		});
		
		
		Button btnDrop = (Button) findViewById(R.id.btnDrop);
		btnDrop.setOnClickListener(new OnClickListener() {
			public void onClick(View v) { 
				DropToSelectedContacts();				
			}
		});
		
		Button btnClear = (Button) findViewById(R.id.btnClear);
		btnClear.setOnClickListener(new OnClickListener() {
			public void onClick(View v) 
			{
				selectedNamesList = new ArrayList<String>();
				SharedPreferences.Editor editor = mPreferences.edit();
				editor.putString("SelectedContacts", "");
				editor.putString("DestinationContacts", "");
				editor.commit();
				selectedContactsView.setText("No Contacts Selected (Click on Each Name to Add or Remove)");
			}
		});	
		
		Button btnAll = (Button) findViewById(R.id.btnAll);
		btnAll.setOnClickListener(new OnClickListener() {
			public void onClick(View v) 
			{
				selectedNamesList = new ArrayList<String>();
				int count = myContactsListView.getAdapter().getCount();
				for (int i = 0; i < count; i++) selectedNamesList.add(""+myContactsListView.getItemAtPosition(i));
				selectedContactsView.setText("Selected All Contacts !!!");
			}
		});
		
		filterText = (EditText) findViewById(R.id.search_box);
	    filterText.addTextChangedListener(filterTextWatcher);
	}
	
	private void generateListAdapter()
	{
		mainContactsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, namesList);
		myContactsListView.setAdapter(mainContactsAdapter);
	}
	
	
	@Override
	protected void onResume() 
	{
		super.onResume();	
		LoadExistingSelections();
	}
	
	@Override
	protected void onPause() 
	{ 
		super.onPause();
		progress_dialog.dismiss();
		String names = convertSelectionsToString();
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putString("SelectedContacts", names);
		editor.commit();
	}
	
	@Override
    protected void onSaveInstanceState(Bundle outState)
    {
    	super.onSaveInstanceState(outState);
    }
		
	@Override
	protected void onDestroy() 
	{ 
		super.onDestroy();
		progress_dialog.dismiss();
		contactsdb.close();
	}
	
	
	private TextWatcher filterTextWatcher = new TextWatcher() 
	{	
	    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

	    public void onTextChanged(CharSequence s, int start, int before, int count) {
	    	mainContactsAdapter.getFilter().filter(s);
	    }	    
		public void afterTextChanged(Editable arg0) { }
	};
	
	
	private void handleNameExists(String name) 
	{
		AlertDialog dropDialog = new AlertDialog.Builder(this).create(); 
		dropDialog.setMessage("\""+name+"\" already added. Do you want to remove?");
		final String myname = name;
		
		dropDialog.setButton("No", new DialogInterface.OnClickListener() 
		{  
		      public void onClick(DialogInterface dialog, int which) 
		      {  }
		});      
		
		dropDialog.setButton2("Yes", new DialogInterface.OnClickListener() 
		{  
		      public void onClick(DialogInterface dialog, int which) 
		      {
		    	   selectedNamesList.remove(myname);
	        	   int s = selectedNamesList.size();
	        	   if (s == 0) {
	        		   selectedContactsView.setText("No Contacts Selected (Click on Each Name to Add or Remove)");
	        	   } else {
	        		   selectedContactsView.setText("Selected Contacts: " + convertSelectionsToString2());
	        	   }	
		      }
		});  
		dropDialog.show();
	}

	
	
	private void handleAddingName(String name)
	{
		AlertDialog dropDialog = new AlertDialog.Builder(this).create(); 
		dropDialog.setMessage("Add contact: "+name+"?");
		final String myname = name;
		dropDialog.setButton("No", new DialogInterface.OnClickListener() 
		{  
		      public void onClick(DialogInterface dialog, int which) 
		      {  }
		});      
		
		dropDialog.setButton2("Yes", new DialogInterface.OnClickListener() 
		{  
		      public void onClick(DialogInterface dialog, int which) 
		      {
		    	   selectedNamesList.add(myname);
        		   selectedContactsView.setText("Selected Contacts: " + convertSelectionsToString2());
		      }
		});  
		dropDialog.show();
	} 
	
	
	private String convertSelectionsToString2()
	{
		String result = "";
		int s = selectedNamesList.size();
		if (s == 0) return "";
		result += selectedNamesList.get(0);
		for (int i=1; i<s; i++) { result += ", "+selectedNamesList.get(i); }
		return result;
	}
	

	
	private void LoadExistingSelections() 
	{ 
		selectedNamesList = new ArrayList<String>();
		String selected = mPreferences.getString("SelectedContacts", "");
		
		ArrayList<String> savedNamesList = new ArrayList<String>();
		savedNamesList.addAll(Arrays.asList(selected.split(",")));
		
		int count = myContactsListView.getAdapter().getCount();
		
		for (int i = 0; i < count; i++) 
		{
			String currentItem = (String) myContactsListView.getAdapter().getItem(i);
			if (savedNamesList.contains(currentItem)) 
			{
				selectedNamesList.add(currentItem);
			}
		}
		
		int s = selectedNamesList.size();
 	   	if (s == 0) {
 		   selectedContactsView.setText("No Contacts Selected (Click on Each Name to Add or Remove)");
 	    } else {
 		   selectedContactsView.setText("Selected Contacts: " + convertSelectionsToString2());
 	    }
	}
	
	
	private String convertSelectionsToString()
	{
		String result = "";
		int s = selectedNamesList.size();
		if (s == 0) return "";
		result += selectedNamesList.get(0);
		for (int i=1; i<s; i++) { result += ","+selectedNamesList.get(i); }
		return result;
	}
	
	
	private void DropToSelectedContacts() 
	{
		// convert the current selections to a string of names separated by comma and save to shared preferences
		// The only way to get out of this activity is to click the "Done" button.

		int s = selectedNamesList.size();
		String destUids = "";
		
		if (selectedNamesList.contains("Myself")) {
			destUids += myuid;
		}
		
		s = selectedNamesList.size();
		for (int i = 0; i < s ; i++) 
		{
			String myname = selectedNamesList.get(i).trim();
			if (myname.equals("Myself")) continue;
			Cursor c = contactsdb.getByName(myname);
			boolean b = c.moveToFirst();
			if (b==true) 
				if (destUids.length() == 0) destUids += contactsdb.getUid(c);
				else destUids += ","+contactsdb.getUid(c);
		}
		
		SharedPreferences.Editor editor2 = mPreferences.edit();
		editor2.putString("DestinationContacts", destUids);
		editor2.commit();
		
		Intent i=new Intent(Contacts.this, MakeMark.class);
		startActivity(i);
	}
	

	

	private class DownloadContactsList extends AsyncTask<String, Void, String> 
	{
		@Override
		protected String doInBackground(String... urls) 
		{
			String response = "";
			for (String url : urls) 
			{
				DefaultHttpClient client = new DefaultHttpClient();
				HttpGet httpGet = new HttpGet(url);
				try {
					HttpResponse execute = client.execute(httpGet);
					InputStream content = execute.getEntity().getContent();
					BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
					String s = "";
					while ((s = buffer.readLine()) != null) {
						response += s;
					}
				} catch (Exception e) { }
			}
			return response;
		}
		
		@Override
		protected void onPreExecute()
		{
	        progress_dialog.setMessage("Updating Contacts!");
	        progress_dialog.show();
		}

		@Override
		protected void onPostExecute(String result) 
		{	// result is a JSON string which holds the contacts
			progress_dialog.dismiss();
			processContactsJSON(result);	
			generateListAdapter();
			Toast.makeText(getBaseContext(), String.valueOf(namesList.size())+" contacts", Toast.LENGTH_SHORT).show();
		}
	}
	
	
	private boolean processContactsJSON(String result)
	{
		Cursor contactsdbCursor = contactsdb.getAllContacts("name");
		int rows = contactsdbCursor.getCount();
		if (rows > 0) contactsdb.deleteall();
		
		try {
			JSONObject jObject = new JSONObject(result);
			JSONArray menuitemArray = jObject.getJSONArray("contacts");
			int N = menuitemArray.length();
			for (int i = 0; i < N; i++) 
			{
				String displayname = menuitemArray.getJSONObject(i).getString("displayname").toString();
				String uid = menuitemArray.getJSONObject(i).getString("uid").toString();
				contactsdb.insert(displayname, uid, "", "");
				namesList.add(displayname);
			}
			return true;
		} 
		catch (JSONException e1) { }
		return false;
	}

	
	public void getContactsFromServer() 
	{
		DownloadContactsList task = new DownloadContactsList();
		String verified_email = mPreferences.getString("VerifiedEmail", "");
		String requestURL = "http://eggdroplabs.com:9002/android_contacts/?="+verified_email;
		task.execute(new String[] { requestURL});
	}
	
	
	public void refreshContactsFromServer() 
	{
		DownloadContactsList task = new DownloadContactsList();
		String verified_email = mPreferences.getString("VerifiedEmail", "");
		String requestURL = "http://eggdroplabs.com:9002/android_contacts_refresh/?="+verified_email;
		task.execute(new String[] { requestURL});
	}
	
	public ArrayList<String> stringToArrayList(String mystring, String separator) 
	{
		String[] pieces = mystring.split(separator);
		for (int i = pieces.length - 1; i >= 0; i--) {
			pieces[i] = pieces[i].trim();
		}
		return new ArrayList<String>(Arrays.asList(pieces));
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		new MenuInflater(this).inflate(R.menu.contacts_options, menu);
		return(super.onCreateOptionsMenu(menu));
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		if (item.getItemId()==R.id.refresh_contacts) 
		{
			namesList = new ArrayList<String>();
			namesList.add("Myself");
			refreshContactsFromServer();
        }
		
		else if (item.getItemId()==R.id.view_selected) 
		{
			int s = selectedNamesList.size();
			if (s == 0) {
				Toast.makeText(getBaseContext(), "No Contacts Selected", Toast.LENGTH_SHORT).show();
			} 
			else {
				String names = convertSelectionsToString();
				SharedPreferences.Editor editor = mPreferences.edit();
				editor.putString("SelectedContacts", names);
				editor.commit();
				
				Intent i = new Intent(Contacts.this, ViewSelected.class);
				i.putExtra(NAMES, names);
				startActivity(i);
			}
        }
		return(super.onOptionsItemSelected(item));
	}
	
	@Override
	public void onBackPressed() {
	   return;
	}

}
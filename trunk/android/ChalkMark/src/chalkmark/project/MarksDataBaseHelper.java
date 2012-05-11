/* File: MarkDataBase.java
 * 
 * Main Activity for the ChalkMark Data Base
 * 
 */

package chalkmark.project;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class MarksDataBaseHelper extends SQLiteOpenHelper
{
	private static final String DATABASE_NAME = "geomarks.db";
	private static final int SCHEMA_VERSION = 1;
	private static final String TABLE_NAME_BASKET = "chalkboard";
	
	private String select_str = "SELECT _id, " +
		"myuid, " +
		"from_uid, " +
		"from_name, " +
		"from_image, " +
		"subject, " +
		"body, " +
		"image, " +
		"status, " +
		"lat, " +
		"lon, " +
		"real_address, " +
		"expiration, " +
		"timedropped, " +
		"datereceived, " +
		"received_sec, " +
		"imagepair ";
	
	public MarksDataBaseHelper(Context context)
	{
		super(context, DATABASE_NAME, null, SCHEMA_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) 
	{ // Create the needed database tables
		String dbstring = 
			"CREATE TABLE chalkboard (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"myuid TEXT, " +			// This user's unique ID. Used to filter what shows up in the Chalkboard
				"from_uid TEXT, " +			// the unique ID of the sender
				"from_name TEXT, " +		// the Sender's Facebook display name.
				"from_image TEXT, " + 		// URL of contact image
				"subject TEXT, " +			// The subject of the message
				"body TEXT, " + 			// The body of the message
				"image TEXT, " +  			// URL of attached image
				"status TEXT, " + 			// Status can be read or unread
				"lat REAL, " +				// 
				"lon REAL, " +
				"real_address STRING, " +
				"expiration INTEGER, " +	// days remaining till expiration
				"timedropped TEXT, " +
				"datereceived TEXT, " +
				"received_sec REAL," +
				"imagepair" +
			");";
		db.execSQL(dbstring);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
	{
		// Upgrading database, this will drop tables and recreate.
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_BASKET);
		onCreate(db);
	}
	
	public void insert(
			String myuid, String from_uid, String from_name, String from_image,
			String subject, String body, String image, String status, 
			double lat, double lon, String real_address,
			int expiration, String timedropped, String datereceived, double received_sec,
			String imagepair)
	{ /* In this function we pour the individual attributes of an Mark, such as its subject, body, etc., 
	   * into a ContentValues container and insert the container into the database.
	   */ 
		ContentValues cv = new ContentValues();
		cv.put("myuid", myuid);
		cv.put("from_uid", from_uid);
		cv.put("from_name", from_name);
		cv.put("from_image", from_image);
		cv.put("subject", subject);
		cv.put("body", body);
		cv.put("image", image);
		cv.put("status", status);
		cv.put("lat", lat);
		cv.put("lon", lon);
		cv.put("real_address", real_address);
		cv.put("expiration", expiration);
		cv.put("timedropped", timedropped);
		cv.put("datereceived", datereceived);
		cv.put("received_sec", received_sec);
		cv.put("imagepair", imagepair);
		getWritableDatabase().insert("chalkboard","subject", cv);
	}
	
	public void deleteMarkByID(String ID)
	{ /* Return an Mark from the database that matches the input ID
	   */
		String[] args = {ID};
		getWritableDatabase().delete("chalkboard", "_ID=?", args);
	}
	
	
	public Cursor getAllMarks(String OrderBy)
	{ /* This function rerpesents a cursor with a columnar data for the egg basket table
	   * A cursor is an encapsulation of the result set of the query, plus the query that
	   * was used to create it.
	   */
		return(getReadableDatabase().rawQuery(select_str + "FROM chalkboard ORDER BY " + OrderBy + " DESC", null));
	}
	
	public Cursor getMarksFilteredBy(String selector, String value, String OrderBy)
	{ /* Get all Geomarks filtered by a specific attribute in the specified order */
		return(getReadableDatabase().rawQuery(select_str + "FROM chalkboard WHERE " + selector + " = \"" + value + "\" ORDER BY " + OrderBy + " DESC", null));
	}
	
	
	public void update(String ID, 
			String myuid, String from_uid, String from_name, String from_image,
			String subject, String body, String image, String status, 
			double lat, double lon, String real_address,
			int expiration, String timedropped, String datereceived, double received_sec,
			String imagepair)
	{
		ContentValues cv=new ContentValues();
		String[] args={ID};
		cv.put("myuid", myuid);
		cv.put("from_uid", from_uid);
		cv.put("from_name", from_name);
		cv.put("from_image", from_image);
		cv.put("subject", subject);
		cv.put("body", body);
		cv.put("image", image);
		cv.put("status", status);
		cv.put("lat", lat);
		cv.put("lon", lon);
		cv.put("real_address", real_address);
		cv.put("expiration", expiration);
		cv.put("timedropped", timedropped);
		cv.put("datereceived", datereceived);
		cv.put("received_sec", received_sec);
		cv.put("imagepair", imagepair);
		getWritableDatabase().update("chalkboard", cv, "_ID=?", args);
	}
	
	
	public void updateStatus(String ID, String status)
	{
		ContentValues cv=new ContentValues();
		String[] args={ID};
		cv.put("status", status);
		getWritableDatabase().update("chalkboard", cv, "_ID=?", args);
	}
	
	
	public Cursor getByID(String ID)
	{ /* Return an Mark from the database that matches the input ID
	   */
		String[] args = {ID};
		return (getReadableDatabase().rawQuery(select_str + "FROM chalkboard WHERE _ID=?", args));
	}
	
	
	/* Implement Some Other Queary Functions  For individual items in the database
	 */
	
	public String getMyUid(Cursor c) {
		return(c.getString(1));
	}
	
	public String getFromUid(Cursor c) {
		return(c.getString(2));
	}
	
	public String getFromName(Cursor c) {
		return(c.getString(3));
	}
	
	public String getFromImage(Cursor c) {
		return(c.getString(4));
	}
	
	public String getSubject(Cursor c) {
		return(c.getString(5));
	}
	
	public String getBody(Cursor c) {
		return(c.getString(6));
	}
	
	public String getImage(Cursor c) {
		return(c.getString(7));
	}
	
	public String getStatus(Cursor c) {
		return(c.getString(8));
	}
	
	public double getLat(Cursor c) {
		return(c.getDouble(9));
	}
	
	public double getLon(Cursor c) {
		return(c.getDouble(10));
	}
	
	public String getRealAddress(Cursor c) {
		return(c.getString(11));
	}
	
	public int getExpiration(Cursor c) {
		return(c.getInt(12));
	}
	
	public String getTimeDropped(Cursor c) {
		return(c.getString(13));
	}
	
	public String getDateReceived(Cursor c) {
		return(c.getString(14));
	}
	
	public double getReceivedSec(Cursor c) {
		return(c.getDouble(15));
	}
	
	public String getImagePair(Cursor c) {
		return(c.getString(16));
	}
}
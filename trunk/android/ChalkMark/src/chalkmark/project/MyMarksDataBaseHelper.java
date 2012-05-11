/* File: MyMarksDatabaseHelper.java
 * 
 * The marks that I have dropped for people
 * 
 */

package chalkmark.project;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class MyMarksDataBaseHelper extends SQLiteOpenHelper
{
	private static final String DATABASE_NAME = "mymarks.db";
	private static final int SCHEMA_VERSION = 1;
	private static final String TABLE_NAME_MARKS = "mymarks";
	
	private String select_str = "SELECT _id, " +
		"myuid, " +
		"to_uid, " +
		"from_image, " +
		"subject, " +
		"body, " +
		"image, " +
		"lat, " +
		"lon, " +
		"real_address, " +
		"expiration, " +
		"timedropped, " +
		"radius_yards, " +
		"imagepair, " +
		"message_id ";
	
	
	public MyMarksDataBaseHelper(Context context)
	{
		super(context, DATABASE_NAME, null, SCHEMA_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) 
	{ // Create the needed database tables
		String dbstring = 
			"CREATE TABLE mymarks (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"myuid TEXT, " +			// This user's unique ID. Used to filter what shows up in the mymarks
				"to_uid TEXT, " +			// the unique ID of the sender
				"from_image TEXT, " + 		// URL of contact image
				"subject TEXT, " +			// The subject of the message
				"body TEXT, " + 			// The body of the message
				"image TEXT, " +  			// URL of attached image
				"lat REAL, " +				// 
				"lon REAL, " +
				"real_address STRING, " +
				"expiration INTEGER, " +	// days remaining till expiration
				"timedropped REAL, " +
				"radius_yards INTEGER, " +
				"imagepair STRING, " +
				"message_id" +
			");";
		db.execSQL(dbstring);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
	{
		// Upgrading database, this will drop tables and recreate.
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_MARKS);
		onCreate(db);
	}
	
	public void insert(
			String myuid, String to_uid, String from_image,
			String subject, String body, String image,
			double lat, double lon, String real_address,
			int expiration, double timedropped, int radius_yards,
			String imagepair, String message_id)
	{ /* In this function we pour the individual attributes of a Mark, such as its subject, body, etc., 
	   * into a ContentValues container and insert the container into the database.
	   */ 
		ContentValues cv = new ContentValues();
		cv.put("myuid", myuid);
		cv.put("to_uid", to_uid);
		cv.put("from_image", from_image);
		cv.put("subject", subject);
		cv.put("body", body);
		cv.put("image", image);
		cv.put("lat", lat);
		cv.put("lon", lon);
		cv.put("real_address", real_address);
		cv.put("expiration", expiration);
		cv.put("timedropped", timedropped);
		cv.put("radius_yards", radius_yards);
		cv.put("imagepair", imagepair);
		cv.put("message_id", message_id);
		getWritableDatabase().insert("mymarks","subject", cv);
	}
	
	public void deleteMarkByID(String ID)
	{ /* Return an Mark from the database that matches the input ID
	   */
		String[] args = {ID};
		getWritableDatabase().delete("mymarks", "_ID=?", args);
	}
	
	
	public Cursor getAllMarks(String OrderBy)
	{ /* This function rerpesents a cursor with a columnar data for the egg basket table
	   * A cursor is an encapsulation of the result set of the query, plus the query that
	   * was used to create it.
	   */
		return(getReadableDatabase().rawQuery(select_str + "FROM mymarks ORDER BY " + OrderBy + " DESC", null));
	}
	
	public Cursor getMarksFilteredBy(String selector, String value, String OrderBy)
	{ /* Get all Geomarks filtered by a specific attribute in the specified order */
		return(getReadableDatabase().rawQuery(select_str + "FROM mymarks WHERE " + selector + " = \"" + value + "\" ORDER BY " + OrderBy + " DESC", null));
	}
	
	
	public void update(String ID, 
			String myuid, String to_uid, String from_image,
			String subject, String body, String image,
			double lat, double lon, String real_address,
			int expiration, double timedropped, int radius_yards,
			String imagepair, String message_id)
	{
		ContentValues cv=new ContentValues();
		String[] args={ID};
		cv.put("myuid", myuid);
		cv.put("to_uid", to_uid);
		cv.put("from_image", from_image);
		cv.put("subject", subject);
		cv.put("body", body);
		cv.put("image", image);
		cv.put("lat", lat);
		cv.put("lon", lon);
		cv.put("real_address", real_address);
		cv.put("expiration", expiration);
		cv.put("timedropped", timedropped);
		cv.put("radius_yards", radius_yards);
		cv.put("imagepair", imagepair);
		cv.put("message_id", message_id);
		getWritableDatabase().update("mymarks", cv, "_ID=?", args);
	}
	
	
	public void updateMsgID(String Row_ID, String msgid)
	{
		ContentValues cv=new ContentValues();
		String[] args={Row_ID};
		cv.put("message_id", msgid);
		getWritableDatabase().update("mymarks", cv, "_ID=?", args);
	}
	
	
	public Cursor getByID(String ID)
	{ /* Return an Mark from the database that matches the input ID
	   */
		String[] args = {ID};
		return (getReadableDatabase().rawQuery(select_str + "FROM mymarks WHERE _ID=?", args));
	}
	
	
	/* Implement Some Other Queary Functions  For individual items in the database
	 */
	
	public String getRowID(Cursor c) {
		return(c.getString(0));
	}
	
	public String getMyUid(Cursor c) {
		return(c.getString(1));
	}
	
	public String getToid(Cursor c) {
		return(c.getString(2));
	}
	
	public String getFromImage(Cursor c) {
		return(c.getString(3));
	}
	
	public String getSubject(Cursor c) {
		return(c.getString(4));
	}
	
	public String getBody(Cursor c) {
		return(c.getString(5));
	}
	
	public String getImage(Cursor c) {
		return(c.getString(6));
	}
	
	
	public double getLat(Cursor c) {
		return(c.getDouble(7));
	}
	
	public double getLon(Cursor c) {
		return(c.getDouble(8));
	}
	
	public String getRealAddress(Cursor c) {
		return(c.getString(9));
	}
	
	public int getExpiration(Cursor c) {
		return(c.getInt(10));
	}
	
	public double getTimeDropped(Cursor c) {
		return(c.getDouble(11));
	}
	
	public int getRadius(Cursor c) {
		return(c.getInt(12));
	}
	
	public String getImagePair(Cursor c) {
		return(c.getString(13));
	}
	
	public String getMsgID(Cursor c) {
		return(c.getString(14));
	}
	
}
/* File: EggDataBase.java
 * 
 * Main Activity for the EggDrop Data Base
 * 
 */

package chalkmark.project;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

class ContactsDataBaseHelper extends SQLiteOpenHelper
{
        private static final String DATABASE_NAME="contacts.db";
        private static final int SCHEMA_VERSION = 1;
        private static final String TABLE_NAME_CONTACTS = "contacts";
        
        public ContactsDataBaseHelper(Context context)
        {
                super(context, DATABASE_NAME, null, SCHEMA_VERSION);
        }
        
        @Override
        public void onCreate(SQLiteDatabase db) 
        { // Create the needed database tables
                db.execSQL(
                	"CREATE TABLE contacts (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                		"name TEXT, " +
                		"uid TEXT, " +
                		"email TEXT, " +
                		"type TEXT);"
                );
        }
        
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
        {
                Log.w("Example", "Upgrading database, this will drop tables and recreate.");
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_CONTACTS);
                onCreate(db);
        }
        
        public void insert(String name, String uid, String email, String type)
        { /* In this function we pour the individual attributes of a contact
           * into a ContentValues container and insert the container into the database.
           */ 
                ContentValues cv = new ContentValues();
                cv.put("name", name);
                cv.put("uid", uid);
                cv.put("email", email);
                cv.put("type", type);
                getWritableDatabase().insert("contacts","name", cv);
        }
        
        public void deleteContactByID(String ID)
        { /* Delete a contact from the database that matches the input ID
           */
                String[] args = {ID};
                getWritableDatabase().delete("contacts", "_ID=?", args);
        }
        
        public void deleteall() {
            getWritableDatabase().delete("contacts", null, null);
        }
        
        
        public Cursor getAllContacts(String OrderBy)
        { /* This function represents a cursor with a columnar data for the contacts table
           * A cursor is an encapsulation of the result set of the query, plus the query that
           * was used to create it.
           */
                return(getReadableDatabase()
                	.rawQuery("SELECT _id, name, uid, email, type " +
                		"FROM contacts ORDER BY " + OrderBy, null));
        }
        
        public Cursor getContactsFilteredBy(String selector, String value, String OrderBy)
        { /* Get all contacts filtered by a specific attribute in the specified order */
                return(getReadableDatabase()
                   .rawQuery("SELECT _id, name, uid, email, type " +
                   		"FROM contacts WHERE " + selector + " = \"" + value + "\" ORDER BY " + OrderBy, null));
        }
        
        public void update(String ID, String name, String uid, String email, String type)
        {
                ContentValues cv=new ContentValues();
                String[] args={ID};
                cv.put("name", name);
                cv.put("uid", uid);
                cv.put("email", email);
                cv.put("type", type);
                getWritableDatabase().update("contacts", cv, "_ID=?", args);
        }
        
        
        public Cursor getByID(String ID)
        { /* Return a contact from the database that matches the input ID
           */
                String[] args = {ID};
                return (getReadableDatabase()
                      .rawQuery("SELECT _id, name, uid, email, type " +
                      		"FROM contacts WHERE _ID=?", args));
        }
        
        public Cursor getByName(String name)
        { /* Return a contact from the database that matches the input ID
           */
                String[] args = {name};
                return (getReadableDatabase()
                      .rawQuery("SELECT _id, name, uid, email, type " +
                      		"FROM contacts WHERE name=?", args));
        }
        
        
        /* Implement Some Other Queary Functions  For individual items in the database
         */
        
        public String getName(Cursor c) {
                return(c.getString(1));
        }
        
        public String getUid(Cursor c) {
            return(c.getString(2));
        }
        
        public String getEmail(Cursor c) {
                return(c.getString(3));
        }
        
        public String getType(Cursor c) {
                return(c.getString(4));
        }
}
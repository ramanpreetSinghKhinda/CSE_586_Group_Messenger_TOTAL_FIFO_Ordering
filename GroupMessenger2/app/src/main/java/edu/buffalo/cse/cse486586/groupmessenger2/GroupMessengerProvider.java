package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * <p/>
 * Please read:
 * <p/>
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * <p/>
 * before you start to get yourself familiarized with ContentProvider.
 * <p/>
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 *
 * @author stevko
 */
public class GroupMessengerProvider extends ContentProvider {
    /**
     * Referred below tutorial for understanding concepts.
     * However my implementation and usage is completely different.
     * <p/>
     * http://developer.android.com/guide/topics/providers/content-provider-creating.html
     * http://www.tutorialspoint.com/android/android_content_providers.htm
     */
    private static final String TAG = GroupMessengerProvider.class.getName();

    /**
     * Database specific constant declarations
     */
    private SQLiteDatabase db;
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "GroupMessenger";
    private static final String TABLE_NAME = "PA_2_Part_B";
    private static final String COL_KEY_FIELD = "key";
    private static final String COL_VALUE_FIELD = "value";

    // A string that defines the SQL statement for creating a table
    private static final String CREATE_DB_TABLE = " CREATE TABLE " +
            TABLE_NAME +
            " (" +
            COL_KEY_FIELD + " TEXT NOT NULL, " +
            COL_VALUE_FIELD + " TEXT NOT NULL);";

    /**
     * Helper class that actually creates and manages the provider's underlying data repository.
     */
    private static class RamanDatabaseHelper extends SQLiteOpenHelper {
        /**
         * Instantiates an open helper for the provider's SQLite data repository
         * Do not do database creation and upgrade here.
         */
        RamanDatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        /**
         * Creates the data repository.
         * This is called when the provider attempts to open the repository
         * and SQLite reports that it doesn't exist.
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            // Creates the Database table
            db.execSQL(CREATE_DB_TABLE);
            Log.v(TAG, "Raman Database Table created");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.v(TAG, "Raman Database upgraded from VERSION : " + oldVersion + " to VERSION : " + newVersion);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }

    @Override
    public boolean onCreate() {
        Log.v(TAG, "***** Raman inside onCreate() START *****");

        Context context = getContext();

        /**
         * Creates a new helper object. This method always returns quickly.
         * Notice that the database itself isn't created or opened
         * until SQLiteOpenHelper.getWritableDatabase is called
         */
        RamanDatabaseHelper dbHelper = new RamanDatabaseHelper(context);

        /**
         * Create a write able database which will trigger its
         * creation if it doesn't already exist.
         */
        db = dbHelper.getWritableDatabase();

        Log.v(TAG, "***** Raman inside onCreate() END. The created db is : " + db + " *****");
        return (db == null) ? false : true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        Log.v(TAG, "***** Raman inside query() START. Uri : " + uri + " , Selection : " + selection + " *****");

        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */

        /**
         * This is a convenience class that helps build SQL queries to be sent to SQLiteDatabase objects
         *
         * Reference : http://developer.android.com/reference/android/database/sqlite/SQLiteQueryBuilder.html
         */
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        // Sets the list of tables to query
        queryBuilder.setTables(TABLE_NAME);

        Cursor cursor = null;
        if(null == selection){
            Log.v(TAG, "***** Running Raman query *****");
            cursor = queryBuilder.query(db, projection, null, null, null, null, sortOrder);
        } else {
            cursor = queryBuilder.query(db, projection, COL_KEY_FIELD + "=?", new String[]{selection}, null, null, sortOrder);
        }
        Log.v(TAG, "***** Raman inside query END. Cursor returned is : " + cursor + " *****");
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.v(TAG, "***** Raman inside insert() START. Uri : " + uri + " , ContentValues : " + values.toString() + " *****");

        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         *
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */

        // checking if a value with specified key already exists
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(TABLE_NAME);
        Cursor cursor = queryBuilder.query(db, null, COL_KEY_FIELD + "=?", new String[]{values.getAsString(COL_KEY_FIELD)}, null, null, null);

        if (cursor.moveToFirst()) {
            Log.v(TAG, "Raman the specific KEY : " + values.getAsString(COL_KEY_FIELD) + " already exists hence only UPDATE the VALUE");

            db.update(TABLE_NAME, values, COL_KEY_FIELD + "=?", new String[]{values.getAsString(COL_KEY_FIELD)});
        } else {
            Log.v(TAG, "Raman inserting new KEY-VALUE pair");

            /**
             * Add a new record
             *
             * @return the row ID of the newly inserted row, or -1 if an error occurred
             */
            long rowId = db.insert(TABLE_NAME, "", values);

            /**
             * If record is added successfully
             */
            if (rowId > 0) {
                /**
                 * Appends the given ID to the end of the path
                 * This is used to access a particular row in case
                 */
                Uri contentUri = ContentUris.withAppendedId(uri, rowId);

                Log.v(TAG, "***** Raman inside insert() END. Query runs Successfully *****");
                return contentUri;
            }
        }
        Log.v(TAG, "***** Raman inside insert() END. Query Un-Successful *****");
        return uri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.v(TAG, "***** Raman inside delete() START. Uri : " + uri + " , Selection : " + selection + " *****");

        /**
         * TODO This is for testing purpose
         *
         * @return the number of rows affected if a whereClause is passed in, 0
         *         otherwise. To remove all rows and get a count pass "1" as the
         *         whereClause.
         */
        int rows = db.delete(TABLE_NAME, null, null);

        Log.v(TAG, "****** Raman inside delete() END. Rows deleted : " + rows + " *****");
        return rows;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Log.v(TAG, "***** Raman inside update() :-  Uri : " + uri + " , ContentValues : " + values.toString() + " , Selection : " + selection + " *****");

        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        Log.v(TAG, "****** Raman inside getType(). Uri : " + uri + " *****");

        // You do not need to implement this.
        return null;
    }
}


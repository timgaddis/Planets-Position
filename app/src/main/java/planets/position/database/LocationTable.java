package planets.position.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class LocationTable {

    public static final String TABLE_LOCATION = "location";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_LATITUDE = "lat";
    public static final String COLUMN_LONGITUDE = "long";
    public static final String COLUMN_ELEVATION = "elevation";
    public static final String COLUMN_TEMP = "tempature";
    public static final String COLUMN_PRESSURE = "pressure";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_OFFSET = "offset";
    public static final String COLUMN_IOFFSET = "ioffset";

    private static final String DATABASE_CREATE = "create table "
            + TABLE_LOCATION + "(" + COLUMN_ID
            + " integer primary key autoincrement, " + COLUMN_NAME
            + " text not null, " + COLUMN_LATITUDE + " real,"
            + COLUMN_LONGITUDE + " real, " + COLUMN_ELEVATION + " real, "
            + COLUMN_TEMP + " real, " + COLUMN_PRESSURE + " real, "
            + COLUMN_DATE + " integer, " + COLUMN_OFFSET + " real, "
            + COLUMN_IOFFSET + " integer" + ");";

    public static void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
        database.execSQL("insert into " + TABLE_LOCATION + "(" + COLUMN_ID
                + "," + COLUMN_NAME + "," + COLUMN_LATITUDE + ","
                + COLUMN_LONGITUDE + "," + COLUMN_ELEVATION + "," + COLUMN_TEMP
                + "," + COLUMN_PRESSURE + "," + COLUMN_DATE + ","
                + COLUMN_OFFSET + "," + COLUMN_IOFFSET
                + ") VALUES (0,\"default\",-91.0,0.0,0.0,0.0,0.0,0,0.0,15);");
    }

    public static void onUpgrade(SQLiteDatabase database, int oldVersion,
                                 int newVersion) {
        Log.w(LocationTable.class.getName(), "Upgrading database from version "
                + oldVersion + " to " + newVersion
                + ", which will destroy all old data");
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATION);
        onCreate(database);
    }
}

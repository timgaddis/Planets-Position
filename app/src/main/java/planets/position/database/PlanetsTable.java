package planets.position.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class PlanetsTable {

    public static final String TABLE_PLANET = "planets";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_RA = "ra";
    public static final String COLUMN_DEC = "dec";
    public static final String COLUMN_AZ = "az";
    public static final String COLUMN_ALT = "alt";
    public static final String COLUMN_DISTANCE = "dis";
    public static final String COLUMN_MAGNITUDE = "mag";
    public static final String COLUMN_SET_TIME = "setT";

    private static final String DATABASE_CREATE = "create table "
            + TABLE_PLANET + "(" + COLUMN_ID
            + " integer primary key autoincrement, " + COLUMN_NAME
            + " text not null, " + COLUMN_RA + " real," + COLUMN_DEC
            + " real, " + COLUMN_AZ + " real, " + COLUMN_ALT + " real, "
            + COLUMN_DISTANCE + " real, " + COLUMN_MAGNITUDE + " real, "
            + COLUMN_SET_TIME + " integer " + ");";

    public static void onCreate(SQLiteDatabase database) {
        String ip1, ip2;
        database.execSQL(DATABASE_CREATE);
        ip1 = "insert into " + TABLE_PLANET + "(" + COLUMN_ID + ","
                + COLUMN_NAME + "," + COLUMN_RA + "," + COLUMN_DEC + ","
                + COLUMN_AZ + "," + COLUMN_ALT + "," + COLUMN_DISTANCE + ","
                + COLUMN_MAGNITUDE + "," + COLUMN_SET_TIME + ") VALUES (";
        ip2 = ", 'P', 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0);";
        for (int i = 0; i < 10; i++) {
            database.execSQL(ip1 + i + ip2);
        }
    }

    public static void onUpgrade(SQLiteDatabase database, int oldVersion,
                                 int newVersion) {
        Log.w(PlanetsTable.class.getName(), "Upgrading database from version "
                + oldVersion + " to " + newVersion
                + ", which will destroy all old data");
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_PLANET);
        onCreate(database);
    }
}

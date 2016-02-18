package planets.position.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class LunarOccultationTable {

    public static final String TABLE_LUNAR_OCCULT = "lunarOccult";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_LOCAL_TYPE = "localType";
    public static final String COLUMN_GLOBAL_TYPE = "globalType";
    public static final String COLUMN_LOCAL = "local";
    public static final String COLUMN_LOCAL_MAX = "localMax";
    public static final String COLUMN_LOCAL_FIRST = "localFirst";
    public static final String COLUMN_LOCAL_SECOND = "localSecond";
    public static final String COLUMN_LOCAL_THIRD = "localThird";
    public static final String COLUMN_LOCAL_FOURTH = "localFourth";
    public static final String COLUMN_MOONRISE = "moonRise";
    public static final String COLUMN_MOONSET = "moonSet";
    public static final String COLUMN_MOONS_AZ = "moonAzStart";
    public static final String COLUMN_MOONS_ALT = "moonAltStart";
    public static final String COLUMN_MOONE_AZ = "moonAzEnd";
    public static final String COLUMN_MOONE_ALT = "moonAltEnd";
    public static final String COLUMN_GLOBAL_MAX = "globalMax";
    public static final String COLUMN_GLOBAL_BEGIN = "globalBegin";
    public static final String COLUMN_GLOBAL_END = "globalEnd";
    public static final String COLUMN_GLOBAL_TOTAL_BEGIN = "globalTotalBegin";
    public static final String COLUMN_GLOBAL_TOTAL_END = "globalTotalEnd";
    public static final String COLUMN_GLOBAL_CENTER_BEGIN = "globalCenterBegin";
    public static final String COLUMN_GLOBAL_CENTER_END = "globalCenterEnd";
    public static final String COLUMN_OCCULT_DATE = "occultDate";
    public static final String COLUMN_OCCULT_PLANET = "occultPlanet";

    private static final String DATABASE_CREATE = "create table "
            + TABLE_LUNAR_OCCULT + "(" + COLUMN_ID
            + " integer primary key autoincrement, " + COLUMN_LOCAL_TYPE
            + " integer, " + COLUMN_GLOBAL_TYPE + " integer," + COLUMN_LOCAL
            + " integer, " + COLUMN_LOCAL_MAX + " real, " + COLUMN_LOCAL_FIRST
            + " real, " + COLUMN_LOCAL_SECOND + " real, " + COLUMN_LOCAL_THIRD
            + " real, " + COLUMN_LOCAL_FOURTH + " real, " + COLUMN_MOONRISE
            + " real, " + COLUMN_MOONSET + " real, " + COLUMN_MOONS_AZ
            + " real, " + COLUMN_MOONS_ALT + " real, " + COLUMN_MOONE_AZ
            + " real, " + COLUMN_MOONE_ALT + " real, " + COLUMN_GLOBAL_MAX
            + " real, " + COLUMN_GLOBAL_BEGIN + " real, " + COLUMN_GLOBAL_END
            + " real, " + COLUMN_GLOBAL_TOTAL_BEGIN + " real, "
            + COLUMN_GLOBAL_TOTAL_END + " real, " + COLUMN_GLOBAL_CENTER_BEGIN
            + " real, " + COLUMN_GLOBAL_CENTER_END + " real, "
            + COLUMN_OCCULT_DATE + " real, " + COLUMN_OCCULT_PLANET
            + " integer);";

    public static void onCreate(SQLiteDatabase database) {
        String ip1, ip2;
        database.execSQL(DATABASE_CREATE);
        ip1 = "insert into " + TABLE_LUNAR_OCCULT + "(" + COLUMN_ID + ","
                + COLUMN_LOCAL_TYPE + "," + COLUMN_GLOBAL_TYPE + ","
                + COLUMN_LOCAL + "," + COLUMN_LOCAL_MAX + ","
                + COLUMN_LOCAL_FIRST + "," + COLUMN_LOCAL_SECOND + ","
                + COLUMN_LOCAL_THIRD + "," + COLUMN_LOCAL_FOURTH + ","
                + COLUMN_MOONRISE + "," + COLUMN_MOONSET + ","
                + COLUMN_MOONS_AZ + "," + COLUMN_MOONS_ALT + ","
                + COLUMN_MOONE_AZ + "," + COLUMN_MOONE_ALT + ","
                + COLUMN_GLOBAL_MAX + "," + COLUMN_GLOBAL_BEGIN + ","
                + COLUMN_GLOBAL_END + "," + COLUMN_GLOBAL_TOTAL_BEGIN + ","
                + COLUMN_GLOBAL_TOTAL_END + "," + COLUMN_GLOBAL_CENTER_BEGIN
                + "," + COLUMN_GLOBAL_CENTER_END + "," + COLUMN_OCCULT_DATE
                + "," + COLUMN_OCCULT_PLANET + ") VALUES (";
        ip2 = ",0,0,-1,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"
                + "0.0,0.0,0.0,0.0,-1);";
        for (int i = 0; i < 10; i++) {
            database.execSQL(ip1 + i + ip2);
        }
    }

    public static void onUpgrade(SQLiteDatabase database, int oldVersion,
                                 int newVersion) {
        Log.w(LunarOccultationTable.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_LUNAR_OCCULT);
        onCreate(database);
    }
}

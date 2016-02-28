package planets.position.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import planets.position.PlanetsMain;

class PlanetsDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "PlanetsDatabase.db";
    private static final int DATABASE_VERSION = 203;
    private final Context mContext;

    public PlanetsDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        LocationTable.onCreate(database);
        PlanetsTable.onCreate(database);
        SolarEclipseTable.onCreate(database);
        LunarEclipseTable.onCreate(database);
        LunarOccultationTable.onCreate(database);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion,
                          int newVersion) {
        // clear existing preferences
        mContext.getSharedPreferences(PlanetsMain.MAIN_PREFS, 0).edit().clear().commit();
        LocationTable.onUpgrade(database, oldVersion, newVersion);
        PlanetsTable.onUpgrade(database, oldVersion, newVersion);
        SolarEclipseTable.onUpgrade(database, oldVersion, newVersion);
        LunarEclipseTable.onUpgrade(database, oldVersion, newVersion);
        LunarOccultationTable.onUpgrade(database, oldVersion, newVersion);
    }

}

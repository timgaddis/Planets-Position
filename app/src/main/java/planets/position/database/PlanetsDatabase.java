package planets.position.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

public class PlanetsDatabase {

    private SQLiteDatabase database;
    private PlanetsDatabaseHelper dbHelper;

    private String[] locationColumns = {LocationTable.COLUMN_ID,
            LocationTable.COLUMN_LATITUDE, LocationTable.COLUMN_LONGITUDE,
            LocationTable.COLUMN_ELEVATION, LocationTable.COLUMN_OFFSET,
            LocationTable.COLUMN_IOFFSET};
    private String[] whatsUpColumns = {PlanetsTable.COLUMN_ID,
            PlanetsTable.COLUMN_NAME, PlanetsTable.COLUMN_AZ,
            PlanetsTable.COLUMN_ALT};
    private String[] planetDataColumns = {PlanetsTable.COLUMN_ID,
            PlanetsTable.COLUMN_NAME, PlanetsTable.COLUMN_RA,
            PlanetsTable.COLUMN_DEC, PlanetsTable.COLUMN_AZ,
            PlanetsTable.COLUMN_ALT, PlanetsTable.COLUMN_DISTANCE,
            PlanetsTable.COLUMN_MAGNITUDE, PlanetsTable.COLUMN_SET_TIME};
    private String[] solarEclipseColumns = {SolarEclipseTable.COLUMN_ID,
            SolarEclipseTable.COLUMN_GLOBAL_TYPE,
            SolarEclipseTable.COLUMN_ECLIPSE_DATE,
            SolarEclipseTable.COLUMN_ECLIPSE_TYPE,
            SolarEclipseTable.COLUMN_LOCAL};
    private String[] solarDataColumns = {SolarEclipseTable.COLUMN_ID,
            SolarEclipseTable.COLUMN_LOCAL, SolarEclipseTable.COLUMN_LOCAL_MAX,
            SolarEclipseTable.COLUMN_LOCAL_FIRST,
            SolarEclipseTable.COLUMN_LOCAL_SECOND,
            SolarEclipseTable.COLUMN_LOCAL_THIRD,
            SolarEclipseTable.COLUMN_LOCAL_FOURTH,
            SolarEclipseTable.COLUMN_SUNRISE,
            SolarEclipseTable.COLUMN_SUNSET,
            SolarEclipseTable.COLUMN_GLOBAL_MAX,
            SolarEclipseTable.COLUMN_GLOBAL_BEGIN,
            SolarEclipseTable.COLUMN_GLOBAL_END,
            SolarEclipseTable.COLUMN_GLOBAL_TOTAL_BEGIN,
            SolarEclipseTable.COLUMN_GLOBAL_TOTAL_END,
            SolarEclipseTable.COLUMN_GLOBAL_CENTER_BEGIN,
            SolarEclipseTable.COLUMN_GLOBAL_CENTER_END,
            SolarEclipseTable.COLUMN_FRACTION_COVERED,
            SolarEclipseTable.COLUMN_SUN_AZ, SolarEclipseTable.COLUMN_SUN_ALT,
            SolarEclipseTable.COLUMN_LOCAL_MAG,
            SolarEclipseTable.COLUMN_SAROS_NUM,
            SolarEclipseTable.COLUMN_SAROS_MEMBER_NUM,
            SolarEclipseTable.COLUMN_ECLIPSE_DATE,
            SolarEclipseTable.COLUMN_ECLIPSE_TYPE};

    public PlanetsDatabase(Context context) {
        dbHelper = new PlanetsDatabaseHelper(context);
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        database.close();
    }

    public int addLocation(ContentValues values) {
        return database.update(LocationTable.TABLE_LOCATION, values,
                LocationTable.COLUMN_ID + " = 0", null);
    }

    public Bundle getLocation() {

        Cursor c = database.query(LocationTable.TABLE_LOCATION, locationColumns,
                null, null, null, null, null);
        c.moveToFirst();

        Bundle out = new Bundle();
        out.putDouble("latitude", c.getDouble(c.getColumnIndex(LocationTable.COLUMN_LATITUDE)));
        out.putDouble("longitude", c.getDouble(c.getColumnIndex(LocationTable.COLUMN_LONGITUDE)));
        out.putDouble("elevation", c.getDouble(c.getColumnIndex(LocationTable.COLUMN_ELEVATION)));
        out.putDouble("offset", c.getDouble(c.getColumnIndex(LocationTable.COLUMN_OFFSET)));
        out.putInt("ioffset", c.getInt(c.getColumnIndex(LocationTable.COLUMN_IOFFSET)));

        c.close();

        return out;
    }

    public Cursor getPlanets() {
        return database.query(PlanetsTable.TABLE_PLANET, whatsUpColumns, "alt > 0.0",
                null, null, null, null);
    }

    public Bundle getPlanet(long planet) {

        Bundle out = new Bundle();
        Cursor c = database.query(PlanetsTable.TABLE_PLANET, planetDataColumns,
                PlanetsTable.COLUMN_ID + " = ?", new String[]{String.valueOf(planet)},
                null, null, null);
        c.moveToFirst();

        out.putString("name", c.getString(c.getColumnIndex(PlanetsTable.COLUMN_NAME)));
        out.putDouble("ra", c.getDouble(c.getColumnIndex(PlanetsTable.COLUMN_RA)));
        out.putDouble("dec", c.getDouble(c.getColumnIndex(PlanetsTable.COLUMN_DEC)));
        out.putDouble("az", c.getDouble(c.getColumnIndex(PlanetsTable.COLUMN_AZ)));
        out.putDouble("alt", c.getDouble(c.getColumnIndex(PlanetsTable.COLUMN_ALT)));
        out.putDouble("distance", c.getDouble(c.getColumnIndex(PlanetsTable.COLUMN_DISTANCE)));
        out.putDouble("mag", c.getDouble(c.getColumnIndex(PlanetsTable.COLUMN_MAGNITUDE)));
        out.putLong("setTime", c.getLong(c.getColumnIndex(PlanetsTable.COLUMN_SET_TIME)));

        c.close();

        return out;
    }

    public int addPlanet(ContentValues values, int row) {
        return database.update(PlanetsTable.TABLE_PLANET, values, PlanetsTable.COLUMN_ID + " = ?",
                new String[]{String.valueOf(row)});
    }

    public Cursor getSolarEclipseList() {
        return database.query(SolarEclipseTable.TABLE_SOLAR_ECLIPSE, solarEclipseColumns, null,
                null, null, null, SolarEclipseTable.COLUMN_GLOBAL_BEGIN);
    }

    public Bundle getSolarEclipse(long solar) {

        Bundle out = new Bundle();
        Cursor c = database.query(SolarEclipseTable.TABLE_SOLAR_ECLIPSE, solarDataColumns,
                SolarEclipseTable.COLUMN_ID + " = ?", new String[]{String.valueOf(solar)},
                null, null, null);
        c.moveToFirst();

        out.putLong(SolarEclipseTable.COLUMN_ECLIPSE_DATE, c.getLong(c.getColumnIndex(SolarEclipseTable.COLUMN_ECLIPSE_DATE)));
        out.putString(SolarEclipseTable.COLUMN_ECLIPSE_TYPE, c.getString(c.getColumnIndex(SolarEclipseTable.COLUMN_ECLIPSE_TYPE)));
        out.putDouble(SolarEclipseTable.COLUMN_GLOBAL_CENTER_BEGIN, c.getDouble(c.getColumnIndex(SolarEclipseTable.COLUMN_GLOBAL_CENTER_BEGIN)));
        out.putDouble(SolarEclipseTable.COLUMN_GLOBAL_CENTER_END, c.getDouble(c.getColumnIndex(SolarEclipseTable.COLUMN_GLOBAL_CENTER_END)));
        out.putInt(SolarEclipseTable.COLUMN_LOCAL, c.getInt(c.getColumnIndex(SolarEclipseTable.COLUMN_LOCAL)));
        out.putDouble(SolarEclipseTable.COLUMN_SUNRISE, c.getDouble(c.getColumnIndex(SolarEclipseTable.COLUMN_SUNRISE)));
        out.putDouble(SolarEclipseTable.COLUMN_SUNSET, c.getDouble(c.getColumnIndex(SolarEclipseTable.COLUMN_SUNSET)));
        out.putDouble(SolarEclipseTable.COLUMN_LOCAL_FIRST, c.getDouble(c.getColumnIndex(SolarEclipseTable.COLUMN_LOCAL_FIRST)));
        out.putDouble(SolarEclipseTable.COLUMN_LOCAL_SECOND, c.getDouble(c.getColumnIndex(SolarEclipseTable.COLUMN_LOCAL_SECOND)));
        out.putDouble(SolarEclipseTable.COLUMN_LOCAL_MAX, c.getDouble(c.getColumnIndex(SolarEclipseTable.COLUMN_LOCAL_MAX)));
        out.putDouble(SolarEclipseTable.COLUMN_LOCAL_THIRD, c.getDouble(c.getColumnIndex(SolarEclipseTable.COLUMN_LOCAL_THIRD)));
        out.putDouble(SolarEclipseTable.COLUMN_LOCAL_FOURTH, c.getDouble(c.getColumnIndex(SolarEclipseTable.COLUMN_LOCAL_FOURTH)));
        out.putDouble(SolarEclipseTable.COLUMN_SUN_AZ, c.getDouble(c.getColumnIndex(SolarEclipseTable.COLUMN_SUN_AZ)));
        out.putDouble(SolarEclipseTable.COLUMN_SUN_ALT, c.getDouble(c.getColumnIndex(SolarEclipseTable.COLUMN_SUN_ALT)));
        out.putDouble(SolarEclipseTable.COLUMN_FRACTION_COVERED, c.getDouble(c.getColumnIndex(SolarEclipseTable.COLUMN_FRACTION_COVERED)));
        out.putDouble(SolarEclipseTable.COLUMN_LOCAL_MAG, c.getDouble(c.getColumnIndex(SolarEclipseTable.COLUMN_LOCAL_MAG)));
        out.putInt(SolarEclipseTable.COLUMN_SAROS_NUM, c.getInt(c.getColumnIndex(SolarEclipseTable.COLUMN_SAROS_NUM)));
        out.putInt(SolarEclipseTable.COLUMN_SAROS_MEMBER_NUM, c.getInt(c.getColumnIndex(SolarEclipseTable.COLUMN_SAROS_MEMBER_NUM)));
        out.putDouble(SolarEclipseTable.COLUMN_GLOBAL_BEGIN, c.getDouble(c.getColumnIndex(SolarEclipseTable.COLUMN_GLOBAL_BEGIN)));
        out.putDouble(SolarEclipseTable.COLUMN_GLOBAL_TOTAL_BEGIN, c.getDouble(c.getColumnIndex(SolarEclipseTable.COLUMN_GLOBAL_TOTAL_BEGIN)));
        out.putDouble(SolarEclipseTable.COLUMN_GLOBAL_MAX, c.getDouble(c.getColumnIndex(SolarEclipseTable.COLUMN_GLOBAL_MAX)));
        out.putDouble(SolarEclipseTable.COLUMN_GLOBAL_TOTAL_END, c.getDouble(c.getColumnIndex(SolarEclipseTable.COLUMN_GLOBAL_TOTAL_END)));
        out.putDouble(SolarEclipseTable.COLUMN_GLOBAL_END, c.getDouble(c.getColumnIndex(SolarEclipseTable.COLUMN_GLOBAL_END)));

        c.close();

        return out;
    }

    public int addSolarEclipse(ContentValues values, int row) {
        return database.update(SolarEclipseTable.TABLE_SOLAR_ECLIPSE, values, SolarEclipseTable.COLUMN_ID + " = ?",
                new String[]{String.valueOf(row)});
    }
}

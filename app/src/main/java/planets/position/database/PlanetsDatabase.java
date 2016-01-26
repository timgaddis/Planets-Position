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
}

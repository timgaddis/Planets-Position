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

    public PlanetsDatabase(Context context) {
        dbHelper = new PlanetsDatabaseHelper(context);
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        database.close();
    }

    public long addLocation(ContentValues values) {
        return database.insert(LocationTable.TABLE_LOCATION, null, values);
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
}

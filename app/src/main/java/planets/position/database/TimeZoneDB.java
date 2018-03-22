/*
 * Planet's Position
 * A program to calculate the position of the planets in the night sky based
 * on a given location on Earth.
 * Copyright (c) 2018 Tim Gaddis
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package planets.position.database;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;

import java.io.File;

public class TimeZoneDB extends SQLiteOpenHelper {

    private final String CITIES_TABLE = "worldcities";
    private final String ZONE_TABLE = "zone";

    public final static String DB_NAME = "timezoneDB.db";
    private final Context myContext;
    private SQLiteDatabase timezoneDatabase;

    private String[] timezoneColumns = {"gmt_offset", "dst"};

    public TimeZoneDB(Context context) {
        super(context, DB_NAME, null, 1);
        this.myContext = context;
    }

    public void open() throws SQLException {
        String myPath = myContext.getFilesDir().getAbsolutePath() + File.separator + "databases"
                + File.separator + DB_NAME;
        timezoneDatabase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
    }

    public  void close() {
        super.close();
        if (timezoneDatabase != null)
            timezoneDatabase.close();
    }

    public Cursor getZoneList() {
        return timezoneDatabase.query(ZONE_TABLE, null, null, null,
                null, null, "zone_name");
    }

    public int getZoneID(String zone) {

        int id;
        Cursor c = timezoneDatabase.query(ZONE_TABLE, null, "zone_name = ?",
                new String[]{zone}, null, null, null);
        c.moveToFirst();
        if (c.getCount() > 0) {
            id = c.getInt(c.getColumnIndex("_id"));
        } else {
            id = -1;
        }
        c.close();

        return id;
    }

    /**
     * @param zone Timezone id
     * @param time Time in seconds from Jan 1, 1970
     * @return gmt offset in seconds
     */
    public int getZoneOffset(int zone, long time) {

        int offset;
        String TIMEZONE_TABLE = "timezone";
        Cursor c = timezoneDatabase.query(TIMEZONE_TABLE, timezoneColumns,
                "zone_id = ? AND time_start <= ?",
                new String[]{String.valueOf(zone), String.valueOf(time)}, null,
                null, "time_start DESC", "1");
        c.moveToFirst();
        if (c.getCount() > 0) {
            offset = c.getInt(c.getColumnIndex("gmt_offset"));
//            dst = c.getInt(c.getColumnIndex("dst"));
        } else {
            offset = -1;
//            dst = -1;
        }
        c.close();
        return offset;
    }

    public Cursor getStateList(String name) {
        String STATES_TALE = "states";
        return timezoneDatabase.query(STATES_TALE, null, "country = ?",
                new String[]{name}, null, null, "state");
    }

    public Cursor getCityList(String country, String state) {
        return timezoneDatabase.query(CITIES_TABLE, new String[]{"_id", "city"},
                "country = ? AND state <= ?", new String[]{country, state},
                null, null, "city");
    }

    public Bundle getCityData(long id) {
        Bundle b = new Bundle();

        Cursor c = timezoneDatabase.query(CITIES_TABLE, null, "_id = ?",
                new String[]{String.valueOf(id)}, null, null, null);
        c.moveToFirst();

        b.putDouble("lat", c.getDouble(c.getColumnIndex("lat")));
        b.putDouble("lng", c.getDouble(c.getColumnIndex("lng")));
        b.putDouble("alt", c.getDouble(c.getColumnIndex("altitude")));
        b.putString("timezone", c.getString(c.getColumnIndex("timezone")));
        b.putString("city", c.getString(c.getColumnIndex("city")));

        c.close();
        return b;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}

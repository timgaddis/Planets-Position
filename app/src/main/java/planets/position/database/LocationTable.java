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

    private static final String DATABASE_CREATE = String.format("create table %s(%s integer primary key autoincrement, %s text not null, %s real,%s real, %s real, %s real, %s real, %s integer, %s real, %s integer);", TABLE_LOCATION, COLUMN_ID, COLUMN_NAME, COLUMN_LATITUDE, COLUMN_LONGITUDE, COLUMN_ELEVATION, COLUMN_TEMP, COLUMN_PRESSURE, COLUMN_DATE, COLUMN_OFFSET, COLUMN_IOFFSET);

    public static void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
        database.execSQL(String.format("insert into %s(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) VALUES (0,\"default\",-91.0,0.0,0.0,0.0,0.0,0,0.0,15);", TABLE_LOCATION, COLUMN_ID, COLUMN_NAME, COLUMN_LATITUDE, COLUMN_LONGITUDE, COLUMN_ELEVATION, COLUMN_TEMP, COLUMN_PRESSURE, COLUMN_DATE, COLUMN_OFFSET, COLUMN_IOFFSET));
    }

    public static void onUpgrade(int oldVersion, int newVersion) {
        Log.w(LocationTable.class.getName(), "Upgrading database from version "
                + oldVersion + " to " + newVersion);
//        database.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATION);
//        onCreate(database);
    }
}

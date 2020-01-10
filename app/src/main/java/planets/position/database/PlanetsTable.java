/*
 * Planet's Position
 * A program to calculate the position of the planets in the night sky based
 * on a given location on Earth.
 * Copyright (c) 2020 Tim Gaddis
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

public class PlanetsTable {

    public static final String TABLE_NAME = "planets";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_NUMBER = "number";
    public static final String COLUMN_RISE = "rise";
    public static final String COLUMN_RA = "ra";
    public static final String COLUMN_DEC = "dec";
    public static final String COLUMN_AZ = "az";
    public static final String COLUMN_ALT = "alt";
    public static final String COLUMN_DISTANCE = "dis";
    public static final String COLUMN_MAGNITUDE = "mag";
    public static final String COLUMN_SET_TIME = "setT";
    public static final String COLUMN_RISE_TIME = "riseT";
    public static final String COLUMN_TRANSIT = "transit";

    private static final String DATABASE_CREATE = String.format("create table %s(%s integer primary key autoincrement, %s text not null, %s integer, %s integer, %s real, %s real, %s real, %s real, %s real, %s real, %s real, %s real, %s real);", TABLE_NAME, COLUMN_ID, COLUMN_NAME, COLUMN_NUMBER, COLUMN_RISE, COLUMN_RA, COLUMN_DEC, COLUMN_AZ, COLUMN_ALT, COLUMN_DISTANCE, COLUMN_MAGNITUDE, COLUMN_SET_TIME, COLUMN_RISE_TIME, COLUMN_TRANSIT);

    public static void onCreate(SQLiteDatabase database) {
        String ip1, ip2;
        database.execSQL(DATABASE_CREATE);
        ip1 = String.format("insert into %s(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) VALUES (", TABLE_NAME, COLUMN_ID, COLUMN_NAME, COLUMN_NUMBER, COLUMN_RISE, COLUMN_RA, COLUMN_DEC, COLUMN_AZ, COLUMN_ALT, COLUMN_DISTANCE, COLUMN_MAGNITUDE, COLUMN_SET_TIME, COLUMN_RISE_TIME, COLUMN_TRANSIT);
        ip2 = ", 'P', 0, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);";
        for (int i = 0; i < 10; i++) {
            database.execSQL(ip1 + i + ip2);
        }
    }

    static void onUpgrade(SQLiteDatabase database, int oldVersion,
                          int newVersion) {
        Log.w(PlanetsTable.class.getName(), "Upgrading database from version "
                + oldVersion + " to " + newVersion
                + ", which will destroy all old data");
        database.execSQL(String.format("DROP TABLE IF EXISTS %s", TABLE_NAME));
        onCreate(database);
    }
}

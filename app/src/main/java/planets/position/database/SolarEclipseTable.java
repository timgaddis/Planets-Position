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

public class SolarEclipseTable {

    public static final String TABLE_NAME = "solarEclipse";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_LOCAL_TYPE = "localType";
    public static final String COLUMN_GLOBAL_TYPE = "globalType";
    public static final String COLUMN_LOCAL = "local";
    public static final String COLUMN_LOCAL_MAX = "localMax";
    public static final String COLUMN_LOCAL_FIRST = "localFirst";
    public static final String COLUMN_LOCAL_SECOND = "localSecond";
    public static final String COLUMN_LOCAL_THIRD = "localThird";
    public static final String COLUMN_LOCAL_FOURTH = "localFourth";
    public static final String COLUMN_SUNRISE = "sunRise";
    public static final String COLUMN_SUNSET = "sunSet";
    public static final String COLUMN_RATIO = "ratio";
    public static final String COLUMN_FRACTION_COVERED = "fractionCovered";
    public static final String COLUMN_SUN_AZ = "sunAz";
    public static final String COLUMN_SUN_ALT = "sunAlt";
    public static final String COLUMN_LOCAL_MAG = "localMag";
    public static final String COLUMN_SAROS_NUM = "sarosNum";
    public static final String COLUMN_SAROS_MEMBER_NUM = "sarosMemNum";
    public static final String COLUMN_MOON_AZ = "moonAz";
    public static final String COLUMN_MOON_ALT = "moonAlt";
    public static final String COLUMN_GLOBAL_MAX = "globalMax";
    public static final String COLUMN_GLOBAL_BEGIN = "globalBegin";
    public static final String COLUMN_GLOBAL_END = "globalEnd";
    public static final String COLUMN_GLOBAL_TOTAL_BEGIN = "globalTotalBegin";
    public static final String COLUMN_GLOBAL_TOTAL_END = "globalTotalEnd";
    public static final String COLUMN_GLOBAL_CENTER_BEGIN = "globalCenterBegin";
    public static final String COLUMN_GLOBAL_CENTER_END = "globalCenterEnd";
    public static final String COLUMN_ECLIPSE_DATE = "eclipseDate";
    public static final String COLUMN_ECLIPSE_TYPE = "eclipseType";

    private static final String DATABASE_CREATE = String.format("create table %s(%s integer primary key autoincrement, %s integer, %s integer,%s integer, %s real, %s real, %s real, %s real, %s real, %s real, %s real, %s real, %s real, %s real, %s real, %s real, %s integer, %s integer, %s real, %s real, %s real, %s real, %s real, %s real, %s real, %s real, %s real, %s real, %s text);", TABLE_NAME, COLUMN_ID, COLUMN_LOCAL_TYPE, COLUMN_GLOBAL_TYPE, COLUMN_LOCAL, COLUMN_LOCAL_MAX, COLUMN_LOCAL_FIRST, COLUMN_LOCAL_SECOND, COLUMN_LOCAL_THIRD, COLUMN_LOCAL_FOURTH, COLUMN_SUNRISE, COLUMN_SUNSET, COLUMN_RATIO, COLUMN_FRACTION_COVERED, COLUMN_SUN_AZ, COLUMN_SUN_ALT, COLUMN_LOCAL_MAG, COLUMN_SAROS_NUM, COLUMN_SAROS_MEMBER_NUM, COLUMN_MOON_AZ, COLUMN_MOON_ALT, COLUMN_GLOBAL_MAX, COLUMN_GLOBAL_BEGIN, COLUMN_GLOBAL_END, COLUMN_GLOBAL_TOTAL_BEGIN, COLUMN_GLOBAL_TOTAL_END, COLUMN_GLOBAL_CENTER_BEGIN, COLUMN_GLOBAL_CENTER_END, COLUMN_ECLIPSE_DATE, COLUMN_ECLIPSE_TYPE);

    public static void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    static void onUpgrade(SQLiteDatabase database, int oldVersion,
                                 int newVersion) {
        Log.w(SolarEclipseTable.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        database.execSQL(String.format("DROP TABLE IF EXISTS %s", TABLE_NAME));
        onCreate(database);
    }
}

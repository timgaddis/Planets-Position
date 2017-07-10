/*
 * Planet's Position
 * A program to calculate the position of the planets in the night sky based
 * on a given location on Earth.
 * Copyright (c) 2017 Tim Gaddis
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

public class LunarEclipseTable {

    public static final String TABLE_LUNAR_ECLIPSE = "lunarEclipse";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_LOCAL_TYPE = "localType";
    public static final String COLUMN_GLOBAL_TYPE = "globalType";
    public static final String COLUMN_LOCAL = "local";
    public static final String COLUMN_UMBRAL_MAG = "umbralMag";
    public static final String COLUMN_PENUMBRAL_MAG = "penumbralMag";
    public static final String COLUMN_MOON_AZ = "moonAz";
    public static final String COLUMN_MOON_ALT = "moonAlt";
    public static final String COLUMN_MOONRISE = "moonRise";
    public static final String COLUMN_MOONSET = "moonSet";
    public static final String COLUMN_SAROS_NUM = "sarosNum";
    public static final String COLUMN_SAROS_MEMBER_NUM = "sarosMemNum";
    public static final String COLUMN_MAX_ECLIPSE = "maxEclipse";
    public static final String COLUMN_PARTIAL_BEGIN = "partialBegin";
    public static final String COLUMN_PARTIAL_END = "partialEnd";
    public static final String COLUMN_TOTAL_BEGIN = "totalBegin";
    public static final String COLUMN_TOTAL_END = "totalEnd";
    public static final String COLUMN_PENUMBRAL_BEGIN = "penumbralBegin";
    public static final String COLUMN_PENUMBRAL_END = "penumbralEnd";
    public static final String COLUMN_ECLIPSE_DATE = "eclipseDate";
    public static final String COLUMN_ECLIPSE_TYPE = "eclipseType";

    private static final String DATABASE_CREATE = "create table "
            + TABLE_LUNAR_ECLIPSE + "(" + COLUMN_ID
            + " integer primary key autoincrement, " + COLUMN_LOCAL_TYPE
            + " integer, " + COLUMN_GLOBAL_TYPE + " integer," + COLUMN_LOCAL
            + " integer, " + COLUMN_UMBRAL_MAG + " real, "
            + COLUMN_PENUMBRAL_MAG + " real, " + COLUMN_MOON_AZ + " real, "
            + COLUMN_MOON_ALT + " real, " + COLUMN_MOONRISE + " real, "
            + COLUMN_MOONSET + " real, " + COLUMN_SAROS_NUM + " integer, "
            + COLUMN_SAROS_MEMBER_NUM + " integer, " + COLUMN_MAX_ECLIPSE
            + " real, " + COLUMN_PARTIAL_BEGIN + " real, " + COLUMN_PARTIAL_END
            + " real, " + COLUMN_TOTAL_BEGIN + " real, " + COLUMN_TOTAL_END
            + " real, " + COLUMN_PENUMBRAL_BEGIN + " real, "
            + COLUMN_PENUMBRAL_END + " real, " + COLUMN_ECLIPSE_DATE
            + " real, " + COLUMN_ECLIPSE_TYPE + " text);";

    public static void onCreate(SQLiteDatabase database) {
        String ip1, ip2;
        database.execSQL(DATABASE_CREATE);
        ip1 = "insert into " + TABLE_LUNAR_ECLIPSE + "(" + COLUMN_ID + ","
                + COLUMN_LOCAL_TYPE + "," + COLUMN_GLOBAL_TYPE + ","
                + COLUMN_LOCAL + "," + COLUMN_UMBRAL_MAG + ","
                + COLUMN_PENUMBRAL_MAG + "," + COLUMN_MOON_AZ + ","
                + COLUMN_MOON_ALT + "," + COLUMN_MOONRISE + ","
                + COLUMN_MOONSET + "," + COLUMN_SAROS_NUM + ","
                + COLUMN_SAROS_MEMBER_NUM + "," + COLUMN_MAX_ECLIPSE + ","
                + COLUMN_PARTIAL_BEGIN + "," + COLUMN_PARTIAL_END + ","
                + COLUMN_TOTAL_BEGIN + "," + COLUMN_TOTAL_END + ","
                + COLUMN_PENUMBRAL_BEGIN + "," + COLUMN_PENUMBRAL_END + ","
                + COLUMN_ECLIPSE_DATE + "," + COLUMN_ECLIPSE_TYPE
                + ") VALUES (";
        ip2 = ",0,0,0,0,0,0.0,0.0,0.0,0.0,0,0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,'T');";
        for (int i = 0; i < 10; i++) {
            database.execSQL(ip1 + i + ip2);
        }
    }

    public static void onUpgrade(SQLiteDatabase database, int oldVersion,
                                 int newVersion) {
        Log.w(LunarEclipseTable.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_LUNAR_ECLIPSE);
        onCreate(database);
    }
}

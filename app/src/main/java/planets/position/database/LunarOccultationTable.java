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

public class LunarOccultationTable {

    public static final String TABLE_NAME = "lunarOccult";
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

    private static final String DATABASE_CREATE = String.format("create table %s(%s integer primary key autoincrement, %s integer, %s integer,%s integer, %s real, %s real, %s real, %s real, %s real, %s real, %s real, %s real, %s real, %s real, %s real, %s real, %s real, %s real, %s real, %s real, %s real, %s real, %s real, %s integer);", TABLE_NAME, COLUMN_ID, COLUMN_LOCAL_TYPE, COLUMN_GLOBAL_TYPE, COLUMN_LOCAL, COLUMN_LOCAL_MAX, COLUMN_LOCAL_FIRST, COLUMN_LOCAL_SECOND, COLUMN_LOCAL_THIRD, COLUMN_LOCAL_FOURTH, COLUMN_MOONRISE, COLUMN_MOONSET, COLUMN_MOONS_AZ, COLUMN_MOONS_ALT, COLUMN_MOONE_AZ, COLUMN_MOONE_ALT, COLUMN_GLOBAL_MAX, COLUMN_GLOBAL_BEGIN, COLUMN_GLOBAL_END, COLUMN_GLOBAL_TOTAL_BEGIN, COLUMN_GLOBAL_TOTAL_END, COLUMN_GLOBAL_CENTER_BEGIN, COLUMN_GLOBAL_CENTER_END, COLUMN_OCCULT_DATE, COLUMN_OCCULT_PLANET);

    public static void onCreate(SQLiteDatabase database) {
        String ip1, ip2;
        database.execSQL(DATABASE_CREATE);
        ip1 = String.format("insert into %s(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) VALUES (", TABLE_NAME, COLUMN_ID, COLUMN_LOCAL_TYPE, COLUMN_GLOBAL_TYPE, COLUMN_LOCAL, COLUMN_LOCAL_MAX, COLUMN_LOCAL_FIRST, COLUMN_LOCAL_SECOND, COLUMN_LOCAL_THIRD, COLUMN_LOCAL_FOURTH, COLUMN_MOONRISE, COLUMN_MOONSET, COLUMN_MOONS_AZ, COLUMN_MOONS_ALT, COLUMN_MOONE_AZ, COLUMN_MOONE_ALT, COLUMN_GLOBAL_MAX, COLUMN_GLOBAL_BEGIN, COLUMN_GLOBAL_END, COLUMN_GLOBAL_TOTAL_BEGIN, COLUMN_GLOBAL_TOTAL_END, COLUMN_GLOBAL_CENTER_BEGIN, COLUMN_GLOBAL_CENTER_END, COLUMN_OCCULT_DATE, COLUMN_OCCULT_PLANET);
        ip2 = ",0,0,-1,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"
                + "0.0,0.0,0.0,0.0,-1);";
        for (int i = 0; i < 10; i++) {
            database.execSQL(ip1 + i + ip2);
        }
    }

    public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.w(LunarOccultationTable.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        database.execSQL(String.format("DROP TABLE IF EXISTS %s", TABLE_NAME));
        onCreate(database);
    }
}

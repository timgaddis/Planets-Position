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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import planets.position.PlanetsMain;

class PlanetsDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "PlanetsDatabase.db";
    private static final int DATABASE_VERSION = 213;
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
        mContext.getSharedPreferences(PlanetsMain.MAIN_PREFS, 0).edit().clear().apply();
        LocationTable.onUpgrade(oldVersion, newVersion);
        PlanetsTable.onUpgrade(database, oldVersion, newVersion);
        SolarEclipseTable.onUpgrade(database, oldVersion, newVersion);
        LunarEclipseTable.onUpgrade(database, oldVersion, newVersion);
        LunarOccultationTable.onUpgrade(database, oldVersion, newVersion);
    }

}

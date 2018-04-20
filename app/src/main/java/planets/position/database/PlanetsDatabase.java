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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

public class PlanetsDatabase {

    private SQLiteDatabase database;
    private PlanetsDatabaseHelper dbHelper;

    private final String[] locationColumns = {LocationTable.COLUMN_ID,
            LocationTable.COLUMN_LATITUDE, LocationTable.COLUMN_LONGITUDE,
            LocationTable.COLUMN_ELEVATION, LocationTable.COLUMN_OFFSET,
            LocationTable.COLUMN_ZONE_ID, LocationTable.COLUMN_ZONE_NAME};
    private final String[] whatsUpColumns = {PlanetsTable.COLUMN_NUMBER,
            PlanetsTable.COLUMN_NAME, PlanetsTable.COLUMN_ALT,
            PlanetsTable.COLUMN_RISE_TIME, PlanetsTable.COLUMN_SET_TIME, PlanetsTable.COLUMN_ID};
    private final String[] planetDataColumns = {PlanetsTable.COLUMN_ID,
            PlanetsTable.COLUMN_NAME, PlanetsTable.COLUMN_RA,
            PlanetsTable.COLUMN_DEC, PlanetsTable.COLUMN_AZ,
            PlanetsTable.COLUMN_ALT, PlanetsTable.COLUMN_DISTANCE,
            PlanetsTable.COLUMN_MAGNITUDE, PlanetsTable.COLUMN_SET_TIME,
            PlanetsTable.COLUMN_RISE_TIME, PlanetsTable.COLUMN_TRANSIT};
    private final String[] solarEclipseColumns = {SolarEclipseTable.COLUMN_ID,
            SolarEclipseTable.COLUMN_GLOBAL_TYPE,
            SolarEclipseTable.COLUMN_ECLIPSE_DATE,
            SolarEclipseTable.COLUMN_ECLIPSE_TYPE,
            SolarEclipseTable.COLUMN_LOCAL};
    private final String[] solarDataColumns = {SolarEclipseTable.COLUMN_ID,
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
    private final String[] lunarEclipseColumns = {LunarEclipseTable.COLUMN_ID,
            LunarEclipseTable.COLUMN_GLOBAL_TYPE,
            LunarEclipseTable.COLUMN_ECLIPSE_DATE,
            LunarEclipseTable.COLUMN_ECLIPSE_TYPE,
            LunarEclipseTable.COLUMN_LOCAL};
    private final String[] lunarDataColumns = {LunarEclipseTable.COLUMN_ID,
            LunarEclipseTable.COLUMN_LOCAL,
            LunarEclipseTable.COLUMN_MAX_ECLIPSE,
            LunarEclipseTable.COLUMN_PENUMBRAL_BEGIN,
            LunarEclipseTable.COLUMN_PENUMBRAL_END,
            LunarEclipseTable.COLUMN_TOTAL_BEGIN,
            LunarEclipseTable.COLUMN_TOTAL_END,
            LunarEclipseTable.COLUMN_PARTIAL_BEGIN,
            LunarEclipseTable.COLUMN_PARTIAL_END,
            LunarEclipseTable.COLUMN_MOONRISE,
            LunarEclipseTable.COLUMN_MOONSET,
            LunarEclipseTable.COLUMN_MOON_AZ,
            LunarEclipseTable.COLUMN_MOON_ALT,
            LunarEclipseTable.COLUMN_UMBRAL_MAG,
            LunarEclipseTable.COLUMN_PENUMBRAL_MAG,
            LunarEclipseTable.COLUMN_SAROS_NUM,
            LunarEclipseTable.COLUMN_SAROS_MEMBER_NUM,
            LunarEclipseTable.COLUMN_ECLIPSE_DATE,
            LunarEclipseTable.COLUMN_ECLIPSE_TYPE};
    private final String[] lunarOccultColumns = {LunarOccultationTable.COLUMN_ID,
            LunarOccultationTable.COLUMN_OCCULT_DATE,
            LunarOccultationTable.COLUMN_OCCULT_PLANET,
            LunarOccultationTable.COLUMN_LOCAL};
    private final String[] occultDataColumns = {LunarOccultationTable.COLUMN_ID,
            LunarOccultationTable.COLUMN_LOCAL,
            LunarOccultationTable.COLUMN_LOCAL_MAX,
            LunarOccultationTable.COLUMN_LOCAL_FIRST,
            LunarOccultationTable.COLUMN_LOCAL_FOURTH,
            LunarOccultationTable.COLUMN_MOONRISE,
            LunarOccultationTable.COLUMN_MOONSET,
            LunarOccultationTable.COLUMN_MOONS_AZ,
            LunarOccultationTable.COLUMN_MOONS_ALT,
            LunarOccultationTable.COLUMN_MOONE_AZ,
            LunarOccultationTable.COLUMN_MOONE_ALT,
            LunarOccultationTable.COLUMN_GLOBAL_MAX,
            LunarOccultationTable.COLUMN_GLOBAL_BEGIN,
            LunarOccultationTable.COLUMN_GLOBAL_END,
            LunarOccultationTable.COLUMN_OCCULT_DATE,
            LunarOccultationTable.COLUMN_OCCULT_PLANET};

    public PlanetsDatabase(Context context) {
        dbHelper = PlanetsDatabaseHelper.getInstance(context);
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        database.close();
    }

    public long addLocation(ContentValues values) {
        return database.update(LocationTable.TABLE_NAME, values,
                LocationTable.COLUMN_ID + " = ?", new String[]{String.valueOf(0)});
    }

    public Bundle getLocation() {
        Bundle out = new Bundle();
        Cursor c = database.query(LocationTable.TABLE_NAME, locationColumns,
                null, null, null, null, null);
        c.moveToFirst();
        if (c.getCount() > 0) {
            out.putDouble("latitude", c.getDouble(c.getColumnIndex(LocationTable.COLUMN_LATITUDE)));
            out.putDouble("longitude", c.getDouble(c.getColumnIndex(LocationTable.COLUMN_LONGITUDE)));
            out.putDouble("elevation", c.getDouble(c.getColumnIndex(LocationTable.COLUMN_ELEVATION)));
            out.putDouble("offset", c.getDouble(c.getColumnIndex(LocationTable.COLUMN_OFFSET)));
            out.putInt("zoneID", c.getInt(c.getColumnIndex(LocationTable.COLUMN_ZONE_ID)));
            out.putString("zoneName", c.getString(c.getColumnIndex(LocationTable.COLUMN_ZONE_NAME)));
        }
        c.close();

        return out;
    }

    public Cursor getPlanetsSet() {
        return database.query(PlanetsTable.TABLE_NAME, whatsUpColumns, "alt > 0.0",
                null, null, null, null);
    }

    public Cursor getPlanetsRise() {
        return database.query(PlanetsTable.TABLE_NAME, whatsUpColumns, "alt <= 0.0",
                null, null, null, null);
    }

    public Cursor getPlanetsAll() {
        return database.query(PlanetsTable.TABLE_NAME, whatsUpColumns, null,
                null, null, null, null);
    }

    public Bundle getPlanet(long planet) {

        Bundle out = new Bundle();
        Cursor c = database.query(PlanetsTable.TABLE_NAME, planetDataColumns,
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
        out.putDouble("setTime", c.getDouble(c.getColumnIndex(PlanetsTable.COLUMN_SET_TIME)));
        out.putDouble("riseTime", c.getDouble(c.getColumnIndex(PlanetsTable.COLUMN_RISE_TIME)));
        out.putDouble("transit", c.getDouble(c.getColumnIndex(PlanetsTable.COLUMN_TRANSIT)));

        c.close();

        return out;
    }

    public void addPlanet(ContentValues values, int row) {
        database.update(PlanetsTable.TABLE_NAME, values, PlanetsTable.COLUMN_ID + " = ?",
                new String[]{String.valueOf(row)});
    }

    public Cursor getSolarEclipseList() {
        return database.query(SolarEclipseTable.TABLE_NAME, solarEclipseColumns, null,
                null, null, null, SolarEclipseTable.COLUMN_GLOBAL_BEGIN);
    }

    public Bundle getSolarEclipse(long solar) {

        Bundle out = new Bundle();
        Cursor c = database.query(SolarEclipseTable.TABLE_NAME, solarDataColumns,
                SolarEclipseTable.COLUMN_ID + " = ?", new String[]{String.valueOf(solar)},
                null, null, null);
        c.moveToFirst();

        out.putDouble(SolarEclipseTable.COLUMN_ECLIPSE_DATE, c.getDouble(c.getColumnIndex(SolarEclipseTable.COLUMN_ECLIPSE_DATE)));
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

    public void addSolarEclipse(ContentValues values, int row) {
        database.update(SolarEclipseTable.TABLE_NAME, values, SolarEclipseTable.COLUMN_ID + " = ?",
                new String[]{String.valueOf(row)});
    }

    public Cursor getLunarEclipseList() {
        return database.query(LunarEclipseTable.TABLE_NAME, lunarEclipseColumns, null,
                null, null, null, LunarEclipseTable.COLUMN_MAX_ECLIPSE);
    }

    public void addLunarEclipse(ContentValues values, int row) {
        database.update(LunarEclipseTable.TABLE_NAME, values, LunarEclipseTable.COLUMN_ID + " = ?",
                new String[]{String.valueOf(row)});
    }

    public Bundle getLunarEclipse(long lunar) {

        Bundle out = new Bundle();
        Cursor c = database.query(LunarEclipseTable.TABLE_NAME, lunarDataColumns,
                LunarEclipseTable.COLUMN_ID + " = ?", new String[]{String.valueOf(lunar)},
                null, null, null);
        c.moveToFirst();

        out.putDouble(LunarEclipseTable.COLUMN_ECLIPSE_DATE, c.getDouble(c.getColumnIndex(LunarEclipseTable.COLUMN_ECLIPSE_DATE)));
        out.putString(LunarEclipseTable.COLUMN_ECLIPSE_TYPE, c.getString(c.getColumnIndex(LunarEclipseTable.COLUMN_ECLIPSE_TYPE)));
        out.putInt(LunarEclipseTable.COLUMN_LOCAL, c.getInt(c.getColumnIndex(LunarEclipseTable.COLUMN_LOCAL)));
        out.putDouble(LunarEclipseTable.COLUMN_MAX_ECLIPSE, c.getDouble(c.getColumnIndex(LunarEclipseTable.COLUMN_MAX_ECLIPSE)));
        out.putDouble(LunarEclipseTable.COLUMN_PENUMBRAL_BEGIN, c.getDouble(c.getColumnIndex(LunarEclipseTable.COLUMN_PENUMBRAL_BEGIN)));
        out.putDouble(LunarEclipseTable.COLUMN_PENUMBRAL_END, c.getDouble(c.getColumnIndex(LunarEclipseTable.COLUMN_PENUMBRAL_END)));
        out.putDouble(LunarEclipseTable.COLUMN_PARTIAL_BEGIN, c.getDouble(c.getColumnIndex(LunarEclipseTable.COLUMN_PARTIAL_BEGIN)));
        out.putDouble(LunarEclipseTable.COLUMN_PARTIAL_END, c.getDouble(c.getColumnIndex(LunarEclipseTable.COLUMN_PARTIAL_END)));
        out.putDouble(LunarEclipseTable.COLUMN_TOTAL_BEGIN, c.getDouble(c.getColumnIndex(LunarEclipseTable.COLUMN_TOTAL_BEGIN)));
        out.putDouble(LunarEclipseTable.COLUMN_TOTAL_END, c.getDouble(c.getColumnIndex(LunarEclipseTable.COLUMN_TOTAL_END)));
        out.putDouble(LunarEclipseTable.COLUMN_MOONRISE, c.getDouble(c.getColumnIndex(LunarEclipseTable.COLUMN_MOONRISE)));
        out.putDouble(LunarEclipseTable.COLUMN_MOONSET, c.getDouble(c.getColumnIndex(LunarEclipseTable.COLUMN_MOONSET)));
        out.putDouble(LunarEclipseTable.COLUMN_MOON_AZ, c.getDouble(c.getColumnIndex(LunarEclipseTable.COLUMN_MOON_AZ)));
        out.putDouble(LunarEclipseTable.COLUMN_MOON_ALT, c.getDouble(c.getColumnIndex(LunarEclipseTable.COLUMN_MOON_ALT)));
        out.putDouble(LunarEclipseTable.COLUMN_PENUMBRAL_MAG, c.getDouble(c.getColumnIndex(LunarEclipseTable.COLUMN_PENUMBRAL_MAG)));
        out.putDouble(LunarEclipseTable.COLUMN_UMBRAL_MAG, c.getDouble(c.getColumnIndex(LunarEclipseTable.COLUMN_UMBRAL_MAG)));
        out.putInt(LunarEclipseTable.COLUMN_SAROS_NUM, c.getInt(c.getColumnIndex(LunarEclipseTable.COLUMN_SAROS_NUM)));
        out.putInt(LunarEclipseTable.COLUMN_SAROS_MEMBER_NUM, c.getInt(c.getColumnIndex(LunarEclipseTable.COLUMN_SAROS_MEMBER_NUM)));

        c.close();
        return out;
    }

    public Cursor getLunarOccultList() {
        return database.query(LunarOccultationTable.TABLE_NAME, lunarOccultColumns,
                LunarOccultationTable.COLUMN_OCCULT_PLANET + " > ?",
                new String[]{String.valueOf(-1)}, null, null,
                LunarOccultationTable.COLUMN_OCCULT_PLANET + "," + LunarOccultationTable.COLUMN_GLOBAL_MAX);
    }

    public void addLunarOccult(ContentValues values, int row) {
        database.update(LunarOccultationTable.TABLE_NAME, values, LunarOccultationTable.COLUMN_ID + " = ?",
                new String[]{String.valueOf(row)});
    }

    public Bundle getLunarOccult(long occult) {

        Bundle out = new Bundle();
        Cursor c = database.query(LunarOccultationTable.TABLE_NAME, occultDataColumns,
                LunarOccultationTable.COLUMN_ID + " = ?", new String[]{String.valueOf(occult)},
                null, null, null);
        c.moveToFirst();

        out.putDouble(LunarOccultationTable.COLUMN_OCCULT_DATE, c.getDouble(c.getColumnIndex(LunarOccultationTable.COLUMN_OCCULT_DATE)));
        out.putInt(LunarOccultationTable.COLUMN_OCCULT_PLANET, c.getInt(c.getColumnIndex(LunarOccultationTable.COLUMN_OCCULT_PLANET)));
        out.putInt(LunarOccultationTable.COLUMN_LOCAL, c.getInt(c.getColumnIndex(LunarOccultationTable.COLUMN_LOCAL)));
        out.putDouble(LunarOccultationTable.COLUMN_MOONRISE, c.getDouble(c.getColumnIndex(LunarOccultationTable.COLUMN_MOONRISE)));
        out.putDouble(LunarOccultationTable.COLUMN_MOONSET, c.getDouble(c.getColumnIndex(LunarOccultationTable.COLUMN_MOONSET)));
        out.putDouble(LunarOccultationTable.COLUMN_LOCAL_FIRST, c.getDouble(c.getColumnIndex(LunarOccultationTable.COLUMN_LOCAL_FIRST)));
        out.putDouble(LunarOccultationTable.COLUMN_LOCAL_MAX, c.getDouble(c.getColumnIndex(LunarOccultationTable.COLUMN_LOCAL_MAX)));
        out.putDouble(LunarOccultationTable.COLUMN_LOCAL_FOURTH, c.getDouble(c.getColumnIndex(LunarOccultationTable.COLUMN_LOCAL_FOURTH)));
        out.putDouble(LunarOccultationTable.COLUMN_MOONS_AZ, c.getDouble(c.getColumnIndex(LunarOccultationTable.COLUMN_MOONS_AZ)));
        out.putDouble(LunarOccultationTable.COLUMN_MOONS_ALT, c.getDouble(c.getColumnIndex(LunarOccultationTable.COLUMN_MOONS_ALT)));
        out.putDouble(LunarOccultationTable.COLUMN_MOONE_AZ, c.getDouble(c.getColumnIndex(LunarOccultationTable.COLUMN_MOONE_AZ)));
        out.putDouble(LunarOccultationTable.COLUMN_MOONE_ALT, c.getDouble(c.getColumnIndex(LunarOccultationTable.COLUMN_MOONE_ALT)));
        out.putDouble(LunarOccultationTable.COLUMN_GLOBAL_BEGIN, c.getDouble(c.getColumnIndex(LunarOccultationTable.COLUMN_GLOBAL_BEGIN)));
        out.putDouble(LunarOccultationTable.COLUMN_GLOBAL_MAX, c.getDouble(c.getColumnIndex(LunarOccultationTable.COLUMN_GLOBAL_MAX)));
        out.putDouble(LunarOccultationTable.COLUMN_GLOBAL_END, c.getDouble(c.getColumnIndex(LunarOccultationTable.COLUMN_GLOBAL_END)));

        c.close();
        return out;
    }

}

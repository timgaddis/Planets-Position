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

package planets.position.lunar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import planets.position.FragmentListener;
import planets.position.R;
import planets.position.database.LunarEclipseTable;
import planets.position.database.PlanetsDatabase;
import planets.position.database.TimeZoneDB;
import planets.position.util.JDUTC;
import planets.position.util.PlanetDatePicker;

public class LunarEclipse extends Fragment {

    public static final int TASK_FRAGMENT = 100;
    private static final int DATE_FRAGMENT = 50;
    private static final String TASK_FRAGMENT_TAG = "lunarEclipseTask";

    private FragmentListener mCallbacks;
    private FragmentManager mFM;
    private LunarEclipseTask taskFragment;
    private LunarEclipseData eclipseData;
    private TimeZoneDB tzDB;
    private ListView lunarList;
    private int zoneID;
    private double offset, firstDate, lastDate;
    private final double[] g = new double[3];
    private double[] time;
    private boolean firstRun, newLoc;
    private DateFormat mDateFormat;
    private PlanetsDatabase planetsDB;
    private SharedPreferences settings;
    private JDUTC jdUTC;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_eclipse, container, false);
        lunarList = v.findViewById(R.id.eclipseList);

        lunarList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                // display eclipse data
                FragmentTransaction ft = null;
                if (getFragmentManager() != null) {
                    ft = getFragmentManager().beginTransaction();
                }
                eclipseData = new LunarEclipseData();
                Bundle args = new Bundle();
                args.putLong("lunarNum", id);
                args.putInt("zoneID", zoneID);
                eclipseData.setArguments(args);
                if (ft != null) {
                    ft.replace(R.id.content_frame, eclipseData, "eclipseData");
                    ft.addToBackStack(null);
                    ft.commit();
                }
            }
        });

        if (mCallbacks != null) {
            mCallbacks.onToolbarTitleChange("Lunar Eclipse", 3);
        }

        planetsDB = new PlanetsDatabase(getActivity().getApplicationContext());

        if (savedInstanceState == null) {
            loadLocation();
        } else {
            offset = savedInstanceState.getDouble("offset", 0);
            zoneID = savedInstanceState.getInt("zoneID", 0);
            newLoc = savedInstanceState.getBoolean("newLoc");
        }

        time = jdUTC.getCurrentTime(offset);

        mFM = getFragmentManager();
        if (mFM != null) {
            taskFragment = (LunarEclipseTask) mFM.findFragmentByTag(TASK_FRAGMENT_TAG);
        }

        if (taskFragment != null) {
            taskFragment.setTargetFragment(this, TASK_FRAGMENT);
        } else {
            if (firstRun) {
                lunarList.setVisibility(View.INVISIBLE);
                launchTask(time[1], 0.0);
            } else {
                loadEclipses();
            }
        }

        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        jdUTC = new JDUTC();
        tzDB = new TimeZoneDB(getActivity().getApplicationContext());
        mDateFormat = android.text.format.DateFormat
                .getDateFormat(getActivity().getApplicationContext());
        settings = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        firstRun = settings.getBoolean("leFirstRun", true);
        firstDate = settings.getFloat("leFirstDate", 0);
        lastDate = settings.getFloat("leLastDate", 0);

        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putDouble("offset", offset);
        outState.putInt("zoneID", zoneID);
        outState.putBoolean("newLoc", newLoc);
    }

    @Override
    public void onResume() {
        loadLocation();
        if (newLoc && taskFragment == null) {
            // if new location then compute eclipses
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("newLocation", false);
            editor.apply();
            newLoc = false;
            lunarList.setVisibility(View.INVISIBLE);
            launchTask(firstDate, 0.0);
        }
        super.onResume();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getTargetFragment() == null) {
            // attach to PlanetsMain
            if (!(context instanceof FragmentListener)) {
                throw new IllegalStateException(
                        "Activity must implement the FragmentListener interface.");
            }
            mCallbacks = (FragmentListener) context;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DATE_FRAGMENT) {
            // Set Date
            Calendar c = Calendar.getInstance();
            c.clear();
            c.set(data.getIntExtra("year", 0),
                    data.getIntExtra("month", 0),
                    data.getIntExtra("day", 0));
            // convert local time to utc
            tzDB.open();
            int off = tzDB.getZoneOffset(zoneID, c.getTimeInMillis() / 1000L);
            offset = off / 60.0;
            tzDB.close();
            c.add(Calendar.MINUTE, (int) (offset * -1));
            time = jdUTC.utcjd(c.get(Calendar.MONTH) + 1,
                    c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.YEAR),
                    c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE),
                    c.get(Calendar.SECOND));
            lunarList.setVisibility(View.INVISIBLE);
            launchTask(time[1], 0.0);
        } else if (requestCode == TASK_FRAGMENT) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    SharedPreferences.Editor editor;
                    taskFragment = null;
                    firstDate = data.getDoubleExtra("first", 0);
                    lastDate = data.getDoubleExtra("last", 0);
                    editor = settings.edit();
                    editor.putBoolean("leFirstRun", false);
                    editor.putBoolean("newLocation", false);
                    editor.putFloat("leFirstDate", (float) firstDate);
                    editor.putFloat("leLastDate", (float) lastDate);
                    editor.apply();
                    firstRun = false;
                    loadEclipses();
                    break;
                case Activity.RESULT_CANCELED:
                    Log.e(TASK_FRAGMENT_TAG, "Lunar eclipse task canceled.");
                    break;
                case 100:// Local Eclipse calc error
                    Log.e(TASK_FRAGMENT_TAG, "Local Lunar Eclipse calc error 100.");
                    try {
                        throw new Exception();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case 200:// Global Eclipse calc error
                    Log.e(TASK_FRAGMENT_TAG, "Global Lunar Eclipse calc error 200.");
                    try {
                        throw new Exception();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case 300:// Local Eclipse calc error
                    Log.e(TASK_FRAGMENT_TAG, "Local Lunar Eclipse calc error 300.");
                    try {
                        throw new Exception();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.eclipse_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        PlanetDatePicker datePickerFragment;
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_previous:
                lunarList.setVisibility(View.INVISIBLE);
                launchTask(firstDate, 1.0);
                return true;
            case R.id.action_next:
                lunarList.setVisibility(View.INVISIBLE);
                launchTask(lastDate, 0.0);
                return true;
            case R.id.action_calendar:
                datePickerFragment = new PlanetDatePicker();
                datePickerFragment.setTargetFragment(this, DATE_FRAGMENT);
                datePickerFragment.setStyle(DialogFragment.STYLE_NORMAL, 0);
                if (getFragmentManager() != null) {
                    datePickerFragment.show(getFragmentManager(), "datePickerDialog");
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void loadLocation() {
        // read location from Shared Preferences
        g[1] = settings.getFloat("latitude", 0);
        g[0] = settings.getFloat("longitude", 0);
        g[2] = settings.getFloat("elevation", 0);
        offset = settings.getFloat("offset", 0);
        zoneID = settings.getInt("zoneID", 0);
        newLoc = settings.getBoolean("newLocation", true);
    }

    private void launchTask(double time, double back) {
        taskFragment = (LunarEclipseTask) mFM.findFragmentByTag(TASK_FRAGMENT_TAG);
        if (taskFragment == null) {
            taskFragment = new LunarEclipseTask();
            taskFragment.setData(taskFragment.new ComputeEclipseTask(), g, time,
                    back);
            taskFragment.setTargetFragment(this, TASK_FRAGMENT);
            taskFragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
            taskFragment.show(mFM, TASK_FRAGMENT_TAG);
        }
    }

    private void loadEclipses() {
        SimpleCursorAdapter cursorAdapter;
        planetsDB.open();
        Cursor leCursor = planetsDB.getLunarEclipseList();
        String[] from = new String[]{LunarEclipseTable.COLUMN_GLOBAL_TYPE,
                LunarEclipseTable.COLUMN_ECLIPSE_DATE,
                LunarEclipseTable.COLUMN_ECLIPSE_TYPE,
                LunarEclipseTable.COLUMN_LOCAL};
        int[] to = new int[]{R.id.rowEclImage, R.id.rowEclDate,
                R.id.rowEclType, R.id.rowEclLocal};
        cursorAdapter = new SimpleCursorAdapter(getActivity()
                .getApplicationContext(), R.layout.eclipse_list_row, leCursor,
                from, to, 0);
        // customize the fields
        cursorAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int column) {
                switch (column) {
                    case 1: {// type image
                        ImageView iv = (ImageView) view;
                        int val = cursor.getInt(cursor
                                .getColumnIndex(LunarEclipseTable.COLUMN_GLOBAL_TYPE));
                        if ((val & 4) == 4) // LE_ECL_TOTAL
                            iv.setImageResource(R.drawable.ic_lunar_total);
                        else if ((val & 64) == 64) // LE_ECL_PENUMBRAL
                            iv.setImageResource(R.drawable.ic_lunar_penumbral);
                        else if ((val & 16) == 16) // LE_ECL_PARTIAL
                            iv.setImageResource(R.drawable.ic_lunar_partial);
                        else
                            iv.setImageResource(R.drawable.ic_planet_moon);
                        return true;
                    }
                    case 2: {// date
                        TextView tv = (TextView) view;
                        double date = cursor.getDouble(cursor
                                .getColumnIndex(LunarEclipseTable.COLUMN_ECLIPSE_DATE));
                        Calendar gc = new GregorianCalendar();
                        gc.setTimeInMillis(jdUTC.jdmills(date));
                        tv.setText(mDateFormat.format(gc.getTime()));
                        return true;
                    }
                    case 3: {// type
                        TextView tv = (TextView) view;
                        String type = cursor.getString(cursor
                                .getColumnIndex(LunarEclipseTable.COLUMN_ECLIPSE_TYPE));
                        tv.setText(type);
                        return true;
                    }
                    case 4: {// local
                        ImageView iv = (ImageView) view;
                        int l = cursor.getInt(cursor
                                .getColumnIndex(LunarEclipseTable.COLUMN_LOCAL));
                        if (l > 0)
                            iv.setVisibility(View.VISIBLE);
                        else
                            iv.setVisibility(View.INVISIBLE);
                        return true;
                    }
                }
                return false;
            }
        });
        lunarList.setAdapter(cursorAdapter);
        lunarList.setVisibility(View.VISIBLE);
        planetsDB.close();
    }
}

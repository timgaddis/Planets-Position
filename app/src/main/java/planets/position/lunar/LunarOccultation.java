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
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import planets.position.FragmentListener;
import planets.position.PlanetsMain;
import planets.position.R;
import planets.position.database.LunarOccultationTable;
import planets.position.database.PlanetsDatabase;
import planets.position.database.TimeZoneDB;
import planets.position.util.JDUTC;
import planets.position.util.PlanetDatePicker;

public class LunarOccultation extends Fragment {

    public static final int TASK_FRAGMENT = 100;
    private static final int DATE_FRAGMENT = 50;
    private static final int PLANETS_DIALOG = 200;
    private static final String TASK_FRAGMENT_TAG = "lunarOccultTask";

    private FragmentListener mCallbacks;
    private FragmentManager mFM;
    private MenuItem next, previous;
    private ListView occultList;
    private List<String> planetArray;
    private LunarOccultTask taskFragment;
    private LunarOccultData occultData;
    private TimeZoneDB tzDB;
    private double offset, firstDate, lastDate;
    private final double[] g = new double[3];
    private double[] time;
    private boolean allPlanets, newLoc;
    private int planetNum = 1, zoneID, spinnerPos = -1;
    private PlanetsDatabase planetsDB;
    private SharedPreferences settings;
    private DateFormat mDateFormat;
    private JDUTC jdUTC;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_lunar_occult, container,
                false);

        Spinner planetsSpinner = v.findViewById(R.id.planetsSpinner);
        occultList = v.findViewById(R.id.occultList);
        planetArray = Arrays.asList(getResources().getStringArray(
                R.array.occult_array));

        occultList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                // display occultation data
                FragmentTransaction ft = null;
                if (getFragmentManager() != null) {
                    ft = getFragmentManager().beginTransaction();
                }
                occultData = new LunarOccultData();
                Bundle args = new Bundle();
                args.putLong("occultNum", id);
                args.putInt("zoneID", zoneID);
                occultData.setArguments(args);
                if (ft != null) {
                    ft.replace(R.id.content_frame, occultData, "occultData");
                    ft.addToBackStack(null);
                    ft.commit();
                }
            }
        });

        if (mCallbacks != null) {
            mCallbacks.onToolbarTitleChange("Lunar Occultation", 4);
        }

        planetsDB = new PlanetsDatabase(getActivity().getApplicationContext());

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.occult_array, R.layout.spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_drop_item);
        planetsSpinner.setAdapter(adapter);
        planetsSpinner.setSelection(planetNum - 1);
        planetsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    next.setVisible(false);
                    previous.setVisible(false);
                } else {
                    next.setVisible(true);
                    previous.setVisible(true);
                }
                if (spinnerPos != position) {
                    spinnerPos = position;
                    planetNum = position + 1;
                    Calendar c = Calendar.getInstance();
                    tzDB.open();
                    int off = tzDB.getZoneOffset(zoneID, c.getTimeInMillis() / 1000L);
                    offset = off / 60.0;
                    tzDB.close();
                    time = jdUTC.getCurrentTime(offset);
                    occultList.setVisibility(View.INVISIBLE);
                    launchTask(time[1], 0.0, planetNum);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        if (savedInstanceState == null) {
            loadLocation();
        } else {
            offset = savedInstanceState.getDouble("offset", 0);
            zoneID = savedInstanceState.getInt("zoneID", 0);
            newLoc = savedInstanceState.getBoolean("newLoc");
            spinnerPos = savedInstanceState.getInt("spinnerPos", -1);
        }

        time = jdUTC.getCurrentTime(offset);

        mFM = getFragmentManager();
        if (mFM != null) {
            taskFragment = (LunarOccultTask) mFM.findFragmentByTag(TASK_FRAGMENT_TAG);
        }
        if (taskFragment != null) {
            taskFragment.setTargetFragment(this, TASK_FRAGMENT);
        } else {
            loadOccults();
        }

        if (firstDate == 0)
            firstDate = time[1];

        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        jdUTC = new JDUTC();
        // Get date and time formats from system
        mDateFormat = android.text.format.DateFormat
                .getDateFormat(getActivity().getApplicationContext());
        tzDB = new TimeZoneDB(getActivity().getApplicationContext());
        // Restore preferences
        settings = getActivity().getSharedPreferences(PlanetsMain.MAIN_PREFS, 0);
        firstDate = settings.getFloat("loFirstDate", 0);
        lastDate = settings.getFloat("loLastDate", 0);
        allPlanets = settings.getBoolean("loAllPlanets", true);
        planetNum = settings.getInt("loPlanetNum", 1);
        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putDouble("offset", offset);
        outState.putInt("zoneID", zoneID);
        outState.putBoolean("newLoc", newLoc);
        outState.putInt("spinnerPos", spinnerPos);
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
    public void onResume() {
        loadLocation();
        if (newLoc) {
            // if new location then compute eclipses
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("newLocation", false);
            editor.apply();
            newLoc = false;
            occultList.setVisibility(View.INVISIBLE);
            launchTask(firstDate, 0.0, planetNum);
        }
        super.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Calendar c;
        int off;
        switch (requestCode) {
            case PLANETS_DIALOG:
                planetNum = resultCode + 1;
                if (resultCode == 0) {
                    next.setVisible(false);
                    previous.setVisible(false);
                } else {
                    next.setVisible(true);
                    previous.setVisible(true);
                }
                c = Calendar.getInstance();
                tzDB.open();
                off = tzDB.getZoneOffset(zoneID, c.getTimeInMillis() / 1000L);
                offset = off / 60.0;
                tzDB.close();
                time = jdUTC.getCurrentTime(offset);
                occultList.setVisibility(View.INVISIBLE);
                launchTask(time[1], 0.0, planetNum);
                break;
            case DATE_FRAGMENT:
                // Set Date
                c = Calendar.getInstance();
                c.clear();
                c.set(data.getIntExtra("year", 0),
                        data.getIntExtra("month", 0),
                        data.getIntExtra("day", 0));
                // convert local time to utc
                tzDB.open();
                off = tzDB.getZoneOffset(zoneID, c.getTimeInMillis() / 1000L);
                offset = off / 60.0;
                tzDB.close();
                c.add(Calendar.MINUTE, (int) (offset * -1));
                time = jdUTC.utcjd(c.get(Calendar.MONTH) + 1,
                        c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.YEAR),
                        c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE),
                        c.get(Calendar.SECOND));
                occultList.setVisibility(View.INVISIBLE);
                launchTask(time[1], 0.0, planetNum);
                break;
            case TASK_FRAGMENT:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        SharedPreferences.Editor editor;
                        taskFragment = null;
                        firstDate = data.getDoubleExtra("first", 0);
                        lastDate = data.getDoubleExtra("last", 0);
                        allPlanets = data.getBooleanExtra("allPlanets", true);
                        editor = settings.edit();
                        editor.putBoolean("loFirstRun", false);
                        editor.putFloat("loFirstDate", (float) firstDate);
                        editor.putFloat("loLastDate", (float) lastDate);
                        editor.putBoolean("loAllPlanets", allPlanets);
                        editor.putInt("loPlanetNum", planetNum);
                        editor.apply();
                        loadOccults();
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.e(TASK_FRAGMENT_TAG, "Lunar occultation task canceled.");
                        break;
                    case 100:// Local Occultation calc error
                        Log.e(TASK_FRAGMENT_TAG,
                                "Local Lunar Occultation calc error 100.");
                        try {
                            throw new Exception();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case 200:// Global Occultation calc error
                        Log.e(TASK_FRAGMENT_TAG,
                                "Global Lunar Occultation calc error 200.");
                        try {
                            throw new Exception();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case 300:// Local Occultation calc error
                        Log.e(TASK_FRAGMENT_TAG,
                                "Local Lunar Occultation calc error 300.");
                        try {
                            throw new Exception();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case 400:// Local Occultation calc error
                        Log.e(TASK_FRAGMENT_TAG,
                                "Local Lunar Occultation calc error 400.");
                        try {
                            throw new Exception();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case 500:// Global Occultation calc error
                        Log.e(TASK_FRAGMENT_TAG,
                                "Global Lunar Occultation calc error 500.");
                        try {
                            throw new Exception();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                }
                break;
            default:
                // Cancel button
                break;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.eclipse_menu, menu);
        next = menu.findItem(R.id.action_next);
        previous = menu.findItem(R.id.action_previous);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (allPlanets) {
            next.setVisible(false);
            previous.setVisible(false);
        } else {
            next.setVisible(true);
            previous.setVisible(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_previous:
                occultList.setVisibility(View.INVISIBLE);
                launchTask(firstDate, 1.0, planetNum);
                return true;
            case R.id.action_next:
                occultList.setVisibility(View.INVISIBLE);
                launchTask(lastDate, 0.0, planetNum);
                return true;
            case R.id.action_calendar:
                PlanetDatePicker datePickerFragment = new PlanetDatePicker();
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
        newLoc = true;
        // Read location from database
        planetsDB.open();
        Bundle loc = planetsDB.getLocation();
        planetsDB.close();
        g[1] = loc.getDouble("latitude");
        g[0] = loc.getDouble("longitude");
        g[2] = loc.getDouble("elevation");
        offset = loc.getDouble("offset");
        zoneID = loc.getInt("zoneID");
    }

    private void launchTask(double time, double back, int planet) {
        taskFragment = (LunarOccultTask) mFM.findFragmentByTag(TASK_FRAGMENT_TAG);
        if (taskFragment == null) {
            taskFragment = new LunarOccultTask();
            taskFragment.setData(taskFragment.new ComputeOccultTask(), g, time, back, planet);
            taskFragment.setTargetFragment(this, TASK_FRAGMENT);
            taskFragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
            taskFragment.show(mFM, TASK_FRAGMENT_TAG);
        }
    }

    private void loadOccults() {
        SimpleCursorAdapter cursorAdapter;
        planetsDB.open();
        Cursor loCursor = planetsDB.getLunarOccultList();
        String[] from = new String[]{
                LunarOccultationTable.COLUMN_OCCULT_DATE,
                LunarOccultationTable.COLUMN_OCCULT_PLANET,
                LunarOccultationTable.COLUMN_LOCAL};
        int[] to = new int[]{R.id.rowOccDate,
                R.id.rowOccPlanet, R.id.rowOccLocal};
        cursorAdapter = new SimpleCursorAdapter(getActivity().getApplicationContext(),
                R.layout.occult_list_row, loCursor, from, to, 0);
        // customize the fields
        cursorAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int column) {
                switch (column) {
                    case 1: {// date
                        TextView tv = (TextView) view;
                        double date = cursor.getDouble(cursor
                                .getColumnIndex(LunarOccultationTable.COLUMN_OCCULT_DATE));
                        Calendar gc = new GregorianCalendar();
                        gc.setTimeInMillis(jdUTC.jdmills(date));
                        tv.setText(mDateFormat.format(gc.getTime()));
                        return true;
                    }
                    case 2: {// planet
                        TextView tv = (TextView) view;
                        int i = cursor.getInt(cursor
                                .getColumnIndex(LunarOccultationTable.COLUMN_OCCULT_PLANET));
                        tv.setText(planetArray.get(i - 1));
                        return true;
                    }
                    case 3: // local
                        ImageView iv = (ImageView) view;
                        int l = cursor.getInt(cursor
                                .getColumnIndex(LunarOccultationTable.COLUMN_LOCAL));
                        if (l > 0)
                            iv.setVisibility(View.VISIBLE);
                        else
                            iv.setVisibility(View.INVISIBLE);
                        return true;
                }
                return false;
            }
        });
        occultList.setAdapter(cursorAdapter);
        occultList.setVisibility(View.VISIBLE);
        planetsDB.close();
    }
}

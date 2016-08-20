/*
 * Planet's Position
 * A program to calculate the position of the planets in the night sky based
 * on a given location on Earth.
 * Copyright (c) 2016 Tim Gaddis
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import planets.position.FragmentListener;
import planets.position.PlanetDialog;
import planets.position.PlanetsMain;
import planets.position.R;
import planets.position.database.LunarOccultationTable;
import planets.position.database.PlanetsDatabase;
import planets.position.util.JDUTC;
import planets.position.util.PlanetDatePicker;

public class LunarOccultation extends Fragment {

    public static final int TASK_FRAGMENT = 100;
    private static final int DATE_FRAGMENT = 50;
    public static final int PLANETS_DIALOG = 200;
    private static final String TASK_FRAGMENT_TAG = "lunarOccultTask";

    private FragmentListener mCallbacks;
    private FragmentManager mFM;
    private MenuItem next, previous;
    private Button nameButton;
    private ListView occultList;
    private List<String> planetArray;
    private LunarOccultTask taskFragment;
    private LunarOccultData occultData;
    private PlanetDialog planetDialog;
    private double offset, firstDate, lastDate;
    private final double[] g = new double[3];
    private double[] time;
    private boolean firstRun, allPlanets, newLoc;
    private int planetNum = 1;
    private PlanetsDatabase planetsDB;
    private SharedPreferences settings;
    private DateFormat mDateFormat;
    private JDUTC jdUTC;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_lunar_occult, container,
                false);

        nameButton = (Button) v.findViewById(R.id.nameButton);
        occultList = (ListView) v.findViewById(R.id.occultList);
        planetArray = Arrays.asList(getResources().getStringArray(
                R.array.occult_array));

        nameButton.setText(planetArray.get(planetNum - 1));

        occultList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                // display occultation data
                FragmentTransaction ft = getFragmentManager()
                        .beginTransaction();
                occultData = new LunarOccultData();
                Bundle args = new Bundle();
                args.putLong("occultNum", id);
                args.putDouble("offset", offset);
                occultData.setArguments(args);
                ft.replace(R.id.content_frame, occultData, "occultData");
                ft.addToBackStack(null);
                ft.commit();
            }
        });

        if (mCallbacks != null) {
            mCallbacks.onToolbarTitleChange("Lunar Occultation", 4);
        }

        planetsDB = new PlanetsDatabase(getActivity().getApplicationContext());

        nameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                planetDialog = PlanetDialog.newInstance(R.array.occult_array, R.string.planet_select);
                planetDialog.setTargetFragment(LunarOccultation.this, PLANETS_DIALOG);
                planetDialog.show(getActivity().getSupportFragmentManager(), "loPlanetDialog");
            }
        });

        time = jdUTC.getCurrentTime(offset);
        loadLocation();

        mFM = getFragmentManager();
        taskFragment = (LunarOccultTask) mFM
                .findFragmentByTag(TASK_FRAGMENT_TAG);
        if (taskFragment != null) {
            taskFragment.setTargetFragment(this, TASK_FRAGMENT);
        } else {
            if (firstRun) {
                occultList.setVisibility(View.INVISIBLE);
                launchTask(time[1], 0.0, planetNum);
            } else {
                loadOccults();
            }
        }

        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        jdUTC = new JDUTC();

        // Get date and time formats from system
        mDateFormat = android.text.format.DateFormat
                .getDateFormat(getActivity().getApplicationContext());

        // Restore preferences
        settings = getActivity()
                .getSharedPreferences(PlanetsMain.MAIN_PREFS, 0);

        firstRun = settings.getBoolean("loFirstRun", true);
        firstDate = settings.getFloat("loFirstDate", 0);
        lastDate = settings.getFloat("loLastDate", 0);
        allPlanets = settings.getBoolean("loAllPlanets", true);
        planetNum = settings.getInt("loPlanetNum", 1);

        setHasOptionsMenu(true);
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
        switch (requestCode) {
            case PLANETS_DIALOG:
                nameButton.setText(planetArray.get(resultCode));
                planetNum = resultCode + 1;
                if (resultCode == 0) {
                    next.setVisible(false);
                    previous.setVisible(false);
                } else {
                    next.setVisible(true);
                    previous.setVisible(true);
                }
                time = jdUTC.getCurrentTime(offset);
                occultList.setVisibility(View.INVISIBLE);
                launchTask(time[1], 0.0, planetNum);
                break;
            case DATE_FRAGMENT:
                switch (resultCode) {
                    case 60:
                        // Set button
                        Calendar c = Calendar.getInstance();
                        c.clear();
                        c.set(data.getIntExtra("year", 0),
                                data.getIntExtra("month", 0),
                                data.getIntExtra("day", 0));
                        // convert local time to utc
                        c.add(Calendar.MINUTE, (int) (offset * -60));
                        time = jdUTC.utcjd(c.get(Calendar.MONTH) + 1,
                                c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.YEAR),
                                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE),
                                c.get(Calendar.SECOND));
                        occultList.setVisibility(View.INVISIBLE);
                        launchTask(time[1], 0.0, planetNum);
                        break;
                    case 70:
                        // Cancel button
                        break;
                }
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
                        firstRun = false;
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
                datePickerFragment.show(getFragmentManager(), "datePickerDialog");
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
        newLoc = settings.getBoolean("newLocation", true);
    }

    private void launchTask(double time, double back, int planet) {
        taskFragment = (LunarOccultTask) mFM
                .findFragmentByTag(TASK_FRAGMENT_TAG);
        if (taskFragment == null) {
            taskFragment = new LunarOccultTask();
            taskFragment.setData(taskFragment.new ComputeOccultTask(), g, time,
                    back, planet);
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
        cursorAdapter = new SimpleCursorAdapter(getActivity()
                .getApplicationContext(), R.layout.occult_list_row, loCursor,
                from, to, 0);
        // customize the fields
        cursorAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int column) {
                if (column == 1) {// date
                    TextView tv = (TextView) view;
                    double date = cursor.getDouble(cursor
                            .getColumnIndex(LunarOccultationTable.COLUMN_OCCULT_DATE));
                    Calendar gc = new GregorianCalendar();
                    gc.setTimeInMillis(jdUTC.jdmills(date));
                    tv.setText(mDateFormat.format(gc.getTime()));
                    return true;
                } else if (column == 2) {// planet
                    TextView tv = (TextView) view;
                    int i = cursor.getInt(cursor
                            .getColumnIndex(LunarOccultationTable.COLUMN_OCCULT_PLANET));
                    tv.setText(planetArray.get(i - 1));
                    return true;
                } else if (column == 3) {// local
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

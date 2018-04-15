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

package planets.position;

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
import android.support.v7.widget.AppCompatRadioButton;
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
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import planets.position.database.PlanetsDatabase;
import planets.position.database.PlanetsTable;
import planets.position.database.TimeZoneDB;
import planets.position.util.JDUTC;

public class WhatsUpNow extends Fragment {

    private static final int TASK_FRAGMENT = 0;
    private static final String TASK_FRAGMENT_TAG = "upTask";
    private static final int UPDATE_WAIT = 300000;// 5 min

    private FragmentListener mCallbacks;
    private FragmentManager mFM;
    private WhatsUpTask taskFragment;
    private long lastUpdate = 0, now;
    private double offset;
    private int viewIndex, zoneID;
    private final double[] g = new double[3];
    private boolean newLoc;
    private SharedPreferences settings;
    private TextView updateText;
    private AppCompatRadioButton riseRadio, setRadio, allRadio;
    private ListView planetsList;
    private DateFormat mDateFormat, mTimeFormat;
    private TimeZoneDB tzDB;
    private JDUTC jdUTC;
    private PlanetsDatabase planetsDB;
    private final int[] images = {R.drawable.ic_planet_sun,
            R.drawable.ic_planet_moon, R.drawable.ic_planet_mercury,
            R.drawable.ic_planet_venus, R.drawable.ic_planet_mars,
            R.drawable.ic_planet_jupiter, R.drawable.ic_planet_saturn,
            R.drawable.ic_planet_uranus, R.drawable.ic_planet_neptune,
            R.drawable.ic_planet_pluto};

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_whats_up, container, false);
        updateText = v.findViewById(R.id.lastUpdateText);
        planetsList = v.findViewById(R.id.planetList);
        RadioGroup rgLayout = v.findViewById(R.id.radioLayout);
        riseRadio = v.findViewById(R.id.riseRadioButton);
        setRadio = v.findViewById(R.id.setRadioButton);
        allRadio = v.findViewById(R.id.allRadioButton);

        rgLayout.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.riseRadioButton:
                        viewIndex = 0;
                        break;
                    case R.id.setRadioButton:
                        viewIndex = 1;
                        break;
                    case R.id.allRadioButton:
                        viewIndex = 2;
                        break;
                }
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt("viewIndex", viewIndex);
                editor.apply();
                loadPlanets(viewIndex);
            }
        });

        planetsList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                FragmentTransaction ft = null;
                if (getFragmentManager() != null) {
                    ft = getFragmentManager().beginTransaction();
                }
                WhatsUpData data = new WhatsUpData();
                Bundle args = new Bundle();
                args.putLong("planetNum", id);
                args.putLong("dateTime", lastUpdate);
                args.putInt("zoneID", zoneID);
                data.setArguments(args);
                if (ft != null) {
                    ft.replace(R.id.content_frame, data);
                    ft.addToBackStack(null);
                    ft.commit();
                }
            }
        });

        if (mCallbacks != null) {
            mCallbacks.onToolbarTitleChange("Rise / Set", 6);
        }

        mFM = getFragmentManager();
        assert mFM != null;
        taskFragment = (WhatsUpTask) mFM.findFragmentByTag(TASK_FRAGMENT_TAG);

        loadLocation();

        viewIndex = settings.getInt("viewIndex", 2);

        if (taskFragment != null) {
            taskFragment.setTargetFragment(this, TASK_FRAGMENT);
        } else {
            now = Calendar.getInstance().getTimeInMillis();
            // if no value or old value greater than UPDATE_WAIT
            if (lastUpdate == 0 || (now - lastUpdate > UPDATE_WAIT || newLoc)) {
                if (newLoc) {
                    // if new location then compute eclipses
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("newLocation", false);
                    editor.apply();
                    newLoc = false;
                }
                lastUpdate = now;
                planetsList.setVisibility(View.INVISIBLE);
                launchTask(offset);
            } else {
                loadPlanets(viewIndex);
            }
        }

        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tzDB = new TimeZoneDB(getActivity().getApplicationContext());
        planetsDB = new PlanetsDatabase(getActivity().getApplicationContext());
        jdUTC = new JDUTC();
        mDateFormat = android.text.format.DateFormat
                .getDateFormat(getActivity().getApplicationContext());
        mTimeFormat = android.text.format.DateFormat
                .getTimeFormat(getActivity().getApplicationContext());
        settings = getActivity().getSharedPreferences(PlanetsMain.MAIN_PREFS, 0);
        lastUpdate = settings.getLong("lastUpdate", 0);
        setHasOptionsMenu(true);
        setRetainInstance(true);
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
            planetsList.setVisibility(View.INVISIBLE);
            launchTask(offset);
        } else if (lastUpdate > 0)
            loadPlanets(viewIndex);
        super.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TASK_FRAGMENT) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    taskFragment = null;
                    // Save current time
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putLong("lastUpdate", now);
                    editor.apply();
                    loadPlanets(viewIndex);
                    break;
                case Activity.RESULT_CANCELED:
                    Log.e(TASK_FRAGMENT_TAG, "Planet computation task canceled.");
                    break;
                case 100:// ComputePlanetsTask error
                    Toast.makeText(getActivity().getApplicationContext(),
                            "There was a error calculating the planets.",
                            Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.whats_up_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar menu item selection
        switch (item.getItemId()) {
            case R.id.action_refresh:
                lastUpdate = Calendar.getInstance().getTimeInMillis();
                now = lastUpdate;
                tzDB.open();
                int off = tzDB.getZoneOffset(zoneID, now / 1000L);
                tzDB.close();
                offset = off / 60.0;
                planetsList.setVisibility(View.INVISIBLE);
                launchTask(offset);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void launchTask(double offset) {
        taskFragment = new WhatsUpTask();
        taskFragment.setData(taskFragment.new ComputePlanetsTask(), g,
                offset);
        taskFragment.setTargetFragment(this, TASK_FRAGMENT);
        taskFragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        taskFragment.show(mFM, TASK_FRAGMENT_TAG);
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
        zoneID = loc.getInt("zoneID");
        tzDB.open();
        int off = tzDB.getZoneOffset(zoneID, Calendar.getInstance().getTimeInMillis() / 1000L);
        tzDB.close();
        offset = off / 60.0;
    }

    private void loadPlanets(int index) {
        Cursor plCursor;
        SimpleCursorAdapter cursorAdapter;
        Calendar gc = new GregorianCalendar();
        gc.clear();
        gc.setTimeInMillis(lastUpdate);
        planetsDB.open();
        switch (index) {
            case 0:
                plCursor = planetsDB.getPlanetsRise();
                updateText.setText(String.format("What's rising on %s @ %s",
                        mDateFormat.format(gc.getTime()), mTimeFormat.format(gc.getTime())));
                riseRadio.setChecked(true);
                break;
            case 1:
                plCursor = planetsDB.getPlanetsSet();
                updateText.setText(String.format("What's setting on %s @ %s",
                        mDateFormat.format(gc.getTime()), mTimeFormat.format(gc.getTime())));
                setRadio.setChecked(true);
                break;
            default:
                plCursor = planetsDB.getPlanetsAll();
                updateText.setText(String.format("All Planets on %s @ %s",
                        mDateFormat.format(gc.getTime()), mTimeFormat.format(gc.getTime())));
                allRadio.setChecked(true);
                break;
        }
        String[] from = new String[]{PlanetsTable.COLUMN_NUMBER,
                PlanetsTable.COLUMN_NAME, PlanetsTable.COLUMN_ALT,
                PlanetsTable.COLUMN_RISE_TIME, PlanetsTable.COLUMN_SET_TIME};
        int[] to = new int[]{R.id.rowImage, R.id.rowName, R.id.rowRiseSet,
                R.id.rowRSDate, R.id.rowRSTime};
        cursorAdapter = new SimpleCursorAdapter(getActivity().getApplicationContext(),
                R.layout.whats_up_row, plCursor, from, to, 0);
        cursorAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int column) {
                double alt = cursor.getDouble(cursor
                        .getColumnIndex(PlanetsTable.COLUMN_ALT));
                Calendar c = Calendar.getInstance();
                double time;
                if (column == 0) {// image
                    ImageView iv = (ImageView) view;
                    int i = cursor.getInt(cursor.getColumnIndex(PlanetsTable.COLUMN_NUMBER));
                    iv.setImageResource(images[i]);
                    return true;
                }
                if (column == 1) {// name
                    TextView tv = (TextView) view;
                    String name = cursor.getString(cursor
                            .getColumnIndex(PlanetsTable.COLUMN_NAME));
                    tv.setText(name);
                    return true;
                }
                if (column == 2) {// rise/set label
                    TextView tv = (TextView) view;
                    if (alt > 0)
                        tv.setText(R.string.data_set);
                    else
                        tv.setText(R.string.data_rise);
                    return true;
                }
                if (column == 3) {// rise/set date
                    TextView tv = (TextView) view;
                    if (alt > 0) {
                        time = cursor.getDouble(cursor.getColumnIndex(PlanetsTable.COLUMN_SET_TIME));
                    } else {
                        time = cursor.getDouble(cursor.getColumnIndex(PlanetsTable.COLUMN_RISE_TIME));
                    }
                    c.clear();
                    c.setTimeInMillis(jdUTC.jdmills(time, offset));
                    tv.setText(mDateFormat.format(c.getTime()));
                    return true;
                }
                if (column == 4) {// rise/set time
                    TextView tv = (TextView) view;
                    if (alt > 0) {
                        time = cursor.getDouble(cursor.getColumnIndex(PlanetsTable.COLUMN_SET_TIME));
                    } else {
                        time = cursor.getDouble(cursor.getColumnIndex(PlanetsTable.COLUMN_RISE_TIME));
                    }
                    c.clear();
                    c.setTimeInMillis(jdUTC.jdmills(time, offset));
                    tv.setText(mTimeFormat.format(c.getTime()));
                    return true;
                }
                return false;
            }
        });
        planetsList.setAdapter(cursorAdapter);
        planetsList.setVisibility(View.VISIBLE);
    }
}

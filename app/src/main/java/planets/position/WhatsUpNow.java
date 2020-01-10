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

package planets.position;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import planets.position.database.PlanetsDatabase;
import planets.position.database.PlanetsTable;
import planets.position.database.TimeZoneDB;
import planets.position.util.JDUTC;
import planets.position.util.PositionFormat;

class WhatsUpNow extends Fragment {

    static final int TASK_FRAGMENT = 10;
    private static final String TASK_FRAGMENT_TAG = "upTask";
    private static final int UPDATE_WAIT = 300000;// 5 min

    private FragmentListener mCallbacks;
    private FragmentManager mFM;
    private WhatsUpTask taskFragment;
    private long lastUpdate = 0, now;
    private double offset;
    private int zoneID, viewIndex;
    private final double[] g = new double[3];
    private boolean newLoc;
    private SharedPreferences settings;
    private TextView updateText;
    private AppCompatRadioButton riseRadio, setRadio, allRadio;
    private ListView planetsList;
    private DateFormat mDateFormat, mTimeFormat;
    private TimeZoneDB tzDB;
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

        viewIndex = settings.getInt("viewIndex", 1);

        if (taskFragment != null) {
            taskFragment.setTargetFragment(this, TASK_FRAGMENT);
        } else {
            loadPlanets(viewIndex);
        }

        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tzDB = new TimeZoneDB(getActivity().getApplicationContext());
        planetsDB = new PlanetsDatabase(getActivity().getApplicationContext());
        mDateFormat = android.text.format.DateFormat
                .getDateFormat(getActivity().getApplicationContext());
        mTimeFormat = android.text.format.DateFormat
                .getTimeFormat(getActivity().getApplicationContext());
        settings = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        lastUpdate = settings.getLong("lastUpdate", 0);
        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(@NonNull Context context) {
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
        now = Calendar.getInstance().getTimeInMillis();
        // if no value or old value greater than UPDATE_WAIT
        if (lastUpdate == 0 || (now - lastUpdate > UPDATE_WAIT || newLoc)) {
            // if new location then compute eclipses
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("newLocation", false);
            editor.apply();
            newLoc = false;
            lastUpdate = now;
            planetsList.setVisibility(View.INVISIBLE);
            launchTask(offset);
        }
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
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.whats_up_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar menu item selection
        if (item.getItemId() == R.id.action_refresh) {
            lastUpdate = Calendar.getInstance().getTimeInMillis();
            now = lastUpdate;
            tzDB.open();
            int off = tzDB.getZoneOffset(zoneID, now / 1000L);
            tzDB.close();
            offset = off / 60.0;
            planetsList.setVisibility(View.INVISIBLE);
            launchTask(offset);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void launchTask(double offset) {
        taskFragment = (WhatsUpTask) mFM.findFragmentByTag(TASK_FRAGMENT_TAG);
        if (taskFragment == null) {
            taskFragment = new WhatsUpTask();
            taskFragment.setData(taskFragment.new ComputePlanetsTask(), g,
                    offset);
            taskFragment.setTargetFragment(this, TASK_FRAGMENT);
            taskFragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
            taskFragment.show(mFM, TASK_FRAGMENT_TAG);
        }
    }

    private void loadLocation() {
        // read location from Shared Preferences
        g[1] = settings.getFloat("latitude", 0);
        g[0] = settings.getFloat("longitude", 0);
        g[2] = settings.getFloat("elevation", 0);
        offset = settings.getFloat("offset", 0) * 60.0;
        zoneID = settings.getInt("zoneID", 0);
        newLoc = settings.getBoolean("newLocation", true);

        tzDB.open();
        int off = tzDB.getZoneOffset(zoneID, Calendar.getInstance().getTimeInMillis() / 1000L);
        tzDB.close();
        offset = off / 60.0;
    }

    private void loadPlanets(int index) {
        Cursor plCursor;
        final SimpleCursorAdapter cursorAdapter;
        Calendar gc = new GregorianCalendar();
        final PositionFormat pf = new PositionFormat(getActivity());
        final JDUTC jdUTC = new JDUTC();
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
        String[] from = new String[]{PlanetsTable.COLUMN_NUMBER, PlanetsTable.COLUMN_NAME,
                PlanetsTable.COLUMN_AZ, PlanetsTable.COLUMN_ALT, PlanetsTable.COLUMN_RISE,
                PlanetsTable.COLUMN_RISE_TIME, PlanetsTable.COLUMN_SET_TIME};
        int[] to = new int[]{R.id.rowImage, R.id.rowName, R.id.rowAZ, R.id.rowALT, R.id.rowRiseSet,
                R.id.rowRSDate};
        cursorAdapter = new SimpleCursorAdapter(getActivity().getApplicationContext(),
                R.layout.whats_up_row, plCursor, from, to, 0);
        cursorAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int column) {
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
                if (column == 2) {// Azimuth
                    TextView tv = (TextView) view;
                    double az = cursor.getDouble(cursor.getColumnIndex(PlanetsTable.COLUMN_AZ));
                    tv.setText(pf.formatAZ(az));
                    return true;
                }
                if (column == 3) {// Altitude
                    TextView tv = (TextView) view;
                    double alt = cursor.getDouble(cursor.getColumnIndex(PlanetsTable.COLUMN_ALT));
                    tv.setText(pf.formatALT(alt));
                    return true;
                }
                if (column == 4) {// rise
                    TextView tv = (TextView) view;
                    int rise = cursor.getInt(cursor.getColumnIndex(PlanetsTable.COLUMN_RISE));
                    if (rise > 0) {
                        tv.setText(R.string.data_set);
                    } else {
                        tv.setText(R.string.data_rise);
                    }
                    return true;
                }
                if (column == 5) {// rise/set date & time
                    TextView tv = (TextView) view;
                    int rise = cursor.getInt(cursor.getColumnIndex(PlanetsTable.COLUMN_RISE));
                    if (rise > 0) {
                        time = cursor.getDouble(cursor.getColumnIndex(PlanetsTable.COLUMN_SET_TIME));
                    } else {
                        time = cursor.getDouble(cursor.getColumnIndex(PlanetsTable.COLUMN_RISE_TIME));
                    }
                    c.clear();
                    c.setTimeInMillis(jdUTC.jdmills(time, offset));
                    tv.setText(String.format("%s %s", mDateFormat.format(c.getTime()),
                            mTimeFormat.format(c.getTime())));
                    return true;
                }
                return false;
            }
        });
        planetsList.setAdapter(cursorAdapter);
        planetsList.setVisibility(View.VISIBLE);
    }
}

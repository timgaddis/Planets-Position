/*
 * Planet's Position
 * A program to calculate the position of the planets in the night sky based
 * on a given location on Earth.
 * Copyright (C) 2016  Tim Gaddis
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
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import planets.position.database.PlanetsDatabase;
import planets.position.database.PlanetsTable;
import planets.position.util.PositionFormat;

public class WhatsUpNow extends Fragment {

    private static final int TASK_FRAGMENT = 0;
    private static final String TASK_FRAGMENT_TAG = "upTask";
    private static final int UPDATE_WAIT = 300000;// 5 min

    private FragmentListener mCallbacks;
    private FragmentManager mFM;
    private WhatsUpTask taskFragment;
    private long lastUpdate = 0, now;
    private double offset;
    private final double[] g = new double[3];
    private boolean newLoc;
    private PositionFormat pf;
    private SharedPreferences settings;
    private TextView updateText;
    private ListView planetsList;
    private DateFormat mDateFormat, mTimeFormat;
    private PlanetsDatabase planetsDB;
    private final int[] images = {R.drawable.ic_planet_sun,
            R.drawable.ic_planet_moon, R.drawable.ic_planet_mercury,
            R.drawable.ic_planet_venus, R.drawable.ic_planet_mars,
            R.drawable.ic_planet_jupiter, R.drawable.ic_planet_saturn,
            R.drawable.ic_planet_uranus, R.drawable.ic_planet_neptune,
            R.drawable.ic_planet_pluto};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_whats_up, container, false);
        updateText = (TextView) v.findViewById(R.id.lastUpdateText);
        planetsList = (ListView) v.findViewById(R.id.planetList);
        pf = new PositionFormat(getActivity().getApplicationContext());
        planetsDB = new PlanetsDatabase(getActivity().getApplicationContext());

        planetsList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                FragmentTransaction ft = getFragmentManager()
                        .beginTransaction();
                WhatsUpData data = new WhatsUpData();
                Bundle args = new Bundle();
                args.putLong("planetNum", id);
                args.putLong("dateTime", lastUpdate);
                data.setArguments(args);
                ft.replace(R.id.content_frame, data);
                ft.addToBackStack(null);
                ft.commit();
            }
        });

        if (mCallbacks != null) {
            mCallbacks.onToolbarTitleChange("What's Up Now", 6);
        }

        mFM = getFragmentManager();
        taskFragment = (WhatsUpTask) mFM.findFragmentByTag(TASK_FRAGMENT_TAG);

        loadLocation();

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
                launchTask();
            } else {
                loadPlanets();
            }
        }

        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get date and time formats from system
        mDateFormat = android.text.format.DateFormat
                .getDateFormat(getActivity().getApplicationContext());
        mTimeFormat = android.text.format.DateFormat
                .getTimeFormat(getActivity().getApplicationContext());

        // Restore preferences
        settings = getActivity()
                .getSharedPreferences(PlanetsMain.MAIN_PREFS, 0);

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
            launchTask();
        } else if (lastUpdate > 0)
            loadPlanets();
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
                    loadPlanets();
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
                planetsList.setVisibility(View.INVISIBLE);
                launchTask();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void launchTask() {
        taskFragment = new WhatsUpTask();
        taskFragment.setData(taskFragment.new ComputePlanetsTask(), g,
                offset);
        taskFragment.setTargetFragment(this, TASK_FRAGMENT);
        taskFragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        taskFragment.show(mFM, TASK_FRAGMENT_TAG);
    }

    private void loadLocation() {
        // read location from Shared Preferences
        g[1] = settings.getFloat("latitude", 0);
        g[0] = settings.getFloat("longitude", 0);
        g[2] = settings.getFloat("elevation", 0);
        offset = settings.getFloat("offset", 0);
        newLoc = settings.getBoolean("newLocation", true);
    }

    private void loadPlanets() {
        Cursor plCursor;
        SimpleCursorAdapter cursorAdapter;
        Calendar gc = new GregorianCalendar();
        gc.setTimeInMillis(lastUpdate);
        updateText.setText(String.format("What's Up on %s @ %s",
                mDateFormat.format(gc.getTime()), mTimeFormat.format(gc.getTime())));
        planetsDB.open();
        plCursor = planetsDB.getPlanets();
        String[] from = new String[]{PlanetsTable.COLUMN_ID,
                PlanetsTable.COLUMN_NAME, PlanetsTable.COLUMN_AZ,
                PlanetsTable.COLUMN_ALT};
        int[] to = new int[]{R.id.rowImage, R.id.rowName, R.id.rowAZ,
                R.id.rowALT};
        cursorAdapter = new SimpleCursorAdapter(getActivity()
                .getApplicationContext(), R.layout.whats_up_row, plCursor,
                from, to, 0);
        // customize the az and alt fields
        cursorAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int column) {
                if (column == 0) {// image
                    ImageView iv = (ImageView) view;
                    int i = cursor.getInt(cursor
                            .getColumnIndex(PlanetsTable.COLUMN_ID));
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
                if (column == 2) {// az
                    TextView tv = (TextView) view;
                    double az = cursor.getDouble(cursor
                            .getColumnIndex(PlanetsTable.COLUMN_AZ));
                    tv.setText(pf.formatAZ(az));
                    return true;
                }
                if (column == 3) {// alt
                    TextView tv = (TextView) view;
                    double alt = cursor.getDouble(cursor
                            .getColumnIndex(PlanetsTable.COLUMN_ALT));
                    tv.setText(pf.formatALT(alt));
                    return true;
                }
                return false;
            }
        });
        planetsList.setAdapter(cursorAdapter);
        planetsList.setVisibility(View.VISIBLE);
        planetsDB.close();
    }
}

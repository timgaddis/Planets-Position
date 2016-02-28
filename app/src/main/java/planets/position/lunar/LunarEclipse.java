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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import planets.position.FragmentListener;
import planets.position.PlanetsMain;
import planets.position.R;
import planets.position.database.LunarEclipseTable;
import planets.position.database.PlanetsDatabase;
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
    private ListView lunarList;
    private double offset, firstDate, lastDate;
    private final double[] g = new double[3];
    private double[] time;
    private boolean firstRun, newLoc;
    private DateFormat mDateFormat;
    private PlanetsDatabase planetsDB;
    private SharedPreferences settings;
    private JDUTC jdUTC;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_eclipse, container, false);
        lunarList = (ListView) v.findViewById(R.id.eclipseList);

        lunarList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                // display eclipse data
                FragmentTransaction ft = getFragmentManager()
                        .beginTransaction();
                eclipseData = new LunarEclipseData();
                Bundle args = new Bundle();
                args.putLong("lunarNum", id);
                args.putDouble("offset", offset);
                eclipseData.setArguments(args);
                ft.replace(R.id.content_frame, eclipseData, "eclipseData");
                ft.addToBackStack(null);
                ft.commit();
            }
        });

        if (mCallbacks != null) {
            mCallbacks.onToolbarTitleChange("Lunar Eclipse", 3);
        }

        planetsDB = new PlanetsDatabase(getActivity().getApplicationContext());
        loadLocation();
        time = jdUTC.getCurrentTime(offset);

        mFM = getFragmentManager();
        taskFragment = (LunarEclipseTask) mFM
                .findFragmentByTag(TASK_FRAGMENT_TAG);

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

        // Get date and time formats from system
        mDateFormat = android.text.format.DateFormat
                .getDateFormat(getActivity().getApplicationContext());

        // Restore preferences
        settings = getActivity()
                .getSharedPreferences(PlanetsMain.MAIN_PREFS, 0);

        firstRun = settings.getBoolean("leFirstRun", true);
        firstDate = settings.getFloat("leFirstDate", 0);
        lastDate = settings.getFloat("leLastDate", 0);

        setHasOptionsMenu(true);
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
                    lunarList.setVisibility(View.INVISIBLE);
                    launchTask(time[1], 0.0);
                    break;
                case 70:
                    // Cancel button
                    break;
            }
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

    private void launchTask(double time, double back) {
        taskFragment = new LunarEclipseTask();
        taskFragment.setData(taskFragment.new ComputeEclipseTask(), g, time,
                back);
        taskFragment.setTargetFragment(this, TASK_FRAGMENT);
        taskFragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        taskFragment.show(mFM, TASK_FRAGMENT_TAG);
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
                if (column == 1) {// type image
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
                } else if (column == 2) {// date
                    TextView tv = (TextView) view;
                    double date = cursor.getDouble(cursor
                            .getColumnIndex(LunarEclipseTable.COLUMN_ECLIPSE_DATE));
                    Calendar gc = new GregorianCalendar();
                    gc.setTimeInMillis(jdUTC.jdmills(date));
                    tv.setText(mDateFormat.format(gc.getTime()));
                    return true;
                } else if (column == 3) {// type
                    TextView tv = (TextView) view;
                    String type = cursor.getString(cursor
                            .getColumnIndex(LunarEclipseTable.COLUMN_ECLIPSE_TYPE));
                    tv.setText(type);
                    return true;
                } else if (column == 4) {// local
                    ImageView iv = (ImageView) view;
                    int l = cursor.getInt(cursor
                            .getColumnIndex(LunarEclipseTable.COLUMN_LOCAL));
                    if (l > 0)
                        iv.setVisibility(View.VISIBLE);
                    else
                        iv.setVisibility(View.INVISIBLE);
                    return true;
                }
                return false;
            }
        });
        lunarList.setAdapter(cursorAdapter);
        lunarList.setVisibility(View.VISIBLE);
        planetsDB.close();
    }
}

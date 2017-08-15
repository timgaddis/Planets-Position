/*
 * Planet's Position
 * A program to calculate the position of the planets in the night sky based
 * on a given location on Earth.
 * Copyright (c) 2017 Tim Gaddis
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

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import planets.position.database.LocationTable;
import planets.position.database.PlanetsDatabase;
import planets.position.location.LocationDialog;
import planets.position.location.LocationHelper;
import planets.position.location.LocationTask;
import planets.position.location.UserLocation;
import planets.position.lunar.LunarEclipse;
import planets.position.lunar.LunarOccultation;
import planets.position.settings.Settings;
import planets.position.solar.SolarEclipse;

public class PlanetsMain extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, FragmentListener, LocationTask.LocationCallbackMain,
        FileCopyTask.FileCopyCallback, LocationHelper.LocationMainCallback {

    public static final String TAG = "PlanetsMain";
    public static final String MAIN_PREFS = "MainPrefsFile";

    private ActionBarDrawerToggle toggle;
    private NavigationView navigationView;
    private SharedPreferences settings;
    private LocationTask locationTask;
    private LocationHelper locationHelper;
    private Snackbar mySnackbar;
    private FileCopyTask copyTask;
    private PlanetsDatabase planetsDB;
    private int ioffset = -1, fragIndex;
    private double latitude, longitude, elevation, offset;
    private List<String> gmtValues;
    private CharSequence actionTitle;
    private FragmentManager fm;
    private View mLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_planets_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            getDelegate().setSupportActionBar(toolbar);
        }

        mLayout = findViewById(R.id.content_frame);
        planetsDB = new PlanetsDatabase(this);
        gmtValues = Arrays.asList(getResources().getStringArray(
                R.array.gmt_values));
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        assert drawer != null;
        drawer.addDrawerListener(toggle);
        assert getDelegate().getSupportActionBar() != null;
        getDelegate().getSupportActionBar().setHomeButtonEnabled(true);
        getDelegate().getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        assert navigationView != null;
        navigationView.setNavigationItemSelectedListener(this);

        settings = getSharedPreferences(MAIN_PREFS, 0);

        loadLocation();

        fm = getSupportFragmentManager();
        copyTask = (FileCopyTask) fm.findFragmentByTag("copyTask");

        locationTask = (LocationTask) getSupportFragmentManager()
                .findFragmentByTag(LocationTask.TAG);
        if (locationTask == null) {
            locationTask = LocationTask.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .add(locationTask, LocationTask.TAG)
                    .commit();
        }

        locationHelper = (LocationHelper) getSupportFragmentManager().
                findFragmentByTag(LocationHelper.TAG);
        if (locationHelper == null) {
            locationHelper = LocationHelper.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .add(locationHelper, LocationHelper.TAG)
                    .commit();
        }

        if ((checkFiles("semo_18.se1") && checkFiles("sepl_18.se1"))) {
            if (latitude < -90) {
                startLocationDialog();
            }
        } else {
            startCopyFileTask();
        }

        if (savedInstanceState == null) {
            selectItem(0, false);
        }

        SharedPreferences.Editor editor = settings.edit();
        editor.putString("ephPath", getFilesDir().getPath() + File.separator + "ephemeris");
        editor.putBoolean("hasCalendar", checkForCalendar());
        editor.apply();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        toggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        toggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        fragIndex = savedInstanceState.getInt("frag");
        actionTitle = savedInstanceState.getCharSequence("title");
        onToolbarTitleChange(actionTitle, fragIndex);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        locationTask.setTargetFragment(null, -1);
        outState.putInt("frag", fragIndex);
        outState.putCharSequence("title", actionTitle);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        assert drawer != null;
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_solar_ecl) {
            selectItem(1, false);
        } else if (id == R.id.nav_lunar_ecl) {
            selectItem(3, false);
        } else if (id == R.id.nav_lunar_occ) {
            selectItem(4, false);
        } else if (id == R.id.nav_sky_pos) {
            selectItem(5, false);
        } else if (id == R.id.nav_whats_up) {
            selectItem(6, false);
        } else if (id == R.id.nav_location) {
            selectItem(7, false);
        } else if (id == R.id.nav_settings) {
            selectItem(8, false);
        } else if (id == R.id.nav_about) {
            selectItem(9, false);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        assert drawer != null;
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void loadLocation() {
        if (settings.contains("latitude")) {
            // read location from Shared Preferences
            latitude = settings.getFloat("latitude", 0);
            longitude = settings.getFloat("longitude", 0);
            elevation = settings.getFloat("elevation", 0);
            offset = settings.getFloat("offset", 0);
            ioffset = settings.getInt("ioffset", 15);
        } else {
            // Read location from database
            planetsDB.open();
            Bundle loc = planetsDB.getLocation();
            planetsDB.close();

            latitude = loc.getDouble("latitude");
            longitude = loc.getDouble("longitude");
            elevation = loc.getDouble("elevation");
            offset = loc.getDouble("offset");
            ioffset = loc.getInt("ioffset");
        }
    }

    // Save location to database and Shared Preferences
    private boolean saveLocation() {

        long date = Calendar.getInstance().getTimeInMillis();

        // save to Shared Preferences
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat("latitude", (float) latitude);
        editor.putFloat("longitude", (float) longitude);
        editor.putFloat("elevation", (float) elevation);
        editor.putFloat("offset", (float) offset);
        editor.putInt("ioffset", ioffset);
        editor.putLong("locDate", date);
        editor.putBoolean("newLocation", true);
        boolean out = editor.commit();

        // save to database
        ContentValues values = new ContentValues();
        values.put(LocationTable.COLUMN_NAME, "Home");
        values.put(LocationTable.COLUMN_LATITUDE, latitude);
        values.put(LocationTable.COLUMN_LONGITUDE, longitude);
        values.put(LocationTable.COLUMN_TEMP, 0.0);
        values.put(LocationTable.COLUMN_PRESSURE, 0.0);
        values.put(LocationTable.COLUMN_ELEVATION, elevation);
        values.put(LocationTable.COLUMN_DATE, date);
        values.put(LocationTable.COLUMN_OFFSET, offset);
        values.put(LocationTable.COLUMN_IOFFSET, ioffset);

        planetsDB.open();
        long row = planetsDB.addLocation(values);
        planetsDB.close();

        return row > -1 && out;
    }

    public void navigate(int position) {
        selectItem(position, false);
    }

    private void selectItem(int position, boolean edit) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Bundle args = new Bundle();

        switch (position) {
            case 0: // Main navigaton
                ft.replace(R.id.content_frame, new Navigation());
                ft.commit();
                break;
            case 1: // Solar Eclipse
                if (longitude == 0)
                    loadLocation();
                ft.replace(R.id.content_frame, new SolarEclipse());
                ft.addToBackStack(null);
                ft.commit();
                break;
            case 3: // Lunar Eclipse
                ft.replace(R.id.content_frame, new LunarEclipse());
                ft.addToBackStack(null);
                ft.commit();
                break;
            case 4: // Lunar Occultation
                ft.replace(R.id.content_frame, new LunarOccultation());
                ft.addToBackStack(null);
                ft.commit();
                break;
            case 5: // Sky Position
                if (longitude == 0)
                    loadLocation();
                ft.replace(R.id.content_frame, new SkyPosition());
                ft.addToBackStack(null);
                ft.commit();
                break;
            case 6: // What's Up Now
                if (longitude == 0)
                    loadLocation();
                ft.replace(R.id.content_frame, new WhatsUpNow());
                ft.addToBackStack(null);
                ft.commit();
                break;
            case 7: // User Location
                UserLocation userLoc = new UserLocation();
                args.putBoolean("edit", edit);
                userLoc.setArguments(args);
                ft.replace(R.id.content_frame, userLoc);
                ft.addToBackStack(null);
                ft.commit();
                break;
            case 8: // Settings
                ft.replace(R.id.content_frame, new Settings());
                ft.addToBackStack(null);
                ft.commit();
                break;
            case 9: // About
                ft.replace(R.id.content_frame, new About());
                ft.addToBackStack(null);
                ft.commit();
                break;
            default:
                break;
        }
    }

    @Override
    public void onToolbarTitleChange(CharSequence title, int index) {
        assert getDelegate().getSupportActionBar() != null;
        getDelegate().getSupportActionBar().setTitle(title);
        fragIndex = index;
        actionTitle = title;
        int[] menuID = {R.id.nav_solar_ecl, R.id.nav_lunar_ecl, R.id.nav_lunar_occ,
                R.id.nav_sky_pos, R.id.nav_whats_up, R.id.nav_location, R.id.nav_settings,
                R.id.nav_about};
        //clear previous selection
        for (int i = 0; i < 8; i++) {
            navigationView.getMenu().findItem(menuID[i]).setChecked(false);
        }

        switch (index) {
            case 0: // Main navigaton
                break;
            case 1: // Solar Eclipse
                navigationView.getMenu().findItem(R.id.nav_solar_ecl).setChecked(true);
                break;
            case 3: // Lunar Eclipse
                navigationView.getMenu().findItem(R.id.nav_lunar_ecl).setChecked(true);
                break;
            case 4: // Lunar Occultation
                navigationView.getMenu().findItem(R.id.nav_lunar_occ).setChecked(true);
                break;
            case 5: // Sky Position
                navigationView.getMenu().findItem(R.id.nav_sky_pos).setChecked(true);
                break;
            case 6: // What's Up Now
                navigationView.getMenu().findItem(R.id.nav_whats_up).setChecked(true);
                break;
            case 7: // User Location
                navigationView.getMenu().findItem(R.id.nav_location).setChecked(true);
                break;
            case 8: // Settings
                navigationView.getMenu().findItem(R.id.nav_settings).setChecked(true);
                break;
            case 9: // About
                navigationView.getMenu().findItem(R.id.nav_about).setChecked(true);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LocationTask.REQUEST_RESOLVE_ERROR) {
            if (resultCode == RESULT_OK) {
                startLocationTask();
            }
        }
    }

    @Override
    public void onLocationPermissionGranted() {
        Snackbar.make(mLayout, R.string.permision_location,
                Snackbar.LENGTH_SHORT).show();
        locationHelper.setLocationPermissionDenied(false);
        startLocationTask();
    }

    @Override
    public void onLocationPermissionDenied() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            Log.i(PlanetsMain.TAG,
                    "Displaying Location permission rationale to provide additional context.");
            // Show an expanation and try again
            Snackbar.make(mLayout, R.string.permission_reason, Snackbar.LENGTH_LONG).show();
            locationHelper.checkLocationPermissions(true);
        } else {
            locationHelper.setCheckingPermission(false);
            locationHelper.setLocationPermissionDenied(true);
        }
    }

    // ********************************
    // ******* Location callback ******
    // ********************************
    @Override
    public void onLocationFoundMain(Bundle data) {
        mySnackbar.dismiss();
        locationTask.stop();
        if (data != null) {
            if (data.getBoolean("locationNull")) {
                Toast.makeText(this, "Location not found.", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "onLocationFoundMain, Location found");
                latitude = data.getDouble("latitude");
                longitude = data.getDouble("longitude");
                elevation = data.getDouble("elevation");
                offset = data.getDouble("offset");
                ioffset = gmtValues.indexOf(offset + "");
                if (saveLocation())
                    Toast.makeText(this, "Location saved.", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this, "Location not saved.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d(TAG, "onLocationFoundMain, No location found");
            Toast.makeText(this, "No location found.", Toast.LENGTH_SHORT).show();
        }
    }

    // ********************************
    // ***** Location dialog code *****
    // ********************************
    @Override
    public void onDialogPositiveClick() {
        // GPS
        locationHelper.setCheckingPermission(true);
        locationHelper.checkLocationPermissions(true);
    }

    @Override
    public void onDialogNegativeClick() {
        // Manual
        selectItem(7, true);
    }
    // ********************************

    private void startLocationDialog() {
        // No location
        if (!locationHelper.isCheckingPermission()) {
            DialogFragment newFragment = new LocationDialog();
            newFragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
            newFragment.setCancelable(false);
            newFragment.show(getSupportFragmentManager(), "locationDialog");
        }
    }

    private void startLocationTask() {
        mySnackbar = Snackbar.make(mLayout, R.string.location_dialog, Snackbar.LENGTH_INDEFINITE);
        mySnackbar.setAction(R.string.action_cancel, new MyCancelListener());
        mySnackbar.show();
        locationTask.start(true);
    }

    private class MyCancelListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (mySnackbar.isShown()) {
                locationTask.stop();
                mySnackbar.dismiss();
            }
        }
    }

    private void startCopyFileTask() {
        if (copyTask == null) {
            // copy files task
            copyTask = new FileCopyTask();
            copyTask.setTask(copyTask.new CopyFilesTask());
            copyTask.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
            copyTask.show(fm, "copyTask");
        }
    }

    /**
     * Checks to see if the given file exists in internal storage.
     *
     * @param name file name to check
     * @return true if exists, false otherwise
     */
    private boolean checkFiles(String name) {
        String p = getApplicationContext().getFilesDir().getAbsolutePath() +
                File.separator + "ephemeris" + File.separator + name;
        File f = new File(p);
        return f.exists();
    }

    @Override
    public void onCopyFinished() {
        copyTask = null;
        startLocationDialog();
    }

    private boolean checkForCalendar() {
        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setData(CalendarContract.Events.CONTENT_URI);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
        return activities.size() > 0;
    }
}

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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import java.io.File;
import java.util.List;

import planets.position.database.PlanetsDatabase;
import planets.position.database.TimeZoneDB;
import planets.position.location.LocationDialog;
import planets.position.location.UserLocation;
import planets.position.lunar.LunarEclipse;
import planets.position.lunar.LunarOccultation;
import planets.position.settings.Settings;
import planets.position.solar.SolarEclipse;

public class PlanetsMain extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, FragmentListener,
        FileCopyTask.FileCopyCallback {

    public static final String TAG = "PlanetsMain";
    public static final String MAIN_PREFS = "MainPrefsFile";

    private ActionBarDrawerToggle toggle;
    private NavigationView navigationView;
    private SharedPreferences settings;
    private FileCopyTask copyTask;
    private PlanetsDatabase planetsDB;
    private int fragIndex;
    private double latitude, longitude;
    private CharSequence actionTitle;
    private FragmentManager fm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_planets_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            getDelegate().setSupportActionBar(toolbar);
        }

        planetsDB = new PlanetsDatabase(this);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        assert drawer != null;
        drawer.addDrawerListener(toggle);
        assert getDelegate().getSupportActionBar() != null;
        getDelegate().getSupportActionBar().setHomeButtonEnabled(true);
        getDelegate().getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        navigationView = findViewById(R.id.nav_view);
        assert navigationView != null;
        navigationView.setNavigationItemSelectedListener(this);

        settings = getSharedPreferences(MAIN_PREFS, 0);

        loadLocation();

        fm = getSupportFragmentManager();
        copyTask = (FileCopyTask) fm.findFragmentByTag("copyTask");

        if (!checkFiles(TimeZoneDB.DB_NAME))
            startCopyFileTask();

        if (latitude < -90) {
            startLocationDialog();
        }

        if (savedInstanceState == null) {
            selectItem(0, false, false);
        }

        SharedPreferences.Editor editor = settings.edit();
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
        outState.putInt("frag", fragIndex);
        outState.putCharSequence("title", actionTitle);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        assert drawer != null;
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_solar_ecl) {
            selectItem(1, false, false);
        } else if (id == R.id.nav_lunar_ecl) {
            selectItem(3, false, false);
        } else if (id == R.id.nav_lunar_occ) {
            selectItem(4, false, false);
        } else if (id == R.id.nav_sky_pos) {
            selectItem(5, false, false);
        } else if (id == R.id.nav_whats_up) {
            selectItem(6, false, false);
        } else if (id == R.id.nav_location) {
            selectItem(7, false, false);
        } else if (id == R.id.nav_settings) {
            selectItem(8, false, false);
        } else if (id == R.id.nav_about) {
            selectItem(9, false, false);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        assert drawer != null;
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void loadLocation() {
        if (settings.contains("latitude")) {
            // read location from Shared Preferences
            latitude = settings.getFloat("latitude", 0);
            longitude = settings.getFloat("longitude", 0);
        } else {
            // Read location from database
            planetsDB.open();
            Bundle loc = planetsDB.getLocation();
            planetsDB.close();
            if (!loc.isEmpty()) {
                latitude = loc.getDouble("latitude");
                longitude = loc.getDouble("longitude");
            } else {
                latitude = -91.0;
                longitude = 0.0;
            }
        }
    }

    public void navigate(int position) {
        selectItem(position, false, false);
    }

    private void selectItem(int position, boolean edit, boolean location) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

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
                Bundle b = new Bundle();
                b.putBoolean("edit", edit);
                b.putBoolean("loc", location);
                Intent i = new Intent(this, UserLocation.class);
                i.putExtras(b);
                startActivity(i);
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

    // ********************************
    // ***** Location dialog code *****
    // ********************************
    @Override
    public void onDialogPositiveClick() {
        // GPS
        selectItem(7, false, true);
    }

    @Override
    public void onDialogNegativeClick() {
        // Manual
        selectItem(7, true, false);
    }
    // ********************************

    private void startLocationDialog() {
        // No location
        DialogFragment newFragment = new LocationDialog();
        newFragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        newFragment.setCancelable(false);
        newFragment.show(getSupportFragmentManager(), "locationDialog");
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
                File.separator + "databases" + File.separator + name;
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

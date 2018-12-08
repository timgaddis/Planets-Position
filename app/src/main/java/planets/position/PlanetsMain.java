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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.List;
import java.util.Queue;

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

    //    public static final String TAG = "PlanetsMain";
    private static final int LOCATION = 200;

    private ActionBarDrawerToggle toggle;
    private NavigationView navigationView;
    private FileCopyTask copyTask;
    private SharedPreferences settings;
    private int fragIndex;
    private double latitude, longitude;
    private boolean isRunning;
    private CharSequence actionTitle;
    private Queue<DeferredFragmentTransaction> deferredFragmentTransactions;

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

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        assert drawer != null;
        drawer.addDrawerListener(toggle);
        assert getDelegate().getSupportActionBar() != null;
        getDelegate().getSupportActionBar().setHomeButtonEnabled(true);
        getDelegate().getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        deferredFragmentTransactions = new ArrayDeque<>();

        navigationView = findViewById(R.id.nav_view);
        assert navigationView != null;
        navigationView.setNavigationItemSelectedListener(this);

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        long versionNumber;
        try {
            versionNumber = getAppVersionCode(this);
        } catch (PackageManager.NameNotFoundException e) {
            versionNumber = -1;
        }

        if (!settings.contains("appVersion")) {
            SharedPreferences.Editor editor = settings.edit();
            editor.clear();
            editor.putInt("appVersion", (int) versionNumber);
            editor.apply();
        }

        loadLocation();

        copyTask = (FileCopyTask) getSupportFragmentManager().findFragmentByTag("copyTask");

        if (!checkFiles())
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

    @TargetApi(Build.VERSION_CODES.P)
    private static long getAppVersionCode(Context context) throws PackageManager.NameNotFoundException {
        PackageInfo pinfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        long returnValue;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P
                && !"P".equals(Build.VERSION.CODENAME)) {
            returnValue = pinfo.versionCode;
        } else {
            returnValue = pinfo.getLongVersionCode();
        }
        return returnValue;
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
    protected void onPause() {
        super.onPause();
        isRunning = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isRunning = true;
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        while (!deferredFragmentTransactions.isEmpty()) {
            deferredFragmentTransactions.remove().commit();
        }
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

        switch (id) {
            case R.id.nav_solar_ecl:
                selectItem(1, false, false);
                break;
            case R.id.nav_lunar_ecl:
                selectItem(3, false, false);
                break;
            case R.id.nav_lunar_occ:
                selectItem(4, false, false);
                break;
            case R.id.nav_sky_pos:
                selectItem(5, false, false);
                break;
            case R.id.nav_whats_up:
                selectItem(6, false, false);
                break;
            case R.id.nav_location:
                selectItem(7, false, false);
                break;
            case R.id.nav_settings:
                selectItem(8, false, false);
                break;
            case R.id.nav_about:
                selectItem(9, false, false);
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        assert drawer != null;
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOCATION) {
            if (resultCode == RESULT_OK) {
                Bundle b = data.getExtras();
                // save to Shared Preferences
                if (b != null) {
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putFloat("latitude", (float) b.getDouble("latitude"));
                    editor.putFloat("longitude", (float) b.getDouble("longitude"));
                    editor.putFloat("elevation", (float) b.getDouble("elevation"));
                    editor.putFloat("offset", (float) b.getDouble("offset"));
                    editor.putInt("zoneID", b.getInt("zoneID"));
                    editor.putString("zoneName", b.getString("zoneName"));
                    editor.putLong("date", b.getLong("date"));
                    editor.putBoolean("newLocation", b.getBoolean("newLocation"));
                    editor.apply();
                }
            }
        }
        DialogFragment newFragment = (DialogFragment)
                getSupportFragmentManager().findFragmentByTag("locationDialog");
        if (newFragment != null)
            replaceFragment(-2, null, newFragment, "", false);
        onToolbarTitleChange("Planet's Position", 0);
        selectItem(0, false, false);
        loadLocation();
    }

    private void loadLocation() {
        double offset;
        int zoneID;
        TimeZoneDB tzDB = new TimeZoneDB(getApplicationContext());

        // read location from Shared Preferences
        latitude = settings.getFloat("latitude", (float) -91.0);
        longitude = settings.getFloat("longitude", 0);
        zoneID = settings.getInt("zoneID", -1);

        if (zoneID > 0) {
            // Update GMT offset
            tzDB.open();
            int off = tzDB.getZoneOffset(zoneID, Calendar.getInstance().getTimeInMillis() / 1000L);
            tzDB.close();
            offset = off / 3600.0;

            SharedPreferences.Editor editor = settings.edit();
            editor.putFloat("offset", (float) offset);
            editor.apply();
        }
    }

    public void navigate(int position) {
        selectItem(position, false, false);
    }

    private void selectItem(int position, boolean edit, boolean location) {

        switch (position) {
            case 0: // Main navigaton
                replaceFragment(R.id.content_frame, new Navigation(), null, "", false);
                break;
            case 1: // Solar Eclipse
                if (longitude == 0)
                    loadLocation();
                replaceFragment(R.id.content_frame, new SolarEclipse(), null, "", true);
                break;
            case 3: // Lunar Eclipse
                replaceFragment(R.id.content_frame, new LunarEclipse(), null, "", true);
                break;
            case 4: // Lunar Occultation
                replaceFragment(R.id.content_frame, new LunarOccultation(), null, "", true);
                break;
            case 5: // Sky Position
                if (longitude == 0)
                    loadLocation();
                replaceFragment(R.id.content_frame, new SkyPosition(), null, "", true);
                break;
            case 6: // Rise / Set
                if (longitude == 0)
                    loadLocation();
                replaceFragment(R.id.content_frame, new WhatsUpNow(), null, "", true);
                break;
            case 7: // User Location
                Bundle b = new Bundle();
                b.putBoolean("edit", edit);
                b.putBoolean("loc", location);
                Intent i = new Intent(this, UserLocation.class);
                i.putExtras(b);
                startActivityForResult(i, LOCATION);
                break;
            case 8: // Settings
                replaceFragment(R.id.content_frame, new Settings(), null, "", true);
                break;
            case 9: // About
                replaceFragment(R.id.content_frame, new About(), null, "", true);
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
            case 6: // Rise / Set
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

    private void replaceFragment(int contentFrameId, Fragment replacingFragment, DialogFragment dialogFragment,
                                 String tag, final boolean back) {

        if (!isRunning) {
            DeferredFragmentTransaction deferredFragmentTransaction = new DeferredFragmentTransaction() {
                @Override
                public void commit() {
                    if (getContentFrameId() > 0) {
                        replaceFragmentInternal(getContentFrameId(), getReplacingFragment(), back);
                    } else {
                        dialogFragmentInternal(getContentFrameId(), getDialogFragment(), getContentTag());
                    }
                }
            };

            deferredFragmentTransaction.setContentFrameId(contentFrameId);
            deferredFragmentTransaction.setReplacingFragment(replacingFragment);
            deferredFragmentTransaction.setDialogFragment(dialogFragment);
            deferredFragmentTransaction.setContentTag(tag);

            deferredFragmentTransactions.add(deferredFragmentTransaction);
        } else {
            if (contentFrameId > 0) {
                replaceFragmentInternal(contentFrameId, replacingFragment, back);
            } else {
                dialogFragmentInternal(contentFrameId, dialogFragment, tag);
            }
        }
    }

    private void replaceFragmentInternal(int contentFrameId, Fragment replacingFragment, boolean backStack) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(contentFrameId, replacingFragment);
        if (backStack)
            ft.addToBackStack(null);
        ft.commit();
    }

    private void dialogFragmentInternal(int contentID, DialogFragment dialogFragment, String tag) {
        if (contentID == -1) {
            dialogFragment.show(getSupportFragmentManager(), tag);
        } else if (contentID == -2) {
            dialogFragment.dismiss();
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
        replaceFragment(-1, null, newFragment, "locationDialog", false);
    }

    private void startCopyFileTask() {
        if (copyTask == null) {
            // copy files task
            copyTask = new FileCopyTask();
            copyTask.setTask(copyTask.new CopyFilesTask());
            copyTask.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
            replaceFragment(-1, null, copyTask, "copyTask", false);
        }
    }

    /**
     * Checks to see if the given file exists in internal storage.
     *
     * @return true if exists, false otherwise
     */
    private boolean checkFiles() {
        String p = getApplicationContext().getFilesDir().getAbsolutePath() +
                File.separator + "databases" + File.separator + TimeZoneDB.DB_NAME;
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

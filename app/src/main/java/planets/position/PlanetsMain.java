package planets.position;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.util.Calendar;

import planets.position.Location.LocationDialog;
import planets.position.Location.LocationLib;
import planets.position.Location.UserLocation;

public class PlanetsMain extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, FragmentListener, LocationLib.LocationCallback,
        FileCopyTask.FileCopyCallback {

    public static final String TAG = "PlanetsMain";
    public static final String LOC_PREFS = "LocationPrefsFile";
    public static final int REQUEST_LOCATION = 100;
    public static final int REQUEST_STORAGE = 200;

    private ActionBarDrawerToggle toggle;
    private NavigationView navigationView;
    private SharedPreferences settings;
    private PermissionLib permissionLib;
    private LocationLib locationLib;
    private FileCopyTask copyTask;
    private int ioffset = -1;
    private double latitude, longitude, elevation, offset;
    private FragmentManager fm;
    private View mLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        setContentView(R.layout.activity_planets_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            getDelegate().setSupportActionBar(toolbar);
        }

        mLayout = findViewById(R.id.content_frame);
        permissionLib = new PermissionLib(this);
        locationLib = new LocationLib(this, this, null, 100);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        getDelegate().getSupportActionBar().setHomeButtonEnabled(true);
        getDelegate().getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        settings = getSharedPreferences(LOC_PREFS, 0);

        loadLocation();

        fm = getSupportFragmentManager();
        copyTask = (FileCopyTask) fm.findFragmentByTag("copyTask");

        if (!(checkFiles("semo_18.se1") && checkFiles("sepl_18.se1"))) {
            startCopyFileTask();
        } else {
            if (latitude < -90) {
                startLocationTask();
            }
        }

        if (savedInstanceState == null) {
            selectItem(0, false, false);
        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        toggle.syncState();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.planets_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_solar_ecl) {

        } else if (id == R.id.nav_lunar_ecl) {

        } else if (id == R.id.nav_lunar_occ) {

        } else if (id == R.id.nav_sky_pos) {

        } else if (id == R.id.nav_whats_up) {

        } else if (id == R.id.nav_location) {
            selectItem(7, false, true);
        } else if (id == R.id.nav_settings) {
            selectItem(8, false, true);
        } else if (id == R.id.nav_about) {
            selectItem(9, false, true);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
            // Check if the only required permission has been granted
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Location permission has now been granted.");
                Snackbar.make(mLayout, R.string.permision_location,
                        Snackbar.LENGTH_SHORT).show();
                locationLib.connect();
            } else {
                Log.i(TAG, "Location permission was NOT granted.");
                Snackbar.make(mLayout, R.string.permision_not_location,
                        Snackbar.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
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
            latitude = -100.0;
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
        editor.putLong("date", date);
//        boolean out = editor.commit();
        Log.d(TAG, "saveLocation");
        return editor.commit();

        // save to database
//        ContentValues values = new ContentValues();
//        values.put(LocationTable.COLUMN_LATITUDE, latitude);
//        values.put(LocationTable.COLUMN_LONGITUDE, longitude);
//        values.put(LocationTable.COLUMN_TEMP, 0.0);
//        values.put(LocationTable.COLUMN_PRESSURE, 0.0);
//        values.put(LocationTable.COLUMN_ELEVATION, elevation);
//        values.put(LocationTable.COLUMN_DATE, date);
//        values.put(LocationTable.COLUMN_OFFSET, offset);
//        values.put(LocationTable.COLUMN_IOFFSET, ioffset);
//
//        int rows = cr.update(Uri.withAppendedPath(
//                        PlanetsContentProvider.LOCATION_URI, String.valueOf(0)),
//                values, null, null);
//        return rows == 1 && out;
    }

    public void navigate(int position, boolean edit, boolean back) {
        selectItem(position, edit, back);
    }

    private void selectItem(int position, boolean edit, boolean back) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Bundle args = new Bundle();

        switch (position) {
            case 0: // Main navigaton
                ft.replace(R.id.content_frame, new Navigation());
                if (back)
                    ft.addToBackStack(null);
                ft.commit();
                break;
//            case 1:
//                if (longitude == 0)
//                    loadLocation();
//                title = "Solar Eclipse";
//                ft.replace(R.id.content_frame, new SolarEclipse());
//                if (back)
//                    ft.addToBackStack(null);
//                ft.commit();
//                break;
//            case 3:
//                title = "Lunar Eclipse";
//                ft.replace(R.id.content_frame, new LunarEclipse());
//                if (back)
//                    ft.addToBackStack(null);
//                ft.commit();
//                break;
//            case 4:
//                title = "Lunar Occultation";
//                ft.replace(R.id.content_frame, new LunarOccultation());
//                if (back)
//                    ft.addToBackStack(null);
//                ft.commit();
//                break;
//            case 5:
//                if (longitude == 0)
//                    loadLocation();
//                title = "Sky Position";
//                ft.replace(R.id.content_frame, new SkyPosition());
//                if (back)
//                    ft.addToBackStack(null);
//                ft.commit();
//                break;
//            case 6:
//                if (longitude == 0)
//                    loadLocation();
//                title = "What's Up Now";
//                ft.replace(R.id.content_frame, new WhatsUpNow());
//                if (back)
//                    ft.addToBackStack(null);
//                ft.commit();
//                break;
            case 7: // User Location
                UserLocation userLoc = new UserLocation();
                args.putBoolean("edit", edit);
                userLoc.setArguments(args);
                ft.replace(R.id.content_frame, userLoc);
                if (back)
                    ft.addToBackStack(null);
                ft.commit();
                break;
            case 8: // Settings
                ft.replace(R.id.content_frame, new SettingsFragment());
                ft.addToBackStack(null);
                ft.commit();
                break;
            case 9: // About
                ft.replace(R.id.content_frame, new About());
                ft.addToBackStack(null);
                ft.commit();
                break;
        }
    }

    // ********************************
    // ***** LocationLib callback *****
    // ********************************
    @Override
    public void onLocationFound(Location location) {

        locationLib.disconnect();
        if (location != null) {
            Log.d(TAG, "onLocationFound, Location found");
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            elevation = location.getAltitude();
            offset = Calendar.getInstance().getTimeZone()
                    .getOffset(location.getTime()) / 3600000.0;
            saveLocation();
        } else {
            Log.d(TAG, "onLocationFound, No location found");
            Toast.makeText(this, "No location found.",
                    Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onToolbarTitleChange(CharSequence title, int index) {
        //clear previous selection
        for (int i = 0; i <= 7; i++) {
            navigationView.getMenu().getItem(i).setChecked(false);
        }
        getDelegate().getSupportActionBar().setTitle(title);
        if (index >= 0) {
            navigationView.getMenu().getItem(index).setChecked(true);
        }
    }

    // ********************************
    // ***** Location dialog code *****
    // ********************************
    @Override
    public void onDialogPositiveClick() {
        // GPS
        // Check for location permission
        Log.i(TAG, "GPS button pressed. Checking permission.");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Location permission not granted");
            permissionLib.requestLocationPermission(mLayout);
        } else {
            Log.i(TAG,
                    "Location permission has already been granted.");
            locationLib.connect();
        }
    }

    @Override
    public void onDialogNegativeClick() {
        // Manual
        selectItem(7, true, true);
    }

    private void startLocationTask() {
        // No location
        DialogFragment newFragment = new LocationDialog();
        newFragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
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
                File.separator + "ephemeris" + File.separator + name;
        File f = new File(p);
        return f.exists();
    }

    @Override
    public void onCopyFinished() {
        copyTask = null;
        startLocationTask();
    }
}

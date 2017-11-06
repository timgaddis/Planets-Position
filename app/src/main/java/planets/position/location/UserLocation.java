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

package planets.position.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import planets.position.BuildConfig;
import planets.position.PlanetsMain;
import planets.position.R;
import planets.position.database.LocationTable;
import planets.position.database.PlanetsDatabase;

public class UserLocation extends AppCompatActivity implements UserLocationDialog.LocationDialogListener {

    private final static int LATITUDE_REQUEST = 100;
    private final static int LONGITUDE_REQUEST = 200;
    private final static int ELEVATION_REQUEST = 300;
    private final static int OFFSET_REQUEST = 400;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 500;
    private static final String TAG = "UserLocation";

    private TextView latitudeText, longitudeText, elevationText, gmtOffsetText,
            editLocText;
    private MenuItem editLoc, saveLoc;
    private UserLocationDialog offsetDialog;
    private PlanetsDatabase planetsDB;
    private int ioffset = -1;
    private double latitude, longitude, elevation, offset;
    private boolean edit = false, startLoc = false;
    private List<String> gmtArray, gmtValues;
    private SharedPreferences settings;
    private FusedLocationProviderClient mFusedLocationClient;
    private Location mLastLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_location);

        Toolbar myToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        myToolbar.setTitle("User Location");

        ActionBar ab = getSupportActionBar();
        assert ab != null;
        ab.setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            // load data passed from main activity
            Bundle b = getIntent().getExtras();
            if (b != null) {
                edit = b.getBoolean("edit");
                startLoc = b.getBoolean("loc");
            }
        } else {
            edit = savedInstanceState.getBoolean("edit");
            startLoc = savedInstanceState.getBoolean("loc");
        }

        if (edit)
            editLocText.setVisibility(View.VISIBLE);

        latitudeText = findViewById(R.id.newLatText);
        longitudeText = findViewById(R.id.newLongText);
        elevationText = findViewById(R.id.newElevationText);
        gmtOffsetText = findViewById(R.id.newGMTOffsetText);
        editLocText = findViewById(R.id.locEditText);

        gmtArray = Arrays.asList(getResources().getStringArray(
                R.array.gmt_array));
        gmtValues = Arrays.asList(getResources().getStringArray(
                R.array.gmt_values));

        settings = getSharedPreferences(PlanetsMain.MAIN_PREFS, 0);
        planetsDB = new PlanetsDatabase(this);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (startLoc)
            startGPS();
        else
            loadLocation();

        latitudeText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edit) {
                    offsetDialog = UserLocationDialog.newInstance(
                            R.string.loc_edit_lat, -1, 0, latitude, LATITUDE_REQUEST);
                    offsetDialog.show(getSupportFragmentManager(), "latitudeDialog");
                }
            }
        });
        longitudeText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edit) {
                    offsetDialog = UserLocationDialog.newInstance(
                            R.string.loc_edit_long, -1, 1, longitude, LONGITUDE_REQUEST);
                    offsetDialog.show(getSupportFragmentManager(), "longitudeDialog");
                }
            }
        });
        elevationText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edit) {
                    offsetDialog = UserLocationDialog.newInstance(
                            R.string.loc_edit_ele, -1, 2, elevation, ELEVATION_REQUEST);
                    offsetDialog.show(getSupportFragmentManager(), "elevationDialog");
                }
            }
        });
        gmtOffsetText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edit) {
                    offsetDialog = UserLocationDialog.newInstance(
                            R.string.loc_gmt, R.array.gmt_array, 3, -1.0, OFFSET_REQUEST);
                    offsetDialog.show(getSupportFragmentManager(), "offsetDialog");
                }
            }
        });

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("edit", edit);
        outState.putBoolean("loc", startLoc);
        if (edit)
            saveLocation();
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.location_menu, menu);
        editLoc = menu.findItem(R.id.action_edit);
        saveLoc = menu.findItem(R.id.action_save);
        if (edit) {
            editLoc.setVisible(false);
            saveLoc.setVisible(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle actionbar item selection
        switch (item.getItemId()) {
            case R.id.action_save:
                edit = false;
                editLocText.setVisibility(View.GONE);
                editLoc.setVisible(true);
                saveLoc.setVisible(false);
                if (saveLocation()) {
                    Toast.makeText(getApplicationContext(),
                            "Location saved.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Location not saved.", Toast.LENGTH_SHORT).show();
                }
                displayLocation();
                return true;
            case R.id.action_edit:
                edit = true;
                editLocText.setVisibility(View.VISIBLE);
                editLoc.setVisible(false);
                saveLoc.setVisible(true);
                return true;
            case R.id.action_gps:
                startGPS();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startGPS() {
        if (!checkPermissions()) {
            requestPermissions();
        } else {
            getLastLocation();
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
        displayLocation();
    }

    private void displayLocation() {
        if (latitude >= 0) {
            latitudeText.setText(String.format(Locale.getDefault(), "%.6f° N", latitude));
        } else {
            latitudeText.setText(String.format(Locale.getDefault(), "%.6f° S", Math.abs(latitude)));
        }
        if (longitude >= 0) {
            longitudeText.setText(String.format(Locale.getDefault(), "%.6f° E", longitude));
        } else {
            longitudeText.setText(String.format(Locale.getDefault(), "%.6f° W", Math.abs(longitude)));
        }
        elevationText.setText(String.format(Locale.getDefault(), "%.1f m", elevation));
        if (ioffset >= 0) {
            gmtOffsetText.setText(gmtArray.get(ioffset));
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

    @Override
    public void onDialogPositiveClick(int id, double value, boolean hemisphere) {
        switch (id) {
            case LATITUDE_REQUEST:
                latitude = value;
                if (hemisphere)
                    latitude *= -1.0;
                break;
            case LONGITUDE_REQUEST:
                longitude = value;
                if (hemisphere)
                    longitude *= -1.0;
                break;
            case ELEVATION_REQUEST:
                elevation = value;
                break;
            default:
                break;
        }
        if (saveLocation()) {
            Toast.makeText(getApplicationContext(),
                    "Location saved.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(),
                    "Location not saved.", Toast.LENGTH_SHORT).show();
        }
        displayLocation();
    }

    @Override
    public void onDialogNegativeClick() {
        displayLocation();
    }

    @Override
    public void onDialogGMTClick(int off) {
        ioffset = off;
        if (ioffset >= 0) {
            offset = Double.parseDouble(gmtValues.get(ioffset));
        } else {
            offset = -1.0;
        }
        if (saveLocation()) {
            Toast.makeText(getApplicationContext(),
                    "Location saved.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(),
                    "Location not saved.", Toast.LENGTH_SHORT).show();
        }
        displayLocation();
    }

    /**
     * Shows a {@link Snackbar} using {@code text}.
     *
     * @param text The Snackbar text.
     */
    private void showSnackbar(final String text) {
        View container = findViewById(R.id.user_main);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
        }
    }

    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        mFusedLocationClient.getLastLocation()
                .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            mLastLocation = task.getResult();
                            latitude = mLastLocation.getLatitude();
                            longitude = mLastLocation.getLongitude();
                            elevation = mLastLocation.getAltitude();
                            offset = Calendar.getInstance().getTimeZone()
                                    .getOffset(mLastLocation.getTime()) / 3600000.0;
                            ioffset = gmtValues.indexOf(offset + "");
                            startLoc = false;
                            if (saveLocation()) {
                                Toast.makeText(getApplicationContext(),
                                        "Location saved.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getApplicationContext(),
                                        "Location not saved.", Toast.LENGTH_SHORT).show();
                            }
                            displayLocation();
                        } else {
                            showSnackbar(getString(R.string.no_location_detected));
                        }
                    }
                });
    }

    private boolean checkPermissions() {
        int permissionStateC = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        int permissionStateF = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return (permissionStateF == PackageManager.PERMISSION_GRANTED) &&
                (permissionStateC == PackageManager.PERMISSION_GRANTED);
    }

    private void startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(UserLocation.this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_PERMISSIONS_REQUEST_CODE);
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");

            showSnackbar(R.string.permission_reason, android.R.string.ok,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            startLocationPermissionRequest();
                        }
                    });

        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            startLocationPermissionRequest();
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted.
                getLastLocation();
            } else {
                // Permission denied.
                showSnackbar(R.string.permission_reason, R.string.action_settings,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }
}

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

package planets.position.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
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
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import planets.position.BuildConfig;
import planets.position.R;
import planets.position.database.LocationTable;
import planets.position.database.PlanetsDatabase;
import planets.position.database.TimeZoneDB;

public class UserLocation extends AppCompatActivity implements UserTimezoneDialog.TimezoneDialogListener, UserCityDialog.CityDialogListener {

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 500;
    private static final String TAG = "UserLocation";

    private TextView latitudeText, longitudeText, elevationText, timezoneText, gmtOffsetText;
    private EditText latitudeEdit, longitudeEdit, elevationEdit, timezoneEdit;
    private Spinner spinnerLat, spinnerLong;
    private LinearLayout layoutEdit, layoutLong, layoutLat;
    private MenuItem editLoc, saveLoc;
    private UserTimezoneDialog timezoneDialog;
    private UserCityDialog cityDialog;
    private TimeZoneDB tzDB;
    private PlanetsDatabase planetsDB;
    private int zoneID;
    private double latitude, longitude, elevation, offset;
    private String zoneName;
    private boolean edit = false, startLoc = false;
    private FusedLocationProviderClient mFusedLocationClient;
    private Location mLastLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_location);
        Button buttonCity, buttonEdit;

        latitudeText = findViewById(R.id.newLatText);
        longitudeText = findViewById(R.id.newLongText);
        elevationText = findViewById(R.id.newElevationText);
        gmtOffsetText = findViewById(R.id.newGMTOffsetText);
        timezoneText = findViewById(R.id.newTimezoneText);
        buttonCity = findViewById(R.id.buttonCity);
        buttonEdit = findViewById(R.id.buttonEdit);
        latitudeEdit = findViewById(R.id.newLatEdit);
        longitudeEdit = findViewById(R.id.newLongEdit);
        elevationEdit = findViewById(R.id.newElevationEdit);
        timezoneEdit = findViewById(R.id.newTimezoneEdit);
        spinnerLat = findViewById(R.id.spinnerLat);
        spinnerLong = findViewById(R.id.spinnerLong);
        layoutEdit = findViewById(R.id.layoutEdit);
        layoutLat = findViewById(R.id.layoutLatEdit);
        layoutLong = findViewById(R.id.layoutLongEdit);

        timezoneEdit.setInputType(InputType.TYPE_NULL);
        timezoneEdit.setFocusable(false);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        tzDB = new TimeZoneDB(getApplicationContext());
        planetsDB = new PlanetsDatabase(getApplicationContext());

        Toolbar myToolbar = findViewById(R.id.toolbar1);
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
            layoutEdit.setVisibility(View.VISIBLE);

        if (startLoc)
            startGPS();
        else
            loadLocation();

        timezoneEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timezoneDialog = UserTimezoneDialog.newInstance();
                timezoneDialog.show(getSupportFragmentManager(), "timezoneDialog");
            }
        });

        buttonCity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // launch UserCityDialog
                cityDialog = UserCityDialog.newInstance();
                cityDialog.show(getSupportFragmentManager(), "cityDialog");
            }
        });

        buttonEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show the EditTexts
                latitudeText.setVisibility(View.GONE);
                layoutLat.setVisibility(View.VISIBLE);
                latitudeEdit.setText(String.valueOf(Math.abs(latitude)));
                if (latitude >= 0)
                    spinnerLat.setSelection(0);
                else
                    spinnerLat.setSelection(1);

                longitudeText.setVisibility(View.GONE);
                layoutLong.setVisibility(View.VISIBLE);
                longitudeEdit.setText(String.valueOf(Math.abs(longitude)));
                if (longitude >= 0)
                    spinnerLong.setSelection(0);
                else
                    spinnerLong.setSelection(1);

                elevationText.setVisibility(View.GONE);
                elevationEdit.setVisibility(View.VISIBLE);
                elevationEdit.setText(String.valueOf(elevation));

                timezoneText.setVisibility(View.GONE);
                timezoneEdit.setVisibility(View.VISIBLE);
                timezoneEdit.setText(zoneName);
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
                layoutEdit.setVisibility(View.GONE);
                editLoc.setVisible(true);
                saveLoc.setVisible(false);

                latitude = Double.parseDouble(latitudeEdit.getText().toString());
                if (spinnerLat.getSelectedItemPosition() == 1)
                    latitude *= -1.0;
                latitudeText.setVisibility(View.VISIBLE);
                layoutLat.setVisibility(View.GONE);

                longitude = Double.parseDouble(longitudeEdit.getText().toString());
                if (spinnerLong.getSelectedItemPosition() == 1)
                    longitude *= -1.0;
                longitudeText.setVisibility(View.VISIBLE);
                layoutLong.setVisibility(View.GONE);

                elevation = Double.parseDouble(elevationEdit.getText().toString());
                elevationText.setVisibility(View.VISIBLE);
                elevationEdit.setVisibility(View.GONE);

                // get timezone
                Calendar c = Calendar.getInstance();
                tzDB.open();
                int off = tzDB.getZoneOffset(zoneID, c.getTimeInMillis() / 1000L);
                tzDB.close();
                offset = off / 3600;

                timezoneText.setVisibility(View.VISIBLE);
                timezoneEdit.setVisibility(View.GONE);

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
                layoutEdit.setVisibility(View.VISIBLE);
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
        // Read location from database
        planetsDB.open();
        Bundle loc = planetsDB.getLocation();
        planetsDB.close();
        latitude = loc.getDouble("latitude");
        longitude = loc.getDouble("longitude");
        elevation = loc.getDouble("elevation");
        offset = loc.getDouble("offset");
        zoneID = loc.getInt("zoneID");
        zoneName = loc.getString("zoneName");
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

        timezoneText.setText(zoneName);
        gmtOffsetText.setText(String.valueOf(offset));
    }

    private boolean saveLocation() {

        long date = Calendar.getInstance().getTimeInMillis();
        // save to database
        ContentValues values = new ContentValues();
        values.put(LocationTable.COLUMN_NAME, "Default");
        values.put(LocationTable.COLUMN_LATITUDE, latitude);
        values.put(LocationTable.COLUMN_LONGITUDE, longitude);
        values.put(LocationTable.COLUMN_TEMP, 0.0);
        values.put(LocationTable.COLUMN_PRESSURE, 0.0);
        values.put(LocationTable.COLUMN_ELEVATION, elevation);
        values.put(LocationTable.COLUMN_DATE, date);
        values.put(LocationTable.COLUMN_OFFSET, offset);
        values.put(LocationTable.COLUMN_ZONE_ID, zoneID);
        values.put(LocationTable.COLUMN_ZONE_NAME, zoneName);

        planetsDB.open();
        planetsDB.eraseTable(LocationTable.TABLE_NAME);
        long row = planetsDB.addLocation(values);
        planetsDB.close();

        return row > -1;
    }

    // UserTimezoneDialog
    @Override
    public void onZoneSelection(int id, String name) {
        zoneID = id;
        zoneName = name;

        timezoneEdit.setText(zoneName);
        Calendar c = Calendar.getInstance();
        tzDB.open();
        int off = tzDB.getZoneOffset(zoneID, c.getTimeInMillis() / 1000L);
        tzDB.close();
        offset = off / 3600;

        if (saveLocation()) {
            Toast.makeText(getApplicationContext(),
                    "Location saved.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(),
                    "Location not saved.", Toast.LENGTH_SHORT).show();
        }
        displayLocation();
    }

    // UserCityDialog
    @Override
    public void onDialogPositiveClick(long id) {
        edit = false;
        layoutEdit.setVisibility(View.GONE);
        editLoc.setVisible(true);
        saveLoc.setVisible(false);
        tzDB.open();
        Bundle data = tzDB.getCityData(id);
        Calendar c = Calendar.getInstance();
        latitude = data.getDouble("lat", 0);
        longitude = data.getDouble("lng", 0);
        elevation = data.getDouble("alt", 0);
        zoneName = data.getString("timezone", "");
        zoneID = tzDB.getZoneID(zoneName);
        int off = tzDB.getZoneOffset(zoneID, c.getTimeInMillis() / 1000L);
        offset = off / 3600;
        tzDB.close();

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

                            Calendar c = Calendar.getInstance();
                            TimeZone t = TimeZone.getDefault();
                            zoneName = t.getID();
                            tzDB.open();
                            zoneID = tzDB.getZoneID(zoneName);
                            int off = tzDB.getZoneOffset(zoneID, c.getTimeInMillis() / 1000L);
                            offset = off / 3600;
                            tzDB.close();

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

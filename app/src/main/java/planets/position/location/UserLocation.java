/*
 * Planet's Position
 * A program to calculate the position of the planets in the night sky based
 * on a given location on Earth.
 * Copyright (c) 2016 Tim Gaddis
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
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import planets.position.FragmentListener;
import planets.position.PlanetsMain;
import planets.position.R;
import planets.position.database.LocationTable;
import planets.position.database.PlanetsDatabase;

public class UserLocation extends Fragment {

    private final static int LATITUDE_REQUEST = 100;
    private final static int LONGITUDE_REQUEST = 200;
    private final static int ELEVATION_REQUEST = 300;
    private final static int OFFSET_REQUEST = 400;

    private FragmentListener mCallbacks;
    private TextView latitudeText, longitudeText, elevationText, gmtOffsetText,
            editLocText;
    private Snackbar mySnackbar;
    private View mLayout;
    private MenuItem editLoc, saveLoc;
    private UserLocationDialog offsetDialog;
    private LocationTask locationTask;
    private LocationHelper locHelper;
    private PlanetsDatabase planetsDB;
    private int ioffset = -1;
    private double latitude, longitude, elevation, offset;
    private boolean edit = false;
    private List<String> gmtArray, gmtValues;
    private SharedPreferences settings;

    public UserLocation() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_user_location, container, false);
        if (mCallbacks != null) {
            mCallbacks.onToolbarTitleChange("User Location", 7);
        }

        latitudeText = (TextView) v.findViewById(R.id.newLatText);
        longitudeText = (TextView) v.findViewById(R.id.newLongText);
        elevationText = (TextView) v.findViewById(R.id.newElevationText);
        gmtOffsetText = (TextView) v.findViewById(R.id.newGMTOffsetText);
        editLocText = (TextView) v.findViewById(R.id.locEditText);
        mLayout = v.findViewById(R.id.user_main);

        gmtArray = Arrays.asList(getResources().getStringArray(
                R.array.gmt_array));
        gmtValues = Arrays.asList(getResources().getStringArray(
                R.array.gmt_values));

        settings = getActivity()
                .getSharedPreferences(PlanetsMain.MAIN_PREFS, 0);

        planetsDB = new PlanetsDatabase(getActivity().getApplicationContext());

        locationTask = (LocationTask) getActivity().getSupportFragmentManager()
                .findFragmentByTag(LocationTask.TAG);
        if (locationTask == null) {
            locationTask = LocationTask.newInstance();
            getActivity().getSupportFragmentManager().beginTransaction()
                    .add(locationTask, LocationTask.TAG)
                    .commit();
        }
        locationTask.setTargetFragment(this, LocationTask.LOCATION_TASK);

        locHelper = (LocationHelper) getActivity().getSupportFragmentManager().
                findFragmentByTag(LocationHelper.TAG);
        if (locHelper == null) {
            locHelper = LocationHelper.newInstance();
            locHelper.setTargetFragment(this, 0);
            getActivity().getSupportFragmentManager().beginTransaction()
                    .add(locHelper, LocationHelper.TAG)
                    .commit();
        } else {
            locHelper.setTargetFragment(this, 0);
        }

        if (savedInstanceState == null) {
            // load bundle from previous activity
            Bundle bundle = getArguments();
            if (bundle != null)
                edit = bundle.getBoolean("edit");
        }

        if (edit)
            editLocText.setVisibility(View.VISIBLE);

        loadLocation();

        latitudeText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edit) {
                    offsetDialog = UserLocationDialog.newInstance(
                            R.string.loc_edit_lat, -1, 0, latitude);
                    offsetDialog.setTargetFragment(UserLocation.this,
                            LATITUDE_REQUEST);
                    offsetDialog.show(getFragmentManager(), "latitudeDialog");
                }
            }
        });
        longitudeText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edit) {
                    offsetDialog = UserLocationDialog.newInstance(
                            R.string.loc_edit_long, -1, 1, longitude);
                    offsetDialog.setTargetFragment(UserLocation.this,
                            LONGITUDE_REQUEST);
                    offsetDialog.show(getFragmentManager(), "longitudeDialog");
                }
            }
        });
        elevationText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edit) {
                    offsetDialog = UserLocationDialog.newInstance(
                            R.string.loc_edit_ele, -1, 2, elevation);
                    offsetDialog.setTargetFragment(UserLocation.this,
                            ELEVATION_REQUEST);
                    offsetDialog.show(getFragmentManager(), "elevationDialog");
                }
            }
        });
        gmtOffsetText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edit) {
                    offsetDialog = UserLocationDialog.newInstance(
                            R.string.loc_gmt, R.array.gmt_array, 3, -1.0);
                    offsetDialog.setTargetFragment(UserLocation.this,
                            OFFSET_REQUEST);
                    offsetDialog.show(getFragmentManager(), "offsetDialog");
                }
            }
        });

        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        if (savedInstanceState != null) {
            edit = savedInstanceState.getBoolean("edit");
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.location_menu, menu);
        // inflater.inflate(R.menu.base_menu, menu);
        editLoc = menu.findItem(R.id.action_edit);
        saveLoc = menu.findItem(R.id.action_save);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (edit) {
            editLoc.setVisible(false);
            saveLoc.setVisible(true);
        }
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
                    Toast.makeText(getActivity().getApplicationContext(),
                            "Location saved.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity().getApplicationContext(),
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
                locHelper.setCheckingPermission(true);
                locHelper.checkLocationPermissions(false);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPause() {
        if (edit)
            saveLocation();
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("edit", edit);
        if (edit)
            saveLocation();
        locationTask.setTargetFragment(null, -1);
        super.onSaveInstanceState(outState);
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
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Decide what to do based on the original request code
        switch (requestCode) {
            case LATITUDE_REQUEST:
                // Enter an latitude value
                if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                    String value = data.getStringExtra("value");
                    boolean south = data.getBooleanExtra("south", false);
                    try {
                        latitude = Double.parseDouble(value);
                        if (south)
                            latitude *= -1;
                    } catch (NumberFormatException ex) {
                        Toast.makeText(getActivity().getApplicationContext(),
                                "Enter a number for the latitude.",
                                Toast.LENGTH_LONG).show();
                    }
                }
                break;
            case LONGITUDE_REQUEST:
                // Enter an longitude value
                if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                    String value = data.getStringExtra("value");
                    boolean west = data.getBooleanExtra("west", false);
                    try {
                        longitude = Double.parseDouble(value);
                        if (west)
                            longitude *= -1;
                    } catch (NumberFormatException ex) {
                        Toast.makeText(getActivity().getApplicationContext(),
                                "Enter a number for the longitude.",
                                Toast.LENGTH_LONG).show();
                    }
                }
                break;
            case ELEVATION_REQUEST:
                // Enter an elevation value
                if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                    String value = data.getStringExtra("value");
                    try {
                        elevation = Double.parseDouble(value);
                    } catch (NumberFormatException ex) {
                        Toast.makeText(getActivity().getApplicationContext(),
                                "Enter a number for the elevation.",
                                Toast.LENGTH_LONG).show();
                    }
                }
                break;
            case OFFSET_REQUEST:
                // Select a value from the GMT offset list
                ioffset = resultCode;
                if (ioffset >= 0) {
                    offset = Double.parseDouble(gmtValues.get(ioffset));
                    gmtOffsetText.setText(gmtArray.get(ioffset));
                } else {
                    offset = -1.0;
                }
                break;
            case LocationTask.LOCATION_TASK:
                mySnackbar.dismiss();
                Bundle b = data.getExtras();
                if (b.getBoolean("locationNull")) {
                    Toast.makeText(getActivity().getApplicationContext(),
                            "Location not found.", Toast.LENGTH_LONG).show();
                } else {
                    latitude = b.getDouble("latitude");
                    longitude = b.getDouble("longitude");
                    elevation = b.getDouble("elevation");
                    offset = b.getDouble("offset");
                    ioffset = gmtValues.indexOf(offset + "");
                }
                locationTask.stop();
                break;
            case LocationHelper.LOCATION_HELPER:
                if (resultCode > 0) {
                    // Location Permission Granted
                    locHelper.setCheckingPermission(false);
                    locHelper.setLocationPermissionDenied(false);
                    startLocationTask();
                } else {
                    // Location Permission Denied
                    if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                            Manifest.permission.ACCESS_FINE_LOCATION)) {
                        Log.i(PlanetsMain.TAG,
                                "Displaying Location permission rationale to provide additional context.");
                        // Show an expanation and try again
                        Snackbar.make(mLayout, R.string.permission_reason, Snackbar.LENGTH_LONG).show();
                        locHelper.checkLocationPermissions(false);
                    } else {
                        locHelper.setCheckingPermission(false);
                        locHelper.setLocationPermissionDenied(true);
                    }
                }
                break;
        }
        if (saveLocation()) {
            Toast.makeText(getActivity().getApplicationContext(),
                    "Location saved.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity().getApplicationContext(),
                    "Location not saved.", Toast.LENGTH_SHORT).show();
        }
        displayLocation();
    }

    private void startLocationTask() {
        mySnackbar = Snackbar.make(mLayout, R.string.location_dialog, Snackbar.LENGTH_INDEFINITE);
        mySnackbar.setActionTextColor(Color.WHITE);
        mySnackbar.setAction(R.string.action_cancel, new MyCancelListener());
        mySnackbar.show();
        locationTask.start(false);
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

}

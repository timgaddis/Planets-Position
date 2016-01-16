/*
 * Copyright (c) 2014. Tim Gaddis
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

package planets.position.Location;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import planets.position.FragmentListener;
import planets.position.PlanetsMain;

public class LocationLib implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    public final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
//    public final static int LOCATION_TASK = 500;

    private GoogleApiClient googleApiClient;
    private GoogleApiAvailability gaa;
    //    private LocationRequest mLocationRequest;
    private Context context;
    private FragmentActivity activity;
    private Location location;
    FragmentListener mListener;

    public LocationLib(Context c, FragmentActivity a) {
        context = c;
        activity = a;
        mListener = (FragmentListener) a;
        googleApiClient = new GoogleApiClient.Builder(c)
                .addApi(LocationServices.API).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();
        LocationRequest mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setNumUpdates(1);
        mLocationRequest.setExpirationDuration(5000);
        gaa = GoogleApiAvailability.getInstance();
    }

    public void connect() {
        Log.d(PlanetsMain.TAG, "LocationLib connect");
        Log.d(PlanetsMain.TAG, "googleApiClient:" + googleApiClient.toString());
        googleApiClient.connect();
    }

    public void disconnect() {
        googleApiClient.disconnect();
    }

    public Location getLocation() {
//        Location loc = null;
        Log.d(PlanetsMain.TAG, "LocationLib getLocation");
        Log.d(PlanetsMain.TAG, "location:" + location);
        return location;
//        Log.d(PlanetsMain.TAG, "fine:" + ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION));
//        Log.d(PlanetsMain.TAG, "coarse:" + ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION));
//        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
//                == PackageManager.PERMISSION_GRANTED &&
//                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
//                        == PackageManager.PERMISSION_GRANTED) {
//            loc = LocationServices.FusedLocationApi
//                    .getLastLocation(googleApiClient);
//        }
//        Log.d(PlanetsMain.TAG, "location:" + loc);
//        return loc;
    }

    public boolean gConnect() {
        Log.d(PlanetsMain.TAG, "gConnect:" + googleApiClient.isConnected());
        return googleApiClient.isConnected();
    }

    public boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode = gaa.isGooglePlayServicesAvailable(context);
        // If Google Play services is available
        if (resultCode == ConnectionResult.SUCCESS) {
            // In debug mode, log the status
            Log.d("Location Updates", "Google Play services is available.");
            // Continue
            return true;
            // Google Play services was not available for some reason
        } else {
            // Get the error code
            // int errorCode = connectionResult.getErrorCode();
            // Get the error dialog from Google Play services
            Dialog errorDialog = gaa.getErrorDialog(activity, resultCode,
                    CONNECTION_FAILURE_RESOLUTION_REQUEST);

            // If Google Play services can provide an error dialog
            if (errorDialog != null) {
                // Create a new DialogFragment for the error dialog
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                // Set the dialog in the DialogFragment
                errorFragment.setDialog(errorDialog);
                // Show the error dialog in the DialogFragment
                errorFragment.show((activity).getSupportFragmentManager(),
                        "Location Updates");
            }
            return false;
        }
    }

    // Define a DialogFragment that displays the error dialog
    public static class ErrorDialogFragment extends DialogFragment {
        // Global field to contain the error dialog
        private Dialog mDialog;

        // Default constructor. Sets the dialog field to null
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        // Set the dialog to display
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        // Return a Dialog to the DialogFragment.
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }

    private void showErrorDialog(int errorCode) {
        // Get the error dialog from Google Play services
        Dialog errorDialog = gaa.getErrorDialog(activity, errorCode,
                CONNECTION_FAILURE_RESOLUTION_REQUEST);

        // If Google Play services can provide an error dialog
        if (errorDialog != null) {

            // Create a new DialogFragment in which to show the error dialog
            ErrorDialogFragment errorFragment = new ErrorDialogFragment();

            // Set the dialog in the DialogFragment
            errorFragment.setDialog(errorDialog);

            // Show the error dialog in the DialogFragment
            errorFragment.show(activity.getSupportFragmentManager(),
                    "LocationError");
        }
    }

    @Override
    public void onConnected(Bundle dataBundle) {
        // Display the connection status
        // Toast.makeText(activity, "Connected", Toast.LENGTH_SHORT).show();
        Log.d(PlanetsMain.TAG, "onConnected:" + googleApiClient.isConnected());
//        Log.d(PlanetsMain.TAG, "onConnected");
        Log.d(PlanetsMain.TAG, "fine:" + ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION));
        Log.d(PlanetsMain.TAG, "coarse:" + ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION));
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
            location = LocationServices.FusedLocationApi
                    .getLastLocation(googleApiClient);
            mListener.onLocationFound(location,0);
        }
    }

    // @Override
    // public void onDisconnected() {
    // // Display the connection status
    // // Toast.makeText(activity, "Disconnected. Please re-connect.",
    // // Toast.LENGTH_SHORT).show();
    // }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // Google Play services can resolve some errors it detects. If the error
        // has a resolution, try sending an Intent to start a Google Play
        // services activity that can resolve error.
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(activity,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
                // Thrown if Google Play services canceled the original
                // PendingIntent
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            // If no resolution is available, display a dialog to the user with
            // the error.
            showErrorDialog(connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location arg0) {
    }

    @Override
    public void onConnectionSuspended(int cause) {
    }
}

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
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.Calendar;

public class LocationTask extends Fragment implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String DIALOG_ERROR = "dialog_error";
    public static final String TAG = "LocationTask";
    public static final int REQUEST_RESOLVE_ERROR = 1001;
    public final static int LOCATION_TASK = 500;

    private boolean resolvingError = false;
    private boolean fromMain = false;
    private GoogleApiClient googleApiClient;
    private static GoogleApiAvailability gaa;
    private LocationCallbackMain callbacksMain;

    public interface LocationCallbackMain {
        void onLocationFoundMain(Bundle data);
    }

    public static LocationTask newInstance() {
        return new LocationTask();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        googleApiClient = new GoogleApiClient.Builder(getActivity())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();
        LocationRequest mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setNumUpdates(1);
        mLocationRequest.setExpirationDuration(5000);
        gaa = GoogleApiAvailability.getInstance();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof LocationCallbackMain) {
            callbacksMain = (LocationCallbackMain) context;
        } else {
            throw new IllegalStateException(
                    "Activity must implement the LocationCallbackMain interface.");
        }
    }

    private void onLocationTaskFound(Location location) {
        Bundle b = new Bundle();
        Intent data = new Intent();
        if (location == null) {
            b.putBoolean("locationNull", true);
        } else {
            Log.d(TAG, "Task onLocationFoundMain, Location found");
            b.putBoolean("locationNull", false);
            b.putDouble("latitude", location.getLatitude());
            b.putDouble("longitude", location.getLongitude());
            b.putDouble("elevation", location.getAltitude());
            b.putDouble("offset", Calendar.getInstance().getTimeZone()
                    .getOffset(location.getTime()) / 3600000.0);
        }
        if (fromMain) {
            // PlanetsMain
            callbacksMain.onLocationFoundMain(b);
        } else {
            // UserLocation
            data.putExtras(b);
            getTargetFragment().onActivityResult(LOCATION_TASK,
                    Activity.RESULT_OK, data);
        }
    }

    public void start(boolean main) {
        fromMain = main;
        googleApiClient.connect();
    }

    public void stop() {
        googleApiClient.disconnect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Location location;
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
            location = LocationServices.FusedLocationApi
                    .getLastLocation(googleApiClient);
        } else {
            location = null;
        }
        onLocationTaskFound(location);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        if (!resolvingError && result.hasResolution()) {
            try {
                resolvingError = true;
                result.startResolutionForResult(getActivity(), REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                start(fromMain);
            }
        } else {
            // Show dialog using GoogleApiAvailability.getErrorDialog()
            showErrorDialog(result.getErrorCode());
            resolvingError = true;
        }
    }

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.setTargetFragment(this, LOCATION_TASK);
        dialogFragment.show(getActivity().getSupportFragmentManager(), "errordialog");
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    private void onDialogDismissed() {
        resolvingError = false;
    }

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() {
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return gaa.getErrorDialog(this.getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((LocationTask) getTargetFragment()).onDialogDismissed();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onLocationChanged(Location location) {
    }

}

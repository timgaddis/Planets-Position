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
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;

import planets.position.util.PermissionUtil;

public class LocationHelper extends Fragment {

    private static final int REQUEST_LOCATION_PERMISSIONS = 10;
    public static final int LOCATION_HELPER = 600;
    public static final String TAG = "LocationHelper";

    private LocationMainCallback mCallback;
    private boolean sLocationPermissionDenied;
    private boolean fromActivity;
    private boolean checkingPermission;

    public interface LocationMainCallback {
        void onLocationPermissionGranted();

        void onLocationPermissionDenied();
    }

    public static LocationHelper newInstance() {
        return new LocationHelper();
    }

    public LocationHelper() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkingPermission = false;
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof LocationMainCallback) {
            mCallback = (LocationMainCallback) context;
        } else {
            throw new IllegalArgumentException("activity must implement LocationHelper.LocationCallbackMain");
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        setTargetFragment(null, -1);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
    }

    public boolean isCheckingPermission() {
        return checkingPermission;
    }

    public void setCheckingPermission(boolean checkingPermission) {
        this.checkingPermission = checkingPermission;
    }

    public void checkLocationPermissions(boolean a) {
        fromActivity = a;
        if (PermissionUtil.hasSelfPermission(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION})) {
            if (fromActivity)
                mCallback.onLocationPermissionGranted();
            else
                getTargetFragment().onActivityResult(LOCATION_HELPER, 1, null);
        } else {
            // UNCOMMENT TO SUPPORT ANDROID M RUNTIME PERMISSIONS
            if (!sLocationPermissionDenied) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSIONS);
            }
        }
    }

    public void setLocationPermissionDenied(boolean LocationPermissionDenied) {
        this.sLocationPermissionDenied = LocationPermissionDenied;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == REQUEST_LOCATION_PERMISSIONS) {
            if (PermissionUtil.verifyPermissions(grantResults)) {
                checkingPermission = false;
                if (fromActivity)
                    mCallback.onLocationPermissionGranted();
                else
                    getTargetFragment().onActivityResult(LOCATION_HELPER, 1, null);
            } else {
                Log.i("BaseActivity", "LOCATION permission was NOT granted.");
                if (fromActivity)
                    mCallback.onLocationPermissionDenied();
                else
                    getTargetFragment().onActivityResult(LOCATION_HELPER, -1, null);
            }

        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

}

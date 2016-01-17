package planets.position.Location;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Calendar;

import planets.position.PermissionLib;
import planets.position.PlanetsMain;
import planets.position.R;

public class LocationTask extends DialogFragment implements LocationLib.LocationTaskCallback {

    private PermissionLib permissionLib;
    private LocationLib locationLib;
    private Context context;
    private FragmentActivity activity;
    private View mLayout;

    public LocationTask() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_location_task, container, false);
        View tv = v.findViewById(R.id.progress_text);
        ((TextView) tv).setText(R.string.location_dialog);
        getDialog().setCanceledOnTouchOutside(false);

        locationLib = new LocationLib(activity.getApplicationContext(), activity, this, 200);
        permissionLib = new PermissionLib((AppCompatActivity) activity);

        start();

        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public void setData(FragmentActivity activity, Context context, View mLayout) {
        this.activity = activity;
        this.context = context;
        this.mLayout = mLayout;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PlanetsMain.REQUEST_LOCATION) {
            // Check if the only required permission has been granted
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(PlanetsMain.TAG, "Location permission has now been granted.");
                Snackbar.make(mLayout, R.string.permision_location,
                        Snackbar.LENGTH_SHORT).show();
                locationLib.connect();
            } else {
                Log.i(PlanetsMain.TAG, "Location permission was NOT granted.");
                Snackbar.make(mLayout, R.string.permision_not_location,
                        Snackbar.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onLocationTaskFound(Location location) {
        if (isResumed())
            dismiss();
        Bundle b = new Bundle();
        Intent data = new Intent();
        if (location == null) {
            b.putBoolean("locationNull", true);
        } else {
            Log.d(PlanetsMain.TAG, "Task onLocationFound, Location found");
            b.putBoolean("locationNull", false);
            b.putDouble("latitude", location.getLatitude());
            b.putDouble("longitude", location.getLongitude());
            b.putDouble("elevation", location.getAltitude());
            b.putDouble("offset", Calendar.getInstance().getTimeZone()
                    .getOffset(location.getTime()) / 3600000.0);
        }
        data.putExtras(b);
        getTargetFragment().onActivityResult(LocationLib.LOCATION_TASK,
                Activity.RESULT_OK, data);
    }

    public void start() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i(PlanetsMain.TAG, "Location permission not granted");
            permissionLib.requestLocationPermission(mLayout);
        } else {
            Log.i(PlanetsMain.TAG,
                    "Location permission has already been granted.");
            locationLib.connect();
        }
    }
}

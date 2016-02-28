package planets.position;

import android.Manifest;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

public class PermissionLib {

    private final AppCompatActivity activity;

    public PermissionLib(AppCompatActivity activity) {
        this.activity = activity;
    }

    public void requestLocationPermission(View mLayout) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            Log.i(PlanetsMain.TAG,
                    "Displaying Location permission rationale to provide additional context.");
            Snackbar.make(mLayout, R.string.alert_string,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.about_ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(activity,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    PlanetsMain.REQUEST_LOCATION);
                        }
                    })
                    .show();
        } else {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PlanetsMain.REQUEST_LOCATION);
        }
    }

}

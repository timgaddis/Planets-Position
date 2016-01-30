package planets.position.solar;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import planets.position.PlanetsMain;
import planets.position.R;

public class SolarEclipseMap extends Fragment {

    private double latitude, longitude, start, end;
    private long eclDate;
    private String eclType;
    private GoogleMap mMap;
    private MapView mapView;
    private TextView overlayTop, overlayBottom;
    private DateFormat mDateFormat;
    private SharedPreferences settings;

    // load c library
    static {
        System.loadLibrary("planets_swiss");
    }

    // c function prototypes
    @SuppressWarnings("JniMissingFunction")
    public native double[] solarMapPos(byte[] eph, double d2);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = getActivity().getSharedPreferences(PlanetsMain.MAIN_PREFS, 0);
        // load bundle from previous activity
        Bundle bundle = getArguments();
        if (bundle != null) {
            latitude = bundle.getDouble("latitude", 0);
            longitude = bundle.getDouble("longitude", 0);
            start = bundle.getDouble("start", 0);
            end = bundle.getDouble("end", 0);
            eclDate = bundle.getLong("date");
            eclType = bundle.getString("type");
        }
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater
                .inflate(R.layout.fragment_solar_map, container, false);

        overlayTop = (TextView) v.findViewById(R.id.overTop);
        overlayBottom = (TextView) v.findViewById(R.id.overBottom);

        mDateFormat = android.text.format.DateFormat
                .getDateFormat(getActivity().getApplicationContext());

        MapsInitializer.initialize(getActivity());

        switch (GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(getActivity())) {
            case ConnectionResult.SUCCESS:
                mapView = (MapView) v.findViewById(R.id.solarMap);
                mapView.onCreate(savedInstanceState);
                setUpMapIfNeeded();
                break;
            case ConnectionResult.SERVICE_MISSING:
                Toast.makeText(getActivity(), "SERVICE MISSING", Toast.LENGTH_SHORT)
                        .show();
                break;
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                Toast.makeText(getActivity(), "UPDATE REQUIRED", Toast.LENGTH_SHORT)
                        .show();
                break;
            default:
                Toast.makeText(
                        getActivity(),
                        GooglePlayServicesUtil
                                .isGooglePlayServicesAvailable(getActivity()),
                        Toast.LENGTH_SHORT).show();
        }
        return v;
    }

    @Override
    public void onResume() {
        if (mapView != null)
            mapView.onResume();
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    private void setUpMapIfNeeded() {
        if (mapView != null) {
            mMap = mapView.getMap();
            setUpMap();
        }
    }

    private void setUpMap() {
        double[] data;
        Calendar gc = new GregorianCalendar();
        final double NUM = 80.0;
        double interval = (end - start) / NUM;
        double date = start + interval;

        gc.setTimeInMillis(eclDate);
        overlayTop.setText(mDateFormat.format(gc.getTime()));
        overlayBottom.setText(String.format("%s Eclipse", eclType));

        LatLng loc = new LatLng(latitude, longitude);
        LatLng pathStart;
        PolylineOptions path = new PolylineOptions();
        MarkerOptions options = new MarkerOptions();
        options.position(loc);
        mMap.addMarker(options);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(loc));

        data = solarMapPos(settings.getString("ephPath", "").getBytes(), start);
        pathStart = new LatLng(data[1], data[0]);

        path.add(pathStart);
        for (int x = 0; x < NUM; x++) {
            data = solarMapPos(settings.getString("ephPath", "").getBytes(), date);
            path.add(new LatLng(data[1], data[0]));
            date += interval;
        }
        mMap.addPolyline(path);
    }

}

package planets.position.solar;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import planets.position.PlanetsMain;
import planets.position.R;

public class SolarEclipseMap extends AppCompatActivity implements OnMapReadyCallback {

    private DateFormat mDateFormat;
    private TextView overlayTop, overlayBottom;
    private double latitude, longitude, start, end;
    private long eclDate;
    private String eclType;
    private SharedPreferences settings;

    // load c library
    static {
        System.loadLibrary("planets_swiss");
    }

    // c function prototypes
    @SuppressWarnings("JniMissingFunction")
    public native double[] solarMapPos(byte[] eph, double d2);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solar_eclipse_map);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        settings = getSharedPreferences(PlanetsMain.MAIN_PREFS, 0);
        mDateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());

        overlayTop = (TextView) findViewById(R.id.overTop);
        overlayBottom = (TextView) findViewById(R.id.overBottom);

        if (savedInstanceState == null) {
            mapFragment.setRetainInstance(true);
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                latitude = extras.getDouble("latitude", 0);
                longitude = extras.getDouble("longitude", 0);
                start = extras.getDouble("start", 0);
                end = extras.getDouble("end", 0);
                eclDate = extras.getLong("date", 0);
                eclType = extras.getString("type", "");
            }
        } else {
            // load data from config change
            latitude = savedInstanceState.getDouble("latitude");
            longitude = savedInstanceState.getDouble("longitude");
            start = savedInstanceState.getDouble("start");
            end = savedInstanceState.getDouble("end");
            eclDate = savedInstanceState.getLong("date");
            eclType = savedInstanceState.getString("type");
        }

        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putDouble("latitude", latitude);
        outState.putDouble("longitude", longitude);
        outState.putDouble("start", start);
        outState.putDouble("end", end);
        outState.putLong("date", eclDate);
        outState.putString("type", eclType);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
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
        googleMap.addMarker(options);
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(loc));

        data = solarMapPos(settings.getString("ephPath", "").getBytes(), start);
        pathStart = new LatLng(data[1], data[0]);

        path.add(pathStart);
        for (int x = 0; x < NUM; x++) {
            data = solarMapPos(settings.getString("ephPath", "").getBytes(), date);
            path.add(new LatLng(data[1], data[0]));
            date += interval;
        }
        googleMap.addPolyline(path);
    }
}

/*
 * Planet's Position
 * A program to calculate the position of the planets in the night sky based
 * on a given location on Earth.
 * Copyright (c) 2019 Tim Gaddis
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

package planets.position.solar;

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

import planets.position.R;

public class SolarEclipseMap extends AppCompatActivity implements OnMapReadyCallback {

    private DateFormat mDateFormat;
    private TextView overlayTop, overlayBottom;
    private double latitude, longitude, start, end;
    private long eclDate;
    private String eclType;

    // load c library
    static {
        System.loadLibrary("planets_swiss");
    }

    // c function prototypes
    public native double[] solarMapPos(double d2);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solar_eclipse_map);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mDateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());

        overlayTop = findViewById(R.id.overTop);
        overlayBottom = findViewById(R.id.overBottom);

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

        data = solarMapPos(start);
        pathStart = new LatLng(data[1], data[0]);

        path.add(pathStart);
        for (int x = 0; x < NUM; x++) {
            data = solarMapPos(date);
            path.add(new LatLng(data[1], data[0]));
            date += interval;
        }
        googleMap.addPolyline(path);
    }
}

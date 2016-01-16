package planets.position.Location;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import planets.position.FragmentListener;
import planets.position.PlanetsMain;
import planets.position.R;

public class UserLocation extends Fragment {

    private FragmentListener mCallbacks;
    private TextView latitudeText, longitudeText, elevationText, gmtOffsetText,
            editLocText;
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
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_user_location, container, false);
        if (mCallbacks != null) {
            mCallbacks.onToolbarTitleChange("User Location", 5);
        }

        latitudeText = (TextView) v.findViewById(R.id.newLatLabel);
        longitudeText = (TextView) v.findViewById(R.id.newLongText);
        elevationText = (TextView) v.findViewById(R.id.newElevationText);
        gmtOffsetText = (TextView) v.findViewById(R.id.newGMTOffsetText);
        editLocText = (TextView) v.findViewById(R.id.locEditText);

        gmtArray = Arrays.asList(getResources().getStringArray(
                R.array.gmt_array));
        gmtValues = Arrays.asList(getResources().getStringArray(
                R.array.gmt_values));

        settings = getActivity()
                .getSharedPreferences(PlanetsMain.LOC_PREFS, 0);

        if (savedInstanceState == null) {
            // load bundle from previous activity
            Bundle bundle = getArguments();
            if (bundle != null)
                edit = bundle.getBoolean("edit");
        }

        if (edit)
            editLocText.setVisibility(View.VISIBLE);

        loadLocation();

        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (savedInstanceState != null) {
            edit = savedInstanceState.getBoolean("edit");
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
        super.onSaveInstanceState(outState);
    }

    private void loadLocation() {

        if (settings.contains("latitude")) {
            // read location from Shared Preferences
            latitude = settings.getFloat("latitude", 0);
            longitude = settings.getFloat("longitude", 0);
            elevation = settings.getFloat("elevation", 0);
            offset = settings.getFloat("offset", 0);
            ioffset = settings.getInt("ioffset", 15);
        }
        displayLocation();
    }

    private void displayLocation() {
        if (latitude >= 0) {
            latitudeText.setText(String.format("%.6f° N", latitude));
        } else {
            latitudeText.setText(String.format("%.6f° S", Math.abs(latitude)));
        }
        if (longitude >= 0) {
            longitudeText.setText(String.format("%.6f° E", longitude));
        } else {
            longitudeText.setText(String.format("%.6f° W", Math.abs(longitude)));
        }
        elevationText.setText(String.format("%.1f m", elevation));
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
        return out;
        // save to database
//        ContentValues values = new ContentValues();
//        values.put(LocationTable.COLUMN_LATITUDE, latitude);
//        values.put(LocationTable.COLUMN_LONGITUDE, longitude);
//        values.put(LocationTable.COLUMN_TEMP, 0.0);
//        values.put(LocationTable.COLUMN_PRESSURE, 0.0);
//        values.put(LocationTable.COLUMN_ELEVATION, elevation);
//        values.put(LocationTable.COLUMN_DATE, date);
//        values.put(LocationTable.COLUMN_OFFSET, offset);
//        values.put(LocationTable.COLUMN_IOFFSET, ioffset);
//
//        int rows = cr.update(Uri.withAppendedPath(
//                        PlanetsContentProvider.LOCATION_URI, String.valueOf(0)),
//                values, null, null);
//        return rows == 1 && out;
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

}

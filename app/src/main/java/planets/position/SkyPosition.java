package planets.position;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import planets.position.Util.JDUTC;
import planets.position.Util.PlanetDatePicker;
import planets.position.Util.PlanetTimePicker;
import planets.position.Util.RiseSet;


public class SkyPosition extends Fragment {

    Spinner spinner;
    private Button timeButton, dateButton;
    private TextView pRAText, pDecText, pMagText, pRiseText, pSetText;
    private TextView pAzText, pAltText, pBelowText, pDistText;
    private int mHour, mMinute, mDay, mMonth, mYear, planetNum = 0;
    private DateFormat mDateFormat, mTimeFormat;
    private double latitude, longitude, elevation;
    private double offset;
    private double[] g = new double[3];
    private JDUTC jdUTC;
    private RiseSet riseSet;
    private PlanetDatePicker datePickerFragment;
    private PlanetTimePicker timePickerFragment;
    private SharedPreferences settings;
    private FragmentListener mCallbacks;

    public SkyPosition() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_sky_position, container, false);

        spinner = (Spinner) v.findViewById(R.id.spinner1);
        timeButton = (Button) v.findViewById(R.id.timeButton);
        dateButton = (Button) v.findViewById(R.id.dateButton);
        pAzText = (TextView) v.findViewById(R.id.pos_az_text);
        pAltText = (TextView) v.findViewById(R.id.pos_alt_text);
        pRAText = (TextView) v.findViewById(R.id.pos_ra_text);
        pDecText = (TextView) v.findViewById(R.id.pos_dec_text);
        pBelowText = (TextView) v.findViewById(R.id.pos_below_text);
        pDistText = (TextView) v.findViewById(R.id.pos_dis_text);
        pMagText = (TextView) v.findViewById(R.id.pos_mag_text);
        pRiseText = (TextView) v.findViewById(R.id.pos_riseTime_text);
        pSetText = (TextView) v.findViewById(R.id.pos_setTime_text);
        jdUTC = new JDUTC();
        riseSet = new RiseSet();

        mDateFormat = android.text.format.DateFormat
                .getDateFormat(getActivity().getApplicationContext());
        mTimeFormat = android.text.format.DateFormat
                .getTimeFormat(getActivity().getApplicationContext());

        settings = getActivity()
                .getSharedPreferences(PlanetsMain.LOC_PREFS, 0);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                ((AppCompatActivity) getActivity()).getSupportActionBar()
                        .getThemedContext(), R.array.planets_array,
                R.layout.spinner_planets);
        adapter.setDropDownViewResource(R.layout.spinner_planets_dropdown);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                planetNum = position;
                computeLocation();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        timeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timePickerFragment = new PlanetTimePicker();
                timePickerFragment.setData(mHour, mMinute);
                timePickerFragment.setTargetFragment(SkyPosition.this, 50);
                timePickerFragment.setStyle(DialogFragment.STYLE_NORMAL, 0);
                timePickerFragment.show(getFragmentManager(),
                        "timePickerDialog");

            }
        });

        dateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                datePickerFragment = new PlanetDatePicker();
                datePickerFragment.setData(mYear, mMonth, mDay);
                datePickerFragment.setTargetFragment(SkyPosition.this, 50);
                datePickerFragment.setStyle(DialogFragment.STYLE_NORMAL, 0);
                datePickerFragment.show(getFragmentManager(),
                        "datePickerDialog");

            }
        });

        loadLocation();
        g[1] = latitude;
        g[0] = longitude;
        g[2] = elevation;

        riseSet.setLocation(latitude, longitude, elevation);

        if (savedInstanceState == null) {
            // get the current date, time
            final Calendar c = Calendar.getInstance();
            mHour = c.get(Calendar.HOUR_OF_DAY);
            mMinute = c.get(Calendar.MINUTE);
            mYear = c.get(Calendar.YEAR);
            mMonth = c.get(Calendar.MONTH);
            mDay = c.get(Calendar.DAY_OF_MONTH);
        } else {
            mHour = savedInstanceState.getInt("hour");
            mMinute = savedInstanceState.getInt("minute");
            mYear = savedInstanceState.getInt("year");
            mMonth = savedInstanceState.getInt("month");
            mDay = savedInstanceState.getInt("day");
            planetNum = savedInstanceState.getInt("planetNum");
        }

        updateDisplay();
        computeLocation();

        if (mCallbacks != null) {
            mCallbacks.onToolbarTitleChange("Sky Position", 3);
        }

        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
//        inflater.inflate(R.menu.live_menu, menu);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("hour", mHour);
        outState.putInt("minute", mMinute);
        outState.putInt("day", mDay);
        outState.putInt("month", mMonth);
        outState.putInt("year", mYear);
        outState.putInt("planetNum", planetNum);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 50) {
            switch (resultCode) {
                case 60:
                    // Date picker set button
                    mYear = data.getIntExtra("year", 0);
                    mMonth = data.getIntExtra("month", 0);
                    mDay = data.getIntExtra("day", 0);
                    updateDisplay();
                    computeLocation();
                    break;
                case 65:
                    // Time picker set button
                    mHour = data.getIntExtra("hour", 0);
                    mMinute = data.getIntExtra("minute", 0);
                    updateDisplay();
                    computeLocation();
                    break;
                case 70:
                    // Cancel button
                    break;
            }
        }
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

    /**
     * Get location from Shared Preferences
     */
    private void loadLocation() {
        // read location from Shared Preferences
        latitude = settings.getFloat("latitude", 0);
        longitude = settings.getFloat("longitude", 0);
        elevation = settings.getFloat("elevation", 0);
        offset = settings.getFloat("offset", 0);
    }

    // updates the date and time in the Buttons
    private void updateDisplay() {
        Calendar gc = new GregorianCalendar(mYear, mMonth, mDay, mHour,
                mMinute, 0);
        dateButton.setText(mDateFormat.format(gc.getTime()));
        timeButton.setText(mTimeFormat.format(gc.getTime()));
    }

    private void computeLocation() {
        if (planetNum >= 0 && planetNum < 10) {
//            double jd=2457408.5;
            double[] data;
//            double ra, dec, t, d;
            int m;
            Calendar utc;

            pRAText.setText(String.format("%d", planetNum));

            utc = new GregorianCalendar(mYear, mMonth, mDay, mHour, mMinute, 0);
            m = (int) (offset * 60);
            utc.add(Calendar.MINUTE, m * -1);

            data = jdUTC.utcjd(utc.get(Calendar.MONTH) + 1,
                    utc.get(Calendar.DAY_OF_MONTH), utc.get(Calendar.YEAR),
                    utc.get(Calendar.HOUR_OF_DAY), utc.get(Calendar.MINUTE),
                    utc.get(Calendar.SECOND));

            // jdTT = data[0];
            // jdUT = data[1];

            if (data == null) {
                Log.e("Date error", "pos date error");
//                return;
            } else {
                pRiseText.setText(String.format("%f", data[0]));
                pSetText.setText(String.format("%f", data[1]));
            }

//            pSetText.setText(String.format("%s\n%s", mDateFormat.format(utc.getTime()),
//                    mTimeFormat.format(utc.getTime())));
        }
    }
}

/*
 * Planet's Position
 * A program to calculate the position of the planets in the night sky based
 * on a given location on Earth.
 * Copyright (c) 2016 Tim Gaddis
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

package planets.position;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import planets.position.util.JDUTC;
import planets.position.util.PlanetDatePicker;
import planets.position.util.PlanetTimePicker;
import planets.position.util.PositionFormat;
import planets.position.util.RiseSet;

public class SkyPosition extends Fragment {

    private static final int PLANETS_DIALOG = 100;
    private static final int TIME_DIALOG = 200;
    private static final int DATE_DIALOG = 300;

    private Button nameButton, timeButton, dateButton;
    private TextView pRAText, pDecText, pMagText, pRiseText, pSetText;
    private TextView pAzText, pAltText, pBelowText, pDistText;
    private int mHour, mMinute, mDay, mMonth, mYear, planetNum = 0;
    private DateFormat mDateFormat, mTimeFormat;
    private double latitude, longitude, elevation;
    private double offset;
    private final double[] g = new double[3];
    private List<String> planetsArray;
    private JDUTC jdUTC;
    private RiseSet riseSet;
    private PlanetDialog planetDialog;
    private PlanetDatePicker datePickerFragment;
    private PlanetTimePicker timePickerFragment;
    private SharedPreferences settings;
    private FragmentListener mCallbacks;
    private PositionFormat pf;

    // load c library
    static {
        System.loadLibrary("planets_swiss");
    }

    // c function prototype
    @SuppressWarnings("JniMissingFunction")
    public native double[] planetPosData(byte[] eph, double d1, double d2, int p,
                                         double[] loc, double press, double temp);

    public SkyPosition() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_sky_position, container, false);

        nameButton = (Button) v.findViewById(R.id.nameButton);
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
        pf = new PositionFormat(getActivity());
        planetsArray = Arrays.asList(getResources().getStringArray(R.array.planets_array));

        mDateFormat = android.text.format.DateFormat
                .getDateFormat(getActivity().getApplicationContext());
        mTimeFormat = android.text.format.DateFormat
                .getTimeFormat(getActivity().getApplicationContext());

        settings = getActivity()
                .getSharedPreferences(PlanetsMain.MAIN_PREFS, 0);

        riseSet = new RiseSet(settings.getString("ephPath", ""));

        nameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                planetDialog = PlanetDialog.newInstance(R.array.planets_array, R.string.planet_select);
                planetDialog.setTargetFragment(SkyPosition.this, PLANETS_DIALOG);
                planetDialog.show(getActivity().getSupportFragmentManager(), "planetDialog");
            }
        });

        timeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timePickerFragment = new PlanetTimePicker();
                timePickerFragment.setData(mHour, mMinute);
                timePickerFragment.setTargetFragment(SkyPosition.this, TIME_DIALOG);
                timePickerFragment.setStyle(DialogFragment.STYLE_NORMAL, 0);
                timePickerFragment.show(getFragmentManager(), "timePickerDialog");

            }
        });

        dateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                datePickerFragment = new PlanetDatePicker();
                datePickerFragment.setData(mYear, mMonth, mDay);
                datePickerFragment.setTargetFragment(SkyPosition.this, DATE_DIALOG);
                datePickerFragment.setStyle(DialogFragment.STYLE_NORMAL, 0);
                datePickerFragment.show(getFragmentManager(), "datePickerDialog");

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
        nameButton.setText(planetsArray.get(planetNum));

        if (mCallbacks != null) {
            mCallbacks.onToolbarTitleChange("Sky Position", 5);
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
        inflater.inflate(R.menu.live_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_live_position:
                // launch live position activity
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                LivePosition livePos = new LivePosition();
                Bundle args = new Bundle();
                args.putInt("planetNum", planetNum);
                livePos.setArguments(args);
                ft.replace(R.id.content_frame, livePos, "livePosition");
                ft.addToBackStack(null);
                ft.commit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
        switch (requestCode) {
            case DATE_DIALOG:
                // Date picker set button
                mYear = data.getIntExtra("year", 0);
                mMonth = data.getIntExtra("month", 0);
                mDay = data.getIntExtra("day", 0);
                updateDisplay();
                computeLocation();
                break;
            case TIME_DIALOG:
                // Time picker set button
                mHour = data.getIntExtra("hour", 0);
                mMinute = data.getIntExtra("minute", 0);
                updateDisplay();
                computeLocation();
                break;
            case PLANETS_DIALOG:
                nameButton.setText(planetsArray.get(resultCode));
                planetNum = resultCode;
                computeLocation();
                break;
            default:
                // Cancel button
                break;
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
            double[] data;
            double ra, dec, t, d;
            int m;
            Calendar utc;

            utc = new GregorianCalendar(mYear, mMonth, mDay, mHour, mMinute, 0);
            m = (int) (offset * 60);
            utc.add(Calendar.MINUTE, m * -1);

            data = jdUTC.utcjd(utc.get(Calendar.MONTH) + 1,
                    utc.get(Calendar.DAY_OF_MONTH), utc.get(Calendar.YEAR),
                    utc.get(Calendar.HOUR_OF_DAY), utc.get(Calendar.MINUTE),
                    utc.get(Calendar.SECOND));

            if (data == null) {
                Log.e("Date error", "pos date error");
                return;
            }
            // jdTT = data[0];
            // jdUT = data[1];
            d = data[1];

            data = planetPosData(settings.getString("ephPath", "").getBytes(), data[0], data[1],
                    planetNum, g, 0.0, 0.0);
            if (data == null) {
                Log.e("Position error", "planetPosData error");
                return;
            }
            ra = data[0];
            dec = data[1];

            // convert ra to hours
            ra = ra / 15;

            pRAText.setText(pf.formatRA(ra));
            pDecText.setText(pf.formatDec(dec));
            pAzText.setText(pf.formatAZ(data[3]));
            pAltText.setText(pf.formatALT(data[4]));

            if (planetNum == 1)
                pDistText.setText(String.format(Locale.getDefault(), "%.4f AU", data[2]));
            else
                pDistText.setText(String.format(Locale.getDefault(), "%.2f AU", data[2]));
            pMagText.setText(String.format(Locale.getDefault(), "%.2f", data[5]));

            t = riseSet.getSet(d, planetNum);
            if (t < 0) {
                Log.e("Position error", "planetPosData set error");
                return;
            }
            utc.setTimeInMillis(jdUTC.jdmills(t, offset));
            pSetText.setText(String.format("%s %s", mDateFormat.format(utc.getTime()),
                    mTimeFormat.format(utc.getTime())));

            t = riseSet.getRise(d, planetNum);
            if (t < 0) {
                Log.e("Position error", "planetPosData rise error");
                return;
            }
            utc.setTimeInMillis(jdUTC.jdmills(t, offset));
            pRiseText.setText(String.format("%s %s", mDateFormat.format(utc.getTime()),
                    mTimeFormat.format(utc.getTime())));

            if (data[4] <= 0.0) {
                // below horizon
                pBelowText.setVisibility(View.VISIBLE);
            } else {
                // above horizon
                pBelowText.setVisibility(View.GONE);
            }
        }
    }
}

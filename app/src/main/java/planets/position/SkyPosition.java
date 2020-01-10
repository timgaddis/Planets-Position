/*
 * Planet's Position
 * A program to calculate the position of the planets in the night sky based
 * on a given location on Earth.
 * Copyright (c) 2020 Tim Gaddis
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import planets.position.database.TimeZoneDB;
import planets.position.util.JDUTC;
import planets.position.util.PlanetDatePicker;
import planets.position.util.PlanetTimePicker;
import planets.position.util.PositionFormat;
import planets.position.util.RiseSet;

class SkyPosition extends Fragment {

    private static final int TIME_DIALOG = 200;
    private static final int DATE_DIALOG = 300;

    private Button timeButton, dateButton;
    private TextView pRAText, pDecText, pMagText, pRiseText, pSetText;
    private TextView pAzText, pAltText, pBelowText, pDistText, pTransitText;
    private int mHour, mMinute, mDay, mMonth, mYear, zoneID, planetNum = 0;
    private DateFormat mDateFormat, mTimeFormat;
    private double offset;
    private final double[] g = new double[3];
    private JDUTC jdUTC;
    private RiseSet riseSet;
    private TimeZoneDB tzDB;
    private PlanetDatePicker datePickerFragment;
    private PlanetTimePicker timePickerFragment;
    private FragmentListener mCallbacks;
    private SharedPreferences settings;
    private PositionFormat pf;

    // load c library
    static {
        System.loadLibrary("planets_swiss");
    }

    // c function prototype
    public native double[] planetPosData(double d1, double d2, int p, double[] loc);

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_sky_position, container, false);

        Spinner planetsSpinner = v.findViewById(R.id.planetsSpinner);
        timeButton = v.findViewById(R.id.timeButton);
        dateButton = v.findViewById(R.id.dateButton);
        pAzText = v.findViewById(R.id.pos_az_text);
        pAltText = v.findViewById(R.id.pos_alt_text);
        pRAText = v.findViewById(R.id.pos_ra_text);
        pDecText = v.findViewById(R.id.pos_dec_text);
        pBelowText = v.findViewById(R.id.pos_below_text);
        pDistText = v.findViewById(R.id.pos_dis_text);
        pMagText = v.findViewById(R.id.pos_mag_text);
        pRiseText = v.findViewById(R.id.pos_riseTime_text);
        pSetText = v.findViewById(R.id.pos_setTime_text);
        pTransitText = v.findViewById(R.id.pos_transitTime_text);
        jdUTC = new JDUTC();
        pf = new PositionFormat(getActivity());

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.planets_array, R.layout.spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_drop_item);
        planetsSpinner.setAdapter(adapter);

        planetsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
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
                timePickerFragment.setTargetFragment(SkyPosition.this, TIME_DIALOG);
                timePickerFragment.setStyle(DialogFragment.STYLE_NORMAL, 0);
                if (getFragmentManager() != null) {
                    timePickerFragment.show(getFragmentManager(), "timePickerDialog");
                }

            }
        });

        dateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                datePickerFragment = new PlanetDatePicker();
                datePickerFragment.setData(mYear, mMonth, mDay);
                datePickerFragment.setTargetFragment(SkyPosition.this, DATE_DIALOG);
                datePickerFragment.setStyle(DialogFragment.STYLE_NORMAL, 0);
                if (getFragmentManager() != null) {
                    datePickerFragment.show(getFragmentManager(), "datePickerDialog");
                }

            }
        });

        loadLocation();
        riseSet = new RiseSet(g);

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
            mCallbacks.onToolbarTitleChange("Sky Position", 5);
        }

        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDateFormat = android.text.format.DateFormat
                .getDateFormat(getActivity().getApplicationContext());
        mTimeFormat = android.text.format.DateFormat
                .getTimeFormat(getActivity().getApplicationContext());
        settings = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        tzDB = new TimeZoneDB(getActivity().getApplicationContext());
        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.live_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_live_position) {// launch live position activity
            FragmentTransaction ft = null;
            if (getFragmentManager() != null) {
                ft = getFragmentManager().beginTransaction();
            }
            LivePosition livePos = new LivePosition();
            Bundle args = new Bundle();
            args.putInt("planetNum", planetNum);
            livePos.setArguments(args);
            if (ft != null) {
                ft.replace(R.id.content_frame, livePos, "livePosition");
                ft.addToBackStack(null);
                ft.commit();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
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
            default:
                break;
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
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

    private void loadLocation() {
        // read location from Shared Preferences
        g[1] = settings.getFloat("latitude", 0);
        g[0] = settings.getFloat("longitude", 0);
        g[2] = settings.getFloat("elevation", 0);
        offset = settings.getFloat("offset", 0);
        zoneID = settings.getInt("zoneID", 0);
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
            Calendar utc;

            utc = new GregorianCalendar(mYear, mMonth, mDay, mHour, mMinute, 0);
            tzDB.open();
            int off = tzDB.getZoneOffset(zoneID, utc.getTimeInMillis() / 1000L);
            offset = off / 3600.0;
            tzDB.close();
            utc.add(Calendar.MINUTE, (int) (offset * -60));

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

            data = planetPosData(data[0], data[1], planetNum, g);
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
            utc.setTimeInMillis(jdUTC.jdmills(t, offset * 60.0));
            pSetText.setText(String.format("%s\n%s", mDateFormat.format(utc.getTime()),
                    mTimeFormat.format(utc.getTime())));

            t = riseSet.getRise(d, planetNum);
            if (t < 0) {
                Log.e("Position error", "planetPosData rise error");
                return;
            }
            utc.setTimeInMillis(jdUTC.jdmills(t, offset * 60.0));
            pRiseText.setText(String.format("%s\n%s", mDateFormat.format(utc.getTime()),
                    mTimeFormat.format(utc.getTime())));

            t = riseSet.getTransit(d, planetNum);
            if (t < 0) {
                Log.e("Position error", "planetPosData transit error");
                return;
            }
            utc.setTimeInMillis(jdUTC.jdmills(t, offset * 60.0));
            pTransitText.setText(String.format("%s\n%s", mDateFormat.format(utc.getTime()),
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

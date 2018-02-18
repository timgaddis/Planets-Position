/*
 * Planet's Position
 * A program to calculate the position of the planets in the night sky based
 * on a given location on Earth.
 * Copyright (c) 2018 Tim Gaddis
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import planets.position.util.JDUTC;
import planets.position.util.PositionFormat;

public class LivePosition extends Fragment {

    private TextView pDateText, pTimeText, pRAText, pDecText,
            pMagText, pRiseText, pRise, pAzText, pAltText, pDistText,
            pBelowText, pTransitText;
    private int planetNum = 0;
    private DateFormat mDateFormat, mTimeFormat;
    private double offset;
    private final double[] g = new double[3];
    private Calendar utc;
    private JDUTC jdUTC;
    private PositionFormat pf;
    private SharedPreferences settings;
    private FragmentListener mCallbacks;
    private Intent intent;
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateUI(intent);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        intent = new Intent(getActivity().getApplicationContext(),
                LivePositionService.class);

        settings = getActivity()
                .getSharedPreferences(PlanetsMain.MAIN_PREFS, 0);

        if (savedInstanceState == null) {
            // load data passed from Sky Position activity
            Bundle bundle = getArguments();
            if (bundle != null) {
                planetNum = bundle.getInt("planetNum");
            }
        } else {
            // load data from config change
            planetNum = savedInstanceState.getInt("planetNum");
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        TextView pNameText;
        List<String> planetNames;
        View v = inflater.inflate(R.layout.fragment_live_position, container,
                false);
        pNameText = v.findViewById(R.id.live_name_text);
        pDateText = v.findViewById(R.id.live_date_text);
        pTimeText = v.findViewById(R.id.live_time_text);
        pAzText = v.findViewById(R.id.live_az_text);
        pAltText = v.findViewById(R.id.live_alt_text);
        pRAText = v.findViewById(R.id.live_ra_text);
        pDecText = v.findViewById(R.id.live_dec_text);
        pDistText = v.findViewById(R.id.live_dis_text);
        pMagText = v.findViewById(R.id.live_mag_text);
        pRiseText = v.findViewById(R.id.live_riseTime_text);
        pRise = v.findViewById(R.id.live_riseTime);
        pTransitText = v.findViewById(R.id.live_transitTime_text);
        pBelowText = v.findViewById(R.id.live_below_text);
        pf = new PositionFormat(getActivity());
        jdUTC = new JDUTC();
        mDateFormat = android.text.format.DateFormat
                .getDateFormat(getActivity().getApplicationContext());
        mTimeFormat = android.text.format.DateFormat
                .getTimeFormat(getActivity().getApplicationContext());
        planetNames = Arrays.asList(getResources().getStringArray(
                R.array.planets_array));
        pNameText.setText(planetNames.get(planetNum));
        utc = Calendar.getInstance();

        if (mCallbacks != null) {
            mCallbacks.onToolbarTitleChange("Real Time Position", 0);
        }

        loadLocation();

        return v;
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
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("planetNum", planetNum);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadLocation();
        intent.putExtra("offset", offset);
        intent.putExtra("latitude", g[1]);
        intent.putExtra("longitude", g[0]);
        intent.putExtra("elevation", g[2]);
        intent.putExtra("planetNum", planetNum);
        getActivity().getApplicationContext().startService(intent);
        getActivity().getApplicationContext().registerReceiver(
                broadcastReceiver,
                new IntentFilter(LivePositionService.BROADCAST_ACTION));
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().getApplicationContext().unregisterReceiver(
                broadcastReceiver);
        getActivity().getApplicationContext().stopService(intent);
    }

    private void loadLocation() {
        // read location from Shared Preferences
        g[1] = settings.getFloat("latitude", 0);
        g[0] = settings.getFloat("longitude", 0);
        g[2] = settings.getFloat("elevation", 0);
        offset = settings.getFloat("offset", 0);
    }

    private void updateUI(Intent intent) {
        double[] data;
        double riseT, setT, transitT, ra;
        Calendar c = Calendar.getInstance();
        data = intent.getDoubleArrayExtra("data");
        riseT = intent.getDoubleExtra("riseT", 0.0);
        setT = intent.getDoubleExtra("setT", 0.0);
        transitT = intent.getDoubleExtra("transit", 0.0);
        pDateText.setText(mDateFormat.format(c.getTime()));
        pTimeText.setText(mTimeFormat.format(c.getTime()));
        // convert ra to hours
        ra = data[0] / 15;
        pRAText.setText(pf.formatRA(ra));
        pDecText.setText(pf.formatDec(data[1]));
        pAzText.setText(pf.formatAZ(data[3]));
        pAltText.setText(pf.formatALT(data[4]));
        if (planetNum == 1)
            pDistText.setText(String.format(Locale.getDefault(), "%.4f AU", data[2]));
        else
            pDistText.setText(String.format(Locale.getDefault(), "%.2f AU", data[2]));
        pMagText.setText(String.format(Locale.getDefault(), "%.2f", data[5]));
        if (data[4] <= 0) {
            pRise.setText(R.string.data_rise);
            pBelowText.setVisibility(View.VISIBLE);
            utc.setTimeInMillis(jdUTC.jdmills(riseT, offset));
        } else {
            pRise.setText(R.string.data_set);
            pBelowText.setVisibility(View.GONE);
            utc.setTimeInMillis(jdUTC.jdmills(setT, offset));
        }
        pRiseText.setText(String.format("%s  %s", mDateFormat.format(utc.getTime()),
                mTimeFormat.format(utc.getTime())));

        utc.setTimeInMillis(jdUTC.jdmills(transitT, offset));
        pTransitText.setText(String.format("%s  %s", mDateFormat.format(utc.getTime()),
                mTimeFormat.format(utc.getTime())));
    }
}

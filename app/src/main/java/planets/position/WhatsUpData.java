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

package planets.position;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;

import planets.position.database.PlanetsDatabase;
import planets.position.database.TimeZoneDB;
import planets.position.util.JDUTC;
import planets.position.util.PositionFormat;

public class WhatsUpData extends Fragment {

    private TextView pRAText, pDecText, pMagText, pSetText, pAzText, pSet;
    private TextView pAltText, pDistText, pNameText, pDate, pTime, pTransitText;
    private long planetNum = 0, lastUpdate = 0;
    private int zoneID;
    private DateFormat mDateFormat, mTimeFormat;
    private PlanetsDatabase planetsDB;
    private FragmentListener mCallbacks;
    private PositionFormat pf;
    private JDUTC jdUTC;
    private TimeZoneDB tzDB;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_planet_data, container,
                false);
        pNameText = v.findViewById(R.id.data_name_text);
        pAzText = v.findViewById(R.id.data_az_text);
        pAltText = v.findViewById(R.id.data_alt_text);
        pRAText = v.findViewById(R.id.data_ra_text);
        pDecText = v.findViewById(R.id.data_dec_text);
        pDistText = v.findViewById(R.id.data_dis_text);
        pMagText = v.findViewById(R.id.data_mag_text);
        pSetText = v.findViewById(R.id.data_setTime_text);
        pSet = v.findViewById(R.id.data_setTime);
        pDate = v.findViewById(R.id.data_date_text);
        pTime = v.findViewById(R.id.data_time_text);
        pTransitText = v.findViewById(R.id.data_transitTime_text);

        planetsDB = new PlanetsDatabase(getActivity().getApplicationContext());

        if (mCallbacks != null) {
            mCallbacks.onToolbarTitleChange("Rise / Set", 6);
        }

        if (savedInstanceState != null) {
            // load data from config change
            planetNum = savedInstanceState.getLong("planetNum");
            lastUpdate = savedInstanceState.getLong("dateTime");
            zoneID = savedInstanceState.getInt("zoneID");
        } else {
            // load bundle from previous activity
            Bundle bundle = getArguments();
            if (bundle != null) {
                planetNum = bundle.getLong("planetNum");
                lastUpdate = bundle.getLong("dateTime");
                zoneID = bundle.getInt("zoneID");
            }
        }
        loadPlanet();
        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        jdUTC = new JDUTC();
        pf = new PositionFormat(getActivity());
        tzDB = new TimeZoneDB(getActivity().getApplicationContext());
        mDateFormat = android.text.format.DateFormat
                .getDateFormat(getActivity().getApplicationContext());
        mTimeFormat = android.text.format.DateFormat
                .getTimeFormat(getActivity().getApplicationContext());
        setHasOptionsMenu(true);
        setRetainInstance(true);
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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putLong("planetNum", planetNum);
        outState.putLong("dateTime", lastUpdate);
        outState.putInt("zoneID", zoneID);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        loadPlanet();
        super.onResume();
    }

    private void loadPlanet() {
        Bundle b;
        double alt, offset;
        double time;
        Calendar c = Calendar.getInstance();
        c.clear();
        c.setTimeInMillis(lastUpdate);
        pDate.setText(mDateFormat.format(c.getTime()));
        pTime.setText(mTimeFormat.format(c.getTime()));

        planetsDB.open();
        b = planetsDB.getPlanet(planetNum);
        planetsDB.close();

        pNameText.setText(b.getString("name"));
        pRAText.setText(pf.formatRA(b.getDouble("ra")));
        pDecText.setText(pf.formatDec(b.getDouble("dec")));
        pAzText.setText(pf.formatAZ(b.getDouble("az")));
        alt = b.getDouble("alt");
        pAltText.setText(pf.formatALT(alt));
        if (planetNum == 1)
            pDistText.setText(String.format(Locale.getDefault(), "%.4f AU",
                    b.getDouble("distance")));
        else
            pDistText.setText(String.format(Locale.getDefault(), "%.2f AU",
                    b.getDouble("distance")));
        pMagText.setText(String.format(Locale.getDefault(),
                "%.2f", b.getDouble("mag")));

        if (alt > 0) {
            time = b.getDouble("setTime");
            pSet.setText(R.string.data_set);
        } else {
            time = b.getDouble("riseTime");
            pSet.setText(R.string.data_rise);
        }
        tzDB.open();
        int off = tzDB.getZoneOffset(zoneID, lastUpdate / 1000L);
        tzDB.close();
        offset = off / 60.0;
        c.clear();
        c.setTimeInMillis(jdUTC.jdmills(time, offset));
        pSetText.setText(String.format("%s %s", mDateFormat.format(c.getTime()),
                mTimeFormat.format(c.getTime())));

        time = b.getDouble("transit");
        c.clear();
        c.setTimeInMillis(jdUTC.jdmills(time, offset));
        pTransitText.setText(String.format("%s %s", mDateFormat.format(c.getTime()),
                mTimeFormat.format(c.getTime())));
    }
}

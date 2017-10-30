/*
 * Planet's Position
 * A program to calculate the position of the planets in the night sky based
 * on a given location on Earth.
 * Copyright (c) 2017 Tim Gaddis
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

package planets.position.lunar;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import planets.position.FragmentListener;
import planets.position.PlanetsMain;
import planets.position.R;
import planets.position.database.LunarOccultationTable;
import planets.position.database.PlanetsDatabase;
import planets.position.util.JDUTC;
import planets.position.util.PositionFormat;

public class LunarOccultData extends Fragment {

    private TextView loDateText, loPlanetText, loStartText, loMaxText,
            loEndText, loMoonSAzText, loMoonSAltText, loMoonEAzText,
            loMoonEAltText, loMoonRiseText, loMoonSetText;
    private LinearLayout loLocalLayout, loLocalVisible, loMoonLayout;
    private long occultNum = 0, eclStart, eclEnd;
    private double offset;
    private int local, planet;
    private DateFormat mDateFormat, mTimeFormat;
    private List<String> planetArray;
    private PositionFormat pf;
    private PlanetsDatabase planetsDB;
    private SharedPreferences settings;
    private FragmentListener mCallbacks;
    private JDUTC jdUTC;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_occult_data, container, false);
        loDateText = v.findViewById(R.id.lo_date);
        loPlanetText = v.findViewById(R.id.lo_planet);
        loStartText = v.findViewById(R.id.lo_start_text);
        loMaxText = v.findViewById(R.id.lo_max_text);
        loEndText = v.findViewById(R.id.lo_end_text);
        loMoonRiseText = v.findViewById(R.id.lo_moonrise_text);
        loMoonSetText = v.findViewById(R.id.lo_moonset_text);
        loMoonSAzText = v.findViewById(R.id.lo_moons_az_text);
        loMoonSAltText = v.findViewById(R.id.lo_moons_alt_text);
        loMoonEAzText = v.findViewById(R.id.lo_moone_az_text);
        loMoonEAltText = v.findViewById(R.id.lo_moone_alt_text);
        loMoonLayout = v.findViewById(R.id.lo_moon_layout);
        loLocalVisible = v.findViewById(R.id.lo_local_visible);
        loLocalLayout = v.findViewById(R.id.lo_data_layout1);
        jdUTC = new JDUTC();
        pf = new PositionFormat(getActivity());
        planetArray = Arrays.asList(getResources().getStringArray(
                R.array.planets_array));
        mDateFormat = android.text.format.DateFormat
                .getDateFormat(getActivity().getApplicationContext());
        mTimeFormat = android.text.format.DateFormat
                .getTimeFormat(getActivity().getApplicationContext());

        planetsDB = new PlanetsDatabase(getActivity().getApplicationContext());
        settings = getActivity().getSharedPreferences(PlanetsMain.MAIN_PREFS, 0);

        if (mCallbacks != null) {
            mCallbacks.onToolbarTitleChange("Lunar Occultation", 4);
        }

        if (savedInstanceState != null) {
            // load data from config change
            occultNum = savedInstanceState.getLong("occultNum");
            offset = savedInstanceState.getDouble("offset");
        } else {
            // load bundle from previous activity
            Bundle bundle = getArguments();
            if (bundle != null) {
                occultNum = bundle.getLong("occultNum");
                offset = bundle.getDouble("offset");
            }
        }
        loadOccultation();
        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    public void onSaveInstanceState(Bundle outState) {
        outState.putLong("occultNum", occultNum);
        outState.putDouble("offset", offset);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        loadOccultation();
        super.onResume();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (settings.getBoolean("hasCalendar", false))
            inflater.inflate(R.menu.calendar_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_calendar:
                // add eclipse to calendar
                Intent intent = new Intent(Intent.ACTION_INSERT);
                intent.setData(Events.CONTENT_URI);
                intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, eclStart);
                intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, eclEnd);
                intent.putExtra(Events.TITLE, "Lunar Occultation");
                if (local > 0) {
                    intent.putExtra(Events.DESCRIPTION, "Lunar Occultation of "
                            + planetArray.get(planet));
                } else {
                    intent.putExtra(Events.DESCRIPTION,
                            "This occultation is not visible locally.");
                }
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void loadOccultation() {
        int planetColor;
        double moonRise, moonSet, temp;
        Calendar gc = new GregorianCalendar();
        planetsDB.open();
        Bundle b = planetsDB.getLunarOccult(occultNum);
        planetsDB.close();
        planetColor = ContextCompat.getColor
                (getActivity().getApplicationContext(), R.color.planet_set_color);
        gc.setTimeInMillis(jdUTC.jdmills(b.getDouble(LunarOccultationTable.COLUMN_OCCULT_DATE)));
        loDateText.setText(mDateFormat.format(gc.getTime()));
        planet = b.getInt(LunarOccultationTable.COLUMN_OCCULT_PLANET, -1);
        if (planet >= 0)
            loPlanetText.setText(planetArray.get(planet));
        else
            loPlanetText.setText("");
        local = b.getInt(LunarOccultationTable.COLUMN_LOCAL, -1);
        if (local > 0) {
            // local occultation
            moonRise = b.getDouble(LunarOccultationTable.COLUMN_MOONRISE, 0);
            if (moonRise > 0) {
                gc.setTimeInMillis(jdUTC.jdmills(moonRise, offset));
                loMoonRiseText.setText(String.format("%s\n%s", mDateFormat.format(gc.getTime()),
                        mTimeFormat.format(gc.getTime())));
            } else {
                loMoonRiseText.setText("");
            }
            moonSet = b.getDouble(LunarOccultationTable.COLUMN_MOONSET, 0);
            if (moonSet > 0) {
                gc.setTimeInMillis(jdUTC.jdmills(moonSet, offset));
                loMoonSetText.setText(String.format("%s\n%s", mDateFormat.format(gc.getTime()),
                        mTimeFormat.format(gc.getTime())));
            } else {
                loMoonSetText.setText("");
            }
            temp = b.getDouble(LunarOccultationTable.COLUMN_LOCAL_FIRST, 0);
            if (temp > 0) {
                eclStart = jdUTC.jdmills(temp, offset);
                gc.setTimeInMillis(eclStart);
                loStartText.setText(String.format("%s\n%s", mDateFormat.format(gc.getTime()),
                        mTimeFormat.format(gc.getTime())));
                if (temp < moonRise || temp > moonSet)
                    loStartText.setTextColor(planetColor);
            } else {
                loStartText.setText("");
            }
            temp = b.getDouble(LunarOccultationTable.COLUMN_LOCAL_MAX, 0);
            if (temp > 0) {
                gc.setTimeInMillis(jdUTC.jdmills(temp, offset));
                loMaxText.setText(String.format("%s\n%s", mDateFormat.format(gc.getTime()),
                        mTimeFormat.format(gc.getTime())));
                if (temp < moonRise || temp > moonSet)
                    loMaxText.setTextColor(planetColor);
            } else {
                loMaxText.setText("");
            }
            temp = b.getDouble(LunarOccultationTable.COLUMN_LOCAL_FOURTH, 0);
            if (temp > 0) {
                eclEnd = jdUTC.jdmills(temp, offset);
                gc.setTimeInMillis(eclEnd);
                loEndText.setText(String.format("%s\n%s", mDateFormat.format(gc.getTime()),
                        mTimeFormat.format(gc.getTime())));
                if (temp < moonRise || temp > moonSet)
                    loEndText.setTextColor(planetColor);
            } else {
                loEndText.setText("");
            }
            temp = b.getDouble(LunarOccultationTable.COLUMN_MOONS_AZ, 0);
            if (temp > 0) {
                loMoonSAzText.setText(pf.formatAZ(temp));
            } else {
                loMoonSAzText.setText("");
            }
            temp = b.getDouble(LunarOccultationTable.COLUMN_MOONS_ALT, 0);
            if (temp > 0) {
                loMoonSAltText.setText(pf.formatALT(temp));
            } else {
                loMoonSAltText.setText("");
            }
            temp = b.getDouble(LunarOccultationTable.COLUMN_MOONE_AZ, 0);
            if (temp > 0) {
                loMoonEAzText.setText(pf.formatAZ(temp));
            } else {
                loMoonEAzText.setText("");
            }
            temp = b.getDouble(LunarOccultationTable.COLUMN_MOONE_ALT, 0);
            if (temp > 0) {
                loMoonEAltText.setText(pf.formatALT(temp));
            } else {
                loMoonEAltText.setText("");
            }
        } else {
            // global occultation
            temp = b.getDouble(LunarOccultationTable.COLUMN_GLOBAL_BEGIN, 0);
            if (temp > 0) {
                eclStart = jdUTC.jdmills(temp, offset);
                gc.setTimeInMillis(eclStart);
                loStartText.setText(String.format("%s\n%s", mDateFormat.format(gc.getTime()),
                        mTimeFormat.format(gc.getTime())));
            } else {
                loStartText.setText("");
            }
            temp = b.getDouble(LunarOccultationTable.COLUMN_GLOBAL_MAX, 0);
            if (temp > 0) {
                gc.setTimeInMillis(jdUTC.jdmills(temp, offset));
                loMaxText.setText(String.format("%s\n%s", mDateFormat.format(gc.getTime()),
                        mTimeFormat.format(gc.getTime())));
            } else {
                loMaxText.setText("");
            }
            temp = b.getDouble(LunarOccultationTable.COLUMN_GLOBAL_END, 0);
            if (temp > 0) {
                eclEnd = jdUTC.jdmills(temp, offset);
                gc.setTimeInMillis(eclEnd);
                loEndText.setText(String.format("%s\n%s", mDateFormat.format(gc.getTime()),
                        mTimeFormat.format(gc.getTime())));
            } else {
                loEndText.setText("");
            }
            loMoonLayout.setVisibility(View.GONE);
            loLocalLayout.setVisibility(View.GONE);
            loLocalVisible.setVisibility(View.VISIBLE);
        }
    }
}

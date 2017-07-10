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
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import planets.position.FragmentListener;
import planets.position.PlanetsMain;
import planets.position.R;
import planets.position.database.LunarEclipseTable;
import planets.position.database.PlanetsDatabase;
import planets.position.database.SolarEclipseTable;
import planets.position.util.JDUTC;
import planets.position.util.PositionFormat;

public class LunarEclipseData extends Fragment {

    private TextView leDateText, leTypeText, leStartText, leTStartText,
            lePStartText, leMaxText, leTEndText, lePEndText, leEndText, leMoonRise, leMoonSet,
            leAzText, leAltText, lePMagText, leUMagText, leSarosText, leSarosMText;
    private LinearLayout leLocalLayout, leLocalVisible, leMoonRiseLayout;
    private long lunarNum = 0, eclStart, eclEnd;
    private double offset, mag;
    private boolean local;
    private String eclType;
    private DateFormat mDateFormat, mTimeFormat;
    private PositionFormat pf;
    private PlanetsDatabase planetsDB;
    private SharedPreferences settings;
    private FragmentListener mCallbacks;
    private JDUTC jdUTC;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_lunar_data, container,
                false);
        leDateText = (TextView) v.findViewById(R.id.le_date);
        leTypeText = (TextView) v.findViewById(R.id.le_type);
        leStartText = (TextView) v.findViewById(R.id.le_start_text);
        lePStartText = (TextView) v.findViewById(R.id.le_pstart_text);
        leTStartText = (TextView) v.findViewById(R.id.le_tstart_text);
        leMaxText = (TextView) v.findViewById(R.id.le_max_text);
        leTEndText = (TextView) v.findViewById(R.id.le_tend_text);
        lePEndText = (TextView) v.findViewById(R.id.le_pend_text);
        leEndText = (TextView) v.findViewById(R.id.le_end_text);
        leMoonRise = (TextView) v.findViewById(R.id.le_moonrise_text);
        leMoonSet = (TextView) v.findViewById(R.id.le_moonset_text);
        leAzText = (TextView) v.findViewById(R.id.le_moon_az_text);
        leAltText = (TextView) v.findViewById(R.id.le_moon_alt_text);
        lePMagText = (TextView) v.findViewById(R.id.le_pmag_text);
        leUMagText = (TextView) v.findViewById(R.id.le_umag_text);
        leSarosText = (TextView) v.findViewById(R.id.le_saros_text);
        leSarosMText = (TextView) v.findViewById(R.id.le_sarosm_text);
        leMoonRiseLayout = (LinearLayout) v.findViewById(R.id.le_moonrise_layout);
        leLocalVisible = (LinearLayout) v.findViewById(R.id.le_local_visible);
        leLocalLayout = (LinearLayout) v.findViewById(R.id.le_data_layout1);
        jdUTC = new JDUTC();
        pf = new PositionFormat(getActivity());

        mDateFormat = android.text.format.DateFormat
                .getDateFormat(getActivity().getApplicationContext());
        mTimeFormat = android.text.format.DateFormat
                .getTimeFormat(getActivity().getApplicationContext());

        planetsDB = new PlanetsDatabase(getActivity().getApplicationContext());
        settings = getActivity().getSharedPreferences(PlanetsMain.MAIN_PREFS, 0);

        if (mCallbacks != null) {
            mCallbacks.onToolbarTitleChange("Lunar Eclipse", 3);
        }

        if (savedInstanceState != null) {
            // load data from config change
            lunarNum = savedInstanceState.getLong("lunarNum");
            offset = savedInstanceState.getDouble("offset");
        } else {
            // load bundle from previous activity
            Bundle bundle = getArguments();
            if (bundle != null) {
                lunarNum = bundle.getLong("lunarNum");
                offset = bundle.getDouble("offset");
            }
        }
        loadEclipse();
        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putLong("lunarNum", lunarNum);
        outState.putDouble("offset", offset);
        super.onSaveInstanceState(outState);
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
    public void onResume() {
        loadEclipse();
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
        switch (item.getItemId()) {
            case R.id.action_calendar:
                // add eclipse to calendar
                Intent intent = new Intent(Intent.ACTION_INSERT);
                intent.setData(Events.CONTENT_URI);
                intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, eclStart);
                intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, eclEnd);
                intent.putExtra(Events.TITLE, eclType + " Lunar Eclipse");
                if (local) {
                    String desc = "Magnitude: " + String.format(Locale.getDefault(), "%.2f", mag);
                    intent.putExtra(Events.DESCRIPTION, desc);
                } else {
                    intent.putExtra(Events.DESCRIPTION,
                            "This eclipse is not visible locally.");
                }
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void loadEclipse() {
        int planetColor;
        boolean total = false, partial;
        double moonRise, moonSet, temp;
        Calendar gc = new GregorianCalendar();

        planetsDB.open();
        Bundle b = planetsDB.getLunarEclipse(lunarNum);
        planetsDB.close();

        planetColor = ContextCompat.getColor
                (getActivity().getApplicationContext(), R.color.planet_set_color);

        gc.setTimeInMillis(jdUTC.jdmills(b.getDouble(LunarEclipseTable.COLUMN_ECLIPSE_DATE)));
        leDateText.setText(mDateFormat.format(gc.getTime()));

        String type = b.getString(SolarEclipseTable.COLUMN_ECLIPSE_TYPE, "");
        partial = type.equals("Partial");
        eclType = type;
        if (type.equals("Total")) {
            total = true;
            partial = true;
        }
        leTypeText.setText(type);
        local = b.getInt(LunarEclipseTable.COLUMN_LOCAL, -1) > 0;

        moonRise = b.getDouble(LunarEclipseTable.COLUMN_MOONRISE, 0);
        if (moonRise > 0) {
            gc.setTimeInMillis(jdUTC.jdmills(moonRise, offset));
            leMoonRise.setText(String.format("%s\n%s", mDateFormat.format(gc.getTime()),
                    mTimeFormat.format(gc.getTime())));
        } else {
            leMoonRise.setText("");
        }
        moonSet = b.getDouble(LunarEclipseTable.COLUMN_MOONSET, 0);
        if (moonSet > 0) {
            gc.setTimeInMillis(jdUTC.jdmills(moonSet, offset));
            leMoonSet.setText(String.format("%s\n%s", mDateFormat.format(gc.getTime()),
                    mTimeFormat.format(gc.getTime())));
        } else {
            leMoonSet.setText("");
        }
        temp = b.getDouble(LunarEclipseTable.COLUMN_PENUMBRAL_BEGIN, 0);
        if (temp > 0) {
            eclStart = jdUTC.jdmills(temp, offset);
            gc.setTimeInMillis(eclStart);
            leStartText.setText(String.format("%s\n%s", mDateFormat.format(gc.getTime()),
                    mTimeFormat.format(gc.getTime())));
            if (local)
                if (temp < moonRise || temp > moonSet) {
                    leStartText.setTextColor(planetColor);
                }
        } else {
            leStartText.setText(" \n ");
        }
        temp = b.getDouble(LunarEclipseTable.COLUMN_PARTIAL_BEGIN, 0);
        if (temp > 0 && partial) {
            gc.setTimeInMillis(jdUTC.jdmills(temp, offset));
            lePStartText.setText(String.format("%s\n%s", mDateFormat.format(gc.getTime()),
                    mTimeFormat.format(gc.getTime())));
            if (local)
                if (temp < moonRise || temp > moonSet) {
                    lePStartText.setTextColor(planetColor);
                }
        } else {
            lePStartText.setText(" \n ");
        }
        temp = b.getDouble(LunarEclipseTable.COLUMN_TOTAL_BEGIN, 0);
        if (temp > 0 && total) {
            gc.setTimeInMillis(jdUTC.jdmills(temp, offset));
            leTStartText.setText(String.format("%s\n%s", mDateFormat.format(gc.getTime()),
                    mTimeFormat.format(gc.getTime())));
            if (local)
                if (temp < moonRise || temp > moonSet) {
                    leTStartText.setTextColor(planetColor);
                }
        } else {
            leTStartText.setText(" \n ");
        }
        temp = b.getDouble(LunarEclipseTable.COLUMN_MAX_ECLIPSE, 0);
        if (temp > 0) {
            gc.setTimeInMillis(jdUTC.jdmills(temp, offset));
            leMaxText.setText(String.format("%s\n%s", mDateFormat.format(gc.getTime()),
                    mTimeFormat.format(gc.getTime())));
            if (local)
                if (temp < moonRise || temp > moonSet) {
                    leMaxText.setTextColor(planetColor);
                }
        } else {
            leMaxText.setText(" \n ");
        }
        temp = b.getDouble(LunarEclipseTable.COLUMN_TOTAL_END, 0);
        if (temp > 0 && total) {
            gc.setTimeInMillis(jdUTC.jdmills(temp, offset));
            leTEndText.setText(String.format("%s\n%s", mDateFormat.format(gc.getTime()),
                    mTimeFormat.format(gc.getTime())));
            if (local)
                if (temp < moonRise || temp > moonSet) {
                    leTEndText.setTextColor(planetColor);
                }
        } else {
            leTEndText.setText(" \n ");
        }
        temp = b.getDouble(LunarEclipseTable.COLUMN_PARTIAL_END, 0);
        if (temp > 0 && partial) {
            gc.setTimeInMillis(jdUTC.jdmills(temp, offset));
            lePEndText.setText(String.format("%s\n%s", mDateFormat.format(gc.getTime()),
                    mTimeFormat.format(gc.getTime())));
            if (local)
                if (temp < moonRise || temp > moonSet) {
                    lePEndText.setTextColor(planetColor);
                }
        } else {
            lePEndText.setText(" \n ");
        }
        temp = b.getDouble(LunarEclipseTable.COLUMN_PENUMBRAL_END, 0);
        if (temp > 0) {
            eclEnd = jdUTC.jdmills(temp, offset);
            gc.setTimeInMillis(eclEnd);
            leEndText.setText(String.format("%s\n%s", mDateFormat.format(gc.getTime()),
                    mTimeFormat.format(gc.getTime())));
            if (local)
                if (temp < moonRise || temp > moonSet) {
                    leEndText.setTextColor(planetColor);
                }
        } else {
            leEndText.setText(" \n ");
        }

        if (local) {
            // local eclipse
            temp = b.getDouble(LunarEclipseTable.COLUMN_MOON_AZ, 0);
            if (temp > 0) {
                leAzText.setText(pf.formatAZ(temp));
            } else {
                leAzText.setText("");
            }
            temp = b.getDouble(LunarEclipseTable.COLUMN_MOON_ALT, 0);
            if (temp > 0) {
                leAltText.setText(pf.formatALT(temp));
            } else {
                leAltText.setText("");
            }
            // penumbral magnitude
            temp = b.getDouble(LunarEclipseTable.COLUMN_PENUMBRAL_MAG, 0);
            if (temp > 0) {
                mag = temp;
                lePMagText.setText(String.format(Locale.getDefault(), "%.2f", temp));
            } else {
                mag = 0;
                lePMagText.setText("");
            }
            // umbral magnitude
            temp = b.getDouble(LunarEclipseTable.COLUMN_UMBRAL_MAG, 0);
            if (temp > 0) {
                mag = temp;
                leUMagText.setText(String.format(Locale.getDefault(), "%.2f", temp));
            } else {
                mag = 0;
                leUMagText.setText("");
            }
            leSarosText.setText(String.valueOf(b.getInt(LunarEclipseTable.COLUMN_SAROS_NUM, 0)));
            leSarosMText.setText(String.valueOf(b.getInt(LunarEclipseTable.COLUMN_SAROS_MEMBER_NUM, 0)));
        } else {
            // global eclipse
            leLocalLayout.setVisibility(View.GONE);
            leMoonRiseLayout.setVisibility(View.GONE);
            leLocalVisible.setVisibility(View.VISIBLE);
        }
    }
}

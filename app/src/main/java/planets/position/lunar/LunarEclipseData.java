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

package planets.position.lunar;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import planets.position.FragmentListener;
import planets.position.R;
import planets.position.database.LunarEclipseTable;
import planets.position.database.PlanetsDatabase;
import planets.position.database.SolarEclipseTable;
import planets.position.database.TimeZoneDB;
import planets.position.util.JDUTC;
import planets.position.util.PositionFormat;

public class LunarEclipseData extends Fragment {

    private TextView leDateText, leTypeText, leStartText, leTStartText, leLocalVisible,
            lePStartText, leMaxText, leTEndText, lePEndText, leEndText, leMoonRise, leMoonSet,
            leAzText, leAltText, lePMagText, leUMag, leUMagText, leSarosText, leSarosMText, leLocalTime;
    private ConstraintLayout leLocalLayout, leMoonLayout, lePartialLayout, leTotalLayout;
    private long lunarNum = 0, eclStart, eclEnd;
    private double mag;
    private int zoneID;
    private boolean local;
    private String eclType;
    private DateFormat mDateFormat, mTimeFormat;
    private PositionFormat pf;
    private PlanetsDatabase planetsDB;
    private SharedPreferences settings;
    private FragmentListener mCallbacks;
    private TimeZoneDB tzDB;
    private JDUTC jdUTC;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_lunar_data, container,
                false);
        leDateText = v.findViewById(R.id.le_date);
        leTypeText = v.findViewById(R.id.le_type);
        leLocalTime = v.findViewById(R.id.le_local_time);
        leStartText = v.findViewById(R.id.le_start_text);
        lePStartText = v.findViewById(R.id.le_pstart_text);
        leTStartText = v.findViewById(R.id.le_tstart_text);
        leMaxText = v.findViewById(R.id.le_max_text);
        leTEndText = v.findViewById(R.id.le_tend_text);
        lePEndText = v.findViewById(R.id.le_pend_text);
        leEndText = v.findViewById(R.id.le_end_text);
        leMoonRise = v.findViewById(R.id.le_moonrise_text);
        leMoonSet = v.findViewById(R.id.le_moonset_text);
        leAzText = v.findViewById(R.id.le_moon_az_text);
        leAltText = v.findViewById(R.id.le_moon_alt_text);
        lePMagText = v.findViewById(R.id.le_pmag_text);
        leUMag = v.findViewById(R.id.le_umag);
        leUMagText = v.findViewById(R.id.le_umag_text);
        leSarosText = v.findViewById(R.id.le_saros_text);
        leSarosMText = v.findViewById(R.id.le_sarosm_text);
        leLocalVisible = v.findViewById(R.id.le_no_visible);
        leMoonLayout = v.findViewById(R.id.le_moon_layout);
        leLocalLayout = v.findViewById(R.id.le_data_layout);
        lePartialLayout = v.findViewById(R.id.le_partial_layout);
        leTotalLayout = v.findViewById(R.id.le_total_layout);

        if (mCallbacks != null) {
            mCallbacks.onToolbarTitleChange("Lunar Eclipse", 3);
        }

        if (savedInstanceState != null) {
            // load data from config change
            lunarNum = savedInstanceState.getLong("lunarNum");
            zoneID = savedInstanceState.getInt("zoneID");
        } else {
            // load bundle from previous activity
            Bundle bundle = getArguments();
            if (bundle != null) {
                lunarNum = bundle.getLong("lunarNum");
                zoneID = bundle.getInt("zoneID");
            }
        }
        loadEclipse();
        return v;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putLong("lunarNum", lunarNum);
        outState.putInt("zoneID", zoneID);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        jdUTC = new JDUTC();
        pf = new PositionFormat(getActivity());
        planetsDB = new PlanetsDatabase(getActivity().getApplicationContext());
        settings = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        mDateFormat = android.text.format.DateFormat
                .getDateFormat(getActivity().getApplicationContext());
        mTimeFormat = android.text.format.DateFormat
                .getTimeFormat(getActivity().getApplicationContext());
        tzDB = new TimeZoneDB(getActivity().getApplicationContext());
        setHasOptionsMenu(true);
        setRetainInstance(true);
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

    @Override
    public void onResume() {
        loadEclipse();
        super.onResume();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (settings.getBoolean("hasCalendar", false))
            inflater.inflate(R.menu.calendar_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_calendar) {// add eclipse to calendar
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
        }
        return super.onOptionsItemSelected(item);
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

        long max = jdUTC.jdmills(b.getDouble(LunarEclipseTable.COLUMN_ECLIPSE_DATE));
        tzDB.open();
        int off = tzDB.getZoneOffset(zoneID, max / 1000L);
        double offset = off / 60.0;
        tzDB.close();
        gc.setTimeInMillis(max);
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
        if (temp > 0) {
            gc.setTimeInMillis(jdUTC.jdmills(temp, offset));
            lePStartText.setText(String.format("%s\n%s", mDateFormat.format(gc.getTime()),
                    mTimeFormat.format(gc.getTime())));
            if (local)
                if (temp < moonRise || temp > moonSet) {
                    lePStartText.setTextColor(planetColor);
                }
        }
        temp = b.getDouble(LunarEclipseTable.COLUMN_TOTAL_BEGIN, 0);
        if (temp > 0) {
            gc.setTimeInMillis(jdUTC.jdmills(temp, offset));
            leTStartText.setText(String.format("%s\n%s", mDateFormat.format(gc.getTime()),
                    mTimeFormat.format(gc.getTime())));
            if (local)
                if (temp < moonRise || temp > moonSet) {
                    leTStartText.setTextColor(planetColor);
                }
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
        if (temp > 0) {
            gc.setTimeInMillis(jdUTC.jdmills(temp, offset));
            leTEndText.setText(String.format("%s\n%s", mDateFormat.format(gc.getTime()),
                    mTimeFormat.format(gc.getTime())));
            if (local)
                if (temp < moonRise || temp > moonSet) {
                    leTEndText.setTextColor(planetColor);
                }
        }
        temp = b.getDouble(LunarEclipseTable.COLUMN_PARTIAL_END, 0);
        if (temp > 0) {
            gc.setTimeInMillis(jdUTC.jdmills(temp, offset));
            lePEndText.setText(String.format("%s\n%s", mDateFormat.format(gc.getTime()),
                    mTimeFormat.format(gc.getTime())));
            if (local)
                if (temp < moonRise || temp > moonSet) {
                    lePEndText.setTextColor(planetColor);
                }
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

        if (!partial)
            lePartialLayout.setVisibility(View.GONE);

        if (!total)
            leTotalLayout.setVisibility(View.GONE);

        if (local) {
            // local eclipse
            leLocalTime.setText(R.string.ecl_local);
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
                leUMag.setVisibility(View.GONE);
                leUMagText.setVisibility(View.GONE);
            }
            leSarosText.setText(String.valueOf(b.getInt(LunarEclipseTable.COLUMN_SAROS_NUM, 0)));
            leSarosMText.setText(String.valueOf(b.getInt(LunarEclipseTable.COLUMN_SAROS_MEMBER_NUM, 0)));
        } else {
            // global eclipse
            leLocalTime.setText(R.string.ecl_universal);
            leLocalLayout.setVisibility(View.GONE);
            leMoonLayout.setVisibility(View.GONE);
            leLocalVisible.setVisibility(View.VISIBLE);
        }
    }
}

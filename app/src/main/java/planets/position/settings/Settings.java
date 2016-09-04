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

package planets.position.settings;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

import planets.position.FragmentListener;
import planets.position.PlanetsMain;
import planets.position.R;

public class Settings extends Fragment {

    private static final int SETTINGS_DIALOG = 100;

    private SharedPreferences settings;
    private ListView settingsList;
    private int raFormat, decFormat, azFormat, altFormat;
    private FragmentListener mCallbacks;
    private SettingsDialog settingDialog;
    private ArrayList<Setting> settingList;
    private CustomSettingsAdapter adapter;

    public Settings() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_settings, container, false);

        settingsList = (ListView) v.findViewById(R.id.settingsList);
        settingList = loadSettings();
        adapter = new CustomSettingsAdapter(getActivity(), settingList);
        settingsList.setAdapter(adapter);

        if (mCallbacks != null) {
            mCallbacks.onToolbarTitleChange("Settings", 8);
        }

        settingsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                Object o = settingsList.getItemAtPosition(position);
                Setting s = (Setting) o;
                settingDialog = SettingsDialog.newInstance(s.getArray(), s.getTitle(), position);
                settingDialog.setTargetFragment(Settings.this, SETTINGS_DIALOG);
                settingDialog.show(getActivity().getSupportFragmentManager(), "settingsDialog");
            }
        });

        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = getActivity()
                .getSharedPreferences(PlanetsMain.MAIN_PREFS, 0);

        raFormat = settings.getInt("raFormat", 0);
        decFormat = settings.getInt("decFormat", 0);
        azFormat = settings.getInt("azFormat", 0);
        altFormat = settings.getInt("altFormat", 0);
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SETTINGS_DIALOG:
                int p = data.getIntExtra("position", 0);
                if (p == 0)
                    raFormat = resultCode;
                else if (p == 1)
                    decFormat = resultCode;
                else if (p == 2)
                    azFormat = resultCode;
                else if (p == 3)
                    altFormat = resultCode;
                saveSettings();
                settingList = loadSettings();
                adapter.updateResults(settingList);
                break;
            default:
                // Cancel button
                break;
        }
    }

    private ArrayList<Setting> loadSettings() {

        ArrayList<Setting> sList = new ArrayList<>();

        Setting s = new Setting();
        s.setTitle(R.string.pref_title_ra_format);
        s.setArray(R.array.pref_ra_titles);
        s.setFormat(getActivity().getResources().getStringArray(R.array.pref_ra_titles)[raFormat]);
        sList.add(s);

        s = new Setting();
        s.setTitle(R.string.pref_title_dec_format);
        s.setArray(R.array.pref_dec_titles);
        s.setFormat(getActivity().getResources().getStringArray(R.array.pref_dec_titles)[decFormat]);
        sList.add(s);

        s = new Setting();
        s.setTitle(R.string.pref_title_az_format);
        s.setArray(R.array.pref_az_titles);
        s.setFormat(getActivity().getResources().getStringArray(R.array.pref_az_titles)[azFormat]);
        sList.add(s);

        s = new Setting();
        s.setTitle(R.string.pref_title_alt_format);
        s.setArray(R.array.pref_alt_titles);
        s.setFormat(getActivity().getResources().getStringArray(R.array.pref_alt_titles)[altFormat]);
        sList.add(s);

        return sList;
    }

    private void saveSettings() {

        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("raFormat", raFormat);
        editor.putInt("decFormat", decFormat);
        editor.putInt("azFormat", azFormat);
        editor.putInt("altFormat", altFormat);
        editor.apply();

    }
}

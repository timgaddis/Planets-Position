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

package planets.position.location;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import planets.position.R;
import planets.position.database.TimeZoneDB;

public class UserCityDialog extends DialogFragment {

    private Spinner spinState;
    private Spinner spinCity;
    private CityDialogListener mListener;
    private TimeZoneDB tzDB;
    private String country, state;
    private long cityID;

    public interface CityDialogListener {
        void onDialogPositiveClick(long id);

        void onDialogNegativeClick();
    }

    public static UserCityDialog newInstance() {
        return new UserCityDialog();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tzDB = new TimeZoneDB(getActivity().getApplicationContext());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (CityDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement CityDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity(), R.style.LocDialogTheme);
        LayoutInflater inflaterLat = getActivity().getLayoutInflater();
        @SuppressLint("InflateParams")
        View v = inflaterLat.inflate(R.layout.fragment_user_city_dialog, null);
        Spinner spinCountry = v.findViewById(R.id.spinnerCountry);
        spinState = v.findViewById(R.id.spinnerState);
        spinCity = v.findViewById(R.id.spinnerCity);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.country_array, R.layout.spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_drop_item);
        spinCountry.setAdapter(adapter);

        spinCountry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                country = (String) parent.getItemAtPosition(position);
                spinState.setVisibility(View.VISIBLE);
                tzDB.open();
                Cursor states = tzDB.getStateList(country);
                SimpleCursorAdapter cursorAdapter = new SimpleCursorAdapter(getActivity().getApplicationContext(),
                        R.layout.spinner_item, states, new String[]{"state"},
                        new int[]{R.id.spinText}, 0);
                cursorAdapter.setDropDownViewResource(R.layout.spinner_drop_item);
                spinState.setAdapter(cursorAdapter);
                tzDB.close();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spinState.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Cursor c = (Cursor) parent.getItemAtPosition(position);
                state = c.getString(c.getColumnIndex("state"));
                spinCity.setVisibility(View.VISIBLE);
                tzDB.open();
                Cursor cities = tzDB.getCityList(country, state);
                SimpleCursorAdapter cursorAdapter = new SimpleCursorAdapter(getActivity().getApplicationContext(),
                        R.layout.spinner_item, cities, new String[]{"city"},
                        new int[]{R.id.spinText}, 0);
                cursorAdapter.setDropDownViewResource(R.layout.spinner_drop_item);
                spinCity.setAdapter(cursorAdapter);
                tzDB.close();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spinCity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                cityID = id;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        alert.setTitle(R.string.loc_user_city)
                .setView(v)
                .setPositiveButton("Save",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                mListener.onDialogPositiveClick(cityID);
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                mListener.onDialogNegativeClick();
                            }
                        });

        return alert.create();
    }

}

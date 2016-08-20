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

package planets.position.location;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;

import planets.position.R;

public class UserLocationDialog extends DialogFragment {

    private int title, list, index;
    private double value, newValue;
    private EditText inputLat, inputLong, inputElevation;
    private RadioButton rbSouth, rbWest;

    public static UserLocationDialog newInstance(int title, int list,
                                                 int index, double value) {
        UserLocationDialog frag = new UserLocationDialog();
        Bundle args = new Bundle();
        args.putInt("title", title);
        args.putInt("list", list);
        args.putInt("index", index);
        args.putDouble("value", value);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            index = savedInstanceState.getInt("index");
            list = savedInstanceState.getInt("list");
            title = savedInstanceState.getInt("title");
            value = savedInstanceState.getDouble("newValue");
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putInt("index", index);
        bundle.putInt("list", list);
        bundle.putInt("title", title);
        // bundle.putDouble("value", value);
        switch (index) {
            case 0:
                if (!inputLat.getText().toString().equals(""))
                    newValue = Double.parseDouble(inputLat.getText().toString());
                else
                    newValue = 0.0;
                if (rbSouth.isChecked())
                    newValue *= -1;
                break;
            case 1:
                if (!inputLong.getText().toString().equals(""))
                    newValue = Double.parseDouble(inputLong.getText().toString());
                else
                    newValue = 0.0;
                if (rbWest.isChecked())
                    newValue *= -1;
                break;
            case 2:
                if (!inputElevation.getText().toString().equals(""))
                    newValue = Double.parseDouble(inputElevation.getText()
                            .toString());
                else
                    newValue = 0.0;
                break;
        }
        bundle.putDouble("newValue", newValue);
        super.onSaveInstanceState(bundle);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            title = getArguments().getInt("title");
            list = getArguments().getInt("list");
            index = getArguments().getInt("index");
            value = getArguments().getDouble("value");
        }

        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity(), R.style.LocDialogTheme);

        switch (index) {
            case 0:
                // Latitude
                LayoutInflater inflaterLat = getActivity().getLayoutInflater();
                View v1 = inflaterLat.inflate(R.layout.lat_dialog, null);
                inputLat = (EditText) v1.findViewById(R.id.locLatText);
                rbSouth = (RadioButton) v1.findViewById(R.id.radioLatSouth);
                inputLat.setText(String.valueOf(Math.abs(value)));
                if (value < 0)
                    rbSouth.setChecked(true);
                alert.setTitle(title)
                        .setView(v1)
                        .setPositiveButton("Save",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        String value = inputLat.getText()
                                                .toString();
                                        Intent i = new Intent();
                                        i.putExtra("value", value);
                                        i.putExtra("south", rbSouth.isChecked());
                                        getTargetFragment().onActivityResult(
                                                getTargetRequestCode(), id, i);
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        getTargetFragment().onActivityResult(
                                                getTargetRequestCode(), id, null);
                                    }
                                });
                break;
            case 1:
                // Longitude
                LayoutInflater inflater1 = getActivity().getLayoutInflater();
                View v2 = inflater1.inflate(R.layout.long_dialog, null);
                inputLong = (EditText) v2.findViewById(R.id.locLongText);
                rbWest = (RadioButton) v2.findViewById(R.id.radioLongWest);
                inputLong.setText(String.valueOf(Math.abs(value)));
                if (value < 0)
                    rbWest.setChecked(true);
                alert.setTitle(title)
                        .setView(v2)
                        .setPositiveButton("Save",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        String value = inputLong.getText()
                                                .toString();
                                        Intent i = new Intent();
                                        i.putExtra("value", value);
                                        i.putExtra("west", rbWest.isChecked());
                                        getTargetFragment().onActivityResult(
                                                getTargetRequestCode(), id, i);
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        getTargetFragment().onActivityResult(
                                                getTargetRequestCode(), id, null);
                                    }
                                });
                break;
            case 2:
                // Elevation
                inputElevation = new EditText(getActivity());
                inputElevation.setInputType(InputType.TYPE_CLASS_NUMBER
                        | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                inputElevation.setText(String.valueOf(value));
                alert.setTitle(title)
                        .setView(inputElevation)
                        .setPositiveButton("Save",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        String value = inputElevation.getText()
                                                .toString();
                                        Intent i = new Intent();
                                        i.putExtra("value", value);
                                        getTargetFragment().onActivityResult(
                                                getTargetRequestCode(), id, i);
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        getTargetFragment().onActivityResult(
                                                getTargetRequestCode(), id, null);
                                    }
                                });
                break;
            case 3:
                // GMT offset
                alert.setTitle(title).setItems(list,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // select a value from list
                                getTargetFragment().onActivityResult(
                                        getTargetRequestCode(), which, null);
                            }
                        });
                break;
        }
        return alert.create();
    }

}

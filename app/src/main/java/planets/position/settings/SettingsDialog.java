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

package planets.position.settings;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import planets.position.R;

public class SettingsDialog extends DialogFragment {

    private int array, title, position;

    public SettingsDialog() {
        // Required empty public constructor
    }

    public static SettingsDialog newInstance(int array, int title, int position) {

        Bundle args = new Bundle();
        SettingsDialog fragment = new SettingsDialog();
        args.putInt("array", array);
        args.putInt("title", title);
        args.putInt("pos", position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            array = savedInstanceState.getInt("array");
            title = savedInstanceState.getInt("title");
            position = savedInstanceState.getInt("pos");
        }
        setRetainInstance(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("array", array);
        outState.putInt("title", title);
        outState.putInt("pos", position);
        super.onSaveInstanceState(outState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            array = getArguments().getInt("array");
            title = getArguments().getInt("title");
            position = getArguments().getInt("pos");
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.LocDialogTheme);
        builder.setTitle(title);
        builder.setItems(array, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent i = new Intent();
                i.putExtra("position", position);
                getTargetFragment().onActivityResult(getTargetRequestCode(), which, i);
            }
        });

        return builder.create();
    }

}

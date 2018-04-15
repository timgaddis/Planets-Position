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

package planets.position.location;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import planets.position.R;
import planets.position.database.TimeZoneDB;

public class UserTimezoneDialog extends DialogFragment {

    private TimezoneDialogListener mListener;
    private TimeZoneDB timeZoneDB;

    public interface TimezoneDialogListener {
        void onZoneSelection(int id, String name);
    }

    public static UserTimezoneDialog newInstance() {
        return new UserTimezoneDialog();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        timeZoneDB = new TimeZoneDB(getActivity());
        setRetainInstance(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof TimezoneDialogListener) {
            mListener = (TimezoneDialogListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement TimezoneDialogListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity(), R.style.LocDialogTheme);
        alert.setTitle(R.string.loc_tz_title);
        timeZoneDB.open();
        final Cursor c = timeZoneDB.getZoneList();
        c.moveToFirst();
        alert.setCursor(c, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                c.moveToPosition(which);
                String name = c.getString(c.getColumnIndex("zone_name"));
                int id = c.getInt(c.getColumnIndex("_id"));
                mListener.onZoneSelection(id, name);
            }
        }, "zone_name");
        timeZoneDB.close();
        return alert.create();
    }
}

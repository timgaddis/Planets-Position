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

package planets.position.util;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.widget.TimePicker;

import java.util.Calendar;

import planets.position.R;

public class PlanetTimePicker extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

    private int mHour = -1, mMinute = -1;

    public void setData(int hour, int minute) {
        mHour = hour;
        mMinute = minute;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (mHour < 0) {
            Calendar c = Calendar.getInstance();
            mHour = c.get(Calendar.HOUR_OF_DAY);
            mMinute = c.get(Calendar.MINUTE);
        }
        // Create a new instance of TimePickerDialog and return it
        return new TimePickerDialog(getTargetFragment().getActivity(), R.style.datepicker,
                this, mHour, mMinute,
                DateFormat.is24HourFormat(getTargetFragment().getActivity()));
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        // Set
        Intent data = new Intent();
        Bundle b = new Bundle();
        b.putInt("hour", hourOfDay);
        b.putInt("minute", minute);
        data.putExtras(b);
        getTargetFragment().onActivityResult(getTargetRequestCode(), 0, data);
    }

}

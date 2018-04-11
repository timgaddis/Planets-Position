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

package planets.position;

import android.app.Activity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

import planets.position.database.PlanetsDatabase;
import planets.position.database.PlanetsTable;
import planets.position.util.JDUTC;
import planets.position.util.RiseSet;

public class WhatsUpTask extends DialogFragment {

    private ComputePlanetsTask mTask;
    private List<String> planetNames;
    private double offset;
    private double[] g;
    private JDUTC jdUTC;
    private ProgressBar pb;
    private TextView tv;

    // load c library
    static {
        System.loadLibrary("planets_swiss");
    }

    // c function prototypes
    public native double[] planetUpData(double d1, double d2, int p, double[] loc);

    public void setData(ComputePlanetsTask task, double[] loc, double off) {
        mTask = task;
        offset = off;
        g = loc;
        mTask.setFragment(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        jdUTC = new JDUTC();
        planetNames = Arrays.asList(getResources().getStringArray(
                R.array.planets_array));
        setRetainInstance(true);
        if (mTask != null)
            mTask.execute();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.progress_dialog_hor, container,
                false);
        tv = v.findViewById(R.id.progress_text);
        pb = v.findViewById(R.id.progressBar);
        pb.setMax(10);
        pb.getProgressDrawable().setColorFilter(Color.LTGRAY, PorterDuff.Mode.SRC_IN);
        getDialog().setCanceledOnTouchOutside(false);

        return v;
    }

    /**
     * workaround for issue #17423
     * https://code.google.com/p/android/issues/detail?id=17423
     */
    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setOnDismissListener(null);
        super.onDestroyView();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mTask != null) {
            mTask.cancel(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mTask == null)
            dismiss();
    }

    private void taskFinished() {
        if (isResumed())
            dismiss();
        mTask = null;
        if (getTargetFragment() != null)
            getTargetFragment().onActivityResult(0, Activity.RESULT_OK, null);
    }

    public class ComputePlanetsTask extends AsyncTask<Void, Integer, Void> {

        WhatsUpTask mFragment;
        double[] data = null, time;
        double r, s, ra, transit;
        PlanetsDatabase planetsDB;
        private RiseSet riseSet;
        private ContentValues values;

        void setFragment(WhatsUpTask fragment) {
            mFragment = fragment;
        }

        @Override
        protected void onPreExecute() {
            values = new ContentValues();
            time = jdUTC.getCurrentTime(offset);
            planetsDB = new PlanetsDatabase(mFragment.getActivity().getApplicationContext());
            riseSet = new RiseSet(g);
        }

        protected void onProgressUpdate(Integer... values) {
            pb.setProgress(values[0]);
            tv.setText(String.format("Calculating %s", planetNames.get(values[1])));
        }

        @Override
        protected Void doInBackground(Void... params) {
            planetsDB.open();
            planetsDB.eraseTable(PlanetsTable.TABLE_NAME);
            for (int i = 0; i < 10; i++) {
                if (this.isCancelled()) {
                    if (getTargetFragment() != null) {
                        getTargetFragment().onActivityResult(0,
                                Activity.RESULT_CANCELED, null);
                    }
                    break;
                }
                values.clear();
                data = planetUpData(time[0], time[1], i, g);
                if (data == null) {
                    Log.e("Position error",
                            "WhatsUpTask - ComputePlanetsTask error");
                    if (getTargetFragment() != null) {
                        getTargetFragment().onActivityResult(0, 100, null);
                    }
                    break;
                }

                s = riseSet.getSet(time[1], i);
                if (s < 0) {
                    Log.e("Position error", "ComputePlanetsTask set error");
                    break;
                }

                r = riseSet.getRise(time[1], i);
                if (r < 0) {
                    Log.e("Position error", "ComputePlanetsTask rise error");
                    break;
                }

                transit = riseSet.getTransit(time[1], i);
                if (transit < 0) {
                    Log.e("Position error", "ComputePlanetsTask transit error");
                    break;
                }

                ra = data[0];
                // convert ra to hours
                ra = ra / 15;

                values.put(PlanetsTable.COLUMN_NAME, planetNames.get(i));
                values.put(PlanetsTable.COLUMN_NUMBER, i);
                values.put(PlanetsTable.COLUMN_RA, ra);
                values.put(PlanetsTable.COLUMN_DEC, data[1]);
                values.put(PlanetsTable.COLUMN_AZ, data[3]);
                values.put(PlanetsTable.COLUMN_ALT, data[4]);
                values.put(PlanetsTable.COLUMN_DISTANCE, data[2]);
                values.put(PlanetsTable.COLUMN_MAGNITUDE, data[5]);
                values.put(PlanetsTable.COLUMN_SET_TIME, s);
                values.put(PlanetsTable.COLUMN_RISE_TIME, r);
                values.put(PlanetsTable.COLUMN_TRANSIT, transit);

                planetsDB.addPlanet(values);
                publishProgress(i + 1, i);
            }
            planetsDB.close();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (mFragment == null)
                return;
            mFragment.taskFinished();
        }
    }

}

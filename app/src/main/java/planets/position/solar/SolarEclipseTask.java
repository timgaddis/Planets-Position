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

package planets.position.solar;

import android.app.Activity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
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

import planets.position.R;
import planets.position.database.PlanetsDatabase;
import planets.position.database.SolarEclipseTable;
import planets.position.util.RiseSet;

public class SolarEclipseTask extends DialogFragment {

    private ComputeEclipseTask mTask;
    private double firstEcl, lastEcl, startTime, backward;
    private double[] g;
    private ProgressBar pb;
    private RiseSet riseSet;

    // load c library
    static {
        System.loadLibrary("planets_swiss");
    }

    // c function prototypes
    public native double[] solarDataLocal(double d2, double[] loc, int back);

    public native double[] solarDataGlobal(double d2, int back);

    public void setData(ComputeEclipseTask task, double[] loc, double time,
                        double back) {
        mTask = task;
        g = loc;
        startTime = time;
        backward = back;
        mTask.setFragment(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setRetainInstance(true);
        if (mTask != null)
            mTask.execute(startTime, backward);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        TextView tv;
        View v = inflater.inflate(R.layout.progress_dialog_hor, container, false);
        tv = v.findViewById(R.id.progress_text);
        pb = v.findViewById(R.id.progressBar);
        tv.setText(R.string.eclipse_dialog);
        pb.setMax(10);
        pb.getProgressDrawable().setColorFilter(Color.LTGRAY, PorterDuff.Mode.SRC_IN);
        getDialog().setCanceledOnTouchOutside(false);
        riseSet = new RiseSet(g);
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
        if (getTargetFragment() != null) {
            Intent data = new Intent();
            Bundle b = new Bundle();
            b.putDouble("first", firstEcl);
            b.putDouble("last", lastEcl);
            data.putExtras(b);
            getTargetFragment().onActivityResult(SolarEclipse.TASK_FRAGMENT,
                    Activity.RESULT_OK, data);
        }
    }

    public class ComputeEclipseTask extends AsyncTask<Double, Integer, Void> {

        SolarEclipseTask mFragment;
        PlanetsDatabase planetsDB;
        private ContentValues values;
        private String eclType;

        void setFragment(SolarEclipseTask fragment) {
            mFragment = fragment;
        }

        @Override
        protected void onPreExecute() {
            values = new ContentValues();
            planetsDB = new PlanetsDatabase(mFragment.getActivity().getApplicationContext());
        }

        protected void onProgressUpdate(Integer... values) {
            pb.setProgress(values[0] + 1);
        }

        @Override
        protected Void doInBackground(Double... params) {
            double start, sunset, sunrise;
            double[] data1, data2;
            int back, i, val;

            planetsDB.open();
            back = (int) Math.round(params[1]);
            start = params[0];

            // compute first local eclipse
            data2 = solarDataLocal(start, g, back);
            if (data2 == null) {
                Log.e("Solar Eclipse error", "solarDataLocal data2 error");
                if (getTargetFragment() != null) {
                    getTargetFragment().onActivityResult(
                            SolarEclipse.TASK_FRAGMENT, 100, null);
                }
                return null;
            }

            for (i = 0; i < 10; i++) {
                if (this.isCancelled()) {
                    if (getTargetFragment() != null) {
                        getTargetFragment().onActivityResult(
                                SolarEclipse.TASK_FRAGMENT,
                                Activity.RESULT_CANCELED, null);
                    }
                    break;
                }
                values.clear();

                // Global Eclipse Calculation
                data1 = solarDataGlobal(start, back);
                if (data1 == null) {
                    Log.e("Solar Eclipse error", "solarDataGlobal data1 error");
                    if (getTargetFragment() != null) {
                        getTargetFragment().onActivityResult(
                                SolarEclipse.TASK_FRAGMENT, 200, null);
                    }
                    break;
                }
                // create type string use data1[0] (global type)
                val = (int) data1[0];
                if ((val & 4) == 4) // SE_ECL_TOTAL
                    eclType = "Total";
                else if ((val & 8) == 8) // SE_ECL_ANNULAR
                    eclType = "Annular";
                else if ((val & 16) == 16) // SE_ECL_PARTIAL
                    eclType = "Partial";
                else if ((val & 32) == 32) // SE_ECL_ANNULAR_TOTAL
                    eclType = "Hybrid";
                else
                    eclType = "Other";

                // save the beginning time of the eclipse
                if (i == 0) {
                    if (back == 0)
                        firstEcl = data1[3];
                    else
                        lastEcl = data1[4];
                }
                // save the ending time of the eclipse
                if (i == 9) {
                    if (back == 0)
                        lastEcl = data1[4];
                    else
                        firstEcl = data1[3];
                }

                if (Math.abs(data2[1] - data1[1]) <= 1.0) {
                    // if local eclipse time is within one day of the global
                    // time, then eclipse is visible locally

                    // create type string use data2[0] (local type)
                    val = (int) data2[0];
                    if ((val & 4) == 4) // SE_ECL_TOTAL
                        eclType += "|Total";
                    else if ((val & 8) == 8) // SE_ECL_ANNULAR
                        eclType += "|Annular";
                    else if ((val & 16) == 16) // SE_ECL_PARTIAL
                        eclType += "|Partial";
                    else if ((val & 32) == 32) // SE_ECL_ANNULAR_TOTAL
                        eclType += "|Hybrid";
                    else
                        eclType += "|Other";

                    sunset = riseSet.getSet(data2[2], 0);
                    sunrise = riseSet.getRise(sunset - 1, 0);

                    values.put(SolarEclipseTable.COLUMN_LOCAL_TYPE, (int) data2[0]);
                    values.put(SolarEclipseTable.COLUMN_GLOBAL_TYPE, (int) data1[0]);
                    values.put(SolarEclipseTable.COLUMN_LOCAL, 1);
                    values.put(SolarEclipseTable.COLUMN_LOCAL_MAX, data2[1]);
                    values.put(SolarEclipseTable.COLUMN_LOCAL_FIRST, data2[2]);
                    values.put(SolarEclipseTable.COLUMN_LOCAL_SECOND, data2[3]);
                    values.put(SolarEclipseTable.COLUMN_LOCAL_THIRD, data2[4]);
                    values.put(SolarEclipseTable.COLUMN_LOCAL_FOURTH, data2[5]);
                    values.put(SolarEclipseTable.COLUMN_SUNRISE, sunrise);
                    values.put(SolarEclipseTable.COLUMN_SUNSET, sunset);
                    values.put(SolarEclipseTable.COLUMN_RATIO, data2[7]);
                    values.put(SolarEclipseTable.COLUMN_FRACTION_COVERED, data2[8]);
                    values.put(SolarEclipseTable.COLUMN_SUN_AZ, data2[10]);
                    values.put(SolarEclipseTable.COLUMN_SUN_ALT, data2[11]);
                    values.put(SolarEclipseTable.COLUMN_LOCAL_MAG, data2[14]);
                    values.put(SolarEclipseTable.COLUMN_SAROS_NUM, (int) data2[15]);
                    values.put(SolarEclipseTable.COLUMN_SAROS_MEMBER_NUM, (int) data2[16]);
                    values.put(SolarEclipseTable.COLUMN_MOON_AZ, data2[17]);
                    values.put(SolarEclipseTable.COLUMN_MOON_ALT, data2[18]);
                    values.put(SolarEclipseTable.COLUMN_GLOBAL_MAX, data1[1]);
                    values.put(SolarEclipseTable.COLUMN_GLOBAL_BEGIN, data1[3]);
                    values.put(SolarEclipseTable.COLUMN_GLOBAL_END, data1[4]);
                    values.put(SolarEclipseTable.COLUMN_GLOBAL_TOTAL_BEGIN, data1[5]);
                    values.put(SolarEclipseTable.COLUMN_GLOBAL_TOTAL_END, data1[6]);
                    values.put(SolarEclipseTable.COLUMN_GLOBAL_CENTER_BEGIN, data1[7]);
                    values.put(SolarEclipseTable.COLUMN_GLOBAL_CENTER_END, data1[8]);
                    values.put(SolarEclipseTable.COLUMN_ECLIPSE_DATE, data2[1]);
                    values.put(SolarEclipseTable.COLUMN_ECLIPSE_TYPE, eclType);

                    if (back == 0)
                        start = data1[4];
                    else
                        start = data1[3];

                    data2 = solarDataLocal(start, g, back);
                    if (data2 == null) {
                        Log.e("Solar Eclipse error",
                                "computeEclipses data2a error");
                        if (getTargetFragment() != null) {
                            getTargetFragment().onActivityResult(
                                    SolarEclipse.TASK_FRAGMENT, 300, null);
                        }
                        break;
                    }
                } else {
                    // Global Eclipse
                    values.put(SolarEclipseTable.COLUMN_LOCAL_TYPE, -1);
                    values.put(SolarEclipseTable.COLUMN_GLOBAL_TYPE, (int) data1[0]);
                    values.put(SolarEclipseTable.COLUMN_LOCAL, 0);
                    values.put(SolarEclipseTable.COLUMN_LOCAL_MAX, -1);
                    values.put(SolarEclipseTable.COLUMN_LOCAL_FIRST, -1);
                    values.put(SolarEclipseTable.COLUMN_LOCAL_SECOND, -1);
                    values.put(SolarEclipseTable.COLUMN_LOCAL_THIRD, -1);
                    values.put(SolarEclipseTable.COLUMN_LOCAL_FOURTH, -1);
                    values.put(SolarEclipseTable.COLUMN_SUNRISE, -1);
                    values.put(SolarEclipseTable.COLUMN_SUNSET, -1);
                    values.put(SolarEclipseTable.COLUMN_RATIO, -1);
                    values.put(SolarEclipseTable.COLUMN_FRACTION_COVERED, -1);
                    values.put(SolarEclipseTable.COLUMN_SUN_AZ, -1);
                    values.put(SolarEclipseTable.COLUMN_SUN_ALT, -1);
                    values.put(SolarEclipseTable.COLUMN_LOCAL_MAG, -1);
                    values.put(SolarEclipseTable.COLUMN_SAROS_NUM, -1);
                    values.put(SolarEclipseTable.COLUMN_SAROS_MEMBER_NUM, -1);
                    values.put(SolarEclipseTable.COLUMN_MOON_AZ, -1);
                    values.put(SolarEclipseTable.COLUMN_MOON_ALT, -1);
                    values.put(SolarEclipseTable.COLUMN_GLOBAL_MAX, data1[1]);
                    values.put(SolarEclipseTable.COLUMN_GLOBAL_BEGIN, data1[3]);
                    values.put(SolarEclipseTable.COLUMN_GLOBAL_END, data1[4]);
                    values.put(SolarEclipseTable.COLUMN_GLOBAL_TOTAL_BEGIN, data1[5]);
                    values.put(SolarEclipseTable.COLUMN_GLOBAL_TOTAL_END, data1[6]);
                    values.put(SolarEclipseTable.COLUMN_GLOBAL_CENTER_BEGIN, data1[7]);
                    values.put(SolarEclipseTable.COLUMN_GLOBAL_CENTER_END, data1[8]);
                    values.put(SolarEclipseTable.COLUMN_ECLIPSE_DATE, data1[1]);
                    values.put(SolarEclipseTable.COLUMN_ECLIPSE_TYPE, eclType);

                    if (back == 0)
                        start = data1[4];
                    else
                        start = data1[3];
                }
                planetsDB.addSolarEclipse(values, i);
                publishProgress(i);
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

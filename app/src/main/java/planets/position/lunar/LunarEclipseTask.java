/*
 * Planet's Position
 * A program to calculate the position of the planets in the night sky based
 * on a given location on Earth.
 * Copyright (C) 2016  Tim Gaddis
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

import android.app.Activity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import planets.position.PlanetsMain;
import planets.position.R;
import planets.position.database.LunarEclipseTable;
import planets.position.database.PlanetsDatabase;
import planets.position.util.RiseSet;

public class LunarEclipseTask extends DialogFragment {

    private ComputeEclipseTask mTask;
    private double firstEcl, lastEcl, startTime, backward;
    private double[] g;
    private ProgressBar pb;
    private RiseSet riseSet;
    private SharedPreferences settings;

    // load c library
    static {
        System.loadLibrary("planets_swiss");
    }

    // c function prototypes
    @SuppressWarnings("JniMissingFunction")
    public native double[] lunarDataLocal(byte[] eph, double d2, double[] loc, int back);

    @SuppressWarnings("JniMissingFunction")
    public native double[] lunarDataGlobal(byte[] eph, double d2, int back);

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
        super.onCreate(savedInstanceState);

        settings = getActivity().getSharedPreferences(PlanetsMain.MAIN_PREFS, 0);
        setRetainInstance(true);
        if (mTask != null)
            mTask.execute(startTime, backward);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        TextView tv;
        View v = inflater.inflate(R.layout.progress_dialog_hor, container,
                false);
        tv = (TextView) v.findViewById(R.id.progress_text);
        pb = (ProgressBar) v.findViewById(R.id.progressBar);
        tv.setText(R.string.eclipse_dialog);
        pb.setMax(10);
        getDialog().setCanceledOnTouchOutside(false);
        riseSet = new RiseSet(g);
        riseSet.setEphPath(settings.getString("ephPath", ""));
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
            getTargetFragment().onActivityResult(LunarEclipse.TASK_FRAGMENT,
                    Activity.RESULT_OK, data);
        }
    }

    public class ComputeEclipseTask extends AsyncTask<Double, Integer, Void> {

        LunarEclipseTask mFragment;
        PlanetsDatabase planetsDB;
        private ContentValues values;
        private String eclType;

        void setFragment(LunarEclipseTask fragment) {
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
            double start, moonrise, moonset;
            double[] data1, data2;
            int back, i;

            planetsDB.open();
            back = (int) Math.round(params[1]);
            start = params[0];

            // compute first local eclipse
            data2 = lunarDataLocal(settings.getString("ephPath", "").getBytes(), start, g, back);
            if (data2 == null) {
                Log.e("Lunar Eclipse error", "lunarDataLocal data2 error");
                getTargetFragment().onActivityResult(
                        LunarEclipse.TASK_FRAGMENT, 100, null);
                return null;
            }

            for (i = 0; i < 10; i++) {
                if (this.isCancelled()) {
                    getTargetFragment().onActivityResult(
                            LunarEclipse.TASK_FRAGMENT,
                            Activity.RESULT_CANCELED, null);
                    break;
                }
                values.clear();

                // Global Eclipse Calculation
                data1 = lunarDataGlobal(settings.getString("ephPath", "").getBytes(), start, back);
                if (data1 == null) {
                    Log.e("Lunar Eclipse error", "lunarDataGlobal data1 error");
                    getTargetFragment().onActivityResult(
                            LunarEclipse.TASK_FRAGMENT, 200, null);
                    break;
                }
                // save the beginning time of the eclipse
                if (i == 0)
                    if (back == 0)
                        firstEcl = data1[7];
                    else
                        lastEcl = data1[8];
                // save the ending time of the eclipse
                if (i == 9)
                    if (back == 0)
                        lastEcl = data1[8];
                    else
                        firstEcl = data1[7];

                // create type string use data1[0]
                int val = (int) data1[0];
                if ((val & 4) == 4) // SE_ECL_TOTAL
                    eclType = "Total";
                else if ((val & 64) == 64) // SE_ECL_PENUMBRAL
                    eclType = "Penumbral";
                else if ((val & 16) == 16) // SE_ECL_PARTIAL
                    eclType = "Partial";
                else
                    eclType = "Other";

                if (Math.abs(data2[1] - data1[1]) <= 1.0) {
                    // if local eclipse time is within one day of the
                    // global time, then eclipse is visible locally

                    moonset = riseSet.getSet(data1[7], 1);
                    moonrise = riseSet.getRise(moonset - 1.0, 1);

                    values.put(LunarEclipseTable.COLUMN_LOCAL_TYPE,
                            (int) data2[0]);
                    values.put(LunarEclipseTable.COLUMN_GLOBAL_TYPE,
                            (int) data1[0]);
                    values.put(LunarEclipseTable.COLUMN_LOCAL, 1);
                    values.put(LunarEclipseTable.COLUMN_UMBRAL_MAG, data2[11]);
                    values.put(LunarEclipseTable.COLUMN_PENUMBRAL_MAG,
                            data2[12]);
                    values.put(LunarEclipseTable.COLUMN_MOON_AZ, data2[15]);
                    values.put(LunarEclipseTable.COLUMN_MOON_ALT, data2[17]);
                    values.put(LunarEclipseTable.COLUMN_MOONRISE, moonrise);
                    values.put(LunarEclipseTable.COLUMN_MOONSET, moonset);
                    values.put(LunarEclipseTable.COLUMN_SAROS_NUM,
                            (int) data2[20]);
                    values.put(LunarEclipseTable.COLUMN_SAROS_MEMBER_NUM,
                            (int) data2[21]);
                    values.put(LunarEclipseTable.COLUMN_MAX_ECLIPSE, data1[1]);
                    values.put(LunarEclipseTable.COLUMN_PARTIAL_BEGIN, data1[3]);
                    values.put(LunarEclipseTable.COLUMN_PARTIAL_END, data1[4]);
                    values.put(LunarEclipseTable.COLUMN_TOTAL_BEGIN, data1[5]);
                    values.put(LunarEclipseTable.COLUMN_TOTAL_END, data1[6]);
                    values.put(LunarEclipseTable.COLUMN_PENUMBRAL_BEGIN,
                            data1[7]);
                    values.put(LunarEclipseTable.COLUMN_PENUMBRAL_END, data1[8]);
                    values.put(LunarEclipseTable.COLUMN_ECLIPSE_DATE, data1[1]);
                    values.put(LunarEclipseTable.COLUMN_ECLIPSE_TYPE, eclType);

                    if (back == 0)
                        start = data1[8];
                    else
                        start = data1[7];

                    data2 = lunarDataLocal(settings.getString("ephPath", "").getBytes(), start, g, back);
                    if (data2 == null) {
                        Log.e("Lunar Eclipse error",
                                "computeEclipses data2a error");
                        getTargetFragment().onActivityResult(
                                LunarEclipse.TASK_FRAGMENT, 300, null);
                        break;
                    }
                } else {
                    // Global Eclipse
                    values.put(LunarEclipseTable.COLUMN_LOCAL_TYPE, -1);
                    values.put(LunarEclipseTable.COLUMN_GLOBAL_TYPE,
                            (int) data1[0]);
                    values.put(LunarEclipseTable.COLUMN_LOCAL, 0);
                    values.put(LunarEclipseTable.COLUMN_UMBRAL_MAG, -1);
                    values.put(LunarEclipseTable.COLUMN_PENUMBRAL_MAG, -1);
                    values.put(LunarEclipseTable.COLUMN_MOON_AZ, -1);
                    values.put(LunarEclipseTable.COLUMN_MOON_ALT, -1);
                    values.put(LunarEclipseTable.COLUMN_MOONRISE, -1);
                    values.put(LunarEclipseTable.COLUMN_MOONSET, -1);
                    values.put(LunarEclipseTable.COLUMN_SAROS_NUM, -1);
                    values.put(LunarEclipseTable.COLUMN_SAROS_MEMBER_NUM, -1);
                    values.put(LunarEclipseTable.COLUMN_MAX_ECLIPSE, data1[1]);
                    values.put(LunarEclipseTable.COLUMN_PARTIAL_BEGIN, data1[3]);
                    values.put(LunarEclipseTable.COLUMN_PARTIAL_END, data1[4]);
                    values.put(LunarEclipseTable.COLUMN_TOTAL_BEGIN, data1[5]);
                    values.put(LunarEclipseTable.COLUMN_TOTAL_END, data1[6]);
                    values.put(LunarEclipseTable.COLUMN_PENUMBRAL_BEGIN,
                            data1[7]);
                    values.put(LunarEclipseTable.COLUMN_PENUMBRAL_END, data1[8]);
                    values.put(LunarEclipseTable.COLUMN_ECLIPSE_DATE, data1[1]);
                    values.put(LunarEclipseTable.COLUMN_ECLIPSE_TYPE, eclType);

                    if (back == 0)
                        start = data1[8];
                    else
                        start = data1[7];
                }
                planetsDB.addLunarEclipse(values, i);
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

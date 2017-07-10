/*
 * Planet's Position
 * A program to calculate the position of the planets in the night sky based
 * on a given location on Earth.
 * Copyright (c) 2017 Tim Gaddis
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
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

import planets.position.PlanetsMain;
import planets.position.R;
import planets.position.database.LunarOccultationTable;
import planets.position.database.PlanetsDatabase;
import planets.position.util.RiseSet;

public class LunarOccultTask extends DialogFragment {

    private ComputeOccultTask mTask;
    private double firstEcl, lastEcl, startTime, backward;
    private boolean allPlanets = false;
    private double[] g;
    private int planetNum;
    private List<String> planetArray;
    private RiseSet riseSet;
    private ProgressBar pb;
    private TextView tv;
    private SharedPreferences settings;

    // load c library
    static {
        System.loadLibrary("planets_swiss");
    }

    // c function prototypes
    @SuppressWarnings("JniMissingFunction")
    public native double[] lunarOccultLocal(String eph, double d2, double[] loc,
                                            int planet, int back);

    @SuppressWarnings("JniMissingFunction")
    public native double[] lunarOccultGlobal(String eph, double d2, int planet, int back);

    public void setData(ComputeOccultTask task, double[] loc, double time,
                        double back, int planet) {
        mTask = task;
        g = loc;
        startTime = time;
        backward = back;
        planetNum = planet;
        mTask.setFragment(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        settings = getActivity().getSharedPreferences(PlanetsMain.MAIN_PREFS, 0);
        if (mTask != null)
            mTask.execute(startTime, backward);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.progress_dialog_hor, container,
                false);
        tv = (TextView) v.findViewById(R.id.progress_text);
        pb = (ProgressBar) v.findViewById(R.id.progressBar);
        if (planetNum < 2)
            pb.setMax(8);
        else
            pb.setMax(10);
        pb.getProgressDrawable().setColorFilter(Color.LTGRAY, PorterDuff.Mode.SRC_IN);
        planetArray = Arrays.asList(getResources().getStringArray(
                R.array.planets_array));
        riseSet = new RiseSet(g);
        riseSet.setEphPath(settings.getString("ephPath", ""));
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

//    public Status getStatus() {
//        return mTask.getStatus();
//    }

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
            b.putBoolean("allPlanets", allPlanets);
            data.putExtras(b);
            getTargetFragment().onActivityResult(
                    LunarOccultation.TASK_FRAGMENT, Activity.RESULT_OK, data);
        }
    }

    public class ComputeOccultTask extends AsyncTask<Double, Integer, Void> {

        LunarOccultTask mFragment;
        PlanetsDatabase planetsDB;
        private ContentValues values;

        void setFragment(LunarOccultTask fragment) {
            mFragment = fragment;
        }

        @Override
        protected void onPreExecute() {
            values = new ContentValues();
            planetsDB = new PlanetsDatabase(mFragment.getActivity().getApplicationContext());
        }

        protected void onProgressUpdate(Integer... values) {
            pb.setProgress(values[0]);
            if (values[2] == 0)
                tv.setText(String.format("Calculating the first\noccultation for %s",
                        planetArray.get(values[1])));
            else
                tv.setText(String.format("Calculating occultations for %s",
                        planetArray.get(values[1])));
        }

        @Override
        protected Void doInBackground(Double... params) {
            double start, moonrise, moonset;
            double[] data1, data2;
            int back, i;

            planetsDB.open();
            back = (int) Math.round(params[1]);
            start = params[0];

            if (planetNum > 1) {
                // compute occultations for the given planet
                publishProgress(0, planetNum, 1);
                allPlanets = false;
                // compute first local eclipse
                data2 = lunarOccultLocal(settings.getString("ephPath", ""), start, g, planetNum, back);
                if (data2 == null) {
                    Log.e("Lunar Occultation error",
                            "lunarOccultLocal data2 error");
                    getTargetFragment().onActivityResult(
                            LunarOccultation.TASK_FRAGMENT, 100, null);
                    return null;
                }

                for (i = 0; i < 10; i++) {
                    if (this.isCancelled()) {
                        getTargetFragment().onActivityResult(
                                LunarOccultation.TASK_FRAGMENT,
                                Activity.RESULT_CANCELED, null);
                        break;
                    }
                    values.clear();

                    // Global Occultation Calculation
                    data1 = lunarOccultGlobal(settings.getString("ephPath", ""), start, planetNum, back);
                    if (data1 == null) {
                        Log.e("Lunar Occultation error",
                                "lunarOccultGlobal data1 error");
                        getTargetFragment().onActivityResult(
                                LunarOccultation.TASK_FRAGMENT, 200, null);
                        break;
                    }
                    // save the beginning time of the occultation
                    if (i == 0)
                        if (back == 0)
                            firstEcl = data1[3];
                        else
                            lastEcl = data1[4];
                    // save the ending time of the occultation
                    if (i == 9)
                        if (back == 0)
                            lastEcl = data1[4];
                        else
                            firstEcl = data1[3];
                    if (Math.abs(data2[1] - data1[1]) <= 1.0) {
                        // if local occultation time is within one day of the
                        // global time, then occultation is visible locally

                        moonset = riseSet.getSet(data1[3], 1);
                        moonrise = riseSet.getRise(moonset - 1.0, 1);

                        values.put(LunarOccultationTable.COLUMN_LOCAL_TYPE,
                                (int) data2[0]);
                        values.put(LunarOccultationTable.COLUMN_GLOBAL_TYPE,
                                (int) data1[0]);
                        values.put(LunarOccultationTable.COLUMN_LOCAL, 1);
                        values.put(LunarOccultationTable.COLUMN_LOCAL_MAX,
                                data2[1]);
                        values.put(LunarOccultationTable.COLUMN_LOCAL_FIRST,
                                data2[2]);
                        values.put(LunarOccultationTable.COLUMN_LOCAL_SECOND,
                                data2[3]);
                        values.put(LunarOccultationTable.COLUMN_LOCAL_THIRD,
                                data2[4]);
                        values.put(LunarOccultationTable.COLUMN_LOCAL_FOURTH,
                                data2[5]);
                        values.put(LunarOccultationTable.COLUMN_MOONRISE, moonrise);
                        values.put(LunarOccultationTable.COLUMN_MOONSET, moonset);
                        values.put(LunarOccultationTable.COLUMN_MOONS_AZ,
                                data2[11]);
                        values.put(LunarOccultationTable.COLUMN_MOONS_ALT,
                                data2[12]);
                        values.put(LunarOccultationTable.COLUMN_MOONE_AZ,
                                data2[13]);
                        values.put(LunarOccultationTable.COLUMN_MOONE_ALT,
                                data2[14]);
                        values.put(LunarOccultationTable.COLUMN_GLOBAL_MAX,
                                data1[1]);
                        values.put(LunarOccultationTable.COLUMN_GLOBAL_BEGIN,
                                data1[3]);
                        values.put(LunarOccultationTable.COLUMN_GLOBAL_END,
                                data1[4]);
                        values.put(
                                LunarOccultationTable.COLUMN_GLOBAL_TOTAL_BEGIN,
                                data1[5]);
                        values.put(
                                LunarOccultationTable.COLUMN_GLOBAL_TOTAL_END,
                                data1[6]);
                        values.put(
                                LunarOccultationTable.COLUMN_GLOBAL_CENTER_BEGIN,
                                data1[7]);
                        values.put(
                                LunarOccultationTable.COLUMN_GLOBAL_CENTER_END,
                                data1[8]);
                        values.put(LunarOccultationTable.COLUMN_OCCULT_DATE,
                                data2[1]);
                        values.put(LunarOccultationTable.COLUMN_OCCULT_PLANET,
                                planetNum);

                        if (back == 0)
                            start = data1[1] + 2.0;
                        else
                            start = data1[1] - 2.0;

                        data2 = lunarOccultLocal(settings.getString("ephPath", ""), start, g, planetNum, back);
                        if (data2 == null) {
                            Log.e("Lunar Occultation error",
                                    "computeOccultations data2a error");
                            getTargetFragment().onActivityResult(
                                    LunarOccultation.TASK_FRAGMENT, 300, null);
                            break;
                        }
                    } else {
                        // Global Occultation
                        values.put(LunarOccultationTable.COLUMN_LOCAL_TYPE, -1);
                        values.put(LunarOccultationTable.COLUMN_GLOBAL_TYPE,
                                (int) data1[0]);
                        values.put(LunarOccultationTable.COLUMN_LOCAL, 0);
                        values.put(LunarOccultationTable.COLUMN_LOCAL_MAX, -1);
                        values.put(LunarOccultationTable.COLUMN_LOCAL_FIRST, -1);
                        values.put(LunarOccultationTable.COLUMN_LOCAL_SECOND,
                                -1);
                        values.put(LunarOccultationTable.COLUMN_LOCAL_THIRD, -1);
                        values.put(LunarOccultationTable.COLUMN_LOCAL_FOURTH,
                                -1);
                        values.put(LunarOccultationTable.COLUMN_MOONRISE, -1);
                        values.put(LunarOccultationTable.COLUMN_MOONSET, -1);
                        values.put(LunarOccultationTable.COLUMN_MOONS_AZ, -1);
                        values.put(LunarOccultationTable.COLUMN_MOONS_ALT, -1);
                        values.put(LunarOccultationTable.COLUMN_MOONE_AZ, -1);
                        values.put(LunarOccultationTable.COLUMN_MOONE_ALT, -1);
                        values.put(LunarOccultationTable.COLUMN_GLOBAL_MAX,
                                data1[1]);
                        values.put(LunarOccultationTable.COLUMN_GLOBAL_BEGIN,
                                data1[3]);
                        values.put(LunarOccultationTable.COLUMN_GLOBAL_END,
                                data1[4]);
                        values.put(
                                LunarOccultationTable.COLUMN_GLOBAL_TOTAL_BEGIN,
                                data1[5]);
                        values.put(
                                LunarOccultationTable.COLUMN_GLOBAL_TOTAL_END,
                                data1[6]);
                        values.put(
                                LunarOccultationTable.COLUMN_GLOBAL_CENTER_BEGIN,
                                data1[7]);
                        values.put(
                                LunarOccultationTable.COLUMN_GLOBAL_CENTER_END,
                                data1[8]);
                        values.put(LunarOccultationTable.COLUMN_OCCULT_DATE,
                                data1[1]);
                        values.put(LunarOccultationTable.COLUMN_OCCULT_PLANET,
                                planetNum);

                        if (back == 0)
                            start = data1[1] + 2.0;
                        else
                            start = data1[1] - 2.0;
                    }
                    planetsDB.addLunarOccult(values, i);
                    publishProgress(i + 1, planetNum, 1);
                }
            } else {
                // compute occultations for all planets
                firstEcl = -1;
                lastEcl = -1;
                allPlanets = true;
                publishProgress(0, 2, 0);
                for (i = 0; i < 8; i++) {
                    if (this.isCancelled()) {
                        getTargetFragment().onActivityResult(
                                LunarOccultation.TASK_FRAGMENT,
                                Activity.RESULT_CANCELED, null);
                        break;
                    }
                    values.clear();

                    // Local Occultation Calculation
                    data2 = lunarOccultLocal(settings.getString("ephPath", ""), start, g, i + 2, back);
                    if (data2 == null) {
                        Log.e("Lunar Occultation error",
                                "lunarOccultLocal data2 error");
                        getTargetFragment().onActivityResult(
                                LunarOccultation.TASK_FRAGMENT, 400, null);
                        break;
                    }

                    // Global Occultation Calculation
                    data1 = lunarOccultGlobal(settings.getString("ephPath", ""), start, i + 2, back);
                    if (data1 == null) {
                        Log.e("Lunar Occultation error",
                                "lunarOccultGlobal data1 error");
                        getTargetFragment().onActivityResult(
                                LunarOccultation.TASK_FRAGMENT, 500, null);
                        break;
                    }

                    if (Math.abs(data2[1] - data1[1]) <= 1.0) {
                        // if local occultation time is within one day of the
                        // global time, then occultation is visible locally
                        values.put(LunarOccultationTable.COLUMN_LOCAL_TYPE,
                                (int) data2[0]);
                        values.put(LunarOccultationTable.COLUMN_GLOBAL_TYPE,
                                (int) data1[0]);
                        values.put(LunarOccultationTable.COLUMN_LOCAL, 1);
                        values.put(LunarOccultationTable.COLUMN_LOCAL_MAX,
                                data2[1]);
                        values.put(LunarOccultationTable.COLUMN_LOCAL_FIRST,
                                data2[2]);
                        values.put(LunarOccultationTable.COLUMN_LOCAL_SECOND,
                                data2[3]);
                        values.put(LunarOccultationTable.COLUMN_LOCAL_THIRD,
                                data2[4]);
                        values.put(LunarOccultationTable.COLUMN_LOCAL_FOURTH,
                                data2[5]);
                        values.put(LunarOccultationTable.COLUMN_MOONS_AZ,
                                data2[11]);
                        values.put(LunarOccultationTable.COLUMN_MOONS_ALT,
                                data2[12]);
                        values.put(LunarOccultationTable.COLUMN_MOONE_AZ,
                                data2[13]);
                        values.put(LunarOccultationTable.COLUMN_MOONE_ALT,
                                data2[14]);
                        values.put(LunarOccultationTable.COLUMN_GLOBAL_MAX,
                                data1[1]);
                        values.put(LunarOccultationTable.COLUMN_GLOBAL_BEGIN,
                                data1[3]);
                        values.put(LunarOccultationTable.COLUMN_GLOBAL_END,
                                data1[4]);
                        values.put(
                                LunarOccultationTable.COLUMN_GLOBAL_TOTAL_BEGIN,
                                data1[5]);
                        values.put(
                                LunarOccultationTable.COLUMN_GLOBAL_TOTAL_END,
                                data1[6]);
                        values.put(
                                LunarOccultationTable.COLUMN_GLOBAL_CENTER_BEGIN,
                                data1[7]);
                        values.put(
                                LunarOccultationTable.COLUMN_GLOBAL_CENTER_END,
                                data1[8]);
                        values.put(LunarOccultationTable.COLUMN_OCCULT_DATE,
                                data2[1]);
                        values.put(LunarOccultationTable.COLUMN_OCCULT_PLANET,
                                i + 2);
                    } else {
                        // Global Occultation
                        values.put(LunarOccultationTable.COLUMN_LOCAL_TYPE, -1);
                        values.put(LunarOccultationTable.COLUMN_GLOBAL_TYPE,
                                (int) data1[0]);
                        values.put(LunarOccultationTable.COLUMN_LOCAL, 0);
                        values.put(LunarOccultationTable.COLUMN_LOCAL_MAX, -1);
                        values.put(LunarOccultationTable.COLUMN_LOCAL_FIRST, -1);
                        values.put(LunarOccultationTable.COLUMN_LOCAL_SECOND,
                                -1);
                        values.put(LunarOccultationTable.COLUMN_LOCAL_THIRD, -1);
                        values.put(LunarOccultationTable.COLUMN_LOCAL_FOURTH,
                                -1);
                        values.put(LunarOccultationTable.COLUMN_MOONS_AZ, -1);
                        values.put(LunarOccultationTable.COLUMN_MOONS_ALT, -1);
                        values.put(LunarOccultationTable.COLUMN_MOONE_AZ, -1);
                        values.put(LunarOccultationTable.COLUMN_MOONE_ALT, -1);
                        values.put(LunarOccultationTable.COLUMN_GLOBAL_MAX,
                                data1[1]);
                        values.put(LunarOccultationTable.COLUMN_GLOBAL_BEGIN,
                                data1[3]);
                        values.put(LunarOccultationTable.COLUMN_GLOBAL_END,
                                data1[4]);
                        values.put(
                                LunarOccultationTable.COLUMN_GLOBAL_TOTAL_BEGIN,
                                data1[5]);
                        values.put(
                                LunarOccultationTable.COLUMN_GLOBAL_TOTAL_END,
                                data1[6]);
                        values.put(
                                LunarOccultationTable.COLUMN_GLOBAL_CENTER_BEGIN,
                                data1[7]);
                        values.put(
                                LunarOccultationTable.COLUMN_GLOBAL_CENTER_END,
                                data1[8]);
                        values.put(LunarOccultationTable.COLUMN_OCCULT_DATE,
                                data1[1]);
                        values.put(LunarOccultationTable.COLUMN_OCCULT_PLANET,
                                i + 2);
                    }
                    planetsDB.addLunarOccult(values, i + 2);
                    if (i + 1 < 8)
                        publishProgress(i + 1, i + 3, 0);
                }
                // clears values in database for sun and moon
                values.clear();
                values.put(LunarOccultationTable.COLUMN_OCCULT_PLANET, -1);
                planetsDB.addLunarOccult(values, 0);
                planetsDB.addLunarOccult(values, 1);
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

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

import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import planets.position.database.TimeZoneDB;

public class FileCopyTask extends DialogFragment {

    private FileCopyCallback mCallbacks;
    private CopyFilesTask mTask;
    private boolean mRunning = false;

    public interface FileCopyCallback {
        void onCopyFinished();
    }

    public void setTask(CopyFilesTask task) {
        mTask = task;
        mTask.setFragment(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // attach to PlanetsMain
        if (!(context instanceof FileCopyCallback)) {
            throw new IllegalStateException(
                    "Activity must implement the FileCopyCallback interface.");
        }
        mCallbacks = (FileCopyCallback) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        start();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.progress_dialog, container, false);
        View tv = v.findViewById(R.id.progress_text);
        ((TextView) tv).setText(R.string.copy_dialog);
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

    /**
     * Start the background task.
     */
    private void start() {
        if (!mRunning) {
            mTask.execute();
            mRunning = true;
        }
    }

    private void taskFinished() {
        if (isResumed())
            dismiss();
        mTask = null;

        if (mCallbacks != null) {
            mCallbacks.onCopyFinished();
        }
    }

    /**
     * AsyncTask to copy files from the assets directory to the sdcard.
     *
     * @author tgaddis
     */
    public class CopyFilesTask extends AsyncTask<Void, Void, Void> {

        FileCopyTask mFragment;

        void setFragment(FileCopyTask fragment) {
            mFragment = fragment;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                copyFile(TimeZoneDB.DB_NAME);
            } catch (IOException e) {
                Log.e("CopyFile error", e.getMessage());
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (mFragment == null)
                return;
            mRunning = false;
            mFragment.taskFinished();
        }
    }

    private void copyFile(String filename) throws IOException {
        InputStream myInput;
        OutputStream myOutput;

        String p = getActivity().getApplicationContext().getFilesDir().getAbsolutePath() +
                File.separator + "databases";

        File dir = new File(p);
        if (dir.mkdirs() || dir.isDirectory()) {
//            Log.d(PlanetsMain.TAG, "File: " + dir.getAbsolutePath() + File.separator + filename);
            File f = new File(dir.getAbsolutePath() + File.separator + filename);
            if (!f.exists()) {
                myInput = getActivity().getAssets().open(filename);
                myOutput = new FileOutputStream(f);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = myInput.read(buffer)) > 0) {
                    myOutput.write(buffer, 0, length);
                }
                // Close the streams
                myOutput.flush();
                myOutput.close();
                myInput.close();

            }
        }
    }
}

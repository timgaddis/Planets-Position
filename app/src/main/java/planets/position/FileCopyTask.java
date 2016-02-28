package planets.position;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // attach to PlanetsMain
        if (!(activity instanceof FragmentListener)) {
            throw new IllegalStateException(
                    "Activity must implement the FragmentListener interface.");
        }
        mCallbacks = (FileCopyCallback) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        start();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
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

    /**
     * Cancel the background task.
     */
    public void cancel() {
        if (mRunning) {
            mTask.cancel(false);
            mTask = null;
            mRunning = false;
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
            Log.d("CopyFilesTask", "in doInBackground");
            // copy the ephermeris files from assets folder to the sd card.
            try {
                // copyFile("seas_18.se1"); // 225440
                copyFile("semo_18.se1"); // 1305686
                copyFile("sepl_18.se1"); // 484065
            } catch (IOException e) {
                // e.printStackTrace();
                Log.e("CopyFile error", e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Log.d("CopyFilesTask", "in onPostExecute");
            if (mFragment == null)
                return;
            mRunning = false;
            mFragment.taskFinished();
        }
    }

    /**
     * copies the given files from the assets folder to the ephermeris folder in
     * internal storage
     */
    private void copyFile(String filename) throws IOException {
        InputStream myInput;
        OutputStream myOutput;

        String p = getActivity().getApplicationContext().getFilesDir().getAbsolutePath() +
                File.separator + "ephemeris";
        // /data/user/0/planets.position/files/ephemeris

        // check if ephemeris dir is in internal storage, if not create dir
        File dir = new File(p);
        if (dir.mkdirs() || dir.isDirectory()) {
            Log.d(PlanetsMain.TAG, "File: " + dir.getAbsolutePath() + File.separator + filename);
            File f = new File(dir + File.separator + filename);
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

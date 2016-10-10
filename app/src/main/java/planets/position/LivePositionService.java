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

package planets.position;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.Calendar;

import planets.position.util.JDUTC;
import planets.position.util.RiseSet;

public class LivePositionService extends Service {

    public static final String BROADCAST_ACTION = "planets.position.displayplanet";
    private double offset;
    private final double[] g = new double[3];
    private int planetNum;
    private final Handler handler = new Handler();
    private Intent intent;
    private SharedPreferences settings;
    private JDUTC jdUTC;
    private RiseSet riseSet;

    // load c library
    static {
        System.loadLibrary("planets_swiss");
    }

    // c function prototype
    @SuppressWarnings("JniMissingFunction")
    public native static double[] planetLiveData(String eph, double d2, int p,
                                                 double[] loc, double press, double temp);

    @Override
    public void onCreate() {
        super.onCreate();
        settings = getSharedPreferences(PlanetsMain.MAIN_PREFS, 0);
        intent = new Intent(BROADCAST_ACTION);
        jdUTC = new JDUTC();
        riseSet = new RiseSet(settings.getString("ephPath", ""));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        offset = intent.getDoubleExtra("offset", 0);
        g[0] = intent.getDoubleExtra("longitude", 0);
        g[1] = intent.getDoubleExtra("latitude", 0);
        g[2] = intent.getDoubleExtra("elevation", 0);
        planetNum = intent.getIntExtra("planetNum", 0);
        riseSet.setLocation(g);
        handler.removeCallbacks(sendUpdatesToUI);
        handler.postDelayed(sendUpdatesToUI, 1000); // 1 second
        return super.onStartCommand(intent, flags, startId);
    }

    private final Runnable sendUpdatesToUI = new Runnable() {
        public void run() {
            computeLocation();
            handler.postDelayed(this, 1000); // 1 second
        }
    };

    private void computeLocation() {
        int m;
        double[] d, data;
        double setT, riseT;
        Calendar utc = Calendar.getInstance();
        // convert local time to UTC time
        m = (int) (offset * 60);
        utc.add(Calendar.MINUTE, m * -1);

        d = jdUTC.utcjd(utc.get(Calendar.MONTH) + 1,
                utc.get(Calendar.DAY_OF_MONTH), utc.get(Calendar.YEAR),
                utc.get(Calendar.HOUR_OF_DAY), utc.get(Calendar.MINUTE),
                utc.get(Calendar.SECOND));
        if (d == null) {
            Log.e("UpdatePosition error", "pos date error");
            return;
        }
        // jdTT = d[0];
        // jdUT = d[1];

        data = planetLiveData(settings.getString("ephPath", ""), d[1], planetNum, g, 0.0, 0.0);
        if (data == null) {
            Log.e("UpdatePosition error", "planetLiveData error");
            return;
        }
        setT = riseSet.getSet(d[1], planetNum);
        if (setT < 0) {
            Log.e("UpdatePosition error", "planetLiveData set error");
            return;
        }
        riseT = riseSet.getRise(d[1], planetNum);
        if (riseT < 0) {
            Log.e("UpdatePosition error", "planetLiveData rise error");
            return;
        }
        intent.putExtra("data", data);
        intent.putExtra("setT", setT);
        intent.putExtra("riseT", riseT);
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(sendUpdatesToUI);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}

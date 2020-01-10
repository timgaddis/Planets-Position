/*
 * Planet's Position
 * A program to calculate the position of the planets in the night sky based
 * on a given location on Earth.
 * Copyright (c) 2020 Tim Gaddis
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

import android.util.Log;

import java.util.Calendar;

public class JDUTC {

    // load c library
    static {
        System.loadLibrary("planets_swiss");
    }

    // c function prototypes
    public native static double[] utc2jd(int m, int d, int y, int hr, int min,
                                         double sec);

    public native static String jd2utc(double jdate);

    /**
     * Calls the utc2jd JNI c function. Converts from an utc date to a jullian
     * date.
     *
     * @param m   month
     * @param d   day
     * @param y   year
     * @param hr  hour
     * @param min minute
     * @param sec second
     * @return array of 2 jullian dates, [ut1,tt]
     */
    public double[] utcjd(int m, int d, int y, int hr, int min, double sec) {
        return utc2jd(m, d, y, hr, min, sec);
    }

    /**
     * Returns the given Julian date in milliseconds in local time.
     *
     * @param jdate  The given date in Jullian format.
     * @param offset The UTC offset in minutes.
     * @return Long containing the milliseconds.
     */
    public long jdmills(double jdate, double offset) {
        String[] dateArr;
        Calendar utc = Calendar.getInstance();
        dateArr = jd2utc(jdate).split("_");
        utc.set(Integer.parseInt(dateArr[1]), Integer.parseInt(dateArr[2]) - 1,
                Integer.parseInt(dateArr[3]), Integer.parseInt(dateArr[4]),
                Integer.parseInt(dateArr[5]));
        utc.set(Calendar.MILLISECOND,
                (int) (Double.parseDouble(dateArr[6]) * 1000));
        // convert utc to local time
        utc.add(Calendar.MINUTE, (int) (offset));
        return utc.getTimeInMillis();
    }

    /**
     * Returns the given Jullian date in milliseconds in UTC time.
     *
     * @param jdate The given date in Jullian format.
     * @return Long containing the milliseconds.
     */
    public long jdmills(double jdate) {
        String[] dateArr;
        Calendar utc = Calendar.getInstance();
        dateArr = jd2utc(jdate).split("_");
        utc.set(Integer.parseInt(dateArr[1]), Integer.parseInt(dateArr[2]) - 1,
                Integer.parseInt(dateArr[3]), Integer.parseInt(dateArr[4]),
                Integer.parseInt(dateArr[5]));
        utc.set(Calendar.MILLISECOND,
                (int) (Double.parseDouble(dateArr[6]) * 1000));
        return utc.getTimeInMillis();
    }

    /**
     * Returns the the current time in UT1 and TT Jullian format.
     *
     * @param offset The UTC offset in minutes.
     * @return Double array containing the times with [0]=TT and [1]=UT1.
     */
    public double[] getCurrentTime(double offset) {
        double[] time;
        Calendar c;
        c = Calendar.getInstance();
        // convert local time to utc
        c.add(Calendar.MINUTE, (int) (offset * -1));
        time = utcjd(c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
                c.get(Calendar.YEAR), c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE), c.get(Calendar.SECOND));
        if (time == null) {
            Log.e("JDUTC getCurrentTime", "utcjd error");
        }
        return time;
    }
}

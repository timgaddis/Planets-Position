/*
 * Copyright (c) 2014. Tim Gaddis
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

package planets.position.Util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Locale;

public class PositionFormat {
    private SharedPreferences sharedPref;

    public PositionFormat(Context context) {
        super();
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
    }

    // Input value examples
    // RA: 5.458967510328336
    // DEC: 23.2260666095222
    // AZ: 298.3351453874998
    // ALT: 33.81055373204086

    /**
     * Format right ascension based on user preference.
     *
     * @param value Calculated right ascension value.
     * @return Formated string.
     */
    public String formatRA(double value) {
        int index;
        String output = "";
        Locale locale = Locale.getDefault();
        double ra, ras;
        int rah, ram;
        index = Integer.parseInt(sharedPref.getString("ra_format", "0"));
        switch (index) {
            case 0: // HH MM SS
                ra = value;
                rah = (int) ra;
                ra -= rah;
                ra *= 60;
                ram = (int) ra;
                ra -= ram;
                ras = ra * 60;
                output = String.format(locale, "%dh %dm %.0fs", rah, ram, ras);
                break;
            case 1: // HH MM.MM
                ra = value;
                rah = (int) ra;
                ra -= rah;
                ra *= 60;
                output = String.format(locale, "%dh %.2fm", rah, ra);
                break;
            case 2: // HH.HHHHHH
                output = String.format(locale, "%.6f\u00b0", value);
                break;
        }
        return output;
    }

    /**
     * Format declination based on user preference.
     *
     * @param value Calculated declination value.
     * @return Formated string.
     */
    public String formatDec(double value) {
        int index;
        String output = "";
        Locale locale = Locale.getDefault();
        double dec, decs;
        int decd, decm;
        char decSign;
        index = Integer.parseInt(sharedPref.getString("dec_format", "0"));
        switch (index) {
            case 0: // DD MM SS
                dec = value;
                if (dec < 0) {
                    decSign = '-';
                    dec *= -1;
                } else {
                    decSign = '+';
                }
                decd = (int) dec;
                dec -= decd;
                dec *= 60;
                decm = (int) dec;
                dec -= decm;
                decs = dec * 60;
                output = String.format(locale, "%c%d\u00b0 %d\' %.0f\"", decSign,
                        decd, decm, decs);
                break;
            case 1: // DD MM.MM
                dec = value;
                if (dec < 0) {
                    decSign = '-';
                    dec *= -1;
                } else {
                    decSign = '+';
                }
                decd = (int) dec;
                dec -= decd;
                dec *= 60;
                decm = (int) dec;
                dec -= decm;
                dec *= 60;
                output = String.format(locale, "%c%d\u00b0 %.2f\'", decSign, decd,
                        dec);
                break;
            case 2: // DD.DDDDDD
                dec = value;
                if (dec < 0) {
                    decSign = '-';
                    dec *= -1;
                } else {
                    decSign = '+';
                }
                output = String.format(locale, "%c%.6f\u00b0", decSign, dec);
                break;
        }
        return output;
    }

    /**
     * Format azimuth based on user preference.
     *
     * @param value Calculated azimuth value.
     * @return Formated string.
     */
    public String formatAZ(double value) {
        int index;
        String output = "";
        double az, azs;
        int azd, azm;
        Locale locale = Locale.getDefault();
        index = Integer.parseInt(sharedPref.getString("az_format", "0"));
        switch (index) {
            case 0: // DDD MM SS
                az = value;
                azd = (int) az;
                az -= azd;
                az *= 60;
                azm = (int) az;
                az -= azm;
                azs = az * 60;
                output = String.format(locale, "%d\u00b0 %dm %.0fs", azd, azm, azs);
                break;
            case 1: // DDD MM.MM
                az = value;
                azd = (int) az;
                az -= azd;
                az *= 60;
                output = String.format(locale, "%d\u00b0 %.2fm", azd, az);
                break;
            case 2: // DDD.DDDDDD
                output = String.format(locale, "%.6f\u00b0", value);
                break;
            case 3: // QDD.DQ
                double bearing;
                char q1;
                char q2;
                if (value > 90.0 && value < 270.0) {
                    q1 = 'S';
                    if (value <= 180.0) {
                        // Q2
                        bearing = 180.0 - value;
                        q2 = 'E';
                    } else {
                        // Q3
                        bearing = value - 180.0;
                        q2 = 'W';
                    }
                } else {
                    q1 = 'N';
                    if (value <= 90.0) {
                        // Q1
                        bearing = value;
                        q2 = 'E';
                    } else {
                        // Q4
                        bearing = 360.0 - value;
                        q2 = 'W';
                    }
                }
                output = String.format(locale, "%c %.1f\u00b0 %c", q1, bearing, q2);
                break;
        }
        return output;
    }

    /**
     * Format altitude based on user preference.
     *
     * @param value Calculated altitude value.
     * @return Formated string.
     */
    public String formatALT(double value) {
        int index;
        String output = "";
        Locale locale = Locale.getDefault();
        double alt, alts;
        int altd, altm;
        index = Integer.parseInt(sharedPref.getString("alt_format", "0"));
        switch (index) {
            case 0: // DD MM SS
                alt = value;
                altd = (int) alt;
                alt -= altd;
                alt *= 60;
                altm = (int) alt;
                alt -= altm;
                alts = alt * 60;
                output = String.format(locale, "%d\u00b0 %d\' %.0f\"", altd, altm,
                        alts);
                break;
            case 1: // DD MM.MM
                alt = value;
                altd = (int) alt;
                alt -= altd;
                alt *= 60;
                altm = (int) alt;
                alt -= altm;
                alt *= 60;
                output = String.format(locale, "%d\u00b0 %.2f\'", altd, alt);
                break;
            case 2: // DD.DDDDDD
                output = String.format(locale, "%.6f\u00b0", value);
                break;
        }
        return output;
    }
}

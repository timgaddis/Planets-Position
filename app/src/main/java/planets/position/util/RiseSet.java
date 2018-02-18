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

package planets.position.util;

public class RiseSet {

    private double[] g = new double[3];

    // load c library
    static {
        System.loadLibrary("planets_swiss");
    }

    // c function prototypes
    public native static double planetRise(double dUT, int p, double[] loc);

    public native static double planetSet(double dUT, int p, double[] loc);

    public native static double planetTransit(double dUT, int p, double[] loc);

    public RiseSet(double[] loc) {
        g = loc;
    }


    public void setLocation(double lat, double lng, double ele) {
        g[1] = lat;
        g[0] = lng;
        g[2] = ele;
    }

    public void setLocation(double[] loc) {
        g = loc;
    }

    public double getRise(double jdate, int planet) {
        return planetRise(jdate, planet, g);
    }

    public double getSet(double jdate, int planet) {
        return planetSet(jdate, planet, g);
    }

    public double getTransit(double jdate, int planet) {
        return planetTransit(jdate, planet, g);
    }
}

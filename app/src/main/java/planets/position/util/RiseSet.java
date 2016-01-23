package planets.position.util;

public class RiseSet {

    private double[] g = new double[3];
    private String ephPath;

    // load c library
    static {
        System.loadLibrary("planets_swiss");
    }

    // c function prototypes
    @SuppressWarnings("JniMissingFunction")
    public native static double planetRise(String eph, double dUT, int p, double[] loc, double press,
                                           double temp);

    @SuppressWarnings("JniMissingFunction")
    public native static double planetSet(String eph, double dUT, int p, double[] loc, double press,
                                          double temp);

    public RiseSet(double[] loc) {
        g = loc;
    }

    public RiseSet(String eph) {
        ephPath = eph;
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
        return planetRise(ephPath, jdate, planet, g, 0.0, 0.0);
    }

    public double getSet(double jdate, int planet) {
        return planetSet(ephPath, jdate, planet, g, 0.0, 0.0);
    }

}

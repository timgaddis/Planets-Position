#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <android/log.h>
#include "swiss/swephexp.h"

/*
 * Convert a calendar date ( year, month, day, hour, min, sec) to a Julian date.
 * Swiss Ephemeris function called:
 * 		swe_utc_to_jd
 * Input: year, month, day, hour, min, sec
 * Output: double array with Julian date in ut1 and tt values.
 */
jdoubleArray Java_planets_position_util_JDUTC_utc2jd(JNIEnv *env, jobject this,
                                                     jint m, jint d, jint y, jint hr, jint min,
                                                     jdouble sec) {

    char serr[256];
    double dret[2];
    int retval;
    jdoubleArray result;
    result = (*env)->NewDoubleArray(env, 5);
    if (result == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "Position_utc2jd",
                            "JNI ERROR NewDoubleArray: out of memory error");
        return NULL; /* out of memory error thrown */
    }

    retval = swe_utc_to_jd(y, m, d, hr, min, sec, SE_GREG_CAL, dret, serr);
    if (retval == ERR) {
        __android_log_print(ANDROID_LOG_ERROR, "Position_utc2jd",
                            "JNI ERROR swe_utc_to_jd: %-256s", serr);
        return NULL;
    }

    (*env)->SetDoubleArrayRegion(env, result, 0, 2, dret);
    return result;

}

/*
 * Covert a given Julian date to a calendar date in utc.
 * Swiss Ephemeris function called:
 * 		swe_jdut1_to_utc
 * Input: Julian date
 * Output: String containing a calendar date
 */
jstring Java_planets_position_util_JDUTC_jd2utc(JNIEnv *env, jobject this,
                                                jdouble juldate) {

    char *outFormat = "_%i_%i_%i_%i_%i_%2.1f_";
    char output[30];
    int i, y, mo, d, h, mi;
    double s;

    swe_jdut1_to_utc(juldate, SE_GREG_CAL, &y, &mo, &d, &h, &mi, &s);

    i = sprintf(output, outFormat, y, mo, d, h, mi, s);
    return (*env)->NewStringUTF(env, output);
}

/*
 * Return the rise time for a given planet at a given date.
 * Swiss Ephemeris function called:
 * 		swe_set_ephe_path
 * 		swe_rise_trans
 * 		swe_close
 * Input: Julian date in ut1, planet number, location array, atmospheric pressure and temperature
 * Output: Julian date as a double
 */
jdouble Java_planets_position_util_RiseSet_planetRise(JNIEnv *env, jobject this, jbyteArray eph,
                                                      jdouble d_ut, jint p, jdoubleArray loc,
                                                      jdouble atpress, jdouble attemp) {

    char serr[256];
    double g[3], riseT;
    int i;
    jboolean isCopy;
    char *ephString = (char *) (*env)->GetByteArrayElements(env, eph, &isCopy);

    (*env)->GetDoubleArrayRegion(env, loc, 0, 3, g);

    swe_set_ephe_path(ephString);

    i = swe_rise_trans(d_ut, p, "", SEFLG_SWIEPH, SE_CALC_RISE, g, atpress, attemp, &riseT, serr);
    if (i == ERR) {
        __android_log_print(ANDROID_LOG_ERROR, "planetRise", "JNI ERROR swe_rise_trans: %-256s",
                            serr);
        swe_close();
        return -1.0;
    }
    swe_close();
    if (isCopy) {
        (*env)->ReleaseByteArrayElements(env, eph, ephString, JNI_ABORT);
    }

    return riseT;
}

/*
 * Return the set time for a given planet at a given date.
 * Swiss Ephemeris function called:
 * 		swe_set_ephe_path
 * 		swe_rise_trans
 * 		swe_close
 * Input: Julian date in ut1, planet number, location array, atmospheric pressure and temperature
 * Output: Julian date as a double
 */
jdouble Java_planets_position_util_RiseSet_planetSet(JNIEnv *env, jobject this, jbyteArray eph,
                                                     jdouble d_ut, jint p, jdoubleArray loc,
                                                     jdouble atpress, jdouble attemp) {

    char serr[256];
    double g[3], setT;
    int i;
    jboolean isCopy;
    char *ephString = (char *) (*env)->GetByteArrayElements(env, eph, &isCopy);

    (*env)->GetDoubleArrayRegion(env, loc, 0, 3, g);

    swe_set_ephe_path(ephString);

    i = swe_rise_trans(d_ut, p, "", SEFLG_SWIEPH, SE_CALC_SET, g, atpress, attemp, &setT, serr);
    if (i == ERR) {
        __android_log_print(ANDROID_LOG_ERROR, "planetSet", "JNI ERROR swe_rise_trans: %-256s",
                            serr);
        swe_close();
        return -1.0;
    }
    swe_close();
    if (isCopy) {
        (*env)->ReleaseByteArrayElements(env, eph, ephString, JNI_ABORT);
    }

    return setT;
}

/*
 * Calculate the position of a given planet in the sky.
 * Swiss Ephemeris functions called:
 * 		swe_set_ephe_path
 * 		swe_set_topo
 * 		swe_calc
 * 		swe_azalt
 * 		swe_pheno_ut
 * 		swe_close
 * Input: Julian date in ephemeris time, Julian date in ut1, planet number,
 * 		location array, atmospheric pressure and temperature.
 * Output: Double array containing RA, Dec, distance, azimuth, altitude,
 * 		magnitude, set time, and rise time of planet.
 */
jdoubleArray Java_planets_position_SkyPosition_planetPosData(JNIEnv *env, jobject this,
                                                             jbyteArray eph,
                                                             jdouble d_et, jdouble d_ut, jint p,
                                                             jdoubleArray loc, jdouble atpress,
                                                             jdouble attemp) {

    char serr[256];
    double x2[3], az[3], g[3], attr[20], setT, riseT;
    int i, iflag = SEFLG_SWIEPH | SEFLG_EQUATORIAL | SEFLG_TOPOCTR;
    jboolean isCopy;
    jdoubleArray result;

    result = (*env)->NewDoubleArray(env, 8);
    if (result == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "planetPosData",
                            "JNI ERROR NewDoubleArray: out of memory error");
        return NULL; /* out of memory error thrown */
    }

    (*env)->GetDoubleArrayRegion(env, loc, 0, 3, g);

    char *ephString = (char *) (*env)->GetByteArrayElements(env, eph, &isCopy);
    swe_set_ephe_path(ephString);

    swe_set_topo(g[0], g[1], g[2]);
    i = swe_calc(d_et, p, iflag, x2, serr);
    if (i == ERR) {
        __android_log_print(ANDROID_LOG_ERROR, "planetPosData",
                            "JNI ERROR swe_calc: %-256s", serr);
        swe_close();
        return NULL;
    } else {
        swe_azalt(d_ut, SE_EQU2HOR, g, atpress, attemp, x2, az);
        i = swe_pheno_ut(d_ut, p, SEFLG_SWIEPH, attr, serr);
        if (i == ERR) {
            __android_log_print(ANDROID_LOG_ERROR, "planetPosData",
                                "JNI ERROR swe_pheno_ut: %-256s", serr);
            swe_close();
            return NULL;
        }
        swe_close();

        /*rotates azimuth origin to north*/
        az[0] += 180;
        if (az[0] > 360)
            az[0] -= 360;

        // move from the temp structure to the java structure
        (*env)->SetDoubleArrayRegion(env, result, 0, 3, x2);
        (*env)->SetDoubleArrayRegion(env, result, 3, 2, az);
        (*env)->SetDoubleArrayRegion(env, result, 5, 1, &attr[4]);

        if (isCopy) {
            (*env)->ReleaseByteArrayElements(env, eph, ephString, JNI_ABORT);
        }

        return result;
    }
}

/*
 * Calculate the position of a given planet in the sky.
 * Swiss Ephemeris functions called:
 * 		swe_set_ephe_path
 * 		swe_set_topo
 * 		swe_calc
 * 		swe_azalt
 * 		swe_pheno_ut
 * 		swe_close
 * Input: Julian date in ephemeris time, Julian date in ut1, planet number,
 * 		location array, atmospheric pressure and temperature.
 * Output: Double array containing RA, Dec, distance, azimuth, altitude,
 * 		magnitude, and set time of planet.
 */
jdoubleArray Java_planets_position_WhatsUpTask_planetUpData(JNIEnv *env, jobject this,
                                                            jbyteArray eph, jdouble d_et,
                                                            jdouble d_ut, jint p, jdoubleArray loc,
                                                            jdouble atpress, jdouble attemp) {

    char serr[256];
    double x2[3], az[3], g[3], attr[20];
    int i, iflag = SEFLG_SWIEPH | SEFLG_EQUATORIAL | SEFLG_TOPOCTR;
    jboolean isCopy;
    jdoubleArray result;

    result = (*env)->NewDoubleArray(env, 7);
    if (result == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "planetUpData",
                            "JNI ERROR NewDoubleArray: out of memory error");
        return NULL; /* out of memory error thrown */
    }

    (*env)->GetDoubleArrayRegion(env, loc, 0, 3, g);

    char *ephString = (char *) (*env)->GetByteArrayElements(env, eph, &isCopy);
    swe_set_ephe_path(ephString);

    swe_set_topo(g[0], g[1], g[2]);
    i = swe_calc(d_et, p, iflag, x2, serr);
    if (i == ERR) {
        __android_log_print(ANDROID_LOG_ERROR, "planetUpData",
                            "JNI ERROR swe_calc: %-256s", serr);
        swe_close();
        return NULL;
    } else {
        swe_azalt(d_ut, SE_EQU2HOR, g, atpress, attemp, x2, az);
        i = swe_pheno_ut(d_ut, p, SEFLG_SWIEPH, attr, serr);
        if (i == ERR) {
            __android_log_print(ANDROID_LOG_ERROR, "planetUpData",
                                "JNI ERROR swe_pheno_ut: %-256s", serr);
            swe_close();
            return NULL;
        }
        swe_close();

        /*rotates azimuth origin to north*/
        az[0] += 180;
        if (az[0] > 360)
            az[0] -= 360;

        // move from the temp structure to the java structure
        (*env)->SetDoubleArrayRegion(env, result, 0, 3, x2);
        (*env)->SetDoubleArrayRegion(env, result, 3, 2, az);
        (*env)->SetDoubleArrayRegion(env, result, 5, 1, &attr[4]);

        if (isCopy) {
            (*env)->ReleaseByteArrayElements(env, eph, ephString, JNI_ABORT);
        }

        return result;
    }
}

/*
 * Calculate the next solar eclipse globally after a given date.
 * Swiss Ephemeris functions called:
 * 		swe_set_ephe_path
 * 		swe_sol_eclipse_when_glob
 * 		swe_close
 * Input: Julian date in ut1, search direction(0=forward|1=back).
 * Output: Double array containing eclipse type and eclipse event times.
 */
jdoubleArray Java_planets_position_solar_SolarEclipseTask_solarDataGlobal(
        JNIEnv *env, jobject this, jbyteArray eph, jdouble d_ut, jint back) {

    char serr[256];
    double tret[10], rval;
    int retval;
    jboolean isCopy;
    jdoubleArray result;

    /*__android_log_print(ANDROID_LOG_INFO, "solarDataGlobal", "date: %f", d_ut);*/

    result = (*env)->NewDoubleArray(env, 9);
    if (result == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "solarDataGlobal",
                            "JNI ERROR NewDoubleArray: out of memory error");
        return NULL; /* out of memory error thrown */
    }

    char *ephString = (char *) (*env)->GetByteArrayElements(env, eph, &isCopy);
    swe_set_ephe_path(ephString);

    retval = swe_sol_eclipse_when_glob(d_ut, SEFLG_SWIEPH, 0, tret, back, serr);
    if (retval == ERR) {
        __android_log_print(ANDROID_LOG_ERROR, "solarDataGlobal",
                            "JNI ERROR swe_sol_eclipse_when_glob: %-256s", serr);
        swe_close();
        return NULL;
    }

    rval = retval * 1.0;
    swe_close();

    // move from the temp structure to the java structure
    (*env)->SetDoubleArrayRegion(env, result, 0, 1, &rval);
    (*env)->SetDoubleArrayRegion(env, result, 1, 8, tret);

    if (isCopy) {
        (*env)->ReleaseByteArrayElements(env, eph, ephString, JNI_ABORT);
    }

    return result;
}

/*
 * Calculate the next solar eclipse locally after a given date.
 * Swiss Ephemeris functions called:
 * 		swe_set_ephe_path
 * 		swe_set_topo
 * 		swe_sol_eclipse_when_loc
 * 		swe_calc_ut
 * 		swe_azalt
 * 		swe_close
 * Input: Julian date in ut1, location array, search direction(0=forward|1=back).
 * Output: Double array containing local eclipse type ,local eclipse event times,
 * 			eclipse attributes, and moon position
 */
jdoubleArray Java_planets_position_solar_SolarEclipseTask_solarDataLocal(
        JNIEnv *env, jobject this, jbyteArray eph, jdouble d_ut, jdoubleArray loc, jint back) {

    char serr[256];
    double g[3], attr[20], tret[10], az[6], x2[6], rval;
    int retval, i;
    int iflag = SEFLG_SWIEPH | SEFLG_EQUATORIAL | SEFLG_TOPOCTR;
    jboolean isCopy;
    jdoubleArray result;

    result = (*env)->NewDoubleArray(env, 19);
    if (result == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "solarDataLocal",
                            "JNI ERROR NewDoubleArray: out of memory error");
        return NULL; /* out of memory error thrown */
    }

    char *ephString = (char *) (*env)->GetByteArrayElements(env, eph, &isCopy);
    swe_set_ephe_path(ephString);

    (*env)->GetDoubleArrayRegion(env, loc, 0, 3, g);
    swe_set_topo(g[0], g[1], g[2]);

    retval = swe_sol_eclipse_when_loc(d_ut, SEFLG_SWIEPH, g, tret, attr, back,
                                      serr);
    if (retval == ERR) {
        __android_log_print(ANDROID_LOG_ERROR, "solarDataLocal",
                            "JNI ERROR swe_sol_eclipse_when_loc: %-256s", serr);
        swe_close();
        return NULL;
    } else {
        // rotate azimuth of sun 180 degrees
        attr[4] += 180;
        if (attr[4] >= 360)
            attr[4] -= 360;
        // calculate moon position at max eclipse
        i = swe_calc_ut(tret[0], 1, iflag, x2, serr);
        if (i == ERR) {
            __android_log_print(ANDROID_LOG_ERROR, "solarDataLocal",
                                "JNI ERROR swe_calc_ut: %-256s", serr);
            swe_close();
            return NULL;
        }
        swe_azalt(tret[0], SE_EQU2HOR, g, 0, 0, x2, az);
        // rotate azimuth of moon 180 degrees
        az[0] += 180;
        if (az[0] > 360)
            az[0] -= 360;

        rval = retval * 1.0;
        swe_close();

        // move from the temp structure to the java structure
        (*env)->SetDoubleArrayRegion(env, result, 0, 1, &rval);
        (*env)->SetDoubleArrayRegion(env, result, 1, 5, tret);
        (*env)->SetDoubleArrayRegion(env, result, 6, 11, attr);
        (*env)->SetDoubleArrayRegion(env, result, 17, 2, az);

        if (isCopy) {
            (*env)->ReleaseByteArrayElements(env, eph, ephString, JNI_ABORT);
        }

        return result;
    }
}

/*
 * Calculate the geographic position of where a solar eclipse occurs for a
 * 		given date.
 * Swiss Ephemeris functions called:
 * 		swe_set_ephe_path
 * 		swe_sol_eclipse_where
 * 		swe_close
 * Input: Julian date in ut1.
 * Output: Double array containing longitude and latitude, [lng,lat].
 */
jdoubleArray Java_planets_position_solar_SolarEclipseMap_solarMapPos(
        JNIEnv *env, jobject this, jbyteArray eph, jdouble d_ut) {

    char serr[256];
    double attr[20], g[2];
    int retval;
    jboolean isCopy;
    jdoubleArray result;

    result = (*env)->NewDoubleArray(env, 2);
    if (result == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "solarMapPos",
                            "JNI ERROR NewDoubleArray: out of memory error");
        return NULL; /* out of memory error thrown */
    }

    char *ephString = (char *) (*env)->GetByteArrayElements(env, eph, &isCopy);
    swe_set_ephe_path(ephString);

    swe_sol_eclipse_where(d_ut, SEFLG_SWIEPH, g, attr, serr);
    swe_close();

    // move from the temp structure to the java structure
    (*env)->SetDoubleArrayRegion(env, result, 0, 2, g);

    if (isCopy) {
        (*env)->ReleaseByteArrayElements(env, eph, ephString, JNI_ABORT);
    }

    return result;
}

/*
 * Calculate the next lunar eclipse globally after a given date.
 * Swiss Ephemeris functions called:
 * 		swe_set_ephe_path
 * 		swe_lun_eclipse_when
 * 		swe_close
 * Input: Julian date in ut1, search direction(0=forward|1=back).
 * Output: Double array containing eclipse type and eclipse event times.
 */
jdoubleArray Java_planets_position_lunar_LunarEclipseTask_lunarDataGlobal(
        JNIEnv *env, jobject this, jbyteArray eph, jdouble d_ut, jint back) {

    char serr[256];
    double tret[10], rval;
    int retval;
    jboolean isCopy;
    jdoubleArray result;

    result = (*env)->NewDoubleArray(env, 9);
    if (result == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "lunarDataGlobal",
                            "JNI ERROR NewDoubleArray: out of memory error");
        return NULL; /* out of memory error thrown */
    }

    char *ephString = (char *) (*env)->GetByteArrayElements(env, eph, &isCopy);
    swe_set_ephe_path(ephString);

    retval = swe_lun_eclipse_when(d_ut, SEFLG_SWIEPH, 0, tret, back, serr);
    if (retval == ERR) {
        __android_log_print(ANDROID_LOG_ERROR, "lunarDataGlobal",
                            "JNI ERROR swe_lun_eclipse_when: %-256s", serr);
        swe_close();
        return NULL;
    }

    rval = retval * 1.0;
    swe_close();

    // move from the temp structure to the java structure
    (*env)->SetDoubleArrayRegion(env, result, 0, 1, &rval);
    (*env)->SetDoubleArrayRegion(env, result, 1, 8, tret);

    if (isCopy) {
        (*env)->ReleaseByteArrayElements(env, eph, ephString, JNI_ABORT);
    }

    return result;
}

/*
 * Calculate the next lunar eclipse locally after a given date.
 * Swiss Ephemeris functions called:
 * 		swe_set_ephe_path
 * 		swe_lun_eclipse_when_loc
 * 		swe_close
 * Input: Julian date in ut1, location array, search direction(0=forward|1=back).
 * Output: Double array containing local eclipse type ,local eclipse event times
 * 			and eclipse attributes
 */
jdoubleArray Java_planets_position_lunar_LunarEclipseTask_lunarDataLocal(
        JNIEnv *env, jobject this, jbyteArray eph, jdouble d_ut, jdoubleArray loc, jint back) {

    char serr[256];
    double g[3], tret[10], attr[20], rval;
    int retval;
    jboolean isCopy;
    jdoubleArray result;

    result = (*env)->NewDoubleArray(env, 22);
    if (result == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "lunarDataLocal",
                            "JNI ERROR NewDoubleArray: out of memory error");
        return NULL; /* out of memory error thrown */
    }

    (*env)->GetDoubleArrayRegion(env, loc, 0, 3, g);

    char *ephString = (char *) (*env)->GetByteArrayElements(env, eph, &isCopy);
    swe_set_ephe_path(ephString);

    retval = swe_lun_eclipse_when_loc(d_ut, SEFLG_SWIEPH, g, tret, attr, back,
                                      serr);
    if (retval == ERR) {
        __android_log_print(ANDROID_LOG_ERROR, "lunarDataLocal",
                            "JNI ERROR swe_lun_eclipse_when_loc: %-256s", serr);
        swe_close();
        return NULL;
    }
    // rotate azimuth of moon 180 degrees
    attr[4] += 180;
    if (attr[4] >= 360)
        attr[4] -= 360;

    rval = retval * 1.0;
    swe_close();

    // move from the temp structure to the java structure
    (*env)->SetDoubleArrayRegion(env, result, 0, 1, &rval);
    (*env)->SetDoubleArrayRegion(env, result, 1, 10, tret);
    (*env)->SetDoubleArrayRegion(env, result, 11, 11, attr);

    if (isCopy) {
        (*env)->ReleaseByteArrayElements(env, eph, ephString, JNI_ABORT);
    }

    return result;
}

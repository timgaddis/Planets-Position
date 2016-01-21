
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
jdoubleArray Java_planets_position_Util_JDUTC_utc2jd(JNIEnv *env, jobject this,
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
jstring Java_planetsposition_Util_JDUTC_jd2utc(JNIEnv *env, jobject this,
                                                jdouble juldate) {

    char *outFormat = "_%i_%i_%i_%i_%i_%2.1f_";
    char output[30];
    int i, y, mo, d, h, mi;
    double s;

    swe_jdut1_to_utc(juldate, SE_GREG_CAL, &y, &mo, &d, &h, &mi, &s);

    i = sprintf(output, outFormat, y, mo, d, h, mi, s);
    return (*env)->NewStringUTF(env, output);
}

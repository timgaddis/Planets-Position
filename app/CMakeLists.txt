cmake_minimum_required(VERSION 3.4.1)

add_library(planets_swiss
            SHARED
            src/main/jni/planets_swiss.c
            src/main/jni/swiss/swecl.c
            src/main/jni/swiss/swedate.c
            src/main/jni/swiss/swedate.h
            src/main/jni/swiss/swehouse.c
            src/main/jni/swiss/swehouse.h
            src/main/jni/swiss/swejpl.c
            src/main/jni/swiss/swejpl.h
            src/main/jni/swiss/swemmoon.c
            src/main/jni/swiss/swemplan.c
            src/main/jni/swiss/swemptab.h
            src/main/jni/swiss/swenut2000a.h
            src/main/jni/swiss/sweodef.h
            src/main/jni/swiss/sweph.c
            src/main/jni/swiss/sweph.h
            src/main/jni/swiss/swephexp.h
            src/main/jni/swiss/swephlib.c
            src/main/jni/swiss/swephlib.h)

find_library( log-lib
              log )

target_link_libraries( planets_swiss
                       ${log-lib} )

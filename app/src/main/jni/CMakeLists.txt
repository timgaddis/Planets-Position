cmake_minimum_required(VERSION 3.4.1)

add_library(planets_swiss
        SHARED
        planets_swiss.c
        swiss/swecl.c
        swiss/swedate.c
        swiss/swedate.h
        swiss/swehouse.c
        swiss/swehouse.h
        swiss/swejpl.c
        swiss/swejpl.h
        swiss/swemmoon.c
        swiss/swemplan.c
        swiss/swemptab.h
        swiss/swenut2000a.h
        swiss/sweodef.h
        swiss/sweph.c
        swiss/sweph.h
        swiss/swephexp.h
        swiss/swephlib.c
        swiss/swephlib.h)

find_library(log-lib
        log)

target_link_libraries(planets_swiss
        ${log-lib})

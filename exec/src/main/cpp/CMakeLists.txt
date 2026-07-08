cmake_minimum_required(VERSION 3.10)
project(pty_native)

set(CMAKE_C_STANDARD 99)
set(CMAKE_C_STANDARD_REQUIRED ON)

add_library(pty_native SHARED
        pty_native.c
)

target_include_directories(pty_native PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

find_library(UTIL_LIBRARY util)
if (UTIL_LIBRARY)
    target_link_libraries(pty_native PRIVATE ${UTIL_LIBRARY})
endif ()

find_library(LOG_LIBRARY log)
if (LOG_LIBRARY)
    target_link_libraries(pty_native PRIVATE ${LOG_LIBRARY})
endif ()

target_compile_options(pty_native PRIVATE -Wall -Wextra -fPIC)

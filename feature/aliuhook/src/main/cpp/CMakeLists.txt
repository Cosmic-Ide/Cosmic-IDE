# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

cmake_minimum_required(VERSION 3.18.1)
project("aliuhook")

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
include_directories(xz/linux/lib/xz xz/linux/include/linux xz/userspace)

add_library(aliuhook SHARED
        aliuhook.cpp
        elf_img.cpp
        profile_saver.cpp
        hidden_api.cpp
        invoke_constructor.cpp
        xz/linux/lib/xz/xz_crc32.c
        xz/linux/lib/xz/xz_crc64.c
        xz/linux/lib/xz/xz_dec_bcj.c
        xz/linux/lib/xz/xz_dec_lzma2.c
        xz/linux/lib/xz/xz_dec_stream.c
)

find_package(lsplant REQUIRED CONFIG)
find_package(dobby REQUIRED CONFIG)

target_link_libraries(aliuhook log lsplant::lsplant dobby::dobby)

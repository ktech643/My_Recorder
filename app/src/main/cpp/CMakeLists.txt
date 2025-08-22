# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

#add_subdirectory("libmcdll")

add_definitions(-D_M64 -D_LIN -D_ANDROID -D_LIB_MODE -D_STREAM_MANAGER -D_NO_NV -DANDROID_STL=c++_shared)

set(distribution_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs)

message("SYSTEM: ${CMAKE_SYSTEM_NAME} ABI: ${ANDROID_ABI} (win32=${WIN32} unix=${UNIX})")
message("NDK: ${ANDROID_NDK})")
include_directories(
        ${distribution_DIR}/include
)
link_directories(
        ${distribution_DIR}/lib/${ANDROID_ABI}
)

add_library(native-lib SHARED native-lib.cpp)
find_library( gl-lib GLESv3 )
find_library( egl-lib EGL )

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++14")

target_link_libraries(native-lib android mcdll log  ${gl-lib} ${egl-lib} avcodec)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++17")

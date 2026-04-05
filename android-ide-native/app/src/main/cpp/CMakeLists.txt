cmake_minimum_required(VERSION 3.22.1)
project("androidide_native" VERSION 1.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(
    androidide_native
    SHARED
    native_executor.cpp
    tool_wrapper.cpp
    file_utils.cpp
    jni_bridge.cpp
)

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(
    androidide_native
    PRIVATE
        ${log-lib}
        ${android-lib}
)

target_compile_options(androidide_native PRIVATE
    -O2
    -Wall
    -Wextra
    -fstack-protector-strong
    -D_FORTIFY_SOURCE=2
    -fvisibility=hidden
)

if (CMAKE_BUILD_TYPE STREQUAL "Release")
    target_compile_options(androidide_native PRIVATE -DNDEBUG)
endif()

cmake_minimum_required(VERSION 3.4.1)


find_package(OpenCV REQUIRED)
include_directories(${OpenCV_INCLUDE_DIRS})
include_directories( ${CMAKE_SOURCE_DIR}/3rdparty/)

add_subdirectory( ${CMAKE_SOURCE_DIR}/3rdparty/aruco)
find_library( log-lib log )
add_subdirectory( ${CMAKE_SOURCE_DIR}/3rdparty/marker_mapper)
find_library( log-lib log )

add_library( native-lib SHARED src/main/cpp/native-lib.cpp)
target_link_libraries(native-lib  ${log-lib} ${OpenCV_LIBS} aruco marker_mapper   )



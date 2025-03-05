cmake_minimum_required(VERSION 3.22.1)
project("patterndb")

# Incluye la carpeta de includes
include_directories(${CMAKE_SOURCE_DIR}/include)

# Crea la biblioteca compartida a partir de tu fuente (ajusta el nombre del archivo según corresponda)
add_library(native-lib SHARED patterndb.cpp)

target_link_libraries(native-lib
        android
        log)

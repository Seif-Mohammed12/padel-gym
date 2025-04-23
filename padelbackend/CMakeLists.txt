cmake_minimum_required(VERSION 3.30)
project(padelbackend)

set(CMAKE_CXX_STANDARD 20)

add_executable(padelbackend
        server.cpp)

# Include JSON library
include_directories(include)  # Path to json.hpp

# Platform-specific socket libraries
if (WIN32)
    set(SOCKET_LIBS ws2_32)
else()
    set(SOCKET_LIBS)
endif()

# Link sockets library
target_link_libraries(padelbackend ${SOCKET_LIBS})
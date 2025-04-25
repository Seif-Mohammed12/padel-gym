cmake_minimum_required(VERSION 3.30)
project(padel-gym)

set(CMAKE_CXX_STANDARD 20)

# Include JSON library
include_directories(include)  # Path to json.hpp

# Platform-specific socket libraries
if (WIN32)
    set(SOCKET_LIBS ws2_32)
else()
    set(SOCKET_LIBS)
endif()

# Add the executable
add_executable(padelbackend
        server.cpp
        FileManager.cpp
        FileManager.h
)

# Link the socket library
target_link_libraries(padelbackend PRIVATE ws2_32)



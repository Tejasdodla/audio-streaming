# Install script for directory: C:/ncs/modules/lib/picolibc/newlib/libc

# Set the install prefix
if(NOT DEFINED CMAKE_INSTALL_PREFIX)
  set(CMAKE_INSTALL_PREFIX "C:/Program Files (x86)/Zephyr-Kernel")
endif()
string(REGEX REPLACE "/$" "" CMAKE_INSTALL_PREFIX "${CMAKE_INSTALL_PREFIX}")

# Set the install configuration name.
if(NOT DEFINED CMAKE_INSTALL_CONFIG_NAME)
  if(BUILD_TYPE)
    string(REGEX REPLACE "^[^A-Za-z0-9_]+" ""
           CMAKE_INSTALL_CONFIG_NAME "${BUILD_TYPE}")
  else()
    set(CMAKE_INSTALL_CONFIG_NAME "")
  endif()
  message(STATUS "Install configuration: \"${CMAKE_INSTALL_CONFIG_NAME}\"")
endif()

# Set the component getting installed.
if(NOT CMAKE_INSTALL_COMPONENT)
  if(COMPONENT)
    message(STATUS "Install component: \"${COMPONENT}\"")
    set(CMAKE_INSTALL_COMPONENT "${COMPONENT}")
  else()
    set(CMAKE_INSTALL_COMPONENT)
  endif()
endif()

# Is this installation the result of a crosscompile?
if(NOT DEFINED CMAKE_CROSSCOMPILING)
  set(CMAKE_CROSSCOMPILING "TRUE")
endif()

# Set path to fallback-tool for dependency-resolution.
if(NOT DEFINED CMAKE_OBJDUMP)
  set(CMAKE_OBJDUMP "C:/tools/arm-gnu-toolchain-12.3.rel1-mingw-w64-i686-arm-none-eabi/bin/arm-none-eabi-objdump.exe")
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for the subdirectory.
  include("C:/Users/CID/Desktop/5th-sense/audio-streaming/firmware/build/firmware/modules/picolibc/newlib/libc/machine/cmake_install.cmake")
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for the subdirectory.
  include("C:/Users/CID/Desktop/5th-sense/audio-streaming/firmware/build/firmware/modules/picolibc/newlib/libc/include/cmake_install.cmake")
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for the subdirectory.
  include("C:/Users/CID/Desktop/5th-sense/audio-streaming/firmware/build/firmware/modules/picolibc/newlib/libc/argz/cmake_install.cmake")
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for the subdirectory.
  include("C:/Users/CID/Desktop/5th-sense/audio-streaming/firmware/build/firmware/modules/picolibc/newlib/libc/ctype/cmake_install.cmake")
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for the subdirectory.
  include("C:/Users/CID/Desktop/5th-sense/audio-streaming/firmware/build/firmware/modules/picolibc/newlib/libc/errno/cmake_install.cmake")
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for the subdirectory.
  include("C:/Users/CID/Desktop/5th-sense/audio-streaming/firmware/build/firmware/modules/picolibc/newlib/libc/iconv/cmake_install.cmake")
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for the subdirectory.
  include("C:/Users/CID/Desktop/5th-sense/audio-streaming/firmware/build/firmware/modules/picolibc/newlib/libc/misc/cmake_install.cmake")
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for the subdirectory.
  include("C:/Users/CID/Desktop/5th-sense/audio-streaming/firmware/build/firmware/modules/picolibc/newlib/libc/picolib/cmake_install.cmake")
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for the subdirectory.
  include("C:/Users/CID/Desktop/5th-sense/audio-streaming/firmware/build/firmware/modules/picolibc/newlib/libc/posix/cmake_install.cmake")
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for the subdirectory.
  include("C:/Users/CID/Desktop/5th-sense/audio-streaming/firmware/build/firmware/modules/picolibc/newlib/libc/search/cmake_install.cmake")
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for the subdirectory.
  include("C:/Users/CID/Desktop/5th-sense/audio-streaming/firmware/build/firmware/modules/picolibc/newlib/libc/signal/cmake_install.cmake")
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for the subdirectory.
  include("C:/Users/CID/Desktop/5th-sense/audio-streaming/firmware/build/firmware/modules/picolibc/newlib/libc/ssp/cmake_install.cmake")
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for the subdirectory.
  include("C:/Users/CID/Desktop/5th-sense/audio-streaming/firmware/build/firmware/modules/picolibc/newlib/libc/stdlib/cmake_install.cmake")
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for the subdirectory.
  include("C:/Users/CID/Desktop/5th-sense/audio-streaming/firmware/build/firmware/modules/picolibc/newlib/libc/string/cmake_install.cmake")
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for the subdirectory.
  include("C:/Users/CID/Desktop/5th-sense/audio-streaming/firmware/build/firmware/modules/picolibc/newlib/libc/time/cmake_install.cmake")
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for the subdirectory.
  include("C:/Users/CID/Desktop/5th-sense/audio-streaming/firmware/build/firmware/modules/picolibc/newlib/libc/tinystdio/cmake_install.cmake")
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for the subdirectory.
  include("C:/Users/CID/Desktop/5th-sense/audio-streaming/firmware/build/firmware/modules/picolibc/newlib/libc/xdr/cmake_install.cmake")
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for the subdirectory.
  include("C:/Users/CID/Desktop/5th-sense/audio-streaming/firmware/build/firmware/modules/picolibc/newlib/libc/locale/cmake_install.cmake")
endif()

string(REPLACE ";" "\n" CMAKE_INSTALL_MANIFEST_CONTENT
       "${CMAKE_INSTALL_MANIFEST_FILES}")
if(CMAKE_INSTALL_LOCAL_ONLY)
  file(WRITE "C:/Users/CID/Desktop/5th-sense/audio-streaming/firmware/build/firmware/modules/picolibc/newlib/libc/install_local_manifest.txt"
     "${CMAKE_INSTALL_MANIFEST_CONTENT}")
endif()

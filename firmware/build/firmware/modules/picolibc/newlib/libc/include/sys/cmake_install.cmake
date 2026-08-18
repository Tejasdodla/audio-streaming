# Install script for directory: C:/ncs/modules/lib/picolibc/newlib/libc/include/sys

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

if(CMAKE_INSTALL_COMPONENT STREQUAL "Unspecified" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/sys" TYPE FILE FILES
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/auxv.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/cdefs.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/config.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/custom_file.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/_default_fcntl.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/dirent.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/dir.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/errno.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/fcntl.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/features.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/file.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/iconvnls.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/_initfini.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/_intsup.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/_locale.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/lock.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/param.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/queue.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/resource.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/sched.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/select.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/_select.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/_sigset.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/stat.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/_stdint.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/string.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/syslimits.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/timeb.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/time.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/times.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/_timespec.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/timespec.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/_timeval.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/tree.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/_types.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/types.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/_tz_structs.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/unistd.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/utime.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/wait.h"
    "C:/ncs/modules/lib/picolibc/newlib/libc/include/sys/_wait.h"
    )
endif()

string(REPLACE ";" "\n" CMAKE_INSTALL_MANIFEST_CONTENT
       "${CMAKE_INSTALL_MANIFEST_FILES}")
if(CMAKE_INSTALL_LOCAL_ONLY)
  file(WRITE "C:/Users/CID/Desktop/5th-sense/audio-streaming/firmware/build/firmware/modules/picolibc/newlib/libc/include/sys/install_local_manifest.txt"
     "${CMAKE_INSTALL_MANIFEST_CONTENT}")
endif()

cmake_minimum_required(VERSION 3.20)

project(thoth_taglib CXX)

# TagLib sets these for its own subdirectory only, so repeat them here: tag_c is defined at this
# level and needs the same C++ level, and the static libtag.a has to be PIC to link into a shared
# library on ELF platforms.
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

set(TAGLIB_SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/taglib)
if(NOT EXISTS ${TAGLIB_SOURCE_DIR}/CMakeLists.txt)
  message(FATAL_ERROR
    "TagLib submodule missing at ${TAGLIB_SOURCE_DIR}.\n"
    "Run: git submodule update --init --recursive")
endif()

set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(BUILD_BINDINGS OFF CACHE BOOL "" FORCE)
set(BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(BUILD_TESTING OFF CACHE BOOL "" FORCE)
set(WITH_ZLIB OFF CACHE BOOL "" FORCE)
set(VISIBILITY_HIDDEN ON CACHE BOOL "" FORCE)
# TagLib's ENABLE_STATIC_RUNTIME only rewrites the flags in its own scope, which would leave tag_c
# on the dynamic runtime and mismatched against libtag.a. Setting the policy-backed variable applies
# to both targets, so the shipped dll carries the CRT with it.
set(CMAKE_MSVC_RUNTIME_LIBRARY MultiThreaded$<$<CONFIG:Debug>:Debug>)

add_subdirectory(${TAGLIB_SOURCE_DIR} taglib EXCLUDE_FROM_ALL)

# BUILD_BINDINGS=OFF leaves no tag_c target, so define our own: SHARED with TagLib linked in
# statically, giving one self-contained file to ship and System.load per platform.
add_library(tag_c SHARED ${TAGLIB_SOURCE_DIR}/bindings/c/tag_c.cpp)

# Inherit tag's internal include layout (one directory per format) so added formats keep working.
target_include_directories(tag_c PRIVATE
  ${TAGLIB_SOURCE_DIR}/bindings/c
  ${TAGLIB_SOURCE_DIR}/taglib
  $<TARGET_PROPERTY:tag,INCLUDE_DIRECTORIES>
)
target_compile_definitions(tag_c PRIVATE MAKE_TAGLIB_C_LIB)
target_link_libraries(tag_c PRIVATE tag)

# tag propagates TAGLIB_STATIC, collapsing TAGLIB_C_EXPORT to nothing, so the C API is exported
# only by leaving visibility at default here (TagLib's own symbols were compiled hidden). MSVC has
# no visibility equivalent, hence the export-all .def.
set_target_properties(tag_c PROPERTIES
  WINDOWS_EXPORT_ALL_SYMBOLS TRUE
  C_VISIBILITY_PRESET default
  CXX_VISIBILITY_PRESET default
)

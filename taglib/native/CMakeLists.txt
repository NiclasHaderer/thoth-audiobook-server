cmake_minimum_required(VERSION 3.20)

project(thoth_taglib CXX)

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
if(MSVC)
  set(ENABLE_STATIC_RUNTIME ON CACHE BOOL "" FORCE)
endif()

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

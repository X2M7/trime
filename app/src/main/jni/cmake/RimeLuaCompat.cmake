# SPDX-License-Identifier: GPL-3.0-or-later

# Keep the pinned plugin untouched while adapting two 32-bit Android issues:
# use the native size_t maximum for an unlimited lookup, and avoid redirecting
# stdio to fseeko64/ftello64 before those symbols exist on Android API 24.
function(trime_adapt_rime_lua)
  set(original "${CMAKE_SOURCE_DIR}/librime-lua/src/types.cc")
  set(stage "${CMAKE_BINARY_DIR}/trime-rime-lua-stage/types.cc")
  set(output "${CMAKE_BINARY_DIR}/trime-rime-lua/types.cc")
  set_property(DIRECTORY APPEND PROPERTY CMAKE_CONFIGURE_DEPENDS "${original}")
  configure_file("${original}" "${stage}" COPYONLY)

  trime_replace_dependency_cmake(
    "${stage}"
    "#include <chrono>"
    "#include <chrono>\n#include <limits>")
  trime_replace_dependency_cmake(
    "${stage}"
    "    limit = limit == 0 ? 0xffffffffffffffff : limit;"
    "    limit = limit == 0 ? std::numeric_limits<size_t>::max() : limit;")

  # Publish only changed content so repeated configuration stays incremental.
  configure_file("${stage}" "${output}" COPYONLY)
  get_target_property(sources rime-lua-objs SOURCES)
  set(matches 0)
  foreach(source IN LISTS sources)
    if(source MATCHES "(^|/)src/types\\.cc$")
      list(REMOVE_ITEM sources "${source}")
      math(EXPR matches "${matches} + 1")
    endif()
  endforeach()
  if(NOT matches EQUAL 1)
    message(FATAL_ERROR "Re-audit Rime Lua source registration: src/types.cc")
  endif()
  list(APPEND sources "${output}")
  set_property(TARGET rime-lua-objs PROPERTY SOURCES "${sources}")
  target_include_directories(rime-lua-objs PRIVATE
                             "${CMAKE_SOURCE_DIR}/librime-lua/src")

  if(ANDROID
     AND ANDROID_PLATFORM_LEVEL LESS 24
     AND ANDROID_ABI MATCHES "^(armeabi-v7a|x86)$")
    target_compile_definitions(rime-lua-objs PRIVATE _FILE_OFFSET_BITS=32)
  endif()
endfunction()

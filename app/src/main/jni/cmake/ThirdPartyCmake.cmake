# SPDX-License-Identifier: GPL-3.0-or-later

function(trime_replace_dependency_cmake path before after)
  file(READ "${path}" content)
  string(FIND "${content}" "${before}" first)
  string(FIND "${content}" "${before}" last REVERSE)
  if(first LESS 0 OR NOT first EQUAL last)
    message(FATAL_ERROR "Re-audit the Trime CMake adaptation for ${path}")
  endif()
  string(REPLACE "${before}" "${after}" content "${content}")
  file(WRITE "${path}" "${content}")
endfunction()

# CMake 3.31 has no CMAKE_POLICY_VERSION_MINIMUM. Adapt build-tree copies only;
# never edit pinned submodules or suppress policy/deprecation diagnostics.
function(trime_add_dependency source_dir binary_dir minimum)
  get_filename_component(source "${source_dir}" ABSOLUTE BASE_DIR
                         "${CMAKE_CURRENT_SOURCE_DIR}")
  set(destination "${CMAKE_CURRENT_BINARY_DIR}/trime-deps/${binary_dir}")
  file(GLOB_RECURSE inputs CONFIGURE_DEPENDS "${source}/*")
  list(FILTER inputs EXCLUDE REGEX "(^|/)\\.git(/|$)")
  set_property(
    DIRECTORY
    APPEND
    PROPERTY CMAKE_CONFIGURE_DEPENDS ${inputs})

  # Regenerate this owned directory so deleted upstream files cannot linger.
  file(REMOVE_RECURSE "${destination}")
  file(
    COPY "${source}/"
    DESTINATION "${destination}"
    PATTERN ".git" EXCLUDE)
  trime_replace_dependency_cmake(
    "${destination}/CMakeLists.txt"
    "cmake_minimum_required(VERSION ${minimum})"
    "cmake_minimum_required(VERSION ${minimum}...3.10)")

  if(binary_dir STREQUAL "OpenCC")
    # Dictionary generation needs a host interpreter, not Android Python libs.
    trime_replace_dependency_cmake(
      "${destination}/data/CMakeLists.txt"
      "find_package(PythonInterp REQUIRED)"
      "find_package(Python3 REQUIRED COMPONENTS Interpreter)\nset(PYTHON_EXECUTABLE \"\${Python3_EXECUTABLE}\")"
    )
  endif()
  add_subdirectory("${destination}" "${binary_dir}")
endfunction()

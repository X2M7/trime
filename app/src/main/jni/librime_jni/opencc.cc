// SPDX-FileCopyrightText: 2015 - 2024 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

#include <opencc/Common.hpp>
#include <opencc/DictConverter.hpp>
#include <opencc/Exception.hpp>
#include <opencc/SimpleConverter.hpp>
#include <string>

#include "jni-utils.h"

// opencc

extern "C" JNIEXPORT jstring JNICALL
Java_com_osfans_trime_data_opencc_OpenCCDictManager_openCCLineConv(
    JNIEnv* env, jclass clazz, jstring input, jstring config_file_name) {
  return jniCall(env, [&]() -> jstring {
    opencc::SimpleConverter converter(CString(env, config_file_name));
    const auto text = static_cast<std::string>(CString(env, input));
    // OpenCC's segmenter stops at NUL even for its std::string overload.
    // Treat NUL as a boundary and preserve the rest of the editor's text.
    if (text.find('\0') == std::string::npos)
      return makeJavaString(env, converter.Convert(text));
    std::string converted;
    size_t start = 0;
    for (;;) {
      const auto end = text.find('\0', start);
      converted += converter.Convert(text.substr(start, end - start));
      if (end == std::string::npos) break;
      converted += '\0';
      start = end + 1;
    }
    return makeJavaString(env, converted);
  });
}

extern "C" JNIEXPORT void JNICALL
Java_com_osfans_trime_data_opencc_OpenCCDictManager_openCCDictConv(
    JNIEnv* env, jclass clazz, jstring src, jstring dest, jboolean mode) {
  jniCall(env, [&] {
    auto src_file = CString(env, src);
    auto dest_file = CString(env, dest);
    if (mode) {
      opencc::ConvertDictionary(src_file, dest_file, "ocd2", "text");
    } else {
      opencc::ConvertDictionary(src_file, dest_file, "text", "ocd2");
    }
  });
}

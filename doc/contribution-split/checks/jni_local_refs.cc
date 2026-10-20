// SPDX-License-Identifier: GPL-3.0-or-later
// Host regression for the production header with a real JNI function table.
// The table is mocked; this does not execute ART, a JVM, or the Rime engine.
#include <algorithm>
#include <iostream>
#include <map>
#include <string>
#include <vector>

#include "objconv.h"

namespace {
struct String : _jstring {
  std::string value;
};
struct Array : _jobjectArray {
  std::vector<String> items;
};
struct State {
  std::map<jobject, std::size_t> references;
  std::map<jstring, std::size_t> borrowed;
  std::size_t live = 0;
  std::size_t peak = 0;
  std::size_t acquisitions = 0;
  std::size_t releases = 0;
  std::size_t deletions = 0;
  std::size_t violations = 0;
};
State state;

jsize GetArrayLength(JNIEnv*, jarray array) {
  return static_cast<jsize>(static_cast<Array*>(array)->items.size());
}
jobject GetObjectArrayElement(JNIEnv*, jobjectArray array, jsize index) {
  if (state.live != 0) ++state.violations;
  auto object = &static_cast<Array*>(array)->items[static_cast<std::size_t>(index)];
  ++state.references[object];
  ++state.live;
  state.peak = std::max(state.peak, state.live);
  return object;
}
const char* GetStringUTFChars(JNIEnv*, jstring string, jboolean*) {
  if (state.references[string] == 0) ++state.violations;
  ++state.borrowed[string];
  ++state.acquisitions;
  return static_cast<String*>(string)->value.c_str();
}
void ReleaseStringUTFChars(JNIEnv*, jstring string, const char* chars) {
  if (state.references[string] == 0 || state.borrowed[string] == 0 ||
      chars != static_cast<String*>(string)->value.c_str()) {
    ++state.violations;
  } else {
    --state.borrowed[string];
  }
  ++state.releases;
}
void DeleteLocalRef(JNIEnv*, jobject object) {
  auto string = static_cast<jstring>(object);
  // CString must release its borrowed chars before JRef deletes the reference.
  if (state.borrowed[string] != 0 || state.references[object] == 0) {
    ++state.violations;
  } else {
    --state.references[object];
    --state.live;
  }
  ++state.deletions;
}
}  // namespace

int main() {
  JNINativeInterface functions{};
  functions.GetArrayLength = GetArrayLength;
  functions.GetObjectArrayElement = GetObjectArrayElement;
  functions.GetStringUTFChars = GetStringUTFChars;
  functions.ReleaseStringUTFChars = ReleaseStringUTFChars;
  functions.DeleteLocalRef = DeleteLocalRef;
  JNIEnv env{&functions};
  Array empty;
  if (!stringArrayToStringVector(&env, &empty).empty() || state.live != 0)
    ++state.violations;
  Array array;
  std::vector<std::string> expected;
  constexpr std::size_t count = 2048;
  constexpr std::size_t repetitions = 4;
  for (std::size_t i = 0; i < count; ++i) {
    String item;
    item.value = i == 0 ? "" : (i % 7 == 0 ? "duplicate" : "schema_" + std::to_string(i));
    expected.push_back(item.value);
    array.items.push_back(std::move(item));
  }
  for (std::size_t i = 0; i < repetitions; ++i) {
    const auto actual = stringArrayToStringVector(&env, &array);
    if (actual != expected) ++state.violations;
    if (state.live != 0) ++state.violations;
    if (!stringArrayToStringVector(&env, &empty).empty()) ++state.violations;
  }
  const auto operations = count * repetitions;
  const bool passed = state.violations == 0 && state.live == 0 &&
                      state.peak == 1 && state.acquisitions == operations &&
                      state.releases == operations && state.deletions == operations;
  std::cout << "{\"passed\":" << (passed ? "true" : "false")
            << ",\"elements_per_call\":" << count
            << ",\"repetitions\":" << repetitions
            << ",\"live_local_refs\":" << state.live
            << ",\"peak_local_refs\":" << state.peak
            << ",\"GetStringUTFChars\":" << state.acquisitions
            << ",\"ReleaseStringUTFChars\":" << state.releases
            << ",\"DeleteLocalRef\":" << state.deletions
            << ",\"violations\":" << state.violations << "}\n";
  return passed ? 0 : 1;
}

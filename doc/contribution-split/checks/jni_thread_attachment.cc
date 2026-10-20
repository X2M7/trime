// SPDX-License-Identifier: GPL-3.0-or-later
// Executes the production JEnv against the unmodified NDK JavaVM function table.
// This mock does not execute ART, a Java VM, or a Rime callback.
#include <iostream>
#include <stdexcept>
#include <type_traits>

#include "jni-utils.h"

#ifdef REQUIRE_UNIQUE_OWNER
static_assert(!std::is_copy_constructible_v<JEnv>);
static_assert(!std::is_copy_assignable_v<JEnv>);
static_assert(!std::is_move_constructible_v<JEnv>);
static_assert(!std::is_move_assignable_v<JEnv>);
#endif

namespace {
struct State {
  bool attached = false;
  jint forcedGetResult = JNI_OK;
  jint attachResult = JNI_OK;
  int getCalls = 0;
  int attachCalls = 0;
  int detachCalls = 0;
  int versionErrors = 0;
};
State state;
JNINativeInterface nativeFunctions{};
JNIEnv env{&nativeFunctions};
_jclass dummyClass;
char dummyMethod;

jint GetEnv(JavaVM*, void** value, jint version) {
  ++state.getCalls;
  if (version != JNI_VERSION_1_6) ++state.versionErrors;
  if (state.forcedGetResult != JNI_OK) {
    *value = nullptr;
    return state.forcedGetResult;
  }
  *value = state.attached ? &env : nullptr;
  return state.attached ? JNI_OK : JNI_EDETACHED;
}
jint Attach(JavaVM*, JNIEnv** value, void*) {
  ++state.attachCalls;
  if (state.attachResult == JNI_OK) {
    state.attached = true;
    *value = &env;
  } else {
    *value = nullptr;
  }
  return state.attachResult;
}
jint Detach(JavaVM*) {
  ++state.detachCalls;
  state.attached = false;
  return JNI_OK;
}
jclass FindClass(JNIEnv*, const char*) { return &dummyClass; }
jobject NewGlobalRef(JNIEnv*, jobject object) { return object; }
jmethodID Method(JNIEnv*, jclass, const char*, const char*) {
  return reinterpret_cast<jmethodID>(&dummyMethod);
}

void Check(bool condition, const char* message) {
  if (!condition) throw std::runtime_error(message);
}

// Calls the existing production factory, including mandatory C++17 elision.
JEnv ThroughFactory(const GlobalRefSingleton& global) {
  return global.AttachEnv();
}

void AlreadyAttached(JavaVM* vm, const GlobalRefSingleton&) {
  state = State{};
  state.attached = true;
  {
    JEnv owner(vm);
    Check(static_cast<JNIEnv*>(owner) == &env, "existing Java environment lost");
  }
  Check(state.getCalls == 1 && state.attachCalls == 0 && state.detachCalls == 0 && state.attached,
        "already attached Java thread was detached or reattached");
}
void OwnedAttachment(JavaVM* vm, const GlobalRefSingleton&) {
  state = State{};
  {
    JEnv owner(vm);
    Check(static_cast<JNIEnv*>(owner) == &env, "native attach did not return environment");
    Check(state.attached && state.detachCalls == 0, "detached before owner destruction");
  }
  Check(state.getCalls == 1 && state.attachCalls == 1 && state.detachCalls == 1 && !state.attached,
        "successful owned attach was not detached exactly once");
}
void FailedAttachment(JavaVM* vm, const GlobalRefSingleton&) {
  state = State{};
  state.attachResult = JNI_ERR;
  {
    JEnv owner(vm);
    Check(static_cast<JNIEnv*>(owner) == nullptr, "mock failed attach unexpectedly produced environment");
  }
  Check(state.getCalls == 1 && state.attachCalls == 1 && state.detachCalls == 0 && !state.attached,
        "failed attach was detached");
}
void FailedGetEnv(JavaVM* vm, const GlobalRefSingleton&) {
  state = State{};
  state.forcedGetResult = JNI_EVERSION;
  {
    JEnv owner(vm);
    Check(static_cast<JNIEnv*>(owner) == nullptr, "failed GetEnv unexpectedly produced environment");
  }
  Check(state.getCalls == 1 && state.attachCalls == 0 && state.detachCalls == 0,
        "unexpected GetEnv status caused attach/detach");
}
void NestedOwners(JavaVM*, const GlobalRefSingleton& global) {
  state = State{};
  {
    auto outer = ThroughFactory(global);
    {
      auto inner = ThroughFactory(global);
      Check(static_cast<JNIEnv*>(inner) == &env, "nested environment changed");
    }
    Check(state.getCalls == 2 && state.attachCalls == 1 && state.detachCalls == 0 && state.attached,
          "nested borrower detached outer attachment");
    Check(static_cast<JNIEnv*>(outer) == &env, "outer environment changed");
  }
  Check(state.detachCalls == 1 && !state.attached, "nested owners failed to detach once");
}
void RepeatedFactories(JavaVM*, const GlobalRefSingleton& global) {
  state = State{};
  for (int i = 0; i < 128; ++i) {
    auto owner = ThroughFactory(global);
    Check(static_cast<JNIEnv*>(owner) == &env, "factory environment missing");
  }
  Check(state.getCalls == 128 && state.attachCalls == 128 && state.detachCalls == 128 && !state.attached,
        "factory return ownership leaked or duplicated");
}
void UniqueOwnership(JavaVM*, const GlobalRefSingleton&) {
  state = State{};
  Check(!std::is_copy_constructible_v<JEnv> && !std::is_copy_assignable_v<JEnv> &&
            !std::is_move_constructible_v<JEnv> && !std::is_move_assignable_v<JEnv>,
        "thread attachment owner remains copyable or movable");
}
}  // namespace

int main() {
  nativeFunctions.FindClass = FindClass;
  nativeFunctions.NewGlobalRef = NewGlobalRef;
  nativeFunctions.GetMethodID = Method;
  nativeFunctions.GetStaticMethodID = Method;
  JNIInvokeInterface invocation{};
  invocation.GetEnv = GetEnv;
  invocation.AttachCurrentThread = Attach;
  invocation.DetachCurrentThread = Detach;
  JavaVM vm{&invocation};
  // Use the actual GlobalRefSingleton constructor/factory. Its class and
  // method registration requests receive inert mock objects, never a JVM.
  GlobalRefSingleton global(&vm);
  struct Case {
    const char* name;
    void (*run)(JavaVM*, const GlobalRefSingleton&);
  };
  const Case cases[] = {
      {"already-attached", AlreadyAttached}, {"owned-attachment", OwnedAttachment},
      {"failed-attachment", FailedAttachment}, {"failed-GetEnv", FailedGetEnv},
      {"nested-owners", NestedOwners}, {"repeated-factories", RepeatedFactories},
      {"unique-ownership", UniqueOwnership}};
  int failures = 0;
  for (const auto& item : cases) {
    bool passed = true;
    try {
      item.run(&vm, global);
      Check(state.versionErrors == 0, "JNI version changed");
    } catch (const std::exception& failure) {
      passed = false;
      ++failures;
      // Print immediately while failure.what() remains valid.
      std::cout << "{\"case\":\"" << item.name << "\",\"passed\":false,\"error\":\""
                << failure.what() << "\",\"GetEnv\":" << state.getCalls
                << ",\"attach\":" << state.attachCalls << ",\"detach\":" << state.detachCalls << "}\n";
    }
    if (passed) {
      std::cout << "{\"case\":\"" << item.name << "\",\"passed\":true,\"GetEnv\":"
                << state.getCalls << ",\"attach\":" << state.attachCalls
                << ",\"detach\":" << state.detachCalls << "}\n";
    }
  }
  std::cout << "{\"passed\":" << (failures == 0 ? "true" : "false")
            << ",\"cases\":7,\"failed_cases\":" << failures << "}\n";
  return failures == 0 ? 0 : 1;
}

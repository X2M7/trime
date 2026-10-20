// SPDX-License-Identifier: GPL-3.0-or-later
#include "jni-utils.h"
#include <iostream>
#include <stdexcept>
#include <thread>

static void Require(bool value, const char* reason) {
  if (!value) throw std::runtime_error(reason);
}
static JEnv ThroughFactory(JavaVM* vm) { return JEnv(vm); }
static jint Current(JavaVM* vm) {
  void* current = nullptr;
  return vm->GetEnv(&current, JNI_VERSION_1_6);
}
struct WorkerResult {
  jint before = JNI_ERR;
  jint inside = JNI_ERR;
  jint afterNested = JNI_ERR;
  jint after = JNI_ERR;
  jint cleanup = JNI_ERR;
  bool stringUsable = false;
  bool nestedUsedSameEnvironment = false;
  bool error = false;
};
int main() {
  JavaVMOption options[] = {{const_cast<char*>("-Xcheck:jni"), nullptr},
                           {const_cast<char*>("-Xms16m"), nullptr},
                           {const_cast<char*>("-Xmx128m"), nullptr},
                           {const_cast<char*>("-XX:MaxMetaspaceSize=64m"), nullptr},
                           {const_cast<char*>("-XX:ReservedCodeCacheSize=32m"), nullptr},
                           {const_cast<char*>("-XX:ActiveProcessorCount=1"), nullptr},
                           {const_cast<char*>("-XX:+UseSerialGC"), nullptr},
                           {const_cast<char*>("-Djava.awt.headless=true"), nullptr}};
  JavaVMInitArgs args{};
  args.version = JNI_VERSION_1_6;
  args.nOptions = sizeof(options) / sizeof(options[0]);
  args.options = options;
  args.ignoreUnrecognized = JNI_FALSE;
  JavaVM* vm = nullptr;
  JNIEnv* environment = nullptr;
  jint created = JNI_CreateJavaVM(&vm, &environment, &args);
  if (created != JNI_OK || vm == nullptr || environment == nullptr) {
    std::cout << "{\"create\":" << created << "}\n";
    return 2;
  }
  bool mainBorrowed = false;
  bool mainNested = false;
  bool mainStillAttached = false;
  {
    auto borrowed = ThroughFactory(vm);
    mainBorrowed = static_cast<JNIEnv*>(borrowed) == environment;
    {
      auto inner = ThroughFactory(vm);
      mainNested = static_cast<JNIEnv*>(inner) == environment;
    }
    mainNested = mainNested && Current(vm) == JNI_OK;
  }
  mainStillAttached = Current(vm) == JNI_OK;
  WorkerResult worker;
  std::thread nativeThread([&] {
    try {
      worker.before = Current(vm);
      {
        auto owned = ThroughFactory(vm);
        worker.inside = Current(vm);
        Require(static_cast<JNIEnv*>(owned) != nullptr, "native attachment has no environment");
        {
          auto inner = ThroughFactory(vm);
          worker.nestedUsedSameEnvironment = static_cast<JNIEnv*>(inner) == static_cast<JNIEnv*>(owned);
        }
        worker.afterNested = Current(vm);
        jstring text = owned->NewStringUTF("native-guard");
        Require(text != nullptr, "NewStringUTF failed");
        worker.stringUsable = owned->GetStringUTFLength(text) == 12 && !owned->ExceptionCheck();
        owned->DeleteLocalRef(text);
      }
      worker.after = Current(vm);
    } catch (...) {
      worker.error = true;
      worker.after = Current(vm);
    }
    // Explicit test cleanup is necessary for the leaking baseline. It is
    // deliberately outside the ownership guard and prevents JVM shutdown hangs.
    worker.cleanup = Current(vm) == JNI_OK ? vm->DetachCurrentThread() : JNI_OK;
  });
  nativeThread.join();
  bool baselineLeak = worker.after == JNI_OK;
  bool mainPass = mainBorrowed && mainNested && mainStillAttached;
  bool nativePass = !worker.error && worker.before == JNI_EDETACHED && worker.inside == JNI_OK
      && worker.afterNested == JNI_OK && worker.nestedUsedSameEnvironment && worker.stringUsable
      && worker.after == JNI_EDETACHED && worker.cleanup == JNI_OK;
  std::cout << "{\"case\":\"main-thread-borrow-and-nesting\",\"passed\":"
            << (mainPass ? "true" : "false") << ",\"stillAttached\":"
            << (mainStillAttached ? "true" : "false") << "}\n";
  std::cout << "{\"case\":\"native-worker-lifetime\",\"passed\":"
            << (nativePass ? "true" : "false") << ",\"before\":" << worker.before
            << ",\"inside\":" << worker.inside << ",\"afterNested\":" << worker.afterNested
            << ",\"after\":" << worker.after << ",\"cleanup\":" << worker.cleanup
            << ",\"stringUsable\":" << (worker.stringUsable ? "true" : "false")
            << ",\"nestedSameEnvironment\":" << (worker.nestedUsedSameEnvironment ? "true" : "false")
            << ",\"baselineLeakObserved\":" << (baselineLeak ? "true" : "false") << "}\n";
  // A failure must not leave a native non-daemon thread attached at DestroyJavaVM.
  if (Current(vm) == JNI_EDETACHED) vm->AttachCurrentThread(&environment, nullptr);
  jint destroyed = vm->DestroyJavaVM();
  std::cout << "{\"destroy\":" << destroyed << ",\"passed\":"
            << ((mainPass && nativePass && destroyed == JNI_OK) ? "true" : "false") << "}\n";
  return mainPass && nativePass && destroyed == JNI_OK ? 0 : 1;
}

# Focused JNI attachment ownership reproduction (Linux host)

Run from a directory containing reproduce-host-vm.py and host-vm-check.cc:

```sh
python3 reproduce-host-vm.py --repo REPO --base BASE --head HEAD --ndk NDK --jdk JDK --out OUT
```

The recorded comparison uses base `144f6111d0498da85e0ffa250245f18955179ed3`
and head `2283878e8751305de9daa3e64a6c5448381e9b94`.

- REPO: an existing Git checkout with both commits already available.
- BASE: the upstream commit before this patch.
- HEAD: the ownership patch commit.
- NDK: Android NDK root containing toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include/jni.h.
- JDK: JDK 17 root containing lib/server/libjvm.so.
- OUT: a new output directory. Existing output directories are refused.
- Optional --compiler: clang++ executable, otherwise found from PATH.

This builds two tiny C++17 executables with -fno-elide-constructors,
-Wall -Wextra -Werror and the exact production headers from Git. Each starts
an actual HotSpot VM with -Xcheck:jni and a 128 MiB maximum heap. One std::thread
checks detached -> attached -> detached across the native guard; nested guards
must preserve the outer environment, and main-thread borrowed guards must leave
the VM attachment intact. Real string JNI calls execute inside the native scope.

A leaking baseline exits 1 but is explicitly detached by the test before worker
termination, and DestroyJavaVM must return 0. The fixed executable exits 0.
All source/tool hashes, commands, original stdout/stderr and observed VM statuses
are written to OUT/report.json. Unexpected compiler/JVM diagnostics fail the check.

The test does not initialize Trime classes or GlobalRefSingleton, load Rime,
run Android/ART, or measure input performance. It does not change any repository
file or install dependencies. Mock failure injection is recorded separately.
The earlier run without -Xcheck:jni and the checked rerun use the same logical
cases; do not double-count them as additional test cases.

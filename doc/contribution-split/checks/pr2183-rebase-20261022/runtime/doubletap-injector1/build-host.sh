#!/bin/sh
set -eu

SELF_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
JDK_DIR=/home/xumin/Public/android-toolchain/jdk-17.0.19+10
SDK_DIR=/home/xumin/Public/android-toolchain/android-sdk
BUILD_DIR="$SELF_DIR/build"
PATH="$JDK_DIR/bin:$PATH"
export PATH

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/classes"
"$JDK_DIR/bin/javac" -J-Xmx256m -source 8 -target 8 \
  -bootclasspath "$SDK_DIR/platforms/android-35/android.jar" \
  -d "$BUILD_DIR/classes" \
  "$SELF_DIR/src/TrimeRealDoubleTap.java"
JAVA_HOME="$JDK_DIR" \
  "$SDK_DIR/build-tools/35.0.0/d8" -JXmx256m --release --min-api 21 \
  --output "$SELF_DIR/trime-real-doubletap.jar" \
  "$BUILD_DIR/classes/org/x2m7/trime/readiness/input/TrimeRealDoubleTap.class"

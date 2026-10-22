# Real MotionEvent double-tap helper

This host-built helper emits `DOWN_1`, `UP_1`, `DOWN_2`, and `UP_2` from one `app_process` process. Each adjacent event is separated by 60 ms, so the intended UP-to-UP spacing is 120 ms. It injects touchscreen/finger events through Android's input manager with `WAIT_FOR_FINISH`; it does not call a Trime view, `performClick`, or `GestureFrame` directly.

The frozen standard Shift invocation is:

```sh
CLASSPATH=/data/local/tmp/trime-real-doubletap.jar app_process /system/bin \
  org.x2m7.trime.readiness.input.TrimeRealDoubleTap 54 996 60
```

The runtime operator must bind the exact jar SHA, identity, coordinates, source APK/head/theme, target PID/starttime, and exact command. Exit zero and four accepted events are necessary but insufficient. Acceptance requires the continuous target log to prove app-side UP spacing at most 300 ms. S2 additionally requires `KeyView ... behavior=DOUBLE_CLICK`, `processKeyAction ... type=DOUBLE_CLICK`, and exact editor text `AA`. S1 is a negative control: the same rapid physical gesture on a key without a double action should log two `CLICK` releases and retain exact `aa`.

`identity1.json` records the build inputs, invocation, evidence contract, source provenance, and limitations. No device run is claimed here.

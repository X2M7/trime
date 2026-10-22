package org.x2m7.trime.readiness.input;

import android.os.Process;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.MotionEvent;

import java.lang.reflect.Method;

/** Shell/root-only QA helper that injects one real two-contact tap sequence. */
public final class TrimeRealDoubleTap {
    private static final int WAIT_FOR_FINISH = 2;
    private static final int SOURCE_TOUCHSCREEN = InputDevice.SOURCE_TOUCHSCREEN;
    private static final int DISPLAY_ID = 0;
    private static final int EVENT_COUNT = 4;

    private final Object manager;
    private final Method inject;
    private final Method setDisplayId;
    private final String backend;
    private final float x;
    private final float y;
    private final long gapMs;
    private int attempted;
    private int accepted;
    private long firstDownMs;
    private long firstUpCallMs;
    private long secondUpCallMs;

    private TrimeRealDoubleTap(float x, float y, long gapMs) throws Exception {
        this.x = x;
        this.y = y;
        this.gapMs = gapMs;

        Object selectedManager;
        Method selectedInject;
        String selectedBackend;
        try {
            Class<?> type = Class.forName("android.hardware.input.InputManagerGlobal");
            selectedManager = type.getMethod("getInstance").invoke(null);
            selectedInject = type.getMethod("injectInputEvent", InputEvent.class, int.class);
            selectedBackend = type.getName();
        } catch (ClassNotFoundException unavailableOnOldAndroid) {
            Class<?> type = Class.forName("android.hardware.input.InputManager");
            selectedManager = type.getMethod("getInstance").invoke(null);
            selectedInject = type.getMethod("injectInputEvent", InputEvent.class, int.class);
            selectedBackend = type.getName();
        }
        manager = selectedManager;
        inject = selectedInject;
        backend = selectedBackend;

        Method displaySetter = null;
        try {
            displaySetter = InputEvent.class.getMethod("setDisplayId", int.class);
        } catch (NoSuchMethodException absentOnOldAndroid) {
            // API 21 events already target the default display.
        }
        setDisplayId = displaySetter;
    }

    private static int touchDeviceId() {
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice device = InputDevice.getDevice(id);
            if (device != null && device.supportsSource(SOURCE_TOUCHSCREEN)) return id;
        }
        return 0;
    }

    private boolean event(int action, String name, long downTimeMs) throws Exception {
        long eventTimeMs = SystemClock.uptimeMillis();
        MotionEvent.PointerProperties pointer = new MotionEvent.PointerProperties();
        pointer.id = 0;
        pointer.toolType = MotionEvent.TOOL_TYPE_FINGER;
        MotionEvent.PointerCoords coordinates = new MotionEvent.PointerCoords();
        coordinates.x = x;
        coordinates.y = y;
        coordinates.pressure =
                action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL ? 0.0f : 1.0f;
        coordinates.size = 1.0f;
        MotionEvent event = MotionEvent.obtain(
                downTimeMs,
                eventTimeMs,
                action,
                1,
                new MotionEvent.PointerProperties[] {pointer},
                new MotionEvent.PointerCoords[] {coordinates},
                0,
                0,
                1.0f,
                1.0f,
                touchDeviceId(),
                0,
                SOURCE_TOUCHSCREEN,
                0);
        boolean ok = false;
        long callStartMs = SystemClock.uptimeMillis();
        try {
            if (setDisplayId != null) setDisplayId.invoke(event, DISPLAY_ID);
            Object result = inject.invoke(manager, event, WAIT_FOR_FINISH);
            if (!(result instanceof Boolean)) {
                throw new IllegalStateException("Non-boolean injectInputEvent result");
            }
            ok = ((Boolean) result).booleanValue();
            return ok;
        } finally {
            long callEndMs = SystemClock.uptimeMillis();
            attempted++;
            if (ok) accepted++;
            if (action == MotionEvent.ACTION_UP && firstUpCallMs == 0) {
                firstUpCallMs = callStartMs;
            } else if (action == MotionEvent.ACTION_UP) {
                secondUpCallMs = callStartMs;
            }
            System.out.println("TRIME_REAL_DOUBLETAP_EVENT"
                    + " index=" + attempted
                    + " action=" + name
                    + " down_uptime_ms=" + downTimeMs
                    + " event_uptime_ms=" + eventTimeMs
                    + " call_start_uptime_ms=" + callStartMs
                    + " call_end_uptime_ms=" + callEndMs
                    + " pid=" + Process.myPid()
                    + " uid=" + Process.myUid()
                    + " accepted=" + ok
                    + " source=" + SOURCE_TOUCHSCREEN
                    + " display_id=" + DISPLAY_ID
                    + " x=" + x
                    + " y=" + y);
            System.out.flush();
            event.recycle();
        }
    }

    private void requireAccepted(int action, String name, long downTimeMs) throws Exception {
        if (!event(action, name, downTimeMs)) {
            throw new IllegalStateException(name + " injection rejected");
        }
    }

    private void run() throws Exception {
        long currentDownMs = 0;
        boolean contactDown = false;
        try {
            firstDownMs = SystemClock.uptimeMillis();
            currentDownMs = firstDownMs;
            requireAccepted(MotionEvent.ACTION_DOWN, "DOWN_1", firstDownMs);
            contactDown = true;
            SystemClock.sleep(gapMs);
            requireAccepted(MotionEvent.ACTION_UP, "UP_1", firstDownMs);
            contactDown = false;
            SystemClock.sleep(gapMs);
            long secondDownMs = SystemClock.uptimeMillis();
            currentDownMs = secondDownMs;
            requireAccepted(MotionEvent.ACTION_DOWN, "DOWN_2", secondDownMs);
            contactDown = true;
            SystemClock.sleep(gapMs);
            requireAccepted(MotionEvent.ACTION_UP, "UP_2", secondDownMs);
            contactDown = false;
        } finally {
            if (contactDown) {
                try {
                    event(MotionEvent.ACTION_CANCEL, "CANCEL_CLEANUP", currentDownMs);
                } catch (Exception cleanupFailure) {
                    System.err.println("TRIME_REAL_DOUBLETAP_CLEANUP_FAILED "
                            + cleanupFailure.getClass().getName());
                }
            }
        }

        long upCallDeltaMs = secondUpCallMs - firstUpCallMs;
        if (attempted != EVENT_COUNT || accepted != EVENT_COUNT) {
            throw new IllegalStateException("Expected four accepted events");
        }
        if (upCallDeltaMs <= 0 || upCallDeltaMs > 300) {
            throw new IllegalStateException("Injector-side UP call delta outside (0,300] ms: "
                    + upCallDeltaMs);
        }
        System.out.println("TRIME_REAL_DOUBLETAP_SUMMARY"
                + " completed=true"
                + " pid=" + Process.myPid()
                + " uid=" + Process.myUid()
                + " backend=" + backend
                + " attempted=" + attempted
                + " accepted=" + accepted
                + " gap_ms=" + gapMs
                + " injector_up_call_delta_ms=" + upCallDeltaMs
                + " x=" + x
                + " y=" + y);
        System.out.flush();
    }

    public static void main(String[] args) throws Exception {
        int uid = Process.myUid();
        if (uid != 0 && uid != 2000) {
            throw new SecurityException("Expected root or shell UID, got " + uid);
        }
        if (args.length != 3) {
            throw new IllegalArgumentException("Expected: x y gap_ms");
        }
        int x = Integer.parseInt(args[0]);
        int y = Integer.parseInt(args[1]);
        int gapMs = Integer.parseInt(args[2]);
        if (x < 0 || y < 0) throw new IllegalArgumentException("Coordinates must be non-negative");
        if (gapMs < 20 || gapMs > 100 || gapMs * 2 > 300) {
            throw new IllegalArgumentException("gap_ms must be within [20,100]");
        }
        new TrimeRealDoubleTap(x, y, gapMs).run();
    }
}

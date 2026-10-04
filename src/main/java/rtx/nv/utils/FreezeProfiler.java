package rtx.nv.utils;
import rtx.nv.NV;

public final class FreezeProfiler {
    private static final long FREEZE_THRESHOLD_MS = 400L;
    private static final long SAMPLE_INTERVAL_MS = 100L;
    private static final int MAX_FRAMES = 30;
    private static volatile long lastFrameNanos = System.nanoTime();
    private static volatile Thread renderThread;
    private static volatile boolean started;

    private FreezeProfiler() {
    }

    static {
        started = false;
    }

    public static void markFrame() {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment()) return;
        lastFrameNanos = System.nanoTime();
        if (renderThread == null) {
            renderThread = Thread.currentThread();
        }
        if (!started) {
            started = true;
            FreezeProfiler.startWatchdog();
        }
    }

    private static void startWatchdog() {
        Thread thread = new Thread(() -> {
            long l = -1L;
            while (true) {
                try {
                    Thread.sleep(100L);
                }
                catch (InterruptedException interruptedException) {
                    return;
                }
                Thread targetThread = renderThread;
                if (targetThread == null) continue;
                long l2 = (System.nanoTime() - lastFrameNanos) / 1000000L;
                if (l2 > 400L) {
                    if (l >= 0L) { l = l2; continue; }
                    StackTraceElement[] stackTraceElementArray = targetThread.getStackTrace();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("[FREEZE-PROF] render thread stuck ~").append(l2).append("ms:\n");
                    int n = Math.min(stackTraceElementArray.length, 20);
                    for (int i = 0; i < n; i++) {
                        stringBuilder.append("    at ").append(stackTraceElementArray[i]).append('\n');
                    }
                    // Log once per freeze; continuous stack dumps amplify a stalled frame.
                    if (l < 0L) NV.LOGGER.warn(stringBuilder.toString());
                    l = l2;
                    continue;
                }
                if (l <= 0L) continue;
                NV.LOGGER.warn("[FREEZE-PROF] recovered after ~{}ms freeze", (Object)l);
                l = -1L;
            }
        }, "nv-freeze-profiler");
        thread.setDaemon(true);
        thread.start();
    }
}


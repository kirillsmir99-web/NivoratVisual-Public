package rtx.nv.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import net.fabricmc.loader.api.FabricLoader;

/** CPU time inside GameRenderer.render; excludes the client gametest tick barrier. */
public final class FrameMetrics {
    private static final long[] samples = new long[4096];
    private static int count;
    private static long start;
    private static String scenario;
    private static final StringBuilder report = new StringBuilder("scenario,samples,median_cpu_ms,p95_cpu_ms,p99_cpu_ms\n");
    public static void start(String name) { count = 0; scenario = name; }
    public static void head() { if (scenario != null) start = System.nanoTime(); }
    public static void tail() { if (scenario != null && count < samples.length) samples[count++] = System.nanoTime() - start; }
    public static void finish() {
        if (scenario == null || count == 0) throw new AssertionError("No render samples");
        long[] sorted = Arrays.copyOf(samples, count);
        Arrays.sort(sorted);
        report.append(scenario).append(',').append(count);
        for (double q : new double[]{.5,.95,.99}) report.append(',').append(String.format(java.util.Locale.ROOT,"%.4f", sorted[Math.min(count-1,(int)Math.ceil(count*q)-1)]/1e6));
        report.append('\n');
        rtx.nv.NV.LOGGER.info("[NV-METRICS] {} samples={}", scenario, count);
        scenario = null;
    }
    public static void save() {
        try { Files.writeString(FabricLoader.getInstance().getGameDir().resolve("render-cpu.csv"), report); }
        catch (java.io.IOException e) { throw new RuntimeException(e); }
    }
}

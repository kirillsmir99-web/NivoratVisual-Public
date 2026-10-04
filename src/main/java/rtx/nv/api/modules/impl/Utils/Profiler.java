package rtx.nv.api.modules.impl.Utils;
import rtx.nv.api.events.EventHandler;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.entity.Entity;
import rtx.nv.NV;
import rtx.nv.api.events.impl.render.HudRenderEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.utils.storage.RepositoryStorage;

public final class Profiler
extends Module {
    private static final int CAPACITY = 512;
    private static final int SPIKE_CAPACITY = 512;
    private static final double SPIKE_FLOOR_MS = 5.0;
    private static final double SPIKE_MULTIPLIER = 2.0;
    private static final DateTimeFormatter REPORT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_SSS", Locale.ROOT).withZone(ZoneId.systemDefault());
    private final double[] frames = new double[512];
    private final double[] scratch = new double[512];
    private final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private final Deque<Spike> spikes = new ArrayDeque<Spike>();
    private int count;
    private int head;
    private long startedAtMs;
    private long lastFrameNanos;
    private double baselineMs = 4.0;
    private long lastGcCount = -1L;
    private long lastGcPauseMs = -1L;
    private long lastHeapUsed = -1L;
    private long allocAccum;
    private long allocRatePerSec;
    private long allocWindowStart;
    private long lastWorldSample;
    private int baseEntities;
    private int baseParticles = -1;
    private long lastSectionSample;
    private int pcRecent;
    private int puRecent;
    private long pcRecentTime;
    private String lastSectionStats;
    private Field particleEngineField;
    private Field particlesMapField;
    private boolean reflectionResolved;

    public Profiler() {
        super("Profiler", "\u0421\u043e\u0431\u0438\u0440\u0430\u0435\u0442 \u043f\u0440\u0438\u0447\u0438\u043d\u044b \u043f\u0440\u043e\u0441\u0430\u0434\u043e\u043a FPS \u0438 \u0441\u043e\u0445\u0440\u0430\u043d\u044f\u0435\u0442 \u043e\u0442\u0447\u0451\u0442 \u043f\u0440\u0438 \u0432\u044b\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0438.", Category.UTILS);
    }

    private void push(double d) {
        this.frames[this.head] = d;
        this.head = (this.head + 1) % 512;
        if (this.count < 512) {
            ++this.count;
        }
    }

    private static Field findField(Class<?> clazz, String string) {
        for (Class<?> clazz2 = clazz; clazz2 != null && clazz2 != Object.class; clazz2 = clazz2.getSuperclass()) {
            try {
                return clazz2.getDeclaredField(string);
            }
            catch (NoSuchFieldException noSuchFieldException) {
                continue;
            }
        }
        return null;
    }

    private static String truncate(String string, int n) {
        return string.length() <= n ? string : string.substring(0, n - 1) + "...";
    }

    private static int parseStat(String string, String string2) {
        int n = string.indexOf(string2);
        if (n < 0) {
            return -1;
        }
        n += string2.length();
        while (n < string.length() && string.charAt(n) == ' ') {
            ++n;
        }
        int n2 = n;
        while (n < string.length() && Character.isDigit(string.charAt(n))) {
            ++n;
        }
        try {
            return n2 == n ? -1 : Integer.parseInt(string.substring(n2, n));
        }
        catch (NumberFormatException numberFormatException) {
            return -1;
        }
    }

    private long[] sampleGc() {
        long l = 0L;
        long l2 = 0L;
        for (GarbageCollectorMXBean garbageCollectorMXBean : this.gcBeans) {
            long l3 = garbageCollectorMXBean.getCollectionCount();
            long l4 = garbageCollectorMXBean.getCollectionTime();
            if (l3 > 0L) {
                l += l3;
            }
            if (l4 <= 0L) continue;
            l2 += l4;
        }
        return new long[]{l, l2};
    }

    private List<String> topThreads(int n) {
        ArrayList<String> arrayList = new ArrayList<String>();
        try {
            if (!this.threadBean.isThreadCpuTimeSupported()) {
                arrayList.add("JVM \u043d\u0435 \u043f\u043e\u0434\u0434\u0435\u0440\u0436\u0438\u0432\u0430\u0435\u0442 CPU-\u0442\u0430\u0439\u043c\u0438\u043d\u0433 \u043f\u043e\u0442\u043e\u043a\u043e\u0432.");
                return arrayList;
            }
            if (!this.threadBean.isThreadCpuTimeEnabled()) {
                this.threadBean.setThreadCpuTimeEnabled(true);
            }
            long[] lArray = this.threadBean.getAllThreadIds();
            ArrayList<ThreadCpu> arrayList2 = new ArrayList<ThreadCpu>();
            for (long l : lArray) {
                long l2 = this.threadBean.getThreadCpuTime(l);
                if (l2 <= 0L) continue;
                ThreadInfo threadInfo = this.threadBean.getThreadInfo(l);
                arrayList2.add(new ThreadCpu((String)(threadInfo == null ? "#" + l : threadInfo.getThreadName()), l2));
            }
            arrayList2.sort((threadCpu, threadCpu2) -> Long.compare(threadCpu2.cpuNanos, threadCpu.cpuNanos));
            for (int i = 0; i < Math.min(n, arrayList2.size()); ++i) {
                ThreadCpu threadCpu3 = (ThreadCpu)arrayList2.get(i);
                arrayList.add(Profiler.truncate(threadCpu3.name, 28) + " " + threadCpu3.cpuNanos / 1000000L + " ms");
            }
        }
        catch (Throwable throwable) {
            arrayList.add("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0447\u0438\u0442\u0430\u0442\u044c \u043f\u043e\u0442\u043e\u043a\u0438: " + throwable.getClass().getSimpleName());
        }
        return arrayList;
    }

    private void saveReport() {
        long l = System.currentTimeMillis();
        ArrayList<String> arrayList = new ArrayList<String>();
        arrayList.add("\u041e\u0442\u0447\u0451\u0442 NV Profiler");
        arrayList.add("\u041d\u0430\u0447\u0430\u043b\u043e: " + String.valueOf(Instant.ofEpochMilli(this.startedAtMs)));
        arrayList.add("\u041e\u043a\u043e\u043d\u0447\u0430\u043d\u0438\u0435: " + String.valueOf(Instant.ofEpochMilli(l)));
        arrayList.add("\u0414\u043b\u0438\u0442\u0435\u043b\u044c\u043d\u043e\u0441\u0442\u044c: " + Math.max(0L, l - this.startedAtMs) / 1000L + " \u0441");
        arrayList.add("");
        this.appendFrameStats(arrayList);
        this.appendRuntimeStats(arrayList);
        this.appendWorldStats(arrayList);
        this.appendThreadStats(arrayList);
        this.appendSpikes(arrayList, l);
        Path path = RepositoryStorage.configRoot().resolve("profiler");
        Path path2 = path.resolve("profiler-" + REPORT_TIME.format(Instant.ofEpochMilli(l)) + ".txt");
        try {
            Files.createDirectories(path, new FileAttribute[0]);
            Files.writeString(path2, (CharSequence)(String.join((CharSequence)System.lineSeparator(), arrayList) + System.lineSeparator()), StandardCharsets.UTF_8, new OpenOption[0]);
            NV.LOGGER.info("[Profiler] Saved report to {}", (Object)path2);
        }
        catch (IOException iOException) {
            NV.LOGGER.error("[Profiler] Failed to save report to {}", (Object)path2, (Object)iOException);
        }
    }

    private static long usedHeap() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static String mib(long l) {
        double d = (double)l / 1048576.0;
        return d >= 1024.0 ? String.format(Locale.ROOT, "%.2f GiB", d / 1024.0) : String.format(Locale.ROOT, "%.0f MiB", d);
    }

    private void resetStats() {
        this.count = 0;
        this.head = 0;
        this.lastFrameNanos = 0L;
        this.baselineMs = 4.0;
        this.lastGcCount = -1L;
        this.lastGcPauseMs = -1L;
        this.lastHeapUsed = -1L;
        this.allocAccum = 0L;
        this.allocRatePerSec = 0L;
        this.allocWindowStart = 0L;
        this.lastWorldSample = 0L;
        this.baseEntities = 0;
        this.baseParticles = -1;
        this.lastSectionSample = 0L;
        this.pcRecent = 0;
        this.puRecent = 0;
        this.pcRecentTime = 0L;
        this.lastSectionStats = null;
        this.spikes.clear();
    }

    @EventHandler
    private void onHud(HudRenderEvent hudRenderEvent) {
        long l = System.nanoTime();
        if (this.lastFrameNanos == 0L) {
            this.lastFrameNanos = l;
            return;
        }
        double d = (double)(l - this.lastFrameNanos) / 1000000.0;
        this.lastFrameNanos = l;
        if (d <= 0.0 || d > 1000.0) {
            return;
        }
        this.push(d);
        this.recomputeBaseline();
        long l2 = System.currentTimeMillis();
        this.updateAllocation(l2);
        long[] lArray = this.sampleGc();
        long l3 = this.lastGcCount < 0L ? 0L : lArray[0] - this.lastGcCount;
        long l4 = this.gcPauseTotalMs();
        long l5 = this.lastGcPauseMs < 0L ? 0L : l4 - this.lastGcPauseMs;
        this.lastGcCount = lArray[0];
        this.lastGcPauseMs = l4;
        this.maybeSampleWorld(l2);
        this.detectSpike(d, l3, l5, l2);
    }

    @Override
    protected void onDisable() {
        this.saveReport();
    }

    @Override
    protected void onEnable() {
        this.resetStats();
        this.startedAtMs = System.currentTimeMillis();
    }

    private void maybeSampleWorld(long l) {
        if (l - this.lastSectionSample >= 200L) {
            this.lastSectionSample = l;
            this.sampleSections(l);
        }
        if (l - this.lastWorldSample < 500L) {
            return;
        }
        this.lastWorldSample = l;
        this.baseEntities = this.countEntities();
        this.baseParticles = this.countParticles();
    }

    private void detectSpike(double d, long l, long l2, long l3) {
        Object object;
        String string;
        if (this.count < 20 || d < 5.0 || d < this.baselineMs * 2.0) {
            return;
        }
        int n = this.countEntities();
        int n2 = this.countParticles();
        int n3 = n - this.baseEntities;
        int n4 = n2 < 0 || this.baseParticles < 0 ? 0 : n2 - this.baseParticles;
        boolean bl = l3 - this.pcRecentTime <= 2000L && (this.pcRecent >= 4 || this.puRecent >= 2);
        l2 = Math.max(0L, l2);
        if (l > 0L && l2 >= 5L && (double)l2 >= 0.35 * d) {
            string = "\u0421\u0431\u043e\u0440\u043a\u0430 \u043c\u0443\u0441\u043e\u0440\u0430 (GC)";
            object = String.format(Locale.ROOT, "STW-\u043f\u0430\u0443\u0437\u0430 ~%d \u043c\u0441, \u0430\u043b\u043b\u043e\u043a\u0430\u0446\u0438\u0438 %s/\u0441", l2, Profiler.mib(this.allocRatePerSec));
        } else if (bl) {
            string = "\u041f\u0435\u0440\u0435\u0441\u0442\u0440\u043e\u0439\u043a\u0430 \u0447\u0430\u043d\u043a\u043e\u0432";
            object = "\u043e\u0447\u0435\u0440\u0435\u0434\u044c pC:" + this.pcRecent + " pU:" + this.puRecent + ", \u0440\u0435\u043d\u0434\u0435\u0440 \u0441\u0442\u0440\u043e\u0438\u0442 \u0433\u0435\u043e\u043c\u0435\u0442\u0440\u0438\u044e";
        } else if (n4 > 800 || n2 > 5000) {
            string = "\u0427\u0430\u0441\u0442\u0438\u0446\u044b";
            object = "\u0447\u0430\u0441\u0442\u0438\u0446: " + n2 + " (+" + n4 + ")";
        } else if (n3 > 30) {
            string = "\u041f\u043e\u0434\u0433\u0440\u0443\u0437\u043a\u0430 \u0441\u0443\u0449\u043d\u043e\u0441\u0442\u0435\u0439";
            object = "\u0441\u0443\u0449\u043d\u043e\u0441\u0442\u0435\u0439: " + n + " (+" + n3 + ")";
        } else if (this.allocRatePerSec > 1572864000L) {
            string = "\u0412\u044b\u0441\u043e\u043a\u0438\u0435 \u0430\u043b\u043b\u043e\u043a\u0430\u0446\u0438\u0438";
            object = "\u0430\u043b\u043b\u043e\u043a\u0430\u0446\u0438\u0438 " + Profiler.mib(this.allocRatePerSec) + "/\u0441, \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0435 \u0434\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u043d\u0430 GC";
        } else {
            string = "\u0412\u043d\u0435\u0448\u043d\u0438\u0439 \u0441\u0442\u043e\u043f";
            object = "GC, \u0447\u0430\u043d\u043a\u0438 \u0438 \u043c\u0438\u0440 \u0441\u043f\u043e\u043a\u043e\u0439\u043d\u044b: \u0432\u0435\u0440\u043e\u044f\u0442\u043d\u044b \u041e\u0421, \u0434\u0440\u0430\u0439\u0432\u0435\u0440, \u0444\u043e\u043d\u043e\u0432\u044b\u0435 \u043f\u0440\u043e\u0446\u0435\u0441\u0441\u044b \u0438\u043b\u0438 \u0442\u0440\u043e\u0442\u0442\u043b\u0438\u043d\u0433";
        }
        this.spikes.addFirst(new Spike(l3, d, this.baselineMs, string, (String)object, l2, this.allocRatePerSec, this.pcRecent, this.puRecent, n, n2));
        while (this.spikes.size() > 512) {
            this.spikes.removeLast();
        }
    }

    private void recomputeBaseline() {
        if (this.count == 0) {
            return;
        }
        for (int i = 0; i < this.count; ++i) {
            this.scratch[i] = this.frames[(this.head - this.count + i + 512) % 512];
        }
        Arrays.sort(this.scratch, 0, this.count);
        this.baselineMs = this.scratch[this.count / 2];
    }

    private void sampleSections(long l) {
        String string = this.sectionStats();
        if (string == null) {
            return;
        }
        this.lastSectionStats = string;
        int n = Profiler.parseStat(string, "pC:");
        int n2 = Profiler.parseStat(string, "pU:");
        if (l - this.pcRecentTime > 2000L) {
            this.pcRecent = 0;
            this.puRecent = 0;
        }
        if (n >= 0 && (n >= this.pcRecent || l - this.pcRecentTime > 2000L)) {
            this.pcRecent = n;
            this.pcRecentTime = l;
        }
        if (n2 > this.puRecent) {
            this.puRecent = n2;
            this.pcRecentTime = l;
        }
    }

    private void appendRuntimeStats(List<String> list) {
        list.add("");
        list.add("\u041f\u0430\u043c\u044f\u0442\u044c \u0438 GC");
        list.add("\u041a\u0443\u0447\u0430: " + Profiler.mib(Profiler.usedHeap()) + " / " + Profiler.mib(Runtime.getRuntime().maxMemory()) + "; \u0430\u043b\u043b\u043e\u043a\u0430\u0446\u0438\u0438: " + Profiler.mib(this.allocRatePerSec) + "/\u0441");
        for (GarbageCollectorMXBean garbageCollectorMXBean : this.gcBeans) {
            list.add(garbageCollectorMXBean.getName() + ": " + garbageCollectorMXBean.getCollectionCount() + " \u0441\u0431\u043e\u0440\u043e\u043a, " + garbageCollectorMXBean.getCollectionTime() + " \u043c\u0441 \u0432\u0441\u0435\u0433\u043e");
        }
    }

    private void appendWorldStats(List<String> list) {
        list.add("");
        list.add("\u041c\u0438\u0440");
        if (this.mc.world == null) {
            list.add("\u0412 \u043c\u043e\u043c\u0435\u043d\u0442 \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0438\u044f \u043c\u0438\u0440 \u043d\u0435 \u0431\u044b\u043b \u0437\u0430\u0433\u0440\u0443\u0436\u0435\u043d.");
            return;
        }
        int n = this.countParticles();
        list.add("\u0421\u0443\u0449\u043d\u043e\u0441\u0442\u0438: " + this.countEntities() + "; \u0447\u0430\u0441\u0442\u0438\u0446\u044b: " + String.valueOf(n < 0 ? "\u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u043d\u043e" : Integer.valueOf(n)));
        if (this.lastSectionStats != null) {
            list.add("\u0421\u0442\u0430\u0442\u0438\u0441\u0442\u0438\u043a\u0430 \u0441\u0435\u043a\u0446\u0438\u0439: " + this.lastSectionStats);
        }
    }

    private void appendSpikes(List<String> list, long l) {
        list.add("");
        list.add("\u041d\u0430\u0439\u0434\u0435\u043d\u043e \u043f\u043e\u0434\u043e\u0437\u0440\u0435\u043d\u0438\u0439: " + this.spikes.size());
        if (this.spikes.isEmpty()) {
            list.add("\u041f\u0440\u043e\u0441\u0430\u0434\u043e\u043a, \u043f\u0440\u043e\u0448\u0435\u0434\u0448\u0438\u0445 \u043f\u043e\u0440\u043e\u0433 \u0434\u0435\u0442\u0435\u043a\u0442\u043e\u0440\u0430, \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u043e.");
            return;
        }
        for (Spike spike : this.spikes) {
            long l2 = Math.max(0L, l - spike.timeMs) / 1000L;
            list.add(String.format(Locale.ROOT, "- %.1f \u043c\u0441 (\u043d\u043e\u0440\u043c\u0430 %.1f \u043c\u0441), %s, \u0437\u0430 %d \u0441 \u0434\u043e \u0432\u044b\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u044f", spike.frameMs, spike.baselineMs, spike.cause, l2));
            list.add(String.format(Locale.ROOT, "  %s; \u043f\u0430\u0443\u0437\u0430 GC %d \u043c\u0441; \u0430\u043b\u043b\u043e\u043a\u0430\u0446\u0438\u0438 %s/\u0441; pC%d pU%d; \u0441\u0443\u0449\u043d\u043e\u0441\u0442\u0438 %d; \u0447\u0430\u0441\u0442\u0438\u0446\u044b %s", spike.detail, spike.gcPauseMs, Profiler.mib(spike.allocPerSec), spike.pc, spike.pu, spike.entities, spike.particles < 0 ? "\u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u043d\u043e" : String.valueOf(spike.particles)));
        }
    }

    private void appendFrameStats(List<String> list) {
        list.add("\u041a\u0430\u0434\u0440\u044b");
        if (this.count == 0) {
            list.add("\u041a\u043e\u0440\u0440\u0435\u043a\u0442\u043d\u044b\u0445 \u043a\u0430\u0434\u0440\u043e\u0432 \u043d\u0435 \u0437\u0430\u043f\u0438\u0441\u0430\u043d\u043e.");
            return;
        }
        double d = 0.0;
        double d2 = Double.MAX_VALUE;
        double d3 = 0.0;
        for (int i = 0; i < this.count; ++i) {
            double d4 = this.frames[(this.head - this.count + i + 512) % 512];
            d += d4;
            d2 = Math.min(d2, d4);
            d3 = Math.max(d3, d4);
        }
        this.recomputeBaseline();
        double d5 = d / (double)this.count;
        double d6 = this.scratch[(int)Math.floor(0.99 * (double)(this.count - 1))];
        double d7 = this.scratch[(int)Math.floor(0.999 * (double)(this.count - 1))];
        list.add(String.format(Locale.ROOT, "\u0412\u044b\u0431\u043e\u0440\u043a\u0430: %d; \u0441\u0440\u0435\u0434\u043d\u0435\u0435 %.1f \u043c\u0441 (%.0f FPS); \u043c\u0438\u043d. %.1f \u043c\u0441; \u043c\u0430\u043a\u0441. %.1f \u043c\u0441", this.count, d5, 1000.0 / d5, d2, d3));
        list.add(String.format(Locale.ROOT, "1%% low: %.0f FPS (%.1f \u043c\u0441); 0.1%% low: %.0f FPS (%.1f \u043c\u0441); \u043c\u0435\u0434\u0438\u0430\u043d\u0430 %.1f \u043c\u0441", 1000.0 / d6, d6, 1000.0 / d7, d7, this.baselineMs));
    }

    private int countParticles() {
        if (this.mc.world == null) {
            return -1;
        }
        try {
            Object object;
            Object object2;
            if (!this.reflectionResolved) {
                this.particleEngineField = Profiler.findField(this.mc.getClass(), "particleEngine");
                if (this.particleEngineField != null) {
                    this.particleEngineField.setAccessible(true);
                    object2 = this.particleEngineField.get(this.mc);
                    if (object2 != null) {
                        this.particlesMapField = Profiler.findField(object2.getClass(), "particles");
                        if (this.particlesMapField != null) {
                            this.particlesMapField.setAccessible(true);
                        }
                    }
                }
                this.reflectionResolved = true;
            }
            if (this.particleEngineField == null || this.particlesMapField == null) {
                return -1;
            }
            object2 = this.particleEngineField.get(this.mc);
            Object object3 = object = object2 == null ? null : this.particlesMapField.get(object2);
            if (!(object instanceof Map)) {
                return -1;
            }
            Map map = (Map)object;
            int n = 0;
            for (Object v : map.values()) {
                if (!(v instanceof Collection)) continue;
                Collection collection = (Collection)v;
                n += collection.size();
            }
            return n;
        }
        catch (Throwable throwable) {
            return -1;
        }
    }

    private int countEntities() {
        if (this.mc.world == null) {
            return 0;
        }
        try {
            int n = 0;
            for (Entity entity : this.mc.world.getEntities()) {
                ++n;
            }
            return n;
        }
        catch (Throwable throwable) {
            return -1;
        }
    }

    private String sectionStats() {
        try {
            Method method = this.mc.worldRenderer.getClass().getMethod("getSectionStatistics", new Class[0]);
            Object object = method.invoke((Object)this.mc.worldRenderer, new Object[0]);
            return object == null ? null : object.toString();
        }
        catch (Throwable throwable) {
            return null;
        }
    }

    private void appendThreadStats(List<String> list) {
        list.add("");
        list.add("\u0422\u043e\u043f JVM-\u043f\u043e\u0442\u043e\u043a\u043e\u0432 \u043f\u043e \u043d\u0430\u043a\u043e\u043f\u043b\u0435\u043d\u043d\u043e\u043c\u0443 CPU-\u0432\u0440\u0435\u043c\u0435\u043d\u0438");
        for (String string : this.topThreads(6)) {
            list.add("- " + string);
        }
    }

    private void updateAllocation(long l) {
        long l2 = Profiler.usedHeap();
        if (this.lastHeapUsed >= 0L && l2 > this.lastHeapUsed) {
            this.allocAccum += l2 - this.lastHeapUsed;
        }
        this.lastHeapUsed = l2;
        if (this.allocWindowStart == 0L) {
            this.allocWindowStart = l;
        }
        if (l - this.allocWindowStart >= 1000L) {
            this.allocRatePerSec = this.allocAccum;
            this.allocAccum = 0L;
            this.allocWindowStart = l;
        }
    }

    private long gcPauseTotalMs() {
        long l = 0L;
        for (GarbageCollectorMXBean garbageCollectorMXBean : this.gcBeans) {
            long l2;
            if (garbageCollectorMXBean.getName().toLowerCase(Locale.ROOT).contains("concurrent") || (l2 = garbageCollectorMXBean.getCollectionTime()) <= 0L) continue;
            l += l2;
        }
        return l;
    }

    public static record Spike(
        long timeMs,
        double frameMs,
        double baselineMs,
        String cause,
        String detail,
        long gcPauseMs,
        long allocPerSec,
        int pc,
        int pu,
        int entities,
        int particles
    ) {}

    public static record ThreadCpu(String name, long cpuNanos) {}
}


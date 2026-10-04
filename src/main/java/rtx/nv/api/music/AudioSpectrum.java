package rtx.nv.api.music;

import com.google.gson.JsonArray;

public final class AudioSpectrum {
    private static volatile float[] bands = new float[12];
    private static volatile long sampled;
    private AudioSpectrum() {}
    public static void update(JsonArray values) {
        float[] next = new float[12];
        if (values != null) for (int i=0; i<Math.min(12, values.size()); i++) {
            try { float v=values.get(i).getAsFloat();next[i]=Float.isFinite(v)?Math.max(0,Math.min(1,v)):0; } catch(RuntimeException ignored) {}
        }
        bands=next;sampled=System.nanoTime();
    }
    public static float level(int index, int total) {
        if (System.nanoTime()-sampled > 1_000_000_000L) return 0;
        float[] values=bands;
        int start=Math.min(11,index*12/Math.max(1,total));
        int end=Math.min(12,Math.max(start+1,(index+1)*12/Math.max(1,total)));
        float peak=0;for(int i=start;i<end;i++) peak=Math.max(peak,values[i]);return peak;
    }
}

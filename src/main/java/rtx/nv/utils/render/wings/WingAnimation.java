package rtx.nv.utils.render.wings;

import java.util.List;
import java.util.Map;

public record WingAnimation(float length, Map<String, List<Keyframe>> boneKeyframes) {
    public record Keyframe(float time, float rx, float ry, float rz) {}

    public WingAnimation {
        var normalized = new java.util.HashMap<String, List<Keyframe>>();
        for (var entry : boneKeyframes.entrySet()) {
            var keys = new java.util.ArrayList<>(entry.getValue());
            keys.sort(java.util.Comparator.comparingDouble(Keyframe::time));
            if (length > 0 && keys.size() > 1) {
                Keyframe first = keys.getFirst(), last = keys.getLast();
                // Imported loops sometimes finish in a different pose. Match the
                // explicit closing frame to the opening pose before interpolation.
                if (Math.abs(first.time) < .0001f && Math.abs(last.time-length) < .0001f) {
                    keys.set(keys.size()-1, new Keyframe(last.time, first.rx, first.ry, first.rz));
                }
            }
            normalized.put(entry.getKey(), List.copyOf(keys));
        }
        boneKeyframes = Map.copyOf(normalized);
    }

    public float[] sampleBone(String boneName, float timeSec) {
        List<Keyframe> list = boneKeyframes.get(boneName);
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() == 1) {
            Keyframe k = list.get(0);
            return new float[]{k.rx, k.ry, k.rz};
        }

        float t = length > 0 ? timeSec % length : timeSec;
        if (t < 0) {
            t += length;
        }

        Keyframe first = list.getFirst();
        Keyframe last = list.getLast();
        Keyframe prev = last;
        Keyframe next = first;
        float prevTime = last.time - Math.max(0, length);
        float nextTime = first.time;

        for (int i = 0; i < list.size(); i++) {
            Keyframe k = list.get(i);
            if (k.time <= t) {
                prev = k;
                prevTime = k.time;
                next = first;
                nextTime = first.time + Math.max(0, length);
            }
            if (k.time >= t) {
                next = k;
                nextTime = k.time;
                break;
            }
        }

        if (prev == next || nextTime <= prevTime) {
            return new float[]{prev.rx, prev.ry, prev.rz};
        }

        float factor = (t - prevTime) / (nextTime - prevTime);
        factor = Math.clamp(factor, 0.0f, 1.0f);
        factor = factor * factor * (3.0f - 2.0f * factor);

        return new float[]{
                interpolateAngle(prev.rx, next.rx, factor),
                interpolateAngle(prev.ry, next.ry, factor),
                interpolateAngle(prev.rz, next.rz, factor)
        };
    }

    private static float interpolateAngle(float from, float to, float factor) {
        float delta = (to - from) % 360.0f;
        if (delta > 180) delta -= 360;
        if (delta < -180) delta += 360;
        return from + delta * factor;
    }
}

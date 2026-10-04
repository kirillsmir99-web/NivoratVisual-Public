package rtx.nv.utils.render.wings;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class WingModelRegistry {
    private static final Map<String, WingModel> CACHE = new ConcurrentHashMap<>();

    private WingModelRegistry() {}

    public static WingModel getModel(String id) {
        if (rtx.nv.ClientEdition.isTrial() && !"crystal".equals(id)) return null;
        if (id == null || id.isBlank()) {
            return null;
        }
        return CACHE.computeIfAbsent(id, WingModelRegistry::loadModel);
    }

    private static WingModel loadModel(String id) {
        String resourcePath = "/assets/nv/models/wings/" + id + ".json";
        try (InputStream stream = WingModelRegistry.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return null;
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            String name = root.has("name") ? root.get("name").getAsString() : id;
            int texW = root.has("texture_width") ? root.get("texture_width").getAsInt() : 64;
            int texH = root.has("texture_height") ? root.get("texture_height").getAsInt() : 64;
            int totalTexH = root.has("total_texture_height") ? root.get("total_texture_height").getAsInt() : texH;

            float[] offset = parseFloatArray(root.get("offset"), 3);
            float offX = offset[0];
            float offY = offset[1];
            float offZ = offset[2];

            List<WingBone> rootBones = new ArrayList<>();
            if (root.has("bones")) {
                for (JsonElement boneElem : root.getAsJsonArray("bones")) {
                    if (boneElem.isJsonObject()) {
                        rootBones.add(parseBone(boneElem.getAsJsonObject(), null));
                    }
                }
            }

            Map<String, WingAnimation> animMap = new HashMap<>();
            if (root.has("animations")) {
                JsonObject animsObj = root.getAsJsonObject("animations");
                for (Map.Entry<String, JsonElement> aEntry : animsObj.entrySet()) {
                    String aName = aEntry.getKey();
                    if (!aEntry.getValue().isJsonObject()) continue;
                    JsonObject aObj = aEntry.getValue().getAsJsonObject();
                    float aLen = aObj.has("length") ? aObj.get("length").getAsFloat() : 1.0f;
                    Map<String, List<WingAnimation.Keyframe>> bMap = new HashMap<>();
                    if (aObj.has("bones") && aObj.get("bones").isJsonObject()) {
                        JsonObject bObj = aObj.getAsJsonObject("bones");
                        for (Map.Entry<String, JsonElement> bkEntry : bObj.entrySet()) {
                            String bName = bkEntry.getKey();
                            if (!bkEntry.getValue().isJsonArray()) continue;
                            List<WingAnimation.Keyframe> kfList = new ArrayList<>();
                            for (JsonElement kElem : bkEntry.getValue().getAsJsonArray()) {
                                if (!kElem.isJsonObject()) continue;
                                JsonObject kObj = kElem.getAsJsonObject();
                                float t = kObj.has("t") ? kObj.get("t").getAsFloat() : 0.0f;
                                float[] r = parseFloatArray(kObj.get("r"), 3);
                                kfList.add(new WingAnimation.Keyframe(t, r[0], r[1], r[2]));
                            }
                            kfList.sort(java.util.Comparator.comparingDouble(WingAnimation.Keyframe::time));
                            bMap.put(bName, List.copyOf(kfList));
                        }
                    }
                    animMap.put(aName, new WingAnimation(aLen, bMap));
                }
            }

            return new WingModel(id, name, texW, texH, totalTexH, offX, offY, offZ, rootBones, animMap);
        } catch (Exception e) {
            return null;
        }
    }

    private static WingBone parseBone(JsonObject obj, Boolean parentSide) {
        String name = obj.has("name") ? obj.get("name").getAsString() : "bone";
        float[] origin = parseFloatArray(obj.get("origin"), 3);
        float[] rotation = parseFloatArray(obj.get("rotation"), 3);

        boolean isLeft = false;
        boolean isRight = false;

        // Physical coordinates on Minecraft player model: +X is Left, -X is Right
        if (origin[0] > 0.1f) {
            isLeft = true;
        } else if (origin[0] < -0.1f) {
            isRight = true;
        } else if (parentSide != null) {
            isLeft = parentSide;
            isRight = !parentSide;
        }

        List<WingCube> cubes = new ArrayList<>();
        if (obj.has("cubes")) {
            float sumX = 0.0f;
            int cubeCount = 0;
            for (JsonElement cubeElem : obj.getAsJsonArray("cubes")) {
                if (cubeElem.isJsonObject()) {
                    JsonObject cubeObj = cubeElem.getAsJsonObject();
                    cubes.add(parseCube(cubeObj));
                    float[] from = parseFloatArray(cubeObj.get("from"), 3);
                    float[] to = parseFloatArray(cubeObj.get("to"), 3);
                    sumX += (from[0] + to[0]) * 0.5f;
                    cubeCount++;
                }
            }
            if (!isLeft && !isRight && cubeCount > 0) {
                float avgX = sumX / cubeCount;
                if (avgX > 0.1f) {
                    isLeft = true;
                } else if (avgX < -0.1f) {
                    isRight = true;
                }
            }
        }

        if (!isLeft && !isRight) {
            String lowerName = name.toLowerCase(java.util.Locale.ROOT);
            if (lowerName.contains("left") || lowerName.contains("_l") || lowerName.startsWith("l_") || lowerName.endsWith("_l")) {
                isLeft = true;
            } else if (lowerName.contains("right") || lowerName.contains("_r") || lowerName.startsWith("r_") || lowerName.endsWith("_r")) {
                isRight = true;
            } else if (obj.has("is_left") || obj.has("is_right")) {
                isLeft = obj.has("is_left") && obj.get("is_left").getAsBoolean();
                isRight = obj.has("is_right") && obj.get("is_right").getAsBoolean();
            }
        }

        Boolean currentSide = isLeft ? Boolean.TRUE : (isRight ? Boolean.FALSE : parentSide);
        List<WingBone> children = new ArrayList<>();
        if (obj.has("children")) {
            for (JsonElement childElem : obj.getAsJsonArray("children")) {
                if (childElem.isJsonObject()) {
                    children.add(parseBone(childElem.getAsJsonObject(), currentSide));
                }
            }
        }

        return new WingBone(name, origin, rotation, isLeft, isRight, cubes, children);
    }

    private static WingCube parseCube(JsonObject obj) {
        float[] from = parseFloatArray(obj.get("from"), 3);
        float[] to = parseFloatArray(obj.get("to"), 3);
        float[] origin = parseFloatArray(obj.get("origin"), 3);
        float[] rotation = obj.has("rotation") && !obj.get("rotation").isJsonNull()
                ? parseFloatArray(obj.get("rotation"), 3)
                : null;

        float minX = Math.min(from[0], to[0]);
        float maxX = Math.max(from[0], to[0]);
        float minY = Math.min(from[1], to[1]);
        float maxY = Math.max(from[1], to[1]);
        float minZ = Math.min(from[2], to[2]);
        float maxZ = Math.max(from[2], to[2]);

        List<WingFace> faceList = new ArrayList<>();
        if (obj.has("faces")) {
            JsonObject facesObj = obj.getAsJsonObject("faces");
            for (Map.Entry<String, JsonElement> entry : facesObj.entrySet()) {
                String faceName = entry.getKey();
                float[] uv = parseFloatArray(entry.getValue(), 4);
                if (uv[0] == uv[2] && uv[1] == uv[3]) {
                    continue;
                }
                WingFace face = createFace(faceName, minX, maxX, minY, maxY, minZ, maxZ, uv[0], uv[1], uv[2], uv[3]);
                if (face != null) {
                    faceList.add(face);
                }
            }
        }

        return new WingCube(faceList.toArray(new WingFace[0]), origin, rotation);
    }

    private static WingFace createFace(String faceName, float minX, float maxX, float minY, float maxY, float minZ, float maxZ, float u1, float v1, float u2, float v2) {
        float[] x;
        float[] y;
        float[] z;
        float[] u = new float[]{u1, u2, u2, u1};
        float[] v = new float[]{v1, v1, v2, v2};
        float nx = 0, ny = 0, nz = 0;

        switch (faceName.toLowerCase()) {
            case "north" -> {
                x = new float[]{maxX, minX, minX, maxX};
                y = new float[]{maxY, maxY, minY, minY};
                z = new float[]{minZ, minZ, minZ, minZ};
                nz = -1;
            }
            case "south" -> {
                x = new float[]{minX, maxX, maxX, minX};
                y = new float[]{maxY, maxY, minY, minY};
                z = new float[]{maxZ, maxZ, maxZ, maxZ};
                nz = 1;
            }
            case "west" -> {
                x = new float[]{minX, minX, minX, minX};
                y = new float[]{maxY, maxY, minY, minY};
                z = new float[]{minZ, maxZ, maxZ, minZ};
                nx = -1;
            }
            case "east" -> {
                x = new float[]{maxX, maxX, maxX, maxX};
                y = new float[]{maxY, maxY, minY, minY};
                z = new float[]{maxZ, minZ, minZ, maxZ};
                nx = 1;
            }
            case "up" -> {
                x = new float[]{minX, maxX, maxX, minX};
                y = new float[]{maxY, maxY, maxY, maxY};
                z = new float[]{minZ, minZ, maxZ, maxZ};
                ny = 1;
            }
            case "down" -> {
                x = new float[]{minX, maxX, maxX, minX};
                y = new float[]{minY, minY, minY, minY};
                z = new float[]{maxZ, maxZ, minZ, minZ};
                ny = -1;
            }
            default -> {
                return null;
            }
        }

        return new WingFace(x, y, z, u, v, nx, ny, nz);
    }

    private static float[] parseFloatArray(JsonElement elem, int count) {
        float[] arr = new float[count];
        if (elem != null && elem.isJsonArray()) {
            JsonArray jsonArr = elem.getAsJsonArray();
            for (int i = 0; i < count && i < jsonArr.size(); i++) {
                arr[i] = jsonArr.get(i).getAsFloat();
            }
        }
        return arr;
    }
}

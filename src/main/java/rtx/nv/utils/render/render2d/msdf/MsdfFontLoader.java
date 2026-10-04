package rtx.nv.utils.render.render2d.msdf;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.ReloadableTexture;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import rtx.nv.NV;

final class MsdfFontLoader {
    private MsdfFontLoader() {
    }

    static MsdfFont load(String string) {
        Identifier identifier = Identifier.of("nv", string + ".json");
        Identifier identifier2 = Identifier.of("nv", string + ".png");
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        try {
            minecraftClient.getTextureManager().registerTexture(identifier2, (ReloadableTexture)new ResourceTexture(identifier2));
        }
        catch (Throwable throwable) {
            NV.LOGGER.warn("[MSDF] Failed to register atlas texture {}", identifier2, throwable);
            return null;
        }
        Optional<Resource> optional = minecraftClient.getResourceManager().getResource(identifier);
        if (optional.isEmpty()) {
            NV.LOGGER.warn("[MSDF] Layout JSON not found: {}", identifier);
            return null;
        }
        try (BufferedReader bufferedReader = optional.get().getReader()) {
            JsonObject jsonObject = JsonParser.parseReader((Reader)bufferedReader).getAsJsonObject();
            JsonObject jsonObject2 = jsonObject.getAsJsonObject("atlas");
            int n = jsonObject2.get("width").getAsInt();
            int n2 = jsonObject2.get("height").getAsInt();
            JsonObject jsonObject3 = jsonObject.getAsJsonObject("metrics");
            float f2 = jsonObject3.get("lineHeight").getAsFloat();
            float f3 = jsonObject3.get("ascender").getAsFloat();
            float f4 = jsonObject3.get("descender").getAsFloat();
            HashMap<Integer, MsdfGlyph> hashMap = new HashMap<>(512);
            for (JsonElement glyphElem : jsonObject.getAsJsonArray("glyphs")) {
                JsonObject jsonObject4 = glyphElem.getAsJsonObject();
                int n3 = jsonObject4.get("unicode").getAsInt();
                float f5 = jsonObject4.has("advance") ? jsonObject4.get("advance").getAsFloat() : 0.0f;
                if (!jsonObject4.has("planeBounds") || !jsonObject4.has("atlasBounds")) {
                    hashMap.put(n3, MsdfGlyph.nonDrawable(f5));
                    continue;
                }
                JsonObject jsonObject5 = jsonObject4.getAsJsonObject("planeBounds");
                JsonObject jsonObject6 = jsonObject4.getAsJsonObject("atlasBounds");
                float f = jsonObject5.get("left").getAsFloat();
                float f7 = jsonObject5.get("bottom").getAsFloat();
                float f8 = jsonObject5.get("right").getAsFloat();
                float f9 = jsonObject5.get("top").getAsFloat();
                float f10 = jsonObject6.get("left").getAsFloat();
                float f11 = jsonObject6.get("bottom").getAsFloat();
                float f12 = jsonObject6.get("right").getAsFloat();
                float f13 = jsonObject6.get("top").getAsFloat();
                float f14 = f10 / (float)n;
                float f15 = f12 / (float)n;
                float f16 = ((float)n2 - f13) / (float)n2;
                float f17 = ((float)n2 - f11) / (float)n2;
                hashMap.put(n3, new MsdfGlyph(true, f5, f, f9, f8, f7, f14, f16, f15, f17));
            }
            HashMap<Long, Float> hashMap2 = new HashMap<>();
            if (jsonObject.has("kerning")) {
                JsonArray kerningArray = jsonObject.getAsJsonArray("kerning");
                for (JsonElement jsonElement : kerningArray) {
                    JsonObject jsonObject7 = jsonElement.getAsJsonObject();
                    int n4 = jsonObject7.get("unicode1").getAsInt();
                    int n5 = jsonObject7.get("unicode2").getAsInt();
                    float f = jsonObject7.get("advance").getAsFloat();
                    if (f == 0.0f) continue;
                    hashMap2.put((long)n4 << 32 | (long)n5 & 0xFFFFFFFFL, f);
                }
            }
            NV.LOGGER.info("[MSDF] Loaded font {} ({} glyphs, {}x{} atlas)", string, hashMap.size(), n, n2);
            return new MsdfFont(identifier2, n, n2, f2, f3, f4, hashMap, hashMap2);
        }
        catch (Throwable throwable3) {
            NV.LOGGER.warn("[MSDF] Failed to parse layout {}", identifier, throwable3);
            return null;
        }
    }
}

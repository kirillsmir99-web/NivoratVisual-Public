package rtx.nv.api.mods.geckolib.loading.json.raw;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.MathHelper;
import rtx.nv.api.mods.geckolib.GeckoLibConstants;
import rtx.nv.api.mods.geckolib.util.JsonUtil;

public record FaceUV(String materialInstance, double[] uv, double[] uvSize, FaceUV.Rotation rotation) {
    public FaceUV.Rotation uvRotation() {
        return this.rotation;
    }

    public static JsonDeserializer<FaceUV> deserializer() throws JsonParseException {
        return (jsonElement, type, jsonDeserializationContext) -> {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            String string = JsonHelper.getString((JsonObject)jsonObject, (String)"material_instance", null);
            double[] dArray = JsonUtil.jsonArrayToDoubleArray(JsonHelper.getArray((JsonObject)jsonObject, (String)"uv", null));
            double[] dArray2 = JsonUtil.jsonArrayToDoubleArray(JsonHelper.getArray((JsonObject)jsonObject, (String)"uv_size", null));
            FaceUV.Rotation rotation = FaceUV.Rotation.fromValue(JsonHelper.getInt((JsonObject)jsonObject, (String)"uv_rotation", (int)0));
            return new FaceUV(string, dArray, dArray2, rotation);
        };
    }


    public static enum Rotation {
        NONE,
        CLOCKWISE_90,
        CLOCKWISE_180,
        CLOCKWISE_270;
    
    
        public static Rotation fromValue(int n) throws JsonParseException {
            try {
                return Rotation.values()[n % 360 / 90];
            }
            catch (Exception exception) {
                GeckoLibConstants.LOGGER.error("Invalid Face UV rotation: {}", (Object)n);
                return Rotation.fromValue(MathHelper.floor((float)((float)Math.abs(n) / 90.0f)) * 90);
            }
        }
    
        public float[] rotateUvs(float f, float f2, float f3, float f4) {
            float[] fArray;
            switch (this.ordinal()) {
                default: {
                    throw new MatchException(null, null);
                }
                case 0: {
                    float[] fArray2 = new float[8];
                    fArray2[0] = f;
                    fArray2[1] = f2;
                    fArray2[2] = f3;
                    fArray2[3] = f2;
                    fArray2[4] = f3;
                    fArray2[5] = f4;
                    fArray2[6] = f;
                    fArray = fArray2;
                    fArray2[7] = f4;
                    break;
                }
                case 1: {
                    float[] fArray3 = new float[8];
                    fArray3[0] = f3;
                    fArray3[1] = f2;
                    fArray3[2] = f3;
                    fArray3[3] = f4;
                    fArray3[4] = f;
                    fArray3[5] = f4;
                    fArray3[6] = f;
                    fArray = fArray3;
                    fArray3[7] = f2;
                    break;
                }
                case 2: {
                    float[] fArray4 = new float[8];
                    fArray4[0] = f3;
                    fArray4[1] = f4;
                    fArray4[2] = f;
                    fArray4[3] = f4;
                    fArray4[4] = f;
                    fArray4[5] = f2;
                    fArray4[6] = f3;
                    fArray = fArray4;
                    fArray4[7] = f2;
                    break;
                }
                case 3: {
                    float[] fArray5 = new float[8];
                    fArray5[0] = f;
                    fArray5[1] = f4;
                    fArray5[2] = f;
                    fArray5[3] = f2;
                    fArray5[4] = f3;
                    fArray5[5] = f2;
                    fArray5[6] = f3;
                    fArray = fArray5;
                    fArray5[7] = f4;
                }
            }
            return fArray;
        }
    }
}


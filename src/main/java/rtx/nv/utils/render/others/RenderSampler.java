package rtx.nv.utils.render.others;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import java.util.OptionalDouble;
import net.minecraft.client.gl.GpuSampler;

public final class RenderSampler {
    private static GpuSampler linear;
    private static GpuSampler nearest;
    private static GpuSampler linearRepeat;

    private RenderSampler() {
    }

    public static GpuSampler nearest() {
        if (nearest == null) {
            nearest = RenderSystem.getDevice().createSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.NEAREST, FilterMode.NEAREST, 1, OptionalDouble.empty());
        }
        return nearest;
    }

    public static GpuSampler linear() {
        if (linear == null) {
            linear = RenderSystem.getDevice().createSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, 1, OptionalDouble.empty());
        }
        return linear;
    }

    public static GpuSampler linearRepeat() {
        if (linearRepeat == null) {
            linearRepeat = RenderSystem.getDevice().createSampler(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.LINEAR, FilterMode.LINEAR, 1, OptionalDouble.empty());
        }
        return linearRepeat;
    }
}


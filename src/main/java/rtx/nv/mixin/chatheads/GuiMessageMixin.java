package rtx.nv.mixin.chatheads;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import rtx.nv.api.mods.chatheads.HeadData;
import rtx.nv.api.mods.chatheads.mixininterface.HeadRenderable;

@Mixin(net.minecraft.client.gui.hud.ChatHudLine.Visible.class)

public abstract class GuiMessageMixin
implements HeadRenderable {
    @Unique
    @NotNull
    public HeadData chatheads_headData = HeadData.EMPTY;

    @Override
    public void chatheads_setHeadData(@NotNull HeadData headData) {
        this.chatheads_headData = headData;
    }

    @Override
    @NotNull
    public HeadData chatheads_getHeadData() {
        return this.chatheads_headData;
    }
}


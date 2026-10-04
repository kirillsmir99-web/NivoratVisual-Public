package rtx.nv.mixin.chatheads;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.mods.chatheads.ChatHeads;
import rtx.nv.api.mods.chatheads.HeadData;
import rtx.nv.api.mods.chatheads.mixininterface.HeadRenderable;

@Mixin(net.minecraft.client.gui.hud.ChatHudLine.class)

public abstract class GuiMessageLineMixin
implements HeadRenderable {
    @Unique
    @NotNull
    public HeadData chatheads_headData = HeadData.EMPTY;

    @Inject(method="<init>", at={@At(value="TAIL")}, require = 0)
    public void chatheads_setOwnerForFirstLine(CallbackInfo callbackInfo) {
        this.chatheads_headData = ChatHeads.getLineData();
        ChatHeads.setLineData(HeadData.EMPTY);
    }

    @Override
    @NotNull
    public HeadData chatheads_getHeadData() {
        return this.chatheads_headData;
    }
}


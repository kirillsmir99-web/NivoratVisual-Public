package rtx.nv.mixin.chatheads;

import java.util.List;
import net.minecraft.client.resource.server.ReloadScheduler;
import net.minecraft.client.resource.server.ServerResourcePackLoader;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourceType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rtx.nv.api.mods.chatheads.ChatHeads;

@Mixin(ServerResourcePackLoader.class)
public abstract class DownloadedPackSourceMixin {
    @Inject(method="toProfiles", at={@At(value="RETURN")}, require = 1)
    public void chatheads_checkForDisableResource(List<ReloadScheduler.PackInfo> list, CallbackInfoReturnable<List<ResourcePackProfile>> cir) {
        List<ResourcePackProfile> packs = cir.getReturnValue();
        if (packs == null) {
            return;
        }
        ChatHeads.serverDisabledChatHeads = false;
        for (ResourcePackProfile serverPack : packs) {
            ResourcePack resources = serverPack.createResourcePack();
            try {
                if (resources.open(ResourceType.CLIENT_RESOURCES, ChatHeads.DISABLE_RESOURCE) == null) continue;
                ChatHeads.serverDisabledChatHeads = true;
                ChatHeads.LOGGER.info("Chat Heads disabled by server request");
            }
            finally {
                if (resources != null) {
                    resources.close();
                }
            }
        }
    }
}

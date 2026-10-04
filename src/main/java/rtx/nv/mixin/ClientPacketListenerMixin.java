package rtx.nv.mixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.events.EventBus;
import rtx.nv.api.events.impl.player.TotemPopEvent;

@Mixin(net.minecraft.client.network.ClientPlayNetworkHandler.class)

public abstract class ClientPacketListenerMixin {
    @Inject(method="onEntityStatus", at={@At(value="HEAD")}, require = 0)
    private void nv_onTotemPop(EntityStatusS2CPacket packet, CallbackInfo ci) {
        if (packet.getStatus() != 35) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) {
            return;
        }
        Entity entity = packet.getEntity((World)mc.world);
        if (!(entity instanceof LivingEntity)) {
            return;
        }
        LivingEntity living = (LivingEntity)entity;
        ItemStack totem = this.nv_findTotem(living);
        boolean enchanted = !totem.isEmpty() && totem.hasEnchantments();
        EventBus.get().post(new TotemPopEvent(living, enchanted));
    }

    @Unique
    private ItemStack nv_findTotem(LivingEntity entity) {
        for (Hand hand : Hand.values()) {
            ItemStack stack = entity.getStackInHand(hand);
            if (!stack.contains(DataComponentTypes.DEATH_PROTECTION) && !stack.isOf(Items.TOTEM_OF_UNDYING)) continue;
            return stack;
        }
        return ItemStack.EMPTY;
    }
}


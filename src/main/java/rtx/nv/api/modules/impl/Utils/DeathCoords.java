package rtx.nv.api.modules.impl.Utils;
import rtx.nv.api.events.EventHandler;
import java.util.Locale;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import rtx.nv.api.events.impl.network.PacketEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;

public final class DeathCoords
extends Module {
    public DeathCoords() {
        super("Death Coords", "\u0412\u044b\u0432\u043e\u0434\u0438\u0442 \u043a\u043e\u043e\u0440\u0434\u0438\u043d\u0430\u0442\u044b \u0441\u043c\u0435\u0440\u0442\u0438 \u0432 \u0447\u0430\u0442.", Category.UTILS);
    }

    @EventHandler
    private void onPacket(PacketEvent packetEvent) {
        if (!packetEvent.isReceive() || this.mc.player == null || this.mc.world == null) {
            return;
        }
        if (!(packetEvent.getPacket() instanceof DeathMessageS2CPacket)) {
            return;
        }
        String string = String.format(Locale.ROOT, "%.0f %.0f %.0f", this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
        MutableText mutableText = Text.literal((String)string).styled(style -> style.withClickEvent((ClickEvent)new ClickEvent.CopyToClipboard(string)));
        this.mc.player.sendMessage((Text)mutableText, false);
    }
}


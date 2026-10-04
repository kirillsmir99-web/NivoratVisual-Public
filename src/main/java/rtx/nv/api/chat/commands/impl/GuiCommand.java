package rtx.nv.api.chat.commands.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import rtx.nv.api.chat.commands.Command;
import rtx.nv.api.ui.UI;
import rtx.nv.utils.sounds.Sounds;

import java.util.Arrays;
import java.util.List;

public final class GuiCommand extends Command {
    public GuiCommand() {
        super("gui", "Opens client click menu", "menu", "clickgui");
    }

    @Override
    public void execute(String string, String[] stringArray) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        mc.send(() -> {
            mc.setScreen((Screen) UI.INSTANCE);
            Sounds.play("gui_open");
        });
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList("Opens client click menu.", "Usage:", "> gui");
    }
}

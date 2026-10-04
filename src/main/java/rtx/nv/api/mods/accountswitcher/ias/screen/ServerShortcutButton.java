package rtx.nv.api.mods.accountswitcher.ias.screen;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.util.Identifier;

public class ServerShortcutButton extends ButtonWidget {
    public ServerShortcutButton(int x, int y, Supplier<Identifier> textureSupplier, String name, Consumer<ButtonWidget> onPress) {
        super(x, y, 20, 20, net.minecraft.text.Text.literal(name), onPress::accept, DEFAULT_NARRATION_SUPPLIER);
    }

    public void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
    }
}

package rtx.nv.api.mods.accountswitcher.ias.screen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public final class AccountScreen extends Screen {
    private final Screen parent;
    private final List<ServerShortcut> serverShortcuts = new ArrayList<>();
    private TextFieldWidget search;
    private AccountList list;
    private ButtonWidget login;
    private ButtonWidget offlineLogin;
    private ButtonWidget edit;
    private ButtonWidget delete;

    public AccountScreen(Screen parent) {
        super(Text.literal("Account Switcher"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> {
            if (this.client != null) {
                this.client.setScreen(this.parent);
            }
        }).dimensions(this.width / 2 - 100, this.height - 28, 200, 20).build());
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
    }

    public static record ServerShortcut(String name, String address) {}

    public static final class AccountList {
        // Stub for list widget
    }
}

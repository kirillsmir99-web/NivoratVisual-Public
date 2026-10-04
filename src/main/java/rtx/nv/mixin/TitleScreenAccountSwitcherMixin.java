package rtx.nv.mixin;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PressableTextWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.mods.accountswitcher.ias.screen.AccountScreen;
import rtx.nv.mixin.ScreenAccessor;

@Mixin(TitleScreen.class)
public abstract class TitleScreenAccountSwitcherMixin extends Screen {
    protected TitleScreenAccountSwitcherMixin(Text title) {
        super(title);
    }

    @Inject(method="init", at={@At(value="TAIL")}, require = 0)
    private void nv_replaceRealmsWithAccountSwitcher(CallbackInfo ci) {
        TitleScreen screen = (TitleScreen)(Object)this;
        ScreenAccessor access = (ScreenAccessor)(Object)this;
        List<Drawable> drawables = access.getRenderables();
        List<Element> children = access.getChildren();
        List<Selectable> selectables = access.getNarratables();
        String realmsLabel = Text.translatable("menu.online").getString();
        ButtonWidget realmsButton = null;
        for (Element child : children) {
            if (child instanceof ButtonWidget button && realmsLabel.equals(button.getMessage().getString())) {
                realmsButton = button;
                break;
            }
        }
        int x = this.width / 2 - 100;
        int y = this.height / 4 + 96;
        int width = 200;
        int height = 20;
        if (realmsButton != null) {
            x = realmsButton.getX();
            y = realmsButton.getY();
            width = realmsButton.getWidth();
            height = realmsButton.getHeight();
            drawables.remove(realmsButton);
            children.remove(realmsButton);
            selectables.remove(realmsButton);
        }
        ButtonWidget switcher = ButtonWidget.builder(Text.literal("Account Switcher"), b -> MinecraftClient.getInstance().setScreen(new AccountScreen(screen))).dimensions(x, y, width, height).build();
        this.addDrawableChild(switcher);
        for (Element child : new ArrayList<Element>(children)) {
            if (child instanceof PressableTextWidget copyright) {
                drawables.remove(copyright);
                children.remove(copyright);
                selectables.remove(copyright);
            }
        }
    }
}

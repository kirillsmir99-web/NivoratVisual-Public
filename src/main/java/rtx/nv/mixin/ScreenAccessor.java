package rtx.nv.mixin;

import java.util.List;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Screen.class)
public interface ScreenAccessor {
    @Accessor("children")
    public List<Element> getChildren();

    @Accessor("selectables")
    public List<Selectable> getNarratables();

    @Accessor("drawables")
    public List<Drawable> getRenderables();
}

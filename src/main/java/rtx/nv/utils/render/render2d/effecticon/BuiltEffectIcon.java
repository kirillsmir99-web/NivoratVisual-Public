package rtx.nv.utils.render.render2d.effecticon;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;

public class BuiltEffectIcon {
    public final RegistryEntry<StatusEffect> effect;
    public final float x;
    public final float y;
    public final float size;
    public final int color;

    public BuiltEffectIcon(RegistryEntry<StatusEffect> effect, float x, float y, float size, int color) {
        this.effect = effect;
        this.x = x;
        this.y = y;
        this.size = size;
        this.color = color;
    }

    public BuiltEffectIcon(RegistryEntry<StatusEffect> effect, float x, float y, float size) {
        this(effect, x, y, size, -1);
    }

    public BuiltEffectIcon(StatusEffectInstance instance, float x, float y, float size, int color) {
        this(instance == null ? null : instance.getEffectType(), x, y, size, color);
    }

    public BuiltEffectIcon(StatusEffectInstance instance, float x, float y, float size) {
        this(instance == null ? null : instance.getEffectType(), x, y, size, -1);
    }

    public void render(DrawContext drawContext) {
        EffectIconRenderer.getInstance().draw(drawContext, this);
    }
}
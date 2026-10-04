package rtx.nv.test;

import rtx.nv.api.ui.theme.*;
import rtx.nv.api.modules.impl.Utils.ClientSounds;
import rtx.nv.api.modules.impl.Visuals.ViewModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;

/** Regressions for user-visible failures: persisted materials, mute and hand transforms. */
final class UxChecks {
    static void verify() {
        ThemeDraft draft = new ThemeDraft();
        draft.setName("NV regression"); draft.setBlur(7); draft.setGlow(.42f);
        draft.setRefraction(.23f); draft.setEdgeStrength(.31f); draft.setGradientAngle(135);
        draft.setActiveAlpha(117); draft.setColorMovement(true);
        CustomTheme live = (CustomTheme) draft.toLiveTheme();
        CustomTheme restored = CustomTheme.fromJson(live.toJson());
        ThemeDraft loaded = new ThemeDraft(); loaded.loadFromTheme(restored);
        if (loaded.getBlur() != 7 || loaded.getGlow() != .42f || loaded.getRefraction() != .23f
            || loaded.getEdgeStrength() != .31f || loaded.getGradientAngle() != 135
            || restored.paletteAlpha()[0] != 117 || !loaded.isColorMovement())
            throw new AssertionError("Theme material/opacity roundtrip failed");
        String name = loaded.getName(); loaded.setName("changed");
        if (!loaded.isDirty()) throw new AssertionError("Unsaved change not detected");
        loaded.setName(name);
        if (loaded.isDirty()) throw new AssertionError("Reverted change still dirty");
        var sounds = ClientSounds.getInstance();
        float previous = sounds.interfaceVolume.getValue();
        try {
            sounds.interfaceVolume.setValue(0);
            if (sounds.getInterfaceVolume() != 0) throw new AssertionError("UI mute ignored");
            if (!net.minecraft.registry.Registries.SOUND_EVENT.containsId(rtx.nv.utils.sounds.SoundManager.NV_GLASS_BUTTON.id()))
                throw new AssertionError("NV sound bank unregistered");
        } finally { sounds.interfaceVolume.setValue(previous); }
        rtx.nv.NV.LOGGER.info("[NV-TEST] Theme material persistence, reverted dirty state and UI mute passed");
    }

    static void verifyHands() {
        var view = ViewModel.getInstance();
        boolean enabled = view.isEnabled();
        float size = view.mainSize.getValue();
        try {
            view.enable(); view.mainSize.setValue(1.5f);
            // Wait-free check at the animation endpoint, with the same public render callback.
            var anim = ViewModel.class.getDeclaredField("mainScaleAnim"); anim.setAccessible(true);
            ((rtx.nv.utils.animations.SmoothAnimation) anim.get(view)).set(1.5);
            var matrices = new MatrixStack();
            if (!ViewModel.applyScale(matrices, Hand.MAIN_HAND) || Math.abs(matrices.peek().getPositionMatrix().m00() - 1.5f) > .001)
                throw new AssertionError("ViewModel scale did not change render matrix");
            view.disable(); matrices = new MatrixStack();
            if (ViewModel.applyScale(matrices, Hand.MAIN_HAND) || matrices.peek().getPositionMatrix().m00() != 1)
                throw new AssertionError("Disabled ViewModel changed render matrix");
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        finally { view.mainSize.setValue(size); if (enabled) view.enable(); else view.disable(); }
        rtx.nv.NV.LOGGER.info("[NV-TEST] ViewModel setting changed hand matrix; disabled callback preserved vanilla matrix");
    }
}

package rtx.nv.test;

import rtx.nv.utils.render.render2d.msdf.MsdfFonts;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Visuals.Crown;
import rtx.nv.api.modules.impl.Utils.ClientSounds;
import rtx.nv.utils.sounds.SoundManager;

/** Checks asset coverage and migration at the actual client resource boundary. */
final class IdentityChecks {
    static void verify() {
        for (String alias : new String[]{"montserrat-regular", "montserrat-medium", "montserrat-semibold", "montserrat-bold", "sf", "sf-medium", "sf-bold"}) {
            if (!MsdfFonts.hasGlyph(alias, "Ёё Корона Перезарядка NV 0123456789"))
                throw new AssertionError("Missing Russian glyph in " + alias);
        }
        for (int cp = 0xE001; cp <= 0xE02F; cp++) {
            if (!MsdfFonts.hasGlyph("nv", cp)) throw new AssertionError("Missing NV icon " + cp);
        }
        if (!MsdfFonts.hasGlyph("heart", "A") || !MsdfFonts.hasGlyph("event-icons", "i\ue104\ue105\ue106"))
            throw new AssertionError("Legacy icon call sites lost coverage");
        if (ModuleManager.get().findByName("China Hat") != ModuleManager.get().get(Crown.class))
            throw new AssertionError("Old crown name lost migration");
        var profile = ClientSounds.getInstance();
        var choice = (rtx.nv.api.modules.settings.impl.ModeSetting) profile.getSettings().all().stream()
                .filter(s -> s.getName().equals("Тема звуков")).findFirst().orElseThrow();
        String previous = choice.getSelected();
        try {
            choice.selected("NV · Стекло");
            if (profile.resolveSoundEvent("pin", SoundManager.SERENE_PIN) != SoundManager.NV_GLASS_PIN
                    || profile.resolveSoundEvent("notification", SoundManager.NOTIFICATION) != SoundManager.NV_GLASS_NOTIFY)
                throw new AssertionError("NV sound profile is inconsistent");
            choice.selected("Serene");
            if (profile.resolveSoundEvent("notification", SoundManager.NOTIFICATION) != SoundManager.NOTIFICATION)
                throw new AssertionError("Alternate sound profile was overridden");
        } finally { choice.selected(previous); }
        rtx.nv.NV.LOGGER.info("[NV-TEST] Identity: Cyrillic in seven font aliases, 47 NV icons, legacy glyphs, crown migration and default sound routing passed");
    }
}

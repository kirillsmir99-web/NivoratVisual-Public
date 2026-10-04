package rtx.nv.test;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.PigEntity;
import rtx.nv.api.drags.*;
import rtx.nv.api.events.EventBus;
import rtx.nv.api.events.impl.player.JumpEvent;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Visuals.*;
import rtx.nv.api.modules.impl.Visuals.killeffect.*;
import rtx.nv.api.modules.settings.impl.*;
import rtx.nv.utils.render.post.jumpdistort.JumpDistortRenderer;
import rtx.nv.utils.render.post.killdistortion.KillDistortionRenderer;
import rtx.nv.utils.render.post.saturation.Saturation2D;
import rtx.nv.utils.render.post.scarglass.ScarGlassRenderer;

final class PostEffectChecks {
    static void verify(ClientGameTestContext context) {
        context.runOnClient(client -> {
            DragController drag = new DragController(20, 20);
            if (!drag.tryGrab(25, 25, 80, 30)) throw new AssertionError("HUD grab failed");
            drag.tick(-10000, 10000, 80, 30, true);
            if (drag.getTargetX() < 5 || drag.getTargetY() > Position.screenHeight()-35) throw new AssertionError("HUD escaped screen");
            drag.tick((Position.screenWidth()-80)*.5f+7, 50, 80, 30, true);
            if (Math.abs(drag.getTargetX()-(Position.screenWidth()-80)*.5f) > .01f) throw new AssertionError("HUD center snap failed");
            drag.cancel();
            if (drag.getTargetX()!=20 || drag.getTargetY()!=20) throw new AssertionError("HUD cancel lost initial position");
            client.player.setYaw(0); client.player.setPitch(12);
            JumpCircle jump = JumpCircle.getInstance();
            jump.getSettings().all().stream().filter(s -> s.getName().equals("Стиль")).forEach(s -> ((SelectSetting)s).selected("Ripple"));
            jump.enable();
            EventBus.get().post(new JumpEvent(client.player));
        });
        long jumpBefore = JumpDistortRenderer.renderedPasses();
        context.waitTicks(5);
        context.takeScreenshot("jump-ripple");
        context.runOnClient(client -> {
            if (JumpDistortRenderer.isDisabledAfterError() || JumpDistortRenderer.renderedPasses()<=jumpBefore) throw new AssertionError("Jump ripple produced no shader pass");
            JumpCircle.getInstance().disable();
            long before = Saturation2D.renderedPasses();
            Saturation2D.applyWithCopy(0);
            if (Saturation2D.isDisabledAfterError() || Saturation2D.renderedPasses()<=before) throw new AssertionError("Saturation shader produced no pass");
        });
        context.runOnClient(client -> {
            Trails trail = ModuleManager.get().get(Trails.class);
            trail.getSettings().all().stream().filter(s -> s.getName().equals("Режим")).forEach(s -> ((SelectSetting)s).selected("Лента"));
            trail.enable();
            var pos = client.player.getEntityPos().add(0,0,3);
            var point = new Trails.TailPoint(pos, System.currentTimeMillis());
            try {
                var field = Trails.class.getDeclaredField("points"); field.setAccessible(true);
                @SuppressWarnings("unchecked") var list = (java.util.List<Trails.TailPoint>)field.get(trail);
                list.add(point);
            } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
        });
        context.waitTicks(5);
        context.takeScreenshot("trail-ribbon");
        context.runOnClient(client -> {
            ModuleManager.get().get(Trails.class).disable();
        });
        long scanBefore = KillEffectScanRenderer.renderedPasses(), distortionBefore = KillDistortionRenderer.renderedPasses();
        PigEntity pig = context.computeOnClient(client -> {
            var pos = client.player.getEntityPos().add(0,0,3);
            var entity = new PigEntity(EntityType.PIG,client.world);
            entity.setPosition(pos); entity.age=3;
            var memory = new KillEffectDeathMemoryTracker.DeathMemory(entity,2500);
            entity.remove(Entity.RemovalReason.DISCARDED);
            if (memory.shouldTrigger(client)) throw new AssertionError("Despawn mistaken for kill");
            entity.setHealth(0);
            if (!memory.shouldTrigger(client)) throw new AssertionError("Death missed");
            KillEffect effect = KillEffect.getInstance();
            effect.getSettings().all().stream().filter(s -> s instanceof ModeSetting && s.getName().equals("Режим")).forEach(s -> ((ModeSetting)s).selected(KillEffect.MODE_SCAN));
            effect.getSettings().all().stream().filter(s -> s.getName().equals("Искажение")).forEach(s -> ((BooleanSetting)s).setValue(true));
            effect.enable();
            KillEffect.notifyEntityDied(entity,client.player.getDamageSources().playerAttack(client.player));
            return entity;
        });
        context.waitTicks(5);
        context.takeScreenshot("kill-scan-distortion");
        context.runOnClient(client -> {
            if (KillEffectScanRenderer.isDisabledAfterError() || KillEffectScanRenderer.renderedPasses()<=scanBefore ||
                KillDistortionRenderer.isDisabledAfterError() || KillDistortionRenderer.renderedPasses()<=distortionBefore)
                throw new AssertionError("Kill notification produced no scan/distortion shader pass");
            KillEffect.getInstance().disable();
            rtx.nv.NV.LOGGER.info("[NV-TEST] HUD clamp/snap/cancel, jump ripple, saturation, trail shards/ribbons, kill scan/distortion and despawn distinction passed");
        });
    }
}

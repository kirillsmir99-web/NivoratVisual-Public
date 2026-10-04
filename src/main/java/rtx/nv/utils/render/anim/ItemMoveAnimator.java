package rtx.nv.utils.render.anim;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import rtx.nv.api.modules.impl.Visuals.BetterMinecraft;

public final class ItemMoveAnimator {
    private static final ItemMoveAnimator INSTANCE = new ItemMoveAnimator();

    public static ItemMoveAnimator getInstance() {
        return INSTANCE;
    }

    private static class AnimEntry {
        final float startX;
        final float startY;
        final long startTime;
        final long duration;

        AnimEntry(float startX, float startY, long startTime, long duration) {
            this.startX = startX;
            this.startY = startY;
            this.startTime = startTime;
            this.duration = duration;
        }
    }

    private static final Map<Integer, AnimEntry> activeAnimations = new ConcurrentHashMap<>();
    private static final Map<Integer, ItemStack> previousStacks = new ConcurrentHashMap<>();
    private static int lastHandlerSyncId = -1;
    private static long lastEmptiedTime = 0L;
    private static int lastEmptiedSlotX = 0;
    private static int lastEmptiedSlotY = 0;

    public static void beginFrame(ScreenHandler handler, int mouseX, int mouseY, int screenX, int screenY) {
        if (handler == null) {
            activeAnimations.clear();
            previousStacks.clear();
            lastHandlerSyncId = -1;
            return;
        }

        if (handler.syncId != lastHandlerSyncId) {
            lastHandlerSyncId = handler.syncId;
            activeAnimations.clear();
            previousStacks.clear();
            for (Slot slot : handler.slots) {
                previousStacks.put(slot.id, slot.getStack().copy());
            }
            return;
        }

        long now = System.currentTimeMillis();
        long duration = BetterMinecraft.itemMoveAnimDurationMs();

        for (Slot slot : handler.slots) {
            ItemStack current = slot.getStack();
            ItemStack prev = previousStacks.get(slot.id);

            if (prev != null) {
                if (!prev.isEmpty() && current.isEmpty()) {
                    lastEmptiedTime = now;
                    lastEmptiedSlotX = slot.x;
                    lastEmptiedSlotY = slot.y;
                } else if (prev.isEmpty() && !current.isEmpty()) {
                    float srcX;
                    float srcY;
                    if (now - lastEmptiedTime < 80L) {
                        srcX = (float)(lastEmptiedSlotX - slot.x);
                        srcY = (float)(lastEmptiedSlotY - slot.y);
                    } else {
                        srcX = (float)(mouseX - screenX - slot.x - 8);
                        srcY = (float)(mouseY - screenY - slot.y - 8);
                    }

                    if (Math.abs(srcX) > 2.0f || Math.abs(srcY) > 2.0f) {
                        float maxDist = BetterMinecraft.isReducedIntensity() ? 70.0f : 140.0f;
                        srcX = Math.max(-maxDist, Math.min(maxDist, srcX));
                        srcY = Math.max(-maxDist, Math.min(maxDist, srcY));
                        activeAnimations.put(slot.id, new AnimEntry(srcX, srcY, now, duration));
                    }
                }
            }

            previousStacks.put(slot.id, current.copy());
        }

        activeAnimations.entrySet().removeIf(entry -> now - entry.getValue().startTime >= entry.getValue().duration);
    }

    public static float[] offset(Slot slot) {
        if (slot == null || activeAnimations.isEmpty()) {
            return null;
        }
        AnimEntry anim = activeAnimations.get(slot.id);
        if (anim == null) {
            return null;
        }
        long elapsed = System.currentTimeMillis() - anim.startTime;
        if (elapsed >= anim.duration) {
            activeAnimations.remove(slot.id);
            return null;
        }
        float progress = Math.min(1.0f, (float) elapsed / (float) anim.duration);
        float t = 1.0f - progress;
        float ease = 1.0f - t * t * t;

        float curX = anim.startX * (1.0f - ease);
        float curY = anim.startY * (1.0f - ease);
        return new float[] { curX, curY, 1.0f };
    }

    public boolean isActive() {
        return !activeAnimations.isEmpty();
    }
}
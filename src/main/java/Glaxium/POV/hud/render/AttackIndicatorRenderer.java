package Glaxium.POV.hud.render;

import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.hud.RecordedHudData;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.AttackIndicator;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

/**
 * Vanilla attack-cooldown fill under the crosshair. Derived from recorded
 * cooldown (or slot / offhand / swing) — no extra editor keyframes.
 */
public final class AttackIndicatorRenderer
{
    private AttackIndicatorRenderer()
    {
    }

    public static boolean shouldDraw()
    {
        AttackIndicator mode = MinecraftClient.getInstance().options.getAttackIndicator().getValue();
        return mode == AttackIndicator.CROSSHAIR;
    }

    public static float progress(
        ReplayKeyframes replay,
        RecordedHudData hud,
        RecordedHandData hand,
        float tick)
    {
        if (hud != null && !hud.attackCooldown.isEmpty())
        {
            return sampleCooldown(hud.attackCooldown, tick);
        }

        return reconstruct(replay, hand, tick);
    }

    public static float sampleCooldown(mchorse.bbs_mod.utils.keyframes.KeyframeChannel<Float> channel, float tick)
    {
        if (channel == null || channel.isEmpty())
        {
            return 1F;
        }

        var keyframes = channel.getKeyframes();
        int size = keyframes.size();
        if (tick < keyframes.get(0).getTick())
        {
            return 1F;
        }
        if (tick >= keyframes.get(size - 1).getTick())
        {
            return MathHelper.clamp(keyframes.get(size - 1).getValue(), 0F, 1F);
        }

        int prevIdx = 0;
        for (int i = 0; i < size; i++)
        {
            if (keyframes.get(i).getTick() <= tick)
            {
                prevIdx = i;
            }
            else
            {
                break;
            }
        }

        var prev = keyframes.get(prevIdx);
        var next = keyframes.get(prevIdx + 1);
        float v0 = prev.getValue();
        float v1 = next.getValue();

        if (v0 >= 0.999F && v1 < v0)
        {
            return 1F;
        }
        if (v1 < v0)
        {
            return 1F;
        }

        float t0 = prev.getTick();
        float t1 = next.getTick();
        if (t1 <= t0)
        {
            return MathHelper.clamp(v1, 0F, 1F);
        }

        float factor = (tick - t0) / (t1 - t0);
        float value = v0 + (v1 - v0) * factor;
        return MathHelper.clamp(value, 0F, 1F);
    }

    public static float liveProgress()
    {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
        {
            return 1F;
        }

        return MathHelper.clamp(client.player.getAttackCooldownProgress(0F), 0F, 1F);
    }

    private static float reconstruct(ReplayKeyframes replay, RecordedHandData hand, float tick)
    {
        if (replay == null || tick < 0F)
        {
            return 1F;
        }

        int slot = MathHelper.clamp(replay.getSelectedSlot(tick), 0, ReplayKeyframes.HOTBAR_SIZE - 1);
        ItemStack main = replay.hotbar.get(slot).interpolate(tick, ItemStack.EMPTY);
        float period = cooldownPeriod(main);
        int lookback = Math.max(2, (int) Math.ceil(period) + 1);

        for (int i = 0; i <= lookback; i++)
        {
            float thenTick = tick - i;
            if (thenTick < 0F)
            {
                return 1F;
            }

            if (resetAt(replay, hand, thenTick))
            {
                return MathHelper.clamp(i / period, 0F, 1F);
            }
        }

        return 1F;
    }

    private static boolean resetAt(ReplayKeyframes replay, RecordedHandData hand, float tick)
    {
        float previousTick = tick - 1F;
        if (previousTick < 0F)
        {
            return false;
        }

        int slot = MathHelper.clamp(replay.getSelectedSlot(tick), 0, ReplayKeyframes.HOTBAR_SIZE - 1);
        int previousSlot = MathHelper.clamp(replay.getSelectedSlot(previousTick), 0, ReplayKeyframes.HOTBAR_SIZE - 1);
        if (slot != previousSlot)
        {
            return true;
        }

        ItemStack main = replay.hotbar.get(slot).interpolate(tick, ItemStack.EMPTY);
        ItemStack previousMain = replay.hotbar.get(previousSlot).interpolate(previousTick, ItemStack.EMPTY);
        if (!ItemStack.areItemsEqual(main, previousMain))
        {
            return true;
        }

        ItemStack off = replay.offHand.interpolate(tick, ItemStack.EMPTY);
        ItemStack previousOff = replay.offHand.interpolate(previousTick, ItemStack.EMPTY);
        if (!ItemStack.areItemsEqual(off, previousOff))
        {
            return true;
        }

        return false;
    }

    private static float cooldownPeriod(ItemStack stack)
    {
        double base = 4.0D;
        double addition = 0.0D;
        double multiplyBase = 1.0D;
        double multiplyTotal = 1.0D;

        if (stack != null && !stack.isEmpty())
        {
            for (EntityAttributeModifier modifier : stack.getAttributeModifiers(EquipmentSlot.MAINHAND)
                .get(EntityAttributes.GENERIC_ATTACK_SPEED))
            {
                switch (modifier.getOperation())
                {
                    case ADDITION -> addition += modifier.getValue();
                    case MULTIPLY_BASE -> multiplyBase += modifier.getValue();
                    case MULTIPLY_TOTAL -> multiplyTotal *= (1.0D + modifier.getValue());
                }
            }
        }

        double speed = Math.max(0.0001D, (base + addition) * multiplyBase * multiplyTotal);
        return (float) (1.0D / speed * 20.0D);
    }
}

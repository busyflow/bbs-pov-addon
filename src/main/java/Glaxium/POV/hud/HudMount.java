package Glaxium.POV.hud;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

/** Vanilla replaces the hunger bar with mount hearts while riding a living vehicle. */
public final class HudMount
{
    private HudMount()
    {
    }

    public static LivingEntity jumpingMount(PlayerEntity player)
    {
        if (player == null)
        {
            return null;
        }

        return player.getVehicle() instanceof LivingEntity living ? living : null;
    }

    public static int heartSlots(LivingEntity mount)
    {
        if (mount == null || mount.isRemoved())
        {
            return 0;
        }

        int slots = ((int) (mount.getMaxHealth() + 0.5F)) / 2;
        return Math.max(0, Math.min(30, slots));
    }
}

package Glaxium.POV.hand.recording;

import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.integration.access.minecraft.HeldItemRendererPovAccess;
import mchorse.bbs_mod.forms.forms.Form;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;

/** Writes hand channels. Recording-only clock state is not serialized. */
public final class HandRecorder
{
    private int lastRecordedTick = Integer.MIN_VALUE;
    /** A rejected food/drink right click can still start a client hand swing
     * even though LivingEntity never enters its active-use state. Keep that
     * rejected swing out of POV playback until the swing itself has ended. */
    private boolean suppressRejectedConsumeSwing;
    private int spyglassCooldown = 0;

    public void reset()
    {
        this.lastRecordedTick = Integer.MIN_VALUE;
        this.suppressRejectedConsumeSwing = false;
        this.spyglassCooldown = 0;
    }

    public boolean hasRecorded()
    {
        return this.lastRecordedTick != Integer.MIN_VALUE;
    }

    public void record(RecordedHandData data, int tick, ClientPlayerEntity player, Form recordingForm)
    {
        /* The Form argument is intentionally ignored. Model, texture and Pose are
         * independent POV-editor decisions, not actor recording data. */
        MinecraftClient client = MinecraftClient.getInstance();
        boolean using = player.isUsingItem();
        boolean rejectedConsume = !using
            && client.options.useKey.isPressed()
            && (hasConsumeAction(player.getMainHandStack())
                || hasConsumeAction(player.getOffHandStack()));

        if (using)
        {
            this.suppressRejectedConsumeSwing = false;
        }
        else if (rejectedConsume)
        {
            this.suppressRejectedConsumeSwing = true;
        }
        else if (!player.handSwinging)
        {
            this.suppressRejectedConsumeSwing = false;
        }

        boolean usingSpyglass = using && (player.getActiveItem().isOf(net.minecraft.item.Items.SPYGLASS) || player.isUsingSpyglass());
        if (usingSpyglass)
        {
            this.spyglassCooldown = 4;
        }
        else if (this.spyglassCooldown > 0)
        {
            this.spyglassCooldown--;
        }

        float rawSwing = this.suppressRejectedConsumeSwing
            ? 0F
            : clamp(player.getHandSwingProgress(1.0F), 0F, 1F);
        boolean isOffHand = player.preferredHand == Hand.OFF_HAND;
        boolean isLeftArm = (player.getMainArm() == net.minecraft.util.Arm.LEFT && !isOffHand)
            || (player.getMainArm() == net.minecraft.util.Arm.RIGHT && isOffHand);

        float rightSwing = player.handSwinging && !isLeftArm ? rawSwing : 0F;
        float leftSwing = player.handSwinging && isLeftArm ? rawSwing : 0F;

        data.rightSwingProgress.insert(tick, rightSwing);
        data.leftSwingProgress.insert(tick, leftSwing);
        HeldItemRendererPovAccess held = (HeldItemRendererPovAccess)
            client.gameRenderer.firstPersonRenderer;

        float mainEquip = clamp(1F - held.bbsPov$getEquipProgressMainHand(), 0F, 1F);
        float offEquip = clamp(1F - held.bbsPov$getEquipProgressOffHand(), 0F, 1F);

        if (usingSpyglass || (this.spyglassCooldown > 0 && player.getMainHandStack().isOf(net.minecraft.item.Items.SPYGLASS)))
        {
            mainEquip = 0F;
        }
        if (usingSpyglass || (this.spyglassCooldown > 0 && player.getOffHandStack().isOf(net.minecraft.item.Items.SPYGLASS)))
        {
            offEquip = 0F;
        }

        data.mainEquipProgress.insert(tick, mainEquip);
        data.offEquipProgress.insert(tick, offEquip);
        data.activeHand.insert(tick, using
            ? player.getActiveHand() == Hand.OFF_HAND ? 2 : 1
            : 0);
        data.activeItem.insert(tick, using ? player.getActiveItem().copy() : ItemStack.EMPTY);
        data.showUseParticles.insert(tick, using);
        data.useTime.insert(tick, Math.max(0, player.getItemUseTime()));
        /* These are the exact two values consumed by GameRenderer.bobView(). */
        data.bobPhase.insert(tick, player.horizontalSpeed);
        data.bobStrength.insert(tick, Math.max(0F, player.strideDistance));
        data.renderYaw.insert(tick, player.renderYaw);
        data.renderPitch.insert(tick, player.renderPitch);
        this.lastRecordedTick = tick;
    }

    private static boolean hasConsumeAction(ItemStack stack)
    {
        UseAction action = stack.getUseAction();

        return action == UseAction.EAT || action == UseAction.DRINK;
    }

    private static float clamp(float value, float min, float max)
    {
        return Math.max(min, Math.min(max, value));
    }
}

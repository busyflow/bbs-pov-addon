package Glaxium.POV.hand.playback;

import Glaxium.POV.hand.HandState;
import Glaxium.POV.hand.RecordedHandData;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;

/** Samples hand channels into a frame-local {@link HandState}. */
public final class HandSampler
{
    private HandSampler()
    {
    }

    public static HandState sample(RecordedHandData data, ReplayKeyframes replay, float tick)
    {
        HandState state = new HandState();
        Pose basePose = data.getBasePose();
        Pose defaultPose = basePose != null ? basePose.copy() : new Pose();

        state.visible = data.visible.interpolate(tick, true);
        state.model = data.model.interpolate(tick, null);
        /* Null intentionally means "use the selected model's own texture." */
        state.texture = data.texture.interpolate(tick, null);
        state.color = data.color.isEmpty() ? null : data.color.interpolate(tick, null);
        state.colorOverlay = data.colorOverlay.isEmpty() ? null : data.colorOverlay.interpolate(tick, null);
        state.cameraOffset.copy(data.cameraOffset.interpolate(tick, new Transform()));
        state.pose.copy(data.pose.interpolate(tick, defaultPose));
        state.itemPose.copy(data.itemPose.interpolate(tick, defaultPose));
        state.rightHandVisible = data.rightHandVisible.interpolate(tick, true);
        state.leftHandVisible = data.leftHandVisible.interpolate(tick, false);

        PoseTransform defaultRight = basePose != null && basePose.get("right_arm") != null
            ? copyPoseTransform(basePose.get("right_arm"))
            : new PoseTransform();
        PoseTransform defaultLeft = basePose != null && basePose.get("left_arm") != null
            ? copyPoseTransform(basePose.get("left_arm"))
            : new PoseTransform();

        state.rightPose.copy(data.rightPose.interpolate(tick, defaultRight));
        state.leftPose.copy(data.leftPose.interpolate(tick, defaultLeft));
        state.mainHand = copyItem(replay.getMainHandStack(tick));
        state.offHand = copyItem(replay.offHand.interpolate(tick, ItemStack.EMPTY));
        state.rightSwingProgress = sampleSwingProgress(data.rightSwingProgress, tick);
        state.previousRightSwingProgress = state.rightSwingProgress;
        state.leftSwingProgress = sampleSwingProgress(data.leftSwingProgress, tick);
        state.previousLeftSwingProgress = state.leftSwingProgress;
        state.mainEquipProgress = clamp(data.mainEquipProgress.interpolate(tick, 0F), 0F, 1F);
        state.previousMainEquipProgress = clamp(
            data.mainEquipProgress.interpolate(tick - 1F, state.mainEquipProgress), 0F, 1F);
        state.offEquipProgress = clamp(data.offEquipProgress.interpolate(tick, 0F), 0F, 1F);
        state.previousOffEquipProgress = clamp(
            data.offEquipProgress.interpolate(tick - 1F, state.offEquipProgress), 0F, 1F);
        int wholeTick = (int) Math.floor(tick);
        int active = data.activeHand.interpolate(wholeTick, 0);
        state.usingItem = active != 0;
        state.activeHand = active == 2 ? Hand.OFF_HAND : Hand.MAIN_HAND;
        state.activeItem = copyItem(data.activeItem.interpolate(wholeTick, ItemStack.EMPTY));
        state.showUseParticles = data.showUseParticles.interpolate(tick, true);
        state.useTime = sampleUseTime(data, tick, active);
        state.bobPhase = data.bobPhase.interpolate(tick, 0F);
        state.previousBobPhase = data.bobPhase.interpolate(tick - 1F, state.bobPhase);
        state.bobStrength = Math.max(0F, data.bobStrength.interpolate(tick, 0F));
        state.previousBobStrength = Math.max(0F, data.bobStrength.interpolate(tick - 1F, state.bobStrength));
        state.mainArm = data.mainArm.interpolate(tick, false) ? Arm.LEFT : Arm.RIGHT;
        state.viewYaw = replay.yaw.interpolate(tick, 0D).floatValue();
        state.previousViewYaw = replay.yaw.interpolate(tick - 1F, (double) state.viewYaw).floatValue();
        state.viewPitch = replay.pitch.interpolate(tick, 0D).floatValue();
        state.previousViewPitch = replay.pitch.interpolate(tick - 1F, (double) state.viewPitch).floatValue();

        state.renderYaw = data.renderYaw.interpolate(tick, state.viewYaw);
        state.previousRenderYaw = data.renderYaw.interpolate(tick - 1F, state.renderYaw);
        state.renderPitch = data.renderPitch.interpolate(tick, state.viewPitch);
        state.previousRenderPitch = data.renderPitch.interpolate(tick - 1F, state.renderPitch);
        state.worldInteraction = data.worldInteraction.interpolate(tick, false);
        state.replayInteraction = data.replayInteraction.interpolate(tick, false);

        return state;
    }

    /** Swing progress is a cyclic phase, not an ordinary scalar. Across a
     * recorded 0.83 -> 0 reset, interpolate forward toward 1 and let the exact
     * next tick become 0. This preserves smooth sub-tick playback without the
     * backwards swipe created by normal float interpolation. */
    private static float sampleSwingProgress(KeyframeChannel<Float> channel, float tick)
    {
        int wholeTick = (int) Math.floor(tick);
        float transition = tick - wholeTick;
        float current = clamp(channel.interpolate(wholeTick, 0F), 0F, 1F);

        if (transition <= 0F)
        {
            return current;
        }

        float next = clamp(channel.interpolate(wholeTick + 1F, current), 0F, 1F);

        if (current > 0F && next < current)
        {
            float unwrapped = current + (next + 1F - current) * transition;
            return unwrapped >= 1F ? unwrapped - 1F : unwrapped;
        }

        return clamp(channel.interpolate(tick, current), 0F, 1F);
    }

    /** Use time is stored as exact integer recorder ticks, but Minecraft's eat
     * and drink transforms are rendered every frame. A completed item resets
     * 32 -> 1 (or 0 when use stops); blend that final segment back to neutral
     * instead of holding the maximum pose and snapping on the next frame. */
    private static float sampleUseTime(RecordedHandData data, float tick, int active)
    {
        if (active == 0)
        {
            return 0F;
        }

        int wholeTick = (int) Math.floor(tick);
        float transition = tick - wholeTick;
        int current = Math.max(0, data.useTime.interpolate(wholeTick, 0));

        if (transition <= 0F)
        {
            return current;
        }

        int nextActive = data.activeHand.interpolate(wholeTick + 1F, 0);
        int next = Math.max(0, data.useTime.interpolate(wholeTick + 1F, current));

        if (nextActive != active)
        {
            /* Do not run the use clock backwards during the final fractional
             * tick. Vanilla holds the last use pose and resets on the next
             * discrete tick. Reversing here caused the pre/post-eating kick. */
            return current;
        }

        if (next < current)
        {
            return current + (next - current) * transition;
        }

        if (next - current > 2)
        {
            return current;
        }

        return current + (next - current) * transition;
    }

    private static ItemStack copyItem(ItemStack stack)
    {
        return stack == null ? ItemStack.EMPTY : stack.copy();
    }

    private static PoseTransform copyPoseTransform(Transform source)
    {
        PoseTransform copy = new PoseTransform();
        if (source != null)
        {
            copy.copy(source);
        }
        return copy;
    }

    private static float clamp(float value, float min, float max)
    {
        return Math.max(min, Math.min(max, value));
    }
}

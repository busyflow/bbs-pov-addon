package Glaxium.POV.hand.playback;

import Glaxium.POV.PovAddon;
import Glaxium.POV.actions.camera.CameraShakeApplier;
import Glaxium.POV.camera.PovCameraMode;
import Glaxium.POV.camera.clip.PovCameraClip;
import Glaxium.POV.camera.clip.PovCameraClips;
import Glaxium.POV.hand.HandState;
import Glaxium.POV.hand.render.PovHandMatrices;
import Glaxium.POV.hand.editor.PovHandPicking;
import Glaxium.POV.hand.PovItemPose;
import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.replay.PovReplaySettings;
import mchorse.bbs_mod.client.renderer.LivePlayerItemUse;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.controller.UIFilmController;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/** Applies sampled POV state only for the duration of GameRenderer.renderHand(). */
public final class PovHandPlayback
{
    private static HandPlaybackSession active;
    private static boolean reportedFailure;
    public static boolean suppressFormTransform = false;

    private PovHandPlayback()
    {
    }

    public static boolean isSuppressFormTransform()
    {
        return suppressFormTransform
            || Glaxium.POV.editor.UIPovHandEditor.isActive()
            || isActive();
    }

    public static boolean isSuppressFormTransform(mchorse.bbs_mod.forms.forms.Form form)
    {
        if (form == null || mchorse.bbs_mod.forms.FormUtils.getRoot(form) != form)
        {
            return false;
        }

        return isSuppressFormTransform();
    }

    public static boolean isHandActive(float renderTransition)
    {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        PovPlaybackContext.Frame playback = PovPlaybackContext.getActive(renderTransition);
        UIFilmPanel panel = playback == null ? PovReplaySettings.getFilmPanel() : null;

        if (playback != null && !client.options.getPerspective().isFirstPerson())
        {
            return false;
        }

        if (player == null || (playback == null && panel == null))
        {
            return false;
        }

        Replay replay;
        if (playback != null)
        {
            if (!playback.clip().hands.get())
            {
                return false;
            }
            replay = playback.replay();
        }
        else
        {
            if (panel.getData() == null)
            {
                return false;
            }

            boolean inHandEditor = Glaxium.POV.editor.UIPovHandEditor.isActive();
            int povMode = panel.getController().getPovMode();

            if (!inHandEditor && (povMode == UIFilmController.CAMERA_MODE_FREE || povMode == UIFilmController.CAMERA_MODE_ORBIT))
            {
                return false;
            }

            boolean povEditMode = povMode == PovCameraMode.POV || inHandEditor;
            if (povEditMode)
            {
                replay = panel.replayEditor != null ? panel.replayEditor.getReplay() : null;
                if (replay == null && inHandEditor && Glaxium.POV.editor.UIPovHandEditor.getActive() != null)
                {
                    replay = Glaxium.POV.editor.UIPovHandEditor.getActive().getReplay();
                }
            }
            else
            {
                Film film = (Film) panel.getData();
                int cursor = panel.getCursor();
                boolean isPlaying = !inHandEditor && panel.getRunner() != null && panel.getRunner().isRunning();
                float transition = isPlaying ? Math.max(0F, Math.min(1F, renderTransition)) : 0F;
                PovCameraClip clip = PovCameraClips.resolve(film, cursor + transition);

                if (clip == null || !clip.hands.get())
                {
                    return false;
                }

                replay = PovCameraClips.resolveReplay(film, clip);
            }
        }

        return replay != null && (replay.keyframes instanceof ReplayKeyframesPovAccess);
    }

    public static HandState resolveActiveState(float renderTransition)
    {
        if (active != null)
        {
            return active.state;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        PovPlaybackContext.Frame playback = PovPlaybackContext.getActive(renderTransition);
        UIFilmPanel panel = playback == null ? PovReplaySettings.getFilmPanel() : null;

        if (playback != null && !client.options.getPerspective().isFirstPerson())
        {
            return null;
        }

        if (playback == null && panel == null)
        {
            return null;
        }

        Replay replay;
        float tick;
        if (playback != null)
        {
            if (!playback.clip().hands.get())
            {
                return null;
            }
            replay = playback.replay();
            tick = playback.replayTick();
        }
        else
        {
            if (panel.getData() == null)
            {
                return null;
            }
            int povMode = panel.getController().getPovMode();
            boolean inHandEditor = Glaxium.POV.editor.UIPovHandEditor.isActive();
            if (!inHandEditor && (povMode == UIFilmController.CAMERA_MODE_FREE || povMode == UIFilmController.CAMERA_MODE_ORBIT))
            {
                return null;
            }

            boolean povEditMode = povMode == PovCameraMode.POV || inHandEditor;
            Film film = (Film) panel.getData();
            int cursor = panel.getCursor();
            boolean isPlaying = !inHandEditor && panel.getRunner() != null && panel.getRunner().isRunning();
            float transition = isPlaying ? Math.max(0F, Math.min(1F, renderTransition)) : 0F;

            if (povEditMode)
            {
                replay = panel.replayEditor != null ? panel.replayEditor.getReplay() : null;
                if (replay == null && inHandEditor && Glaxium.POV.editor.UIPovHandEditor.getActive() != null)
                {
                    replay = Glaxium.POV.editor.UIPovHandEditor.getActive().getReplay();
                }
            }
            else
            {
                PovCameraClip clip = PovCameraClips.resolve(film, cursor + transition);
                if (clip == null || !clip.hands.get())
                {
                    return null;
                }
                replay = PovCameraClips.resolveReplay(film, clip);
            }

            if (replay == null)
            {
                return null;
            }
            tick = replay.getTick(cursor) + transition;
        }

        if (!(replay.keyframes instanceof ReplayKeyframesPovAccess access))
        {
            return null;
        }

        RecordedHandData data = access.bbsPov$getHand();
        if (data == null)
        {
            return null;
        }
        return HandSampler.sample(data, replay.keyframes, tick);
    }

    public static boolean begin(float renderTransition)
    {
        if (active != null)
        {
            /* Defensive cleanup if another mixin cancelled the previous renderHand call. */
            end();
        }

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        PovPlaybackContext.Frame playback = PovPlaybackContext.getActive(renderTransition);
        UIFilmPanel panel = playback == null ? PovReplaySettings.getFilmPanel() : null;

        /* F5 changes only the Minecraft perspective. Keep the POV HUD overlay,
         * but a first-person hand must never be forced into either third-person
         * camera during Right-Control playback. */
        if (playback != null && !client.options.getPerspective().isFirstPerson())
        {
            return false;
        }

        if (player == null || playback == null && panel == null)
        {
            return false;
        }

        Replay replay;
        float tick;
        boolean isPlaying;
        float transition;

        if (playback != null)
        {
            if (!playback.clip().hands.get())
            {
                return false;
            }

            replay = playback.replay();
            tick = playback.replayTick();
            isPlaying = !playback.controller().paused;
            transition = isPlaying ? Math.max(0F, Math.min(1F, renderTransition)) : 0F;
        }
        else
        {
            boolean inHandEditor = Glaxium.POV.editor.UIPovHandEditor.isActive();
            int povMode = panel.getController().getPovMode();

            if (!inHandEditor && (povMode == UIFilmController.CAMERA_MODE_FREE || povMode == UIFilmController.CAMERA_MODE_ORBIT))
            {
                return false;
            }

            boolean povEditMode = povMode == PovCameraMode.POV || inHandEditor;
            Film film = (Film) panel.getData();
            int cursor = panel.getCursor();
            isPlaying = !inHandEditor && panel.getRunner() != null && panel.getRunner().isRunning();
            transition = isPlaying
                ? Math.max(0F, Math.min(1F, renderTransition))
                : 0F;

            if (povEditMode)
            {
                /* POV Camera Mode is the full hand-editing workspace. It keeps
                 * using the Replay Editor selection and does not require a clip. */
                replay = panel.replayEditor != null ? panel.replayEditor.getReplay() : null;

                if (replay == null && inHandEditor && Glaxium.POV.editor.UIPovHandEditor.getActive() != null)
                {
                    replay = Glaxium.POV.editor.UIPovHandEditor.getActive().getReplay();
                }
            }
            else
            {
                /* In every normal camera mode, a POV Camera Clip is the only
                 * thing allowed to put replay hands on screen. */
                PovCameraClip clip = PovCameraClips.resolve(film, cursor + transition);

                if (clip == null || !clip.hands.get())
                {
                    return false;
                }

                replay = PovCameraClips.resolveReplay(film, clip);
            }

            if (replay == null)
            {
                return false;
            }

            tick = replay.getTick(cursor) + transition;
        }

        if (!(replay.keyframes instanceof ReplayKeyframesPovAccess access))
        {
            return false;
        }

        RecordedHandData data = access.bbsPov$getHand();
        HandState state;

        if (Glaxium.POV.editor.UIPovHandEditor.isActive())
        {
            state = HandStateApplier.createDefaultHandEditorState(data, replay);
        }
        else
        {
            state = data == null ? null : HandSampler.sample(data, replay.keyframes, tick);

            if (state == null)
            {
                return false;
            }

            HandStateApplier.inheritReplayModel(data, state, replay, panel);
        }

        /* Right-Control playback enables BBS's LivePlayerItemUse override before
         * renderHand(). That override intercepts isUsingItem(), getActiveHand(),
         * getActiveItem() and getItemUseTimeLeft(), so it used to replace half of
         * the POV state below with BBS's independently sampled action-clip clock.
         * The POV hand keyframes are the sole first-person source for this pass. */
        if (playback != null)
        {
            LivePlayerItemUse.endFrame();
        }

        HandPlaybackSession next = new HandPlaybackSession(player, client.gameRenderer.firstPersonRenderer, state, data, tick, isPlaying, transition);

        try
        {
            next.apply(replay);
            active = next;

            if (!PovHandPicking.isStencilPass())
            {
                PovHandMatrices.clear();
                PovHandPicking.clearItemBounds();
            }

            reportedFailure = false;
            return true;
        }
        catch (Throwable throwable)
        {
            next.restore();

            if (!reportedFailure)
            {
                PovAddon.LOGGER.error("Couldn't prepare BBS POV hand playback", throwable);
                reportedFailure = true;
            }

            return false;
        }
    }

    public static boolean render(
        HeldItemRenderer renderer,
        float tickDelta,
        MatrixStack matrices,
        VertexConsumerProvider.Immediate consumers,
        ClientPlayerEntity player,
        int light)
    {
        HandPlaybackSession current = active;

        if (current == null || current.player != player)
        {
            return false;
        }

        if (!current.state.visible)
        {
            consumers.draw();
            end();
            return true;
        }

        matrices.push();

        try
        {
            /* The Film cursor is the animation clock. Minecraft's live tickDelta keeps
             * cycling even while that cursor is paused, so using it here blends the
             * same Film tick back and forth against tick - 1 and makes the hand shiver. */
            /* Hands render from an identity base (screen overlay), so re-apply the
             * same baked Camera Shake the world got in tiltViewWhenHurt — otherwise
             * the view tilts and the hands stay glued to the screen. */
            CameraShakeApplier.apply(matrices, tickDelta);
            MatrixStackUtils.applyTransform(matrices, current.state.cameraOffset);
            renderer.renderItem(current.getItemRenderTickDelta(), matrices, consumers, player, light);
            // Finish held-item layers before the independent bodypart pass.
            /* [PORTING NOTE: BBS FILM STENCIL PICKING FIX]
             * When isStencilPass() is true during Film picking, we must pass the active
             * StencilMap to current.renderBodyParts() so the bodyparts are drawn with the
             * picker shader into the stencil buffer. Otherwise, body parts are omitted from
             * picking and cannot be hovered/clicked in Film editor. */
            if (Glaxium.POV.hand.editor.PovHandPicking.isStencilPass())
            {
                current.renderBodyParts(light, Glaxium.POV.hand.editor.PovHandPicking.getStencilMap());
            }
            else
            {
                current.renderBodyParts(light);
            }
        }
        finally
        {
            matrices.pop();
            end();
        }

        return true;
    }

    /** BBS suppresses vanilla camera bob while a Film controller is active, so the
     * recorded first-person hand needs its own copy of vanilla's bob transform. */
    public static void applyBob(MatrixStack matrices, float influence)
    {
        HandPlaybackSession current = active;

        if (current != null && current.state != null && influence > 0F)
        {
            applyBob(matrices, current.state, influence);
        }
    }

    public static void applyBob(MatrixStack matrices, HandState state, float influence)
    {
        if (state == null || influence <= 0F)
        {
            return;
        }

        float phase = -state.bobPhase;
        float strength = state.bobStrength * influence;
        float sin = MathHelper.sin(phase * (float) Math.PI);
        float cos = MathHelper.cos(phase * (float) Math.PI);
        matrices.translate(sin * strength * 0.5F, -Math.abs(cos * strength), 0F);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin * strength * 3F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(
            Math.abs(MathHelper.cos(phase * (float) Math.PI - 0.2F) * strength) * 5F));
    }

    public static void applyTransforms(MatrixStack matrices)
    {
        HandPlaybackSession current = active;

        if (current != null)
        {
            matrices.push();
            CameraShakeApplier.apply(
                matrices,
                MinecraftClient.getInstance().getTickDelta());
            MatrixStackUtils.applyTransform(matrices, current.state.cameraOffset);
        }
    }

    public static void popTransforms(MatrixStack matrices)
    {
        matrices.pop();
    }

    public static float getCurrentItemRenderTickDelta(float fallback)
    {
        HandPlaybackSession current = active;

        return current == null ? fallback : current.getItemRenderTickDelta();
    }

    public static void end()
    {
        HandPlaybackSession current = active;
        active = null;

        if (current != null)
        {
            current.restore();
        }
    }

    public static boolean isActive()
    {
        return active != null;
    }

    public static HandState getActiveState()
    {
        return active != null ? active.state : null;
    }

    /** ModelFormRenderer's MAIN/OFF hand values identify the physical right/left
     * model slots, even when Minecraft's logical main arm is left-handed. */
    public static boolean shouldRenderModelHand(Hand hand)
    {
        HandPlaybackSession current = active;

        return current == null
            || (hand == Hand.MAIN_HAND
                ? current.state.rightHandVisible
                : current.state.leftHandVisible);
    }

    public static boolean shouldRenderArm(Arm arm)
    {
        HandPlaybackSession current = active;

        return current == null
            || (arm == Arm.RIGHT
                ? current.state.rightHandVisible
                : current.state.leftHandVisible);
    }

    /** Final pose resolved from this frame's POV pose and per-limb keyframes. */
    public static Pose getRenderPose()
    {
        HandPlaybackSession current = active;

        return current == null ? null : current.renderPose;
    }

    /** Transform for the logical hand item currently entering vanilla's item renderer. */
    public static PoseTransform getItemPose(boolean leftHanded)
    {
        HandPlaybackSession current = active;

        if (current == null)
        {
            return new PoseTransform();
        }

        String bone = PovItemPose.bone(leftHanded, current.state.mainArm);

        return PovItemPose.transform(current.state.itemPose, bone);
    }

    public static String getItemBone(boolean leftHanded)
    {
        HandPlaybackSession current = active;

        return current == null ? null : PovItemPose.bone(leftHanded, current.state.mainArm);
    }

    /** Keep the picking render identical to the already rendered visible hand. */
    public static void useCapturedPose(Pose pose)
    {
        HandPlaybackSession current = active;

        if (current != null && pose != null)
        {
            current.renderPose = pose.copy();
        }
    }

    /** Render only nested bodyparts into BBS's picker framebuffer. The hand
     * itself is intentionally omitted so BodyPart mode cannot select a hand. */
    public static void renderBodyPartsForPicking(int light, mchorse.bbs_mod.ui.framework.elements.utils.StencilMap stencilMap)
    {
        HandPlaybackSession current = active;

        if (current != null)
        {
            current.renderBodyParts(light, stencilMap);
        }
    }

    public static void renderBodyParts(int light)
    {
        HandPlaybackSession current = active;

        if (current != null)
        {
            current.renderBodyParts(light);
        }
    }

    public static float getActiveTransition()
    {
        HandPlaybackSession current = active;

        return current != null ? current.transition : 0F;
    }

    public static float getArmFix(Arm arm)
    {
        HandPlaybackSession current = active;

        if (current == null)
        {
            return 0F;
        }

        float maxFix = 0F;

        String bone = arm == Arm.RIGHT ? "right_arm" : "left_arm";
        PoseTransform pose = current.state != null ? (arm == Arm.RIGHT ? current.state.rightPose : current.state.leftPose) : null;

        if (pose != null && pose.fix > 0F)
        {
            maxFix = Math.max(maxFix, pose.fix);
        }

        if (current.state != null && current.state.pose != null)
        {
            maxFix = Math.max(maxFix, getPoseFixForArm(current.state.pose, bone));
        }

        if (current.povForm != null)
        {
            if (current.povForm.pose.get() != null)
            {
                maxFix = Math.max(maxFix, getPoseFixForArm(current.povForm.pose.get(), bone));
            }
            if (current.povForm.poseOverlay.get() != null)
            {
                maxFix = Math.max(maxFix, getPoseFixForArm(current.povForm.poseOverlay.get(), bone));
            }
            for (mchorse.bbs_mod.settings.values.core.ValuePose addOverlay : current.povForm.additionalOverlays)
            {
                if (addOverlay.get() != null)
                {
                    maxFix = Math.max(maxFix, getPoseFixForArm(addOverlay.get(), bone));
                }
            }
        }

        return MathHelper.clamp(maxFix, 0F, 1F);
    }

    private static float getPoseFixForArm(Pose pose, String armRoot)
    {
        if (pose == null || pose.transforms.isEmpty())
        {
            return 0F;
        }

        float max = 0F;
        for (java.util.Map.Entry<String, PoseTransform> entry : pose.transforms.entrySet())
        {
            String name = entry.getKey();
            PoseTransform pt = entry.getValue();
            if (pt != null && pt.fix > 0F)
            {
                if (name.equals(armRoot) || name.startsWith(armRoot) || name.contains(armRoot))
                {
                    max = Math.max(max, pt.fix);
                }
            }
        }
        return max;
    }

    public static float getHandFix(Hand hand)
    {
        HandPlaybackSession current = active;

        if (current == null || current.player == null)
        {
            return 0F;
        }

        Arm arm = hand == Hand.MAIN_HAND
            ? current.player.getMainArm()
            : current.player.getMainArm().getOpposite();

        return getArmFix(arm);
    }

    public static void applyHandAnimations(MatrixStack matrices, Hand hand)
    {
        HandPlaybackSession current = active;

        if (current == null || current.state == null)
        {
            return;
        }

        float fix = getHandFix(hand);
        float influence = 1.0F - fix;

        if (influence <= 0F)
        {
            return;
        }

        float pitchDiff = current.state.viewPitch - current.state.renderPitch;
        float yawDiff = current.state.viewYaw - current.state.renderYaw;

        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitchDiff * 0.1F * influence));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yawDiff * 0.1F * influence));

        applyBob(matrices, current.state, influence);
    }
}

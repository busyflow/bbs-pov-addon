package Glaxium.POV.hand.playback;

import Glaxium.POV.bodypart.PovBodyPartPlayback;
import Glaxium.POV.hand.HandState;
import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.integration.access.minecraft.ClientPlayerEntityPovAccess;
import Glaxium.POV.integration.access.minecraft.HeldItemRendererPovAccess;
import Glaxium.POV.integration.access.minecraft.LivingEntityPovAccess;
import Glaxium.POV.integration.access.bbs.MorphPovAccess;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.utils.pose.Pose;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;

/** Captures and restores live player/hand renderer state for one renderHand pass. */
public final class HandPlaybackSession
{
    final ClientPlayerEntity player;
    private final HeldItemRenderer renderer;
    final HandState state;
    private final RecordedHandData data;
    private final float tick;
    private final boolean isPlaying;
    final float transition;
    Pose renderPose;
    private final HeldItemRendererPovAccess held;
    private final ClientPlayerEntityPovAccess clientPlayer;
    private final LivingEntityPovAccess living;
    private final Morph morph;

    private final ItemStack playerMain;
    private final ItemStack playerOff;
    private final ItemStack cachedMain;
    private final ItemStack cachedOff;
    private final float equipMain;
    private final float prevEquipMain;
    private final float equipOff;
    private final float prevEquipOff;
    private final boolean handSwinging;
    private final Hand preferredHand;
    private final int handSwingTicks;
    private final float lastHandSwingProgress;
    private final float handSwingProgress;
    private final boolean usingItem;
    private final Hand clientActiveHand;
    private final ItemStack activeItem;
    private final int useTimeLeft;
    private final Arm mainArm;
    private final float horizontalSpeed;
    private final float prevHorizontalSpeed;
    private final float strideDistance;
    private final float prevStrideDistance;
    private final float yaw;
    private final float previousYaw;
    private final float pitch;
    private final float previousPitch;
    private final float renderYaw;
    private final float lastRenderYaw;
    private final float renderPitch;
    private final float lastRenderPitch;
    private final float bodyYaw;
    private final float previousBodyYaw;
    private final float headYaw;
    private final float previousHeadYaw;
    private final Form originalForm;
    ModelForm povForm;

    HandPlaybackSession(
        ClientPlayerEntity player,
        HeldItemRenderer renderer,
        HandState state,
        RecordedHandData data,
        float tick,
        boolean isPlaying,
        float transition)
    {
        this.player = player;
        this.renderer = renderer;
        this.state = state;
        this.data = data;
        this.tick = tick;
        this.isPlaying = isPlaying;
        this.transition = transition;
        this.renderPose = state.pose.copy();
        this.held = (HeldItemRendererPovAccess) renderer;
        this.clientPlayer = (ClientPlayerEntityPovAccess) player;
        this.living = (LivingEntityPovAccess) player;
        this.morph = Morph.getMorph(player);
        this.playerMain = player.getMainHandStack();
        this.playerOff = player.getOffHandStack();
        this.cachedMain = this.held.bbsPov$getMainHand();
        this.cachedOff = this.held.bbsPov$getOffHand();
        this.equipMain = this.held.bbsPov$getEquipProgressMainHand();
        this.prevEquipMain = this.held.bbsPov$getPrevEquipProgressMainHand();
        this.equipOff = this.held.bbsPov$getEquipProgressOffHand();
        this.prevEquipOff = this.held.bbsPov$getPrevEquipProgressOffHand();
        this.handSwinging = player.handSwinging;
        this.preferredHand = player.preferredHand;
        this.handSwingTicks = player.handSwingTicks;
        this.lastHandSwingProgress = player.lastHandSwingProgress;
        this.handSwingProgress = player.handSwingProgress;
        this.usingItem = this.clientPlayer.bbsPov$isUsingItem();
        this.clientActiveHand = this.clientPlayer.bbsPov$getClientActiveHand();
        this.activeItem = this.living.bbsPov$getActiveItemStack();
        this.useTimeLeft = this.living.bbsPov$getItemUseTimeLeft();
        this.mainArm = player.getMainArm();
        this.horizontalSpeed = player.horizontalSpeed;
        this.prevHorizontalSpeed = player.prevHorizontalSpeed;
        this.strideDistance = player.strideDistance;
        this.prevStrideDistance = player.prevStrideDistance;
        this.yaw = player.getYaw();
        this.previousYaw = player.prevYaw;
        this.pitch = player.getPitch();
        this.previousPitch = player.prevPitch;
        this.renderYaw = player.renderYaw;
        this.lastRenderYaw = player.lastRenderYaw;
        this.renderPitch = player.renderPitch;
        this.lastRenderPitch = player.lastRenderPitch;
        this.bodyYaw = player.bodyYaw;
        this.previousBodyYaw = player.prevBodyYaw;
        this.headYaw = player.headYaw;
        this.previousHeadYaw = player.prevHeadYaw;
        this.originalForm = this.morph == null ? null : this.morph.getForm();
    }

    void apply(Replay replay)
    {
        ItemStack handStack = this.state.activeHand == Hand.OFF_HAND
            ? this.state.offHand
            : this.state.mainHand;
        ItemStack activeStack = this.state.activeItem.isEmpty()
            ? handStack
            : this.state.activeItem;
        /* HeldItemRenderer chooses EAT/DRINK from the stack passed to its
         * hand render, not LivingEntity.activeItemStack. Replay inventory
         * can already contain the post-use result (stew -> bowl, or the
         * next food stack) while the recorded use event is still active.
         * Keep the exact start-of-use snapshot in the rendered hand for
         * the whole event so its animation cannot intermittently vanish. */
        ItemStack renderMain = this.state.usingItem && this.state.activeHand == Hand.MAIN_HAND
            ? activeStack
            : this.state.mainHand;
        ItemStack renderOff = this.state.usingItem && this.state.activeHand == Hand.OFF_HAND
            ? activeStack
            : this.state.offHand;

        this.player.setStackInHand(Hand.MAIN_HAND, renderMain);
        this.player.setStackInHand(Hand.OFF_HAND, renderOff);
        this.held.bbsPov$setMainHand(renderMain);
        this.held.bbsPov$setOffHand(renderOff);
        if (Glaxium.POV.editor.UIPovHandEditor.isActive())
        {
            this.held.bbsPov$setEquipProgressMainHand(1F);
            this.held.bbsPov$setPrevEquipProgressMainHand(1F);
            this.held.bbsPov$setEquipProgressOffHand(1F);
            this.held.bbsPov$setPrevEquipProgressOffHand(1F);
            this.player.handSwinging = false;
            this.player.handSwingTicks = 0;
            this.player.lastHandSwingProgress = 0F;
            this.player.handSwingProgress = 0F;
        }
        else
        {
            float main = 1F - this.state.mainEquipProgress;
            float previousMain = main;
            float off = 1F - this.state.offEquipProgress;
            float previousOff = off;

            if (this.state.usingItem && !activeStack.isEmpty() && activeStack.isOf(net.minecraft.item.Items.SPYGLASS))
            {
                if (this.state.activeHand == Hand.MAIN_HAND)
                {
                    main = 1F;
                    previousMain = 1F;
                }
                else
                {
                    off = 1F;
                    previousOff = 1F;
                }
            }

            this.held.bbsPov$setEquipProgressMainHand(main);
            this.held.bbsPov$setPrevEquipProgressMainHand(previousMain);
            this.held.bbsPov$setEquipProgressOffHand(off);
            this.held.bbsPov$setPrevEquipProgressOffHand(previousOff);

            boolean rightSwinging = this.state.rightSwingProgress > 0F;
            boolean leftSwinging = this.state.leftSwingProgress > 0F;
            this.player.handSwinging = rightSwinging || leftSwinging;
            boolean isOffHandSwinging = (this.state.mainArm == Arm.RIGHT)
                ? (leftSwinging && !rightSwinging)
                : (rightSwinging && !leftSwinging);
            this.player.preferredHand = isOffHandSwinging ? Hand.OFF_HAND : Hand.MAIN_HAND;
            this.player.handSwingTicks = 0;
            float activeSwing = rightSwinging ? this.state.rightSwingProgress : this.state.leftSwingProgress;
            float prevActiveSwing = rightSwinging ? this.state.previousRightSwingProgress : this.state.previousLeftSwingProgress;
            this.player.lastHandSwingProgress = prevActiveSwing;
            this.player.handSwingProgress = activeSwing;
            boolean isUsing = this.state.usingItem && !activeStack.isEmpty();
            this.clientPlayer.bbsPov$setUsingItem(isUsing);
            this.clientPlayer.bbsPov$setClientActiveHand(this.state.activeHand);
            this.living.bbsPov$setLivingFlag(1, isUsing);
            this.living.bbsPov$setLivingFlag(2, this.state.activeHand == Hand.OFF_HAND);
            this.living.bbsPov$setActiveItemStack(activeStack);
            int maxUse = activeStack.getMaxUseTime() > 0 ? activeStack.getMaxUseTime() : 32;
            int remaining = Math.max(1, (int) Math.floor(maxUse - this.state.useTime));
            this.living.bbsPov$setItemUseTimeLeft(isUsing ? remaining : 0);

            this.player.horizontalSpeed = 0F;
            this.player.prevHorizontalSpeed = 0F;
            this.player.strideDistance = 0F;
            this.player.prevStrideDistance = 0F;

            this.player.setYaw(this.state.viewYaw);
            this.player.prevYaw = this.state.viewYaw;
            this.player.renderYaw = this.state.viewYaw;
            this.player.lastRenderYaw = this.state.viewYaw;
            this.player.bodyYaw = this.state.viewYaw;
            this.player.prevBodyYaw = this.state.viewYaw;
            this.player.headYaw = this.state.viewYaw;
            this.player.prevHeadYaw = this.state.viewYaw;
            this.player.setPitch(this.state.viewPitch);
            this.player.prevPitch = this.state.viewPitch;
            this.player.renderPitch = this.state.viewPitch;
            this.player.lastRenderPitch = this.state.viewPitch;
        }

        if (this.morph != null)
        {
            ModelForm form = HandFormCache.get(replay);
            HandStateApplier.applyForm(form, this.state, this.data, this.tick);
            this.povForm = form;

            HandFormCache.updateIfNeeded(
                form,
                this.morph.entity,
                replay,
                this.tick,
                this.isPlaying);

            FormRenderer<?> renderer = FormUtilsClient.getRenderer(form);

            if (renderer instanceof ModelFormRenderer modelRenderer)
            {
                modelRenderer.ensureAnimator(this.isPlaying ? this.transition : 0F);
                /* Resolve once from the keyframes sampled for this Film frame.
                 * renderArm may be invoked through another cached renderer, so it
                 * must not be allowed to substitute that renderer's form pose. */
                this.renderPose = modelRenderer.getPose();
            }

            ((MorphPovAccess) this.morph).bbsPov$setFormRaw(form);
        }
    }

    void renderBodyParts(int light)
    {
        PovBodyPartPlayback.render(this.povForm, light);
    }

    void renderBodyParts(int light, mchorse.bbs_mod.ui.framework.elements.utils.StencilMap stencilMap)
    {
        PovBodyPartPlayback.render(this.povForm, light, stencilMap);
    }

    float getTransition()
    {
        return this.transition;
    }

    /** Reconstruct vanilla's fractional use clock. HeldItemRenderer uses
     * remainingTicks - tickDelta + 1, while our keyframes store continuous
     * elapsed time. */
    float getItemRenderTickDelta()
    {
        if (!this.state.usingItem)
        {
            return 1F;
        }

        ItemStack handStack = this.state.activeHand == Hand.OFF_HAND
            ? this.state.offHand
            : this.state.mainHand;
        ItemStack activeStack = this.state.activeItem.isEmpty()
            ? handStack
            : this.state.activeItem;
        int maxUse = activeStack.getMaxUseTime() > 0 ? activeStack.getMaxUseTime() : 32;
        float desiredRemaining = Math.max(1F, (float) maxUse - this.state.useTime);
        int storedRemaining = Math.max(1, (int) Math.floor(desiredRemaining));

        return Math.max(0F, Math.min(1F, storedRemaining + 1F - desiredRemaining));
    }

    void restore()
    {
        this.player.setStackInHand(Hand.MAIN_HAND, this.playerMain);
        this.player.setStackInHand(Hand.OFF_HAND, this.playerOff);
        this.held.bbsPov$setMainHand(this.cachedMain);
        this.held.bbsPov$setOffHand(this.cachedOff);
        this.held.bbsPov$setEquipProgressMainHand(this.equipMain);
        this.held.bbsPov$setPrevEquipProgressMainHand(this.prevEquipMain);
        this.held.bbsPov$setEquipProgressOffHand(this.equipOff);
        this.held.bbsPov$setPrevEquipProgressOffHand(this.prevEquipOff);
        this.player.handSwinging = this.handSwinging;
        this.player.preferredHand = this.preferredHand;
        this.player.handSwingTicks = this.handSwingTicks;
        this.player.lastHandSwingProgress = this.lastHandSwingProgress;
        this.player.handSwingProgress = this.handSwingProgress;
        this.player.setMainArm(this.mainArm);
        this.clientPlayer.bbsPov$setUsingItem(this.usingItem);
        this.clientPlayer.bbsPov$setClientActiveHand(this.clientActiveHand);
        this.living.bbsPov$setLivingFlag(1, this.usingItem);
        this.living.bbsPov$setLivingFlag(2, this.clientActiveHand == Hand.OFF_HAND);
        this.living.bbsPov$setActiveItemStack(this.activeItem);
        this.living.bbsPov$setItemUseTimeLeft(this.useTimeLeft);

        if (!Glaxium.POV.editor.UIPovHandEditor.isActive())
        {
            this.player.horizontalSpeed = this.horizontalSpeed;
            this.player.prevHorizontalSpeed = this.prevHorizontalSpeed;
            this.player.strideDistance = this.strideDistance;
            this.player.prevStrideDistance = this.prevStrideDistance;
            this.player.setYaw(this.yaw);
            this.player.prevYaw = this.previousYaw;
            this.player.setPitch(this.pitch);
            this.player.prevPitch = this.previousPitch;
            this.player.renderYaw = this.renderYaw;
            this.player.lastRenderYaw = this.lastRenderYaw;
            this.player.renderPitch = this.renderPitch;
            this.player.lastRenderPitch = this.lastRenderPitch;
            this.player.bodyYaw = this.bodyYaw;
            this.player.prevBodyYaw = this.previousBodyYaw;
            this.player.headYaw = this.headYaw;
            this.player.prevHeadYaw = this.previousHeadYaw;
        }

        if (this.morph != null)
        {
            ((MorphPovAccess) this.morph).bbsPov$setFormRaw(this.originalForm);
        }
    }
}

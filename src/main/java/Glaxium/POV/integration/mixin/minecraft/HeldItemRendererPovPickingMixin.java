package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.hand.HandState;
import Glaxium.POV.hand.PovItemPose;
import Glaxium.POV.hand.editor.PovHandGizmo;
import Glaxium.POV.hand.editor.PovHandPicking;
import Glaxium.POV.hand.playback.PovHandPlayback;
import Glaxium.POV.hand.render.PovHandMatrices;
import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.replay.PovReplaySettings;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({HeldItemRenderer.class})
public class HeldItemRendererPovPickingMixin {
   @Unique
   private boolean bbsPov$startedPlayback;
   @Unique
   private boolean bbsPov$itemPosePushed;

   @ModifyVariable(
      method = {"renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0
   )
   private int bbsPov$cameraLight(int original) {
      MinecraftClient client = MinecraftClient.getInstance();
      return client.world != null && PovHandPlayback.isHandActive(client.getTickDelta())
         ? WorldRenderer.getLightmapCoordinates(client.world, client.gameRenderer.getCamera().getBlockPos())
         : original;
   }

   @Inject(
      method = {"renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$beginItemPlayback(float tickDelta, MatrixStack matrices, Immediate consumers, ClientPlayerEntity player, int light, CallbackInfo info) {
      if (!PovHandPlayback.isActive()) {
         this.bbsPov$startedPlayback = PovHandPlayback.begin(tickDelta);
         if (this.bbsPov$startedPlayback) {
            PovHandPlayback.applyTransforms(matrices);
         } else if (PovPlaybackContext.getActive() != null
            || PovReplaySettings.getFilmPanel() != null
            || BBSModClient.getCameraController().getCurrent() != null
            || MinecraftClient.getInstance().currentScreen instanceof UIScreen) {
            info.cancel();
         }
      }
   }

   @Inject(
      method = {"renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V"},
      at = {@At("RETURN")}
   )
   private void bbsPov$endItemPlayback(float tickDelta, MatrixStack matrices, Immediate consumers, ClientPlayerEntity player, int light, CallbackInfo info) {
      if (this.bbsPov$startedPlayback) {
         try {
            consumers.draw();
            if (PovHandPicking.isStencilPass()) {
               PovHandPlayback.renderBodyPartsForPicking(light, PovHandPicking.getStencilMap());
            } else {
               PovHandPlayback.renderBodyParts(light);
            }
         } finally {
            this.bbsPov$startedPlayback = false;
            PovHandPlayback.popTransforms(matrices);
            PovHandGizmo.captureVisual();
            PovHandPlayback.end();
         }
      }
   }

   @ModifyVariable(
      method = {"renderFirstPersonItem"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 2
   )
   private float bbsPov$overrideSwingProgress(float originalSwingProgress, AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand) {
      if (PovHandPlayback.isActive()) {
         HandState state = PovHandPlayback.getActiveState();
         if (state != null) {
            Arm arm = hand == Hand.MAIN_HAND ? state.mainArm : state.mainArm.getOpposite();
            return arm == Arm.RIGHT ? state.rightSwingProgress : state.leftSwingProgress;
         }
      }

      return originalSwingProgress;
   }

   @Inject(
      method = {"renderFirstPersonItem"},
      at = {@At("HEAD")}
   )
   private void bbsPov$beforeRenderFirstPersonItem(
      AbstractClientPlayerEntity player,
      float tickDelta,
      float pitch,
      Hand hand,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      MatrixStack matrices,
      VertexConsumerProvider consumers,
      int light,
      CallbackInfo info
   ) {
      if (PovHandPlayback.isActive()) {
         matrices.push();
         PovHandPlayback.applyHandAnimations(matrices, hand);
      }
   }

   @Inject(
      method = {"renderFirstPersonItem"},
      at = {@At("RETURN")}
   )
   private void bbsPov$afterRenderFirstPersonItem(
      AbstractClientPlayerEntity player,
      float tickDelta,
      float pitch,
      Hand hand,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      MatrixStack matrices,
      VertexConsumerProvider consumers,
      int light,
      CallbackInfo info
   ) {
      if (PovHandPlayback.isActive()) {
         matrices.pop();
      }
   }

   @Inject(
      method = {"renderFirstPersonItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$renderEmptyOffHand(
      AbstractClientPlayerEntity player,
      float tickDelta,
      float pitch,
      Hand hand,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      MatrixStack matrices,
      VertexConsumerProvider consumers,
      int light,
      CallbackInfo info
   ) {
      if (PovHandPlayback.isActive() && hand == Hand.OFF_HAND && item.isEmpty() && !player.isInvisible()) {
         Arm arm = player.getMainArm().getOpposite();
         if (PovHandPlayback.shouldRenderArm(arm)) {
            ((HeldItemRendererPovAccessor)this).bbsPov$renderArmHoldingItem(matrices, consumers, light, equipProgress, swingProgress, arm);
            info.cancel();
         }
      }
   }

   @Redirect(
      method = {"renderFirstPersonItem"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;isUsingSpyglass()Z"
      )
   )
   private boolean bbsPov$neverHideSpyglassHand(AbstractClientPlayerEntity player) {
      return PovHandPlayback.isActive() ? false : player.isUsingSpyglass();
   }

   @Redirect(
      method = {"renderFirstPersonItem"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/item/ItemStack;getUseAction()Lnet/minecraft/util/UseAction;"
      )
   )
   private UseAction bbsPov$overrideUseAction(ItemStack stack) {
      UseAction original = stack.getUseAction();
      if (PovHandPlayback.isActive()) {
         HandState state = PovHandPlayback.getActiveState();
         if (state != null && state.usingItem && original == UseAction.NONE) {
            return UseAction.EAT;
         }
      }

      return original;
   }

   @Redirect(
      method = {"applyEatOrDrinkTransformation"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/item/ItemStack;getMaxUseTime()I"
      )
   )
   private int bbsPov$overrideMaxUseTime(ItemStack stack) {
      int original = stack.getMaxUseTime();
      return PovHandPlayback.isActive() && original <= 0 ? 32 : original;
   }

   @Inject(
      method = {"renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = {@At("HEAD")}
   )
   private void bbsPov$applyItemPose(
      LivingEntity entity,
      ItemStack stack,
      ModelTransformationMode mode,
      boolean leftHanded,
      MatrixStack matrices,
      VertexConsumerProvider consumers,
      int light,
      CallbackInfo info
   ) {
      this.bbsPov$itemPosePushed = false;
      if (PovHandPlayback.isActive() && !PovHandPicking.isStencilPass()) {
         String bone = PovHandPlayback.getItemBone(leftHanded);
         PoseTransform transform = PovHandPlayback.getItemPose(leftHanded);
         Matrix4f origin = new Matrix4f(matrices.peek().getPositionMatrix());
         matrices.push();
         PovItemPose.apply(matrices, transform);
         this.bbsPov$itemPosePushed = true;
         PovHandMatrices.captureItem(bone, origin, matrices.peek().getPositionMatrix());
         MinecraftClient client = MinecraftClient.getInstance();
         BakedModel model = client.getItemRenderer().getModel(stack, entity.getWorld(), entity, entity.getId());
         MatrixStack itemBounds = new MatrixStack();
         MatrixStackUtils.multiply(itemBounds, matrices.peek().getPositionMatrix());
         model.getTransformation().getTransformation(mode).apply(leftHanded, itemBounds);
         itemBounds.translate(-0.5F, -0.5F, -0.5F);
         PovHandPicking.captureItemBounds(bone, itemBounds.peek().getPositionMatrix());
      }
   }

   @Inject(
      method = {"renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = {@At("RETURN")}
   )
   private void bbsPov$restoreItemPose(
      LivingEntity entity,
      ItemStack stack,
      ModelTransformationMode mode,
      boolean leftHanded,
      MatrixStack matrices,
      VertexConsumerProvider consumers,
      int light,
      CallbackInfo info
   ) {
      if (this.bbsPov$itemPosePushed) {
         matrices.pop();
         this.bbsPov$itemPosePushed = false;
      }
   }

   @Inject(
      method = {"renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$hideItemFromHandStencil(
      LivingEntity entity,
      ItemStack stack,
      ModelTransformationMode mode,
      boolean leftHanded,
      MatrixStack matrices,
      VertexConsumerProvider consumers,
      int light,
      CallbackInfo info
   ) {
      if (PovHandPicking.isStencilPass()) {
         info.cancel();
      }
   }
}

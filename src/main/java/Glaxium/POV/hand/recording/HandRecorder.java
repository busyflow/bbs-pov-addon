package Glaxium.POV.hand.recording;

import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.integration.access.minecraft.HeldItemRendererPovAccess;
import mchorse.bbs_mod.forms.forms.Form;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;

public final class HandRecorder {
   private int lastRecordedTick = Integer.MIN_VALUE;
   private boolean suppressRejectedConsumeSwing;
   private int spyglassCooldown = 0;

   public void reset() {
      this.lastRecordedTick = Integer.MIN_VALUE;
      this.suppressRejectedConsumeSwing = false;
      this.spyglassCooldown = 0;
   }

   public boolean hasRecorded() {
      return this.lastRecordedTick != Integer.MIN_VALUE;
   }

   public void record(RecordedHandData data, int tick, ClientPlayerEntity player, Form recordingForm) {
      MinecraftClient client = MinecraftClient.getInstance();
      boolean using = player.isUsingItem();
      boolean rejectedConsume = !using
         && client.options.useKey.isPressed()
         && (hasConsumeAction(player.getMainHandStack()) || hasConsumeAction(player.getOffHandStack()));
      if (using) {
         this.suppressRejectedConsumeSwing = false;
      } else if (rejectedConsume) {
         this.suppressRejectedConsumeSwing = true;
      } else if (!player.handSwinging) {
         this.suppressRejectedConsumeSwing = false;
      }

      boolean usingSpyglass = using && (player.getActiveItem().isOf(Items.SPYGLASS) || player.isUsingSpyglass());
      if (usingSpyglass) {
         this.spyglassCooldown = 4;
      } else if (this.spyglassCooldown > 0) {
         this.spyglassCooldown--;
      }

      float rawSwing = this.suppressRejectedConsumeSwing ? 0.0F : clamp(player.getHandSwingProgress(1.0F), 0.0F, 1.0F);
      boolean isOffHand = player.preferredHand == Hand.OFF_HAND;
      boolean isLeftArm = player.getMainArm() == Arm.LEFT && !isOffHand || player.getMainArm() == Arm.RIGHT && isOffHand;
      float rightSwing = player.handSwinging && !isLeftArm ? rawSwing : 0.0F;
      float leftSwing = player.handSwinging && isLeftArm ? rawSwing : 0.0F;
      data.rightSwingProgress.insert((float)tick, rightSwing);
      data.leftSwingProgress.insert((float)tick, leftSwing);
      HeldItemRendererPovAccess held = (HeldItemRendererPovAccess)client.gameRenderer.firstPersonRenderer;
      float mainEquip = clamp(1.0F - held.bbsPov$getEquipProgressMainHand(), 0.0F, 1.0F);
      float offEquip = clamp(1.0F - held.bbsPov$getEquipProgressOffHand(), 0.0F, 1.0F);
      if (usingSpyglass || this.spyglassCooldown > 0 && player.getMainHandStack().isOf(Items.SPYGLASS)) {
         mainEquip = 0.0F;
      }

      if (usingSpyglass || this.spyglassCooldown > 0 && player.getOffHandStack().isOf(Items.SPYGLASS)) {
         offEquip = 0.0F;
      }

      data.mainEquipProgress.insert((float)tick, mainEquip);
      data.offEquipProgress.insert((float)tick, offEquip);
      data.activeHand.insert((float)tick, using ? (player.getActiveHand() == Hand.OFF_HAND ? 2 : 1) : 0);
      data.activeItem.insert((float)tick, using ? player.getActiveItem().copy() : ItemStack.EMPTY);
      data.showUseParticles.insert((float)tick, using);
      data.useTime.insert((float)tick, Math.max(0, player.getItemUseTime()));
      data.bobPhase.insert((float)tick, player.horizontalSpeed);
      data.bobStrength.insert((float)tick, Math.max(0.0F, player.strideDistance));
      data.renderYaw.insert((float)tick, player.renderYaw);
      data.renderPitch.insert((float)tick, player.renderPitch);
      this.lastRecordedTick = tick;
   }

   private static boolean hasConsumeAction(ItemStack stack) {
      UseAction action = stack.getUseAction();
      return action == UseAction.EAT || action == UseAction.DRINK;
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }
}

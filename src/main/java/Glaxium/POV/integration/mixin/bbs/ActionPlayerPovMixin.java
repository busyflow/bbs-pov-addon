package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.playback.PovPlayerProtection;
import mchorse.bbs_mod.actions.ActionPlayer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {ActionPlayer.class},
   remap = false
)
public abstract class ActionPlayerPovMixin {
   @Shadow
   private ServerPlayerEntity serverPlayer;
   @Shadow
   private boolean borrowedEquipment;
   @Unique
   private boolean bbsPov$protectedPlayer;

   @Redirect(
      method = {"<init>"},
      at = @At(
         value = "INVOKE",
         target = "Lmchorse/bbs_mod/actions/ActionPlayer;applyFilmPlayerSettingsTo"
      )
   )
   private void bbsPov$preserveRealHealth(ServerPlayerEntity player, float filmHealth, float filmHunger, int filmXpLevel, float filmXpProgress) {
      float safeHealth = Math.max(1.0F, player.getHealth());
      ActionPlayer.applyFilmPlayerSettingsTo(player, safeHealth, filmHunger, filmXpLevel, filmXpProgress);
   }

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   private void bbsPov$protectBorrowedPlayer(CallbackInfo info) {
      if (this.borrowedEquipment && this.serverPlayer != null) {
         this.serverPlayer.hurtTime = 0;
         this.serverPlayer.maxHurtTime = 0;
         PovPlayerProtection.acquire(this.serverPlayer);
         this.bbsPov$protectedPlayer = true;
      }
   }

   @Inject(
      method = {"stop"},
      at = {@At("RETURN")}
   )
   private void bbsPov$releaseBorrowedPlayer(CallbackInfo info) {
      if (this.bbsPov$protectedPlayer) {
         PovPlayerProtection.release(this.serverPlayer);
         this.bbsPov$protectedPlayer = false;
      }
   }
}

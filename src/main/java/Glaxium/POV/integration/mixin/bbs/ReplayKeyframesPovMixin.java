package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.hud.RecordedHudData;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {ReplayKeyframes.class},
   remap = false
)
public class ReplayKeyframesPovMixin implements ReplayKeyframesPovAccess {
   @Unique
   private RecordedHudData bbsPov$hud;
   @Unique
   private RecordedHandData bbsPov$hand;
   @Unique
   private RecordedPovActions bbsPov$actions;

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   private void bbsPov$addChannels(String id, CallbackInfo info) {
      this.bbsPov$hud = new RecordedHudData();
      this.bbsPov$hud.addTo((ReplayKeyframes)this);
      this.bbsPov$hand = new RecordedHandData();
      this.bbsPov$hand.addTo((ReplayKeyframes)this);
      this.bbsPov$actions = new RecordedPovActions();
      ((ReplayKeyframes)this).add(this.bbsPov$actions);
   }

   @Override
   public RecordedHudData bbsPov$getHud() {
      return this.bbsPov$hud;
   }

   @Override
   public RecordedHandData bbsPov$getHand() {
      return this.bbsPov$hand;
   }

   @Override
   public RecordedPovActions bbsPov$getActions() {
      return this.bbsPov$actions;
   }
}

package Glaxium.POV.actions.clip;

import Glaxium.POV.actions.PovActionType;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public final class SemanticHudPovActionClip extends PovActionClip {
   public final ValueString hudType = new ValueString("hud_type", PovActionType.TOASTS.id);
   public final KeyframeChannel<String> state = this.channel("state", KeyframeFactories.STRING);
   public final KeyframeChannel<Float> opacity = this.channel("opacity", KeyframeFactories.FLOAT);

   public SemanticHudPovActionClip() {
      this(PovActionType.TOASTS);
   }

   public SemanticHudPovActionClip(PovActionType type) {
      this.hudType.set(type == null ? PovActionType.TOASTS.id : type.id);
      this.add(this.hudType);
   }

   @Override
   public PovActionType getActionType() {
      return this.hudType == null ? PovActionType.TOASTS : PovActionType.fromId((String)this.hudType.get());
   }

   @Override
   public void normalize() {
      super.normalize();
      constant(this.state);
      clamp(this.opacity, 0.0F, 1.0F);
   }

   protected Clip create() {
      return new SemanticHudPovActionClip(this.getActionType());
   }
}

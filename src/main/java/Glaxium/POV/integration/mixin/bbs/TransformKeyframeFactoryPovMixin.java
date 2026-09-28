package Glaxium.POV.integration.mixin.bbs;

import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.utils.keyframes.factories.TransformKeyframeFactory;
import mchorse.bbs_mod.utils.pose.Transform;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {TransformKeyframeFactory.class},
   remap = false
)
public abstract class TransformKeyframeFactoryPovMixin {
   @Inject(
      method = {"fromData(Lmchorse/bbs_mod/data/types/BaseType;)Lmchorse/bbs_mod/utils/pose/Transform;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$readLegacyLayout(BaseType data, CallbackInfoReturnable<Transform> info) {
      if (data != null && data.isList()) {
         Vector4f old = DataStorageUtils.vector4fFromData(data.asList());
         Transform transform = new Transform();
         boolean genericDefault = old.z == 0.0F && old.w == 1.0F;
         float scale = genericDefault ? 1.0F : old.z;
         float rotation = genericDefault ? 0.0F : (float)Math.toRadians((double)old.w);
         transform.translate.set(old.x, old.y, 0.0F);
         transform.scale.set(scale, scale, 1.0F);
         transform.rotate.set(0.0F, 0.0F, rotation);
         info.setReturnValue(transform);
      }
   }
}

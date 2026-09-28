package Glaxium.POV.integration.mixin.bbs;

import mchorse.bbs_mod.settings.values.base.BaseValueNumber;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(
   value = {BaseValueNumber.class},
   remap = false
)
public interface BaseValueNumberAccessor {
   @Accessor("max")
   void bbsPov$setMaximum(Number var1);
}

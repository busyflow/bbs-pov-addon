package Glaxium.POV.integration.mixin.bbs;

import java.util.List;
import mchorse.bbs_mod.settings.values.core.ValueList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(
   value = {ValueList.class},
   remap = false
)
public interface ValueListPovAccessor {
   @Accessor("list")
   List bbsPov$getList();
}

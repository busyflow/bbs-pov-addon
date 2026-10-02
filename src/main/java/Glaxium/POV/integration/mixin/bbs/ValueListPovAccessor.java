package Glaxium.POV.integration.mixin.bbs;

import mchorse.bbs_mod.settings.values.core.ValueList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@SuppressWarnings("rawtypes")
@Mixin(value = ValueList.class, remap = false)
public interface ValueListPovAccessor
{
    @Accessor("list")
    List bbsPov$getList();
}

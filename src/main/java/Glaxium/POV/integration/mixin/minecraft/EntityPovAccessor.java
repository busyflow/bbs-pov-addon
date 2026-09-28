package Glaxium.POV.integration.mixin.minecraft;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({Entity.class})
public interface EntityPovAccessor {
   @Accessor("netherPortalTime")
   int bbsPov$getNetherPortalTime();
}

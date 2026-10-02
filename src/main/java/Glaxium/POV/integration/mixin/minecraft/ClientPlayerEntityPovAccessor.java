package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.integration.access.minecraft.ClientPlayerEntityPovAccess;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPlayerEntity.class)
public interface ClientPlayerEntityPovAccessor extends ClientPlayerEntityPovAccess
{
    @Accessor("usingItem") boolean bbsPov$isUsingItem();
    @Accessor("usingItem") void bbsPov$setUsingItem(boolean value);
    @Accessor("activeHand") Hand bbsPov$getClientActiveHand();
    @Accessor("activeHand") void bbsPov$setClientActiveHand(Hand hand);
}

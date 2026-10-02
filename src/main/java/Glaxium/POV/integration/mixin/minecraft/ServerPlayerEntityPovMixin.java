package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.playback.PovPlayerProtection;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Film health is visual data; playback must never damage the actual player. */
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityPovMixin
{
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void bbsPov$cancelPlaybackDamage(
        DamageSource source,
        float amount,
        CallbackInfoReturnable<Boolean> info)
    {
        if (PovPlayerProtection.contains((ServerPlayerEntity) (Object) this))
        {
            info.setReturnValue(false);
        }
    }

    @Inject(method = "getPlayerListName", at = @At("RETURN"), cancellable = true)
    private void bbsPov$getMorphedPlayerListName(CallbackInfoReturnable<net.minecraft.text.Text> cir)
    {
        String replayName = Glaxium.POV.actions.chat.ChatMorphHelper.getActiveReplayName();
        if (replayName != null && !replayName.isEmpty())
        {
            cir.setReturnValue(net.minecraft.text.Text.literal(replayName));
            return;
        }
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        String morphName = Glaxium.POV.actions.chat.ChatMorphHelper.getPlayerMorphName(self);
        if (morphName != null && !morphName.isEmpty())
        {
            cir.setReturnValue(net.minecraft.text.Text.literal(morphName));
        }
    }
}

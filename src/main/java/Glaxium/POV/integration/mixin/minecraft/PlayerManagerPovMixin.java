package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.actions.chat.ChatMorphHelper;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(PlayerManager.class)
public class PlayerManagerPovMixin
{
    @Shadow
    @Final
    private List<ServerPlayerEntity> players;

    @Inject(method = "getPlayer(Ljava/lang/String;)Lnet/minecraft/server/network/ServerPlayerEntity;", at = @At("RETURN"), cancellable = true)
    private void bbsPov$getPlayerByMorphOrReplay(String name, CallbackInfoReturnable<ServerPlayerEntity> cir)
    {
        if (cir.getReturnValue() == null && name != null && !name.isEmpty())
        {
            for (ServerPlayerEntity player : this.players)
            {
                String replayName = ChatMorphHelper.getActiveReplayName();
                if (replayName != null && replayName.equalsIgnoreCase(name))
                {
                    cir.setReturnValue(player);
                    return;
                }
                String morphName = ChatMorphHelper.getPlayerMorphName(player);
                if (morphName != null && morphName.equalsIgnoreCase(name))
                {
                    cir.setReturnValue(player);
                    return;
                }
            }
        }
    }

    @Inject(method = "getPlayerNames", at = @At("RETURN"), cancellable = true)
    private void bbsPov$getPlayerNames(CallbackInfoReturnable<String[]> cir)
    {
        java.util.List<String> result = new java.util.ArrayList<>();
        String replayName = ChatMorphHelper.getActiveReplayName();
        for (ServerPlayerEntity player : this.players)
        {
            if (replayName != null && !replayName.isEmpty())
            {
                result.add(replayName);
            }
            else
            {
                String morphName = ChatMorphHelper.getPlayerMorphName(player);
                if (morphName != null && !morphName.isEmpty())
                {
                    result.add(morphName);
                }
                else
                {
                    result.add(player.getGameProfile().getName());
                }
            }
        }

        if (replayName != null && !replayName.isEmpty())
        {
            for (ServerPlayerEntity player : this.players)
            {
                String morphName = ChatMorphHelper.getPlayerMorphName(player);
                if (morphName != null && !morphName.equalsIgnoreCase(replayName))
                {
                    result.removeIf(n -> n.equalsIgnoreCase(morphName));
                }
            }
        }

        cir.setReturnValue(result.toArray(new String[0]));
    }
}

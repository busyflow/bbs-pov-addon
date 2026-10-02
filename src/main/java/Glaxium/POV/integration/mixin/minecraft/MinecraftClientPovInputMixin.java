package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.playback.PovPlaybackInput;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.Films;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.GameModeSelectionScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SleepingChatScreen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Right-Control replay playback cannot trigger the local player's attack/use actions. */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientPovInputMixin
{
    /** Right-Control playback owns its first-person state. Clear any live use
     * state before Minecraft processes this tick so a physical right click (or
     * a use started immediately before playback) cannot leak into the replay
     * hand between our render-time apply/restore calls. */
    @Inject(method = "tick", at = @At("HEAD"))
    private void bbsPov$detachLiveUseState(CallbackInfo info)
    {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (!PovPlaybackInput.isFirstPersonPlayback())
        {
            return;
        }

        if (client.currentScreen instanceof HandledScreen<?>
            || client.currentScreen instanceof BookScreen
            || client.currentScreen instanceof BookEditScreen
            || client.currentScreen instanceof GameModeSelectionScreen
            || client.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen)
        {
            client.setScreen(null);
        }

        ClientPlayerEntity player = client.player;

        client.options.useKey.setPressed(false);
        client.options.attackKey.setPressed(false);
        client.options.forwardKey.setPressed(false);
        client.options.backKey.setPressed(false);
        client.options.leftKey.setPressed(false);
        client.options.rightKey.setPressed(false);
        client.options.jumpKey.setPressed(false);
        client.options.sneakKey.setPressed(false);
        client.options.sprintKey.setPressed(false);
        client.options.inventoryKey.setPressed(false);
        client.options.chatKey.setPressed(false);
        client.options.commandKey.setPressed(false);
        client.options.pickItemKey.setPressed(false);
        client.options.dropKey.setPressed(false);
        client.options.swapHandsKey.setPressed(false);

        if (player != null)
        {
            ((ClientPlayerEntityPovAccessor) player).bbsPov$setUsingItem(false);
            ((ClientPlayerEntityPovAccessor) player).bbsPov$setClientActiveHand(Hand.MAIN_HAND);
            ((LivingEntityPovAccessor) player).bbsPov$setActiveItemStack(ItemStack.EMPTY);
            ((LivingEntityPovAccessor) player).bbsPov$setItemUseTimeLeft(0);
        }
    }

    /**
     * BBS only calls {@code Films.update()} when the client is not paused.
     * Books (and every other vanilla container) pause singleplayer, so the
     * recorder runs once on open and then freezes at a 1-tick GUI clip.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void bbsPov$keepRecordingOpenGuis(CallbackInfo info)
    {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (!client.isPaused())
        {
            return;
        }

        Screen screen = client.currentScreen;
        if (!(screen instanceof BookScreen
            || screen instanceof BookEditScreen
            || screen instanceof HandledScreen<?>
            || screen instanceof GameModeSelectionScreen
            || screen instanceof GameMenuScreen
            || screen instanceof SleepingChatScreen))
        {
            return;
        }

        Films films = BBSModClient.getFilms();
        if (films != null && films.getRecorder() != null)
        {
            films.update();
        }
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void bbsPov$blockAttack(CallbackInfoReturnable<Boolean> info)
    {
        if (PovPlaybackInput.isFirstPersonPlayback())
        {
            info.setReturnValue(false);
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void bbsPov$blockUse(CallbackInfo info)
    {
        if (PovPlaybackInput.isFirstPersonPlayback())
        {
            info.cancel();
        }
    }
}

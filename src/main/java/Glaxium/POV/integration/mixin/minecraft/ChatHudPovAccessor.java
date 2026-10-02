package Glaxium.POV.integration.mixin.minecraft;

import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.hud.ChatHudLine;
import java.util.List;

@Mixin(ChatHud.class)
public interface ChatHudPovAccessor
{
    @Accessor("scrolledLines")
    int bbsPov$getScrolledLines();

    @Accessor("visibleMessages")
    List<ChatHudLine.Visible> bbsPov$getVisibleMessages();
}

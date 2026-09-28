package Glaxium.POV.integration.mixin.minecraft;

import java.util.List;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine.Visible;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ChatHud.class})
public interface ChatHudPovAccessor {
   @Accessor("scrolledLines")
   int bbsPov$getScrolledLines();

   @Accessor("visibleMessages")
   List<Visible> bbsPov$getVisibleMessages();
}

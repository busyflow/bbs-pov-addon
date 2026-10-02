package Glaxium.POV.integration.mixin.minecraft;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChatScreen.class)
public interface ChatScreenPovAccessor
{
    @Accessor("chatField")
    TextFieldWidget bbsPov$getChatField();
}

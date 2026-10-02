package Glaxium.POV.integration.mixin.minecraft;

import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TexturedButtonWidget.class)
public interface TexturedButtonWidgetPovAccessor
{
    @Accessor("textures")
    ButtonTextures bbsPov$getTextures();
}

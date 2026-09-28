package Glaxium.POV.integration.mixin.minecraft;

import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({TextFieldWidget.class})
public interface TextFieldWidgetPovAccessor {
   @Accessor("selectionStart")
   int bbsPov$getSelectionStart();

   @Accessor("selectionEnd")
   int bbsPov$getSelectionEnd();
}

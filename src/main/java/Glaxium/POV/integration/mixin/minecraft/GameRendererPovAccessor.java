package Glaxium.POV.integration.mixin.minecraft;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({GameRenderer.class})
public interface GameRendererPovAccessor {
   @Invoker("loadPostProcessor")
   void bbsPov$loadPostProcessor(Identifier var1);
}

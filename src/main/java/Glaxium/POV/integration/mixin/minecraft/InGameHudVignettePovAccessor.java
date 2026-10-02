package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.integration.access.minecraft.InGameHudVignettePovAccess;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(InGameHud.class)
public interface InGameHudVignettePovAccessor extends InGameHudVignettePovAccess
{
    @Invoker("renderVignetteOverlay")
    void bbsPov$renderVignetteOverlay(DrawContext context, Entity entity);
}

package Glaxium.POV.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** Vanilla-style inverted Film/playback crosshair. */
public final class PovCrosshairRenderer
{
    private static final Identifier CROSSHAIR_TEXTURE = new Identifier("textures/gui/icons.png");

    private PovCrosshairRenderer()
    {
    }

    public static void render(Batcher2D batcher, int width, int height)
    {
        render(batcher, width, height, 1.0F);
    }

    public static void render(Batcher2D batcher, int width, int height, float attackCooldownProgress)
    {
        /* This is Minecraft's normal crosshair path: the 15x15 GUI sprite
         * and destination-color inversion blend. */
        DrawContext context = batcher.getContext();

        batcher.flush();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.blendFuncSeparate(
            GlStateManager.SrcFactor.ONE_MINUS_DST_COLOR,
            GlStateManager.DstFactor.ONE_MINUS_SRC_COLOR,
            GlStateManager.SrcFactor.ZERO,
            GlStateManager.DstFactor.ONE);

        int cx = width / 2;
        int cy = height / 2;
        context.drawTexture(CROSSHAIR_TEXTURE, cx - 7, cy - 7, 0, 0, 15, 15);

        /* Vanilla attack indicator below crosshair when cooldown is charging (< 1.0F) */
        if (attackCooldownProgress >= 0F && attackCooldownProgress < 1.0F
            && Glaxium.POV.hud.render.AttackIndicatorRenderer.shouldDraw())
        {
            int k = cx - 8;
            int j = cy - 7 + 16;
            int l = (int) (attackCooldownProgress * 17.0F);

            context.drawTexture(CROSSHAIR_TEXTURE, k, j, 36, 94, 16, 4);
            context.drawTexture(CROSSHAIR_TEXTURE, k, j, 52, 94, l, 4);
        }
        context.draw();

        RenderSystem.blendFuncSeparate(
            GlStateManager.SrcFactor.SRC_ALPHA,
            GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SrcFactor.ZERO,
            GlStateManager.DstFactor.ONE);
    }
}

package Glaxium.POV.render;

import Glaxium.POV.hud.render.AttackIndicatorRenderer;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** Vanilla-style inverted Film/playback crosshair. */
public final class PovCrosshairRenderer
{
    private static final Identifier CROSSHAIR_TEXTURE = new Identifier("hud/crosshair");
    private static final Identifier ATTACK_BACKGROUND =
        new Identifier("hud/crosshair_attack_indicator_background");
    private static final Identifier ATTACK_PROGRESS =
        new Identifier("hud/crosshair_attack_indicator_progress");

    private PovCrosshairRenderer()
    {
    }

    public static void render(Batcher2D batcher, int width, int height)
    {
        render(batcher, width, height, 1F);
    }

    public static void render(Batcher2D batcher, int width, int height, float attackCooldown)
    {
        /* This is Minecraft 1.20.4's normal (non-debug) crosshair path: the
         * native 15x15 GUI sprite and destination-color inversion blend. */
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

        context.drawGuiTexture(
            CROSSHAIR_TEXTURE,
            (width - 15) / 2,
            (height - 15) / 2,
            15,
            15);

        if (AttackIndicatorRenderer.shouldDraw() && attackCooldown >= 0F && attackCooldown < 1F)
        {
            int x = width / 2 - 8;
            int y = height / 2 - 7 + 16;
            int fill = (int) (attackCooldown * 17.0F);
            context.drawGuiTexture(ATTACK_BACKGROUND, x, y, 16, 4);
            context.drawGuiTexture(ATTACK_PROGRESS, 16, 4, 0, 0, x, y, fill, 4);
        }
        context.draw();

        RenderSystem.blendFuncSeparate(
            GlStateManager.SrcFactor.SRC_ALPHA,
            GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SrcFactor.ZERO,
            GlStateManager.DstFactor.ONE);
    }
}

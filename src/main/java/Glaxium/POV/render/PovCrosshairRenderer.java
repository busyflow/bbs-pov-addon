package Glaxium.POV.render;

import Glaxium.POV.hud.render.AttackIndicatorRenderer;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public final class PovCrosshairRenderer {
   private static final Identifier CROSSHAIR_TEXTURE = new Identifier("textures/gui/icons.png");

   private PovCrosshairRenderer() {
   }

   public static void render(Batcher2D batcher, int width, int height) {
      render(batcher, width, height, 1.0F);
   }

   public static void render(Batcher2D batcher, int width, int height, float attackCooldownProgress) {
      DrawContext context = batcher.getContext();
      batcher.flush();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.enableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.blendFuncSeparate(SrcFactor.ONE_MINUS_DST_COLOR, DstFactor.ONE_MINUS_SRC_COLOR, SrcFactor.ONE, DstFactor.ZERO);
      int cx = width / 2;
      int cy = height / 2;
      context.drawTexture(CROSSHAIR_TEXTURE, cx - 7, cy - 7, 0, 0, 15, 15);
      if (attackCooldownProgress >= 0.0F && attackCooldownProgress < 1.0F && AttackIndicatorRenderer.shouldDraw()) {
         int k = cx - 8;
         int j = cy - 7 + 16;
         int l = (int)(attackCooldownProgress * 17.0F);
         context.drawTexture(CROSSHAIR_TEXTURE, k, j, 36, 94, 16, 4);
         context.drawTexture(CROSSHAIR_TEXTURE, k, j, 52, 94, l, 4);
      }

      context.draw();
      RenderSystem.defaultBlendFunc();
   }
}

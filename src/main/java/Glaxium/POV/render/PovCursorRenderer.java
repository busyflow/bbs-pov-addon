package Glaxium.POV.render;

import Glaxium.POV.actions.gui.render.common.GuiSlotRenderer;
import Glaxium.POV.config.PovSettings;
import Glaxium.POV.hud.HudState;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.joml.Matrix4f;

public final class PovCursorRenderer {
   private PovCursorRenderer() {
   }

   public static void render(Batcher2D batcher, HudState state, int width, int height) {
      if (state != null && state.cursorVisible) {
         float screenCenterX = (float)width / 2.0F;
         float screenCenterY = (float)height / 2.0F;
         float curScreenX = screenCenterX + state.cursorLayout.translate.x * 2.0F;
         float curScreenY = screenCenterY - state.cursorLayout.translate.y * 2.0F;
         float scaleX = Math.max(0.001F, state.cursorLayout.scale.x);
         float scaleY = Math.max(0.001F, state.cursorLayout.scale.y);
         ItemStack cursorStack = state.cursorItem;
         if (cursorStack != null && !cursorStack.isEmpty()) {
            MatrixStack guiMatrices = batcher.getContext().getMatrices();
            MatrixStack modelView = RenderSystem.getModelViewStack();
            Matrix4f previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
            guiMatrices.push();
            modelView.push();

            try {
               RenderSystem.enableDepthTest();
               RenderSystem.depthFunc(515);
               RenderSystem.depthMask(true);
               RenderSystem.clearDepth(1.0);
               RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
               DiffuseLighting.enableGuiDepthLighting();
               GuiSlotRenderer.drawSlotItem(batcher, cursorStack, (int)(curScreenX - 8.0F), (int)(curScreenY - 8.0F));
               batcher.flush();
            } finally {
               DiffuseLighting.disableGuiDepthLighting();
               modelView.pop();
               RenderSystem.applyModelViewMatrix();
               guiMatrices.pop();
               RenderSystem.setProjectionMatrix(previousProjection, VertexSorter.BY_Z);
               RenderSystem.colorMask(true, true, true, true);
               RenderSystem.depthFunc(515);
               RenderSystem.clearDepth(1.0);
               RenderSystem.enableCull();
               RenderSystem.disableDepthTest();
               RenderSystem.depthMask(false);
            }
         }

         batcher.flush();
         RenderSystem.disableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         MatrixStack matrices = batcher.getContext().getMatrices();
         matrices.push();
         matrices.translate(curScreenX, curScreenY, 0.0F);
         float defaultScale = PovSettings.getCursorDefaultScale();
         matrices.scale(scaleX * defaultScale, scaleY * defaultScale, 1.0F);
         PovSettings.renderCursor(batcher);
         batcher.flush();
         matrices.pop();
      }
   }
}

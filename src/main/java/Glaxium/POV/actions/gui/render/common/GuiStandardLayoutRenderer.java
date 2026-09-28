package Glaxium.POV.actions.gui.render.common;

import Glaxium.POV.actions.gui.GuiTypeEntry;
import Glaxium.POV.actions.gui.render.GuiPointerHover;
import Glaxium.POV.actions.gui.render.GuiRenderContext;
import Glaxium.POV.actions.gui.render.GuiScreenChrome;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

public final class GuiStandardLayoutRenderer {
   private GuiStandardLayoutRenderer() {
   }

   public static void render(GuiRenderContext ctx, GuiScreenChrome chrome) {
      if (chrome == null) {
         chrome = GuiScreenChrome.EMPTY;
      }

      MatrixStack matrices = ctx.matrices;
      Batcher2D batcher = ctx.batcher;
      float globalTick = ctx.globalTick;
      float localTick = ctx.localTick;
      int screenWidth = ctx.screenWidth;
      int screenHeight = ctx.screenHeight;
      GuiTypeEntry entry = ctx.entry;
      String guiId = ctx.guiId;
      float scaleX = ctx.scaleX;
      float scaleY = ctx.scaleY;
      float opacity = ctx.opacity;
      float bgOpacity = ctx.bgOpacity;
      float originX = ctx.originX;
      float originY = ctx.originY;
      boolean recipeOpen = ctx.recipeOpen;
      boolean cursorVisible = ctx.cursorVisible;
      float cursorGuiX = ctx.cursorGuiX;
      float cursorGuiY = ctx.cursorGuiY;
      GuiPointerHover pointerHover = ctx.hover;
      int topAlpha = (int)(192.0F * opacity * bgOpacity);
      int bottomAlpha = (int)(208.0F * opacity * bgOpacity);
      if (!ctx.skipEntityPreview && bottomAlpha > 0) {
         int topColor = topAlpha << 24 | 1052688;
         int bottomColor = bottomAlpha << 24 | 1052688;
         batcher.flush();
         RenderSystem.disableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         batcher.getContext().fillGradient(RenderLayer.getGuiOverlay(), 0, 0, screenWidth, screenHeight, topColor, bottomColor, 0);
         batcher.flush();
      }

      batcher.flush();
      matrices.push();
      matrices.translate(originX, originY, 0.0F);
      matrices.scale(scaleX, scaleY, 1.0F);
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      batcher.getContext().setShaderColor(1.0F, 1.0F, 1.0F, opacity);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, opacity);
      chrome.drawBackground(ctx);
      chrome.drawEarlyChrome(ctx);
      if (recipeOpen) {
         GuiRecipeBookRenderer.render(
            batcher,
            ctx.clip,
            ctx.replayKeyframes,
            guiId,
            localTick,
            globalTick,
            originX,
            originY,
            scaleX,
            scaleY,
            screenWidth,
            screenHeight,
            opacity,
            cursorVisible ? cursorGuiX : -1000.0F,
            cursorVisible ? cursorGuiY : -1000.0F,
            pointerHover
         );
      }

      batcher.getContext().draw();
      int titleColor = (int)(255.0F * opacity) << 24 | 4210752;
      boolean drewTitle = false;
      if (entry.title != null && !entry.title.isBlank() && !"villager".equals(entry.id)) {
         int tx = entry.titleX;
         if (tx < 0) {
            tx = (entry.regionWidth - MinecraftClient.getInstance().textRenderer.getWidth(entry.title)) / 2;
         }

         batcher.text(entry.title, (float)tx, (float)entry.titleY, titleColor, false);
         drewTitle = true;
      }

      if (entry.inventoryTitleX >= 0) {
         batcher.text("Inventory", (float)entry.inventoryTitleX, (float)entry.inventoryTitleY, titleColor, false);
         drewTitle = true;
      }

      if (drewTitle) {
         batcher.flush();
      }

      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(true);
      RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
      DiffuseLighting.enableGuiDepthLighting();
      ItemStack hoveredStack = chrome.renderItems(ctx);
      chrome.drawLateItems(ctx);
      if (recipeOpen) {
         GuiRecipeBookRenderer.renderItems(
            batcher,
            ctx.clip,
            ctx.replayKeyframes,
            guiId,
            localTick,
            globalTick,
            originX,
            originY,
            scaleX,
            scaleY,
            screenWidth,
            screenHeight,
            cursorVisible ? cursorGuiX : -1000.0F,
            cursorVisible ? cursorGuiY : -1000.0F,
            pointerHover
         );
      }

      if (recipeOpen) {
         ItemStack ghostHover = GuiRecipeBookRenderer.renderGhosts(
            batcher,
            ctx.clip,
            ctx.replayKeyframes,
            guiId,
            localTick,
            globalTick,
            opacity,
            cursorVisible ? cursorGuiX : -1000.0F,
            cursorVisible ? cursorGuiY : -1000.0F
         );
         if (pointerHover.item == null && ghostHover != null && !ghostHover.isEmpty()) {
            pointerHover.item = ghostHover;
            pointerHover.itemGroup = false;
         }
      }

      if (pointerHover.item != null && !pointerHover.item.isEmpty()) {
         hoveredStack = pointerHover.item;
         pointerHover.itemGroup = false;
      }

      batcher.flush();
      DiffuseLighting.disableGuiDepthLighting();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      matrices.pop();
      chrome.drawPreview(ctx);
      GuiCursorRenderer.renderTooltips(ctx, hoveredStack);
      batcher.getContext().setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }
}

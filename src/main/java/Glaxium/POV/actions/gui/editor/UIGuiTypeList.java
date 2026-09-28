package Glaxium.POV.actions.gui.editor;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.GuiTypeEntry;
import Glaxium.POV.actions.gui.render.GuiActionRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import java.util.function.Consumer;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

public class UIGuiTypeList extends UIList<GuiTypeEntry> {
   public static final int ITEM_HEIGHT = 48;
   private static final GuiPovActionClip PREVIEW_CLIP = new GuiPovActionClip();

   public UIGuiTypeList(Consumer<List<GuiTypeEntry>> callback) {
      super(callback);
      this.scroll.scrollItemSize = 48;
   }

   private static <T> void setChannel(KeyframeChannel<T> channel, T value) {
      if (channel != null) {
         if (channel.isEmpty()) {
            channel.insert(0.0F, value);
         } else {
            ((Keyframe)channel.getKeyframes().get(0)).setValue(value);
         }
      }
   }

   protected String elementToString(UIContext context, int index, GuiTypeEntry element) {
      return element == null ? "" : element.name + " " + element.id + " " + element.category;
   }

   protected void renderElementPart(UIContext context, GuiTypeEntry element, int i, int x, int y, boolean hover, boolean selected) {
      if (element != null) {
         int boxX = x + 4;
         int boxY = y + 3;
         int boxW = 60;
         int boxH = 42;
         context.batcher.box((float)boxX, (float)boxY, (float)(boxX + boxW), (float)(boxY + boxH), -2013265920);
         context.batcher.outline((float)boxX, (float)boxY, (float)(boxX + boxW), (float)(boxY + boxH), selected ? -855638017 : 1157627903);
         this.renderPreview(context, element, boxX, boxY, boxW, boxH);
         int iconX = boxX + boxW + 10;
         int iconY = y + 13;
         float iconScale = 1.35F;
         ItemStack itemIcon = element.getIcon();
         if (itemIcon != null && !itemIcon.isEmpty()) {
            try {
               context.batcher.flush();
               MatrixStack matrices = context.batcher.getContext().getMatrices();
               matrices.push();
               matrices.translate((float)iconX, (float)iconY, 100.0F);
               matrices.scale(iconScale, iconScale, 1.0F);
               RenderSystem.enableDepthTest();
               RenderSystem.depthMask(true);
               RenderSystem.depthFunc(515);
               RenderSystem.enableCull();
               RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
               DiffuseLighting.enableGuiDepthLighting();
               context.batcher.getContext().drawItem(itemIcon, 0, 0);
               context.batcher.getContext().draw();
               DiffuseLighting.disableGuiDepthLighting();
               RenderSystem.disableDepthTest();
               RenderSystem.depthMask(false);
               RenderSystem.disableCull();
               matrices.pop();
            } catch (Exception var27) {
            }
         }

         int textX = iconX + 28;
         TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
         int titleColor = selected ? 16777215 : (hover ? 16773290 : 16777215);
         context.batcher.textShadow(element.name, (float)textX, (float)(y + 10), titleColor);
         String catText = element.category != null ? element.category : "GUI";
         int catColor = GuiTypeEntry.getCategoryColor(element.category);
         int nameWidth = textRenderer.getWidth(element.name);
         int badgeX = textX + nameWidth + 6;
         int badgeY = y + 9;
         int badgeW = textRenderer.getWidth(catText) + 6;
         int badgeH = 10;
         context.batcher.box((float)badgeX, (float)badgeY, (float)(badgeX + badgeW), (float)(badgeY + badgeH), catColor & 16777215 | 855638016);
         context.batcher.outline((float)badgeX, (float)badgeY, (float)(badgeX + badgeW), (float)(badgeY + badgeH), catColor & 16777215 | -1728053248);
         context.batcher.text(catText, (float)(badgeX + 3), (float)(badgeY + 1), catColor);
         int subtitleColor = selected ? 13691135 : (hover ? 11579568 : 8947848);
         context.batcher.textShadow("minecraft:" + element.id, (float)textX, (float)(y + 25), subtitleColor);
      }
   }

   private void renderPreview(UIContext context, GuiTypeEntry element, int boxX, int boxY, int boxW, int boxH) {
      int innerPad = 2;
      int maxW = boxW - innerPad * 2;
      int maxH = boxH - innerPad * 2;
      int regW = Math.max(1, element.regionWidth);
      int regH = Math.max(1, element.regionHeight);
      if ("creative_inventory".equals(element.id)) {
         regW = 195;
         regH = 192;
      }

      float scale = Math.min((float)maxW / (float)regW, (float)maxH / (float)regH);
      int drawW = Math.max(1, Math.round((float)regW * scale));
      int drawH = Math.max(1, Math.round((float)regH * scale));
      int drawX = boxX + innerPad + (maxW - drawW) / 2;
      int drawY = boxY + innerPad + (maxH - drawH) / 2;
      setChannel(PREVIEW_CLIP.state, element.id);
      setChannel(PREVIEW_CLIP.getDarknessOpacity(element.id), 0.0F);
      setChannel(PREVIEW_CLIP.getCursorVisible(element.id), false);
      setChannel(PREVIEW_CLIP.getOpacity(element.id), 1.0F);
      setChannel(PREVIEW_CLIP.getFurnaceLit(element.id), 0.0F);
      setChannel(PREVIEW_CLIP.getFurnaceCook(element.id), 0.0F);
      setChannel(PREVIEW_CLIP.getBrewProgress(element.id), 0.0F);
      setChannel(PREVIEW_CLIP.getBrewFuel(element.id), 0.0F);
      setChannel(PREVIEW_CLIP.getBrewBubbles(element.id), false);
      setChannel(PREVIEW_CLIP.anvilName, "");
      setChannel(PREVIEW_CLIP.getLayout(element.id), new Transform());
      MatrixStack matrices = context.batcher.getContext().getMatrices();
      matrices.push();
      matrices.translate((float)drawX + (float)drawW / 2.0F, (float)drawY + (float)drawH / 2.0F, 0.0F);
      matrices.scale(scale, scale, 1.0F);
      matrices.translate((float)(-regW) / 2.0F, (float)(-regH) / 2.0F, 0.0F);
      context.batcher.flush();
      context.batcher.getContext().enableScissor(boxX, boxY, boxX + boxW, boxY + boxH);
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(true);
      RenderSystem.depthFunc(515);
      RenderSystem.enableCull();
      RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);

      try {
         GuiActionRenderer.renderGuiClip(matrices, context.batcher, null, PREVIEW_CLIP, null, 0.0F, regW, regH, false, true);
         context.batcher.getContext().draw();
      } catch (Exception var22) {
      } finally {
         DiffuseLighting.disableGuiDepthLighting();
         RenderSystem.disableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.disableCull();
         context.batcher.flush();
         context.batcher.getContext().disableScissor();
         matrices.pop();
      }
   }

   static {
      PREVIEW_CLIP.tick.set(0);
      PREVIEW_CLIP.duration.set(100);
      PREVIEW_CLIP.enchantBookOpen.insert(0.0F, 0.0F);
      PREVIEW_CLIP.enchantPlayerLevel.insert(0.0F, 0);
      PREVIEW_CLIP.enchantSeed.insert(0.0F, 0);
      PREVIEW_CLIP.enchantOffers.insert(0.0F, "");
      PREVIEW_CLIP.gamemodeSelection.insert(0.0F, 0);
      PREVIEW_CLIP.creativeTab.insert(0.0F, 0);
      PREVIEW_CLIP.creativePage.insert(0.0F, 0);
      PREVIEW_CLIP.creativeScroll.insert(0.0F, 0.0F);
      PREVIEW_CLIP.creativeRow.insert(0.0F, 0);
      PREVIEW_CLIP.anvilName.insert(0.0F, "");
   }
}

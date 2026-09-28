package Glaxium.POV.actions.toast.render;

import Glaxium.POV.actions.PovActionType;
import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.actions.clip.PovActionClip;
import Glaxium.POV.actions.clip.ToastPovActionClip;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

public final class ToastActionRenderer {
   private static final Identifier TOASTS_TEXTURE = new Identifier("textures/gui/toasts.png");

   private ToastActionRenderer() {
   }

   public static void renderHUD(Batcher2D batcher, RecordedPovActions actions, float tick, int width, int height) {
      if (actions != null) {
         DrawContext context = batcher.getContext();
         if (context != null) {
            List<ToastPovActionClip> activeClips = new ArrayList<>();

            for (PovActionClip clip : actions.getClips(ToastPovActionClip.class)) {
               if (clip instanceof ToastPovActionClip) {
                  ToastPovActionClip toastClip = (ToastPovActionClip)clip;
                  if (toastClip.isActive(tick)) {
                     activeClips.add(toastClip);
                  }
               }
            }

            if (!activeClips.isEmpty()) {
               TextRenderer font = MinecraftClient.getInstance().textRenderer;
               int baseLayer = PovActionType.TOASTS.seedLayer();

               for (ToastPovActionClip clipx : activeClips) {
                  float elapsed = tick - (float)((Integer)clipx.tick.get()).intValue();
                  int duration = (Integer)clipx.duration.get();
                  if (!(elapsed < 0.0F) && !(elapsed > (float)duration)) {
                     float slide = calculateSlideProgress(elapsed, duration);
                     if (!(slide <= 0.001F)) {
                        int toastW = 160;
                        int toastH = 32;
                        int x = width - (int)((float)toastW * slide);
                        int slot = Math.max(0, Math.min(4, (Integer)clipx.layer.get() - baseLayer));
                        int y = 4 + slot * 34;
                        renderSingleToast(context, font, clipx, x, y);
                     }
                  }
               }
            }
         }
      }
   }

   public static float calculateSlideProgress(float elapsed, int duration) {
      if (duration <= 0) {
         return 0.0F;
      } else {
         int slideIn = Math.max(4, Math.min(15, Math.round((float)duration * 0.15F)));
         int slideOut = Math.max(4, Math.min(15, Math.round((float)duration * 0.15F)));
         if (duration < slideIn + slideOut) {
            slideIn = duration / 2;
            slideOut = duration - slideIn;
         }

         if (elapsed < (float)slideIn) {
            float p = elapsed / (float)Math.max(1, slideIn);
            return MathHelper.clamp(p * p, 0.0F, 1.0F);
         } else if (elapsed > (float)(duration - slideOut)) {
            float p = ((float)duration - elapsed) / (float)Math.max(1, slideOut);
            return MathHelper.clamp(p * p, 0.0F, 1.0F);
         } else {
            return 1.0F;
         }
      }
   }

   public static void renderSingleToast(DrawContext context, TextRenderer font, ToastPovActionClip clip, int x, int y) {
      String frameType = clip.getEffectiveFrameType();
      int vOffset = getVOffsetForFrameType(frameType);
      RenderSystem.enableBlend();
      context.drawTexture(TOASTS_TEXTURE, x, y, 0, vOffset, 160, 32);
      Link customTex = clip.getCustomTexture();
      if (customTex != null) {
         Texture tex = BBSModClient.getTextures().getTexture(customTex);
         if (tex != null && tex.isValid() && tex.id > 0) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderTexture(0, tex.id);
            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
            BufferBuilder builder = Tessellator.getInstance().getBuffer();
            builder.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
            builder.vertex(matrix, (float)(x + 8), (float)(y + 8 + 16), 0.0F).texture(0.0F, 1.0F).next();
            builder.vertex(matrix, (float)(x + 8 + 16), (float)(y + 8 + 16), 0.0F).texture(1.0F, 1.0F).next();
            builder.vertex(matrix, (float)(x + 8 + 16), (float)(y + 8), 0.0F).texture(1.0F, 0.0F).next();
            builder.vertex(matrix, (float)(x + 8), (float)(y + 8), 0.0F).texture(0.0F, 0.0F).next();
            BufferRenderer.drawWithGlobalProgram(builder.end());
         }
      } else {
         String iconId = clip.getEffectiveIcon();
         ItemStack stack = createItemStack(iconId);
         if ("recipe".equalsIgnoreCase(frameType)) {
            context.getMatrices().push();
            context.getMatrices().translate((float)x, (float)y, 0.0F);
            context.getMatrices().scale(0.6F, 0.6F, 1.0F);
            renderItem(context, new ItemStack(Items.CRAFTING_TABLE), 3, 3);
            context.getMatrices().pop();
         }

         renderItem(context, stack, x + 8, y + 8);
      }

      String title = clip.getEffectiveTitle();
      String desc = clip.getEffectiveDescription();
      int titleColor = getTitleColor(frameType);
      int descColor = "recipe".equalsIgnoreCase(frameType) ? -16777216 : -1;
      context.drawText(font, title, x + 30, y + 7, titleColor, false);
      context.drawText(font, desc, x + 30, y + 18, descColor, false);
   }

   private static void renderItem(DrawContext context, ItemStack stack, int x, int y) {
      if (stack != null && !stack.isEmpty()) {
         RenderSystem.enableDepthTest();
         RenderSystem.depthFunc(515);
         RenderSystem.depthMask(true);
         context.drawItem(stack, x, y);
         RenderSystem.disableDepthTest();
      }
   }

   private static int getVOffsetForFrameType(String frameType) {
      if ("recipe".equalsIgnoreCase(frameType) || "challenge".equalsIgnoreCase(frameType)) {
         return 32;
      } else if ("system".equalsIgnoreCase(frameType)) {
         return 64;
      } else {
         return "tutorial".equalsIgnoreCase(frameType) ? 96 : 0;
      }
   }

   private static int getTitleColor(String frameType) {
      if ("challenge".equalsIgnoreCase(frameType)) {
         return -43521;
      } else if ("goal".equalsIgnoreCase(frameType)) {
         return -11141121;
      } else if ("recipe".equalsIgnoreCase(frameType)) {
         return -11534256;
      } else {
         return "system".equalsIgnoreCase(frameType) ? -1 : -171;
      }
   }

   private static ItemStack createItemStack(String iconId) {
      if (iconId != null && !iconId.isEmpty()) {
         try {
            Item item = (Item)Registries.ITEM.get(new Identifier(iconId));
            if (item != null && item != Items.AIR) {
               return new ItemStack(item);
            }
         } catch (Exception var2) {
         }
      }

      return new ItemStack(Items.DIAMOND);
   }
}

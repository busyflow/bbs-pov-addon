package Glaxium.POV.actions.statuseffect.render;

import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.clip.StatusEffectsPovActionClip;
import Glaxium.POV.actions.statuseffect.StatusEffectEntry;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.StatusEffectSpriteManager;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class StatusEffectActionRenderer {
   private static final Identifier INVENTORY_TEXTURE = new Identifier("textures/gui/container/inventory.png");

   private StatusEffectActionRenderer() {
   }

   public static void renderHUD(Batcher2D batcher, RecordedPovActions actions, float tick, int width, int height) {
      if (actions != null) {
         GuiPovActionClip gui = actions.getActiveGui(tick);
         if (gui != null) {
            String state = gui.state.isEmpty() ? "inventory" : (String)gui.state.interpolate(gui.getLocalTick(tick), "inventory");
            if ("inventory".equals(state) || "creative_inventory".equals(state)) {
               return;
            }
         }

         StatusEffectsPovActionClip clip = actions.getActiveStatusEffects(tick);
         if (clip != null && !clip.getEffects().isEmpty()) {
            DrawContext context = batcher.getContext();
            if (context != null) {
               float localTick = clip.getLocalTick(tick);
               renderHudEffects(context, clip.getEffects(), localTick, width);
            }
         }
      }
   }

   public static void renderLiveHUD(Batcher2D batcher, int width, int height) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null) {
         ;
      }
   }

   private static void renderHudEffects(DrawContext context, List<StatusEffectEntry> effects, float elapsedTicks, int screenWidth) {
      MinecraftClient client = MinecraftClient.getInstance();
      StatusEffectSpriteManager spriteManager = client.getStatusEffectSpriteManager();
      int x = screenWidth - 25;
      int y = 1;
      int count = 0;

      for (StatusEffectEntry entry : effects) {
         if (!entry.isExpired(elapsedTicks)) {
            StatusEffect effect = entry.getStatusEffect();
            if (effect != null) {
               Sprite sprite = spriteManager.getSprite(effect);
               if (sprite != null) {
                  float alpha = 1.0F;
                  if (!entry.isUnlimited()) {
                     int remainingTicks = (int)((float)entry.getDurationSeconds() * 20.0F - elapsedTicks);
                     if (remainingTicks <= 200) {
                        int m = Math.max(0, remainingTicks);
                        int n = 10 - m / 20;
                        alpha = MathHelper.clamp((float)m / 10.0F / 20.0F * 0.5F, 0.0F, 0.5F)
                           + MathHelper.cos((float)m * (float) Math.PI / 5.0F) * MathHelper.clamp((float)n / 10.0F * 0.25F, 0.0F, 0.25F);
                        alpha = MathHelper.clamp(alpha, 0.0F, 1.0F);
                     }
                  }

                  RenderSystem.enableBlend();
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                  context.drawTexture(INVENTORY_TEXTURE, x, y, 141, 166, 24, 24);
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
                  context.drawSprite(x + 3, y + 3, 0, 18, 18, sprite);
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                  x -= 25;
                  if (++count % 10 == 0) {
                     x = screenWidth - 25;
                     y += 26;
                  }
               }
            }
         }
      }
   }

   public static void renderInventorySidebar(DrawContext context, RecordedPovActions actions, float tick, int guiLeft, int guiTop, int guiWidth, int guiHeight) {
      if (actions != null) {
         StatusEffectsPovActionClip clip = actions.getActiveStatusEffects(tick);
         if (clip != null && !clip.getEffects().isEmpty()) {
            float localTick = clip.getLocalTick(tick);
            if (!(localTick < 0.0F)) {
               List<StatusEffectEntry> effects = clip.getEffects();
               MinecraftClient client = MinecraftClient.getInstance();
               StatusEffectSpriteManager spriteManager = client.getStatusEffectSpriteManager();
               TextRenderer textRenderer = client.textRenderer;
               int startX = guiLeft + guiWidth + 2;
               int startY = guiTop;
               int cardWidth = 120;
               int cardHeight = 32;

               for (StatusEffectEntry entry : effects) {
                  if (!entry.isExpired(localTick)) {
                     StatusEffect effect = entry.getStatusEffect();
                     if (effect != null) {
                        context.drawTexture(INVENTORY_TEXTURE, startX, startY, 0, 166, cardWidth, cardHeight);
                        Sprite sprite = spriteManager.getSprite(effect);
                        if (sprite != null) {
                           RenderSystem.enableBlend();
                           context.drawSprite(startX + 6, startY + 7, 0, 18, 18, sprite);
                        }

                        String name = entry.getDisplayName();
                        if (entry.getAmplifier() > 0) {
                           name = name + " " + toRoman(entry.getAmplifier() + 1);
                        }

                        context.drawTextWithShadow(textRenderer, name, startX + 28, startY + 6, 16777215);
                        String duration = entry.formatDuration(localTick);
                        context.drawTextWithShadow(textRenderer, duration, startX + 28, startY + 16, 8355711);
                        startY += cardHeight + 2;
                     }
                  }
               }
            }
         }
      }
   }

   private static String toRoman(int n) {
      return switch (n) {
         case 1 -> "I";
         case 2 -> "II";
         case 3 -> "III";
         case 4 -> "IV";
         case 5 -> "V";
         case 6 -> "VI";
         case 7 -> "VII";
         case 8 -> "VIII";
         case 9 -> "IX";
         case 10 -> "X";
         default -> String.valueOf(n);
      };
   }
}

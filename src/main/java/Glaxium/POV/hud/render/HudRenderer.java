package Glaxium.POV.hud.render;

import Glaxium.POV.hud.HudState;
import Glaxium.POV.render.PovViewportMetrics;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import java.util.Random;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;

public class HudRenderer {
   private static final int HUD_GREEN = 8453920;
   private static final int BAR_ICON_Y = -17;
   private static final int EXPERIENCE_BAR_Y = -7;
   private static final int EXPERIENCE_TEXT_Y = -13;
   private static final float SCALE_PIVOT_X = 91.0F;
   private static final float SCALE_PIVOT_Y = 0.5F;
   private static final int MAX_HEALTH_ROWS = 60;
   private static final float MAX_HEALTH_CONTAINER = 1200.0F;
   private static final Identifier WIDGETS_TEXTURE = new Identifier("textures/gui/widgets.png");
   private static final Identifier ICONS_TEXTURE = new Identifier("textures/gui/icons.png");
   private static boolean wasHeartRegenerationEnabled;
   private static long heartRegenerationStartTick;

   public static void renderHotbars(MatrixStack stack, Batcher2D batcher, List<HudState> hotbars) {
      if (hotbars != null && !hotbars.isEmpty()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         int width = PovViewportMetrics.getFilmScaledWidth();
         int height = PovViewportMetrics.getFilmScaledHeight();
         renderHotbars(stack, batcher, hotbars, 0, 0, width, height);
      }
   }

   public static void renderHotbars(MatrixStack stack, Batcher2D batcher, List<HudState> hotbars, int originX, int originY, int width, int height) {
      if (hotbars != null && !hotbars.isEmpty()) {
         for (HudState hotbar : hotbars) {
            renderHotbar(stack, batcher, hotbar, originX, originY, width, height);
         }
      }
   }

   public static void renderHotbar(MatrixStack stack, Batcher2D batcher, HudState hotbar, int originX, int originY, int width, int height) {
      float alpha = MathHelper.clamp(hotbar.alpha, 0.0F, 1.0F);
      if (!(alpha <= 0.0F)) {
         Transform transform = hotbar.layout;
         float scaleX = safeScale(transform.scale.x);
         float scaleY = safeScale(transform.scale.y);
         int hotbarWidth = 182;
         float x = (float)originX + (float)width / 2.0F + transform.translate.x * 2.0F - (float)hotbarWidth / 2.0F;
         float bottom = (float)(originY + height) - transform.translate.y * 2.0F;
         float y = bottom - 0.5F - 21.5F * scaleY;
         batcher.flush();
         stack.push();
         stack.translate(x, y, 0.0F);
         stack.translate(91.0F, 0.5F, 0.0F);
         stack.scale(scaleX, scaleY, 1.0F);
         stack.translate(-91.0F, -0.5F, 0.0F);
         RenderSystem.disableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         batcher.getContext().setShaderColor(1.0F, 1.0F, 1.0F, alpha);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
         batcher.getContext().drawTexture(WIDGETS_TEXTURE, 0, 0, 0, 0, 182, 22);
         boolean hasOffhandItem = hotbar.offhandItem != null && !hotbar.offhandItem.isEmpty();
         if (hasOffhandItem) {
            batcher.getContext().drawTexture(WIDGETS_TEXTURE, -29, -1, 24, 22, 29, 24);
         }

         int selectedSlot = MathHelper.clamp(hotbar.selectedSlot, 0, 8);
         batcher.getContext().drawTexture(WIDGETS_TEXTURE, selectedSlot * 20 - 1, -1, 0, 22, 24, 24);
         if (hotbar.statusBarsVisible) {
            int barsY = -17;
            int heartType = MathHelper.clamp(hotbar.heartType, 0, 4);
            int absorptionType = heartType == 2 ? 2 : 3;
            int healthSlots = MathHelper.ceil(MathHelper.clamp(hotbar.healthContainer, 0.0F, 1200.0F) / 2.0F);
            healthSlots = MathHelper.clamp(healthSlots, 0, 600);
            int healthRows = Math.max(1, Math.min(60, (healthSlots + 9) / 10));
            int absorptionSlots = MathHelper.ceil(MathHelper.clamp(hotbar.absorptionContainer, 0.0F, 1200.0F) / 2.0F);
            absorptionSlots = MathHelper.clamp(absorptionSlots, 0, 600);
            int absorptionRows = absorptionSlots <= 0 ? 0 : Math.max(1, Math.min(60, (absorptionSlots + 9) / 10));
            Random heartShakeRandom = hotbar.health <= 4.0F ? new Random(thisTickSeed()) : null;
            Random hungerShakeRandom = hotbar.hunger <= 6.0F ? new Random(thisTickSeed() + 17L) : null;
            int regenerationHeartIndex = -1;
            long hudTick = currentHudTick();
            long healthFlashAge = (long)Math.floor((double)hotbar.healthFlashAge);
            boolean sharedOutlineBlinking = hotbar.heartFlash && Math.floorMod(healthFlashAge / 3L, 2L) == 0L;
            if (hotbar.heartRegeneration && healthSlots > 0 && hotbar.health > 0.0F) {
               if (!wasHeartRegenerationEnabled) {
                  heartRegenerationStartTick = hudTick;
               }

               wasHeartRegenerationEnabled = true;
               int cycleLength = healthSlots + 5;
               int cycleIndex = cycleLength <= 0 ? 0 : Math.floorMod(hudTick - heartRegenerationStartTick, cycleLength);
               regenerationHeartIndex = cycleIndex < healthSlots ? cycleIndex : -1;
            } else if (wasHeartRegenerationEnabled) {
               wasHeartRegenerationEnabled = false;
            }

            renderHealthBar(
               batcher,
               hotbar.health,
               hotbar.previousHealth,
               hotbar.heartFlash,
               heartType,
               hotbar.hardcore,
               0,
               barsY,
               healthSlots,
               heartShakeRandom,
               regenerationHeartIndex,
               healthFlashAge
            );
            if (absorptionSlots > 0) {
               renderAbsorptionBar(
                  batcher,
                  hotbar.absorption,
                  hotbar.recentAbsorptionLow,
                  hotbar.recentAbsorptionHigh,
                  hotbar.absorptionFlash,
                  sharedOutlineBlinking,
                  absorptionType,
                  hotbar.hardcore,
                  0,
                  barsY - healthRows * 10,
                  absorptionSlots,
                  heartShakeRandom,
                  hudTick
               );
            }

            if (hotbar.armor > 0.0F) {
               renderArmorBar(batcher, hotbar.armor, 0, barsY - (healthRows + absorptionRows) * 10, 10);
            }

            int mountSlots = MathHelper.ceil(MathHelper.clamp(hotbar.mountHealthContainer, 0.0F, 60.0F) / 2.0F);
            if (mountSlots > 0) {
               renderMountHealthBar(batcher, hotbar.mountHealth, 173, barsY, mountSlots, null);
            } else {
               renderFoodBar(batcher, hotbar.hunger, hotbar.hungerEffect, 173, barsY, 10, hungerShakeRandom);
            }

            renderAirBar(batcher, hotbar.air, 173, barsY - 10);
            float experience = MathHelper.clamp(hotbar.experience, 0.0F, 1.0F);
            int xpPixels = MathHelper.ceil(experience * 182.0F);
            batcher.getContext().drawTexture(ICONS_TEXTURE, 0, -7, 0, 64, 182, 5);
            if (xpPixels > 0) {
               batcher.getContext().drawTexture(ICONS_TEXTURE, 0, -7, 0, 69, xpPixels, 5);
            }

            if (hotbar.experienceLevel > 0) {
               String level = Integer.toString(hotbar.experienceLevel);
               int levelX = (182 - batcher.getFont().getWidth(level)) / 2;
               int outlineColor = applyAlpha(0, alpha);
               int levelColor = applyAlpha(8453920, alpha);
               batcher.text(level, (float)(levelX - 1), -13.0F, outlineColor, false);
               batcher.text(level, (float)(levelX + 1), -13.0F, outlineColor, false);
               batcher.text(level, (float)levelX, -14.0F, outlineColor, false);
               batcher.text(level, (float)levelX, -12.0F, outlineColor, false);
               batcher.text(level, (float)levelX, -13.0F, levelColor, false);
            }
         }

         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(true);
         Vector3f light0 = new Vector3f(0.85F, 0.85F, -1.0F).normalize();
         Vector3f light1 = new Vector3f(-0.85F, 0.85F, 1.0F).normalize();
         RenderSystem.setupGui3DDiffuseLighting(light0, light1);

         for (int i = 0; i < 9; i++) {
            ItemStack stackItem = hotbar.items[i];
            if (stackItem != null && !stackItem.isEmpty()) {
               int itemX = 3 + i * 20;
               int itemY = 3;
               batcher.getContext().drawItem(stackItem, itemX, itemY);
               batcher.getContext().drawItemInSlot(batcher.getFont().getRenderer(), stackItem, itemX, itemY);
            }
         }

         if (hasOffhandItem) {
            int offhandX = -26;
            int offhandY = 3;
            batcher.getContext().drawItem(hotbar.offhandItem, offhandX, offhandY);
            batcher.getContext().drawItemInSlot(batcher.getFont().getRenderer(), hotbar.offhandItem, offhandX, offhandY);
         }

         batcher.getContext().draw();
         DiffuseLighting.disableGuiDepthLighting();
         RenderSystem.disableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.disableBlend();
         batcher.getContext().setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         stack.pop();
         batcher.flush();
      }
   }

   private static float safeScale(float value) {
      if (!Float.isFinite(value)) {
         return 1.0F;
      } else if (Math.abs(value) < 0.05F) {
         return value < 0.0F ? -0.05F : 0.05F;
      } else {
         return value;
      }
   }

   private static void renderHealthBar(
      Batcher2D batcher,
      float health,
      float previousHealth,
      boolean healthFlash,
      int heartType,
      boolean hardcore,
      int x,
      int y,
      int slots,
      Random lowHealthShakeRandom,
      int regenerationHeartIndex,
      long healthFlashAge
   ) {
      if (slots > 0) {
         int flashTicksPerPhase = 3;
         float current = MathHelper.clamp(health, 0.0F, (float)slots * 2.0F) / 2.0F;
         float previous = MathHelper.clamp(previousHealth, 0.0F, (float)slots * 2.0F) / 2.0F;
         boolean showBlinkingPhase = healthFlash && Math.floorMod(healthFlashAge / 3L, 2L) == 0L;
         boolean showPaleLayer = showBlinkingPhase && current < previous;

         for (int i = 0; i < slots; i++) {
            int row = i / 10;
            int col = i % 10;
            int iconX = x + col * 8;
            int iconY = y - row * 10;
            if (lowHealthShakeRandom != null) {
               iconY += lowHealthShakeRandom.nextInt(2);
            }

            if (i == regenerationHeartIndex) {
               iconY -= 2;
            }

            int containerU = 16 + (showBlinkingPhase ? 9 : 0);
            int containerV = hardcore ? 45 : 0;
            batcher.getContext().drawTexture(ICONS_TEXTURE, iconX, iconY, containerU, containerV, 9, 9);
            if (showPaleLayer) {
               drawHeartFill(batcher, previous - (float)i, heartType, hardcore, true, iconX, iconY);
            }

            drawHeartFill(batcher, current - (float)i, heartType, hardcore, false, iconX, iconY);
         }
      }
   }

   private static void drawHeartFill(Batcher2D batcher, float amount, int heartType, boolean hardcore, boolean blinking, int x, int y) {
      if (!(amount <= 0.0F)) {
         boolean half = amount < 1.0F && amount >= 0.5F;
         if (amount >= 0.5F) {
            int textureIndex;
            boolean hasBlinking;
            switch (heartType) {
               case 1:
                  textureIndex = 4;
                  hasBlinking = true;
                  break;
               case 2:
                  textureIndex = 6;
                  hasBlinking = true;
                  break;
               case 3:
                  textureIndex = 8;
                  hasBlinking = false;
                  break;
               case 4:
                  textureIndex = 9;
                  hasBlinking = false;
                  break;
               default:
                  textureIndex = 2;
                  hasBlinking = true;
            }

            int u = 16 + (textureIndex * 2 + (blinking && hasBlinking ? 2 : 0) + (half ? 1 : 0)) * 9;
            int v = hardcore ? 45 : 0;
            batcher.getContext().drawTexture(ICONS_TEXTURE, x, y, u, v, 9, 9);
         }
      }
   }

   private static void renderAbsorptionBar(
      Batcher2D batcher,
      float value,
      float recentHealthLow,
      float recentHealthHigh,
      boolean heartFlash,
      boolean sharedOutlineBlinking,
      int absorptionType,
      boolean hardcore,
      int x,
      int y,
      int slots,
      Random lowHealthShakeRandom,
      long hudTick
   ) {
      if (slots > 0) {
         int FLASH_TICKS_PER_PHASE = 3;
         float normalized = MathHelper.clamp(value, 0.0F, (float)slots * 2.0F) / 2.0F;
         boolean recentlyIncreased = recentHealthHigh - recentHealthLow > 0.05F && value >= recentHealthHigh - 0.05F;
         boolean heartAffected = heartFlash || recentlyIncreased;
         boolean flashPhaseOn = hudTick / 3L % 2L == 0L;
         boolean outlineBlinking = sharedOutlineBlinking || heartAffected && !flashPhaseOn;

         for (int i = 0; i < slots; i++) {
            int row = i / 10;
            int col = i % 10;
            int iconX = x + col * 8;
            int iconY = y - row * 10;
            if (lowHealthShakeRandom != null) {
               iconY += lowHealthShakeRandom.nextInt(2);
            }

            int containerU = 16 + (outlineBlinking ? 9 : 0);
            int containerV = hardcore ? 45 : 0;
            batcher.getContext().drawTexture(ICONS_TEXTURE, iconX, iconY, containerU, containerV, 9, 9);
            float current = normalized - (float)i;
            drawHeartFill(batcher, current, absorptionType, hardcore, false, iconX, iconY);
         }
      }
   }

   private static long thisTickSeed() {
      return currentHudTick() * 312871L;
   }

   private static long currentHudTick() {
      MinecraftClient mc = MinecraftClient.getInstance();
      return mc.world != null ? mc.world.getTime() : System.currentTimeMillis() / 50L;
   }

   private static void renderArmorBar(Batcher2D batcher, float value, int x, int y, int slots) {
      if (slots > 0) {
         float normalized = MathHelper.clamp(value, 0.0F, (float)slots * 2.0F) / 2.0F;

         for (int i = 0; i < slots; i++) {
            int row = i / 10;
            int col = i % 10;
            int iconX = x + col * 8;
            int iconY = y - row * 10;
            float current = normalized - (float)i;
            if (current >= 1.0F) {
               batcher.getContext().drawTexture(ICONS_TEXTURE, iconX, iconY, 34, 9, 9, 9);
            } else if (current >= 0.5F) {
               batcher.getContext().drawTexture(ICONS_TEXTURE, iconX, iconY, 25, 9, 9, 9);
            } else {
               batcher.getContext().drawTexture(ICONS_TEXTURE, iconX, iconY, 16, 9, 9, 9);
            }
         }
      }
   }

   private static void renderFoodBar(Batcher2D batcher, float value, boolean hungerEffect, int x, int y, int slots, Random lowHungerShakeRandom) {
      if (slots > 0) {
         float normalized = MathHelper.clamp(value, 0.0F, (float)slots * 2.0F) / 2.0F;

         for (int i = 0; i < slots; i++) {
            int row = i / 10;
            int col = i % 10;
            int iconX = x - col * 8;
            int iconY = y - row * 10;
            if (lowHungerShakeRandom != null) {
               iconY += lowHungerShakeRandom.nextInt(2);
            }

            int emptyU = hungerEffect ? 133 : 16;
            batcher.getContext().drawTexture(ICONS_TEXTURE, iconX, iconY, emptyU, 27, 9, 9);
            float current = normalized - (float)i;
            if (current >= 1.0F) {
               int fullU = hungerEffect ? 88 : 52;
               batcher.getContext().drawTexture(ICONS_TEXTURE, iconX, iconY, fullU, 27, 9, 9);
            } else if (current >= 0.5F) {
               int halfU = hungerEffect ? 97 : 61;
               batcher.getContext().drawTexture(ICONS_TEXTURE, iconX, iconY, halfU, 27, 9, 9);
            }
         }
      }
   }

   private static int applyAlpha(int color, float alpha) {
      int a = MathHelper.clamp(Math.round(MathHelper.clamp(alpha, 0.0F, 1.0F) * 255.0F), 0, 255);
      return a << 24 | color & 16777215;
   }

   private static void renderAirBar(Batcher2D batcher, float air, int x, int y) {
      if (!(air >= 300.0F)) {
         int full = MathHelper.ceil((air - 2.0F) * 10.0F / 300.0F);
         int popping = MathHelper.ceil(air * 10.0F / 300.0F) - full;
         full = MathHelper.clamp(full, 0, 10);
         popping = MathHelper.clamp(popping, 0, 10 - full);

         for (int i = 0; i < full + popping; i++) {
            int iconX = x - i * 8;
            int u = i < full ? 16 : 25;
            batcher.getContext().drawTexture(ICONS_TEXTURE, iconX, y, u, 18, 9, 9);
         }
      }
   }

   private static void renderMountHealthBar(Batcher2D batcher, float value, int x, int y, int slots, Random shakeRandom) {
      if (slots > 0) {
         float normalized = MathHelper.clamp(value, 0.0F, (float)slots * 2.0F) / 2.0F;

         for (int i = 0; i < slots; i++) {
            int row = i / 10;
            int col = i % 10;
            int iconX = x - col * 8;
            int iconY = y - row * 10;
            if (shakeRandom != null) {
               iconY += shakeRandom.nextInt(2);
            }

            batcher.getContext().drawTexture(ICONS_TEXTURE, iconX, iconY, 52, 9, 9, 9);
            float current = normalized - (float)i;
            if (current >= 1.0F) {
               batcher.getContext().drawTexture(ICONS_TEXTURE, iconX, iconY, 88, 9, 9, 9);
            } else if (current >= 0.5F) {
               batcher.getContext().drawTexture(ICONS_TEXTURE, iconX, iconY, 97, 9, 9, 9);
            }
         }
      }
   }
}

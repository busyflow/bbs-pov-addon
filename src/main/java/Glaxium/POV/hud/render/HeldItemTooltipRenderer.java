package Glaxium.POV.hud.render;

import Glaxium.POV.hud.HudState;
import Glaxium.POV.integration.access.minecraft.InGameHudHeldItemPovAccess;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

public final class HeldItemTooltipRenderer {
   private HeldItemTooltipRenderer() {
   }

   public static void renderPlayback(Batcher2D batcher, ReplayKeyframes replay, HudState state, float tick, int width, int height) {
      if (state != null && replay != null && state.visible) {
         int slot = MathHelper.clamp(state.selectedSlot, 0, 8);
         ItemStack stack = state.items[slot];
         int fade = remainingFade(replay, tick, stack);
         render(batcher, stack, fade, state.statusBarsVisible, state.layout, width, height);
      }
   }

   public static void renderLive(Batcher2D batcher, HudState state, int width, int height) {
      if (state != null && state.visible) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.inGameHud instanceof InGameHudHeldItemPovAccess access) {
            ItemStack stack = access.bbsPov$getHeldItemTooltipStack();
            if (stack == null || stack.isEmpty()) {
               int slot = MathHelper.clamp(state.selectedSlot, 0, 8);
               stack = state.items[slot];
            }

            render(batcher, stack, access.bbsPov$getHeldItemTooltipFade(), state.statusBarsVisible, state.layout, width, height);
         }
      }
   }

   public static void render(Batcher2D batcher, ItemStack stack, int fade, boolean statusBarsVisible, Transform layout, int width, int height) {
      if (fade > 0 && stack != null && !stack.isEmpty()) {
         MutableText name = Text.empty().append(stack.getName()).formatted(stack.getRarity().formatting);
         if (stack.hasCustomName()) {
            name = name.formatted(Formatting.ITALIC);
         }

         MinecraftClient client = MinecraftClient.getInstance();
         TextRenderer texts = client.textRenderer;
         int textWidth = texts.getWidth(name);
         float layoutX = layout == null ? 0.0F : layout.translate.x * 2.0F;
         float layoutY = layout == null ? 0.0F : layout.translate.y * 2.0F;
         int x = (width - textWidth) / 2 + Math.round(layoutX);
         int y = height - 59 - Math.round(layoutY);
         if (!statusBarsVisible) {
            y += 14;
         }

         int alpha = (int)((float)fade * 256.0F / 10.0F);
         if (alpha > 255) {
            alpha = 255;
         }

         if (alpha > 0) {
            DrawContext context = batcher.getContext();
            batcher.flush();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            int background = client.options.getTextBackgroundColor(0);
            context.fill(x - 2, y - 2, x + textWidth + 2, y + 9 + 2, background);
            context.drawTextWithShadow(texts, name, x, y, 16777215 + (alpha << 24));
            context.draw();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            batcher.flush();
         }
      }
   }

   private static int remainingFade(ReplayKeyframes replay, float tick, ItemStack now) {
      if (now != null && !now.isEmpty()) {
         double displayTime = 1.0;
         SimpleOption<Double> option = MinecraftClient.getInstance().options.getNotificationDisplayTime();
         if (option != null && option.getValue() != null) {
            displayTime = (Double)option.getValue();
         }

         int max = Math.max(1, (int)(40.0 * displayTime));

         for (int i = 1; i <= max + 1; i++) {
            float thenTick = tick - (float)i;
            if (thenTick < 0.0F) {
               return 0;
            }

            int thenSlot = MathHelper.clamp(replay.getSelectedSlot(thenTick), 0, 8);
            ItemStack then = (ItemStack)((KeyframeChannel)replay.hotbar.get(thenSlot)).interpolate(thenTick, ItemStack.EMPTY);
            if (!sameHeldName(then, now)) {
               return Math.max(0, max - i + 1);
            }
         }

         return 0;
      } else {
         return 0;
      }
   }

   private static boolean sameHeldName(ItemStack a, ItemStack b) {
      boolean aEmpty = a == null || a.isEmpty();
      boolean bEmpty = b == null || b.isEmpty();
      return !aEmpty && !bEmpty ? a.isOf(b.getItem()) && a.getName().equals(b.getName()) : aEmpty && bEmpty;
   }
}

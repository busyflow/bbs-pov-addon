package Glaxium.POV.actions.gui.render.common;

import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

public final class GuiTextRenderer {
   private GuiTextRenderer() {
   }

   public static float sampleFloat(KeyframeChannel<Float> channel, float tick, float fallback) {
      if (channel != null && !channel.isEmpty()) {
         Float value = (Float)channel.interpolate(tick, fallback);
         return value == null ? fallback : value;
      } else {
         return fallback;
      }
   }

   public static int sampleInt(KeyframeChannel<Integer> channel, float tick, int fallback) {
      if (channel != null && !channel.isEmpty()) {
         Integer value = (Integer)channel.interpolate(tick, fallback);
         return value == null ? fallback : value;
      } else {
         return fallback;
      }
   }

   public static boolean sampleBool(KeyframeChannel<Boolean> channel, float tick, boolean fallback) {
      if (channel != null && !channel.isEmpty()) {
         Boolean value = (Boolean)channel.interpolate(tick, fallback);
         return value == null ? fallback : value;
      } else {
         return fallback;
      }
   }

   public static String sampleString(KeyframeChannel<String> channel, float tick, String fallback) {
      if (channel != null && !channel.isEmpty()) {
         String value = (String)channel.interpolate(tick, fallback);
         return value == null ? fallback : value;
      } else {
         return fallback;
      }
   }

   public static boolean inBounds(float x, float y, int left, int top, int width, int height) {
      return x >= (float)left && x < (float)(left + width) && y >= (float)top && y < (float)(top + height);
   }

   public static boolean isSearchFocused(KeyframeChannel<Boolean> channel, float tick, String value, boolean hovered) {
      return channel != null && !channel.isEmpty() ? Boolean.TRUE.equals(channel.interpolate(tick, false)) : hovered || value != null && !value.isBlank();
   }

   public static void drawSearchField(
      Batcher2D batcher, String value, int x, int y, int width, int height, boolean focused, boolean drawBackground, float opacity, int selStart, int selEnd
   ) {
      if (drawBackground) {
         int borderColor = focused ? -1 : -6250336;
         batcher.getContext().fill(x - 1, y - 1, x + width + 1, y + height + 1, borderColor);
         batcher.getContext().fill(x, y, x + width, y + height, -16777216);
      }

      String text = value == null ? "" : value;
      int textX = x + (drawBackground ? 4 : 0);
      int textY = drawBackground ? y + Math.max(0, (height - 8) / 2) : y;
      int alpha = (int)(255.0F * opacity) << 24;
      boolean empty = text.isBlank();
      TextRenderer font = MinecraftClient.getInstance().textRenderer;
      if (empty && !focused && drawBackground) {
         batcher.getContext().drawTextWithShadow(font, "Search...", textX, textY, alpha | 8421504);
      } else {
         if (!empty) {
            batcher.getContext().drawTextWithShadow(font, text, textX, textY, alpha | 16777215);
         }

         int start = MathHelper.clamp(Math.min(selStart, selEnd), 0, text.length());
         int end = MathHelper.clamp(Math.max(selStart, selEnd), 0, text.length());
         boolean selected = start != end;
         if (selected) {
            int x1 = textX + font.getWidth(text.substring(0, start));
            int x2 = textX + font.getWidth(text.substring(0, end));
            batcher.getContext().fill(RenderLayer.getGuiTextHighlight(), x1, textY - 1, x2, textY + 9, -16776961);
         } else if (focused && Util.getMeasuringTimeMs() / 300L % 2L == 0L) {
            int caret = MathHelper.clamp(selStart, 0, text.length());
            int caretX = textX + font.getWidth(text.substring(0, caret));
            if (caret < text.length()) {
               batcher.getContext().fill(RenderLayer.getGuiOverlay(), caretX, textY - 1, caretX + 1, textY + 10, alpha | 13684944);
            } else {
               batcher.getContext().drawTextWithShadow(font, "_", caretX, textY, alpha | 16777215);
            }
         }
      }
   }
}

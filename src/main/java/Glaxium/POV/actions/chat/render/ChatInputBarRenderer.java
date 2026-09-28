package Glaxium.POV.actions.chat.render;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public final class ChatInputBarRenderer {
   private ChatInputBarRenderer() {
   }

   public static void renderInputBar(
      DrawContext context,
      TextRenderer font,
      String text,
      int cursorPos,
      int selStart,
      int selEnd,
      boolean showRecs,
      int width,
      int height,
      float cursorX,
      float cursorY,
      boolean cursorVisible
   ) {
      if (context != null && font != null) {
         if (text == null) {
            text = "";
         }

         cursorPos = Math.max(0, Math.min(cursorPos, text.length()));
         int barY1 = height - 14;
         int barY2 = height - 2;
         int barX1 = 2;
         int barX2 = width - 2;
         context.fill(barX1, barY1, barX2, barY2, Integer.MIN_VALUE);
         int textX = 4;
         int textY = height - 12;
         ChatCommandSuggestor.ParseResultInfo info = null;
         String ghostPreview = null;
         if (showRecs && text.startsWith("/")) {
            info = ChatCommandSuggestor.getParsedInfo(text, cursorPos);
            ghostPreview = ChatCommandSuggestor.renderCommandSuggestions(context, font, text, info, textX, barY1, width, cursorX, cursorY, cursorVisible);
         }

         boolean hasSelection = selStart >= 0 && selEnd >= 0 && selStart != selEnd;
         int minSel = hasSelection ? Math.max(0, Math.min(selStart, selEnd)) : -1;
         int maxSel = hasSelection ? Math.min(text.length(), Math.max(selStart, selEnd)) : -1;
         if (!hasSelection) {
            ChatCommandSuggestor.renderColoredCommandText(context, font, text, textX, textY, ghostPreview, info);
         } else {
            String before = text.substring(0, minSel);
            String selected = text.substring(minSel, maxSel);
            String after = text.substring(maxSel);
            ChatCommandSuggestor.renderColoredCommandText(context, font, before, textX, textY, null, info);
            int x = textX + font.getWidth(before);
            int selWidth = font.getWidth(selected);
            context.fill(x, textY - 1, x + selWidth, textY + 9, -2147483393);
            context.drawTextWithShadow(font, selected, x, textY, -1);
            x += selWidth;
            ChatCommandSuggestor.renderColoredCommandText(context, font, after, x, textY, ghostPreview, info);
         }

         boolean cursorBlink = System.currentTimeMillis() / 300L % 2L == 0L;
         if (cursorBlink && !hasSelection) {
            String textBeforeCursor = text.substring(0, cursorPos);
            int cursorScreenX = textX + font.getWidth(textBeforeCursor);
            if (cursorPos < text.length()) {
               context.fill(cursorScreenX, textY - 1, cursorScreenX + 1, textY + 9, -3092272);
            } else {
               context.drawTextWithShadow(font, "_", cursorScreenX, textY, -1);
            }
         }
      }
   }
}

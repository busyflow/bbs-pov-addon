package Glaxium.POV.actions.chat.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;

/** Renders the chat input bar, command suggestions, selection, and typing cursor. */
public final class ChatInputBarRenderer
{
    private ChatInputBarRenderer()
    {
    }

    public static void renderInputBar(Batcher2D batcher, TextRenderer font, String text, int cursorPos, int selStart, int selEnd, boolean showRecs, int width, int height, float cursorX, float cursorY, boolean cursorVisible)
    {
        if (batcher == null || font == null)
        {
            return;
        }

        DrawContext context = batcher.getContext();

        if (text == null)
        {
            text = "";
        }

        cursorPos = Math.max(0, Math.min(cursorPos, text.length()));

        int barY1 = height - 14;
        int barY2 = height - 2;
        int barX1 = 2;
        int barX2 = width - 2;

        // 1. Transparent black background bar matching vanilla chat opacity using Batcher2D
        MinecraftClient mc = MinecraftClient.getInstance();
        int barColor = (mc != null && mc.options != null)
            ? mc.options.getTextBackgroundColor(Integer.MIN_VALUE)
            : ChatHistoryRenderer.getChatBgColor(1.0F);
        batcher.box(barX1, barY1, barX2, barY2, barColor);
        batcher.flush();

        int textX = 4;
        int textY = height - 12;

        ChatCommandSuggestor.ParseResultInfo info = null;
        String ghostPreview = null;

        if (showRecs && text.startsWith("/"))
        {
            info = ChatCommandSuggestor.getParsedInfo(text, cursorPos);
            ghostPreview = ChatCommandSuggestor.renderCommandSuggestions(context, font, text, info, textX, barY1, width, cursorX, cursorY, cursorVisible);
        }

        boolean hasSelection = selStart >= 0 && selEnd >= 0 && selStart != selEnd;
        int minSel = hasSelection ? Math.max(0, Math.min(selStart, selEnd)) : -1;
        int maxSel = hasSelection ? Math.min(text.length(), Math.max(selStart, selEnd)) : -1;

        // 2. Render Text
        if (!hasSelection)
        {
            ChatCommandSuggestor.renderColoredCommandText(context, font, text, textX, textY, ghostPreview, info);
        }
        else
        {
            String before = text.substring(0, minSel);
            String selected = text.substring(minSel, maxSel);
            String after = text.substring(maxSel);

            int x = textX;
            ChatCommandSuggestor.renderColoredCommandText(context, font, before, x, textY, null, info);
            x += font.getWidth(before);

            int selWidth = font.getWidth(selected);
            context.fill(x, textY - 1, x + selWidth, textY + 9, 0x800000FF);
            context.drawTextWithShadow(font, selected, x, textY, 0xFFFFFFFF);
            x += selWidth;

            ChatCommandSuggestor.renderColoredCommandText(context, font, after, x, textY, ghostPreview, info);
        }

        // 3. Render Flickering Cursor "_"
        boolean cursorBlink = (System.currentTimeMillis() / 300L) % 2L == 0L;
        if (cursorBlink && !hasSelection)
        {
            String textBeforeCursor = text.substring(0, cursorPos);
            int cursorScreenX = textX + font.getWidth(textBeforeCursor);
            if (cursorPos < text.length())
            {
                context.fill(cursorScreenX, textY - 1, cursorScreenX + 1, textY + 9, 0xFFD0D0D0);
            }
            else
            {
                context.drawTextWithShadow(font, "_", cursorScreenX, textY, 0xFFFFFFFF);
            }
        }
    }
}

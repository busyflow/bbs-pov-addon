package Glaxium.POV.actions.gui.render.screen;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.data.BookSnapshot;
import Glaxium.POV.actions.gui.render.common.GuiStandardLayoutRenderer;
import Glaxium.POV.actions.gui.render.GuiRenderContext;
import Glaxium.POV.actions.gui.render.GuiRenderer;
import Glaxium.POV.actions.gui.render.GuiScreenChrome;
import Glaxium.POV.actions.gui.render.common.GuiTextRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/** Book / writable-book clip renderer. Saved type id remains {@code book}. */
public final class BookGuiRenderer implements GuiRenderer, GuiScreenChrome
{
    public static final BookGuiRenderer INSTANCE = new BookGuiRenderer();

    private static final Identifier PAGE_FORWARD = new Identifier("widget/page_forward");
    private static final Identifier PAGE_FORWARD_HIGHLIGHTED = new Identifier("widget/page_forward_highlighted");
    private static final Identifier PAGE_BACKWARD = new Identifier("widget/page_backward");
    private static final Identifier PAGE_BACKWARD_HIGHLIGHTED = new Identifier("widget/page_backward_highlighted");
    private static final Identifier WIDGET_BUTTON = new Identifier("widget/button");
    private static final Identifier WIDGET_BUTTON_HIGHLIGHTED = new Identifier("widget/button_highlighted");

    private BookGuiRenderer()
    {
    }

    @Override
    public void render(GuiRenderContext ctx)
    {
        GuiStandardLayoutRenderer.render(ctx, this);
    }

    @Override
    public void drawEarlyChrome(GuiRenderContext ctx)
    {
        float cursorX = ctx.cursorVisible ? ctx.cursorGuiX : -1000F;
        float cursorY = ctx.cursorVisible ? ctx.cursorGuiY : -1000F;
        drawChrome(ctx.batcher, ctx.clip, ctx.guiId, ctx.localTick, ctx.opacity, cursorX, cursorY);
    }

    private static void drawChrome(
        Batcher2D batcher,
        GuiPovActionClip clip,
        String guiId,
        float tick,
        float opacity,
        float cursorX,
        float cursorY)
    {
        boolean writable = GuiTextRenderer.sampleBool(clip.getBookWritable(guiId), tick, false);
        boolean signing = writable && GuiTextRenderer.sampleBool(clip.getBookSigning(guiId), tick, false);
        List<String> pages = BookSnapshot.unpack(GuiTextRenderer.sampleString(clip.getBookPages(guiId), tick, ""));
        int pageCount = Math.max(1, pages.size());
        int page = MathHelper.clamp(GuiTextRenderer.sampleInt(clip.getBookPage(guiId), tick, 0), 0, pageCount - 1);
        String title = GuiTextRenderer.sampleString(clip.getBookTitle(guiId), tick, "");
        String author = GuiTextRenderer.sampleString(clip.getBookAuthor(guiId), tick, "");
        int selStart = GuiTextRenderer.sampleInt(clip.getBookSelStart(guiId), tick, 0);
        int selEnd = GuiTextRenderer.sampleInt(clip.getBookSelEnd(guiId), tick, 0);
        var font = MinecraftClient.getInstance().textRenderer;
        int ink = bookInk(opacity);

        if (signing)
        {
            Text header = Text.translatable("book.editTitle");
            batcher.getContext().drawText(
                font,
                header,
                36 + (114 - font.getWidth(header)) / 2,
                34,
                ink,
                false);
            int titleX = 36 + (114 - font.getWidth(title)) / 2;
            batcher.getContext().drawText(font, title, titleX, 50, ink, false);
            Text signedBy = Text.translatable("book.byAuthor", author);
            batcher.getContext().drawText(
                font,
                signedBy,
                36 + (114 - font.getWidth(signedBy)) / 2,
                60,
                bookInk(opacity) | 0x555555,
                false);
            batcher.getContext().drawTextWrapped(
                font,
                Text.translatable("book.finalizeWarning"),
                36,
                82,
                114,
                ink);
            drawBookTitleCaret(batcher, title, titleX, 50, selStart, selEnd, opacity);
        }
        else
        {
            String pageText = pages.get(page);
            StringVisitable visitable = writable
                ? Text.literal(pageText)
                : parseBookPage(pageText);
            List<net.minecraft.text.OrderedText> lines = font.wrapLines(visitable, 114);
            for (int i = 0; i < lines.size(); i++)
            {
                batcher.getContext().drawText(font, lines.get(i), 36, 32 + i * 9, ink, false);
            }

            Text indicator = Text.translatable("book.pageIndicator", page + 1, pageCount);
            batcher.getContext().drawText(
                font,
                indicator,
                192 - 44 - font.getWidth(indicator),
                18,
                ink,
                false);

            if (page > 0)
            {
                boolean hover = GuiTextRenderer.inBounds(cursorX, cursorY, 43, 159, 23, 13);
                batcher.getContext().drawGuiTexture(hover ? PAGE_BACKWARD_HIGHLIGHTED : PAGE_BACKWARD, 43, 159, 23, 13);
            }
            // Writable books can always append a page, so the forward arrow stays visible.
            if (writable || page < pageCount - 1)
            {
                boolean hover = GuiTextRenderer.inBounds(cursorX, cursorY, 116, 159, 23, 13);
                batcher.getContext().drawGuiTexture(hover ? PAGE_FORWARD_HIGHLIGHTED : PAGE_FORWARD, 116, 159, 23, 13);
            }

            if (writable)
            {
                drawBookPageCaret(batcher, pageText, selStart, selEnd, opacity);
            }
        }

        if (signing)
        {
            drawBookButton(batcher, -4, 196, 98, 20, Text.translatable("book.finalizeButton"), cursorX, cursorY, opacity);
            drawBookButton(batcher, 98, 196, 98, 20, ScreenTexts.CANCEL, cursorX, cursorY, opacity);
        }
        else if (writable)
        {
            drawBookButton(batcher, -4, 196, 98, 20, Text.translatable("book.signButton"), cursorX, cursorY, opacity);
            drawBookButton(batcher, 98, 196, 98, 20, ScreenTexts.DONE, cursorX, cursorY, opacity);
        }
        else
        {
            drawBookButton(batcher, -4, 196, 200, 20, ScreenTexts.DONE, cursorX, cursorY, opacity);
        }
    }

    private static StringVisitable parseBookPage(String page)
    {
        if (page == null || page.isEmpty())
        {
            return StringVisitable.EMPTY;
        }

        try
        {
            Text parsed = Text.Serialization.fromJson(page);
            return parsed != null ? parsed : Text.literal(page);
        }
        catch (Exception ignored)
        {
            return Text.literal(page);
        }
    }

    private static void drawBookButton(
        Batcher2D batcher,
        int x,
        int y,
        int width,
        int height,
        Text label,
        float cursorX,
        float cursorY,
        float opacity)
    {
        boolean hover = GuiTextRenderer.inBounds(cursorX, cursorY, x, y, width, height);
        batcher.getContext().drawGuiTexture(hover ? WIDGET_BUTTON_HIGHLIGHTED : WIDGET_BUTTON, x, y, width, height);
        var font = MinecraftClient.getInstance().textRenderer;
        int color = ((int) (0xFF * opacity) << 24) | 0xFFFFFF;
        batcher.getContext().drawCenteredTextWithShadow(
            font,
            label,
            x + width / 2,
            y + (height - 8) / 2,
            color);
    }

    private static int bookInk(float opacity)
    {
        return (int) (0xFF * opacity) << 24;
    }

    private static void drawBookPageCaret(
        Batcher2D batcher,
        String page,
        int selStart,
        int selEnd,
        float opacity)
    {
        String text = page == null ? "" : page;
        var font = MinecraftClient.getInstance().textRenderer;
        List<String> lines = new ArrayList<>();
        font.getTextHandler().wrapLines(text, 114, net.minecraft.text.Style.EMPTY, true, (style, start, end) ->
            lines.add(text.substring(start, end)));
        if (lines.isEmpty())
        {
            lines.add("");
        }

        int start = MathHelper.clamp(Math.min(selStart, selEnd), 0, text.length());
        int end = MathHelper.clamp(Math.max(selStart, selEnd), 0, text.length());
        int cursor = 0;
        for (int i = 0; i < lines.size(); i++)
        {
            String line = lines.get(i);
            int lineStart = cursor;
            int lineEnd = cursor + line.length();
            int y = 32 + i * 9;
            if (start != end && end > lineStart && start < lineEnd)
            {
                int x1 = 36 + font.getWidth(line.substring(0, Math.max(0, start - lineStart)));
                int x2 = 36 + font.getWidth(line.substring(0, Math.min(line.length(), end - lineStart)));
                batcher.getContext().fill(RenderLayer.getGuiTextHighlight(), x1, y - 1, x2, y + 9, 0xFF0000FF);
            }
            cursor = lineEnd;
        }

        if (start == end && Util.getMeasuringTimeMs() / 300L % 2L == 0)
        {
            int remaining = MathHelper.clamp(selStart, 0, text.length());
            int lineY = 32;
            String line = "";
            for (String candidate : lines)
            {
                if (remaining <= candidate.length())
                {
                    line = candidate;
                    break;
                }
                remaining -= candidate.length();
                lineY += 9;
            }
            remaining = MathHelper.clamp(remaining, 0, line.length());
            int caretX = 36 + font.getWidth(line.substring(0, remaining));
            int alpha = (int) (0xFF * opacity) << 24;
            if (remaining < line.length())
            {
                batcher.getContext().fill(RenderLayer.getGuiOverlay(), caretX, lineY - 1, caretX + 1, lineY + 10, alpha);
            }
            else
            {
                batcher.getContext().drawText(MinecraftClient.getInstance().textRenderer, "_", caretX, lineY, alpha, false);
            }
        }
    }

    private static void drawBookTitleCaret(
        Batcher2D batcher,
        String text,
        int x,
        int y,
        int selStart,
        int selEnd,
        float opacity)
    {
        String title = text == null ? "" : text;
        var font = MinecraftClient.getInstance().textRenderer;
        int start = MathHelper.clamp(Math.min(selStart, selEnd), 0, title.length());
        int end = MathHelper.clamp(Math.max(selStart, selEnd), 0, title.length());
        if (start != end)
        {
            int x1 = x + font.getWidth(title.substring(0, start));
            int x2 = x + font.getWidth(title.substring(0, end));
            batcher.getContext().fill(RenderLayer.getGuiTextHighlight(), x1, y - 1, x2, y + 9, 0xFF0000FF);
        }
        if (start == end && Util.getMeasuringTimeMs() / 300L % 2L == 0)
        {
            int caret = MathHelper.clamp(selStart, 0, title.length());
            int caretX = x + font.getWidth(title.substring(0, caret));
            batcher.getContext().drawText(font, "_", caretX, y, bookInk(opacity), false);
        }
    }





}

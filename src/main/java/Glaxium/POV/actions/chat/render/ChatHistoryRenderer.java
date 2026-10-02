package Glaxium.POV.actions.chat.render;

import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.actions.clip.ChatPovActionClip;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.integration.mixin.minecraft.ChatHudPovAccessor;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHudLine;

import java.util.ArrayList;
import java.util.List;

/** Renders chat message history, fading HUD messages, message indicators, and the vanilla scrollbar. */
public final class ChatHistoryRenderer
{
    private ChatHistoryRenderer()
    {
    }

    public static int getChatBgColor(float factor)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        double textBgOpacity = (mc != null && mc.options != null && mc.options.getTextBackgroundOpacity() != null)
            ? mc.options.getTextBackgroundOpacity().getValue()
            : 0.5D;
        int alpha = (int) (255.0D * textBgOpacity * factor);
        alpha = Math.max(0, Math.min(255, alpha));
        return alpha << 24;
    }

    public static void renderPlaybackHistory(Batcher2D batcher, Film film, RecordedPovActions actions, float replayTick, float filmTick, int width, int height, boolean barVisible, int scrollOffset)
    {
        if (batcher == null)
        {
            return;
        }

        DrawContext context = batcher.getContext();
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer font = mc.textRenderer;
        if (font == null)
        {
            return;
        }

        List<ActiveExecutionText> activeList = new ArrayList<>();

        if (film != null && film.replays != null && !film.replays.getList().isEmpty())
        {
            for (Replay r : film.replays.getList())
            {
                if (r != null && r.keyframes instanceof ReplayKeyframesPovAccess rAccess)
                {
                    RecordedPovActions rActions = rAccess.bbsPov$getActions();
                    if (rActions != null)
                    {
                        int looping = r.looping.get();
                        float rTick = looping > 0 ? (filmTick % looping) : filmTick;
                        collectExecutionTexts(rActions, 0F, rTick, activeList, barVisible);
                    }
                }
            }
        }
        else if (actions != null)
        {
            collectExecutionTexts(actions, 0F, replayTick, activeList, barVisible);
        }

        if (activeList.isEmpty())
        {
            return;
        }

        // Sort by global tick ascending (oldest first, newest last)
        activeList.sort((a, b) -> Float.compare(a.globalTick, b.globalTick));

        int bottomY = height - 40;
        int boxX1 = 2;

        int chatWidth = (mc.inGameHud != null && mc.inGameHud.getChatHud() != null)
            ? mc.inGameHud.getChatHud().getWidth()
            : ((mc.options != null && mc.options.getChatWidth() != null)
                ? (int) Math.ceil(mc.options.getChatWidth().getValue() * 280.0D + 40.0D)
                : 320);
        int boxX2 = Math.min(boxX1 + chatWidth + 4, width - 2);
        int textX = boxX1 + 4;

        if (barVisible)
        {
            int maxVisibleLines = 20;
            if (mc.options != null && mc.options.getChatHeightFocused() != null)
            {
                maxVisibleLines = Math.max(1, (int) Math.floor((mc.options.getChatHeightFocused().getValue() * 160.0D + 20.0D) / 9.0D));
            }
            maxVisibleLines = Math.min(maxVisibleLines, Math.max(1, (bottomY - 20) / 9));

            // Open Chat Mode: all messages up to current tick are available in chat history
            int totalLines = activeList.size();
            int maxScroll = Math.max(0, totalLines - maxVisibleLines);
            int clampedScroll = Math.max(0, Math.min(scrollOffset, maxScroll));

            for (int i = 0; i < maxVisibleLines; i++)
            {
                int itemIdx = (totalLines - 1) - clampedScroll - i;
                if (itemIdx < 0 || itemIdx >= totalLines)
                {
                    break;
                }

                ActiveExecutionText item = activeList.get(itemIdx);
                int lineY = bottomY - (i * 9);
                if (lineY < 10)
                {
                    break;
                }

                String cleanText = item.text.replace("\r", "").replace("\n", "");
                // Background spans full chat width with vanilla background opacity using Batcher2D
                batcher.box(boxX1, lineY - 1, boxX2, lineY + 8, getChatBgColor(1.0F));
                batcher.flush();

                // Left vertical indicator bar for non-player / system / command messages (Vanilla MessageIndicator.SYSTEM = 13684944 / 0xD0D0D0)
                String plainText = cleanText.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
                if (!plainText.startsWith("<"))
                {
                    batcher.box(boxX1, lineY - 1, boxX1 + 2, lineY + 8, 0xFFD0D0D0);
                    batcher.flush();
                }

                if (context != null)
                {
                    context.drawText(font, cleanText, textX, lineY, 0xFFFFFFFF, true);
                }
            }

            // Render vanilla dual-tone slim scrollbar inside the right edge of the chat background box
            if (totalLines > maxVisibleLines)
            {
                int u = maxVisibleLines * 9;
                int t = totalLines * 9;
                int w = Math.max(4, u * u / t);
                int v = clampedScroll * u / totalLines;
                int thumbBottom = bottomY + 8 - v;
                int thumbTop = thumbBottom - w;

                int barX = boxX2 - 4;
                batcher.box(barX, thumbTop, barX + 1, thumbBottom, 0xFF4A4D6D);
                batcher.box(barX + 1, thumbTop, barX + 2, thumbBottom, 0xFF8D92B8);
                batcher.flush();
            }
        }
        else
        {
            int maxHudLines = 10;
            if (mc.options != null && mc.options.getChatHeightUnfocused() != null)
            {
                maxHudLines = Math.max(1, (int) Math.floor((mc.options.getChatHeightUnfocused().getValue() * 160.0D + 20.0D) / 9.0D));
            }
            maxHudLines = Math.min(maxHudLines, Math.max(1, (bottomY - 20) / 9));

            // Closed HUD Mode: display recent messages fading out after their duration
            int renderedHudCount = 0;
            for (int i = activeList.size() - 1; i >= 0 && renderedHudCount < maxHudLines; i--)
            {
                ActiveExecutionText item = activeList.get(i);
                int lineY = bottomY - (renderedHudCount * 9);

                if (lineY < 0)
                {
                    break;
                }

                float remaining = item.duration - item.age;
                float alpha = 1.0F;
                if (remaining < 20F)
                {
                    alpha = Math.max(0.0F, remaining / 20.0F);
                }

                int textAlpha = (int) (255 * alpha);
                if (textAlpha < 4)
                {
                    continue;
                }

                int textColor = (textAlpha << 24) | 0xFFFFFF;

                String cleanText = item.text.replace("\r", "").replace("\n", "");
                batcher.box(boxX1, lineY - 1, boxX2, lineY + 8, getChatBgColor(alpha));
                batcher.flush();

                String plainText = cleanText.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
                if (!plainText.startsWith("<"))
                {
                    int tagColor = (textAlpha << 24) | 0xD0D0D0;
                    batcher.box(boxX1, lineY - 1, boxX1 + 2, lineY + 8, tagColor);
                    batcher.flush();
                }

                if (context != null)
                {
                    context.drawText(font, cleanText, textX, lineY, textColor, true);
                }
                renderedHudCount++;
            }
        }
    }

    public static void renderLiveHistory(DrawContext context, int width, int height)
    {
        if (context == null)
        {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.inGameHud == null || mc.inGameHud.getChatHud() == null)
        {
            return;
        }

        TextRenderer font = mc.textRenderer;
        if (font == null)
        {
            return;
        }

        List<ChatHudLine.Visible> visibleMessages = ((ChatHudPovAccessor) mc.inGameHud.getChatHud()).bbsPov$getVisibleMessages();
        int scrolledLines = ((ChatHudPovAccessor) mc.inGameHud.getChatHud()).bbsPov$getScrolledLines();

        if (visibleMessages == null || visibleMessages.isEmpty())
        {
            return;
        }

        int bottomY = height - 40;
        int boxX1 = 2;
        int chatWidth = 320;
        if (mc.options != null && mc.options.getChatWidth() != null)
        {
            chatWidth = (int) Math.ceil(mc.options.getChatWidth().getValue() * 280.0D + 40.0D);
        }
        int boxX2 = Math.min(boxX1 + chatWidth + 4, width - 2);
        int textX = boxX1 + 4;

        int maxVisibleLines = 20;
        if (mc.options != null && mc.options.getChatHeightFocused() != null)
        {
            maxVisibleLines = Math.max(1, (int) Math.floor((mc.options.getChatHeightFocused().getValue() * 160.0D + 20.0D) / 9.0D));
        }
        maxVisibleLines = Math.min(maxVisibleLines, Math.max(1, (bottomY - 20) / 9));

        int totalLines = visibleMessages.size();
        int maxScroll = Math.max(0, totalLines - maxVisibleLines);
        int clampedScroll = Math.max(0, Math.min(scrolledLines, maxScroll));

        for (int i = 0; i < maxVisibleLines; i++)
        {
            int itemIdx = clampedScroll + i;
            if (itemIdx < 0 || itemIdx >= totalLines)
            {
                break;
            }

            ChatHudLine.Visible visibleLine = visibleMessages.get(itemIdx);
            if (visibleLine == null)
            {
                continue;
            }

            int lineY = bottomY - (i * 9);
            if (lineY < 10)
            {
                break;
            }

            context.fill(boxX1, lineY - 1, boxX2, lineY + 8, getChatBgColor(1.0F));

            if (visibleLine.indicator() != null)
            {
                int tagColor = (255 << 24) | (visibleLine.indicator().indicatorColor() & 0xFFFFFF);
                context.fill(boxX1, lineY - 1, boxX1 + 2, lineY + 8, tagColor);
            }

            context.drawText(font, visibleLine.content(), textX, lineY, 0xFFFFFFFF, true);
        }

        if (totalLines > maxVisibleLines)
        {
            int u = maxVisibleLines * 9;
            int t = totalLines * 9;
            int w = Math.max(4, u * u / t);
            int v = clampedScroll * u / totalLines;
            int thumbBottom = bottomY + 8 - v;
            int thumbTop = thumbBottom - w;

            int barX = boxX2 - 4;
            context.fill(barX, thumbTop, barX + 1, thumbBottom, 0xFF4A4D6D);
            context.fill(barX + 1, thumbTop, barX + 2, thumbBottom, 0xFF8D92B8);
        }
    }

    private static void collectExecutionTexts(RecordedPovActions actions, float baseOffset, float currentTick, List<ActiveExecutionText> activeList, boolean barVisible)
    {
        for (ChatPovActionClip chatClip : actions.getClips(ChatPovActionClip.class))
        {
            if (chatClip != null && chatClip.executedText != null && !chatClip.executedText.isEmpty())
            {
                float clipStart = baseOffset + chatClip.tick.get();

                for (Keyframe<String> kf : chatClip.executedText.getList())
                {
                    if (kf == null || kf.getValue() == null || kf.getValue().trim().isEmpty())
                    {
                        continue;
                    }

                    String rawVal = kf.getValue();
                    if (Glaxium.POV.actions.chat.editor.UIExecutedTextKeyframeFactory.isHiddenFromHud(rawVal) && !barVisible)
                    {
                        continue;
                    }

                    float kfLocalTick = kf.getTick();
                    float globalKfTick = clipStart + kfLocalTick;
                    float age = currentTick - globalKfTick;
                    float duration = kf.getDuration() > 0 ? kf.getDuration() : 200F;

                    if (age < 0F)
                    {
                        continue;
                    }

                    if (!barVisible && age >= duration)
                    {
                        continue;
                    }

                    String val = Glaxium.POV.actions.chat.editor.UIExecutedTextKeyframeFactory.getRawText(rawVal).replace("\r", "");
                    String[] lines = val.split("\n");
                    for (int lineIdx = 0; lineIdx < lines.length; lineIdx++)
                    {
                        String line = lines[lineIdx];
                        if (line != null && !line.trim().isEmpty())
                        {
                            activeList.add(new ActiveExecutionText(globalKfTick + lineIdx * 0.0001F, age, duration, line));
                        }
                    }
                }
            }
        }
    }

    private static class ActiveExecutionText
    {
        final float globalTick;
        final float age;
        final float duration;
        final String text;

        ActiveExecutionText(float globalTick, float age, float duration, String text)
        {
            this.globalTick = globalTick;
            this.age = age;
            this.duration = duration;
            this.text = text;
        }
    }
}

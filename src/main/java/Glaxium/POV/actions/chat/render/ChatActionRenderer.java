package Glaxium.POV.actions.chat.render;

import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.actions.clip.ChatPovActionClip;
import Glaxium.POV.hud.HotbarLayoutTransform;
import Glaxium.POV.hud.HudState;
import Glaxium.POV.integration.mixin.minecraft.ChatScreenPovAccessor;
import Glaxium.POV.integration.mixin.minecraft.TextFieldWidgetPovAccessor;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;

/** Main coordinator for rendering chat playback HUD, live chat recording overlay, and chat action clips. */
public final class ChatActionRenderer
{
    private ChatActionRenderer()
    {
    }

    public static void renderHUD(Batcher2D batcher, RecordedPovActions actions, float tick, int width, int height, HudState state)
    {
        renderHUD(batcher, null, actions, tick, tick, width, height, state);
    }

    public static void renderHUD(Batcher2D batcher, Film film, RecordedPovActions actions, float replayTick, float filmTick, int width, int height, HudState state)
    {
        if (actions == null && film == null)
        {
            return;
        }

        DrawContext context = batcher.getContext();
        if (context == null)
        {
            return;
        }

        ChatPovActionClip active = actions != null ? actions.getActiveChat(replayTick) : null;
        boolean barVis = active != null && (active.barVisible.isEmpty() || active.barVisible.interpolate(replayTick - active.tick.get(), true));
        int scrollOffset = 0;
        if (active != null)
        {
            scrollOffset = active.chatScroll.interpolate(replayTick - active.tick.get(), 0);
        }

        ChatHistoryRenderer.renderPlaybackHistory(batcher, film, actions, replayTick, filmTick, width, height, barVis, scrollOffset);

        if (active == null)
        {
            return;
        }

        float curX = -1000F;
        float curY = -1000F;
        boolean curVis = false;
        if (state != null && state.cursorVisible && state.cursorLayout != null)
        {
            curVis = true;
            curX = (width / 2F) + (float) state.cursorLayout.translate.x * HotbarLayoutTransform.PIXELS_PER_UNIT;
            curY = (height / 2F) - (float) state.cursorLayout.translate.y * HotbarLayoutTransform.PIXELS_PER_UNIT;
        }

        renderChatClip(batcher, active, replayTick, width, height, curX, curY, curVis);
    }

    public static void renderChatClip(Batcher2D batcher, ChatPovActionClip clip, float tick, int width, int height, float cursorX, float cursorY, boolean cursorVisible)
    {
        float localTick = tick - clip.tick.get();
        if (localTick < 0F || localTick > clip.duration.get())
        {
            return;
        }

        boolean barVisible = clip.barVisible.isEmpty() || clip.barVisible.interpolate(localTick, true);
        if (!barVisible)
        {
            return;
        }

        String text = clip.text.interpolate(localTick, "");
        int cursorPos = clip.cursorPos.interpolate(localTick, text.length());
        int selStart = clip.selStart.interpolate(localTick, -1);
        int selEnd = clip.selEnd.interpolate(localTick, -1);
        boolean showRecs = clip.showRecommendations.get();

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer font = mc.textRenderer;
        if (font == null)
        {
            return;
        }

        ChatInputBarRenderer.renderInputBar(batcher, font, text, cursorPos, selStart, selEnd, showRecs, width, height, cursorX, cursorY, cursorVisible);
    }

    public static void renderLiveChat(Batcher2D batcher, ChatScreen chatScreen, int width, int height)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null)
        {
            return;
        }
        DrawContext context = batcher.getContext();
        if (context == null)
        {
            return;
        }
        TextRenderer font = mc.textRenderer;
        if (font == null)
        {
            return;
        }

        double mouseX = mc.mouse.getX() * (double) mc.getWindow().getScaledWidth() / (double) mc.getWindow().getWidth();
        double mouseY = mc.mouse.getY() * (double) mc.getWindow().getScaledHeight() / (double) mc.getWindow().getHeight();
        float cursorX = (float) mouseX;
        float cursorY = (float) mouseY;

        // 1. Live server chat history
        ChatHistoryRenderer.renderLiveHistory(context, width, height);

        // 2. Live typing input bar
        TextFieldWidget field = ((ChatScreenPovAccessor) chatScreen).bbsPov$getChatField();
        if (field != null)
        {
            String text = field.getText();
            int cursorPos = field.getCursor();
            int selStart = ((TextFieldWidgetPovAccessor) field).bbsPov$getSelectionStart();
            int selEnd = ((TextFieldWidgetPovAccessor) field).bbsPov$getSelectionEnd();

            ChatInputBarRenderer.renderInputBar(batcher, font, text, cursorPos, selStart, selEnd, true, width, height, cursorX, cursorY, true);
        }
    }

    public static void renderExecutionTexts(Batcher2D batcher, RecordedPovActions actions, float tick, int width, int height, boolean barVisible)
    {
        ChatHistoryRenderer.renderPlaybackHistory(batcher, null, actions, tick, tick, width, height, barVisible, 0);
    }

    public static void renderExecutionTexts(Batcher2D batcher, RecordedPovActions actions, float tick, int width, int height, boolean barVisible, int scrollOffset)
    {
        ChatHistoryRenderer.renderPlaybackHistory(batcher, null, actions, tick, tick, width, height, barVisible, scrollOffset);
    }

    public static void renderExecutionTexts(Batcher2D batcher, Film film, RecordedPovActions actions, float replayTick, float filmTick, int width, int height, boolean barVisible, int scrollOffset)
    {
        ChatHistoryRenderer.renderPlaybackHistory(batcher, film, actions, replayTick, filmTick, width, height, barVisible, scrollOffset);
    }
}

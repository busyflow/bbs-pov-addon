package Glaxium.POV.actions.chat.render;

import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.actions.clip.ChatPovActionClip;
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

public final class ChatActionRenderer {
   private ChatActionRenderer() {
   }

   public static void renderHUD(Batcher2D batcher, RecordedPovActions actions, float tick, int width, int height, HudState state) {
      renderHUD(batcher, null, actions, tick, tick, width, height, state);
   }

   public static void renderHUD(
      Batcher2D batcher, Film film, RecordedPovActions actions, float replayTick, float filmTick, int width, int height, HudState state
   ) {
      if (actions != null || film != null) {
         DrawContext context = batcher.getContext();
         if (context != null) {
            ChatPovActionClip active = actions != null ? actions.getActiveChat(replayTick) : null;
            boolean barVis = active != null && (Boolean)active.barVisible.interpolate(replayTick - (float)((Integer)active.tick.get()).intValue(), false);
            int scrollOffset = 0;
            if (active != null) {
               scrollOffset = (Integer)active.chatScroll.interpolate(replayTick - (float)((Integer)active.tick.get()).intValue(), 0);
            }

            ChatHistoryRenderer.renderPlaybackHistory(context, film, actions, replayTick, filmTick, width, height, barVis, scrollOffset);
            if (active != null) {
               float curX = -1000.0F;
               float curY = -1000.0F;
               boolean curVis = false;
               if (state != null && state.cursorVisible && state.cursorLayout != null) {
                  curVis = true;
                  curX = (float)width / 2.0F + state.cursorLayout.translate.x * 2.0F;
                  curY = (float)height / 2.0F - state.cursorLayout.translate.y * 2.0F;
               }

               renderChatClip(context, active, replayTick, width, height, curX, curY, curVis);
            }
         }
      }
   }

   public static void renderChatClip(
      DrawContext context, ChatPovActionClip clip, float tick, int width, int height, float cursorX, float cursorY, boolean cursorVisible
   ) {
      float localTick = tick - (float)((Integer)clip.tick.get()).intValue();
      if (!(localTick < 0.0F) && !(localTick > (float)((Integer)clip.duration.get()).intValue())) {
         boolean barVisible = (Boolean)clip.barVisible.interpolate(localTick, true);
         if (barVisible) {
            String text = (String)clip.text.interpolate(localTick, "");
            int cursorPos = (Integer)clip.cursorPos.interpolate(localTick, text.length());
            int selStart = (Integer)clip.selStart.interpolate(localTick, -1);
            int selEnd = (Integer)clip.selEnd.interpolate(localTick, -1);
            boolean showRecs = (Boolean)clip.showRecommendations.get();
            MinecraftClient mc = MinecraftClient.getInstance();
            TextRenderer font = mc.textRenderer;
            if (font != null) {
               ChatInputBarRenderer.renderInputBar(context, font, text, cursorPos, selStart, selEnd, showRecs, width, height, cursorX, cursorY, cursorVisible);
            }
         }
      }
   }

   public static void renderLiveChat(Batcher2D batcher, ChatScreen chatScreen, int width, int height) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc != null) {
         DrawContext context = batcher.getContext();
         if (context != null) {
            TextRenderer font = mc.textRenderer;
            if (font != null) {
               double mouseX = mc.mouse.getX() * (double)mc.getWindow().getScaledWidth() / (double)mc.getWindow().getWidth();
               double mouseY = mc.mouse.getY() * (double)mc.getWindow().getScaledHeight() / (double)mc.getWindow().getHeight();
               float cursorX = (float)mouseX;
               float cursorY = (float)mouseY;
               ChatHistoryRenderer.renderLiveHistory(context, width, height);
               TextFieldWidget field = ((ChatScreenPovAccessor)chatScreen).bbsPov$getChatField();
               if (field != null) {
                  String text = field.getText();
                  int cursorPos = field.getCursor();
                  int selStart = ((TextFieldWidgetPovAccessor)field).bbsPov$getSelectionStart();
                  int selEnd = ((TextFieldWidgetPovAccessor)field).bbsPov$getSelectionEnd();
                  ChatInputBarRenderer.renderInputBar(context, font, text, cursorPos, selStart, selEnd, true, width, height, cursorX, cursorY, true);
               }
            }
         }
      }
   }

   public static void renderExecutionTexts(DrawContext context, RecordedPovActions actions, float tick, int width, int height, boolean barVisible) {
      ChatHistoryRenderer.renderPlaybackHistory(context, null, actions, tick, tick, width, height, barVisible, 0);
   }

   public static void renderExecutionTexts(
      DrawContext context, RecordedPovActions actions, float tick, int width, int height, boolean barVisible, int scrollOffset
   ) {
      ChatHistoryRenderer.renderPlaybackHistory(context, null, actions, tick, tick, width, height, barVisible, scrollOffset);
   }

   public static void renderExecutionTexts(
      DrawContext context, Film film, RecordedPovActions actions, float replayTick, float filmTick, int width, int height, boolean barVisible, int scrollOffset
   ) {
      ChatHistoryRenderer.renderPlaybackHistory(context, film, actions, replayTick, filmTick, width, height, barVisible, scrollOffset);
   }
}

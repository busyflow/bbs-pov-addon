package Glaxium.POV.actions.chat.render;

import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.actions.chat.editor.UIExecutedTextKeyframeFactory;
import Glaxium.POV.actions.clip.ChatPovActionClip;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.integration.mixin.minecraft.ChatHudPovAccessor;
import java.util.ArrayList;
import java.util.List;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHudLine.Visible;

public final class ChatHistoryRenderer {
   private ChatHistoryRenderer() {
   }

   public static void renderPlaybackHistory(
      DrawContext context, Film film, RecordedPovActions actions, float replayTick, float filmTick, int width, int height, boolean barVisible, int scrollOffset
   ) {
      if (context != null) {
         MinecraftClient mc = MinecraftClient.getInstance();
         TextRenderer font = mc.textRenderer;
         if (font != null) {
            List<ChatHistoryRenderer.ActiveExecutionText> activeList = new ArrayList<>();
            if (film != null && film.replays != null && !film.replays.getList().isEmpty()) {
               for (Replay r : film.replays.getList()) {
                  if (r != null) {
                     ReplayKeyframes boxX2 = r.keyframes;
                     if (boxX2 instanceof ReplayKeyframesPovAccess) {
                        ReplayKeyframesPovAccess rAccess = (ReplayKeyframesPovAccess)boxX2;
                        RecordedPovActions rActions = rAccess.bbsPov$getActions();
                        if (rActions != null) {
                           int looping = (Integer)r.looping.get();
                           float rTick = looping > 0 ? filmTick % (float)looping : filmTick;
                           collectExecutionTexts(rActions, 0.0F, rTick, activeList, barVisible);
                        }
                     }
                  }
               }
            } else if (actions != null) {
               collectExecutionTexts(actions, 0.0F, replayTick, activeList, barVisible);
            }

            if (!activeList.isEmpty()) {
               activeList.sort((a, b) -> Float.compare(a.globalTick, b.globalTick));
               int bottomY = height - 40;
               int boxX1 = 2;
               int chatWidth = 320;
               if (mc.options != null && mc.options.getChatWidth() != null) {
                  chatWidth = (int)Math.ceil((Double)mc.options.getChatWidth().getValue() * 280.0 + 40.0);
               }

               int boxX2 = Math.min(boxX1 + chatWidth + 4, width - 2);
               int textX = boxX1 + 4;
               if (barVisible) {
                  int maxVisibleLines = 20;
                  if (mc.options != null && mc.options.getChatHeightFocused() != null) {
                     maxVisibleLines = Math.max(1, (int)Math.floor(((Double)mc.options.getChatHeightFocused().getValue() * 160.0 + 20.0) / 9.0));
                  }

                  maxVisibleLines = Math.min(maxVisibleLines, Math.max(1, (bottomY - 20) / 9));
                  int totalLines = activeList.size();
                  int maxScroll = Math.max(0, totalLines - maxVisibleLines);
                  int clampedScroll = Math.max(0, Math.min(scrollOffset, maxScroll));

                  for (int i = 0; i < maxVisibleLines; i++) {
                     int itemIdx = totalLines - 1 - clampedScroll - i;
                     if (itemIdx < 0 || itemIdx >= totalLines) {
                        break;
                     }

                     ChatHistoryRenderer.ActiveExecutionText item = activeList.get(itemIdx);
                     int lineY = bottomY - i * 9;
                     if (lineY < 10) {
                        break;
                     }

                     String cleanText = item.text.replace("\r", "").replace("\n", "");
                     context.fill(boxX1, lineY - 1, boxX2, lineY + 8, Integer.MIN_VALUE);
                     String plainText = cleanText.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
                     if (!plainText.startsWith("<")) {
                        context.fill(boxX1, lineY - 1, boxX1 + 2, lineY + 8, -3092272);
                     }

                     context.drawText(font, cleanText, textX, lineY, -1, true);
                  }

                  if (totalLines > maxVisibleLines) {
                     int u = maxVisibleLines * 9;
                     int t = totalLines * 9;
                     int w = Math.max(4, u * u / t);
                     int v = clampedScroll * u / totalLines;
                     int thumbBottom = bottomY + 8 - v;
                     int thumbTop = thumbBottom - w;
                     int barX = boxX2 - 4;
                     context.fill(barX, thumbTop, barX + 1, thumbBottom, -11907731);
                     context.fill(barX + 1, thumbTop, barX + 2, thumbBottom, -7499080);
                  }
               } else {
                  int maxHudLines = 10;
                  if (mc.options != null && mc.options.getChatHeightUnfocused() != null) {
                     maxHudLines = Math.max(1, (int)Math.floor(((Double)mc.options.getChatHeightUnfocused().getValue() * 160.0 + 20.0) / 9.0));
                  }

                  maxHudLines = Math.min(maxHudLines, Math.max(1, (bottomY - 20) / 9));
                  int renderedHudCount = 0;

                  for (int i = activeList.size() - 1; i >= 0 && renderedHudCount < maxHudLines; i--) {
                     ChatHistoryRenderer.ActiveExecutionText itemx = activeList.get(i);
                     int lineYx = bottomY - renderedHudCount * 9;
                     if (lineYx < 0) {
                        break;
                     }

                     float remaining = itemx.duration - itemx.age;
                     float alpha = 1.0F;
                     if (remaining < 20.0F) {
                        alpha = Math.max(0.0F, remaining / 20.0F);
                     }

                     if (!(alpha <= 0.01F)) {
                        int bgAlpha = (int)(128.0F * alpha);
                        int textAlpha = (int)(255.0F * alpha);
                        int bgColor = bgAlpha << 24;
                        int textColor = textAlpha << 24 | 16777215;
                        String cleanText = itemx.text.replace("\r", "").replace("\n", "");
                        context.fill(boxX1, lineYx - 1, boxX2, lineYx + 8, bgColor);
                        String plainText = cleanText.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
                        if (!plainText.startsWith("<")) {
                           int tagColor = textAlpha << 24 | 13684944;
                           context.fill(boxX1, lineYx - 1, boxX1 + 2, lineYx + 8, tagColor);
                        }

                        context.drawText(font, cleanText, textX, lineYx, textColor, true);
                        renderedHudCount++;
                     }
                  }
               }
            }
         }
      }
   }

   public static void renderLiveHistory(DrawContext context, int width, int height) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc != null && mc.inGameHud != null && mc.inGameHud.getChatHud() != null) {
         TextRenderer font = mc.textRenderer;
         if (font != null) {
            List<Visible> visibleMessages = ((ChatHudPovAccessor)mc.inGameHud.getChatHud()).bbsPov$getVisibleMessages();
            int scrolledLines = ((ChatHudPovAccessor)mc.inGameHud.getChatHud()).bbsPov$getScrolledLines();
            if (visibleMessages != null && !visibleMessages.isEmpty()) {
               int bottomY = height - 40;
               int boxX1 = 2;
               int chatWidth = 320;
               if (mc.options != null && mc.options.getChatWidth() != null) {
                  chatWidth = (int)Math.ceil((Double)mc.options.getChatWidth().getValue() * 280.0 + 40.0);
               }

               int boxX2 = Math.min(boxX1 + chatWidth + 4, width - 2);
               int textX = boxX1 + 4;
               int maxVisibleLines = 20;
               if (mc.options != null && mc.options.getChatHeightFocused() != null) {
                  maxVisibleLines = Math.max(1, (int)Math.floor(((Double)mc.options.getChatHeightFocused().getValue() * 160.0 + 20.0) / 9.0));
               }

               maxVisibleLines = Math.min(maxVisibleLines, Math.max(1, (bottomY - 20) / 9));
               int totalLines = visibleMessages.size();
               int maxScroll = Math.max(0, totalLines - maxVisibleLines);
               int clampedScroll = Math.max(0, Math.min(scrolledLines, maxScroll));

               for (int i = 0; i < maxVisibleLines; i++) {
                  int itemIdx = clampedScroll + i;
                  if (itemIdx < 0 || itemIdx >= totalLines) {
                     break;
                  }

                  Visible visibleLine = visibleMessages.get(itemIdx);
                  if (visibleLine != null) {
                     int lineY = bottomY - i * 9;
                     if (lineY < 10) {
                        break;
                     }

                     context.fill(boxX1, lineY - 1, boxX2, lineY + 8, Integer.MIN_VALUE);
                     if (visibleLine.indicator() != null) {
                        int tagColor = 0xFF000000 | visibleLine.indicator().indicatorColor() & 16777215;
                        context.fill(boxX1, lineY - 1, boxX1 + 2, lineY + 8, tagColor);
                     }

                     context.drawText(font, visibleLine.content(), textX, lineY, -1, true);
                  }
               }

               if (totalLines > maxVisibleLines) {
                  int u = maxVisibleLines * 9;
                  int t = totalLines * 9;
                  int w = Math.max(4, u * u / t);
                  int v = clampedScroll * u / totalLines;
                  int thumbBottom = bottomY + 8 - v;
                  int thumbTop = thumbBottom - w;
                  int barX = boxX2 - 4;
                  context.fill(barX, thumbTop, barX + 1, thumbBottom, -11907731);
                  context.fill(barX + 1, thumbTop, barX + 2, thumbBottom, -7499080);
               }
            }
         }
      }
   }

   private static void collectExecutionTexts(
      RecordedPovActions actions, float baseOffset, float currentTick, List<ChatHistoryRenderer.ActiveExecutionText> activeList, boolean barVisible
   ) {
      for (ChatPovActionClip chatClip : actions.getClips(ChatPovActionClip.class)) {
         if (chatClip != null && chatClip.executedText != null && !chatClip.executedText.isEmpty()) {
            float clipStart = baseOffset + (float)((Integer)chatClip.tick.get()).intValue();

            for (Keyframe<String> kf : chatClip.executedText.getList()) {
               if (kf != null && kf.getValue() != null && !((String)kf.getValue()).trim().isEmpty()) {
                  String rawVal = (String)kf.getValue();
                  if (!UIExecutedTextKeyframeFactory.isHiddenFromHud(rawVal) || barVisible) {
                     float kfLocalTick = kf.getTick();
                     float globalKfTick = clipStart + kfLocalTick;
                     float age = currentTick - globalKfTick;
                     float duration = kf.getDuration() > 0.0F ? kf.getDuration() : 200.0F;
                     if (!(age < 0.0F) && (barVisible || !(age >= duration))) {
                        String val = UIExecutedTextKeyframeFactory.getRawText(rawVal).replace("\r", "");
                        String[] lines = val.split("\n");

                        for (int lineIdx = 0; lineIdx < lines.length; lineIdx++) {
                           String line = lines[lineIdx];
                           if (line != null && !line.trim().isEmpty()) {
                              activeList.add(new ChatHistoryRenderer.ActiveExecutionText(globalKfTick + (float)lineIdx * 1.0E-4F, age, duration, line));
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static class ActiveExecutionText {
      final float globalTick;
      final float age;
      final float duration;
      final String text;

      ActiveExecutionText(float globalTick, float age, float duration, String text) {
         this.globalTick = globalTick;
         this.age = age;
         this.duration = duration;
         this.text = text;
      }
   }
}

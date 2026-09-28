package Glaxium.POV.actions.chat.recording;

import Glaxium.POV.actions.PovActionType;
import Glaxium.POV.actions.clip.ChatPovActionClip;
import Glaxium.POV.config.PovSettings;
import Glaxium.POV.hud.RecordedHudData;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.integration.mixin.minecraft.ChatHudPovAccessor;
import Glaxium.POV.integration.mixin.minecraft.ChatScreenPovAccessor;
import Glaxium.POV.integration.mixin.minecraft.TextFieldWidgetPovAccessor;
import java.util.Objects;
import java.util.Optional;
import mchorse.bbs_mod.film.Recorder;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ChatRecorder {
   private ChatPovActionClip recordingChatClip;
   private String lastText = null;
   private Boolean lastBarVisible = null;
   private Integer lastCursor = null;
   private Integer lastSelStart = null;
   private Integer lastSelEnd = null;
   private Integer lastScroll = null;
   private float lastRecordedCurTx = Float.NaN;
   private float lastRecordedCurTy = Float.NaN;
   private float lastCursorKeyTick = Float.NaN;
   private static ChatRecorder activeInstance = null;
   private static int currentRecorderTick = 0;
   private static float lastExecutedLocalTick = -1.0F;
   private static int executedTextCounter = 0;
   private static boolean replayingExternalMessage = false;

   public static void setReplayingExternalMessage(boolean replaying) {
      replayingExternalMessage = replaying;
   }

   public void reset() {
      activeInstance = null;
      this.recordingChatClip = null;
      this.lastText = null;
      this.lastBarVisible = null;
      this.lastCursor = null;
      this.lastSelStart = null;
      this.lastSelEnd = null;
      this.lastScroll = null;
      this.lastRecordedCurTx = Float.NaN;
      this.lastRecordedCurTy = Float.NaN;
      this.lastCursorKeyTick = Float.NaN;
      lastExecutedLocalTick = -1.0F;
      executedTextCounter = 0;
      replayingExternalMessage = false;
   }

   public static void onChatMessageReceived(Text message) {
      if (!replayingExternalMessage && activeInstance != null && activeInstance.recordingChatClip != null && message != null) {
         String formatted = toFormattedString(message);
         if (formatted != null && !formatted.trim().isEmpty()) {
            float localTick = (float)(currentRecorderTick - (Integer)activeInstance.recordingChatClip.tick.get());
            if (localTick < 0.0F) {
               localTick = 0.0F;
            }

            String[] lines = formatted.split("\n");

            for (String line : lines) {
               if (line != null && !line.trim().isEmpty()) {
                  if (Math.abs(localTick - lastExecutedLocalTick) < 1.0E-4F) {
                     executedTextCounter++;
                  } else {
                     lastExecutedLocalTick = localTick;
                     executedTextCounter = 0;
                  }

                  float keyTick = localTick + (float)executedTextCounter * 0.001F;
                  activeInstance.recordingChatClip.executedText.insert(keyTick, line);
               }
            }
         }
      }
   }

   public static String toFormattedString(Text text) {
      if (text == null) {
         return "";
      } else {
         StringBuilder sb = new StringBuilder();
         text.visit((style, string) -> {
            if (style != null) {
               if (style.getColor() != null) {
                  Formatting formatting = Formatting.byName(style.getColor().getName());
                  if (formatting != null) {
                     sb.append(formatting.toString());
                  } else {
                     sb.append("§f");
                  }
               }

               if (style.isBold()) {
                  sb.append("§l");
               }

               if (style.isItalic()) {
                  sb.append("§o");
               }

               if (style.isUnderlined()) {
                  sb.append("§n");
               }

               if (style.isStrikethrough()) {
                  sb.append("§m");
               }

               if (style.isObfuscated()) {
                  sb.append("§k");
               }
            }

            sb.append(string);
            return Optional.empty();
         }, Style.EMPTY);
         return sb.toString();
      }
   }

   public void finish(ReplayKeyframesPovAccess access, int tick) {
      if (this.recordingChatClip != null) {
         int duration = Math.max(1, tick - (Integer)this.recordingChatClip.tick.get());
         this.recordingChatClip.duration.set(duration);
         this.recordingChatClip = null;
      }

      RecordedHudData hotbar = access != null ? access.bbsPov$getHud() : null;
      if (hotbar != null && this.lastBarVisible != null && this.lastBarVisible) {
         hotbar.cursorVisible.insert((float)tick, false);
      }
   }

   public void record(ReplayKeyframesPovAccess access, Recorder recorder) {
      if (PovSettings.isBakeActions()) {
         if (!recorder.hasNotStarted() && recorder.tick >= 0) {
            activeInstance = this;
            currentRecorderTick = recorder.tick;
            Screen screen = MinecraftClient.getInstance().currentScreen;
            RecordedHudData hotbar = access != null ? access.bbsPov$getHud() : null;
            if (screen instanceof ChatScreen chatScreen) {
               if (this.recordingChatClip == null) {
                  this.recordingChatClip = (ChatPovActionClip)access.bbsPov$getActions().add(PovActionType.CHAT, recorder.tick, 1);
                  this.recordingChatClip.createDefaultKeyframes();
                  this.lastText = null;
                  this.lastBarVisible = null;
                  this.lastCursor = null;
                  this.lastSelStart = null;
                  this.lastSelEnd = null;
                  if (hotbar != null) {
                     hotbar.cursorVisible.insert((float)recorder.tick, true);
                  }
               }

               float localTick = (float)(recorder.tick - (Integer)this.recordingChatClip.tick.get());
               this.recordingChatClip.duration.set(Math.max(1, (int)localTick + 1));
               TextFieldWidget field = ((ChatScreenPovAccessor)chatScreen).bbsPov$getChatField();
               if (field != null) {
                  String text = field.getText();
                  int cursor = field.getCursor();
                  int selStart = ((TextFieldWidgetPovAccessor)field).bbsPov$getSelectionStart();
                  int selEnd = ((TextFieldWidgetPovAccessor)field).bbsPov$getSelectionEnd();
                  if (this.lastBarVisible == null || !this.lastBarVisible) {
                     this.recordValue(this.recordingChatClip.barVisible, true, localTick, this.lastBarVisible);
                     this.lastBarVisible = true;
                     if (hotbar != null) {
                        hotbar.cursorVisible.insert((float)recorder.tick, true);
                     }
                  }

                  this.recordValue(this.recordingChatClip.text, text, localTick, this.lastText);
                  this.lastText = text;
                  this.recordValue(this.recordingChatClip.cursorPos, cursor, localTick, this.lastCursor);
                  this.lastCursor = cursor;
                  this.recordValue(this.recordingChatClip.selStart, selStart, localTick, this.lastSelStart);
                  this.lastSelStart = selStart;
                  this.recordValue(this.recordingChatClip.selEnd, selEnd, localTick, this.lastSelEnd);
                  this.lastSelEnd = selEnd;
                  int scroll = 0;
                  if (MinecraftClient.getInstance().inGameHud != null && MinecraftClient.getInstance().inGameHud.getChatHud() != null) {
                     scroll = ((ChatHudPovAccessor)MinecraftClient.getInstance().inGameHud.getChatHud()).bbsPov$getScrolledLines();
                  }

                  this.recordValue(this.recordingChatClip.chatScroll, scroll, localTick, this.lastScroll);
                  this.lastScroll = scroll;
               }
            } else if (this.recordingChatClip != null) {
               float localTick = (float)(recorder.tick - (Integer)this.recordingChatClip.tick.get());
               this.recordingChatClip.duration.set(Math.max(1, (int)localTick + 1));
               if (this.lastBarVisible == null || this.lastBarVisible) {
                  this.recordValue(this.recordingChatClip.barVisible, false, localTick, this.lastBarVisible);
                  this.lastBarVisible = false;
                  if (hotbar != null) {
                     hotbar.cursorVisible.insert((float)recorder.tick, false);
                  }
               }
            }
         }
      }
   }

   public void sampleCursor(ReplayKeyframesPovAccess access, Recorder recorder, float tickDelta) {
      if (this.recordingChatClip != null && !recorder.hasNotStarted() && recorder.tick >= 0) {
         Screen screen = MinecraftClient.getInstance().currentScreen;
         if (screen instanceof ChatScreen) {
            RecordedHudData hotbar = access != null ? access.bbsPov$getHud() : null;
            if (hotbar != null) {
               MinecraftClient mc = MinecraftClient.getInstance();
               double mouseX = mc.mouse.getX() * (double)mc.getWindow().getScaledWidth() / (double)mc.getWindow().getWidth();
               double mouseY = mc.mouse.getY() * (double)mc.getWindow().getScaledHeight() / (double)mc.getWindow().getHeight();
               double screenCenterX = (double)mc.getWindow().getScaledWidth() / 2.0;
               double screenCenterY = (double)mc.getWindow().getScaledHeight() / 2.0;
               float curTx = (float)((mouseX - screenCenterX) / 2.0);
               float curTy = (float)((screenCenterY - mouseY) / 2.0);
               float fraction = Math.max(0.0F, Math.min(1.0F, tickDelta));
               float absTick = (float)recorder.tick + fraction;
               KeyframeChannel<Transform> hudCursorLayout = hotbar.cursorLayout;
               if (Float.isNaN(this.lastCursorKeyTick)) {
                  hotbar.cursorVisible.insert(absTick, true);
                  this.insertCursorKey(hudCursorLayout, absTick, curTx, curTy);
                  this.lastRecordedCurTx = curTx;
                  this.lastRecordedCurTy = curTy;
                  this.lastCursorKeyTick = absTick;
               } else if (absTick > this.lastCursorKeyTick + 0.35F
                  && (Math.abs(curTx - this.lastRecordedCurTx) > 0.03F || Math.abs(curTy - this.lastRecordedCurTy) > 0.03F)) {
                  if (absTick - this.lastCursorKeyTick > 0.8F) {
                     this.insertCursorKey(hudCursorLayout, absTick - 0.1F, this.lastRecordedCurTx, this.lastRecordedCurTy);
                  }

                  this.insertCursorKey(hudCursorLayout, absTick, curTx, curTy);
                  this.lastRecordedCurTx = curTx;
                  this.lastRecordedCurTy = curTy;
                  this.lastCursorKeyTick = absTick;
               }
            }
         }
      }
   }

   private void insertCursorKey(KeyframeChannel<Transform> channel, float tick, float x, float y) {
      if (channel != null) {
         Transform transform = new Transform();
         transform.translate.set(x, y, 0.0F);
         int index = channel.insert(tick, transform);
         if (index >= 0 && index < channel.getKeyframes().size()) {
            ((Keyframe)channel.getKeyframes().get(index)).getInterpolation().setInterp(Interpolations.LINEAR);
         }
      }
   }

   private <T> void recordValue(KeyframeChannel<T> channel, T value, float tick, T last) {
      if (channel.isEmpty() || !Objects.equals(value, last)) {
         channel.insert(tick, value);
      }
   }
}

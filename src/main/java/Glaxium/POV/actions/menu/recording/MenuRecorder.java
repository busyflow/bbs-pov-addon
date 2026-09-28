package Glaxium.POV.actions.menu.recording;

import Glaxium.POV.actions.PovActionType;
import Glaxium.POV.actions.clip.MenuPovActionClip;
import Glaxium.POV.actions.menu.render.MenuSleepOverlay;
import Glaxium.POV.actions.menu.schema.MenuTypeResolver;
import Glaxium.POV.config.PovSettings;
import Glaxium.POV.hud.RecordedHudData;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.integration.access.minecraft.DeathScreenPovAccess;
import java.util.List;
import java.util.Objects;
import mchorse.bbs_mod.film.Recorder;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SleepingChatScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

public final class MenuRecorder {
   private MenuPovActionClip recordingClip;
   private float lastRecordedCurTx = Float.NaN;
   private float lastRecordedCurTy = Float.NaN;
   private float lastCursorKeyTick = Float.NaN;
   private boolean cursorWasMoving;

   public void reset() {
      this.recordingClip = null;
      this.lastRecordedCurTx = Float.NaN;
      this.lastRecordedCurTy = Float.NaN;
      this.lastCursorKeyTick = Float.NaN;
      this.cursorWasMoving = false;
   }

   public void finish(ReplayKeyframesPovAccess access, int tick) {
      this.finalizeClip(access, tick);
   }

   public void record(ReplayKeyframesPovAccess access, Recorder recorder) {
      if (PovSettings.isBakeMenu()) {
         if (!recorder.hasNotStarted() && recorder.tick >= 0) {
            MinecraftClient client = MinecraftClient.getInstance();
            Screen screen = client.currentScreen;
            String menuType = MenuTypeResolver.resolve(screen);
            String recordingType = this.recordingClip != null && !this.recordingClip.state.isEmpty()
               ? (String)this.recordingClip.state.get(0).getValue()
               : null;
            int sleepTimer = client.player == null ? 0 : client.player.getSleepTimer();
            if (menuType == null && "sleep".equals(recordingType) && sleepTimer > 0) {
               menuType = "sleep";
            }

            if (menuType == null) {
               if ("sleep".equals(recordingType) && this.recordingClip != null) {
                  float localTick = (float)(recorder.tick - (Integer)this.recordingClip.tick.get());
                  this.recordValue(this.recordingClip.opacity, 0.0F, localTick);
                  this.recordValue(this.recordingClip.leaveBed, false, localTick);
               }

               this.finalizeClip(access, recorder.tick);
            } else {
               if (recordingType != null && !recordingType.equals(menuType)) {
                  this.finalizeClip(access, recorder.tick);
               }

               if (this.recordingClip == null) {
                  this.recordingClip = (MenuPovActionClip)access.bbsPov$getActions().add(PovActionType.MENU, recorder.tick, 1);
                  this.recordingClip.state.insert(0.0F, menuType);
                  this.recordingClip.cursorVisible.insert(0.0F, true);
                  RecordedHudData hud = access.bbsPov$getHud();
                  if (hud != null) {
                     hud.cursorVisible.insert((float)recorder.tick, true);
                  }
               }

               float localTick = (float)(recorder.tick - (Integer)this.recordingClip.tick.get());
               this.recordingClip.duration.set(Math.max(1, (int)localTick + 1));
               if (screen != null) {
                  this.recordCursorMotion(access, recorder, 0.0F);
                  this.recordValue(this.recordingClip.cursorVisible, true, localTick);
               } else {
                  this.recordValue(this.recordingClip.cursorVisible, false, localTick);
                  RecordedHudData hud = access == null ? null : access.bbsPov$getHud();
                  if (hud != null) {
                     this.recordValue(hud.cursorVisible, false, (float)recorder.tick);
                  }
               }

               this.recordTypeFields(screen, menuType, localTick, client.player);
            }
         }
      }
   }

   private void recordTypeFields(Screen screen, String menuType, float localTick, ClientPlayerEntity player) {
      if ("death".equals(menuType) && screen instanceof DeathScreen) {
         String message = "";
         String score = "Score: 0";
         if (screen instanceof DeathScreenPovAccess deathAccess) {
            Text deathMessage = deathAccess.bbsPov$getDeathMessage();
            Text scoreText = deathAccess.bbsPov$getScoreText();
            if (deathMessage != null) {
               message = deathMessage.getString();
            }

            if (scoreText != null) {
               score = scoreText.getString();
            }
         }

         this.recordValue(this.recordingClip.deathMessage, message, localTick);
         this.recordValue(this.recordingClip.score, score, localTick);
         this.recordValue(this.recordingClip.bgOpacity, 1.0F, localTick);
         boolean active = true;
         if (screen instanceof DeathScreenPovAccess deathAccess) {
            active = deathAccess.bbsPov$getTicksSinceDeath() >= 20;
         } else if (localTick < 20.0F) {
            active = false;
         }

         this.recordValue(this.recordingClip.buttonsActive, active, localTick);
      } else if ("sleep".equals(menuType)) {
         int timer = player == null ? 0 : player.getSleepTimer();
         this.recordValue(this.recordingClip.opacity, MenuSleepOverlay.progress(timer), localTick);
         this.recordValue(this.recordingClip.leaveBed, screen instanceof SleepingChatScreen, localTick);
      }
   }

   public void sampleCursor(ReplayKeyframesPovAccess access, Recorder recorder, float tickDelta) {
      if (this.recordingClip != null && !recorder.hasNotStarted() && recorder.tick >= 0) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.currentScreen != null) {
            this.recordCursorMotion(access, recorder, tickDelta);
         }
      }
   }

   private void recordCursorMotion(ReplayKeyframesPovAccess access, Recorder recorder, float tickDelta) {
      MinecraftClient client = MinecraftClient.getInstance();
      double mouseX = client.mouse.getX() * (double)client.getWindow().getScaledWidth() / (double)client.getWindow().getWidth();
      double mouseY = client.mouse.getY() * (double)client.getWindow().getScaledHeight() / (double)client.getWindow().getHeight();
      double centerX = (double)client.getWindow().getScaledWidth() / 2.0;
      double centerY = (double)client.getWindow().getScaledHeight() / 2.0;
      float curTx = (float)((mouseX - centerX) / 2.0);
      float curTy = (float)((centerY - mouseY) / 2.0);
      float fraction = Math.max(0.0F, Math.min(1.0F, tickDelta));
      float absTick = (float)recorder.tick + fraction;
      float localTick = absTick - (float)((Integer)this.recordingClip.tick.get()).intValue();
      RecordedHudData hud = access == null ? null : access.bbsPov$getHud();
      KeyframeChannel<Transform> clipCursor = this.recordingClip.cursorLayout;
      KeyframeChannel<Transform> hudCursor = hud == null ? null : hud.cursorLayout;
      boolean posChanged = Math.abs(curTx - this.lastRecordedCurTx) > 0.005F || Math.abs(curTy - this.lastRecordedCurTy) > 0.005F;
      if (Float.isNaN(this.lastCursorKeyTick)) {
         this.insertCursorKey(clipCursor, localTick, curTx, curTy);
         if (hudCursor != null) {
            this.insertCursorKey(hudCursor, absTick, curTx, curTy);
         }

         this.lastRecordedCurTx = curTx;
         this.lastRecordedCurTy = curTy;
         this.lastCursorKeyTick = absTick;
         this.cursorWasMoving = false;
      } else if (posChanged) {
         if (absTick > this.lastCursorKeyTick + 0.05F) {
            if (absTick - this.lastCursorKeyTick > 0.5F && !this.cursorWasMoving) {
               this.insertCursorKey(clipCursor, localTick - 0.05F, this.lastRecordedCurTx, this.lastRecordedCurTy);
               if (hudCursor != null) {
                  this.insertCursorKey(hudCursor, absTick - 0.05F, this.lastRecordedCurTx, this.lastRecordedCurTy);
               }
            }

            this.insertCursorKey(clipCursor, localTick, curTx, curTy);
            if (hudCursor != null) {
               this.insertCursorKey(hudCursor, absTick, curTx, curTy);
            }

            this.lastRecordedCurTx = curTx;
            this.lastRecordedCurTy = curTy;
            this.lastCursorKeyTick = absTick;
            this.cursorWasMoving = true;
         }
      } else if (this.cursorWasMoving) {
         this.insertCursorKey(clipCursor, localTick, curTx, curTy);
         if (hudCursor != null) {
            this.insertCursorKey(hudCursor, absTick, curTx, curTy);
         }

         this.lastRecordedCurTx = curTx;
         this.lastRecordedCurTy = curTy;
         this.lastCursorKeyTick = absTick;
         this.cursorWasMoving = false;
      }
   }

   private void finalizeClip(ReplayKeyframesPovAccess access, int tick) {
      if (this.recordingClip != null) {
         float localEnd = (float)(tick - (Integer)this.recordingClip.tick.get());
         if (!Float.isNaN(this.lastRecordedCurTx) && !Float.isNaN(this.lastCursorKeyTick)) {
            this.insertCursorKey(this.recordingClip.cursorLayout, localEnd, this.lastRecordedCurTx, this.lastRecordedCurTy);
         }

         this.recordingClip.cursorVisible.insert(localEnd, false);
         RecordedHudData hud = access == null ? null : access.bbsPov$getHud();
         if (hud != null) {
            float absTick = (float)tick;
            if (!Float.isNaN(this.lastRecordedCurTx) && !Float.isNaN(this.lastCursorKeyTick) && absTick > this.lastCursorKeyTick) {
               this.insertCursorKey(hud.cursorLayout, absTick, this.lastRecordedCurTx, this.lastRecordedCurTy);
            }

            hud.cursorVisible.insert(absTick, false);
         }

         this.recordingClip.ensureBakingBounds();
         this.recordingClip = null;
         this.lastRecordedCurTx = Float.NaN;
         this.lastRecordedCurTy = Float.NaN;
         this.lastCursorKeyTick = Float.NaN;
      }
   }

   private <T> void recordValue(KeyframeChannel<T> channel, T value, float tick) {
      if (channel != null) {
         if (channel.isEmpty()) {
            channel.insert(0.0F, value);
         } else {
            List<? extends Keyframe<T>> keyframes = channel.getKeyframes();
            Keyframe<T> previous = (Keyframe<T>)keyframes.get(keyframes.size() - 1);
            if (!Objects.equals(previous.getValue(), value)) {
               if (tick - previous.getTick() > 1.0F) {
                  channel.insert(tick - 1.0F, previous.getValue());
               }

               channel.insert(tick, value);
            }
         }
      }
   }

   private void insertCursorKey(KeyframeChannel<Transform> cursorLayout, float tick, float tx, float ty) {
      if (cursorLayout != null) {
         Transform transform = new Transform();
         transform.translate.set(tx, ty, 0.0F);
         int index = cursorLayout.insert(tick, transform);
         if (index >= 0 && index < cursorLayout.getKeyframes().size()) {
            ((Keyframe)cursorLayout.getKeyframes().get(index)).getInterpolation().setInterp(Interpolations.LINEAR);
         }
      }
   }
}

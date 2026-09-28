package Glaxium.POV.recording;

import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.actions.bossbar.recording.BossBarRecorder;
import Glaxium.POV.actions.camera.recording.CameraShakeRecorder;
import Glaxium.POV.actions.chat.editor.UIExecutedTextKeyframeFactory;
import Glaxium.POV.actions.chat.recording.ChatRecorder;
import Glaxium.POV.actions.clip.ChatPovActionClip;
import Glaxium.POV.actions.gui.recording.GuiRecorder;
import Glaxium.POV.actions.menu.recording.MenuRecorder;
import Glaxium.POV.actions.particle.recording.ParticleRecorder;
import Glaxium.POV.actions.screeneffect.recording.ScreenEffectRecorder;
import Glaxium.POV.actions.statuseffect.recording.StatusEffectRecorder;
import Glaxium.POV.actions.toast.recording.ToastRecorder;
import Glaxium.POV.config.PovSettings;
import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.hud.RecordedHudData;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import java.util.List;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.Recorder;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

public final class PovRecordingSession {
   private static PovRecordingSession current;
   private final Form recordingForm;
   private Film film;
   private int replayIndex;
   private int lastDispatchedTick = -1;
   private final GuiRecorder gui = new GuiRecorder();
   private final CameraShakeRecorder cameraShake = new CameraShakeRecorder();
   private final MenuRecorder menu = new MenuRecorder();
   private final ParticleRecorder particles = new ParticleRecorder();
   private final BossBarRecorder bossBars = new BossBarRecorder();
   private final ChatRecorder chat = new ChatRecorder();
   private final StatusEffectRecorder statusEffects = new StatusEffectRecorder();
   private final ScreenEffectRecorder screenEffects = new ScreenEffectRecorder();

   private PovRecordingSession(Form recordingForm) {
      this.recordingForm = recordingForm;
      this.gui.reset();
      this.cameraShake.reset();
      this.menu.reset();
      this.particles.reset();
      this.bossBars.reset();
      this.chat.reset();
      this.statusEffects.reset();
      this.screenEffects.reset();
   }

   public static PovRecordingSession start(Recorder recorder, Film film, Form form, int replayIndex, int tick) {
      PovRecordingSession session = new PovRecordingSession(form);
      session.film = film;
      session.replayIndex = replayIndex;
      session.lastDispatchedTick = tick - 1;
      session.particles.arm();
      current = session;
      Replay targetReplay = null;
      if (film != null && film.replays.getList().size() > replayIndex) {
         targetReplay = (Replay)film.replays.getList().get(replayIndex);
      }

      if (recorder.keyframes instanceof ReplayKeyframesPovAccess access) {
         RecordedHudData hud = access.bbsPov$getHud();
         if (hud != null) {
            if (PovSettings.isBakeActions()) {
               hud.ensureStartRecordingBounds((float)tick, targetReplay != null ? targetReplay.keyframes : null);
            } else {
               hud.cursorLayout.removeAll();
               hud.cursorVisible.removeAll();
               hud.cursorItem.removeAll();
            }
         }
      }

      if (!PovSettings.isBakeAnyActions() && recorder.keyframes instanceof ReplayKeyframesPovAccess accessx) {
         RecordedPovActions actions = accessx.bbsPov$getActions();
         if (actions != null) {
            actions.clearAll();
         }
      }

      return session;
   }

   public void recordFrame(Recorder recorder) {
      if (!recorder.hasNotStarted() && recorder.tick >= 0) {
         if (recorder.keyframes instanceof ReplayKeyframesPovAccess access) {
            int var7 = recorder.tick;
            if (this.film != null && this.film.replays != null && var7 > this.lastDispatchedTick) {
               int startRange = Math.max(0, this.lastDispatchedTick + 1);

               for (int t = startRange; t <= var7; t++) {
                  dispatchExternalChatMessages(this.film, this.replayIndex, t);
               }

               this.lastDispatchedTick = var7;
            }

            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            RecordedHudData hud = access.bbsPov$getHud();
            if (player != null && hud != null) {
               hud.recording.record(hud, recorder.tick, player);
            }

            RecordedHandData hand = access.bbsPov$getHand();
            if (player != null && hand != null) {
               hand.recording.record(hand, recorder.tick, player, this.recordingForm);
            }

            this.gui.record(access, recorder);
            this.cameraShake.record(access, recorder, player);
            this.menu.record(access, recorder);
            this.particles.record(access, recorder, player);
            this.bossBars.record(access, recorder);
            this.chat.record(access, recorder);
            this.statusEffects.record(access, recorder);
            this.screenEffects.record(access, recorder);
         }
      }
   }

   public static void dispatchExternalChatMessages(Film film, int currentReplayIndex, int targetTick) {
      if (film != null && film.replays != null) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc != null && mc.inGameHud != null && mc.inGameHud.getChatHud() != null) {
            List<Replay> list = film.replays.getList();

            for (int i = 0; i < list.size(); i++) {
               if (i != currentReplayIndex || currentReplayIndex < 0) {
                  Replay other = list.get(i);
                  if (other != null && other.keyframes instanceof ReplayKeyframesPovAccess otherAccess) {
                     RecordedPovActions otherActions = otherAccess.bbsPov$getActions();
                     if (otherActions != null) {
                        int looping = (Integer)other.looping.get();
                        int otherTick = looping > 0 ? targetTick % looping : targetTick;

                        for (ChatPovActionClip chatClip : otherActions.getClips(ChatPovActionClip.class)) {
                           if (chatClip != null && chatClip.executedText != null && !chatClip.executedText.isEmpty()) {
                              float clipStart = (float)((Integer)chatClip.tick.get()).intValue();

                              for (Keyframe<String> kf : chatClip.executedText.getList()) {
                                 if (kf != null && kf.getValue() != null && !((String)kf.getValue()).trim().isEmpty()) {
                                    float globalTick = clipStart + kf.getTick();
                                    if ((int)Math.floor((double)globalTick) == otherTick) {
                                       String val = UIExecutedTextKeyframeFactory.getRawText((String)kf.getValue()).replace("\r", "");
                                       String[] lines = val.split("\n");

                                       for (String line : lines) {
                                          if (line != null && !line.trim().isEmpty()) {
                                             try {
                                                ChatRecorder.setReplayingExternalMessage(true);
                                                mc.inGameHud.getChatHud().addMessage(Text.literal(line), null, null);
                                             } finally {
                                                ChatRecorder.setReplayingExternalMessage(false);
                                             }
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static void sampleCursor(float tickDelta) {
      PovRecordingSession session = current;
      Recorder recorder = BBSModClient.getFilms().getRecorder();
      if (session != null && recorder != null && !recorder.hasNotStarted() && recorder.tick >= 0) {
         if (recorder.keyframes instanceof ReplayKeyframesPovAccess access) {
            session.gui.sampleCursor(access, recorder, tickDelta);
            session.menu.sampleCursor(access, recorder, tickDelta);
            session.chat.sampleCursor(access, recorder, tickDelta);
         }
      }
   }

   public void finish(Recorder recorder) {
      try {
         if (recorder.keyframes instanceof ReplayKeyframesPovAccess access) {
            this.gui.finish(access, recorder.tick);
            this.cameraShake.finish(access, recorder.tick);
            this.menu.finish(access, recorder.tick);
            this.particles.finish(recorder.tick);
            this.bossBars.finish(access, recorder.tick);
            this.chat.finish(access, recorder.tick);
            this.statusEffects.finish(access, recorder.tick);
            this.screenEffects.finish(access, recorder.tick);
            ToastRecorder.finish(access, recorder.tick);
            RecordedHudData hud = access.bbsPov$getHud();
            if (hud != null) {
               hud.ensureEndRecordingBounds((float)recorder.tick);
            }

            return;
         }
      } finally {
         if (current == this) {
            current = null;
         }
      }
   }

   public static PovRecordingSession getCurrent() {
      return current;
   }

   public Film getFilm() {
      return this.film;
   }

   public int getReplayIndex() {
      return this.replayIndex;
   }

   public Replay getReplay() {
      return this.film != null && this.film.replays != null && this.replayIndex >= 0 && this.film.replays.getList().size() > this.replayIndex
         ? (Replay)this.film.replays.getList().get(this.replayIndex)
         : null;
   }
}

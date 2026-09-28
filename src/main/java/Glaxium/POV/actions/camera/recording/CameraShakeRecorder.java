package Glaxium.POV.actions.camera.recording;

import Glaxium.POV.actions.PovActionType;
import Glaxium.POV.actions.clip.CameraShakePovActionClip;
import Glaxium.POV.config.PovSettings;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import java.util.List;
import java.util.Objects;
import mchorse.bbs_mod.film.Recorder;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import net.minecraft.client.network.ClientPlayerEntity;

public final class CameraShakeRecorder {
   private CameraShakePovActionClip recordingClip;
   private int previousHurtTime;
   private int previousDeathTime;

   public void reset() {
      this.recordingClip = null;
      this.previousHurtTime = 0;
      this.previousDeathTime = 0;
   }

   public void finish(ReplayKeyframesPovAccess access, int tick) {
      this.finalizeClip(tick);
   }

   public void record(ReplayKeyframesPovAccess access, Recorder recorder, ClientPlayerEntity player) {
      if (PovSettings.isBakeCameraShake() && player != null) {
         if (!recorder.hasNotStarted() && recorder.tick >= 0) {
            int hurtTime = player.hurtTime;
            int maxHurtTime = Math.max(1, player.maxHurtTime);
            float damageTiltYaw = player.getDamageTiltYaw();
            int deathTime = player.deathTime;
            boolean inPulse = hurtTime > 0 || deathTime > 0;
            if (this.recordingClip == null) {
               boolean hurtRising = hurtTime > 0 && this.previousHurtTime <= 0;
               boolean deathRising = deathTime > 0 && this.previousDeathTime <= 0;
               if (hurtRising || deathRising) {
                  this.openClip(access, recorder.tick, hurtTime, maxHurtTime, damageTiltYaw, deathTime);
               }
            } else {
               int localTick = Math.max(0, recorder.tick - (Integer)this.recordingClip.tick.get());
               this.recordingClip.duration.set(Math.max(1, localTick + 1));
               this.recordValue(this.recordingClip.active, inPulse, (float)localTick);
               this.recordValue(this.recordingClip.hurtTime, hurtTime, (float)localTick);
               this.recordValue(this.recordingClip.maxHurtTime, maxHurtTime, (float)localTick);
               this.recordValue(this.recordingClip.damageTiltYaw, damageTiltYaw, (float)localTick);
               this.recordValue(this.recordingClip.deathTime, deathTime, (float)localTick);
               if (!inPulse) {
                  this.finalizeClip(recorder.tick);
               }
            }

            this.previousHurtTime = hurtTime;
            this.previousDeathTime = deathTime;
         }
      }
   }

   private void openClip(ReplayKeyframesPovAccess access, int tick, int hurtTime, int maxHurtTime, float damageTiltYaw, int deathTime) {
      this.recordingClip = (CameraShakePovActionClip)access.bbsPov$getActions().add(PovActionType.CAMERA_SHAKE, tick, 1);
      this.recordValue(this.recordingClip.active, true, 0.0F);
      this.recordValue(this.recordingClip.hurtTime, hurtTime, 0.0F);
      this.recordValue(this.recordingClip.maxHurtTime, maxHurtTime, 0.0F);
      this.recordValue(this.recordingClip.damageTiltYaw, damageTiltYaw, 0.0F);
      this.recordValue(this.recordingClip.deathTime, deathTime, 0.0F);
   }

   private void finalizeClip(int tick) {
      if (this.recordingClip != null) {
         int localTick = Math.max(0, tick - (Integer)this.recordingClip.tick.get());
         this.recordingClip.duration.set(Math.max(1, localTick + 1));
         this.recordValue(this.recordingClip.active, false, (float)localTick);
         this.recordingClip.ensureBakingBounds();
         this.recordingClip = null;
      }
   }

   private <T> void recordValue(KeyframeChannel<T> channel, T value, float tick) {
      if (channel != null) {
         if (channel.isEmpty()) {
            channel.insert(0.0F, value);
         } else {
            List<? extends Keyframe<T>> keyframes = channel.getKeyframes();
            Keyframe<T> previousKey = (Keyframe<T>)keyframes.get(keyframes.size() - 1);
            T previous = (T)previousKey.getValue();
            if (!Objects.equals(previous, value)) {
               if (tick - previousKey.getTick() > 1.0F) {
                  channel.insert(tick - 1.0F, previous);
               }

               channel.insert(tick, value);
            }
         }
      }
   }
}

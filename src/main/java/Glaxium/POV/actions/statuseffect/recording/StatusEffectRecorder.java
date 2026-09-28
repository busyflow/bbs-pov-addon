package Glaxium.POV.actions.statuseffect.recording;

import Glaxium.POV.actions.PovActionType;
import Glaxium.POV.actions.clip.StatusEffectsPovActionClip;
import Glaxium.POV.actions.statuseffect.StatusEffectEntry;
import Glaxium.POV.config.PovSettings;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import mchorse.bbs_mod.film.Recorder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public final class StatusEffectRecorder {
   private StatusEffectsPovActionClip recordingClip;
   private Set<String> currentEffectIds = new HashSet<>();

   public void reset() {
      this.recordingClip = null;
      this.currentEffectIds.clear();
   }

   public void finish(ReplayKeyframesPovAccess access, int tick) {
      this.finalizeClip(tick);
   }

   public void record(ReplayKeyframesPovAccess access, Recorder recorder) {
      if (PovSettings.isBakeStatusEffects()) {
         if (!recorder.hasNotStarted() && recorder.tick >= 0) {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player == null) {
               this.finalizeClip(recorder.tick);
            } else {
               Collection<StatusEffectInstance> active = player.getStatusEffects();
               if (active != null && !active.isEmpty()) {
                  Set<String> newEffectIds = new HashSet<>();

                  for (StatusEffectInstance inst : active) {
                     Identifier id = Registries.STATUS_EFFECT.getId(inst.getEffectType());
                     if (id != null) {
                        newEffectIds.add(id.toString());
                     }
                  }

                  if (this.recordingClip != null && !this.currentEffectIds.equals(newEffectIds)) {
                     this.finalizeClip(recorder.tick);
                  }

                  if (this.recordingClip == null) {
                     this.recordingClip = (StatusEffectsPovActionClip)access.bbsPov$getActions().add(PovActionType.STATUS_EFFECTS, recorder.tick, 1);
                     this.currentEffectIds = newEffectIds;
                     List<StatusEffectInstance> sortedActive = new ArrayList<>(active);
                     sortedActive.sort(null);

                     for (StatusEffectInstance instx : sortedActive) {
                        Identifier id = Registries.STATUS_EFFECT.getId(instx.getEffectType());
                        if (id != null) {
                           boolean unlimited = instx.isInfinite();
                           int durationSeconds = unlimited ? 100 : Math.max(1, instx.getDuration() / 20);
                           int amp = instx.getAmplifier();
                           StatusEffectEntry entry = new StatusEffectEntry(id.toString(), unlimited, durationSeconds, amp);
                           this.recordingClip.addEffect(entry);
                        }
                     }
                  }

                  float localTick = (float)(recorder.tick - (Integer)this.recordingClip.tick.get());
                  this.recordingClip.duration.set(Math.max(1, (int)localTick + 1));
               } else {
                  this.finalizeClip(recorder.tick);
               }
            }
         }
      }
   }

   private void finalizeClip(int tick) {
      if (this.recordingClip != null) {
         this.recordingClip.duration.set(Math.max(1, tick - (Integer)this.recordingClip.tick.get()));
         this.recordingClip = null;
         this.currentEffectIds.clear();
      }
   }
}

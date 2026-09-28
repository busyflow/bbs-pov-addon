package Glaxium.POV.actions.bossbar.recording;

import Glaxium.POV.actions.PovActionType;
import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.actions.bossbar.BossBarLooks;
import Glaxium.POV.actions.bossbar.BossBarTypeEntry;
import Glaxium.POV.actions.clip.BossBarPovActionClip;
import Glaxium.POV.config.PovSettings;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.integration.access.minecraft.BossBarHudPovAccess;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import mchorse.bbs_mod.film.Recorder;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.entity.boss.BossBar.Color;

public final class BossBarRecorder {
   private static final int MAX_BOSSBARS = 9;
   private final Map<UUID, BossBarRecorder.OpenBossBar> recordingClips = new LinkedHashMap<>();

   public void reset() {
      this.recordingClips.clear();
   }

   public void finish(ReplayKeyframesPovAccess access, int tick) {
      for (BossBarRecorder.OpenBossBar open : this.recordingClips.values()) {
         this.finalizeClip(open, tick);
      }

      this.recordingClips.clear();
   }

   public void record(ReplayKeyframesPovAccess access, Recorder recorder) {
      if (PovSettings.isBakeBossBars()) {
         if (!recorder.hasNotStarted() && recorder.tick >= 0) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (!((client.inGameHud == null ? null : client.inGameHud.getBossBarHud()) instanceof BossBarHudPovAccess barAccess)) {
               this.finish(access, recorder.tick);
            } else {
               Map<UUID, ClientBossBar> bars = barAccess.bbsPov$getBossBars();
               if (bars != null && !bars.isEmpty()) {
                  Set<UUID> visibleNow = new LinkedHashSet<>();
                  int count = 0;
                  int baseLayer = PovActionType.BOSS_BARS.seedLayer();

                  for (Entry<UUID, ClientBossBar> entry : bars.entrySet()) {
                     if (count >= 9) {
                        break;
                     }

                     UUID uuid = entry.getKey();
                     ClientBossBar bar = entry.getValue();
                     if (uuid != null && bar != null) {
                        visibleNow.add(uuid);
                        count++;
                        String type = inferType(bar);
                        BossBarRecorder.OpenBossBar open = this.recordingClips.get(uuid);
                        if (open != null && !Objects.equals(open.currentType, type)) {
                           this.finalizeClip(open, recorder.tick);
                           this.recordingClips.remove(uuid);
                           open = null;
                        }

                        if (open == null) {
                           int targetLayer = this.findAvailableLayer(baseLayer, access.bbsPov$getActions(), recorder.tick);
                           BossBarPovActionClip clip = (BossBarPovActionClip)access.bbsPov$getActions().add(PovActionType.BOSS_BARS, recorder.tick, 1);
                           clip.layer.set(targetLayer);
                           clip.state.insert(0.0F, type);
                           open = new BossBarRecorder.OpenBossBar(uuid, clip, targetLayer, type);
                           this.recordingClips.put(uuid, open);
                        }

                        float localTick = (float)(recorder.tick - (Integer)open.clip.tick.get());
                        open.clip.duration.set(Math.max(1, (int)localTick + 1));
                        this.recordValue(open.clip.state, type, localTick);
                        this.recordValue(open.clip.name, bar.getName().getString(), localTick);
                        this.recordValue(open.clip.percent, clamp(bar.getPercent()), localTick);
                        this.recordValue(open.clip.color, BossBarLooks.colorId(bar.getColor()), localTick);
                        this.recordValue(open.clip.style, BossBarLooks.styleId(bar.getStyle()), localTick);
                     }
                  }

                  Iterator<Entry<UUID, BossBarRecorder.OpenBossBar>> iterator = this.recordingClips.entrySet().iterator();

                  while (iterator.hasNext()) {
                     Entry<UUID, BossBarRecorder.OpenBossBar> entry = iterator.next();
                     if (!visibleNow.contains(entry.getKey())) {
                        this.finalizeClip(entry.getValue(), recorder.tick);
                        iterator.remove();
                     }
                  }
               } else {
                  this.finish(access, recorder.tick);
               }
            }
         }
      }
   }

   private int findAvailableLayer(int baseLayer, RecordedPovActions actions, int tick) {
      boolean[] occupied = new boolean[9];

      for (BossBarRecorder.OpenBossBar open : this.recordingClips.values()) {
         int offset = open.layer - baseLayer;
         if (offset >= 0 && offset < 9) {
            occupied[offset] = true;
         }
      }

      if (actions != null) {
         for (Clip c : actions.get()) {
            if (c instanceof BossBarPovActionClip) {
               BossBarPovActionClip barClip = (BossBarPovActionClip)c;
               int start = (Integer)barClip.tick.get();
               int end = start + (Integer)barClip.duration.get();
               if (tick >= start && tick < end) {
                  int offset = (Integer)barClip.layer.get() - baseLayer;
                  if (offset >= 0 && offset < 9) {
                     occupied[offset] = true;
                  }
               }
            }
         }
      }

      for (int i = 0; i < 9; i++) {
         if (!occupied[i]) {
            return baseLayer + i;
         }
      }

      return baseLayer;
   }

   private void finalizeClip(BossBarRecorder.OpenBossBar open, int tick) {
      if (open != null && open.clip != null) {
         open.clip.duration.set(Math.max(1, tick - (Integer)open.clip.tick.get()));
         open.clip.ensureBakingBounds();
      }
   }

   private static String inferType(ClientBossBar bar) {
      String name = bar.getName() == null ? "" : bar.getName().getString().toLowerCase(Locale.ROOT);
      if (name.contains("raid")) {
         return BossBarTypeEntry.RAID.id;
      } else if (name.contains("wither")) {
         return BossBarTypeEntry.WITHER.id;
      } else if (name.contains("dragon")) {
         return BossBarTypeEntry.DRAGON.id;
      } else {
         Color color = bar.getColor();
         if (color == Color.RED) {
            return BossBarTypeEntry.RAID.id;
         } else {
            return color == Color.PURPLE ? BossBarTypeEntry.WITHER.id : BossBarTypeEntry.DRAGON.id;
         }
      }
   }

   private static float clamp(float value) {
      return Math.max(0.0F, Math.min(1.0F, value));
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

   private static final class OpenBossBar {
      final UUID uuid;
      final BossBarPovActionClip clip;
      final int layer;
      final String currentType;

      OpenBossBar(UUID uuid, BossBarPovActionClip clip, int layer, String currentType) {
         this.uuid = uuid;
         this.clip = clip;
         this.layer = layer;
         this.currentType = currentType;
      }
   }
}

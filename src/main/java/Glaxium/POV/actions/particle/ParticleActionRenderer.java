package Glaxium.POV.actions.particle;

import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.actions.clip.ParticleEffectPovActionClip;
import Glaxium.POV.actions.clip.ScreenEffectPovActionClip;
import Glaxium.POV.actions.particle.recording.ParticleRecorder;
import Glaxium.POV.editor.UIPovHandEditor;
import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.integration.access.bbs.FilmsPovAccess;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.render.PovViewportMetrics;
import Glaxium.POV.replay.PovReplaySettings;
import java.util.List;
import java.util.Map;
import java.util.Random;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Vec3d;

public final class ParticleActionRenderer {
   private static final Random RANDOM = new Random();
   private static int lastPlayingFilmTick = Integer.MIN_VALUE;

   private ParticleActionRenderer() {
   }

   public static void tick() {
      if (!UIPovHandEditor.isActive() && !ParticleRecorder.isArmed()) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.world != null && client.particleManager != null) {
            BaseFilmController world = activeWorldController();
            UIFilmPanel panel = PovViewportMetrics.resolveFilmPanel();
            if (panel == null) {
               panel = PovReplaySettings.getFilmPanel();
            }

            Film film;
            int cursor;
            boolean playing;
            Map<String, IEntity> entities;
            if (world != null) {
               film = world.film;
               cursor = world.getTick();
               playing = !world.paused;
               entities = world.getEntities();
            } else {
               if (panel == null || panel.getData() == null || panel.getController() == null) {
                  lastPlayingFilmTick = Integer.MIN_VALUE;
                  return;
               }

               film = (Film)panel.getData();
               cursor = panel.getCursor();
               playing = panel.getController().isPlaying();
               entities = panel.getController().getEntities();
            }

            if (film != null && entities != null) {
               if (playing) {
                  if (cursor == lastPlayingFilmTick) {
                     return;
                  }

                  lastPlayingFilmTick = cursor;
               } else {
                  lastPlayingFilmTick = Integer.MIN_VALUE;
               }

               List<Replay> replays = film.replays.getList();
               ParticleRecorder.suspendCapture();

               try {
                  for (int replayIndex = 0; replayIndex < replays.size(); replayIndex++) {
                     Replay replay = replays.get(replayIndex);
                     ReplayKeyframes actions = replay.keyframes;
                     if (actions instanceof ReplayKeyframesPovAccess) {
                        ReplayKeyframesPovAccess access = (ReplayKeyframesPovAccess)actions;
                        RecordedPovActions actionsx = access.bbsPov$getActions();
                        if (actionsx != null) {
                           IEntity entity = entities.get(replay.getId());
                           Vec3d origin;
                           float width;
                           float height;
                           if (entity != null) {
                              origin = ParticleSpaces.lerpPos(entity, 1.0F);
                              width = ParticleSpaces.width(entity);
                              height = ParticleSpaces.height(entity);
                           } else if (client.player != null) {
                              origin = client.player.getPos();
                              width = client.player.getWidth();
                              height = client.player.getHeight();
                           } else {
                              if (client.cameraEntity == null) {
                                 continue;
                              }

                              origin = client.cameraEntity.getPos();
                              width = client.cameraEntity.getWidth();
                              height = client.cameraEntity.getHeight();
                           }

                           float replayTick = (float)replay.getTick(cursor);
                           boolean particleEmitted = false;
                           RecordedHandData handData = access.bbsPov$getHand();
                           if (handData != null) {
                              int wholeTick = (int)Math.floor((double)replayTick);
                              int active = (Integer)handData.activeHand.interpolate((float)wholeTick, 0);
                              boolean showParticles = (Boolean)handData.showUseParticles.interpolate(replayTick, true);
                              if (active != 0 && showParticles) {
                                 emitEatingParticles(client, origin, width, height, entity, replay, replayTick, handData);
                                 particleEmitted = true;
                              }
                           }

                           for (Clip clip : actionsx.get()) {
                              if (clip instanceof ParticleEffectPovActionClip particleClip && particleClip.isActive(replayTick)) {
                                 if (!particleEmitted) {
                                    emit(client, origin, width, height, particleClip, entity, replay, replayTick);
                                    particleEmitted = true;
                                 }
                                 continue;
                              }

                              if (clip instanceof ScreenEffectPovActionClip screenClip && screenClip.isActive(replayTick) && screenClip.hasEffect("totem")) {
                                 emitTotem(client, origin, width, height, screenClip, replayTick - (float)((Integer)screenClip.tick.get()).intValue());
                              }
                           }
                        }
                     }
                  }
               } finally {
                  ParticleRecorder.resumeCapture();
               }
            }
         }
      }
   }

   private static void emitTotem(MinecraftClient client, Vec3d origin, float width, float height, ScreenEffectPovActionClip clip, float localTick) {
      boolean particles = clip.totemParticles.isEmpty() ? false : (Boolean)clip.totemParticles.interpolate(localTick);
      if (!particles) {
         clip.lastTotemParticleTick = Integer.MIN_VALUE;
      } else {
         int curTick = (int)localTick;
         if (curTick >= 0 && clip.lastTotemParticleTick != curTick) {
            clip.lastTotemParticleTick = curTick;

            for (int i = 0; i < 16; i++) {
               double d = (double)(RANDOM.nextFloat() * 2.0F - 1.0F);
               double e = (double)(RANDOM.nextFloat() * 2.0F - 1.0F);
               double f = (double)(RANDOM.nextFloat() * 2.0F - 1.0F);
               if (d * d + e * e + f * f <= 1.0) {
                  double px = origin.x + d / 4.0 * (double)width;
                  double py = origin.y + (0.5 + e / 4.0) * (double)height;
                  double pz = origin.z + f / 4.0 * (double)width;
                  client.particleManager.addParticle(ParticleTypes.TOTEM_OF_UNDYING, px, py, pz, d, e + 0.2, f);
               }
            }
         }
      }
   }

   private static void emitEatingParticles(
      MinecraftClient client, Vec3d origin, float width, float height, IEntity entity, Replay replay, float replayTick, RecordedHandData handData
   ) {
      int wholeTick = (int)Math.floor((double)replayTick);
      ItemStack itemToUse = (ItemStack)handData.activeItem.interpolate((float)wholeTick, ItemStack.EMPTY);
      if ((itemToUse == null || itemToUse.isEmpty() || itemToUse.isOf(Items.AIR)) && replay != null && replay.keyframes != null) {
         ItemStack held = replay.keyframes.getMainHandStack(replayTick);
         if (held == null || held.isEmpty() || held.isOf(Items.AIR)) {
            held = (ItemStack)replay.keyframes.offHand.interpolate(replayTick, ItemStack.EMPTY);
         }

         if (held != null && !held.isEmpty() && !held.isOf(Items.AIR)) {
            itemToUse = held;
         }
      }

      if (itemToUse != null && !itemToUse.isEmpty() && !itemToUse.isOf(Items.AIR)) {
         UseAction useAction = itemToUse.getUseAction();
         if (useAction == UseAction.EAT || useAction == UseAction.DRINK) {
            ParticleEffect effect = new ItemStackParticleEffect(ParticleTypes.ITEM, itemToUse);
            float yaw = entity != null
               ? entity.getHeadYaw()
               : (replay != null && replay.keyframes != null ? (float)((Double)replay.keyframes.yaw.interpolate(replayTick, 0.0)).doubleValue() : 0.0F);
            float pitch = entity != null
               ? entity.getPitch()
               : (replay != null && replay.keyframes != null ? (float)((Double)replay.keyframes.pitch.interpolate(replayTick, 0.0)).doubleValue() : 0.0F);
            float radPitch = -pitch * (float) (Math.PI / 180.0);
            float radYaw = -yaw * (float) (Math.PI / 180.0);
            int count = 1 + RANDOM.nextInt(3);

            for (int i = 0; i < count; i++) {
               Vec3d vel = new Vec3d(((double)RANDOM.nextFloat() - 0.5) * 0.1, (double)RANDOM.nextFloat() * 0.1 + 0.1, 0.0).rotateX(radPitch).rotateY(radYaw);
               double d = (double)(-RANDOM.nextFloat()) * 0.4 - 0.2;
               Vec3d offset = new Vec3d(((double)RANDOM.nextFloat() - 0.5) * 0.3, d, 0.6).rotateX(radPitch).rotateY(radYaw);
               Vec3d pos = origin.add(0.0, (double)height * 0.85, 0.0).add(offset);
               client.particleManager.addParticle(effect, pos.x, pos.y, pos.z, vel.x, vel.y + 0.05, vel.z);
            }
         }
      }
   }

   private static void emit(
      MinecraftClient client, Vec3d origin, float width, float height, ParticleEffectPovActionClip clip, IEntity entity, Replay replay, float replayTick
   ) {
      ParticleEffect effect = ParticleEffects.fromClip(clip);
      if (effect != null) {
         if (effect.getType() == ParticleTypes.ITEM) {
            ItemStack itemToUse = null;
            if (replay != null && replay.keyframes instanceof ReplayKeyframesPovAccess access) {
               RecordedHandData handData = access.bbsPov$getHand();
               if (handData != null) {
                  int wholeTick = (int)Math.floor((double)replayTick);
                  ItemStack active = (ItemStack)handData.activeItem.interpolate((float)wholeTick, ItemStack.EMPTY);
                  if (active != null && !active.isEmpty() && !active.isOf(Items.AIR)) {
                     itemToUse = active;
                  }
               }
            }

            if (itemToUse == null || itemToUse.isEmpty() || itemToUse.isOf(Items.AIR)) {
               itemToUse = clip.extraItem();
            }

            if ((itemToUse == null || itemToUse.isEmpty() || itemToUse.isOf(Items.AIR)) && replay != null && replay.keyframes != null) {
               ItemStack held = replay.keyframes.getMainHandStack(replayTick);
               if (held == null || held.isEmpty() || held.isOf(Items.AIR)) {
                  held = (ItemStack)replay.keyframes.offHand.interpolate(replayTick, ItemStack.EMPTY);
               }

               if (held != null && !held.isEmpty() && !held.isOf(Items.AIR)) {
                  itemToUse = held;
               }
            }

            if (itemToUse == null || itemToUse.isEmpty() || itemToUse.isOf(Items.AIR)) {
               itemToUse = new ItemStack(Items.APPLE);
            }

            ParticleEffect var21 = new ItemStackParticleEffect(ParticleTypes.ITEM, itemToUse);
            float yaw = entity != null
               ? entity.getHeadYaw()
               : (replay != null ? (float)((Double)replay.keyframes.yaw.interpolate(replayTick, 0.0)).doubleValue() : 0.0F);
            float pitch = entity != null
               ? entity.getPitch()
               : (replay != null ? (float)((Double)replay.keyframes.pitch.interpolate(replayTick, 0.0)).doubleValue() : 0.0F);
            float radPitch = -pitch * (float) (Math.PI / 180.0);
            float radYaw = -yaw * (float) (Math.PI / 180.0);
            int count = 1 + RANDOM.nextInt(3);

            for (int i = 0; i < count; i++) {
               Vec3d vel = new Vec3d(((double)RANDOM.nextFloat() - 0.5) * 0.1, (double)RANDOM.nextFloat() * 0.1 + 0.1, 0.0).rotateX(radPitch).rotateY(radYaw);
               double d = (double)(-RANDOM.nextFloat()) * 0.4 - 0.2;
               Vec3d offset = new Vec3d(((double)RANDOM.nextFloat() - 0.5) * 0.3, d, 0.6).rotateX(radPitch).rotateY(radYaw);
               Vec3d pos = origin.add(0.0, (double)height * 0.85, 0.0).add(offset);
               client.particleManager.addParticle(var21, pos.x, pos.y, pos.z, vel.x, vel.y + 0.05, vel.z);
            }
         } else {
            boolean status = effect.getType() == ParticleTypes.ENTITY_EFFECT || effect.getType() == ParticleTypes.AMBIENT_ENTITY_EFFECT;
            if (!status || RANDOM.nextBoolean()) {
               Vec3d pos = ParticleSpaces.pointInActorAabb(origin, width, height, RANDOM);
               double vx;
               double vy;
               double vz;
               if (status) {
                  vx = (double)((Float)clip.dustR.get()).floatValue();
                  vy = (double)((Float)clip.dustG.get()).floatValue();
                  vz = (double)((Float)clip.dustB.get()).floatValue();
               } else {
                  vx = (RANDOM.nextDouble() - 0.5) * 0.15;
                  vy = RANDOM.nextDouble() * 0.2;
                  vz = (RANDOM.nextDouble() - 0.5) * 0.15;
               }

               client.particleManager.addParticle(effect, pos.x, pos.y, pos.z, vx, vy, vz);
            }
         }
      }
   }

   public static boolean hidesLivePlayerParticles() {
      if (ParticleRecorder.isArmed() || ParticleRecorder.isPlaybackEmit()) {
         return false;
      } else if (activeWorldController() != null) {
         return true;
      } else {
         UIFilmPanel panel = PovViewportMetrics.resolveFilmPanel();
         if (panel == null) {
            panel = PovReplaySettings.getFilmPanel();
         }

         return panel != null && panel.getData() != null;
      }
   }

   private static BaseFilmController activeWorldController() {
      List<BaseFilmController> controllers = ((FilmsPovAccess)BBSModClient.getFilms()).bbsPov$getControllers();
      if (controllers != null && !controllers.isEmpty()) {
         for (int i = controllers.size() - 1; i >= 0; i--) {
            BaseFilmController controller = controllers.get(i);
            if (controller != null && !controller.hasFinished()) {
               return controller;
            }
         }

         return null;
      } else {
         return null;
      }
   }
}

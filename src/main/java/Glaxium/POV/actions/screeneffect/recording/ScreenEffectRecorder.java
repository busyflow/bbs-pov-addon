package Glaxium.POV.actions.screeneffect.recording;

import Glaxium.POV.actions.PovActionType;
import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.actions.clip.ScreenEffectPovActionClip;
import Glaxium.POV.config.PovSettings;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.integration.access.minecraft.InGameHudVignettePovAccess;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import mchorse.bbs_mod.film.Recorder;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.effect.StatusEffectInstance.FactorCalculationData;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.BlockPos.Mutable;

public final class ScreenEffectRecorder {
   private ScreenEffectPovActionClip recordingClip;
   private final Set<String> currentlyActiveEffects = new HashSet<>();
   private boolean wasScoping = false;
   private int spyglassReleaseTicksRemaining = 0;
   private float spyglassLastZoom = 1.0F;
   private static ItemStack pendingTotemItem = null;
   private static boolean pendingTotemFlipped = false;
   private static boolean totemTriggered = false;
   private final Map<String, Float> lastFloatValues = new HashMap<>();
   private final Map<String, Integer> lastRecordedTicks = new HashMap<>();

   public static void onFloatingItem(ItemStack item, boolean flipped) {
      pendingTotemItem = item;
      pendingTotemFlipped = flipped;
      totemTriggered = true;
   }

   public void reset() {
      this.recordingClip = null;
      this.currentlyActiveEffects.clear();
      this.wasScoping = false;
      this.spyglassReleaseTicksRemaining = 0;
      this.spyglassLastZoom = 1.0F;
      this.lastFloatValues.clear();
      this.lastRecordedTicks.clear();
      pendingTotemItem = null;
      pendingTotemFlipped = false;
      totemTriggered = false;
   }

   private static BlockState getSuffocatingBlockState(ClientPlayerEntity player) {
      if (player != null && player.getWorld() != null) {
         Mutable mutable = new Mutable();

         for (int i = 0; i < 8; i++) {
            double d = player.getX() + (double)(((float)((i >> 0) % 2) - 0.5F) * player.getWidth() * 0.8F);
            double e = player.getEyeY() + (double)(((float)((i >> 1) % 2) - 0.5F) * 0.1F);
            double f = player.getZ() + (double)(((float)((i >> 2) % 2) - 0.5F) * player.getWidth() * 0.8F);
            mutable.set(d, e, f);
            BlockState blockState = player.getWorld().getBlockState(mutable);
            if (blockState.getRenderType() != BlockRenderType.INVISIBLE && blockState.shouldSuffocate(player.getWorld(), mutable)) {
               return blockState;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public void record(ReplayKeyframesPovAccess access, Recorder recorder) {
      if (access != null && recorder != null && PovSettings.isBakeScreenEffects()) {
         RecordedPovActions actions = access.bbsPov$getActions();
         if (actions != null) {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (player != null) {
               int tick = recorder.tick;
               float vignetteDarkness = 0.0F;
               if (client.inGameHud instanceof InGameHudVignettePovAccess accessHud) {
                  vignetteDarkness = accessHud.bbsPov$getVignetteDarkness();
               }

               Set<String> activeNow = new HashSet<>();
               BlockState suffocationState = getSuffocatingBlockState(player);
               if (player.isOnFire() || player.isInLava() || player.isSubmergedIn(FluidTags.LAVA)) {
                  activeNow.add("fire");
               }

               if (player.getFreezingScale() > 0.05F) {
                  activeNow.add("frost");
               }

               boolean inPortalBlock = player.getWorld() != null && player.getWorld().getBlockState(player.getBlockPos()).isOf(Blocks.NETHER_PORTAL);
               boolean inPortal = inPortalBlock || player.nauseaIntensity > 0.001F && !player.hasStatusEffect(StatusEffects.NAUSEA);
               if (inPortal) {
                  activeNow.add("portal");
               }

               if (player.getEquippedStack(EquipmentSlot.HEAD).isOf(Items.CARVED_PUMPKIN)) {
                  activeNow.add("pumpkin");
               }

               boolean isScoping = player.isUsingSpyglass() || player.isUsingItem() && player.getActiveItem().isOf(Items.SPYGLASS);
               if (isScoping) {
                  activeNow.add("spyglass");
               }

               if (suffocationState != null || player.isInsideWall()) {
                  activeNow.add("suffocation");
               }

               if (player.hasStatusEffect(StatusEffects.NAUSEA) || player.nauseaIntensity > 0.001F || inPortalBlock) {
                  activeNow.add("nausea");
               }

               if (player.hasStatusEffect(StatusEffects.DARKNESS)) {
                  activeNow.add("darkness");
               }

               if (player.hasStatusEffect(StatusEffects.BLINDNESS)) {
                  activeNow.add("blindness");
               }

               if (player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                  activeNow.add("night_vision");
               }

               if (player.isSubmergedIn(FluidTags.WATER) || player.isSubmergedInWater()) {
                  activeNow.add("underwater");
               }

               if (vignetteDarkness > 0.02F) {
                  activeNow.add("vignette");
               }

               boolean totemPopThisTick = totemTriggered && pendingTotemItem != null;
               if (totemPopThisTick) {
                  activeNow.add("totem");
               }

               if (this.recordingClip == null) {
                  if (activeNow.isEmpty()) {
                     return;
                  }

                  this.recordingClip = (ScreenEffectPovActionClip)actions.add(PovActionType.SCREEN_EFFECT, tick, 1);
                  this.recordingClip.title.set("Screen Effects");
                  this.currentlyActiveEffects.clear();
               }

               int localTick = tick - (Integer)this.recordingClip.tick.get();
               if (!activeNow.isEmpty()) {
                  this.recordingClip.duration.set(Math.max((Integer)this.recordingClip.duration.get(), localTick + 1));

                  for (String effectId : activeNow) {
                     boolean isNewlyStarted = !this.currentlyActiveEffects.contains(effectId);
                     if (isNewlyStarted) {
                        this.recordingClip.addEffect(effectId, false);
                        if (effectId.equals("totem")) {
                           String itemId = Registries.ITEM.getId(pendingTotemItem.getItem()).toString();
                           this.recordingClip.totemItem.set(itemId);
                           if (localTick > 0) {
                              if (this.recordingClip.totemVisible.isEmpty()) {
                                 this.recordingClip.totemVisible.insert(0.0F, false);
                                 if (localTick > 1) {
                                    this.recordingClip.totemVisible.insert((float)(localTick - 1), false);
                                 }
                              }

                              if (this.recordingClip.totemParticles.isEmpty()) {
                                 this.recordingClip.totemParticles.insert(0.0F, false);
                                 if (localTick > 1) {
                                    this.recordingClip.totemParticles.insert((float)(localTick - 1), false);
                                 }
                              }

                              if (this.recordingClip.totemProgress.isEmpty()) {
                                 this.recordingClip.totemProgress.insert(0.0F, 0.0F);
                                 if (localTick > 1) {
                                    this.recordingClip.totemProgress.insert((float)(localTick - 1), 0.0F);
                                 }
                              }

                              if (this.recordingClip.totemFlipped.isEmpty()) {
                                 this.recordingClip.totemFlipped.insert(0.0F, false);
                                 if (localTick > 1) {
                                    this.recordingClip.totemFlipped.insert((float)(localTick - 1), false);
                                 }
                              }
                           }

                           this.recordingClip.totemVisible.insert((float)localTick, true);
                           this.recordingClip.totemProgress.insert((float)localTick, 0.0F);
                           this.recordingClip.totemProgress.insert((float)(localTick + 40), 1.0F);
                           this.recordingClip.totemFlipped.insert((float)localTick, pendingTotemFlipped);
                           this.recordingClip.totemFlipped.insert((float)(localTick + 40), false);
                           this.recordingClip.totemParticles.insert((float)localTick, true);
                           this.recordingClip.totemParticles.insert((float)(localTick + 30), false);
                           this.recordingClip.totemVisible.insert((float)(localTick + 40), false);
                           this.recordingClip.duration.set(Math.max((Integer)this.recordingClip.duration.get(), localTick + 40));
                           totemTriggered = false;
                           pendingTotemItem = null;
                           pendingTotemFlipped = false;
                        } else {
                           recordVisible(this.recordingClip, effectId, localTick, true);
                        }
                     }

                     if (!effectId.equals("totem")) {
                        this.recordEffectKeyframes(
                           this.recordingClip, effectId, localTick, player, vignetteDarkness, suffocationState, inPortalBlock, isNewlyStarted
                        );
                     }
                  }

                  for (String effectId : this.currentlyActiveEffects) {
                     if (!activeNow.contains(effectId) && !effectId.equals("totem")) {
                        recordVisible(this.recordingClip, effectId, localTick, false);
                        this.recordEffectEnd(this.recordingClip, effectId, localTick);
                     }
                  }

                  this.currentlyActiveEffects.clear();
                  this.currentlyActiveEffects.addAll(activeNow);
                  this.currentlyActiveEffects.remove("totem");
               } else if (!this.currentlyActiveEffects.isEmpty()) {
                  for (String effectIdx : this.currentlyActiveEffects) {
                     recordVisible(this.recordingClip, effectIdx, localTick, false);
                     this.recordEffectEnd(this.recordingClip, effectIdx, localTick);
                  }

                  this.currentlyActiveEffects.clear();
               }
            }
         }
      }
   }

   private static void recordVisible(ScreenEffectPovActionClip clip, String effectId, int localTick, boolean visible) {
      KeyframeChannel<Boolean> channel = getVisibleChannel(clip, effectId);
      if (channel != null) {
         if (visible && localTick > 0) {
            if (channel.isEmpty()) {
               channel.insert(0.0F, false);
            }

            channel.insert((float)(localTick - 1), false);
         }

         channel.insert((float)localTick, visible);
      }
   }

   private static KeyframeChannel<Boolean> getVisibleChannel(ScreenEffectPovActionClip clip, String effectId) {
      return switch (effectId) {
         case "vignette" -> clip.vignetteVisible;
         case "fire" -> clip.fireVisible;
         case "frost" -> clip.frostVisible;
         case "portal" -> clip.portalVisible;
         case "pumpkin" -> clip.pumpkinVisible;
         case "spyglass" -> clip.spyglassVisible;
         case "suffocation" -> clip.suffocationVisible;
         case "night_vision" -> clip.nightVisionVisible;
         case "blindness" -> clip.blindnessVisible;
         case "totem" -> clip.totemVisible;
         case "nausea" -> clip.nauseaVisible;
         case "darkness" -> clip.darknessVisible;
         case "underwater" -> clip.underwaterVisible;
         default -> null;
      };
   }

   private void recordFloatChannel(
      KeyframeChannel<Float> channel, String channelKey, int localTick, float value, float defaultInactiveVal, float deltaThreshold, boolean isNewlyStarted
   ) {
      if (!isNewlyStarted && !channel.isEmpty()) {
         Float lastVal = this.lastFloatValues.get(channelKey);
         Integer lastTick = this.lastRecordedTicks.get(channelKey);
         if (lastVal == null) {
            lastVal = (Float)channel.interpolate((float)localTick);
         }

         if (lastTick == null) {
            lastTick = localTick;
         }

         if (Math.abs(value - lastVal) >= deltaThreshold) {
            channel.insert((float)localTick, value);
            this.lastFloatValues.put(channelKey, value);
            this.lastRecordedTicks.put(channelKey, localTick);
         }
      } else {
         if (localTick > 0) {
            if (channel.isEmpty()) {
               channel.insert(0.0F, defaultInactiveVal);
            }

            channel.insert((float)(localTick - 1), defaultInactiveVal);
         }

         channel.insert((float)localTick, value);
         this.lastFloatValues.put(channelKey, value);
         this.lastRecordedTicks.put(channelKey, localTick);
      }
   }

   private void recordFloatEnd(KeyframeChannel<Float> channel, String channelKey, int localTick, float defaultInactiveVal) {
      if (!channel.isEmpty()) {
         Float lastVal = this.lastFloatValues.get(channelKey);
         Integer lastTick = this.lastRecordedTicks.get(channelKey);
         if (lastVal != null && lastTick != null && localTick - lastTick > 1) {
            channel.insert((float)(localTick - 1), lastVal);
         }

         channel.insert((float)localTick, defaultInactiveVal);
         this.lastFloatValues.remove(channelKey);
         this.lastRecordedTicks.remove(channelKey);
      }
   }

   private void recordEffectEnd(ScreenEffectPovActionClip clip, String effectId, int localTick) {
      switch (effectId) {
         case "vignette":
            this.recordFloatEnd(clip.vignetteOpacity, "vignetteOpacity", localTick, 0.0F);
            break;
         case "nausea":
            this.recordFloatEnd(clip.nauseaDistortion, "nauseaDistortion", localTick, 0.0F);
            this.recordFloatEnd(clip.nauseaOpacity, "nauseaOpacity", localTick, 0.0F);
            break;
         case "frost":
            this.recordFloatEnd(clip.frostProgress, "frostProgress", localTick, 0.0F);
            this.recordFloatEnd(clip.frostZoom, "frostZoom", localTick, 1.0F);
            break;
         case "portal":
            this.recordFloatEnd(clip.portalOpacity, "portalOpacity", localTick, 0.0F);
            break;
         case "pumpkin":
            this.recordFloatEnd(clip.pumpkinOpacity, "pumpkinOpacity", localTick, 0.0F);
            break;
         case "spyglass":
            if (!clip.spyglassZoom.isEmpty()) {
               Integer lastTick = this.lastRecordedTicks.get("spyglassZoom");
               if (lastTick == null || localTick - 1 > lastTick) {
                  clip.spyglassZoom.insert((float)(localTick - 1), this.spyglassLastZoom);
               }

               for (int i = 0; i < 4; i++) {
                  float progress = (float)(i + 1) / 4.0F;
                  float eased = (float)Math.sin((double)MathHelper.clamp(progress, 0.0F, 1.0F) * Math.PI / 2.0);
                  float zoom = MathHelper.lerp(eased, this.spyglassLastZoom, 1.0F);
                  clip.spyglassZoom.insert((float)(localTick + i), zoom);
               }

               clip.duration.set(Math.max((Integer)clip.duration.get(), localTick + 4));
               this.lastFloatValues.remove("spyglassZoom");
               this.lastRecordedTicks.remove("spyglassZoom");
            }

            this.recordFloatEnd(clip.spyglassScale, "spyglassScale", localTick, 1.12F);
            break;
         case "suffocation":
            this.recordFloatEnd(clip.suffocationOpacity, "suffocationOpacity", localTick, 0.0F);
            break;
         case "darkness":
            this.recordFloatEnd(clip.darknessOpacity, "darknessOpacity", localTick, 0.0F);
            this.recordFloatEnd(clip.darknessRadius, "darknessRadius", localTick, 15.0F);
            break;
         case "blindness":
            this.recordFloatEnd(clip.blindnessOpacity, "blindnessOpacity", localTick, 0.0F);
            this.recordFloatEnd(clip.blindnessRadius, "blindnessRadius", localTick, 5.0F);
            break;
         case "night_vision":
            this.recordFloatEnd(clip.nightVisionOpacity, "nightVisionOpacity", localTick, 0.0F);
            break;
         case "underwater":
            this.recordFloatEnd(clip.underwaterOpacity, "underwaterOpacity", localTick, 0.0F);
      }
   }

   private void recordEffectKeyframes(
      ScreenEffectPovActionClip clip,
      String effectId,
      int localTick,
      ClientPlayerEntity player,
      float vignetteDarkness,
      BlockState suffocationState,
      boolean inPortalBlock,
      boolean isNewlyStarted
   ) {
      switch (effectId) {
         case "vignette":
            this.recordFloatChannel(clip.vignetteOpacity, "vignetteOpacity", localTick, vignetteDarkness, 0.0F, 0.01F, isNewlyStarted);
            if (isNewlyStarted || clip.vignetteColor.isEmpty()) {
               if (localTick > 0 && clip.vignetteColor.isEmpty()) {
                  clip.vignetteColor.insert(0.0F, new Color(0.0F, 0.0F, 0.0F, 1.0F));
                  if (localTick > 1) {
                     clip.vignetteColor.insert((float)localTick - 1.0F, new Color(0.0F, 0.0F, 0.0F, 1.0F));
                  }
               }

               clip.vignetteColor.insert((float)localTick, new Color(0.0F, 0.0F, 0.0F, 1.0F));
            }
            break;
         case "nausea":
            float intensity = player.nauseaIntensity;
            this.recordFloatChannel(clip.nauseaDistortion, "nauseaDistortion", localTick, intensity, 0.0F, 0.01F, isNewlyStarted);
            this.recordFloatChannel(clip.nauseaOpacity, "nauseaOpacity", localTick, 1.0F, 0.0F, 0.01F, isNewlyStarted);
            break;
         case "frost":
            float freeze = player.getFreezingScale();
            boolean scoping = player.isUsingSpyglass() || player.isUsingItem() && player.getActiveItem().isOf(Items.SPYGLASS);
            Float lastFrostZoom = this.lastFloatValues.get("frostZoom");
            float fovMult = scoping ? (lastFrostZoom != null ? lastFrostZoom : 1.0F) : player.getFovMultiplier();
            this.recordFloatChannel(clip.frostProgress, "frostProgress", localTick, freeze, 0.0F, 0.01F, isNewlyStarted);
            this.recordFloatChannel(clip.frostZoom, "frostZoom", localTick, fovMult, 1.0F, 0.005F, isNewlyStarted);
            break;
         case "portal":
            float portalVal = inPortalBlock && player.nauseaIntensity <= 0.001F ? 1.0F : player.nauseaIntensity;
            this.recordFloatChannel(clip.portalOpacity, "portalOpacity", localTick, portalVal, 0.0F, 0.01F, isNewlyStarted);
         case "fire":
         default:
            break;
         case "pumpkin":
            this.recordFloatChannel(clip.pumpkinOpacity, "pumpkinOpacity", localTick, 1.0F, 0.0F, 0.01F, isNewlyStarted);
            break;
         case "spyglass":
            float liveScale = 1.12F;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.inGameHud instanceof InGameHudVignettePovAccess accessHud) {
               float hudScale = accessHud.bbsPov$getSpyglassScale();
               if (hudScale > 0.001F) {
                  liveScale = hudScale;
               }
            }

            this.recordFloatChannel(clip.spyglassScale, "spyglassScale", localTick, liveScale, 0.5F, 0.005F, isNewlyStarted);
            int useTime = player.getItemUseTime();
            float zoom = (float)(0.102 + 0.898 * Math.pow(0.5, (double)Math.max(0, useTime)));
            if (zoom <= 0.1025F) {
               zoom = 0.102F;
            }

            zoom = MathHelper.clamp(zoom, 0.102F, 1.0F);
            this.spyglassLastZoom = zoom;
            if (isNewlyStarted) {
               if (localTick > 0) {
                  if (clip.spyglassZoom.isEmpty()) {
                     clip.spyglassZoom.insert(0.0F, 1.0F);
                  }

                  clip.spyglassZoom.insert((float)(localTick - 1), 1.0F);
               }

               clip.spyglassZoom.insert((float)localTick, zoom);
               this.lastFloatValues.put("spyglassZoom", zoom);
               this.lastRecordedTicks.put("spyglassZoom", localTick);
            } else {
               Float lastVal = this.lastFloatValues.get("spyglassZoom");
               if (lastVal == null || Math.abs(zoom - lastVal) >= 0.005F || zoom == 0.102F && lastVal > 0.102F) {
                  clip.spyglassZoom.insert((float)localTick, zoom);
                  this.lastFloatValues.put("spyglassZoom", zoom);
                  this.lastRecordedTicks.put("spyglassZoom", localTick);
               }
            }
            break;
         case "suffocation":
            String blockId = "minecraft:stone";
            if (suffocationState != null) {
               Identifier id = Registries.BLOCK.getId(suffocationState.getBlock());
               if (id != null) {
                  blockId = id.toString();
               }
            }

            this.recordFloatChannel(clip.suffocationOpacity, "suffocationOpacity", localTick, 1.0F, 0.0F, 0.02F, isNewlyStarted);
            if (isNewlyStarted || clip.suffocationBlock.isEmpty()) {
               if (localTick > 0 && clip.suffocationBlock.isEmpty()) {
                  clip.suffocationBlock.insert(0.0F, blockId);
                  if (localTick > 1) {
                     clip.suffocationBlock.insert((float)localTick - 1.0F, blockId);
                  }
               }

               clip.suffocationBlock.insert((float)localTick, blockId);
            }
            break;
         case "darkness":
            float darkFactor = 1.0F;
            if (player.hasStatusEffect(StatusEffects.DARKNESS)) {
               StatusEffectInstance statusInst = player.getStatusEffect(StatusEffects.DARKNESS);
               if (statusInst != null && statusInst.getFactorCalculationData().isPresent()) {
                  darkFactor = ((FactorCalculationData)statusInst.getFactorCalculationData().get()).lerp(player, 1.0F);
               }
            }

            this.recordFloatChannel(clip.darknessOpacity, "darknessOpacity", localTick, darkFactor, 0.0F, 0.02F, isNewlyStarted);
            this.recordFloatChannel(clip.darknessRadius, "darknessRadius", localTick, 15.0F, 15.0F, 0.1F, isNewlyStarted);
            break;
         case "blindness":
            this.recordFloatChannel(clip.blindnessOpacity, "blindnessOpacity", localTick, 1.0F, 0.0F, 0.02F, isNewlyStarted);
            this.recordFloatChannel(clip.blindnessRadius, "blindnessRadius", localTick, 5.0F, 5.0F, 0.1F, isNewlyStarted);
            break;
         case "night_vision":
            float strength = 1.0F;
            if (player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
               StatusEffectInstance statusInst = player.getStatusEffect(StatusEffects.NIGHT_VISION);
               if (statusInst != null && statusInst.getDuration() <= 200) {
                  int dur = statusInst.getDuration();
                  float f = 0.7F + MathHelper.sin(((float)dur - 1.0F) * (float) Math.PI * 0.2F) * 0.3F;
                  strength = MathHelper.clamp(f, 0.0F, 1.0F);
               }
            }

            this.recordFloatChannel(clip.nightVisionOpacity, "nightVisionOpacity", localTick, strength, 0.0F, 0.02F, isNewlyStarted);
            break;
         case "underwater":
            this.recordFloatChannel(clip.underwaterOpacity, "underwaterOpacity", localTick, 0.1F, 0.0F, 0.02F, isNewlyStarted);
      }
   }

   public void finish(ReplayKeyframesPovAccess access, int endTick) {
      if (this.recordingClip != null) {
         if (!this.currentlyActiveEffects.isEmpty()) {
            int localTick = endTick - (Integer)this.recordingClip.tick.get();

            for (String effectId : this.currentlyActiveEffects) {
               recordVisible(this.recordingClip, effectId, localTick, false);
               this.recordEffectEnd(this.recordingClip, effectId, localTick);
            }

            this.currentlyActiveEffects.clear();
         }

         this.recordingClip.trimToRecording(endTick);
         this.recordingClip.ensureBakingBounds();
         this.recordingClip = null;
         this.wasScoping = false;
         this.spyglassReleaseTicksRemaining = 0;
         this.spyglassLastZoom = 1.0F;
         this.lastFloatValues.clear();
         this.lastRecordedTicks.clear();
      }

      if (access != null && access.bbsPov$getActions() != null) {
         for (Clip clip : access.bbsPov$getActions().get()) {
            if (clip instanceof ScreenEffectPovActionClip effectClip) {
               effectClip.trimToRecording(endTick);
               effectClip.ensureBakingBounds();
            }
         }
      }
   }
}

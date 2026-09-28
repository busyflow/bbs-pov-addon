package Glaxium.POV.hud.recording;

import Glaxium.POV.hud.HudMount;
import Glaxium.POV.hud.RecordedHudData;
import java.util.Arrays;
import java.util.Objects;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public final class HudRecorder {
   private static final int HEALTH_STABLE_TICKS = 20;
   private static final int DAMAGE_FLASH_TICKS = 20;
   private static final int HEAL_FLASH_TICKS = 10;
   private int lastRecordedTick = Integer.MIN_VALUE;
   private int lastRecordedHealth;
   private int recordedRenderHealth;
   private int lastHealthChangeTick;
   private int healthFlashEndTick;
   private Integer lastHealth = null;
   private Integer lastPrevHealth = null;
   private Integer lastHealthContainer = null;
   private Integer lastAbsorption = null;
   private Integer lastAbsorptionContainer = null;
   private Integer lastHeartType = null;
   private Boolean lastHardcore = null;
   private Boolean lastRegeneration = null;
   private Integer lastArmor = null;
   private Integer lastHunger = null;
   private Boolean lastHungerEffect = null;
   private Integer lastMountHealthContainer = null;
   private Integer lastMountHealth = null;
   private Integer lastAir = null;
   private Double lastExperience = null;
   private Integer lastExperienceLevel = null;
   private Boolean lastHeartFlash = null;
   private Float lastAttackCooldown = null;
   private Boolean lastStatusBarsVisible = null;
   private final ItemStack[] lastInventory = new ItemStack[27];

   public void reset() {
      this.lastRecordedTick = Integer.MIN_VALUE;
      this.lastRecordedHealth = 20;
      this.recordedRenderHealth = 20;
      this.lastHealthChangeTick = 0;
      this.healthFlashEndTick = 0;
      this.lastHealth = null;
      this.lastPrevHealth = null;
      this.lastHealthContainer = null;
      this.lastAbsorption = null;
      this.lastAbsorptionContainer = null;
      this.lastHeartType = null;
      this.lastHardcore = null;
      this.lastRegeneration = null;
      this.lastArmor = null;
      this.lastHunger = null;
      this.lastHungerEffect = null;
      this.lastMountHealthContainer = null;
      this.lastMountHealth = null;
      this.lastAir = null;
      this.lastExperience = null;
      this.lastExperienceLevel = null;
      this.lastHeartFlash = null;
      this.lastAttackCooldown = null;
      this.lastStatusBarsVisible = null;
      Arrays.fill(this.lastInventory, null);
   }

   public void record(RecordedHudData data, int tick, PlayerEntity player) {
      int absorptionValue = Math.max(0, Math.round(player.getAbsorptionAmount()));
      int healthValue = Math.max(0, (int)Math.ceil((double)player.getHealth()));
      boolean firstFrame = this.lastRecordedTick == Integer.MIN_VALUE || tick <= this.lastRecordedTick;
      if (firstFrame) {
         this.lastRecordedHealth = healthValue;
         this.recordedRenderHealth = healthValue;
         this.lastHealthChangeTick = tick;
         this.healthFlashEndTick = tick;
      } else if (healthValue != this.lastRecordedHealth) {
         this.lastHealthChangeTick = tick;
         this.healthFlashEndTick = tick + (healthValue < this.lastRecordedHealth ? 20 : 10);
      } else if (tick - this.lastHealthChangeTick > 20) {
         this.recordedRenderHealth = healthValue;
      }

      boolean healthFlash = tick < this.healthFlashEndTick;
      int healthContainer = Math.round(player.getMaxHealth());
      int heartType = player.hasStatusEffect(StatusEffects.POISON) ? 1 : (player.hasStatusEffect(StatusEffects.WITHER) ? 2 : (player.isFrozen() ? 4 : 0));
      boolean hardcore = player.getWorld().getLevelProperties().isHardcore();
      boolean regeneration = player.hasStatusEffect(StatusEffects.REGENERATION);
      int armor = player.getArmor();
      int hunger = player.getHungerManager().getFoodLevel();
      boolean hungerEffect = player.hasStatusEffect(StatusEffects.HUNGER);
      LivingEntity mount = HudMount.jumpingMount(player);
      int mountSlots = HudMount.heartSlots(mount);
      int mountHealthContainer = mountSlots * 2;
      int mountHealth = mount == null ? 0 : Math.max(0, (int)Math.ceil((double)mount.getHealth()));
      int air = Math.max(0, Math.min(300, player.getAir()));
      double experience = (double)player.experienceProgress;
      int experienceLevel = player.experienceLevel;
      MinecraftClient client = MinecraftClient.getInstance();
      float attackCooldown = client != null && client.currentScreen != null ? 1.0F : player.getAttackCooldownProgress(0.0F);
      boolean statusBarsVisible = client.interactionManager == null || client.interactionManager.hasStatusBars();
      recordValue(data.health, healthValue, (float)tick, this.lastHealth);
      this.lastHealth = healthValue;
      recordValue(data.previousHealth, this.recordedRenderHealth, (float)tick, this.lastPrevHealth);
      this.lastPrevHealth = this.recordedRenderHealth;
      recordValue(data.healthContainer, healthContainer, (float)tick, this.lastHealthContainer);
      this.lastHealthContainer = healthContainer;
      recordValue(data.absorption, absorptionValue, (float)tick, this.lastAbsorption);
      this.lastAbsorption = absorptionValue;
      recordValue(data.absorptionContainer, absorptionValue, (float)tick, this.lastAbsorptionContainer);
      this.lastAbsorptionContainer = absorptionValue;
      recordValue(data.heartType, heartType, (float)tick, this.lastHeartType);
      this.lastHeartType = heartType;
      recordValue(data.hardcore, hardcore, (float)tick, this.lastHardcore);
      this.lastHardcore = hardcore;
      recordValue(data.regeneration, regeneration, (float)tick, this.lastRegeneration);
      this.lastRegeneration = regeneration;
      recordValue(data.armor, armor, (float)tick, this.lastArmor);
      this.lastArmor = armor;
      recordValue(data.hunger, hunger, (float)tick, this.lastHunger);
      this.lastHunger = hunger;
      recordValue(data.hungerEffect, hungerEffect, (float)tick, this.lastHungerEffect);
      this.lastHungerEffect = hungerEffect;
      recordValue(data.mountHealthContainer, mountHealthContainer, (float)tick, this.lastMountHealthContainer);
      this.lastMountHealthContainer = mountHealthContainer;
      recordValue(data.mountHealth, mountHealth, (float)tick, this.lastMountHealth);
      this.lastMountHealth = mountHealth;
      recordValue(data.air, air, (float)tick, this.lastAir);
      this.lastAir = air;
      if (this.lastExperience == null || Math.abs(experience - this.lastExperience) > 1.0E-4) {
         recordValue(data.experience, experience, (float)tick, this.lastExperience);
         this.lastExperience = experience;
      }

      recordValue(data.experienceLevel, experienceLevel, (float)tick, this.lastExperienceLevel);
      this.lastExperienceLevel = experienceLevel;
      recordValue(data.heartFlash, healthFlash, (float)tick, this.lastHeartFlash);
      this.lastHeartFlash = healthFlash;
      if (this.lastAttackCooldown == null || Math.abs(attackCooldown - this.lastAttackCooldown) > 0.001F) {
         if (this.lastAttackCooldown != null && this.lastAttackCooldown >= 0.999F && attackCooldown < 0.999F && tick > 0) {
            recordValue(data.attackCooldown, 1.0F, (float)(tick - 1), this.lastAttackCooldown);
         }

         recordValue(data.attackCooldown, attackCooldown, (float)tick, this.lastAttackCooldown);
         this.lastAttackCooldown = attackCooldown;
      }

      recordValue(data.statusBarsVisible, statusBarsVisible, (float)tick, this.lastStatusBarsVisible);
      this.lastStatusBarsVisible = statusBarsVisible;
      boolean inventoryChanged = false;

      for (int i = 0; i < 27; i++) {
         ItemStack stack = (ItemStack)player.getInventory().main.get(9 + i);
         ItemStack cur = stack == null ? ItemStack.EMPTY : stack;
         if (this.lastInventory[i] == null || !ItemStack.areEqual(cur, this.lastInventory[i])) {
            data.inventory.get(i).insert((float)tick, cur.copy());
            this.lastInventory[i] = cur.copy();
            inventoryChanged = true;
         }
      }

      if (firstFrame || inventoryChanged) {
         data.inventoryAnchor.insert((float)tick, true);
      }

      this.lastRecordedTick = tick;
      this.lastRecordedHealth = healthValue;
   }

   private static <T> void recordValue(KeyframeChannel<T> channel, T value, float tick, T lastVal) {
      if (channel != null) {
         if (channel.isEmpty() || !Objects.equals(value, lastVal)) {
            channel.insert(tick, value);
         }
      }
   }
}

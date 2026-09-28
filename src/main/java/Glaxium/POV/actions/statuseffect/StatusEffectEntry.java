package Glaxium.POV.actions.statuseffect;

import mchorse.bbs_mod.data.types.MapType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class StatusEffectEntry {
   private String effectId = "minecraft:speed";
   private boolean unlimited = false;
   private int durationSeconds = 100;
   private int amplifier = 0;

   public StatusEffectEntry() {
   }

   public StatusEffectEntry(String effectId, boolean unlimited, int durationSeconds, int amplifier) {
      this.effectId = effectId != null ? effectId : "minecraft:speed";
      this.unlimited = unlimited;
      this.durationSeconds = Math.max(1, durationSeconds);
      this.amplifier = Math.max(0, amplifier);
   }

   public String getEffectId() {
      return this.effectId;
   }

   public void setEffectId(String effectId) {
      this.effectId = effectId != null ? effectId : "minecraft:speed";
   }

   public boolean isUnlimited() {
      return this.unlimited;
   }

   public void setUnlimited(boolean unlimited) {
      this.unlimited = unlimited;
   }

   public int getDurationSeconds() {
      return this.durationSeconds;
   }

   public void setDurationSeconds(int durationSeconds) {
      this.durationSeconds = Math.max(1, durationSeconds);
   }

   public int getAmplifier() {
      return this.amplifier;
   }

   public void setAmplifier(int amplifier) {
      this.amplifier = Math.max(0, amplifier);
   }

   public StatusEffect getStatusEffect() {
      Identifier id = Identifier.tryParse(this.effectId);
      return id != null && Registries.STATUS_EFFECT.containsId(id) ? (StatusEffect)Registries.STATUS_EFFECT.get(id) : StatusEffects.SPEED;
   }

   public String getDisplayName() {
      StatusEffect effect = this.getStatusEffect();
      if (effect != null) {
         Text name = effect.getName();
         if (name != null) {
            return name.getString();
         }
      }

      return this.effectId;
   }

   public int getRemainingSeconds(float elapsedTicks) {
      if (this.unlimited) {
         return -1;
      } else {
         int elapsedSec = (int)Math.floor((double)(elapsedTicks / 20.0F));
         return Math.max(0, this.durationSeconds - elapsedSec);
      }
   }

   public boolean isExpired(float elapsedTicks) {
      return this.unlimited ? false : elapsedTicks >= (float)this.durationSeconds * 20.0F;
   }

   public String formatDuration(float elapsedTicks) {
      if (this.unlimited) {
         return "∞";
      } else {
         int remaining = this.getRemainingSeconds(elapsedTicks);
         int mins = remaining / 60;
         int secs = remaining % 60;
         return String.format("%d:%02d", mins, secs);
      }
   }

   public String formatDurationSecondsOnly(int seconds) {
      if (this.unlimited) {
         return "∞";
      } else {
         int mins = seconds / 60;
         int secs = seconds % 60;
         return mins > 0 ? String.format("%dmin %02ds (%d:%02d)", mins, secs, mins, secs) : String.format("%ds (0:%02d)", secs, secs);
      }
   }

   public StatusEffectEntry copy() {
      return new StatusEffectEntry(this.effectId, this.unlimited, this.durationSeconds, this.amplifier);
   }

   public void toData(MapType data) {
      data.putString("id", this.effectId);
      data.putBool("unlimited", this.unlimited);
      data.putInt("duration", this.durationSeconds);
      data.putInt("amplifier", this.amplifier);
   }

   public void fromData(MapType data) {
      if (data.has("id")) {
         this.effectId = data.getString("id");
      }

      if (data.has("unlimited")) {
         this.unlimited = data.getBool("unlimited");
      }

      if (data.has("duration")) {
         this.durationSeconds = data.getInt("duration");
      }

      if (data.has("amplifier")) {
         this.amplifier = data.getInt("amplifier");
      }
   }
}

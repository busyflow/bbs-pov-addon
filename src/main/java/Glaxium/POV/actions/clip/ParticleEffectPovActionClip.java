package Glaxium.POV.actions.clip;

import Glaxium.POV.actions.PovActionType;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public final class ParticleEffectPovActionClip extends ViewParticlePovActionClip {
   public final ValueString particle = new ValueString("particle", "minecraft:poof");
   public final ValueString blockId = new ValueString("block", "minecraft:stone");
   public final ValueString itemId = new ValueString("item", "minecraft:apple");
   public final ValueFloat dustR = new ValueFloat("dust_r", 1.0F, 0.0F, 1.0F);
   public final ValueFloat dustG = new ValueFloat("dust_g", 0.0F, 0.0F, 1.0F);
   public final ValueFloat dustB = new ValueFloat("dust_b", 0.0F, 0.0F, 1.0F);
   public final ValueFloat dustScale = new ValueFloat("dust_scale", 1.0F, 0.01F, 4.0F);

   public ParticleEffectPovActionClip() {
      this.add(this.particle);
      this.add(this.blockId);
      this.add(this.itemId);
      this.add(this.dustR);
      this.add(this.dustG);
      this.add(this.dustB);
      this.add(this.dustScale);
   }

   public ItemStack extraItem() {
      return parseItem((String)this.itemId.get(), Items.APPLE);
   }

   static ItemStack parseItem(String id, Item fallback) {
      Identifier identifier = Identifier.tryParse(id);
      Item item = identifier == null ? fallback : (Item)Registries.ITEM.get(identifier);
      if (item == null || item == Items.AIR) {
         item = fallback != null && fallback != Items.AIR ? fallback : Items.APPLE;
      }

      return new ItemStack(item);
   }

   @Override
   public PovActionType getActionType() {
      return PovActionType.PARTICLE_EFFECT;
   }

   protected Clip create() {
      return new ParticleEffectPovActionClip();
   }
}

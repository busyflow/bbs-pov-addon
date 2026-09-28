package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.integration.access.minecraft.HeldItemRendererPovAccess;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({HeldItemRenderer.class})
public interface HeldItemRendererPovAccessor extends HeldItemRendererPovAccess {
   @Accessor("mainHand")
   @Override
   ItemStack bbsPov$getMainHand();

   @Accessor("mainHand")
   @Override
   void bbsPov$setMainHand(ItemStack var1);

   @Accessor("offHand")
   @Override
   ItemStack bbsPov$getOffHand();

   @Accessor("offHand")
   @Override
   void bbsPov$setOffHand(ItemStack var1);

   @Accessor("equipProgressMainHand")
   @Override
   float bbsPov$getEquipProgressMainHand();

   @Accessor("equipProgressMainHand")
   @Override
   void bbsPov$setEquipProgressMainHand(float var1);

   @Accessor("prevEquipProgressMainHand")
   @Override
   float bbsPov$getPrevEquipProgressMainHand();

   @Accessor("prevEquipProgressMainHand")
   @Override
   void bbsPov$setPrevEquipProgressMainHand(float var1);

   @Accessor("equipProgressOffHand")
   @Override
   float bbsPov$getEquipProgressOffHand();

   @Accessor("equipProgressOffHand")
   @Override
   void bbsPov$setEquipProgressOffHand(float var1);

   @Accessor("prevEquipProgressOffHand")
   @Override
   float bbsPov$getPrevEquipProgressOffHand();

   @Accessor("prevEquipProgressOffHand")
   @Override
   void bbsPov$setPrevEquipProgressOffHand(float var1);

   @Invoker("renderArmHoldingItem")
   @Override
   void bbsPov$renderArmHoldingItem(MatrixStack var1, VertexConsumerProvider var2, int var3, float var4, float var5, Arm var6);
}

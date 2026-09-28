package Glaxium.POV.integration.access.minecraft;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;

public interface HeldItemRendererPovAccess {
   ItemStack bbsPov$getMainHand();

   void bbsPov$setMainHand(ItemStack var1);

   ItemStack bbsPov$getOffHand();

   void bbsPov$setOffHand(ItemStack var1);

   float bbsPov$getEquipProgressMainHand();

   void bbsPov$setEquipProgressMainHand(float var1);

   float bbsPov$getPrevEquipProgressMainHand();

   void bbsPov$setPrevEquipProgressMainHand(float var1);

   float bbsPov$getEquipProgressOffHand();

   void bbsPov$setEquipProgressOffHand(float var1);

   float bbsPov$getPrevEquipProgressOffHand();

   void bbsPov$setPrevEquipProgressOffHand(float var1);

   void bbsPov$renderArmHoldingItem(MatrixStack var1, VertexConsumerProvider var2, int var3, float var4, float var5, Arm var6);
}

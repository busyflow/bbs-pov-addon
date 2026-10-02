package Glaxium.POV.integration.access.minecraft;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;

/** Feature-facing accessor. Mixin implements this; do not import mixin types from features. */
public interface HeldItemRendererPovAccess
{
    ItemStack bbsPov$getMainHand();
    void bbsPov$setMainHand(ItemStack stack);
    ItemStack bbsPov$getOffHand();
    void bbsPov$setOffHand(ItemStack stack);
    float bbsPov$getEquipProgressMainHand();
    void bbsPov$setEquipProgressMainHand(float value);
    float bbsPov$getPrevEquipProgressMainHand();
    void bbsPov$setPrevEquipProgressMainHand(float value);
    float bbsPov$getEquipProgressOffHand();
    void bbsPov$setEquipProgressOffHand(float value);
    float bbsPov$getPrevEquipProgressOffHand();
    void bbsPov$setPrevEquipProgressOffHand(float value);

    void bbsPov$renderArmHoldingItem(
        MatrixStack matrices,
        VertexConsumerProvider consumers,
        int light,
        float equipProgress,
        float swingProgress,
        Arm arm);
}

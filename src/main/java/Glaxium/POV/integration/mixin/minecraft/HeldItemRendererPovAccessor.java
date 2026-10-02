package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.integration.access.minecraft.HeldItemRendererPovAccess;

import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HeldItemRenderer.class)
public interface HeldItemRendererPovAccessor extends HeldItemRendererPovAccess
{
    @Accessor("mainHand") ItemStack bbsPov$getMainHand();
    @Accessor("mainHand") void bbsPov$setMainHand(ItemStack stack);
    @Accessor("offHand") ItemStack bbsPov$getOffHand();
    @Accessor("offHand") void bbsPov$setOffHand(ItemStack stack);
    @Accessor("equipProgressMainHand") float bbsPov$getEquipProgressMainHand();
    @Accessor("equipProgressMainHand") void bbsPov$setEquipProgressMainHand(float value);
    @Accessor("prevEquipProgressMainHand") float bbsPov$getPrevEquipProgressMainHand();
    @Accessor("prevEquipProgressMainHand") void bbsPov$setPrevEquipProgressMainHand(float value);
    @Accessor("equipProgressOffHand") float bbsPov$getEquipProgressOffHand();
    @Accessor("equipProgressOffHand") void bbsPov$setEquipProgressOffHand(float value);
    @Accessor("prevEquipProgressOffHand") float bbsPov$getPrevEquipProgressOffHand();
    @Accessor("prevEquipProgressOffHand") void bbsPov$setPrevEquipProgressOffHand(float value);

    @Invoker("renderArmHoldingItem")
    void bbsPov$renderArmHoldingItem(
        MatrixStack matrices,
        VertexConsumerProvider consumers,
        int light,
        float equipProgress,
        float swingProgress,
        Arm arm);
}

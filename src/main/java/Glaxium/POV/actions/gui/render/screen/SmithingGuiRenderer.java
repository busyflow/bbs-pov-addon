package Glaxium.POV.actions.gui.render.screen;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.render.GuiRenderContext;
import Glaxium.POV.actions.gui.render.GuiRenderer;
import Glaxium.POV.actions.gui.render.GuiScreenChrome;
import Glaxium.POV.actions.gui.render.common.GuiSlotRenderer;
import Glaxium.POV.actions.gui.render.common.GuiStandardLayoutRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import org.joml.Quaternionf;

public final class SmithingGuiRenderer implements GuiRenderer, GuiScreenChrome {
   public static final SmithingGuiRenderer INSTANCE = new SmithingGuiRenderer();
   private static final Quaternionf SMITHING_ARMOR_STAND_ROTATION = new Quaternionf().rotationXYZ(0.43633232F, 0.0F, (float) Math.PI);

   private SmithingGuiRenderer() {
   }

   @Override
   public void render(GuiRenderContext ctx) {
      GuiStandardLayoutRenderer.render(ctx, this);
   }

   @Override
   public void drawEarlyChrome(GuiRenderContext ctx) {
      ItemStack template = GuiSlotRenderer.sampleSlot(ctx.clip, "smithing_table", "template", ctx.localTick);
      ItemStack base = GuiSlotRenderer.sampleSlot(ctx.clip, "smithing_table", "base", ctx.localTick);
      ItemStack addition = GuiSlotRenderer.sampleSlot(ctx.clip, "smithing_table", "addition", ctx.localTick);
      ItemStack result = GuiSlotRenderer.sampleSlot(ctx.clip, "smithing_table", "result", ctx.localTick);
      boolean hasInput = !template.isEmpty() || !base.isEmpty() || !addition.isEmpty();
      boolean hasInvalidRecipe = hasInput && result.isEmpty();
      if (hasInvalidRecipe) {
         ctx.batcher.getContext().drawTexture(ctx.entry.texture, 65, 46, 176.0F, 0.0F, 28, 21, 256, 256);
      }
   }

   @Override
   public void drawPreview(GuiRenderContext ctx) {
      drawArmorStand(ctx.batcher, ctx.clip, ctx.localTick, ctx.originX, ctx.originY, ctx.scaleX, ctx.scaleY);
   }

   private static void drawArmorStand(Batcher2D batcher, GuiPovActionClip clip, float tick, float originX, float originY, float scaleX, float scaleY) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.world != null) {
         ArmorStandEntity stand = new ArmorStandEntity(client.world, 0.0, 0.0, 0.0);
         stand.setHideBasePlate(true);
         stand.setShowArms(true);
         stand.bodyYaw = 210.0F;
         stand.setPitch(25.0F);
         stand.headYaw = stand.getYaw();
         stand.prevHeadYaw = stand.getYaw();

         for (EquipmentSlot slot : EquipmentSlot.values()) {
            stand.equipStack(slot, ItemStack.EMPTY);
         }

         ItemStack output = GuiSlotRenderer.sampleSlot(clip, "smithing_table", "result", tick);
         if (output != null && !output.isEmpty()) {
            ItemStack copy = output.copy();
            if (copy.getItem() instanceof ArmorItem armor) {
               stand.equipStack(armor.getSlotType(), copy);
            } else {
               stand.equipStack(EquipmentSlot.OFFHAND, copy);
            }
         }

         try {
            batcher.flush();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
            InventoryScreen.drawEntity(
               batcher.getContext(),
               Math.round(originX + 141.0F * scaleX),
               Math.round(originY + 75.0F * scaleY),
               Math.round(25.0F * Math.min(scaleX, scaleY)),
               SMITHING_ARMOR_STAND_ROTATION,
               null,
               stand
            );
            batcher.getContext().draw();
            batcher.flush();
         } catch (Exception var16) {
         } finally {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
         }
      }
   }
}

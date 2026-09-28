package Glaxium.POV.actions.gui.render.screen;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.render.GuiRenderContext;
import Glaxium.POV.actions.gui.render.GuiRenderer;
import Glaxium.POV.actions.gui.render.GuiScreenChrome;
import Glaxium.POV.actions.gui.render.common.GuiEntityPreviewRenderer;
import Glaxium.POV.actions.gui.render.common.GuiSlotRenderer;
import Glaxium.POV.actions.gui.render.common.GuiStandardLayoutRenderer;
import Glaxium.POV.integration.access.minecraft.AbstractHorseEntityPovAccess;
import Glaxium.POV.integration.access.minecraft.HorseEntityPovAccess;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public final class MountGuiRenderer implements GuiRenderer, GuiScreenChrome {
   public static final MountGuiRenderer INSTANCE = new MountGuiRenderer();
   private static final int HORSE_SADDLED_FLAG = 4;
   private static HorseEntity previewHorse;
   private static DonkeyEntity previewDonkey;

   private MountGuiRenderer() {
   }

   @Override
   public void render(GuiRenderContext ctx) {
      GuiStandardLayoutRenderer.render(ctx, this);
   }

   @Override
   public void drawPreview(GuiRenderContext ctx) {
      drawMount(
         ctx.batcher,
         ctx.clip,
         ctx.guiId,
         ctx.localTick,
         ctx.originX,
         ctx.originY,
         ctx.scaleX,
         ctx.scaleY,
         ctx.curScreenX,
         ctx.curScreenY,
         ctx.screenWidth,
         ctx.screenHeight
      );
   }

   private static void drawMount(
      Batcher2D batcher,
      GuiPovActionClip clip,
      String guiId,
      float tick,
      float originX,
      float originY,
      float scaleX,
      float scaleY,
      float lookScreenX,
      float lookScreenY,
      int screenWidth,
      int screenHeight
   ) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.world != null) {
         AbstractHorseEntity mount = (AbstractHorseEntity)("donkey".equals(guiId) ? donkeyPreview(client.world) : horsePreview(client.world));
         if (mount != null) {
            applyMountPreviewState(mount, clip, guiId, tick);
            int x1 = Math.round(originX + 26.0F * scaleX);
            int y1 = Math.round(originY + 18.0F * scaleY);
            int x2 = Math.round(originX + 78.0F * scaleX);
            int y2 = Math.round(originY + 70.0F * scaleY);
            int size = Math.max(1, Math.round(17.0F * Math.min(scaleX, scaleY)));
            float centerX = (float)(x1 + x2) / 2.0F;
            float centerY = (float)(y1 + y2) / 2.0F;
            float lookX = centerX + (lookScreenX - centerX) / Math.max(1.0E-4F, scaleX);
            float lookY = centerY + (lookScreenY - centerY) / Math.max(1.0E-4F, scaleY);

            try {
               batcher.flush();
               RenderSystem.enableDepthTest();
               RenderSystem.depthMask(true);
               RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
               GuiEntityPreviewRenderer.draw(batcher.getContext(), x1, y1, x2, y2, size, 0.5882353F, lookX, lookY, mount, screenWidth, screenHeight);
               batcher.flush();
            } catch (Exception var27) {
            } finally {
               RenderSystem.disableDepthTest();
               RenderSystem.depthMask(false);
            }
         }
      }
   }

   private static HorseEntity horsePreview(World world) {
      if (previewHorse == null || previewHorse.getWorld() != world) {
         previewHorse = (HorseEntity)EntityType.HORSE.create(world);
         if (previewHorse != null) {
            previewHorse.setSilent(true);
         }
      }

      return previewHorse;
   }

   private static DonkeyEntity donkeyPreview(World world) {
      if (previewDonkey == null || previewDonkey.getWorld() != world) {
         previewDonkey = (DonkeyEntity)EntityType.DONKEY.create(world);
         if (previewDonkey != null) {
            previewDonkey.setSilent(true);
         }
      }

      return previewDonkey;
   }

   private static void applyMountPreviewState(AbstractHorseEntity mount, GuiPovActionClip clip, String guiId, float tick) {
      if (mount instanceof HorseEntity horse) {
         ((HorseEntityPovAccess)horse).bbsPov$setHorseVariant(GuiPovActionClip.packHorseVariant(clip.sampleHorseVariant(guiId, tick)));
      }

      if (mount instanceof DonkeyEntity donkey) {
         donkey.setHasChest(clip.isMountChestOpen(guiId, tick));
      }

      ItemStack saddle = GuiSlotRenderer.sampleSlot(clip, guiId, "saddle", tick);
      boolean hasSaddle = saddle != null && !saddle.isEmpty();
      AbstractHorseEntityPovAccess access = (AbstractHorseEntityPovAccess)mount;
      access.bbsPov$setHorseFlag(4, hasSaddle);
      SimpleInventory items = access.bbsPov$getItems();
      if (items != null) {
         items.setStack(0, hasSaddle ? saddle.copy() : ItemStack.EMPTY);
      }

      if (mount instanceof HorseEntity) {
         ItemStack armor = GuiSlotRenderer.sampleSlot(clip, guiId, "armor", tick);
         ItemStack worn = armor != null && !armor.isEmpty() ? armor.copy() : ItemStack.EMPTY;
         mount.equipStack(EquipmentSlot.CHEST, worn);
         if (items != null && items.size() > 1) {
            items.setStack(1, worn.copy());
         }
      }
   }
}

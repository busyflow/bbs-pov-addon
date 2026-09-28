package Glaxium.POV.actions.gui.render.screen;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.render.GuiRenderContext;
import Glaxium.POV.actions.gui.render.GuiRenderer;
import Glaxium.POV.actions.gui.render.GuiScreenChrome;
import Glaxium.POV.actions.gui.render.common.GuiSlotRenderer;
import Glaxium.POV.actions.gui.render.common.GuiStandardLayoutRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.block.entity.BannerPatterns;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.BannerPattern.Patterns;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.block.entity.BannerBlockEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BannerItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.LoomScreenHandler;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class LoomGuiRenderer implements GuiRenderer, GuiScreenChrome {
   public static final LoomGuiRenderer INSTANCE = new LoomGuiRenderer();
   private static final Identifier LOOM_TEXTURE = new Identifier("textures/gui/container/loom.png");
   private static ModelPart loomBannerField;

   private LoomGuiRenderer() {
   }

   @Override
   public void render(GuiRenderContext ctx) {
      GuiStandardLayoutRenderer.render(ctx, this);
   }

   @Override
   public void drawEarlyChrome(GuiRenderContext ctx) {
      float cursorX = ctx.cursorVisible ? ctx.cursorGuiX : -1000.0F;
      float cursorY = ctx.cursorVisible ? ctx.cursorGuiY : -1000.0F;
      drawChrome(ctx.batcher, ctx.clip, ctx.localTick, ctx.originX, ctx.originY, ctx.scaleX, ctx.scaleY, cursorX, cursorY);
   }

   private static void drawChrome(
      Batcher2D batcher, GuiPovActionClip clip, float tick, float originX, float originY, float scaleX, float scaleY, float cursorX, float cursorY
   ) {
      ItemStack banner = GuiSlotRenderer.sampleSlot(clip, "loom", "banner", tick);
      ItemStack dye = GuiSlotRenderer.sampleSlot(clip, "loom", "dye", tick);
      ItemStack patternItem = GuiSlotRenderer.sampleSlot(clip, "loom", "pattern", tick);
      ItemStack output = GuiSlotRenderer.sampleSlot(clip, "loom", "result", tick);
      boolean canApply = banner.getItem() instanceof BannerItem && dye.getItem() instanceof DyeItem && BannerBlockEntity.getPatternCount(banner) < 6;
      List<RegistryEntry<BannerPattern>> patterns = canApply ? getLoomPatterns(banner, dye, patternItem) : List.of();
      int selected = findSelectedLoomPattern(patterns, output);
      int rows = MathHelper.ceilDiv(patterns.size(), 4);
      int maxTopRow = Math.max(0, rows - 4);
      int fallbackRow = selected < 0 ? 0 : selected / 4;
      int topRow = MathHelper.clamp((Integer)clip.loomRow.interpolate(tick, fallbackRow), 0, maxTopRow);
      int scrollY = maxTopRow == 0 ? 0 : Math.round((float)topRow * 41.0F / (float)maxTopRow);
      float screenScale = Math.min(scaleX, scaleY);
      batcher.getContext().drawTexture(LOOM_TEXTURE, 119, 13 + scrollY, canApply ? 232 : 244, 0, 12, 15);
      int firstPattern = topRow * 4;
      int shown = Math.min(16, patterns.size() - firstPattern);

      for (int i = 0; i < shown; i++) {
         int patternIndex = firstPattern + i;
         int x = 60 + i % 4 * 14;
         int y = 13 + i / 4 * 14;
         boolean hover = cursorX >= (float)x && cursorX < (float)(x + 14) && cursorY >= (float)y && cursorY < (float)(y + 14);
         int v = patternIndex == selected ? 180 : (hover ? 194 : 166);
         batcher.getContext().drawTexture(LOOM_TEXTURE, x, y, 0, v, 14, 14);
      }

      batcher.getContext().draw();
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(true);
      RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
      DiffuseLighting.disableGuiDepthLighting();

      for (int i = 0; i < shown; i++) {
         int x = 60 + i % 4 * 14;
         int y = 13 + i / 4 * 14;
         renderLoomPattern(batcher, patterns.get(firstPattern + i), originX + (float)x * scaleX, originY + (float)y * scaleY, 6.0F * screenScale, screenScale);
      }

      if (output.getItem() instanceof BannerItem outputBanner) {
         NbtList patternNbt = BannerBlockEntity.getPatternListNbt(output);
         List<Pair<RegistryEntry<BannerPattern>, DyeColor>> outputPatterns = BannerBlockEntity.getPatternsFromNbt(
            outputBanner.getColor(), patternNbt == null ? new NbtList() : patternNbt
         );
         renderBannerCanvas(batcher, outputPatterns, originX + 139.0F * scaleX, originY + 52.0F * scaleY, 24.0F * screenScale, screenScale, false);
      }

      batcher.getContext().draw();
      DiffuseLighting.enableGuiDepthLighting();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
   }

   private static List<RegistryEntry<BannerPattern>> getLoomPatterns(ItemStack banner, ItemStack dye, ItemStack pattern) {
      ClientPlayerEntity player = MinecraftClient.getInstance().player;
      if (player == null) {
         return List.of();
      } else {
         LoomScreenHandler handler = new LoomScreenHandler(0, player.getInventory());
         handler.getBannerSlot().setStack(banner.copy());
         handler.getDyeSlot().setStack(dye.copy());
         handler.getPatternSlot().setStack(pattern.copy());
         handler.onContentChanged(handler.getBannerSlot().inventory);
         return List.copyOf(handler.getBannerPatterns());
      }
   }

   private static int findSelectedLoomPattern(List<RegistryEntry<BannerPattern>> patterns, ItemStack output) {
      NbtList patternNbt = BannerBlockEntity.getPatternListNbt(output);
      if (patternNbt != null && !patternNbt.isEmpty()) {
         String selectedId = patternNbt.getCompound(patternNbt.size() - 1).getString("Pattern");

         for (int i = 0; i < patterns.size(); i++) {
            if (((BannerPattern)patterns.get(i).value()).getId().equals(selectedId)) {
               return i;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   private static void renderLoomPattern(Batcher2D batcher, RegistryEntry<BannerPattern> pattern, float x, float y, float scale, float screenScale) {
      NbtCompound nbt = new NbtCompound();
      nbt.put("Patterns", new Patterns().add(BannerPatterns.BASE, DyeColor.GRAY).add(pattern, DyeColor.WHITE).toNbt());
      ItemStack stack = new ItemStack(Items.GRAY_BANNER);
      BlockItem.setBlockEntityNbt(stack, BlockEntityType.BANNER, nbt);
      renderBannerCanvas(
         batcher, BannerBlockEntity.getPatternsFromNbt(DyeColor.GRAY, BannerBlockEntity.getPatternListNbt(stack)), x, y, scale, screenScale, true
      );
   }

   private static void renderBannerCanvas(
      Batcher2D batcher, List<Pair<RegistryEntry<BannerPattern>, DyeColor>> patterns, float x, float y, float scale, float screenScale, boolean thumbnail
   ) {
      if (loomBannerField == null) {
         loomBannerField = MinecraftClient.getInstance().getEntityModelLoader().getModelPart(EntityModelLayers.BANNER).getChild("flag");
      }

      MatrixStack matrices = new MatrixStack();
      matrices.push();
      if (thumbnail) {
         matrices.translate(x + 0.5F * screenScale, y + 16.0F * screenScale, 0.0F);
      } else {
         matrices.translate(x, y, 0.0F);
      }

      matrices.scale(scale, -scale, 1.0F);
      if (thumbnail) {
         matrices.translate(0.5F, 0.5F, 0.0F);
      }

      matrices.translate(0.5F, 0.5F, 0.5F);
      matrices.scale(0.6666667F, -0.6666667F, -0.6666667F);
      loomBannerField.pitch = 0.0F;
      loomBannerField.pivotY = -32.0F;
      BannerBlockEntityRenderer.renderCanvas(
         matrices, batcher.getContext().getVertexConsumers(), 15728880, OverlayTexture.DEFAULT_UV, loomBannerField, ModelLoader.BANNER_BASE, true, patterns
      );
      matrices.pop();
   }
}

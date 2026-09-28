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
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class GamemodeGuiRenderer implements GuiRenderer, GuiScreenChrome {
   public static final GamemodeGuiRenderer INSTANCE = new GamemodeGuiRenderer();
   private static final Identifier GAMEMODE_TEXTURE = new Identifier("textures/gui/container/gamemode_switcher.png");
   private static final ItemStack GRASS_BLOCK_STACK = new ItemStack(Items.GRASS_BLOCK);
   private static final ItemStack IRON_SWORD_STACK = new ItemStack(Items.IRON_SWORD);
   private static final ItemStack MAP_STACK = new ItemStack(Items.MAP);
   private static final ItemStack ENDER_EYE_STACK = new ItemStack(Items.ENDER_EYE);

   private static void drawSwitcher(Batcher2D batcher, GuiPovActionClip clip, float tick, float originX, float originY, float scaleX, float scaleY) {
      int selectedMode = clip.gamemodeSelection.isEmpty() ? 0 : (Integer)clip.gamemodeSelection.interpolate(tick, 0);
      selectedMode = MathHelper.clamp(selectedMode, 0, 3);
      String title;
      int highlightedSlotIndex;
      switch (selectedMode) {
         case 1:
            title = "Creative Mode";
            highlightedSlotIndex = 0;
            break;
         case 2:
            title = "Adventure Mode";
            highlightedSlotIndex = 2;
            break;
         case 3:
            title = "Spectator Mode";
            highlightedSlotIndex = 3;
            break;
         default:
            title = "Survival Mode";
            highlightedSlotIndex = 1;
      }

      TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
      DrawContext context = batcher.getContext();
      MatrixStack matrices = context.getMatrices();
      matrices.push();
      matrices.translate(originX, originY, 0.0F);
      matrices.scale(scaleX, scaleY, 1.0F);
      context.drawCenteredTextWithShadow(textRenderer, title, 62, 7, 16777215);
      ItemStack[] icons = new ItemStack[]{GRASS_BLOCK_STACK, IRON_SWORD_STACK, MAP_STACK, ENDER_EYE_STACK};

      for (int i = 0; i < 4; i++) {
         int slotX = 3 + i * 31;
         int slotY = 27;
         boolean highlighted = i == highlightedSlotIndex;
         context.drawTexture(GAMEMODE_TEXTURE, slotX, slotY, 0.0F, 75.0F, 26, 26, 128, 128);
         if (highlighted) {
            context.drawTexture(GAMEMODE_TEXTURE, slotX, slotY, 26.0F, 75.0F, 26, 26, 128, 128);
         }

         DiffuseLighting.enableGuiDepthLighting();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(true);
         GuiSlotRenderer.drawSlotItem(batcher, icons[i], slotX + 5, slotY + 5);
         batcher.flush();
         RenderSystem.disableDepthTest();
         RenderSystem.depthMask(false);
         DiffuseLighting.disableGuiDepthLighting();
      }

      Text hintText = Text.empty().append(Text.literal("[ F4 ]").formatted(Formatting.AQUA)).append(Text.literal(" Next").formatted(Formatting.WHITE));
      context.drawCenteredTextWithShadow(textRenderer, hintText, 62, 63, 16777215);
      matrices.pop();
      batcher.flush();
   }

   private GamemodeGuiRenderer() {
   }

   @Override
   public void render(GuiRenderContext ctx) {
      GuiStandardLayoutRenderer.render(ctx, this);
   }

   @Override
   public void drawPreview(GuiRenderContext ctx) {
      drawSwitcher(ctx.batcher, ctx.clip, ctx.localTick, ctx.originX, ctx.originY, ctx.scaleX, ctx.scaleY);
   }
}

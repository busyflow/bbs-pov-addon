package Glaxium.POV.actions.gui.render;

import Glaxium.POV.actions.gui.render.screen.AnvilGuiRenderer;
import Glaxium.POV.actions.gui.render.screen.BeaconGuiRenderer;
import Glaxium.POV.actions.gui.render.screen.BookGuiRenderer;
import Glaxium.POV.actions.gui.render.screen.BrewingGuiRenderer;
import Glaxium.POV.actions.gui.render.screen.CartographyGuiRenderer;
import Glaxium.POV.actions.gui.render.screen.ContainerGuiRenderer;
import Glaxium.POV.actions.gui.render.screen.CreativeGuiRenderer;
import Glaxium.POV.actions.gui.render.screen.EnchantingGuiRenderer;
import Glaxium.POV.actions.gui.render.screen.FurnaceGuiRenderer;
import Glaxium.POV.actions.gui.render.screen.GamemodeGuiRenderer;
import Glaxium.POV.actions.gui.render.screen.LoomGuiRenderer;
import Glaxium.POV.actions.gui.render.screen.MerchantGuiRenderer;
import Glaxium.POV.actions.gui.render.screen.MountGuiRenderer;
import Glaxium.POV.actions.gui.render.screen.SmithingGuiRenderer;
import Glaxium.POV.actions.gui.render.screen.StonecutterGuiRenderer;
import java.util.HashMap;
import java.util.Map;

public final class GuiRendererRegistry {
   private static final Map<String, GuiRenderer> RENDERERS = new HashMap<>();
   private static final GuiRenderer FALLBACK = GuiActionRenderer::renderFallback;

   private GuiRendererRegistry() {
   }

   public static void register(String guiId, GuiRenderer renderer) {
      if (guiId != null && !guiId.isBlank() && renderer != null) {
         RENDERERS.put(guiId, renderer);
      }
   }

   public static GuiRenderer get(String guiId) {
      if (guiId == null) {
         return FALLBACK;
      } else {
         GuiRenderer renderer = RENDERERS.get(guiId);
         return renderer == null ? FALLBACK : renderer;
      }
   }

   static {
      register("villager", MerchantGuiRenderer.INSTANCE);
      register("beacon", BeaconGuiRenderer.INSTANCE);
      register("enchanting_table", EnchantingGuiRenderer.INSTANCE);
      register("loom", LoomGuiRenderer.INSTANCE);
      register("book", BookGuiRenderer.INSTANCE);
      register("creative_inventory", CreativeGuiRenderer.INSTANCE);
      register("horse", MountGuiRenderer.INSTANCE);
      register("donkey", MountGuiRenderer.INSTANCE);
      register("furnace", FurnaceGuiRenderer.INSTANCE);
      register("blast_furnace", FurnaceGuiRenderer.INSTANCE);
      register("smoker", FurnaceGuiRenderer.INSTANCE);
      register("inventory", ContainerGuiRenderer.INSTANCE);
      register("crafting_table", ContainerGuiRenderer.INSTANCE);
      register("grindstone", ContainerGuiRenderer.INSTANCE);
      register("chest", ContainerGuiRenderer.INSTANCE);
      register("large_chest", ContainerGuiRenderer.INSTANCE);
      register("barrel", ContainerGuiRenderer.INSTANCE);
      register("ender_chest", ContainerGuiRenderer.INSTANCE);
      register("shulker_box", ContainerGuiRenderer.INSTANCE);
      register("hopper", ContainerGuiRenderer.INSTANCE);
      register("dispenser", ContainerGuiRenderer.INSTANCE);
      register("dropper", ContainerGuiRenderer.INSTANCE);
      register("anvil", AnvilGuiRenderer.INSTANCE);
      register("smithing_table", SmithingGuiRenderer.INSTANCE);
      register("brewing_stand", BrewingGuiRenderer.INSTANCE);
      register("stonecutter", StonecutterGuiRenderer.INSTANCE);
      register("cartography_table", CartographyGuiRenderer.INSTANCE);
      register("gamemode_switcher", GamemodeGuiRenderer.INSTANCE);
   }
}

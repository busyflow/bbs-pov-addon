package Glaxium.POV.actions.gui.clip;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.GuiRecipeBook;
import Glaxium.POV.actions.gui.GuiSlotSchema;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.item.ItemStack;

public final class GuiClipMigration {
   private GuiClipMigration() {
   }

   public static void migrate(GuiPovActionClip clip, BaseType data) {
      if (data instanceof MapType map) {
         if (map.has("scale") && clip.layout.isEmpty()) {
            float sc = map.getFloat("scale", 1.0F);
            Transform t = new Transform();
            t.scale.set(sc, sc, 1.0F);
            clip.layout.insert(0.0F, t);
         }

         if (map.has("opacity") && clip.opacity.isEmpty()) {
            clip.opacity.insert(0.0F, map.getFloat("opacity", 1.0F));
         }

         if (map.has("darknessOpacity") && clip.darknessOpacity.isEmpty()) {
            clip.darknessOpacity.insert(0.0F, map.getFloat("darknessOpacity", 1.0F));
         }

         String guiId = clip.state.isEmpty() ? "inventory" : (String)clip.state.get(0).getValue();
         migrateLegacyCommon(clip, guiId);
         migrateLegacySlots(clip, guiId);
      }

      for (GuiSlotSchema schema : GuiSlotSchema.getAll()) {
         if (schema.hasCraftingSlots()) {
            populateCraftingAnchor(clip, schema.guiId);
         }
      }
   }

   private static void migrateLegacyCommon(GuiPovActionClip clip, String guiId) {
      copyIfEmpty(clip.getLayout(guiId), clip.layout);
      copyIfEmpty(clip.getOpacity(guiId), clip.opacity);
      copyIfEmpty(clip.getDarknessOpacity(guiId), clip.darknessOpacity);
      copyIfEmpty(clip.getCursorLayout(guiId), clip.cursorLayout);
      copyIfEmpty(clip.getCursorVisible(guiId), clip.cursorVisible);
      copyIfEmpty(clip.getCursorItem(guiId), clip.cursorItem);
      copyIfEmpty(clip.getMouseButtons(guiId), clip.mouseButtons);
      copyIfEmpty(clip.getMouseScroll(guiId), clip.mouseScroll);
      migrateLegacyRecipe(clip, guiId);
   }

   private static void migrateLegacyRecipe(GuiPovActionClip clip, String guiId) {
      String target = GuiRecipeBook.supports(guiId) ? clip.resolveGuiId(guiId) : "inventory";
      copyIfEmpty(clip.getRecipeOpen(target), clip.recipeOpen);
      copyIfEmpty(clip.getRecipeSearch(target), clip.recipeSearch);
      copyIfEmpty(clip.getRecipeSearchFocus(target), clip.recipeSearchFocus);
      copyIfEmpty(clip.getRecipeShowing(target), clip.recipeShowing);
      copyIfEmpty(clip.getRecipeCategory(target), clip.recipeCategory);
      copyIfEmpty(clip.getRecipeSelected(target), clip.recipeSelected);
      copyIfEmpty(clip.getRecipePage(target), clip.recipePage);
      copyIfEmpty(clip.getRecipeButton(target), clip.recipeButton);
      copyIfEmpty(clip.getRecipeSearchSelStart(target), clip.recipeSearchSelStart);
      copyIfEmpty(clip.getRecipeSearchSelEnd(target), clip.recipeSearchSelEnd);
   }

   private static void migrateLegacySlots(GuiPovActionClip clip, String guiId) {
      GuiSlotSchema schema = GuiSlotSchema.get(guiId);

      for (GuiSlotSchema.Slot slot : schema.slots) {
         KeyframeChannel<ItemStack> legacy = GuiSlotChannels.legacySlot(clip, guiId, slot.id());
         copyIfEmpty(clip.getGuiSlot(guiId, slot.id()), legacy);
      }
   }

   private static void populateCraftingAnchor(GuiPovActionClip clip, String guiId) {
      KeyframeChannel<Boolean> anchor = clip.guiCraftingSlotAnchors.get(guiId);
      if (anchor != null && anchor.isEmpty()) {
         for (KeyframeChannel<ItemStack> channel : clip.getCraftingSlots(guiId)) {
            for (Keyframe<ItemStack> keyframe : channel.getKeyframes()) {
               anchor.insert(keyframe.getTick(), true);
            }
         }
      }
   }

   public static <T> void copyIfEmpty(KeyframeChannel<T> target, KeyframeChannel<T> source) {
      if (target != null && target.isEmpty() && source != null && !source.isEmpty()) {
         target.copyOver(source, 0);
      }
   }
}

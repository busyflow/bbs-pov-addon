package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.actions.bossbar.editor.UIBossBarLookKeyframeFactory;
import Glaxium.POV.actions.chat.editor.UIChatTextKeyframeFactory;
import Glaxium.POV.actions.chat.editor.UIExecutedTextKeyframeFactory;
import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.GuiSlotSchema;
import Glaxium.POV.actions.gui.editor.UIGuiSlotKeyframeFactory;
import Glaxium.POV.hud.editor.UIConstrainedNumericKeyframeFactory;
import Glaxium.POV.hud.editor.UIHotbarItemKeyframeFactory;
import Glaxium.POV.hud.editor.UIHotbarTransformKeyframeFactory;
import Glaxium.POV.replay.PovReplaySettings;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {UIKeyframeFactory.class},
   remap = false
)
public abstract class UIKeyframeFactoryPovMixin {
   @Inject(
      method = {"createPanel"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void bbsPov$createLayoutPanel(Keyframe keyframe, UIKeyframes editor, CallbackInfoReturnable<UIKeyframeFactory> info) {
      UIKeyframeSheet sheet = null;
      if (editor != null && editor.getGraph() != null) {
         sheet = editor.getGraph().getSheet(keyframe);
      }

      if (sheet == null && editor != null && editor.getDopeSheet() != null) {
         sheet = editor.getDopeSheet().getSheet(keyframe);
      }

      if (sheet == null && editor != null && editor.getDopeSheet() != null) {
         for (UIKeyframeSheet s : editor.getDopeSheet().getSheets()) {
            if (s.channel == keyframe.getParent() || s.channel != null && s.channel.getKeyframes().contains(keyframe)) {
               sheet = s;
               break;
            }
         }
      }

      if (sheet == null && editor != null && editor.getGraph() != null) {
         for (UIKeyframeSheet sx : editor.getGraph().getSheets()) {
            if (sx.channel == keyframe.getParent() || sx.channel != null && sx.channel.getKeyframes().contains(keyframe)) {
               sheet = sx;
               break;
            }
         }
      }

      String id = null;
      if (sheet != null && sheet.id != null) {
         id = sheet.id;
      } else if (keyframe.getParent() != null) {
         id = keyframe.getParent().getId();
      }

      if (keyframe.getValue() instanceof Transform) {
         boolean isLayout = id != null && (id.contains("layout") || id.contains("cursor"))
            || sheet != null && sheet.title != null && sheet.title.get() != null && sheet.title.get().toLowerCase().contains("layout");
         if (isLayout) {
            info.setReturnValue(new UIHotbarTransformKeyframeFactory(keyframe, editor));
            return;
         }
      }

      if (keyframe.getValue() instanceof ItemStack) {
         info.setReturnValue(new UIHotbarItemKeyframeFactory(keyframe, editor));
      } else {
         if (keyframe.getValue() instanceof String str) {
            if ("bossbar_color".equals(id) || "color".equals(id)) {
               info.setReturnValue(new UIBossBarLookKeyframeFactory(keyframe, editor, UIBossBarLookKeyframeFactory.Mode.COLOR));
               return;
            }

            if ("bossbar_style".equals(id) || "style".equals(id)) {
               info.setReturnValue(new UIBossBarLookKeyframeFactory(keyframe, editor, UIBossBarLookKeyframeFactory.Mode.STYLE));
               return;
            }

            if ("executed_text".equals(id) || "execution_text".equals(id)) {
               info.setReturnValue(new UIExecutedTextKeyframeFactory(keyframe, editor));
               return;
            }

            if ("chat_text".equals(id)) {
               info.setReturnValue(new UIChatTextKeyframeFactory(keyframe, editor));
               return;
            }
         }

         if (id != null) {
            if ("crafting_grid".equals(id) || "inventory_slots".equals(id) || "hotbar_inventory_slots".equals(id)) {
               info.setReturnValue(new UIGuiSlotKeyframeFactory(keyframe, editor));
            } else if ("gui_slots".equals(id)) {
               UIFilmPanel filmPanel = PovReplaySettings.getFilmPanel();
               GuiPovActionClip guiClip = null;
               if (keyframe.getParent() != null && keyframe.getParent().getParent() instanceof GuiPovActionClip clip) {
                  guiClip = clip;
               } else if (filmPanel != null && filmPanel.cameraEditor != null && filmPanel.cameraEditor.getClip() instanceof GuiPovActionClip clip) {
                  guiClip = clip;
               }

               if (guiClip != null) {
                  String guiId = guiClip.state.isEmpty() ? "inventory" : (String)guiClip.state.get(0).getValue();
                  GuiSlotSchema schema = GuiSlotSchema.get(guiId);
                  if (schema.getGroupedSlots().size() > 9) {
                     info.setReturnValue(null);
                     return;
                  }
               }

               info.setReturnValue(new UIGuiSlotKeyframeFactory(keyframe, editor));
            } else {
               if ("gui_opacity".equals(id) || "bg_opacity".equals(id)) {
                  info.setReturnValue(floatPanel(keyframe, editor, 0.0F, 1.0F));
               } else if ("selected_slot".equals(id)) {
                  info.setReturnValue(integerPanel(keyframe, editor, 0, 8));
               } else if ("hotbar_heart_type".equals(id) || "heart_type".equals(id)) {
                  info.setReturnValue(integerPanel(keyframe, editor, 0, 4));
               } else if ("hotbar_armor".equals(id) || "hotbar_hunger".equals(id)) {
                  info.setReturnValue(integerPanel(keyframe, editor, 0, 20));
               } else if ("hotbar_air".equals(id)) {
                  info.setReturnValue(integerPanel(keyframe, editor, 0, 300));
               } else if ("hotbar_experience_level".equals(id)) {
                  info.setReturnValue(integerPanel(keyframe, editor, 0, 9999));
               } else if ("hotbar_experience".equals(id)) {
                  info.setReturnValue(doublePanel(keyframe, editor, 0.0, 1.0));
               } else if ("pov_hand_active_hand".equals(id)) {
                  info.setReturnValue(integerPanel(keyframe, editor, 0, 2));
               } else if ("book_page".equals(id)) {
                  info.setReturnValue(integerPanel(keyframe, editor, 0, 99));
               } else if ("horse_variant".equals(id)) {
                  info.setReturnValue(integerPanel(keyframe, editor, 0, 34));
               } else if ("beacon_primary".equals(id)) {
                  info.setReturnValue(integerPanel(keyframe, editor, 0, 5));
               } else if ("beacon_level".equals(id)) {
                  info.setReturnValue(integerPanel(keyframe, editor, 0, 4));
               } else if ("beacon_secondary".equals(id)) {
                  info.setReturnValue(integerPanel(keyframe, editor, 0, 2));
               } else if (!"pov_hand_right_swing_progress".equals(id)
                  && !"right_swing_progress".equals(id)
                  && !"pov_hand_left_swing_progress".equals(id)
                  && !"left_swing_progress".equals(id)
                  && !"pov_hand_main_equip".equals(id)
                  && !"pov_hand_off_equip".equals(id)) {
                  if (!"bossbar_color".equals(id) && (!"color".equals(id) || !(keyframe.getValue() instanceof String))) {
                     if ("bossbar_style".equals(id) || "style".equals(id) && keyframe.getValue() instanceof String) {
                        info.setReturnValue(new UIBossBarLookKeyframeFactory(keyframe, editor, UIBossBarLookKeyframeFactory.Mode.STYLE));
                     }
                  } else {
                     info.setReturnValue(new UIBossBarLookKeyframeFactory(keyframe, editor, UIBossBarLookKeyframeFactory.Mode.COLOR));
                  }
               } else {
                  info.setReturnValue(floatPanel(keyframe, editor, 0.0F, 1.0F));
               }
            }
         }
      }
   }

   private static UIKeyframeFactory<Integer> integerPanel(Keyframe keyframe, UIKeyframes editor, int minimum, int maximum) {
      return new UIConstrainedNumericKeyframeFactory<>(keyframe, editor, (double)minimum, (double)maximum, true, value -> (int)Math.round(value));
   }

   private static UIKeyframeFactory<Double> doublePanel(Keyframe keyframe, UIKeyframes editor, double minimum, double maximum) {
      return new UIConstrainedNumericKeyframeFactory<>(keyframe, editor, minimum, maximum, false, value -> value);
   }

   private static UIKeyframeFactory<Float> floatPanel(Keyframe keyframe, UIKeyframes editor, float minimum, float maximum) {
      return new UIConstrainedNumericKeyframeFactory<>(keyframe, editor, (double)minimum, (double)maximum, false, value -> (float)value);
   }
}

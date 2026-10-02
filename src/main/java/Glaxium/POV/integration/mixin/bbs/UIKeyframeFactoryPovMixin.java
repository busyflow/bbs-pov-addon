package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.actions.bossbar.editor.UIBossBarLookKeyframeFactory;
import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.hud.editor.UIHotbarTransformKeyframeFactory;
import Glaxium.POV.hud.editor.UIConstrainedNumericKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UITrackValue;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory;
import mchorse.bbs_mod.utils.pose.Transform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Use the 2D form of BBS's native Transform panel only for POV Layout. */
@Mixin(value = UIKeyframeFactory.class, remap = false)
public abstract class UIKeyframeFactoryPovMixin
{
    @Inject(method = "createPanel", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void bbsPov$createLayoutPanel(
        UITrackValue track,
        UIKeyframes editor,
        CallbackInfoReturnable<UIKeyframeFactory> info)
    {
        if (track == null)
        {
            return;
        }

        UIKeyframeSheet sheet = track.sheet;
        String id = sheet != null && sheet.id != null ? sheet.id : "";
        Object value = track.getValue();

        if (value instanceof Transform)
        {
            boolean isLayout = (id != null && (id.contains("layout") || id.contains("cursor")))
                || (sheet != null && sheet.title != null && sheet.title.get() != null && sheet.title.get().toLowerCase().contains("layout"));

            if (isLayout)
            {
                info.setReturnValue(new UIHotbarTransformKeyframeFactory(
                    (UITrackValue<Transform>) track,
                    editor));

                return;
            }
        }

        if (value instanceof net.minecraft.item.ItemStack)
        {
            info.setReturnValue(new Glaxium.POV.hud.editor.UIHotbarItemKeyframeFactory(
                (UITrackValue<net.minecraft.item.ItemStack>) track,
                editor));

            return;
        }

        if (value instanceof String str)
        {
            if ("bossbar_color".equals(id) || "color".equals(id))
            {
                info.setReturnValue(new UIBossBarLookKeyframeFactory(
                    (UITrackValue<String>) track,
                    editor,
                    UIBossBarLookKeyframeFactory.Mode.COLOR));
                return;
            }
            if ("bossbar_style".equals(id) || "style".equals(id))
            {
                info.setReturnValue(new UIBossBarLookKeyframeFactory(
                    (UITrackValue<String>) track,
                    editor,
                    UIBossBarLookKeyframeFactory.Mode.STYLE));
                return;
            }
            if ("executed_text".equals(id) || "execution_text".equals(id))
            {
                info.setReturnValue(new Glaxium.POV.actions.chat.editor.UIExecutedTextKeyframeFactory(
                    (UITrackValue<String>) track,
                    editor));
                return;
            }
            if ("chat_text".equals(id))
            {
                info.setReturnValue(new Glaxium.POV.actions.chat.editor.UIChatTextKeyframeFactory(
                    (UITrackValue<String>) track,
                    editor));
                return;
            }
        }

        if (id == null || id.isEmpty())
        {
            return;
        }

        if ("crafting_grid".equals(id) || "inventory_slots".equals(id) || "hotbar_inventory_slots".equals(id))
        {
            info.setReturnValue(new Glaxium.POV.actions.gui.editor.UIGuiSlotKeyframeFactory(
                (UITrackValue<Boolean>) track,
                editor));
            return;
        }

        if ("gui_slots".equals(id))
        {
            mchorse.bbs_mod.ui.film.UIFilmPanel filmPanel = Glaxium.POV.replay.PovReplaySettings.getFilmPanel();
            GuiPovActionClip guiClip = null;
            if (sheet != null && sheet.channel != null && sheet.channel.getParent() instanceof GuiPovActionClip clip)
            {
                guiClip = clip;
            }
            else if (filmPanel != null && filmPanel.cameraEditor != null && filmPanel.cameraEditor.getClip() instanceof GuiPovActionClip clip)
            {
                guiClip = clip;
            }

            if (guiClip != null)
            {
                String guiId = guiClip.state.isEmpty() ? "inventory" : guiClip.state.get(0).getValue();
                Glaxium.POV.actions.gui.GuiSlotSchema schema = Glaxium.POV.actions.gui.GuiSlotSchema.get(guiId);
                if (schema.getGroupedSlots().size() > 9)
                {
                    info.setReturnValue(null);
                    return;
                }
            }

            info.setReturnValue(new Glaxium.POV.actions.gui.editor.UIGuiSlotKeyframeFactory(
                (UITrackValue<Boolean>) track,
                editor));
            return;
        }

        if ("gui_opacity".equals(id) || "bg_opacity".equals(id))
        {
            info.setReturnValue(floatPanel(track, editor, 0F, 1F));
        }
        else if ("selected_slot".equals(id))
        {
            info.setReturnValue(integerPanel(track, editor, 0, 8));
        }
        else if ("hotbar_heart_type".equals(id) || "heart_type".equals(id))
        {
            info.setReturnValue(integerPanel(track, editor, 0, 4));
        }
        else if ("hotbar_armor".equals(id) || "hotbar_hunger".equals(id))
        {
            info.setReturnValue(integerPanel(track, editor, 0, 20));
        }
        else if ("hotbar_air".equals(id))
        {
            info.setReturnValue(integerPanel(track, editor, 0, 300));
        }
        else if ("hotbar_experience_level".equals(id))
        {
            info.setReturnValue(integerPanel(track, editor, 0, 9999));
        }
        else if ("hotbar_experience".equals(id))
        {
            info.setReturnValue(doublePanel(track, editor, 0D, 1D));
        }
        else if ("pov_hand_active_hand".equals(id))
        {
            info.setReturnValue(integerPanel(track, editor, 0, 2));
        }
        else if ("book_page".equals(id))
        {
            info.setReturnValue(integerPanel(track, editor, 0, 99));
        }
        else if ("horse_variant".equals(id))
        {
            info.setReturnValue(integerPanel(
                track,
                editor,
                0,
                Glaxium.POV.actions.clip.GuiPovActionClip.HORSE_VARIANT_COUNT - 1));
        }
        else if ("beacon_primary".equals(id))
        {
            info.setReturnValue(integerPanel(track, editor, 0, 5));
        }
        else if ("beacon_level".equals(id))
        {
            info.setReturnValue(integerPanel(track, editor, 0, 4));
        }
        else if ("beacon_secondary".equals(id))
        {
            info.setReturnValue(integerPanel(track, editor, 0, 2));
        }
        else if ("pov_hand_right_swing_progress".equals(id)
            || "right_swing_progress".equals(id)
            || "pov_hand_left_swing_progress".equals(id)
            || "left_swing_progress".equals(id)
            || "pov_hand_main_equip".equals(id)
            || "pov_hand_off_equip".equals(id))
        {
            info.setReturnValue(floatPanel(track, editor, 0F, 1F));
        }
        else if ("bossbar_color".equals(id) || ("color".equals(id) && value instanceof String))
        {
            info.setReturnValue(new UIBossBarLookKeyframeFactory(
                (UITrackValue<String>) track,
                editor,
                UIBossBarLookKeyframeFactory.Mode.COLOR));
        }
        else if ("bossbar_style".equals(id) || ("style".equals(id) && value instanceof String))
        {
            info.setReturnValue(new UIBossBarLookKeyframeFactory(
                (UITrackValue<String>) track,
                editor,
                UIBossBarLookKeyframeFactory.Mode.STYLE));
        }
    }

    @SuppressWarnings("unchecked")
    private static UIKeyframeFactory<Integer> integerPanel(
        UITrackValue track,
        UIKeyframes editor,
        int minimum,
        int maximum)
    {
        return new UIConstrainedNumericKeyframeFactory<Integer>(
            (UITrackValue<Integer>) track,
            editor,
            minimum,
            maximum,
            true,
            value -> (int) Math.round(value));
    }

    @SuppressWarnings("unchecked")
    private static UIKeyframeFactory<Double> doublePanel(
        UITrackValue track,
        UIKeyframes editor,
        double minimum,
        double maximum)
    {
        return new UIConstrainedNumericKeyframeFactory<Double>(
            (UITrackValue<Double>) track,
            editor,
            minimum,
            maximum,
            false,
            value -> value);
    }

    @SuppressWarnings("unchecked")
    private static UIKeyframeFactory<Float> floatPanel(
        UITrackValue track,
        UIKeyframes editor,
        float minimum,
        float maximum)
    {
        return new UIConstrainedNumericKeyframeFactory<Float>(
            (UITrackValue<Float>) track,
            editor,
            minimum,
            maximum,
            false,
            value -> (float) value);
    }
}

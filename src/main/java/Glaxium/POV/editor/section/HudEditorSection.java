package Glaxium.POV.editor.section;

import Glaxium.POV.editor.UIPovEditor;
import Glaxium.POV.hud.RecordedHudData;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.pose.Transform;

/** HUD (Hotbar) keyframe sheets. */
public final class HudEditorSection implements PovEditorSection
{
    public static final HudEditorSection INSTANCE = new HudEditorSection();

    private HudEditorSection()
    {
    }

    @Override
    public void fillSheets(UIPovEditor editor, boolean resetView)
    {
        Replay replay = editor.getReplay();
        if (replay == null || !(replay.keyframes instanceof ReplayKeyframesPovAccess access))
        {
            return;
        }

        RecordedHudData hud = access.bbsPov$getHud();
        if (hud == null)
        {
            return;
        }

        hud.ensureNativeSlotDefaults(replay.keyframes);

        int color = 0;
        UIKeyframeSheet layoutSheet = editor.createSheet(
            "Layout",
            hud.layout,
            Icons.LAYOUT,
            color++,
            null,
            () -> hud.layout.interpolate(
                editor.getFilmPanel().getCursor(),
                new Transform()).copy());

        UIKeyframeSheet slotsSheet = editor.createSheet(
            "Slots",
            hud.slotsLayout,
            Icons.HOTBAR,
            color++,
            null,
            () -> hud.slotsLayout.interpolate(
                editor.getFilmPanel().getCursor(),
                new Transform()).copy());
        slotsSheet.setParent(layoutSheet);

        UIKeyframeSheet heartsSheet = editor.createSheet(
            "Hearts",
            hud.heartsLayout,
            Icons.HEART,
            color++,
            null,
            () -> hud.heartsLayout.interpolate(
                editor.getFilmPanel().getCursor(),
                new Transform()).copy());
        heartsSheet.setParent(layoutSheet);

        UIKeyframeSheet foodSheet = editor.createSheet(
            "Food",
            hud.foodLayout,
            Icons.CROPS,
            color++,
            null,
            () -> hud.foodLayout.interpolate(
                editor.getFilmPanel().getCursor(),
                new Transform()).copy());
        foodSheet.setParent(layoutSheet);

        UIKeyframeSheet expSheet = editor.createSheet(
            "XP Bar",
            hud.expLayout,
            Icons.SHARD,
            color++,
            null,
            () -> hud.expLayout.interpolate(
                editor.getFilmPanel().getCursor(),
                new Transform()).copy());
        expSheet.setParent(layoutSheet);

        color = editor.addSheet("Visible", hud.visible, Icons.VISIBLE, color,
            () -> hud.visible.interpolate(editor.getReplayTick(), true));
        color = editor.addSheet("Status Bars", hud.statusBarsVisible, Icons.HEART, color,
            () -> hud.statusBarsVisible.interpolate(editor.getReplayTick(), true));
        color = editor.addSheet("Crosshair", hud.crosshair, Icons.POINTER, color,
            () -> hud.crosshair.interpolate(editor.getReplayTick(), true));
        color = editor.addSheet("Cursor Layout", hud.cursorLayout, Icons.POINTER, color,
            () -> hud.cursorLayout.interpolate(editor.getFilmPanel().getCursor(), new Transform()).copy());
        color = editor.addSheet("Cursor Visible", hud.cursorVisible, Icons.LOOKING, color,
            () -> false);
        color = editor.addSheet("Cursor Item", hud.cursorItem, Icons.BLOCK, color);
        color = editor.addSheet("Selected Slot", replay.keyframes.selectedSlot, Icons.POINTER, color);

        for (int i = 0; i < replay.keyframes.hotbar.size(); i++)
        {
            color = editor.addSheet("Slot " + (i + 1), replay.keyframes.hotbar.get(i), Icons.HOTBAR, color);
        }

        color = editor.addSheet("Inventory Slots", hud.inventoryAnchor, Icons.KEY_CAP, color, () -> true);
        color = editor.addSheet("Offhand", replay.keyframes.offHand, Icons.LIMB, color);
        color = editor.addSheet("Health", hud.health, Icons.HEART, color);
        color = editor.addSheet("Previous Health", hud.previousHealth, Icons.HEART, color);
        color = editor.addSheet("Health Flash", hud.heartFlash, Icons.HEART_ALT, color);
        color = editor.addSheet("Health Container", hud.healthContainer, Icons.HEART_ALT, color);
        color = editor.addSheet("Absorption", hud.absorption, Icons.HEART, color);
        color = editor.addSheet("Absorption Container", hud.absorptionContainer, Icons.HEART_ALT, color);
        color = editor.addSheet("Heart Type", hud.heartType, Icons.HEART, color);
        color = editor.addSheet("Hardcore", hud.hardcore, Icons.SKULL, color);
        color = editor.addSheet("Heart Regeneration", hud.regeneration, Icons.HEART, color);
        color = editor.addSheet("Armor", hud.armor, Icons.ARMOR_CHESTPLATE, color);
        color = editor.addSheet("Hunger", hud.hunger, Icons.CROPS, color);
        color = editor.addSheet("Hunger Effect", hud.hungerEffect, Icons.CROPS, color);
        color = editor.addSheet("Mount Health", hud.mountHealth, Icons.HEART, color);
        color = editor.addSheet("Mount Health Container", hud.mountHealthContainer, Icons.HEART_ALT, color);
        color = editor.addSheet("Air", hud.air, Icons.BUBBLE, color);
        color = editor.addSheet("Experience", hud.experience, Icons.SHARD, color);
        color = editor.addSheet("Experience Level", hud.experienceLevel, Icons.SHARD, color);
        editor.addSheet("Golden Heart Flash", hud.absorptionFlash, Icons.HEART_ALT, color);
    }
}

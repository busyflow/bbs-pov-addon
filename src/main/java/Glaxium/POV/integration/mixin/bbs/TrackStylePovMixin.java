package Glaxium.POV.integration.mixin.bbs;

import mchorse.bbs_mod.film.replays.tracks.TrackStyle;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

@Mixin(value = TrackStyle.class, remap = false)
public class TrackStylePovMixin
{
    private static final Map<String, Icon> POV_ICONS = new HashMap<>();

    static
    {
        /* Hand tab channels */
        POV_ICONS.put("visible", Icons.VISIBLE);
        POV_ICONS.put("model", Icons.MORPH);
        POV_ICONS.put("texture", Icons.IMAGE);
        POV_ICONS.put("color", Icons.COLOR);
        POV_ICONS.put("color overlay", Icons.COLOR);
        POV_ICONS.put("color_overlay", Icons.COLOR);
        POV_ICONS.put("camera offset", Icons.LAYOUT);
        POV_ICONS.put("camera_offset", Icons.LAYOUT);
        POV_ICONS.put("pose", Icons.POSE);
        POV_ICONS.put("item pose", Icons.BLOCK);
        POV_ICONS.put("item_pose", Icons.BLOCK);
        POV_ICONS.put("right hand visible", Icons.VISIBLE);
        POV_ICONS.put("right_hand_visible", Icons.VISIBLE);
        POV_ICONS.put("left hand visible", Icons.VISIBLE);
        POV_ICONS.put("left_hand_visible", Icons.VISIBLE);
        POV_ICONS.put("off hand item", Icons.BLOCK);
        POV_ICONS.put("offhand", Icons.LIMB);
        POV_ICONS.put("off_hand", Icons.LIMB);
        POV_ICONS.put("right swing", Icons.MAIN_HANDLE);
        POV_ICONS.put("right_swing", Icons.MAIN_HANDLE);
        POV_ICONS.put("right_swing_progress", Icons.MAIN_HANDLE);
        POV_ICONS.put("left swing", Icons.LEFT_HANDLE);
        POV_ICONS.put("left_swing", Icons.LEFT_HANDLE);
        POV_ICONS.put("left_swing_progress", Icons.LEFT_HANDLE);
        POV_ICONS.put("main equip", Icons.MAIN_HANDLE);
        POV_ICONS.put("main_equip", Icons.MAIN_HANDLE);
        POV_ICONS.put("offhand equip", Icons.LEFT_HANDLE);
        POV_ICONS.put("off_equip", Icons.LEFT_HANDLE);
        POV_ICONS.put("active use hand", Icons.POINTER);
        POV_ICONS.put("active_hand", Icons.POINTER);
        POV_ICONS.put("active use item", Icons.BLOCK);
        POV_ICONS.put("active_item", Icons.BLOCK);
        POV_ICONS.put("use time", Icons.TIME);
        POV_ICONS.put("use_time", Icons.TIME);
        POV_ICONS.put("bob phase", Icons.CURVES);
        POV_ICONS.put("bob_phase", Icons.CURVES);
        POV_ICONS.put("bob strength", Icons.CURVES);
        POV_ICONS.put("bob_strength", Icons.CURVES);
        POV_ICONS.put("render yaw", Icons.SPHERE);
        POV_ICONS.put("render_yaw", Icons.SPHERE);
        POV_ICONS.put("render pitch", Icons.SPHERE);
        POV_ICONS.put("render_pitch", Icons.SPHERE);
        POV_ICONS.put("left-handed main arm", Icons.LIMB);
        POV_ICONS.put("main_arm", Icons.LIMB);

        /* HUD / Hotbar channels */
        POV_ICONS.put("layout", Icons.LAYOUT);
        POV_ICONS.put("slots", Icons.HOTBAR);
        POV_ICONS.put("slots layout", Icons.HOTBAR);
        POV_ICONS.put("slots_layout", Icons.HOTBAR);
        POV_ICONS.put("hearts", Icons.HEART);
        POV_ICONS.put("hearts layout", Icons.HEART);
        POV_ICONS.put("hearts_layout", Icons.HEART);
        POV_ICONS.put("food", Icons.CROPS);
        POV_ICONS.put("food layout", Icons.CROPS);
        POV_ICONS.put("food_layout", Icons.CROPS);
        POV_ICONS.put("xp bar", Icons.SHARD);
        POV_ICONS.put("exp layout", Icons.SHARD);
        POV_ICONS.put("exp_layout", Icons.SHARD);
        POV_ICONS.put("status bars", Icons.HEART);
        POV_ICONS.put("status_bars_visible", Icons.HEART);
        POV_ICONS.put("crosshair", Icons.POINTER);
        POV_ICONS.put("cursor layout", Icons.POINTER);
        POV_ICONS.put("cursor_layout", Icons.POINTER);
        POV_ICONS.put("cursor visible", Icons.LOOKING);
        POV_ICONS.put("cursor_visible", Icons.LOOKING);
        POV_ICONS.put("cursor item", Icons.BLOCK);
        POV_ICONS.put("cursor_item", Icons.BLOCK);
        POV_ICONS.put("selected slot", Icons.POINTER);
        POV_ICONS.put("selected_slot", Icons.POINTER);
        for (int i = 1; i <= 9; i++)
        {
            POV_ICONS.put("slot " + i, Icons.HOTBAR);
        }
        POV_ICONS.put("inventory slots", Icons.KEY_CAP);
        POV_ICONS.put("inventory_slots", Icons.KEY_CAP);
        POV_ICONS.put("health", Icons.HEART);
        POV_ICONS.put("previous health", Icons.HEART);
        POV_ICONS.put("previous_health", Icons.HEART);
        POV_ICONS.put("health flash", Icons.HEART_ALT);
        POV_ICONS.put("heart_flash", Icons.HEART_ALT);
        POV_ICONS.put("health container", Icons.HEART_ALT);
        POV_ICONS.put("health_container", Icons.HEART_ALT);
        POV_ICONS.put("absorption", Icons.HEART);
        POV_ICONS.put("absorption container", Icons.HEART_ALT);
        POV_ICONS.put("absorption_container", Icons.HEART_ALT);
        POV_ICONS.put("golden heart flash", Icons.HEART_ALT);
        POV_ICONS.put("absorption_flash", Icons.HEART_ALT);
        POV_ICONS.put("heart type", Icons.HEART);
        POV_ICONS.put("heart_type", Icons.HEART);
        POV_ICONS.put("hardcore", Icons.SKULL);
        POV_ICONS.put("heart regeneration", Icons.HEART);
        POV_ICONS.put("regeneration", Icons.HEART);
        POV_ICONS.put("armor", Icons.ARMOR_CHESTPLATE);
        POV_ICONS.put("hunger", Icons.CROPS);
        POV_ICONS.put("hunger effect", Icons.CROPS);
        POV_ICONS.put("hunger_effect", Icons.CROPS);
        POV_ICONS.put("mount health", Icons.HEART);
        POV_ICONS.put("mount_health", Icons.HEART);
        POV_ICONS.put("mount health container", Icons.HEART_ALT);
        POV_ICONS.put("mount_health_container", Icons.HEART_ALT);
        POV_ICONS.put("air", Icons.BUBBLE);
        POV_ICONS.put("experience", Icons.SHARD);
        POV_ICONS.put("experience level", Icons.SHARD);
        POV_ICONS.put("experience_level", Icons.SHARD);
    }

    @Inject(method = "icon", at = @At("HEAD"), cancellable = true)
    private static void bbsPov$getIcon(String key, CallbackInfoReturnable<Icon> info)
    {
        if (key == null)
        {
            return;
        }

        String lower = key.trim().toLowerCase();
        if (lower.startsWith("pose_overlay") || lower.startsWith("pose overlay"))
        {
            info.setReturnValue(Icons.POSE);
            return;
        }

        if (POV_ICONS.containsKey(lower))
        {
            info.setReturnValue(POV_ICONS.get(lower));
        }
    }
}

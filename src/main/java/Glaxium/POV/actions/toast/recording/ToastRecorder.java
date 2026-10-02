package Glaxium.POV.actions.toast.recording;

import Glaxium.POV.actions.PovActionType;
import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.actions.clip.ToastPovActionClip;
import Glaxium.POV.actions.toast.ToastPresets;
import Glaxium.POV.actions.toast.ToastTypeEntry;
import Glaxium.POV.config.PovSettings;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.integration.mixin.minecraft.AdvancementToastPovAccessor;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.client.toast.AdvancementToast;
import net.minecraft.client.toast.RecipeToast;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.TutorialToast;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

/** Captures live Minecraft toasts during replay recording. */
public final class ToastRecorder
{
    private ToastRecorder()
    {
    }

    public static void finish(ReplayKeyframesPovAccess access, int endTick)
    {
        if (access == null || access.bbsPov$getActions() == null)
        {
            return;
        }

        for (Clip clip : access.bbsPov$getActions().get())
        {
            if (clip instanceof ToastPovActionClip toastClip)
            {
                toastClip.trimToRecording(endTick);
            }
        }
    }

    public static void onToastAdded(Toast toast, RecordedPovActions actions, int tick)
    {
        if (actions == null || toast == null || !PovSettings.isBakeToasts())
        {
            return;
        }

        // Allocate to the first available layer out of the 5 dedicated toast layers (seedLayer..seedLayer+4)
        int baseLayer = PovActionType.TOASTS.seedLayer();
        boolean[] occupied = new boolean[5];
        for (Clip c : actions.get())
        {
            if (c instanceof ToastPovActionClip tc)
            {
                int start = tc.tick.get();
                int end = start + tc.duration.get();
                if (tick >= start && tick < end)
                {
                    int offset = tc.layer.get() - baseLayer;
                    if (offset >= 0 && offset < 5)
                    {
                        occupied[offset] = true;
                    }
                }
            }
        }

        int targetLayer = baseLayer;
        for (int i = 0; i < 5; i++)
        {
            if (!occupied[i])
            {
                targetLayer = baseLayer + i;
                break;
            }
        }

        ToastPovActionClip clip = (ToastPovActionClip) actions.add(PovActionType.TOASTS, tick, 100);
        clip.layer.set(targetLayer);

        if (toast instanceof AdvancementToast advancementToast)
        {
            try
            {
                net.minecraft.advancement.Advancement entry = null;
                if (advancementToast instanceof AdvancementToastPovAccessor accessor)
                {
                    entry = accessor.bbsPov$getAdvancement();
                }

                if (entry == null)
                {
                    for (java.lang.reflect.Field f : AdvancementToast.class.getDeclaredFields())
                    {
                        f.setAccessible(true);
                        Object obj = f.get(advancementToast);
                        if (obj instanceof net.minecraft.advancement.Advancement advEntry)
                        {
                            entry = advEntry;
                            break;
                        }
                    }
                }

                if (entry != null && entry.getDisplay() != null)
                {
                    var d = entry.getDisplay();
                    String titleText = d.getFrame().getToastText().getString();
                    String descText = d.getTitle().getString();

                    clip.setCustomTitle(titleText);
                    clip.setCustomDescription(descText);
                    clip.title.set(descText);

                    ItemStack iconStack = d.getIcon();
                    if (iconStack != null && !iconStack.isEmpty())
                    {
                        clip.setCustomIcon(Registries.ITEM.getId(iconStack.getItem()).toString());
                    }
                    clip.setFrameType(d.getFrame().getId());

                    ToastTypeEntry match = ToastPresets.findByTitleOrDesc(descText);
                    if (match != null)
                    {
                        clip.setPresetId(match.id);
                    }
                }
            }
            catch (Exception ignored)
            {}
        }
        else if (toast instanceof RecipeToast recipeToast)
        {
            clip.setPresetId("rec_crafting_table");
            clip.setFrameType("recipe");
            clip.setCustomTitle("New Recipes Unlocked!");
            clip.setCustomDescription("Check your recipe book");
            clip.title.set("Recipe Unlocked");

            try
            {
                for (java.lang.reflect.Field f : RecipeToast.class.getDeclaredFields())
                {
                    f.setAccessible(true);
                    Object val = f.get(recipeToast);
                    if (val instanceof java.util.List<?> list && !list.isEmpty())
                    {
                        Object first = list.get(0);
                        if (first instanceof ItemStack is && !is.isEmpty())
                        {
                            clip.setCustomIcon(Registries.ITEM.getId(is.getItem()).toString());
                            break;
                        }
                        else if (first instanceof net.minecraft.recipe.Recipe<?> re)
                        {
                            var world = net.minecraft.client.MinecraftClient.getInstance().world;
                            if (world != null)
                            {
                                ItemStack is = re.getOutput(world.getRegistryManager());
                                if (is != null && !is.isEmpty())
                                {
                                    clip.setCustomIcon(Registries.ITEM.getId(is.getItem()).toString());
                                    break;
                                }
                            }
                        }
                    }
                    else if (val instanceof ItemStack is && !is.isEmpty())
                    {
                        clip.setCustomIcon(Registries.ITEM.getId(is.getItem()).toString());
                        break;
                    }
                }
            }
            catch (Exception ignored)
            {}
        }
        else if (toast instanceof TutorialToast)
        {
            clip.setPresetId("tut_movement");
            clip.setFrameType("tutorial");
            clip.title.set("Tutorial");
        }
        else if (toast instanceof SystemToast)
        {
            clip.setPresetId("sys_screenshot");
            clip.setFrameType("system");
            clip.title.set("System Toast");
        }
    }
}

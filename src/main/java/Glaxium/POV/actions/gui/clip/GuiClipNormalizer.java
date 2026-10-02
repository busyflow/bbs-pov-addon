package Glaxium.POV.actions.gui.clip;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.clip.PovActionClip;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import net.minecraft.item.ItemStack;

import java.util.Map;

/** Applies GUI-clip keyframe interpolation defaults after the base clip normalize. */
public final class GuiClipNormalizer
{
    private GuiClipNormalizer()
    {
    }

    public static void apply(GuiPovActionClip clip)
    {
        PovActionClip.constant(clip.state);
        PovActionClip.constant(clip.mouseButtons);
        PovActionClip.constant(clip.mouseScroll);
        PovActionClip.constant(clip.cursorVisible);
        PovActionClip.constant(clip.creativeTab);
        PovActionClip.constant(clip.creativePage);
        PovActionClip.constant(clip.creativeSearch);
        PovActionClip.constant(clip.creativeSearchFocus);
        PovActionClip.constant(clip.loomRow);
        PovActionClip.constant(clip.stonecutterRow);
        PovActionClip.constant(clip.creativeSearchSelStart);
        PovActionClip.constant(clip.creativeSearchSelEnd);
        PovActionClip.constant(clip.recipeSearchSelStart);
        PovActionClip.constant(clip.recipeSearchSelEnd);
        PovActionClip.constant(clip.recipeOpen);
        PovActionClip.constant(clip.recipeSearch);
        PovActionClip.constant(clip.recipeSearchFocus);
        PovActionClip.constant(clip.recipeShowing);
        PovActionClip.constant(clip.recipeCategory);
        PovActionClip.constant(clip.recipeSelected);
        PovActionClip.constant(clip.recipePage);
        PovActionClip.constant(clip.recipeButton);
        PovActionClip.constant(clip.anvilName);
        PovActionClip.constant(clip.anvilNameFocus);
        PovActionClip.constant(clip.anvilNameSelStart);
        PovActionClip.constant(clip.anvilNameSelEnd);
        PovActionClip.constant(clip.anvilError);
        PovActionClip.constant(clip.enchantOffers);
        PovActionClip.constant(clip.enchantSeed);
        PovActionClip.constant(clip.enchantPlayerLevel);
        PovActionClip.constant(clip.enchantCreative);
        PovActionClip.constant(clip.gamemodeSelection);
        PovActionClip.clamp(clip.gamemodeSelection, 0, 3);
        PovActionClip.constant(clip.beaconPrimary);
        PovActionClip.constant(clip.beaconSecondary);
        PovActionClip.constant(clip.beaconLevel);
        PovActionClip.clamp(clip.beaconPrimary, 0, 5);
        PovActionClip.clamp(clip.beaconSecondary, 0, 2);
        PovActionClip.clamp(clip.beaconLevel, 0, 4);
        PovActionClip.clamp(clip.creativeScroll, 0F, 1F);
        PovActionClip.clamp(clip.opacity, 0F, 1F);
        PovActionClip.clamp(clip.darknessOpacity, 0F, 1F);

        for (KeyframeChannel<ItemStack> ch : clip.namedSlots.values())
        {
            PovActionClip.constant(ch);
        }

        for (Map<String, KeyframeChannel<ItemStack>> slots : clip.guiSlots.values())
        {
            for (KeyframeChannel<ItemStack> channel : slots.values())
            {
                PovActionClip.constant(channel);
            }
        }
        for (String guiId : clip.guiLayouts.keySet())
        {
            PovActionClip.constant(clip.guiCursorVisibilities.get(guiId));
            PovActionClip.constant(clip.guiMouseButtons.get(guiId));
            PovActionClip.constant(clip.guiMouseScrolls.get(guiId));
            PovActionClip.constant(clip.guiDragSlots.get(guiId));
            PovActionClip.constant(clip.guiCursorItems.get(guiId));
            PovActionClip.constant(clip.guiPrimarySlotAnchors.get(guiId));
            PovActionClip.constant(clip.guiCraftingSlotAnchors.get(guiId));
            PovActionClip.constant(clip.guiRecipeOpens.get(guiId));
            PovActionClip.constant(clip.guiRecipeSearches.get(guiId));
            PovActionClip.constant(clip.guiRecipeSearchFocuses.get(guiId));
            PovActionClip.constant(clip.guiRecipeShowings.get(guiId));
            PovActionClip.constant(clip.guiRecipeCategories.get(guiId));
            PovActionClip.constant(clip.guiRecipeSelecteds.get(guiId));
            PovActionClip.constant(clip.guiRecipePages.get(guiId));
            PovActionClip.constant(clip.guiRecipeButtons.get(guiId));
            PovActionClip.constant(clip.guiRecipeSearchSelStarts.get(guiId));
            PovActionClip.constant(clip.guiRecipeSearchSelEnds.get(guiId));
            if (clip.guiFurnaceLit.get(guiId) != null)
            {
                PovActionClip.clamp(clip.guiFurnaceLit.get(guiId), 0F, 1F);
                PovActionClip.clamp(clip.guiFurnaceCook.get(guiId), 0F, 1F);
            }
            if (clip.guiBrewProgress.get(guiId) != null)
            {
                PovActionClip.clamp(clip.guiBrewProgress.get(guiId), 0F, 1F);
                PovActionClip.clamp(clip.guiBrewFuel.get(guiId), 0F, 1F);
                PovActionClip.constant(clip.guiBrewBubbles.get(guiId));
            }
            if (clip.guiHorseVariants.get(guiId) != null)
            {
                PovActionClip.constant(clip.guiHorseVariants.get(guiId));
            }
            if (clip.guiMountChests.get(guiId) != null)
            {
                PovActionClip.constant(clip.guiMountChests.get(guiId));
            }
            if (clip.guiMerchantSelectedOffers.get(guiId) != null)
            {
                PovActionClip.constant(clip.guiMerchantOffers.get(guiId));
                PovActionClip.constant(clip.guiMerchantProfessions.get(guiId));
                PovActionClip.constant(clip.guiMerchantLevels.get(guiId));
                PovActionClip.constant(clip.guiMerchantExperiences.get(guiId));
                PovActionClip.constant(clip.guiMerchantSelectedOffers.get(guiId));
                PovActionClip.constant(clip.guiMerchantScrollOffsets.get(guiId));
                PovActionClip.constant(clip.guiMerchantTitles.get(guiId));
                PovActionClip.constant(clip.guiMerchantCanLevels.get(guiId));
            }
            if (clip.guiBookWritables.get(guiId) != null)
            {
                PovActionClip.constant(clip.guiBookWritables.get(guiId));
                PovActionClip.constant(clip.guiBookSignings.get(guiId));
                PovActionClip.constant(clip.guiBookPagesIndex.get(guiId));
                PovActionClip.constant(clip.guiBookPages.get(guiId));
                PovActionClip.constant(clip.guiBookTitles.get(guiId));
                PovActionClip.constant(clip.guiBookAuthors.get(guiId));
                PovActionClip.constant(clip.guiBookSelStarts.get(guiId));
                PovActionClip.constant(clip.guiBookSelEnds.get(guiId));
            }
            PovActionClip.clamp(clip.guiOpacities.get(guiId), 0F, 1F);
            PovActionClip.clamp(clip.guiDarknessOpacities.get(guiId), 0F, 1F);
        }
    }
}

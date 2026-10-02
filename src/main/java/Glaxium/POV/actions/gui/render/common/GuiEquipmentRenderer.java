package Glaxium.POV.actions.gui.render.common;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SmithingTemplateItem;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;

import java.util.List;

/** Empty equipment, smithing, loom, enchanting, and mount slot placeholders. */
public final class GuiEquipmentRenderer
{
    private static final Identifier HORSE_TEXTURE = new Identifier("textures/gui/container/horse.png");
    private static final Identifier LOOM_TEXTURE = new Identifier("textures/gui/container/loom.png");
    private static final List<Identifier> SMITHING_TEMPLATE_PLACEHOLDERS = List.of(
        new Identifier("item/empty_slot_smithing_template_armor_trim"),
        new Identifier("item/empty_slot_smithing_template_netherite_upgrade"));

    private GuiEquipmentRenderer()
    {
    }

    public static void renderEmptyEquipmentSlots(
        Batcher2D batcher,
        GuiPovActionClip clip,
        String guiId,
        float tick)
    {
        String[] slotIds = {"armor_head", "armor_chest", "armor_legs", "armor_feet", "offhand"};
        Identifier[] sprites = {
            PlayerScreenHandler.EMPTY_HELMET_SLOT_TEXTURE,
            PlayerScreenHandler.EMPTY_CHESTPLATE_SLOT_TEXTURE,
            PlayerScreenHandler.EMPTY_LEGGINGS_SLOT_TEXTURE,
            PlayerScreenHandler.EMPTY_BOOTS_SLOT_TEXTURE,
            PlayerScreenHandler.EMPTY_OFFHAND_ARMOR_SLOT};
        int[][] positions = "creative_inventory".equals(guiId)
            ? new int[][] {{54, 6}, {54, 33}, {108, 6}, {108, 33}, {35, 20}}
            : new int[][] {{8, 8}, {8, 26}, {8, 44}, {8, 62}, {77, 62}};

        for (int i = 0; i < slotIds.length; i++)
        {
            KeyframeChannel<ItemStack> channel = clip.getGuiSlot(guiId, slotIds[i]);
            ItemStack stack = channel == null || channel.isEmpty()
                ? ItemStack.EMPTY
                : channel.interpolate(tick, ItemStack.EMPTY);

            if (stack == null || stack.isEmpty())
            {
                var sprite = MinecraftClient.getInstance()
                    .getSpriteAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE)
                    .apply(sprites[i]);
                batcher.getContext().drawSprite(
                    positions[i][0],
                    positions[i][1],
                    0,
                    16,
                    16,
                    sprite);
            }
        }
    }

    public static void renderEmptySlotPlaceholders(
        Batcher2D batcher,
        GuiPovActionClip clip,
        String guiId,
        float tick,
        float opacity)
    {
        if ("inventory".equals(guiId))
        {
            renderEmptyEquipmentSlots(batcher, clip, guiId, tick);
        }
        else if ("loom".equals(guiId))
        {
            if (GuiSlotRenderer.isSlotEmpty(clip, "loom", "banner", tick))
            {
                batcher.getContext().drawTexture(LOOM_TEXTURE, 13, 26, 176, 0, 16, 16);
            }
            if (GuiSlotRenderer.isSlotEmpty(clip, "loom", "dye", tick))
            {
                batcher.getContext().drawTexture(LOOM_TEXTURE, 33, 26, 192, 0, 16, 16);
            }
            if (GuiSlotRenderer.isSlotEmpty(clip, "loom", "pattern", tick))
            {
                batcher.getContext().drawTexture(LOOM_TEXTURE, 23, 45, 208, 0, 16, 16);
            }
        }
        else if ("smithing_table".equals(guiId))
        {
            GuiSlotRenderer.drawCyclingSlotPlaceholder(
                batcher,
                clip,
                guiId,
                "template",
                tick,
                SMITHING_TEMPLATE_PLACEHOLDERS,
                8,
                48,
                opacity);

            KeyframeChannel<ItemStack> templateChannel = clip.getGuiSlot(guiId, "template");
            ItemStack templateStack = templateChannel == null || templateChannel.isEmpty()
                ? ItemStack.EMPTY
                : templateChannel.interpolate(tick, ItemStack.EMPTY);

            if (templateStack != null && templateStack.getItem() instanceof SmithingTemplateItem template)
            {
                GuiSlotRenderer.drawCyclingSlotPlaceholder(
                    batcher,
                    clip,
                    guiId,
                    "base",
                    tick,
                    template.getEmptyBaseSlotTextures(),
                    26,
                    48,
                    opacity);
                GuiSlotRenderer.drawCyclingSlotPlaceholder(
                    batcher,
                    clip,
                    guiId,
                    "addition",
                    tick,
                    template.getEmptyAdditionsSlotTextures(),
                    44,
                    48,
                    opacity);
            }
        }
        else if ("enchanting_table".equals(guiId)
            && GuiSlotRenderer.isSlotEmpty(clip, guiId, "lapis", tick))
        {
            GuiSlotRenderer.drawBlockAtlasPlaceholder(
                batcher,
                new Identifier("item/empty_slot_lapis_lazuli"),
                35,
                47,
                opacity);
        }
        else if ("horse".equals(guiId))
        {
            /* 1.20.1 keeps these overlays below the 166px GUI background. */
            batcher.getContext().drawTexture(HORSE_TEXTURE, 7, 17, 18, 220, 18, 18);
            batcher.getContext().drawTexture(HORSE_TEXTURE, 7, 35, 0, 220, 18, 18);
        }
        else if ("donkey".equals(guiId))
        {
            batcher.getContext().drawTexture(HORSE_TEXTURE, 7, 17, 18, 220, 18, 18);
            if (clip.isMountChestOpen(guiId, tick))
            {
                batcher.getContext().drawTexture(HORSE_TEXTURE, 79, 17, 0, 220, 90, 54);
            }
        }
    }
}

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
    private static final Identifier HORSE_SADDLE_SLOT = new Identifier("container/horse/saddle_slot");
    private static final Identifier HORSE_ARMOR_SLOT = new Identifier("container/horse/armor_slot");
    private static final Identifier HORSE_CHEST_SLOTS = new Identifier("container/horse/chest_slots");
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
            GuiSlotRenderer.drawGuiSlotPlaceholder(batcher, clip, guiId, "banner", tick, "container/loom/banner_slot", 13, 26, 16);
            GuiSlotRenderer.drawGuiSlotPlaceholder(batcher, clip, guiId, "dye", tick, "container/loom/dye_slot", 33, 26, 16);
            GuiSlotRenderer.drawGuiSlotPlaceholder(batcher, clip, guiId, "pattern", tick, "container/loom/pattern_slot", 23, 45, 16);
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
            batcher.getContext().drawGuiTexture(HORSE_SADDLE_SLOT, 7, 17, 18, 18);
            batcher.getContext().drawGuiTexture(HORSE_ARMOR_SLOT, 7, 35, 18, 18);
        }
        else if ("donkey".equals(guiId))
        {
            batcher.getContext().drawGuiTexture(HORSE_SADDLE_SLOT, 7, 17, 18, 18);
            if (clip.isMountChestOpen(guiId, tick))
            {
                batcher.getContext().drawGuiTexture(HORSE_CHEST_SLOTS, 79, 17, 90, 54);
            }
        }
    }
}

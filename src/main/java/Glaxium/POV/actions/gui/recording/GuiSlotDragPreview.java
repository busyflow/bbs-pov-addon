package Glaxium.POV.actions.gui.recording;

import Glaxium.POV.actions.gui.GuiSlotSchema;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Encodes vanilla cursor-drag slot previews so replay can show ghost stacks and outlines. */
public final class GuiSlotDragPreview
{
    private GuiSlotDragPreview()
    {
    }

    public static String keyForSlot(String guiId, Slot slot)
    {
        if (slot == null)
        {
            return null;
        }

        if (slot.inventory instanceof PlayerInventory)
        {
            int index = slot.getIndex();
            if (index >= 0 && index < 9)
            {
                return "hotbar_" + index;
            }
            if (index >= 9 && index < 36)
            {
                return "inv_" + (index - 9);
            }
        }

        GuiSlotSchema schema = GuiSlotSchema.get(guiId);
        if (schema != null)
        {
            for (GuiSlotSchema.Slot mapped : schema.slots)
            {
                if (mapped.handlerIndex() == slot.id)
                {
                    return mapped.id();
                }
            }
            for (GuiSlotSchema.Slot mapped : schema.slots)
            {
                if (mapped.x() == slot.x && mapped.y() == slot.y)
                {
                    return mapped.id();
                }
            }
        }

        return keyAt(guiId, slot.x, slot.y);
    }

    public static String keyAt(String guiId, int x, int y)
    {
        GuiSlotSchema schema = GuiSlotSchema.get(guiId);
        for (GuiSlotSchema.Slot mapped : schema.slots)
        {
            if (mapped.x() == x && mapped.y() == y)
            {
                return mapped.id();
            }
        }

        int invX = inventoryX(guiId);
        int invY = inventoryY(guiId);
        int hotbarY = invY + 58;
        if ("creative_inventory".equals(guiId))
        {
            invX = 9;
            invY = 54;
            hotbarY = 112;
        }

        if (schema.playerInventory)
        {
            for (int i = 0; i < 27; i++)
            {
                if (x == invX + (i % 9) * 18 && y == invY + (i / 9) * 18)
                {
                    return "inv_" + i;
                }
            }

            for (int i = 0; i < 9; i++)
            {
                if (x == invX + i * 18 && y == hotbarY)
                {
                    return "hotbar_" + i;
                }
            }
        }

        return null;
    }

    public static ItemStack previewStack(
        ScreenHandler handler,
        Slot slot,
        ItemStack cursor,
        Set<Slot> dragSlots,
        int button)
    {
        if (slot == null || cursor == null || cursor.isEmpty())
        {
            return ItemStack.EMPTY;
        }

        int max = Math.min(cursor.getMaxCount(), slot.getMaxItemCount(cursor));
        int existing = slot.getStack().isEmpty() ? 0 : slot.getStack().getCount();
        int count = Math.min(max, ScreenHandler.calculateStackSize(dragSlots, button, cursor) + existing);
        return cursor.copyWithCount(count);
    }

    public static String encode(int button, int originalCount, ItemStack stack, List<String> keys)
    {
        if (keys == null || keys.isEmpty())
        {
            return "";
        }

        String itemId = "";
        if (stack != null && !stack.isEmpty())
        {
            Identifier id = Registries.ITEM.getId(stack.getItem());
            if (id != null)
            {
                itemId = id.toString();
            }
        }

        return button + "|" + originalCount + "|" + itemId + "|" + String.join(",", keys);
    }

    public static String encode(int button, int originalCount, List<String> keys)
    {
        return encode(button, originalCount, ItemStack.EMPTY, keys);
    }

    public static ItemStack decodeItem(String encoded)
    {
        if (encoded == null || encoded.isBlank())
        {
            return ItemStack.EMPTY;
        }

        String[] parts = encoded.split("\\|", 4);
        if (parts.length >= 4 && !parts[2].isBlank())
        {
            try
            {
                Identifier id = new Identifier(parts[2]);
                Item item = Registries.ITEM.get(id);
                if (item != null && item != Items.AIR)
                {
                    return new ItemStack(item);
                }
            }
            catch (Exception ignored)
            {
            }
        }
        return ItemStack.EMPTY;
    }

    public static Set<String> decodeKeys(String encoded)
    {
        String payload = payload(encoded);
        if (payload.isEmpty())
        {
            return Set.of();
        }

        Set<String> keys = new LinkedHashSet<>();
        for (String part : payload.split(","))
        {
            if (!part.isBlank())
            {
                keys.add(part);
            }
        }
        return keys;
    }

    public static int decodeButton(String encoded)
    {
        return partInt(encoded, 0, 0);
    }

    public static int decodeOriginalCount(String encoded)
    {
        return partInt(encoded, 1, 0);
    }

    public static int previewCount(int button, int originalCount, int slotCount, int maxCount, int existing)
    {
        if (slotCount <= 0)
        {
            return existing;
        }

        int placed = switch (button)
        {
            case 1 -> 1;
            case 2 -> maxCount;
            default -> Math.max(1, originalCount / slotCount);
        };
        return Math.min(maxCount, placed + existing);
    }

    public static List<String> keys(Set<Slot> dragSlots, String guiId)
    {
        if (dragSlots == null || dragSlots.size() <= 1)
        {
            return List.of();
        }

        List<String> keys = new ArrayList<>();
        for (Slot slot : dragSlots)
        {
            String key = keyForSlot(guiId, slot);
            if (key != null)
            {
                keys.add(key);
            }
        }
        Collections.sort(keys);
        return keys;
    }

    private static String payload(String encoded)
    {
        if (encoded == null || encoded.isBlank())
        {
            return "";
        }

        int last = encoded.lastIndexOf('|');
        return last < 0 ? encoded : encoded.substring(last + 1);
    }

    private static int partInt(String encoded, int index, int fallback)
    {
        if (encoded == null || encoded.isBlank())
        {
            return fallback;
        }

        String[] parts = encoded.split("\\|", 3);
        if (index >= parts.length)
        {
            return fallback;
        }

        try
        {
            return Integer.parseInt(parts[index]);
        }
        catch (NumberFormatException ignored)
        {
            return fallback;
        }
    }

    private static int inventoryX(String guiId)
    {
        return "villager".equals(guiId) ? 108 : "beacon".equals(guiId) ? 36 : 8;
    }

    private static int inventoryY(String guiId)
    {
        return switch (guiId)
        {
            case "beacon" -> 137;
            case "large_chest" -> 139;
            case "chest", "barrel", "ender_chest" -> 85;
            case "hopper" -> 51;
            default -> 84;
        };
    }
}

package Glaxium.POV.actions.toast;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/** Metadata definition for a toast preset or entry. */
public class ToastTypeEntry
{
    public final String id;
    public final String category;
    public final String title;
    public final String description;
    public final String titleKey;
    public final String descriptionKey;
    public final String iconItemId;
    public final String frameType; // "task", "goal", "challenge", "recipe", "tutorial", "system"
    public final String textureId; // "toast/advancement", "toast/recipe", "toast/system", "toast/tutorial"

    public ToastTypeEntry(
        String id,
        String category,
        String title,
        String description,
        String titleKey,
        String descriptionKey,
        String iconItemId,
        String frameType,
        String textureId)
    {
        this.id = id;
        this.category = category;
        this.title = title;
        this.description = description;
        this.titleKey = titleKey == null ? "" : titleKey;
        this.descriptionKey = descriptionKey == null ? "" : descriptionKey;
        this.iconItemId = iconItemId;
        this.frameType = frameType;
        this.textureId = textureId;
    }

    public ToastTypeEntry(
        String id,
        String category,
        String title,
        String description,
        String iconItemId,
        String frameType,
        String textureId)
    {
        this(id, category, title, description, "", "", iconItemId, frameType, textureId);
    }

    public net.minecraft.text.Text getTitleText()
    {
        if (this.titleKey != null && !this.titleKey.isEmpty())
        {
            return net.minecraft.text.Text.translatable(this.titleKey);
        }
        return net.minecraft.text.Text.literal(this.title != null ? this.title : "");
    }

    public net.minecraft.text.Text getDescriptionText()
    {
        if (this.descriptionKey != null && !this.descriptionKey.isEmpty())
        {
            return net.minecraft.text.Text.translatable(this.descriptionKey);
        }
        return net.minecraft.text.Text.literal(this.description != null ? this.description : "");
    }

    public ItemStack createIconStack()
    {
        if (this.iconItemId != null && !this.iconItemId.isEmpty())
        {
            try
            {
                Item item = Registries.ITEM.get(new Identifier(this.iconItemId));
                if (item != null && item != Items.AIR)
                {
                    return new ItemStack(item);
                }
            }
            catch (Exception ignored)
            {}
        }
        return new ItemStack(Items.DIAMOND);
    }

    public int getTitleColor()
    {
        if ("challenge".equalsIgnoreCase(this.frameType))
        {
            return 0xFFFF55FF; // Purple
        }
        if ("goal".equalsIgnoreCase(this.frameType))
        {
            return 0xFF55FFFF; // Aqua
        }
        if ("recipe".equalsIgnoreCase(this.frameType))
        {
            return 0xFF500050; // Dark Purple (vanilla recipe color -11534256)
        }
        if ("system".equalsIgnoreCase(this.frameType))
        {
            return 0xFFFFFFFF; // White
        }
        return 0xFFFFFF55; // Yellow default (Task / Tutorial)
    }
}

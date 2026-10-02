package Glaxium.POV.actions.bossbar;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The three vanilla boss-bar sources this action can show. */
public final class BossBarTypeEntry
{
    private static final Map<String, BossBarTypeEntry> REGISTRY = new LinkedHashMap<>();
    private static final List<BossBarTypeEntry> ALL = new ArrayList<>();

    public final String id;
    public final String name;
    public final String defaultTitle;
    public final String defaultColor;
    public final String defaultStyle;

    private BossBarTypeEntry(String id, String name, String defaultTitle, String defaultColor, String defaultStyle)
    {
        this.id = id;
        this.name = name;
        this.defaultTitle = defaultTitle;
        this.defaultColor = defaultColor;
        this.defaultStyle = defaultStyle;
    }

    private static BossBarTypeEntry register(
        String id,
        String name,
        String defaultTitle,
        String defaultColor,
        String defaultStyle)
    {
        BossBarTypeEntry entry = new BossBarTypeEntry(id, name, defaultTitle, defaultColor, defaultStyle);
        REGISTRY.put(id, entry);
        ALL.add(entry);
        return entry;
    }

    public static List<BossBarTypeEntry> getAll()
    {
        return Collections.unmodifiableList(ALL);
    }

    public static BossBarTypeEntry findById(String id)
    {
        return id == null ? null : REGISTRY.get(id);
    }

    public static BossBarTypeEntry next(String currentId)
    {
        BossBarTypeEntry current = findById(currentId);
        int index = current == null ? -1 : ALL.indexOf(current);
        return ALL.get((index + 1) % ALL.size());
    }

    public ItemStack getIcon()
    {
        return switch (this.id)
        {
            case "wither" -> new ItemStack(Items.WITHER_SKELETON_SKULL);
            case "raid" -> new ItemStack(Items.TOTEM_OF_UNDYING);
            default -> new ItemStack(Items.DRAGON_HEAD);
        };
    }

    public static final BossBarTypeEntry DRAGON = register(
        "dragon", "Ender Dragon", "Ender Dragon", "pink", "progress");
    public static final BossBarTypeEntry WITHER = register(
        "wither", "Wither", "Wither", "purple", "progress");
    public static final BossBarTypeEntry RAID = register(
        "raid", "Raid", "Raid", "red", "notched_10");
}

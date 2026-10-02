package Glaxium.POV.actions.menu;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registry of bakeable Menu action types. */
public final class MenuTypeEntry
{
    private static final Map<String, MenuTypeEntry> REGISTRY = new LinkedHashMap<>();
    private static final List<MenuTypeEntry> ALL = new ArrayList<>();

    public final String id;
    public final String name;

    private MenuTypeEntry(String id, String name)
    {
        this.id = id;
        this.name = name;
    }

    private static MenuTypeEntry register(String id, String name)
    {
        MenuTypeEntry entry = new MenuTypeEntry(id, name);
        REGISTRY.put(id, entry);
        ALL.add(entry);
        return entry;
    }

    public static List<MenuTypeEntry> getAll()
    {
        return Collections.unmodifiableList(ALL);
    }

    public static MenuTypeEntry findById(String id)
    {
        return id == null ? null : REGISTRY.get(id);
    }

    public static MenuTypeEntry next(String currentId)
    {
        List<MenuTypeEntry> all = ALL;
        if (all.isEmpty())
        {
            return GAME_MENU;
        }

        MenuTypeEntry current = findById(currentId);
        int index = current == null ? -1 : all.indexOf(current);

        return all.get((index + 1) % all.size());
    }

    public ItemStack getIcon()
    {
        return switch (this.id)
        {
            case "death" -> new ItemStack(Items.SKELETON_SKULL);
            case "sleep" -> new ItemStack(Items.RED_BED);
            default -> new ItemStack(Items.PAINTING);
        };
    }

    public static final MenuTypeEntry GAME_MENU = register("game_menu", "Game Menu");
    public static final MenuTypeEntry DEATH = register("death", "Death");
    public static final MenuTypeEntry SLEEP = register("sleep", "Sleep");
}

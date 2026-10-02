package Glaxium.POV.actions.toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Built-in presets for Advancements, Recipes, Tutorials, and System Toasts. */
public final class ToastPresets
{
    private static final List<ToastTypeEntry> PRESETS = new ArrayList<>();

    static
    {
        // Advancements - Story & Adventure
        add("adv_stone_age", "Advancements", "Advancement Made!", "Stone Age", "advancements.toast.task", "advancements.story.mine_stone.title", "minecraft:wooden_pickaxe", "task", "toast/advancement");
        add("adv_getting_upgrade", "Advancements", "Advancement Made!", "Getting an Upgrade", "advancements.toast.task", "advancements.story.upgrade_tools.title", "minecraft:stone_pickaxe", "task", "toast/advancement");
        add("adv_acquire_hardware", "Advancements", "Advancement Made!", "Acquire Hardware", "advancements.toast.task", "advancements.story.smelt_iron.title", "minecraft:iron_ingot", "task", "toast/advancement");
        add("adv_suit_up", "Advancements", "Advancement Made!", "Suit Up", "advancements.toast.task", "advancements.story.obtain_armor.title", "minecraft:iron_chestplate", "task", "toast/advancement");
        add("adv_hot_stuff", "Advancements", "Advancement Made!", "Hot Stuff", "advancements.toast.task", "advancements.story.lava_bucket.title", "minecraft:lava_bucket", "task", "toast/advancement");
        add("adv_isnt_it_iron_pick", "Advancements", "Advancement Made!", "Isn't It Iron Pick", "advancements.toast.task", "advancements.story.iron_tools.title", "minecraft:iron_pickaxe", "task", "toast/advancement");
        add("adv_not_today", "Advancements", "Advancement Made!", "Not Today, Thank You", "advancements.toast.task", "advancements.story.deflect_arrow.title", "minecraft:shield", "task", "toast/advancement");
        add("adv_ice_bucket", "Advancements", "Advancement Made!", "Ice Bucket Challenge", "advancements.toast.task", "advancements.story.form_obsidian.title", "minecraft:obsidian", "task", "toast/advancement");
        add("adv_diamonds", "Advancements", "Advancement Made!", "Diamonds!", "advancements.toast.task", "advancements.story.mine_diamond.title", "minecraft:diamond", "task", "toast/advancement");
        add("adv_we_need_to_go_deeper", "Advancements", "Advancement Made!", "We Need to Go Deeper", "advancements.toast.task", "advancements.story.enter_the_nether.title", "minecraft:flint_and_steel", "task", "toast/advancement");
        add("adv_cover_me_in_diamonds", "Advancements", "Goal Reached!", "Cover Me in Diamonds", "advancements.toast.goal", "advancements.story.shiny_gear.title", "minecraft:diamond_chestplate", "goal", "toast/advancement");
        add("adv_enchanter", "Advancements", "Advancement Made!", "Enchanter", "advancements.toast.task", "advancements.story.enchant_item.title", "minecraft:enchanting_table", "task", "toast/advancement");
        add("adv_zombie_doctor", "Advancements", "Goal Reached!", "Zombie Doctor", "advancements.toast.goal", "advancements.story.cure_zombie_villager.title", "minecraft:golden_apple", "goal", "toast/advancement");
        add("adv_eye_spy", "Advancements", "Advancement Made!", "Eye Spy", "advancements.toast.task", "advancements.story.follow_ender_eye.title", "minecraft:ender_eye", "task", "toast/advancement");
        add("adv_the_end", "Advancements", "Advancement Made!", "The End?", "advancements.toast.task", "advancements.story.enter_the_end.title", "minecraft:end_stone", "task", "toast/advancement");
        add("adv_free_the_end", "Advancements", "Challenge Complete!", "Free the End", "advancements.toast.challenge", "advancements.end.kill_dragon.title", "minecraft:dragon_head", "challenge", "toast/advancement");
        add("adv_monster_hunter", "Advancements", "Advancement Made!", "Monster Hunter", "advancements.toast.task", "advancements.adventure.kill_a_mob.title", "minecraft:iron_sword", "task", "toast/advancement");
        add("adv_monsters_hunted", "Advancements", "Challenge Complete!", "Monsters Hunted", "advancements.toast.challenge", "advancements.adventure.kill_all_mobs.title", "minecraft:diamond_sword", "challenge", "toast/advancement");
        add("adv_return_to_sender", "Advancements", "Challenge Complete!", "Return to Sender", "advancements.toast.challenge", "advancements.nether.return_to_sender.title", "minecraft:ghast_tear", "challenge", "toast/advancement");
        add("adv_uneasy_alliance", "Advancements", "Challenge Complete!", "Uneasy Alliance", "advancements.toast.challenge", "advancements.nether.uneasy_alliance.title", "minecraft:ghast_tear", "challenge", "toast/advancement");
        add("adv_sniper_duel", "Advancements", "Challenge Complete!", "Sniper Duel", "advancements.toast.challenge", "advancements.adventure.sniper_duel.title", "minecraft:bow", "challenge", "toast/advancement");
        add("adv_bullseye", "Advancements", "Challenge Complete!", "Bullseye", "advancements.toast.challenge", "advancements.adventure.bullseye.title", "minecraft:target", "challenge", "toast/advancement");
        add("adv_how_did_we_get_here", "Advancements", "Challenge Complete!", "How Did We Get Here?", "advancements.toast.challenge", "advancements.nether.all_effects.title", "minecraft:beacon", "challenge", "toast/advancement");
        add("adv_hero_of_village", "Advancements", "Challenge Complete!", "Hero of the Village", "advancements.toast.challenge", "advancements.adventure.hero_of_the_village.title", "minecraft:totem_of_undying", "challenge", "toast/advancement");
        add("adv_postmortal", "Advancements", "Goal Reached!", "Postmortal", "advancements.toast.goal", "advancements.adventure.totem_of_undying.title", "minecraft:totem_of_undying", "goal", "toast/advancement");
        add("adv_great_view", "Advancements", "Challenge Complete!", "Great View From Up Here", "advancements.toast.challenge", "advancements.end.levitate.title", "minecraft:shulker_shell", "challenge", "toast/advancement");
        add("adv_subspace_bubble", "Advancements", "Challenge Complete!", "Subspace Bubble", "advancements.toast.challenge", "advancements.nether.fast_travel.title", "minecraft:netherrack", "challenge", "toast/advancement");

        // Recipes
        add("rec_crafting_table", "Recipes", "Recipe Unlocked!", "Crafting Table", "recipe.toast.title", "block.minecraft.crafting_table", "minecraft:crafting_table", "recipe", "toast/recipe");
        add("rec_furnace", "Recipes", "Recipe Unlocked!", "Furnace", "recipe.toast.title", "block.minecraft.furnace", "minecraft:furnace", "recipe", "toast/recipe");
        add("rec_chest", "Recipes", "Recipe Unlocked!", "Chest", "recipe.toast.title", "block.minecraft.chest", "minecraft:chest", "recipe", "toast/recipe");
        add("rec_wooden_planks", "Recipes", "Recipe Unlocked!", "Oak Planks", "recipe.toast.title", "block.minecraft.oak_planks", "minecraft:oak_planks", "recipe", "toast/recipe");
        add("rec_bread", "Recipes", "Recipe Unlocked!", "Bread", "recipe.toast.title", "item.minecraft.bread", "minecraft:bread", "recipe", "toast/recipe");
        add("rec_golden_apple", "Recipes", "Recipe Unlocked!", "Golden Apple", "recipe.toast.title", "item.minecraft.golden_apple", "minecraft:golden_apple", "recipe", "toast/recipe");
        add("rec_iron_pickaxe", "Recipes", "Recipe Unlocked!", "Iron Pickaxe", "recipe.toast.title", "item.minecraft.iron_pickaxe", "minecraft:iron_pickaxe", "recipe", "toast/recipe");
        add("rec_diamond_sword", "Recipes", "Recipe Unlocked!", "Diamond Sword", "recipe.toast.title", "item.minecraft.diamond_sword", "minecraft:diamond_sword", "recipe", "toast/recipe");

        // Tutorials
        add("tut_movement", "Tutorials", "Tutorial", "Movement Keys (WASD)", "tutorial.move.title", "tutorial.move.description", "minecraft:compass", "tutorial", "toast/tutorial");
        add("tut_look", "Tutorials", "Tutorial", "Look Around with Mouse", "tutorial.look.title", "tutorial.look.description", "minecraft:spyglass", "tutorial", "toast/tutorial");
        add("tut_punch_tree", "Tutorials", "Tutorial", "Punch a Tree Trunk", "tutorial.punch_tree.title", "tutorial.punch_tree.description", "minecraft:oak_log", "tutorial", "toast/tutorial");
        add("tut_open_inventory", "Tutorials", "Tutorial", "Open Inventory (E)", "tutorial.open_inventory.title", "tutorial.open_inventory.description", "minecraft:chest", "tutorial", "toast/tutorial");
        add("tut_craft_workbench", "Tutorials", "Tutorial", "Craft a Workbench", "tutorial.craft_planks.title", "tutorial.craft_planks.description", "minecraft:crafting_table", "tutorial", "toast/tutorial");

        // System
        add("sys_screenshot", "System", "Screenshot", "Saved as screenshot.png", "screenshot.success", "", "minecraft:painting", "system", "toast/system");
        add("sys_world_saved", "System", "World Saved", "All chunks have been saved", "menu.savingLevel", "", "minecraft:writable_book", "system", "toast/system");
        add("sys_narrator", "System", "Narrator", "Narrator is now active", "options.narrator", "narrator.toast.enabled", "minecraft:bell", "system", "toast/system");
    }

    private static void add(
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
        PRESETS.add(new ToastTypeEntry(id, category, title, description, titleKey, descriptionKey, iconItemId, frameType, textureId));
    }

    public static List<ToastTypeEntry> getAll()
    {
        return Collections.unmodifiableList(PRESETS);
    }

    public static ToastTypeEntry getById(String id)
    {
        if (id == null || id.isEmpty())
        {
            return PRESETS.get(0);
        }
        for (ToastTypeEntry entry : PRESETS)
        {
            if (entry.id.equalsIgnoreCase(id))
            {
                return entry;
            }
        }
        return PRESETS.get(0);
    }

    public static List<String> getCategories()
    {
        List<String> categories = new ArrayList<>();
        for (ToastTypeEntry entry : PRESETS)
        {
            if (!categories.contains(entry.category))
            {
                categories.add(entry.category);
            }
        }
        return categories;
    }

    public static List<ToastTypeEntry> getByCategory(String category)
    {
        List<ToastTypeEntry> list = new ArrayList<>();
        for (ToastTypeEntry entry : PRESETS)
        {
            if (entry.category.equalsIgnoreCase(category))
            {
                list.add(entry);
            }
        }
        return list;
    }

    public static ToastTypeEntry findByTitleOrDesc(String text)
    {
        if (text == null || text.trim().isEmpty())
        {
            return null;
        }
        for (ToastTypeEntry entry : PRESETS)
        {
            if (entry.description.equalsIgnoreCase(text) || entry.title.equalsIgnoreCase(text))
            {
                return entry;
            }
        }
        return null;
    }
}

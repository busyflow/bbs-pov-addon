package Glaxium.POV.actions.toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ToastPresets {
   private static final List<ToastTypeEntry> PRESETS = new ArrayList<>();

   private static void add(String id, String category, String title, String description, String iconItemId, String frameType, String textureId) {
      PRESETS.add(new ToastTypeEntry(id, category, title, description, iconItemId, frameType, textureId));
   }

   public static List<ToastTypeEntry> getAll() {
      return Collections.unmodifiableList(PRESETS);
   }

   public static ToastTypeEntry getById(String id) {
      if (id != null && !id.isEmpty()) {
         for (ToastTypeEntry entry : PRESETS) {
            if (entry.id.equalsIgnoreCase(id)) {
               return entry;
            }
         }

         return PRESETS.get(0);
      } else {
         return PRESETS.get(0);
      }
   }

   public static List<String> getCategories() {
      List<String> categories = new ArrayList<>();

      for (ToastTypeEntry entry : PRESETS) {
         if (!categories.contains(entry.category)) {
            categories.add(entry.category);
         }
      }

      return categories;
   }

   public static List<ToastTypeEntry> getByCategory(String category) {
      List<ToastTypeEntry> list = new ArrayList<>();

      for (ToastTypeEntry entry : PRESETS) {
         if (entry.category.equalsIgnoreCase(category)) {
            list.add(entry);
         }
      }

      return list;
   }

   public static ToastTypeEntry findByTitleOrDesc(String text) {
      if (text != null && !text.trim().isEmpty()) {
         for (ToastTypeEntry entry : PRESETS) {
            if (entry.description.equalsIgnoreCase(text) || entry.title.equalsIgnoreCase(text)) {
               return entry;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   static {
      add("adv_stone_age", "Advancements", "Advancement Made!", "Stone Age", "minecraft:wooden_pickaxe", "task", "toast/advancement");
      add("adv_getting_upgrade", "Advancements", "Advancement Made!", "Getting an Upgrade", "minecraft:stone_pickaxe", "task", "toast/advancement");
      add("adv_acquire_hardware", "Advancements", "Advancement Made!", "Acquire Hardware", "minecraft:iron_ingot", "task", "toast/advancement");
      add("adv_suit_up", "Advancements", "Advancement Made!", "Suit Up", "minecraft:iron_chestplate", "task", "toast/advancement");
      add("adv_hot_stuff", "Advancements", "Advancement Made!", "Hot Stuff", "minecraft:lava_bucket", "task", "toast/advancement");
      add("adv_isnt_it_iron_pick", "Advancements", "Advancement Made!", "Isn't It Iron Pick", "minecraft:iron_pickaxe", "task", "toast/advancement");
      add("adv_not_today", "Advancements", "Advancement Made!", "Not Today, Thank You", "minecraft:shield", "task", "toast/advancement");
      add("adv_ice_bucket", "Advancements", "Advancement Made!", "Ice Bucket Challenge", "minecraft:obsidian", "task", "toast/advancement");
      add("adv_diamonds", "Advancements", "Advancement Made!", "Diamonds!", "minecraft:diamond", "task", "toast/advancement");
      add("adv_we_need_to_go_deeper", "Advancements", "Advancement Made!", "We Need to Go Deeper", "minecraft:flint_and_steel", "task", "toast/advancement");
      add("adv_cover_me_in_diamonds", "Advancements", "Goal Reached!", "Cover Me in Diamonds", "minecraft:diamond_chestplate", "goal", "toast/advancement");
      add("adv_enchanter", "Advancements", "Advancement Made!", "Enchanter", "minecraft:enchanting_table", "task", "toast/advancement");
      add("adv_zombie_doctor", "Advancements", "Goal Reached!", "Zombie Doctor", "minecraft:golden_apple", "goal", "toast/advancement");
      add("adv_eye_spy", "Advancements", "Advancement Made!", "Eye Spy", "minecraft:ender_eye", "task", "toast/advancement");
      add("adv_the_end", "Advancements", "Advancement Made!", "The End?", "minecraft:end_stone", "task", "toast/advancement");
      add("adv_free_the_end", "Advancements", "Challenge Complete!", "Free the End", "minecraft:dragon_head", "challenge", "toast/advancement");
      add("adv_monster_hunter", "Advancements", "Advancement Made!", "Monster Hunter", "minecraft:iron_sword", "task", "toast/advancement");
      add("adv_monsters_hunted", "Advancements", "Challenge Complete!", "Monsters Hunted", "minecraft:diamond_sword", "challenge", "toast/advancement");
      add("adv_return_to_sender", "Advancements", "Challenge Complete!", "Return to Sender", "minecraft:ghast_tear", "challenge", "toast/advancement");
      add("adv_uneasy_alliance", "Advancements", "Challenge Complete!", "Uneasy Alliance", "minecraft:ghast_tear", "challenge", "toast/advancement");
      add("adv_sniper_duel", "Advancements", "Challenge Complete!", "Sniper Duel", "minecraft:bow", "challenge", "toast/advancement");
      add("adv_bullseye", "Advancements", "Challenge Complete!", "Bullseye", "minecraft:target", "challenge", "toast/advancement");
      add("adv_how_did_we_get_here", "Advancements", "Challenge Complete!", "How Did We Get Here?", "minecraft:beacon", "challenge", "toast/advancement");
      add("adv_hero_of_village", "Advancements", "Challenge Complete!", "Hero of the Village", "minecraft:totem_of_undying", "challenge", "toast/advancement");
      add("adv_postmortal", "Advancements", "Goal Reached!", "Postmortal", "minecraft:totem_of_undying", "goal", "toast/advancement");
      add("adv_great_view", "Advancements", "Challenge Complete!", "Great View From Up Here", "minecraft:shulker_shell", "challenge", "toast/advancement");
      add("adv_subspace_bubble", "Advancements", "Challenge Complete!", "Subspace Bubble", "minecraft:netherrack", "challenge", "toast/advancement");
      add("rec_crafting_table", "Recipes", "Recipe Unlocked!", "Crafting Table", "minecraft:crafting_table", "recipe", "toast/recipe");
      add("rec_furnace", "Recipes", "Recipe Unlocked!", "Furnace", "minecraft:furnace", "recipe", "toast/recipe");
      add("rec_chest", "Recipes", "Recipe Unlocked!", "Chest", "minecraft:chest", "recipe", "toast/recipe");
      add("rec_wooden_planks", "Recipes", "Recipe Unlocked!", "Oak Planks", "minecraft:oak_planks", "recipe", "toast/recipe");
      add("rec_bread", "Recipes", "Recipe Unlocked!", "Bread", "minecraft:bread", "recipe", "toast/recipe");
      add("rec_golden_apple", "Recipes", "Recipe Unlocked!", "Golden Apple", "minecraft:golden_apple", "recipe", "toast/recipe");
      add("rec_iron_pickaxe", "Recipes", "Recipe Unlocked!", "Iron Pickaxe", "minecraft:iron_pickaxe", "recipe", "toast/recipe");
      add("rec_diamond_sword", "Recipes", "Recipe Unlocked!", "Diamond Sword", "minecraft:diamond_sword", "recipe", "toast/recipe");
      add("tut_movement", "Tutorials", "Tutorial", "Movement Keys (WASD)", "minecraft:compass", "tutorial", "toast/tutorial");
      add("tut_look", "Tutorials", "Tutorial", "Look Around with Mouse", "minecraft:spyglass", "tutorial", "toast/tutorial");
      add("tut_punch_tree", "Tutorials", "Tutorial", "Punch a Tree Trunk", "minecraft:oak_log", "tutorial", "toast/tutorial");
      add("tut_open_inventory", "Tutorials", "Tutorial", "Open Inventory (E)", "minecraft:chest", "tutorial", "toast/tutorial");
      add("tut_craft_workbench", "Tutorials", "Tutorial", "Craft a Workbench", "minecraft:crafting_table", "tutorial", "toast/tutorial");
      add("sys_screenshot", "System", "Screenshot", "Saved as screenshot.png", "minecraft:painting", "system", "toast/system");
      add("sys_world_saved", "System", "World Saved", "All chunks have been saved", "minecraft:writable_book", "system", "toast/system");
      add("sys_narrator", "System", "Narrator", "Narrator is now active", "minecraft:bell", "system", "toast/system");
   }
}

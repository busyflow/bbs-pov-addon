package Glaxium.POV.actions.gui;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GuiTypeEntry
{
    private static final Map<String, GuiTypeEntry> REGISTRY = new LinkedHashMap<>();
    private static final List<GuiTypeEntry> ALL_ENTRIES = new ArrayList<>();

    public final String id;
    public final String name;
    public final String category;
    public final Identifier texture;
    public final int u;
    public final int v;
    public final int regionWidth;
    public final int regionHeight;
    public final int textureWidth;
    public final int textureHeight;
    public final String title;
    public final String titleKey;
    public final int titleX;
    public final int titleY;
    /** Player-inventory label. Negative X means vanilla does not draw it. */
    public final int inventoryTitleX;
    public final int inventoryTitleY;

    public GuiTypeEntry(
        String id,
        String name,
        String category,
        Identifier texture,
        int u,
        int v,
        int regionWidth,
        int regionHeight,
        int textureWidth,
        int textureHeight,
        String title,
        String titleKey,
        int titleX,
        int titleY)
    {
        this.id = id;
        this.name = name;
        this.category = category;
        this.texture = texture;
        this.u = u;
        this.v = v;
        this.regionWidth = regionWidth;
        this.regionHeight = regionHeight;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.title = title;
        this.titleKey = titleKey == null ? "" : titleKey;
        this.titleX = titleX;
        this.titleY = titleY;
        this.inventoryTitleX = inventoryTitleX(id);
        this.inventoryTitleY = this.inventoryTitleX < 0 ? -1 : regionHeight - 94;
    }

    public net.minecraft.text.Text getTitleText()
    {
        if (this.titleKey != null && !this.titleKey.isEmpty())
        {
            return net.minecraft.text.Text.translatable(this.titleKey);
        }
        return this.title != null && !this.title.isEmpty() ? net.minecraft.text.Text.literal(this.title) : net.minecraft.text.Text.empty();
    }

    private static int inventoryTitleX(String id)
    {
        return switch (id)
        {
            case "inventory", "creative_inventory", "beacon", "book", "gamemode_switcher" -> -1;
            case "villager" -> 107;
            default -> 8;
        };
    }

    public ItemStack getIcon()
    {
        return switch (this.id)
        {
            case "inventory" -> new ItemStack(Items.CHEST);
            case "creative_inventory" -> new ItemStack(Items.COMPASS);
            case "crafting_table" -> new ItemStack(Items.CRAFTING_TABLE);
            case "anvil" -> new ItemStack(Items.ANVIL);
            case "smithing_table" -> new ItemStack(Items.SMITHING_TABLE);
            case "grindstone" -> new ItemStack(Items.GRINDSTONE);
            case "stonecutter" -> new ItemStack(Items.STONECUTTER);
            case "cartography_table" -> new ItemStack(Items.CARTOGRAPHY_TABLE);
            case "loom" -> new ItemStack(Items.LOOM);
            case "furnace" -> new ItemStack(Items.FURNACE);
            case "blast_furnace" -> new ItemStack(Items.BLAST_FURNACE);
            case "smoker" -> new ItemStack(Items.SMOKER);
            case "brewing_stand" -> new ItemStack(Items.BREWING_STAND);
            case "chest", "large_chest" -> new ItemStack(Items.CHEST);
            case "barrel" -> new ItemStack(Items.BARREL);
            case "ender_chest" -> new ItemStack(Items.ENDER_CHEST);
            case "shulker_box" -> new ItemStack(Items.SHULKER_BOX);
            case "hopper" -> new ItemStack(Items.HOPPER);
            case "dispenser" -> new ItemStack(Items.DISPENSER);
            case "dropper" -> new ItemStack(Items.DROPPER);
            case "enchanting_table" -> new ItemStack(Items.ENCHANTING_TABLE);
            case "beacon" -> new ItemStack(Items.BEACON);
            case "villager" -> new ItemStack(Items.EMERALD);
            case "horse" -> new ItemStack(Items.SADDLE);
            case "donkey" -> new ItemStack(Items.CHEST);
            case "book" -> new ItemStack(Items.WRITABLE_BOOK);
            case "gamemode_switcher" -> new ItemStack(Items.COMMAND_BLOCK);
            default -> new ItemStack(Items.CHEST);
        };
    }

    public static int getCategoryColor(String category)
    {
        return switch (category == null ? "" : category)
        {
            case "Player" -> 0xff2b78d6;
            case "Crafting" -> 0xffd97824;
            case "Smelting" -> 0xffe04826;
            case "Brewing" -> 0xff993ad8;
            case "Containers" -> 0xff8c6239;
            case "Redstone" -> 0xffd62828;
            case "Enchanting" -> 0xffb826b8;
            case "Special" -> 0xffd49b13;
            case "Trading" -> 0xff25b84c;
            case "Entities" -> 0xffa0541c;
            default -> 0xff888888;
        };
    }

    public static GuiTypeEntry register(
        String id,
        String name,
        String category,
        String texturePath,
        int u,
        int v,
        int regionWidth,
        int regionHeight,
        int textureWidth,
        int textureHeight,
        String title,
        String titleKey,
        int titleX,
        int titleY)
    {
        Identifier tex = new Identifier("minecraft", texturePath);
        GuiTypeEntry entry = new GuiTypeEntry(
            id,
            name,
            category,
            tex,
            u,
            v,
            regionWidth,
            regionHeight,
            textureWidth,
            textureHeight,
            title,
            titleKey,
            titleX,
            titleY
        );
        REGISTRY.put(id, entry);
        ALL_ENTRIES.add(entry);
        return entry;
    }

    public static GuiTypeEntry register(
        String id,
        String name,
        String category,
        String texturePath,
        int u,
        int v,
        int regionWidth,
        int regionHeight,
        int textureWidth,
        int textureHeight,
        String title,
        int titleX,
        int titleY)
    {
        return register(id, name, category, texturePath, u, v, regionWidth, regionHeight, textureWidth, textureHeight, title, "", titleX, titleY);
    }

    public static List<GuiTypeEntry> getAll()
    {
        return Collections.unmodifiableList(ALL_ENTRIES);
    }

    public static GuiTypeEntry findById(String id)
    {
        if (id == null || id.isBlank())
        {
            return INVENTORY;
        }
        GuiTypeEntry entry = REGISTRY.get(id);
        if (entry != null)
        {
            return entry;
        }
        for (GuiTypeEntry e : ALL_ENTRIES)
        {
            if (e.id.equalsIgnoreCase(id) || e.id.endsWith(":" + id) || id.endsWith(":" + e.id))
            {
                return e;
            }
        }
        return INVENTORY;
    }

    // Player
    public static final GuiTypeEntry INVENTORY = register(
        "inventory", "Inventory", "Player", "textures/gui/container/inventory.png",
        0, 0, 176, 166, 256, 256, "Crafting", "container.crafting", 97, 6
    );
    public static final GuiTypeEntry CREATIVE = register(
        "creative_inventory", "Creative Inventory", "Player", "textures/gui/container/creative_inventory/tab_items.png",
        0, 0, 195, 136, 256, 256, "", "", 0, 0
    );

    // Crafting & Workstations
    public static final GuiTypeEntry CRAFTING_TABLE = register(
        "crafting_table", "Crafting Table", "Crafting", "textures/gui/container/crafting_table.png",
        0, 0, 176, 166, 256, 256, "Crafting", "container.crafting", 29, 6
    );
    public static final GuiTypeEntry ANVIL = register(
        "anvil", "Anvil", "Crafting", "textures/gui/container/anvil.png",
        0, 0, 176, 166, 256, 256, "Repair & Name", "container.repair", 60, 6
    );
    public static final GuiTypeEntry SMITHING_TABLE = register(
        "smithing_table", "Smithing Table", "Crafting", "textures/gui/container/smithing.png",
        0, 0, 176, 166, 256, 256, "Upgrade Gear", "container.upgrade", 44, 15
    );
    public static final GuiTypeEntry GRINDSTONE = register(
        "grindstone", "Grindstone", "Crafting", "textures/gui/container/grindstone.png",
        0, 0, 176, 166, 256, 256, "Repair & Disenchant", "container.grindstone_title", 8, 6
    );
    public static final GuiTypeEntry STONECUTTER = register(
        "stonecutter", "Stonecutter", "Crafting", "textures/gui/container/stonecutter.png",
        0, 0, 176, 166, 256, 256, "Stonecutter", "container.stonecutter", 8, 6
    );
    public static final GuiTypeEntry CARTOGRAPHY_TABLE = register(
        "cartography_table", "Cartography Table", "Crafting", "textures/gui/container/cartography_table.png",
        0, 0, 176, 166, 256, 256, "Cartography Table", "container.cartography_table", 8, 4
    );
    public static final GuiTypeEntry LOOM = register(
        "loom", "Loom", "Crafting", "textures/gui/container/loom.png",
        0, 0, 176, 166, 256, 256, "Loom", "container.loom", 8, 6
    );

    // Smelting & Brewing
    public static final GuiTypeEntry FURNACE = register(
        "furnace", "Furnace", "Smelting", "textures/gui/container/furnace.png",
        0, 0, 176, 166, 256, 256, "Furnace", "container.furnace", -1, 6
    );
    public static final GuiTypeEntry BLAST_FURNACE = register(
        "blast_furnace", "Blast Furnace", "Smelting", "textures/gui/container/blast_furnace.png",
        0, 0, 176, 166, 256, 256, "Blast Furnace", "container.blast_furnace", -1, 6
    );
    public static final GuiTypeEntry SMOKER = register(
        "smoker", "Smoker", "Smelting", "textures/gui/container/smoker.png",
        0, 0, 176, 166, 256, 256, "Smoker", "container.smoker", -1, 6
    );
    public static final GuiTypeEntry BREWING_STAND = register(
        "brewing_stand", "Brewing Stand", "Brewing", "textures/gui/container/brewing_stand.png",
        0, 0, 176, 166, 256, 256, "Brewing Stand", "container.brewing", -1, 6
    );

    // Storage
    public static final GuiTypeEntry CHEST_SMALL = register(
        "chest", "Chest (Small 9x3)", "Containers", "textures/gui/container/generic_54.png",
        0, 0, 176, 168, 256, 256, "Chest", "container.chest", 8, 6
    );
    public static final GuiTypeEntry CHEST_LARGE = register(
        "large_chest", "Chest (Large 9x6)", "Containers", "textures/gui/container/generic_54.png",
        0, 0, 176, 222, 256, 256, "Large Chest", "container.chestDouble", 8, 6
    );
    public static final GuiTypeEntry BARREL = register(
        "barrel", "Barrel", "Containers", "textures/gui/container/generic_54.png",
        0, 0, 176, 168, 256, 256, "Barrel", "container.barrel", 8, 6
    );
    public static final GuiTypeEntry ENDER_CHEST = register(
        "ender_chest", "Ender Chest", "Containers", "textures/gui/container/generic_54.png",
        0, 0, 176, 168, 256, 256, "Ender Chest", "container.enderchest", 8, 6
    );
    public static final GuiTypeEntry SHULKER_BOX = register(
        "shulker_box", "Shulker Box", "Containers", "textures/gui/container/shulker_box.png",
        0, 0, 176, 166, 256, 256, "Shulker Box", "container.shulkerBox", 8, 6
    );
    public static final GuiTypeEntry HOPPER = register(
        "hopper", "Hopper", "Containers", "textures/gui/container/hopper.png",
        0, 0, 176, 133, 256, 256, "Item Hopper", "container.hopper", 8, 6
    );
    public static final GuiTypeEntry DISPENSER = register(
        "dispenser", "Dispenser", "Redstone", "textures/gui/container/dispenser.png",
        0, 0, 176, 166, 256, 256, "Dispenser", "container.dispenser", -1, 6
    );
    public static final GuiTypeEntry DROPPER = register(
        "dropper", "Dropper", "Redstone", "textures/gui/container/dispenser.png",
        0, 0, 176, 166, 256, 256, "Dropper", "container.dropper", -1, 6
    );
    // Magic & Special
    public static final GuiTypeEntry ENCHANTING_TABLE = register(
        "enchanting_table", "Enchanting Table", "Enchanting", "textures/gui/container/enchanting_table.png",
        0, 0, 176, 166, 256, 256, "Enchant", "container.enchant", 8, 5
    );
    public static final GuiTypeEntry BEACON = register(
        "beacon", "Beacon", "Special", "textures/gui/container/beacon.png",
        0, 0, 230, 219, 256, 256, "", "", 0, 0
    );
    public static final GuiTypeEntry VILLAGER = register(
        "villager", "Villager Trading", "Trading", "textures/gui/container/villager.png",
        0, 0, 276, 166, 512, 256, "Merchant", "entity.minecraft.villager", 136, 6
    );
    public static final GuiTypeEntry HORSE = register(
        "horse", "Horse Inventory", "Entities", "textures/gui/container/horse.png",
        0, 0, 176, 166, 256, 256, "Horse", "entity.minecraft.horse", 8, 6
    );
    public static final GuiTypeEntry DONKEY = register(
        "donkey", "Donkey Inventory", "Entities", "textures/gui/container/horse.png",
        0, 0, 176, 166, 256, 256, "Donkey", "entity.minecraft.donkey", 8, 6
    );
    /** Vanilla BookEditScreen/BookScreen draw the book at y=2, not vertically centered. */
    public static final int BOOK_SCREEN_Y = 2;
    public static final GuiTypeEntry BOOK = register(
        "book", "Book", "Special", "textures/gui/book.png",
        0, 0, 192, 220, 256, 256, "", "", 0, 0
    );
    public static final GuiTypeEntry GAMEMODE_SWITCHER = register(
        "gamemode_switcher", "Gamemode Switcher", "Special", "textures/gui/container/gamemode_switcher.png",
        0, 0, 125, 75, 128, 128, "", "", 0, 0
    );

    @Override
    public String toString()
    {
        return this.name + " " + this.id + " " + this.category;
    }
}

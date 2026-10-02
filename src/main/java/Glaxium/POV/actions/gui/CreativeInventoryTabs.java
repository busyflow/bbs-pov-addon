package Glaxium.POV.actions.gui;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.text.TranslatableTextContent;

import java.util.List;

public final class CreativeInventoryTabs
{
    private static Boolean isForgeCache = null;

    private CreativeInventoryTabs()
    {}

    public static boolean isForge()
    {
        if (isForgeCache != null)
        {
            return isForgeCache;
        }
        if (Boolean.getBoolean("bbs_pov.force_forge"))
        {
            isForgeCache = true;
            return true;
        }
        try
        {
            if (FabricLoader.getInstance().isModLoaded("connector")
                || FabricLoader.getInstance().isModLoaded("connectormod")
                || FabricLoader.getInstance().isModLoaded("forge")
                || FabricLoader.getInstance().isModLoaded("neoforge"))
            {
                isForgeCache = true;
                return true;
            }
        }
        catch (Throwable ignored)
        {
        }
        try
        {
            Class.forName("net.neoforged.fml.loading.FMLLoader");
            isForgeCache = true;
            return true;
        }
        catch (Throwable ignored)
        {
        }
        try
        {
            Class.forName("net.minecraftforge.fml.loading.FMLLoader");
            isForgeCache = true;
            return true;
        }
        catch (Throwable ignored)
        {
        }
        isForgeCache = false;
        return false;
    }

    public static boolean isCommonGroup(ItemGroup group)
    {
        if (group == null)
        {
            return false;
        }
        ItemGroup.Type type = group.getType();
        return type == ItemGroup.Type.SEARCH || type == ItemGroup.Type.INVENTORY || type == ItemGroup.Type.HOTBAR;
    }

    public static boolean isVanillaGroup(ItemGroup group)
    {
        if (group == null)
        {
            return false;
        }
        if (isCommonGroup(group))
        {
            return true;
        }
        if (group.getDisplayName() != null && group.getDisplayName().getContent() instanceof TranslatableTextContent trans)
        {
            String key = trans.getKey();
            if (key != null && key.startsWith("itemGroup.") && !key.contains(":") && !key.startsWith("itemGroup.bbs"))
            {
                return true;
            }
        }
        return false;
    }

    public static List<ItemGroup> groups()
    {
        return ItemGroups.getGroupsToDisplay().stream()
            .filter(group -> !group.getIcon().isOf(Items.COMMAND_BLOCK))
            .toList();
    }

    private static int getFabricPage(ItemGroup group)
    {
        if (group == null)
        {
            return 0;
        }
        if (group instanceof net.fabricmc.fabric.impl.itemgroup.FabricItemGroup fig)
        {
            return fig.getPage();
        }
        try
        {
            java.lang.reflect.Method m = group.getClass().getMethod("getPage");
            Object res = m.invoke(group);
            if (res instanceof Integer i)
            {
                return i;
            }
        }
        catch (Throwable ignored)
        {
        }
        try
        {
            java.lang.reflect.Method m = group.getClass().getMethod("fabric_getPage");
            Object res = m.invoke(group);
            if (res instanceof Integer i)
            {
                return i;
            }
        }
        catch (Throwable ignored)
        {
        }
        return 0;
    }

    public static int page(ItemGroup group, int currentPage)
    {
        if (isForge())
        {
            if (isCommonGroup(group))
            {
                return currentPage;
            }
            if (isVanillaGroup(group))
            {
                return 0;
            }
            List<ItemGroup> moddedGroups = groups().stream()
                .filter(g -> !isVanillaGroup(g) && g.getType() != ItemGroup.Type.SEARCH && g.getType() != ItemGroup.Type.INVENTORY)
                .toList();
            int idx = moddedGroups.indexOf(group);
            return idx >= 0 ? (1 + idx / 10) : 0;
        }

        if (isCommonGroup(group))
        {
            return currentPage;
        }

        return getFabricPage(group);
    }

    public static boolean visibleOnPage(ItemGroup group, int page)
    {
        if (isForge())
        {
            if (isCommonGroup(group))
            {
                return true;
            }
            if (page == 0)
            {
                return isVanillaGroup(group);
            }
            return page(group, page) == page;
        }

        return isCommonGroup(group)
            || page(group, page) == page;
    }

    public static int maxPage()
    {
        if (isForge())
        {
            List<ItemGroup> moddedGroups = groups().stream()
                .filter(g -> !isVanillaGroup(g) && g.getType() != ItemGroup.Type.SEARCH && g.getType() != ItemGroup.Type.INVENTORY)
                .toList();
            return moddedGroups.isEmpty() ? 0 : 1 + (moddedGroups.size() - 1) / 10;
        }

        int max = 0;
        int nonCommon = 0;
        for (ItemGroup group : groups())
        {
            if (!isCommonGroup(group))
            {
                max = Math.max(max, page(group, 0));
            }
        }

        /* Match Fabric's pagination threshold before display filtering. The
         * operator tab is hidden by this renderer, but it still occupies a
         * registered page slot and must not make the BBS page inaccessible. */
        for (ItemGroup group : ItemGroups.getGroupsToDisplay())
        {
            if (!isCommonGroup(group))
            {
                nonCommon++;
            }
        }

        int countedPages = Math.max(0, (nonCommon + 9) / 10 - 1);
        return Math.max(max, countedPages);
    }

    public static boolean isSpecial(ItemGroup group, int page)
    {
        if (isForge())
        {
            return isCommonGroup(group);
        }
        return group != null && group.isSpecial();
    }

    public static boolean isTop(ItemGroup group, int page)
    {
        if (isForge())
        {
            if (isCommonGroup(group))
            {
                return group.getType() == ItemGroup.Type.SEARCH;
            }
            if (page == 0)
            {
                List<ItemGroup> vanillaGroups = groups().stream()
                    .filter(CreativeInventoryTabs::isVanillaGroup)
                    .filter(g -> !isCommonGroup(g))
                    .toList();
                int idx = vanillaGroups.indexOf(group);
                if (idx >= 0)
                {
                    return idx < 5;
                }
            }
            else
            {
                List<ItemGroup> moddedGroups = groups().stream()
                    .filter(g -> !isVanillaGroup(g) && !isCommonGroup(g))
                    .toList();
                int pageStart = (page - 1) * 10;
                int idx = moddedGroups.indexOf(group) - pageStart;
                if (idx >= 0 && idx < 10)
                {
                    return idx < 5;
                }
            }
        }
        return group != null && group.getRow() == ItemGroup.Row.TOP;
    }

    public static int getColumn(ItemGroup group, int page)
    {
        if (isForge())
        {
            if (isCommonGroup(group))
            {
                return 6;
            }
            if (page == 0)
            {
                List<ItemGroup> vanillaGroups = groups().stream()
                    .filter(CreativeInventoryTabs::isVanillaGroup)
                    .filter(g -> !isCommonGroup(g))
                    .toList();
                int idx = vanillaGroups.indexOf(group);
                if (idx >= 0)
                {
                    return idx % 5;
                }
            }
            else
            {
                List<ItemGroup> moddedGroups = groups().stream()
                    .filter(g -> !isVanillaGroup(g) && !isCommonGroup(g))
                    .toList();
                int pageStart = (page - 1) * 10;
                int idx = moddedGroups.indexOf(group) - pageStart;
                if (idx >= 0 && idx < 10)
                {
                    return idx % 5;
                }
            }
        }
        return group == null ? 0 : net.minecraft.util.math.MathHelper.clamp(group.getColumn(), 0, 6);
    }
}

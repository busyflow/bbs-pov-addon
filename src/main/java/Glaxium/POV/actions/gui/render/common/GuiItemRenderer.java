package Glaxium.POV.actions.gui.render.common;

import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;

/** Item-group display context and item-stack drawing for GUI clips. */
public final class GuiItemRenderer
{
    private static boolean itemGroupsPopulated = false;

    private GuiItemRenderer()
    {
    }

    public static void drawSlotItem(Batcher2D batcher, ItemStack stack, int x, int y)
    {
        GuiSlotRenderer.drawSlotItem(batcher, stack, x, y);
    }

    public static void ensureItemGroupsPopulated()
    {
        if (itemGroupsPopulated)
        {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null)
        {
            try
            {
                ItemGroups.updateDisplayContext(client.world.getEnabledFeatures(), true, client.world.getRegistryManager());
                itemGroupsPopulated = true;
            }
            catch (Exception ignored)
            {
            }
        }
    }
}

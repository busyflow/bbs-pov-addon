package Glaxium.POV.actions.gui.render.screen;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.render.common.GuiStandardLayoutRenderer;
import Glaxium.POV.actions.gui.render.GuiRenderContext;
import Glaxium.POV.actions.gui.render.GuiRenderer;
import Glaxium.POV.actions.gui.render.GuiScreenChrome;
import Glaxium.POV.actions.gui.render.common.GuiEntityPreviewRenderer;
import Glaxium.POV.actions.gui.render.common.GuiSlotRenderer;
import Glaxium.POV.integration.access.minecraft.AbstractHorseEntityPovAccess;
import Glaxium.POV.integration.access.minecraft.HorseEntityPovAccess;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

/** Horse / donkey mount screen clip renderer. Saved type ids remain {@code horse} and {@code donkey}. */
public final class MountGuiRenderer implements GuiRenderer, GuiScreenChrome
{
    public static final MountGuiRenderer INSTANCE = new MountGuiRenderer();

    private static final int HORSE_SADDLED_FLAG = 4;
    private static HorseEntity previewHorse;
    private static DonkeyEntity previewDonkey;

    private MountGuiRenderer()
    {
    }

    @Override
    public void render(GuiRenderContext ctx)
    {
        GuiStandardLayoutRenderer.render(ctx, this);
    }

    @Override
    public void drawPreview(GuiRenderContext ctx)
    {
        if (ctx.skipEntityPreview)
        {
            return;
        }
        drawMount(ctx);
    }

    private static void drawMount(GuiRenderContext ctx)
    {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null)
        {
            return;
        }

        AbstractHorseEntity mount = "donkey".equals(ctx.guiId)
            ? donkeyPreview(client.world)
            : horsePreview(client.world);
        if (mount == null)
        {
            return;
        }

        applyMountPreviewState(mount, ctx.clip, ctx.guiId, ctx.localTick);

        float centerX = 51F;
        float centerY = 60F;
        int size = 17;
        float lookX = ctx.cursorVisible ? ctx.cursorGuiX : centerX;
        float lookY = ctx.cursorVisible ? ctx.cursorGuiY : (centerY - 36F);

        try
        {
            ctx.batcher.flush();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
            GuiEntityPreviewRenderer.draw(
                ctx.batcher.getContext(),
                centerX,
                centerY,
                size,
                10F / 17F,
                lookX,
                lookY,
                mount);
            ctx.batcher.flush();
        }
        catch (Exception ignored)
        {
        }
        finally
        {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
        }
    }


    private static HorseEntity horsePreview(World world)
    {
        if (previewHorse == null || previewHorse.getWorld() != world)
        {
            previewHorse = EntityType.HORSE.create(world);
            if (previewHorse != null)
            {
                previewHorse.setSilent(true);
            }
        }

        return previewHorse;
    }

    private static DonkeyEntity donkeyPreview(World world)
    {
        if (previewDonkey == null || previewDonkey.getWorld() != world)
        {
            previewDonkey = EntityType.DONKEY.create(world);
            if (previewDonkey != null)
            {
                previewDonkey.setSilent(true);
            }
        }

        return previewDonkey;
    }

    private static void applyMountPreviewState(
        AbstractHorseEntity mount,
        GuiPovActionClip clip,
        String guiId,
        float tick)
    {
        if (mount instanceof HorseEntity horse)
        {
            ((HorseEntityPovAccess) horse).bbsPov$setHorseVariant(
                GuiPovActionClip.packHorseVariant(clip.sampleHorseVariant(guiId, tick)));
        }

        if (mount instanceof DonkeyEntity donkey)
        {
            donkey.setHasChest(clip.isMountChestOpen(guiId, tick));
        }

        ItemStack saddle = GuiSlotRenderer.sampleSlot(clip, guiId, "saddle", tick);
        boolean hasSaddle = saddle != null && !saddle.isEmpty();
        AbstractHorseEntityPovAccess access = (AbstractHorseEntityPovAccess) mount;
        access.bbsPov$setHorseFlag(HORSE_SADDLED_FLAG, hasSaddle);

        SimpleInventory items = access.bbsPov$getItems();
        if (items != null)
        {
            items.setStack(0, hasSaddle ? saddle.copy() : ItemStack.EMPTY);
        }

        if (mount instanceof HorseEntity)
        {
            ItemStack armor = GuiSlotRenderer.sampleSlot(clip, guiId, "armor", tick);
            ItemStack worn = armor == null || armor.isEmpty() ? ItemStack.EMPTY : armor.copy();
            mount.equipStack(EquipmentSlot.CHEST, worn);
            if (items != null && items.size() > 1)
            {
                items.setStack(1, worn.copy());
            }
        }
    }

}

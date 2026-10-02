package Glaxium.POV.actions.gui.render.screen;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.render.common.GuiStandardLayoutRenderer;
import Glaxium.POV.actions.gui.render.GuiRenderContext;
import Glaxium.POV.actions.gui.render.GuiRenderer;
import Glaxium.POV.actions.gui.render.GuiScreenChrome;
import Glaxium.POV.actions.gui.render.common.GuiEntityPreviewRenderer;
import Glaxium.POV.actions.gui.render.common.GuiSlotRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

/** Smithing-table clip renderer. Saved type id remains {@code smithing_table}. */
public final class SmithingGuiRenderer implements GuiRenderer, GuiScreenChrome
{
    public static final SmithingGuiRenderer INSTANCE = new SmithingGuiRenderer();

    private static final Quaternionf SMITHING_ARMOR_STAND_ROTATION =
        new Quaternionf().rotationXYZ(0.43633232F, 0F, (float) Math.PI);

    private SmithingGuiRenderer()
    {
    }

    @Override
    public void render(GuiRenderContext ctx)
    {
        GuiStandardLayoutRenderer.render(ctx, this);
    }

    @Override
    public void drawEarlyChrome(GuiRenderContext ctx)
    {
        ItemStack template = GuiSlotRenderer.sampleSlot(ctx.clip, "smithing_table", "template", ctx.localTick);
        ItemStack base = GuiSlotRenderer.sampleSlot(ctx.clip, "smithing_table", "base", ctx.localTick);
        ItemStack addition = GuiSlotRenderer.sampleSlot(ctx.clip, "smithing_table", "addition", ctx.localTick);
        ItemStack result = GuiSlotRenderer.sampleSlot(ctx.clip, "smithing_table", "result", ctx.localTick);
        boolean hasInput = (!template.isEmpty() || !base.isEmpty() || !addition.isEmpty());
        boolean hasInvalidRecipe = hasInput && result.isEmpty();
        if (hasInvalidRecipe)
        {
            ctx.batcher.getContext().drawTexture(
                ctx.entry.texture,
                65,
                46,
                176F,
                0F,
                28,
                21,
                256,
                256);
        }
    }

    @Override
    public void drawPreview(GuiRenderContext ctx)
    {
        if (ctx.skipEntityPreview)
        {
            return;
        }
        drawArmorStand(ctx);
    }

    private static void drawArmorStand(GuiRenderContext ctx)
    {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null)
        {
            return;
        }

        ArmorStandEntity stand = new ArmorStandEntity(client.world, 0D, 0D, 0D);
        stand.setHideBasePlate(true);
        stand.setShowArms(true);
        stand.bodyYaw = 210F;
        stand.setPitch(25F);
        stand.headYaw = stand.getYaw();
        stand.prevHeadYaw = stand.getYaw();
        for (EquipmentSlot slot : EquipmentSlot.values())
        {
            stand.equipStack(slot, ItemStack.EMPTY);
        }

        ItemStack output = GuiSlotRenderer.sampleSlot(ctx.clip, "smithing_table", "result", ctx.localTick);
        if (output != null && !output.isEmpty())
        {
            ItemStack copy = output.copy();
            if (copy.getItem() instanceof ArmorItem armor)
            {
                stand.equipStack(armor.getSlotType(), copy);
            }
            else
            {
                stand.equipStack(EquipmentSlot.OFFHAND, copy);
            }
        }

        try
        {
            ctx.batcher.flush();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
            InventoryScreen.drawEntity(
                ctx.batcher.getContext(),
                141,
                75,
                25,
                new Vector3f(),
                SMITHING_ARMOR_STAND_ROTATION,
                null,
                stand);
            ctx.batcher.getContext().draw();
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

}

package Glaxium.POV.actions.gui.render.screen;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.render.common.GuiStandardLayoutRenderer;
import Glaxium.POV.actions.gui.render.GuiRenderContext;
import Glaxium.POV.actions.gui.render.GuiRenderer;
import Glaxium.POV.actions.gui.render.GuiScreenChrome;
import Glaxium.POV.actions.gui.render.common.GuiSlotRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.block.entity.BannerPatterns;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.block.entity.BannerBlockEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BannerItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.LoomScreenHandler;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

import java.util.List;

/** Loom clip renderer. Saved type id remains {@code loom}. */
public final class LoomGuiRenderer implements GuiRenderer, GuiScreenChrome
{
    public static final LoomGuiRenderer INSTANCE = new LoomGuiRenderer();

    private static ModelPart loomBannerField;

    private LoomGuiRenderer()
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
        float cursorX = ctx.cursorVisible ? ctx.cursorGuiX : -1000F;
        float cursorY = ctx.cursorVisible ? ctx.cursorGuiY : -1000F;
        drawChrome(
            ctx.batcher,
            ctx.clip,
            ctx.localTick,
            ctx.originX,
            ctx.originY,
            ctx.scaleX,
            ctx.scaleY,
            cursorX,
            cursorY);
    }

    private static void drawChrome(
        Batcher2D batcher,
        GuiPovActionClip clip,
        float tick,
        float originX,
        float originY,
        float scaleX,
        float scaleY,
        float cursorX,
        float cursorY)
    {
        ItemStack banner = GuiSlotRenderer.sampleSlot(clip, "loom", "banner", tick);
        ItemStack dye = GuiSlotRenderer.sampleSlot(clip, "loom", "dye", tick);
        ItemStack patternItem = GuiSlotRenderer.sampleSlot(clip, "loom", "pattern", tick);
        ItemStack output = GuiSlotRenderer.sampleSlot(clip, "loom", "result", tick);
        boolean canApply = banner.getItem() instanceof BannerItem
            && dye.getItem() instanceof DyeItem
            && BannerBlockEntity.getPatternCount(banner) < 6;
        List<RegistryEntry<BannerPattern>> patterns = canApply
            ? getLoomPatterns(banner, dye, patternItem)
            : List.of();
        int selected = findSelectedLoomPattern(patterns, output);
        int rows = MathHelper.ceilDiv(patterns.size(), 4);
        int maxTopRow = Math.max(0, rows - 4);
        int fallbackRow = selected < 0 ? 0 : selected / 4;
        int topRow = MathHelper.clamp(clip.loomRow.interpolate(tick, fallbackRow), 0, maxTopRow);
        int scrollY = maxTopRow == 0 ? 0 : Math.round(topRow * 41F / maxTopRow);
        float screenScale = Math.min(scaleX, scaleY);

        batcher.getContext().drawGuiTexture(
            new Identifier(canApply ? "container/loom/scroller" : "container/loom/scroller_disabled"),
            119,
            13 + scrollY,
            12,
            15);

        int firstPattern = topRow * 4;
        int shown = Math.min(16, patterns.size() - firstPattern);
        for (int i = 0; i < shown; i++)
        {
            int patternIndex = firstPattern + i;
            int x = 60 + (i % 4) * 14;
            int y = 13 + (i / 4) * 14;
            boolean hover = cursorX >= x && cursorX < x + 14 && cursorY >= y && cursorY < y + 14;
            String background = patternIndex == selected
                ? "container/loom/pattern_selected"
                : hover
                    ? "container/loom/pattern_highlighted"
                    : "container/loom/pattern";
            batcher.getContext().drawGuiTexture(new Identifier(background), x, y, 14, 14);
        }

        batcher.getContext().draw();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
        DiffuseLighting.disableGuiDepthLighting();

        for (int i = 0; i < shown; i++)
        {
            int x = 60 + (i % 4) * 14;
            int y = 13 + (i / 4) * 14;
            renderLoomPattern(
                batcher,
                patterns.get(firstPattern + i),
                originX + x * scaleX,
                originY + y * scaleY,
                6F * screenScale,
                screenScale);
        }

        if (output.getItem() instanceof BannerItem outputBanner)
        {
            NbtList patternNbt = BannerBlockEntity.getPatternListNbt(output);
            List<com.mojang.datafixers.util.Pair<RegistryEntry<BannerPattern>, DyeColor>> outputPatterns =
                BannerBlockEntity.getPatternsFromNbt(
                    outputBanner.getColor(),
                    patternNbt == null ? new NbtList() : patternNbt);
            renderBannerCanvas(
                batcher,
                outputPatterns,
                originX + 139F * scaleX,
                originY + 52F * scaleY,
                24F * screenScale,
                screenScale,
                false);
        }

        batcher.getContext().draw();
        DiffuseLighting.enableGuiDepthLighting();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
    }

    private static List<RegistryEntry<BannerPattern>> getLoomPatterns(
        ItemStack banner,
        ItemStack dye,
        ItemStack pattern)
    {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null)
        {
            return List.of();
        }

        LoomScreenHandler handler = new LoomScreenHandler(0, player.getInventory());
        handler.getBannerSlot().setStack(banner.copy());
        handler.getDyeSlot().setStack(dye.copy());
        handler.getPatternSlot().setStack(pattern.copy());
        handler.onContentChanged(handler.getBannerSlot().inventory);
        return List.copyOf(handler.getBannerPatterns());
    }

    private static int findSelectedLoomPattern(
        List<RegistryEntry<BannerPattern>> patterns,
        ItemStack output)
    {
        var patternNbt = BannerBlockEntity.getPatternListNbt(output);
        if (patternNbt == null || patternNbt.isEmpty())
        {
            return -1;
        }

        String selectedId = patternNbt.getCompound(patternNbt.size() - 1).getString("Pattern");
        for (int i = 0; i < patterns.size(); i++)
        {
            if (patterns.get(i).value().getId().equals(selectedId))
            {
                return i;
            }
        }

        return -1;
    }

    private static void renderLoomPattern(
        Batcher2D batcher,
        RegistryEntry<BannerPattern> pattern,
        float x,
        float y,
        float scale,
        float screenScale)
    {
        NbtCompound nbt = new NbtCompound();
        nbt.put(
            BannerBlockEntity.PATTERNS_KEY,
            new BannerPattern.Patterns()
                .add(BannerPatterns.BASE, DyeColor.GRAY)
                .add(pattern, DyeColor.WHITE)
                .toNbt());
        ItemStack stack = new ItemStack(Items.GRAY_BANNER);
        BlockItem.setBlockEntityNbt(stack, BlockEntityType.BANNER, nbt);
        renderBannerCanvas(
            batcher,
            BannerBlockEntity.getPatternsFromNbt(
                DyeColor.GRAY,
                BannerBlockEntity.getPatternListNbt(stack)),
            x,
            y,
            scale,
            screenScale,
            true);
    }

    private static void renderBannerCanvas(
        Batcher2D batcher,
        List<com.mojang.datafixers.util.Pair<RegistryEntry<BannerPattern>, DyeColor>> patterns,
        float x,
        float y,
        float scale,
        float screenScale,
        boolean thumbnail)
    {
        if (loomBannerField == null)
        {
            loomBannerField = MinecraftClient.getInstance()
                .getEntityModelLoader()
                .getModelPart(EntityModelLayers.BANNER)
                .getChild("flag");
        }

        MatrixStack matrices = new MatrixStack();
        matrices.push();

        if (thumbnail)
        {
            matrices.translate(x + 0.5F * screenScale, y + 16F * screenScale, 0F);
        }
        else
        {
            matrices.translate(x, y, 0F);
        }

        matrices.scale(scale, -scale, 1F);

        if (thumbnail)
        {
            matrices.translate(0.5F, 0.5F, 0F);
        }

        matrices.translate(0.5F, 0.5F, 0.5F);
        matrices.scale(0.6666667F, -0.6666667F, -0.6666667F);
        loomBannerField.pitch = 0F;
        loomBannerField.pivotY = -32F;
        BannerBlockEntityRenderer.renderCanvas(
            matrices,
            batcher.getContext().getVertexConsumers(),
            15728880,
            OverlayTexture.DEFAULT_UV,
            loomBannerField,
            ModelLoader.BANNER_BASE,
            true,
            patterns);
        matrices.pop();
    }


}

package Glaxium.POV.actions.toast.render;

import Glaxium.POV.actions.PovActionType;
import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.actions.clip.PovActionClip;
import Glaxium.POV.actions.clip.ToastPovActionClip;
import Glaxium.POV.actions.toast.ToastPresets;
import Glaxium.POV.actions.toast.ToastTypeEntry;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Renders vanilla-accurate Toast notifications with smooth sliding animations. */
public final class ToastActionRenderer
{
    private static final Identifier TOASTS_TEXTURE = new Identifier("textures/gui/toasts.png");

    private ToastActionRenderer()
    {
    }

    public static void renderHUD(Batcher2D batcher, RecordedPovActions actions, float tick, int width, int height)
    {
        if (actions == null)
        {
            return;
        }

        DrawContext context = batcher.getContext();
        if (context == null)
        {
            return;
        }

        List<ToastPovActionClip> activeClips = new ArrayList<>();
        for (PovActionClip clip : actions.getClips(ToastPovActionClip.class))
        {
            if (clip instanceof ToastPovActionClip toastClip && toastClip.isActive(tick))
            {
                activeClips.add(toastClip);
            }
        }

        if (activeClips.isEmpty())
        {
            return;
        }

        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        int baseLayer = PovActionType.TOASTS.seedLayer();

        for (ToastPovActionClip clip : activeClips)
        {
            float elapsed = tick - clip.tick.get();
            int duration = clip.duration.get();

            if (elapsed < 0F || elapsed > duration)
            {
                continue;
            }

            float slide = calculateSlideProgress(elapsed, duration);
            if (slide <= 0.001F)
            {
                continue;
            }

            int toastW = 160;
            int toastH = 32;
            int x = width - (int) (toastW * slide);
            int slot = Math.max(0, Math.min(4, clip.layer.get() - baseLayer));
            int y = 4 + slot * 34;

            renderSingleToast(context, font, clip, x, y);
        }
    }

    public static float calculateSlideProgress(float elapsed, int duration)
    {
        if (duration <= 0)
        {
            return 0F;
        }

        int slideIn = Math.max(4, Math.min(15, Math.round(duration * 0.15F)));
        int slideOut = Math.max(4, Math.min(15, Math.round(duration * 0.15F)));

        if (duration < slideIn + slideOut)
        {
            slideIn = duration / 2;
            slideOut = duration - slideIn;
        }

        if (elapsed < slideIn)
        {
            float p = elapsed / (float) Math.max(1, slideIn);
            return MathHelper.clamp(p * p, 0F, 1F);
        }
        else if (elapsed > duration - slideOut)
        {
            float p = (duration - elapsed) / (float) Math.max(1, slideOut);
            return MathHelper.clamp(p * p, 0F, 1F);
        }
        else
        {
            return 1.0F;
        }
    }

    public static void renderSingleToast(
        DrawContext context,
        TextRenderer font,
        ToastPovActionClip clip,
        int x,
        int y)
    {
        String frameType = clip.getEffectiveFrameType();
        int vOffset = getVOffsetForFrameType(frameType);

        RenderSystem.enableBlend();
        context.drawTexture(TOASTS_TEXTURE, x, y, 0, vOffset, 160, 32);

        // Draw Icon or Custom Texture
        mchorse.bbs_mod.resources.Link customTex = clip.getCustomTexture();
        if (customTex != null)
        {
            Texture tex = BBSModClient.getTextures().getTexture(customTex);
            if (tex != null && tex.isValid() && tex.id > 0)
            {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderTexture(0, tex.id);
                RenderSystem.setShader(GameRenderer::getPositionTexProgram);
                Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
                BufferBuilder builder = Tessellator.getInstance().getBuffer();
                builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
                builder.vertex(matrix, x + 8, y + 8 + 16, 0).texture(0F, 1F).next();
                builder.vertex(matrix, x + 8 + 16, y + 8 + 16, 0).texture(1F, 1F).next();
                builder.vertex(matrix, x + 8 + 16, y + 8, 0).texture(1F, 0F).next();
                builder.vertex(matrix, x + 8, y + 8, 0).texture(0F, 0F).next();
                BufferRenderer.drawWithGlobalProgram(builder.end());
            }
        }
        else
        {
            String iconId = clip.getEffectiveIcon();
            ItemStack stack = createItemStack(iconId);

            if ("recipe".equalsIgnoreCase(frameType))
            {
                // Vanilla RecipeToast renders a scaled crafting table in the corner
                context.getMatrices().push();
                context.getMatrices().translate(x, y, 0);
                context.getMatrices().scale(0.6F, 0.6F, 1.0F);
                renderItem(context, new ItemStack(Items.CRAFTING_TABLE), 3, 3);
                context.getMatrices().pop();
            }

            renderItem(context, stack, x + 8, y + 8);
        }

        // Draw Title & Description
        net.minecraft.text.Text titleText = clip.getEffectiveTitleText();
        net.minecraft.text.Text descText = clip.getEffectiveDescriptionText();
        int titleColor = getTitleColor(frameType);
        int descColor = "recipe".equalsIgnoreCase(frameType) ? 0xFF000000 : 0xFFFFFFFF;

        context.drawText(font, titleText, x + 30, y + 7, titleColor, false);
        context.drawText(font, descText, x + 30, y + 18, descColor, false);
    }

    private static void renderItem(DrawContext context, ItemStack stack, int x, int y)
    {
        if (stack == null || stack.isEmpty())
        {
            return;
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515); // GL_LEQUAL
        RenderSystem.depthMask(true);
        context.drawItem(stack, x, y);
        RenderSystem.disableDepthTest();
    }

    private static int getVOffsetForFrameType(String frameType)
    {
        if ("recipe".equalsIgnoreCase(frameType) || "challenge".equalsIgnoreCase(frameType))
        {
            return 32;
        }
        if ("system".equalsIgnoreCase(frameType))
        {
            return 64;
        }
        if ("tutorial".equalsIgnoreCase(frameType))
        {
            return 96;
        }
        return 0;
    }

    private static int getTitleColor(String frameType)
    {
        if ("challenge".equalsIgnoreCase(frameType))
        {
            return 0xFFFF55FF; // Purple
        }
        if ("goal".equalsIgnoreCase(frameType))
        {
            return 0xFF55FFFF; // Aqua
        }
        if ("recipe".equalsIgnoreCase(frameType))
        {
            return 0xFF500050; // Dark Purple (vanilla recipe color -11534256)
        }
        if ("system".equalsIgnoreCase(frameType))
        {
            return 0xFFFFFFFF; // White
        }
        return 0xFFFFFF55; // Yellow (Task / Tutorial)
    }

    private static ItemStack createItemStack(String iconId)
    {
        if (iconId != null && !iconId.isEmpty())
        {
            try
            {
                Item item = Registries.ITEM.get(new Identifier(iconId));
                if (item != null && item != Items.AIR)
                {
                    return new ItemStack(item);
                }
            }
            catch (Exception ignored)
            {}
        }
        return new ItemStack(Items.DIAMOND);
    }
}

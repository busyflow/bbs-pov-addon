package Glaxium.POV.actions.screeneffect.render;

import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.actions.clip.PovActionClip;
import Glaxium.POV.actions.clip.ScreenEffectPovActionClip;
import Glaxium.POV.actions.screeneffect.ScreenEffectPresets;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import Glaxium.POV.camera.clip.PovCameraClip;
import Glaxium.POV.camera.clip.PovCameraClips;
import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.replay.PovReplaySettings;
import Glaxium.POV.camera.PovCameraMode;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.controller.UIFilmController;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Renders keyframe-driven vanilla Screen Effects (Vignette, Fire, Frost, Portal, Pumpkin, Spyglass, etc.) */
public final class ScreenEffectActionRenderer
{
    private static final Identifier VIGNETTE_TEX = new Identifier("textures/misc/vignette.png");
    private static final Identifier SPYGLASS_TEX = new Identifier("textures/misc/spyglass_scope.png");
    private static final Identifier FROST_TEX = new Identifier("textures/misc/powder_snow_outline.png");
    private static final Identifier PUMPKIN_TEX = new Identifier("textures/misc/pumpkinblur.png");
    private static final Identifier UNDERWATER_TEX = new Identifier("textures/misc/underwater.png");

    private ScreenEffectActionRenderer()
    {
    }

    public static void render(
        MatrixStack matrices,
        Batcher2D batcher,
        RecordedPovActions actions,
        float tick,
        int width,
        int height)
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

        List<ScreenEffectPovActionClip> activeClips = new ArrayList<>();
        for (PovActionClip clip : actions.getClips(ScreenEffectPovActionClip.class))
        {
            if (clip instanceof ScreenEffectPovActionClip effectClip && effectClip.isActive(tick))
            {
                activeClips.add(effectClip);
            }
        }

        if (activeClips.isEmpty())
        {
            return;
        }

        batcher.flush();
        context.draw();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.gameRenderer == null || client.gameRenderer.getCamera() == null || client.gameRenderer.getCamera().getSubmersionType() != net.minecraft.client.render.CameraSubmersionType.LAVA)
        {
            net.minecraft.client.render.BackgroundRenderer.clearFog();
        }

        List<ActiveEffect> effectsToRender = new ArrayList<>();
        for (ScreenEffectPovActionClip clip : activeClips)
        {
            float elapsed = tick - clip.tick.get();
            int duration = clip.duration.get();

            if (elapsed < 0F || elapsed > duration)
            {
                continue;
            }

            for (String effectId : clip.getActiveEffectList())
            {
                effectsToRender.add(new ActiveEffect(clip, effectId, elapsed));
            }
        }

        effectsToRender.sort(java.util.Comparator.comparingInt(e -> getEffectLayer(e.effectId())));

        for (ActiveEffect effect : effectsToRender)
        {
            renderEffect(context, effect.clip(), effect.effectId(), effect.elapsed(), width, height);
        }

        context.draw();
        batcher.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
    }

    private static int getEffectLayer(String effectId)
    {
        return switch (effectId.toLowerCase())
        {
            case "suffocation" -> 10;
            case "underwater" -> 15;
            case "pumpkin" -> 20;
            case "spyglass" -> 25;
            case "frost" -> 30;
            case "portal" -> 40;
            case "fire" -> 50;
            case "vignette" -> 60;
            case "totem" -> 80;
            default -> 100;
        };
    }

    private record ActiveEffect(ScreenEffectPovActionClip clip, String effectId, float elapsed) {}

    private static void renderEffect(
        DrawContext context,
        ScreenEffectPovActionClip clip,
        String effectId,
        float elapsed,
        int width,
        int height)
    {
        switch (effectId.toLowerCase())
        {
            case "vignette" -> renderVignette(context, clip, elapsed, width, height);
            case "underwater" -> renderUnderwater(context, clip, elapsed, width, height);
            case "night_vision" -> renderNightVision(context, clip, elapsed, width, height);
            case "blindness" -> renderBlindness(context, clip, elapsed, width, height);
            case "darkness" -> renderDarkness(context, clip, elapsed, width, height);
            case "spyglass" -> renderSpyglass(context, clip, elapsed, width, height);
            case "frost" -> renderFrost(context, clip, elapsed, width, height);
            case "portal" -> renderPortal(context, clip, elapsed, width, height);
            case "fire" -> renderFire(context, clip, elapsed, width, height);
            case "pumpkin" -> renderPumpkin(context, clip, elapsed, width, height);
            case "suffocation" -> renderSuffocation(context, clip, elapsed, width, height);
            case "totem" -> renderTotem(context, clip, elapsed, width, height);
        }
    }

    private static void renderVignette(DrawContext context, ScreenEffectPovActionClip clip, float elapsed, int width, int height)
    {
        boolean isVisible = clip.vignetteVisible.isEmpty() ? true : clip.vignetteVisible.interpolate(elapsed);
        if (!isVisible) return;

        float intensity = clip.vignetteOpacity.isEmpty() ? 0.5F : clip.vignetteOpacity.interpolate(elapsed);
        if (intensity <= 0.001F) return;

        Color color = clip.vignetteColor.isEmpty() ? null : clip.vignetteColor.interpolate(elapsed);
        float r = color != null ? color.r : 0F;
        float g = color != null ? color.g : 0F;
        float b = color != null ? color.b : 0F;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
            com.mojang.blaze3d.platform.GlStateManager.SrcFactor.ZERO,
            com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE_MINUS_SRC_COLOR,
            com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA,
            com.mojang.blaze3d.platform.GlStateManager.DstFactor.ZERO);

        if (r == 0F && g == 0F && b == 0F)
        {
            RenderSystem.setShaderColor(intensity, intensity, intensity, 1.0F);
        }
        else
        {
            RenderSystem.setShaderColor(r * intensity, g * intensity, b * intensity, 1.0F);
        }

        context.drawTexture(VIGNETTE_TEX, 0, 0, width, height, 0F, 0F, 256, 256, 256, 256);
        context.draw();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
    }

    private static void renderUnderwater(DrawContext context, ScreenEffectPovActionClip clip, float elapsed, int width, int height)
    {
        if (!isFirstPerson()) return;

        boolean isVisible = clip.underwaterVisible.isEmpty() ? true : clip.underwaterVisible.interpolate(elapsed);
        if (!isVisible) return;

        float intensity = clip.underwaterOpacity.isEmpty() ? 0.1F : clip.underwaterOpacity.interpolate(elapsed);
        if (intensity <= 0.001F) return;

        Color tint = clip.underwaterColor.get();
        float tr = tint != null ? tint.r : 1.0F;
        float tg = tint != null ? tint.g : 1.0F;
        float tb = tint != null ? tint.b : 1.0F;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(tr, tg, tb, intensity);
        context.drawTexture(UNDERWATER_TEX, 0, 0, width, height, 0F, 0F, 256, 256, 256, 256);
        context.draw();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static void renderSpyglass(DrawContext context, ScreenEffectPovActionClip clip, float elapsed, int width, int height)
    {
        if (!isFirstPerson()) return;

        boolean isVisible = clip.spyglassVisible.isEmpty() ? true : clip.spyglassVisible.interpolate(elapsed);
        if (!isVisible) return;

        float scale = clip.spyglassScale.isEmpty() ? 1.12F : clip.spyglassScale.interpolate(elapsed);

        float f = (float) Math.min(width, height);
        float h = Math.min((float) width / f, (float) height / f) * scale;
        int i = MathHelper.floor(f * h);
        int j = MathHelper.floor(f * h);
        int k = (width - i) / 2;
        int l = (height - j) / 2;
        int m = k + i;
        int n = l + j;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        context.drawTexture(SPYGLASS_TEX, k, l, 0, 0.0F, 0.0F, i, j, i, j);

        context.fill(0, n, width, height, 0xFF000000);
        context.fill(0, 0, width, l, 0xFF000000);
        context.fill(0, l, k, n, 0xFF000000);
        context.fill(m, l, width, n, 0xFF000000);
        context.draw();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static void renderFrost(DrawContext context, ScreenEffectPovActionClip clip, float elapsed, int width, int height)
    {
        boolean isVisible = clip.frostVisible.isEmpty() ? true : clip.frostVisible.interpolate(elapsed);
        if (!isVisible) return;

        float intensity = clip.frostProgress.isEmpty() ? 1.0F : clip.frostProgress.interpolate(elapsed);
        if (intensity <= 0.001F) return;

        Color tint = clip.frostColor.get();
        float tr = tint != null ? tint.r : 1.0F;
        float tg = tint != null ? tint.g : 1.0F;
        float tb = tint != null ? tint.b : 1.0F;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(tr, tg, tb, intensity);
        context.drawTexture(FROST_TEX, 0, 0, width, height, 0F, 0F, 256, 256, 256, 256);
        context.draw();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static void renderPortal(DrawContext context, ScreenEffectPovActionClip clip, float elapsed, int width, int height)
    {
        boolean isVisible = clip.portalVisible.isEmpty() ? true : clip.portalVisible.interpolate(elapsed);
        if (!isVisible) return;

        float intensity = clip.portalOpacity.isEmpty() ? 1.0F : clip.portalOpacity.interpolate(elapsed);
        if (intensity <= 0.001F) return;

        Sprite sprite = MinecraftClient.getInstance()
            .getBlockRenderManager()
            .getModels()
            .getModelParticleSprite(Blocks.NETHER_PORTAL.getDefaultState());

        if (sprite == null) return;

        Color tint = clip.portalColor.get();
        float tr = tint != null ? tint.r : 1.0F;
        float tg = tint != null ? tint.g : 1.0F;
        float tb = tint != null ? tint.b : 1.0F;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        context.setShaderColor(tr, tg, tb, intensity);
        context.drawSprite(0, 0, 0, width, height, sprite);
        context.draw();
        context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static void renderFire(DrawContext context, ScreenEffectPovActionClip clip, float elapsed, int width, int height)
    {
        if (!isFirstPerson()) return;

        boolean isVisible = clip.fireVisible.isEmpty() ? true : clip.fireVisible.interpolate(elapsed);
        if (!isVisible) return;

        Sprite sprite = net.minecraft.client.render.model.ModelLoader.FIRE_1.getSprite();
        if (sprite == null) return;

        Color tint = clip.fireColor.get();
        float tr = tint != null ? tint.r : 1.0F;
        float tg = tint != null ? tint.g : 1.0F;
        float tb = tint != null ? tint.b : 1.0F;

        Matrix4f prevProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        float aspect = width / (float) Math.max(1, height);
        Matrix4f fireProjection = new Matrix4f().setPerspective((float) Math.toRadians(70.0), aspect, 0.05F, 100.0F);
        RenderSystem.setProjectionMatrix(fireProjection, com.mojang.blaze3d.systems.VertexSorter.BY_Z);

        MatrixStack fireMatrices = new MatrixStack();
        fireMatrices.loadIdentity();

        RenderSystem.setShader(GameRenderer::getPositionColorTexProgram);
        RenderSystem.depthFunc(519);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, sprite.getAtlasId());

        float f = sprite.getMinU();
        float g = sprite.getMaxU();
        float h = (f + g) / 2.0F;
        float i = sprite.getMinV();
        float j = sprite.getMaxV();
        float k = (i + j) / 2.0F;
        float l = sprite.getAnimationFrameDelta();
        float m = MathHelper.lerp(l, f, h);
        float n = MathHelper.lerp(l, g, h);
        float o = MathHelper.lerp(l, i, k);
        float p = MathHelper.lerp(l, j, k);

        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        float alpha = 0.9F;

        for (int r = 0; r < 2; ++r)
        {
            fireMatrices.push();
            float s = -((float) (r * 2 - 1)) * 0.24F;
            float t = -0.3F;
            float u = 0.0F;
            fireMatrices.translate(s, t, u);
            fireMatrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees((float) (r * 2 - 1) * 10.0F));
            Matrix4f matrix4f = fireMatrices.peek().getPositionMatrix();
            bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE);
            bufferBuilder.vertex(matrix4f, -0.5F, -0.5F, -0.5F).color(tr, tg, tb, alpha).texture(n, p).next();
            bufferBuilder.vertex(matrix4f, 0.5F, -0.5F, -0.5F).color(tr, tg, tb, alpha).texture(m, p).next();
            bufferBuilder.vertex(matrix4f, 0.5F, 0.5F, -0.5F).color(tr, tg, tb, alpha).texture(m, o).next();
            bufferBuilder.vertex(matrix4f, -0.5F, 0.5F, -0.5F).color(tr, tg, tb, alpha).texture(n, o).next();
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
            fireMatrices.pop();
        }

        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(515);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setProjectionMatrix(prevProjection, com.mojang.blaze3d.systems.VertexSorter.BY_Z);
    }

    private static void renderPumpkin(DrawContext context, ScreenEffectPovActionClip clip, float elapsed, int width, int height)
    {
        boolean isVisible = clip.pumpkinVisible.isEmpty() ? true : clip.pumpkinVisible.interpolate(elapsed);
        if (!isVisible) return;

        float intensity = clip.pumpkinOpacity.isEmpty() ? 1.0F : clip.pumpkinOpacity.interpolate(elapsed);
        if (intensity <= 0.001F) return;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, intensity);
        context.drawTexture(PUMPKIN_TEX, 0, 0, width, height, 0F, 0F, 256, 256, 256, 256);
        context.draw();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static void renderSuffocation(DrawContext context, ScreenEffectPovActionClip clip, float elapsed, int width, int height)
    {
        boolean isVisible = clip.suffocationVisible.isEmpty() ? true : clip.suffocationVisible.interpolate(elapsed);
        if (!isVisible)
        {
            return;
        }

        String blockId = clip.suffocationBlock.isEmpty() ? "minecraft:stone" : clip.suffocationBlock.interpolate(elapsed);
        Block block = Blocks.STONE;
        if (blockId != null && !blockId.trim().isEmpty())
        {
            Identifier id = Identifier.tryParse(blockId.contains(":") ? blockId : "minecraft:" + blockId);
            if (id != null)
            {
                if (Registries.BLOCK.containsId(id))
                {
                    block = Registries.BLOCK.get(id);
                }
                else if (Registries.ITEM.containsId(id))
                {
                    Item item = Registries.ITEM.get(id);
                    if (item instanceof BlockItem bi)
                    {
                        block = bi.getBlock();
                    }
                }
            }
        }

        Sprite sprite = MinecraftClient.getInstance()
            .getBlockRenderManager()
            .getModels()
            .getModelParticleSprite(block.getDefaultState());

        if (sprite == null)
        {
            return;
        }

        Matrix4f prevProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        float aspect = width / (float) Math.max(1, height);
        Matrix4f suffocationProjection = new Matrix4f().setPerspective((float) Math.toRadians(70.0), aspect, 0.05F, 100.0F);
        RenderSystem.setProjectionMatrix(suffocationProjection, com.mojang.blaze3d.systems.VertexSorter.BY_Z);

        MatrixStack suffocationMatrices = new MatrixStack();
        suffocationMatrices.loadIdentity();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorTexProgram);
        RenderSystem.setShaderTexture(0, sprite.getAtlasId());

        float l = sprite.getMinU();
        float m = sprite.getMaxU();
        float n = sprite.getMinV();
        float o = sprite.getMaxV();

        float r = 0.1F;
        float g = 0.1F;
        float b = 0.1F;
        float a = 1.0F;

        Matrix4f matrix4f = suffocationMatrices.peek().getPositionMatrix();
        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE);
        builder.vertex(matrix4f, -1.0F, -1.0F, -0.5F).color(r, g, b, a).texture(m, o).next();
        builder.vertex(matrix4f, 1.0F, -1.0F, -0.5F).color(r, g, b, a).texture(l, o).next();
        builder.vertex(matrix4f, 1.0F, 1.0F, -0.5F).color(r, g, b, a).texture(l, n).next();
        builder.vertex(matrix4f, -1.0F, 1.0F, -0.5F).color(r, g, b, a).texture(m, n).next();
        BufferRenderer.drawWithGlobalProgram(builder.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setProjectionMatrix(prevProjection, com.mojang.blaze3d.systems.VertexSorter.BY_Z);
    }

    private static void renderTotem(DrawContext context, ScreenEffectPovActionClip clip, float elapsed, int width, int height)
    {
        boolean isVisible = clip.totemVisible.isEmpty() ? true : clip.totemVisible.interpolate(elapsed);
        if (!isVisible)
        {
            return;
        }

        float progress = clip.totemProgress.isEmpty() ? 0.0F : clip.totemProgress.interpolate(elapsed);
        if (progress <= 0.0001F || progress >= 1.0F)
        {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        String itemId = clip.totemItem.get();
        if (itemId == null || itemId.isEmpty())
        {
            itemId = "minecraft:totem_of_undying";
        }
        ItemStack stack = createItemStack(itemId);

        float f = MathHelper.clamp(progress, 0.0F, 1.0F);
        float g = f * f;
        float h = f * g;
        float j = 10.25F * h * g - 24.95F * g * g + 25.5F * h - 13.8F * g + 4.0F * f;
        float k = j * (float) Math.PI;

        boolean flipped = clip.totemFlipped.isEmpty() ? false : clip.totemFlipped.interpolate(elapsed);

        // Subtle hand-origin offset (matching vanilla floatingItem width/height, flipped for left hand)
        float f10 = (flipped ? 0.5F : -0.5F) * ((float) width / 4.0F);
        float f11 = 0.5F * ((float) height / 4.0F);

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(
            (float)(width / 2) + f10 * MathHelper.abs(MathHelper.sin(k * 2.0F)),
            (float)(height / 2) + f11 * MathHelper.abs(MathHelper.sin(k * 2.0F)),
            -50.0F
        );
        float scale = 50.0F + 175.0F * MathHelper.sin(k);
        matrices.scale(scale, -scale, scale);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((flipped ? -900.0F : 900.0F) * MathHelper.abs(MathHelper.sin(k))));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(6.0F * MathHelper.cos(f * 8.0F)));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((flipped ? -6.0F : 6.0F) * MathHelper.cos(f * 8.0F)));

        net.minecraft.client.render.DiffuseLighting.enableGuiDepthLighting();
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();

        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
        client.getItemRenderer().renderItem(
            stack,
            ModelTransformationMode.FIXED,
            15728880,
            OverlayTexture.DEFAULT_UV,
            matrices,
            immediate,
            client.world,
            0
        );
        immediate.draw();
        matrices.pop();

        RenderSystem.enableCull();
        RenderSystem.disableDepthTest();
        net.minecraft.client.render.DiffuseLighting.disableGuiDepthLighting();
    }

    private static void renderNightVision(DrawContext context, ScreenEffectPovActionClip clip, float elapsed, int width, int height)
    {
        // Night vision is handled authentically via LightmapTextureManager world lightmap/gamma boost, not a 2D quad overlay.
    }

    private static void renderBlindness(DrawContext context, ScreenEffectPovActionClip clip, float elapsed, int width, int height)
    {
        if (!isFirstPerson()) return;

        boolean isVisible = clip.blindnessVisible.isEmpty() ? true : clip.blindnessVisible.interpolate(elapsed);
        if (!isVisible) return;

        float intensity = clip.blindnessOpacity.isEmpty() ? 1.0F : clip.blindnessOpacity.interpolate(elapsed);
        if (intensity <= 0.001F) return;

        // Render soft dark vignette overlay ensuring blindness is visible with Iris shaders
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
            com.mojang.blaze3d.platform.GlStateManager.SrcFactor.ZERO,
            com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE_MINUS_SRC_COLOR,
            com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA,
            com.mojang.blaze3d.platform.GlStateManager.DstFactor.ZERO);
        RenderSystem.setShaderColor(intensity * 0.9F, intensity * 0.9F, intensity * 0.9F, 1.0F);
        context.drawTexture(VIGNETTE_TEX, 0, 0, width, height, 0F, 0F, 256, 256, 256, 256);
        context.draw();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
    }

    private static void renderDarkness(DrawContext context, ScreenEffectPovActionClip clip, float elapsed, int width, int height)
    {
        // Darkness is handled authentically via World Fog & LightmapTextureManager / Iris shader uniforms (fog radius & lightmap pulsation), not a 2D screen overlay.
    }

    public static boolean isFirstPerson()
    {
        MinecraftClient client = MinecraftClient.getInstance();
        PovPlaybackContext.Frame playback = PovPlaybackContext.getActive(client.getTickDelta());
        if (playback != null)
        {
            if (playback.clip() != null)
            {
                return playback.clip().isFirstPerson();
            }
            return client.options.getPerspective().isFirstPerson();
        }

        UIFilmPanel panel = PovReplaySettings.getFilmPanel();
        if (panel != null && panel.getData() != null)
        {
            int povMode = panel.getController().getPovMode();
            if (povMode == PovCameraMode.POV || povMode == UIFilmController.CAMERA_MODE_FIRST_PERSON)
            {
                return true;
            }
            if (povMode == UIFilmController.CAMERA_MODE_THIRD_PERSON_BACK
                || povMode == UIFilmController.CAMERA_MODE_THIRD_PERSON_FRONT
                || povMode == UIFilmController.CAMERA_MODE_FREE
                || povMode == UIFilmController.CAMERA_MODE_ORBIT)
            {
                return false;
            }

            float filmTick = panel.getRunner() != null && panel.getRunner().isRunning()
                ? panel.getRunner().ticks + client.getTickDelta()
                : panel.getCursor();
            PovCameraClip clip = PovCameraClips.resolve(panel.getData(), filmTick);
            if (clip != null)
            {
                return clip.isFirstPerson();
            }
        }

        return client.options.getPerspective().isFirstPerson();
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
        return new ItemStack(Items.TOTEM_OF_UNDYING);
    }
}

package Glaxium.POV.actions.screeneffect.render;

import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.actions.clip.PovActionClip;
import Glaxium.POV.actions.clip.ScreenEffectPovActionClip;
import Glaxium.POV.camera.clip.PovCameraClip;
import Glaxium.POV.camera.clip.PovCameraClips;
import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.replay.PovReplaySettings;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.colors.Color;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public final class ScreenEffectActionRenderer {
   private static final Identifier VIGNETTE_TEX = new Identifier("textures/misc/vignette.png");
   private static final Identifier SPYGLASS_TEX = new Identifier("textures/misc/spyglass_scope.png");
   private static final Identifier FROST_TEX = new Identifier("textures/misc/powder_snow_outline.png");
   private static final Identifier PUMPKIN_TEX = new Identifier("textures/misc/pumpkinblur.png");
   private static final Identifier UNDERWATER_TEX = new Identifier("textures/misc/underwater.png");

   private ScreenEffectActionRenderer() {
   }

   public static void render(MatrixStack matrices, Batcher2D batcher, RecordedPovActions actions, float tick, int width, int height) {
      if (actions != null) {
         DrawContext context = batcher.getContext();
         if (context != null) {
            List<ScreenEffectPovActionClip> activeClips = new ArrayList<>();

            for (PovActionClip clip : actions.getClips(ScreenEffectPovActionClip.class)) {
               if (clip instanceof ScreenEffectPovActionClip) {
                  ScreenEffectPovActionClip effectClip = (ScreenEffectPovActionClip)clip;
                  if (effectClip.isActive(tick)) {
                     activeClips.add(effectClip);
                  }
               }
            }

            if (!activeClips.isEmpty()) {
               batcher.flush();
               context.draw();
               MinecraftClient client = MinecraftClient.getInstance();
               if (client.gameRenderer == null
                  || client.gameRenderer.getCamera() == null
                  || client.gameRenderer.getCamera().getSubmersionType() != CameraSubmersionType.LAVA) {
                  BackgroundRenderer.clearFog();
               }

               List<ScreenEffectActionRenderer.ActiveEffect> effectsToRender = new ArrayList<>();

               for (ScreenEffectPovActionClip clipx : activeClips) {
                  float elapsed = tick - (float)((Integer)clipx.tick.get()).intValue();
                  int duration = (Integer)clipx.duration.get();
                  if (!(elapsed < 0.0F) && !(elapsed > (float)duration)) {
                     for (String effectId : clipx.getActiveEffectList()) {
                        effectsToRender.add(new ScreenEffectActionRenderer.ActiveEffect(clipx, effectId, elapsed));
                     }
                  }
               }

               effectsToRender.sort(Comparator.comparingInt(e -> getEffectLayer(e.effectId())));

               for (ScreenEffectActionRenderer.ActiveEffect effect : effectsToRender) {
                  renderEffect(context, effect.clip(), effect.effectId(), effect.elapsed(), width, height);
               }

               context.draw();
               batcher.flush();
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               RenderSystem.defaultBlendFunc();
            }
         }
      }
   }

   private static int getEffectLayer(String effectId) {
      String var1 = effectId.toLowerCase();

      return switch (var1) {
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

   private static void renderEffect(DrawContext context, ScreenEffectPovActionClip clip, String effectId, float elapsed, int width, int height) {
      String var6 = effectId.toLowerCase();
      switch (var6) {
         case "vignette":
            renderVignette(context, clip, elapsed, width, height);
            break;
         case "underwater":
            renderUnderwater(context, clip, elapsed, width, height);
            break;
         case "night_vision":
            renderNightVision(context, clip, elapsed, width, height);
            break;
         case "blindness":
            renderBlindness(context, clip, elapsed, width, height);
            break;
         case "darkness":
            renderDarkness(context, clip, elapsed, width, height);
            break;
         case "spyglass":
            renderSpyglass(context, clip, elapsed, width, height);
            break;
         case "frost":
            renderFrost(context, clip, elapsed, width, height);
            break;
         case "portal":
            renderPortal(context, clip, elapsed, width, height);
            break;
         case "fire":
            renderFire(context, clip, elapsed, width, height);
            break;
         case "pumpkin":
            renderPumpkin(context, clip, elapsed, width, height);
            break;
         case "suffocation":
            renderSuffocation(context, clip, elapsed, width, height);
            break;
         case "totem":
            renderTotem(context, clip, elapsed, width, height);
      }
   }

   private static void renderVignette(DrawContext context, ScreenEffectPovActionClip clip, float elapsed, int width, int height) {
      boolean isVisible = clip.vignetteVisible.isEmpty() ? true : (Boolean)clip.vignetteVisible.interpolate(elapsed);
      if (isVisible) {
         float intensity = clip.vignetteOpacity.isEmpty() ? 0.5F : (Float)clip.vignetteOpacity.interpolate(elapsed);
         if (!(intensity <= 0.001F)) {
            Color color = clip.vignetteColor.isEmpty() ? null : (Color)clip.vignetteColor.interpolate(elapsed);
            float r = color != null ? color.r : 0.0F;
            float g = color != null ? color.g : 0.0F;
            float b = color != null ? color.b : 0.0F;
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(SrcFactor.ZERO, DstFactor.ONE_MINUS_SRC_COLOR, SrcFactor.SRC_ALPHA, DstFactor.ZERO);
            if (r == 0.0F && g == 0.0F && b == 0.0F) {
               RenderSystem.setShaderColor(intensity, intensity, intensity, 1.0F);
            } else {
               RenderSystem.setShaderColor(r * intensity, g * intensity, b * intensity, 1.0F);
            }

            context.drawTexture(VIGNETTE_TEX, 0, 0, width, height, 0.0F, 0.0F, 256, 256, 256, 256);
            context.draw();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.defaultBlendFunc();
         }
      }
   }

   private static void renderUnderwater(DrawContext context, ScreenEffectPovActionClip clip, float elapsed, int width, int height) {
      if (isFirstPerson()) {
         boolean isVisible = clip.underwaterVisible.isEmpty() ? true : (Boolean)clip.underwaterVisible.interpolate(elapsed);
         if (isVisible) {
            float intensity = clip.underwaterOpacity.isEmpty() ? 0.1F : (Float)clip.underwaterOpacity.interpolate(elapsed);
            if (!(intensity <= 0.001F)) {
               Color tint = (Color)clip.underwaterColor.get();
               float tr = tint != null ? tint.r : 1.0F;
               float tg = tint != null ? tint.g : 1.0F;
               float tb = tint != null ? tint.b : 1.0F;
               RenderSystem.disableDepthTest();
               RenderSystem.depthMask(false);
               RenderSystem.enableBlend();
               RenderSystem.defaultBlendFunc();
               RenderSystem.setShaderColor(tr, tg, tb, intensity);
               context.drawTexture(UNDERWATER_TEX, 0, 0, width, height, 0.0F, 0.0F, 256, 256, 256, 256);
               context.draw();
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               RenderSystem.depthMask(true);
               RenderSystem.enableDepthTest();
            }
         }
      }
   }

   private static void renderSpyglass(DrawContext context, ScreenEffectPovActionClip clip, float elapsed, int width, int height) {
      if (isFirstPerson()) {
         boolean isVisible = clip.spyglassVisible.isEmpty() ? true : (Boolean)clip.spyglassVisible.interpolate(elapsed);
         if (isVisible) {
            float scale = clip.spyglassScale.isEmpty() ? 1.12F : (Float)clip.spyglassScale.interpolate(elapsed);
            float f = (float)Math.min(width, height);
            float h = Math.min((float)width / f, (float)height / f) * scale;
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
            context.fill(0, n, width, height, -16777216);
            context.fill(0, 0, width, l, -16777216);
            context.fill(0, l, k, n, -16777216);
            context.fill(m, l, width, n, -16777216);
            context.draw();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
         }
      }
   }

   private static void renderFrost(DrawContext context, ScreenEffectPovActionClip clip, float elapsed, int width, int height) {
      boolean isVisible = clip.frostVisible.isEmpty() ? true : (Boolean)clip.frostVisible.interpolate(elapsed);
      if (isVisible) {
         float intensity = clip.frostProgress.isEmpty() ? 1.0F : (Float)clip.frostProgress.interpolate(elapsed);
         if (!(intensity <= 0.001F)) {
            Color tint = (Color)clip.frostColor.get();
            float tr = tint != null ? tint.r : 1.0F;
            float tg = tint != null ? tint.g : 1.0F;
            float tb = tint != null ? tint.b : 1.0F;
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(tr, tg, tb, intensity);
            context.drawTexture(FROST_TEX, 0, 0, width, height, 0.0F, 0.0F, 256, 256, 256, 256);
            context.draw();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
         }
      }
   }

   private static void renderPortal(DrawContext context, ScreenEffectPovActionClip clip, float elapsed, int width, int height) {
      boolean isVisible = clip.portalVisible.isEmpty() ? true : (Boolean)clip.portalVisible.interpolate(elapsed);
      if (isVisible) {
         float intensity = clip.portalOpacity.isEmpty() ? 1.0F : (Float)clip.portalOpacity.interpolate(elapsed);
         if (!(intensity <= 0.001F)) {
            Sprite sprite = MinecraftClient.getInstance().getBlockRenderManager().getModels().getModelParticleSprite(Blocks.NETHER_PORTAL.getDefaultState());
            if (sprite != null) {
               Color tint = (Color)clip.portalColor.get();
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
         }
      }
   }

   private static void renderFire(DrawContext context, ScreenEffectPovActionClip clip, float elapsed, int width, int height) {
      if (isFirstPerson()) {
         boolean isVisible = clip.fireVisible.isEmpty() ? true : (Boolean)clip.fireVisible.interpolate(elapsed);
         if (isVisible) {
            Sprite sprite = ModelLoader.FIRE_1.getSprite();
            if (sprite != null) {
               Color tint = (Color)clip.fireColor.get();
               float tr = tint != null ? tint.r : 1.0F;
               float tg = tint != null ? tint.g : 1.0F;
               float tb = tint != null ? tint.b : 1.0F;
               Matrix4f prevProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
               float aspect = (float)width / (float)Math.max(1, height);
               Matrix4f fireProjection = new Matrix4f().setPerspective((float)Math.toRadians(70.0), aspect, 0.05F, 100.0F);
               RenderSystem.setProjectionMatrix(fireProjection, VertexSorter.BY_Z);
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

               for (int r = 0; r < 2; r++) {
                  fireMatrices.push();
                  float s = -((float)(r * 2 - 1)) * 0.24F;
                  float t = -0.3F;
                  float u = 0.0F;
                  fireMatrices.translate(s, t, u);
                  fireMatrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(r * 2 - 1) * 10.0F));
                  Matrix4f matrix4f = fireMatrices.peek().getPositionMatrix();
                  bufferBuilder.begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE);
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
               RenderSystem.setProjectionMatrix(prevProjection, VertexSorter.BY_Z);
            }
         }
      }
   }

   private static void renderPumpkin(DrawContext context, ScreenEffectPovActionClip clip, float elapsed, int width, int height) {
      boolean isVisible = clip.pumpkinVisible.isEmpty() ? true : (Boolean)clip.pumpkinVisible.interpolate(elapsed);
      if (isVisible) {
         float intensity = clip.pumpkinOpacity.isEmpty() ? 1.0F : (Float)clip.pumpkinOpacity.interpolate(elapsed);
         if (!(intensity <= 0.001F)) {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, intensity);
            context.drawTexture(PUMPKIN_TEX, 0, 0, width, height, 0.0F, 0.0F, 256, 256, 256, 256);
            context.draw();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
         }
      }
   }

   private static void renderSuffocation(DrawContext context, ScreenEffectPovActionClip clip, float elapsed, int width, int height) {
      boolean isVisible = clip.suffocationVisible.isEmpty() ? true : (Boolean)clip.suffocationVisible.interpolate(elapsed);
      if (isVisible) {
         String blockId = clip.suffocationBlock.isEmpty() ? "minecraft:stone" : (String)clip.suffocationBlock.interpolate(elapsed);
         Block block = Blocks.STONE;
         if (blockId != null && !blockId.trim().isEmpty()) {
            Identifier id = Identifier.tryParse(blockId.contains(":") ? blockId : "minecraft:" + blockId);
            if (id != null) {
               if (Registries.BLOCK.containsId(id)) {
                  block = (Block)Registries.BLOCK.get(id);
               } else if (Registries.ITEM.containsId(id)) {
                  Item item = (Item)Registries.ITEM.get(id);
                  if (item instanceof BlockItem bi) {
                     block = bi.getBlock();
                  }
               }
            }
         }

         Sprite sprite = MinecraftClient.getInstance().getBlockRenderManager().getModels().getModelParticleSprite(block.getDefaultState());
         if (sprite != null) {
            Matrix4f prevProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
            float aspect = (float)width / (float)Math.max(1, height);
            Matrix4f suffocationProjection = new Matrix4f().setPerspective((float)Math.toRadians(70.0), aspect, 0.05F, 100.0F);
            RenderSystem.setProjectionMatrix(suffocationProjection, VertexSorter.BY_Z);
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
            builder.begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE);
            builder.vertex(matrix4f, -1.0F, -1.0F, -0.5F).color(r, g, b, a).texture(m, o).next();
            builder.vertex(matrix4f, 1.0F, -1.0F, -0.5F).color(r, g, b, a).texture(l, o).next();
            builder.vertex(matrix4f, 1.0F, 1.0F, -0.5F).color(r, g, b, a).texture(l, n).next();
            builder.vertex(matrix4f, -1.0F, 1.0F, -0.5F).color(r, g, b, a).texture(m, n).next();
            BufferRenderer.drawWithGlobalProgram(builder.end());
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setProjectionMatrix(prevProjection, VertexSorter.BY_Z);
         }
      }
   }

   private static void renderTotem(DrawContext context, ScreenEffectPovActionClip clip, float elapsed, int width, int height) {
      boolean isVisible = clip.totemVisible.isEmpty() ? true : (Boolean)clip.totemVisible.interpolate(elapsed);
      if (isVisible) {
         float progress = clip.totemProgress.isEmpty() ? 0.0F : (Float)clip.totemProgress.interpolate(elapsed);
         if (!(progress <= 1.0E-4F) && !(progress >= 1.0F)) {
            MinecraftClient client = MinecraftClient.getInstance();
            String itemId = (String)clip.totemItem.get();
            if (itemId == null || itemId.isEmpty()) {
               itemId = "minecraft:totem_of_undying";
            }

            ItemStack stack = createItemStack(itemId);
            float f = MathHelper.clamp(progress, 0.0F, 1.0F);
            float g = f * f;
            float h = f * g;
            float j = 10.25F * h * g - 24.95F * g * g + 25.5F * h - 13.8F * g + 4.0F * f;
            float k = j * (float) Math.PI;
            boolean flipped = clip.totemFlipped.isEmpty() ? false : (Boolean)clip.totemFlipped.interpolate(elapsed);
            float f10 = (flipped ? 0.5F : -0.5F) * ((float)width / 4.0F);
            float f11 = 0.5F * ((float)height / 4.0F);
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
            DiffuseLighting.enableGuiDepthLighting();
            RenderSystem.enableDepthTest();
            RenderSystem.disableCull();
            Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
            client.getItemRenderer()
               .renderItem(stack, ModelTransformationMode.FIXED, 15728880, OverlayTexture.DEFAULT_UV, matrices, immediate, client.world, 0);
            immediate.draw();
            matrices.pop();
            RenderSystem.enableCull();
            RenderSystem.disableDepthTest();
            DiffuseLighting.disableGuiDepthLighting();
         }
      }
   }

   private static void renderNightVision(DrawContext context, ScreenEffectPovActionClip clip, float elapsed, int width, int height) {
   }

   private static void renderBlindness(DrawContext context, ScreenEffectPovActionClip clip, float elapsed, int width, int height) {
   }

   private static void renderDarkness(DrawContext context, ScreenEffectPovActionClip clip, float elapsed, int width, int height) {
   }

   public static boolean isFirstPerson() {
      MinecraftClient client = MinecraftClient.getInstance();
      PovPlaybackContext.Frame playback = PovPlaybackContext.getActive(client.getTickDelta());
      if (playback != null) {
         return playback.clip() != null ? playback.clip().isFirstPerson() : client.options.getPerspective().isFirstPerson();
      } else {
         UIFilmPanel panel = PovReplaySettings.getFilmPanel();
         if (panel != null && panel.getData() != null) {
            int povMode = panel.getController().getPovMode();
            if (povMode == 6 || povMode == 3) {
               return true;
            }

            if (povMode == 4 || povMode == 5 || povMode == 1 || povMode == 2) {
               return false;
            }

            float filmTick = panel.getRunner() != null && panel.getRunner().isRunning()
               ? (float)panel.getRunner().ticks + client.getTickDelta()
               : (float)panel.getCursor();
            PovCameraClip clip = PovCameraClips.resolve((Film)panel.getData(), filmTick);
            if (clip != null) {
               return clip.isFirstPerson();
            }
         }

         return client.options.getPerspective().isFirstPerson();
      }
   }

   private static ItemStack createItemStack(String iconId) {
      if (iconId != null && !iconId.isEmpty()) {
         try {
            Item item = (Item)Registries.ITEM.get(new Identifier(iconId));
            if (item != null && item != Items.AIR) {
               return new ItemStack(item);
            }
         } catch (Exception var2) {
         }
      }

      return new ItemStack(Items.TOTEM_OF_UNDYING);
   }

   private static record ActiveEffect(ScreenEffectPovActionClip clip, String effectId, float elapsed) {
   }
}

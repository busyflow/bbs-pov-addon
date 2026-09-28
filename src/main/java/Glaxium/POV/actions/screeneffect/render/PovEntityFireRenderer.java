package Glaxium.POV.actions.screeneffect.render;

import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.actions.clip.ScreenEffectPovActionClip;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.replay.PovReplaySettings;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.FilmControllerContext;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.math.RotationAxis;

public final class PovEntityFireRenderer {
   private PovEntityFireRenderer() {
   }

   public static float resolveFireIntensity(FilmControllerContext context) {
      if (context != null && context.replay != null) {
         if (context.replay.keyframes instanceof ReplayKeyframesPovAccess access) {
            RecordedPovActions actions = access.bbsPov$getActions();
            if (actions == null) {
               return 0.0F;
            } else {
               float replayTick = resolveReplayTick(context);

               for (Clip clip : actions.get()) {
                  if (clip instanceof ScreenEffectPovActionClip se && se.isActive(replayTick) && se.hasEffect("fire")) {
                     float elapsed = se.getLocalTick(replayTick);
                     boolean visible = se.fireVisible.isEmpty() ? true : (Boolean)se.fireVisible.interpolate(elapsed);
                     return visible ? 1.0F : 0.0F;
                  }
               }

               return 0.0F;
            }
         } else {
            return 0.0F;
         }
      } else {
         return 0.0F;
      }
   }

   private static float resolveReplayTick(FilmControllerContext context) {
      PovPlaybackContext.Frame playback = PovPlaybackContext.getActive(context.transition);
      if (playback != null && playback.replay() == context.replay) {
         return playback.replayTick();
      } else {
         UIFilmPanel panel = PovReplaySettings.getFilmPanel();
         if (panel != null && panel.getData() instanceof Film) {
            int cursor = panel.getCursor();
            boolean playing = panel.getController().isPlaying();
            float transition = playing ? Math.max(0.0F, Math.min(1.0F, context.transition)) : 0.0F;
            return (float)context.replay.getTick(cursor) + transition;
         } else {
            return (float)context.replay.getTick(0) + context.transition;
         }
      }
   }

   public static void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Camera camera, float width, float height, float intensity) {
      if (!(intensity <= 0.001F) && vertexConsumers != null && camera != null) {
         Sprite sprite0 = ModelLoader.FIRE_0.getSprite();
         Sprite sprite1 = ModelLoader.FIRE_1.getSprite();
         if (sprite0 != null && sprite1 != null) {
            matrices.push();
            float f = width * 1.4F;
            matrices.scale(f, f, f);
            float g = 0.5F;
            float i = height / f;
            float j = 0.0F;
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.translate(0.0F, 0.0F, -0.3F + (float)((int)i) * 0.02F);
            float k = 0.0F;
            int l = 0;
            VertexConsumer vertexConsumer = vertexConsumers.getBuffer(TexturedRenderLayers.getEntityCutout());

            for (Entry entry = matrices.peek(); i > 0.0F; l++) {
               Sprite sprite = l % 2 == 0 ? sprite0 : sprite1;
               float u0 = sprite.getMinU();
               float v0 = sprite.getMinV();
               float u1 = sprite.getMaxU();
               float v1 = sprite.getMaxV();
               if (l / 2 % 2 == 0) {
                  float temp = u1;
                  u1 = u0;
                  u0 = temp;
               }

               drawFireVertex(entry, vertexConsumer, g - 0.0F, 0.0F - j, k, u1, v1, intensity);
               drawFireVertex(entry, vertexConsumer, -g - 0.0F, 0.0F - j, k, u0, v1, intensity);
               drawFireVertex(entry, vertexConsumer, -g - 0.0F, 1.4F - j, k, u0, v0, intensity);
               drawFireVertex(entry, vertexConsumer, g - 0.0F, 1.4F - j, k, u1, v0, intensity);
               i -= 0.45F;
               j -= 0.45F;
               g *= 0.9F;
               k += 0.03F;
            }

            matrices.pop();
         }
      }
   }

   private static void drawFireVertex(Entry entry, VertexConsumer vertices, float x, float y, float z, float u, float v, float alpha) {
      vertices.vertex(entry.getPositionMatrix(), x, y, z)
         .color(1.0F, 1.0F, 1.0F, alpha)
         .texture(u, v)
         .overlay(OverlayTexture.DEFAULT_UV)
         .light(15728880)
         .normal(entry.getNormalMatrix(), 0.0F, 1.0F, 0.0F)
         .next();
   }
}

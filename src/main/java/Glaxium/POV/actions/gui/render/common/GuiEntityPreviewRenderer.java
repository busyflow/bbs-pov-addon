package Glaxium.POV.actions.gui.render.common;

import Glaxium.POV.actions.gui.render.GuiRenderContext;
import Glaxium.POV.actions.gui.render.LiveGuiPreviewRenderer;
import Glaxium.POV.camera.clip.PovCameraClips;
import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.integration.access.bbs.MorphPovAccess;
import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.render.PovViewportMetrics;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormTranslucentQueue;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

public final class GuiEntityPreviewRenderer {
   private GuiEntityPreviewRenderer() {
   }

   public static void draw(
      DrawContext context,
      int x1,
      int y1,
      int x2,
      int y2,
      int size,
      float yOffset,
      float lookX,
      float lookY,
      LivingEntity entity,
      int screenWidth,
      int screenHeight
   ) {
      float centerX = (float)(x1 + x2) / 2.0F;
      float centerY = (float)(y1 + y2) / 2.0F;
      int[] viewport = new int[4];
      GL11.glGetIntegerv(2978, viewport);
      int vpX = viewport[0];
      int vpY = viewport[1];
      int vpW = viewport[2];
      int vpH = viewport[3];
      boolean hasScissor = false;
      if (vpW > 0 && vpH > 0 && screenWidth > 0 && screenHeight > 0) {
         double scaleFactorX = (double)vpW / (double)screenWidth;
         double scaleFactorY = (double)vpH / (double)screenHeight;
         int minX = Math.min(x1, x2);
         int maxX = Math.max(x1, x2);
         int minY = Math.min(y1, y2);
         int maxY = Math.max(y1, y2);
         int physX = vpX + (int)Math.round((double)minX * scaleFactorX);
         int physY = vpY + (int)Math.round((double)(screenHeight - maxY) * scaleFactorY);
         int physW = Math.max(0, (int)Math.round((double)(maxX - minX) * scaleFactorX));
         int physH = Math.max(0, (int)Math.round((double)(maxY - minY) * scaleFactorY));
         int scissorX = Math.max(vpX, physX);
         int scissorY = Math.max(vpY, physY);
         int scissorW = Math.max(0, Math.min(vpX + vpW, physX + physW) - scissorX);
         int scissorH = Math.max(0, Math.min(vpY + vpH, physY + physH) - scissorY);
         context.draw();
         RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);
         hasScissor = true;
      }

      float f = (float)Math.atan((double)((centerX - lookX) / 40.0F));
      float g = (float)Math.atan((double)((centerY - lookY) / 40.0F));
      Quaternionf quaternionf = new Quaternionf().rotateZ((float) Math.PI);
      Quaternionf quaternionf2 = new Quaternionf().rotateX(g * 20.0F * (float) (Math.PI / 180.0));
      quaternionf.mul(quaternionf2);
      float bodyYaw = entity.bodyYaw;
      float yaw = entity.getYaw();
      float pitch = entity.getPitch();
      float prevHeadYaw = entity.prevHeadYaw;
      float headYaw = entity.headYaw;
      entity.bodyYaw = 180.0F + f * 20.0F;
      entity.setYaw(180.0F + f * 40.0F);
      entity.setPitch(-g * 20.0F);
      entity.headYaw = entity.getYaw();
      entity.prevHeadYaw = entity.getYaw();

      try {
         InventoryScreen.drawEntity(context, (int)centerX, (int)centerY, size, quaternionf, quaternionf2, entity);
         context.draw();
      } finally {
         entity.bodyYaw = bodyYaw;
         entity.setYaw(yaw);
         entity.setPitch(pitch);
         entity.prevHeadYaw = prevHeadYaw;
         entity.headYaw = headYaw;
         if (hasScissor) {
            RenderSystem.disableScissor();
         }
      }
   }

   public static void drawInventoryPreview(GuiRenderContext ctx, boolean creativeSurvival) {
      if (ctx != null && !ctx.skipEntityPreview) {
         Batcher2D batcher = ctx.batcher;
         RecordedHandData handData = ctx.handData;
         float globalTick = ctx.globalTick;
         int screenWidth = ctx.screenWidth;
         int screenHeight = ctx.screenHeight;
         float scaleX = ctx.scaleX;
         float scaleY = ctx.scaleY;
         float originX = ctx.originX;
         float originY = ctx.originY;
         float curScreenX = ctx.curScreenX;
         float curScreenY = ctx.curScreenY;
         int charX1 = (int)(originX + (float)(creativeSurvival ? 73 : 26) * scaleX);
         int charY1 = (int)(originY + (float)(creativeSurvival ? 6 : 8) * scaleY);
         int charX2 = (int)(originX + (float)(creativeSurvival ? 105 : 75) * scaleX);
         int charY2 = (int)(originY + (float)(creativeSurvival ? 49 : 78) * scaleY);
         int charSize = (int)((float)(creativeSurvival ? 20 : 30) * Math.min(scaleX, scaleY));
         LivingEntity targetEntity = null;
         UIFilmPanel filmPanel = PovViewportMetrics.resolveFilmPanel();
         ModelForm replayModelForm = null;
         if (filmPanel != null && filmPanel.getController() != null && filmPanel.getData() != null) {
            Replay sourceReplay;
            if (filmPanel.getController().getPovMode() == 6) {
               sourceReplay = filmPanel.replayEditor.getReplay();
            } else {
               sourceReplay = PovCameraClips.resolveReplay((Film)filmPanel.getData(), (float)filmPanel.getCursor());
            }

            int selector = PovCameraClips.indexOfReplay((Film)filmPanel.getData(), sourceReplay);
            IEntity target = selector < 0 ? null : (IEntity)filmPanel.getController().getEntities().get(selector);
            if (target instanceof MCEntity mc && mc.getMcEntity() instanceof LivingEntity living) {
               targetEntity = living;
            }

            Form sourceForm = target == null ? (sourceReplay == null ? null : (Form)sourceReplay.form.get()) : target.getForm();
            if ((sourceForm == null ? null : FormUtils.getRoot(sourceForm)) instanceof ModelForm mf) {
               replayModelForm = mf;
            }
         }

         PovPlaybackContext.Frame frame = PovPlaybackContext.getActive();
         if (frame != null && frame.replay() != null) {
            int selectorx = (Integer)frame.clip().selector.get();
            IEntity playbackTarget = selectorx < 0 ? null : (IEntity)frame.controller().getEntities().get(selectorx);
            if (playbackTarget instanceof MCEntity mc && mc.getMcEntity() instanceof LivingEntity living) {
               targetEntity = living;
            }

            if (replayModelForm == null) {
               Form form = playbackTarget == null ? (Form)frame.replay().form.get() : playbackTarget.getForm();
               if ((form == null ? null : FormUtils.getRoot(form)) instanceof ModelForm mf) {
                  replayModelForm = mf;
               }
            }
         }

         if (targetEntity == null) {
            targetEntity = MinecraftClient.getInstance().player;
         }

         if (LiveGuiPreviewRenderer.isRenderingLive()) {
            if (targetEntity != null) {
               boolean queueWasActive = FormTranslucentQueue.suspend();

               try {
                  batcher.flush();
                  RenderSystem.enableDepthTest();
                  RenderSystem.depthFunc(515);
                  RenderSystem.depthMask(true);
                  RenderSystem.colorMask(true, true, true, true);
                  RenderSystem.enableCull();
                  RenderSystem.enableBlend();
                  RenderSystem.defaultBlendFunc();
                  RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
                  float centerX = (float)(charX1 + charX2) / 2.0F;
                  float centerY = (float)(charY1 + charY2) / 2.0F;
                  float lookX = centerX + (curScreenX - centerX) / scaleX;
                  float lookY = centerY + (curScreenY - centerY) / scaleY;
                  draw(batcher.getContext(), charX1, charY1, charX2, charY2, charSize, 0.0625F, lookX, lookY, targetEntity, screenWidth, screenHeight);
                  batcher.flush();
               } catch (Exception var42) {
               } finally {
                  FormTranslucentQueue.restore(queueWasActive);
                  RenderSystem.disableDepthTest();
                  RenderSystem.depthMask(false);
               }
            }
         } else if (targetEntity != null) {
            boolean queueWasActive = FormTranslucentQueue.suspend();
            Morph morph = Morph.getMorph(targetEntity);
            if (morph == null) {
               morph = new Morph(targetEntity);
            }

            Form originalForm = morph == null ? null : morph.getForm();
            String targetModel = handData != null && !handData.model.isEmpty() ? (String)handData.model.interpolate(globalTick, null) : null;
            Link targetTexture = handData != null && !handData.texture.isEmpty() ? (Link)handData.texture.interpolate(globalTick, null) : null;
            if (targetModel == null && replayModelForm != null) {
               targetModel = (String)replayModelForm.model.get();
               if (targetTexture == null) {
                  targetTexture = (Link)replayModelForm.texture.get();
               }
            }

            if (targetModel == null && originalForm instanceof ModelForm mf) {
               targetModel = (String)mf.model.get();
               if (targetTexture == null) {
                  targetTexture = (Link)mf.texture.get();
               }
            }

            if (targetModel == null || targetModel.isBlank()) {
               targetModel = "player/steve";
            }

            if (targetTexture == null && replayModelForm != null && targetModel.equals(replayModelForm.model.get())) {
               targetTexture = (Link)replayModelForm.texture.get();
            }

            ModelForm previewForm = new ModelForm();
            previewForm.visible.set(true);
            previewForm.model.set(targetModel);
            previewForm.texture.set(targetTexture);
            if (morph != null) {
               previewForm.update(morph.entity);
               if (FormUtilsClient.getRenderer(previewForm) instanceof ModelFormRenderer modelRenderer) {
                  modelRenderer.ensureAnimator(0.0F);
               }

               ((MorphPovAccess)morph).bbsPov$setFormRaw(previewForm);
            }

            try {
               batcher.flush();
               RenderSystem.enableDepthTest();
               RenderSystem.depthFunc(515);
               RenderSystem.depthMask(true);
               RenderSystem.colorMask(true, true, true, true);
               RenderSystem.enableCull();
               RenderSystem.enableBlend();
               RenderSystem.defaultBlendFunc();
               RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
               float centerX = (float)(charX1 + charX2) / 2.0F;
               float centerY = (float)(charY1 + charY2) / 2.0F;
               float lookX = centerX + (curScreenX - centerX) / scaleX;
               float lookY = centerY + (curScreenY - centerY) / scaleY;
               draw(batcher.getContext(), charX1, charY1, charX2, charY2, charSize, 0.0625F, lookX, lookY, targetEntity, screenWidth, screenHeight);
               batcher.flush();
            } catch (Exception var41) {
            } finally {
               FormTranslucentQueue.restore(queueWasActive);
               RenderSystem.disableDepthTest();
               RenderSystem.depthMask(false);
               if (morph != null) {
                  ((MorphPovAccess)morph).bbsPov$setFormRaw(originalForm);
               }
            }
         }
      }
   }
}

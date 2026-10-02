package Glaxium.POV.actions.gui.render.common;

import Glaxium.POV.actions.gui.render.GuiRenderContext;
import Glaxium.POV.actions.gui.render.LiveGuiPreviewRenderer;
import Glaxium.POV.camera.PovCameraMode;
import Glaxium.POV.camera.clip.PovCameraClips;
import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.integration.access.bbs.MorphPovAccess;
import Glaxium.POV.playback.PovPlaybackContext;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.forms.FormTranslucentQueue;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

/** Inventory-style 3D entity preview with scissor to the GUI box. */
public final class GuiEntityPreviewRenderer
{
    private GuiEntityPreviewRenderer()
    {
    }

    public static void draw(
        DrawContext context,
        float centerX,
        float centerY,
        int size,
        float yOffset,
        float lookX,
        float lookY,
        LivingEntity entity)
    {
        float entityHeight = entity != null ? entity.getHeight() : 1.8F;
        if (entityHeight <= 0.1F)
        {
            entityHeight = 1.8F;
        }
        float headY = centerY - (entityHeight * size * 0.7F);
        float f = (float) Math.atan((centerX - lookX) / 40.0F);
        float g = (float) Math.atan((headY - lookY) / 40.0F);
        Quaternionf quaternionf = (new Quaternionf()).rotateZ((float) Math.PI);
        Quaternionf quaternionf2 = (new Quaternionf()).rotateX(g * 20.0F * 0.017453292F);
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

        Vector3f vector3f = new Vector3f(0.0F, entity.getHeight() / 2.0F + yOffset, 0.0F);

        try
        {
            InventoryScreen.drawEntity(
                context,
                (int) centerX,
                (int) centerY,
                size,
                vector3f,
                quaternionf,
                quaternionf2,
                entity
            );
            context.draw();
        }
        finally
        {
            entity.bodyYaw = bodyYaw;
            entity.setYaw(yaw);
            entity.setPitch(pitch);
            entity.prevHeadYaw = prevHeadYaw;
            entity.headYaw = headYaw;
        }
    }

    public static void drawInventoryPreview(GuiRenderContext ctx, boolean creativeSurvival)
    {
        if (ctx == null || ctx.skipEntityPreview)
        {
            return;
        }
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

        net.minecraft.entity.LivingEntity targetEntity = null;
        mchorse.bbs_mod.ui.film.UIFilmPanel filmPanel = Glaxium.POV.render.PovViewportMetrics.resolveFilmPanel();
        ModelForm replayModelForm = null;

        if (filmPanel != null && filmPanel.getController() != null && filmPanel.getData() != null)
        {
            Replay sourceReplay;

            if (filmPanel.getController().getPovMode() == PovCameraMode.POV)
            {
                sourceReplay = filmPanel.replayEditor.getReplay();
            }
            else
            {
                sourceReplay = PovCameraClips.resolveReplay(
                    filmPanel.getData(),
                    filmPanel.getCursor());
            }

            int selector = PovCameraClips.indexOfReplay(filmPanel.getData(), sourceReplay);
            IEntity target = selector < 0
                ? null
                : filmPanel.getController().getEntities().get(selector);

            if (target instanceof MCEntity mc && mc.getMcEntity() instanceof net.minecraft.entity.LivingEntity living)
            {
                targetEntity = living;
            }

            Form sourceForm = target == null
                ? sourceReplay == null ? null : sourceReplay.form.get()
                : target.getForm();
            Form root = sourceForm == null ? null : FormUtils.getRoot(sourceForm);

            if (root instanceof ModelForm mf)
            {
                replayModelForm = mf;
            }
        }

        PovPlaybackContext.Frame frame = PovPlaybackContext.getActive();

        if (frame != null && frame.replay() != null)
        {
            int selector = frame.clip().selector.get();
            IEntity playbackTarget = selector < 0
                ? null
                : frame.controller().getEntities().get(selector);

            if (playbackTarget instanceof MCEntity mc
                && mc.getMcEntity() instanceof net.minecraft.entity.LivingEntity living)
            {
                targetEntity = living;
            }

            if (replayModelForm == null)
            {
                Form form = playbackTarget == null
                    ? frame.replay().form.get()
                    : playbackTarget.getForm();
                Form root = form == null ? null : FormUtils.getRoot(form);

                if (root instanceof ModelForm mf)
                {
                    replayModelForm = mf;
                }
            }
        }

        if (targetEntity == null)
        {
            targetEntity = MinecraftClient.getInstance().player;
        }

        float entityHeight = targetEntity != null ? targetEntity.getHeight() : 1.8F;
        if (entityHeight <= 0.1F)
        {
            entityHeight = 1.8F;
        }

        int charSize = creativeSurvival
            ? (int) Math.min(20, 36F / entityHeight)
            : (int) Math.min(30, 54F / entityHeight);
        float centerX = creativeSurvival ? 88F : 51F;
        float centerY = creativeSurvival ? 26F : 43F;
        float lookX = ctx.cursorVisible ? ctx.cursorGuiX : centerX;
        float lookY = ctx.cursorVisible ? ctx.cursorGuiY : centerY;

        if (LiveGuiPreviewRenderer.isRenderingLive())
        {
            if (targetEntity != null)
            {
                /* The survival inventory renders a real BBS Morph.  It can contain
                 * translucent model pixels, which otherwise get put into BBS's world
                 * translucency queue and replay after the carried-item renderer has
                 * changed the GUI matrices.  That is why this only broke when an item
                 * was on the cursor.  A GUI entity preview must be one immediate pass. */
                boolean queueWasActive = FormTranslucentQueue.suspend();

                try
                {
                    batcher.flush();
                    /* The native HandledScreen draws its carried stack after
                     * our overlay.  On the next frame that item path can leave
                     * culling/depth state behind, so establish the full entity
                     * preview contract here instead of relying on GUI defaults. */
                    RenderSystem.enableDepthTest();
                    RenderSystem.depthFunc(GL11.GL_LEQUAL);
                    RenderSystem.depthMask(true);
                    RenderSystem.colorMask(true, true, true, true);
                    RenderSystem.enableCull();
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);

                    GuiEntityPreviewRenderer.draw(
                        batcher.getContext(),
                        centerX,
                        centerY,
                        charSize,
                        0.0625F,
                        lookX,
                        lookY,
                        targetEntity
                    );
                    batcher.flush();
                }
                catch (Exception ignored)
                {
                }
                finally
                {
                    FormTranslucentQueue.restore(queueWasActive);
                    RenderSystem.disableDepthTest();
                    RenderSystem.depthMask(false);
                }
            }
        }
        else if (targetEntity != null)
        {
            /* Film preview reaches this branch (the live recorder uses the
             * branch above).  Keep its BBS Morph out of the world-level
             * translucent queue: the cursor item is drawn after this GUI
             * preview and otherwise changes the GL state before queued rear
             * faces replay. */
            boolean queueWasActive = FormTranslucentQueue.suspend();
            Morph morph = Morph.getMorph(targetEntity);
            if (morph == null)
            {
                morph = new Morph(targetEntity);
            }
            Form originalForm = morph == null ? null : morph.getForm();

            String targetModel = handData != null && !handData.model.isEmpty()
                ? handData.model.interpolate(globalTick, null)
                : null;
            Link targetTexture = handData != null && !handData.texture.isEmpty()
                ? handData.texture.interpolate(globalTick, null)
                : null;

            if (targetModel == null && replayModelForm != null)
            {
                targetModel = replayModelForm.model.get();
                if (targetTexture == null)
                {
                    targetTexture = replayModelForm.texture.get();
                }
            }

            if (targetModel == null && originalForm instanceof ModelForm mf)
            {
                targetModel = mf.model.get();
                if (targetTexture == null)
                {
                    targetTexture = mf.texture.get();
                }
            }

            if (targetModel == null || targetModel.isBlank())
            {
                targetModel = RecordedHandData.DEFAULT_MODEL;
            }
            if (targetTexture == null && replayModelForm != null && targetModel.equals(replayModelForm.model.get()))
            {
                targetTexture = replayModelForm.texture.get();
            }

            ModelForm previewForm = new ModelForm();
            if (replayModelForm != null)
            {
                previewForm.copy(replayModelForm);
            }
            else if (originalForm instanceof ModelForm mf)
            {
                previewForm.copy(mf);
            }
            previewForm.visible.set(true);
            if (targetModel != null && !targetModel.isBlank())
            {
                previewForm.model.set(targetModel);
            }
            if (targetTexture != null)
            {
                previewForm.texture.set(targetTexture);
            }

            if (morph != null)
            {
                previewForm.update(morph.entity);
                FormRenderer<?> formRenderer = FormUtilsClient.getRenderer(previewForm);
                if (formRenderer instanceof ModelFormRenderer modelRenderer)
                {
                    modelRenderer.ensureAnimator(0F);
                }
                ((MorphPovAccess) morph).bbsPov$setFormRaw(previewForm);
            }

            try
            {
                batcher.flush();

                /* The GUI layer disables depth writes. InventoryScreen only
                 * enables depth testing, so BBS model back faces otherwise
                 * render through the front and make the actor look flattened. */
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
                RenderSystem.depthMask(true);
                RenderSystem.colorMask(true, true, true, true);
                RenderSystem.enableCull();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);

                GuiEntityPreviewRenderer.draw(
                    batcher.getContext(),
                    centerX,
                    centerY,
                    charSize,
                    0.0625F,
                    lookX,
                    lookY,
                    targetEntity
                );
                batcher.flush();
            }
            catch (Exception ignored)
            {
            }
            finally
            {
                FormTranslucentQueue.restore(queueWasActive);
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);

                if (morph != null)
                {
                    ((MorphPovAccess) morph).bbsPov$setFormRaw(originalForm);
                }
            }
        }
    }
}

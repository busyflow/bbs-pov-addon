package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.PovAddon;
import Glaxium.POV.camera.PovCameraMode;
import Glaxium.POV.camera.clip.PovCameraClip;
import Glaxium.POV.camera.clip.PovCameraClips;
import Glaxium.POV.hand.editor.PovHandGizmo;
import Glaxium.POV.hand.editor.PovHandPicking;
import Glaxium.POV.hand.playback.PovHandPlayback;
import Glaxium.POV.hand.render.PovHandMatrices;
import Glaxium.POV.integration.access.bbs.UIFilmPanelPovAccess;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.FilmControllerContext;
import mchorse.bbs_mod.film.FilmEntityRenderer;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.controller.FilmStencilPicker;
import mchorse.bbs_mod.ui.film.controller.UIFilmController;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.StencilFormFramebuffer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FilmStencilPicker.class, remap = false)
public class FilmStencilPickerPovMixin
{
    @Shadow @Final private UIFilmController controller;
    @Shadow @Final private StencilFormFramebuffer stencil;
    @Shadow @Final private StencilMap stencilMap;

    @Inject(method = "renderStencil", at = @At("HEAD"), cancellable = true)
    private void bbsPov$renderHandPicker(
        WorldRenderContext worldContext,
        UIContext context,
        boolean alt,
        CallbackInfo info)
    {
        if (this.controller.getPovMode() != PovCameraMode.POV)
        {
            return;
        }

        info.cancel();

        UIFilmPanel panel = this.controller.panel;
        Area viewport = panel.preview.getViewport();
        boolean handEditor = panel instanceof UIFilmPanelPovAccess access
            && access.bbsPov$getEditor() != null
            && access.bbsPov$getEditor().isPoseGizmoSection();

        if (!handEditor || !viewport.isInside(context) || viewport.w <= 0 || viewport.h <= 0)
        {
            this.stencil.clearPicking();
            PovHandPicking.abortStencil();
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null)
        {
            this.stencil.clearPicking();
            PovHandPicking.abortStencil();
            return;
        }

        this.stencil.setup(Link.bbs("stencil_film"));
        Texture texture = this.stencil.getFramebuffer().getMainTexture();
        int videoWidth = BBSRendering.getVideoWidth();
        int videoHeight = BBSRendering.getVideoHeight();

        if (texture.width != videoWidth || texture.height != videoHeight)
        {
            this.stencil.resizeGUI(videoWidth, videoHeight);
            texture = this.stencil.getFramebuffer().getMainTexture();
        }

        this.stencilMap.setup();
        this.stencil.apply();
        PovHandPicking.beginStencil(this.stencilMap);

        MatrixStack matrices = worldContext.matrixStack();
        matrices.push();
        matrices.loadIdentity();
        RenderSystem.getModelViewStack().push();
        RenderSystem.getModelViewStack().loadIdentity();
        RenderSystem.applyModelViewMatrix();

        try
        {
            Matrix4f handProjection = PovHandPicking.getProjection();

            if (handProjection == null)
            {
                handProjection = client.gameRenderer.getBasicProjectionMatrix(70D);
                if (videoWidth > 0 && videoHeight > 0)
                {
                    handProjection.m00(handProjection.m11() / (videoWidth / (float) videoHeight));
                }
                PovHandPicking.captureProjection(handProjection);
            }

            RenderSystem.setProjectionMatrix(handProjection, VertexSorter.BY_Z);

            boolean began = PovHandPlayback.begin(worldContext.tickDelta());

            if (began)
            {
                PovHandPlayback.useCapturedPose(PovHandMatrices.getCapturedPose());

                VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();
                BlockPos cameraPos = BlockPos.ofFloored(
                    panel.getCamera().position.x,
                    panel.getCamera().position.y,
                    panel.getCamera().position.z);
                int light = client.world == null
                    ? 0x00F000F0
                    : WorldRenderer.getLightmapCoordinates(client.world, cameraPos);

                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(true);
                RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);

                PovHandPlayback.render(
                    client.gameRenderer.firstPersonRenderer,
                    worldContext.tickDelta(),
                    matrices,
                    consumers,
                    player,
                    light);

                consumers.draw();
                PovHandGizmo.renderStencil(this.stencilMap);
            }

            int x = (int) (((context.mouseX - viewport.x) / (float) viewport.w) * texture.width);
            int y = (int) ((1F - (context.mouseY - viewport.y) / (float) viewport.h) * texture.height);
            int tolerance = Math.round((Integer) BBSSettings.gizmoHoverTolerance.get()
                * texture.width / (float) viewport.w);

            /* [PORTING NOTE: BBS GIZMO HOVER TOLERANCE FIX]
             * BBS's StencilFormFramebuffer.pick(x, y, radius, handleMax) uses handleMax to
             * restrict hover tolerance searching ONLY to gizmo handles (1..handleMax).
             * Any index > handleMax (bones, bodyparts) resolves strictly at the exact center
             * pixel. Passing Integer.MAX_VALUE would cause bones to grab from tolerance radius,
             * causing flickering / mispicking between nearby limbs. Pass Gizmo.STENCIL_MAX. */
            this.stencil.pick(x, y, tolerance, mchorse.bbs_mod.ui.utils.Gizmo.STENCIL_MAX);
            this.stencil.unbind(this.stencilMap);
            PovHandPicking.finishStencil(this.stencil.getPicked());
        }
        catch (Throwable throwable)
        {
            PovAddon.LOGGER.error("POV hand stencil render failed", throwable);
            this.stencil.unbind(this.stencilMap);
            PovHandPicking.abortStencil();
        }
        finally
        {
            PovHandPlayback.end();
            RenderSystem.getModelViewStack().pop();
            RenderSystem.applyModelViewMatrix();
            matrices.pop();
            client.getFramebuffer().beginWrite(true);
        }
    }

    @Redirect(
        method = "renderStencil",
        at = @At(
            value = "INVOKE",
            target = "Lmchorse/bbs_mod/film/FilmEntityRenderer;renderEntity(Lmchorse/bbs_mod/film/FilmControllerContext;)V"))
    private void bbsPov$hideHeadLookSourceFromPicker(FilmControllerContext renderContext)
    {
        UIFilmPanel panel = this.controller.panel;

        if (Glaxium.POV.editor.UIPovHandEditor.isActive() && renderContext.replay == panel.replayEditor.getReplay())
        {
            return;
        }

        int povMode = this.controller.getPovMode();

        if (povMode == UIFilmController.CAMERA_MODE_FREE || povMode == UIFilmController.CAMERA_MODE_ORBIT)
        {
            FilmEntityRenderer.renderEntity(renderContext);
            return;
        }

        Film film = (Film) panel.getData();
        float filmTick = panel.getRunner() != null && panel.getRunner().isRunning()
            ? panel.getRunner().ticks + renderContext.transition
            : panel.getCursor();
        PovCameraClip clip = PovCameraClips.resolve(film, filmTick);

        if (clip != null
            && clip.headLook.get()
            && renderContext.replay == PovCameraClips.resolveReplay(film, clip))
        {
            return;
        }

        FilmEntityRenderer.renderEntity(renderContext);
    }
}

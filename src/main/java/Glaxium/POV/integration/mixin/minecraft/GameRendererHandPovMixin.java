package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.hand.HandState;
import Glaxium.POV.hand.playback.PovHandPlayback;
import Glaxium.POV.hand.render.PovHandDepthManager;
import Glaxium.POV.hand.editor.PovHandPicking;
import Glaxium.POV.hand.editor.PovHandGizmo;
import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.editor.UIPovHandEditor;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.ui.utils.Area;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.joml.Matrix4f;

import net.minecraft.client.gui.screen.Screen;
import mchorse.bbs_mod.ui.framework.UIScreen;
import org.spongepowered.asm.mixin.Unique;

/** Render before BBS/Iris cancels renderHand. */
@Mixin(value = GameRenderer.class, priority = 200)
public abstract class GameRendererHandPovMixin
{
    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private BufferBuilderStorage buffers;
    @Shadow @Final private LightmapTextureManager lightmapTextureManager;
    @Shadow @Final public HeldItemRenderer firstPersonRenderer;

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void bbsPov$renderHandBeforeBbsCancellation(
        MatrixStack matrices,
        Camera camera,
        float tickDelta,
        CallbackInfo info)
    {
        if (UIPovHandEditor.isActive())
        {
            info.cancel();
            return;
        }

        if (BBSRendering.isIrisShadersEnabled() && PovHandPlayback.isHandActive(tickDelta))
        {
            info.cancel();
            return;
        }

        if (!PovHandPlayback.begin(tickDelta))
        {
            if (PovPlaybackContext.getActive() != null
                || Glaxium.POV.replay.PovReplaySettings.getFilmPanel() != null
                || mchorse.bbs_mod.BBSModClient.getCameraController().getCurrent() != null
                || this.client.currentScreen instanceof UIScreen)
            {
                info.cancel();
            }

            return;
        }

        HandState state = PovHandPlayback.getActiveState();
        boolean worldInteraction = state != null && state.worldInteraction;
        boolean replayInteraction = state != null && state.replayInteraction;

        ClientPlayerEntity player = this.client.player;

        if (player == null)
        {
            PovHandPlayback.end();
            return;
        }

        GameRenderer renderer = (GameRenderer) (Object) this;
        /* A POV hand is a screen overlay, not part of the Film camera lens.
         * Keep its projection permanently fixed regardless of Minecraft,
         * Film-camera, sprint or effect-driven FOV changes. */
        double fov = 70D;
        Matrix4f handProjection = renderer.getBasicProjectionMatrix(fov);
        int videoWidth = BBSRendering.isCustomSize() ? BBSRendering.getVideoWidth() : this.client.getWindow().getFramebufferWidth();
        int videoHeight = BBSRendering.isCustomSize() ? BBSRendering.getVideoHeight() : this.client.getWindow().getFramebufferHeight();

        /* getBasicProjectionMatrix() uses the Minecraft window framebuffer's
         * aspect ratio. Film renders into BBS's video framebuffer, which can be
         * 16:9, square, 4K, etc. Preserve vertical FOV and rebuild horizontal FOV
         * for the actual Film target so hand pixels and mouse rays share scale. */
        boolean inHandEditor = UIPovHandEditor.isActive();
        Area editorFrame = inHandEditor && UIPovHandEditor.getActive() != null
            ? UIPovHandEditor.getActive().getFrameArea()
            : null;

        if (inHandEditor && editorFrame != null)
        {
            /* Remove world view completely and clear to BBS dark background (#181818). */
            RenderSystem.clearColor(0.094F, 0.094F, 0.094F, 1.0F);
            RenderSystem.clear(16640, MinecraftClient.IS_SYSTEM_MAC);

            double scale = this.client.getWindow().getScaleFactor();
            int vx = (int) Math.round(editorFrame.x * scale);
            int vy = (int) Math.round((this.client.getWindow().getScaledHeight() - (editorFrame.y + editorFrame.h)) * scale);
            int vw = (int) Math.round(editorFrame.w * scale);
            int vh = (int) Math.round(editorFrame.h * scale);

            RenderSystem.viewport(vx, vy, vw, vh);

            handProjection.setPerspective((float) Math.toRadians(fov), 16.0F / 9.0F, 0.05F, 100.0F);
            renderer.loadProjectionMatrix(handProjection);
        }
        else
        {
            if (videoWidth > 0 && videoHeight > 0)
            {
                float aspect = videoWidth / (float) videoHeight;

                handProjection.m00(handProjection.m11() / aspect);
                renderer.loadProjectionMatrix(handProjection);
            }
            else
            {
                renderer.loadProjectionMatrix(handProjection);
            }

            PovHandDepthManager.prepareDepthForHand(worldInteraction, replayInteraction);
        }
        PovHandPicking.captureProjection(RenderSystem.getProjectionMatrix());
        /* Preserve the caller's matrix. Resetting before push leaked an identity base
         * back into the Film render pass and made Camera Offset appear nondeterministic. */
        matrices.push();
        matrices.loadIdentity();
        this.lightmapTextureManager.enable();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);

        try
        {
            VertexConsumerProvider.Immediate consumers = this.buffers.getEntityVertexConsumers();
            /* First-person light belongs to the Film camera position, not the local
             * spectator/player left behind elsewhere in the world. */
            int light = this.client.world == null
                ? 0x00F000F0
                : WorldRenderer.getLightmapCoordinates(this.client.world, camera.getBlockPos());
            PovHandPlayback.render(
                this.firstPersonRenderer,
                tickDelta,
                matrices,
                consumers,
                player,
                light);

            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);

            consumers.draw();

            /* Capture while the first-person projection is still active. Doing
             * this later from Film's HUD pass captures the orthographic GUI
             * projection and produces the black, hover-dependent pseudo-gizmo. */
            PovHandGizmo.captureVisual();
        }
        finally
        {
            if (inHandEditor)
            {
                RenderSystem.viewport(
                    0, 0,
                    this.client.getWindow().getFramebufferWidth(),
                    this.client.getWindow().getFramebufferHeight()
                );
                RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
            }

            PovHandDepthManager.endHandDepth(worldInteraction, replayInteraction);
            this.lightmapTextureManager.disable();
            matrices.pop();
            PovHandPlayback.end();
        }

        info.cancel();
    }
}

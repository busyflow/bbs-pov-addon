package Glaxium.POV.render;

import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.actions.bossbar.render.BossBarActionRenderer;
import Glaxium.POV.actions.gui.render.GuiActionRenderer;
import Glaxium.POV.actions.menu.render.MenuActionRenderer;
import Glaxium.POV.camera.clip.PovCameraClip;
import Glaxium.POV.camera.clip.PovCameraClips;
import Glaxium.POV.camera.PovCameraMode;
import Glaxium.POV.editor.UIPovHandEditor;
import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.hud.HudState;
import Glaxium.POV.hud.RecordedHudData;
import Glaxium.POV.hud.playback.HudSampler;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.hud.render.AttackIndicatorRenderer;
import Glaxium.POV.hud.render.HudRenderer;
import Glaxium.POV.hud.render.HeldItemTooltipRenderer;
import Glaxium.POV.integration.access.minecraft.InGameHudVignettePovAccess;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.controller.UIFilmController;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

import java.util.List;

/** Renders the selected first-person actor's POV channels directly in Film preview. */
public final class PovOverlayRenderer
{
    private static boolean vignetteDrawnThisFrame;

    private PovOverlayRenderer()
    {
    }

    public static void beginFrame()
    {
        vignetteDrawnThisFrame = false;
    }

    public static boolean shouldSkipHudVignette()
    {
        return false;
    }

    public static void render(MatrixStack matrices, Batcher2D batcher)
    {
        UIFilmPanel filmPanel = PovViewportMetrics.resolveFilmPanel();

        if (filmPanel == null)
        {
            return;
        }

        float filmTick;
        float tickDelta = MinecraftClient.getInstance().getTickDelta();

        if (filmPanel.getRunner() != null && filmPanel.getRunner().isRunning())
        {
            filmTick = filmPanel.getRunner().ticks + tickDelta;
        }
        else
        {
            filmTick = filmPanel.getCursor();
        }

        int povMode = filmPanel.getController().getPovMode();

        if (povMode == UIFilmController.CAMERA_MODE_FREE
            || povMode == UIFilmController.CAMERA_MODE_ORBIT
            || UIPovHandEditor.isActive())
        {
            return;
        }

        boolean povEditMode = povMode == PovCameraMode.POV;
        Film film = (Film) filmPanel.getData();
        if (film == null)
        {
            return;
        }

        PovCameraClip clip = povEditMode ? null : PovCameraClips.resolve(film, filmTick);
        if (!povEditMode && clip == null)
        {
            return;
        }

        Replay replay = povEditMode
            ? filmPanel.replayEditor.getReplay()
            : PovCameraClips.resolveReplay(film, clip);

        if (replay == null)
        {
            replay = film.getFirstPersonReplay();
        }
        if (replay == null && film.replays != null && !film.replays.getList().isEmpty())
        {
            replay = film.replays.getList().get(0);
        }

        if (replay == null || !(replay.keyframes instanceof ReplayKeyframesPovAccess access))
        {
            return;
        }

        boolean showHud = povEditMode || clip.hud.get();
        boolean showCrosshairOutput = povEditMode || clip.crosshair.get();
        boolean showActions = povEditMode || clip.actions.get();
        boolean showScreenEffects = povEditMode || (clip.actions.get() && clip.screenEffects.get());
        boolean showCursor = povEditMode || clip.cursor.get();
        RecordedHudData hud = access.bbsPov$getHud();
        Glaxium.POV.hand.RecordedHandData hand = access.bbsPov$getHand();
        RecordedPovActions actions = access.bbsPov$getActions();
        int looping = replay.looping.get();
        float replayTick = looping > 0 ? (filmTick % looping) : filmTick;
        HudState state = hud == null ? null : HudSampler.sample(hud, replay.keyframes, replayTick);

        int width = PovViewportMetrics.getFilmScaledWidth();
        int height = PovViewportMetrics.getFilmScaledHeight();
        Matrix4f previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        Matrix4f screenProjection = new Matrix4f().ortho(0F, width, height, 0F, -1000F, 3000F);

        RenderSystem.setProjectionMatrix(screenProjection, VertexSorter.BY_Z);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
            GlStateManager.SrcFactor.SRC_ALPHA,
            GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SrcFactor.ZERO,
            GlStateManager.DstFactor.ONE
        );
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.gameRenderer == null || client.gameRenderer.getCamera() == null || client.gameRenderer.getCamera().getSubmersionType() != net.minecraft.client.render.CameraSubmersionType.LAVA)
        {
            net.minecraft.client.render.BackgroundRenderer.clearFog();
        }

        try
        {
            if (showScreenEffects && actions != null)
            {
                Glaxium.POV.actions.screeneffect.render.ScreenEffectActionRenderer.render(
                    matrices,
                    batcher,
                    actions,
                    replayTick,
                    width,
                    height);
            }

            if (showHud && state != null && state.visible)
            {
                HudRenderer.renderHotbars(matrices, batcher, List.of(state), 0, 0, width, height);
                batcher.flush();
                if (!showActions || actions == null || actions.getActiveGui(replayTick) == null)
                {
                    HeldItemTooltipRenderer.renderPlayback(
                        batcher, replay.keyframes, state, replayTick, width, height);
                }
            }

            /* Clip Crosshair is independent from the clip HUD toggle, but
             * still follows the replay's authored crosshair state. */
            boolean stateAllowsCrosshair = state == null || state.crosshair;
            boolean showCrosshair = showCrosshairOutput
                && stateAllowsCrosshair
                && (!showActions || actions == null || (actions.getActiveGui(replayTick) == null
                    && !MenuActionRenderer.blocksCrosshair(actions, replayTick)));

            if (showCrosshair)
            {
                PovCrosshairRenderer.render(
                    batcher,
                    width,
                    height,
                    AttackIndicatorRenderer.progress(replay.keyframes, hud, hand, replayTick));
            }

            if (showActions && actions != null)
            {
                BossBarActionRenderer.render(batcher, actions, replayTick, width, height);
                Glaxium.POV.actions.statuseffect.render.StatusEffectActionRenderer.renderHUD(batcher, actions, replayTick, width, height);
                Glaxium.POV.actions.chat.render.ChatActionRenderer.renderHUD(batcher, film, actions, replayTick, filmTick, width, height, state);
                Glaxium.POV.actions.toast.render.ToastActionRenderer.renderHUD(batcher, actions, replayTick, width, height);
                GuiActionRenderer.render(
                    matrices,
                    batcher,
                    replay.keyframes,
                    actions,
                    hand,
                    replayTick,
                    width,
                    height,
                    showCursor);
                MenuActionRenderer.render(
                    matrices,
                    batcher,
                    actions,
                    replayTick,
                    width,
                    height);
            }

            if (showCursor && !MenuActionRenderer.hidesCursor(actions, replayTick))
            {
                PovCursorRenderer.render(batcher, state, width, height);
            }
        }
        finally
        {
            if (batcher.getContext() != null)
            {
                batcher.getContext().draw();
            }
            batcher.flush();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.setProjectionMatrix(previousProjection, VertexSorter.BY_Z);
        }
    }

    /** Render path used only by Films.playFilm()/Right Ctrl. */
    public static void renderPlayback(Batcher2D batcher, float tickDelta)
    {
        PovPlaybackContext.Frame playback = PovPlaybackContext.getActive(tickDelta);

        if (playback == null
            || !(playback.replay().keyframes instanceof ReplayKeyframesPovAccess access))
        {
            return;
        }

        PovCameraClip clip = playback.clip();
        boolean showHud = clip == null || clip.hud.get();
        boolean showCrosshairOutput = clip == null || clip.crosshair.get();
        boolean showActions = clip == null || clip.actions.get();
        boolean showCursor = clip == null || clip.cursor.get();
        RecordedHudData hud = access.bbsPov$getHud();
        Glaxium.POV.hand.RecordedHandData hand = access.bbsPov$getHand();
        RecordedPovActions actions = access.bbsPov$getActions();
        HudState state = hud == null
            ? null
            : HudSampler.sample(hud, playback.replay().keyframes, playback.replayTick());

        int width = PovViewportMetrics.getMinecraftScaledWidth();
        int height = PovViewportMetrics.getMinecraftScaledHeight();
        MatrixStack matrices = batcher.getContext().getMatrices();
        Matrix4f previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        Matrix4f screenProjection = new Matrix4f().ortho(0F, width, height, 0F, -1000F, 3000F);

        RenderSystem.setProjectionMatrix(screenProjection, VertexSorter.BY_Z);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
            GlStateManager.SrcFactor.SRC_ALPHA,
            GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SrcFactor.ZERO,
            GlStateManager.DstFactor.ONE
        );

        try
        {
            if (showActions && actions != null)
            {
                Glaxium.POV.actions.screeneffect.render.ScreenEffectActionRenderer.render(
                    matrices,
                    batcher,
                    actions,
                    playback.replayTick(),
                    width,
                    height);
            }

            if (showHud && state != null && state.visible)
            {
                HudRenderer.renderHotbars(matrices, batcher, List.of(state), 0, 0, width, height);
                batcher.flush();
                if (!showActions || actions == null || actions.getActiveGui(playback.replayTick()) == null)
                {
                    HeldItemTooltipRenderer.renderPlayback(
                        batcher,
                        playback.replay().keyframes,
                        state,
                        playback.replayTick(),
                        width,
                        height);
                }
            }

            boolean stateAllowsCrosshair = state == null || state.crosshair;
            boolean showCrosshair = showCrosshairOutput
                && stateAllowsCrosshair
                && (!showActions || actions == null || (actions.getActiveGui(playback.replayTick()) == null
                    && !MenuActionRenderer.blocksCrosshair(actions, playback.replayTick())));

            if (showCrosshair)
            {
                PovCrosshairRenderer.render(
                    batcher,
                    width,
                    height,
                    AttackIndicatorRenderer.progress(
                        playback.replay().keyframes,
                        hud,
                        hand,
                        playback.replayTick()));
            }

            if (showActions && actions != null)
            {
                BossBarActionRenderer.render(
                    batcher,
                    actions,
                    playback.replayTick(),
                    width,
                    height);
                Glaxium.POV.actions.statuseffect.render.StatusEffectActionRenderer.renderHUD(
                    batcher,
                    actions,
                    playback.replayTick(),
                    width,
                    height);
                Glaxium.POV.actions.chat.render.ChatActionRenderer.renderHUD(
                    batcher,
                    playback.film(),
                    actions,
                    playback.replayTick(),
                    (float) playback.filmTick(),
                    width,
                    height,
                    state);
                Glaxium.POV.actions.toast.render.ToastActionRenderer.renderHUD(
                    batcher,
                    actions,
                    playback.replayTick(),
                    width,
                    height);
                GuiActionRenderer.render(
                    matrices,
                    batcher,
                    playback.replay().keyframes,
                    actions,
                    hand,
                    playback.replayTick(),
                    width,
                    height,
                    showCursor);
                MenuActionRenderer.render(
                    matrices,
                    batcher,
                    actions,
                    playback.replayTick(),
                    width,
                    height);
            }

            if (showCursor && !MenuActionRenderer.hidesCursor(actions, playback.replayTick()))
            {
                PovCursorRenderer.render(batcher, state, width, height);
            }
        }
        finally
        {
            if (batcher.getContext() != null)
            {
                batcher.getContext().draw();
            }
            batcher.flush();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.setProjectionMatrix(previousProjection, VertexSorter.BY_Z);
        }
    }

}

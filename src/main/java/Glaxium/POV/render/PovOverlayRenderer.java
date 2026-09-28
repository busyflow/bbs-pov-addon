package Glaxium.POV.render;

import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.actions.bossbar.render.BossBarActionRenderer;
import Glaxium.POV.actions.chat.render.ChatActionRenderer;
import Glaxium.POV.actions.gui.render.GuiActionRenderer;
import Glaxium.POV.actions.menu.render.MenuActionRenderer;
import Glaxium.POV.actions.screeneffect.render.ScreenEffectActionRenderer;
import Glaxium.POV.actions.statuseffect.render.StatusEffectActionRenderer;
import Glaxium.POV.actions.toast.render.ToastActionRenderer;
import Glaxium.POV.camera.clip.PovCameraClip;
import Glaxium.POV.camera.clip.PovCameraClips;
import Glaxium.POV.editor.UIPovHandEditor;
import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.hud.HudState;
import Glaxium.POV.hud.RecordedHudData;
import Glaxium.POV.hud.playback.HudSampler;
import Glaxium.POV.hud.render.AttackIndicatorRenderer;
import Glaxium.POV.hud.render.HeldItemTooltipRenderer;
import Glaxium.POV.hud.render.HudRenderer;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.playback.PovPlaybackContext;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import java.util.List;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public final class PovOverlayRenderer {
   private static boolean vignetteDrawnThisFrame;

   private PovOverlayRenderer() {
   }

   public static void beginFrame() {
      vignetteDrawnThisFrame = false;
   }

   public static boolean shouldSkipHudVignette() {
      return false;
   }

   public static void render(MatrixStack matrices, Batcher2D batcher) {
      UIFilmPanel filmPanel = PovViewportMetrics.resolveFilmPanel();
      if (filmPanel != null) {
         float tickDelta = MinecraftClient.getInstance().getTickDelta();
         float filmTick;
         if (filmPanel.getRunner() != null && filmPanel.getRunner().isRunning()) {
            filmTick = (float)filmPanel.getRunner().ticks + tickDelta;
         } else if (filmPanel.getController().isPlaying()) {
            filmTick = (float)filmPanel.getCursor() + tickDelta;
         } else {
            filmTick = (float)filmPanel.getCursor();
         }

         int povMode = filmPanel.getController().getPovMode();
         if (povMode != 1 && povMode != 2 && !UIPovHandEditor.isActive()) {
            boolean povEditMode = povMode == 6;
            Film film = (Film)filmPanel.getData();
            PovCameraClip clip = povEditMode ? null : PovCameraClips.resolve(film, filmTick);
            Replay replay = povEditMode ? filmPanel.replayEditor.getReplay() : PovCameraClips.resolveReplay(film, clip);
            if (replay != null && replay.keyframes instanceof ReplayKeyframesPovAccess access) {
               boolean showHud = povEditMode || clip != null && (Boolean)clip.hud.get();
               boolean showCrosshairOutput = povEditMode || clip != null && (Boolean)clip.crosshair.get();
               boolean showActions = povEditMode || clip != null && (Boolean)clip.actions.get();
               boolean showScreenEffects = povEditMode || clip != null && (Boolean)clip.actions.get() && (Boolean)clip.screenEffects.get();
               boolean showCursor = povEditMode || clip != null && (Boolean)clip.cursor.get();
               RecordedHudData hud = access.bbsPov$getHud();
               RecordedHandData hand = access.bbsPov$getHand();
               RecordedPovActions actions = access.bbsPov$getActions();
               int looping = (Integer)replay.looping.get();
               float replayTick = looping > 0 ? filmTick % (float)looping : filmTick;
               HudState state = hud == null ? null : HudSampler.sample(hud, replay.keyframes, replayTick);
               int width = PovViewportMetrics.getFilmScaledWidth();
               int height = PovViewportMetrics.getFilmScaledHeight();
               Matrix4f previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
               Matrix4f screenProjection = new Matrix4f().ortho(0.0F, (float)width, (float)height, 0.0F, -1000.0F, 3000.0F);
               RenderSystem.setProjectionMatrix(screenProjection, VertexSorter.BY_Z);
               MinecraftClient client = MinecraftClient.getInstance();
               if (client.gameRenderer == null
                  || client.gameRenderer.getCamera() == null
                  || client.gameRenderer.getCamera().getSubmersionType() != CameraSubmersionType.LAVA) {
                  BackgroundRenderer.clearFog();
               }

               try {
                  if (showScreenEffects && actions != null) {
                     ScreenEffectActionRenderer.render(matrices, batcher, actions, replayTick, width, height);
                  }

                  if (showHud && state != null && state.visible) {
                     HudRenderer.renderHotbars(matrices, batcher, List.of(state), 0, 0, width, height);
                     batcher.flush();
                     if (!showActions || actions == null || actions.getActiveGui(replayTick) == null) {
                        HeldItemTooltipRenderer.renderPlayback(batcher, replay.keyframes, state, replayTick, width, height);
                     }
                  }

                  boolean isFirstPerson = MinecraftClient.getInstance().options.getPerspective().isFirstPerson();
                  boolean showCrosshair = isFirstPerson
                     && showCrosshairOutput
                     && state != null
                     && state.visible
                     && state.crosshair
                     && (
                        !showActions || actions == null || actions.getActiveGui(replayTick) == null && !MenuActionRenderer.blocksCrosshair(actions, replayTick)
                     );
                  if (showCrosshair) {
                     PovCrosshairRenderer.render(batcher, width, height, AttackIndicatorRenderer.progress(replay.keyframes, hud, hand, replayTick));
                  }

                  if (showActions && actions != null) {
                     GuiActionRenderer.render(matrices, batcher, replay.keyframes, actions, hand, replayTick, width, height, showCursor);
                     BossBarActionRenderer.render(batcher, actions, replayTick, width, height);
                     StatusEffectActionRenderer.renderHUD(batcher, actions, replayTick, width, height);
                     ToastActionRenderer.renderHUD(batcher, actions, replayTick, width, height);
                     ChatActionRenderer.renderHUD(batcher, film, actions, replayTick, filmTick, width, height, state);
                     MenuActionRenderer.render(matrices, batcher, actions, replayTick, width, height);
                  }

                  if (showCursor && !MenuActionRenderer.hidesCursor(actions, replayTick)) {
                     PovCursorRenderer.render(batcher, state, width, height);
                  }
               } finally {
                  if (batcher.getContext() != null) {
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
      }
   }

   public static void renderPlayback(Batcher2D batcher, float tickDelta) {
      PovPlaybackContext.Frame playback = PovPlaybackContext.getActive(tickDelta);
      if (playback != null && playback.replay().keyframes instanceof ReplayKeyframesPovAccess access) {
         PovCameraClip clip = playback.clip();
         boolean showHud = (Boolean)clip.hud.get();
         boolean showCrosshairOutput = (Boolean)clip.crosshair.get();
         boolean showActions = (Boolean)clip.actions.get();
         boolean showCursor = (Boolean)clip.cursor.get();
         RecordedHudData hud = access.bbsPov$getHud();
         RecordedHandData hand = access.bbsPov$getHand();
         RecordedPovActions actions = access.bbsPov$getActions();
         HudState state = hud == null ? null : HudSampler.sample(hud, playback.replay().keyframes, playback.replayTick());
         int width = PovViewportMetrics.getMinecraftScaledWidth();
         int height = PovViewportMetrics.getMinecraftScaledHeight();
         MatrixStack matrices = batcher.getContext().getMatrices();
         Matrix4f previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
         Matrix4f screenProjection = new Matrix4f().ortho(0.0F, (float)width, (float)height, 0.0F, -1000.0F, 3000.0F);
         RenderSystem.setProjectionMatrix(screenProjection, VertexSorter.BY_Z);

         try {
            if (showActions && actions != null) {
               ScreenEffectActionRenderer.render(matrices, batcher, actions, playback.replayTick(), width, height);
            }

            if (showHud && state != null && state.visible) {
               HudRenderer.renderHotbars(matrices, batcher, List.of(state), 0, 0, width, height);
               batcher.flush();
               if (!showActions || actions == null || actions.getActiveGui(playback.replayTick()) == null) {
                  HeldItemTooltipRenderer.renderPlayback(batcher, playback.replay().keyframes, state, playback.replayTick(), width, height);
               }
            }

            boolean isFirstPerson = MinecraftClient.getInstance().options.getPerspective().isFirstPerson();
            boolean showCrosshair = isFirstPerson
               && showCrosshairOutput
               && state != null
               && state.visible
               && state.crosshair
               && (
                  !showActions
                     || actions == null
                     || actions.getActiveGui(playback.replayTick()) == null && !MenuActionRenderer.blocksCrosshair(actions, playback.replayTick())
               );
            if (showCrosshair) {
               PovCrosshairRenderer.render(
                  batcher, width, height, AttackIndicatorRenderer.progress(playback.replay().keyframes, hud, hand, playback.replayTick())
               );
            }

            if (showActions && actions != null) {
               GuiActionRenderer.render(matrices, batcher, playback.replay().keyframes, actions, hand, playback.replayTick(), width, height, showCursor);
               BossBarActionRenderer.render(batcher, actions, playback.replayTick(), width, height);
               StatusEffectActionRenderer.renderHUD(batcher, actions, playback.replayTick(), width, height);
               ToastActionRenderer.renderHUD(batcher, actions, playback.replayTick(), width, height);
               ChatActionRenderer.renderHUD(batcher, playback.film(), actions, playback.replayTick(), (float)playback.filmTick(), width, height, state);
               MenuActionRenderer.render(matrices, batcher, actions, playback.replayTick(), width, height);
            }

            if (showCursor && !MenuActionRenderer.hidesCursor(actions, playback.replayTick())) {
               PovCursorRenderer.render(batcher, state, width, height);
            }
         } finally {
            if (batcher.getContext() != null) {
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
}

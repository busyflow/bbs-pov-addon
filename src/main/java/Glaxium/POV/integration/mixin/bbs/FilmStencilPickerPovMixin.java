package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.PovAddon;
import Glaxium.POV.camera.clip.PovCameraClip;
import Glaxium.POV.camera.clip.PovCameraClips;
import Glaxium.POV.editor.UIPovHandEditor;
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
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
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

@Mixin(
   value = {FilmStencilPicker.class},
   remap = false
)
public class FilmStencilPickerPovMixin {
   @Shadow
   @Final
   private UIFilmController controller;
   @Shadow
   @Final
   private StencilFormFramebuffer stencil;
   @Shadow
   @Final
   private StencilMap stencilMap;

   @Inject(
      method = {"renderStencil"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$renderHandPicker(WorldRenderContext worldContext, UIContext context, boolean alt, CallbackInfo info) {
      if (this.controller.getPovMode() == 6) {
         UIFilmPanel panel;
         Area viewport;
         boolean var10000;
         label111: {
            info.cancel();
            panel = this.controller.panel;
            viewport = panel.preview.getViewport();
            if (panel instanceof UIFilmPanelPovAccess access && access.bbsPov$getEditor() != null && access.bbsPov$getEditor().isPoseGizmoSection()) {
               var10000 = true;
               break label111;
            }

            var10000 = false;
         }

         boolean handEditor = var10000;
         if (handEditor && viewport.isInside(context) && viewport.w > 0 && viewport.h > 0) {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (player == null) {
               this.stencil.clearPicking();
               PovHandPicking.abortStencil();
            } else {
               this.stencil.setup(Link.bbs("stencil_film"));
               Texture texture = this.stencil.getFramebuffer().getMainTexture();
               int videoWidth = BBSRendering.getVideoWidth();
               int videoHeight = BBSRendering.getVideoHeight();
               if (texture.width != videoWidth || texture.height != videoHeight) {
                  this.stencil.resizeGUI(videoWidth, videoHeight);
                  texture = this.stencil.getFramebuffer().getMainTexture();
               }

               this.stencilMap.setup();
               this.stencil.apply();
               PovHandPicking.beginStencil(this.stencilMap);
               MatrixStack matrices = worldContext.matrixStack();
               matrices.push();
               matrices.loadIdentity();

               try {
                  Matrix4f handProjection = PovHandPicking.getProjection();
                  if (handProjection != null) {
                     RenderSystem.setProjectionMatrix(handProjection, VertexSorter.BY_Z);
                  }

                  boolean began = PovHandPlayback.begin(worldContext.tickDelta());
                  if (began) {
                     PovHandPlayback.useCapturedPose(PovHandMatrices.getCapturedPose());
                     Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();
                     BlockPos cameraPos = BlockPos.ofFloored(panel.getCamera().position.x, panel.getCamera().position.y, panel.getCamera().position.z);
                     int light = client.world == null ? 15728880 : WorldRenderer.getLightmapCoordinates(client.world, cameraPos);
                     PovHandPlayback.render(client.gameRenderer.firstPersonRenderer, worldContext.tickDelta(), matrices, consumers, player, light);
                     PovHandGizmo.renderStencil(this.stencilMap);
                  }

                  int x = (int)((float)(context.mouseX - viewport.x) / (float)viewport.w * (float)texture.width);
                  int y = (int)((1.0F - (float)(context.mouseY - viewport.y) / (float)viewport.h) * (float)texture.height);
                  int tolerance = Math.round((float)((Integer)BBSSettings.gizmoHoverTolerance.get() * texture.width) / (float)viewport.w);
                  this.stencil.pick(x, y, tolerance, 19);
                  this.stencil.unbind(this.stencilMap);
                  PovHandPicking.finishStencil(this.stencil.getPicked());
               } catch (Throwable var22) {
                  PovAddon.LOGGER.error("POV hand stencil render failed", var22);
                  this.stencil.unbind(this.stencilMap);
                  PovHandPicking.abortStencil();
               } finally {
                  PovHandPlayback.end();
                  matrices.pop();
                  client.getFramebuffer().beginWrite(true);
               }
            }
         } else {
            this.stencil.clearPicking();
            PovHandPicking.abortStencil();
         }
      }
   }

   @Redirect(
      method = {"renderStencil"},
      at = @At(
         value = "INVOKE",
         target = "Lmchorse/bbs_mod/film/FilmEntityRenderer;renderEntity(Lmchorse/bbs_mod/film/FilmControllerContext;)V"
      )
   )
   private void bbsPov$hideHeadLookSourceFromPicker(FilmControllerContext renderContext) {
      UIFilmPanel panel = this.controller.panel;
      if (!UIPovHandEditor.isActive() || renderContext.replay != panel.replayEditor.getReplay()) {
         int povMode = this.controller.getPovMode();
         if (povMode != 1 && povMode != 2) {
            Film film = (Film)panel.getData();
            float filmTick = panel.getRunner() != null && panel.getRunner().isRunning()
               ? (float)panel.getRunner().ticks + renderContext.transition
               : (float)panel.getCursor();
            PovCameraClip clip = PovCameraClips.resolve(film, filmTick);
            if (clip == null || !(Boolean)clip.headLook.get() || renderContext.replay != PovCameraClips.resolveReplay(film, clip)) {
               FilmEntityRenderer.renderEntity(renderContext);
            }
         } else {
            FilmEntityRenderer.renderEntity(renderContext);
         }
      }
   }
}

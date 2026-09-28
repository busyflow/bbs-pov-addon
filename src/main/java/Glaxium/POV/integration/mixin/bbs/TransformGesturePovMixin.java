package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.editor.UIPovHandEditor;
import Glaxium.POV.hud.editor.IUIPropTransform2DLayout;
import Glaxium.POV.integration.access.bbs.IGizmoDragFirstPerson;
import Glaxium.POV.render.PovViewportMetrics;
import Glaxium.POV.replay.PovReplaySettings;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.drag.TransformGesture;
import mchorse.bbs_mod.ui.framework.elements.input.drag.TransformOp;
import mchorse.bbs_mod.ui.framework.elements.input.drag.DragStrategyFactory.Variant;
import mchorse.bbs_mod.ui.framework.elements.input.drag.TransformGesture.Host;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.GizmoDrag;
import mchorse.bbs_mod.utils.Axis;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.MinecraftClient;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {TransformGesture.class},
   remap = false
)
public abstract class TransformGesturePovMixin {
   @Shadow
   @Final
   private Host host;
   @Shadow
   private boolean editing;
   @Shadow
   private Axis axis;
   @Shadow
   private Axis axis2;
   @Shadow
   private boolean hotkeyMode;
   @Shadow
   @Final
   private Transform cache;
   @Unique
   private UIElement bbsPov$axisGuide;
   @Unique
   private int bbsPov$startMouseX;
   @Unique
   private int bbsPov$startMouseY;
   @Unique
   private double bbsPov$startTranslateX;
   @Unique
   private double bbsPov$startTranslateY;
   @Unique
   private Vector3f bbsPov$rotationPivot;
   @Unique
   private Vector3f bbsPov$startTranslate;
   @Unique
   private Quaternionf bbsPov$startRotation;

   @Shadow
   public abstract void accept();

   @Shadow
   public abstract void reject();

   @Shadow
   public abstract void enableMode(TransformOp var1, Axis var2, Axis var3);

   @Shadow
   public abstract TransformOp getOp();

   @Shadow
   public abstract Axis getAxis();

   @Shadow
   public abstract Axis getAxis2();

   @Shadow
   public abstract boolean isEditing();

   @Unique
   private boolean bbsPov$isLayout() {
      if (this.host instanceof IUIPropTransform2DLayout layout && layout.bbsPov$is2DLayout()) {
         return true;
      }

      return false;
   }

   @Inject(
      method = {"enableMode(Lmchorse/bbs_mod/ui/framework/elements/input/drag/TransformOp;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$enableTwoDimensionalMode(TransformOp mode, CallbackInfo info) {
      if (this.bbsPov$isLayout()) {
         if (mode == TransformOp.TRANSLATE) {
            this.enableMode(TransformOp.TRANSLATE, Axis.X, Axis.Y);
            this.bbsPov$ensureGuide();
            info.cancel();
         } else if (mode == TransformOp.ROTATE) {
            info.cancel();
         }
      }
   }

   @Inject(
      method = {"startEdit"},
      at = {@At("TAIL")}
   )
   private void bbsPov$captureStartDrag(TransformOp op, Axis axis, Axis axis2, Variant variant, GizmoDrag gizmoDrag, boolean hotkey, CallbackInfo info) {
      label24: {
         if (gizmoDrag instanceof IGizmoDragFirstPerson fpDrag && fpDrag.bbsPov$getRotationPivot() != null) {
            this.bbsPov$rotationPivot = new Vector3f(fpDrag.bbsPov$getRotationPivot());
            if (this.cache != null) {
               this.bbsPov$startTranslate = new Vector3f(this.cache.translate);
               this.bbsPov$startRotation = this.cache.createRotation();
            }
            break label24;
         }

         this.bbsPov$rotationPivot = null;
         this.bbsPov$startTranslate = null;
         this.bbsPov$startRotation = null;
      }

      if (this.bbsPov$isLayout()) {
         UIContext context = this.host.getContext();
         if (context != null) {
            this.bbsPov$startMouseX = context.mouseX;
            this.bbsPov$startMouseY = context.mouseY;
         }

         if (this.cache != null) {
            this.bbsPov$startTranslateX = (double)(Object)this.cache.translate.x;
            this.bbsPov$startTranslateY = (double)(Object)this.cache.translate.y;
         }
      }
   }

   @Inject(
      method = {"updateDrag"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$finishReleasedMouseDrag(UIContext context, CallbackInfo info) {
      if (UIPovHandEditor.isActive() && this.editing && !this.hotkeyMode) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.getWindow() != null && GLFW.glfwGetMouseButton(client.getWindow().getHandle(), 0) == 0) {
            this.accept();
            info.cancel();
         }
      }
   }

   @Inject(
      method = {"updateDrag"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$updateTwoDimensionalDrag(UIContext context, CallbackInfo info) {
      if (this.bbsPov$isLayout()) {
         if (this.isEditing() && this.getOp() == TransformOp.TRANSLATE) {
            int currentMouseX = context.mouseX;
            int currentMouseY = context.mouseY;
            double dx = (double)(currentMouseX - this.bbsPov$startMouseX);
            double dy = (double)(currentMouseY - this.bbsPov$startMouseY);
            if (Window.isShiftPressed()) {
               dx *= 0.1;
               dy *= 0.1;
            }

            Area viewport = this.bbsPov$getViewport();
            double filmWidth = this.bbsPov$getFilmGuiWidth();
            double filmHeight = this.bbsPov$getFilmGuiHeight();
            double factorX = viewport != null && viewport.w > 0 ? filmWidth / (double)viewport.w : 1.0;
            double factorY = viewport != null && viewport.h > 0 ? filmHeight / (double)viewport.h : 1.0;
            double layoutDeltaX = dx * factorX / 2.0;
            double layoutDeltaY = dy * factorY / 2.0;
            double newX = this.bbsPov$startTranslateX;
            double newY = this.bbsPov$startTranslateY;
            Axis currentAxis = this.getAxis();
            Axis currentAxis2 = this.getAxis2();
            if (currentAxis2 != null) {
               newX += layoutDeltaX;
               newY -= layoutDeltaY;
            } else if (currentAxis == Axis.X) {
               newX += layoutDeltaX;
            } else if (currentAxis == Axis.Y) {
               newY -= layoutDeltaY;
            }

            if (Window.isCtrlPressed()) {
               newX = (double)Math.round(newX);
               newY = (double)Math.round(newY);
            }

            this.host.setT(null, newX, newY, this.cache != null ? (double)(Object)this.cache.translate.z : 0.0);
            this.host.refreshFields();
            info.cancel();
         }
      }
   }

   @Inject(
      method = {"setAxis"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$rejectLayoutZ(Axis axis, CallbackInfo info) {
      if (this.bbsPov$isLayout() && axis == Axis.Z) {
         info.cancel();
      }
   }

   @Inject(
      method = {"setAxis"},
      at = {@At("TAIL")}
   )
   private void bbsPov$showAxisGuide(Axis axis, CallbackInfo info) {
      if (this.bbsPov$isLayout() && axis != Axis.Z) {
         this.bbsPov$ensureGuide();
      }
   }

   @Unique
   private void bbsPov$ensureGuide() {
      UIContext context = this.host.getContext();
      if (context != null) {
         if (this.bbsPov$axisGuide == null) {
            this.bbsPov$axisGuide = new UIElement() {
               public void render(UIContext guideContext) {
                  if (TransformGesturePovMixin.this.isEditing() && TransformGesturePovMixin.this.getOp() == TransformOp.TRANSLATE) {
                     Area viewport = TransformGesturePovMixin.this.bbsPov$getViewport();
                     Transform transform = TransformGesturePovMixin.this.host.getTransform();
                     if (viewport != null && transform != null) {
                        int pivotX = TransformGesturePovMixin.this.bbsPov$getPivotX(viewport, transform);
                        int pivotY = TransformGesturePovMixin.this.bbsPov$getPivotY(viewport, transform);
                        guideContext.batcher.box((float)(pivotX - 4), (float)pivotY, (float)(pivotX + 5), (float)(pivotY + 1), -285212673);
                        guideContext.batcher.box((float)pivotX, (float)(pivotY - 4), (float)(pivotX + 1), (float)(pivotY + 5), -285212673);
                        if (TransformGesturePovMixin.this.getAxis2() == null && TransformGesturePovMixin.this.getAxis() == Axis.X) {
                           guideContext.batcher.box((float)viewport.x, (float)pivotY, (float)viewport.ex(), (float)(pivotY + 1), -855687104);
                        } else if (TransformGesturePovMixin.this.getAxis2() == null && TransformGesturePovMixin.this.getAxis() == Axis.Y) {
                           guideContext.batcher.box((float)pivotX, (float)viewport.y, (float)(pivotX + 1), (float)viewport.ey(), -868155584);
                        }
                     }
                  }

                  super.render(guideContext);
               }
            };
         }

         if (!this.bbsPov$axisGuide.hasParent()) {
            context.menu.overlay.add(this.bbsPov$axisGuide);
         }
      }
   }

   @Unique
   private Area bbsPov$getViewport() {
      UIFilmPanel panel = PovReplaySettings.getFilmPanel();
      return panel != null && panel.preview != null ? panel.preview.getViewport() : null;
   }

   @Unique
   private int bbsPov$getPivotX(Area viewport, Transform transform) {
      double filmWidth = this.bbsPov$getFilmGuiWidth();
      double filmX = filmWidth / 2.0 + (double)(transform.translate.x * 2.0F);
      return viewport.x + (int)Math.round(filmX * (double)viewport.w / filmWidth);
   }

   @Unique
   private int bbsPov$getPivotY(Area viewport, Transform transform) {
      double filmHeight = this.bbsPov$getFilmGuiHeight();
      double filmY = filmHeight / 2.0 - (double)(transform.translate.y * 2.0F);
      UIFilmPanel panel = PovReplaySettings.getFilmPanel();
      if (panel != null
         && panel.replayEditor != null
         && panel.replayEditor.keyframeEditor != null
         && panel.replayEditor.keyframeEditor.view != null
         && panel.replayEditor.keyframeEditor.view.getGraph() != null) {
         UIKeyframeSheet sheet = panel.replayEditor.keyframeEditor.view.getGraph().getLastSheet();
         if (sheet != null && ("hotbar_layout".equals(sheet.id) || "layout".equals(sheet.id))) {
            filmY = filmHeight - (double)(transform.translate.y * 2.0F);
         }
      }

      return viewport.y + (int)Math.round(filmY * (double)viewport.h / filmHeight);
   }

   @Unique
   private double bbsPov$getFilmGuiWidth() {
      return (double)PovViewportMetrics.getFilmScaledWidth();
   }

   @Unique
   private double bbsPov$getFilmGuiHeight() {
      return (double)PovViewportMetrics.getFilmScaledHeight();
   }

   @Inject(
      method = {"updateDrag"},
      at = {@At(
         value = "INVOKE",
         target = "Lmchorse/bbs_mod/ui/framework/elements/input/drag/TransformGesture$Host;refreshFields()V",
         shift = Shift.BEFORE
      )}
   )
   private void bbsPov$compensateFirstPersonRotationPivot(UIContext context, CallbackInfo info) {
      if (this.bbsPov$rotationPivot != null
         && this.getOp() == TransformOp.ROTATE
         && this.host != null
         && this.bbsPov$startTranslate != null
         && this.bbsPov$startRotation != null) {
         Transform curTransform = this.host.getTransform();
         if (curTransform != null) {
            Quaternionf curRotation = curTransform.createRotation();
            Vector3f r0 = this.bbsPov$startRotation.transform(new Vector3f(this.bbsPov$rotationPivot));
            Vector3f r1 = curRotation.transform(new Vector3f(this.bbsPov$rotationPivot));
            Vector3f delta = new Vector3f(r0).sub(r1);
            this.host
               .setT(
                  null,
                  (double)(this.bbsPov$startTranslate.x + delta.x),
                  (double)(this.bbsPov$startTranslate.y + delta.y),
                  (double)(this.bbsPov$startTranslate.z + delta.z)
               );
         }
      }
   }

   @Inject(
      method = {"sphereWorldRadius"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void bbsPov$fallbackSphereWorldRadius(CallbackInfoReturnable<Float> info) {
      if ((Float)info.getReturnValue() <= 0.0F) {
         info.setReturnValue(0.5F);
      }
   }

   @Inject(
      method = {"accept", "reject"},
      at = {@At("TAIL")}
   )
   private void bbsPov$cleanupDragState(CallbackInfo info) {
      this.bbsPov$rotationPivot = null;
      this.bbsPov$startTranslate = null;
      this.bbsPov$startRotation = null;
      if (this.bbsPov$axisGuide != null && this.bbsPov$axisGuide.hasParent()) {
         this.bbsPov$axisGuide.removeFromParent();
      }
   }
}

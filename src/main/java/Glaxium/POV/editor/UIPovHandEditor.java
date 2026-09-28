package Glaxium.POV.editor;

import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.hand.editor.HandBoneHierarchy;
import Glaxium.POV.hand.editor.HandBoneUtils;
import Glaxium.POV.hand.render.PovHandMatrices;
import Glaxium.POV.integration.access.bbs.UIFilmPanelPovAccess;
import java.util.List;
import java.util.Objects;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.cubic.IBoneHierarchy;
import mchorse.bbs_mod.cubic.IModel;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.BodyPart;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCacheEntry;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.forms.UIFormList;
import mchorse.bbs_mod.ui.forms.UIFormPalette;
import mchorse.bbs_mod.ui.forms.editors.UIFormEditor;
import mchorse.bbs_mod.ui.forms.editors.UIForms.FormEntry;
import mchorse.bbs_mod.ui.forms.editors.forms.UIModelForm;
import mchorse.bbs_mod.ui.forms.editors.panels.UIFormPanel;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIModelPoseEditor;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.EventPropagation;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.MinecraftClient;
import org.joml.Matrix4f;

public class UIPovHandEditor extends UIElement {
   private static UIPovHandEditor active;
   private final UIFilmPanel filmPanel;
   private final UIDashboard dashboard;
   private Replay replay;
   private RecordedHandData hand;
   private UIFormPalette palette;
   private UIFormEditor formEditor;
   private String filteredModel = "";
   private Form lastConfiguredForm;
   private String lastConfiguredModel;
   private boolean configuredCamera;

   public static boolean isActive() {
      return active != null;
   }

   public static UIPovHandEditor getActive() {
      return active;
   }

   public static UIPovHandEditor open(UIFilmPanel filmPanel, Replay replay, RecordedHandData hand) {
      if (filmPanel == null) {
         return null;
      } else {
         UIDashboard dashboard = filmPanel.dashboard;
         if (active != null) {
            active.closeEditor();
         }

         if (filmPanel.getContext() != null) {
            filmPanel.getContext().closeContextMenu();
         }

         UIElement container = (UIElement)(dashboard != null && dashboard.main != null ? dashboard.main : filmPanel.getRoot());
         if (container == null) {
            return null;
         } else {
            UIPovHandEditor editor = new UIPovHandEditor(filmPanel, dashboard, replay, hand);
            editor.resetFlex().relative(container).full(container);
            container.add(editor);
            editor.resize();
            active = editor;
            return editor;
         }
      }
   }

   public UIPovHandEditor(UIFilmPanel filmPanel, UIDashboard dashboard, Replay replay, RecordedHandData hand) {
      active = this;
      this.eventPropagataion(EventPropagation.BLOCK).markContainer();
      this.filmPanel = filmPanel;
      this.dashboard = dashboard;
      this.replay = replay;
      this.hand = hand;
      Form initial = hand == null ? null : hand.ensureBaseForm(replay == null ? null : (Form)replay.form.get());
      this.palette = new UIFormPalette(form -> {
         if (form != null && this.hand != null) {
            this.hand.baseForm.set(FormUtils.copy(form));
         }
      });
      this.palette.noBackground();
      this.palette.cantExit();
      this.palette.setSelected(initial);
      this.palette.edit(true);
      this.formEditor = this.palette.editor;
      this.formEditor.resetFlex().relative(this).full(this);
      this.configureEditor();
      this.add(this.formEditor);
   }

   private void configureEditor() {
      if (this.formEditor != null) {
         this.formEditor.renderer.setVisible(true);
         this.formEditor.renderer.grid = false;
         this.formEditor.renderer.updatable();
         Area gap = this.getGapArea();
         this.formEditor.renderer.area.set(gap.x, gap.y, gap.w, gap.h);
         this.formEditor.renderer.resetFlex().relative(this).xy(gap.x, gap.y).wh(gap.w, gap.h);
         this.formEditor.renderer.setPosition(0.0F, 0.0F, 0.0F);
         this.formEditor.renderer.setDistance(0.0F);
         this.formEditor.renderer.setRotation(0.0F, 0.0F);
         this.formEditor.icons.setVisible(false);
         this.formEditor.statesEditor.setVisible(false);
         this.formEditor.finish.setVisible(false);
         this.formEditor.openStateEditor.setVisible(false);
         this.formEditor.bodyPartEditor.useTarget.setVisible(false);
         this.formEditor.forms.x(0);
         FormEntry currentEntry = this.formEditor.formsList != null ? (FormEntry)this.formEditor.formsList.getCurrentFirst() : null;
         boolean isRoot = currentEntry == null || currentEntry.part == null;
         Form currentForm = isRoot ? this.getRootForm() : (currentEntry != null ? currentEntry.getForm() : this.formEditor.form);
         String currentModel = currentForm instanceof ModelForm mf ? (String)mf.model.get() : "";
         if (this.formEditor.editor instanceof UIModelForm modelEditor && modelEditor.modelPanel != null) {
            if (isRoot) {
               if (modelEditor.modelPanel.shapeKeysSection != null) {
                  modelEditor.modelPanel.shapeKeysSection.removeFromParent();
               }

               if (modelEditor.modelPanel.poseEditor != null
                  && (currentForm != this.lastConfiguredForm || !Objects.equals(currentModel, this.lastConfiguredModel))
                  && currentForm instanceof ModelForm mfx) {
                  filterModelPoseEditor(modelEditor.modelPanel.poseEditor, mfx);
               }
            } else if (modelEditor.modelPanel.poseEditor != null
               && (currentForm != this.lastConfiguredForm || !Objects.equals(currentModel, this.lastConfiguredModel))
               && currentForm instanceof ModelForm mfx) {
               ModelInstance instance = ModelFormRenderer.getModel(mfx);
               if (instance != null && instance.getModel() != null) {
                  modelEditor.modelPanel.poseEditor.fillGroups(instance.getModel(), instance.getFlippedParts(), false, null);
               }
            }
         }

         this.lastConfiguredForm = currentForm;
         this.lastConfiguredModel = currentModel;
      }
   }

   public static void filterModelPoseEditor(UIModelPoseEditor poseEditor, ModelForm modelForm) {
      if (poseEditor != null && modelForm != null) {
         ModelInstance instance = ModelFormRenderer.getModel(modelForm);
         if (instance != null && instance.getModel() != null) {
            HandBoneUtils.HandBones handBones = HandBoneUtils.collect(instance);
            if (!handBones.depths().isEmpty()) {
               IBoneHierarchy filteredHierarchy = new HandBoneHierarchy(instance.getModel(), handBones);
               poseEditor.fillGroups(filteredHierarchy, instance.getFlippedParts(), false, null);
               String currentBone = poseEditor.getGroup();
               if (currentBone != null && !handBones.contains(currentBone)) {
                  String defaultBone = handBones.mainRoot() != null && handBones.contains(handBones.mainRoot())
                     ? handBones.mainRoot()
                     : handBones.depths().keySet().iterator().next();
                  poseEditor.selectBone(defaultBone);
               }
            }
         }
      }
   }

   public UIFilmPanel getFilmPanel() {
      return this.filmPanel;
   }

   public UIFormEditor getFormEditor() {
      return this.formEditor;
   }

   public Form getRootForm() {
      if (this.formEditor != null && this.formEditor.formsList != null) {
         List<FormEntry> list = this.formEditor.formsList.getList();
         if (list != null && !list.isEmpty() && list.get(0) != null && list.get(0).getForm() != null) {
            return list.get(0).getForm();
         }
      }

      if (this.formEditor != null && this.formEditor.form != null) {
         return FormUtils.getRoot(this.formEditor.form);
      } else {
         return this.hand != null ? (Form)this.hand.baseForm.get() : null;
      }
   }

   public Form getPreviewForm() {
      return this.getRootForm();
   }

   public static BodyPart findBodyPart(Form form) {
      if (form == null) {
         return null;
      } else {
         UIPovHandEditor editor = getActive();
         if (editor == null) {
            return null;
         } else {
            Form root = editor.getRootForm();
            if (root != null && root.parts != null) {
               for (BodyPart part : root.parts.getAllTyped()) {
                  if (part.getForm() == form) {
                     return part;
                  }
               }

               return null;
            } else {
               return null;
            }
         }
      }
   }

   public static int findBodyPartIndex(Form form) {
      UIPovHandEditor editor = getActive();
      if (editor == null) {
         return -1;
      } else {
         Form root = editor.getRootForm();
         if (root != null && root.parts != null) {
            int index = 0;

            for (BodyPart part : root.parts.getAllTyped()) {
               if (part.getForm() == form) {
                  return index;
               }

               index++;
            }

            return -1;
         } else {
            return -1;
         }
      }
   }

   public static Matrix4f getBodyPartParent(BodyPart part) {
      Matrix4f parent = new Matrix4f();
      if (part == null) {
         return parent;
      } else {
         String bone = (String)part.bone.get();
         if (bone != null && !bone.isEmpty()) {
            Matrix4f boneMat = PovHandMatrices.getFull(bone);
            if (boneMat != null) {
               parent.set(boneMat);
            } else {
               parent.rotateY((float) Math.PI);
            }
         } else {
            parent.translate(0.0F, -0.75F, -1.2F);
            parent.rotateY((float)Math.toRadians(180.0));
         }

         return parent;
      }
   }

   public static Matrix4f getBodyPartBase(BodyPart part) {
      Matrix4f base = getBodyPartParent(part);
      if (part != null && part.transform != null && part.transform.get() != null) {
         Matrix4f local = new Matrix4f();
         ((Transform)part.transform.get()).setupMatrix(local);
         base.mul(local);
      }

      return base;
   }

   public static Matrix4f getBodyPartBoneMatrix(ModelForm bodyPartForm, String bone, boolean local, Matrix4f bodyPartBase) {
      if (bodyPartForm != null && bone != null && !bone.isEmpty()) {
         ModelInstance instance = ModelFormRenderer.getModel(bodyPartForm);
         if (instance != null && instance.getModel() != null) {
            MatrixCache matrices = new MatrixCache();
            instance.captureMatrices(matrices);
            MatrixCacheEntry entry = matrices.get(bone);
            if (entry != null) {
               Matrix4f boneMat = local ? entry.matrix() : entry.origin();
               if (boneMat != null) {
                  return new Matrix4f(bodyPartBase).rotateY((float) Math.PI).mul(boneMat);
               }
            }

            return bodyPartBase;
         } else {
            return bodyPartBase;
         }
      } else {
         return bodyPartBase;
      }
   }

   public static Matrix4f evaluateBodyPartBoneMatrix(ModelForm bodyPartForm, String bone, Matrix4f bodyPartBase) {
      return getBodyPartBoneMatrix(bodyPartForm, bone, true, bodyPartBase);
   }

   public static Matrix4f evaluateBodyPartBoneMatrix(ModelForm bodyPartForm, String bone, Transform baseline, Transform edited, Matrix4f bodyPartBase) {
      if (bodyPartForm != null && bone != null && !bone.isEmpty()) {
         ModelInstance instance = ModelFormRenderer.getModel(bodyPartForm);
         if (instance != null && instance.getModel() != null) {
            IModel model = instance.getModel();
            Pose pose = (Pose)bodyPartForm.pose.get();
            Pose originalPose = pose == null ? new Pose() : pose.copy();
            Pose working = originalPose.copy();
            if (baseline != null && edited != null) {
               PoseTransform transform = working.transforms.computeIfAbsent(bone, ignored -> new PoseTransform());
               transform.translate
                  .add(edited.translate.x - baseline.translate.x, edited.translate.y - baseline.translate.y, edited.translate.z - baseline.translate.z);
               Transform rotationDelta = new Transform();
               rotationDelta.setModeQuaternion();
               rotationDelta.quat.set(baseline.createRotation()).invert().mul(edited.createRotation());
               transform.addRotation(rotationDelta);
            }

            Matrix4f var12;
            try {
               model.resetPose();
               model.applyPose(working);
               MatrixCache matrices = new MatrixCache();
               instance.captureMatrices(matrices);
               MatrixCacheEntry entry = matrices.get(bone);
               if (entry == null || entry.matrix() == null) {
                  return bodyPartBase;
               }

               var12 = new Matrix4f(bodyPartBase).rotateY((float) Math.PI).mul(entry.matrix());
            } finally {
               model.resetPose();
               model.applyPose(originalPose);
            }

            return var12;
         } else {
            return bodyPartBase;
         }
      } else {
         return bodyPartBase;
      }
   }

   public void closeEditor() {
      if (active == this) {
         active = null;
      }

      if (this.filmPanel != null && this.filmPanel.getContext() != null) {
         this.filmPanel.getContext().closeContextMenu();
      }

      this.syncToBaseForm();
      this.removeFromParent();
      if (this.dashboard != null) {
         this.dashboard.resize(this.dashboard.width, this.dashboard.height);
      }

      if (this.filmPanel instanceof UIFilmPanelPovAccess access && access.bbsPov$getEditor() != null) {
         access.bbsPov$getEditor().reloadHandModel();
      }
   }

   public void syncToBaseForm() {
      Form root = this.getRootForm();
      if (root != null && this.hand != null) {
         this.hand.baseForm.set(FormUtils.copy(root));
      }
   }

   public Replay getReplay() {
      return this.replay;
   }

   public Area getGapArea() {
      int screenW;
      int screenH;
      int left;
      int right;
      screenW = this.area.w > 0 ? this.area.w : MinecraftClient.getInstance().getWindow().getScaledWidth();
      screenH = this.area.h > 0 ? this.area.h : MinecraftClient.getInstance().getWindow().getScaledHeight();
      left = this.formEditor != null && this.formEditor.forms != null && this.formEditor.forms.isVisible() ? this.formEditor.forms.area.ex() : this.area.x;
      right = this.area.ex() > 0 ? this.area.ex() : screenW;
      label61:
      if (this.formEditor != null && this.formEditor.editor != null) {
         if (this.formEditor.editor.view instanceof UIFormPanel panel && panel.options != null && panel.options.isVisible()) {
            right = panel.options.area.x;
            break label61;
         }

         if (this.formEditor.editor instanceof UIModelForm modelEditor
            && modelEditor.modelPanel != null
            && modelEditor.modelPanel.options != null
            && modelEditor.modelPanel.options.isVisible()) {
            right = modelEditor.modelPanel.options.area.x;
         }
      }

      if (left <= 0) {
         left = (int)((float)screenW * 0.15F);
      }

      if (right >= screenW || right <= left) {
         right = (int)((float)screenW * 0.85F);
      }

      int gapY = this.area.y;
      int gapW = Math.max(1, right - left);
      int gapH = Math.max(1, this.area.h > 0 ? this.area.h : screenH);
      return new Area(left, gapY, gapW, gapH);
   }

   public Area getFrameArea() {
      Area gap = this.getGapArea();
      float aspect = 1.7777778F;
      int frameW = gap.w;
      int frameH = Math.round((float)frameW / aspect);
      if (frameH > gap.h) {
         frameH = gap.h;
         frameW = Math.round((float)frameH * aspect);
      }

      int frameX = gap.x + (gap.w - frameW) / 2;
      int frameY = gap.y + (gap.h - frameH) / 2;
      return new Area(frameX, frameY, frameW, frameH);
   }

   public void render(UIContext context) {
      this.configureEditor();
      context.batcher.box((float)this.area.x, (float)this.area.y, (float)this.area.ex(), (float)this.area.ey(), -15198184);
      Area gap = this.getGapArea();
      Area frame = this.getFrameArea();
      if (this.formEditor != null && this.formEditor.renderer != null) {
         this.formEditor.renderer.area.set(gap.x, gap.y, gap.w, gap.h);
         if (this.formEditor.renderer.getGizmoStencil() != null) {
            this.formEditor.renderer.getGizmoStencil().resizeGUI(gap.w, gap.h);
         }

         float halfTan = (float)Math.tan(Math.toRadians(35.0));
         float fovY = 2.0F * (float)Math.atan((double)(halfTan * ((float)gap.h / (float)frame.h)));
         this.formEditor.renderer.camera.fov = fovY;
      }

      boolean prevLight = BBSSettings.lightInputs;
      BBSSettings.lightInputs = true;

      try {
         super.render(context);
      } finally {
         BBSSettings.lightInputs = prevLight;
      }

      if (!this.isFormPickerOpen()) {
         if (frame.y > gap.y) {
            context.batcher.box((float)gap.x, (float)gap.y, (float)gap.ex(), (float)frame.y, -2013265920);
         }

         if (frame.ey() < gap.ey()) {
            context.batcher.box((float)gap.x, (float)frame.ey(), (float)gap.ex(), (float)gap.ey(), -2013265920);
         }

         if (frame.x > gap.x) {
            context.batcher.box((float)gap.x, (float)frame.y, (float)frame.x, (float)frame.ey(), -2013265920);
         }

         if (frame.ex() < gap.ex()) {
            context.batcher.box((float)frame.ex(), (float)frame.y, (float)gap.ex(), (float)frame.ey(), -2013265920);
         }

         context.batcher.outline((float)frame.x, (float)frame.y, (float)frame.ex(), (float)frame.ey(), -1996488705);
      }
   }

   public boolean isFormPickerOpen() {
      if (this.formEditor != null) {
         for (UIFormList list : this.formEditor.getChildren(UIFormList.class)) {
            if (list.isVisible()) {
               return true;
            }
         }
      }

      for (UIFormList listx : this.getChildren(UIFormList.class)) {
         if (listx.isVisible()) {
            return true;
         }
      }

      if (this.palette != null) {
         for (UIFormList listxx : this.palette.getChildren(UIFormList.class)) {
            if (listxx.isVisible()) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean subMouseReleased(UIContext context) {
      this.syncToBaseForm();
      return super.subMouseReleased(context);
   }

   public boolean subKeyPressed(UIContext context) {
      if (context.isPressed(256)) {
         this.closeEditor();
         return true;
      } else {
         return super.subKeyPressed(context);
      }
   }
}

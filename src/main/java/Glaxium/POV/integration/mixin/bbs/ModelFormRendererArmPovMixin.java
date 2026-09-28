package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.bodypart.PovBodyPartRenderer;
import Glaxium.POV.editor.UIPovHandEditor;
import Glaxium.POV.hand.editor.PovHandPicking;
import Glaxium.POV.hand.playback.PovHandPlayback;
import Glaxium.POV.hand.render.PovHandMatrices;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.cubic.IModel;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.model.ArmorSlot;
import mchorse.bbs_mod.cubic.model.bobj.BOBJModel;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.BodyPart;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {ModelFormRenderer.class},
   remap = false
)
public abstract class ModelFormRendererArmPovMixin extends FormRenderer<ModelForm> implements PovBodyPartRenderer {
   @Shadow
   private MatrixCache bones;
   @Shadow
   private IEntity entity;
   @Unique
   private ModelInstance bbsPov$armModel;
   @Unique
   private Map<ModelGroup, Boolean> bbsPov$armVisibility;
   @Unique
   private Map<BOBJBone, Boolean> bbsPov$armBobjVisibility;
   @Unique
   private Matrix4f bbsPov$armRenderBase;
   @Unique
   private Matrix4f bbsPov$editorSceneBase;
   @Unique
   private Hand bbsPov$renderedHand;
   @Unique
   private float bbsPov$renderTransition;
   @Unique
   private FormRenderingContext bbsPov$activeFormContext;
   @Unique
   private int bbsPov$activeBaseTarget;

   public ModelFormRendererArmPovMixin(ModelForm form) {
      super(form);
   }

   @Inject(
      method = {"renderFirstPersonHand"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$captureModelState(MatrixStack matrices, int light, Hand hand, CallbackInfoReturnable<Boolean> info) {
      ((ModelFormRenderer)(Object)this).ensureAnimator(0.0F);
      if (PovHandPlayback.isActive() && !PovHandPlayback.shouldRenderModelHand(hand)) {
         info.setReturnValue(true);
      } else {
         ModelInstance instance = ((ModelFormRenderer)(Object)this).getModel();
         if (instance != null && instance.getModel() != null) {
            this.bbsPov$armModel = instance;
            this.bbsPov$renderedHand = hand;
            this.bbsPov$armVisibility = new IdentityHashMap<>();
            this.bbsPov$armBobjVisibility = new IdentityHashMap<>();

            for (ModelGroup group : instance.getModel().getAllGroups()) {
               this.bbsPov$armVisibility.put(group, group.visible);
            }

            if (instance.getModel() instanceof BOBJModel bobjModel) {
               for (BOBJBone bone : bobjModel.getAllBOBJBones()) {
                  this.bbsPov$armBobjVisibility.put(bone, bone.visible);
               }

               ArmorSlot slot = hand == Hand.MAIN_HAND ? instance.getFpMain() : instance.getFpOffhand();
               if (slot != null && slot.group != null && !slot.group.isBlank()) {
                  for (BOBJBone bone : bobjModel.getAllBOBJBones()) {
                     bone.visible = bbsPov$belongsToSlot(bone, slot.group);
                  }
               }
            }
         }
      }
   }

   @Inject(
      method = {"renderFirstPersonHand"},
      at = {@At(
         value = "INVOKE",
         target = "Lmchorse/bbs_mod/forms/renderers/ModelFormRenderer;renderModel(Lmchorse/bbs_mod/forms/entities/IEntity;Ljava/util/function/Supplier;Lnet/minecraft/client/util/math/MatrixStack;Lmchorse/bbs_mod/cubic/ModelInstance;IILmchorse/bbs_mod/utils/colors/Color;Lmchorse/bbs_mod/utils/colors/Color;ZLmchorse/bbs_mod/ui/framework/elements/utils/StencilMap;FLnet/minecraft/client/util/math/MatrixStack;)V",
         shift = Shift.BEFORE
      )}
   )
   private void bbsPov$captureRenderBase(MatrixStack matrices, int light, Hand hand, CallbackInfoReturnable<Boolean> info) {
      if ((PovHandPlayback.isActive() || UIPovHandEditor.isActive()) && !PovHandPicking.isStencilPass()) {
         this.bbsPov$armRenderBase = new Matrix4f(matrices.peek().getPositionMatrix());
      }
   }

   @Inject(
      method = {"renderModel"},
      at = {@At(
         value = "INVOKE",
         target = "Lmchorse/bbs_mod/cubic/ModelInstance;render(Lnet/minecraft/client/util/math/MatrixStack;Ljava/util/function/Supplier;Lmchorse/bbs_mod/utils/colors/Color;IILmchorse/bbs_mod/ui/framework/elements/utils/StencilMap;Lmchorse/bbs_mod/obj/shapes/ShapeKeys;Ljava/util/function/Function;)V",
         shift = Shift.BEFORE
      )}
   )
   private void bbsPov$applyFinalHandPose(
      IEntity entity,
      Supplier<ShaderProgram> shader,
      MatrixStack matrices,
      ModelInstance instance,
      int light,
      int overlay,
      Color color,
      Color formColor,
      boolean additive,
      StencilMap stencilMap,
      float transition,
      MatrixStack world,
      CallbackInfo info
   ) {
      this.bbsPov$renderTransition = transition;
      boolean matched = instance == this.bbsPov$armModel;
      if (matched) {
         IModel model = instance.getModel();
         if (model != null) {
            if (PovHandPlayback.isActive() || UIPovHandEditor.isActive()) {
               model.resetPose();
               Pose pose = PovHandPlayback.getRenderPose();
               if (pose == null && UIPovHandEditor.isActive()) {
                  pose = ((ModelFormRenderer)(Object)this).getPose();
               }

               if (pose != null) {
                  model.applyPose(pose);
               }
            }

            if (model instanceof BOBJModel bobjModel && this.bbsPov$renderedHand != null) {
               ArmorSlot slot = this.bbsPov$renderedHand == Hand.MAIN_HAND ? instance.getFpMain() : instance.getFpOffhand();
               if (slot != null && slot.group != null && !slot.group.isBlank()) {
                  for (BOBJBone bone : bobjModel.getAllBOBJBones()) {
                     bone.visible = bbsPov$belongsToSlot(bone, slot.group);
                  }
               }
            }
         }
      }
   }

   @ModifyArg(
      method = {"renderFirstPersonHand"},
      at = @At(
         value = "INVOKE",
         target = "Lmchorse/bbs_mod/forms/renderers/ModelFormRenderer;renderModel(Lmchorse/bbs_mod/forms/entities/IEntity;Ljava/util/function/Supplier;Lnet/minecraft/client/util/math/MatrixStack;Lmchorse/bbs_mod/cubic/ModelInstance;IILmchorse/bbs_mod/utils/colors/Color;Lmchorse/bbs_mod/utils/colors/Color;ZLmchorse/bbs_mod/ui/framework/elements/utils/StencilMap;FLnet/minecraft/client/util/math/MatrixStack;)V"
      ),
      index = 9
   )
   private StencilMap bbsPov$renderHandStencil(StencilMap original) {
      if (this.bbsPov$activeFormContext != null && this.bbsPov$activeFormContext.isPicking() && this.bbsPov$activeFormContext.stencilMap != null) {
         return this.bbsPov$activeFormContext.stencilMap;
      } else {
         StencilMap map = PovHandPicking.getStencilMap();
         if (PovHandPicking.isStencilPass() && map != null) {
            ModelFormRenderer renderer = (ModelFormRenderer)(Object)this;
            ModelInstance instance = renderer.getModel();
            if (instance != null && instance.getModel() != null && PovHandPicking.beginModelMapping(instance)) {
               instance.fillStencilMap(map, renderer.getForm());
            }

            return map;
         } else {
            return original;
         }
      }
   }

   @ModifyArg(
      method = {"renderFirstPersonHand"},
      at = @At(
         value = "INVOKE",
         target = "Lmchorse/bbs_mod/forms/renderers/ModelFormRenderer;renderModel(Lmchorse/bbs_mod/forms/entities/IEntity;Ljava/util/function/Supplier;Lnet/minecraft/client/util/math/MatrixStack;Lmchorse/bbs_mod/cubic/ModelInstance;IILmchorse/bbs_mod/utils/colors/Color;Lmchorse/bbs_mod/utils/colors/Color;ZLmchorse/bbs_mod/ui/framework/elements/utils/StencilMap;FLnet/minecraft/client/util/math/MatrixStack;)V"
      ),
      index = 1
   )
   private Supplier<ShaderProgram> bbsPov$useNativePickerShader(Supplier<ShaderProgram> original) {
      if (this.bbsPov$activeFormContext != null && this.bbsPov$activeFormContext.isPicking()) {
         ShaderProgram program = BBSShaders.getPickerModelsProgram();
         GlUniform target = program.getUniform("Target");
         if (target != null) {
            target.set(this.bbsPov$activeBaseTarget);
         }

         return () -> program;
      } else {
         return PovHandPicking.isStencilPass() ? ModelFormRendererArmPovMixin::bbsPov$getPickerShader : original;
      }
   }

   @Unique
   private static ShaderProgram bbsPov$getPickerShader() {
      ShaderProgram program = BBSShaders.getPickerModelsProgram();
      GlUniform target = program.getUniform("Target");
      if (target != null) {
         target.set(PovHandPicking.getStencilTarget());
      }

      return program;
   }

   @Inject(
      method = {"renderFirstPersonHand"},
      at = {@At("RETURN")}
   )
   private void bbsPov$restoreModelState(MatrixStack matrices, int light, Hand hand, CallbackInfoReturnable<Boolean> info) {
      ModelInstance instance = this.bbsPov$armModel;
      Map<ModelGroup, Boolean> visibility = this.bbsPov$armVisibility;
      Map<BOBJBone, Boolean> bobjVisibility = this.bbsPov$armBobjVisibility;
      if (instance != null
         && this.bbsPov$armRenderBase != null
         && this.bbsPov$renderedHand != null
         && !PovHandPicking.isStencilPass()
         && (this.bbsPov$activeFormContext == null || !this.bbsPov$activeFormContext.isPicking())
         && (PovHandPlayback.isActive() || UIPovHandEditor.isActive())) {
         MatrixCache cache = new MatrixCache();
         instance.captureMatrices(cache);
         Matrix4f renderBase = this.bbsPov$editorSceneBase != null
            ? new Matrix4f(this.bbsPov$editorSceneBase).invert().mul(this.bbsPov$armRenderBase)
            : this.bbsPov$armRenderBase;
         PovHandMatrices.capture(
            instance,
            this.bbsPov$renderedHand == Hand.MAIN_HAND ? instance.getFpMain() : instance.getFpOffhand(),
            renderBase,
            cache,
            ((ModelFormRenderer)(Object)this).getPose()
         );
      }

      this.bbsPov$armModel = null;
      this.bbsPov$armVisibility = null;
      this.bbsPov$armBobjVisibility = null;
      this.bbsPov$armRenderBase = null;
      this.bbsPov$renderedHand = null;
      if (instance != null) {
         if (visibility != null) {
            for (Entry<ModelGroup, Boolean> entry : visibility.entrySet()) {
               entry.getKey().visible = entry.getValue();
            }
         }

         if (bobjVisibility != null) {
            for (Entry<BOBJBone, Boolean> entry : bobjVisibility.entrySet()) {
               entry.getKey().visible = entry.getValue();
            }
         }
      }
   }

   @Unique
   private static boolean bbsPov$belongsToSlot(BOBJBone bone, String slotGroup) {
      for (BOBJBone current = bone; current != null; current = current.parentBone) {
         if (slotGroup.equals(current.name)) {
            return true;
         }
      }

      return false;
   }

   @Inject(
      method = {"render3D"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$renderBothArmsInHandEditor(FormRenderingContext context, CallbackInfo info) {
      if (UIPovHandEditor.isActive()) {
         UIPovHandEditor editor = UIPovHandEditor.getActive();
         if (editor == null || editor.getRootForm() == null || ((ModelFormRenderer)(Object)this).getForm() != editor.getRootForm()) {
            return;
         }

         info.cancel();
         this.bbsPov$editorSceneBase = new Matrix4f(context.stack.peek().getPositionMatrix());
         ModelFormRenderer self = (ModelFormRenderer)(Object)this;
         self.ensureAnimator(0.0F);
         this.bbsPov$activeFormContext = context;
         this.bbsPov$activeBaseTarget = context.isPicking() && context.stencilMap != null ? context.getPickingIndex() : 0;
         boolean prevSuppress = PovHandPlayback.suppressFormTransform;
         PovHandPlayback.suppressFormTransform = true;

         try {
            this.bbsPov$renderFirstPersonArm(context, Hand.OFF_HAND);
            this.bbsPov$renderFirstPersonArm(context, Hand.MAIN_HAND);
            if (context.stencilMap != null) {
               ModelInstance model = self.getModel();
               if (model != null) {
                  model.fillStencilMap(context.stencilMap, self.getForm());
               }
            }

            if (self.getForm() != null && self.getForm().parts != null) {
               int partIndex = 0;

               for (BodyPart part : self.getForm().parts.getAllTyped()) {
                  if (part.getForm() == null) {
                     partIndex++;
                  } else {
                     context.stack.push();
                     if (context.world != null) {
                        context.world.push();
                     }

                     String bone = (String)part.bone.get();
                     if (bone != null && !bone.isEmpty()) {
                        Matrix4f boneMat = part.filterBoneMatrix(PovHandMatrices.getFull(bone));
                        if (boneMat != null) {
                           MatrixStackUtils.multiply(context.stack, boneMat);
                           if (context.world != null) {
                              MatrixStackUtils.multiply(context.world, boneMat);
                           }
                        } else {
                           context.stack.multiply(RotationAxis.POSITIVE_Y.rotation((float) Math.PI));
                           if (context.world != null) {
                              context.world.multiply(RotationAxis.POSITIVE_Y.rotation((float) Math.PI));
                           }
                        }
                     } else {
                        context.stack.translate(0.0F, -0.75F, -1.2F);
                        context.stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
                        if (context.world != null) {
                           context.world.translate(0.0F, -0.75F, -1.2F);
                           context.world.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
                        }
                     }

                     this.renderBodyPart(part, context);
                     Form attachment = part.getForm();
                     if (attachment instanceof ModelForm) {
                        ModelForm bodyPartModelForm = (ModelForm)attachment;
                        FormRenderer var19 = FormUtilsClient.getRenderer(part.getForm());
                        if (var19 instanceof ModelFormRenderer) {
                           ModelFormRenderer modelRenderer = (ModelFormRenderer)var19;
                           MatrixStack attachmentx = new MatrixStack();
                           attachmentx.loadIdentity();
                           if (bone != null && !bone.isBlank()) {
                              Matrix4f parent = PovHandMatrices.getFull(bone);
                              if (parent != null) {
                                 MatrixStackUtils.multiply(attachmentx, parent);
                              }
                           } else {
                              attachmentx.translate(0.0F, -0.75F, -1.2F);
                              attachmentx.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
                           }

                           String formPath = FormUtils.getPath(part.getForm());
                           PovHandMatrices.captureBodyPart(
                              formPath, modelRenderer, part.getRenderEntity(this.entity), attachmentx, (Transform)part.transform.get()
                           );
                           if (partIndex >= 0 && !String.valueOf(partIndex).equals(formPath)) {
                              PovHandMatrices.captureBodyPart(
                                 String.valueOf(partIndex), modelRenderer, part.getRenderEntity(this.entity), attachmentx, (Transform)part.transform.get()
                              );
                           }

                           if (part.getId() != null && !part.getId().equals(formPath) && !part.getId().equals(String.valueOf(partIndex))) {
                              PovHandMatrices.captureBodyPart(
                                 part.getId(), modelRenderer, part.getRenderEntity(this.entity), attachmentx, (Transform)part.transform.get()
                              );
                           }
                        }
                     }

                     context.stack.pop();
                     if (context.world != null) {
                        context.world.pop();
                     }

                     partIndex++;
                  }
               }
            }
         } finally {
            this.bbsPov$activeFormContext = null;
            this.bbsPov$editorSceneBase = null;
            PovHandPlayback.suppressFormTransform = prevSuppress;
         }
      }
   }

   @Inject(
      method = {"renderBodyParts"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$cancelNativeBodyPartsInHandEditor(FormRenderingContext context, CallbackInfo info) {
      if (UIPovHandEditor.isActive()) {
         UIPovHandEditor editor = UIPovHandEditor.getActive();
         if (editor != null && editor.getRootForm() != null && ((ModelFormRenderer)(Object)this).getForm() == editor.getRootForm()) {
            if (this.bones != null) {
               this.bones.clear();
            }

            info.cancel();
         }
      }
   }

   @Unique
   private void bbsPov$renderFirstPersonArm(FormRenderingContext context, Hand hand) {
      MatrixStack matrices = context.stack;
      matrices.push();
      boolean isRight = hand == Hand.MAIN_HAND;
      float f = isRight ? 1.0F : -1.0F;
      matrices.translate(f * 0.64000005F, -0.6F, -0.71999997F);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f * 45.0F));
      matrices.translate(f * -1.0F, 3.6F, 3.5F);
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f * 120.0F));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(200.0F));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f * -135.0F));
      matrices.translate(f * 5.6F, 0.0F, 0.0F);
      ((ModelFormRenderer)(Object)this).renderArm(matrices, LightmapTextureManager.pack(15, 15), null, hand);
      matrices.pop();
   }

   @Override
   public void bbsPov$renderBodyParts(int light, StencilMap stencilMap) {
      if (!UIPovHandEditor.isActive() && this.form != null && ((ModelForm)(Object)this.form).parts != null) {
         int partIndex = 0;
         boolean prevSuppress = PovHandPlayback.suppressFormTransform;
         PovHandPlayback.suppressFormTransform = true;

         try {
            for (BodyPart part : ((ModelForm)(Object)this.form).parts.getAllTyped()) {
               Form partForm = part.getForm();
               if (partForm != null && (Boolean)partForm.visible.get()) {
                  MatrixStack stack = new MatrixStack();
                  String bone = (String)part.bone.get();
                  if (bone != null && !bone.isBlank()) {
                     Matrix4f parent = PovHandMatrices.getFull(bone);
                     if (parent == null) {
                        partIndex++;
                        continue;
                     }

                     MatrixStackUtils.multiply(stack, parent);
                  } else {
                     stack.translate(0.0F, -0.75F, -1.2F);
                     stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
                  }

                  float transition = this.bbsPov$renderTransition;
                  if (transition == 0.0F) {
                     transition = PovHandPlayback.getActiveTransition();
                  }

                  FormRenderingContext context = new FormRenderingContext()
                     .set(FormRenderType.ITEM_FP, this.entity, stack, light, OverlayTexture.DEFAULT_UV, transition)
                     .stencilMap(stencilMap);
                  if (stencilMap != null) {
                     PovHandPicking.registerBodyPart(partForm, partIndex);
                  }

                  boolean depthTest = GL11.glIsEnabled(2929);
                  boolean depthWrite = GL11.glGetBoolean(2930);
                  int depthFunction = GL11.glGetInteger(2932);

                  try {
                     RenderSystem.enableDepthTest();
                     RenderSystem.depthMask(true);
                     RenderSystem.depthFunc(515);
                     this.renderBodyPart(part, context);
                  } finally {
                     RenderSystem.depthFunc(depthFunction == 519 ? 515 : depthFunction);
                     RenderSystem.depthMask(depthWrite);
                     if (depthTest) {
                        RenderSystem.enableDepthTest();
                     } else {
                        RenderSystem.disableDepthTest();
                     }
                  }

                  partIndex++;
               } else {
                  partIndex++;
               }
            }

            if (stencilMap == null) {
               this.bbsPov$captureBodyPartRecursive(this.form, this.entity, "");
            }
         } finally {
            PovHandPlayback.suppressFormTransform = prevSuppress;
         }
      }
   }

   @Unique
   private void bbsPov$captureBodyPartRecursive(Form form, IEntity entity, String parentPath) {
      if (form != null && form.parts != null) {
         for (BodyPart part : form.parts.getAllTyped()) {
            Form partForm = part.getForm();
            if (partForm != null && (Boolean)partForm.visible.get()) {
               String bone = (String)part.bone.get();
               MatrixStack attachment = new MatrixStack();
               attachment.loadIdentity();
               if (parentPath != null && !parentPath.isEmpty()) {
                  Matrix4f parent = bone != null && !bone.isBlank() ? PovHandMatrices.getFull(parentPath + "/" + bone) : PovHandMatrices.getFull(parentPath);
                  if (parent == null) {
                     parent = PovHandMatrices.getFull(parentPath);
                  }

                  if (parent != null) {
                     MatrixStackUtils.multiply(attachment, parent);
                  }
               } else if (bone != null && !bone.isBlank()) {
                  Matrix4f parentx = PovHandMatrices.getFull(bone);
                  if (parentx != null) {
                     MatrixStackUtils.multiply(attachment, parentx);
                  }
               } else {
                  attachment.translate(0.0F, -0.75F, -1.2F);
                  attachment.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
               }

               String formPath = FormUtils.getPath(partForm);
               if (partForm instanceof ModelForm && FormUtilsClient.getRenderer(partForm) instanceof ModelFormRenderer modelRenderer) {
                  PovHandMatrices.captureBodyPart(formPath, modelRenderer, part.getRenderEntity(entity), attachment, (Transform)part.transform.get());
                  MatrixStackUtils.applyTransform(attachment, (Transform)part.transform.get());
                  this.bbsPov$captureBodyPartRecursive(partForm, entity, formPath);
               }
            }
         }
      }
   }
}

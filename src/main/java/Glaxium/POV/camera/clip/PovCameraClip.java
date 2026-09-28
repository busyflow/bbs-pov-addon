package Glaxium.POV.camera.clip;

import java.util.List;
import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.clips.CameraClipContext;
import mchorse.bbs_mod.camera.clips.misc.TrackerFrame;
import mchorse.bbs_mod.camera.data.Angle;
import mchorse.bbs_mod.camera.data.Point;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.RayTracing;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;

public class PovCameraClip extends CameraClip {
   public static final int VIEW_FIRST_PERSON = 0;
   public static final int VIEW_THIRD_PERSON_BACK = 1;
   public static final int VIEW_THIRD_PERSON_FRONT = 2;
   public static final String[] PERSPECTIVE_LABELS = new String[]{"First Person View", "Third Person (Back)", "Third Person (Front)"};
   public final ValueInt selector = new ValueInt("selector", -1);
   public final ValueBoolean hands = new ValueBoolean("hands", true);
   public final ValueBoolean hud = new ValueBoolean("hud", true);
   public final ValueBoolean crosshair = new ValueBoolean("crosshair", true);
   public final ValueBoolean actions = new ValueBoolean("actions", true);
   public final ValueBoolean screenEffects = new ValueBoolean("screen_effects", true);
   public final ValueBoolean cursor = new ValueBoolean("cursor", true);
   public final ValueBoolean cameraShake = new ValueBoolean("camera_shake", true);
   public final ValueBoolean headLook = new ValueBoolean("head_look", true);
   public final ValueBoolean hardcoreLook = new ValueBoolean("hardcore_look", false);
   public final ValueBoolean blockOutline = new ValueBoolean("block_outline", true);
   public final ValueInt perspective = new ValueInt("perspective", 0);
   public final ValueFloat fov = new ValueFloat("fov", 70.0F, 1.0F, 180.0F);

   public PovCameraClip() {
      this.add(this.selector);
      this.add(this.hands);
      this.add(this.hud);
      this.add(this.crosshair);
      this.add(this.actions);
      this.add(this.screenEffects);
      this.add(this.cursor);
      this.add(this.cameraShake);
      this.add(this.headLook);
      this.add(this.hardcoreLook);
      this.add(this.blockOutline);
      this.add(this.perspective);
      this.add(this.fov);
   }

   public static String getPerspectiveLabel(int mode) {
      return mode >= 0 && mode < PERSPECTIVE_LABELS.length ? PERSPECTIVE_LABELS[mode] : PERSPECTIVE_LABELS[0];
   }

   public static int nextPerspective(int mode) {
      return (mode + 1) % PERSPECTIVE_LABELS.length;
   }

   public boolean isFirstPerson() {
      return (Integer)this.perspective.get() == 0;
   }

   protected void applyClip(ClipContext context, Position position) {
      if (this.isTopPovClip(context)) {
         if ((Boolean)this.headLook.get() && context instanceof CameraClipContext cameraContext) {
            int index = (Integer)this.selector.get();
            IEntity entity = null;
            if (index >= 0 && cameraContext.entities != null) {
               if (context.clips != null && context.clips.getParent() instanceof Film film) {
                  List<Replay> replays = film.replays.getList();
                  if (index < replays.size()) {
                     entity = (IEntity)cameraContext.entities.get(replays.get(index).getId());
                  }
               }

               if (entity == null) {
                  int i = 0;

                  for (IEntity candidate : cameraContext.entities.values()) {
                     if (i == index) {
                        entity = candidate;
                        break;
                     }

                     i++;
                  }
               }
            }

            if (entity != null) {
               float transition = context.transition;
               double x = Lerps.lerp(entity.getPrevX(), entity.getX(), (double)transition);
               double y = Lerps.lerp(entity.getPrevY(), entity.getY(), (double)transition) + entity.getEyeHeight();
               double z = Lerps.lerp(entity.getPrevZ(), entity.getZ(), (double)transition);
               float headYaw = (float)Lerps.lerpYaw((double)entity.getPrevHeadYaw(), (double)entity.getHeadYaw(), (double)transition);
               float pitch = Lerps.lerp(entity.getPrevPitch(), entity.getPitch(), transition);
               float yaw = headYaw + 180.0F;
               float roll = 0.0F;
               if ((Boolean)this.hardcoreLook.get()) {
                  TrackerFrame frame = this.resolveHardcoreHeadFrame(cameraContext, entity, position, transition);
                  if (frame != null) {
                     double scaleY = 1.0;
                     Form form = entity.getForm();
                     if (form != null) {
                        scaleY = Math.max(0.001, (double)((Transform)form.transform.get()).scale.y);
                     }

                     double eyeDiff = (entity.getEyeHeight() - 1.5) / scaleY;
                     Point eyeOffset = new Point(0.0, eyeDiff, 0.0);
                     Vector3d headPos = frame.position(eyeOffset);
                     Angle headAngle = frame.angles(new Point(0.0, 0.0, 0.0));
                     x = headPos.x;
                     y = headPos.y;
                     z = headPos.z;
                     yaw = headAngle.yaw;
                     pitch = headAngle.pitch;
                     roll = headAngle.roll;
                     headYaw = yaw - 180.0F;
                  }
               }

               int view = (Integer)this.perspective.get();
               if (view != 1 && view != 2) {
                  position.point.set(x, y, z);
                  position.angle.yaw = yaw;
                  position.angle.pitch = pitch;
               } else {
                  float radYaw = headYaw * (float) (Math.PI / 180.0);
                  float radPitch = pitch * (float) (Math.PI / 180.0);
                  float cosPitch = (float)Math.cos((double)radPitch);
                  float sinPitch = (float)Math.sin((double)radPitch);
                  float cosYaw = (float)Math.cos((double)radYaw);
                  float sinYaw = (float)Math.sin((double)radYaw);
                  double fx = (double)(-sinYaw * cosPitch);
                  double fy = (double)(-sinPitch);
                  double fz = (double)(cosYaw * cosPitch);
                  double maxDistance = 4.0;
                  MinecraftClient mc = MinecraftClient.getInstance();
                  if (view == 1) {
                     double dist = maxDistance;
                     if (mc.world != null) {
                        Vec3d eye = new Vec3d(x, y, z);
                        Vec3d dir = new Vec3d(-fx, -fy, -fz);
                        BlockHitResult hit = RayTracing.rayTrace(mc.world, eye, dir, maxDistance);
                        if (hit != null && hit.getType() == Type.BLOCK) {
                           dist = Math.max(0.0, eye.distanceTo(hit.getPos()) - 0.15);
                        }
                     }

                     position.point.set(x - dist * fx, y - dist * fy, z - dist * fz);
                     position.angle.yaw = yaw;
                     position.angle.pitch = pitch;
                  } else {
                     double dist = maxDistance;
                     if (mc.world != null) {
                        Vec3d eye = new Vec3d(x, y, z);
                        Vec3d dir = new Vec3d(fx, fy, fz);
                        BlockHitResult hit = RayTracing.rayTrace(mc.world, eye, dir, maxDistance);
                        if (hit != null && hit.getType() == Type.BLOCK) {
                           dist = Math.max(0.0, eye.distanceTo(hit.getPos()) - 0.15);
                        }
                     }

                     double camX = x + dist * fx;
                     double camY = y + dist * fy;
                     double camZ = z + dist * fz;
                     position.point.set(camX, camY, camZ);
                     if (dist > 0.05) {
                        Angle look = Angle.angle(x - camX, y - camY, z - camZ);
                        position.angle.yaw = look.yaw;
                        position.angle.pitch = look.pitch;
                     } else {
                        position.angle.yaw = headYaw;
                        position.angle.pitch = -pitch;
                     }
                  }
               }

               position.angle.roll = roll;
            }
         }

         if ((Boolean)this.headLook.get()) {
            position.angle.fov = (Float)this.fov.get();
         }
      }
   }

   private TrackerFrame resolveHardcoreHeadFrame(CameraClipContext cameraContext, IEntity entity, Position position, float transition) {
      Form form = entity.getForm();
      if (form == null) {
         return null;
      } else {
         FormRenderer formRenderer = FormUtilsClient.getRenderer(form);
         if (formRenderer instanceof ModelFormRenderer modelFormRenderer) {
            modelFormRenderer.ensureAnimator(transition);
         }

         String headBone = "head";
         if (formRenderer != null) {
            MatrixCache map = formRenderer.collectMatrices(entity, transition);
            if (!map.has(headBone)) {
               for (String key : map.keySet()) {
                  if (key.equalsIgnoreCase("head") || key.toLowerCase().endsWith("/head") || key.toLowerCase().endsWith(".head")) {
                     headBone = key;
                     break;
                  }
               }
            }

            if (!map.has(headBone)) {
               for (Object boneObj : formRenderer.getBones()) {
                  String bone = String.valueOf(boneObj);
                  if (bone.equalsIgnoreCase("head") || bone.toLowerCase().endsWith("head")) {
                     headBone = bone;
                     break;
                  }
               }
            }
         }

         return TrackerFrame.resolve(cameraContext.entities, entity, headBone, position.point.x, position.point.y, position.point.z, transition);
      }
   }

   private boolean isTopPovClip(ClipContext context) {
      if (context.clips == null) {
         return true;
      } else {
         PovCameraClip top = null;
         int topLayer = Integer.MIN_VALUE;

         for (Clip candidate : context.clips.getClips(context.ticks)) {
            if (candidate instanceof PovCameraClip) {
               PovCameraClip pov = (PovCameraClip)candidate;
               if ((Boolean)pov.enabled.get() && (Integer)pov.layer.get() >= topLayer) {
                  top = pov;
                  topLayer = (Integer)pov.layer.get();
               }
            }
         }

         return top == this;
      }
   }

   protected Clip create() {
      return new PovCameraClip();
   }
}

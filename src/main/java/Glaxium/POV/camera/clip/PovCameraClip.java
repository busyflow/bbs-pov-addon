package Glaxium.POV.camera.clip;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.clips.CameraClipContext;
import mchorse.bbs_mod.camera.clips.misc.TrackerFrame;
import mchorse.bbs_mod.camera.data.Angle;
import mchorse.bbs_mod.camera.data.Point;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.FilmMatrices;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.forms.utils.Anchor;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.RayTracing;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.interps.Lerps;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.List;

/**
 * Camera-timeline POV source. It decides which replay supplies the first-person
 * overlays and, optionally, lets that replay's recorded head drive the camera.
 * Hand viewport editing remains exclusively tied to PovCameraMode.POV.
 */
public class PovCameraClip extends CameraClip
{
    public static final int VIEW_FIRST_PERSON = 0;
    public static final int VIEW_THIRD_PERSON_BACK = 1;
    public static final int VIEW_THIRD_PERSON_FRONT = 2;

    public static final String[] PERSPECTIVE_LABELS = {
        "First Person View",
        "Third Person (Back)",
        "Third Person (Front)"
    };

    public final ValueInt selector = new ValueInt("selector", -1);
    public final ValueString replayId = new ValueString("replay_id", "");
    public final ValueBoolean hands = new ValueBoolean("hands", true);
    public final ValueBoolean hud = new ValueBoolean("hud", true);
    public final ValueBoolean crosshair = new ValueBoolean("crosshair", true);
    public final ValueBoolean actions = new ValueBoolean("actions", true);
    public final ValueBoolean screenEffects = new ValueBoolean("screen_effects", true);
    public final ValueBoolean cursor = new ValueBoolean("cursor", true);
    public final ValueBoolean cameraShake = new ValueBoolean("camera_shake", true);
    public final ValueBoolean headLook = new ValueBoolean("head_look", true);
    public final ValueBoolean headOnly = new ValueBoolean("head_only", false);
    public final ValueBoolean followAnchor = new ValueBoolean("follow_anchor", true);
    public final ValueBoolean hardcoreLook = new ValueBoolean("hardcore_look", false);
    public final ValueBoolean blockOutline = new ValueBoolean("block_outline", true);
    public final ValueInt perspective = new ValueInt("perspective", VIEW_FIRST_PERSON);
    public final ValueFloat fov = new ValueFloat("fov", 70F, 1F, 180F);

    public PovCameraClip()
    {
        super();

        this.add(this.selector);
        this.add(this.replayId);
        this.add(this.hands);
        this.add(this.hud);
        this.add(this.crosshair);
        this.add(this.actions);
        this.add(this.screenEffects);
        this.add(this.cursor);
        this.add(this.cameraShake);
        this.add(this.headLook);
        this.add(this.headOnly);
        this.add(this.followAnchor);
        this.add(this.hardcoreLook);
        this.add(this.blockOutline);
        this.add(this.perspective);
        this.add(this.fov);
    }

    public static String getPerspectiveLabel(int mode)
    {
        if (mode < 0 || mode >= PERSPECTIVE_LABELS.length)
        {
            return PERSPECTIVE_LABELS[0];
        }

        return PERSPECTIVE_LABELS[mode];
    }

    public static int nextPerspective(int mode)
    {
        return (mode + 1) % PERSPECTIVE_LABELS.length;
    }

    public boolean isFirstPerson()
    {
        return this.perspective.get() == VIEW_FIRST_PERSON;
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        /* Camera clips are applied from low to high layer, so the normal shot is
         * already in Position. Ignore lower overlapping POV clips: only the
         * highest enabled POV clip at this tick is allowed to affect the camera. */
        if (!this.isTopPovClip(context))
        {
            return;
        }

        if (this.headLook.get() && context instanceof CameraClipContext cameraContext)
        {
            int index = this.selector.get();
            IEntity entity = null;
            Replay replay = null;

            if (cameraContext.entities != null)
            {
                if (context.clips != null && context.clips.getParent() instanceof Film film)
                {
                    replay = PovCameraClips.resolveReplay(film, this);
                    if (replay != null)
                    {
                        entity = cameraContext.entities.get(replay.getId());
                    }
                }

                if (entity == null && index >= 0)
                {
                    int i = 0;
                    for (IEntity candidate : cameraContext.entities.values())
                    {
                        if (i == index)
                        {
                            entity = candidate;
                            break;
                        }
                        i++;
                    }
                }
            }

            if (entity != null)
            {
                float transition = context.transition;
                Form form = entity.getForm();
                float filmTick = (float) context.ticks + transition;
                double eyeHeight = PovCameraClips.getSmoothPovEyeHeight(replay, entity, form, filmTick);
                double x = Lerps.lerp(entity.getPrevX(), entity.getX(), transition);
                double y = Lerps.lerp(entity.getPrevY(), entity.getY(), transition) + eyeHeight;
                double z = Lerps.lerp(entity.getPrevZ(), entity.getZ(), transition);
                /* BBS Camera.rotation uses the opposite forward basis from the
                 * replay entity yaw. Its native first-person mode adds PI, so
                 * the Position/Angle equivalent is +180 degrees. */
                float headYaw = (float) Lerps.lerpYaw(
                    entity.getPrevHeadYaw(),
                    entity.getHeadYaw(),
                    transition);
                float pitch = Lerps.lerp(entity.getPrevPitch(), entity.getPitch(), transition);
                float yaw = headYaw + 180F;
                float roll = 0F;

                if (this.followAnchor.get() && cameraContext.entities != null)
                {
                    Anchor anchor = FilmMatrices.getAnchor(entity, replay);
                    if (anchor != null)
                    {
                        Matrix4f entityMatrix = FilmMatrices.getMatrixForRenderWithRotation(entity, 0D, 0D, 0D, transition);
                        Pair<Matrix4f, Float> total = FilmMatrices.getTotalMatrix(cameraContext.entities, anchor, entityMatrix, 0D, 0D, 0D, transition, 0);
                        if (total.a != null)
                        {
                            Vector3f eyePos = new Vector3f(0F, (float) eyeHeight, 0F);
                            total.a.transformPosition(eyePos);
                            x = eyePos.x;
                            y = eyePos.y;
                            z = eyePos.z;

                            if (!this.hardcoreLook.get())
                            {
                                Vector3f forward = new Vector3f(0F, 0F, 1F);
                                total.a.transformDirection(forward);
                                float anchorYaw = (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));

                                boolean isControllingThis = false;
                                UIFilmPanel panel = Glaxium.POV.replay.PovReplaySettings.getFilmPanel();
                                if (panel != null && panel.getController() != null && panel.getController().isControlling())
                                {
                                    IEntity controlled = panel.getController().getControlled();
                                    if (controlled != null)
                                    {
                                        if (controlled == entity)
                                        {
                                            isControllingThis = true;
                                        }
                                        else if (controlled instanceof MCEntity mcControlled && entity instanceof MCEntity mcEntity)
                                        {
                                            isControllingThis = mcControlled.getMcEntity() == mcEntity.getMcEntity();
                                        }
                                    }
                                }

                                yaw = isControllingThis
                                    ? headYaw + 180F
                                    : headYaw + (anchor.inheritRotation ? anchorYaw : 0F) + 180F;
                                headYaw = yaw - 180F;
                            }
                        }
                    }
                }

                if (this.hardcoreLook.get())
                {
                    TrackerFrame frame = this.resolveHardcoreHeadFrame(cameraContext, entity, position, transition);

                    if (frame != null)
                    {
                        double scaleY = 1.0D;
                        if (form != null && form.transform != null && form.transform.get() != null)
                        {
                            scaleY = Math.max(0.001D, form.transform.get().scale.y);
                        }

                        /* Standard models have head pivot at 1.5m and eyes at ~1.625m */
                        double eyeDiff = (eyeHeight - 1.5D) / scaleY;
                        Point eyeOffset = new Point(0D, eyeDiff, 0D);

                        Vector3d headPos = frame.position(eyeOffset);
                        Angle headAngle = frame.angles(new Point(0, 0, 0));

                        x = headPos.x;
                        y = headPos.y;
                        z = headPos.z;

                        yaw = headAngle.yaw;
                        pitch = headAngle.pitch;
                        roll = headAngle.roll;
                        headYaw = yaw - 180F;
                    }
                }

                int view = this.perspective.get();

                if (view == VIEW_THIRD_PERSON_BACK || view == VIEW_THIRD_PERSON_FRONT)
                {
                    double originX = this.headOnly.get() ? position.point.x : x;
                    double originY = this.headOnly.get() ? position.point.y : y;
                    double originZ = this.headOnly.get() ? position.point.z : z;

                    float radYaw = headYaw * ((float) Math.PI / 180F);
                    float radPitch = pitch * ((float) Math.PI / 180F);

                    float cosPitch = (float) Math.cos(radPitch);
                    float sinPitch = (float) Math.sin(radPitch);
                    float cosYaw = (float) Math.cos(radYaw);
                    float sinYaw = (float) Math.sin(radYaw);

                    double fx = -sinYaw * cosPitch;
                    double fy = -sinPitch;
                    double fz = cosYaw * cosPitch;

                    double maxDistance = 4.0D;
                    MinecraftClient mc = MinecraftClient.getInstance();

                    if (view == VIEW_THIRD_PERSON_BACK)
                    {
                        double dist = maxDistance;
                        if (mc.world != null)
                        {
                            Vec3d eye = new Vec3d(originX, originY, originZ);
                            Vec3d dir = new Vec3d(-fx, -fy, -fz);
                            BlockHitResult hit = RayTracing.rayTrace(mc.world, eye, dir, maxDistance);

                            if (hit != null && hit.getType() == HitResult.Type.BLOCK)
                            {
                                dist = Math.max(0.0D, eye.distanceTo(hit.getPos()) - 0.15D);
                            }
                        }

                        position.point.set(originX - dist * fx, originY - dist * fy, originZ - dist * fz);
                        position.angle.yaw = yaw;
                        position.angle.pitch = pitch;
                    }
                    else
                    {
                        double dist = maxDistance;
                        if (mc.world != null)
                        {
                            Vec3d eye = new Vec3d(originX, originY, originZ);
                            Vec3d dir = new Vec3d(fx, fy, fz);
                            BlockHitResult hit = RayTracing.rayTrace(mc.world, eye, dir, maxDistance);

                            if (hit != null && hit.getType() == HitResult.Type.BLOCK)
                            {
                                dist = Math.max(0.0D, eye.distanceTo(hit.getPos()) - 0.15D);
                            }
                        }

                        double camX = originX + dist * fx;
                        double camY = originY + dist * fy;
                        double camZ = originZ + dist * fz;

                        position.point.set(camX, camY, camZ);
                        if (dist > 0.05D)
                        {
                            Angle look = Angle.angle(originX - camX, originY - camY, originZ - camZ);
                            position.angle.yaw = look.yaw;
                            position.angle.pitch = look.pitch;
                        }
                        else
                        {
                            position.angle.yaw = headYaw;
                            position.angle.pitch = -pitch;
                        }
                    }
                }
                else
                {
                    if (!this.headOnly.get())
                    {
                        position.point.set(x, y, z);
                    }
                    position.angle.yaw = yaw;
                    position.angle.pitch = pitch;
                }

                position.angle.roll = roll;
            }
        }

        if (this.headLook.get())
        {
            position.angle.fov = this.fov.get();
        }
    }

    private TrackerFrame resolveHardcoreHeadFrame(CameraClipContext cameraContext, IEntity entity, Position position, float transition)
    {
        Form form = entity.getForm();
        if (form == null)
        {
            return null;
        }

        FormRenderer formRenderer = FormUtilsClient.getRenderer(form);
        if (formRenderer instanceof ModelFormRenderer modelFormRenderer)
        {
            modelFormRenderer.ensureAnimator(transition);
        }

        String headBone = "head";
        if (formRenderer != null)
        {
            MatrixCache map = formRenderer.collectMatrices(entity, transition);
            if (!map.has(headBone))
            {
                for (String key : map.keySet())
                {
                    if (key.equalsIgnoreCase("head") || key.toLowerCase().endsWith("/head") || key.toLowerCase().endsWith(".head"))
                    {
                        headBone = key;
                        break;
                    }
                }
            }

            if (!map.has(headBone))
            {
                List<String> bones = formRenderer.getBones();
                for (String bone : bones)
                {
                    if (bone.equalsIgnoreCase("head") || bone.toLowerCase().endsWith("head"))
                    {
                        headBone = bone;
                        break;
                    }
                }
            }
        }

        return TrackerFrame.resolve(
            cameraContext.entities,
            entity,
            headBone,
            position.point.x,
            position.point.y,
            position.point.z,
            transition
        );
    }


    private boolean isTopPovClip(ClipContext context)
    {
        if (context.clips == null)
        {
            return true;
        }

        PovCameraClip top = null;
        int topLayer = Integer.MIN_VALUE;

        for (Clip candidate : context.clips.getClips(context.ticks))
        {
            if (candidate instanceof PovCameraClip pov
                && pov.enabled.get()
                && pov.layer.get() >= topLayer)
            {
                top = pov;
                topLayer = pov.layer.get();
            }
        }

        return top == this;
    }

    @Override
    protected Clip create()
    {
        return new PovCameraClip();
    }
}

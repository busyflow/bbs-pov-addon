package Glaxium.POV.hand;

import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;

/** Fully sampled, frame-local state used by the POV hand renderer. */
public final class HandState
{
    public boolean visible = true;
    public String model = RecordedHandData.DEFAULT_MODEL;
    public Link texture;
    public Color color;
    public Color colorOverlay;
    public final Transform cameraOffset = new Transform();
    public final Pose pose = new Pose();
    public final Pose itemPose = new Pose();
    public final PoseTransform rightPose = new PoseTransform();
    public final PoseTransform leftPose = new PoseTransform();
    public boolean rightHandVisible = true;
    public boolean leftHandVisible;
    public ItemStack mainHand = ItemStack.EMPTY;
    public ItemStack offHand = ItemStack.EMPTY;
    public float rightSwingProgress;
    public float previousRightSwingProgress;
    public float leftSwingProgress;
    public float previousLeftSwingProgress;
    public float mainEquipProgress;
    public float previousMainEquipProgress;
    public float offEquipProgress;
    public float previousOffEquipProgress;
    public Hand activeHand = Hand.MAIN_HAND;
    public boolean usingItem;
    /** Item retained by LivingEntity for the current use action. This is not
     * always the same as the hand stack at completion (e.g. stew -> bowl). */
    public ItemStack activeItem = ItemStack.EMPTY;
    public boolean showUseParticles = true;
    /** Continuous elapsed use time sampled between recorded ticks. */
    public float useTime;
    public float bobPhase;
    public float previousBobPhase;
    public float bobStrength;
    public float previousBobStrength;
    public Arm mainArm = Arm.RIGHT;
    /** Actor view sampled from BBS replay keyframes; never from the live client player. */
    public float viewYaw;
    public float previousViewYaw;
    public float viewPitch;
    public float previousViewPitch;
    public float renderYaw;
    public float previousRenderYaw;
    public float renderPitch;
    public float previousRenderPitch;
    public boolean worldInteraction;
    public boolean replayInteraction;
}

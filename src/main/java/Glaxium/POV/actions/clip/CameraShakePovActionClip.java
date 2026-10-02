package Glaxium.POV.actions.clip;

import Glaxium.POV.actions.PovActionType;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import java.util.List;

/**
 * Baked / authored recreation of vanilla {@code tiltViewWhenHurt}.
 * Channels are the real hurt-camera inputs, not procedural strength/frequency.
 */
public final class CameraShakePovActionClip extends PovActionClip
{
    /** Toggle: whether this hurt pulse is affecting the camera. */
    public final KeyframeChannel<Boolean> active = this.channel("active", KeyframeFactories.BOOLEAN);
    /** Vanilla {@code LivingEntity.hurtTime} remaining. */
    public final KeyframeChannel<Integer> hurtTime = this.channel("hurt_time", KeyframeFactories.INTEGER);
    /** Vanilla {@code LivingEntity.maxHurtTime} (usually 10). */
    public final KeyframeChannel<Integer> maxHurtTime = this.channel("max_hurt_time", KeyframeFactories.INTEGER);
    /** Vanilla {@code PlayerEntity.damageTiltYaw} hit direction. */
    public final KeyframeChannel<Float> damageTiltYaw = this.channel("damage_tilt_yaw", KeyframeFactories.FLOAT);
    /** Vanilla {@code LivingEntity.deathTime}; 0 while alive. */
    public final KeyframeChannel<Integer> deathTime = this.channel("death_time", KeyframeFactories.INTEGER);

    public CameraShakePovActionClip()
    {
        super();
    }

    @Override
    public PovActionType getActionType()
    {
        return PovActionType.CAMERA_SHAKE;
    }

    @Override
    public void normalize()
    {
        super.normalize();
        clamp(this.hurtTime, 0, 200);
        clamp(this.maxHurtTime, 0, 200);
        clamp(this.deathTime, 0, 200);
    }

    public void ensureBakingBounds()
    {
        float end = this.duration.get();

        this.padChannel(this.active, end);
        this.padChannel(this.hurtTime, end);
        this.padChannel(this.maxHurtTime, end);
        this.padChannel(this.damageTiltYaw, end);
        this.padChannel(this.deathTime, end);
    }

    private static <T> void padChannel(KeyframeChannel<T> channel, float end)
    {
        if (channel == null || channel.isEmpty() || end <= 0F)
        {
            return;
        }

        List<? extends Keyframe<T>> keyframes = channel.getKeyframes();
        Keyframe<T> first = keyframes.get(0);
        Keyframe<T> last = keyframes.get(keyframes.size() - 1);

        if (first.getTick() > 0F)
        {
            channel.insert(0F, first.getValue());
        }

        if (last.getTick() < end)
        {
            channel.insert(end, last.getValue());
        }

        constant(channel);
    }

    @Override
    protected Clip create()
    {
        return new CameraShakePovActionClip();
    }
}

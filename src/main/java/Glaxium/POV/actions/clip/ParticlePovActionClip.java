package Glaxium.POV.actions.clip;

import Glaxium.POV.actions.PovActionType;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public final class ParticlePovActionClip extends PovActionClip
{
    public final KeyframeChannel<String> particle = this.channel("particle", KeyframeFactories.STRING);
    public final KeyframeChannel<Integer> count = this.channel("count", KeyframeFactories.INTEGER);
    public final KeyframeChannel<Float> positionX = this.channel("pos_x", KeyframeFactories.FLOAT);
    public final KeyframeChannel<Float> positionY = this.channel("pos_y", KeyframeFactories.FLOAT);
    public final KeyframeChannel<Float> positionZ = this.channel("pos_z", KeyframeFactories.FLOAT);
    public final KeyframeChannel<Float> velocityX = this.channel("vel_x", KeyframeFactories.FLOAT);
    public final KeyframeChannel<Float> velocityY = this.channel("vel_y", KeyframeFactories.FLOAT);
    public final KeyframeChannel<Float> velocityZ = this.channel("vel_z", KeyframeFactories.FLOAT);
    public final KeyframeChannel<Float> spread = this.channel("spread", KeyframeFactories.FLOAT);

    /** 0 = View space, 1 = Local/Actor space, 2 = World space. */
    public final ValueInt space = new ValueInt("space", 0, 0, 2);
    public final ValueInt seed = new ValueInt("seed", 0, 0, Integer.MAX_VALUE);
    public final ValueString extraArgs = new ValueString("extra_args", "");

    public ParticlePovActionClip()
    {
        super();
        this.add(this.space);
        this.add(this.seed);
        this.add(this.extraArgs);
    }

    @Override
    public PovActionType getActionType()
    {
        return PovActionType.PARTICLE_EFFECT;
    }

    @Override
    public void normalize()
    {
        super.normalize();
        constant(this.particle);
        clamp(this.count, 0, 4096);
        clamp(this.spread, 0F, 64F);
    }

    @Override
    protected Clip create()
    {
        return new ParticlePovActionClip();
    }
}

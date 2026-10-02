package Glaxium.POV.actions.clip;

import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import org.joml.Vector3f;

/** Shared one-shot emitter settings for camera-tied particle actions. */
public abstract class ViewParticlePovActionClip extends PovActionClip
{
    public final ValueInt count = new ValueInt("count", 1, 0, 4096);
    public final ValueFloat spread = new ValueFloat("spread", 0F, 0F, 64F);
    public final ValueFloat velX = new ValueFloat("vel_x", 0F);
    public final ValueFloat velY = new ValueFloat("vel_y", 0F);
    public final ValueFloat velZ = new ValueFloat("vel_z", 0F);
    public final ValueInt seed = new ValueInt("seed", 0, 0, Integer.MAX_VALUE);

    public final ValueFloat tx = new ValueFloat("tx", 0F);
    public final ValueFloat ty = new ValueFloat("ty", 0F);
    public final ValueFloat tz = new ValueFloat("tz", 0F);
    public final ValueFloat sx = new ValueFloat("sx", 1F);
    public final ValueFloat sy = new ValueFloat("sy", 1F);
    public final ValueFloat sz = new ValueFloat("sz", 1F);
    public final ValueFloat rx = new ValueFloat("rx", 0F);
    public final ValueFloat ry = new ValueFloat("ry", 0F);
    public final ValueFloat rz = new ValueFloat("rz", 0F);

    protected ViewParticlePovActionClip()
    {
        super();
        this.add(this.count);
        this.add(this.spread);
        this.add(this.velX);
        this.add(this.velY);
        this.add(this.velZ);
        this.add(this.seed);
        this.add(this.tx);
        this.add(this.ty);
        this.add(this.tz);
        this.add(this.sx);
        this.add(this.sy);
        this.add(this.sz);
        this.add(this.rx);
        this.add(this.ry);
        this.add(this.rz);
    }

    public void transformPoint(Vector3f point)
    {
        point.mul(this.sx.get(), this.sy.get(), this.sz.get());
        point.rotateX((float) Math.toRadians(this.rx.get()));
        point.rotateY((float) Math.toRadians(this.ry.get()));
        point.rotateZ((float) Math.toRadians(this.rz.get()));
        point.add(this.tx.get(), this.ty.get(), this.tz.get());
    }

    public void transformVector(Vector3f vector)
    {
        vector.mul(this.sx.get(), this.sy.get(), this.sz.get());
        vector.rotateX((float) Math.toRadians(this.rx.get()));
        vector.rotateY((float) Math.toRadians(this.ry.get()));
        vector.rotateZ((float) Math.toRadians(this.rz.get()));
    }

    public float uniformScale()
    {
        return (this.sx.get() + this.sy.get() + this.sz.get()) / 3F;
    }
}

package Glaxium.POV.actions.clip;

import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import org.joml.Vector3f;

public abstract class ViewParticlePovActionClip extends PovActionClip {
   public final ValueInt count = new ValueInt("count", 1, 0, 4096);
   public final ValueFloat spread = new ValueFloat("spread", 0.0F, 0.0F, 64.0F);
   public final ValueFloat velX = new ValueFloat("vel_x", 0.0F);
   public final ValueFloat velY = new ValueFloat("vel_y", 0.0F);
   public final ValueFloat velZ = new ValueFloat("vel_z", 0.0F);
   public final ValueInt seed = new ValueInt("seed", 0, 0, Integer.MAX_VALUE);
   public final ValueFloat tx = new ValueFloat("tx", 0.0F);
   public final ValueFloat ty = new ValueFloat("ty", 0.0F);
   public final ValueFloat tz = new ValueFloat("tz", 0.0F);
   public final ValueFloat sx = new ValueFloat("sx", 1.0F);
   public final ValueFloat sy = new ValueFloat("sy", 1.0F);
   public final ValueFloat sz = new ValueFloat("sz", 1.0F);
   public final ValueFloat rx = new ValueFloat("rx", 0.0F);
   public final ValueFloat ry = new ValueFloat("ry", 0.0F);
   public final ValueFloat rz = new ValueFloat("rz", 0.0F);

   protected ViewParticlePovActionClip() {
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

   public void transformPoint(Vector3f point) {
      point.mul((Float)this.sx.get(), (Float)this.sy.get(), (Float)this.sz.get());
      point.rotateX((float)Math.toRadians((double)((Float)this.rx.get()).floatValue()));
      point.rotateY((float)Math.toRadians((double)((Float)this.ry.get()).floatValue()));
      point.rotateZ((float)Math.toRadians((double)((Float)this.rz.get()).floatValue()));
      point.add((Float)this.tx.get(), (Float)this.ty.get(), (Float)this.tz.get());
   }

   public void transformVector(Vector3f vector) {
      vector.mul((Float)this.sx.get(), (Float)this.sy.get(), (Float)this.sz.get());
      vector.rotateX((float)Math.toRadians((double)((Float)this.rx.get()).floatValue()));
      vector.rotateY((float)Math.toRadians((double)((Float)this.ry.get()).floatValue()));
      vector.rotateZ((float)Math.toRadians((double)((Float)this.rz.get()).floatValue()));
   }

   public float uniformScale() {
      return ((Float)this.sx.get() + (Float)this.sy.get() + (Float)this.sz.get()) / 3.0F;
   }
}

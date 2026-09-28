package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.recording.ActiveRecordingRange;
import java.util.List;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {KeyframeChannel.class},
   remap = false
)
public abstract class KeyframeChannelPovMixin {
   @Inject(
      method = {"copyOver"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$copyOverRange(KeyframeChannel channel, int tick, CallbackInfo ci) {
      if (ActiveRecordingRange.isActive()) {
         KeyframeChannel self = (KeyframeChannel)(Object)this;
         if (self.getFactory() != channel.getFactory() || channel.isEmpty()) {
            ci.cancel();
            return;
         }

         self.preNotify();
         int[] range = ActiveRecordingRange.get();
         double rangeStart = (double)(range[0] + tick);
         double rangeEnd = (double)(range[1] + tick);
         List keyframes = ((ValueListPovAccessor)self).bbsPov$getList();
         keyframes.removeIf(next -> {
            Keyframe kf = (Keyframe)next;
            return (double)kf.getTick() >= rangeStart - 1.0E-4 && (double)kf.getTick() <= rangeEnd + 1.0E-4;
         });

         for (Object o : channel.getKeyframes()) {
            Keyframe keyframe = (Keyframe)o;
            Keyframe value = new Keyframe(keyframe.getId(), keyframe.getFactory());
            value.fromData(keyframe.toData());
            value.setTick((float)tick + value.getTick());
            keyframes.add(value);
         }

         keyframes.sort((a, b) -> Float.compare(((Keyframe)a).getTick(), ((Keyframe)b).getTick()));
         self.sync();
         self.postNotify();
         ci.cancel();
      }
   }

   @Inject(
      method = {"remove"},
      at = {@At("HEAD")}
   )
   private void bbsPov$removeGroupedSlotKeyframes(int index, CallbackInfo info) {
      KeyframeChannel<?> channel = (KeyframeChannel<?>)(Object)this;
      if (index >= 0 && index < channel.getKeyframes().size() && channel.getParent() instanceof GuiPovActionClip clip && clip.isSlotAnchor(channel)) {
         float tick = channel.get(index).getTick();
         int anchorsAtTick = 0;

         for (Keyframe<?> keyframe : channel.getKeyframes()) {
            if (Math.abs(keyframe.getTick() - tick) < 1.0E-4F) {
               anchorsAtTick++;
            }
         }

         if (anchorsAtTick <= 1) {
            clip.removeSlotKeyframes(channel, tick);
         }
      }
   }

   @Inject(
      method = {"fromData"},
      at = {@At("HEAD")}
   )
   private void bbsPov$upgradeLayoutFactory(BaseType data, CallbackInfo info) {
      KeyframeChannel<?> channel = (KeyframeChannel<?>)(Object)this;
      if (channel.getId() != null && channel.getId().contains("layout") && data != null && data.isMap()) {
         data.asMap().putString("type", "transform");
      }
   }
}

package Glaxium.POV.actions;

import Glaxium.POV.actions.clip.BossBarPovActionClip;
import Glaxium.POV.actions.clip.CameraShakePovActionClip;
import Glaxium.POV.actions.clip.ChatPovActionClip;
import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.clip.MenuPovActionClip;
import Glaxium.POV.actions.clip.ParticleEffectPovActionClip;
import Glaxium.POV.actions.clip.PovActionClip;
import Glaxium.POV.actions.clip.ScreenEffectPovActionClip;
import Glaxium.POV.actions.clip.SemanticHudPovActionClip;
import Glaxium.POV.actions.clip.StatusEffectsPovActionClip;
import Glaxium.POV.actions.clip.ToastPovActionClip;
import Glaxium.POV.actions.timeline.PovActionTimelineFactory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.item.ItemStack;

public final class RecordedPovActions extends Clips {
   private final List<PovActionClip> sessionClips = new ArrayList<>();

   public RecordedPovActions() {
      super("pov_actions", new PovActionTimelineFactory());
   }

   public PovActionClip add(PovActionType type, int tick, int duration) {
      PovActionClip clip = (PovActionClip)(switch (type) {
         case GUI -> new GuiPovActionClip();
         case MENU -> new MenuPovActionClip();
         case CAMERA_SHAKE -> new CameraShakePovActionClip();
         case PARTICLE_EFFECT -> new ParticleEffectPovActionClip();
         case BOSS_BARS -> new BossBarPovActionClip();
         case SCREEN_EFFECT -> new ScreenEffectPovActionClip();
         case STATUS_EFFECTS -> new StatusEffectsPovActionClip();
         case TOASTS -> new ToastPovActionClip();
         case CHAT -> new ChatPovActionClip();
         default -> new SemanticHudPovActionClip(type);
      });
      clip.tick.set(Math.max(0, tick));
      clip.duration.set(Math.max(1, duration));
      clip.layer.set(type.seedLayer());
      this.addClip(clip);
      this.sessionClips.add(clip);
      this.sync();
      return clip;
   }

   public ChatPovActionClip getActiveChat(float tick) {
      ChatPovActionClip top = null;

      for (Clip clip : this.get()) {
         if (clip instanceof ChatPovActionClip) {
            ChatPovActionClip chat = (ChatPovActionClip)clip;
            if (chat.isActive(tick) && (top == null || (Integer)chat.layer.get() >= (Integer)top.layer.get())) {
               top = chat;
            }
         }
      }

      return top;
   }

   public CameraShakePovActionClip getActiveCameraShake(float tick) {
      CameraShakePovActionClip top = null;

      for (Clip clip : this.get()) {
         if (clip instanceof CameraShakePovActionClip) {
            CameraShakePovActionClip shake = (CameraShakePovActionClip)clip;
            if (shake.isActive(tick) && (top == null || (Integer)shake.layer.get() >= (Integer)top.layer.get())) {
               top = shake;
            }
         }
      }

      return top;
   }

   public StatusEffectsPovActionClip getActiveStatusEffects(float tick) {
      StatusEffectsPovActionClip top = null;

      for (Clip clip : this.get()) {
         if (clip instanceof StatusEffectsPovActionClip) {
            StatusEffectsPovActionClip effects = (StatusEffectsPovActionClip)clip;
            if (effects.isActive(tick) && (top == null || (Integer)effects.layer.get() >= (Integer)top.layer.get())) {
               top = effects;
            }
         }
      }

      return top;
   }

   public List<PovActionClip> takeSessionClips() {
      List<PovActionClip> recorded = new ArrayList<>(this.sessionClips);
      this.sessionClips.clear();
      return recorded;
   }

   public void clearAll() {
      for (Clip clip : new ArrayList<Clip>(this.get())) {
         this.remove(clip);
      }

      this.sync();
   }

   public void trimForRecordingRange(int startTick, int endTick) {
      List<Clip> toAdd = new ArrayList<>();

      for (Clip c : new ArrayList<Clip>(this.get())) {
         int start = (Integer)c.tick.get();
         int duration = (Integer)c.duration.get();
         int end = start + duration;
         if (end > startTick && start < endTick) {
            if (start >= startTick && end <= endTick) {
               this.remove(c);
            } else if (start < startTick && end > endTick) {
               Clip rightPiece = c.copy();
               int cutAmount = endTick - start;
               rightPiece.tick.set(endTick);
               rightPiece.duration.set(Math.max(1, end - endTick));
               if (rightPiece instanceof PovActionClip povRight) {
                  for (KeyframeChannel<?> channel : povRight.getChannels()) {
                     trimLeftChannel(channel, (float)cutAmount);
                  }
               }

               toAdd.add(rightPiece);
               int leftDuration = Math.max(1, startTick - start);
               c.duration.set(leftDuration);
               if (c instanceof PovActionClip povClip) {
                  for (KeyframeChannel<?> channel : povClip.getChannels()) {
                     trimRightChannel(channel, (float)leftDuration);
                  }
               }
            } else if (start < startTick && end <= endTick) {
               int newDuration = Math.max(1, startTick - start);
               c.duration.set(newDuration);
               if (c instanceof PovActionClip povClip) {
                  for (KeyframeChannel<?> channel : povClip.getChannels()) {
                     trimRightChannel(channel, (float)newDuration);
                  }
               }
            } else if (start < endTick && end > endTick) {
               int cutAmountx = endTick - start;
               c.tick.set(endTick);
               c.duration.set(Math.max(1, end - endTick));
               if (c instanceof PovActionClip povClip) {
                  for (KeyframeChannel<?> channel : povClip.getChannels()) {
                     trimLeftChannel(channel, (float)cutAmountx);
                  }
               }
            }
         }
      }

      for (Clip clip : toAdd) {
         this.addClip(clip);
      }

      this.sync();
   }

   public void trimForRecordingAt(int timelineTick) {
      this.trimForRecordingRange(timelineTick, Integer.MAX_VALUE);
   }

   private static void trimRightChannel(KeyframeChannel channel, float maxDuration) {
      if (!channel.isEmpty()) {
         boolean hasKeyAtEnd = false;

         for (Object obj : channel.getKeyframes()) {
            Keyframe<?> kf = (Keyframe<?>)obj;
            if (Math.abs(kf.getTick() - maxDuration) < 1.0E-4F) {
               hasKeyAtEnd = true;
               break;
            }
         }

         if (!hasKeyAtEnd) {
            Object initial = ((Keyframe)channel.getKeyframes().get(0)).getValue();
            Object endValue = channel.interpolate(maxDuration, initial);
            if (endValue != null) {
               if (endValue instanceof ItemStack stack) {
                  endValue = stack.copy();
               } else if (endValue instanceof Transform transform) {
                  endValue = transform.copy();
               }

               channel.insert(maxDuration, endValue);
            }
         }

         List list = channel.getKeyframes();

         for (int i = list.size() - 1; i >= 0; i--) {
            Keyframe<?> kf = (Keyframe<?>)list.get(i);
            if (kf.getTick() > maxDuration + 1.0E-4F) {
               channel.remove(i);
            }
         }

         channel.sort();
      }
   }

   private static void trimLeftChannel(KeyframeChannel channel, float cutAmount) {
      if (!channel.isEmpty()) {
         boolean hasKeyAtCut = false;

         for (Object obj : channel.getKeyframes()) {
            Keyframe<?> kf = (Keyframe<?>)obj;
            if (Math.abs(kf.getTick() - cutAmount) < 1.0E-4F) {
               hasKeyAtCut = true;
               break;
            }
         }

         if (!hasKeyAtCut) {
            Object initial = ((Keyframe)channel.getKeyframes().get(0)).getValue();
            Object cutValue = channel.interpolate(cutAmount, initial);
            if (cutValue != null) {
               if (cutValue instanceof ItemStack stack) {
                  cutValue = stack.copy();
               } else if (cutValue instanceof Transform transform) {
                  cutValue = transform.copy();
               }

               channel.insert(cutAmount, cutValue);
            }
         }

         List list = channel.getKeyframes();

         for (int i = list.size() - 1; i >= 0; i--) {
            Keyframe<?> kf = (Keyframe<?>)list.get(i);
            if (kf.getTick() < cutAmount - 1.0E-4F) {
               channel.remove(i);
            }
         }

         for (Object objx : channel.getKeyframes()) {
            Keyframe<?> kf = (Keyframe<?>)objx;
            kf.setTick(kf.getTick() - cutAmount);
         }

         channel.sort();
      }
   }

   public List<PovActionClip> getActive(float tick) {
      List<PovActionClip> active = new ArrayList<>();

      for (Clip clip : this.get()) {
         if (clip instanceof PovActionClip) {
            PovActionClip povClip = (PovActionClip)clip;
            if (povClip.isActive(tick)) {
               active.add(povClip);
            }
         }
      }

      active.sort(Comparator.comparingInt(clipx -> (Integer)clipx.layer.get()));
      return active;
   }

   public MenuPovActionClip getActiveMenu(float tick) {
      MenuPovActionClip top = null;

      for (Clip clip : this.get()) {
         if (clip instanceof MenuPovActionClip) {
            MenuPovActionClip menu = (MenuPovActionClip)clip;
            if (menu.isActive(tick) && (top == null || (Integer)menu.layer.get() >= (Integer)top.layer.get())) {
               top = menu;
            }
         }
      }

      return top;
   }

   public GuiPovActionClip getActiveGui(float tick) {
      GuiPovActionClip top = null;

      for (Clip clip : this.get()) {
         if (clip instanceof GuiPovActionClip) {
            GuiPovActionClip gui = (GuiPovActionClip)clip;
            if (gui.isActive(tick) && (top == null || (Integer)gui.layer.get() >= (Integer)top.layer.get())) {
               top = gui;
            }
         }
      }

      return top;
   }

   public List<BossBarPovActionClip> getActiveBossBars(float tick) {
      List<BossBarPovActionClip> active = new ArrayList<>();

      for (Clip clip : this.get()) {
         if (clip instanceof BossBarPovActionClip) {
            BossBarPovActionClip bar = (BossBarPovActionClip)clip;
            if (bar.isActive(tick)) {
               active.add(bar);
            }
         }
      }

      active.sort(Comparator.comparingInt(c -> (Integer)c.layer.get()));
      return active;
   }

   public BossBarPovActionClip getActiveBossBar(float tick) {
      BossBarPovActionClip top = null;

      for (Clip clip : this.get()) {
         if (clip instanceof BossBarPovActionClip) {
            BossBarPovActionClip bar = (BossBarPovActionClip)clip;
            if (bar.isActive(tick) && (top == null || (Integer)bar.layer.get() >= (Integer)top.layer.get())) {
               top = bar;
            }
         }
      }

      return top;
   }

   public void fromData(BaseType data) {
      super.fromData(data);

      for (Clip clip : this.get()) {
         if (clip instanceof PovActionClip povClip) {
            povClip.normalize();
         }
      }
   }
}

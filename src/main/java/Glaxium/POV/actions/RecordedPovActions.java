package Glaxium.POV.actions;

import Glaxium.POV.actions.clip.BossBarPovActionClip;
import Glaxium.POV.actions.clip.CameraShakePovActionClip;
import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.clip.MenuPovActionClip;
import Glaxium.POV.actions.clip.ParticleEffectPovActionClip;
import Glaxium.POV.actions.clip.PovActionClip;
import Glaxium.POV.actions.clip.ScreenEffectPovActionClip;
import Glaxium.POV.actions.clip.SemanticHudPovActionClip;
import Glaxium.POV.actions.timeline.PovActionTimelineFactory;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Native BBS Clips container owned directly by an actor's ReplayKeyframes. */
public final class RecordedPovActions extends Clips
{
    public RecordedPovActions()
    {
        super("pov_actions", new PovActionTimelineFactory());
    }

    private final List<PovActionClip> sessionClips = new ArrayList<>();

    public PovActionClip add(PovActionType type, int tick, int duration)
    {
        PovActionClip clip = switch (type)
        {
            case GUI -> new GuiPovActionClip();
            case MENU -> new MenuPovActionClip();
            case CAMERA_SHAKE -> new CameraShakePovActionClip();
            case PARTICLE_EFFECT -> new ParticleEffectPovActionClip();
            case BOSS_BARS -> new BossBarPovActionClip();
            case SCREEN_EFFECT -> new ScreenEffectPovActionClip();
            case STATUS_EFFECTS -> new Glaxium.POV.actions.clip.StatusEffectsPovActionClip();
            case TOASTS -> new Glaxium.POV.actions.clip.ToastPovActionClip();
            case CHAT -> new Glaxium.POV.actions.clip.ChatPovActionClip();
            default -> new SemanticHudPovActionClip(type);
        };

        clip.tick.set(Math.max(0, tick));
        clip.duration.set(Math.max(1, duration));
        clip.layer.set(type.seedLayer());
        this.addClip(clip);
        this.sessionClips.add(clip);
        this.sync();

        return clip;
    }

    public Glaxium.POV.actions.clip.ChatPovActionClip getActiveChat(float tick)
    {
        Glaxium.POV.actions.clip.ChatPovActionClip top = null;

        for (Clip clip : this.get())
        {
            if (clip instanceof Glaxium.POV.actions.clip.ChatPovActionClip chat && chat.isActive(tick)
                && (top == null || chat.layer.get() >= top.layer.get()))
            {
                top = chat;
            }
        }

        return top;
    }

    public CameraShakePovActionClip getActiveCameraShake(float tick)
    {
        CameraShakePovActionClip top = null;

        for (Clip clip : this.get())
        {
            if (clip instanceof CameraShakePovActionClip shake && shake.isActive(tick)
                && (top == null || shake.layer.get() >= top.layer.get()))
            {
                top = shake;
            }
        }

        return top;
    }

    public Glaxium.POV.actions.clip.StatusEffectsPovActionClip getActiveStatusEffects(float tick)
    {
        Glaxium.POV.actions.clip.StatusEffectsPovActionClip top = null;

        for (Clip clip : this.get())
        {
            if (clip instanceof Glaxium.POV.actions.clip.StatusEffectsPovActionClip effects && effects.isActive(tick)
                && (top == null || effects.layer.get() >= top.layer.get()))
            {
                top = effects;
            }
        }

        return top;
    }

    public List<PovActionClip> takeSessionClips()
    {
        List<PovActionClip> recorded = new ArrayList<>(this.sessionClips);
        this.sessionClips.clear();
        return recorded;
    }

    public void clearAll()
    {
        for (Clip clip : new ArrayList<>(this.get()))
        {
            this.remove(clip);
        }
        this.sync();
    }

    /**
     * Trims action clips for an outside recording session within [startTick, endTick]:
     * - Clips starting at or after endTick are kept untouched.
     * - Clips ending at or before startTick are kept untouched.
     * - Clips fully within [startTick, endTick] are removed.
     * - Clips straddling startTick are trimmed to end at startTick.
     * - Clips straddling endTick are cut so their left part is removed and they start at endTick.
     * - Clips straddling both startTick and endTick are split into left and right pieces.
     */
    public void trimForRecordingRange(int startTick, int endTick)
    {
        List<Clip> toAdd = new ArrayList<>();

        for (Clip c : new ArrayList<>(this.get()))
        {
            int start = c.tick.get();
            int duration = c.duration.get();
            int end = start + duration;

            if (end <= startTick || start >= endTick)
            {
                continue;
            }

            if (start >= startTick && end <= endTick)
            {
                this.remove(c);
                continue;
            }

            if (start < startTick && end > endTick)
            {
                Clip rightPiece = c.copy();
                int cutAmount = endTick - start;
                rightPiece.tick.set(endTick);
                rightPiece.duration.set(Math.max(1, end - endTick));
                if (rightPiece instanceof PovActionClip povRight)
                {
                    for (KeyframeChannel<?> channel : povRight.getChannels())
                    {
                        trimLeftChannel(channel, cutAmount);
                    }
                }
                toAdd.add(rightPiece);

                int leftDuration = Math.max(1, startTick - start);
                c.duration.set(leftDuration);
                if (c instanceof PovActionClip povClip)
                {
                    for (KeyframeChannel<?> channel : povClip.getChannels())
                    {
                        trimRightChannel(channel, leftDuration);
                    }
                }
                continue;
            }

            if (start < startTick && end <= endTick)
            {
                int newDuration = Math.max(1, startTick - start);
                c.duration.set(newDuration);
                if (c instanceof PovActionClip povClip)
                {
                    for (KeyframeChannel<?> channel : povClip.getChannels())
                    {
                        trimRightChannel(channel, newDuration);
                    }
                }
                continue;
            }

            if (start < endTick && end > endTick)
            {
                int cutAmount = endTick - start;
                c.tick.set(endTick);
                c.duration.set(Math.max(1, end - endTick));
                if (c instanceof PovActionClip povClip)
                {
                    for (KeyframeChannel<?> channel : povClip.getChannels())
                    {
                        trimLeftChannel(channel, cutAmount);
                    }
                }
                continue;
            }
        }

        for (Clip clip : toAdd)
        {
            this.addClip(clip);
        }

        this.sync();
    }

    public void trimForRecordingAt(int timelineTick)
    {
        this.trimForRecordingRange(timelineTick, Integer.MAX_VALUE);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void trimRightChannel(KeyframeChannel channel, float maxDuration)
    {
        if (channel.isEmpty())
        {
            return;
        }

        boolean hasKeyAtEnd = false;
        for (Object obj : channel.getKeyframes())
        {
            Keyframe<?> kf = (Keyframe<?>) obj;
            if (Math.abs(kf.getTick() - maxDuration) < 0.0001F)
            {
                hasKeyAtEnd = true;
                break;
            }
        }

        if (!hasKeyAtEnd)
        {
            Object initial = ((Keyframe<?>) channel.getKeyframes().get(0)).getValue();
            Object endValue = channel.interpolate(maxDuration, initial);
            if (endValue != null)
            {
                if (endValue instanceof ItemStack stack)
                {
                    endValue = stack.copy();
                }
                else if (endValue instanceof Transform transform)
                {
                    endValue = transform.copy();
                }
                channel.insert(maxDuration, endValue);
            }
        }

        List list = channel.getKeyframes();
        for (int i = list.size() - 1; i >= 0; i--)
        {
            Keyframe<?> kf = (Keyframe<?>) list.get(i);
            if (kf.getTick() > maxDuration + 0.0001F)
            {
                channel.remove(i);
            }
        }
        channel.sort();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void trimLeftChannel(KeyframeChannel channel, float cutAmount)
    {
        if (channel.isEmpty())
        {
            return;
        }

        boolean hasKeyAtCut = false;
        for (Object obj : channel.getKeyframes())
        {
            Keyframe<?> kf = (Keyframe<?>) obj;
            if (Math.abs(kf.getTick() - cutAmount) < 0.0001F)
            {
                hasKeyAtCut = true;
                break;
            }
        }

        if (!hasKeyAtCut)
        {
            Object initial = ((Keyframe<?>) channel.getKeyframes().get(0)).getValue();
            Object cutValue = channel.interpolate(cutAmount, initial);
            if (cutValue != null)
            {
                if (cutValue instanceof ItemStack stack)
                {
                    cutValue = stack.copy();
                }
                else if (cutValue instanceof Transform transform)
                {
                    cutValue = transform.copy();
                }
                channel.insert(cutAmount, cutValue);
            }
        }

        List list = channel.getKeyframes();
        for (int i = list.size() - 1; i >= 0; i--)
        {
            Keyframe<?> kf = (Keyframe<?>) list.get(i);
            if (kf.getTick() < cutAmount - 0.0001F)
            {
                channel.remove(i);
            }
        }

        for (Object obj : channel.getKeyframes())
        {
            Keyframe<?> kf = (Keyframe<?>) obj;
            kf.setTick(kf.getTick() - cutAmount);
        }
        channel.sort();
    }

    public List<PovActionClip> getActive(float tick)
    {
        List<PovActionClip> active = new ArrayList<>();

        for (Clip clip : this.get())
        {
            if (clip instanceof PovActionClip povClip && povClip.isActive(tick))
            {
                active.add(povClip);
            }
        }

        active.sort(Comparator.comparingInt(clip -> clip.layer.get()));

        return active;
    }

    public MenuPovActionClip getActiveMenu(float tick)
    {
        MenuPovActionClip top = null;

        for (Clip clip : this.get())
        {
            if (clip instanceof MenuPovActionClip menu && menu.isActive(tick)
                && (top == null || menu.layer.get() >= top.layer.get()))
            {
                top = menu;
            }
        }

        return top;
    }

    public GuiPovActionClip getActiveGui(float tick)
    {
        GuiPovActionClip top = null;

        for (Clip clip : this.get())
        {
            if (clip instanceof GuiPovActionClip gui && gui.isActive(tick)
                && (top == null || gui.layer.get() >= top.layer.get()))
            {
                top = gui;
            }
        }

        return top;
    }

    public List<BossBarPovActionClip> getActiveBossBars(float tick)
    {
        List<BossBarPovActionClip> active = new ArrayList<>();

        for (Clip clip : this.get())
        {
            if (clip instanceof BossBarPovActionClip bar && bar.isActive(tick))
            {
                active.add(bar);
            }
        }
        active.sort(Comparator.comparingInt(c -> c.layer.get()));
        return active;
    }

    public BossBarPovActionClip getActiveBossBar(float tick)
    {
        BossBarPovActionClip top = null;

        for (Clip clip : this.get())
        {
            if (clip instanceof BossBarPovActionClip bar && bar.isActive(tick)
                && (top == null || bar.layer.get() >= top.layer.get()))
            {
                top = bar;
            }
        }

        return top;
    }

    @Override
    public void fromData(BaseType data)
    {
        super.fromData(data);

        for (Clip clip : this.get())
        {
            if (clip instanceof PovActionClip povClip)
            {
                povClip.normalize();
            }
        }
    }
}

package Glaxium.POV.actions.timeline;

import Glaxium.POV.actions.PovActionType;
import Glaxium.POV.actions.clip.BossBarPovActionClip;
import Glaxium.POV.actions.clip.CameraShakePovActionClip;
import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.clip.MenuPovActionClip;
import Glaxium.POV.actions.clip.ParticleEffectPovActionClip;
import Glaxium.POV.actions.clip.PovActionClip;
import Glaxium.POV.actions.clip.ScreenEffectPovActionClip;
import Glaxium.POV.actions.clip.SemanticHudPovActionClip;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.clips.ClipFactoryData;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.factory.IFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Factory consumed directly by BBS's native Action Clips timeline. */
public final class PovActionTimelineFactory implements IFactory<Clip, ClipFactoryData>
{
    private final Map<Link, PovActionType> types = new LinkedHashMap<>();
    private final Map<PovActionType, Link> links = new EnumMap<>(PovActionType.class);

    public PovActionTimelineFactory()
    {
        for (PovActionType type : PovActionType.values())
        {
            Link link = new Link("bbs_pov", type.id);
            this.types.put(link, type);
            this.links.put(type, link);
        }

        this.registerFriendlyNames();
    }

    private void registerFriendlyNames()
    {
        /* Fabric doesn't guarantee that BBS's client entrypoint has populated
         * its L10n object before our own entrypoint runs. Factories are created
         * when BBS builds replay/film data, so register labels here and tolerate
         * any unusually early construction. */
        if (BBSModClient.getL10n() == null)
        {
            return;
        }

        for (PovActionType type : PovActionType.values())
        {
            Link link = this.links.get(type);

            BBSModClient.getL10n()
                .getKey("bbs.ui.camera.clips." + link, type.title)
                .content = type.title;
        }
    }

    @Override
    public Link getType(Clip clip)
    {
        PovActionType type = clip instanceof PovActionClip povClip
            ? povClip.getActionType()
            : PovActionType.GUI;

        return this.links.get(type);
    }

    @Override
    public Clip create(Link link)
    {
        PovActionType type = this.types.getOrDefault(link, PovActionType.GUI);
        Clip clip = switch (type)
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

        clip.layer.set(type.seedLayer());
        return clip;
    }

    @Override
    public ClipFactoryData getData(Clip clip)
    {
        return this.getData(this.getType(clip));
    }

    @Override
    public ClipFactoryData getData(Link link)
    {
        PovActionType type = this.types.getOrDefault(link, PovActionType.GUI);
        Icon icon = Icons.ACTION;
        int color = 0x55DD99;

        if (type == PovActionType.GUI)
        {
            icon = Icons.LAYOUT;
            color = 0x55AAFF;
        }
        else if (type == PovActionType.MENU)
        {
            icon = Icons.LAYOUT;
            color = 0x66CCFF;
        }
        else if (type == PovActionType.PARTICLE_EFFECT)
        {
            icon = Icons.PARTICLE;
            color = 0xFFAA33;
        }
        else if (type == PovActionType.CAMERA_SHAKE)
        {
            icon = Icons.SPHERE;
            color = 0xFF5577;
        }
        else if (type == PovActionType.SCREEN_EFFECT)
        {
            icon = Icons.FADING;
            color = 0xAA55FF;
        }
        else if (type == PovActionType.STATUS_EFFECTS)
        {
            icon = Icons.HEART;
            color = 0xEE4466;
        }
        else if (type == PovActionType.TOASTS)
        {
            icon = Icons.BUBBLE;
            color = 0xFFAA44;
        }
        else if (type == PovActionType.CHAT)
        {
            icon = Icons.CONSOLE;
            color = 0x44DDAA;
        }
        else if (type == PovActionType.BOSS_BARS)
        {
            icon = Icons.SKULL;
            color = 0xCC44EE;
        }

        return new ClipFactoryData(icon, color);
    }

    @Override
    public Collection<Link> getKeys()
    {
        return new ArrayList<>(this.types.keySet());
    }
}

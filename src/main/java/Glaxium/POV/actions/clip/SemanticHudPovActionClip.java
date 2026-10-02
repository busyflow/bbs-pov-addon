package Glaxium.POV.actions.clip;

import Glaxium.POV.actions.PovActionType;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public final class SemanticHudPovActionClip extends PovActionClip
{
    public final ValueString hudType = new ValueString("hud_type", PovActionType.TOASTS.id);
    public final KeyframeChannel<String> state = this.channel("state", KeyframeFactories.STRING);
    public final KeyframeChannel<Float> opacity = this.channel("opacity", KeyframeFactories.FLOAT);

    public SemanticHudPovActionClip()
    {
        this(PovActionType.TOASTS);
    }

    public SemanticHudPovActionClip(PovActionType type)
    {
        super();
        this.hudType.set(type == null ? PovActionType.TOASTS.id : type.id);
        this.add(this.hudType);
    }

    @Override
    public PovActionType getActionType()
    {
        /* PovActionClip's constructor asks for the type before this subclass's
         * fields have been initialized. Use the serialized default during
         * that short construction window; the typed constructor replaces the
         * title and value immediately afterward. */
        return this.hudType == null
            ? PovActionType.TOASTS
            : PovActionType.fromId(this.hudType.get());
    }

    @Override
    public void normalize()
    {
        super.normalize();
        constant(this.state);
        clamp(this.opacity, 0F, 1F);
    }

    @Override
    protected Clip create()
    {
        return new SemanticHudPovActionClip(this.getActionType());
    }
}

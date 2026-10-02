package wemppy.bbs_pov.clips;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import java.util.ArrayList;

public class MenuClip extends CameraClip
{
    public final ValueString menuType = new ValueString("menu_type", "death");

    public final ValueString deathMessage = new ValueString("death_message", "Goober got smashed");
    public final ValueString score = new ValueString("score", "3453556");
    public final ValueInt redBackground = new ValueInt("red_background", 0x60500000);
    public final ValueBoolean buttonsActive = new ValueBoolean("buttons_active", true);
    public final ValueBoolean hardcore = new ValueBoolean("hardcore", false);

    public final KeyframeChannel<String> deathMessageChannel = new KeyframeChannel<>("death_message_kf", KeyframeFactories.STRING);
    public final KeyframeChannel<String> scoreChannel = new KeyframeChannel<>("score_kf", KeyframeFactories.STRING);
    public final KeyframeChannel<Color> redBackgroundChannel = new KeyframeChannel<>("red_background_kf", KeyframeFactories.COLOR);
    public final KeyframeChannel<Boolean> buttonsActiveChannel = new KeyframeChannel<>("buttons_active_kf", KeyframeFactories.BOOLEAN);

    public MenuClip()
    {
        super();

        this.add(this.menuType);
        this.add(this.deathMessage);
        this.add(this.score);
        this.add(this.redBackground);
        this.add(this.buttonsActive);
        this.add(this.hardcore);

        this.add(this.deathMessageChannel);
        this.add(this.scoreChannel);
        this.add(this.redBackgroundChannel);
        this.add(this.buttonsActiveChannel);
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        if (!this.enabled.get())
        {
            return;
        }

        float relTick = context.relativeTick + context.transition;
        float factor = this.envelope.factorEnabled(this.duration.get(), relTick);

        if (factor <= 0F)
        {
            return;
        }

        String msg = !this.deathMessageChannel.isEmpty()
            ? this.deathMessageChannel.interpolate(relTick)
            : this.deathMessage.get();

        String sc = !this.scoreChannel.isEmpty()
            ? this.scoreChannel.interpolate(relTick)
            : this.score.get();

        int bg = !this.redBackgroundChannel.isEmpty()
            ? this.redBackgroundChannel.interpolate(relTick).getARGBColor()
            : this.redBackground.get();

        boolean btns = !this.buttonsActiveChannel.isEmpty()
            ? this.buttonsActiveChannel.interpolate(relTick)
            : this.buttonsActive.get();

        MenuOverlayData data = new MenuOverlayData(
            this.menuType.get(),
            msg,
            sc,
            bg,
            btns,
            this.hardcore.get(),
            factor
        );

        context.clipData.get("bbs_menus", ArrayList::new).add(data);
    }

    @Override
    protected Clip create()
    {
        return new MenuClip();
    }
}

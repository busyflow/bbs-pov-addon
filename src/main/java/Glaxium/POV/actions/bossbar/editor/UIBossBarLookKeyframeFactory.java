package Glaxium.POV.actions.bossbar.editor;

import Glaxium.POV.actions.bossbar.BossBarLooks;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UITrackValue;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory;

/**
 * Keyframe factory for boss bar color and style channels.
 * Displays a button cycling through available colors or styles, matching the actions settings panel.
 */
public class UIBossBarLookKeyframeFactory extends UIKeyframeFactory<String>
{
    public enum Mode
    {
        COLOR,
        STYLE
    }

    private final Mode mode;
    private final UIButton button;
    private String lastValue;

    public UIBossBarLookKeyframeFactory(UITrackValue<String> track, UIKeyframes editor, Mode mode)
    {
        super(track, editor);

        this.mode = mode;
        this.lastValue = track.getValue();

        this.button = new UIButton(IKey.constant(this.getButtonLabel()), this::onButtonClicked);
        this.scroll.add((IUIElement) this.button);
    }

    private String getButtonLabel()
    {
        String val = this.track.getValue();
        if (this.mode == Mode.COLOR)
        {
            return "Color: " + BossBarLooks.displayColor(val);
        }
        else
        {
            return "Style: " + BossBarLooks.displayStyle(val);
        }
    }

    private void updateButtonLabel()
    {
        this.button.label = IKey.constant(this.getButtonLabel());
    }

    private void onButtonClicked(UIButton button)
    {
        String current = this.track.getValue();
        String next = this.mode == Mode.COLOR
            ? BossBarLooks.nextColor(current)
            : BossBarLooks.nextStyle(current);

        this.setValue(next);
        this.lastValue = next;
        this.updateButtonLabel();
    }

    @Override
    public void update()
    {
        super.update();

        String val = this.track.getValue();
        if (val == null ? this.lastValue != null : !val.equals(this.lastValue))
        {
            this.lastValue = val;
            this.updateButtonLabel();
        }
    }

    @Override
    public void render(UIContext context)
    {
        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xff141414);
        super.render(context);
    }
}

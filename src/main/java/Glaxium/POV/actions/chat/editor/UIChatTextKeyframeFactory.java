package Glaxium.POV.actions.chat.editor;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextarea;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

/** Keyframe inspector for Chat Text with a 4x tall scrollable text area. */
public class UIChatTextKeyframeFactory extends UIKeyframeFactory<String>
{
    public UIFormattedTextarea textarea;
    private String lastValue;

    public UIChatTextKeyframeFactory(Keyframe<String> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.lastValue = keyframe.getValue();
        this.textarea = new UIFormattedTextarea((str) ->
        {
            this.setValue(str);
            this.lastValue = str;
        });
        this.textarea.h(80);
        this.textarea.setFormattedText(this.lastValue != null ? this.lastValue : "");

        this.scroll.add(
            UI.label(IKey.constant("Chat Text:")),
            this.textarea
        );
    }

    @Override
    public void update()
    {
        super.update();

        String val = this.keyframe.getValue();
        if (val == null ? this.lastValue != null : !val.equals(this.lastValue))
        {
            this.lastValue = val;
            this.textarea.setFormattedText(val != null ? val : "");
        }
    }

    @Override
    public void render(UIContext context)
    {
        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xff141414);
        super.render(context);
    }
}

package Glaxium.POV.hand.editor;

import Glaxium.POV.render.PovViewportMetrics;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.forms.UIFormPalette;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

import Glaxium.POV.editor.UIPovEditor;
import Glaxium.POV.integration.access.bbs.UIFilmPanelPovAccess;

/**
 * Keyframe factory for model selection that displays a "Pick Model" button
 * which opens BBS's Form menu over the full film editor, exactly like the replay pick button does.
 */
public class UIModelKeyframeFactory extends UIKeyframeFactory<String>
{
    private final UIButton pickModel;
    private String currentModel;

    public UIModelKeyframeFactory(Keyframe<String> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.currentModel = keyframe.getValue();
        if (this.currentModel == null)
        {
            this.currentModel = "";
        }

        this.pickModel = new UIButton(IKey.constant(this.getButtonLabel()), this::onPickModelClicked);
        this.scroll.add((IUIElement) this.pickModel);
    }

    private String getButtonLabel()
    {
        if (this.currentModel == null || this.currentModel.isBlank())
        {
            return "Pick Model";
        }
        return "Model: " + this.currentModel;
    }

    private void updateButtonLabel()
    {
        this.pickModel.label = IKey.constant(this.getButtonLabel());
    }

    private void onPickModelClicked(UIButton button)
    {
        ModelForm currentForm = new ModelForm();
        if (this.currentModel != null && !this.currentModel.isBlank())
        {
            currentForm.model.set(this.currentModel);
        }

        UIElement parent = null;
        UIFilmPanel filmPanel = PovViewportMetrics.resolveFilmPanel();
        if (filmPanel != null)
        {
            parent = filmPanel;
        }
        else if (this.getRoot() != null)
        {
            parent = this.getParentContainer();
        }
        if (parent == null)
        {
            parent = this.getParent();
        }
        if (parent == null)
        {
            parent = this;
        }

        UIFormPalette palette = UIFormPalette.open(parent, false, currentForm, (form) -> {
            if (form == null)
            {
                return;
            }
            Form root = FormUtils.getRoot(form);
            if (root instanceof ModelForm mf)
            {
                String pickedModel = mf.model.get();
                if (pickedModel != null && !pickedModel.isBlank())
                {
                    this.setModel(pickedModel);
                    if (filmPanel instanceof UIFilmPanelPovAccess access)
                    {
                        UIPovEditor povEditor = access.bbsPov$getEditor();
                        if (povEditor != null)
                        {
                            povEditor.refreshSheets(false);
                        }
                    }
                }
            }
        });

        if (palette != null)
        {
            palette.updatable();
        }
    }

    private void setModel(String model)
    {
        this.currentModel = model == null ? "" : model;
        this.setValue(this.currentModel);
        this.updateButtonLabel();
    }

    @Override
    public void update()
    {
        super.update();

        String val = this.keyframe.getValue();
        if (val != null && !val.equals(this.currentModel))
        {
            this.currentModel = val;
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

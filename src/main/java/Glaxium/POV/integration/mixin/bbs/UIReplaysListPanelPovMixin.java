package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.editor.UIPovEditor;
import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.integration.access.bbs.UIFilmPanelPovAccess;
import Glaxium.POV.integration.access.bbs.UIReplaysListPanelPovAccess;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.replays.UIReplayList;
import mchorse.bbs_mod.ui.film.replays.UIReplaysListPanel;
import mchorse.bbs_mod.ui.forms.editors.UIForms;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UISection;
import mchorse.bbs_mod.ui.utils.UIConstants;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Consumer;

@Mixin(value = UIReplaysListPanel.class, remap = false)
public abstract class UIReplaysListPanelPovMixin extends UIElement implements UIReplaysListPanelPovAccess
{
    @Shadow @Final public UIElement content;
    @Shadow @Final public UIElement bar;
    @Shadow @Final public UIReplayList replays;
    @Shadow private UIFilmPanel filmPanel;
    @Shadow private Replay bodyPartsReplay;
    @Shadow @Final public UIForms bodyParts;
    @Shadow @Final private UISection bodyPartsSection;

    @Unique private UIForms bbsPov$povBodyParts;
    @Unique private UISection bbsPov$povBodyPartsSection;
    @Unique private boolean bbsPov$povActive;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void bbsPov$init(UIFilmPanel panel, Consumer<List<Replay>> callback, Consumer<Form> formConsumer, Consumer<String> partConsumer, CallbackInfo info)
    {
        this.bbsPov$povBodyParts = new UIForms(list ->
        {
            if (!list.isEmpty())
            {
                String path = list.get(0).getPath();
                if (this.filmPanel instanceof UIFilmPanelPovAccess access)
                {
                    UIPovEditor editor = access.bbsPov$getEditor();
                    if (editor != null)
                    {
                        editor.selectBodyPart(path);
                    }
                }
            }
        });

        this.bbsPov$povBodyPartsSection = new UISection(IKey.constant("POV Body parts"));
        this.bbsPov$povBodyPartsSection.fields.add(this.bbsPov$povBodyParts);
        this.bbsPov$povBodyPartsSection.setExpanded(false);

        int padding = UIConstants.MARGIN;
        this.bbsPov$povBodyPartsSection.relative(this.content).x(padding).y(1F, -padding).w(1F, -padding * 2).anchorY(1F);
        this.bbsPov$povBodyPartsSection.setVisible(false);

        this.content.addAfter(this.bodyPartsSection, this.bbsPov$povBodyPartsSection);
    }

    @Inject(method = "setBodyPartsReplay", at = @At("TAIL"))
    private void bbsPov$updatePovBodyParts(Replay replay, mchorse.bbs_mod.film.replays.tracks.TimelineBodyPartSelection selection, CallbackInfo info)
    {
        if (this.bbsPov$povBodyParts == null)
        {
            return;
        }

        if (replay == null || !(replay.keyframes instanceof ReplayKeyframesPovAccess keyAccess))
        {
            this.bbsPov$povBodyParts.clear();
            return;
        }

        RecordedHandData hand = keyAccess.bbsPov$getHand();
        Form handForm = hand != null && hand.baseForm.get() != null
            ? hand.baseForm.get()
            : replay.form.get();

        if (handForm == null)
        {
            this.bbsPov$povBodyParts.clear();
            return;
        }

        this.bbsPov$povBodyParts.setForm(handForm);
        if (this.filmPanel instanceof UIFilmPanelPovAccess access)
        {
            UIPovEditor editor = access.bbsPov$getEditor();
            if (editor != null && editor.getSelectedBodyPart() != null)
            {
                this.bbsPov$povBodyParts.setCurrentPath(editor.getSelectedBodyPart());
            }
        }
    }

    @Inject(method = "resize", at = @At("HEAD"), cancellable = true)
    private void bbsPov$resize(CallbackInfo info)
    {
        info.cancel();

        boolean hasReplay = this.bodyPartsReplay != null;
        boolean pov = this.bbsPov$povActive;
        int maxHeight = Math.min(160, this.getFlex().getH() / 2);

        int rowsHeight1 = this.bodyParts.getList().size() * this.bodyParts.scroll.scrollItemSize;
        this.bodyParts.h(Math.max(1, Math.min(rowsHeight1, maxHeight)));

        if (this.bbsPov$povBodyParts != null)
        {
            int rowsHeight2 = this.bbsPov$povBodyParts.getList().size() * this.bbsPov$povBodyParts.scroll.scrollItemSize;
            this.bbsPov$povBodyParts.h(Math.max(1, Math.min(rowsHeight2, maxHeight)));
        }

        this.bodyPartsSection.setVisible(hasReplay && !pov);
        if (this.bbsPov$povBodyPartsSection != null)
        {
            this.bbsPov$povBodyPartsSection.setVisible(hasReplay && pov);
        }

        UISection activeSection = pov ? this.bbsPov$povBodyPartsSection : this.bodyPartsSection;
        this.replays.hTo(hasReplay && activeSection != null ? activeSection.area : this.content.area, hasReplay ? 0F : 1F);

        super.resize();
    }

    @Override
    public void bbsPov$setPovMode(boolean povActive)
    {
        this.bbsPov$povActive = povActive;
        this.resize();
        if (this.content != null)
        {
            this.content.resize();
        }
    }

    @Override
    public UIForms bbsPov$getPovBodyParts()
    {
        return this.bbsPov$povBodyParts;
    }
}

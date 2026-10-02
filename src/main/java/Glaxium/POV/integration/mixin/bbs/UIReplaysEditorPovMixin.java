package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.editor.UIPovEditor;
import Glaxium.POV.integration.access.bbs.UIFilmPanelPovAccess;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Keeps BBS 2.5's native hotbar data, while presenting its slot tracks only
 * in BBS-POV's dedicated editor instead of duplicating them in Replay Editor. */
@Mixin(value = UIReplaysEditor.class, remap = false)
public class UIReplaysEditorPovMixin
{
    @Shadow private UIFilmPanel filmPanel;

    @Inject(method = "selectBodyPart", at = @At("HEAD"))
    private void bbsPov$onSelectBodyPart(String path, CallbackInfo info)
    {
        if (this.filmPanel instanceof UIFilmPanelPovAccess access && access.bbsPov$isPovActive())
        {
            UIPovEditor povEditor = access.bbsPov$getEditor();
            if (povEditor != null)
            {
                povEditor.selectBodyPart(path);
            }
        }
    }

    @Inject(method = "pickFormBone", at = @At("HEAD"))
    private void bbsPov$selectOwnerReplayOnPick(mchorse.bbs_mod.forms.forms.Form form, String bone, boolean insert, CallbackInfo info)
    {
        if (form == null || this.filmPanel == null || this.filmPanel.getData() == null)
        {
            return;
        }

        UIReplaysEditor self = (UIReplaysEditor) (Object) this;
        mchorse.bbs_mod.forms.forms.Form root = mchorse.bbs_mod.forms.FormUtils.getRoot(form);
        mchorse.bbs_mod.film.Film film = (mchorse.bbs_mod.film.Film) this.filmPanel.getData();

        for (mchorse.bbs_mod.film.replays.Replay r : film.replays.getList())
        {
            if (r != null && r.form.get() != null)
            {
                mchorse.bbs_mod.forms.forms.Form rRoot = mchorse.bbs_mod.forms.FormUtils.getRoot(r.form.get());
                if (r.form.get() == root || rRoot == root)
                {
                    if (self.getReplay() != r)
                    {
                        self.setReplay(r);
                    }
                    break;
                }
            }
        }
    }

    @Inject(method = "updateChannelsList", at = @At("HEAD"), cancellable = true)
    private void bbsPov$preventReplayEditorUpdateInPov(CallbackInfo info)
    {
        if (this.filmPanel instanceof UIFilmPanelPovAccess access && access.bbsPov$isPovActive())
        {
            info.cancel();
        }
    }

    @Inject(method = "collectCuratedSheets", at = @At("RETURN"))
    private void bbsPov$hideHotbarSlotsInReplayEditor(
        List<UIKeyframeSheet> sheets,
        CallbackInfo info)
    {
        sheets.removeIf(sheet -> bbsPov$isPovOwnedReplaySheet(sheet.id));
    }

    private static boolean bbsPov$isPovOwnedReplaySheet(String id)
    {
        if ("item_off_hand".equals(id) || "selected_slot".equals(id))
        {
            return true;
        }

        for (int i = 0; i < ReplayKeyframes.HOTBAR_SIZE; i++)
        {
            if (ReplayKeyframes.hotbarChannelId(i).equals(id))
            {
                return true;
            }
        }

        return false;
    }
}

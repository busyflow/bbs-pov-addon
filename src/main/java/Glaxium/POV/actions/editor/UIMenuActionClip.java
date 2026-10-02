package Glaxium.POV.actions.editor;

import Glaxium.POV.actions.clip.MenuPovActionClip;
import Glaxium.POV.actions.menu.MenuTypeEntry;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.utils.icons.Icons;

/** Menu inspector: cycle Game Menu / Death / Sleep; Edit Keyframes hidden for Game Menu. */
public class UIMenuActionClip extends UIPovActionClip<MenuPovActionClip>
{
    public UIButton editKeyframes;
    public UIButton menuType;
    public UIKeyframeEditor keyframes;
    public UIElement menuKeyframesSection;

    public UIMenuActionClip(MenuPovActionClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.keyframes = new UIKeyframeEditor((consumer) -> new UIFilmKeyframes(this.editor, consumer));
        this.keyframes.view.duration(() -> this.clip.duration.get());

        this.editKeyframes = new UIButton(IKey.constant("Edit Keyframes"), (button) ->
        {
            this.updateKeyframeSheets();
            this.editor.embedView(this.keyframes);
            this.keyframes.view.resetView();
            if (this.keyframes.view.getGraph() != null)
            {
                this.keyframes.view.getGraph().clearSelection();
            }
        });

        this.menuType = new UIButton(IKey.constant("Menu Type"), (button) ->
        {
            MenuTypeEntry next = MenuTypeEntry.next(this.clip.resolveType());
            this.editor.editMultiple(this.clip.state, (channel) ->
            {
                if (channel.isEmpty())
                {
                    channel.insert(0F, next.id);
                }
                else
                {
                    channel.get(0).setValue(next.id);
                }
            });
            this.updateMenuTypeButton(next.id);
            this.updateKeyframeSectionVisibility();
            this.updateKeyframeSheets();
        });
    }

    private void updateKeyframeSheets()
    {
        String type = this.clip.resolveType();
        this.keyframes.view.removeAllSheets();

        if ("death".equals(type))
        {
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "death_message",
                IKey.constant("Death Message"),
                0xffffff,
                this.clip.deathMessage,
                null).icon(Icons.FONT).seed(() -> ""));
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "score",
                IKey.constant("Score"),
                0xffff55,
                this.clip.score,
                null).icon(Icons.FONT).seed(() -> "Score: 0"));
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "bg_opacity",
                IKey.constant("Red Background"),
                0xff5555,
                this.clip.bgOpacity,
                null).icon(Icons.COLOR).seed(() -> 1F));
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "buttons_active",
                IKey.constant("Buttons Active"),
                0xaaaaaa,
                this.clip.buttonsActive,
                null).icon(Icons.POINTER).seed(() -> false));
        }
        else if ("sleep".equals(type))
        {
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "opacity",
                IKey.constant("Darkness"),
                0x888888,
                this.clip.opacity,
                null).icon(Icons.FADING).seed(() -> 1F));
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "leave_bed",
                IKey.constant("Leave Bed"),
                0xcccccc,
                this.clip.leaveBed,
                null).icon(Icons.POINTER).seed(() -> true));
        }
    }

    private void updateKeyframeSectionVisibility()
    {
        boolean show = !"game_menu".equals(this.clip.resolveType());
        if (this.menuKeyframesSection == null)
        {
            return;
        }

        if (show)
        {
            if (!this.menuKeyframesSection.hasParent())
            {
                this.panels.add(this.menuKeyframesSection);
            }
        }
        else if (this.menuKeyframesSection.hasParent())
        {
            this.menuKeyframesSection.removeFromParent();
        }

        this.resize();
        if (this.panels != null)
        {
            this.panels.resize();
        }
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(
            IKey.constant("Menu Settings"),
            this.menuType));

        this.menuKeyframesSection = this.section(
            IKey.constant("Menu Keyframes"),
            this.editKeyframes);
    }

    @Override
    public void fillData()
    {
        super.fillData();
        this.updateMenuTypeButton(this.clip.resolveType());
        this.updateKeyframeSectionVisibility();
        this.updateKeyframeSheets();

        if (this.keyframes != null && this.keyframes.view != null && this.keyframes.view.getGraph() != null)
        {
            this.keyframes.view.getGraph().clearSelection();
        }
    }

    @Override
    public void render(mchorse.bbs_mod.ui.framework.UIContext context)
    {
        if (this.keyframes != null && !this.keyframes.hasParent())
        {
            if (this.keyframes.view != null
                && this.keyframes.view.getGraph() != null
                && this.keyframes.view.getGraph().getSelected() != null)
            {
                this.keyframes.view.getGraph().clearSelection();
            }
        }
        super.render(context);
    }

    private void updateMenuTypeButton(String id)
    {
        MenuTypeEntry entry = MenuTypeEntry.findById(id);
        String name = entry == null ? (id == null || id.isBlank() ? "Game Menu" : id) : entry.name;
        this.menuType.label = IKey.constant("Menu: " + name);
    }
}

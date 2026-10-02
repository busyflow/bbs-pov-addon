package Glaxium.POV.actions.editor;

import Glaxium.POV.actions.clip.ToastPovActionClip;
import Glaxium.POV.actions.toast.ToastPresets;
import Glaxium.POV.actions.toast.ToastTypeEntry;
import Glaxium.POV.actions.toast.editor.UIToastOverlayPanel;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIUnifiedPickOverlayPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/** Inspector panel for ToastPovActionClip. */
public class UIToastActionClip extends UIPovActionClip<ToastPovActionClip>
{
    public UIButton pickToast;
    public UIElement previewCard;
    public UITextbox customTitle;
    public UITextbox customDescription;
    public UIButton pickItem;
    public UIIcon clearItem;
    public UIButton pickTexture;
    public UIIcon clearTexture;

    public UIToastActionClip(ToastPovActionClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.pickToast = new UIButton(IKey.constant("Pick Toast Type"), (b) -> this.openToastPicker());
        this.pickToast.tooltip(IKey.constant("Choose a preset toast from Advancements, Recipes, Tutorials, or System notifications"));

        this.previewCard = new UIElement()
        {
            @Override
            public void render(UIContext context)
            {
                int x = this.area.x;
                int y = this.area.y;
                int w = this.area.w;
                int h = this.area.h;

                context.batcher.box(x, y, x + w, y + h, 0xCC181818);
                context.batcher.outline(x, y, x + w, y + h, 0x33FFFFFF);

                DrawContext dc = context.batcher.getContext();
                if (dc != null)
                {
                    ToastTypeEntry entry = ToastPresets.getById(UIToastActionClip.this.clip.getPresetId());
                    Link customTex = UIToastActionClip.this.clip.getCustomTexture();

                    if (customTex != null)
                    {
                        Texture tex = BBSModClient.getTextures().getTexture(customTex);
                        if (tex != null && tex.isValid() && tex.id > 0)
                        {
                            context.batcher.fullTexturedBox(tex, x + 6, y + (h - 16) / 2, 16, 16);
                        }
                    }
                    else
                    {
                        String iconId = UIToastActionClip.this.clip.getEffectiveIcon();
                        ItemStack stack = createItemStack(iconId);
                        RenderSystem.enableBlend();
                        RenderSystem.enableDepthTest();
                        RenderSystem.depthFunc(515);
                        RenderSystem.depthMask(true);
                        if ("recipe".equalsIgnoreCase(UIToastActionClip.this.clip.getEffectiveFrameType()))
                        {
                            dc.getMatrices().push();
                            dc.getMatrices().translate(x + 2, y + (h - 16) / 2 - 2, 0);
                            dc.getMatrices().scale(0.6F, 0.6F, 1.0F);
                            dc.drawItem(new ItemStack(Items.CRAFTING_TABLE), 0, 0);
                            dc.getMatrices().pop();
                        }
                        dc.drawItem(stack, x + 6, y + (h - 16) / 2);
                        RenderSystem.disableDepthTest();
                    }

                    String title = UIToastActionClip.this.clip.getEffectiveTitle();
                    String desc = UIToastActionClip.this.clip.getEffectiveDescription();

                    int maxW = Math.max(10, w - 34);
                    var tr = context.batcher.getFont().getRenderer();
                    String limitedTitle = tr != null ? tr.trimToWidth(title, maxW) : title;
                    String limitedDesc = tr != null ? tr.trimToWidth(desc, maxW) : desc;
                    int descColor = "recipe".equalsIgnoreCase(UIToastActionClip.this.clip.getEffectiveFrameType()) ? 0xFF333333 : 0xFFAAAAAA;

                    context.batcher.text(limitedTitle, x + 28, y + 4, entry.getTitleColor(), false);
                    context.batcher.text(limitedDesc, x + 28, y + 14, descColor, false);
                }

                super.render(context);
            }
        };
        this.previewCard.h(28);

        this.customTitle = new UITextbox(100, (text) -> this.clip.setCustomTitle(text));
        this.customTitle.tooltip(IKey.constant("Optional custom title override (leave empty to use preset title)"));

        this.customDescription = new UITextbox(100, (text) -> this.clip.setCustomDescription(text));
        this.customDescription.tooltip(IKey.constant("Optional custom description override (leave empty to use preset description)"));

        this.pickItem = new UIButton(IKey.constant("Pick Item"), (b) -> this.openItemPicker());
        this.pickItem.tooltip(IKey.constant("Select custom item icon for this toast"));
        this.pickItem.h(20);

        this.clearItem = new UIIcon(Icons.CLOSE, (b) ->
        {
            this.clip.setCustomIcon("");
            this.fillData();
            this.editor.fillData();
        });
        this.clearItem.tooltip(IKey.constant("Reset custom item"));
        this.clearItem.wh(20, 20);

        this.pickTexture = new UIButton(IKey.constant("Pick Texture"), (b) -> this.openTexturePicker());
        this.pickTexture.tooltip(IKey.constant("Choose a custom texture (ignores item icon if selected)"));
        this.pickTexture.h(20);

        this.clearTexture = new UIIcon(Icons.CLOSE, (b) ->
        {
            this.clip.setCustomTexture(null);
            this.fillData();
            this.editor.fillData();
        });
        this.clearTexture.tooltip(IKey.constant("Remove custom texture"));
        this.clearTexture.wh(20, 20);
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(
            IKey.constant("Toast Preset"),
            this.previewCard,
            this.pickToast));

        UIElement itemRow = UI.row(2, 0, 20, this.pickItem, this.clearItem).h(20);
        UIElement textureRow = UI.row(2, 0, 20, this.pickTexture, this.clearTexture).h(20);

        this.panels.add(this.section(
            IKey.constant("Custom Overrides"),
            this.customTitle,
            this.customDescription,
            itemRow,
            textureRow));
    }

    @Override
    public void fillData()
    {
        super.fillData();

        if (!this.customTitle.isFocused())
        {
            this.customTitle.setText(this.clip.getCustomTitle());
        }
        this.customTitle.placeholder(IKey.constant(this.clip.getEffectiveTitle()));

        if (!this.customDescription.isFocused())
        {
            this.customDescription.setText(this.clip.getCustomDescription());
        }
        this.customDescription.placeholder(IKey.constant(this.clip.getEffectiveDescription()));

        String iconId = this.clip.getCustomIcon();
        if (iconId != null && !iconId.isEmpty())
        {
            String shortName = iconId.replace("minecraft:", "");
            this.pickItem.label = IKey.constant("Item: " + shortName);
            this.clearItem.setVisible(true);
        }
        else
        {
            this.pickItem.label = IKey.constant("Pick Item");
            this.clearItem.setVisible(false);
        }

        Link texture = this.clip.getCustomTexture();
        if (texture != null)
        {
            this.pickTexture.label = IKey.constant("Tex: " + texture.path);
            this.clearTexture.setVisible(true);
        }
        else
        {
            this.pickTexture.label = IKey.constant("Pick Texture");
            this.clearTexture.setVisible(false);
        }
    }

    private void openToastPicker()
    {
        UIContext context = this.getContext();
        if (context == null)
        {
            return;
        }

        UIToastOverlayPanel panel = new UIToastOverlayPanel(IKey.constant("Pick Toast"), (entry) ->
        {
            this.clip.applyPreset(entry);
            this.fillData();
            this.editor.fillData();
        });

        UIOverlay.addOverlay(context, panel, 240, 200);
    }

    private void openItemPicker()
    {
        UIContext context = this.getContext();
        if (context == null)
        {
            return;
        }

        ItemStack currentStack = createItemStack(this.clip.getEffectiveIcon());

        UIUnifiedPickOverlayPanel panel = UIUnifiedPickOverlayPanel.forItem((stack) ->
        {
            if (stack != null && !stack.isEmpty())
            {
                String id = Registries.ITEM.getId(stack.getItem()).toString();
                this.clip.setCustomIcon(id);
                this.fillData();
                this.editor.fillData();
            }
        }, currentStack);

        UIOverlay.addOverlay(context, panel, 280, 240);
    }

    private void openTexturePicker()
    {
        UIContext context = this.getContext();
        if (context == null)
        {
            return;
        }

        UITexturePicker.open(context, this.clip.getCustomTexture(), (link) ->
        {
            this.clip.setCustomTexture(link);
            this.fillData();
            this.editor.fillData();
        });
    }

    private static ItemStack createItemStack(String iconId)
    {
        if (iconId != null && !iconId.isEmpty())
        {
            try
            {
                Item item = Registries.ITEM.get(new Identifier(iconId));
                if (item != null && item != Items.AIR)
                {
                    return new ItemStack(item);
                }
            }
            catch (Exception ignored)
            {}
        }
        return new ItemStack(Items.DIAMOND);
    }
}

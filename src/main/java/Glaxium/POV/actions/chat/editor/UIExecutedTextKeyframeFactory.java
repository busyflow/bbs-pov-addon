package Glaxium.POV.actions.chat.editor;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UITrackValue;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory;
import mchorse.bbs_mod.ui.utils.UI;

/**
 * Keyframe editor factory for Executed Text channel.
 * Provides a 4x tall scrollable text area with selection highlight,
 * a color selector on top, and N, B, U, I formatting buttons below it.
 */
public class UIExecutedTextKeyframeFactory extends UIKeyframeFactory<String>
{
    public static final String HIDE_HUD_PREFIX = "\u0001HIDE_HUD\u0001";

    private static final int[][] MC_COLORS = new int[][]{
        {0x000000, 0}, // §0 Black
        {0x0000AA, 1}, // §1 Dark Blue
        {0x00AA00, 2}, // §2 Dark Green
        {0x00AAAA, 3}, // §3 Dark Aqua
        {0xAA0000, 4}, // §4 Dark Red
        {0xAA00AA, 5}, // §5 Dark Purple
        {0xFFAA00, 6}, // §6 Gold
        {0xAAAAAA, 7}, // §7 Gray
        {0x555555, 8}, // §8 Dark Gray
        {0x5555FF, 9}, // §9 Blue
        {0x55FF55, 10}, // §a Green
        {0x55FFFF, 11}, // §b Aqua
        {0xFF5555, 12}, // §c Red
        {0xFF55FF, 13}, // §d Light Purple
        {0xFFFF55, 14}, // §e Yellow
        {0xFFFFFF, 15}  // §f White
    };
    private static final char[] MC_CHARS = new char[]{
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    public UIFormattedTextarea textarea;
    public UIColor color;
    public UIButton btnN;
    public UIButton btnB;
    public UIButton btnU;
    public UIButton btnI;
    public UIToggle showOnHud;

    private String lastValue;
    private int lastEquippedColor = -1;

    public static boolean isHiddenFromHud(String value)
    {
        return value != null && value.startsWith(HIDE_HUD_PREFIX);
    }

    public static String getRawText(String value)
    {
        if (value != null && value.startsWith(HIDE_HUD_PREFIX))
        {
            return value.substring(HIDE_HUD_PREFIX.length());
        }
        return value != null ? value : "";
    }

    public static String formatValue(String rawText, boolean showOnHud)
    {
        if (!showOnHud)
        {
            return HIDE_HUD_PREFIX + (rawText != null ? rawText : "");
        }
        return rawText != null ? rawText : "";
    }

    public UIExecutedTextKeyframeFactory(UITrackValue<String> track, UIKeyframes editor)
    {
        super(track, editor);

        this.lastValue = track.getValue();
        this.textarea = new UIFormattedTextarea((str) ->
        {
            this.updateKeyframeValue(str, this.showOnHud == null || this.showOnHud.getValue());
        });
        this.textarea.h(80);
        this.textarea.setFormattedText(getRawText(this.lastValue));

        this.color = new UIColor((col) ->
        {
            String code = getClosestColorCode(col);
            this.textarea.applyFormat(code);
        });
        this.color.h(20);

        this.btnN = new UIButton(IKey.constant("N"), (b) -> this.textarea.applyNormal());
        this.btnN.tooltip(IKey.constant("Normal / Reset Style (Keep Color)"));

        this.btnB = new UIButton(IKey.constant("B"), (b) -> this.textarea.applyFormat("§l"));
        this.btnB.tooltip(IKey.constant("Bold (§l)"));

        this.btnU = new UIButton(IKey.constant("U"), (b) -> this.textarea.applyFormat("§n"));
        this.btnU.tooltip(IKey.constant("Underline (§n)"));

        this.btnI = new UIButton(IKey.constant("I"), (b) -> this.textarea.applyFormat("§o"));
        this.btnI.tooltip(IKey.constant("Italic (§o)"));

        this.showOnHud = new UIToggle(IKey.constant("Pop up on HUD"), !isHiddenFromHud(this.lastValue), (toggle) ->
        {
            this.updateKeyframeValue(this.textarea.getText(), toggle.getValue());
        });
        this.showOnHud.tooltip(IKey.constant("When disabled, the text won't pop up and fade away on the HUD, but will still show in the chat screen history."));

        this.scroll.add(
            UI.label(IKey.constant("Executed Text:")),
            this.textarea,
            this.color,
            UI.row(this.btnN, this.btnB, this.btnU, this.btnI),
            this.showOnHud
        );
    }

    private void updateKeyframeValue(String rawText, boolean showOnHud)
    {
        String formatted = formatValue(rawText, showOnHud);
        this.setValue(formatted);
        this.lastValue = formatted;
    }

    private static String getClosestColorCode(int rgb)
    {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        int closestIdx = 15;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < MC_COLORS.length; i++)
        {
            int cR = (MC_COLORS[i][0] >> 16) & 0xFF;
            int cG = (MC_COLORS[i][0] >> 8) & 0xFF;
            int cB = MC_COLORS[i][0] & 0xFF;

            double dist = (r - cR) * (r - cR) * 0.30 + (g - cG) * (g - cG) * 0.59 + (b - cB) * (b - cB) * 0.11;
            if (dist < minDistance)
            {
                minDistance = dist;
                closestIdx = i;
            }
        }

        return "§" + MC_CHARS[closestIdx];
    }

    @Override
    public void update()
    {
        super.update();

        String val = this.track.getValue();
        if (val == null ? this.lastValue != null : !val.equals(this.lastValue))
        {
            this.lastValue = val;
            this.textarea.setFormattedText(getRawText(val));
            if (this.showOnHud != null)
            {
                this.showOnHud.setValue(!isHiddenFromHud(val));
            }
        }
    }

    @Override
    public void render(UIContext context)
    {
        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xff141414);

        if (this.color != null && !this.color.isUserEditing())
        {
            int selectedColor = this.textarea.getSelectedColor();
            if (this.lastEquippedColor != selectedColor)
            {
                this.lastEquippedColor = selectedColor;
                this.color.setColor(selectedColor);
            }
        }

        super.render(context);
    }
}

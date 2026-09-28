package Glaxium.POV.actions.chat.editor;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class UIExecutedTextKeyframeFactory extends UIKeyframeFactory<String> {
   public static final String HIDE_HUD_PREFIX = "\u0001HIDE_HUD\u0001";
   private static final int[][] MC_COLORS = new int[][]{
      {0, 0},
      {170, 1},
      {43520, 2},
      {43690, 3},
      {11141120, 4},
      {11141290, 5},
      {16755200, 6},
      {11184810, 7},
      {5592405, 8},
      {5592575, 9},
      {5635925, 10},
      {5636095, 11},
      {16733525, 12},
      {16733695, 13},
      {16777045, 14},
      {16777215, 15}
   };
   private static final char[] MC_CHARS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
   public UIFormattedTextarea textarea;
   public UIColor color;
   public UIButton btnN;
   public UIButton btnB;
   public UIButton btnU;
   public UIButton btnI;
   public UIToggle showOnHud;
   private String lastValue;
   private int lastEquippedColor = -1;

   public static boolean isHiddenFromHud(String value) {
      return value != null && value.startsWith("\u0001HIDE_HUD\u0001");
   }

   public static String getRawText(String value) {
      if (value != null && value.startsWith("\u0001HIDE_HUD\u0001")) {
         return value.substring("\u0001HIDE_HUD\u0001".length());
      } else {
         return value != null ? value : "";
      }
   }

   public static String formatValue(String rawText, boolean showOnHud) {
      if (!showOnHud) {
         return "\u0001HIDE_HUD\u0001" + (rawText != null ? rawText : "");
      } else {
         return rawText != null ? rawText : "";
      }
   }

   public UIExecutedTextKeyframeFactory(Keyframe<String> keyframe, UIKeyframes editor) {
      super(keyframe, editor);
      this.lastValue = (String)keyframe.getValue();
      this.textarea = new UIFormattedTextarea(str -> this.updateKeyframeValue(str, this.showOnHud == null || this.showOnHud.getValue()));
      this.textarea.h(80);
      this.textarea.setFormattedText(getRawText(this.lastValue));
      this.color = new UIColor(col -> {
         String code = getClosestColorCode(col);
         this.textarea.applyFormat(code);
      });
      this.color.h(20);
      this.btnN = new UIButton(IKey.constant("N"), b -> this.textarea.applyNormal());
      this.btnN.tooltip(IKey.constant("Normal / Reset Style (Keep Color)"));
      this.btnB = new UIButton(IKey.constant("B"), b -> this.textarea.applyFormat("§l"));
      this.btnB.tooltip(IKey.constant("Bold (§l)"));
      this.btnU = new UIButton(IKey.constant("U"), b -> this.textarea.applyFormat("§n"));
      this.btnU.tooltip(IKey.constant("Underline (§n)"));
      this.btnI = new UIButton(IKey.constant("I"), b -> this.textarea.applyFormat("§o"));
      this.btnI.tooltip(IKey.constant("Italic (§o)"));
      this.showOnHud = new UIToggle(
         IKey.constant("Pop up on HUD"), !isHiddenFromHud(this.lastValue), toggle -> this.updateKeyframeValue(this.textarea.getText(), toggle.getValue())
      );
      this.showOnHud.tooltip(IKey.constant("When disabled, the text won't pop up and fade away on the HUD, but will still show in the chat screen history."));
      this.scroll
         .add(
            new IUIElement[]{
               UI.label(IKey.constant("Executed Text:")),
               this.textarea,
               this.color,
               UI.row(new UIElement[]{this.btnN, this.btnB, this.btnU, this.btnI}),
               this.showOnHud
            }
         );
   }

   private void updateKeyframeValue(String rawText, boolean showOnHud) {
      String formatted = formatValue(rawText, showOnHud);
      this.setValue(formatted);
      this.lastValue = formatted;
   }

   private static String getClosestColorCode(int rgb) {
      int r = rgb >> 16 & 0xFF;
      int g = rgb >> 8 & 0xFF;
      int b = rgb & 0xFF;
      int closestIdx = 15;
      double minDistance = Double.MAX_VALUE;

      for (int i = 0; i < MC_COLORS.length; i++) {
         int cR = MC_COLORS[i][0] >> 16 & 0xFF;
         int cG = MC_COLORS[i][0] >> 8 & 0xFF;
         int cB = MC_COLORS[i][0] & 0xFF;
         double dist = (double)((r - cR) * (r - cR)) * 0.3 + (double)((g - cG) * (g - cG)) * 0.59 + (double)((b - cB) * (b - cB)) * 0.11;
         if (dist < minDistance) {
            minDistance = dist;
            closestIdx = i;
         }
      }

      return "§" + MC_CHARS[closestIdx];
   }

   public void update() {
      super.update();
      String val = (String)this.keyframe.getValue();
      if (val == null ? this.lastValue != null : !val.equals(this.lastValue)) {
         this.lastValue = val;
         this.textarea.setFormattedText(getRawText(val));
         if (this.showOnHud != null) {
            this.showOnHud.setValue(!isHiddenFromHud(val));
         }
      }
   }

   public void render(UIContext context) {
      context.batcher.box((float)this.area.x, (float)this.area.y, (float)this.area.ex(), (float)this.area.ey(), -15461356);
      if (this.color != null && !this.color.isUserEditing()) {
         int selectedColor = this.textarea.getSelectedColor();
         if (this.lastEquippedColor != selectedColor) {
            this.lastEquippedColor = selectedColor;
            this.color.setColor(selectedColor);
         }
      }

      super.render(context);
   }
}

package Glaxium.POV.actions.chat.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextarea;
import mchorse.bbs_mod.ui.framework.elements.input.text.utils.Cursor;
import mchorse.bbs_mod.ui.framework.elements.input.text.utils.TextLine;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;

public class UIFormattedTextarea extends UITextarea<UIFormattedTextarea.FormattedLine> {
   public static final int[][] MC_COLOR_MAP = new int[][]{
      {0, 48},
      {170, 49},
      {43520, 50},
      {43690, 51},
      {11141120, 52},
      {11141290, 53},
      {16755200, 54},
      {11184810, 55},
      {5592405, 56},
      {5592575, 57},
      {5635925, 97},
      {5636095, 98},
      {16733525, 99},
      {16733695, 100},
      {16777045, 101},
      {16777215, 102}
   };
   private String currentActiveFormat = "§f";

   public UIFormattedTextarea(Consumer<String> callback) {
      super(callback);
      this.background().wrap().padding(2);
   }

   public int getWrappedWidth() {
      return Math.max(10, this.area.w - this.padding * 2 - 2);
   }

   protected void recalculateSizes() {
      super.recalculateSizes();
      this.horizontal.scrollSize = 0;
   }

   protected UIFormattedTextarea.FormattedLine createTextLine(String line) {
      return new UIFormattedTextarea.FormattedLine(line);
   }

   public void setFormattedText(String formatted) {
      this.text.clear();
      if (formatted != null && !formatted.isEmpty()) {
         String[] lines = formatted.split("\n", -1);

         for (String l : lines) {
            StringBuilder plain = new StringBuilder();
            List<String> formats = new ArrayList<>();
            String currentFormat = "§f";

            for (int i = 0; i < l.length(); i++) {
               char c = l.charAt(i);
               if (c == 167 && i + 1 < l.length()) {
                  char code = l.charAt(i + 1);
                  if (code == 'r' || code == 'R') {
                     currentFormat = "§f";
                  } else if ("0123456789abcdef".indexOf(Character.toLowerCase(code)) >= 0) {
                     currentFormat = "§" + Character.toLowerCase(code);
                  } else if ("lmonk".indexOf(Character.toLowerCase(code)) >= 0) {
                     currentFormat = currentFormat + "§" + Character.toLowerCase(code);
                  }

                  i++;
               } else {
                  plain.append(c);
                  formats.add(currentFormat);
               }
            }

            UIFormattedTextarea.FormattedLine line = new UIFormattedTextarea.FormattedLine(plain.toString(), formats);
            this.text.add(line);
         }

         this.cursor.set(0, 0);
         this.deselect();
         if (this.area.w > 0) {
            this.recalculateWrapping();
            this.recalculateSizes();
         }
      } else {
         this.text.add(new UIFormattedTextarea.FormattedLine(""));
         this.cursor.set(0, 0);
         this.deselect();
      }
   }

   public String getFormattedText() {
      StringBuilder sb = new StringBuilder();

      for (int lineIdx = 0; lineIdx < this.text.size(); lineIdx++) {
         if (lineIdx > 0) {
            sb.append("\n");
         }

         UIFormattedTextarea.FormattedLine line = (UIFormattedTextarea.FormattedLine)this.text.get(lineIdx);
         String lastFormat = null;

         for (int i = 0; i < line.text.length(); i++) {
            String f = i < line.formats.size() ? line.formats.get(i) : "§f";
            if (!f.equals(lastFormat)) {
               sb.append(f);
               lastFormat = f;
            }

            sb.append(line.text.charAt(i));
         }
      }

      return sb.toString();
   }

   public static int getColorRgb(char code) {
      char c = Character.toLowerCase(code);

      for (int[] pair : MC_COLOR_MAP) {
         if (pair[1] == c) {
            return pair[0];
         }
      }

      return 16777215;
   }

   public static char extractColorChar(String format) {
      if (format == null) {
         return 'f';
      } else {
         for (int i = 0; i < format.length(); i++) {
            if (format.charAt(i) == 167 && i + 1 < format.length()) {
               char code = Character.toLowerCase(format.charAt(i + 1));
               if ("0123456789abcdef".indexOf(code) >= 0) {
                  return code;
               }

               i++;
            }
         }

         return 'f';
      }
   }

   public int getSelectedColor() {
      if (this.isSelected()) {
         Cursor min = this.getMin();
         Cursor max = this.getMax();
         if (min.line != max.line || min.offset != max.offset) {
            Character commonColor = null;

            for (int lineIdx = min.line; lineIdx <= max.line && lineIdx < this.text.size(); lineIdx++) {
               UIFormattedTextarea.FormattedLine line = (UIFormattedTextarea.FormattedLine)this.text.get(lineIdx);
               int from = lineIdx == min.line ? min.offset : 0;
               int to = lineIdx == max.line ? max.offset : line.text.length();

               for (int i = from; i < to && i < line.formats.size(); i++) {
                  char c = extractColorChar(line.formats.get(i));
                  if (commonColor == null) {
                     commonColor = c;
                  } else if (commonColor != c) {
                     return 0;
                  }
               }
            }

            if (commonColor != null) {
               return getColorRgb(commonColor);
            }
         }
      }

      if (this.hasLine(this.cursor.line)) {
         UIFormattedTextarea.FormattedLine line = (UIFormattedTextarea.FormattedLine)this.text.get(this.cursor.line);
         int idx = this.cursor.offset;
         if (idx >= line.formats.size()) {
            idx = line.formats.size() - 1;
         }

         if (idx >= 0 && idx < line.formats.size()) {
            char c = extractColorChar(line.formats.get(idx));
            return getColorRgb(c);
         }
      }

      char c = extractColorChar(this.currentActiveFormat);
      return getColorRgb(c);
   }

   public void writeCharacter(String character) {
      if (this.hasLine(this.cursor.line)) {
         UIFormattedTextarea.FormattedLine line = (UIFormattedTextarea.FormattedLine)this.text.get(this.cursor.line);
         int index = this.cursor.offset;
         String currentFormat = this.currentActiveFormat != null
            ? this.currentActiveFormat
            : (index > 0 && index <= line.formats.size() ? line.formats.get(index - 1) : "§f");
         super.writeCharacter(character);

         for (int k = 0; k < character.length(); k++) {
            if (index + k <= line.formats.size()) {
               line.formats.add(index + k, currentFormat);
            } else {
               line.formats.add(currentFormat);
            }
         }
      } else {
         super.writeCharacter(character);
      }

      this.notifyFormattedChanged();
   }

   public String deleteCharacter() {
      if (this.hasLine(this.cursor.line)) {
         UIFormattedTextarea.FormattedLine line = (UIFormattedTextarea.FormattedLine)this.text.get(this.cursor.line);
         int index = this.cursor.offset;
         if (index > 0 && index <= line.formats.size()) {
            line.formats.remove(index - 1);
         }
      }

      String res = super.deleteCharacter();
      this.notifyFormattedChanged();
      return res;
   }

   public void deleteSelection() {
      if (this.isSelected()) {
         Cursor min = this.getMin();
         Cursor max = this.getMax();
         if (min.line == max.line && this.hasLine(min.line)) {
            UIFormattedTextarea.FormattedLine line = (UIFormattedTextarea.FormattedLine)this.text.get(min.line);
            int from = Math.min(min.offset, max.offset);
            int to = Math.max(min.offset, max.offset);

            for (int i = to - 1; i >= from && i < line.formats.size(); i--) {
               line.formats.remove(i);
            }
         } else {
            for (int i = max.line; i >= min.line; i--) {
               if (this.hasLine(i)) {
                  UIFormattedTextarea.FormattedLine line = (UIFormattedTextarea.FormattedLine)this.text.get(i);
                  if (i == max.line) {
                     for (int k = max.offset - 1; k >= 0 && k < line.formats.size(); k--) {
                        line.formats.remove(k);
                     }
                  } else if (i == min.line) {
                     for (int k = line.formats.size() - 1; k >= min.offset; k--) {
                        line.formats.remove(k);
                     }
                  }
               }
            }
         }

         super.deleteSelection();
         this.notifyFormattedChanged();
      }
   }

   public void writeNewLine() {
      if (this.hasLine(this.cursor.line)) {
         UIFormattedTextarea.FormattedLine line = (UIFormattedTextarea.FormattedLine)this.text.get(this.cursor.line);
         int index = this.cursor.offset;
         List<String> nextFormats = new ArrayList<>();
         if (index < line.formats.size()) {
            nextFormats.addAll(line.formats.subList(index, line.formats.size()));
            line.formats.subList(index, line.formats.size()).clear();
         }

         super.writeNewLine();
         if (this.hasLine(this.cursor.line)) {
            UIFormattedTextarea.FormattedLine nextLine = (UIFormattedTextarea.FormattedLine)this.text.get(this.cursor.line);
            nextLine.formats.clear();
            nextLine.formats.addAll(nextFormats);
         }
      } else {
         super.writeNewLine();
      }

      this.notifyFormattedChanged();
   }

   public void applyFormat(String code) {
      if (this.isSelected()) {
         Cursor min = this.getMin();
         Cursor max = this.getMax();

         for (int lineIdx = min.line; lineIdx <= max.line && lineIdx < this.text.size(); lineIdx++) {
            UIFormattedTextarea.FormattedLine line = (UIFormattedTextarea.FormattedLine)this.text.get(lineIdx);
            int from = lineIdx == min.line ? min.offset : 0;
            int to = lineIdx == max.line ? max.offset : line.text.length();

            for (int i = from; i < to && i < line.formats.size(); i++) {
               String cur = line.formats.get(i);
               if (isColorCode(code)) {
                  String styles = extractStyles(cur);
                  line.formats.set(i, code + styles);
               } else if (cur.contains(code)) {
                  line.formats.set(i, cur.replace(code, ""));
               } else {
                  line.formats.set(i, cur + code);
               }
            }
         }
      } else if (isColorCode(code)) {
         String styles = extractStyles(this.currentActiveFormat != null ? this.currentActiveFormat : "§f");
         this.currentActiveFormat = code + styles;
      } else {
         String cur = this.currentActiveFormat != null ? this.currentActiveFormat : "§f";
         if (cur.contains(code)) {
            this.currentActiveFormat = cur.replace(code, "");
         } else {
            this.currentActiveFormat = cur + code;
         }
      }

      this.notifyFormattedChanged();
   }

   public void applyNormal() {
      if (this.isSelected()) {
         Cursor min = this.getMin();
         Cursor max = this.getMax();

         for (int lineIdx = min.line; lineIdx <= max.line && lineIdx < this.text.size(); lineIdx++) {
            UIFormattedTextarea.FormattedLine line = (UIFormattedTextarea.FormattedLine)this.text.get(lineIdx);
            int from = lineIdx == min.line ? min.offset : 0;
            int to = lineIdx == max.line ? max.offset : line.text.length();

            for (int i = from; i < to && i < line.formats.size(); i++) {
               char colorChar = extractColorChar(line.formats.get(i));
               line.formats.set(i, "§" + colorChar);
            }
         }
      } else {
         char colorChar = extractColorChar(this.currentActiveFormat);
         this.currentActiveFormat = "§" + colorChar;
      }

      this.notifyFormattedChanged();
   }

   private void notifyFormattedChanged() {
      if (this.callback != null) {
         this.callback.accept(this.getFormattedText());
      }
   }

   private static boolean isColorCode(String code) {
      if (code != null && code.length() >= 2 && code.charAt(0) == 167) {
         char c = Character.toLowerCase(code.charAt(1));
         return "0123456789abcdef".indexOf(c) >= 0;
      } else {
         return false;
      }
   }

   private static String extractStyles(String format) {
      if (format == null) {
         return "";
      } else {
         StringBuilder sb = new StringBuilder();

         for (int i = 0; i < format.length(); i++) {
            if (format.charAt(i) == 167 && i + 1 < format.length()) {
               char c = Character.toLowerCase(format.charAt(i + 1));
               if ("lmno".indexOf(c) >= 0) {
                  sb.append("§").append(c);
               }

               i++;
            }
         }

         return sb.toString();
      }
   }

   protected void renderTextLine(UIContext context, String lineText, int i, int j, int nx, int ny) {
      if (i >= 0 && i < this.text.size()) {
         UIFormattedTextarea.FormattedLine line = (UIFormattedTextarea.FormattedLine)this.text.get(i);
         FontRenderer font = this.getFont();
         int curX = nx;
         int startChar = 0;
         if (line.wrappedLines != null && j > 0) {
            for (int w = 0; w < j; w++) {
               startChar += ((String)line.wrappedLines.get(w)).length();
            }
         }

         for (int k = 0; k < lineText.length(); k++) {
            int globalCharIdx = startChar + k;
            String f = globalCharIdx < line.formats.size() ? line.formats.get(globalCharIdx) : "§f";
            String ch = String.valueOf(lineText.charAt(k));
            context.batcher.text(f + ch, (float)curX, (float)ny, -1, true);
            curX += font.getWidth(ch);
         }
      } else {
         super.renderTextLine(context, lineText, i, j, nx, ny);
      }
   }

   public static class FormattedLine extends TextLine {
      public final List<String> formats = new ArrayList<>();

      public FormattedLine(String plainText) {
         super(plainText);

         for (int i = 0; i < plainText.length(); i++) {
            this.formats.add("§f");
         }
      }

      public FormattedLine(String plainText, List<String> formats) {
         super(plainText);
         this.formats.addAll(formats);

         while (this.formats.size() < plainText.length()) {
            this.formats.add("§f");
         }
      }

      public void calculateWrappedLines(FontRenderer font, int w) {
         if (!this.text.isEmpty() && font.getWidth(this.text) > w) {
            List<String> lines = new ArrayList<>();
            int left = 0;
            int c = this.text.length();

            while (left < c) {
               int right = left;

               while (right < c && font.getWidth(this.text.substring(left, right + 1)) <= w) {
                  right++;
               }

               if (right == c) {
                  lines.add(this.text.substring(left));
                  break;
               }

               if (right == left) {
                  right = left + 1;
               } else {
                  String chunk = this.text.substring(left, right);
                  int spaceIdx = chunk.lastIndexOf(32);
                  if (spaceIdx > 0 && spaceIdx >= chunk.length() - 4) {
                     right = left + spaceIdx + 1;
                  }
               }

               lines.add(this.text.substring(left, right));
               left = right;
            }

            this.wrappedLines = lines.size() < 2 ? null : lines;
         } else {
            this.wrappedLines = null;
         }
      }
   }
}

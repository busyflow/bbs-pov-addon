package Glaxium.POV.actions.chat.editor;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextarea;
import mchorse.bbs_mod.ui.framework.elements.input.text.utils.Cursor;
import mchorse.bbs_mod.ui.framework.elements.input.text.utils.TextLine;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Rich formatted text area that manages Minecraft style codes (§c, §a, §l, §r, etc.)
 * per character, ensuring 1:1 character cursor tracking, accurate selection highlight,
 * boundary-checked wrapping, and reliable backspace / typing.
 */
public class UIFormattedTextarea extends UITextarea<UIFormattedTextarea.FormattedLine>
{
    public static final int[][] MC_COLOR_MAP = new int[][]{
        {0x000000, '0'}, // §0 Black
        {0x0000AA, '1'}, // §1 Dark Blue
        {0x00AA00, '2'}, // §2 Dark Green
        {0x00AAAA, '3'}, // §3 Dark Aqua
        {0xAA0000, '4'}, // §4 Dark Red
        {0xAA00AA, '5'}, // §5 Dark Purple
        {0xFFAA00, '6'}, // §6 Gold
        {0xAAAAAA, '7'}, // §7 Gray
        {0x555555, '8'}, // §8 Dark Gray
        {0x5555FF, '9'}, // §9 Blue
        {0x55FF55, 'a'}, // §a Green
        {0x55FFFF, 'b'}, // §b Aqua
        {0xFF5555, 'c'}, // §c Red
        {0xFF55FF, 'd'}, // §d Light Purple
        {0xFFFF55, 'e'}, // §e Yellow
        {0xFFFFFF, 'f'}  // §f White
    };

    private String currentActiveFormat = "§f";

    public static class FormattedLine extends TextLine
    {
        public final List<String> formats = new ArrayList<>();

        public FormattedLine(String plainText)
        {
            super(plainText);
            for (int i = 0; i < plainText.length(); i++)
            {
                this.formats.add("§f");
            }
        }

        public FormattedLine(String plainText, List<String> formats)
        {
            super(plainText);
            this.formats.addAll(formats);
            while (this.formats.size() < plainText.length())
            {
                this.formats.add("§f");
            }
        }

        @Override
        public void calculateWrappedLines(FontRenderer font, int w)
        {
            if (this.text.isEmpty() || font.getWidth(this.text) <= w)
            {
                this.wrappedLines = null;
                return;
            }

            List<String> lines = new ArrayList<>();
            int left = 0;
            int c = this.text.length();

            while (left < c)
            {
                int right = left;
                while (right < c && font.getWidth(this.text.substring(left, right + 1)) <= w)
                {
                    right++;
                }

                if (right == c)
                {
                    lines.add(this.text.substring(left));
                    break;
                }

                if (right == left)
                {
                    right = left + 1;
                }
                else
                {
                    String chunk = this.text.substring(left, right);
                    int spaceIdx = chunk.lastIndexOf(' ');
                    if (spaceIdx > 0 && spaceIdx >= chunk.length() - 4)
                    {
                        right = left + spaceIdx + 1;
                    }
                }

                lines.add(this.text.substring(left, right));
                left = right;
            }

            this.wrappedLines = lines.size() < 2 ? null : lines;
        }
    }

    public UIFormattedTextarea(Consumer<String> callback)
    {
        super(callback);
        this.background().wrap().padding(2);
    }

    @Override
    public int getWrappedWidth()
    {
        return Math.max(10, this.area.w - this.padding * 2 - 2);
    }

    @Override
    protected void recalculateSizes()
    {
        super.recalculateSizes();
        this.horizontal.scrollSize = 0;
    }

    @Override
    protected FormattedLine createTextLine(String line)
    {
        return new FormattedLine(line);
    }

    public void setFormattedText(String formatted)
    {
        this.text.clear();
        if (formatted == null || formatted.isEmpty())
        {
            this.text.add(new FormattedLine(""));
            this.cursor.set(0, 0);
            this.deselect();
            return;
        }

        String[] lines = formatted.split("\n", -1);
        for (String l : lines)
        {
            StringBuilder plain = new StringBuilder();
            List<String> formats = new ArrayList<>();
            String currentFormat = "§f";

            for (int i = 0; i < l.length(); i++)
            {
                char c = l.charAt(i);
                if (c == '§' && i + 1 < l.length())
                {
                    char code = l.charAt(i + 1);
                    if (code == 'r' || code == 'R')
                    {
                        currentFormat = "§f";
                    }
                    else if ("0123456789abcdef".indexOf(Character.toLowerCase(code)) >= 0)
                    {
                        currentFormat = "§" + Character.toLowerCase(code);
                    }
                    else if ("lmonk".indexOf(Character.toLowerCase(code)) >= 0)
                    {
                        currentFormat += "§" + Character.toLowerCase(code);
                    }
                    i++;
                }
                else
                {
                    plain.append(c);
                    formats.add(currentFormat);
                }
            }

            FormattedLine line = new FormattedLine(plain.toString(), formats);
            this.text.add(line);
        }

        this.cursor.set(0, 0);
        this.deselect();
        if (this.area.w > 0)
        {
            this.recalculateWrapping();
            this.recalculateSizes();
        }
    }

    public String getFormattedText()
    {
        StringBuilder sb = new StringBuilder();
        for (int lineIdx = 0; lineIdx < this.text.size(); lineIdx++)
        {
            if (lineIdx > 0)
            {
                sb.append("\n");
            }
            FormattedLine line = this.text.get(lineIdx);
            String lastFormat = null;
            for (int i = 0; i < line.text.length(); i++)
            {
                String f = (i < line.formats.size()) ? line.formats.get(i) : "§f";
                if (!f.equals(lastFormat))
                {
                    sb.append(f);
                    lastFormat = f;
                }
                sb.append(line.text.charAt(i));
            }
        }
        return sb.toString();
    }

    public static int getColorRgb(char code)
    {
        char c = Character.toLowerCase(code);
        for (int[] pair : MC_COLOR_MAP)
        {
            if (pair[1] == c)
            {
                return pair[0];
            }
        }
        return 0xFFFFFF;
    }

    public static char extractColorChar(String format)
    {
        if (format == null) return 'f';
        for (int i = 0; i < format.length(); i++)
        {
            if (format.charAt(i) == '§' && i + 1 < format.length())
            {
                char code = Character.toLowerCase(format.charAt(i + 1));
                if ("0123456789abcdef".indexOf(code) >= 0)
                {
                    return code;
                }
                i++;
            }
        }
        return 'f';
    }

    public int getSelectedColor()
    {
        if (this.isSelected())
        {
            Cursor min = this.getMin();
            Cursor max = this.getMax();
            if (min.line != max.line || min.offset != max.offset)
            {
                Character commonColor = null;

                for (int lineIdx = min.line; lineIdx <= max.line && lineIdx < this.text.size(); lineIdx++)
                {
                    FormattedLine line = this.text.get(lineIdx);
                    int from = (lineIdx == min.line) ? min.offset : 0;
                    int to = (lineIdx == max.line) ? max.offset : line.text.length();

                    for (int i = from; i < to && i < line.formats.size(); i++)
                    {
                        char c = extractColorChar(line.formats.get(i));
                        if (commonColor == null)
                        {
                            commonColor = c;
                        }
                        else if (commonColor != c)
                        {
                            return 0x000000;
                        }
                    }
                }

                if (commonColor != null)
                {
                    return getColorRgb(commonColor);
                }
            }
        }

        if (this.hasLine(this.cursor.line))
        {
            FormattedLine line = this.text.get(this.cursor.line);
            int idx = this.cursor.offset;
            if (idx >= line.formats.size())
            {
                idx = line.formats.size() - 1;
            }
            if (idx >= 0 && idx < line.formats.size())
            {
                char c = extractColorChar(line.formats.get(idx));
                return getColorRgb(c);
            }
        }
        char c = extractColorChar(this.currentActiveFormat);
        return getColorRgb(c);
    }

    @Override
    public void writeCharacter(String character)
    {
        if (this.hasLine(this.cursor.line))
        {
            FormattedLine line = this.text.get(this.cursor.line);
            int index = this.cursor.offset;
            String currentFormat = this.currentActiveFormat != null ? this.currentActiveFormat : (index > 0 && index <= line.formats.size() ? line.formats.get(index - 1) : "§f");

            super.writeCharacter(character);

            for (int k = 0; k < character.length(); k++)
            {
                if (index + k <= line.formats.size())
                {
                    line.formats.add(index + k, currentFormat);
                }
                else
                {
                    line.formats.add(currentFormat);
                }
            }
        }
        else
        {
            super.writeCharacter(character);
        }

        this.notifyFormattedChanged();
    }

    @Override
    public String deleteCharacter()
    {
        if (this.hasLine(this.cursor.line))
        {
            FormattedLine line = this.text.get(this.cursor.line);
            int index = this.cursor.offset;
            if (index > 0 && index <= line.formats.size())
            {
                line.formats.remove(index - 1);
            }
        }
        String res = super.deleteCharacter();
        this.notifyFormattedChanged();
        return res;
    }

    @Override
    public void deleteSelection()
    {
        if (!this.isSelected())
        {
            return;
        }
        Cursor min = this.getMin();
        Cursor max = this.getMax();
        if (min.line == max.line && this.hasLine(min.line))
        {
            FormattedLine line = this.text.get(min.line);
            int from = Math.min(min.offset, max.offset);
            int to = Math.max(min.offset, max.offset);
            for (int i = to - 1; i >= from && i < line.formats.size(); i--)
            {
                line.formats.remove(i);
            }
        }
        else
        {
            for (int i = max.line; i >= min.line; i--)
            {
                if (!this.hasLine(i)) continue;
                FormattedLine line = this.text.get(i);
                if (i == max.line)
                {
                    for (int k = max.offset - 1; k >= 0 && k < line.formats.size(); k--)
                    {
                        line.formats.remove(k);
                    }
                }
                else if (i == min.line)
                {
                    for (int k = line.formats.size() - 1; k >= min.offset; k--)
                    {
                        line.formats.remove(k);
                    }
                }
            }
        }
        super.deleteSelection();
        this.notifyFormattedChanged();
    }

    @Override
    public void writeNewLine()
    {
        if (this.hasLine(this.cursor.line))
        {
            FormattedLine line = this.text.get(this.cursor.line);
            int index = this.cursor.offset;
            List<String> nextFormats = new ArrayList<>();
            if (index < line.formats.size())
            {
                nextFormats.addAll(line.formats.subList(index, line.formats.size()));
                line.formats.subList(index, line.formats.size()).clear();
            }
            super.writeNewLine();
            if (this.hasLine(this.cursor.line))
            {
                FormattedLine nextLine = this.text.get(this.cursor.line);
                nextLine.formats.clear();
                nextLine.formats.addAll(nextFormats);
            }
        }
        else
        {
            super.writeNewLine();
        }

        this.notifyFormattedChanged();
    }

    public void applyFormat(String code)
    {
        if (this.isSelected())
        {
            Cursor min = this.getMin();
            Cursor max = this.getMax();
            for (int lineIdx = min.line; lineIdx <= max.line && lineIdx < this.text.size(); lineIdx++)
            {
                FormattedLine line = this.text.get(lineIdx);
                int from = (lineIdx == min.line) ? min.offset : 0;
                int to = (lineIdx == max.line) ? max.offset : line.text.length();

                for (int i = from; i < to && i < line.formats.size(); i++)
                {
                    String cur = line.formats.get(i);
                    if (isColorCode(code))
                    {
                        String styles = extractStyles(cur);
                        line.formats.set(i, code + styles);
                    }
                    else
                    {
                        if (cur.contains(code))
                        {
                            line.formats.set(i, cur.replace(code, ""));
                        }
                        else
                        {
                            line.formats.set(i, cur + code);
                        }
                    }
                }
            }
        }
        else
        {
            if (isColorCode(code))
            {
                String styles = extractStyles(this.currentActiveFormat != null ? this.currentActiveFormat : "§f");
                this.currentActiveFormat = code + styles;
            }
            else
            {
                String cur = this.currentActiveFormat != null ? this.currentActiveFormat : "§f";
                if (cur.contains(code))
                {
                    this.currentActiveFormat = cur.replace(code, "");
                }
                else
                {
                    this.currentActiveFormat = cur + code;
                }
            }
        }

        this.notifyFormattedChanged();
    }

    public void applyNormal()
    {
        if (this.isSelected())
        {
            Cursor min = this.getMin();
            Cursor max = this.getMax();
            for (int lineIdx = min.line; lineIdx <= max.line && lineIdx < this.text.size(); lineIdx++)
            {
                FormattedLine line = this.text.get(lineIdx);
                int from = (lineIdx == min.line) ? min.offset : 0;
                int to = (lineIdx == max.line) ? max.offset : line.text.length();

                for (int i = from; i < to && i < line.formats.size(); i++)
                {
                    char colorChar = extractColorChar(line.formats.get(i));
                    line.formats.set(i, "§" + colorChar);
                }
            }
        }
        else
        {
            char colorChar = extractColorChar(this.currentActiveFormat);
            this.currentActiveFormat = "§" + colorChar;
        }

        this.notifyFormattedChanged();
    }

    private void notifyFormattedChanged()
    {
        if (this.callback != null)
        {
            this.callback.accept(this.getFormattedText());
        }
    }

    private static boolean isColorCode(String code)
    {
        if (code == null || code.length() < 2 || code.charAt(0) != '§') return false;
        char c = Character.toLowerCase(code.charAt(1));
        return "0123456789abcdef".indexOf(c) >= 0;
    }

    private static String extractStyles(String format)
    {
        if (format == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < format.length(); i++)
        {
            if (format.charAt(i) == '§' && i + 1 < format.length())
            {
                char c = Character.toLowerCase(format.charAt(i + 1));
                if ("lmno".indexOf(c) >= 0)
                {
                    sb.append("§").append(c);
                }
                i++;
            }
        }
        return sb.toString();
    }

    @Override
    protected void renderTextLine(UIContext context, String lineText, int i, int j, int nx, int ny)
    {
        if (i < 0 || i >= this.text.size())
        {
            super.renderTextLine(context, lineText, i, j, nx, ny);
            return;
        }

        FormattedLine line = this.text.get(i);
        FontRenderer font = this.getFont();
        int curX = nx;

        int startChar = 0;
        if (line.wrappedLines != null && j > 0)
        {
            for (int w = 0; w < j; w++)
            {
                startChar += line.wrappedLines.get(w).length();
            }
        }

        for (int k = 0; k < lineText.length(); k++)
        {
            int globalCharIdx = startChar + k;
            String f = (globalCharIdx < line.formats.size()) ? line.formats.get(globalCharIdx) : "§f";
            String ch = String.valueOf(lineText.charAt(k));
            context.batcher.text(f + ch, curX, ny, Colors.WHITE, true);
            curX += font.getWidth(ch);
        }
    }
}

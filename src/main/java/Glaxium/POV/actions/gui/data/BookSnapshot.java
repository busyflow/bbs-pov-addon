package Glaxium.POV.actions.gui.data;

import java.util.ArrayList;
import java.util.List;

/** Extra written-book / book-and-quill pages and caret. */
public final class BookSnapshot
{
    public static final char PAGE_SEP = '\u001e';

    public final boolean writable;
    public final boolean signing;
    public final int page;
    public final String pages;
    public final String title;
    public final String author;
    public final int selStart;
    public final int selEnd;

    public BookSnapshot(
        boolean writable,
        boolean signing,
        int page,
        String pages,
        String title,
        String author,
        int selStart,
        int selEnd)
    {
        this.writable = writable;
        this.signing = signing;
        this.page = Math.max(0, page);
        this.pages = pages == null ? "" : pages;
        this.title = title == null ? "" : title;
        this.author = author == null ? "" : author;
        this.selStart = Math.max(0, selStart);
        this.selEnd = Math.max(0, selEnd);
    }

    public static String pack(List<String> values)
    {
        if (values == null || values.isEmpty())
        {
            return "";
        }

        StringBuilder packed = new StringBuilder();
        for (int i = 0; i < values.size(); i++)
        {
            if (i > 0)
            {
                packed.append(PAGE_SEP);
            }
            String page = values.get(i);
            packed.append(page == null ? "" : page);
        }
        return packed.toString();
    }

    public static List<String> unpack(String packed)
    {
        List<String> values = new ArrayList<>();
        if (packed == null || packed.isEmpty())
        {
            values.add("");
            return values;
        }

        int start = 0;
        for (int i = 0; i < packed.length(); i++)
        {
            if (packed.charAt(i) == PAGE_SEP)
            {
                values.add(packed.substring(start, i));
                start = i + 1;
            }
        }
        values.add(packed.substring(start));
        if (values.isEmpty())
        {
            values.add("");
        }
        return values;
    }
}

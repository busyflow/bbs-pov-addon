package Glaxium.POV.actions.chat.render;

import Glaxium.POV.actions.chat.ChatMorphHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.command.CommandSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Parses and renders command syntax highlighting, error indicators, and command suggestions. */
public final class ChatCommandSuggestor
{
    private static final int[] ARG_COLORS = new int[]{
        0xFF55FFFF, // Aqua
        0xFFFFFF55, // Yellow
        0xFF55FF55, // Light Green
        0xFFFF55FF, // Light Purple
        0xFFFFAA00  // Gold
    };

    private static final java.util.Set<String> ROOT_COMMANDS = new java.util.HashSet<>(java.util.Arrays.asList(
        "advancement", "attribute", "ban", "ban-ip", "banlist", "bossbar", "clear", "clone", "damage", "data", "datapack", "debug", "defaultgamemode",
        "deop", "difficulty", "effect", "enchant", "execute", "experience", "fill", "fillbiome", "forceload",
        "function", "gamemode", "gamerule", "give", "help", "item", "jfr", "kick", "kill", "list",
        "locate", "loot", "me", "msg", "op", "pardon", "pardon-ip", "particle", "place", "playsound", "publish", "recipe", "reload",
        "return", "ride", "save-all", "save-off", "save-on", "say", "schedule", "scoreboard", "seed", "setblock", "setidletimeout", "setworldspawn",
        "spawnpoint", "spectate", "spreadplayers", "stop", "stopsound", "summon", "tag", "team", "teammsg",
        "teleport", "tell", "tellraw", "tick", "time", "title", "tm", "tp", "trigger", "weather", "whitelist", "worldborder", "xp"
    ));

    private static ParseResultInfo cachedResult = null;
    private static String lastParsedText = "";
    private static int lastParsedCursor = -1;
    private static long lastParsedTime = 0L;

    private ChatCommandSuggestor()
    {
    }

    public static class SuggestionEntry
    {
        public final String text;
        public final String tooltip;

        public SuggestionEntry(String text, String tooltip)
        {
            this.text = text;
            this.tooltip = tooltip;
        }
    }

    public static class ParseResultInfo
    {
        public int startOffset = 0;
        public final List<SuggestionEntry> suggestions = new ArrayList<>();
        public String errorMessage = null;
        public int errorIndex = -1; // Char index where error/incomplete syntax starts
        public boolean isExactMatch = false;
        public String usageHint = null; // Command instruction/usage hint (e.g. "<gamemode> [<target>]")
    }

    public static ParseResultInfo getParsedInfo(String text, int cursorPos)
    {
        if (text.equals(lastParsedText) && cursorPos == lastParsedCursor && System.currentTimeMillis() - lastParsedTime < 500L && cachedResult != null)
        {
            return cachedResult;
        }

        lastParsedText = text;
        lastParsedCursor = cursorPos;
        lastParsedTime = System.currentTimeMillis();

        ParseResultInfo info = new ParseResultInfo();
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getCommandDispatcher() != null && text.startsWith("/"))
        {
            try
            {
                CommandDispatcher<CommandSource> dispatcher = mc.getNetworkHandler().getCommandDispatcher();
                String commandWithoutSlash = text.substring(1);
                int cursorInCommand = Math.max(0, Math.min(cursorPos - 1, commandWithoutSlash.length()));

                com.mojang.brigadier.StringReader reader = new com.mojang.brigadier.StringReader(commandWithoutSlash);
                ParseResults<CommandSource> parse = dispatcher.parse(reader, mc.getNetworkHandler().getCommandSource());

                if (parse.getExceptions() != null && !parse.getExceptions().isEmpty())
                {
                    for (com.mojang.brigadier.exceptions.CommandSyntaxException ex : parse.getExceptions().values())
                    {
                        info.errorMessage = ex.getMessage();
                        info.errorIndex = 1 + ex.getCursor();
                        break;
                    }
                }
                else if (parse.getReader().canRead())
                {
                    info.errorIndex = 1 + parse.getReader().getCursor();
                    info.errorMessage = com.mojang.brigadier.exceptions.CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(parse.getReader()).getMessage();
                }

                CompletableFuture<Suggestions> future = dispatcher.getCompletionSuggestions(parse, cursorInCommand);
                Suggestions brigadierSuggestions = future.getNow(null);
                if (brigadierSuggestions != null && !brigadierSuggestions.isEmpty())
                {
                    info.startOffset = 1 + brigadierSuggestions.getRange().getStart();
                    String playerName = mc.player != null && mc.player.getGameProfile() != null ? mc.player.getGameProfile().getName() : null;
                    String morphName = mc.player != null ? ChatMorphHelper.getPlayerMorphName(mc.player) : null;
                    String replayName = ChatMorphHelper.getActiveReplayName();

                    boolean hasPlayerOrSelector = false;
                    for (com.mojang.brigadier.suggestion.Suggestion s : brigadierSuggestions.getList())
                    {
                        String tip = s.getTooltip() != null ? s.getTooltip().getString() : null;
                        String textVal = s.getText();
                        if (playerName != null && textVal.equalsIgnoreCase(playerName))
                        {
                            if (replayName != null && !replayName.isEmpty())
                            {
                                textVal = replayName;
                                hasPlayerOrSelector = true;
                            }
                            else if (morphName != null && !morphName.isEmpty())
                            {
                                textVal = morphName;
                                hasPlayerOrSelector = true;
                            }
                        }
                        else if (replayName != null && !replayName.isEmpty() && morphName != null && textVal.equalsIgnoreCase(morphName))
                        {
                            textVal = replayName;
                            hasPlayerOrSelector = true;
                        }
                        else if (textVal.startsWith("@"))
                        {
                            hasPlayerOrSelector = true;
                        }

                        boolean shouldAdd = true;
                        if (playerName != null && textVal.equalsIgnoreCase(playerName))
                        {
                            if (morphName != null || replayName != null)
                            {
                                shouldAdd = false;
                            }
                        }
                        if (replayName != null && !replayName.isEmpty() && morphName != null && textVal.equalsIgnoreCase(morphName) && !morphName.equalsIgnoreCase(replayName))
                        {
                            shouldAdd = false;
                        }

                        if (shouldAdd)
                        {
                            boolean alreadyHas = false;
                            for (SuggestionEntry se : info.suggestions)
                            {
                                if (se.text.equalsIgnoreCase(textVal))
                                {
                                    alreadyHas = true;
                                    break;
                                }
                            }
                            if (!alreadyHas)
                            {
                                info.suggestions.add(new SuggestionEntry(textVal, tip));
                            }
                        }
                    }

                    if (hasPlayerOrSelector)
                    {
                        if (replayName != null && !replayName.isEmpty())
                        {
                            boolean alreadyHas = false;
                            for (SuggestionEntry se : info.suggestions)
                            {
                                if (se.text.equalsIgnoreCase(replayName))
                                {
                                    alreadyHas = true;
                                    break;
                                }
                            }
                            if (!alreadyHas)
                            {
                                info.suggestions.add(new SuggestionEntry(replayName, null));
                            }
                        }
                    }
                }
            }
            catch (Exception ignored)
            {
            }
        }

        // Standalone / Offline Fallback matching vanilla commands perfectly
        if (text.startsWith("/"))
        {
            String trimmed = text.substring(1);
            int lastSpace = trimmed.lastIndexOf(' ');

            if (lastSpace == -1)
            {
                // Root command typing
                info.startOffset = 1;
                boolean isComplete = ROOT_COMMANDS.contains(trimmed.toLowerCase());
                if (isComplete)
                {
                    info.isExactMatch = true;
                }
                else
                {
                    info.errorIndex = 1; // Root command is incomplete/invalid -> RED
                }

                if (info.suggestions.isEmpty())
                {
                    for (String cmd : ROOT_COMMANDS)
                    {
                        if (cmd.toLowerCase().startsWith(trimmed.toLowerCase()))
                        {
                            info.suggestions.add(new SuggestionEntry(cmd, null));
                        }
                    }
                }
            }
            else
            {
                String root = trimmed.substring(0, lastSpace).trim();
                info.startOffset = 1 + lastSpace + 1;
                String currentArg = trimmed.substring(lastSpace + 1);

                if (root.equals("gamemode") || root.startsWith("gamemode "))
                {
                    info.usageHint = "<gamemode> [<target>]";
                    String[] parts = root.split(" ");
                    if (parts.length == 1) // "/gamemode <mode>"
                    {
                        String[] modes = new String[]{"adventure", "creative", "spectator", "survival"};
                        boolean exactMatch = false;
                        for (String mode : modes)
                        {
                            if (mode.equalsIgnoreCase(currentArg))
                            {
                                exactMatch = true;
                                break;
                            }
                        }

                        if (exactMatch || currentArg.isEmpty())
                        {
                            info.isExactMatch = exactMatch;
                            if (info.suggestions.isEmpty())
                            {
                                for (String mode : modes)
                                {
                                    info.suggestions.add(new SuggestionEntry(mode, null));
                                }
                            }
                        }
                        else
                        {
                            // Incomplete prefix (e.g. "/gamemode creati") -> RED text, filtered suggestions
                            info.errorIndex = info.startOffset;
                            boolean matchesAny = false;
                            for (String mode : modes)
                            {
                                if (mode.toLowerCase().startsWith(currentArg.toLowerCase()))
                                {
                                    info.suggestions.add(new SuggestionEntry(mode, null));
                                    matchesAny = true;
                                }
                            }
                            if (!matchesAny)
                            {
                                info.errorMessage = "Incorrect argument for command at position " + text.length() + ": ... " + currentArg + "<--[HERE]";
                            }
                        }
                    }
                    else if (parts.length == 2) // "/gamemode <mode> <target>"
                    {
                        info.usageHint = "<gamemode> [<target>]";
                        if (info.suggestions.isEmpty())
                        {
                            List<String> targets = getTargetSuggestions();
                            boolean exactMatch = false;
                            for (String t : targets)
                            {
                                if (t.equalsIgnoreCase(currentArg))
                                {
                                    exactMatch = true;
                                    break;
                                }
                            }
                            if (exactMatch || currentArg.isEmpty())
                            {
                                info.isExactMatch = exactMatch;
                                for (String t : targets) info.suggestions.add(new SuggestionEntry(t, null));
                            }
                            else
                            {
                                info.errorIndex = info.startOffset;
                                for (String t : targets)
                                {
                                    if (t.toLowerCase().startsWith(currentArg.toLowerCase()))
                                    {
                                        info.suggestions.add(new SuggestionEntry(t, null));
                                    }
                                }
                            }
                        }
                    }
                    else if (parts.length > 2) // Extra unexpected argument -> error warning
                    {
                        info.errorIndex = info.startOffset;
                        info.errorMessage = "Incorrect argument for command at position " + text.length() + ": ... " + currentArg + "<--[HERE]";
                    }
                }
                else if (root.equals("difficulty"))
                {
                    info.usageHint = "<difficulty>";
                    String[] diffs = new String[]{"peaceful", "easy", "normal", "hard"};
                    boolean exactMatch = false;
                    for (String d : diffs)
                    {
                        if (d.equalsIgnoreCase(currentArg))
                        {
                            exactMatch = true;
                            break;
                        }
                    }
                    if (exactMatch || currentArg.isEmpty())
                    {
                        info.isExactMatch = exactMatch;
                        if (info.suggestions.isEmpty())
                        {
                            for (String d : diffs) info.suggestions.add(new SuggestionEntry(d, null));
                        }
                    }
                    else
                    {
                        info.errorIndex = info.startOffset;
                        for (String d : diffs)
                        {
                            if (d.toLowerCase().startsWith(currentArg.toLowerCase()))
                            {
                                info.suggestions.add(new SuggestionEntry(d, null));
                            }
                        }
                    }
                }
                else if (root.equals("weather"))
                {
                    info.usageHint = "<weather> [duration]";
                    String[] weathers = new String[]{"clear", "rain", "thunder"};
                    boolean exactMatch = false;
                    for (String w : weathers)
                    {
                        if (w.equalsIgnoreCase(currentArg))
                        {
                            exactMatch = true;
                            break;
                        }
                    }
                    if (exactMatch || currentArg.isEmpty())
                    {
                        info.isExactMatch = exactMatch;
                        if (info.suggestions.isEmpty())
                        {
                            for (String w : weathers) info.suggestions.add(new SuggestionEntry(w, null));
                        }
                    }
                    else
                    {
                        info.errorIndex = info.startOffset;
                        for (String w : weathers)
                        {
                            if (w.toLowerCase().startsWith(currentArg.toLowerCase()))
                            {
                                info.suggestions.add(new SuggestionEntry(w, null));
                            }
                        }
                    }
                }
                else if (root.equals("time"))
                {
                    info.usageHint = "<time>";
                    String[] times = new String[]{"add", "query", "set"};
                    boolean exactMatch = false;
                    for (String t : times)
                    {
                        if (t.equalsIgnoreCase(currentArg))
                        {
                            exactMatch = true;
                            break;
                        }
                    }
                    if (exactMatch || currentArg.isEmpty())
                    {
                        info.isExactMatch = exactMatch;
                        if (info.suggestions.isEmpty())
                        {
                            for (String t : times) info.suggestions.add(new SuggestionEntry(t, null));
                        }
                    }
                    else
                    {
                        info.errorIndex = info.startOffset;
                        for (String t : times)
                        {
                            if (t.toLowerCase().startsWith(currentArg.toLowerCase()))
                            {
                                info.suggestions.add(new SuggestionEntry(t, null));
                            }
                        }
                    }
                }
                else if (root.equals("time set"))
                {
                    info.usageHint = "<time>";
                    String[] sets = new String[]{"day", "midnight", "night", "noon"};
                    boolean exactMatch = false;
                    for (String s : sets)
                    {
                        if (s.equalsIgnoreCase(currentArg))
                        {
                            exactMatch = true;
                            break;
                        }
                    }
                    if (exactMatch || currentArg.isEmpty())
                    {
                        info.isExactMatch = exactMatch;
                        if (info.suggestions.isEmpty())
                        {
                            for (String s : sets) info.suggestions.add(new SuggestionEntry(s, null));
                        }
                    }
                    else
                    {
                        info.errorIndex = info.startOffset;
                        for (String s : sets)
                        {
                            if (s.toLowerCase().startsWith(currentArg.toLowerCase()))
                            {
                                info.suggestions.add(new SuggestionEntry(s, null));
                            }
                        }
                    }
                }
                else if (root.equals("tp") || root.equals("teleport"))
                {
                    info.usageHint = "<destination>";
                    if (info.suggestions.isEmpty())
                    {
                        List<String> targets = getTargetSuggestions();
                        boolean exactMatch = false;
                        for (String t : targets)
                        {
                            if (t.equalsIgnoreCase(currentArg))
                            {
                                exactMatch = true;
                                break;
                            }
                        }
                        if (exactMatch || currentArg.isEmpty())
                        {
                            info.isExactMatch = exactMatch;
                            for (String t : targets) info.suggestions.add(new SuggestionEntry(t, null));
                        }
                        else
                        {
                            info.errorIndex = info.startOffset;
                            for (String t : targets)
                            {
                                if (t.toLowerCase().startsWith(currentArg.toLowerCase()))
                                {
                                    info.suggestions.add(new SuggestionEntry(t, null));
                                }
                            }
                        }
                    }
                }
                else if (root.equals("give"))
                {
                    info.usageHint = "<targets> <item> [count]";
                }
                else if (root.equals("kill"))
                {
                    info.usageHint = "[<targets>]";
                    if (info.suggestions.isEmpty())
                    {
                        List<String> targets = getTargetSuggestions();
                        boolean exactMatch = false;
                        for (String t : targets)
                        {
                            if (t.equalsIgnoreCase(currentArg))
                            {
                                exactMatch = true;
                                break;
                            }
                        }
                        if (exactMatch || currentArg.isEmpty())
                        {
                            info.isExactMatch = exactMatch;
                            for (String t : targets) info.suggestions.add(new SuggestionEntry(t, null));
                        }
                        else
                        {
                            info.errorIndex = info.startOffset;
                            for (String t : targets)
                            {
                                if (t.toLowerCase().startsWith(currentArg.toLowerCase()))
                                {
                                    info.suggestions.add(new SuggestionEntry(t, null));
                                }
                            }
                        }
                    }
                }
                else if (root.equals("clear"))
                {
                    info.usageHint = "[<targets>] [<item>] [<maxCount>]";
                }
                else
                {
                    if (!ROOT_COMMANDS.contains(root.toLowerCase()))
                    {
                        info.errorIndex = 1;
                    }
                }
            }
        }

        cachedResult = info;
        return info;
    }

    public static List<String> getTargetSuggestions()
    {
        List<String> targets = new ArrayList<>();
        targets.add("@a");
        targets.add("@e");
        targets.add("@p");
        targets.add("@r");
        targets.add("@s");
        MinecraftClient mc = MinecraftClient.getInstance();
        String morphName = mc.player != null ? ChatMorphHelper.getPlayerMorphName(mc.player) : null;
        String playerName = mc.player != null && mc.player.getGameProfile() != null ? mc.player.getGameProfile().getName() : null;
        String replayName = ChatMorphHelper.getActiveReplayName();

        if (replayName != null && !replayName.isEmpty())
        {
            if (!targets.contains(replayName)) targets.add(replayName);
        }
        else if (morphName != null && !morphName.isEmpty())
        {
            if (!targets.contains(morphName)) targets.add(morphName);
        }
        else if (playerName != null)
        {
            if (!targets.contains(playerName)) targets.add(playerName);
        }

        return targets;
    }

    public static String renderCommandSuggestions(DrawContext context, TextRenderer font, String text, ParseResultInfo info, int textX, int anchorY, int screenWidth, float cursorX, float cursorY, boolean cursorVisible)
    {
        if (info == null)
        {
            return null;
        }

        // 1. If error exists and no suggestions, render error banner
        if (info.suggestions.isEmpty() && info.errorMessage != null && !info.errorMessage.isEmpty())
        {
            int errorWidth = font.getWidth(info.errorMessage);
            int errorY = anchorY - 14;
            int errorX = 2;
            context.fill(errorX, errorY, errorX + errorWidth + 8, errorY + 12, 0xD0000000);
            context.drawTextWithShadow(font, info.errorMessage, errorX + 4, errorY + 2, 0xFFFFFFFF);
            return null;
        }

        String prefixBeforeStart = text.substring(0, Math.min(info.startOffset, text.length()));
        int tokenStartX = textX + font.getWidth(prefixBeforeStart);

        if (info.suggestions.isEmpty())
        {
            if (info.usageHint != null && !info.usageHint.isEmpty())
            {
                int usageWidth = font.getWidth(info.usageHint) + 2;
                int usageX = Math.min(tokenStartX - 1, screenWidth - usageWidth - 2);
                usageX = Math.max(1, usageX);
                int usageY = anchorY - 12;
                context.fill(usageX, usageY, usageX + usageWidth, usageY + 12, 0xD0000000);
                context.drawTextWithShadow(font, info.usageHint, usageX + 1, usageY + 2, 0xFFFFFFFF);
            }
            return null;
        }

        String currentToken = text.substring(Math.min(info.startOffset, text.length()));

        int maxDisplay = Math.min(10, info.suggestions.size());
        int entryHeight = 12;
        int totalHeight = maxDisplay * entryHeight;
        int popupY = anchorY - totalHeight;

        int maxTextWidth = 0;
        for (int i = 0; i < maxDisplay; i++)
        {
            maxTextWidth = Math.max(maxTextWidth, font.getWidth(info.suggestions.get(i).text));
        }

        int popupWidth = maxTextWidth + 2;
        int popupX = Math.min(tokenStartX - 1, screenWidth - popupWidth - 2);
        popupX = Math.max(1, popupX);

        // Hover detection using cursor coordinates
        int hoveredIndex = -1;
        boolean isMouseOverPopup = cursorVisible && cursorX >= popupX && cursorX <= popupX + popupWidth && cursorY >= popupY && cursorY < popupY + totalHeight;
        if (isMouseOverPopup)
        {
            hoveredIndex = (int) ((cursorY - popupY) / entryHeight);
            if (hoveredIndex < 0 || hoveredIndex >= maxDisplay)
            {
                hoveredIndex = -1;
            }
        }

        if (info.isExactMatch && !isMouseOverPopup)
        {
            if (info.usageHint != null && !info.usageHint.isEmpty())
            {
                int usageWidth = font.getWidth(info.usageHint) + 2;
                int usageX = Math.min(tokenStartX - 1, screenWidth - usageWidth - 2);
                usageX = Math.max(1, usageX);
                int usageY = anchorY - 12;
                context.fill(usageX, usageY, usageX + usageWidth, usageY + 12, 0xD0000000);
                context.drawTextWithShadow(font, info.usageHint, usageX + 1, usageY + 2, 0xFFFFFFFF);
            }
            return null;
        }

        int selectedIndex = hoveredIndex;
        if (selectedIndex == -1)
        {
            if (!currentToken.isEmpty())
            {
                for (int i = 0; i < maxDisplay; i++)
                {
                    if (info.suggestions.get(i).text.equalsIgnoreCase(currentToken) || info.suggestions.get(i).text.toLowerCase().startsWith(currentToken.toLowerCase()))
                    {
                        selectedIndex = i;
                        break;
                    }
                }
            }
            if (selectedIndex == -1)
            {
                selectedIndex = 0;
            }
        }

        SuggestionEntry selectedEntry = (selectedIndex >= 0 && selectedIndex < info.suggestions.size()) ? info.suggestions.get(selectedIndex) : null;
        String tooltip = selectedEntry != null ? selectedEntry.tooltip : null;

        if (tooltip != null && !tooltip.isEmpty())
        {
            int tooltipWidth = font.getWidth(tooltip) + 2;
            int tooltipY = popupY - 12;
            int tooltipX = popupX;
            context.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + 12, 0xD0000000);
            context.drawTextWithShadow(font, tooltip, tooltipX + 1, tooltipY + 2, 0xFFAAAAAA);
        }

        context.fill(popupX, popupY, popupX + popupWidth, popupY + totalHeight, 0xD0000000);

        for (int i = 0; i < maxDisplay; i++)
        {
            SuggestionEntry s = info.suggestions.get(i);
            int rowY = popupY + i * entryHeight;
            boolean isSelected = (i == selectedIndex);

            int textColor = isSelected ? 0xFFFFFF55 : 0xFFAAAAAA;
            context.drawTextWithShadow(font, s.text, popupX + 1, rowY + 2, textColor);
        }

        if (selectedEntry != null)
        {
            if (currentToken.isEmpty())
            {
                return selectedEntry.text;
            }
            else if (selectedEntry.text.toLowerCase().startsWith(currentToken.toLowerCase()))
            {
                return selectedEntry.text.substring(currentToken.length());
            }
        }

        return null;
    }

    public static void renderColoredCommandText(DrawContext context, TextRenderer font, String text, int startX, int y, String ghostPreview, ParseResultInfo info)
    {
        if (text.isEmpty())
        {
            if (ghostPreview != null && !ghostPreview.isEmpty())
            {
                context.drawTextWithShadow(font, ghostPreview, startX, y, 0xFF808080);
            }
            return;
        }

        if (!text.startsWith("/"))
        {
            context.drawTextWithShadow(font, text, startX, y, 0xFFFFFFFF);
            return;
        }

        int currentX = startX;
        int errorIdx = (info != null) ? info.errorIndex : -1;

        context.drawTextWithShadow(font, "/", currentX, y, 0xFFE0E0E0);
        currentX += font.getWidth("/");

        String rest = text.substring(1);
        String[] tokens = rest.split(" ", -1);
        int charPos = 1;

        for (int i = 0; i < tokens.length; i++)
        {
            String token = tokens[i];
            int tokenLen = token.length();
            int color;

            if (errorIdx >= 0 && charPos >= errorIdx)
            {
                color = 0xFFFF5555;
            }
            else if (i == 0)
            {
                color = 0xFFE0E0E0;
            }
            else
            {
                color = ARG_COLORS[(i - 1) % ARG_COLORS.length];
            }

            if (!token.isEmpty())
            {
                context.drawTextWithShadow(font, token, currentX, y, color);
                currentX += font.getWidth(token);
            }

            charPos += tokenLen;

            if (i < tokens.length - 1)
            {
                context.drawTextWithShadow(font, " ", currentX, y, 0xFFFFFFFF);
                currentX += font.getWidth(" ");
                charPos += 1;
            }
        }

        if (ghostPreview != null && !ghostPreview.isEmpty())
        {
            context.drawTextWithShadow(font, ghostPreview, currentX, y, 0xFF808080);
        }
    }
}

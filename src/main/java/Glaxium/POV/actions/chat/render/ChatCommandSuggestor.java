package Glaxium.POV.actions.chat.render;

import Glaxium.POV.actions.chat.ChatMorphHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.command.CommandSource;

public final class ChatCommandSuggestor {
   private static final int[] ARG_COLORS = new int[]{-11141121, -171, -11141291, -43521, -22016};
   private static final Set<String> ROOT_COMMANDS = new HashSet<>(
      Arrays.asList(
         "advancement",
         "attribute",
         "ban",
         "ban-ip",
         "banlist",
         "bossbar",
         "clear",
         "clone",
         "damage",
         "data",
         "datapack",
         "debug",
         "defaultgamemode",
         "deop",
         "difficulty",
         "effect",
         "enchant",
         "execute",
         "experience",
         "fill",
         "fillbiome",
         "forceload",
         "function",
         "gamemode",
         "gamerule",
         "give",
         "help",
         "item",
         "jfr",
         "kick",
         "kill",
         "list",
         "locate",
         "loot",
         "me",
         "msg",
         "op",
         "pardon",
         "pardon-ip",
         "particle",
         "place",
         "playsound",
         "publish",
         "recipe",
         "reload",
         "return",
         "ride",
         "save-all",
         "save-off",
         "save-on",
         "say",
         "schedule",
         "scoreboard",
         "seed",
         "setblock",
         "setidletimeout",
         "setworldspawn",
         "spawnpoint",
         "spectate",
         "spreadplayers",
         "stop",
         "stopsound",
         "summon",
         "tag",
         "team",
         "teammsg",
         "teleport",
         "tell",
         "tellraw",
         "tick",
         "time",
         "title",
         "tm",
         "tp",
         "trigger",
         "weather",
         "whitelist",
         "worldborder",
         "xp"
      )
   );
   private static ChatCommandSuggestor.ParseResultInfo cachedResult = null;
   private static String lastParsedText = "";
   private static int lastParsedCursor = -1;
   private static long lastParsedTime = 0L;

   private ChatCommandSuggestor() {
   }

   public static ChatCommandSuggestor.ParseResultInfo getParsedInfo(String text, int cursorPos) {
      if (text.equals(lastParsedText) && cursorPos == lastParsedCursor && System.currentTimeMillis() - lastParsedTime < 500L && cachedResult != null) {
         return cachedResult;
      } else {
         lastParsedText = text;
         lastParsedCursor = cursorPos;
         lastParsedTime = System.currentTimeMillis();
         ChatCommandSuggestor.ParseResultInfo info = new ChatCommandSuggestor.ParseResultInfo();
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getCommandDispatcher() != null && text.startsWith("/")) {
            try {
               CommandDispatcher<CommandSource> dispatcher = mc.getNetworkHandler().getCommandDispatcher();
               String commandWithoutSlash = text.substring(1);
               int cursorInCommand = Math.max(0, Math.min(cursorPos - 1, commandWithoutSlash.length()));
               StringReader reader = new StringReader(commandWithoutSlash);
               ParseResults<CommandSource> parse = dispatcher.parse(reader, mc.getNetworkHandler().getCommandSource());
               if (parse.getExceptions() != null && !parse.getExceptions().isEmpty()) {
                  Iterator exactMatch = parse.getExceptions().values().iterator();
                  if (exactMatch.hasNext()) {
                     CommandSyntaxException ex = (CommandSyntaxException)exactMatch.next();
                     info.errorMessage = ex.getMessage();
                     info.errorIndex = 1 + ex.getCursor();
                  }
               } else if (parse.getReader().canRead()) {
                  info.errorIndex = 1 + parse.getReader().getCursor();
                  info.errorMessage = CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(parse.getReader()).getMessage();
               }

               CompletableFuture<Suggestions> future = dispatcher.getCompletionSuggestions(parse, cursorInCommand);
               Suggestions brigadierSuggestions = future.getNow(null);
               if (brigadierSuggestions != null && !brigadierSuggestions.isEmpty()) {
                  info.startOffset = 1 + brigadierSuggestions.getRange().getStart();
                  String playerName = mc.player != null && mc.player.getGameProfile() != null ? mc.player.getGameProfile().getName() : null;
                  String morphName = mc.player != null ? ChatMorphHelper.getPlayerMorphName(mc.player) : null;
                  String replayName = ChatMorphHelper.getActiveReplayName();
                  List<String> filmReplays = ChatMorphHelper.getAllFilmReplayNames();
                  boolean hasPlayerOrSelector = false;

                  for (Suggestion s : brigadierSuggestions.getList()) {
                     String tip = s.getTooltip() != null ? s.getTooltip().getString() : null;
                     String textVal = s.getText();
                     if (playerName != null && textVal.equalsIgnoreCase(playerName)) {
                        if (replayName != null && !replayName.isEmpty()) {
                           textVal = replayName;
                           hasPlayerOrSelector = true;
                        } else if (morphName != null && !morphName.isEmpty()) {
                           textVal = morphName;
                           hasPlayerOrSelector = true;
                        } else if (!filmReplays.isEmpty()) {
                           textVal = filmReplays.get(0);
                           hasPlayerOrSelector = true;
                        }
                     } else if (replayName != null && !replayName.isEmpty() && morphName != null && textVal.equalsIgnoreCase(morphName)) {
                        textVal = replayName;
                        hasPlayerOrSelector = true;
                     } else if (textVal.startsWith("@")) {
                        hasPlayerOrSelector = true;
                     }

                     boolean shouldAdd = true;
                     if (playerName != null && textVal.equalsIgnoreCase(playerName) && (morphName != null || replayName != null || !filmReplays.isEmpty())) {
                        shouldAdd = false;
                     }

                     if (replayName != null
                        && !replayName.isEmpty()
                        && morphName != null
                        && textVal.equalsIgnoreCase(morphName)
                        && !morphName.equalsIgnoreCase(replayName)) {
                        shouldAdd = false;
                     }

                     if (shouldAdd) {
                        boolean alreadyHas = false;

                        for (ChatCommandSuggestor.SuggestionEntry se : info.suggestions) {
                           if (se.text.equalsIgnoreCase(textVal)) {
                              alreadyHas = true;
                              break;
                           }
                        }

                        if (!alreadyHas) {
                           info.suggestions.add(new ChatCommandSuggestor.SuggestionEntry(textVal, tip));
                        }
                     }
                  }

                  if (hasPlayerOrSelector) {
                     if (replayName != null && !replayName.isEmpty()) {
                        boolean alreadyHas = false;

                        for (ChatCommandSuggestor.SuggestionEntry sex : info.suggestions) {
                           if (sex.text.equalsIgnoreCase(replayName)) {
                              alreadyHas = true;
                              break;
                           }
                        }

                        if (!alreadyHas) {
                           info.suggestions.add(new ChatCommandSuggestor.SuggestionEntry(replayName, null));
                        }
                     }

                     for (String rName : filmReplays) {
                        if (rName != null && !rName.isEmpty()) {
                           boolean alreadyHas = false;

                           for (ChatCommandSuggestor.SuggestionEntry sexx : info.suggestions) {
                              if (sexx.text.equalsIgnoreCase(rName)) {
                                 alreadyHas = true;
                                 break;
                              }
                           }

                           if (!alreadyHas) {
                              info.suggestions.add(new ChatCommandSuggestor.SuggestionEntry(rName, null));
                           }
                        }
                     }
                  }
               }
            } catch (Exception var24) {
            }
         }

         if (text.startsWith("/")) {
            String trimmed = text.substring(1);
            int lastSpace = trimmed.lastIndexOf(32);
            if (lastSpace == -1) {
               info.startOffset = 1;
               boolean isComplete = ROOT_COMMANDS.contains(trimmed.toLowerCase());
               if (isComplete) {
                  info.isExactMatch = true;
               } else {
                  info.errorIndex = 1;
               }

               if (info.suggestions.isEmpty()) {
                  for (String cmd : ROOT_COMMANDS) {
                     if (cmd.toLowerCase().startsWith(trimmed.toLowerCase())) {
                        info.suggestions.add(new ChatCommandSuggestor.SuggestionEntry(cmd, null));
                     }
                  }
               }
            } else {
               String root = trimmed.substring(0, lastSpace).trim();
               info.startOffset = 1 + lastSpace + 1;
               String currentArg = trimmed.substring(lastSpace + 1);
               if (root.equals("gamemode") || root.startsWith("gamemode ")) {
                  info.usageHint = "<gamemode> [<target>]";
                  String[] parts = root.split(" ");
                  if (parts.length == 1) {
                     String[] modes = new String[]{"adventure", "creative", "spectator", "survival"};
                     boolean exactMatch = false;

                     for (String mode : modes) {
                        if (mode.equalsIgnoreCase(currentArg)) {
                           exactMatch = true;
                           break;
                        }
                     }

                     if (!exactMatch && !currentArg.isEmpty()) {
                        info.errorIndex = info.startOffset;
                        boolean matchesAny = false;

                        for (String modex : modes) {
                           if (modex.toLowerCase().startsWith(currentArg.toLowerCase())) {
                              info.suggestions.add(new ChatCommandSuggestor.SuggestionEntry(modex, null));
                              matchesAny = true;
                           }
                        }

                        if (!matchesAny) {
                           info.errorMessage = "Incorrect argument for command at position " + text.length() + ": ... " + currentArg + "<--[HERE]";
                        }
                     } else {
                        info.isExactMatch = exactMatch;
                        if (info.suggestions.isEmpty()) {
                           for (String modexx : modes) {
                              info.suggestions.add(new ChatCommandSuggestor.SuggestionEntry(modexx, null));
                           }
                        }
                     }
                  } else if (parts.length == 2) {
                     info.usageHint = "<gamemode> [<target>]";
                     if (info.suggestions.isEmpty()) {
                        List<String> targets = getTargetSuggestions();
                        boolean exactMatch = false;

                        for (String t : targets) {
                           if (t.equalsIgnoreCase(currentArg)) {
                              exactMatch = true;
                              break;
                           }
                        }

                        if (!exactMatch && !currentArg.isEmpty()) {
                           info.errorIndex = info.startOffset;

                           for (String tx : targets) {
                              if (tx.toLowerCase().startsWith(currentArg.toLowerCase())) {
                                 info.suggestions.add(new ChatCommandSuggestor.SuggestionEntry(tx, null));
                              }
                           }
                        } else {
                           info.isExactMatch = exactMatch;

                           for (String txx : targets) {
                              info.suggestions.add(new ChatCommandSuggestor.SuggestionEntry(txx, null));
                           }
                        }
                     }
                  } else if (parts.length > 2) {
                     info.errorIndex = info.startOffset;
                     info.errorMessage = "Incorrect argument for command at position " + text.length() + ": ... " + currentArg + "<--[HERE]";
                  }
               } else if (root.equals("difficulty")) {
                  info.usageHint = "<difficulty>";
                  String[] diffs = new String[]{"peaceful", "easy", "normal", "hard"};
                  boolean exactMatch = false;

                  for (String d : diffs) {
                     if (d.equalsIgnoreCase(currentArg)) {
                        exactMatch = true;
                        break;
                     }
                  }

                  if (!exactMatch && !currentArg.isEmpty()) {
                     info.errorIndex = info.startOffset;

                     for (String dx : diffs) {
                        if (dx.toLowerCase().startsWith(currentArg.toLowerCase())) {
                           info.suggestions.add(new ChatCommandSuggestor.SuggestionEntry(dx, null));
                        }
                     }
                  } else {
                     info.isExactMatch = exactMatch;
                     if (info.suggestions.isEmpty()) {
                        for (String dxx : diffs) {
                           info.suggestions.add(new ChatCommandSuggestor.SuggestionEntry(dxx, null));
                        }
                     }
                  }
               } else if (root.equals("weather")) {
                  info.usageHint = "<weather> [duration]";
                  String[] weathers = new String[]{"clear", "rain", "thunder"};
                  boolean exactMatch = false;

                  for (String w : weathers) {
                     if (w.equalsIgnoreCase(currentArg)) {
                        exactMatch = true;
                        break;
                     }
                  }

                  if (!exactMatch && !currentArg.isEmpty()) {
                     info.errorIndex = info.startOffset;

                     for (String wx : weathers) {
                        if (wx.toLowerCase().startsWith(currentArg.toLowerCase())) {
                           info.suggestions.add(new ChatCommandSuggestor.SuggestionEntry(wx, null));
                        }
                     }
                  } else {
                     info.isExactMatch = exactMatch;
                     if (info.suggestions.isEmpty()) {
                        for (String wxx : weathers) {
                           info.suggestions.add(new ChatCommandSuggestor.SuggestionEntry(wxx, null));
                        }
                     }
                  }
               } else if (root.equals("time")) {
                  info.usageHint = "<time>";
                  String[] times = new String[]{"add", "query", "set"};
                  boolean exactMatch = false;

                  for (String txx : times) {
                     if (txx.equalsIgnoreCase(currentArg)) {
                        exactMatch = true;
                        break;
                     }
                  }

                  if (!exactMatch && !currentArg.isEmpty()) {
                     info.errorIndex = info.startOffset;

                     for (String txxx : times) {
                        if (txxx.toLowerCase().startsWith(currentArg.toLowerCase())) {
                           info.suggestions.add(new ChatCommandSuggestor.SuggestionEntry(txxx, null));
                        }
                     }
                  } else {
                     info.isExactMatch = exactMatch;
                     if (info.suggestions.isEmpty()) {
                        for (String txxxx : times) {
                           info.suggestions.add(new ChatCommandSuggestor.SuggestionEntry(txxxx, null));
                        }
                     }
                  }
               } else if (root.equals("time set")) {
                  info.usageHint = "<time>";
                  String[] sets = new String[]{"day", "midnight", "night", "noon"};
                  boolean exactMatch = false;

                  for (String s : sets) {
                     if (s.equalsIgnoreCase(currentArg)) {
                        exactMatch = true;
                        break;
                     }
                  }

                  if (!exactMatch && !currentArg.isEmpty()) {
                     info.errorIndex = info.startOffset;

                     for (String sx : sets) {
                        if (sx.toLowerCase().startsWith(currentArg.toLowerCase())) {
                           info.suggestions.add(new ChatCommandSuggestor.SuggestionEntry(sx, null));
                        }
                     }
                  } else {
                     info.isExactMatch = exactMatch;
                     if (info.suggestions.isEmpty()) {
                        for (String sxx : sets) {
                           info.suggestions.add(new ChatCommandSuggestor.SuggestionEntry(sxx, null));
                        }
                     }
                  }
               } else if (root.equals("tp") || root.equals("teleport")) {
                  info.usageHint = "<destination>";
                  if (info.suggestions.isEmpty()) {
                     List<String> targets = getTargetSuggestions();
                     boolean exactMatch = false;

                     for (String txxxx : targets) {
                        if (txxxx.equalsIgnoreCase(currentArg)) {
                           exactMatch = true;
                           break;
                        }
                     }

                     if (exactMatch || currentArg.isEmpty()) {
                        info.isExactMatch = exactMatch;

                        for (String txxxxx : targets) {
                           info.suggestions.add(new ChatCommandSuggestor.SuggestionEntry(txxxxx, null));
                        }
                     } else {
                        info.errorIndex = info.startOffset;

                        for (String txxxxx : targets) {
                           if (txxxxx.toLowerCase().startsWith(currentArg.toLowerCase())) {
                              info.suggestions.add(new ChatCommandSuggestor.SuggestionEntry(txxxxx, null));
                           }
                        }
                     }
                  }
               } else if (root.equals("give")) {
                  info.usageHint = "<targets> <item> [count]";
               } else if (root.equals("kill")) {
                  info.usageHint = "[<targets>]";
                  if (info.suggestions.isEmpty()) {
                     List<String> targets = getTargetSuggestions();
                     boolean exactMatch = false;

                     for (String txxxxxx : targets) {
                        if (txxxxxx.equalsIgnoreCase(currentArg)) {
                           exactMatch = true;
                           break;
                        }
                     }

                     if (exactMatch || currentArg.isEmpty()) {
                        info.isExactMatch = exactMatch;

                        for (String txxxxxxx : targets) {
                           info.suggestions.add(new ChatCommandSuggestor.SuggestionEntry(txxxxxxx, null));
                        }
                     } else {
                        info.errorIndex = info.startOffset;

                        for (String txxxxxxx : targets) {
                           if (txxxxxxx.toLowerCase().startsWith(currentArg.toLowerCase())) {
                              info.suggestions.add(new ChatCommandSuggestor.SuggestionEntry(txxxxxxx, null));
                           }
                        }
                     }
                  }
               } else if (root.equals("clear")) {
                  info.usageHint = "[<targets>] [<item>] [<maxCount>]";
               } else if (!ROOT_COMMANDS.contains(root.toLowerCase())) {
                  info.errorIndex = 1;
               }
            }
         }

         cachedResult = info;
         return info;
      }
   }

   public static List<String> getTargetSuggestions() {
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
      List<String> filmReplays = ChatMorphHelper.getAllFilmReplayNames();
      if (replayName != null && !replayName.isEmpty()) {
         if (!targets.contains(replayName)) {
            targets.add(replayName);
         }
      } else if (morphName != null && !morphName.isEmpty()) {
         if (!targets.contains(morphName)) {
            targets.add(morphName);
         }
      } else if (filmReplays.isEmpty() && playerName != null && !targets.contains(playerName)) {
         targets.add(playerName);
      }

      for (String rName : filmReplays) {
         if ((replayName == null || replayName.isEmpty() || morphName == null || !rName.equalsIgnoreCase(morphName))
            && rName != null
            && !rName.isEmpty()
            && !targets.contains(rName)) {
            targets.add(rName);
         }
      }

      if (replayName != null && !replayName.isEmpty() && morphName != null && !morphName.equalsIgnoreCase(replayName)) {
         targets.removeIf(t -> t.equalsIgnoreCase(morphName));
      }

      return targets;
   }

   public static String renderCommandSuggestions(
      DrawContext context,
      TextRenderer font,
      String text,
      ChatCommandSuggestor.ParseResultInfo info,
      int textX,
      int anchorY,
      int screenWidth,
      float cursorX,
      float cursorY,
      boolean cursorVisible
   ) {
      if (info == null) {
         return null;
      } else if (info.suggestions.isEmpty() && info.errorMessage != null && !info.errorMessage.isEmpty()) {
         int errorWidth = font.getWidth(info.errorMessage);
         int errorY = anchorY - 14;
         int errorX = 2;
         context.fill(errorX, errorY, errorX + errorWidth + 8, errorY + 12, -805306368);
         context.drawTextWithShadow(font, info.errorMessage, errorX + 4, errorY + 2, -1);
         return null;
      } else {
         String prefixBeforeStart = text.substring(0, Math.min(info.startOffset, text.length()));
         int tokenStartX = textX + font.getWidth(prefixBeforeStart);
         if (info.suggestions.isEmpty()) {
            if (info.usageHint != null && !info.usageHint.isEmpty()) {
               int usageWidth = font.getWidth(info.usageHint) + 2;
               int usageX = Math.min(tokenStartX - 1, screenWidth - usageWidth - 2);
               usageX = Math.max(1, usageX);
               int usageY = anchorY - 12;
               context.fill(usageX, usageY, usageX + usageWidth, usageY + 12, -805306368);
               context.drawTextWithShadow(font, info.usageHint, usageX + 1, usageY + 2, -1);
            }

            return null;
         } else {
            String currentToken = text.substring(Math.min(info.startOffset, text.length()));
            int maxDisplay = Math.min(10, info.suggestions.size());
            int entryHeight = 12;
            int totalHeight = maxDisplay * entryHeight;
            int popupY = anchorY - totalHeight;
            int maxTextWidth = 0;

            for (int i = 0; i < maxDisplay; i++) {
               maxTextWidth = Math.max(maxTextWidth, font.getWidth(info.suggestions.get(i).text));
            }

            int popupWidth = maxTextWidth + 2;
            int popupX = Math.min(tokenStartX - 1, screenWidth - popupWidth - 2);
            popupX = Math.max(1, popupX);
            int hoveredIndex = -1;
            boolean isMouseOverPopup = cursorVisible
               && cursorX >= (float)popupX
               && cursorX <= (float)(popupX + popupWidth)
               && cursorY >= (float)popupY
               && cursorY < (float)(popupY + totalHeight);
            if (isMouseOverPopup) {
               hoveredIndex = (int)((cursorY - (float)popupY) / (float)entryHeight);
               if (hoveredIndex < 0 || hoveredIndex >= maxDisplay) {
                  hoveredIndex = -1;
               }
            }

            if (info.isExactMatch && !isMouseOverPopup) {
               if (info.usageHint != null && !info.usageHint.isEmpty()) {
                  int usageWidth = font.getWidth(info.usageHint) + 2;
                  int usageX = Math.min(tokenStartX - 1, screenWidth - usageWidth - 2);
                  usageX = Math.max(1, usageX);
                  int usageY = anchorY - 12;
                  context.fill(usageX, usageY, usageX + usageWidth, usageY + 12, -805306368);
                  context.drawTextWithShadow(font, info.usageHint, usageX + 1, usageY + 2, -1);
               }

               return null;
            } else {
               int selectedIndex = hoveredIndex;
               if (hoveredIndex == -1) {
                  if (!currentToken.isEmpty()) {
                     for (int i = 0; i < maxDisplay; i++) {
                        if (info.suggestions.get(i).text.equalsIgnoreCase(currentToken)
                           || info.suggestions.get(i).text.toLowerCase().startsWith(currentToken.toLowerCase())) {
                           selectedIndex = i;
                           break;
                        }
                     }
                  }

                  if (selectedIndex == -1) {
                     selectedIndex = 0;
                  }
               }

               ChatCommandSuggestor.SuggestionEntry selectedEntry = selectedIndex >= 0 && selectedIndex < info.suggestions.size()
                  ? info.suggestions.get(selectedIndex)
                  : null;
               String tooltip = selectedEntry != null ? selectedEntry.tooltip : null;
               if (tooltip != null && !tooltip.isEmpty()) {
                  int tooltipWidth = font.getWidth(tooltip) + 2;
                  int tooltipY = popupY - 12;
                  context.fill(popupX, tooltipY, popupX + tooltipWidth, tooltipY + 12, -805306368);
                  context.drawTextWithShadow(font, tooltip, popupX + 1, tooltipY + 2, -5592406);
               }

               context.fill(popupX, popupY, popupX + popupWidth, popupY + totalHeight, -805306368);

               for (int ix = 0; ix < maxDisplay; ix++) {
                  ChatCommandSuggestor.SuggestionEntry s = info.suggestions.get(ix);
                  int rowY = popupY + ix * entryHeight;
                  boolean isSelected = ix == selectedIndex;
                  int textColor = isSelected ? -171 : -5592406;
                  context.drawTextWithShadow(font, s.text, popupX + 1, rowY + 2, textColor);
               }

               if (selectedEntry != null) {
                  if (currentToken.isEmpty()) {
                     return selectedEntry.text;
                  }

                  if (selectedEntry.text.toLowerCase().startsWith(currentToken.toLowerCase())) {
                     return selectedEntry.text.substring(currentToken.length());
                  }
               }

               return null;
            }
         }
      }
   }

   public static void renderColoredCommandText(
      DrawContext context, TextRenderer font, String text, int startX, int y, String ghostPreview, ChatCommandSuggestor.ParseResultInfo info
   ) {
      if (text.isEmpty()) {
         if (ghostPreview != null && !ghostPreview.isEmpty()) {
            context.drawTextWithShadow(font, ghostPreview, startX, y, -8355712);
         }
      } else if (!text.startsWith("/")) {
         context.drawTextWithShadow(font, text, startX, y, -1);
      } else {
         int errorIdx = info != null ? info.errorIndex : -1;
         context.drawTextWithShadow(font, "/", startX, y, -2039584);
         int currentX = startX + font.getWidth("/");
         String rest = text.substring(1);
         String[] tokens = rest.split(" ", -1);
         int charPos = 1;

         for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            int tokenLen = token.length();
            int color;
            if (errorIdx >= 0 && charPos >= errorIdx) {
               color = -43691;
            } else if (i == 0) {
               color = -2039584;
            } else {
               color = ARG_COLORS[(i - 1) % ARG_COLORS.length];
            }

            if (!token.isEmpty()) {
               context.drawTextWithShadow(font, token, currentX, y, color);
               currentX += font.getWidth(token);
            }

            charPos += tokenLen;
            if (i < tokens.length - 1) {
               context.drawTextWithShadow(font, " ", currentX, y, -1);
               currentX += font.getWidth(" ");
               charPos++;
            }
         }

         if (ghostPreview != null && !ghostPreview.isEmpty()) {
            context.drawTextWithShadow(font, ghostPreview, currentX, y, -8355712);
         }
      }
   }

   public static class ParseResultInfo {
      public int startOffset = 0;
      public final List<ChatCommandSuggestor.SuggestionEntry> suggestions = new ArrayList<>();
      public String errorMessage = null;
      public int errorIndex = -1;
      public boolean isExactMatch = false;
      public String usageHint = null;
   }

   public static class SuggestionEntry {
      public final String text;
      public final String tooltip;

      public SuggestionEntry(String text, String tooltip) {
         this.text = text;
         this.tooltip = tooltip;
      }
   }
}

package Glaxium.POV.actions.gui.data;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

public final class MerchantSnapshot {
   private static final String[] PROFESSIONS = new String[]{
      "None",
      "Armorer",
      "Butcher",
      "Cartographer",
      "Cleric",
      "Farmer",
      "Fisherman",
      "Fletcher",
      "Leatherworker",
      "Librarian",
      "Mason",
      "Nitwit",
      "Shepherd",
      "Toolsmith",
      "Weaponsmith"
   };
   private static final String[] LEVELS = new String[]{"Novice", "Apprentice", "Journeyman", "Expert", "Master"};
   public final String offers;
   public final int profession;
   public final int level;
   public final int experience;
   public final int selectedOffer;
   public final int scrollOffset;
   public final String title;
   public final boolean canLevel;

   public MerchantSnapshot(String offers, int profession, int level, int experience, int selectedOffer, int scrollOffset, String title, boolean canLevel) {
      this.offers = offers == null ? "" : offers;
      this.profession = Math.max(0, Math.min(PROFESSIONS.length - 1, profession));
      this.level = Math.max(1, Math.min(5, level));
      this.experience = Math.max(0, experience);
      this.selectedOffer = Math.max(0, selectedOffer);
      this.scrollOffset = Math.max(0, scrollOffset);
      this.title = title == null ? "" : title;
      this.canLevel = canLevel;
   }

   public static MerchantSnapshot fromOffers(
      TradeOfferList tradeOffers,
      int merchantProfession,
      int merchantLevel,
      int merchantXp,
      int selectedIndex,
      int scrollIndex,
      String merchantTitle,
      boolean isLeveledMerchant
   ) {
      String packed = pack(tradeOffers);
      int profession = Math.max(0, Math.min(PROFESSIONS.length - 1, merchantProfession));
      int level = Math.max(1, Math.min(5, merchantLevel));
      String title = merchantTitle != null && !merchantTitle.isEmpty() ? merchantTitle : getDefaultTitle(profession, level, isLeveledMerchant);
      return new MerchantSnapshot(packed, profession, level, merchantXp, selectedIndex, scrollIndex, title, isLeveledMerchant);
   }

   public static String getProfessionName(int profIndex) {
      return profIndex >= 0 && profIndex < PROFESSIONS.length ? PROFESSIONS[profIndex] : "Villager";
   }

   public static String getLevelName(int lvl) {
      int idx = Math.max(1, Math.min(5, lvl)) - 1;
      return LEVELS[idx];
   }

   public static String formatTitle(String baseTitle, int profIndex, int lvl, boolean isLeveled) {
      if (baseTitle != null && baseTitle.contains(" - ")) {
         return baseTitle;
      } else {
         String name = baseTitle != null && !baseTitle.isEmpty() && !"Merchant".equalsIgnoreCase(baseTitle) ? baseTitle : getProfessionName(profIndex);
         return isLeveled && !"None".equals(name) && !"Nitwit".equals(name) && !"Villager".equals(name) && !"Wandering Trader".equals(name)
            ? name + " - " + getLevelName(lvl)
            : name;
      }
   }

   public static String getDefaultTitle(int profIndex, int lvl, boolean isLeveled) {
      return formatTitle("", profIndex, lvl, isLeveled);
   }

   public static String pack(TradeOfferList tradeOffers) {
      if (tradeOffers != null && !tradeOffers.isEmpty()) {
         StringBuilder sb = new StringBuilder();

         for (int i = 0; i < tradeOffers.size(); i++) {
            if (i > 0) {
               sb.append(';');
            }

            TradeOffer offer = (TradeOffer)tradeOffers.get(i);
            packStack(sb, offer.getOriginalFirstBuyItem());
            sb.append('|');
            packStack(sb, offer.getSecondBuyItem());
            sb.append('|');
            packStack(sb, offer.getSellItem());
            sb.append('|').append(offer.getUses());
            sb.append('|').append(offer.getMaxUses());
            sb.append('|').append(offer.getSpecialPrice());
            sb.append('|').append(offer.isDisabled() ? 1 : 0);
         }

         return sb.toString();
      } else {
         return "";
      }
   }

   private static void packStack(StringBuilder sb, ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         Identifier id = Registries.ITEM.getId(stack.getItem());
         sb.append(id != null ? id.toString() : "minecraft:air").append('*').append(stack.getCount());
      } else {
         sb.append("empty");
      }
   }

   public static List<MerchantSnapshot.ParsedOffer> parse(String packed) {
      List<MerchantSnapshot.ParsedOffer> list = new ArrayList<>();
      if (packed != null && !packed.isEmpty()) {
         String[] rows = packed.split(";", -1);

         for (String row : rows) {
            if (!row.isEmpty()) {
               String[] parts = row.split("\\|", -1);
               if (parts.length >= 7) {
                  ItemStack buy1 = parseStack(parts[0]);
                  ItemStack buy2 = parseStack(parts[1]);
                  ItemStack sell = parseStack(parts[2]);
                  int uses = parseInt(parts[3], 0);
                  int maxUses = parseInt(parts[4], 12);
                  int specialPrice = parseInt(parts[5], 0);
                  boolean disabled = parseInt(parts[6], 0) == 1 || uses >= maxUses;
                  list.add(new MerchantSnapshot.ParsedOffer(buy1, buy2, sell, uses, maxUses, specialPrice, disabled));
               }
            }
         }

         return list;
      } else {
         return list;
      }
   }

   private static ItemStack parseStack(String part) {
      if (part != null && !part.isEmpty() && !"empty".equals(part)) {
         int star = part.indexOf(42);
         String itemId = star >= 0 ? part.substring(0, star) : part;
         int count = star >= 0 ? parseInt(part.substring(star + 1), 1) : 1;

         try {
            Identifier id = Identifier.tryParse(itemId);
            if (id != null && Registries.ITEM.containsId(id)) {
               Item item = (Item)Registries.ITEM.get(id);
               return new ItemStack(item, Math.max(1, count));
            }
         } catch (Exception var6) {
         }

         return ItemStack.EMPTY;
      } else {
         return ItemStack.EMPTY;
      }
   }

   private static int parseInt(String str, int fallback) {
      if (str != null && !str.isEmpty()) {
         try {
            return Integer.parseInt(str.trim());
         } catch (NumberFormatException var3) {
            return fallback;
         }
      } else {
         return fallback;
      }
   }

   public static record ParsedOffer(ItemStack buy1, ItemStack buy2, ItemStack sell, int uses, int maxUses, int specialPrice, boolean disabled) {
   }
}

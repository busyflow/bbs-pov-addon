package Glaxium.POV.hud;

import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.item.ItemStack;

public class HudState {
   public static final int HEART_NORMAL = 0;
   public static final int HEART_POISONED = 1;
   public static final int HEART_WITHERED = 2;
   public static final int HEART_ABSORBING = 3;
   public static final int HEART_FROZEN = 4;
   public final ItemStack[] items = new ItemStack[9];
   public boolean visible = true;
   public boolean statusBarsVisible = true;
   public boolean crosshair;
   public boolean cursorVisible = false;
   public final Transform cursorLayout = new Transform();
   public ItemStack cursorItem = ItemStack.EMPTY;
   public ItemStack offhandItem = ItemStack.EMPTY;
   public int selectedSlot;
   public int heartType;
   public boolean hardcore;
   public boolean heartRegeneration;
   public boolean hungerEffect;
   public float health;
   public float previousHealth;
   public float lastHealth;
   public float recentHealthLow;
   public float recentHealthHigh;
   public float healthContainer;
   public float absorption;
   public float recentAbsorptionLow;
   public float recentAbsorptionHigh;
   public float absorptionContainer;
   public float armor;
   public float hunger;
   public float mountHealth;
   public float mountHealthContainer;
   public float air;
   public float experience;
   public int experienceLevel;
   public boolean heartFlash;
   public boolean absorptionFlash;
   public final Transform layout = new Transform();
   public float healthFlashAge;
   public float alpha;
   public int renderOrder;
}

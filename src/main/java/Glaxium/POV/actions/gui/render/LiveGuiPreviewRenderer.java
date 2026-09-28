package Glaxium.POV.actions.gui.render;

import Glaxium.POV.actions.bossbar.render.BossBarActionRenderer;
import Glaxium.POV.actions.chat.render.ChatActionRenderer;
import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.GuiRecipeBook;
import Glaxium.POV.actions.gui.data.GuiCapture;
import Glaxium.POV.actions.gui.data.GuiSnapshot;
import Glaxium.POV.actions.gui.recording.GuiSnapshotCapture;
import Glaxium.POV.actions.menu.render.MenuActionRenderer;
import Glaxium.POV.actions.menu.schema.MenuTypeResolver;
import Glaxium.POV.hud.HudMount;
import Glaxium.POV.hud.HudState;
import Glaxium.POV.hud.render.HeldItemTooltipRenderer;
import Glaxium.POV.hud.render.HudRenderer;
import Glaxium.POV.render.PovCrosshairRenderer;
import Glaxium.POV.render.PovCursorRenderer;
import Glaxium.POV.render.PovViewportMetrics;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import java.util.Map.Entry;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SleepingChatScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.joml.Matrix4f;

public final class LiveGuiPreviewRenderer {
   private static final GuiPovActionClip LIVE_CLIP = new GuiPovActionClip();
   private static final Transform LIVE_CURSOR_TRANSFORM = new Transform();
   private static boolean isRendering = false;

   public static boolean isRenderingLive() {
      return isRendering;
   }

   private LiveGuiPreviewRenderer() {
   }

   private static <T> void setChannel(KeyframeChannel<T> channel, T value) {
      if (channel != null) {
         if (channel.isEmpty()) {
            channel.insert(0.0F, value);
         } else {
            ((Keyframe)channel.getKeyframes().get(0)).setValue(value);
         }
      }
   }

   public static void render(Batcher2D batcher, float tickDelta) {
      MinecraftClient client = MinecraftClient.getInstance();
      Screen screen = client.currentScreen;
      ClientPlayerEntity player = client.player;
      if (player != null) {
         String menuType = MenuTypeResolver.resolveLive(screen, player);
         boolean menuOpen = menuType != null;
         boolean sleepFade = "sleep".equals(menuType) && !(screen instanceof SleepingChatScreen);
         boolean showMenuUi = menuOpen && !sleepFade;
         int screenWidth = PovViewportMetrics.getMinecraftScaledWidth();
         int screenHeight = PovViewportMetrics.getMinecraftScaledHeight();
         GuiCapture captured = !menuOpen && screen != null ? GuiSnapshotCapture.capture(screen, screenWidth, screenHeight) : null;
         GuiSnapshot snapshot = captured == null ? null : captured.snapshot;
         String guiType = snapshot == null ? "" : snapshot.guiType;
         boolean isChatScreen = screen instanceof ChatScreen;
         if (captured != null) {
            LIVE_CLIP.tick.set(0);
            LIVE_CLIP.duration.set(100);
            setChannel(LIVE_CLIP.state, guiType);
            setChannel(LIVE_CLIP.getCursorVisible(guiType), snapshot.cursorVisible);
            setChannel(LIVE_CLIP.getOpacity(guiType), 1.0F);
            setChannel(LIVE_CLIP.getDarknessOpacity(guiType), "gamemode_switcher".equals(guiType) ? 0.0F : 1.0F);
            LIVE_CURSOR_TRANSFORM.translate.set(snapshot.cursorTx, snapshot.cursorTy, 0.0F);
            setChannel(LIVE_CLIP.getCursorLayout(guiType), LIVE_CURSOR_TRANSFORM);
            applyCapturedExtras(captured, guiType);
         } else if (showMenuUi || isChatScreen) {
            updateLiveCursorFromMouse(screenWidth, screenHeight);
         }

         Matrix4f previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
         Matrix4f screenProjection = new Matrix4f().ortho(0.0F, (float)screenWidth, (float)screenHeight, 0.0F, -1000.0F, 3000.0F);
         RenderSystem.setProjectionMatrix(screenProjection, VertexSorter.BY_Z);
         isRendering = true;

         try {
            HudState liveHotbar = createLiveHudState(player, snapshot);
            if (sleepFade && liveHotbar != null) {
               liveHotbar.cursorVisible = false;
            }

            boolean hideHud = "sleep".equals(menuType) && !sleepFade;
            if (liveHotbar != null && !hideHud) {
               HudRenderer.renderHotbar(batcher.getContext().getMatrices(), batcher, liveHotbar, 0, 0, screenWidth, screenHeight);
               batcher.flush();
               if (captured == null && !isChatScreen) {
                  HeldItemTooltipRenderer.renderLive(batcher, liveHotbar, screenWidth, screenHeight);
               }
            }

            if (captured != null) {
               GuiActionRenderer.renderGuiClip(batcher.getContext().getMatrices(), batcher, null, LIVE_CLIP, null, 0.0F, screenWidth, screenHeight);
            }

            BossBarActionRenderer.renderLive(batcher, screenWidth, screenHeight);
            if (menuOpen) {
               MenuActionRenderer.renderLive(batcher, screenWidth, screenHeight);
            }

            if (isChatScreen) {
               ChatActionRenderer.renderLiveChat(batcher, (ChatScreen)screen, screenWidth, screenHeight);
            }

            if (liveHotbar == null || captured == null && !showMenuUi && !isChatScreen) {
               if (sleepFade) {
                  PovCrosshairRenderer.render(batcher, screenWidth, screenHeight);
               }
            } else {
               PovCursorRenderer.render(batcher, liveHotbar, screenWidth, screenHeight);
            }
         } finally {
            isRendering = false;
            RenderSystem.setProjectionMatrix(previousProjection, VertexSorter.BY_DISTANCE);
         }
      }
   }

   private static HudState createLiveHudState(ClientPlayerEntity player, GuiSnapshot snapshot) {
      if (player == null) {
         return null;
      } else {
         HudState state = new HudState();
         state.visible = true;
         state.alpha = 1.0F;
         state.selectedSlot = player.getInventory().selectedSlot;
         state.offhandItem = player.getOffHandStack();

         for (int i = 0; i < 9; i++) {
            state.items[i] = player.getInventory().getStack(i);
         }

         boolean survivalLike = !player.isCreative() && !player.isSpectator();
         state.statusBarsVisible = survivalLike;
         if (survivalLike) {
            state.health = player.getHealth();
            state.healthContainer = player.getMaxHealth();
            state.previousHealth = state.health;
            state.lastHealth = state.health;
            state.recentHealthLow = state.health;
            state.recentHealthHigh = state.health;
            state.absorption = player.getAbsorptionAmount();
            state.absorptionContainer = state.absorption;
            state.armor = (float)player.getArmor();
            state.hunger = (float)player.getHungerManager().getFoodLevel();
            LivingEntity mount = HudMount.jumpingMount(player);
            int mountSlots = HudMount.heartSlots(mount);
            state.mountHealthContainer = (float)(mountSlots * 2);
            state.mountHealth = mount == null ? 0.0F : mount.getHealth();
            state.air = (float)player.getAir();
            state.experience = player.experienceProgress;
            state.experienceLevel = player.experienceLevel;
            state.hardcore = player.getWorld().getLevelProperties().isHardcore();
         }

         state.cursorVisible = true;
         state.cursorLayout.copy(LIVE_CURSOR_TRANSFORM);
         state.cursorItem = snapshot == null ? ItemStack.EMPTY : snapshot.cursorItem;
         return state;
      }
   }

   private static void updateLiveCursorFromMouse(int screenWidth, int screenHeight) {
      MinecraftClient client = MinecraftClient.getInstance();
      double mouseX = client.mouse.getX() * (double)client.getWindow().getScaledWidth() / (double)client.getWindow().getWidth();
      double mouseY = client.mouse.getY() * (double)client.getWindow().getScaledHeight() / (double)client.getWindow().getHeight();
      LIVE_CURSOR_TRANSFORM.translate.set((float)((mouseX - (double)screenWidth / 2.0) / 2.0), (float)(((double)screenHeight / 2.0 - mouseY) / 2.0), 0.0F);
   }

   private static void applyCapturedExtras(GuiCapture captured, String guiType) {
      GuiSnapshot snapshot = captured.snapshot;

      for (Entry<String, ItemStack> slot : snapshot.slots.entrySet()) {
         setChannel(LIVE_CLIP.getGuiSlot(guiType, slot.getKey()), slot.getValue());
      }

      setChannel(LIVE_CLIP.getDragSlots(guiType), snapshot.dragging ? snapshot.dragEncoded : "");
      setChannel(LIVE_CLIP.getCursorItem(guiType), snapshot.cursorItem);
      if (captured.creative != null) {
         setChannel(LIVE_CLIP.creativeTab, captured.creative.tab);
         setChannel(LIVE_CLIP.creativePage, captured.creative.page);
         setChannel(LIVE_CLIP.creativeRow, captured.creative.row);
         setChannel(LIVE_CLIP.creativeScroll, captured.creative.scroll);
         setChannel(LIVE_CLIP.creativeSearch, captured.creative.search);
         setChannel(LIVE_CLIP.creativeSearchFocus, captured.creative.searchFocused);
         setChannel(LIVE_CLIP.creativeSearchSelStart, captured.creative.searchSelStart);
         setChannel(LIVE_CLIP.creativeSearchSelEnd, captured.creative.searchSelEnd);
      }

      if (captured.loom != null) {
         setChannel(LIVE_CLIP.loomRow, captured.loom.row);
      }

      if (captured.stonecutter != null) {
         setChannel(LIVE_CLIP.stonecutterRow, captured.stonecutter.row);
      }

      if (captured.anvil != null) {
         setChannel(LIVE_CLIP.anvilName, captured.anvil.name);
         setChannel(LIVE_CLIP.anvilNameFocus, captured.anvil.focused);
         setChannel(LIVE_CLIP.anvilNameSelStart, captured.anvil.selStart);
         setChannel(LIVE_CLIP.anvilNameSelEnd, captured.anvil.selEnd);
         setChannel(LIVE_CLIP.anvilError, captured.anvil.error);
      }

      if (captured.enchantment != null) {
         setChannel(LIVE_CLIP.enchantOffers, captured.enchantment.offers);
         setChannel(LIVE_CLIP.enchantSeed, captured.enchantment.seed);
         setChannel(LIVE_CLIP.enchantPlayerLevel, captured.enchantment.playerLevel);
         setChannel(LIVE_CLIP.enchantCreative, captured.enchantment.creative);
         setChannel(LIVE_CLIP.enchantBookOpen, captured.enchantment.bookOpen);
      }

      if (captured.beacon != null) {
         setChannel(LIVE_CLIP.beaconLevel, captured.beacon.level);
         setChannel(LIVE_CLIP.beaconPrimary, captured.beacon.primary);
         setChannel(LIVE_CLIP.beaconSecondary, captured.beacon.secondary);
      }

      if (captured.furnace != null) {
         setChannel(LIVE_CLIP.getFurnaceLit(guiType), captured.furnace.lit);
         setChannel(LIVE_CLIP.getFurnaceCook(guiType), captured.furnace.cook);
      }

      if (captured.mount != null) {
         if ("horse".equals(guiType)) {
            setChannel(LIVE_CLIP.getHorseVariant(guiType), captured.mount.horseVariant);
         }

         if ("donkey".equals(guiType)) {
            setChannel(LIVE_CLIP.getMountChest(guiType), captured.mount.chestOpen);
         }
      }

      if (captured.brewing != null) {
         setChannel(LIVE_CLIP.getBrewProgress(guiType), captured.brewing.progress);
         setChannel(LIVE_CLIP.getBrewFuel(guiType), captured.brewing.fuel);
         setChannel(LIVE_CLIP.getBrewBubbles(guiType), captured.brewing.bubbles);
      }

      if (captured.merchant != null) {
         setChannel(LIVE_CLIP.getMerchantOffers(guiType), captured.merchant.offers);
         setChannel(LIVE_CLIP.getMerchantProfession(guiType), captured.merchant.profession);
         setChannel(LIVE_CLIP.getMerchantLevel(guiType), captured.merchant.level);
         setChannel(LIVE_CLIP.getMerchantExperience(guiType), captured.merchant.experience);
         setChannel(LIVE_CLIP.getMerchantSelectedOffer(guiType), captured.merchant.selectedOffer);
         setChannel(LIVE_CLIP.getMerchantScrollOffset(guiType), captured.merchant.scrollOffset);
         setChannel(LIVE_CLIP.getMerchantTitle(guiType), captured.merchant.title);
         setChannel(LIVE_CLIP.getMerchantCanLevel(guiType), captured.merchant.canLevel);
      }

      if (captured.recipeBook != null) {
         setChannel(LIVE_CLIP.getRecipeOpen(guiType), captured.recipeBook.open);
         setChannel(LIVE_CLIP.getRecipeSearch(guiType), captured.recipeBook.search);
         setChannel(LIVE_CLIP.getRecipeSearchFocus(guiType), captured.recipeBook.searchFocused);
         setChannel(LIVE_CLIP.getRecipeSearchSelStart(guiType), captured.recipeBook.searchSelStart);
         setChannel(LIVE_CLIP.getRecipeSearchSelEnd(guiType), captured.recipeBook.searchSelEnd);
         setChannel(LIVE_CLIP.getRecipeShowing(guiType), captured.recipeBook.showing);
         setChannel(LIVE_CLIP.getRecipePage(guiType), captured.recipeBook.page);
         setChannel(LIVE_CLIP.getRecipeButton(guiType), captured.recipeBook.buttonSelected);
         setChannel(LIVE_CLIP.getRecipeSelected(guiType), captured.recipeBook.selected);
         if (GuiRecipeBook.hasCategories(guiType)) {
            setChannel(LIVE_CLIP.getRecipeCategory(guiType), captured.recipeBook.category);
         }
      }

      if (captured.book != null) {
         setChannel(LIVE_CLIP.getBookWritable(guiType), captured.book.writable);
         setChannel(LIVE_CLIP.getBookSigning(guiType), captured.book.signing);
         setChannel(LIVE_CLIP.getBookPage(guiType), captured.book.page);
         setChannel(LIVE_CLIP.getBookPages(guiType), captured.book.pages);
         setChannel(LIVE_CLIP.getBookTitle(guiType), captured.book.title);
         setChannel(LIVE_CLIP.getBookAuthor(guiType), captured.book.author);
         setChannel(LIVE_CLIP.getBookSelStart(guiType), captured.book.selStart);
         setChannel(LIVE_CLIP.getBookSelEnd(guiType), captured.book.selEnd);
      }

      if (captured.gamemode != null) {
         setChannel(LIVE_CLIP.gamemodeSelection, captured.gamemode.selection);
      }
   }
}

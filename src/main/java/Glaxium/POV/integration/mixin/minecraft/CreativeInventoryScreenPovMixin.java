package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.actions.gui.recording.GuiSnapshotCapture;
import net.fabricmc.fabric.impl.client.itemgroup.CreativeGuiExtensions;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventoryScreenPovMixin
{
    @Shadow private static ItemGroup selectedTab;
    @Shadow private float scrollPosition;
    @Shadow private TextFieldWidget searchBox;

    @Inject(method = "handledScreenTick", at = @At("HEAD"))
    private void bbsPov$captureCreativeState(CallbackInfo info)
    {
        TextFieldWidget box = this.searchBox;
        int selStart = 0;
        int selEnd = 0;
        if (box != null)
        {
            TextFieldWidgetPovAccessor access = (TextFieldWidgetPovAccessor) (Object) box;
            selStart = access.bbsPov$getSelectionStart();
            selEnd = access.bbsPov$getSelectionEnd();
        }
        int currentPage = 0;
        if (Glaxium.POV.actions.gui.CreativeInventoryTabs.isForge())
        {
            try
            {
                java.lang.reflect.Field f = net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen.class.getDeclaredField("selectedPage");
                f.setAccessible(true);
                currentPage = f.getInt(this);
            }
            catch (Throwable t1)
            {
                try
                {
                    java.lang.reflect.Method m = net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen.class.getDeclaredMethod("getSelectedPage");
                    m.setAccessible(true);
                    currentPage = (Integer) m.invoke(this);
                }
                catch (Throwable t2)
                {
                    try
                    {
                        currentPage = ((CreativeGuiExtensions) (Object) this).fabric_currentPage();
                    }
                    catch (Throwable ignored)
                    {
                    }
                }
            }
        }
        else
        {
            try
            {
                currentPage = ((CreativeGuiExtensions) (Object) this).fabric_currentPage();
            }
            catch (Throwable ignored)
            {
            }
        }

        GuiSnapshotCapture.updateCreative(
            selectedTab,
            this.scrollPosition,
            box == null ? "" : box.getText(),
            currentPage,
            box != null && box.isVisible() && box.isFocused(),
            selStart,
            selEnd);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void bbsPov$captureOnInit(CallbackInfo info)
    {
        this.bbsPov$captureCreativeState(info);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void bbsPov$captureOnRender(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo info)
    {
        this.bbsPov$captureCreativeState(info);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void bbsPov$resetOnClose(CallbackInfo info)
    {
        GuiSnapshotCapture.resetCreative();
    }
}

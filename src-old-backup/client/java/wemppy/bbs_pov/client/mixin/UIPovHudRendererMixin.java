package wemppy.bbs_pov.client.mixin;

import mchorse.bbs_mod.camera.clips.CameraClipContext;
import mchorse.bbs_mod.camera.clips.overwrite.POVClip;
import mchorse.bbs_mod.camera.clips.overwrite.PovHudData;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.ui.film.UIPovHudRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.clips.ClipContext;
import wemppy.bbs_pov.client.duck.IPovHardcore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.List;

@Mixin(UIPovHudRenderer.class)
public class UIPovHudRendererMixin
{
    @Unique
    private static final Identifier BBS_POV$ICONS = new Identifier("minecraft", "textures/gui/icons.png");

    @Unique
    private static boolean bbs_pov$isHardcore = false;

    @Inject(method = "renderHealth", at = @At("HEAD"))
    private static void onBeforeRenderHealth(DrawContext drawContext, int scaledWidth, int scaledHeight, POVClip clip, PovHudData data, IEntity entity, MinecraftClient mc, CallbackInfo ci)
    {
        bbs_pov$isHardcore = (clip instanceof IPovHardcore hc) && hc.bbs_pov$getHardcoreLook().get();
    }

    @ModifyArg(method = "renderHealth", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIII)V"), index = 4)
    private static int modifyHeartV(int originalV)
    {
        if (bbs_pov$isHardcore && originalV == 0)
        {
            return 45;
        }
        return originalV;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private static void onAfterRender(MatrixStack stack, Batcher2D batcher, ClipContext context, CallbackInfo ci)
    {
        List<PovHudData> list = POVClip.getPovHuds(context);
        if (list == null || list.isEmpty())
        {
            return;
        }

        PovHudData data = list.get(list.size() - 1);
        POVClip clip = data.clip;
        if (!clip.enabled.get() || !clip.showHud.get() || data.factor <= 0F)
        {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();
        DrawContext drawContext = batcher.getContext();

        IEntity entity = null;
        if (context instanceof CameraClipContext cameraContext)
        {
            entity = cameraContext.entities.get(data.replay.getId());
        }

        LivingEntity living = null;
        if (entity instanceof MCEntity mcEnt && mcEnt.getMcEntity() instanceof LivingEntity liv)
        {
            living = liv;
        }
        else if (mc.player != null)
        {
            living = mc.player;
        }

        if (living == null)
        {
            return;
        }

        // 1. Underwater oxygen bubbles
        int air = living.getAir();
        int maxAir = living.getMaxAir();
        if (air < maxAir)
        {
            int bubbleX = width / 2 + 91;
            int bubbleY = height - 49;
            int bubbles = MathHelper.ceil((float) (air - 2) * 10.0F / (float) maxAir);
            int fullBubbles = MathHelper.ceil((float) air * 10.0F / (float) maxAir) - bubbles;

            for (int i = 0; i < bubbles + fullBubbles; i++)
            {
                if (i < bubbles)
                {
                    drawContext.drawTexture(BBS_POV$ICONS, bubbleX - i * 8 - 9, bubbleY, 16, 18, 9, 9);
                }
                else
                {
                    drawContext.drawTexture(BBS_POV$ICONS, bubbleX - i * 8 - 9, bubbleY, 25, 18, 9, 9);
                }
            }
        }

        // 2. Status effect icons in top-right
        Collection<StatusEffectInstance> effects = living.getStatusEffects();
        if (!effects.isEmpty())
        {
            int effX = width - 24;
            int effY = 12;
            for (StatusEffectInstance effect : effects)
            {
                StatusEffect type = effect.getEffectType();
                Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(type);
                drawContext.drawSprite(effX, effY, 0, 18, 18, sprite);
                effX -= 22;
            }
        }
    }
}

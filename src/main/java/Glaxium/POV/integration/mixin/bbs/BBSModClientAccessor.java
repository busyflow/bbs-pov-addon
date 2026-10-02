package Glaxium.POV.integration.mixin.bbs;

import mchorse.bbs_mod.BBSModClient;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = BBSModClient.class, remap = false)
public interface BBSModClientAccessor
{
    @Accessor("keyPlayFilm")
    static KeyBinding bbsPov$getKeyPlayFilm()
    {
        throw new AssertionError();
    }

    @Accessor("keyRecordVideo")
    static KeyBinding bbsPov$getKeyRecordVideo()
    {
        throw new AssertionError();
    }
}

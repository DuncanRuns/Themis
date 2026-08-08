package me.duncanruns.themis.mixin;

import me.duncanruns.themis.ThemisMod;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
    @Inject(method = "<init>(Z)V", at = @At("RETURN"))
    private void onTitle(boolean b, CallbackInfo ci) {
        if (ThemisMod.temporaryConfigLoaded) ThemisMod.tryLoadConfig();
    }
}

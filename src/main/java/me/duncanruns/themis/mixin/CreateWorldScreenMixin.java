package me.duncanruns.themis.mixin;

import me.duncanruns.themis.ThemisMod;
import me.duncanruns.themis.util.PastebinUtil;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.MoreOptionsDialog;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

/**
 * Holy crap secret feature!
 */
@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {
    @Shadow
    @Final
    public MoreOptionsDialog moreOptionsDialog;

    @Inject(method = "createLevel", at = @At("HEAD"))
    private void onCreateLevel(CallbackInfo ci) throws IOException {
        //noinspection ConstantValue
        if (((Object) this).getClass() != CreateWorldScreen.class) {
            return;
        }

        MoreOptionsDialogAccessor modAccessor = (MoreOptionsDialogAccessor) moreOptionsDialog;
        String input = modAccessor.getSeedText();
        if (!input.contains(";tipb:")) return;
        int tipbIndex = input.indexOf(";tipb:");

        String actualSeed = input.substring(0, tipbIndex);
        modAccessor.setSeedText(actualSeed);
        modAccessor.getSeedTextField().setText(actualSeed);

        String toImport = input.substring(tipbIndex + ";tipb:".length());
        String configContents = PastebinUtil.getPastebinContents(PastebinUtil.clean(toImport));
        ThemisMod.loadConfigString(configContents);
    }
}

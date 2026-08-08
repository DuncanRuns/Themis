package me.duncanruns.themis.mixin;

import net.minecraft.client.gui.screen.world.MoreOptionsDialog;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MoreOptionsDialog.class)
public abstract class MoreOptionsDialogMixin {
    @Shadow
    private TextFieldWidget seedTextField;

    @Inject(method = "method_28092", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.seedTextField.setMaxLength(64);
    }

}

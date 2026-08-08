package me.duncanruns.themis.mixin;

import net.minecraft.client.gui.screen.world.MoreOptionsDialog;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MoreOptionsDialog.class)
public interface MoreOptionsDialogAccessor {
    @Accessor("seedText")
    String getSeedText();

    @Accessor("seedText")
    void setSeedText(String seedText);

    @Accessor("seedTextField")
    TextFieldWidget getSeedTextField();
}

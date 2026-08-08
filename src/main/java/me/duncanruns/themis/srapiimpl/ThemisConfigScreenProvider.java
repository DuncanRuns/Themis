package me.duncanruns.themis.srapiimpl;

import me.contaria.speedrunapi.config.api.SpeedrunConfigScreenProvider;
import me.duncanruns.themis.screen.ThemisConfigSelectionScreen;
import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class ThemisConfigScreenProvider implements SpeedrunConfigScreenProvider {
    @Override
    public @NotNull Screen createConfigScreen(Screen parent) {
        return new ThemisConfigSelectionScreen(parent);
    }
}

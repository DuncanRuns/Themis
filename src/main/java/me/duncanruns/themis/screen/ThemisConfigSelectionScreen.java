package me.duncanruns.themis.screen;

import me.duncanruns.themis.ThemisMod;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;

import java.io.IOException;
import java.util.Collection;

public class ThemisConfigSelectionScreen extends Screen {
    private ConfigListWidget configListWidget;
    private final Screen parent;

    public ThemisConfigSelectionScreen(Screen parent) {
        super(Text.method_30163("Themis Config Selection"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        Collection<String> names;
        String selectedFileName;
        try {
            names = ThemisMod.getAvailableConfigsNames();
            selectedFileName = ThemisMod.getSelectedFileName().orElse(null);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read themis config folder! " + e);
        }
        configListWidget = new ConfigListWidget(this.client, width, height, 45, this.height - 64, names, selectedFileName);
        addChild(configListWidget);
        addButton(new ButtonWidget(
                this.width / 2 - 153, this.height - 52, 150, 20,
                new LiteralText("Open Config Folder"),
                buttonWidget -> this.pressOpenConfigFolder()
        ));
        addButton(new ButtonWidget(
                this.width / 2 + 3, this.height - 52, 150, 20,
                new LiteralText("Import..."),
                buttonWidget -> this.pressImport()
        ));
        addButton(new ButtonWidget(
                this.width / 2 - 153, this.height - 28, 150, 20,
                ScreenTexts.DONE,
                buttonWidget -> this.done()
        ));
        // Refresh button
        this.addButton(new ButtonWidget(
                this.width / 2 + 3, this.height - 28, 72, 20,
                new TranslatableText("selectServer.refresh"),
                buttonWidget -> reopenScreen()
        ));

        // Cancel button
        this.addButton(new ButtonWidget(
                this.width / 2 + 3 + 72 + 6, this.height - 28, 72, 20,
                ScreenTexts.CANCEL,
                buttonWidget -> onClose()
        ));
    }

    private void pressOpenConfigFolder() {
        Util.getOperatingSystem().open(ThemisMod.CONFIG_DIR.toUri());
    }

    private void pressImport() {
        assert this.client != null;
        this.client.openScreen(new PastebinImportScreen(this));
    }

    public void reopenScreen() {
        assert this.client != null;
        ThemisConfigSelectionScreen out = new ThemisConfigSelectionScreen(this.parent);
        this.client.openScreen(out);
    }

    private void done() {
        ThemisMod.setSelectedFile(configListWidget.getSelectedFile());
        onClose();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        this.configListWidget.render(matrices, mouseX, mouseY, delta);
        if (this.configListWidget.children().isEmpty()) {
            this.drawCenteredString(matrices, this.textRenderer, "No configs found.", width / 2, (this.height - 19 - textRenderer.fontHeight) / 2, 0xFFFFFF);
        }
        super.render(matrices, mouseX, mouseY, delta);
        this.drawCenteredText(matrices, this.textRenderer, this.title, width / 2, 15, 0xFFFFFF);
        this.drawCenteredString(matrices, this.textRenderer, "Select the config file you want to use.", width / 2, 30, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        ThemisMod.tryLoadConfig();
        assert this.client != null;
        this.client.openScreen(this.parent);
    }
}

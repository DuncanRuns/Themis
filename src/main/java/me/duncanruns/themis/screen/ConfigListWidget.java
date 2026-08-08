package me.duncanruns.themis.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ConfigListWidget extends ElementListWidget<ConfigListWidget.FileEntry> {
    private static final Text CHECKMARK_TEXT = new LiteralText("✔");
    private static final Text EMPTY_TEXT = new LiteralText("");

    private final int rowWidth;

    private String selectedFile;

    public ConfigListWidget(MinecraftClient minecraftClient, int i, int j, int k, int l, Collection<String> fileNames, String selectedFile) {
        super(minecraftClient, i, j, k, l, 25);
        this.rowWidth = fileNames.stream().mapToInt(minecraftClient.textRenderer::getWidth).max().orElse(-25) + 25;
        this.selectedFile = selectedFile;
        for (String fileName : fileNames) {
            FileEntry entry = new FileEntry(fileName);
            if (Objects.equals(fileName, selectedFile)) {
                entry.button.setMessage(CHECKMARK_TEXT);
            }
            addEntry(entry);
        }
    }

    @Override
    public int getRowWidth() {
        return rowWidth;
    }

    public String getSelectedFile() {
        return selectedFile;
    }

    public class FileEntry extends Entry<FileEntry> {
        private final String fileName;
        private final AbstractButtonWidget button;

        FileEntry(String fileName) {
            this.fileName = fileName;
            this.button = new ButtonWidget(ConfigListWidget.this.rowWidth / 2, 0, 20, 20, EMPTY_TEXT, b -> {
                ConfigListWidget.this.children().forEach(e -> e.button.setMessage(EMPTY_TEXT));
                if (Objects.equals(selectedFile, this.fileName)) {
                    selectedFile = null;
                    return;
                }
                selectedFile = this.fileName;
                b.setMessage(CHECKMARK_TEXT);
            }) {
                @Override
                public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
                    // The button is also hovered if the entire entry is hovered
                    this.hovered |= ConfigListWidget.this.hoveredElement(mouseX, mouseY).orElse(null) == FileEntry.this;
                    super.renderButton(matrices, mouseX, mouseY, delta);
                }
            };
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            x = width / 2 - rowWidth / 2;
            button.y = y;
            button.x = x;
            button.render(matrices, mouseX, mouseY, tickDelta);
            int textY = 1 + y + (20 - client.textRenderer.fontHeight) / 2;
            client.textRenderer.drawWithShadow(matrices, fileName, x + 25, textY, 0xFFFFFF);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return this.button.mouseClicked(this.button.x, this.button.y, button);
        }

        @Override
        public List<? extends Element> children() {
            return Collections.singletonList(button);
        }
    }
}

package me.duncanruns.themis.screen;

import me.duncanruns.themis.ThemisMod;
import me.duncanruns.themis.mixin.MinecraftClientAccessor;
import me.duncanruns.themis.util.PastebinUtil;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public class PastebinImportScreen extends Screen {
    private static final Text TEXT = new LiteralText("Enter a Pastebin code or URL");
    private Text lowText = new LiteralText("");
    private int lowTextColor = 0xFF6666;
    private final ThemisConfigSelectionScreen parent;
    private TextFieldWidget textField;

    private ButtonWidget importButton;

    public PastebinImportScreen(ThemisConfigSelectionScreen parent) {
        super(new LiteralText("Import Themis Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.textField = addChild(new TextFieldWidget(
                this.textRenderer,
                width / 2 - 100, height / 2,
                200, 20,
                textField,
                new LiteralText("")
        ));

        importButton = addButton(new ButtonWidget(
                width / 2 - 100, height / 2 + 30,
                97, 20,
                new LiteralText("Import"),
                button -> onPressImport()
        ));
        importButton.active = false;

        addButton(new ButtonWidget(
                width / 2 + 3, height / 2 + 30,
                97, 20,
                ScreenTexts.CANCEL,
                button -> onClose()
        ));
    }

    private void onPressImport() {
        String id = PastebinUtil.clean(textField.getText());
        if (PastebinUtil.isIDInvalid(id)) return;

        lowText = new LiteralText("Retrieving...");
        lowTextColor = 0xAAAAAA;
        assert this.client != null;
        ((MinecraftClientAccessor) this.client).invokeRender(false);

        try {
            String title = PastebinUtil.getTitle(id).orElse(null);
            String contents = PastebinUtil.getPastebinContents(id);
            int issues = ThemisMod.loadConfigString(contents);
            this.client.openScreen(new FinishImportScreen(this.parent, contents, title, issues));
        } catch (Exception e) {
            ThemisMod.LOGGER.error("Failed to get paste!", e);
            lowText = new LiteralText("Failed to get paste! Check log for errors.");
            lowTextColor = 0xFF6666;
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        this.textField.render(matrices, mouseX, mouseY, delta);
        this.drawCenteredText(matrices, this.textRenderer, this.title, width / 2, 15, 0xFFFFFF);
        this.drawCenteredText(matrices, this.textRenderer, TEXT, width / 2, height / 2 - 22, 0xFFFFFF);
        this.drawCenteredText(matrices, this.textRenderer, lowText, width / 2, height / 2 + 60, lowTextColor);
    }

    @Override
    public void tick() {
        textField.tick();
        this.importButton.active = !PastebinUtil.isIDInvalid(PastebinUtil.clean(textField.getText()));
    }

    @Override
    public void onClose() {
        this.parent.reopenScreen();
    }
}

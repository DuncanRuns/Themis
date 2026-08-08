package me.duncanruns.themis.screen;

import me.duncanruns.themis.ThemisMod;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class FinishImportScreen extends Screen {
    private static final Set<Character> ILLEGAL_FILE_CHARS = new HashSet<>();

    static {
        for (char c : "/\n\r\t\0\f`?*\\<>|\":".toCharArray()) {
            ILLEGAL_FILE_CHARS.add(c);
        }
    }

    private static final Text TEXT = new LiteralText("Enter a name for the config");
    private final ThemisConfigSelectionScreen parent;
    private final String configContents;
    private String initialConfigTitle;

    private List<Text> lowText = Collections.emptyList();
    private int lowTextColor = 0xAAAAAA;
    private TextFieldWidget textField;

    private ButtonWidget importButton;

    private String overwrite = null;

    public FinishImportScreen(
            ThemisConfigSelectionScreen parent,
            String configContents,
            @Nullable String initialConfigTitle,
            int loadIssues
    ) {
        super(new LiteralText("Import Themis Config"));
        this.parent = parent;
        this.initialConfigTitle = initialConfigTitle;
        if (loadIssues != 0) this.lowText = Arrays.asList(
                new LiteralText("There were " + loadIssues + " warnings/errors while loading this configuration."),
                new LiteralText("Check the log for more information.")
        );
        this.configContents = configContents;
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
        if (this.initialConfigTitle != null) {
            this.textField.setText(this.initialConfigTitle);
            this.initialConfigTitle = null;
        }

        importButton = addButton(new ButtonWidget(
                width / 2 - 100, height / 2 + 30,
                97, 20,
                new LiteralText("Import"),
                button -> onPressImport()
        ));
        importButton.active = isValidFileName(textField.getText().trim());

        addButton(new ButtonWidget(
                width / 2 + 3, height / 2 + 30,
                97, 20,
                ScreenTexts.CANCEL,
                button -> onClose()
        ));
    }

    private boolean isValidFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) return false;
        for (char c : fileName.toCharArray()) {
            if (ILLEGAL_FILE_CHARS.contains(c)) {
                return false;
            }
        }
        return true;
    }

    private String cleanFileName(String fileName) {
        char[] originalChars = fileName.toCharArray();
        int i = 0;
        char[] outChars = new char[originalChars.length];

        for (char originalChar : originalChars) {
            if (ILLEGAL_FILE_CHARS.contains(originalChar)) continue;
            outChars[i++] = originalChar;
        }
        return new String(outChars, 0, i);
    }

    private void onPressImport() {
        String name = textField.getText().trim();
        if (!isValidFileName(name)) return;

        if (!Objects.equals(overwrite, name)) {
            Collection<String> names;
            try {
                names = ThemisMod.getAvailableConfigsNames();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (names.contains(name)) {
                lowText = Arrays.asList(
                        new LiteralText("Config name is already taken!"),
                        new LiteralText("Press import again to overwrite.")
                );
                lowTextColor = 0xFF6666;
                overwrite = name;
                return;
            }
        }
        try {
            Files.write(ThemisMod.CONFIG_DIR.resolve(name + ".json"), configContents.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ThemisMod.setSelectedFile(name);
        onClose();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        this.textField.render(matrices, mouseX, mouseY, delta);
        this.drawCenteredText(matrices, this.textRenderer, this.title, width / 2, 15, 0xFFFFFF);
        this.drawCenteredText(matrices, this.textRenderer, TEXT, width / 2, height / 2 - 22, 0xFFFFFF);
        int y = height / 2 + 60;
        for (Text text : lowText) {
            this.drawCenteredText(matrices, this.textRenderer, text, width / 2, y, lowTextColor);
            y += textRenderer.fontHeight + 2;
        }
    }

    @Override
    public void tick() {
        textField.tick();
        String name = textField.getText().trim();
        importButton.active = isValidFileName(name);
        if (overwrite != null && !overwrite.equals(name)) {
            overwrite = null;
            lowText = Collections.emptyList();
        }
    }

    @Override
    public void onClose() {
        this.parent.reopenScreen();
    }
}

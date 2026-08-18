package me.duncanruns.themis;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import me.duncanruns.themis.rerollers.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.loot.LootTables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ThemisMod implements ModInitializer {
    public static final String MOD_ID = "themis";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static String VERSION = "";

    public static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
    private static final Path SELECTED_FILE_PATH = CONFIG_DIR.resolve("selected.txt");

    public static boolean temporaryConfigLoaded = false;
    // Configurable
    public static final Map<String, JsonObject> REROLLER_CONFIGS = new HashMap<>();
    public static final Map<String, Long> PRE_SET_SEEDS = new HashMap<>();
    public static final String[] SKULL_REROLLERS = new String[4];
    public static Map<String, List<String>> SEQUENCE_OVERRIDES = Collections.emptyMap();
    public static Set<String> SEQUENCE_OVERRIDES_LOOPING = Collections.emptySet();
    // ---

    public static final Map<String, Supplier<Reroller>> REROLLERS = new HashMap<>();

    static {
        REROLLERS.put(LootTables.PIGLIN_BARTERING_GAMEPLAY.toString(), PiglinBarterReroller::new);
        REROLLERS.put("minecraft:entities/blaze", BlazeReroller::new);
        REROLLERS.put("minecraft:entities/iron_golem", IronGolemReroller::new);
        REROLLERS.put("minecraft:blocks/gravel", GravelReroller::new);
        REROLLERS.put("eye_drops", EyeBreaksReroller::new);
    }

    public static int getLooting(Entity entity) {
        if (!(entity instanceof LivingEntity)) return 0;
        return EnchantmentHelper.getLooting((LivingEntity) entity);
    }

    public static int getLootingForSkullRngId(String key) {
        return Arrays.asList(SKULL_REROLLERS).indexOf(key);
    }

    @Override
    public void onInitialize() {
        VERSION = FabricLoader.getInstance().getModContainer(MOD_ID).map(m -> m.getMetadata().getVersion().getFriendlyString()).orElseThrow(() -> new RuntimeException("Failed to get own mod version!"));

        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        checkSetToRerollerJson();

        tryLoadConfig();
    }

    public static void tryLoadConfig() {
        try {
            loadConfig();
        } catch (IOException e) {
            LOGGER.error("Failed to load config!", e);
        }
    }

    public static void loadConfig() throws IOException {
        clearConfig();
        Optional<String> selectedOpt = getSelectedFileName();
        if (selectedOpt.isPresent()) loadConfig(selectedOpt.get());
        temporaryConfigLoaded = false;
    }

    public static void loadConfig(String fileName) throws IOException {
        Optional<String> contentsOpt = getConfigFileString(fileName);
        if (!contentsOpt.isPresent()) {
            LOGGER.warn("Selected Themis config was not found! {}", fileName);
            return;
        }
        loadConfigString(contentsOpt.get());
    }

    public static Optional<String> getSelectedFileName() throws IOException {
        if (!Files.exists(SELECTED_FILE_PATH)) {
            LOGGER.info("No Themis config is selected.");
            return Optional.empty();
        }
        return Optional.of(new String(Files.readAllBytes(SELECTED_FILE_PATH)));

    }

    private static Optional<String> getConfigFileString(String selected) throws IOException {
        Path configPath = CONFIG_DIR.resolve(selected + ".json");
        if (!Files.exists(configPath)) {
            return Optional.empty();
        }
        return Optional.of(new String(Files.readAllBytes(configPath)));
    }

    /**
     * Returns number of warnings/errors encountered
     */
    public static int loadConfigString(String config) {
        clearConfig();
        int out = 0;
        Gson gson = new Gson();
        JsonObject configJson = gson.fromJson(config, JsonObject.class);
        if (configJson.has("skull_rerollers"))
            out += loadSkullConfig(configJson.getAsJsonObject("skull_rerollers"));
        if (configJson.has("rerollers"))
            out += loadRerollersConfig(configJson.getAsJsonObject("rerollers"));
        if (configJson.has("sequence_overrides")) {
            out += loadSequenceOverridesConfig(gson, configJson);
        }
        return out;
    }

    private static int loadSequenceOverridesConfig(Gson gson, JsonObject configJson) {
        int out = 0;
        SequeunceConfigContainer scc = gson.fromJson(configJson, SequeunceConfigContainer.class);
        if (scc.sequenceOverrides != null) SEQUENCE_OVERRIDES = scc.sequenceOverrides;
        if (scc.sequenceOverridesLooping != null) SEQUENCE_OVERRIDES_LOOPING = scc.sequenceOverridesLooping;
        else {
            LOGGER.error("Invalid sequence_overrides object!");
            out++;
        }
        return out;
    }

    private static void clearConfig() {
        REROLLER_CONFIGS.clear();
        PRE_SET_SEEDS.clear();
        SEQUENCE_OVERRIDES = Collections.emptyMap();
        for (int i = 0; i < 4; i++) SKULL_REROLLERS[i] = "skulls/" + i;
    }

    private static int loadRerollersConfig(JsonObject rerollers) {
        int issues = 0;
        for (Map.Entry<String, JsonElement> e : rerollers.entrySet()) {
            String key = e.getKey();
            if (e.getValue().isJsonPrimitive()) {
                try {
                    PRE_SET_SEEDS.put(key, (long) e.getValue().getAsNumber());
                } catch (Exception ex) {
                    LOGGER.error("Failed to load config for {}", key, ex);
                    issues++;
                    continue;
                }
            }
            Reroller reroller = getReroller(key);
            if (reroller == null) continue;
            // Test config on a sample reroller object to make sure it's valid, no side effects should be caused by
            // Reroller#loadConfig in this context.
            boolean loadSuccess;
            try {
                loadSuccess = reroller.loadConfig(e.getValue().getAsJsonObject());
            } catch (Exception ex) {
                issues++;
                LOGGER.error("Failed to load config for {}", key, ex);
                continue;
            }
            if (!loadSuccess) {
                issues++;
                LOGGER.warn("Failed to load config for {}", key);
                continue;
            }
            REROLLER_CONFIGS.put(key, e.getValue().getAsJsonObject());
            LOGGER.info("Loaded config for {}: {}", key, e.getValue());

        }
        return issues;
    }

    private static @Nullable Reroller getReroller(String key) {
        Reroller reroller;
        if (Arrays.asList(SKULL_REROLLERS).contains(key)) {
            reroller = new SkullReroller(getLootingForSkullRngId(key));
        } else {
            if (!REROLLERS.containsKey(key)) {
                LOGGER.warn("No reroller found for {}", key);
                return null;
            }
            reroller = REROLLERS.get(key).get();
        }
        return reroller;
    }

    private static int loadSkullConfig(JsonObject skullRerollers) {
        int issues = 0;
        for (Map.Entry<String, JsonElement> e : skullRerollers.entrySet()) {
            try {
                int looting = Integer.parseInt(e.getKey());
                if (looting < 0 || looting > 3) throw new IllegalArgumentException("Looting must be between 0 and 3!");
                String target = e.getValue().getAsString();
                if (target.startsWith("skulls/")) target = target.substring("skulls/".length());
                SKULL_REROLLERS[looting] = "skulls/" + target;
            } catch (Exception ex) {
                issues++;
                LOGGER.error("Keys in skull_rerollers must be an integer between 0 and 3!", ex);
            }
        }
        return issues;
    }

    public static boolean deselectConfig() {
        try {
            Files.deleteIfExists(SELECTED_FILE_PATH);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static boolean setSelectedFile(String fileName) {
        if (fileName == null) return deselectConfig();
        try {
            Files.write(SELECTED_FILE_PATH, fileName.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOGGER.error("Failed to write selected file {}", fileName, e);
            return false;
        }
        return true;
    }

    private static void checkSetToRerollerJson() {
        Path oldConfigPath = CONFIG_DIR.resolveSibling("reroller.json");
        if (Files.exists(oldConfigPath)) {
            try {
                Files.move(oldConfigPath, CONFIG_DIR.resolve("reroller.json"));
                LOGGER.info("Moved old config reroller.json into themis folder.");
            } catch (IOException e) {
                LOGGER.error("Failed to move old config file {}", oldConfigPath, e);
            }
            if (!Files.exists(SELECTED_FILE_PATH)) {
                if (setSelectedFile("reroller")) {
                    LOGGER.info("Old reroller config has been selected as the used config");
                }
            }
        }
    }

    public static Collection<String> getAvailableConfigsNames() throws IOException {
        if (!Files.exists(CONFIG_DIR)) return Collections.emptySet();
        try (Stream<Path> list = Files.list(CONFIG_DIR)) {
            return list
                    .map(path -> path.getFileName().toString())
                    .filter(s -> s.endsWith(".json"))
                    .map(s -> s.substring(0, s.length() - 5))
                    .collect(Collectors.toSet());
        }
    }

    public static final class SequeunceConfigContainer {
        @SerializedName("sequence_overrides")
        public Map<String, List<String>> sequenceOverrides = null;
        @SerializedName("sequence_overrides_enable_looping")
        public Set<String> sequenceOverridesLooping = null;
    }
}
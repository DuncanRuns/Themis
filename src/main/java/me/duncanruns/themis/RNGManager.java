package me.duncanruns.themis;

import com.google.gson.JsonObject;
import me.duncanruns.themis.mixinint.RerollerServer;
import me.duncanruns.themis.mixinint.ThemisTagOwner;
import me.duncanruns.themis.random.CountedRandom;
import me.duncanruns.themis.rerollers.Reroller;
import me.duncanruns.themis.rerollers.SkullReroller;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class RNGManager {
    private final Map<String, CountedRandom> randoms = new HashMap<>();
    private final Set<String> failedRerollers = new HashSet<>();
    private String[] skullRerollers = new String[]{"skulls/1", "skulls/2", "skulls/3", "skulls/4"};
    private volatile Reroller currentlyRerolling = null;
    private final long worldSeed;

    public static RNGManager get(MinecraftServer server) {
        return ((RerollerServer) server).themis$getRNGManager();
    }

    /**
     * Utility method, short for RerollerManager.get(server).getRandom(key)
     * or ((RerollerServer) server).reroller$getRerollerManager().getRandom(key)
     */
    public static CountedRandom getRandom(MinecraftServer server, String key) {
        return get(server).getRandom(key);
    }

    public static CountedRandom getSkullRandom(MinecraftServer server, int looting) {
        return get(server).getSkullRandom(looting);
    }

    // Not supposed to be cryptographically strong, just supposed to somewhat mix things up so similar loot tables don't
    // produce the same seed (e.g. spruce leaves vs birch leaves)
    public static long mixSeed(String key, long baseSeed) {
        // Convert the string to bytes in a consistent way
        byte[] bytes = key.getBytes(StandardCharsets.UTF_8);

        // Start with the base seed
        long h = baseSeed ^ 0x9E3779B97F4A7C15L; // use golden ratio constant

        // Simple but effective 64-bit mixing of bytes
        for (byte b : bytes) {
            h ^= b;
            h *= 0xBF58476D1CE4E5B9L; // a MurmurHash3 constant
            h = Long.rotateLeft(h, 27);
            h *= 0x94D049BB133111EBL; // another MurmurHash3 constant
        }

        // Final avalanche (MurmurHash3 finalizer style)
        h ^= (h >>> 30);
        h *= 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 27);
        h *= 0x94D049BB133111EBL;
        h ^= (h >>> 31);

        return h;
    }

    public void load(MinecraftServer server) {
        Set<String> alreadyLoaded = new HashSet<>();
        CompoundTag themisTag = ((ThemisTagOwner) server.getSaveProperties()).themis$getTag();
        if (themisTag.contains("RNGManager")) {
            CompoundTag tag = themisTag.getCompound("RNGManager");
            tag.getKeys()
                    .forEach(s -> {
                        randoms.put(s, CountedRandom.fromTag(tag.getCompound(s)));
                        alreadyLoaded.add(s);
                    });
        }
        if (themisTag.contains("SkullRerollers")) {
            ListTag tag = themisTag.getList("SkullRerollers", 8);
            skullRerollers = new String[4];
            for (int i = 0; i < tag.size(); i++) {
                skullRerollers[i] = tag.asString();
            }
        } else {
            skullRerollers = ThemisMod.SKULL_REROLLERS;
            new HashSet<>(Arrays.asList(skullRerollers)).forEach(s -> {
                if (alreadyLoaded.contains(s)) return;
                if (!ThemisMod.REROLLER_CONFIGS.containsKey(s)) return;
                JsonObject rerollerConfigEntry = ThemisMod.REROLLER_CONFIGS.get(s);
                int i = ThemisMod.getLootingForSkullRngId(s);
                reroll(server, s, rerollerConfigEntry, new SkullReroller(i));
            });
        }


        ThemisMod.PRE_SET_SEEDS.forEach((key, seed) -> {
            if (alreadyLoaded.contains(key)) return;
            randoms.put(key, new CountedRandom(seed, 0));
        });

        ThemisMod.REROLLER_CONFIGS.forEach((key, rerollerConfigEntry) -> {
            if (alreadyLoaded.contains(key)) return;
            if (!ThemisMod.REROLLERS.containsKey(key)) return;
            reroll(server, key, rerollerConfigEntry, ThemisMod.REROLLERS.get(key).get());
        });
        currentlyRerolling = null;
    }

    private void reroll(MinecraftServer server, String key, JsonObject rerollerConfigEntry, Reroller reroller) {
        ThemisMod.LOGGER.info("Rerolling {}...", key);
        currentlyRerolling = reroller;
        OptionalLong rerolledSeed;
        try {
            rerolledSeed = reroller.getRerolledSeed(server, mixSeed(key, worldSeed), rerollerConfigEntry);
        } catch (Exception e) {
            ThemisMod.LOGGER.error("Failed to reroll seed for {}", key, e);
            failedRerollers.add(key);
            return;
        }
        if (rerolledSeed.isPresent()) {
            ThemisMod.LOGGER.info("Successfully rerolled seed for {}", key);
            randoms.put(key, new CountedRandom(rerolledSeed.getAsLong(), 0));
        } else {
            // Prevent rerolling next time
            randoms.put(key, new CountedRandom(mixSeed(key, worldSeed), 0));
            failedRerollers.add(key);
        }
    }

    public @NotNull Tag getRandomsTag() {
        CompoundTag tag = new CompoundTag();
        randoms.forEach((s, countedRandom) -> tag.put(s, countedRandom.toTag()));
        return tag;
    }

    public @NotNull Tag getSkullsTag() {
        ListTag skulls = new ListTag();
        for (String skullReroller : skullRerollers) skulls.add(StringTag.of(skullReroller));
        return skulls;
    }

    public void tick(MinecraftServer thisServer) {
        if (failedRerollers.isEmpty()) return;
        if (thisServer.getPlayerManager().getPlayerList().isEmpty()) return;
        for (String failedReroller : failedRerollers) {
            thisServer.getPlayerManager().broadcastChatMessage(Text.method_30163("Failed to reroll seed for \"" + failedReroller + "\""), MessageType.CHAT, Util.NIL_UUID);
        }
        failedRerollers.clear();
    }

    public RNGManager(long worldSeed) {
        this.worldSeed = worldSeed;
    }

    public Reroller getCurrentlyRerolling() {
        return currentlyRerolling;
    }

    public CountedRandom getRandom(String key) {
        return randoms.computeIfAbsent(key, k -> new CountedRandom(mixSeed(key, worldSeed), 0));
    }

    public CountedRandom getSkullRandom(int looting) {
        return getRandom(skullRerollers[looting]);
    }
}

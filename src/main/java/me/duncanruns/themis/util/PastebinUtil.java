package me.duncanruns.themis.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class PastebinUtil {
    private PastebinUtil() {
    }

    private static final String BASE_RAW = "https://pastebin.com/raw/";
    private static final String BASE = "https://pastebin.com/";
    private static final Set<Character> VALID_CHARS = new HashSet<>();

    static {
        for (char c = 'a'; c <= 'z'; c++) VALID_CHARS.add(c);
        for (char c = 'A'; c <= 'Z'; c++) VALID_CHARS.add(c);
        for (char c = '0'; c <= '9'; c++) VALID_CHARS.add(c);
    }

    public static boolean isIDInvalid(String id) {
        if (id.isEmpty()) return true;
        for (char c : id.toCharArray()) {
            if (!VALID_CHARS.contains(c)) return true;
        }
        return false;
    }

    public static String getPastebinContents(String id) throws IllegalArgumentException, IOException {
        if (isIDInvalid(id)) throw new IllegalArgumentException("Invalid Pastebin ID!");
        id = clean(id);
        String url = BASE_RAW + id;
        return GrabUtil.grab(url);
    }

    public static @NotNull String clean(String in) {
        in = in.trim();
        if (in.contains("?")) in = in.substring(0, in.indexOf("?"));
        if (in.startsWith("http:")) in = "https:" + in.substring("http:".length());
        while (in.startsWith(BASE_RAW)) in = in.substring(BASE_RAW.length());
        while (in.startsWith(BASE)) in = in.substring(BASE.length());
        return in;
    }

    // Epic hacking noises
    public static Optional<String> getTitle(String id) throws IOException {
        if (isIDInvalid(id)) throw new IllegalArgumentException("Invalid Pastebin ID!");
        String url = BASE + id;
        return GrabUtil.grabPageTitle(url).map(s -> {
            if (!s.endsWith(" - Pastebin.com")) return null;
            return s.substring(0, s.length() - " - Pastebin.com".length());
        });
    }
}

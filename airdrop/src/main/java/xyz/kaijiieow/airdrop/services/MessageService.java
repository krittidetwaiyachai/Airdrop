package xyz.kaijiieow.airdrop.services;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import xyz.kaijiieow.airdrop.AirdropPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MessageService {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private final AirdropPlugin plugin;

    public MessageService(AirdropPlugin plugin) {
        this.plugin = plugin;
    }

    public String get(String path, String def) {
        return format(path, def, Collections.emptyMap());
    }

    public String format(String path, String def, Map<String, String> placeholders) {
        String raw = applyPlaceholders(getRaw(path, def), placeholders);
        return colorize(raw);
    }

    public List<String> getList(String path, List<String> def) {
        return formatList(path, def, Collections.emptyMap());
    }

    public List<String> formatList(String path, List<String> def, Map<String, String> placeholders) {
        return formatListInternal(path, def, placeholders, true);
    }

    public List<String> formatPlainList(String path, List<String> def, Map<String, String> placeholders) {
        return formatListInternal(path, def, placeholders, false);
    }

    public Component component(String path, String def, Map<String, String> placeholders) {
        String raw = applyPlaceholders(getRaw(path, def), placeholders);
        return LEGACY_SERIALIZER.deserialize(raw);
    }

    private String getRaw(String path, String def) {
        String key = "messages." + path;
        if (plugin.getConfig().contains(key)) {
            String value = plugin.getConfig().getString(key);
            if (value != null) {
                return value;
            }
        }
        return def;
    }

    private List<String> getRawList(String path, List<String> def) {
        String key = "messages." + path;
        List<String> list = plugin.getConfig().getStringList(key);
        if (list == null || list.isEmpty()) {
            return def;
        }
        return list;
    }

    private List<String> formatListInternal(String path, List<String> def, Map<String, String> placeholders, boolean colorize) {
        List<String> input = new ArrayList<>(getRawList(path, def));
        List<String> result = new ArrayList<>(input.size());
        for (String line : input) {
            String applied = applyPlaceholders(line, placeholders);
            result.add(colorize ? colorize(applied) : applied);
        }
        return result;
    }

    private String applyPlaceholders(String input, Map<String, String> placeholders) {
        String output = input;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            output = output.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return output;
    }

    private String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}

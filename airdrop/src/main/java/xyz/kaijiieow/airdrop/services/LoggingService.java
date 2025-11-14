package xyz.kaijiieow.airdrop.services;

import xyz.kaijiieow.airdrop.AirdropPlugin;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LoggingService {

    private final AirdropPlugin plugin;

    public LoggingService(AirdropPlugin plugin) {
        this.plugin = plugin;
    }

    public void info(String msg) {
        plugin.getLogger().info(msg);
        logDiscord("INFO", msg);
    }

    public void warn(String msg) {
        plugin.getLogger().warning(msg);
        logDiscord("WARN", msg);
    }

    public void error(String msg) {
        plugin.getLogger().severe(msg);
        logDiscord("ERROR", msg);
    }

    private void logDiscord(String level, String msg) {
        String url = plugin.getConfig().getString("logging.discord-webhook-url", "");
        if (url == null || url.isEmpty()) return;

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String safeMsg = msg.replace("\"", "'");
            String json = "{\"content\":\"[" + level + "] " + safeMsg + "\"}";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
            conn.getResponseCode();
        } catch (Exception ignored) {
        }
    }
}

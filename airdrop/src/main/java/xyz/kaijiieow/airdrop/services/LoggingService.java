package xyz.kaijiieow.airdrop.services;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import xyz.kaijiieow.airdrop.AirdropPlugin;
import xyz.kaijiieow.airdrop.core.Airdrop;

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

    public void logMinigameCode(Player player, Airdrop airdrop, String realCode, String scrambled) {
        Location loc = airdrop.getLocation();
        String locationText = "ไม่ทราบ";
        if (loc != null && loc.getWorld() != null) {
            locationText = loc.getWorld().getName() + " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
        }
        String consoleMsg = "MiniGame code generated for " + player.getName() + " @ " + locationText
                + " | answer=" + realCode + " | shown=" + scrambled;
        plugin.getLogger().info(consoleMsg);

        StringBuilder desc = new StringBuilder();
        desc.append("**ผู้เล่น:** ").append(player.getName()).append("\n");
        desc.append("**จุดเกิด:** ").append(locationText).append("\n");
        desc.append("**เฉลยจริง:** `").append(realCode).append("`\n");
        desc.append("**ตัวเลขที่เห็น:** `").append(scrambled).append("`\n");
        desc.append("ใส่ตัวเลขตามลำดับแล้วกดยืนยันเพื่อปลดล็อกให้ไว!");

        sendDiscordEmbed("🔐 สร้างรหัสกล่อง Airdrop", desc.toString(), 0xFFD966);
    }

    private void logDiscord(String level, String msg) {
        String url = plugin.getConfig().getString("logging.discord-webhook-url", "");
        if (url == null || url.isEmpty()) return;

        String safeMsg = msg.replace("\"", "'");
        String json = "{\"content\":\"[" + level + "] " + safeMsg + "\"}";
        sendDiscordPayload(json);
    }

    private void sendDiscordEmbed(String title, String description, int color) {
        String url = plugin.getConfig().getString("logging.discord-webhook-url", "");
        if (url == null || url.isEmpty()) return;

        String json = "{"
                + "\"embeds\":[{"
                + "\"title\":\"" + escapeJson(title) + "\","
                + "\"description\":\"" + escapeJson(description) + "\","
                + "\"color\":" + color + ","
                + "\"footer\":{\"text\":\"AirdropPlugin • แจ้งเตือน\"}"
                + "}]"
                + "}";
        sendDiscordPayload(json);
    }

    private void sendDiscordPayload(String json) {
        String url = plugin.getConfig().getString("logging.discord-webhook-url", "");
        if (url == null || url.isEmpty()) return;
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
            conn.getResponseCode();
        } catch (Exception ignored) {
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

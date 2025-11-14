package xyz.kaijiieow.airdrop.services;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import xyz.kaijiieow.airdrop.AirdropPlugin;
import xyz.kaijiieow.airdrop.core.Airdrop;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant; // <-- เพิ่ม import ตัวนี้

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

    /**
     * (จุดที่แก้ไข)
     * 1. File Log ยังคงมี answer เหมือนเดิม
     * 2. เปลี่ยน Discord Embed ให้ใช้ Fields และเพิ่ม Timestamp
     */
    public void logMinigameCode(Player player, Airdrop airdrop, String realCode, String scrambled) {
        Location loc = airdrop.getLocation();
        String locationText = "ไม่ทราบ";
        if (loc != null && loc.getWorld() != null) {
            locationText = loc.getWorld().getName() + " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
        }

        // 1. File Log (มี answer อยู่แล้ว)
        String consoleMsg = "MiniGame code generated for " + player.getName() + " @ " + locationText
                + " | answer=" + realCode + " | shown=" + scrambled;
        plugin.getLogger().info(consoleMsg);

        // 2. Discord Webhook (ดีไซน์ใหม่)
        String url = plugin.getConfig().getString("logging.discord-webhook-url", "");
        if (url == null || url.isEmpty()) return;

        // สร้าง JSON Payload โดยใช้ Fields
        String jsonPayload = String.format(
            "{\"embeds\":[{\"title\":\"🔐 เริ่มเกมปลดล็อก Airdrop\",\"description\":\"ผู้เล่นกำลังพยายามปลดล็อกกล่อง Airdrop\",\"color\":16766566,\"fields\":[{\"name\":\"👤 ผู้เล่น (Player)\",\"value\":\"`%s`\",\"inline\":true},{\"name\":\"📍 พิกัด (Location)\",\"value\":\"`%s`\",\"inline\":true},{\"name\":\"🔑 รหัสเฉลย (Answer)\",\"value\":\"```\\n%s\\n```\",\"inline\":false},{\"name\":\"🎲 ตัวเลขที่แสดง (Shown)\",\"value\":\"```\\n%s\\n```\",\"inline\":false}],\"footer\":{\"text\":\"AirdropPlugin • แจ้งเตือน\"},\"timestamp\":\"%s\"}]}",
            escapeJson(player.getName()),       // %s (Player)
            escapeJson(locationText),           // %s (Location)
            escapeJson(realCode),               // %s (Answer)
            escapeJson(scrambled),              // %s (Shown)
            Instant.now().toString()            // %s (Timestamp)
        );

        sendDiscordPayload(jsonPayload);
    }

    private void logDiscord(String level, String msg) {
        String url = plugin.getConfig().getString("logging.discord-webhook-url", "");
        if (url == null || url.isEmpty()) return;

        String safeMsg = msg.replace("\"", "'");
        String json = "{\"content\":\"[" + level + "] " + safeMsg + "\"}";
        sendDiscordPayload(json);
    }

    // เมธอดนี้ไม่ได้ใช้แล้วหลังจากแก้ logMinigameCode แต่เก็บไว้เผื่อที่อื่นเรียก
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
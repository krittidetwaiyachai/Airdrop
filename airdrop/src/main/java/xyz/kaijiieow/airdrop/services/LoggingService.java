package xyz.kaijiieow.airdrop.services;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import xyz.kaijiieow.airdrop.AirdropPlugin;
import xyz.kaijiieow.airdrop.core.Airdrop;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

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
     * Helper: สร้าง String Location สวยๆ
     */
    private String formatLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return "ไม่ทราบ";
        }
        return loc.getWorld().getName() + " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }

    /**
     * Log 1: ตอน Airdrop เกิด (แก้ให้รับ Airdrop object และ log เฉลย)
     */
    public void logSpawn(Airdrop airdrop) {
        String locationText = formatLocation(airdrop.getLocation());
        String code = airdrop.getCode(); // <-- ดึงโค้ด

        // 1. Log ไป console (เพิ่ม answer)
        String consoleMsg = "Airdrop spawned at " + locationText + " | answer=" + code;
        plugin.getLogger().info(consoleMsg);

        // 2. Log ไป Discord (เพิ่ม field เฉลย)
        String url = plugin.getConfig().getString("logging.discord-webhook-url", "");
        if (url == null || url.isEmpty()) return;

        // (แก้ 1: เปลี่ยน value ของ "พิกัด" และ "รหัสเฉลย" เป็น code block และแก้ inline)
        String jsonPayload = String.format(
            "{\"embeds\":[{\"title\":\"✨ Airdrop ปรากฏตัวแล้ว!\",\"description\":\"Airdrop กล่องใหม่ได้เกิดในโลก\",\"color\":5814783,\"fields\":[{\"name\":\"📍 พิกัด (Location)\",\"value\":\"```\\n%s\\n```\",\"inline\":false},{\"name\":\"🔑 รหัสเฉลย (Answer)\",\"value\":\"```\\n%s\\n```\",\"inline\":false}],\"footer\":{\"text\":\"AirdropPlugin • แจ้งเตือน\"},\"timestamp\":\"%s\"}]}",
            escapeJson(locationText),       // %s (Location) <-- แก้ value
            escapeJson(code),               // %s (Answer)
            Instant.now().toString()        // %s (Timestamp)
        );

        sendDiscordPayload(jsonPayload);
    }

    /**
     * Log 2: ตอนเริ่มเล่นมินิเกม (เมธอดนี้จะไม่มีใครเรียกใช้แล้ว แต่ทิ้งไว้ก็ได้)
     */
    public void logMinigameCode(Player player, Airdrop airdrop, String realCode, String scrambled) {
        String locationText = formatLocation(airdrop.getLocation());

        // 1. File Log (มี answer อยู่แล้ว)
        String consoleMsg = "MiniGame code generated for " + player.getName() + " @ " + locationText
                + " | answer=" + realCode + " | shown=" + scrambled;
        plugin.getLogger().info(consoleMsg);

        // 2. Discord Webhook (ดีไซน์ใหม่)
        String url = plugin.getConfig().getString("logging.discord-webhook-url", "");
        if (url == null || url.isEmpty()) return;

        // (แก้ 2: เปลี่ยน value ทุกช่องเป็น code block และ inline: false)
        String jsonPayload = String.format(
            "{\"embeds\":[{\"title\":\"🔐 เริ่มเกมปลดล็อก Airdrop\",\"description\":\"ผู้เล่นกำลังพยายามปลดล็อกกล่อง Airdrop\",\"color\":16766566,\"fields\":[{\"name\":\"👤 ผู้เล่น (Player)\",\"value\":\"```\\n%s\\n```\",\"inline\":false},{\"name\":\"📍 พิกัด (Location)\",\"value\":\"```\\n%s\\n```\",\"inline\":false},{\"name\":\"🔑 รหัสเฉลย (Answer)\",\"value\":\"```\\n%s\\n```\",\"inline\":false},{\"name\":\"🎲 ตัวเลขที่แสดง (Shown)\",\"value\":\"```\\n%s\\n```\",\"inline\":false}],\"footer\":{\"text\":\"AirdropPlugin • แจ้งเตือน\"},\"timestamp\":\"%s\"}]}",
            escapeJson(player.getName()),
            escapeJson(locationText),
            escapeJson(realCode),
            escapeJson(scrambled),
            Instant.now().toString()
        );

        sendDiscordPayload(jsonPayload);
    }

    /**
     * Log 3: ตอนผู้เล่นปลดล็อกสำเร็จ
     */
    public void logUnlock(Player player, Airdrop airdrop) {
        String locationText = formatLocation(airdrop.getLocation());
        
        // 1. Console Log
        String consoleMsg = "Player " + player.getName() + " unlocked airdrop " + airdrop.getId() + " at " + locationText;
        plugin.getLogger().info(consoleMsg);

        // 2. Discord Log
        String url = plugin.getConfig().getString("logging.discord-webhook-url", "");
        if (url == null || url.isEmpty()) return;

        // (แก้ 3: เปลี่ยน Avatar URL เป็น minotar.net)
        String playerUUID = player.getUniqueId().toString();
        String avatarURL = "https://minotar.net/avatar/" + playerUUID + "/100.png";

        // (แก้ 4: เปลี่ยน value เป็น code block และ inline: false)
        String jsonPayload = String.format(
            "{\"embeds\":[{\"title\":\"✅ ปลดล็อก Airdrop สำเร็จ\",\"description\":\"ผู้เล่นปลดล็อกกล่อง Airdrop ได้แล้ว\",\"color\":5763719,\"thumbnail\":{\"url\":\"%s\"},\"fields\":[{\"name\":\"👤 ผู้เล่น (Player)\",\"value\":\"```\\n%s\\n```\",\"inline\":false},{\"name\":\"📍 พิกัด (Location)\",\"value\":\"```\\n%s\\n```\",\"inline\":false}],\"footer\":{\"text\":\"AirdropPlugin • แจ้งเตือน\"},\"timestamp\":\"%s\"}]}",
            escapeJson(avatarURL), // <-- %s (Thumbnail URL)
            escapeJson(player.getName()),
            escapeJson(locationText),
            Instant.now().toString()
        );
        sendDiscordPayload(jsonPayload);
    }

    /**
     * Log 4: ตอนเจ้าของเก็บของหมด
     */
    public void logEmptiedByOwner(Player player, Location loc) {
        String locationText = formatLocation(loc);

        // 1. Console Log
        String consoleMsg = "Airdrop at " + locationText + " emptied by owner (" + player.getName() + ") & removed.";
        plugin.getLogger().info(consoleMsg);

        // 2. Discord Log
        String url = plugin.getConfig().getString("logging.discord-webhook-url", "");
        if (url == null || url.isEmpty()) return;
        
        // (แก้ 5: เปลี่ยน Avatar URL เป็น minotar.net)
        String playerUUID = player.getUniqueId().toString();
        String avatarURL = "https://minotar.net/avatar/" + playerUUID + "/100.png";

        // (แก้ 6: เปลี่ยน value เป็น code block และ inline: false)
        String jsonPayload = String.format(
            "{\"embeds\":[{\"title\":\"📦 Airdrop ถูกเก็บโดยเจ้าของ\",\"description\":\"กล่อง Airdrop ถูกเก็บโดยเจ้าของและหายไป\",\"color\":15105570,\"thumbnail\":{\"url\":\"%s\"},\"fields\":[{\"name\":\"👤 เจ้าของ (Owner)\",\"value\":\"```\\n%s\\n```\",\"inline\":false},{\"name\":\"📍 พิกัด (Location)\",\"value\":\"```\\n%s\\n```\",\"inline\":false}],\"footer\":{\"text\":\"AirdropPlugin • แจ้งเตือน\"},\"timestamp\":\"%s\"}]}",
            escapeJson(avatarURL), // <-- %s (Thumbnail URL)
            escapeJson(player.getName()),
            escapeJson(locationText),
            Instant.now().toString()
        );
        sendDiscordPayload(jsonPayload);
    }
    
    /**
     * Log 5: ตอนกล่อง Locked หมดเวลา
     */
    public void logLockedDespawn(Location loc) {
        String locationText = formatLocation(loc);

        // 1. Console Log
        String consoleMsg = "Locked airdrop despawned at " + locationText;
        plugin.getLogger().info(consoleMsg);

        // 2. Discord Log
        String url = plugin.getConfig().getString("logging.discord-webhook-url", "");
        if (url == null || url.isEmpty()) return;

        // (แก้ 7: เปลี่ยน value เป็น code block)
        String jsonPayload = String.format(
            "{\"embeds\":[{\"title\":\"⏱️ Airdrop หมดเวลา (Locked)\",\"description\":\"กล่อง Airdrop ที่ไม่มีคนปลดล็อกได้หมดเวลาและหายไป\",\"color\":15158332,\"fields\":[{\"name\":\"📍 พิกัด (Location)\",\"value\":\"```\\n%s\\n```\",\"inline\":false}],\"footer\":{\"text\":\"AirdropPlugin • แจ้งเตือน\"},\"timestamp\":\"%s\"}]}",
            escapeJson(locationText),
            Instant.now().toString()
        );
        sendDiscordPayload(jsonPayload);
    }


    // --- (เมธอดเดิมที่อยู่ข้างล่าง) ---

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
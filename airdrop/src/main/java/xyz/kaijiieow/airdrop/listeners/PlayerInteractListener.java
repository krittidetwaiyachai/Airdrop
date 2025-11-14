package xyz.kaijiieow.airdrop.listeners;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import xyz.kaijiieow.airdrop.AirdropPlugin;
import xyz.kaijiieow.airdrop.core.Airdrop;
import xyz.kaijiieow.airdrop.core.AirdropState;
import xyz.kaijiieow.airdrop.manager.AirdropManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PlayerInteractListener implements Listener {

    private final AirdropPlugin plugin;
    private final AirdropManager airdropManager;
    private final Random random = new Random();

    // Map: Player UUID -> pending interaction data
    private final Map<UUID, PendingInteraction> pendingInteractions = new ConcurrentHashMap<>();

    public PlayerInteractListener(AirdropPlugin plugin) {
        this.plugin = plugin;
        this.airdropManager = plugin.getAirdropManager();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CHEST) return;

        Optional<Airdrop> opt = airdropManager.getAirdropAt(block);
        if (opt.isEmpty()) return;

        Airdrop airdrop = opt.get();
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        event.setCancelled(true);

        if (airdrop.getState() == AirdropState.LOCKED) {

            // กันสแปมเปิดหลายกล่องพร้อมกัน
            PendingInteraction pending = pendingInteractions.get(playerId);
            if (pending != null && pending.airdrop().getState() != AirdropState.LOCKED) {
                pendingInteractions.remove(playerId);
                pending = null;
            }
            if (pending != null && pending.airdrop() != airdrop) {
                player.sendMessage("§cมึงกำลังกรอกโค้ดกล่องอื่นอยู่!");
                return;
            }

            openCodeDialog(airdrop, player);
            return;
        }

        // ปลดล็อกแล้ว: เช็ก owner
        if (airdrop.getOwnerUuid() != null && airdrop.getOwnerUuid().equals(playerId)) {
            if (block.getState() instanceof Container container) {
                player.openInventory(container.getInventory());
            }
        } else {
            player.sendMessage("§cกล่องนี้เป็นของคนอื่นแล้ว!");
        }
    }

    private void openCodeDialog(Airdrop airdrop, Player player) {
        int length = Math.max(1, plugin.getConfig().getInt("minigame.code-length", 4));

        UUID playerId = player.getUniqueId();
        PendingInteraction data = pendingInteractions.get(playerId);
        if (data == null || data.airdrop() != airdrop) {
            String code = generateCode(length);
            String display = scrambleDisplay(code);
            data = new PendingInteraction(airdrop, code, display);
            pendingInteractions.put(playerId, data);
            plugin.getLoggingService().logMinigameCode(player, airdrop, code, display);
        }

        final PendingInteraction interaction = data;

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("โค้ดสุ่ม: " + interaction.display(), NamedTextColor.GOLD))
                .inputs(List.of(
                    DialogInput.text("code", Component.text("รหัส " + length + " หลัก", NamedTextColor.YELLOW))
                        .width(250)
                        .maxLength(length)
                        .build()
                ))
                .canCloseWithEscape(true)
                .build()
            )
            .type(DialogType.confirmation(
                // ปุ่มยืนยัน
                ActionButton.create(
                    Component.text("ยืนยัน", NamedTextColor.GREEN),
                    Component.text("กดเพื่อส่งรหัส"),
                    100,
                    DialogAction.customClick(
                        (DialogResponseView view, Audience audience) -> {
                            if (!(audience instanceof Player p)) {
                                return;
                            }
                            UUID uid = p.getUniqueId();

                            // ดึงข้อมูล interaction ที่เก็บไว้
                            PendingInteraction pending = pendingInteractions.get(uid);
                            if (pending == null || pending.airdrop() != airdrop) {
                                // ไม่ใช่กล่องนี้ / หมดอายุไปแล้ว
                                return;
                            }

                            String input = view.getText("code");
                            String correctCode = pending.code();

                            // รันของหนักใน main thread
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                if (input != null && input.equals(correctCode)) {
                                    // โค้ดถูก
                                    plugin.getLoggingService().info("Player " + p.getName()
                                            + " unlocked airdrop " + airdrop.getId());
                                    pendingInteractions.remove(uid);
                                    airdropManager.handleUnlock(airdrop, p);
                                } else {
                                    // โค้ดผิด
                                    p.sendMessage("§cรหัสผิดไอ้โง่!");
                                    String soundName = plugin.getConfig()
                                            .getString("sounds.fail", "BLOCK_ANVIL_LAND");
                                    try {
                                        p.playSound(p.getLocation(), Sound.valueOf(soundName), 1f, 1f);
                                    } catch (IllegalArgumentException ignored) {
                                        // ถ้าพิมพ์ชื่อ sound พัง ก็ไม่ต้องเล่น
                                    }

                                    double damage = plugin.getConfig()
                                            .getDouble("minigame.damage-on-fail", 2.0);
                                    if (damage > 0) {
                                        p.damage(damage);
                                    }
                                }
                            });
                        },
                        ClickCallback.Options.builder()
                            .uses(1) // ใช้ได้ครั้งเดียวพอ
                            .lifetime(ClickCallback.DEFAULT_LIFETIME)
                            .build()
                    )
                ),
                // ปุ่มยกเลิก
                ActionButton.create(
                    Component.text("ยกเลิก", NamedTextColor.RED),
                    Component.text("ปิดหน้าต่างนี้"),
                    100,
                    DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player p) {
                                pendingInteractions.remove(p.getUniqueId());
                            }
                        },
                        ClickCallback.Options.builder()
                            .uses(1)
                            .lifetime(ClickCallback.DEFAULT_LIFETIME)
                            .build()
                    )
                )
            ))
        );

        player.sendMessage("§eตัวเลขถูกสลับเป็น §f" + interaction.display() + " §7เรียงให้ถูกแล้วกรอกลงไป!");
        player.showDialog(dialog);
    }

    private String generateCode(int length) {
        String code;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < length; i++) {
                sb.append(random.nextInt(10));
            }
            code = sb.toString();
        } while (length > 1 && allDigitsSame(code));
        return code;
    }

    private String scrambleDisplay(String code) {
        List<Character> digits = code.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toList());
        if (digits.size() <= 1) {
            return digits.stream().map(String::valueOf).collect(Collectors.joining(" "));
        }

        List<Character> scrambled = new ArrayList<>(digits);
        Set<Character> unique = new HashSet<>(digits);
        if (unique.size() > 1) {
            int attempts = 0;
            do {
                Collections.shuffle(scrambled, random);
                attempts++;
            } while (scrambled.equals(digits) && attempts < 10);

            if (scrambled.equals(digits)) {
                Collections.rotate(scrambled, 1);
            }
        }

        return scrambled.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(" "));
    }

    private boolean allDigitsSame(String value) {
        if (value.isEmpty()) return true;
        char first = value.charAt(0);
        for (int i = 1; i < value.length(); i++) {
            if (value.charAt(i) != first) {
                return false;
            }
        }
        return true;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingInteractions.remove(event.getPlayer().getUniqueId());
    }

    private record PendingInteraction(Airdrop airdrop, String code, String display) {
    }
}

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
import xyz.kaijiieow.airdrop.services.MessageService;

import java.util.*;
// (ลบ import java.util.concurrent.ConcurrentHashMap;)
// (ลบ import java.util.stream.Collectors;)

public class PlayerInteractListener implements Listener {

    private final AirdropPlugin plugin;
    private final AirdropManager airdropManager;
    private final MessageService messages;
    // (ลบ private final Random random = new Random();)
    
    // (ลบ Map pendingInteractions)

    public PlayerInteractListener(AirdropPlugin plugin) {
        this.plugin = plugin;
        this.airdropManager = plugin.getAirdropManager();
        this.messages = plugin.getMessageService();
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

            // (ลบ Block กันสแปม PendingInteraction ทั้งหมด)
            
            openCodeDialog(airdrop, player);
            return;
        }

        // ปลดล็อกแล้ว: เช็ก owner
        if (airdrop.getOwnerUuid() != null && airdrop.getOwnerUuid().equals(playerId)) {
            if (block.getState() instanceof Container container) {
                player.openInventory(container.getInventory());
            }
        } else {
            player.sendMessage(messages.get(
                    "airdrop.already-owned",
                    "&cกล่องนี้เป็นของคนอื่นแล้ว!"
            ));
        }
    }

    private void openCodeDialog(Airdrop airdrop, Player player) {
        int length = Math.max(1, plugin.getConfig().getInt("minigame.code-length", 4));
        
        // (ลบ Block ที่ดึง/สร้าง PendingInteraction data ทั้งหมด)
        // (ลบการเรียก loggingService.logMinigameCode)

        Component title = messages.component(
                "minigame.dialog.title",
                "&6โค้ดสุ่ม: {display}",
                Map.of("display", airdrop.getDisplayCode())
        );
        Component inputLabel = messages.component(
                "minigame.dialog.input-label",
                "&eรหัส {length} หลัก",
                Map.of("length", String.valueOf(length))
        );
        Component confirmLabel = messages.component(
                "minigame.dialog.confirm-label",
                "&aยืนยัน",
                Map.of()
        );
        Component confirmDescription = messages.component(
                "minigame.dialog.confirm-description",
                "&7กดเพื่อส่งรหัส",
                Map.of()
        );
        Component cancelLabel = messages.component(
                "minigame.dialog.cancel-label",
                "&cยกเลิก",
                Map.of()
        );
        Component cancelDescription = messages.component(
                "minigame.dialog.cancel-description",
                "&7ปิดหน้าต่างนี้",
                Map.of()
        );

        Dialog dialog = Dialog.create(builder -> builder.empty()
            // (แก้ตรงนี้)
            .base(DialogBase.builder(title)
                .inputs(List.of(
                    DialogInput.text("code", inputLabel)
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
                    confirmLabel,
                    confirmDescription,
                    100,
                    DialogAction.customClick(
                        (DialogResponseView view, Audience audience) -> {
                            if (!(audience instanceof Player p)) {
                                return;
                            }
                            // (ลบ UUID uid = p.getUniqueId();)

                            // (ลบ Block ดึง PendingInteraction)

                            String input = view.getText("code");
                            // (แก้ตรงนี้)
                            String correctCode = airdrop.getCode();

                            // รันของหนักใน main thread
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                if (input != null && input.equals(correctCode)) {
                                    // โค้ดถูก
                                    plugin.getLoggingService().logUnlock(p, airdrop);
                                    // (ลบ pendingInteractions.remove(uid);)
                                    airdropManager.handleUnlock(airdrop, p);
                                } else {
                                    // โค้ดผิด
                                    p.sendMessage(messages.get(
                                            "minigame.code-fail",
                                            "&c[AirDrop] รหัสผิด! โปรดลองใหม่อีกครั้ง"
                                    ));
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
                    cancelLabel,
                    cancelDescription,
                    100,
                    DialogAction.customClick(
                        (view, audience) -> {
                            // (ลบ Block pendingInteractions.remove)
                        },
                        ClickCallback.Options.builder()
                            .uses(1)
                            .lifetime(ClickCallback.DEFAULT_LIFETIME)
                            .build()
                    )
                )
            ))
        );

        // (แก้ตรงนี้)
        player.showDialog(dialog);
    }

    // (ลบเมธอด generateCode)
    // (ลบเมธอด scrambleDisplay)
    // (ลบเมธอด allDigitsSame)

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // (ลบ pendingInteractions.remove(event.getPlayer().getUniqueId());)
    }

    // (ลบ private record PendingInteraction)
}

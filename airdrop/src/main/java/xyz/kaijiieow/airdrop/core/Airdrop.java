package xyz.kaijiieow.airdrop.core;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import xyz.kaijiieow.airdrop.AirdropPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class Airdrop {

    // (เพิ่ม Random static)
    private static final Random random = new Random();

    private final UUID id;
    private final String worldName;
    private final int x, y, z;

    private AirdropState state;
    private UUID ownerUuid;
    private final long createdAt;
    private Long unlockedAt;
    private Long collectExpireAt;
    private Long despawnExpireAt;

    private Integer despawnTaskId;
    private Integer collectTaskId;

    // (เพิ่ม 2 fields นี้)
    private final String code;
    private final String displayCode;

    // (แก้ Constructor ให้รับ codeLength และสร้างโค้ดทันที)
    public Airdrop(UUID id, Location loc, long createdAt) {
        this.id = id;
        this.worldName = loc.getWorld().getName();
        this.x = loc.getBlockX();
        this.y = loc.getBlockY();
        this.z = loc.getBlockZ();
        this.createdAt = createdAt;
        this.state = AirdropState.LOCKED;

        // สร้างโค้ดและเก็บไว้เลย
        int codeLength = Math.max(1, AirdropPlugin.getInstance().getConfig().getInt("minigame.code-length", 4));
        this.code = generateCode(codeLength);
        this.displayCode = scrambleDisplay(this.code);
    }

    public UUID getId() {
        return id;
    }

    public Location getLocation() {
        World w = Bukkit.getWorld(worldName);
        if (w == null) return null;
        return new Location(w, x, y, z);
    }

    // (เพิ่ม Getters 2 อันนี้)
    public String getCode() {
        return code;
    }

    public String getDisplayCode() {
        return displayCode;
    }
    
    // (ที่เหลือเหมือนเดิม)

    public AirdropState getState() {
        return state;
    }

    public void setState(AirdropState state) {
        this.state = state;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public Long getUnlockedAt() {
        return unlockedAt;
    }

    public void setUnlockedAt(Long unlockedAt) {
        this.unlockedAt = unlockedAt;
    }

    public Long getCollectExpireAt() {
        return collectExpireAt;
    }

    public void setCollectExpireAt(Long collectExpireAt) {
        this.collectExpireAt = collectExpireAt;
    }

    public Integer getDespawnTaskId() {
        return despawnTaskId;
    }

    public void setDespawnTaskId(Integer despawnTaskId) {
        this.despawnTaskId = despawnTaskId;
    }

    public Integer getCollectTaskId() {
        return collectTaskId;
    }

    public void setCollectTaskId(Integer collectTaskId) {
        this.collectTaskId = collectTaskId;
    }

    public Long getDespawnExpireAt() {
        return despawnExpireAt;
    }

    public void setDespawnExpireAt(Long despawnExpireAt) {
        this.despawnExpireAt = despawnExpireAt;
    }

    public boolean isLocked() {
        return state == AirdropState.LOCKED;
    }

    public boolean isOwned() {
        return ownerUuid != null;
    }

    // (ย้ายเมธอด 3 อันนี้มาจาก PlayerInteractListener)

    private static String generateCode(int length) {
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

    private static String scrambleDisplay(String code) {
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

    private static boolean allDigitsSame(String value) {
        if (value.isEmpty()) return true;
        char first = value.charAt(0);
        for (int i = 1; i < value.length(); i++) {
            if (value.charAt(i) != first) {
                return false;
            }
        }
        return true;
    }
}
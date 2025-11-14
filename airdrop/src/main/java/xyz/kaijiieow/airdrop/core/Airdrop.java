package com.yourplugin.airdrop.core;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public class Airdrop {

    private final UUID id;
    private final String worldName;
    private final int x, y, z;

    private AirdropState state;
    private UUID ownerUuid;
    private final long createdAt;
    private Long unlockedAt;
    private Long collectExpireAt;

    private Integer despawnTaskId;
    private Integer collectTaskId;

    public Airdrop(UUID id, Location loc, long createdAt) {
        this.id = id;
        this.worldName = loc.getWorld().getName();
        this.x = loc.getBlockX();
        this.y = loc.getBlockY();
        this.z = loc.getBlockZ();
        this.createdAt = createdAt;
        this.state = AirdropState.LOCKED;
    }

    public UUID getId() {
        return id;
    }

    public Location getLocation() {
        World w = Bukkit.getWorld(worldName);
        if (w == null) return null;
        return new Location(w, x, y, z);
    }

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

    public boolean isLocked() {
        return state == AirdropState.LOCKED;
    }

    public boolean isOwned() {
        return ownerUuid != null;
    }
}

package com.yourplugin.airdrop.loot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LootTable {

    private final String id;
    private final int minItems;
    private final int maxItems;
    private final List<LootEntry> entries = new ArrayList<>();
    private final Random random = new Random();

    public LootTable(String id, int minItems, int maxItems) {
        this.id = id;
        this.minItems = minItems;
        this.maxItems = maxItems;
    }

    public String getId() { return id; }
    public List<LootEntry> getEntries() { return entries; }

    public void addEntry(LootEntry entry) {
        entries.add(entry);
    }

    public List<LootEntry> roll() {
        List<LootEntry> result = new ArrayList<>();
        if (entries.isEmpty()) return result;

        int count = minItems + random.nextInt(Math.max(1, maxItems - minItems + 1));
        for (int i = 0; i < count; i++) {
            LootEntry e = weightedRandom();
            if (e != null) result.add(e);
        }
        return result;
    }

    private LootEntry weightedRandom() {
        int total = entries.stream().mapToInt(LootEntry::getWeight).sum();
        if (total <= 0) return null;
        int r = random.nextInt(total);
        int cur = 0;
        for (LootEntry e : entries) {
            cur += e.getWeight();
            if (r < cur) return e;
        }
        return entries.get(entries.size() - 1);
    }
}

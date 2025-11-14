package xyz.kaijiieow.airdrop.loot;

public class LootEntry {
    public enum Type { VANILLA, MMOITEMS }

    private final String id;
    private final Type type;
    private final String materialOrType;
    private final String mmoId;
    private final int minAmount;
    private final int maxAmount;
    private final int weight;

    public LootEntry(String id, Type type, String materialOrType, String mmoId,
                     int minAmount, int maxAmount, int weight) {
        this.id = id;
        this.type = type;
        this.materialOrType = materialOrType;
        this.mmoId = mmoId;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.weight = weight;
    }

    public String getId() { return id; }
    public Type getType() { return type; }
    public String getMaterialOrType() { return materialOrType; }
    public String getMmoId() { return mmoId; }
    public int getMinAmount() { return minAmount; }
    public int getMaxAmount() { return maxAmount; }
    public int getWeight() { return weight; }
}

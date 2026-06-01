package arenamirror.data;

public class MetaUpgradeConfig {
    public MetaUpgradeType upgradeType;
    public String displayName;
    public String description;
    public int maxLevel;
    public MetaUpgradeLevel[] levels;

    public MetaUpgradeConfig() {}

    public MetaUpgradeConfig(MetaUpgradeType type, String name, int maxLevel, MetaUpgradeLevel[] levels) {
        this.upgradeType = type;
        this.displayName = name;
        this.maxLevel = maxLevel;
        this.levels = levels;
    }

    public float getValueAtLevel(int level) {
        if (level <= 0 || levels == null) return 0f;
        int index = Math.max(0, Math.min(levels.length - 1, level - 1));
        return levels[index].value;
    }

    public int getCostForLevel(int level) {
        if (level <= 0 || levels == null) return 0;
        int index = Math.max(0, Math.min(levels.length - 1, level - 1));
        return levels[index].cost;
    }
}

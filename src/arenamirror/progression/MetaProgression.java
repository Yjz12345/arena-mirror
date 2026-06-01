package arenamirror.progression;

import arenamirror.data.*;
import arenamirror.player.*;
import java.util.*;

public class MetaProgression {
    public static MetaProgression instance;

    public Map<MetaUpgradeType, Integer> upgradeLevels = new HashMap<>();
    public Map<MetaUpgradeType, MetaUpgradeConfig> configs = new HashMap<>();
    public int currency;

    public List<CharacterData> unlockedCharacters = new ArrayList<>();
    public List<WeaponData> unlockedWeapons = new ArrayList<>();
    public List<SkillData> unlockedSkills = new ArrayList<>();

    public MetaProgression() {
        instance = this;
        initConfigs();
    }

    private void initConfigs() {
        configs.put(MetaUpgradeType.INITIAL_HP, new MetaUpgradeConfig(
            MetaUpgradeType.INITIAL_HP, "初始生命", 3,
            new MetaUpgradeLevel[]{
                new MetaUpgradeLevel(10, 5),
                new MetaUpgradeLevel(30, 10),
                new MetaUpgradeLevel(60, 15)
            }
        ));
        configs.put(MetaUpgradeType.INITIAL_ATTACK, new MetaUpgradeConfig(
            MetaUpgradeType.INITIAL_ATTACK, "初始攻击", 5,
            new MetaUpgradeLevel[]{
                new MetaUpgradeLevel(10, 2),
                new MetaUpgradeLevel(25, 4),
                new MetaUpgradeLevel(50, 6),
                new MetaUpgradeLevel(80, 8),
                new MetaUpgradeLevel(120, 10)
            }
        ));
        configs.put(MetaUpgradeType.POST_BATTLE_HEAL, new MetaUpgradeConfig(
            MetaUpgradeType.POST_BATTLE_HEAL, "战后回血", 3,
            new MetaUpgradeLevel[]{
                new MetaUpgradeLevel(20, 0.05f),
                new MetaUpgradeLevel(40, 0.10f),
                new MetaUpgradeLevel(70, 0.15f)
            }
        ));
        configs.put(MetaUpgradeType.REROLL_COUNT, new MetaUpgradeConfig(
            MetaUpgradeType.REROLL_COUNT, "重roll次数", 3,
            new MetaUpgradeLevel[]{
                new MetaUpgradeLevel(30, 1),
                new MetaUpgradeLevel(60, 2),
                new MetaUpgradeLevel(100, 3)
            }
        ));
        configs.put(MetaUpgradeType.REVIVAL, new MetaUpgradeConfig(
            MetaUpgradeType.REVIVAL, "复活机会", 1,
            new MetaUpgradeLevel[]{
                new MetaUpgradeLevel(150, 1)
            }
        ));

        for (MetaUpgradeType t : MetaUpgradeType.values()) {
            upgradeLevels.put(t, 0);
        }
    }

    public boolean purchaseUpgrade(MetaUpgradeType type) {
        MetaUpgradeConfig config = configs.get(type);
        if (config == null) return false;

        int currentLevel = upgradeLevels.getOrDefault(type, 0);
        if (currentLevel >= config.maxLevel) return false;

        int cost = config.getCostForLevel(currentLevel + 1);
        if (currency < cost) return false;

        currency -= cost;
        upgradeLevels.put(type, currentLevel + 1);
        applyUpgradeEffect(type, currentLevel + 1);
        return true;
    }

    private void applyUpgradeEffect(MetaUpgradeType type, int level) {
        MetaUpgradeConfig config = configs.get(type);
        if (config == null) return;
        float value = config.getValueAtLevel(level);
        PlayerStats stats = PlayerStats.instance;

        switch (type) {
            case INITIAL_HP: stats.metaHpBonus = value; break;
            case INITIAL_ATTACK: stats.metaAttackBonus = value; break;
            case POST_BATTLE_HEAL: stats.metaHealPercent = value; break;
            case REROLL_COUNT: stats.metaRerollCount = (int)value; break;
            case RARE_SKILL_CHANCE: stats.metaRareChanceBonus = value; break;
            case REVIVAL: stats.metaRevivalAvailable = true; break;
            case RESOURCE_GAIN: stats.metaResourceBonus = value; break;
        }
    }

    public void addCurrency(int amount) {
        currency += amount;
    }

    public int getUpgradeLevel(MetaUpgradeType type) {
        return upgradeLevels.getOrDefault(type, 0);
    }
}

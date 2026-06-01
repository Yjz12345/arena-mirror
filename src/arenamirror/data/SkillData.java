package arenamirror.data;

public class SkillData {
    public String skillName;
    public String description;
    public SkillRarity rarity;
    public boolean isActive;
    public SkillCategory category;
    public String conflictGroupId;
    public String conflictSubGroupId;

    // stat bonuses
    public float maxHpBonus;
    public float attackBonus;
    public float speedBonus;
    public float defenseBonus;
    public float critChanceBonus;
    public float critDamageBonus;
    public float pickupRangeBonus;

    // upgrade
    public int maxLevel = 1;
    public String upgradeType = "numeric"; // "numeric"=数值多次升级, "mechanism"=机制一次升满

    // active skill
    public float cooldown;
    public boolean hasTelegraph;
    public float telegraphDuration = 0.5f;

    public SkillData() {}

    public SkillData(String name, String desc, SkillRarity rarity, boolean isActive, SkillCategory category) {
        this.skillName = name;
        this.description = desc;
        this.rarity = rarity;
        this.isActive = isActive;
        this.category = category;
    }

    public boolean conflictsWith(SkillData other) {
        if (conflictGroupId == null || conflictGroupId.isEmpty()) return false;
        if (other.conflictGroupId == null || other.conflictGroupId.isEmpty()) return false;
        if (!conflictGroupId.equals(other.conflictGroupId)) return false;

        if (conflictSubGroupId == null || conflictSubGroupId.isEmpty() ||
            other.conflictSubGroupId == null || other.conflictSubGroupId.isEmpty())
            return true;

        return conflictSubGroupId.equals(other.conflictSubGroupId);
    }

    @Override public String toString() { return skillName; }
}

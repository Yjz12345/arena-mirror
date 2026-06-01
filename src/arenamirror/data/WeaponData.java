package arenamirror.data;

import java.util.List;
import java.util.ArrayList;

public class WeaponData {
    public String weaponName;
    public String description;
    public WeaponType weaponType;
    public AttackPattern attackPattern;
    public float baseDamage = 10f;
    public float attackSpeed = 1f;
    public float attackRange = 2f;
    public int comboMax = 3;

    public SkillData rightClickSpecial;   // 右键武器特攻
    public float specialCooldown = 3f;

    public List<SkillData> exclusiveSkills = new ArrayList<>(); // 专属技能池（约5个）

    public WeaponData() {}

    public WeaponData(String name, WeaponType type, AttackPattern pattern, float damage, float speed, float range) {
        this.weaponName = name;
        this.weaponType = type;
        this.attackPattern = pattern;
        this.baseDamage = damage;
        this.attackSpeed = speed;
        this.attackRange = range;
    }

    @Override public String toString() { return weaponName; }
}

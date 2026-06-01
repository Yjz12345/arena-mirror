package arenamirror.data;

import java.util.List;
import java.util.ArrayList;

public class CharacterData {
    public String characterName;
    public String description;
    public String talentDescription;

    public float baseMaxHp = 100f;
    public float baseAttack = 10f;
    public float baseSpeed = 5f;
    public float baseDefense = 0f;
    public float baseCritChance = 0.05f;
    public float baseCritDamage = 1.5f;
    public int baseDashCharges = 1;
    public float baseDashDistance = 3f;

    public SkillData qUltimate;       // Q大招
    public SkillData passiveSkill;    // 角色被动（始终生效）
    public List<WeaponType> allowedWeapons;
    public int unlockCost;

    public CharacterData() {}

    public CharacterData(String name, float hp, float atk, float spd) {
        this.characterName = name;
        this.baseMaxHp = hp;
        this.baseAttack = atk;
        this.baseSpeed = spd;
    }

    @Override public String toString() { return characterName; }
}

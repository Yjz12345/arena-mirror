package arenamirror.player;

import arenamirror.data.*;
import arenamirror.core.*;

public class PlayerStats {
    public static PlayerStats instance;

    public CharacterData currentCharacter;
    public WeaponData currentWeapon;

    public float maxHp = 100f;
    public float currentHp = 100f;
    public float attack = 10f;
    public float moveSpeed = 5f;
    public float defense = 0f;
    public float critChance = 0.05f;
    public float critDamage = 1.5f;
    public float pickupRange = 1f;

    // Raw (pre-multiplier) bases to prevent rebuildMods accumulation
    public float rawMaxHp, rawAttack, rawSpeed, rawDefense;

    // meta bonuses
    public float metaHpBonus;
    public float metaAttackBonus;
    public float metaCritBonus;
    public float metaHealPercent;
    public int metaRerollCount;
    public float metaRareChanceBonus;
    public boolean metaRevivalAvailable;
    public boolean metaRevivalUsed;
    public float metaResourceBonus;

    public PlayerStats() {
        instance = this;
        currentCharacter = createDefaultCharacter();
        currentWeapon = createDefaultWeapon();
    }

    public void initForNewRun(CharacterData character, WeaponData weapon) {
        currentCharacter = character != null ? character : createDefaultCharacter();
        currentWeapon = weapon != null ? weapon : createDefaultWeapon();

        maxHp = currentCharacter.baseMaxHp + metaHpBonus;
        attack = currentCharacter.baseAttack + metaAttackBonus;
        moveSpeed = currentCharacter.baseSpeed;
        defense = currentCharacter.baseDefense;
        critChance = currentCharacter.baseCritChance + metaCritBonus;
        critDamage = currentCharacter.baseCritDamage;

        currentHp = maxHp;
        pickupRange = 1f;
        metaRevivalUsed = false;

        // Save raw bases (without multipliers) to prevent rebuildMods accumulation
        rawMaxHp = maxHp;
        rawAttack = attack;
        rawSpeed = moveSpeed;
        rawDefense = defense;

        // Sync to player controller
        if (PlayerController.instance != null) {
            PlayerController.instance.moveSpeed = moveSpeed;
            PlayerController.instance.attackDamage = attack;
            PlayerController.instance.maxDashCharges = currentCharacter.baseDashCharges;
            PlayerController.instance.currentDashCharges = currentCharacter.baseDashCharges;
            PlayerController.instance.attackRange = currentWeapon.attackRange;
            PlayerController.instance.baseAttackRange = currentWeapon.attackRange;
            PlayerController.instance.attackSpeed = currentWeapon.attackSpeed;

            // Set Q and right-click skills from character/weapon
            // Set Q and right-click skills from character/weapon
            PlayerController.instance.qUltimate = currentCharacter.qUltimate;
            PlayerController.instance.qCooldown = 0;
            PlayerController.instance.rightClickSpecial = currentWeapon.rightClickSpecial;
            PlayerController.instance.rightClickCooldown = 0;
            PlayerController.instance.eUniversal = null;
            PlayerController.instance.eCooldown = 0;
            // Register in skill handler so they show as "owned"
            if (currentCharacter.qUltimate != null)
                PlayerSkillHandler.instance.allSkills.add(new SkillInstance(currentCharacter.qUltimate));
            if (currentWeapon.rightClickSpecial != null)
                PlayerSkillHandler.instance.allSkills.add(new SkillInstance(currentWeapon.rightClickSpecial));
        }
    }

    public void takeDamage(float rawDamage) {
        float reduced = rawDamage * (1f - defense / (defense + 100f));
        reduced = Math.max(1f, reduced);
        currentHp = Math.max(0, currentHp - reduced);

        if (currentHp <= 0f) {
            if (metaRevivalAvailable && !metaRevivalUsed) {
                metaRevivalUsed = true;
                currentHp = maxHp * 0.5f;
                return;
            }
            currentHp = 0f;
            GameManager.instance.battleManager.onPlayerKilled();
        }
    }

    public void heal(float amount) {
        currentHp = Math.min(maxHp, currentHp + amount);
    }

    public void healPercent(float percent) {
        heal(maxHp * percent);
    }

    public void applySkillBonus(SkillData skill) {
        rawMaxHp += skill.maxHpBonus;
        rawAttack += skill.attackBonus;
        rawSpeed += skill.speedBonus;
        defense += skill.defenseBonus;
        rawDefense += skill.defenseBonus;
        critChance += skill.critChanceBonus;
        critDamage += skill.critDamageBonus;
        pickupRange += skill.pickupRangeBonus;
    }

    public void removeSkillBonus(SkillData skill) {
        rawMaxHp -= skill.maxHpBonus;
        rawAttack -= skill.attackBonus;
        rawSpeed -= skill.speedBonus;
        defense -= skill.defenseBonus;
        rawDefense -= skill.defenseBonus;
        critChance -= skill.critChanceBonus;
        critDamage -= skill.critDamageBonus;
        pickupRange -= skill.pickupRangeBonus;
    }

    private CharacterData createDefaultCharacter() {
        CharacterData c = new CharacterData("战士", 100f, 10f, 4.8f);
        c.description = "初始角色，均衡属性";
        c.qUltimate = new SkillData("战吼", "怒吼提升攻击力50%，持续3秒", SkillRarity.UNCOMMON, true, SkillCategory.Q_ULTIMATE);
        c.qUltimate.cooldown = 12f;
        c.passiveSkill = new SkillData("坚韧", "生命低于30%时防御+20", SkillRarity.COMMON, false, SkillCategory.CHARACTER_PASSIVE);
        c.passiveSkill.defenseBonus = 20;
        return c;
    }

    private WeaponData createDefaultWeapon() {
        WeaponData w = new WeaponData("短剑", WeaponType.SWORD, AttackPattern.MELEE_SLASH, 6f, 4.0f, 70f);
        w.rightClickSpecial = new SkillData("重斩", "蓄力重击，造成2倍伤害", SkillRarity.COMMON, true, SkillCategory.WEAPON_SPECIAL);
        w.rightClickSpecial.cooldown = 3f;
        w.specialCooldown = 3f;
        return w;
    }
}

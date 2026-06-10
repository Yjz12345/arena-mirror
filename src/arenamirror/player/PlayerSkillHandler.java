package arenamirror.player;

import arenamirror.data.*;
import java.util.*;

public class PlayerSkillHandler {
    public static PlayerSkillHandler instance;

    public List<SkillInstance> allSkills = new ArrayList<>();
    private List<SkillInstance> passiveSkills = new ArrayList<>();

    public float attackMultiplier = 1f, hpMultiplier = 1f, speedMultiplier = 1f;
    public float lifestealPercent = 0f, cooldownReduction = 0f, reflectPercent = 0f, rangeBonus = 0f;

    public PlayerSkillHandler() { instance = this; }

    private SkillInstance si(SkillData d) { return new SkillInstance(d); }

    public SkillInstance findConflictingSkill(SkillData skillData) {
        PlayerController pc = PlayerController.instance;
        if (pc == null) return null;
        SkillCategory cat = skillData.category;
        if (cat == SkillCategory.WEAPON_ENCHANT) {
            int filled = 0; for (int i = 0; i < pc.enchantSlots; i++) if (pc.enchantElements[i] != null) filled++;
            if (filled >= pc.enchantSlots) return findInAllByCategory(SkillCategory.WEAPON_ENCHANT);
            return null;
        }
        if (cat == SkillCategory.DASH_EFFECT) {
            int filled = 0; for (int i = 0; i < pc.dashSlots; i++) if (pc.dashEffects[i] != null) filled++;
            if (filled >= pc.dashSlots) return findInAllByCategory(SkillCategory.DASH_EFFECT);
            return null;
        }
        if (cat == SkillCategory.DASH_PROPERTY) {
            if (skillData.conflictSubGroupId == null) return null;
            if ("extra_charge".equals(skillData.conflictSubGroupId) && pc.extraDashChargesSkill > 0)
                return si(new SkillData("额外","",SkillRarity.COMMON,false,SkillCategory.DASH_PROPERTY));
            if ("distance".equals(skillData.conflictSubGroupId) && pc.dashDistanceMultiplier > 1f)
                return si(new SkillData("距离","",SkillRarity.COMMON,false,SkillCategory.DASH_PROPERTY));
            return null;
        }
        if (cat == SkillCategory.E_UNIVERSAL) return pc.eUniversal != null ? si(pc.eUniversal) : null;
        if (cat == SkillCategory.Q_ULTIMATE) return pc.qUltimate != null ? si(pc.qUltimate) : null;
        if (cat == SkillCategory.WEAPON_SPECIAL) return pc.rightClickSpecial != null ? si(pc.rightClickSpecial) : null;
        for (SkillInstance s : allSkills) if (s.data.conflictsWith(skillData)) return s;
        return null;
    }

    public boolean tryAcquireSkill(SkillData sd) {
        if (findConflictingSkill(sd) != null) return false;
        addInternal(sd);
        return true;
    }

    public void replaceSkill(SkillInstance old, SkillData nu) {
        SkillInstance r = findInAll(old.data);
        if (r != null) {
            allSkills.remove(r);
            if (r.data.category == SkillCategory.STAT_MODIFIER || r.data.category == SkillCategory.CHARACTER_PASSIVE)
            { passiveSkills.remove(r); PlayerStats.instance.removeSkillBonus(r.data); }
        }
        unapply(old.data);
        addInternal(nu);
    }

    private void addInternal(SkillData sd) {
        SkillInstance inst = new SkillInstance(sd);
        allSkills.add(inst);
        if (sd.category == SkillCategory.STAT_MODIFIER || sd.category == SkillCategory.CHARACTER_PASSIVE)
        { passiveSkills.add(inst); PlayerStats.instance.applySkillBonus(sd); }
        apply(sd, true, 1);
    }

    // ── apply / unapply ──
    private void apply(SkillData sd, boolean on, int level) {
        PlayerController pc = PlayerController.instance;
        if (pc == null) return;
        String n = sd.skillName;
        SkillCategory cat = sd.category;

        if (cat == SkillCategory.WEAPON_ENCHANT) {
            if (on) {
                String e = n.contains("火焰")?"fire":n.contains("冰冻")?"ice":"lightning";
                float d = n.contains("火焰")?5:n.contains("冰冻")?3:8;
                for(int i=0;i<pc.enchantSlots;i++) if(pc.enchantElements[i]==null){pc.enchantElements[i]=e;pc.enchantDamages[i]=d;break;}
            } else {
                for(int i=0;i<3;i++) if(matchE(pc,i,n)){pc.enchantElements[i]=null;pc.enchantDamages[i]=0;break;}
            }
        } else if (cat == SkillCategory.DASH_EFFECT) {
            if (on) {
                String e = n.contains("烈焰")?"fire_trail":"shield";
                for(int i=0;i<pc.dashSlots;i++) if(pc.dashEffects[i]==null){pc.dashEffects[i]=e;break;}
            } else {
                String e = n.contains("烈焰")?"fire_trail":"shield";
                for(int i=0;i<3;i++) if(e.equals(pc.dashEffects[i])){pc.dashEffects[i]=null;break;}
            }
        } else if (cat == SkillCategory.DASH_PROPERTY) {
            if ("extra_charge".equals(sd.conflictSubGroupId)) pc.extraDashChargesSkill = on ? level : 0;
            else if ("distance".equals(sd.conflictSubGroupId)) pc.dashDistanceMultiplier = on ? (0.5f+0.5f*level) : 1f;
        } else if (cat == SkillCategory.E_UNIVERSAL) { pc.eUniversal = on ? sd : null; pc.eCooldown = 0; }
        else if (cat == SkillCategory.WEAPON_SPECIAL) {
            pc.rightClickSpecial = on ? sd : null; pc.rightClickCooldown = 0;
            if ("附魔精通".equals(n)) pc.enchantSlots = on ? Math.min(1+level, 3) : 1;
            if ("冲刺大师".equals(n)) pc.dashSlots = on ? Math.min(1+level, 3) : 1;
        } else if (cat == SkillCategory.Q_ULTIMATE) { pc.qUltimate = on ? sd : null; pc.qCooldown = 0; }
        pc.applySkillModifiers();
        rebuildMods();
    }
    private void unapply(SkillData sd) { apply(sd, false, 1); }
    private boolean matchE(PlayerController pc, int s, String n) {
        String e = pc.enchantElements[s];
        return e != null && ((e.equals("fire")&&n.contains("火焰"))||(e.equals("ice")&&n.contains("冰冻"))||(e.equals("lightning")&&n.contains("雷电")));
    }

    // ── rebuild multipliers ──
    public void rebuildMods() {
        PlayerController pc = PlayerController.instance;
        PlayerStats s = PlayerStats.instance;
        if (pc == null || s == null) return;
        attackMultiplier=1f; hpMultiplier=1f; speedMultiplier=1f;
        lifestealPercent=0f; cooldownReduction=0f; reflectPercent=0f; rangeBonus=0f;
        for (SkillInstance inst : allSkills) {
            String n = inst.data.skillName; if (n == null) continue;
            int lv = inst.currentLevel;
            if (n.contains("攻击倍率")) attackMultiplier = 1f + 0.2f * lv;
            else if (n.contains("生命倍率")) hpMultiplier = 1f + 0.15f * lv;
            else if (n.contains("速度倍率")) speedMultiplier = 1f + 0.1f * lv;
            else if (n.contains("吸血")&&!n.contains("打击")) lifestealPercent = 0.1f * lv;
            else if (n.contains("冷却")) cooldownReduction = 0.2f * lv;
            else if (n.contains("范围")) rangeBonus = 15f * lv;
            else if (n.contains("暴击强化")) s.critChance += 0.15f * lv;
            else if (n.contains("暴击伤害")) s.critDamage += 0.5f * lv;
            else if (n.contains("防御精通")) { s.rawDefense += 15 * lv; s.defense = s.rawDefense; }
            else if (n.contains("荆棘")) reflectPercent = 0.2f * lv;
        }
        // Always recalculate from raw bases to prevent multiplicative accumulation
        s.attack = s.rawAttack * attackMultiplier;
        s.maxHp = s.rawMaxHp * hpMultiplier;
        s.moveSpeed = s.rawSpeed * speedMultiplier;
        pc.attackDamage = s.attack;
        pc.moveSpeed = s.moveSpeed * speedMultiplier;
        pc.attackRange = pc.baseAttackRange + rangeBonus;
        // Clamp current HP to max
        if (s.currentHp > s.maxHp) s.currentHp = s.maxHp;
    }

    // ── upgrade ──
    public boolean tryUpgradeSkill(SkillData sd) {
        SkillInstance inst = findInAll(sd);
        if (inst == null || inst.currentLevel >= sd.maxLevel) return false;
        inst.currentLevel++;
        int lv = inst.currentLevel;
        if (sd.category == SkillCategory.STAT_MODIFIER || sd.category == SkillCategory.CHARACTER_PASSIVE)
            PlayerStats.instance.applySkillBonus(sd);
        apply(sd, true, lv);
        rebuildMods();
        return true;
    }

    public int getSkillLevel(SkillData sd) {
        SkillInstance inst = findInAll(sd);
        return inst != null ? inst.currentLevel : 0;
    }

    private SkillInstance findInAll(SkillData sd) {
        for (SkillInstance s : allSkills) if (s.data == sd || s.data.skillName.equals(sd.skillName)) return s;
        return null;
    }

    private SkillInstance findInAllByCategory(SkillCategory cat) {
        for (SkillInstance s : allSkills) if (s.data.category == cat) return s;
        return null;
    }

    public SkillInstance getSkillInstance(SkillData sd) { return findInAll(sd); }

    public List<SkillData> getAllSkills() {
        List<SkillData> r = new ArrayList<>();
        for (SkillInstance s : allSkills) r.add(s.data);
        return r;
    }

    public List<String> getSkillNames() {
        List<String> r = new ArrayList<>();
        for (SkillData s : getAllSkills()) r.add(s.skillName);
        return r;
    }

    public List<SkillData> getUpgradeableSkills() {
        List<SkillData> r = new ArrayList<>();
        for (SkillInstance s : allSkills) if (s.currentLevel < s.data.maxLevel) r.add(s.data);
        return r;
    }

    public void clearAllSkills() {
        for (SkillInstance s : new ArrayList<>(allSkills)) {
            if (s.data.category == SkillCategory.STAT_MODIFIER) PlayerStats.instance.removeSkillBonus(s.data);
            apply(s.data, false, 1);
        }
        allSkills.clear(); passiveSkills.clear();
        rebuildMods();
        PlayerController.instance.resetForNewRun();
    }
}

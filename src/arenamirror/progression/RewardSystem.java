package arenamirror.progression;

import arenamirror.data.*;
import arenamirror.player.*;
import arenamirror.skills.*;
import arenamirror.core.*;
import java.util.*;

public class RewardSystem {
    public static RewardSystem instance;

    public int rerollsRemaining;
    public List<SkillData> currentSkillDraw;
    public float healPercent = 0.2f;

    public String[] statLabels;
    public float[] statValues;

    public RewardSystem() {
        instance = this;
    }

    public void initForNewRun() {
        rerollsRemaining = 1;
        currentSkillDraw = null;
    }

    public void initForLayer() {
        rerollsRemaining = 1 + (PlayerStats.instance != null ? PlayerStats.instance.metaRerollCount : 0);
        // [TEST] 无限重roll
        rerollsRemaining = 999;
    }

    public void applyHeal() {
        float bonus = PlayerStats.instance.metaHealPercent;
        PlayerStats.instance.healPercent(healPercent + bonus);
    }

    /** Generate stat upgrade options scaled by layer */
    public void generateStatOptions() {
        int layer = GameManager.instance != null ? GameManager.instance.currentLayer : 1;
        float scale = 1f + layer / 25f;

        statLabels = new String[] {
            "最大生命 +" + fmt(15 * scale),
            "攻击力 +" + fmt(3 * scale),
            "移动速度 +" + fmt(0.5f * scale),
            "防御 +" + fmt(3 * scale),
            "暴击率 +" + fmt(0.03f * scale)
        };
        statValues = new float[] {15*scale, 3*scale, 0.5f*scale, 3*scale, 0.03f*scale};
    }

    private String fmt(float v) { return String.format("%.1f", v); }

    public void applyStatUpgrade(int index) {
        PlayerStats stats = PlayerStats.instance;
        if (stats == null || statValues == null || index >= statValues.length) return;
        float val = statValues[index];
        switch (index) {
            case 0: stats.rawMaxHp += val; stats.maxHp = stats.rawMaxHp; stats.heal(val); break;
            case 1: stats.rawAttack += val; break;
            case 2: stats.rawSpeed += val; break;
            case 3: stats.rawDefense += val; break;
            case 4: stats.critChance += val; break;
        }
    }

    public List<SkillData> getUpgradeableSkills() {
        return PlayerSkillHandler.instance.getUpgradeableSkills();
    }

    public void upgradeChosenSkill(SkillData skill) {
        PlayerSkillHandler.instance.tryUpgradeSkill(skill);
    }

    public List<SkillData> drawSkills() {
        if (currentSkillDraw == null)
            currentSkillDraw = SkillManager.instance.drawSkills(3);
        return currentSkillDraw;
    }

    public void selectDrawnSkill(int index) {
        if (currentSkillDraw == null || index < 0 || index >= currentSkillDraw.size()) return;
        SkillData chosen = currentSkillDraw.get(index);
        PlayerSkillHandler handler = PlayerSkillHandler.instance;
        if (handler == null) return;
        if (!handler.tryAcquireSkill(chosen)) { /* conflict handled in UI */ }
        currentSkillDraw = null;
    }

    public void replaceConflictingSkill(SkillData newSkill, SkillInstance oldSkill) {
        PlayerSkillHandler.instance.replaceSkill(oldSkill, newSkill);
        currentSkillDraw = null;
    }

    public boolean rerollSkillDraw() {
        if (rerollsRemaining <= 0) return false;
        rerollsRemaining--;
        currentSkillDraw = null;
        drawSkills();
        return true;
    }

    public String getUpgradeTypeLabel(SkillData skill) {
        return "mechanism".equals(skill.upgradeType) ? "机制升级(一次升满)" : "数值升级(可多次)";
    }
}

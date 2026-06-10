package arenamirror.skills;

import arenamirror.data.*;
import arenamirror.player.*;
import java.util.*;

public class SkillManager {
    public static SkillManager instance;
    public List<SkillData> globalSkillPool = new ArrayList<>();
    public float commonWeight = 60f, uncommonWeight = 25f, rareWeight = 12f, legendaryWeight = 3f;

    public SkillManager() { instance = this; initDefaultSkills(); }

    private void initDefaultSkills() {
        // ── 10 被动技 ──
        addPassive("攻击倍率","攻击力×1.2倍",SkillRarity.RARE,3,"numeric");
        addPassive("生命倍率","最大生命×1.15倍",SkillRarity.RARE,3,"numeric");
        addPassive("速度倍率","移速×1.1倍",SkillRarity.UNCOMMON,3,"numeric");
        addPassive("吸血","所有攻击回复10%伤害",SkillRarity.RARE,2,"numeric");
        addPassive("暴击强化","暴击率+15%",SkillRarity.UNCOMMON,2,"numeric");
        addPassive("暴击伤害","暴击伤害+50%",SkillRarity.UNCOMMON,2,"numeric");
        addPassive("防御精通","防御力+15",SkillRarity.COMMON,3,"numeric");
        addPassive("范围扩大","攻击范围+15px",SkillRarity.UNCOMMON,3,"numeric");
        addPassive("冷却缩减","所有技能CD-20%",SkillRarity.RARE,2,"mechanism");
        addPassive("荆棘","被击中反弹20%伤害",SkillRarity.UNCOMMON,2,"numeric");

        // ── 附魔（数值类，默认1槽）──
        addEnchant("火焰附魔","攻击附带火焰伤害",SkillRarity.UNCOMMON);
        addEnchant("冰冻附魔","攻击附带减速",SkillRarity.UNCOMMON);
        addEnchant("雷电附魔","攻击附带连锁闪电",SkillRarity.RARE);

        // ── 冲刺效果（机制类 maxLevel=2）──
        SkillData d1 = new SkillData("烈焰冲刺","冲刺后留下火焰",SkillRarity.UNCOMMON,false,SkillCategory.DASH_EFFECT);
        d1.conflictGroupId="dash_effect"; d1.maxLevel=2; d1.upgradeType="mechanism"; globalSkillPool.add(d1);
        SkillData d2 = new SkillData("护盾冲刺","冲刺后产生护盾",SkillRarity.UNCOMMON,false,SkillCategory.DASH_EFFECT);
        d2.conflictGroupId="dash_effect"; d2.maxLevel=2; d2.upgradeType="mechanism"; globalSkillPool.add(d2);

        // ── 冲刺属性（数值类 maxLevel=3）──
        SkillData dd1 = new SkillData("额外冲刺","冲刺次数+1",SkillRarity.RARE,false,SkillCategory.DASH_PROPERTY);
        dd1.conflictGroupId="dash_property"; dd1.conflictSubGroupId="extra_charge"; dd1.maxLevel=3; dd1.upgradeType="numeric"; globalSkillPool.add(dd1);
        SkillData dd2 = new SkillData("远距冲刺","冲刺距离增加",SkillRarity.COMMON,false,SkillCategory.DASH_PROPERTY);
        dd2.conflictGroupId="dash_property"; dd2.conflictSubGroupId="distance"; dd2.maxLevel=3; dd2.upgradeType="numeric"; globalSkillPool.add(dd2);

        // ── Q 大招 ──
        addQ("旋风","3秒持续AoE,移速翻倍,无法攻击",SkillRarity.RARE,10f,3,"numeric");
        addQ("战吼","3秒内攻击+50%",SkillRarity.UNCOMMON,12f,3,"numeric");
        addQ("无敌","2.5秒无敌+护盾",SkillRarity.RARE,18f,2,"mechanism");
        addQ("嗜血","4秒内攻击+80%,吸血+30%",SkillRarity.RARE,15f,2,"numeric");
        addQ("雷暴","5秒内每0.8秒雷击范围敌人,高伤+短暂晕眩",SkillRarity.LEGENDARY,20f,3,"numeric");

        // ── E 通用技 ──
        addE("投射火球","追踪火球",SkillRarity.COMMON,2.5f,3,"numeric");
        addE("冰霜射击","冰箭缓速敌人",SkillRarity.UNCOMMON,3f,3,"numeric");
        addE("多重箭","散射追踪弹",SkillRarity.UNCOMMON,3.5f,2,"mechanism");
        addE("闪电链","闪电追踪弹",SkillRarity.RARE,4f,3,"numeric");
        addE("毒雾","范围毒雾",SkillRarity.UNCOMMON,5f,2,"mechanism");
        addE("震荡波","范围击退",SkillRarity.UNCOMMON,5f,3,"numeric");
        addE("紧急治疗","回复30%HP",SkillRarity.RARE,15f,2,"mechanism");
        addE("地震打击","范围伤害+缓速",SkillRarity.UNCOMMON,4.5f,3,"numeric");
        addE("吸血打击","伤害+回血50%",SkillRarity.UNCOMMON,4f,3,"numeric");

        // ── 右键武器技 ──
        addWS("剑刃回旋","圆形范围斩击",SkillRarity.UNCOMMON,3f,3,"numeric");
        addWS("蓄力突刺","直线突进高额伤害",SkillRarity.RARE,4f,2,"mechanism");
        addWS("连斩","快速斩击",SkillRarity.UNCOMMON,2.5f,2,"mechanism");
        addWS("冲锋","高速冲向敌人",SkillRarity.COMMON,2f,3,"numeric");
        addWS("附魔精通","牺牲右键,解锁额外附魔槽",SkillRarity.RARE,0f,2,"mechanism");
        addWS("冲刺大师","牺牲右键,解锁额外冲刺效果槽",SkillRarity.RARE,0f,2,"mechanism");
    }

    private void addPassive(String n,String d,SkillRarity r,int ml,String ut){ SkillData s=new SkillData(n,d,r,false,SkillCategory.STAT_MODIFIER); s.maxLevel=ml; s.upgradeType=ut; globalSkillPool.add(s); }
    private void addEnchant(String n,String d,SkillRarity r){ SkillData s=new SkillData(n,d,r,false,SkillCategory.WEAPON_ENCHANT); s.conflictGroupId="weapon_enchant"; s.maxLevel=3; s.upgradeType="numeric"; globalSkillPool.add(s); }
    private void addQ(String n,String d,SkillRarity r,float cd,int ml,String ut){ SkillData s=new SkillData(n,d,r,true,SkillCategory.Q_ULTIMATE); s.cooldown=cd; s.maxLevel=ml; s.upgradeType=ut; globalSkillPool.add(s); }
    private void addE(String n,String d,SkillRarity r,float cd,int ml,String ut){ SkillData s=new SkillData(n,d,r,true,SkillCategory.E_UNIVERSAL); s.cooldown=cd; s.maxLevel=ml; s.upgradeType=ut; globalSkillPool.add(s); }
    private void addWS(String n,String d,SkillRarity r,float cd,int ml,String ut){ SkillData s=new SkillData(n,d,r,true,SkillCategory.WEAPON_SPECIAL); s.cooldown=cd; s.maxLevel=ml; s.upgradeType=ut; globalSkillPool.add(s); }

    public List<SkillData> drawSkills(int count) {
        PlayerSkillHandler handler = PlayerSkillHandler.instance;
        PlayerStats stats = PlayerStats.instance;
        List<SkillData> available = new ArrayList<>();
        for (SkillData skill : globalSkillPool) {
            if (skill.category == SkillCategory.WEAPON_EXCLUSIVE) continue;
            SkillInstance inst = handler != null ? handler.getSkillInstance(skill) : null;
            // Stat modifier passives: never re-draw if already owned (upgrade via 升级)
            if (inst != null && skill.category == SkillCategory.STAT_MODIFIER) continue;
            // Skip active/slot skills already owned (must replace to reacquire)
            if (inst != null && skill.category != SkillCategory.STAT_MODIFIER) continue;
            available.add(skill);
        }
        return weightedRandomSelect(available, count);
    }

    private List<SkillData> weightedRandomSelect(List<SkillData> pool, int count) {
        List<SkillData> result = new ArrayList<>();
        List<SkillData> temp = new ArrayList<>(pool);
        Random rng = new Random();
        float rareBonus = PlayerStats.instance != null ? PlayerStats.instance.metaRareChanceBonus : 0f;
        for (int i = 0; i < count && !temp.isEmpty(); i++) {
            float totalWeight = 0f; float[] weights = new float[temp.size()];
            for (int j = 0; j < temp.size(); j++) {
                float w = getWeight(temp.get(j).rarity);
                if (temp.get(j).rarity.ordinal() >= SkillRarity.UNCOMMON.ordinal()) w += rareBonus;
                weights[j] = w; totalWeight += w;
            }
            float roll = rng.nextFloat() * totalWeight;
            float c = 0f; int sel = 0;
            for (int j = 0; j < temp.size(); j++) { c += weights[j]; if (roll <= c) { sel = j; break; } }
            result.add(temp.get(sel)); temp.remove(sel);
        }
        return result;
    }

    private float getWeight(SkillRarity r) {
        switch (r) { case COMMON: return commonWeight; case UNCOMMON: return uncommonWeight; case RARE: return rareWeight; case LEGENDARY: return legendaryWeight; default: return commonWeight; }
    }

    public void unlockSkill(SkillData skill) { if (!globalSkillPool.contains(skill)) globalSkillPool.add(skill); }
}

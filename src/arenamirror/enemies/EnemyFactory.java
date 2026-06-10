package arenamirror.enemies;

import arenamirror.data.*;
import java.util.*;

public class EnemyFactory {

    // 11 distinct enemy skills (merged from 20 duplicates)
    private static final String[] PRESET_SKILL_NAMES = {
        "投射火球", "冰霜射击", "多重箭", "闪电链",
        "冲锋", "地震践踏", "毒雾", "自爆",
        "吸血打击", "狂暴", "重型劈砍",
        "直线弹", "落石预警", "激光束"
    };

    public static EnemyTemplateData generatePresetEnemy(int layer) {
        Random rng = new Random(layer * 31 + 7);

        EnemyTemplateData template = new EnemyTemplateData();
        template.source = EnemySource.PRESET;
        template.race = EnemyRace.values()[rng.nextInt(EnemyRace.values().length)];
        template.behavior = BehaviorPattern.values()[rng.nextInt(BehaviorPattern.values().length)];
        template.statTendency = StatTendency.values()[rng.nextInt(StatTendency.values().length)];
        template.skills = new ArrayList<>();

        int skillCount = Math.min(layer / 5 + 2, 6);
        List<String> used = new ArrayList<>();
        for (int i = 0; i < skillCount; i++) {
            String name;
            int tries = 0;
            do {
                name = PRESET_SKILL_NAMES[rng.nextInt(PRESET_SKILL_NAMES.length)];
                tries++;
            } while (used.contains(name) && tries < 50);
            used.add(name);
            template.skills.add(createNamedSkill(name, rng, layer));
        }

        return template;
    }

    private static SkillData createNamedSkill(String name, Random rng, int layer) {
        SkillRarity rarity = SkillRarity.COMMON;
        double roll = rng.nextDouble();
        if (roll > 0.92) rarity = SkillRarity.LEGENDARY;
        else if (roll > 0.75) rarity = SkillRarity.RARE;
        else if (roll > 0.45) rarity = SkillRarity.UNCOMMON;

        SkillData skill = new SkillData();
        skill.skillName = name;
        skill.description = "预设敌人技能: " + name;
        skill.rarity = rarity;
        skill.isActive = true;
        skill.category = SkillCategory.ACTIVE_DAMAGE;
        skill.maxLevel = 1;

        switch (name) {
            case "投射火球": case "冰霜射击": case "多重箭": case "闪电链":
                skill.cooldown = 1.2f + rng.nextFloat() * 1f;
                break;
            case "冲锋":
                skill.cooldown = 1.5f + rng.nextFloat() * 1f;
                break;
            case "地震践踏": case "自爆":
                skill.cooldown = 2f + rng.nextFloat() * 1.5f;
                break;
            case "毒雾": case "狂暴":
                skill.cooldown = 3f + rng.nextFloat() * 2f;
                break;
            case "直线弹": case "激光束":
                skill.cooldown = 1f + rng.nextFloat() * 1f;
                break;
            case "落石预警":
                skill.cooldown = 1.5f + rng.nextFloat() * 1.5f;
                break;
            case "吸血打击":
                skill.cooldown = 1.5f + rng.nextFloat() * 1f;
                break;
            default: // 重型劈砍
                skill.cooldown = 0.8f + rng.nextFloat() * 1f;
        }

        return skill;
    }
}

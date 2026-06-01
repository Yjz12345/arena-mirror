package arenamirror.traps;

import arenamirror.data.*;
import arenamirror.rendering.Vec2;
import java.util.*;

public class TrapManager {
    public static TrapManager instance;

    public List<TrapTemplateData> trapTemplates = new ArrayList<>();
    public List<TrapSpawnEntry> activeTraps = new ArrayList<>();

    public TrapManager() {
        instance = this;
        initTemplates();
    }

    private void initTemplates() {
        // 10+ templates
        TrapTemplateData t1 = new TrapTemplateData("锯片+地刺");
        t1.matchTags.addAll(Arrays.asList("melee", "aggressive"));
        t1.trapEntries.add(new TrapSpawnEntry(TrapType.MOVING_SAW, new Vec2(0, -50), 0, 15f, false));
        t1.trapEntries.add(new TrapSpawnEntry(TrapType.SPIKE, new Vec2(80, 30), 2f, 10f, false));
        trapTemplates.add(t1);

        TrapTemplateData t2 = new TrapTemplateData("火焰+地刺");
        t2.matchTags.addAll(Arrays.asList("fire", "ranged"));
        t2.trapEntries.add(new TrapSpawnEntry(TrapType.FIRE, new Vec2(-30, -40), 3f, 12f, false));
        t2.trapEntries.add(new TrapSpawnEntry(TrapType.SPIKE, new Vec2(50, -70), 2f, 10f, false));
        trapTemplates.add(t2);

        TrapTemplateData t3 = new TrapTemplateData("仅有柱子");
        t3.matchTags.add("empty");
        t3.trapEntries.add(new TrapSpawnEntry(TrapType.SPIKE, new Vec2(0, 60), 0, 0, true)); // obstacle
        t3.trapEntries.add(new TrapSpawnEntry(TrapType.SPIKE, new Vec2(60, -30), 0, 0, true));
        trapTemplates.add(t3);

        TrapTemplateData t4 = new TrapTemplateData("空旷");
        trapTemplates.add(t4);
    }

    public TrapTemplateData selectTemplate(LayerSlot slot) {
        if (slot == null) return trapTemplates.get(trapTemplates.size() - 1);

        // Simple tag matching
        TrapTemplateData best = null;
        int bestScore = 0;

        for (TrapTemplateData template : trapTemplates) {
            if (template.matchTags.isEmpty()) continue;

            int score = 0;
            boolean excluded = false;

            for (String exTag : template.excludeTags) {
                if (hasMatchingTag(slot, exTag)) { excluded = true; break; }
            }
            if (excluded) continue;

            for (String tag : template.matchTags) {
                if (hasMatchingTag(slot, tag)) score++;
            }

            if (score > bestScore) {
                bestScore = score;
                best = template;
            }
        }

        return best != null ? best : trapTemplates.get(trapTemplates.size() - 1);
    }

    private boolean hasMatchingTag(LayerSlot slot, String tag) {
        if (slot.originalSkills == null) return false;
        for (SkillData skill : slot.originalSkills) {
            if (skill.skillName != null && skill.skillName.toLowerCase().contains(tag.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public void spawnTraps(LayerSlot slot) {
        activeTraps.clear();
        TrapTemplateData template = selectTemplate(slot);
        if (template != null && template.trapEntries != null) {
            activeTraps.addAll(template.trapEntries);
        }
    }

    public void clearTraps() {
        activeTraps.clear();
    }
}

package arenamirror.data;

import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;

public class EnemyTemplateData {
    public EnemySource source;
    public EnemyRace race;
    public BehaviorPattern behavior;
    public StatTendency statTendency;
    public List<SkillData> skills = new ArrayList<>();
    public String enemySpriteKey; // placeholder for rendering

    public EnemyTemplateData() {}

    public EnemyTemplateData(EnemySource source, EnemyRace race, BehaviorPattern behavior, StatTendency tendency) {
        this.source = source;
        this.race = race;
        this.behavior = behavior;
        this.statTendency = tendency;
    }
}

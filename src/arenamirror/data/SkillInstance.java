package arenamirror.data;

public class SkillInstance {
    public SkillData data;
    public int currentLevel = 1;
    public float cooldownRemaining;

    public SkillInstance(SkillData data) {
        this.data = data;
        this.currentLevel = 1;
    }
}

package arenamirror.data;

import java.util.Arrays;

public class LayerStatEntry {
    public int layer;
    public float hpMultiplier;
    public float attackMultiplier;
    public float defenseMultiplier;
    public float speedMultiplier;
    public float skillDamageMultiplier;

    public LayerStatEntry() {}

    public LayerStatEntry(int layer, float hp, float atk, float def, float spd) {
        this.layer = layer;
        this.hpMultiplier = hp;
        this.attackMultiplier = atk;
        this.defenseMultiplier = def;
        this.speedMultiplier = spd;
        this.skillDamageMultiplier = atk;
    }
}

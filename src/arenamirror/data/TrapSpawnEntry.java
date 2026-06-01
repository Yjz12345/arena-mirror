package arenamirror.data;

import arenamirror.rendering.Vec2;

public class TrapSpawnEntry {
    public TrapType trapType;
    public Vec2 position;
    public float activationInterval;
    public float damage;
    public boolean isObstacle;

    public TrapSpawnEntry() {}

    public TrapSpawnEntry(TrapType type, Vec2 pos, float interval, float dmg, boolean obstacle) {
        this.trapType = type;
        this.position = pos;
        this.activationInterval = interval;
        this.damage = dmg;
        this.isObstacle = obstacle;
    }
}

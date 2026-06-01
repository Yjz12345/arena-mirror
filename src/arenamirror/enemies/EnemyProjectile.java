package arenamirror.enemies;

import arenamirror.rendering.Vec2;

public class EnemyProjectile {
    public Vec2 position;
    public float lifetime;

    public EnemyProjectile(Vec2 pos, float life) {
        position = new Vec2(pos);
        lifetime = life;
    }
}

package arenamirror.enemies;

import arenamirror.rendering.Vec2;

public class EnemyProjectile {
    public Vec2 position;
    public float lifetime;
    public boolean homing = true;          // false = straight line
    public Vec2 velocity = new Vec2(0, 0); // used when !homing
    public int colorType;                  // 0=fire, 1=ice, 2=lightning, 3=laser, 4=straight, 5=poison

    public EnemyProjectile(Vec2 pos, float life) {
        position = new Vec2(pos);
        lifetime = life;
    }

    public EnemyProjectile(Vec2 pos, float life, Vec2 vel) {
        position = new Vec2(pos);
        lifetime = life;
        homing = false;
        velocity = vel;
    }

    public EnemyProjectile(Vec2 pos, float life, Vec2 vel, int type) {
        position = new Vec2(pos);
        lifetime = life;
        homing = false;
        velocity = vel;
        colorType = type;
    }

    public EnemyProjectile(Vec2 pos, float life, int type) {
        position = new Vec2(pos);
        lifetime = life;
        colorType = type;
    }
}
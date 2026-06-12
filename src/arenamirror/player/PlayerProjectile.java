package arenamirror.player;

import arenamirror.rendering.Vec2;

public class PlayerProjectile {
    public Vec2 position;
    public Vec2 velocity;
    public float damage;
    public float lifetime;
    public String element; // "fire", "ice", "lightning", etc.
    public boolean homing = true; // false for straight/laser projectiles

    public PlayerProjectile(Vec2 pos, Vec2 vel, float dmg, float life, String elem) {
        position = new Vec2(pos);
        velocity = vel;
        damage = dmg;
        lifetime = life;
        element = elem;
    }

    public PlayerProjectile(Vec2 pos, Vec2 vel, float dmg, float life, String elem, boolean homing) {
        this(pos, vel, dmg, life, elem);
        this.homing = homing;
    }

    public void update(float dt) {
        position = position.add(velocity.scale(dt));
        lifetime -= dt;
    }

    public boolean isExpired() { return lifetime <= 0; }
}
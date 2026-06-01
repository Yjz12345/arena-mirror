package arenamirror.enemies;

import arenamirror.core.GameManager;
import arenamirror.core.GameState;
import arenamirror.data.*;
import arenamirror.rendering.Vec2;
import java.util.*;

public class EnemyBase {
    public Vec2 position = new Vec2(400, 300);
    public Vec2 velocity = new Vec2(0, 0);

    public float maxHp;
    public float currentHp;
    public float attack;
    public float moveSpeed;
    public float defense;

    public EnemySource source;
    public int layerNumber;
    public int pastLifeId;
    public BehaviorPattern behavior;
    public List<SkillData> skills = new ArrayList<>();

    public boolean isDead;
    public float deathTimer;
    public Map<SkillData, Float> skillCooldowns = new HashMap<>();
    public float attackTimer;
    public float projectileTimer; // for ranged attacks

    public float slowTimer;
    public float slowAmount = 1f;

    // DOT effects
    public float burnTimer;
    public float burnDps;
    public int poisonStacks;
    public float poisonTickTimer;
    public float knockbackTimer;

    // self-buff timer (狂暴/荆棘) - prevents infinite stacking
    public float selfBuffTimer;
    public float baseAttack;   // saved for buff reset
    public float baseMoveSpeed;

    public boolean isTelegraphing;
    public float telegraphTimer;
    public SkillData queuedSkill;

    // hit effects
    public float hitFlashTimer;
    public float lastDamageTaken;
    public float hitStopTimer;

    // attack FX
    public float attackFxTimer;
    public Vec2 lastAttackDir = new Vec2(0, -1);

    public EnemyAI ai = new EnemyAI(this);

    // Visual projectile tracking (with lifetime)
    public List<EnemyProjectile> activeProjectiles = new ArrayList<>();

    public EnemyBase() {}

    public void initialize(LayerSlot slot, int layer) {
        source = slot.enemySource;
        layerNumber = layer;
        pastLifeId = slot.pastLifeId;
        skills = slot.originalSkills != null ? new ArrayList<>(slot.originalSkills) : new ArrayList<>();
        behavior = slot.templateData != null ? slot.templateData.behavior : BehaviorPattern.MELEE_AGGRESSIVE;

        // Standard base values — past life enemies scale to layer baseline, not player's raw stats
        float baseHp = 100f;
        float baseAtk = 8f;
        float baseSpd = 2f;
        float baseDef = 0f;

        LayerStatEntry ls = GameManager.instance.getLayerStats(layer);
        maxHp = baseHp * ls.hpMultiplier;
        attack = baseAtk * ls.attackMultiplier * 2f;
        moveSpeed = baseSpd * ls.speedMultiplier;
        defense = baseDef * ls.defenseMultiplier;
        currentHp = maxHp;
        baseAttack = attack;
        baseMoveSpeed = moveSpeed;

        for (SkillData s : skills) {
            if (s.isActive) skillCooldowns.put(s, (float)(Math.random() * s.cooldown * 0.5));
        }
    }

    public void update(float dt) {
        if (isDead) {
            deathTimer -= dt;
            if (deathTimer <= 0) GameManager.instance.battleManager.onEnemyKilled();
            return;
        }
        GameManager gm = GameManager.instance;
        if (gm == null || gm.currentState != GameState.BATTLE) return;

        // Cooldowns
        List<SkillData> keys = new ArrayList<>(skillCooldowns.keySet());
        for (SkillData s : keys) skillCooldowns.put(s, Math.max(0, skillCooldowns.get(s) - dt));

        attackTimer -= dt;
        projectileTimer -= dt;
        hitFlashTimer -= dt;
        attackFxTimer -= dt;

        if (slowTimer > 0) { slowTimer -= dt; if (slowTimer <= 0) slowAmount = 1f; }
        if (selfBuffTimer > 0) {
            selfBuffTimer -= dt;
            if (selfBuffTimer <= 0) {
                attack = baseAttack;
                moveSpeed = baseMoveSpeed;
            }
        }
        updateDots(dt);

        if (isTelegraphing) {
            telegraphTimer -= dt;
            if (telegraphTimer <= 0) { isTelegraphing = false; castSkill(queuedSkill); }
        }

        // Move projectiles toward player
        updateProjectiles(dt);

        position = position.add(velocity.scale(slowAmount).scale(dt * 60f));
        clampToArena();
        ai.update(dt);
    }

    private void updateProjectiles(float dt) {
        GameManager gm = GameManager.instance;
        if (gm == null || gm.player == null) return;

        for (int i = activeProjectiles.size() - 1; i >= 0; i--) {
            EnemyProjectile ep = activeProjectiles.get(i);
            ep.lifetime -= dt;
            Vec2 p = ep.position;
            Vec2 dir = gm.player.position.sub(p).normalized();
            p.x += dir.x * dt * 150f;
            p.y += dir.y * dt * 150f;
            if (p.distance(gm.player.position) < 15f) {
                gm.player.takeDamage(attack * 0.5f);
                activeProjectiles.remove(i);
            } else if (ep.lifetime <= 0 || p.distance(GameManager.ARENA_CENTER) > GameManager.ARENA_RADIUS + 60) {
                activeProjectiles.remove(i);
            }
        }
    }

    private void clampToArena() {
        Vec2 center = GameManager.ARENA_CENTER;
        float dist = position.distance(center);
        if (dist > GameManager.ARENA_RADIUS - 20) {
            Vec2 dir = position.sub(center).normalized();
            position = center.add(dir.scale(GameManager.ARENA_RADIUS - 20));
        }
    }

    public boolean tryUseSkill(SkillData skill) {
        if (!skill.isActive || isDead) return false;
        Float cd = skillCooldowns.get(skill);
        if (cd != null && cd > 0) return false;

        if (skill.hasTelegraph) {
            isTelegraphing = true; telegraphTimer = skill.telegraphDuration;
            queuedSkill = skill; skillCooldowns.put(skill, skill.cooldown);
            return true;
        }
        castSkill(skill);
        return true;
    }

    private void castSkill(SkillData skill) {
        skillCooldowns.put(skill, skill.cooldown);
        attackFxTimer = 0.3f;
        lastAttackDir = directionToPlayer();
        GameManager gm = GameManager.instance;
        if (gm == null || gm.player == null) return;

        String name = skill.skillName;
        float dist = position.distance(gm.player.position);

        // ── Linear projectile ──
        if (name.contains("直线弹")) {
            Vec2 dir = gm.player.position.sub(position).normalized();
            for (int i = 1; i <= 3; i++) {
                Vec2 off = new Vec2(-dir.y * (i-1) * 15, dir.x * (i-1) * 15);
                spawnProj(position.add(off), 2.5f);
            }
        }
        // ── Falling rock with telegraph ──
        else if (name.contains("落石")) {
            Vec2 target = gm.player.position;
            // Spawn rock projectile at target (deals heavy damage if not dodged)
            spawnProj(new Vec2(target.x, target.y - 120), 1.5f);
        }
        // ── Laser beam ──
        else if (name.contains("激光")) {
            Vec2 dir = gm.player.position.sub(position).normalized();
            // Multiple fast projectiles in a line
            for (int i = 0; i < 5; i++) {
                Vec2 off = position.add(dir.scale(i * 15));
                spawnProj(off, 2f);
            }
        }
        // ── Projectile attacks ──
        else if (name.contains("投射") || name.contains("射击") || name.contains("箭") || name.contains("弹") || name.contains("波") || name.contains("链")) {
            spawnProj(position, 3f);
            if (name.contains("多重")) {
                spawnProj(new Vec2(position.x + 15, position.y), 3f);
                spawnProj(new Vec2(position.x - 15, position.y), 3f);
            }
            if (name.contains("冰")) {
                float d = position.distance(gm.player.position);
                if (d < 120f) gm.player.takeDamage(attack * 0.3f);
            }
        }
        // ── Charge ──
        else if (name.contains("冲锋") || name.contains("突刺") || name.contains("暗影")) {
            Vec2 dir = gm.player.position.sub(position).normalized();
            velocity = dir.scale(moveSpeed * 6f);
            if (dist < 50f) gm.player.takeDamage(attack * 2f);
        }
        // ── AoE ──
        else if (name.contains("旋风") || name.contains("地震") || name.contains("践踏") || name.contains("旋转")) {
            if (dist < 80f) gm.player.takeDamage(attack * 1.8f);
        }
        // ── Self-buff ──
        else if (name.contains("狂暴") || name.contains("荆棘")) {
            attack = baseAttack * 1.3f;
            moveSpeed = baseMoveSpeed * 1.3f;
            selfBuffTimer = 3f;  // temporary buff, prevents infinite stacking
        }
        // ── Poison ──
        else if (name.contains("毒") || name.contains("雾")) {
            if (dist < 100f) gm.player.takeDamage(attack * 0.8f);
            spawnProj(new Vec2(position.x + 30, position.y), 4f);
            spawnProj(new Vec2(position.x - 30, position.y), 4f);
        }
        // ── Summon ──
        else if (name.contains("召唤")) {
            for (int i = 0; i < 3; i++) {
                Vec2 off = new Vec2((float)(Math.random()-0.5)*60, (float)(Math.random()-0.5)*60);
                spawnProj(position.add(off), 4f);
            }
        }
        // ── Self-destruct ──
        else if (name.contains("自爆")) {
            if (dist < 80f) gm.player.takeDamage(attack * 4f);
            takeDamage(maxHp * 0.5f);
        }
        // ── Vampire ──
        else if (name.contains("吸血")) {
            if (dist < 50f) {
                gm.player.takeDamage(attack * 1.5f);
                heal(attack * 0.5f);
            }
        }
        // ── Boomerang ──
        else if (name.contains("回旋")) {
            spawnProj(position, 3f);
        }
        // ── Default melee ──
        else {
            if (dist < 60f) gm.player.takeDamage(attack * 1.5f);
        }
    }

    private void heal(float amount) {
        currentHp = Math.min(maxHp, currentHp + amount);
    }

    private void updateDots(float dt) {
        // Burn: tick-based damage (0.2s interval), more satisfying than continuous
        if (burnTimer > 0) {
            burnTimer -= dt;
            // Tick every 0.2s
            if (((int)((burnTimer + 0.001f) / 0.2f)) != ((int)((burnTimer + dt + 0.001f) / 0.2f))) {
                currentHp -= burnDps * 0.2f;
                if (currentHp <= 0) die();
            }
        }
        // Poison: stacked ticks, one per 0.5s
        if (poisonStacks > 0) {
            poisonTickTimer -= dt;
            if (poisonTickTimer <= 0) {
                poisonTickTimer = 0.5f;
                currentHp -= poisonStacks * 2f;
                poisonStacks--;
                if (currentHp <= 0) die();
            }
        }
    }

    private void spawnProj(Vec2 pos, float life) {
        activeProjectiles.add(new EnemyProjectile(pos, life));
    }

    public void takeDamage(float rawDamage) {
        if (isDead) return;
        float reduced = rawDamage * (1f - defense / (defense + 100f));
        reduced = Math.max(1f, reduced);
        currentHp = Math.max(0, currentHp - reduced);
        lastDamageTaken = reduced;
        hitFlashTimer = 0.1f;
        if (currentHp <= 0f) die();
    }

    private void die() {
        if (isDead) return;
        isDead = true;
        deathTimer = 0.3f;
    }

    public Vec2 directionToPlayer() {
        GameManager gm = GameManager.instance;
        if (gm == null || gm.player == null) return new Vec2(0, 0);
        return gm.player.position.sub(position).normalized();
    }

    public float distanceToPlayer() {
        GameManager gm = GameManager.instance;
        if (gm == null || gm.player == null) return Float.MAX_VALUE;
        return position.distance(gm.player.position);
    }
}

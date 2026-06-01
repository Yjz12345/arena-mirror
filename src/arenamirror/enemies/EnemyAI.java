package arenamirror.enemies;

import arenamirror.core.GameManager;
import arenamirror.core.GameState;
import arenamirror.data.*;
import arenamirror.rendering.Vec2;

public class EnemyAI {
    private EnemyBase enemy;
    private BehaviorPattern behavior;
    private float decisionTimer;
    private float decisionInterval = 0.25f;
    private float skillUsageCap = 0.7f;

    private float approachDist = 50f;
    private float kitingDist = 150f;

    public EnemyAI(EnemyBase enemy) { this.enemy = enemy; }

    public void update(float dt) {
        if (enemy == null || enemy.isDead) return;
        GameManager gm = GameManager.instance;
        if (gm == null || gm.currentState != GameState.BATTLE) return;

        // Don't override knockback velocity
        if (enemy.knockbackTimer > 0) { enemy.knockbackTimer -= dt; return; }

        decisionTimer -= dt;
        if (decisionTimer > 0) return;
        decisionTimer = decisionInterval;

        behavior = enemy.behavior;
        switch (behavior) {
            case MELEE_AGGRESSIVE: meleeAggressive(); break;
            case RANGED_KITING: rangedKiting(); break;
            case SUMMONER: summoner(); break;
            case SELF_DESTRUCT: selfDestruct(); break;
            default: meleeAggressive(); break;
        }
    }

    private void meleeAggressive() {
        float dist = enemy.distanceToPlayer();
        if (dist > approachDist) {
            enemy.velocity = enemy.directionToPlayer().scale(enemy.moveSpeed * 1.3f);
        } else {
            tryUseRandomSkill();
            // Circle strafe + melee hit
            Vec2 dir = enemy.directionToPlayer();
            enemy.velocity = new Vec2(-dir.y, dir.x).scale(enemy.moveSpeed * 0.5f);
            // Melee attack: high contact damage
            if (dist < 30f && enemy.attackTimer <= 0) {
                GameManager.instance.player.takeDamage(enemy.attack * 1.2f);
                enemy.attackTimer = 0.45f;
                enemy.attackFxTimer = 0.25f;
                enemy.lastAttackDir = enemy.directionToPlayer();
            }
        }
    }

    private void rangedKiting() {
        float dist = enemy.distanceToPlayer();
        if (dist < kitingDist * 0.6f) {
            enemy.velocity = enemy.directionToPlayer().scale(-enemy.moveSpeed);
        } else if (dist > kitingDist * 1.3f) {
            enemy.velocity = enemy.directionToPlayer().scale(enemy.moveSpeed * 0.5f);
        } else {
            enemy.velocity = new Vec2(0, 0);
            // Fire projectile at player periodically
            if (enemy.projectileTimer <= 0) {
                fireProjectile();
                enemy.projectileTimer = 1.5f;
            }
            tryUseRandomSkill();
        }
    }

    private void summoner() {
        enemy.velocity = enemy.directionToPlayer().scale(-enemy.moveSpeed * 0.7f);
        // "Summon" = fire multiple projectiles
        if (enemy.projectileTimer <= 0) {
            for (int i = 0; i < 3; i++) {
                Vec2 offset = new Vec2((float)(Math.random() - 0.5) * 30, (float)(Math.random() - 0.5) * 30);
                enemy.activeProjectiles.add(new EnemyProjectile(enemy.position.add(offset), 4f));
            }
            enemy.projectileTimer = 3f;
        }
        tryUseRandomSkill();
    }

    private void selfDestruct() {
        float dist = enemy.distanceToPlayer();
        // Rush toward player
        enemy.velocity = enemy.directionToPlayer().scale(enemy.moveSpeed * 2f);
        // Explode on contact
        if (dist < 25f && enemy.attackTimer <= 0) {
            GameManager.instance.player.takeDamage(enemy.attack * 4f);
            enemy.takeDamage(enemy.maxHp);
            enemy.attackFxTimer = 0.4f;
            enemy.lastAttackDir = enemy.directionToPlayer();
        }
    }

    private void fireProjectile() {
        enemy.activeProjectiles.add(new EnemyProjectile(new Vec2(enemy.position), 3f));
    }

    private void tryUseRandomSkill() {
        if (enemy.skills == null || enemy.skills.isEmpty()) return;
        if (Math.random() > 0.35) return;

        int usableCount = (int)Math.ceil(enemy.skills.size() * skillUsageCap);
        for (int i = 0; i < Math.min(usableCount, 2); i++) {
            SkillData skill = enemy.skills.get((int)(Math.random() * enemy.skills.size()));
            if (skill.isActive) { enemy.tryUseSkill(skill); break; }
        }
    }
}

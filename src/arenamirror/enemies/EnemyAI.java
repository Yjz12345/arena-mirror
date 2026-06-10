package arenamirror.enemies;

import arenamirror.core.GameManager;
import arenamirror.core.GameState;
import arenamirror.data.*;
import arenamirror.rendering.Vec2;
import java.util.*;

public class EnemyAI {
    private EnemyBase enemy;
    private BehaviorPattern behavior;
    private float decisionTimer;
    private float decisionInterval = 0.18f;  // was 0.25
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
        decisionTimer = enemy.source == EnemySource.PAST_LIFE ? 0.12f : decisionInterval;

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
        float meleeRange = enemy.source == EnemySource.PAST_LIFE ? 40f : 30f;
        float appDist = enemy.source == EnemySource.PAST_LIFE ? 60f : approachDist;
        // Past life: always try skills regardless of distance
        if (enemy.source == EnemySource.PAST_LIFE) tryUseRandomSkill();
        if (dist > appDist) {
            enemy.velocity = enemy.directionToPlayer().scale(enemy.moveSpeed * 1.3f);
        } else {
            if (enemy.source != EnemySource.PAST_LIFE) tryUseRandomSkill();
            // Circle strafe + melee hit
            Vec2 dir = enemy.directionToPlayer();
            enemy.velocity = new Vec2(-dir.y, dir.x).scale(enemy.moveSpeed * 0.5f);
            // Melee attack: high contact damage
            if (dist < meleeRange && enemy.attackTimer <= 0) {
                GameManager.instance.player.takeDamage(enemy.attack * 1.2f);
                if (enemy.lifestealPct > 0) enemy.heal(enemy.attack * 1.2f * enemy.lifestealPct);
                enemy.attackTimer = 0.35f;
                enemy.attackFxTimer = 0.25f;
                enemy.lastAttackDir = enemy.directionToPlayer();
            }
        }
    }

    private void rangedKiting() {
        float dist = enemy.distanceToPlayer();
        float kDist = enemy.source == EnemySource.PAST_LIFE ? 180f : kitingDist;
        float fireRate = enemy.source == EnemySource.PAST_LIFE ? 0.8f : 1.5f;
        if (dist < kDist * 0.5f) {
            enemy.velocity = enemy.directionToPlayer().scale(-enemy.moveSpeed);
        } else if (dist > kDist * 1.5f) {
            enemy.velocity = enemy.directionToPlayer().scale(enemy.moveSpeed * 0.5f);
        } else {
            enemy.velocity = new Vec2(0, 0);
            // Past life: never fire generic projectile, only use actual skills
            if (enemy.source != EnemySource.PAST_LIFE && enemy.projectileTimer <= 0) {
                fireProjectile();
                enemy.projectileTimer = fireRate;
            }
            tryUseRandomSkill();
        }
    }

    private void summoner() {
        // Past life: don't use generic summon projectiles, use skills instead
        if (enemy.source == EnemySource.PAST_LIFE) {
            enemy.velocity = enemy.directionToPlayer().scale(-enemy.moveSpeed * 0.7f);
            tryUseRandomSkill();
            return;
        }
        enemy.velocity = enemy.directionToPlayer().scale(-enemy.moveSpeed * 0.7f);
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
        enemy.activeProjectiles.add(new EnemyProjectile(new Vec2(enemy.position), 3f, 0));
    }

    private void tryUseRandomSkill() {
        if (enemy.skills == null || enemy.skills.isEmpty()) return;
        // Normal enemies use skills more often too
        float chance = enemy.source == EnemySource.PAST_LIFE ? 0.85f : 0.45f;
        if (Math.random() > chance) return;

        if (enemy.source == EnemySource.PAST_LIFE) {
            // Try all active skills in random order (use Q/E/weapon slots)
            List<SkillData> shuffled = new ArrayList<>(enemy.skills);
            java.util.Collections.shuffle(shuffled);
            for (SkillData skill : shuffled) {
                if (skill.isActive) enemy.tryUseSkill(skill);
            }
        } else {
            int maxTries = Math.min(enemy.skills.size(), 3);
            for (int i = 0; i < maxTries; i++) {
                SkillData skill = enemy.skills.get((int)(Math.random() * enemy.skills.size()));
                if (skill.isActive) { enemy.tryUseSkill(skill); break; }
            }
        }
    }
}

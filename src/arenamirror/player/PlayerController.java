package arenamirror.player;

import arenamirror.core.GameManager;
import arenamirror.core.GameState;
import arenamirror.data.*;
import arenamirror.rendering.Vec2;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.*;

public class PlayerController {
    public static PlayerController instance;

    public Vec2 position = new Vec2(300, 300);
    public Vec2 velocity = new Vec2(0, 0);
    public Vec2 aimDirection = new Vec2(0, -1);
    public Vec2 mouseWorldPos = new Vec2(0, 0);

    public float moveSpeed = 5f;
    public float baseMoveSpeed = 5f;

    // ── dash ──
    public boolean isDashing;
    public boolean isInvincible;
    public int currentDashCharges = 1;
    public int maxDashCharges = 1;
    public float dashSpeed = 12f;
    public float dashDuration = 0.15f;
    public float dashCooldown = 1f;
    public float dashTimer;
    public float dashCooldownTimer;

    public int extraDashChargesSkill;
    public float dashDistanceMultiplier = 1f;
    // dash effect slots (1 default, up to 3 with 冲刺大师)
    public String[] dashEffects = new String[3];
    public int dashSlots = 1;
    public float shieldTimer;
    public Vec2 dashStartPos;
    public Vec2 dashDirection;  // saved dash direction for trail rendering (not mouse-dependent)
    // Fire trail ground effect (烈焰冲刺 Lv2+)
    public float fireTrailTimer;
    public Vec2 fireTrailStart;  // linear trail start
    public Vec2 fireTrailEnd;    // linear trail end
    public float fireTrailRadius = 50f;
    public float fireTrailDmgTimer;  // direct damage tick

    // hit effects
    public float hitFlashTimer;
    public float lastDamageTaken;
    public float hitStopTimer;

    // continuous AoE
    public float whirlTimer;
    public float whirlPulseTimer;

    // ── LMB normal attack ──
    public float attackTimer;
    public float attackSpeed = 1f;
    public float attackRange = 40f;
    public float baseAttackRange = 70f;
    public float attackDamage = 10f;
    public int comboCount;

    // ── weapon enchants (1 default, up to 3 with 附魔精通) ──
    public String[] enchantElements = new String[3];
    public float[] enchantDamages = new float[3];
    public int enchantSlots = 1;
    public boolean dualEnchantUnlocked;

    // ── Q, E, right-click slots ──
    public SkillData qUltimate;
    public float qCooldown;
    public float qBuffTimer;
    public SkillData eUniversal;
    public float eCooldown;
    public SkillData rightClickSpecial;
    public float rightClickCooldown;

    // FX
    public float qFxTimer, eFxTimer, rightClickFxTimer;
    public float rightClickSwingAngle;
    public String lastECastName;
    public String lastRCastName;
    public float swingAngle;
    public float whirlVisualTimer;

    // projectiles
    public List<PlayerProjectile> playerProjectiles = new ArrayList<>();

    private boolean qWasDown, eWasDown;

    public PlayerController() {
        instance = this;
        currentDashCharges = maxDashCharges;
    }

    public void resetForNewRun() {
        for(int i=0;i<3;i++){ enchantElements[i]=null; enchantDamages[i]=0; dashEffects[i]=null; }
        enchantSlots=1; dashSlots=1;
        extraDashChargesSkill = 0; dashDistanceMultiplier = 1f;
        maxDashCharges = 1 + extraDashChargesSkill;
        currentDashCharges = maxDashCharges;
        resetCombatState();
        playerProjectiles.clear();
        shieldTimer = 0; isDashing = false; isInvincible = false;
        lastECastName = null; lastRCastName = null;
    }

    /** Reset between battles: clear active skills that shouldn't carry over */
    public void resetCombatState() {
        qBuffTimer = 0; whirlTimer = 0; whirlPulseTimer = 0; whirlVisualTimer = 0;
        qFxTimer = 0; eFxTimer = 0; rightClickFxTimer = 0;
        fireTrailTimer = 0; fireTrailStart = null; fireTrailEnd = null; fireTrailDmgTimer = 0;
        attackTimer = 0; comboCount = 0; swingAngle = 0;
        hitFlashTimer = 0; lastDamageTaken = 0;
        dashTimer = 0; isDashing = false; isInvincible = false;
        dashDirection = null;
        velocity = new Vec2(0, 0);
        playerProjectiles.clear();
    }

    public void update(float dt) {
        GameManager gm = GameManager.instance;
        if (gm == null) return;
        if (gm.currentState != GameState.BATTLE) {
            velocity = new Vec2(0, 0);
            return;
        }
        if (gm.isPaused) return;

        updateTimers(dt);
        updateProjectiles(dt);
        updateWhirl(dt);
        updateFireTrail(dt);
        position = position.add(velocity.scale(dt * 60f));
        clampToArena();
    }

    public void handleKeyInput(boolean[] keys, Vec2 mousePos) {
        GameManager gm = GameManager.instance;
        if (gm == null || gm.currentState != GameState.BATTLE || gm.isPaused) return;

        mouseWorldPos = mousePos;
        Vec2 toMouse = mousePos.sub(position);
        if (toMouse.length() > 0.01f) aimDirection = toMouse.normalized();

        Vec2 input = new Vec2(0, 0);
        if (keys[KeyEvent.VK_W]) input.y -= 1;
        if (keys[KeyEvent.VK_S]) input.y += 1;
        if (keys[KeyEvent.VK_A]) input.x -= 1;
        if (keys[KeyEvent.VK_D]) input.x += 1;
        if (input.length() > 1) input = input.normalized();

        if (keys[KeyEvent.VK_SPACE] || keys[KeyEvent.VK_SHIFT]) tryDash(input);

        // Q blocks attack during 旋风 but not 雷暴
        boolean isStorm = qUltimate != null && qUltimate.skillName.contains("雷暴");
        if (whirlTimer <= 0 || isStorm) {
            boolean qDown = keys[KeyEvent.VK_Q];
            boolean eDown = keys[KeyEvent.VK_E];
            if (qDown && !qWasDown) tryQ();
            if (eDown && !eWasDown) tryE();
            qWasDown = qDown;
            eWasDown = eDown;

            if (!isDashing) {
                Vec2 targetVel = input.scale(moveSpeed);
                velocity = velocity.lerp(targetVel, 0.3f);
            }
        } else {
            // During whirl: only move at high speed, no other actions
            if (!isDashing) {
                Vec2 targetVel = input.scale(moveSpeed * 1.5f);
                velocity = velocity.lerp(targetVel, 0.4f);
            }
        }
    }

    public void handleMouseClick(int button) {
        GameManager gm = GameManager.instance;
        if (gm == null || gm.currentState != GameState.BATTLE || gm.isPaused) return;
        if (whirlTimer > 0) return; // no attacks during whirl

        if (button == MouseEvent.BUTTON1 && attackTimer <= 0) performAttack();
        else if (button == MouseEvent.BUTTON3 && rightClickCooldown <= 0) tryRightClick();
    }

    public void clampToArena() {
        Vec2 center = GameManager.ARENA_CENTER;
        float dist = position.distance(center);
        if (dist > GameManager.ARENA_RADIUS - 15) {
            Vec2 dir = position.sub(center).normalized();
            position = center.add(dir.scale(GameManager.ARENA_RADIUS - 15));
        }
    }

    public void tryDash(Vec2 input) {
        if (isDashing || currentDashCharges <= 0) return;
        isDashing = true; isInvincible = true; currentDashCharges--;
        dashStartPos = new Vec2(position);
        Vec2 dir = input.length() > 0.1f ? input : aimDirection;
        dashDirection = dir;  // save for trail rendering
        velocity = dir.scale(dashSpeed * dashDistanceMultiplier);
        dashTimer = dashDuration; dashCooldownTimer = dashCooldown;
        if (hasDashEffect("shield")) shieldTimer = 1.5f;
    }

    private void updateTimers(float dt) {
        attackTimer -= dt; qCooldown -= dt; eCooldown -= dt; rightClickCooldown -= dt;
        if (qBuffTimer > 0) qBuffTimer -= dt;
        hitFlashTimer -= dt;
        if (whirlTimer > 0) { whirlTimer -= dt; whirlPulseTimer -= dt; }
        qFxTimer -= dt; eFxTimer -= dt; rightClickFxTimer -= dt;
        if (fireTrailTimer > 0) fireTrailTimer -= dt;

        if (isDashing) {
            dashTimer -= dt;
            if (dashTimer <= 0) {
                isDashing = false; isInvincible = false;
                // Residual momentum instead of dead stop (feels more natural)
                velocity = dashDirection != null ? dashDirection.scale(dashSpeed * 0.25f) : new Vec2(0, 0);
                if (hasDashEffect("fire_trail")) {
                    GameManager gm = GameManager.instance;
                    int lv = PlayerSkillHandler.instance != null ?
                        PlayerSkillHandler.instance.getSkillLevel(findDashEffectSkill()) : 1;
                    // Lv1: just explosion at dash end
                    if (gm != null && gm.currentEnemy != null && !gm.currentEnemy.isDead) {
                        float dist = position.distance(gm.currentEnemy.position);
                        if (dist < 80f) {
                            gm.currentEnemy.takeDamage(getEnchantDmg() + 5);
                            gm.currentEnemy.burnTimer = Math.max(gm.currentEnemy.burnTimer, 2f);
                            gm.currentEnemy.burnDps = 3f;
                        }
                    }
                    // Lv2+: linear fire trail that persists
                    if (lv >= 2) {
                        fireTrailTimer = 3f;
                        fireTrailStart = new Vec2(dashStartPos);
                        fireTrailEnd = new Vec2(position);
                        fireTrailRadius = 50f;
                        fireTrailDmgTimer = 0f;
                    }
                }
                dashDirection = null;
            }
        } else {
            if (currentDashCharges < maxDashCharges) {
                dashCooldownTimer -= dt;
                if (dashCooldownTimer <= 0) {
                    currentDashCharges++;
                    // Add dashCooldown to carry over excess time (e.g. if timer was -0.3, new timer = 0.7)
                    dashCooldownTimer = dashCooldownTimer + dashCooldown;
                    if (dashCooldownTimer <= 0) dashCooldownTimer = dashCooldown;
                }
            } else {
                // At full charges, prevent timer from accumulating large negative values
                if (dashCooldownTimer < 0) dashCooldownTimer = 0;
            }
        }
        if (shieldTimer > 0) { shieldTimer -= dt; if (shieldTimer <= 0 && !isDashing) isInvincible = false; }
    }

    private float getEnchantDmg() { float t=0; for(float d:enchantDamages)t+=d; return t; }

    private void hitFx(float dmg) {
        PlayerSkillHandler h = PlayerSkillHandler.instance;
        if (h != null && h.lifestealPercent > 0) PlayerStats.instance.heal(dmg * h.lifestealPercent);
        if (h != null && h.reflectPercent > 0) takeDamage(dmg * h.reflectPercent);
    }
    private boolean hasEnchant(String e) { for(String s:enchantElements) if(e.equals(s)) return true; return false; }
    public boolean hasDashEffect(String e) { for(String s:dashEffects) if(e.equals(s)) return true; return false; }

    private void performAttack() {
        attackTimer = 1f / attackSpeed;
        swingAngle = (float)Math.atan2(aimDirection.y, aimDirection.x);
        GameManager gm = GameManager.instance;
        if (gm == null || gm.currentEnemy == null || gm.currentEnemy.isDead) return;

        float dist = position.distance(gm.currentEnemy.position);
        if (dist < attackRange) {
            // Check facing angle: enemy must be within ±55° of aim direction
            Vec2 toEnemy = gm.currentEnemy.position.sub(position);
            float angleToEnemy = (float)Math.atan2(toEnemy.y, toEnemy.x);
            float angleDiff = angleToEnemy - swingAngle;
            // Normalize to [-PI, PI]
            if (angleDiff > Math.PI) angleDiff -= (float)(Math.PI * 2);
            if (angleDiff < -Math.PI) angleDiff += (float)(Math.PI * 2);
            if (Math.abs(angleDiff) > Math.toRadians(55)) return;
            float dmg = (attackDamage + getEnchantDmg()) * (1f + comboCount * 0.1f);
            if (qBuffTimer > 0) dmg *= 1.5f;
            gm.currentEnemy.takeDamage(dmg);
            hitFx(dmg);
            comboCount = (comboCount + 1) % 3;
            if (gm.currentEnemy != null && !gm.currentEnemy.isDead) {
                if (hasEnchant("ice")) { gm.currentEnemy.slowTimer = 1.0f; gm.currentEnemy.slowAmount = 0.4f; }
                if (hasEnchant("lightning")) gm.currentEnemy.takeDamage(dmg * 0.3f);
                if (hasEnchant("fire")) { gm.currentEnemy.burnTimer = Math.max(gm.currentEnemy.burnTimer, 2f); gm.currentEnemy.burnDps = 5f; }
            }
        }
    }

    private void tryRightClick() {
        if (rightClickSpecial == null || rightClickCooldown > 0) return;
        // "附魔精通" and "冲刺大师" sacrifice right-click for extra slots
        if ("附魔精通".equals(rightClickSpecial.skillName) || "冲刺大师".equals(rightClickSpecial.skillName)) return;
        rightClickCooldown = rightClickSpecial.cooldown * (1f - getCdReduction());
        lastRCastName = rightClickSpecial.skillName;
        rightClickFxTimer = 0.35f;
        rightClickSwingAngle = (float)Math.atan2(aimDirection.y, aimDirection.x);
        GameManager gm = GameManager.instance;
        if (gm == null || gm.currentEnemy == null || gm.currentEnemy.isDead) return;

        String n = lastRCastName;
        float dist = position.distance(gm.currentEnemy.position);
        float mult = qBuffTimer > 0 ? 1.5f : 1f;
        float ed = getEnchantDmg();

        if ("剑刃回旋".equals(n)) {
            if (dist < attackRange * 1.5f) { float d = (attackDamage * 2.5f + ed) * mult; gm.currentEnemy.takeDamage(d); hitFx(d); }
        } else if ("蓄力突刺".equals(n)) {
            if (dist < attackRange * 2f) {
                gm.currentEnemy.takeDamage((attackDamage * 4f + ed) * mult);
                if (lvl(rightClickSpecial) >= 2 && gm.currentEnemy != null && !gm.currentEnemy.isDead) {
                    gm.currentEnemy.slowTimer = 0.5f; gm.currentEnemy.slowAmount = 0.1f;
                }
            }
        } else if ("冲锋".equals(n)) {
            if (dist < attackRange * 2.5f) gm.currentEnemy.takeDamage((attackDamage * 3.5f + ed) * mult);
            Vec2 dir = aimDirection;
            if (dir.length() > 0.1f) velocity = dir.scale(dashSpeed * 1.3f);
            dashTimer = 0.1f; isDashing = true; isInvincible = true;
        } else if ("地震打击".equals(n)) {
            if (dist < attackRange * 2f) {
                gm.currentEnemy.takeDamage((attackDamage * 3f + ed) * mult);
                if (gm.currentEnemy != null && !gm.currentEnemy.isDead) {
                    gm.currentEnemy.slowTimer = 0.5f; gm.currentEnemy.slowAmount = 0.3f;
                }
            }
        } else if ("连斩".equals(n)) {
            int hits = lvl(rightClickSpecial) >= 2 ? 5 : 3;
            for (int i = 0; i < hits; i++) {
                if (gm.currentEnemy != null && !gm.currentEnemy.isDead && position.distance(gm.currentEnemy.position) < attackRange * 1.5f)
                    gm.currentEnemy.takeDamage((attackDamage * 0.6f + ed) * mult);
            }
        } else {
            if (dist < attackRange * 1.8f) gm.currentEnemy.takeDamage((attackDamage * 2f + ed) * mult);
        }
    }

    private void tryQ() {
        if (qUltimate == null || qCooldown > 0) return;
        qCooldown = qUltimate.cooldown;
        qFxTimer = 0.6f;
        String name = qUltimate.skillName;
        GameManager gm = GameManager.instance;

        if (name.contains("战吼")) {
            qBuffTimer = 3f;
        } else if (name.contains("旋风")) {
            whirlTimer = 3f; whirlPulseTimer = 0f; whirlVisualTimer = 3f;
        } else if (name.contains("无敌")) {
            isInvincible = true; shieldTimer = 2.5f;
        } else if (name.contains("嗜血")) {
            qBuffTimer = 4f;
            PlayerSkillHandler h = PlayerSkillHandler.instance;
            if (h != null) h.lifestealPercent += 0.3f;
        } else if (name.contains("雷暴")) {
            whirlTimer = 5f; whirlPulseTimer = 0f; whirlVisualTimer = 5f;
        } else if (gm != null && gm.currentEnemy != null && !gm.currentEnemy.isDead) {
            float dist = position.distance(gm.currentEnemy.position);
            if (dist < attackRange * 2.5f) gm.currentEnemy.takeDamage(attackDamage * 4f + getEnchantDmg());
        }
    }

    private void tryE() {
        if (eUniversal == null || eCooldown > 0) return;
        eCooldown = eUniversal.cooldown;
        lastECastName = eUniversal.skillName;
        eFxTimer = 0.5f;
        GameManager gm = GameManager.instance;
        if (gm == null) return;

        String name = eUniversal.skillName;
        Vec2 target = gm.currentEnemy != null ? gm.currentEnemy.position : position.add(aimDirection.scale(200));
        Vec2 dir = target.sub(position).normalized();
        float speed = 200f;
        float dmg = attackDamage * 2f + getEnchantDmg();

        if (name.contains("治疗") || name.contains("回复")) {
            PlayerStats.instance.healPercent(0.3f);
            if (lvl(eUniversal) >= 2) { /* clear debuffs */ }
        } else if (name.contains("毒") || name.contains("雾")) {
            playerProjectiles.add(new PlayerProjectile(position, dir.scale(speed * 0.7f), attackDamage * 1.2f + getEnchantDmg(), 2f, "poison"));
            // AoE damage around player
            if (gm.currentEnemy != null && !gm.currentEnemy.isDead && position.distance(gm.currentEnemy.position) < attackRange * 2f) {
                gm.currentEnemy.takeDamage(attackDamage * 1.2f + getEnchantDmg());
                if (gm.currentEnemy != null && !gm.currentEnemy.isDead) {
                    gm.currentEnemy.poisonStacks += 3;
                    gm.currentEnemy.poisonTickTimer = 0.5f;
                }
            }
            // Lv2 mechanism: lingering slow
            if (lvl(eUniversal) >= 2 && gm.currentEnemy != null && !gm.currentEnemy.isDead) {
                gm.currentEnemy.slowTimer = 2f; gm.currentEnemy.slowAmount = 0.5f;
            }
        } else if (name.contains("震荡") || name.contains("地震") || name.contains("践踏")) {
            if (gm.currentEnemy != null && position.distance(gm.currentEnemy.position) < attackRange * 2f) {
                float d = attackDamage * 2f + getEnchantDmg();
                gm.currentEnemy.takeDamage(d); hitFx(d);
                // Knockback: push enemy away
                Vec2 away = gm.currentEnemy.position.sub(position).normalized();
                gm.currentEnemy.velocity = away.scale(12f);
                gm.currentEnemy.knockbackTimer = 0.2f;
                if (gm.currentEnemy != null && !gm.currentEnemy.isDead) {
                    gm.currentEnemy.slowTimer = 0.5f; gm.currentEnemy.slowAmount = 0.3f;
                }
            }
        } else if (name.contains("多重")) {
            int count = lvl(eUniversal) >= 2 ? 5 : 3;
            for (int i = -(count/2); i <= count/2; i++) {
                Vec2 off = new Vec2(dir.x + i * 0.3f, dir.y + i * 0.3f).normalized();
                playerProjectiles.add(new PlayerProjectile(position, off.scale(speed), attackDamage * 0.8f + getEnchantDmg(), 2f, "multi"));
            }
        } else if (name.contains("冰")) {
            playerProjectiles.add(new PlayerProjectile(position, dir.scale(speed), dmg, 2.5f, "ice"));
        } else if (name.contains("雷") || name.contains("闪电") || name.contains("链")) {
            playerProjectiles.add(new PlayerProjectile(position, dir.scale(speed * 1.3f), attackDamage * 2.5f + getEnchantDmg(), 2f, "lightning"));
        } else if (name.contains("吸血")) {
            if (gm.currentEnemy != null && position.distance(gm.currentEnemy.position) < attackRange * 1.5f) {
                float vd = (attackDamage * 2f + getEnchantDmg());
                gm.currentEnemy.takeDamage(vd);
                PlayerStats.instance.heal(vd * 0.5f);
            }
        } else if (name.contains("投射") || name.contains("射击") || name.contains("火")) {
            playerProjectiles.add(new PlayerProjectile(position, dir.scale(speed), dmg, 2f, "fire"));
        } else {
            playerProjectiles.add(new PlayerProjectile(position, dir.scale(speed), dmg, 2f, "fire"));
        }
    }

    public void takeDamage(float rawDamage) {
        if (isInvincible) return;
        if (shieldTimer > 0) rawDamage *= 0.3f;
        lastDamageTaken = rawDamage; hitFlashTimer = 0.1f;
        PlayerStats.instance.takeDamage(rawDamage);
    }

    public void applySkillModifiers() {
        maxDashCharges = 1 + extraDashChargesSkill;
        if (currentDashCharges > maxDashCharges) currentDashCharges = maxDashCharges;
    }

    public float getCdReduction() {
        PlayerSkillHandler h = PlayerSkillHandler.instance;
        return h != null ? h.cooldownReduction : 0f;
    }

    private int lvl(SkillData sd) {
        PlayerSkillHandler h = PlayerSkillHandler.instance;
        return h != null ? h.getSkillLevel(sd) : 1;
    }

    private void updateWhirl(float dt) {
        if (whirlTimer <= 0) return;
        GameManager gm = GameManager.instance;
        if (gm == null || gm.currentEnemy == null || gm.currentEnemy.isDead) return;
        if (whirlPulseTimer <= 0) {
            String name = qUltimate != null ? qUltimate.skillName : "";
            boolean isStorm = name.contains("雷暴");
            whirlPulseTimer = isStorm ? 0.8f : 0.12f;
            float dist = position.distance(gm.currentEnemy.position);
            float range = isStorm ? attackRange * 2.5f : attackRange * 1.5f;
            if (dist < range) {
                float dmg = isStorm ? attackDamage * 3f : attackDamage * 0.3f;
                gm.currentEnemy.takeDamage(dmg);
                hitFx(dmg);
            }
        }
    }

    private void updateFireTrail(float dt) {
        if (fireTrailTimer <= 0) return;
        GameManager gm = GameManager.instance;
        if (gm == null || gm.currentEnemy == null || gm.currentEnemy.isDead) return;
        // Check distance from enemy to line segment
        if (fireTrailStart != null && fireTrailEnd != null) {
            float distToTrail = pointToSegmentDist(gm.currentEnemy.position, fireTrailStart, fireTrailEnd);
            if (distToTrail < fireTrailRadius) {
                gm.currentEnemy.burnTimer = Math.max(gm.currentEnemy.burnTimer, 1.5f);
                gm.currentEnemy.burnDps = Math.max(gm.currentEnemy.burnDps, 4f);
                // Periodic direct damage
                fireTrailDmgTimer -= dt;
                if (fireTrailDmgTimer <= 0) {
                    fireTrailDmgTimer = 0.5f;
                    gm.currentEnemy.takeDamage(getEnchantDmg() + 3);
                }
            }
        }
    }

    /** Distance from point to line segment */
    private float pointToSegmentDist(Vec2 p, Vec2 a, Vec2 b) {
        Vec2 ab = new Vec2(b.x - a.x, b.y - a.y);
        Vec2 ap = new Vec2(p.x - a.x, p.y - a.y);
        float t = (ap.x * ab.x + ap.y * ab.y) / Math.max(0.0001f, ab.x * ab.x + ab.y * ab.y);
        t = Math.max(0, Math.min(1, t));
        Vec2 closest = new Vec2(a.x + ab.x * t, a.y + ab.y * t);
        return p.distance(closest);
    }

    private SkillData findDashEffectSkill() {
        PlayerSkillHandler h = PlayerSkillHandler.instance;
        if (h == null) return null;
        for (SkillInstance si : h.allSkills) {
            if (si.data.category == SkillCategory.DASH_EFFECT && "烈焰冲刺".equals(si.data.skillName))
                return si.data;
        }
        return null;
    }

    private void updateProjectiles(float dt) {
        GameManager gm = GameManager.instance;
        for (int i = playerProjectiles.size() - 1; i >= 0; i--) {
            PlayerProjectile pp = playerProjectiles.get(i);
            pp.update(dt);
            if (gm != null && gm.currentEnemy != null && !gm.currentEnemy.isDead) {
                Vec2 toEnemy = gm.currentEnemy.position.sub(pp.position);
                float dist = toEnemy.length();
                Vec2 targetDir = toEnemy.normalized();
                pp.velocity = pp.velocity.lerp(targetDir.scale(200f), 0.06f);
                if (dist < 20f) {
                    gm.currentEnemy.takeDamage(pp.damage);
                    hitFx(pp.damage);
                    if (gm.currentEnemy != null && !gm.currentEnemy.isDead) {
                        if ("ice".equals(pp.element)) { gm.currentEnemy.slowTimer = 1.5f; gm.currentEnemy.slowAmount = 0.5f; }
                        else if ("lightning".equals(pp.element)) gm.currentEnemy.takeDamage(pp.damage * 0.4f);
                        else if ("poison".equals(pp.element)) { gm.currentEnemy.poisonStacks += 2; gm.currentEnemy.poisonTickTimer = 0.5f; }
                        else if ("fire".equals(pp.element)) { gm.currentEnemy.burnTimer = Math.max(gm.currentEnemy.burnTimer, 2.5f); gm.currentEnemy.burnDps = 8f; }
                    }
                    playerProjectiles.remove(i);
                    continue;
                }
            }
            if (pp.isExpired() || pp.position.distance(GameManager.ARENA_CENTER) > GameManager.ARENA_RADIUS + 50)
                playerProjectiles.remove(i);
        }
    }
}

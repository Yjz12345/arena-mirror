package arenamirror.rendering;

import arenamirror.core.*;
import arenamirror.data.*;
import arenamirror.player.*;
import arenamirror.enemies.*;
import arenamirror.progression.*;
import arenamirror.traps.*;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class GameRenderer extends JPanel {
    public static GameRenderer instance;

    private GameManager gm;

    // ── 粒子系统 ──
    public List<Particle> particles = new ArrayList<>();
    public float frameDT = 1f / 60f;

    // ── 屏幕震动 ──
    float shakeX, shakeY, shakeIntensity;

    // Reward sub-state
    public static final int SUB_NONE = 0;
    public static final int SUB_SKILL_DRAW = 1;
    public static final int SUB_SKILL_UPGRADE = 2;
    public static final int SUB_REPLACE_CONFIRM = 3;
    public static final int SUB_STAT_UPGRADE = 4;
    public int rewardSubState = SUB_NONE;
    public List<SkillData> drawnSkills;
    public SkillData pendingNewSkill;
    public SkillInstance pendingConflict;

    // Clickable regions for current frame
    public List<ClickRegion> clickRegions = new ArrayList<>();
    public int upgradeScrollOffset;  // scroll for upgrade list

    // ── 粒子内部类 ──
    public static class Particle {
        float x, y, vx, vy, life, maxLife, size;
        Color color;
    }

    public static class ClickRegion {
        public Rectangle rect;
        public String action;
        public int index;
        public ClickRegion(Rectangle r, String a, int i) { rect = r; action = a; index = i; }
    }

    public GameRenderer() {
        instance = this;
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        setFocusable(true);
    }

    // ── 静态辅助方法（供 PlayerController / EnemyBase 调用） ──
    /** 触发屏幕震动 */
    public static void addShake(float amount) {
        if (instance != null) instance.shakeIntensity = Math.max(instance.shakeIntensity, amount);
    }

    /** 生成粒子爆发 */
    public static void spawnParticles(float x, float y, int count, Color color, float speed) {
        if (instance == null) return;
        Random rng = new Random();
        for (int i = 0; i < count; i++) {
            Particle p = new Particle();
            p.x = x; p.y = y;
            float angle = rng.nextFloat() * (float)(Math.PI * 2);
            float spd = speed * (0.5f + rng.nextFloat() * 0.5f);
            p.vx = (float)Math.cos(angle) * spd;
            p.vy = (float)Math.sin(angle) * spd;
            p.life = 0.3f + rng.nextFloat() * 0.4f;
            p.maxLife = p.life;
            p.size = 2 + rng.nextFloat() * 3;
            p.color = color;
            instance.particles.add(p);
        }
    }

    public void setGameManager(GameManager gm) { this.gm = gm; }

    // Called by Main.mousePressed to find which button was clicked
    public ClickRegion getClickAt(int x, int y) {
        for (ClickRegion cr : clickRegions) {
            if (cr.rect.contains(x, y)) return cr;
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (gm == null) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        clickRegions.clear();

        switch (gm.currentState) {
            case MAIN_MENU: drawMainMenu(g2); break;
            case REST_AREA: drawRestArea(g2); break;
            case BATTLE: drawBattle(g2); break;
            case REWARD_SELECTION: drawRewardSelection(g2); break;
            case GAME_OVER: drawGameOver(g2); break;
            case VICTORY: drawVictory(g2); break;
            case PAUSED: drawPauseOverlay(g2); break;
        }
    }

    // ── button helpers ──
    private boolean drawButton(Graphics2D g, String text, int y, String action, int index) {
        return drawButton(g, text, y, 200, 36, action, index);
    }

    private boolean drawButton(Graphics2D g, String text, int y, int w, int h, String action, int index) {
        FontMetrics fm = g.getFontMetrics();
        int x = (getWidth() - w) / 2;
        Rectangle rect = new Rectangle(x, y - h + 8, w, h);

        // hover check
        Point mp = getMousePosition();
        boolean hover = mp != null && rect.contains(mp);

        // 按钮填充（hover 渐变）
        if (hover) {
            GradientPaint btnGp = new GradientPaint(x, y - h + 8, new Color(70, 70, 130), x, y + 8, new Color(40, 40, 80));
            g.setPaint(btnGp);
        } else {
            g.setColor(new Color(40, 40, 60));
        }
        g.fillRoundRect(x, y - h + 8, w, h, 8, 8);
        g.setPaint(null);
        g.setColor(hover ? Color.WHITE : Color.LIGHT_GRAY);
        g.drawRoundRect(x, y - h + 8, w, h, 8, 8);

        int tx = x + (w - fm.stringWidth(text)) / 2;
        int ty = y + 4;
        g.drawString(text, tx, ty);

        clickRegions.add(new ClickRegion(rect, action, index));
        return hover;
    }

    private void drawTitle(Graphics2D g, String text, int y, int size) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, size));
        drawCentered(g, text, y);
    }

    private void drawText(Graphics2D g, String text, int y, int size) {
        g.setColor(Color.LIGHT_GRAY);
        g.setFont(new Font("SansSerif", Font.PLAIN, size));
        drawCentered(g, text, y);
    }

    private void drawText(Graphics2D g, String text, int y) {
        drawText(g, text, y, 14);
    }

    // ══════════════════════════════════════════════
    //  MAIN MENU
    // ══════════════════════════════════════════════
    private void drawMainMenu(Graphics2D g) {
        drawTitle(g, "角斗场：百层之镜", 150, 40);
        drawText(g, "\"你的敌人终将全是你\"", 195, 18);
        drawButton(g, "开始游戏", 280, "start_run", 0);
        drawText(g, "WASD移动 | 鼠标瞄准 | 左键攻击 | 右键特殊 | 空格冲刺 | Q/E技能", 500, 12);
    }

    // ══════════════════════════════════════════════
    //  REST AREA
    // ══════════════════════════════════════════════
    private void drawRestArea(Graphics2D g) {
        drawTitle(g, "休息区", 50, 28);

        int layer = gm.currentLayer;
        LayerSlot slot = gm.layerManager.getLayer(layer);

        drawText(g, "下一层：第 " + layer + " 层", 90, 18);

        if (slot != null) {
            String enemyType = slot.enemySource == EnemySource.PAST_LIFE ? "前世敌人" : "预设敌人";
            g.setColor(slot.enemySource == EnemySource.PAST_LIFE ? new Color(200, 100, 255) : new Color(255, 150, 50));
            drawCentered(g, "敌人类型：" + enemyType, 120, 16);

            if (slot.enemySource == EnemySource.PAST_LIFE && slot.pastLifeRecord != null) {
                drawText(g, "前世：" + slot.pastLifeRecord.characterName, 145, 15);
            } else if (slot.templateData != null) {
                drawText(g, "种族：" + slot.templateData.race + "  |  行为：" + slot.templateData.behavior, 145, 15);
            }

            // Skills
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            drawCentered(g, "技能：", 175);
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            int y = 195;
            if (slot.originalSkills != null && !slot.originalSkills.isEmpty()) {
                for (SkillData skill : slot.originalSkills) {
                    Color c = skill.hasTelegraph ? Color.ORANGE : Color.LIGHT_GRAY;
                    g.setColor(c);
                    drawCentered(g, "  " + (skill.hasTelegraph ? "⚠ " : "· ") + skill.skillName
                        + (skill.hasTelegraph ? " (预兆" + String.format("%.1f", skill.telegraphDuration) + "秒)" : ""), y);
                    y += 18;
                }
            } else {
                drawText(g, "  · 普通攻击", y);
            }
        }

        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        drawButton(g, "进入战斗", 500, "enter_battle", 0);
    }

    // ══════════════════════════════════════════════
    //  BATTLE
    // ══════════════════════════════════════════════
    private void drawBattle(Graphics2D g) {
        // ── 屏幕震动 ──
        if (shakeIntensity > 0) {
            shakeX = (float)(Math.random() - 0.5) * shakeIntensity;
            shakeY = (float)(Math.random() - 0.5) * shakeIntensity;
            shakeIntensity *= 0.85f;
            if (shakeIntensity < 0.5f) shakeIntensity = 0;
            g.translate(shakeX, shakeY);
        }

        // Arena background
        Vec2 center = GameManager.ARENA_CENTER;
        int cx = (int)center.x, cy = (int)center.y;
        int r = (int)GameManager.ARENA_RADIUS;

        // Floor
        g.setColor(new Color(25, 25, 35));
        g.fillOval(cx - r, cy - r, r * 2, r * 2);

        // Grid lines
        g.setColor(new Color(35, 35, 50));
        for (int i = -r; i <= r; i += 40) {
            g.drawLine(cx + i, cy - r, cx + i, cy + r);
            g.drawLine(cx - r, cy + i, cx + r, cy + i);
        }

        // Border
        g.setColor(new Color(80, 80, 100));
        g.setStroke(new BasicStroke(3));
        g.drawOval(cx - r, cy - r, r * 2, r * 2);
        g.setStroke(new BasicStroke(1));

        // Traps
        TrapManager tm = TrapManager.instance;
        if (tm != null && tm.activeTraps != null) {
            for (TrapSpawnEntry trap : tm.activeTraps) {
                int tx = cx + (int)trap.position.x;
                int ty = cy + (int)trap.position.y;
                switch (trap.trapType) {
                    case SPIKE:
                        g.setColor(trap.isObstacle ? Color.DARK_GRAY : new Color(180, 50, 50));
                        drawTriangle(g, tx, ty, 8);
                        break;
                    case FIRE:
                        g.setColor(new Color(255, 120, 20, 200));
                        g.fillOval(tx - 8, ty - 8, 16, 16);
                        break;
                    case MOVING_SAW:
                        g.setColor(Color.GRAY);
                        g.fillOval(tx - 10, ty - 10, 20, 20);
                        g.setColor(Color.DARK_GRAY);
                        g.drawOval(tx - 10, ty - 10, 20, 20);
                        break;
                    case PIT:
                        g.setColor(new Color(10, 10, 20));
                        g.fillOval(tx - 12, ty - 12, 24, 24);
                        break;
                }
            }
        }

        // Fire trail zones (烈焰冲刺 Lv2+ linear trails)
        if (gm.player != null && !gm.player.fireTrails.isEmpty()) {
            for (PlayerController.FireTrail ft : gm.player.fireTrails) {
                float fade = Math.min(1f, ft.timer / 0.5f);
                int sx = (int)ft.start.x, sy = (int)ft.start.y;
                int ex = (int)ft.end.x, ey = (int)ft.end.y;
                int trailW = (int)ft.radius;
                // Outer glow (thinner)
                g.setColor(new Color(255, 80, 20, (int)(40 * fade)));
                g.setStroke(new BasicStroke(trailW * 0.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(sx, sy, ex, ey);
                g.setStroke(new BasicStroke(1));
                // Inner bright line
                g.setColor(new Color(255, 180, 60, (int)(100 * fade)));
                g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(sx, sy, ex, ey);
                g.setStroke(new BasicStroke(1));
            }
        }

        // Enemy
        if (gm.currentEnemy != null) {
            // Draw enemy projectiles (with lifetime, dodgeable)
            for (EnemyProjectile ep : gm.currentEnemy.activeProjectiles) {
                float fade = Math.min(1f, ep.lifetime / 0.5f);
                Color outer, inner;
                switch (ep.colorType) {
                    case 1:  outer = new Color(100, 180, 255); inner = new Color(60, 140, 255); break;  // ice
                    case 2:  outer = new Color(255, 230, 50); inner = new Color(255, 200, 30); break;   // lightning
                    case 3:  outer = new Color(255, 100, 255); inner = new Color(255, 50, 255); break;  // laser
                    case 4:  outer = new Color(200, 200, 200); inner = new Color(255, 255, 255); break; // straight
                    case 5:  outer = new Color(120, 255, 50); inner = new Color(80, 200, 30); break;    // poison
                    default: outer = new Color(255, 100, 80); inner = new Color(255, 60, 40); break;    // fire
                }
                // 光晕（外层半透明大圈）
                g.setColor(new Color(outer.getRed(), outer.getGreen(), outer.getBlue(), (int)(60 * fade)));
                g.fillOval((int)ep.position.x - 8, (int)ep.position.y - 8, 16, 16);
                // 中间层
                g.setColor(new Color(outer.getRed(), outer.getGreen(), outer.getBlue(), (int)(180 * fade)));
                g.fillOval((int)ep.position.x - 5, (int)ep.position.y - 5, 10, 10);
                // 核心
                g.setColor(new Color(inner.getRed(), inner.getGreen(), inner.getBlue(), (int)(255 * fade)));
                g.fillOval((int)ep.position.x - 3, (int)ep.position.y - 3, 6, 6);
            }
            drawEnemy(g, gm.currentEnemy);
        }

        // Player (on top)
        if (gm.player != null) {
            // Draw player projectiles (glowing orbs)
            for (PlayerProjectile pp : gm.player.playerProjectiles) {
                Color orbColor;
                switch (pp.element != null ? pp.element : "") {
                    case "fire": orbColor = new Color(255, 150, 30); break;
                    case "ice": orbColor = new Color(80, 180, 255); break;
                    case "lightning": orbColor = new Color(255, 230, 50); break;
                    case "poison": orbColor = new Color(120, 255, 50); break;
                    case "laser": orbColor = new Color(255, 80, 255); break;
                    case "straight": orbColor = new Color(220, 220, 255); break;
                    default: orbColor = new Color(255, 200, 80); break;
                }
                // Glow
                g.setColor(new Color(orbColor.getRed(), orbColor.getGreen(), orbColor.getBlue(), 60));
                g.fillOval((int)pp.position.x - 6, (int)pp.position.y - 6, 12, 12);
                // Core
                g.setColor(orbColor);
                g.fillOval((int)pp.position.x - 3, (int)pp.position.y - 3, 6, 6);
            }
            drawPlayer(g, gm.player);
        }

        // ── 粒子更新 + 绘制 ──
        updateAndDrawParticles(g, frameDT);

        // ── 恢复震动偏移 ──
        if (shakeX != 0 || shakeY != 0) {
            g.translate(-shakeX, -shakeY);
            shakeX = 0; shakeY = 0;
        }

        // HUD
        drawBattleHUD(g);
    }

    private void drawTriangle(Graphics2D g, int cx, int cy, int s) {
        int[] xs = {cx, cx - s, cx + s};
        int[] ys = {cy - s, cy + s, cy + s};
        g.fillPolygon(xs, ys, 3);
    }

    private void drawPlayer(Graphics2D g, PlayerController p) {
        int px = (int)p.position.x, py = (int)p.position.y;
        float angle = (float)Math.atan2(p.aimDirection.y, p.aimDirection.x);

        // ── Q ultimate FX: expanding gold ring ──
        if (p.qFxTimer > 0) {
            float expand = 1f - p.qFxTimer / 0.5f;
            int qr = (int)(30 + expand * 150);
            g.setColor(new Color(255, 200, 50, (int)(150 * (p.qFxTimer / 0.5f))));
            g.setStroke(new BasicStroke(3));
            g.drawOval(px - qr, py - qr, qr * 2, qr * 2);
            g.setStroke(new BasicStroke(1));
        }

        // ── Q buff glow (战吼) ──
        if (p.qBuffTimer > 0) {
            float pulse = (float)(0.5 + 0.5 * Math.sin(p.qBuffTimer * 10));
            g.setColor(new Color(255, 200, 50, (int)(60 + pulse * 60)));
            g.fillOval(px - 18, py - 18, 36, 36);
        }

        // ── Hit flash ──
        if (p.hitFlashTimer > 0) {
            g.setColor(new Color(255, 50, 50, 140));
            g.fillOval(px - 16, py - 16, 32, 32);
        }

        // ── Dash trail ──
        if (p.isDashing || (p.dashDirection != null)) {
            Vec2 trailDir = p.isDashing && p.dashDirection != null ? p.dashDirection : p.aimDirection;
            boolean fire = p.hasDashEffect("fire_trail");

            // 冲刺尾部粒子（每帧 2-3 个）
            Color dashParticleColor = fire ? new Color(255, 120, 20) : new Color(100, 200, 255);
            for (int i = 0; i < 3; i++) {
                Particle dp = new Particle();
                dp.x = px + (float)(Math.random() - 0.5) * 12;
                dp.y = py + (float)(Math.random() - 0.5) * 12;
                dp.vx = -trailDir.x * (40 + (float)Math.random() * 60);
                dp.vy = -trailDir.y * (40 + (float)Math.random() * 60);
                dp.life = 0.2f + (float)Math.random() * 0.25f;
                dp.maxLife = dp.life;
                dp.size = 2 + (float)Math.random() * 3;
                dp.color = dashParticleColor;
                particles.add(dp);
            }

            // Large fire glow
            if (fire) {
                g.setColor(new Color(255, 120, 20, 70));
                g.fillOval(px - 20, py - 20, 40, 40);
            }
            for (int i = 1; i <= 5; i++) {
                float trailX = px - trailDir.x * i * 8;
                float trailY = py - trailDir.y * i * 8;
                int alpha = (int)(fire ? 100 : 80) - i * 15;
                int r = fire ? 255 : 0, gn = fire ? 140 : 200, b = fire ? 20 : 255;
                g.setColor(new Color(r, gn, b, Math.max(10, alpha)));
                g.fillOval((int)trailX - 6, (int)trailY - 6, 12, 12);
            }
            g.setColor(new Color(fire ? 255 : 100, fire ? 200 : 255, fire ? 50 : 255, fire ? 150 : 100));
            g.setStroke(new BasicStroke(fire ? 5f : 3f));
            int lx = px + (int)(trailDir.x * (fire ? 16 : 10));
            int ly = py + (int)(trailDir.y * (fire ? 16 : 10));
            g.drawLine(px, py, lx, ly);
            g.setStroke(new BasicStroke(1));
        }

        // ── Shield ──
        if (p.shieldTimer > 0) {
            g.setColor(new Color(200, 200, 255, 100));
            g.fillOval(px - 16, py - 16, 32, 32);
            g.setColor(new Color(150, 150, 255, 200));
            g.setStroke(new BasicStroke(2));
            g.drawOval(px - 16, py - 16, 32, 32);
            g.setStroke(new BasicStroke(1));
        }

        // ── Right-click weapon FX (differentiated per skill) ──
        if (p.rightClickFxTimer > 0) {
            float fade = p.rightClickFxTimer / 0.35f;
            String n = p.lastRCastName;
            float a = p.rightClickSwingAngle;
            float r = p.attackRange * 1.5f;

            if ("剑刃回旋".equals(n)) {
                // Full circle AoE
                g.setColor(new Color(100, 255, 200, (int)(70 * fade)));
                g.fillOval(px - (int)r, py - (int)r, (int)r * 2, (int)r * 2);
                g.setColor(new Color(50, 255, 150, (int)(180 * fade)));
                g.setStroke(new BasicStroke(2));
                g.drawOval(px - (int)r, py - (int)r, (int)r * 2, (int)r * 2);
                g.setStroke(new BasicStroke(1));
            } else if ("蓄力突刺".equals(n)) {
                // Long narrow cone forward
                int n2 = 7;
                int[] xp2 = new int[n2 + 2]; int[] yp2 = new int[n2 + 2];
                xp2[0] = px; yp2[0] = py;
                for (int i = 0; i <= n2; i++) {
                    float t = (float)i / n2;
                    float aa = a - (float)Math.toRadians(20) + (float)Math.toRadians(40) * t;
                    xp2[i + 1] = px + (int)(Math.cos(aa) * r * 1.6f);
                    yp2[i + 1] = py + (int)(Math.sin(aa) * r * 1.6f);
                }
                g.setColor(new Color(255, 150, 50, (int)(100 * fade)));
                g.fillPolygon(xp2, yp2, n2 + 2);
                // Stab line
                int lx = px + (int)(Math.cos(a) * r * 1.6f);
                int ly = py + (int)(Math.sin(a) * r * 1.6f);
                g.setColor(new Color(255, 255, 255, (int)(220 * fade)));
                g.setStroke(new BasicStroke(3f * fade));
                g.drawLine(px, py, lx, ly);
                g.setStroke(new BasicStroke(1));
            } else if ("冲锋".equals(n)) {
                // Dash trail forward
                int lx = px + (int)(Math.cos(a) * r * 2f);
                int ly = py + (int)(Math.sin(a) * r * 2f);
                g.setColor(new Color(255, 255, 100, (int)(80 * fade)));
                g.setStroke(new BasicStroke(6f * fade));
                g.drawLine(px, py, lx, ly);
                g.setStroke(new BasicStroke(1));
                // Wide cone behind
                drawFan(g, px, py, a, r, 140, new Color(255, 255, 100, (int)(40 * fade)));
            } else if ("地震打击".equals(n)) {
                // Expanding circle
                float expand = 1f - fade;
                int er = (int)(r * 0.8f + expand * r);
                g.setColor(new Color(180, 140, 50, (int)(80 * fade)));
                g.fillOval(px - er, py - er, er * 2, er * 2);
                g.setColor(new Color(200, 160, 60, (int)(180 * fade)));
                g.drawOval(px - er, py - er, er * 2, er * 2);
            } else if ("连斩".equals(n)) {
                int count = PlayerSkillHandler.instance.getSkillLevel(p.rightClickSpecial) >= 2 ? 5 : 3;
                for (int i = 0; i < count; i++) {
                    float offset = (i - (count-1)/2f) * 0.25f;
                    float sa = a + offset;
                    drawSlashLine(g, px, py, sa, r * 0.9f, new Color(255, 255, 255, (int)(120 * fade)));
                }
            } else if ("吸血打击".equals(n)) {
                // Red narrow cone + healing flash
                drawFan(g, px, py, a, r * 1.3f, 60, new Color(220, 40, 40, (int)(100 * fade)));
                g.setColor(new Color(255, 100, 100, (int)(60 * fade)));
                g.fillOval(px - 20, py - 20, 40, 40);
            } else {
                // Default: 重斩 - standard wide cone + slash
                drawFan(g, px, py, a, r, 140, new Color(100, 200, 255, (int)(80 * fade)));
                float slashA = a - (float)Math.toRadians(70) + (float)Math.toRadians(140) * (1f - fade);
                drawSlashLine(g, px, py, slashA, r * 0.9f, new Color(255, 255, 255, (int)(200 * fade)));
            }
        }

        // ── Normal attack swing arc (polygon fan, stable angle) ──
        if (p.attackTimer > 0) {
            float progress = 1f - Math.min(1f, p.attackTimer * p.attackSpeed);
            float fade = 1f - progress * progress;
            float r = p.attackRange;
            float a = p.swingAngle; // snapped on attack start, doesn't jitter

            // Build polygon fan: 5 points across ±55° from swingAngle
            int n = 7;
            int[] xp = new int[n + 2];
            int[] yp = new int[n + 2];
            xp[0] = px; yp[0] = py;
            for (int i = 0; i <= n; i++) {
                float t = (float)i / n;
                float aa = a - (float)Math.toRadians(55) + (float)Math.toRadians(110) * t;
                xp[i + 1] = px + (int)(Math.cos(aa) * r);
                yp[i + 1] = py + (int)(Math.sin(aa) * r);
            }
            // Enchant-tinted arc fill
            Color arcColor = getEnchantArcColor(p);
            g.setColor(new Color(arcColor.getRed(), arcColor.getGreen(), arcColor.getBlue(), (int)(80 * fade)));
            g.fillPolygon(xp, yp, n + 2);

            // Slash line sweeps within the arc (stable, based on progress not live angle)
            float slashA = a - (float)Math.toRadians(55) + (float)Math.toRadians(110) * progress;
            int sx = px + (int)(Math.cos(slashA) * r * 0.85f);
            int sy = py + (int)(Math.sin(slashA) * r * 0.85f);
            g.setColor(new Color(255, 255, 255, (int)(180 * fade)));
            g.setStroke(new BasicStroke(2.5f * fade));
            g.drawLine(px, py, sx, sy);
            g.setStroke(new BasicStroke(1));
        }

        // ── E skill FX ──
        if (p.eFxTimer > 0) {
            float fade = p.eFxTimer / 0.5f;
            String name = p.lastECastName;
            if (name != null) {
                if (name.contains("治疗") || name.contains("回复")) {
                    g.setColor(new Color(100, 255, 100, (int)(120 * fade)));
                    g.fillOval(px - 25, py - 25, 50, 50);
                } else if (name.contains("毒") || name.contains("雾")) {
                    // Green poison AoE circle
                    int er = (int)(p.attackRange * 2f);
                    g.setColor(new Color(80, 200, 50, (int)(60 * fade)));
                    g.fillOval(px - er, py - er, er * 2, er * 2);
                    g.setColor(new Color(50, 255, 50, (int)(150 * fade)));
                    g.setStroke(new BasicStroke(1.5f));
                    g.drawOval(px - er, py - er, er * 2, er * 2);
                    g.setStroke(new BasicStroke(1));
                } else if (name.contains("震荡") || name.contains("地震") || name.contains("践踏")) {
                    int er = (int)(p.attackRange * 2f);
                    g.setColor(new Color(180, 140, 50, (int)(80 * fade)));
                    g.fillOval(px - er, py - er, er * 2, er * 2);
                    g.setColor(new Color(200, 160, 60, (int)(180 * fade)));
                    g.drawOval(px - er, py - er, er * 2, er * 2);
                } else if (name.contains("旋风")) {
                    // handled by whirl visual
                } else if (name.contains("吸血")) {
                    // Red healing glow matching actual range (attackRange * 1.5)
                    int er = (int)(p.attackRange * 1.5f);
                    g.setColor(new Color(220, 40, 40, (int)(80 * fade)));
                    g.fillOval(px - er, py - er, er * 2, er * 2);
                    g.setColor(new Color(255, 80, 80, (int)(140 * fade)));
                    g.setStroke(new BasicStroke(2));
                    g.drawOval(px - er, py - er, er * 2, er * 2);
                    g.setStroke(new BasicStroke(1));
                    // Central burst
                    g.setColor(new Color(255, 40, 40, (int)(160 * fade)));
                    g.fillOval(px - 12, py - 12, 24, 24);
                } else {
                    // Projectile spawn flash
                    g.setColor(new Color(255, 255, 150, (int)(100 * fade)));
                    g.fillOval(px - 12, py - 12, 24, 24);
                }
            }
        }

        // ── Whirl/雷暴 AoE FX ──
        if (p.whirlTimer > 0) {
            String qName = p.qUltimate != null ? p.qUltimate.skillName : "";
            boolean isStorm = qName.contains("雷暴");
            float wr = isStorm ? p.attackRange * 2.5f : p.attackRange * 1.5f;
            Color wc = isStorm ? new Color(255, 220, 50) : new Color(100, 255, 200);

            // Pulsing fill
            float pulse = (float)Math.sin(p.whirlTimer * 12);
            g.setColor(new Color(wc.getRed(), wc.getGreen(), wc.getBlue(), (int)(25 + pulse * 20)));
            g.fillOval(px - (int)wr, py - (int)wr, (int)wr * 2, (int)wr * 2);
            // Ring
            g.setColor(new Color(wc.getRed(), wc.getGreen(), wc.getBlue(), (int)(100 + pulse * 50)));
            g.setStroke(new BasicStroke(2));
            g.drawOval(px - (int)wr, py - (int)wr, (int)wr * 2, (int)wr * 2);

            // Spin particles (whirl only)
            if (!isStorm) {
                long seed = (long)(p.whirlTimer * 100);
                Random rr = new Random(seed);
                for (int i = 0; i < 8; i++) {
                    float a = (float)(i * Math.PI * 2 / 8 + p.whirlTimer * 5);
                    int dx = px + (int)(Math.cos(a) * wr * 0.7f);
                    int dy = py + (int)(Math.sin(a) * wr * 0.7f);
                    g.setColor(new Color(100, 255, 200, (int)(80 + pulse * 30)));
                    g.fillOval(dx - 3, dy - 3, 6, 6);
                }
            } else {
                // Lightning spark particles
                long seed = (long)(p.whirlTimer * 200);
                Random rr = new Random(seed);
                for (int i = 0; i < 5; i++) {
                    float a = rr.nextFloat() * (float)Math.PI * 2;
                    float d = rr.nextFloat() * wr;
                    int dx = px + (int)(Math.cos(a) * d);
                    int dy = py + (int)(Math.sin(a) * d);
                    if (rr.nextFloat() < 0.3f) {
                        g.setColor(new Color(255, 255, 100, 180));
                        g.setStroke(new BasicStroke(1.5f));
                        g.drawLine(px, py, dx, dy);
                        g.setStroke(new BasicStroke(1));
                    }
                    g.setColor(new Color(255, 230, 50, 150));
                    g.fillOval(dx - 2, dy - 2, 4, 4);
                }
            }
            g.setStroke(new BasicStroke(1));
        }

        // ── Body (径向渐变) ──
        Color bodyColor = p.isInvincible ? new Color(100, 200, 255, 160) : new Color(50, 150, 255);
        float[] pDist = {0.0f, 0.7f, 1.0f};
        Color[] pColors = {
            new Color(180, 220, 255),  // 中心高亮
            bodyColor,                  // 基色
            bodyColor.darker().darker() // 边缘暗色
        };
        RadialGradientPaint prgp = new RadialGradientPaint(px, py, 13, pDist, pColors);
        g.setPaint(prgp);
        g.fillOval(px - 11, py - 11, 22, 22);
        g.setPaint(null);

        // ── Enchant glows ──
        for (int i = 0; i < p.enchantSlots; i++) {
            if (p.enchantElements[i] != null) drawEnchantGlow(g, px, py, p.enchantElements[i], bodyColor, 14 + i * 8);
        }

        g.setColor(Color.WHITE);
        g.drawOval(px - 11, py - 11, 22, 22);

        // ── Damage taken text ──
        if (p.hitFlashTimer > 0) {
            g.setColor(Color.ORANGE);
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.drawString(String.format("-%.0f", p.lastDamageTaken), px - 10, py - 25 - (int)((1 - p.hitFlashTimer / 0.1f) * 12));
        }

        // ── Aim line ──
        int ax = px + (int)(p.aimDirection.x * 20);
        int ay = py + (int)(p.aimDirection.y * 20);
        g.setColor(Color.YELLOW);
        g.setStroke(new BasicStroke(2));
        g.drawLine(px, py, ax, ay);
        g.setStroke(new BasicStroke(1));

        // ── Range ring ──
        g.setColor(new Color(255, 255, 100, 20));
        int ar = (int)p.attackRange;
        g.drawOval(px - ar, py - ar, ar * 2, ar * 2);

        // ── Dash charge dots ──
        for (int i = 0; i < p.maxDashCharges; i++) {
            float a = (float)(i * 2 * Math.PI / Math.max(1, p.maxDashCharges) - Math.PI / 2);
            int dx = px + (int)(Math.cos(a) * 18);
            int dy = py + (int)(Math.sin(a) * 18);
            g.setColor(i < p.currentDashCharges ? Color.CYAN : Color.DARK_GRAY);
            g.fillOval(dx - 3, dy - 3, 6, 6);
        }
    }

    private void drawEnemy(Graphics2D g, EnemyBase e) {
        int ex = (int)e.position.x, ey = (int)e.position.y;

        // Telegraph growing circle
        if (e.isTelegraphing) {
            float progress = 1f - (e.telegraphTimer / (e.queuedSkill != null ? e.queuedSkill.telegraphDuration : 0.5f));
            int tr = (int)(20 + progress * 35);
            g.setColor(new Color(255, 60, 60, 30 + (int)(progress * 80)));
            g.fillOval(ex - tr, ey - tr, tr * 2, tr * 2);
            g.setColor(new Color(255, 200, 0));
            g.drawOval(ex - tr, ey - tr, tr * 2, tr * 2);
        }

        // ── Enemy attack FX: melee arc or charge trail ──
        if (e.attackFxTimer > 0) {
            float fade = e.attackFxTimer / 0.3f;
            float a = (float)Math.atan2(e.lastAttackDir.y, e.lastAttackDir.x);
            // Red melee arc
            drawFan(g, ex, ey, a, 50, 80, new Color(255, 60, 40, (int)(80 * fade)));
            drawSlashLine(g, ex, ey, a, 40, new Color(255, 150, 100, (int)(150 * fade)));
        }

        // Slow effect indicator (blue tint)
        Color enemyColor;
        if (e.source == EnemySource.PAST_LIFE) {
            enemyColor = pastLifeColor(e.pastLifeId);
        } else {
            enemyColor = new Color(220, 80, 80);
        }
        if (e.slowTimer > 0) {
            enemyColor = blendColor(enemyColor, new Color(80, 150, 255), 0.4f);
        }
        if (e.burnTimer > 0) {
            enemyColor = blendColor(enemyColor, new Color(255, 100, 20), 0.5f);
        }
        if (e.poisonStacks > 0) {
            enemyColor = blendColor(enemyColor, new Color(120, 255, 50), 0.4f);
        }

        // Body with pattern based on pastLifeId
        g.setColor(enemyColor);
        int size = 13;

        // Hit flash overlay
        if (e.hitFlashTimer > 0) {
            // White flash
            g.setColor(new Color(255, 255, 255, 200));
            g.fillOval(ex - size - 2, ey - size - 2, (size + 2) * 2, (size + 2) * 2);
            // Impact ring (expanding)
            float ringR = 20 + (0.12f - e.hitFlashTimer) / 0.12f * 25;
            g.setColor(new Color(255, 255, 255, (int)(150 * (e.hitFlashTimer / 0.12f))));
            g.setStroke(new BasicStroke(2));
            g.drawOval(ex - (int)ringR, ey - (int)ringR, (int)ringR * 2, (int)ringR * 2);
            g.setStroke(new BasicStroke(1));
            // Particle burst
            Random rng = new Random(42);
            for (int i = 0; i < 6; i++) {
                float angle = (float)(rng.nextDouble() * Math.PI * 2);
                float dist = 10 + rng.nextFloat() * 15 * (1 - e.hitFlashTimer / 0.1f);
                int px = ex + (int)(Math.cos(angle) * dist);
                int py = ey + (int)(Math.sin(angle) * dist);
                g.setColor(new Color(255, 255, 100, (int)(180 * (e.hitFlashTimer / 0.1f))));
                g.fillOval(px - 2, py - 2, 4, 4);
            }
            // Damage number
            g.setColor(Color.YELLOW);
            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            String dmgText = String.format("%.0f", e.lastDamageTaken);
            g.drawString(dmgText, ex - 10, ey - 20 - (int)((1 - e.hitFlashTimer / 0.1f) * 15));
        }

        // Burn damage number (orange, floats up)
        if (e.burnFlashTimer > 0) {
            g.setColor(new Color(255, 160, 30, (int)(200 * (e.burnFlashTimer / 0.2f))));
            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            String burnText = String.format("%.0f", e.lastDamageTaken);
            g.drawString(burnText, ex - 10, ey - 22 - (int)((1 - e.burnFlashTimer / 0.2f) * 18));
        }

        // ── 敌人身体（径向渐变） ──
        float[] eDist = {0.0f, 0.7f, 1.0f};
        Color[] eColors = {
            enemyColor.brighter(),       // 中心高亮
            enemyColor,                  // 基色
            enemyColor.darker().darker() // 边缘暗色
        };
        if (e.source == EnemySource.PAST_LIFE && e.pastLifeId > 0) {
            // Draw patterned circle for past life
            RadialGradientPaint ergp = new RadialGradientPaint(ex, ey, size, eDist, eColors);
            g.setPaint(ergp);
            g.fillOval(ex - size, ey - size, size * 2, size * 2);
            g.setPaint(null);
            // Pattern: number of stripes = (pastLifeId % 5) + 1
            int stripes = (e.pastLifeId % 5) + 1;
            g.setColor(enemyColor.darker());
            for (int i = 0; i < stripes; i++) {
                double angle = (i * 2 * Math.PI / stripes) - Math.PI / 2;
                int sx = ex + (int)(Math.cos(angle) * size * 0.6);
                int sy = ey + (int)(Math.sin(angle) * size * 0.6);
                g.fillOval(sx - 3, sy - 3, 6, 6);
            }
        } else {
            RadialGradientPaint ergp = new RadialGradientPaint(ex, ey, size, eDist, eColors);
            g.setPaint(ergp);
            g.fillOval(ex - size, ey - size, size * 2, size * 2);
            g.setPaint(null);
        }
        g.setColor(Color.WHITE);
        g.drawOval(ex - size, ey - size, size * 2, size * 2);

        // Label
        g.setFont(new Font("SansSerif", Font.PLAIN, 9));
        String label;
        if (e.source == EnemySource.PAST_LIFE) {
            label = "前世#" + e.pastLifeId;
        } else {
            label = "Lv" + e.layerNumber;
        }
        FontMetrics fm = g.getFontMetrics();
        g.drawString(label, ex - fm.stringWidth(label) / 2, ey - 18);

        // HP bar (渐变)
        float hpPct = Math.max(0, e.currentHp / e.maxHp);
        int bw = 40, bh = 5;
        int bx = ex - bw / 2, by = ey - 26;
        g.setColor(Color.DARK_GRAY);
        g.fillRect(bx - 1, by - 1, bw + 2, bh + 2);
        int filledW = (int)(bw * hpPct);
        if (filledW > 0) {
            GradientPaint hpGp = new GradientPaint(bx, by, new Color(50, 255, 50), bx + filledW, by, new Color(255, 50, 50));
            g.setPaint(hpGp);
            g.fillRect(bx, by, filledW, bh);
            g.setPaint(null);
        }
    }

    /** Generate a unique color from past life ID using golden angle hue spread */
    private Color pastLifeColor(int id) {
        float goldenAngle = 137.508f;
        float hue = ((id * goldenAngle) % 360f) / 360f;
        return Color.getHSBColor(hue, 0.7f, 0.85f);
    }

    private Color blendColor(Color a, Color b, float ratio) {
        int r = (int)(a.getRed() * (1 - ratio) + b.getRed() * ratio);
        int g = (int)(a.getGreen() * (1 - ratio) + b.getGreen() * ratio);
        int bl = (int)(a.getBlue() * (1 - ratio) + b.getBlue() * ratio);
        return new Color(Math.min(255, r), Math.min(255, g), Math.min(255, bl));
    }

    /** 更新粒子物理并绘制 */
    private void updateAndDrawParticles(Graphics2D g, float dt) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            // 物理：重力 + 摩擦
            p.vy += 100 * dt;
            p.vx *= 0.95f;
            p.vy *= 0.95f;
            p.life -= dt;
            p.x += p.vx * dt;
            p.y += p.vy * dt;

            if (p.life <= 0) {
                particles.remove(i);
                continue;
            }

            // 绘制：大小和透明度随生命衰减
            float alpha = p.life / p.maxLife;
            float sz = p.size * alpha;
            g.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), (int)(200 * alpha)));
            g.fillOval((int)(p.x - sz / 2), (int)(p.y - sz / 2), (int)sz, (int)sz);
        }
    }

    private void drawBattleHUD(Graphics2D g) {
        PlayerStats stats = PlayerStats.instance;
        PlayerController pc = PlayerController.instance;
        if (stats == null) return;

        int x = 15, y = 15;
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(x, y, 250, 110, 10, 10);

        // HP bar (渐变)
        g.setColor(Color.DARK_GRAY);
        g.fillRect(x + 8, y + 8, 234, 18);
        float hpPct = Math.max(0, stats.currentHp / stats.maxHp);
        int hpFilledW = (int)(234 * hpPct);
        if (hpFilledW > 0) {
            GradientPaint hpGp = new GradientPaint(x + 8, y + 8, new Color(50, 255, 50), x + 8 + hpFilledW, y + 8, new Color(255, 50, 50));
            g.setPaint(hpGp);
            g.fillRect(x + 8, y + 8, hpFilledW, 18);
            g.setPaint(null);
        }
        g.setColor(Color.WHITE);
        g.drawRect(x + 8, y + 8, 234, 18);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        String hpText = String.format("HP %.0f / %.0f", stats.currentHp, stats.maxHp);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(hpText, x + 125 - fm.stringWidth(hpText) / 2, y + 22);

        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.drawString("第 " + gm.currentLayer + " / 100 层", x + 8, y + 48);

        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        int rowY = y + 65;

        // Dash
        g.drawString("空格: 冲刺 " + pc.currentDashCharges + "/" + pc.maxDashCharges, x + 8, rowY);

        // Q ultimate
        String qStr = "Q: " + (pc.qUltimate != null ? pc.qUltimate.skillName : "无");
        if (pc.qCooldown > 0) qStr += String.format(" %.0fs", pc.qCooldown);
        else qStr += " 就绪";
        g.setColor(pc.qCooldown > 0 ? Color.GRAY : Color.GREEN);
        g.drawString(qStr, x + 130, rowY);

        rowY += 15;

        // E universal
        String eStr = "E: " + (pc.eUniversal != null ? pc.eUniversal.skillName : "空槽");
        if (pc.eUniversal != null && pc.eCooldown > 0) eStr += String.format(" %.0fs", pc.eCooldown);
        else if (pc.eUniversal != null) eStr += " 就绪";
        g.setColor(pc.eUniversal == null ? Color.DARK_GRAY : (pc.eCooldown > 0 ? Color.GRAY : new Color(100, 255, 100)));
        g.drawString(eStr, x + 8, rowY);

        // Right click
        String rStr = "右键: " + (pc.rightClickSpecial != null ? pc.rightClickSpecial.skillName : "武器特攻");
        if (pc.rightClickCooldown > 0) rStr += String.format(" %.0fs", pc.rightClickCooldown);
        else rStr += " 就绪";
        g.setColor(pc.rightClickCooldown > 0 ? Color.GRAY : Color.CYAN);
        g.drawString(rStr, x + 130, rowY);

        rowY += 15;

        // Enchants
        StringBuilder eb = new StringBuilder();
        for (int i = 0; i < pc.enchantSlots; i++) if (pc.enchantElements[i] != null) eb.append(i>0?"+":"").append(pc.enchantElements[i]);
        if (eb.length() > 0) { g.setColor(Color.ORANGE); g.drawString("附魔: " + eb.toString(), x + 8, rowY); }

        // Bottom hint
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRoundRect(120, 572, 560, 22, 8, 8);
        g.setColor(Color.GRAY);
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        drawCentered(g, "WASD移动 | 鼠标瞄准 | 左键普攻 | 右键武器技 | Q大招 | E通用技 | 空格冲刺 | ESC暂停", 588);
    }

    // ══════════════════════════════════════════════
    //  REWARD SELECTION
    // ══════════════════════════════════════════════
    private void drawRewardSelection(Graphics2D g) {
        if (rewardSubState == SUB_SKILL_DRAW) {
            drawSkillDrawSub(g);
            return;
        }
        if (rewardSubState == SUB_SKILL_UPGRADE) {
            drawSkillUpgradeSub(g);
            return;
        }
        if (rewardSubState == SUB_STAT_UPGRADE) {
            drawStatUpgradeSub(g);
            return;
        }
        if (rewardSubState == SUB_REPLACE_CONFIRM) {
            drawReplaceConfirm(g);
            return;
        }

        drawTitle(g, "选择奖励", 45, 26);
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));

        String[] labels = {"回血 (20%)", "升级基础属性", "升级已有技能", "抽取新技能 (三选一)", "随机事件"};
        String[] actions = {"reward_heal", "reward_stat", "reward_upgrade", "reward_draw", "reward_event"};

        for (int i = 0; i < labels.length; i++) {
            drawButton(g, labels[i], 100 + i * 55, 300, 38, actions[i], i);
        }

        drawText(g, "也可按数字键 1-5 快速选择", 420, 12);
    }

    private void drawSkillDrawSub(Graphics2D g) {
        drawTitle(g, "抽取新技能", 45, 24);

        if (drawnSkills == null || drawnSkills.isEmpty()) {
            drawText(g, "没有可抽取的技能", 200);
            drawButton(g, "返回", 450, "reward_back", 0);
            return;
        }

        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        for (int i = 0; i < drawnSkills.size(); i++) {
            SkillData sk = drawnSkills.get(i);
            String label = getRaritySymbol(sk.rarity) + " " + sk.skillName + "  " + sk.description;
            drawButton(g, label, 110 + i * 70, 350, 38, "skill_pick", i);
        }

        // Reroll button
        RewardSystem rs = RewardSystem.instance;
        if (rs != null && rs.rerollsRemaining > 0) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            drawButton(g, "重roll (" + rs.rerollsRemaining + "次)", 360, 160, 32, "skill_reroll", 0);
        }

        drawButton(g, "返回", 480, 140, 32, "reward_back", 0);
    }

    private void drawSkillUpgradeSub(Graphics2D g) {
        drawTitle(g, "升级已有技能", 35, 22);

        RewardSystem rs = RewardSystem.instance;
        List<SkillData> upgrades = rs != null ? rs.getUpgradeableSkills() : new ArrayList<>();

        if (upgrades.isEmpty()) {
            drawText(g, "没有可升级的技能", 200);
            upgradeScrollOffset = 0;
        } else {
            int visibleItems = 7;
            int spacing = 42;
            int h = 30;
            // Clamp scroll
            int maxOffset = Math.max(0, upgrades.size() - visibleItems);
            if (upgradeScrollOffset > maxOffset) upgradeScrollOffset = maxOffset;
            if (upgradeScrollOffset < 0) upgradeScrollOffset = 0;

            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            for (int i = 0; i < visibleItems; i++) {
                int idx = upgradeScrollOffset + i;
                if (idx >= upgrades.size()) break;
                SkillData sk = upgrades.get(idx);
                SkillInstance inst = PlayerSkillHandler.instance.getSkillInstance(sk);
                String label = sk.skillName + "  Lv" + inst.currentLevel + " -> Lv" + (inst.currentLevel + 1);
                drawButton(g, label, 90 + i * spacing, 300, h, "upgrade_pick", idx);
            }

            // Scroll indicators
            if (upgrades.size() > visibleItems) {
                int scrollY = 90 + visibleItems * spacing + 8;
                g.setColor(Color.GRAY);
                g.setFont(new Font("SansSerif", Font.PLAIN, 11));
                drawCentered(g, (upgradeScrollOffset + 1) + "-" + Math.min(upgradeScrollOffset + visibleItems, upgrades.size()) + " / " + upgrades.size(), scrollY);

                // Up arrow (left side)
                if (upgradeScrollOffset > 0) {
                    drawButton(g, "↑", 62, 100, 24, "scroll_up", 0);
                }
                // Down arrow (right side, before 返回)
                if (upgradeScrollOffset < maxOffset) {
                    drawButton(g, "↓", 90 + visibleItems * spacing, 50, 24, "scroll_down", 0);
                }
            }
        }

        drawButton(g, "返回", 540, "reward_back", 0);
    }

    private void drawStatUpgradeSub(Graphics2D g) {
        drawTitle(g, "升级基础属性", 40, 24);
        RewardSystem rs = RewardSystem.instance;
        if (rs.statLabels == null) { drawText(g, "加载中...", 200); return; }

        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        for (int i = 0; i < rs.statLabels.length; i++) {
            drawButton(g, rs.statLabels[i], 100 + i * 55, 320, 38, "stat_pick", i);
        }
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        drawText(g, "当前第" + GameManager.instance.currentLayer + "层", 420);
        drawButton(g, "返回", 480, 140, 32, "reward_back", 0);
    }

    private void drawReplaceConfirm(Graphics2D g) {
        drawTitle(g, "技能冲突", 100, 22);
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        drawCentered(g, "【" + pendingNewSkill.skillName + "】与【" + pendingConflict.data.skillName + "】冲突", 180);
        drawText(g, "是否替换？", 210, 18);

        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        drawButton(g, "替换", 280, 200, 36, "replace_yes", 0);
        drawButton(g, "取消", 340, 200, 36, "replace_no", 0);
    }

    private String getRaritySymbol(SkillRarity r) {
        switch (r) {
            case COMMON: return "[白]";
            case UNCOMMON: return "[蓝]";
            case RARE: return "[紫]";
            case LEGENDARY: return "[金]";
        }
        return "[?]";
    }

    // ══════════════════════════════════════════════
    //  GAME OVER
    // ══════════════════════════════════════════════
    private void drawGameOver(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(Color.RED);
        drawTitle(g, "阵 亡", 120, 42);

        g.setColor(Color.WHITE);
        drawText(g, "你倒在了第 " + gm.currentLayer + " 层", 180, 18);
        if (gm.currentLayer > 1) {
            drawText(g, "你的身影将成为第 " + (gm.currentLayer - 1) + " 层的敌人...", 210, 14);
        }
        int currencyGain = gm.currentLayer * 10;
        drawText(g, "获得局外货币: " + currencyGain, 260, 16);

        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        drawButton(g, "重新开始", 330, "restart", 0);
        drawText(g, "按 ESC 返回主菜单", 400, 12);
    }

    // ══════════════════════════════════════════════
    //  VICTORY
    // ══════════════════════════════════════════════
    private void drawVictory(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(Color.YELLOW);
        drawTitle(g, "通 关！", 120, 44);

        g.setColor(Color.WHITE);
        drawText(g, "你征服了百层之镜", 190, 20);
        drawText(g, "但你的身影将永远留在第99层...", 230, 14);
        drawText(g, "下一次，最终的敌人将是你自己", 280, 16);

        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        drawButton(g, "继续", 350, "continue", 0);
    }

    private Color getEnchantArcColor(PlayerController p) {
        if (p.enchantElements[0] == null) return new Color(255, 255, 200);
        switch (p.enchantElements[0]) {
            case "fire": return new Color(255, 160, 50);
            case "ice": return new Color(120, 180, 255);
            case "lightning": return new Color(255, 240, 80);
            default: return new Color(255, 255, 200);
        }
    }

    // ── FX helpers ──
    private void drawEnchantGlow(Graphics2D g, int cx, int cy, String elem, Color bodyColor, int offset) {
        if (elem == null) return;
        Color glow;
        switch (elem) {
            case "fire": glow = new Color(255, 100, 20, 100); break;
            case "ice": glow = new Color(100, 180, 255, 100); break;
            case "lightning": glow = new Color(255, 220, 50, 100); break;
            default: glow = new Color(255, 255, 255, 60); break;
        }
        g.setColor(glow);
        g.fillOval(cx - offset, cy - offset, offset * 2, offset * 2);
        g.setColor(bodyColor);
        g.fillOval(cx - 11, cy - 11, 22, 22);
    }

    private void drawFan(Graphics2D g, int cx, int cy, float angle, float radius, int degrees, Color color) {
        int n = 7;
        int[] xp = new int[n + 2]; int[] yp = new int[n + 2];
        xp[0] = cx; yp[0] = cy;
        for (int i = 0; i <= n; i++) {
            float t = (float)i / n;
            float a = angle - (float)Math.toRadians(degrees / 2) + (float)Math.toRadians(degrees) * t;
            xp[i + 1] = cx + (int)(Math.cos(a) * radius);
            yp[i + 1] = cy + (int)(Math.sin(a) * radius);
        }
        g.setColor(color);
        g.fillPolygon(xp, yp, n + 2);
    }

    private void drawSlashLine(Graphics2D g, int cx, int cy, float angle, float length, Color color) {
        int ex = cx + (int)(Math.cos(angle) * length);
        int ey = cy + (int)(Math.sin(angle) * length);
        g.setColor(color);
        g.setStroke(new BasicStroke(2.5f));
        g.drawLine(cx, cy, ex, ey);
        g.setStroke(new BasicStroke(1));
    }

    private void drawPauseOverlay(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Color.WHITE);
        drawTitle(g, "已暂停", 280, 36);
        drawText(g, "按 ESC 继续", 340, 14);
    }

    // ── centering ──
    private void drawCentered(Graphics2D g, String text, int y) {
        FontMetrics fm = g.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(text)) / 2;
        g.drawString(text, x, y);
    }

    private void drawCentered(Graphics2D g, String text, int y, int size) {
        g.setFont(new Font("SansSerif", Font.PLAIN, size));
        drawCentered(g, text, y);
    }
}

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
    private GameManager gm;

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

    public static class ClickRegion {
        public Rectangle rect;
        public String action;
        public int index;
        public ClickRegion(Rectangle r, String a, int i) { rect = r; action = a; index = i; }
    }

    public GameRenderer() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        setFocusable(true);
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

        // Neon button: dark fill + cyan outline
        g.setColor(hover ? new Color(0, 30, 50) : new Color(10, 10, 25));
        g.fillRoundRect(x, y - h + 8, w, h, 8, 8);
        g.setColor(hover ? new Color(0, 255, 255, 200) : new Color(0, 255, 255, 80));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(x, y - h + 8, w, h, 8, 8);
        g.setStroke(new BasicStroke(1));

        g.setColor(hover ? new Color(0, 255, 255) : new Color(0, 200, 200));
        int tx = x + (w - fm.stringWidth(text)) / 2;
        int ty = y + 4;
        g.drawString(text, tx, ty);

        clickRegions.add(new ClickRegion(rect, action, index));
        return hover;
    }

    private void drawTitle(Graphics2D g, String text, int y, int size) {
        g.setColor(new Color(0, 255, 255));
        g.setFont(new Font("SansSerif", Font.BOLD, size));
        drawCentered(g, text, y);
    }

    private void drawText(Graphics2D g, String text, int y, int size) {
        g.setColor(new Color(0, 200, 200, 180));
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
            g.setColor(slot.enemySource == EnemySource.PAST_LIFE ? new Color(255, 215, 0) : new Color(255, 0, 255));
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
                    Color c = skill.hasTelegraph ? new Color(255, 150, 50) : new Color(0, 200, 200, 150);
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
        // Arena background
        Vec2 center = GameManager.ARENA_CENTER;
        int cx = (int)center.x, cy = (int)center.y;
        int r = (int)GameManager.ARENA_RADIUS;

        // Floor - pure black rect (neon style)
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 800, 600);

        // Grid lines - dark but visible on black
        g.setColor(new Color(25, 25, 45));
        g.setStroke(new BasicStroke(1));
        for (int i = -r; i <= r; i += 40) {
            g.drawLine(cx + i, cy - r, cx + i, cy + r);
            g.drawLine(cx - r, cy + i, cx + r, cy + i);
        }

        // Border - white neon circle
        g.setColor(new Color(80, 80, 120));
        g.setStroke(new BasicStroke(2.5f));
        g.drawOval(cx - r, cy - r, r * 2, r * 2);
        g.setStroke(new BasicStroke(1));

        // Traps (neon outline style)
        TrapManager tm = TrapManager.instance;
        if (tm != null && tm.activeTraps != null) {
            for (TrapSpawnEntry trap : tm.activeTraps) {
                int tx = cx + (int)trap.position.x;
                int ty = cy + (int)trap.position.y;
                switch (trap.trapType) {
                    case SPIKE:
                        // Neon red outline triangle
                        g.setColor(new Color(255, 60, 60, 180));
                        g.setStroke(new BasicStroke(1.5f));
                        drawTriangleOutline(g, tx, ty, 8);
                        g.setStroke(new BasicStroke(1));
                        break;
                    case FIRE:
                        // Glow + bright ring
                        g.setColor(new Color(255, 80, 255, 50));
                        g.fillOval(tx - 10, ty - 10, 20, 20);
                        g.setColor(new Color(255, 80, 255, 200));
                        g.setStroke(new BasicStroke(1.5f));
                        g.drawOval(tx - 8, ty - 8, 16, 16);
                        g.setStroke(new BasicStroke(1));
                        break;
                    case MOVING_SAW:
                        // Glow ring
                        g.setColor(new Color(255, 0, 255, 40));
                        g.fillOval(tx - 12, ty - 12, 24, 24);
                        g.setColor(new Color(200, 200, 200, 200));
                        g.setStroke(new BasicStroke(2));
                        g.drawOval(tx - 10, ty - 10, 20, 20);
                        g.setStroke(new BasicStroke(1));
                        break;
                    case PIT:
                        // Dark void with purple ring
                        g.setColor(new Color(5, 0, 15));
                        g.fillOval(tx - 12, ty - 12, 24, 24);
                        g.setColor(new Color(100, 0, 200, 150));
                        g.setStroke(new BasicStroke(2));
                        g.drawOval(tx - 12, ty - 12, 24, 24);
                        g.setStroke(new BasicStroke(1));
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
            // Draw enemy projectiles (neon outline dots)
            for (EnemyProjectile ep : gm.currentEnemy.activeProjectiles) {
                float fade = Math.min(1f, ep.lifetime / 0.5f);
                Color neonColor;
                switch (ep.colorType) {
                    case 1:  neonColor = new Color(0, 200, 255); break;     // ice: cyan
                    case 2:  neonColor = new Color(255, 230, 50); break;    // lightning: gold
                    case 3:  neonColor = new Color(255, 0, 255); break;     // laser: magenta
                    case 4:  neonColor = new Color(255, 255, 200); break;   // straight: bright
                    case 5:  neonColor = new Color(0, 255, 100); break;     // poison: green
                    default: neonColor = new Color(255, 80, 255); break;    // fire: magenta-orange
                }
                // Outer glow
                g.setColor(new Color(neonColor.getRed(), neonColor.getGreen(), neonColor.getBlue(), (int)(40 * fade)));
                g.fillOval((int)ep.position.x - 7, (int)ep.position.y - 7, 14, 14);
                // Inner bright ring
                g.setColor(new Color(neonColor.getRed(), neonColor.getGreen(), neonColor.getBlue(), (int)(200 * fade)));
                g.setStroke(new BasicStroke(1.5f));
                g.drawOval((int)ep.position.x - 5, (int)ep.position.y - 5, 10, 10);
                g.setStroke(new BasicStroke(1));
                // Core dot
                g.setColor(new Color(255, 255, 255, (int)(200 * fade)));
                g.fillOval((int)ep.position.x - 2, (int)ep.position.y - 2, 4, 4);
            }
            drawEnemy(g, gm.currentEnemy);
        }

        // Player (on top)
        if (gm.player != null) {
            // Draw player projectiles (neon glow + bright core)
            for (PlayerProjectile pp : gm.player.playerProjectiles) {
                Color neonColor;
                switch (pp.element != null ? pp.element : "") {
                    case "fire":     neonColor = new Color(255, 80, 255); break;    // magenta-orange
                    case "ice":      neonColor = new Color(0, 200, 255); break;     // cyan
                    case "lightning": neonColor = new Color(255, 230, 50); break;   // gold
                    case "poison":   neonColor = new Color(0, 255, 100); break;     // green
                    case "laser":    neonColor = new Color(255, 0, 255); break;     // magenta
                    case "straight": neonColor = new Color(255, 255, 200); break;   // bright white
                    default:         neonColor = new Color(255, 255, 200); break;
                }
                // Outer glow halo
                g.setColor(new Color(neonColor.getRed(), neonColor.getGreen(), neonColor.getBlue(), 40));
                g.fillOval((int)pp.position.x - 8, (int)pp.position.y - 8, 16, 16);
                // Bright ring
                g.setColor(new Color(neonColor.getRed(), neonColor.getGreen(), neonColor.getBlue(), 180));
                g.setStroke(new BasicStroke(1.5f));
                g.drawOval((int)pp.position.x - 5, (int)pp.position.y - 5, 10, 10);
                g.setStroke(new BasicStroke(1));
                // Core bright dot
                g.setColor(new Color(255, 255, 200));
                g.fillOval((int)pp.position.x - 2, (int)pp.position.y - 2, 4, 4);
            }
            drawPlayer(g, gm.player);
        }

        // HUD
        drawBattleHUD(g);
    }

    private void drawTriangle(Graphics2D g, int cx, int cy, int s) {
        int[] xs = {cx, cx - s, cx + s};
        int[] ys = {cy - s, cy + s, cy + s};
        g.drawPolygon(xs, ys, 3);
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
            g.setStroke(new BasicStroke(3 + pulse * 2));
            g.drawOval(px - 16, py - 16, 32, 32);
            g.setStroke(new BasicStroke(1));
        }

        // ── Hit flash ──
        if (p.hitFlashTimer > 0) {
            g.setColor(new Color(255, 50, 50, 180));
            g.setStroke(new BasicStroke(3));
            g.drawOval(px - 14, py - 14, 28, 28);
            g.setStroke(new BasicStroke(1));
        }

        // ── Dash trail (neon outline style) ──
        if (p.isDashing || (p.dashDirection != null)) {
            Vec2 trailDir = p.isDashing && p.dashDirection != null ? p.dashDirection : p.aimDirection;
            boolean fire = p.hasDashEffect("fire_trail");
            Color trailColor = fire ? new Color(255, 80, 255) : new Color(0, 255, 255);

            // Large glow ring
            if (fire) {
                g.setColor(new Color(255, 80, 255, 40));
                g.setStroke(new BasicStroke(8));
                g.drawOval(px - 16, py - 16, 32, 32);
                g.setStroke(new BasicStroke(1));
            }
            // Trail segments as thick outline lines
            for (int i = 1; i <= 5; i++) {
                float trailX = px - trailDir.x * i * 8;
                float trailY = py - trailDir.y * i * 8;
                int alpha = fire ? 100 : 80 - i * 15;
                g.setColor(new Color(trailColor.getRed(), trailColor.getGreen(), trailColor.getBlue(), Math.max(10, alpha)));
                g.setStroke(new BasicStroke(4 - i * 0.5f));
                g.drawOval((int)trailX - 5, (int)trailY - 5, 10, 10);
                g.setStroke(new BasicStroke(1));
            }
            // Front dash line
            g.setColor(new Color(trailColor.getRed(), trailColor.getGreen(), trailColor.getBlue(), fire ? 180 : 150));
            g.setStroke(new BasicStroke(fire ? 5f : 3f));
            int lx = px + (int)(trailDir.x * (fire ? 16 : 10));
            int ly = py + (int)(trailDir.y * (fire ? 16 : 10));
            g.drawLine(px, py, lx, ly);
            g.setStroke(new BasicStroke(1));
        }

        // ── Shield (neon outline) ──
        if (p.shieldTimer > 0) {
            g.setColor(new Color(200, 200, 255, 60));
            g.setStroke(new BasicStroke(4));
            g.drawOval(px - 15, py - 15, 30, 30);
            g.setColor(new Color(150, 150, 255, 200));
            g.setStroke(new BasicStroke(2));
            g.drawOval(px - 15, py - 15, 30, 30);
            g.setStroke(new BasicStroke(1));
        }

        // ── Right-click weapon FX (differentiated per skill) ──
        if (p.rightClickFxTimer > 0) {
            float fade = p.rightClickFxTimer / 0.35f;
            String n = p.lastRCastName;
            float a = p.rightClickSwingAngle;
            float r = p.attackRange * 1.5f;

            if ("剑刃回旋".equals(n)) {
                // Full circle AoE - multi-layer outlines
                g.setColor(new Color(100, 255, 200, (int)(30 * fade)));
                g.setStroke(new BasicStroke(6));
                g.drawOval(px - (int)r, py - (int)r, (int)r * 2, (int)r * 2);
                g.setColor(new Color(50, 255, 150, (int)(180 * fade)));
                g.setStroke(new BasicStroke(2));
                g.drawOval(px - (int)r, py - (int)r, (int)r * 2, (int)r * 2);
                g.setStroke(new BasicStroke(1));
            } else if ("蓄力突刺".equals(n)) {
                // Long narrow cone forward - outline fan
                drawFanOutline(g, px, py, a, r * 1.6f, 40, new Color(255, 150, 50, (int)(100 * fade)));
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
                // Wide cone behind - outline
                drawFanOutline(g, px, py, a, r, 140, new Color(255, 255, 100, (int)(80 * fade)));
            } else if ("地震打击".equals(n)) {
                // Expanding circle - multi-layer rings
                float expand = 1f - fade;
                int er = (int)(r * 0.8f + expand * r);
                g.setColor(new Color(180, 140, 50, (int)(50 * fade)));
                g.setStroke(new BasicStroke(4));
                g.drawOval(px - er, py - er, er * 2, er * 2);
                g.setColor(new Color(200, 160, 60, (int)(180 * fade)));
                g.setStroke(new BasicStroke(2));
                g.drawOval(px - er, py - er, er * 2, er * 2);
                g.setStroke(new BasicStroke(1));
            } else if ("连斩".equals(n)) {
                int count = PlayerSkillHandler.instance.getSkillLevel(p.rightClickSpecial) >= 2 ? 5 : 3;
                for (int i = 0; i < count; i++) {
                    float offset = (i - (count-1)/2f) * 0.25f;
                    float sa = a + offset;
                    drawSlashLine(g, px, py, sa, r * 0.9f, new Color(255, 255, 255, (int)(180 * fade)));
                }
            } else if ("吸血打击".equals(n)) {
                // Red narrow cone + healing ring
                drawFanOutline(g, px, py, a, r * 1.3f, 60, new Color(220, 40, 40, (int)(150 * fade)));
                g.setColor(new Color(255, 100, 100, (int)(120 * fade)));
                g.setStroke(new BasicStroke(3));
                g.drawOval(px - 18, py - 18, 36, 36);
                g.setStroke(new BasicStroke(1));
            } else {
                // Default: 重斩 - wide outline cone + slash
                drawFanOutline(g, px, py, a, r, 140, new Color(100, 200, 255, (int)(150 * fade)));
                float slashA = a - (float)Math.toRadians(70) + (float)Math.toRadians(140) * (1f - fade);
                drawSlashLine(g, px, py, slashA, r * 0.9f, new Color(255, 255, 255, (int)(220 * fade)));
            }
        }

        // ── Normal attack swing arc (neon outline arcs) ──
        if (p.attackTimer > 0) {
            float progress = 1f - Math.min(1f, p.attackTimer * p.attackSpeed);
            float fade = 1f - progress * progress;
            float r = p.attackRange;
            float a = p.swingAngle; // snapped on attack start, doesn't jitter

            // Enchant-tinted filled wedge (translucent, shows attack area)
            Color arcColor = getEnchantArcColor(p);
            g.setColor(new Color(arcColor.getRed(), arcColor.getGreen(), arcColor.getBlue(), (int)(25 * fade)));
            int npts = 9;
            int[] xp = new int[npts + 1];
            int[] yp = new int[npts + 1];
            xp[0] = px; yp[0] = py;
            for (int i = 0; i < npts; i++) {
                float t = (float)i / (npts - 1);
                float aa = a - (float)Math.toRadians(55) + (float)Math.toRadians(110) * t;
                xp[i + 1] = px + (int)(Math.cos(aa) * r);
                yp[i + 1] = py + (int)(Math.sin(aa) * r);
            }
            g.fillPolygon(xp, yp, npts + 1);

            // Outer glow arc on the edge (Java2D angle = -math angle)
            int java2dArcStart = -(int)Math.toDegrees(a) - 55;
            g.setColor(new Color(arcColor.getRed(), arcColor.getGreen(), arcColor.getBlue(), (int)(100 * fade)));
            g.setStroke(new BasicStroke(8));
            g.drawArc(px - (int)r, py - (int)r, (int)r * 2, (int)r * 2, java2dArcStart, 110);

            // Inner bright arc
            g.setColor(new Color(arcColor.getRed(), arcColor.getGreen(), arcColor.getBlue(), (int)(220 * fade)));
            g.setStroke(new BasicStroke(2.5f));
            g.drawArc(px - (int)r, py - (int)r, (int)r * 2, (int)r * 2, java2dArcStart, 110);
            g.setStroke(new BasicStroke(1));

            // Slash line sweeps within the arc (stable, based on progress not live angle)
            float slashA = a - (float)Math.toRadians(55) + (float)Math.toRadians(110) * progress;
            int sx = px + (int)(Math.cos(slashA) * r * 0.85f);
            int sy = py + (int)(Math.sin(slashA) * r * 0.85f);
            g.setColor(new Color(255, 255, 255, (int)(240 * fade)));
            g.setStroke(new BasicStroke(3.5f * fade));
            g.drawLine(px, py, sx, sy);
            g.setStroke(new BasicStroke(1));
        }

        // ── E skill FX (neon outline style) ──
        if (p.eFxTimer > 0) {
            float fade = p.eFxTimer / 0.5f;
            String name = p.lastECastName;
            if (name != null) {
                if (name.contains("治疗") || name.contains("回复")) {
                    g.setColor(new Color(80, 255, 80, (int)(60 * fade)));
                    g.setStroke(new BasicStroke(5));
                    g.drawOval(px - 22, py - 22, 44, 44);
                    g.setColor(new Color(80, 255, 80, (int)(180 * fade)));
                    g.setStroke(new BasicStroke(2));
                    g.drawOval(px - 22, py - 22, 44, 44);
                    g.setStroke(new BasicStroke(1));
                } else if (name.contains("毒") || name.contains("雾")) {
                    // Green poison AoE ring
                    int er = (int)(p.attackRange * 2f);
                    g.setColor(new Color(0, 255, 100, (int)(30 * fade)));
                    g.setStroke(new BasicStroke(5));
                    g.drawOval(px - er, py - er, er * 2, er * 2);
                    g.setColor(new Color(0, 255, 100, (int)(180 * fade)));
                    g.setStroke(new BasicStroke(2));
                    g.drawOval(px - er, py - er, er * 2, er * 2);
                    g.setStroke(new BasicStroke(1));
                } else if (name.contains("震荡") || name.contains("地震") || name.contains("践踏")) {
                    int er = (int)(p.attackRange * 2f);
                    g.setColor(new Color(180, 140, 50, (int)(40 * fade)));
                    g.setStroke(new BasicStroke(5));
                    g.drawOval(px - er, py - er, er * 2, er * 2);
                    g.setColor(new Color(200, 160, 60, (int)(200 * fade)));
                    g.setStroke(new BasicStroke(2));
                    g.drawOval(px - er, py - er, er * 2, er * 2);
                    g.setStroke(new BasicStroke(1));
                } else if (name.contains("旋风")) {
                    // handled by whirl visual
                } else if (name.contains("吸血")) {
                    // Red healing ring - multi-layer outlines
                    int er = (int)(p.attackRange * 1.5f);
                    g.setColor(new Color(220, 40, 40, (int)(40 * fade)));
                    g.setStroke(new BasicStroke(5));
                    g.drawOval(px - er, py - er, er * 2, er * 2);
                    g.setColor(new Color(255, 80, 80, (int)(180 * fade)));
                    g.setStroke(new BasicStroke(2));
                    g.drawOval(px - er, py - er, er * 2, er * 2);
                    g.setStroke(new BasicStroke(1));
                    // Central burst ring
                    g.setColor(new Color(255, 40, 40, (int)(200 * fade)));
                    g.setStroke(new BasicStroke(2));
                    g.drawOval(px - 11, py - 11, 22, 22);
                    g.setStroke(new BasicStroke(1));
                } else {
                    // Projectile spawn flash ring
                    g.setColor(new Color(255, 255, 150, (int)(50 * fade)));
                    g.setStroke(new BasicStroke(4));
                    g.drawOval(px - 12, py - 12, 24, 24);
                    g.setColor(new Color(255, 255, 150, (int)(180 * fade)));
                    g.setStroke(new BasicStroke(2));
                    g.drawOval(px - 12, py - 12, 24, 24);
                    g.setStroke(new BasicStroke(1));
                }
            }
        }

        // ── Whirl/雷暴 AoE FX (neon outline rings) ──
        if (p.whirlTimer > 0) {
            String qName = p.qUltimate != null ? p.qUltimate.skillName : "";
            boolean isStorm = qName.contains("雷暴");
            float wr = isStorm ? p.attackRange * 2.5f : p.attackRange * 1.5f;
            Color wc = isStorm ? new Color(255, 220, 50) : new Color(100, 255, 200);

            // Pulsing rings (multi-layer outlines)
            float pulse = (float)Math.sin(p.whirlTimer * 12);
            // Outer glow ring
            g.setColor(new Color(wc.getRed(), wc.getGreen(), wc.getBlue(), (int)(25 + pulse * 20)));
            g.setStroke(new BasicStroke(5));
            g.drawOval(px - (int)wr, py - (int)wr, (int)wr * 2, (int)wr * 2);
            // Inner bright ring
            g.setColor(new Color(wc.getRed(), wc.getGreen(), wc.getBlue(), (int)(100 + pulse * 50)));
            g.setStroke(new BasicStroke(2));
            g.drawOval(px - (int)wr, py - (int)wr, (int)wr * 2, (int)wr * 2);

            // Spin particles (whirl only) - small bright dots
            if (!isStorm) {
                long seed = (long)(p.whirlTimer * 100);
                Random rr = new Random(seed);
                for (int i = 0; i < 8; i++) {
                    float a = (float)(i * Math.PI * 2 / 8 + p.whirlTimer * 5);
                    int dx = px + (int)(Math.cos(a) * wr * 0.7f);
                    int dy = py + (int)(Math.sin(a) * wr * 0.7f);
                    g.setColor(new Color(100, 255, 200, 180));
                    g.fillOval(dx - 2, dy - 2, 4, 4);
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

        // ── Body (neon cyan multi-layer outline, highly visible) ──
        Color bodyColor = p.isInvincible ? new Color(255, 255, 255, 240) : new Color(0, 255, 255);

        // Dash boost: thicker outer glow
        float glowThickness = p.isDashing ? 14f : 10f;
        Color glowColor = p.isInvincible ? new Color(255, 255, 255, 80) : new Color(0, 255, 255, 100);

        // Outer glow (thick + bright)
        g.setColor(glowColor);
        g.setStroke(new BasicStroke(glowThickness));
        g.drawOval(px - 11, py - 11, 22, 22);

        // Inner bright line
        g.setColor(bodyColor);
        g.setStroke(new BasicStroke(3));
        g.drawOval(px - 11, py - 11, 22, 22);

        g.setStroke(new BasicStroke(1));

        // ── Enchant glows (small colored outline rings) ──
        for (int i = 0; i < p.enchantSlots; i++) {
            if (p.enchantElements[i] != null) drawEnchantGlowNeon(g, px, py, p.enchantElements[i], 14 + i * 8);
        }

        // ── Damage taken text ──
        if (p.hitFlashTimer > 0) {
            g.setColor(new Color(255, 150, 50));
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.drawString(String.format("-%.0f", p.lastDamageTaken), px - 10, py - 25 - (int)((1 - p.hitFlashTimer / 0.1f) * 12));
        }

        // ── Aim line (bright neon) ──
        int ax = px + (int)(p.aimDirection.x * 24);
        int ay = py + (int)(p.aimDirection.y * 24);
        g.setColor(new Color(0, 255, 255, 60));
        g.setStroke(new BasicStroke(4));
        g.drawLine(px, py, ax, ay);
        g.setColor(new Color(0, 255, 255, 220));
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(px, py, ax, ay);
        g.setStroke(new BasicStroke(1));

        // ── Range ring (bright neon, always visible) ──
        g.setColor(new Color(0, 255, 255, 100));
        g.setStroke(new BasicStroke(1.5f));
        int ar = (int)p.attackRange;
        g.drawOval(px - ar, py - ar, ar * 2, ar * 2);
        // Extra glow dots at cardinal points
        g.setColor(new Color(0, 255, 255, 180));
        g.fillOval(px + ar - 2, py - 2, 4, 4);
        g.fillOval(px - ar - 2, py - 2, 4, 4);
        g.fillOval(px - 2, py + ar - 2, 4, 4);
        g.fillOval(px - 2, py - ar - 2, 4, 4);
        g.setStroke(new BasicStroke(1));

        // ── Dash charge dots (neon cyan, empty outlines for unused) ──
        for (int i = 0; i < p.maxDashCharges; i++) {
            float a = (float)(i * 2 * Math.PI / Math.max(1, p.maxDashCharges) - Math.PI / 2);
            int dx = px + (int)(Math.cos(a) * 18);
            int dy = py + (int)(Math.sin(a) * 18);
            if (i < p.currentDashCharges) {
                g.setColor(new Color(0, 255, 255));
                g.fillOval(dx - 3, dy - 3, 6, 6);
            } else {
                g.setColor(new Color(0, 255, 255, 100));
                g.setStroke(new BasicStroke(1.5f));
                g.drawOval(dx - 3, dy - 3, 6, 6);
                g.setStroke(new BasicStroke(1));
            }
        }
    }

    private void drawEnemy(Graphics2D g, EnemyBase e) {
        int ex = (int)e.position.x, ey = (int)e.position.y;

        // Telegraph growing circle (neon outline) — only if alive
        if (!e.isDead && e.isTelegraphing) {
            float progress = 1f - (e.telegraphTimer / (e.queuedSkill != null ? e.queuedSkill.telegraphDuration : 0.5f));
            int tr = (int)(20 + progress * 35);
            g.setColor(new Color(255, 0, 255, (int)(30 + progress * 80)));
            g.setStroke(new BasicStroke(3));
            g.drawOval(ex - tr, ey - tr, tr * 2, tr * 2);
            g.setColor(new Color(255, 200, 0));
            g.setStroke(new BasicStroke(1.5f));
            g.drawOval(ex - tr, ey - tr, tr * 2, tr * 2);
            g.setStroke(new BasicStroke(1));
        }

        // ── Enemy attack FX: melee arc outline — only if alive ──
        if (!e.isDead && e.attackFxTimer > 0) {
            float fade = e.attackFxTimer / 0.3f;
            float a = (float)Math.atan2(e.lastAttackDir.y, e.lastAttackDir.x);
            // Red melee outline arc
            drawFanOutline(g, ex, ey, a, 50, 80, new Color(255, 60, 40, (int)(120 * fade)));
            drawSlashLine(g, ex, ey, a, 40, new Color(255, 150, 100, (int)(200 * fade)));
        }

        // Base enemy color: preset = neon magenta, past life = neon gold
        Color enemyColor;
        if (e.source == EnemySource.PAST_LIFE) {
            enemyColor = new Color(255, 215, 0);  // neon gold
        } else {
            enemyColor = new Color(255, 0, 255);   // neon magenta
        }
        // Status tint adjustments (only when alive)
        if (!e.isDead) {
        if (e.slowTimer > 0) {
            enemyColor = blendColor(enemyColor, new Color(0, 200, 255), 0.3f);
        }
        if (e.burnTimer > 0) {
            enemyColor = blendColor(enemyColor, new Color(255, 80, 255), 0.4f);
        }
        if (e.poisonStacks > 0) {
            enemyColor = blendColor(enemyColor, new Color(0, 255, 100), 0.3f);
        }
        } // end if (!e.isDead)

        int size = 13;

        // Hit flash overlay (bright fill + outline) — only if alive
        if (!e.isDead && e.hitFlashTimer > 0) {
            // Bright white fill flash (like original, for visibility)
            g.setColor(new Color(255, 255, 255, (int)(180 * (e.hitFlashTimer / 0.12f))));
            g.fillOval(ex - size - 2, ey - size - 2, (size + 2) * 2, (size + 2) * 2);
            // Thick outline flash ring
            g.setColor(new Color(255, 255, 255, (int)(255 * (e.hitFlashTimer / 0.12f))));
            g.setStroke(new BasicStroke(5));
            g.drawOval(ex - size - 4, ey - size - 4, (size + 4) * 2, (size + 4) * 2);
            // Impact ring (expanding, thicker)
            float ringR = 15 + (0.12f - e.hitFlashTimer) / 0.12f * 35;
            g.setColor(new Color(255, 255, 255, (int)(200 * (e.hitFlashTimer / 0.12f))));
            g.setStroke(new BasicStroke(3));
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
            // Damage number (neon yellow)
            g.setColor(new Color(255, 255, 0));
            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            String dmgText = String.format("%.0f", e.lastDamageTaken);
            g.drawString(dmgText, ex - 10, ey - 20 - (int)((1 - e.hitFlashTimer / 0.1f) * 15));
        }

        // Burn damage number (neon orange)
        if (e.burnFlashTimer > 0) {
            g.setColor(new Color(255, 160, 30, (int)(200 * (e.burnFlashTimer / 0.2f))));
            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            String burnText = String.format("%.0f", e.lastDamageTaken);
            g.drawString(burnText, ex - 10, ey - 22 - (int)((1 - e.burnFlashTimer / 0.2f) * 18));
        }

        // ── Enemy body: multi-layer neon outline ──
        // Dead enemies fade out
        float deadFade = e.isDead ? Math.max(0, e.deathTimer / 0.3f) : 1f;
        
        // Outer glow (thick + bright, fades when dead)
        g.setColor(new Color(enemyColor.getRed(), enemyColor.getGreen(), enemyColor.getBlue(), (int)(100 * deadFade)));
        g.setStroke(new BasicStroke(8 * deadFade));
        g.drawOval(ex - size, ey - size, size * 2, size * 2);

        // Inner bright line
        g.setColor(new Color(enemyColor.getRed(), enemyColor.getGreen(), enemyColor.getBlue(), (int)(255 * deadFade)));
        g.setStroke(new BasicStroke(3 * deadFade));
        g.drawOval(ex - size, ey - size, size * 2, size * 2);

        // Past life pattern: small dots along the ring
        if (e.source == EnemySource.PAST_LIFE && e.pastLifeId > 0) {
            int stripes = (e.pastLifeId % 5) + 1;
            for (int i = 0; i < stripes; i++) {
                double angle = (i * 2 * Math.PI / stripes) - Math.PI / 2;
                int sx = ex + (int)(Math.cos(angle) * size);
                int sy = ey + (int)(Math.sin(angle) * size);
                g.setColor(new Color(255, 215, 0, 200));
                g.fillOval(sx - 2, sy - 2, 4, 4);
            }
        }

        g.setStroke(new BasicStroke(1));

        // Label (neon color, fades when dead)
        g.setFont(new Font("SansSerif", Font.PLAIN, 9));
        String label;
        Color labelColor;
        if (e.isDead) {
            label = "击杀";
            labelColor = new Color(255, 255, 255, (int)(200 * deadFade));
        } else if (e.source == EnemySource.PAST_LIFE) {
            label = "前世#" + e.pastLifeId;
            labelColor = new Color(255, 215, 0);
        } else {
            label = "Lv" + e.layerNumber;
            labelColor = new Color(255, 0, 255);
        }
        g.setColor(labelColor);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(label, ex - fm.stringWidth(label) / 2, ey - 18);

        // HP bar (neon thin bar)
        float hpPct = Math.max(0, e.currentHp / e.maxHp);
        int bw = 40, bh = 5;
        int bx = ex - bw / 2, by = ey - 26;
        // Dark background
        g.setColor(new Color(40, 0, 40));
        g.fillRect(bx - 1, by - 1, bw + 2, bh + 2);
        // Bright foreground
        Color hpBarColor = hpPct > 0.5f ? new Color(255, 0, 255, 200) : (hpPct > 0.25f ? new Color(255, 215, 0, 200) : new Color(255, 50, 50, 200));
        g.setColor(hpBarColor);
        g.fillRect(bx, by, (int)(bw * hpPct), bh);
        // Glow border
        g.setColor(new Color(255, 0, 255, 100));
        g.setStroke(new BasicStroke(1));
        g.drawRect(bx - 1, by - 1, bw + 2, bh + 2);
        g.setStroke(new BasicStroke(1));
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

    private void drawBattleHUD(Graphics2D g) {
        PlayerStats stats = PlayerStats.instance;
        PlayerController pc = PlayerController.instance;
        if (stats == null) return;

        int x = 15, y = 15;

        // Semi-transparent dark panel
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRoundRect(x, y, 250, 110, 10, 10);
        // Cyan border
        g.setColor(new Color(0, 255, 255, 60));
        g.drawRoundRect(x, y, 250, 110, 10, 10);

        // HP bar (neon style)
        g.setColor(new Color(40, 0, 40));
        g.fillRect(x + 8, y + 8, 234, 18);
        float hpPct = Math.max(0, stats.currentHp / stats.maxHp);
        Color hpColor = hpPct > 0.5f ? new Color(0, 255, 255, 200) : (hpPct > 0.25f ? new Color(255, 215, 0, 200) : new Color(255, 50, 50, 200));
        g.setColor(hpColor);
        g.fillRect(x + 8, y + 8, (int)(234 * hpPct), 18);
        // Glow border
        g.setColor(new Color(0, 255, 255, 100));
        g.setStroke(new BasicStroke(1));
        g.drawRect(x + 8, y + 8, 234, 18);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(new Color(0, 255, 255));
        String hpText = String.format("HP %.0f / %.0f", stats.currentHp, stats.maxHp);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(hpText, x + 125 - fm.stringWidth(hpText) / 2, y + 22);

        // Layer info (neon cyan)
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(0, 255, 255));
        g.drawString("第 " + gm.currentLayer + " / 100 层", x + 8, y + 48);

        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        int rowY = y + 65;

        // Dash (neon cyan)
        g.setColor(new Color(0, 255, 255, 180));
        g.drawString("空格: 冲刺 " + pc.currentDashCharges + "/" + pc.maxDashCharges, x + 8, rowY);

        // Q ultimate
        String qStr = "Q: " + (pc.qUltimate != null ? pc.qUltimate.skillName : "无");
        if (pc.qCooldown > 0) qStr += String.format(" %.0fs", pc.qCooldown);
        else qStr += " 就绪";
        g.setColor(pc.qCooldown > 0 ? new Color(60, 60, 60) : new Color(0, 255, 100));
        g.drawString(qStr, x + 130, rowY);

        rowY += 15;

        // E universal
        String eStr = "E: " + (pc.eUniversal != null ? pc.eUniversal.skillName : "空槽");
        if (pc.eUniversal != null && pc.eCooldown > 0) eStr += String.format(" %.0fs", pc.eCooldown);
        else if (pc.eUniversal != null) eStr += " 就绪";
        g.setColor(pc.eUniversal == null ? new Color(40, 40, 40) : (pc.eCooldown > 0 ? new Color(60, 60, 60) : new Color(0, 255, 100)));
        g.drawString(eStr, x + 8, rowY);

        // Right click
        String rStr = "右键: " + (pc.rightClickSpecial != null ? pc.rightClickSpecial.skillName : "武器特攻");
        if (pc.rightClickCooldown > 0) rStr += String.format(" %.0fs", pc.rightClickCooldown);
        else rStr += " 就绪";
        g.setColor(pc.rightClickCooldown > 0 ? new Color(60, 60, 60) : new Color(0, 255, 255));
        g.drawString(rStr, x + 130, rowY);

        rowY += 15;

        // Enchants (neon orange)
        StringBuilder eb = new StringBuilder();
        for (int i = 0; i < pc.enchantSlots; i++) if (pc.enchantElements[i] != null) eb.append(i>0?"+":"").append(pc.enchantElements[i]);
        if (eb.length() > 0) { g.setColor(new Color(255, 180, 50)); g.drawString("附魔: " + eb.toString(), x + 8, rowY); }

        // Bottom hint (very dark, subtle)
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRoundRect(120, 572, 560, 22, 8, 8);
        g.setColor(new Color(40, 40, 60));
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
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(new Color(255, 50, 50));
        g.setFont(new Font("SansSerif", Font.BOLD, 42));
        drawCentered(g, "阵 亡", 120);

        g.setColor(new Color(255, 0, 255));
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        drawCentered(g, "你倒在了第 " + gm.currentLayer + " 层", 180);
        if (gm.currentLayer > 1) {
            g.setColor(new Color(255, 200, 0));
            drawCentered(g, "你的身影将成为第 " + (gm.currentLayer - 1) + " 层的敌人...", 210);
        }
        int currencyGain = gm.currentLayer * 10;
        g.setColor(new Color(0, 255, 255));
        drawCentered(g, "获得局外货币: " + currencyGain, 260);

        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        drawButton(g, "重新开始", 330, "restart", 0);
        g.setColor(new Color(100, 100, 100));
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        drawCentered(g, "按 ESC 返回主菜单", 400);
    }

    // ══════════════════════════════════════════════
    //  VICTORY
    // ══════════════════════════════════════════════
    private void drawVictory(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(new Color(255, 215, 0));
        g.setFont(new Font("SansSerif", Font.BOLD, 44));
        drawCentered(g, "通 关！", 120);

        g.setColor(new Color(255, 255, 200));
        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        drawCentered(g, "你征服了百层之镜", 190);
        g.setColor(new Color(255, 200, 100));
        drawCentered(g, "但你的身影将永远留在第99层...", 230);
        g.setColor(new Color(0, 255, 255));
        drawCentered(g, "下一次，最终的敌人将是你自己", 280);

        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        drawButton(g, "继续", 350, "continue", 0);
    }

    private Color getEnchantArcColor(PlayerController p) {
        if (p.enchantElements[0] == null) return new Color(0, 255, 255);
        switch (p.enchantElements[0]) {
            case "fire": return new Color(255, 80, 255);
            case "ice": return new Color(0, 200, 255);
            case "lightning": return new Color(255, 230, 50);
            default: return new Color(0, 255, 255);
        }
    }

    // ── FX helpers ──
    private void drawEnchantGlowNeon(Graphics2D g, int cx, int cy, String elem, int offset) {
        if (elem == null) return;
        Color glow;
        switch (elem) {
            case "fire": glow = new Color(255, 80, 255, 120); break;
            case "ice": glow = new Color(0, 200, 255, 120); break;
            case "lightning": glow = new Color(255, 230, 50, 120); break;
            default: glow = new Color(255, 255, 255, 80); break;
        }
        g.setColor(glow);
        g.setStroke(new BasicStroke(2));
        g.drawOval(cx - offset, cy - offset, offset * 2, offset * 2);
        g.setStroke(new BasicStroke(1));
    }

    private void drawFanOutline(Graphics2D g, int cx, int cy, float angle, float radius, int degrees, Color color) {
        // Convert math angle to Java2D screen angle (negate)
        int arcStart = -(int)Math.toDegrees(angle) - degrees / 2;
        // Outer glow arc
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() / 3));
        g.setStroke(new BasicStroke(4));
        g.drawArc(cx - (int)radius, cy - (int)radius, (int)radius * 2, (int)radius * 2, arcStart, degrees);
        // Inner bright arc
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f));
        g.drawArc(cx - (int)radius, cy - (int)radius, (int)radius * 2, (int)radius * 2, arcStart, degrees);
        g.setStroke(new BasicStroke(1));
    }

    // Keep drawFan for backward compatibility (used by existing code) - redirect to outline
    private void drawFan(Graphics2D g, int cx, int cy, float angle, float radius, int degrees, Color color) {
        drawFanOutline(g, cx, cy, angle, radius, degrees, color);
    }

    private void drawSlashLine(Graphics2D g, int cx, int cy, float angle, float length, Color color) {
        int ex = cx + (int)(Math.cos(angle) * length);
        int ey = cy + (int)(Math.sin(angle) * length);
        g.setColor(color);
        g.setStroke(new BasicStroke(2.5f));
        g.drawLine(cx, cy, ex, ey);
        g.setStroke(new BasicStroke(1));
    }

    private void drawTriangleOutline(Graphics2D g, int cx, int cy, int s) {
        int[] xs = {cx, cx - s, cx + s};
        int[] ys = {cy - s, cy + s, cy + s};
        g.drawPolygon(xs, ys, 3);
    }

    private void drawPauseOverlay(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 190));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(new Color(0, 255, 255));
        g.setFont(new Font("SansSerif", Font.BOLD, 36));
        drawCentered(g, "已暂停", 280);
        g.setColor(new Color(0, 200, 200, 150));
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        drawCentered(g, "按 ESC 继续", 340);
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

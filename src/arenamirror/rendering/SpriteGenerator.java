package arenamirror.rendering;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * Procedurally generates pixel-art sprite frames at runtime.
 * No external assets needed — everything is drawn with Java2D.
 */
public class SpriteGenerator {

    public static final int SPRITE_SIZE = 32;

    // 已生成的帧缓存 (key: "player_idle_0", "enemy_melee_0", etc.)
    private static final Map<String, BufferedImage> cache = new HashMap<>();

    /**
     * Returns direction index from velocity vector.
     * 0=down, 1=left, 2=right, 3=up
     */
    public static int getDirectionIndex(Vec2 vel) {
        if (vel == null) return 0;
        float ax = Math.abs(vel.x), ay = Math.abs(vel.y);
        if (ay > ax) return vel.y > 0 ? 0 : 3;  // down or up
        return vel.x < 0 ? 1 : 2;                // left or right
    }

    /** 4方向 × 4帧 的行走动画 */
    public static BufferedImage[] generateWalkFrames(Color body, Color accent, int dir) {
        BufferedImage[] frames = new BufferedImage[4];
        for (int f = 0; f < 4; f++) {
            String key = "walk_" + body.getRGB() + "_" + dir + "_" + f;
            if (cache.containsKey(key)) { frames[f] = cache.get(key); continue; }
            frames[f] = new BufferedImage(SPRITE_SIZE, SPRITE_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = frames[f].createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            // 身体 (中心椭圆) — 上下弹跳
            int bodyY = 10 + (int)(Math.sin(f * Math.PI / 2) * 2);
            g.setColor(body);
            g.fillOval(8, bodyY, 16, 16);

            // 头
            g.setColor(body.brighter());
            g.fillOval(10, bodyY - 8, 12, 10);

            // 眼睛 (根据朝向调整位置)
            g.setColor(Color.WHITE);
            int eyeX = (dir == 1) ? 11 : ((dir == 2) ? 19 : 15);
            g.fillOval(eyeX, bodyY - 4, 3, 3);
            g.setColor(Color.BLACK);
            g.fillOval(eyeX + 1, bodyY - 3, 1, 1);

            // 腿 (根据行走帧摆动)
            int legOffset = (int)(Math.sin(f * Math.PI / 2) * 3);
            g.setColor(accent);
            g.fillRect(11 + legOffset, bodyY + 14, 4, 6);
            g.fillRect(17 - legOffset, bodyY + 14, 4, 6);

            // 武器/手臂 (根据朝向)
            g.setColor(accent.darker());
            if (dir == 1) g.fillRect(4, bodyY + 4, 6, 3);       // 朝左
            else if (dir == 2) g.fillRect(22, bodyY + 4, 6, 3);  // 朝右
            else g.fillRect(20, bodyY + 2, 4, 6);                // 朝下/上

            g.dispose();
            cache.put(key, frames[f]);
        }
        return frames;
    }

    /** 闲置站立帧 */
    public static BufferedImage generateIdleFrame(Color body, Color accent) {
        String key = "idle_" + body.getRGB();
        if (cache.containsKey(key)) return cache.get(key);

        BufferedImage img = new BufferedImage(SPRITE_SIZE, SPRITE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        g.setColor(body);
        g.fillOval(8, 10, 16, 16);       // 身体
        g.setColor(body.brighter());
        g.fillOval(10, 2, 12, 10);        // 头
        g.setColor(Color.WHITE);
        g.fillOval(15, 6, 3, 3);          // 眼睛
        g.setColor(Color.BLACK);
        g.fillOval(16, 7, 1, 1);          // 瞳孔
        g.setColor(accent);
        g.fillRect(12, 24, 4, 6);         // 左腿
        g.fillRect(18, 24, 4, 6);         // 右腿
        g.fillRect(20, 12, 4, 6);         // 手臂

        g.dispose();
        cache.put(key, img);
        return img;
    }

    /** 攻击挥砍帧 */
    public static BufferedImage generateAttackFrame(Color body, Color accent, float angle) {
        String key = "atk_" + body.getRGB() + "_" + (int)(angle * 10);
        if (cache.containsKey(key)) return cache.get(key);

        BufferedImage img = new BufferedImage(SPRITE_SIZE, SPRITE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        g.setColor(body);
        g.fillOval(8, 10, 16, 16);
        g.setColor(body.brighter());
        g.fillOval(10, 2, 12, 10);

        // 挥砍的手臂延伸
        int armX = 22 + (int)(Math.cos(angle) * 8);
        int armY = 14 + (int)(Math.sin(angle) * 8);
        g.setColor(accent.darker());
        g.setStroke(new BasicStroke(3));
        g.drawLine(22, 14, armX, armY);
        g.setStroke(new BasicStroke(1));
        // 武器尖端
        g.setColor(Color.WHITE);
        g.fillOval(armX - 2, armY - 2, 4, 4);

        g.dispose();
        cache.put(key, img);
        return img;
    }

    /** 敌人精灵 (更小, 24x24) */
    public static BufferedImage generateEnemyFrame(Color body, int variant) {
        String key = "enemy_" + body.getRGB() + "_" + variant;
        if (cache.containsKey(key)) return cache.get(key);

        int size = 24;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        g.setColor(body);
        // 不同 variant 不同形状
        switch (variant % 3) {
            case 0: g.fillOval(4, 4, 16, 16); break;           // 圆形
            case 1: g.fillRect(4, 2, 16, 18); break;           // 方形
            case 2:                                                  // 三角形
                int[] xs = {12, 4, 20};
                int[] ys = {2, 20, 20};
                g.fillPolygon(xs, ys, 3);
                break;
        }

        // 眼睛 (红色发光)
        g.setColor(Color.RED);
        g.fillOval(8, 8, 3, 3);
        g.fillOval(14, 8, 3, 3);

        g.dispose();
        cache.put(key, img);
        return img;
    }

    /** 死亡碎裂帧 — 角色碎成碎片，眼睛变 X_X */
    public static BufferedImage generateDeathFrame(Color body) {
        String key = "death_" + body.getRGB();
        if (cache.containsKey(key)) return cache.get(key);

        BufferedImage img = new BufferedImage(SPRITE_SIZE, SPRITE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        Color dark = body.darker().darker();
        // 碎片
        Random rng = new Random(42);
        for (int i = 0; i < 6; i++) {
            int fx = 8 + rng.nextInt(16) - 8;
            int fy = 8 + rng.nextInt(16) - 8;
            g.setColor(i % 2 == 0 ? dark : body);
            g.fillRect(8 + fx, 8 + fy, 5, 5);
        }
        // X_X 眼
        g.setColor(Color.RED);
        g.drawLine(7, 5, 13, 9);
        g.drawLine(13, 5, 7, 9);
        g.drawLine(17, 5, 23, 9);
        g.drawLine(23, 5, 17, 9);

        g.dispose();
        cache.put(key, img);
        return img;
    }
}

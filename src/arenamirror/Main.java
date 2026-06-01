package arenamirror;

import arenamirror.core.*;
import arenamirror.player.*;
import arenamirror.enemies.*;
import arenamirror.data.*;
import arenamirror.skills.*;
import arenamirror.weapons.*;
import arenamirror.progression.*;
import arenamirror.traps.*;
import arenamirror.rendering.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Main extends JPanel implements KeyListener, MouseListener, MouseMotionListener, ActionListener {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final float DT = 1f / 60f;

    private GameManager gm;
    private GameRenderer renderer;
    private boolean[] keys = new boolean[256];
    private Vec2 mousePos = new Vec2(400, 300);
    private Timer gameTimer;

    public Main() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);

        // Init all system singletons
        new PlayerStats();
        new PlayerController();
        new PlayerSkillHandler();
        new SkillManager();
        new WeaponManager();
        new MetaProgression();
        new RewardSystem();
        new TrapManager();
        new SaveSystem();
        new PastLifeLog();

        gm = new GameManager();
        gm.player = PlayerController.instance;

        renderer = new GameRenderer();
        renderer.setGameManager(gm);

        // Event -> repaint
        gm.addRestAreaListener(this::repaintRenderer);
        gm.addBattleStartListener(() -> {
            gm.battleManager.prepareBattle(gm.currentLayer);
            TrapManager.instance.spawnTraps(gm.layerManager.getLayer(gm.currentLayer));
            repaintRenderer();
        });
        gm.addBattleEndListener(this::repaintRenderer);
        gm.addRewardEnteredListener(() -> {
            RewardSystem.instance.initForLayer();
            renderer.rewardSubState = GameRenderer.SUB_NONE;
            repaintRenderer();
        });
        gm.addRewardSelectedListener(() -> {
            renderer.rewardSubState = GameRenderer.SUB_NONE;
            repaintRenderer();
        });
        gm.addGameOverListener(this::repaintRenderer);
        gm.addVictoryListener(this::repaintRenderer);

        setLayout(new BorderLayout());
        add(renderer, BorderLayout.CENTER);

        gameTimer = new Timer((int)(DT * 1000), this);
        gameTimer.start();
    }

    private void repaintRenderer() {
        if (renderer != null) renderer.repaint();
    }

    // ══════════════════════════════════════════════
    //  GAME LOOP
    // ══════════════════════════════════════════════
    @Override
    public void actionPerformed(ActionEvent e) {
        if (gm.currentState == GameState.BATTLE && !gm.isPaused) {
            PlayerController pc = PlayerController.instance;
            // Don't process player input/movement while enemy is in death animation
            if (gm.currentEnemy == null || !gm.currentEnemy.isDead) {
                pc.handleKeyInput(keys, mousePos);
                pc.update(DT);
            } else {
                pc.velocity = new Vec2(0, 0);
            }
            // Always update enemy (even dead, so deathTimer counts down)
            if (gm.currentEnemy != null) gm.currentEnemy.update(DT);
        } else {
            PlayerController.instance.velocity = new Vec2(0, 0);
        }
        renderer.repaint();
    }

    // ══════════════════════════════════════════════
    //  KEYBOARD
    // ══════════════════════════════════════════════
    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code < keys.length) keys[code] = true;

        GameState state = gm.currentState;

        // Global: ESC = pause/back
        if (code == KeyEvent.VK_ESCAPE) {
            if (state == GameState.BATTLE) {
                gm.togglePause();
            } else if (state == GameState.REWARD_SELECTION) {
                // Back out of sub-menu or go back
                if (renderer.rewardSubState != GameRenderer.SUB_NONE) {
                    renderer.rewardSubState = GameRenderer.SUB_NONE;
                }
            }
            return;
        }

        if (state == GameState.MAIN_MENU) {
            if (code == KeyEvent.VK_ENTER) startGame();
        }
        else if (state == GameState.REST_AREA) {
            if (code == KeyEvent.VK_ENTER) gm.enterLayer(gm.currentLayer);
        }
        else if (state == GameState.REWARD_SELECTION) {
            if (renderer.rewardSubState != GameRenderer.SUB_NONE) return; // sub-menu handles via mouse
            handleRewardKey(code);
        }
        else if (state == GameState.GAME_OVER) {
            if (code == KeyEvent.VK_ENTER) startGame();
        }
        else if (state == GameState.VICTORY) {
            if (code == KeyEvent.VK_ENTER) startGame();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code < keys.length) keys[code] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    private void startGame() {
        gm.startNewRun();
    }

    private void handleRewardKey(int code) {
        switch (code) {
            case KeyEvent.VK_1: doReward(0); break;
            case KeyEvent.VK_2: doReward(1); break;
            case KeyEvent.VK_3: doReward(2); break;
            case KeyEvent.VK_4: doReward(3); break;
            case KeyEvent.VK_5: doReward(4); break;
        }
    }

    private void doReward(int index) {
        RewardSystem rs = RewardSystem.instance;
        switch (index) {
            case 0: rs.applyHeal(); gm.onRewardChosen(); break;
            case 1:
                rs.generateStatOptions();
                renderer.rewardSubState = GameRenderer.SUB_STAT_UPGRADE;
                break;
            case 2:
                // Open skill upgrade sub-menu
                renderer.rewardSubState = GameRenderer.SUB_SKILL_UPGRADE;
                break;
            case 3:
                // Open skill draw sub-menu
                renderer.rewardSubState = GameRenderer.SUB_SKILL_DRAW;
                renderer.drawnSkills = rs.drawSkills();
                break;
            case 4:
                gm.onRewardChosen(); // Random event
                break;
        }
    }

    // ══════════════════════════════════════════════
    //  MOUSE (clickable buttons)
    // ══════════════════════════════════════════════
    @Override
    public void mousePressed(MouseEvent e) {
        int mx = e.getX();
        int my = e.getY();
        int btn = e.getButton();

        GameState state = gm.currentState;

        // In battle, clicks go to combat
        if (state == GameState.BATTLE) {
            PlayerController.instance.handleMouseClick(btn);
            return;
        }

        // Check click regions
        GameRenderer.ClickRegion cr = renderer.getClickAt(mx, my);
        if (cr == null) return;

        String action = cr.action;
        RewardSystem rs = RewardSystem.instance;

        switch (action) {
            case "start_run":      startGame(); break;
            case "enter_battle":   gm.enterLayer(gm.currentLayer); break;
            case "restart":        startGame(); break;
            case "continue":       startGame(); break;

            // Main reward buttons
            case "reward_heal":    doReward(0); break;
            case "reward_stat":    doReward(1); break;
            case "reward_upgrade": doReward(2); break;
            case "reward_draw":    doReward(3); break;
            case "reward_event":   doReward(4); break;

            // Back from sub-menu
            case "reward_back":
                renderer.rewardSubState = GameRenderer.SUB_NONE;
                break;

            // Skill draw pick
            case "skill_pick":
                if (renderer.drawnSkills != null && cr.index < renderer.drawnSkills.size()) {
                    SkillData chosen = renderer.drawnSkills.get(cr.index);
                    SkillInstance conflict = PlayerSkillHandler.instance.findConflictingSkill(chosen);
                    if (conflict != null) {
                        renderer.pendingNewSkill = chosen;
                        renderer.pendingConflict = conflict;
                        renderer.rewardSubState = GameRenderer.SUB_REPLACE_CONFIRM;
                    } else {
                        PlayerSkillHandler.instance.tryAcquireSkill(chosen);
                        gm.onRewardChosen();
                    }
                }
                break;

            // Reroll
            case "skill_reroll":
                if (rs.rerollSkillDraw()) {
                    renderer.drawnSkills = rs.drawSkills();
                }
                break;

            // Skill upgrade pick
            // Stat upgrade pick
            case "stat_pick":
                rs.applyStatUpgrade(cr.index);
                gm.onRewardChosen();
                break;

            case "upgrade_pick":
                java.util.List<SkillData> up = rs.getUpgradeableSkills();
                if (up != null && cr.index < up.size()) {
                    rs.upgradeChosenSkill(up.get(cr.index));
                    gm.onRewardChosen();
                }
                break;

            // Replace confirmation
            case "replace_yes":
                if (renderer.pendingNewSkill != null && renderer.pendingConflict != null) {
                    PlayerSkillHandler.instance.replaceSkill(renderer.pendingConflict, renderer.pendingNewSkill);
                }
                gm.onRewardChosen();
                break;

            case "replace_no":
                renderer.rewardSubState = GameRenderer.SUB_SKILL_DRAW;
                break;
        }
    }

    @Override public void mouseMoved(MouseEvent e) {
        mousePos = new Vec2(e.getX(), e.getY());
        // Always sync aim direction, even outside battle
        if (PlayerController.instance != null) {
            Vec2 toMouse = mousePos.sub(PlayerController.instance.position);
            if (toMouse.length() > 0.01f) {
                PlayerController.instance.aimDirection = toMouse.normalized();
            }
            PlayerController.instance.mouseWorldPos = mousePos;
        }
    }

    @Override public void mouseDragged(MouseEvent e) {
        mousePos = new Vec2(e.getX(), e.getY());
        if (PlayerController.instance != null) {
            Vec2 toMouse = mousePos.sub(PlayerController.instance.position);
            if (toMouse.length() > 0.01f) {
                PlayerController.instance.aimDirection = toMouse.normalized();
            }
            PlayerController.instance.mouseWorldPos = mousePos;
        }
    }

    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    // ══════════════════════════════════════════════
    //  ENTRY
    // ══════════════════════════════════════════════
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("角斗场：百层之镜");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            Main game = new Main();
            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            System.out.println("=== 角斗场：百层之镜 ===");
            System.out.println("\"你的敌人终将全是你\"");
            System.out.println("点击按钮或按 ENTER 开始游戏");
        });
    }
}

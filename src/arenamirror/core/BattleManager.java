package arenamirror.core;

import arenamirror.data.*;
import arenamirror.enemies.*;
import arenamirror.rendering.Vec2;
import javax.swing.Timer;

public class BattleManager {
    public boolean battleActive;
    private Timer startTimer;

    public BattleManager() {}

    public void prepareBattle(int layer) {
        GameManager gm = GameManager.instance;
        LayerSlot slot = gm.layerManager.getLayer(layer);
        if (slot == null || slot.isEmpty()) {
            System.err.println("[BattleManager] No enemy data for layer " + layer);
            return;
        }

        battleActive = false;

        // Spawn enemy
        Vec2 spawnPos = new Vec2(GameManager.ARENA_CENTER.x + 100, GameManager.ARENA_CENTER.y);
        EnemyBase enemy = new EnemyBase();
        enemy.initialize(slot, layer);
        enemy.position = spawnPos;
        gm.currentEnemy = enemy;

        // Reset player position
        if (gm.player != null) {
            gm.player.position = new Vec2(GameManager.ARENA_CENTER.x - 100, GameManager.ARENA_CENTER.y);
            gm.player.velocity = new Vec2(0, 0);
            gm.player.resetCombatState();
        }

        // Delay before battle starts
        battleActive = true;
    }

    public void onEnemyKilled() {
        if (!battleActive) return;
        battleActive = false;

        GameManager gm = GameManager.instance;
        gm.currentEnemy = null;
        gm.onBattleWon();
    }

    public void onPlayerKilled() {
        if (!battleActive) return;
        battleActive = false;

        GameManager gm = GameManager.instance;
        gm.currentEnemy = null;
        gm.onBattleLost();
    }
}

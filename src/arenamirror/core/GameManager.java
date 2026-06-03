package arenamirror.core;

import arenamirror.data.*;
import arenamirror.player.*;
import arenamirror.enemies.*;
import arenamirror.progression.*;
import arenamirror.rendering.Vec2;
import arenamirror.data.LayerCurveData;
import java.util.*;

public class GameManager {
    public static GameManager instance;

    public GameState currentState = GameState.MAIN_MENU;
    public int currentLayer;
    public int currentRunLayer = 1;
    public boolean isPaused;

    public LayerManager layerManager;
    public BattleManager battleManager;

    // world bounds (arena)
    public static final float ARENA_RADIUS = 280f;
    public static final Vec2 ARENA_CENTER = new Vec2(400, 300);

    // enemy spawned in battle
    public EnemyBase currentEnemy;
    // player reference
    public PlayerController player;

    public LayerCurveData layerCurve = new LayerCurveData();

    // event listeners
    private List<Runnable> onRestAreaEntered = new ArrayList<>();
    private List<Runnable> onBattleStart = new ArrayList<>();
    private List<Runnable> onBattleEnd = new ArrayList<>();
    private List<Runnable> onRewardEntered = new ArrayList<>();
    private List<Runnable> onRewardSelected = new ArrayList<>();
    private List<Runnable> onGameOver = new ArrayList<>();
    private List<Runnable> onVictory = new ArrayList<>();

    public GameManager() {
        instance = this;
        layerManager = new LayerManager();
        battleManager = new BattleManager();
    }

    // --- event listeners ---
    public void addRestAreaListener(Runnable r) { onRestAreaEntered.add(r); }
    public void addBattleStartListener(Runnable r) { onBattleStart.add(r); }
    public void addBattleEndListener(Runnable r) { onBattleEnd.add(r); }
    public void addRewardEnteredListener(Runnable r) { onRewardEntered.add(r); }
    public void addRewardSelectedListener(Runnable r) { onRewardSelected.add(r); }
    public void addGameOverListener(Runnable r) { onGameOver.add(r); }
    public void addVictoryListener(Runnable r) { onVictory.add(r); }

    private void fire(List<Runnable> list) { for (Runnable r : list) r.run(); }

    public void startNewRun() {
        currentLayer = 0;
        currentRunLayer = 1;

        // MUST clear old skills BEFORE resetting stats, or old bonuses corrupt fresh values
        PlayerSkillHandler.instance.clearAllSkills();
        PlayerStats.instance.initForNewRun(
            PlayerStats.instance.currentCharacter,
            PlayerStats.instance.currentWeapon
        );
        // Apply character passive
        if (PlayerStats.instance.currentCharacter.passiveSkill != null) {
            PlayerSkillHandler.instance.tryAcquireSkill(PlayerStats.instance.currentCharacter.passiveSkill);
        }
        RewardSystem.instance.initForNewRun();
        RewardSystem.instance.initForLayer();

        transitionTo(GameState.REST_AREA);
        fire(onRestAreaEntered);
    }

    public void enterLayer(int layer) {
        currentLayer = layer;
        currentRunLayer = layer;
        transitionTo(GameState.BATTLE);
        fire(onBattleStart);
    }

    public void onBattleWon() {
        fire(onBattleEnd);
        if (currentLayer >= 100) {
            handleVictory();
            return;
        }
        transitionTo(GameState.REWARD_SELECTION);
        fire(onRewardEntered);
    }

    public void onBattleLost() {
        fire(onBattleEnd);
        // Layer 1 or below: no past life created, just deleted
        if (currentLayer <= 1) {
            PastLifeLog.instance.recordSink(currentLayer, 1);
            MetaProgression.instance.addCurrency(currentLayer * 10);
            transitionTo(GameState.GAME_OVER);
            fire(onGameOver);
            return;
        }
        int insertionLayer = Math.max(1, currentLayer - 1);
        layerManager.handleSink(currentLayer, insertionLayer);
        PastLifeLog.instance.recordSink(currentLayer, insertionLayer);
        MetaProgression.instance.addCurrency(currentLayer * 10);
        transitionTo(GameState.GAME_OVER);
        fire(onGameOver);
    }

    private void handleVictory() {
        layerManager.handleSink(100, 100);
        transitionTo(GameState.VICTORY);
        fire(onVictory);
    }

    public void onRewardChosen() {
        fire(onRewardSelected);
        currentLayer++;
        currentRunLayer = currentLayer;
        transitionTo(GameState.REST_AREA);
        fire(onRestAreaEntered);
    }

    public void togglePause() {
        isPaused = !isPaused;
    }

    private void transitionTo(GameState newState) {
        currentState = newState;
    }

    public LayerStatEntry getLayerStats(int layer) {
        return layerCurve.getLayerStats(layer);
    }
}

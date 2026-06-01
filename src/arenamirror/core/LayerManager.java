package arenamirror.core;

import arenamirror.data.*;
import arenamirror.enemies.*;
import arenamirror.player.*;
import java.util.*;
import java.time.LocalDateTime;

public class LayerManager {
    public LayerSlot[] layers = new LayerSlot[100];
    public EnemyTemplateData whiteboardEnemy;
    private int pastLifeCounter = 0;

    public LayerManager() {
        whiteboardEnemy = new EnemyTemplateData(EnemySource.PRESET, EnemyRace.HUMANOID, BehaviorPattern.MELEE_AGGRESSIVE, StatTendency.GLASS_CANNON);
        initializeLayers();
    }

    public LayerSlot getLayer(int layerNumber) {
        if (layerNumber == 0) return createWhiteboardSlot();
        if (layerNumber < 1 || layerNumber > 100) return null;
        return layers[layerNumber - 1];
    }

    private LayerSlot createWhiteboardSlot() {
        LayerSlot slot = new LayerSlot(0);
        slot.enemySource = EnemySource.PRESET;
        slot.templateData = whiteboardEnemy;
        slot.originalMaxHp = 10f;
        slot.originalAttack = 2f;
        slot.originalSpeed = 2f;
        slot.originalDefense = 0f;
        return slot;
    }

    public void initializeLayers() {
        for (int i = 0; i < 100; i++) {
            layers[i] = new LayerSlot(i + 1);
            layers[i].enemySource = EnemySource.PRESET;
            layers[i].templateData = EnemyFactory.generatePresetEnemy(i + 1);
        }
    }

    /**
     * Sink logic:
     * Player fails at layer N -> they become enemy at layer N-1
     * Layers 1..N-1 shift down, layer 1 gets pushed out
     */
    public void handleSink(int failedLayer, int insertionLayer) {
        if (failedLayer <= 0) return;

        int insertIndex = insertionLayer - 1;
        if (insertIndex < 0 || insertIndex >= 100) return;

        LayerSlot pastLifeSlot = createPastLifeSlot(insertionLayer);

        // Shift layers 1..insertIndex down
        if (insertIndex > 0) {
            for (int i = 0; i < insertIndex; i++) {
                layers[i] = layers[i + 1];
                layers[i].layerNumber = i + 1;
            }
        }

        layers[insertIndex] = pastLifeSlot;

        // Realign layer numbers above insertion point
        for (int i = insertIndex + 1; i < Math.min(100, failedLayer); i++) {
            if (layers[i] != null) layers[i].layerNumber = i + 1;
        }
    }

    private LayerSlot createPastLifeSlot(int targetLayer) {
        PlayerStats stats = PlayerStats.instance;
        PlayerSkillHandler skillHandler = PlayerSkillHandler.instance;
        GameManager gm = GameManager.instance;

        LayerSlot slot = new LayerSlot(targetLayer);
        slot.enemySource = EnemySource.PAST_LIFE;
        slot.pastLifeId = ++pastLifeCounter;
        slot.originalSkills = new ArrayList<>(skillHandler.getAllSkills());

        slot.pastLifeRecord = new PastLifeRecord();
        slot.pastLifeRecord.timestamp = LocalDateTime.now();
        slot.pastLifeRecord.layerReached = gm != null ? gm.currentLayer : 0;
        slot.pastLifeRecord.characterName = stats.currentCharacter.characterName;
        slot.pastLifeRecord.weaponName = stats.currentWeapon.weaponName;
        slot.pastLifeRecord.skillNames = skillHandler.getSkillNames();

        slot.originalCharacter = stats.currentCharacter;
        slot.originalWeapon = stats.currentWeapon;
        slot.originalMaxHp = stats.maxHp;
        slot.originalAttack = stats.attack;
        slot.originalSpeed = stats.moveSpeed;
        slot.originalDefense = stats.defense;

        // Build behavior based on weapon type
        BehaviorPattern bp = BehaviorPattern.MELEE_AGGRESSIVE;
        if (stats.currentWeapon.attackPattern == AttackPattern.RANGED_PROJECTILE ||
            stats.currentWeapon.attackPattern == AttackPattern.RANGED_BEAM) {
            bp = BehaviorPattern.RANGED_KITING;
        }
        slot.templateData = new EnemyTemplateData(EnemySource.PAST_LIFE, EnemyRace.HUMANOID, bp, StatTendency.SPEEDSTER);
        slot.templateData.skills = slot.originalSkills;

        return slot;
    }

    public LayerSlot[] getAllLayers() { return layers; }
}

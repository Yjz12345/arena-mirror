package arenamirror.data;

import java.util.List;
import java.util.ArrayList;

public class LayerSlot {
    public int layerNumber;
    public EnemySource enemySource;
    public EnemyTemplateData templateData;
    public PastLifeRecord pastLifeRecord;

    public CharacterData originalCharacter;
    public WeaponData originalWeapon;
    public List<SkillData> originalSkills = new ArrayList<>();
    public float originalMaxHp;
    public float originalAttack;
    public float originalSpeed;
    public float originalDefense;
    public int pastLifeId; // unique color ID per past life run

    public LayerSlot(int layer) {
        this.layerNumber = layer;
        this.enemySource = EnemySource.PRESET;
    }

    public boolean isEmpty() { return templateData == null; }
}

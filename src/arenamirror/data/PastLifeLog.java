package arenamirror.data;

import arenamirror.player.*;
import arenamirror.core.*;
import java.time.LocalDateTime;
import java.util.*;

public class PastLifeLog {
    public static PastLifeLog instance;

    public List<PastLifeRecord> records = new ArrayList<>();
    public int maxLogEntries = 100;

    public PastLifeLog() {
        instance = this;
    }

    public void recordSink(int failedLayer, int insertionLayer) {
        PastLifeRecord record = new PastLifeRecord();
        record.timestamp = LocalDateTime.now();
        record.layerReached = failedLayer;
        record.characterName = PlayerStats.instance.currentCharacter.characterName;
        record.weaponName = PlayerStats.instance.currentWeapon.weaponName;
        record.skillNames = PlayerSkillHandler.instance.getSkillNames();

        records.add(0, record);
        if (records.size() > maxLogEntries) {
            records.remove(records.size() - 1);
        }

        System.out.printf("[前世日志] %s 败于第%d层 (%s, %d技能) -> 第%d层\n",
            record.characterName, failedLayer, record.weaponName,
            record.skillNames.size(), insertionLayer);
    }
}

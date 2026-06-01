package arenamirror.data;

import java.util.*;

public class SaveData {
    public int version = 1;
    public int metaCurrency;
    public List<String> unlockedCharacterIds = new ArrayList<>();
    public List<String> unlockedWeaponIds = new ArrayList<>();
    public List<String> unlockedSkillIds = new ArrayList<>();
    public Map<String, Integer> upgradeLevels = new HashMap<>();
    public List<PastLifeRecord> pastLifeLog = new ArrayList<>();
    public int totalRuns;
    public int totalDeaths;
    public int totalWins;
    public int deepestLayer;
}

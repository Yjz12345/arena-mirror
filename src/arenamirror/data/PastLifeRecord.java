package arenamirror.data;

import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;

public class PastLifeRecord {
    public LocalDateTime timestamp;
    public int layerReached;
    public String characterName;
    public String weaponName;
    public List<String> skillNames = new ArrayList<>();

    @Override public String toString() {
        return String.format("[%s] %s 败于第%d层 (%s, %d技能)",
            timestamp.toLocalDate(), characterName, layerReached, weaponName, skillNames.size());
    }
}

package arenamirror.data;

import java.util.List;
import java.util.ArrayList;

public class TrapTemplateData {
    public String templateName;
    public List<String> matchTags = new ArrayList<>();
    public List<String> excludeTags = new ArrayList<>();
    public List<TrapSpawnEntry> trapEntries = new ArrayList<>();

    public TrapTemplateData() {}

    public TrapTemplateData(String name) {
        this.templateName = name;
    }
}

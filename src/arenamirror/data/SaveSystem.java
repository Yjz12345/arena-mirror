package arenamirror.data;

import java.io.*;
import java.nio.file.*;
import java.util.StringJoiner;

public class SaveSystem {
    public static SaveSystem instance;

    private static final String SAVE_PATH = "arenamirror_save.txt";

    public SaveSystem() {
        instance = this;
    }

    public SaveData load() {
        // Basic save not implemented in first pass
        return new SaveData();
    }

    public void save(SaveData data) {
        // Basic save not implemented in first pass
    }

    public void deleteSave() {
        try { Files.deleteIfExists(Path.of(SAVE_PATH)); } catch (Exception e) {}
    }

    public boolean hasSave() {
        return Files.exists(Path.of(SAVE_PATH));
    }
}

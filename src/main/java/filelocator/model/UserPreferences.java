package filelocator.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class UserPreferences {
    private static final File PREFS_FILE = new File("user-preferences.json");
    private String defaultLocation = "C:\\";

    public static UserPreferences load() {
        UserPreferences prefs = new UserPreferences();
        if (PREFS_FILE.exists()) {
            try {
                String content = Files.readString(PREFS_FILE.toPath());
                // extremely simplified json parsing to avoid new dependencies
                if (content.contains("\"defaultLocation\"")) {
                    int colonIndex = content.indexOf(":");
                    if (colonIndex != -1) {
                        int startQuote = content.indexOf("\"", colonIndex);
                        int endQuote = content.indexOf("\"", startQuote + 1);
                        if (startQuote != -1 && endQuote != -1) {
                            String loc = content.substring(startQuote + 1, endQuote).replace("\\\\", "\\");
                            prefs.setDefaultLocation(loc);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to read user preferences", e);
            }
        } else {
            // First run: save defaults
            prefs.save();
        }
        return prefs;
    }

    public void save() {
        try {
            String json = "{\n  \"defaultLocation\": \"" + defaultLocation.replace("\\", "\\\\") + "\"\n}";
            Files.writeString(PREFS_FILE.toPath(), json);
        } catch (IOException e) {
            log.warn("Failed to save user preferences", e);
        }
    }
}

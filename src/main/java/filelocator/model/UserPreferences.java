package filelocator.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Logger;
import java.util.logging.Level;
import lombok.Data;

@Data
public class UserPreferences {
    private static final Logger log = Logger.getLogger(UserPreferences.class.getName());
    private static final File PREFS_FILE = new File("user-preferences.json");
    private String defaultLocation = "C:\\";
    private java.util.List<String> recentLocations = new java.util.ArrayList<>();
    private String theme = "Dark";
    private long lastIndexingTime = 0;

    public static UserPreferences load() {
        UserPreferences prefs = new UserPreferences();
        if (PREFS_FILE.exists()) {
            try {
                String content = Files.readString(PREFS_FILE.toPath());
                
                // Parse defaultLocation
                int defIndex = content.indexOf("\"defaultLocation\"");
                if (defIndex != -1) {
                    int colonIndex = content.indexOf(":", defIndex);
                    if (colonIndex != -1) {
                        int startQuote = content.indexOf("\"", colonIndex);
                        int endQuote = content.indexOf("\"", startQuote + 1);
                        if (startQuote != -1 && endQuote != -1) {
                            String loc = content.substring(startQuote + 1, endQuote).replace("\\\\", "\\");
                            prefs.setDefaultLocation(loc);
                        }
                    }
                }

                // Parse theme
                int themeIndex = content.indexOf("\"theme\"");
                if (themeIndex != -1) {
                    int colonIndex = content.indexOf(":", themeIndex);
                    if (colonIndex != -1) {
                        int startQuote = content.indexOf("\"", colonIndex);
                        int endQuote = content.indexOf("\"", startQuote + 1);
                        if (startQuote != -1 && endQuote != -1) {
                            String t = content.substring(startQuote + 1, endQuote).trim();
                            if ("Light".equalsIgnoreCase(t) || "Dark".equalsIgnoreCase(t)) {
                                prefs.setTheme(t);
                            }
                        }
                    }
                }

                // Parse lastIndexingTime
                int timeIndex = content.indexOf("\"lastIndexingTime\"");
                if (timeIndex != -1) {
                    int colonIndex = content.indexOf(":", timeIndex);
                    if (colonIndex != -1) {
                        int commaIndex = content.indexOf(",", colonIndex);
                        if (commaIndex == -1) {
                            commaIndex = content.indexOf("}", colonIndex);
                        }
                        if (commaIndex != -1) {
                            try {
                                String val = content.substring(colonIndex + 1, commaIndex).trim();
                                prefs.setLastIndexingTime(Long.parseLong(val));
                            } catch (Exception e) {
                                // Ignore
                            }
                        }
                    }
                }

                // Parse recentLocations
                int recIndex = content.indexOf("\"recentLocations\"");
                if (recIndex != -1) {
                    int startArray = content.indexOf("[", recIndex);
                    int endArray = content.indexOf("]", startArray);
                    if (startArray != -1 && endArray != -1) {
                        String arrayContent = content.substring(startArray + 1, endArray);
                        String[] items = arrayContent.split(",");
                        for (String item : items) {
                            item = item.trim();
                            if (item.startsWith("\"") && item.endsWith("\"")) {
                                String val = item.substring(1, item.length() - 1).replace("\\\\", "\\").trim();
                                if (!val.isEmpty()) {
                                    prefs.getRecentLocations().add(val);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.log(Level.WARNING, "Failed to read user preferences", e);
            }
        } else {
            // First run: save defaults
            prefs.save();
        }
        return prefs;
    }

    public void save() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"theme\": \"").append(theme).append("\",\n");
            sb.append("  \"defaultLocation\": \"").append(defaultLocation.replace("\\", "\\\\")).append("\",\n");
            sb.append("  \"lastIndexingTime\": ").append(lastIndexingTime).append(",\n");
            sb.append("  \"recentLocations\": [\n");
            for (int i = 0; i < recentLocations.size(); i++) {
                sb.append("    \"").append(recentLocations.get(i).replace("\\", "\\\\")).append("\"");
                if (i < recentLocations.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("  ]\n");
            sb.append("}");
            Files.writeString(PREFS_FILE.toPath(), sb.toString());
        } catch (IOException e) {
            log.log(Level.WARNING, "Failed to save user preferences", e);
        }
    }

    public void addRecentLocation(String path) {
        if (path == null || path.isBlank() || "This PC".equalsIgnoreCase(path)) {
            return;
        }
        String normalized = new File(path).getAbsolutePath();
        recentLocations.remove(normalized);
        recentLocations.add(0, normalized);
        if (recentLocations.size() > 10) {
            recentLocations = new java.util.ArrayList<>(recentLocations.subList(0, 10));
        }
        save();
    }
}

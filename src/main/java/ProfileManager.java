import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class ProfileManager {

    public static final String USER_HOME = System.getProperty("user.home");
    public static final String SAVE_DIR = USER_HOME + File.separator + ".steamrandomizer";

    // Interne Hilfsmethode
    private static File getProfileFile(String profileName) {
        return new File(SAVE_DIR + File.separator + profileName + ".json");
    }

    // Liefert eine Liste aller existierenden Profil-Namen
    public static List<String> getAvailableProfiles() {
        List<String> profiles = new ArrayList<>();
        File dir = new File(SAVE_DIR);
        if (!dir.exists()) dir.mkdirs();

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File f : files) {
                profiles.add(f.getName().replace(".json", ""));
            }
        }
        if (profiles.isEmpty()) profiles.add("Default");
        
        return profiles;
    }

    // Speichert ein Profil
    public static void saveProfile(String profileName, UserProfile profile) throws Exception {
        File dir = new File(SAVE_DIR);
        if (!dir.exists()) dir.mkdirs();

        try (FileWriter writer = new FileWriter(getProfileFile(profileName))) {
            Gson gson = new Gson();
            gson.toJson(profile, writer);
        }
    }

    // Lädt ein Profil
    public static UserProfile loadProfile(String profileName) throws Exception {
        File file = getProfileFile(profileName);
        if (!file.exists()) {
            return new UserProfile(); // Wenn es nicht existiert, geben wir ein leeres Profil zurück
        }

        try (FileReader reader = new FileReader(file)) {
            Gson gson = new Gson();
            return gson.fromJson(reader, UserProfile.class);
        }
    }

    // Löscht ein Profil
    public static boolean deleteProfile(String profileName) {
        File file = getProfileFile(profileName);
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }
}
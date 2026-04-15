import com.google.gson.Gson; // NEU
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.io.File; // NEU
import java.io.FileReader; // NEU
import java.io.FileWriter; // NEU
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ViewController {

    @FXML
    private ListView<SteamGameRandompicker.Game> gamePoolListView;
    @FXML
    private ListView<SteamGameRandompicker.Game> gameLibraryListView;
    @FXML
    private TextField searchField;
    @FXML
    private TextField customGameField;
    @FXML
    private Button excludeButton;
    @FXML
    private Button includeButton;
    @FXML
    private Button randomButton;
    @FXML
    private Label resultLabel;
    @FXML
    private ImageView resultImageView;

    private ObservableList<SteamGameRandompicker.Game> poolGames;
    private ObservableList<SteamGameRandompicker.Game> libraryGames;
    private FilteredList<SteamGameRandompicker.Game> filteredLibraryGames;

    private Map<Integer, Image> imageCache = new HashMap<>();
    private Random random = new Random();
    private int customAppIdCounter = -1;

    // ALT
    //private static final String PROFILE_FILE = "profile.json";

    // NEU: Wir holen uns den Benutzerordner und erstellen einen eigenen Unterordner
    private static final String USER_HOME = System.getProperty("user.home");
    private static final String SAVE_DIR = USER_HOME + File.separator + ".steamrandomizer";
    private static final String PROFILE_FILE = SAVE_DIR + File.separator + "profile.json";

    @FXML
    public void initialize() {
        poolGames = FXCollections.observableArrayList();
        libraryGames = FXCollections.observableArrayList();

        gamePoolListView.setItems(poolGames);
        filteredLibraryGames = new FilteredList<>(libraryGames, p -> true);
        gameLibraryListView.setItems(filteredLibraryGames);

        gamePoolListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        gameLibraryListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        setCustomCellFactory(gamePoolListView);
        setCustomCellFactory(gameLibraryListView);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredLibraryGames.setPredicate(game -> {
                if (newValue == null || newValue.isEmpty())
                    return true;
                return game.name.toLowerCase().contains(newValue.toLowerCase());
            });
        });

        // Spiele laden (danach wird automatisch das Profil geladen)
        loadGamesAsync();
    }

    private void setCustomCellFactory(ListView<SteamGameRandompicker.Game> listView) {
        // ... (Dieser Code bleibt exakt gleich wie vorher) ...
        listView.setCellFactory(param -> new ListCell<>() {
            private HBox hbox = new HBox(10);
            private ImageView imageView = new ImageView();
            private Label label = new Label();
            {
                imageView.setFitWidth(120);
                imageView.setPreserveRatio(true);
                label.setStyle("-fx-alignment: center-left; -fx-padding: 5 0 0 0;");
                hbox.getChildren().addAll(imageView, label);
            }

            @Override
            protected void updateItem(SteamGameRandompicker.Game game, boolean empty) {
                super.updateItem(game, empty);
                if (empty || game == null) {
                    setGraphic(null);
                } else {
                    label.setText(game.name);
                    if (game.appid > 0) {
                        if (!imageCache.containsKey(game.appid)) {
                            String imageUrl = "https://cdn.akamai.steamstatic.com/steam/apps/" + game.appid
                                    + "/capsule_184x69.jpg";
                            Image img = new Image(imageUrl, true);
                            imageCache.put(game.appid, img);
                        }
                        imageView.setImage(imageCache.get(game.appid));
                    } else {
                        imageView.setImage(null);
                    }
                    setGraphic(hbox);
                }
            }
        });
    }

    private void loadGamesAsync() {
        Task<List<SteamGameRandompicker.Game>> loadGamesTask = new Task<>() {
            @Override
            protected List<SteamGameRandompicker.Game> call() throws Exception {
                SteamGameRandompicker fetcher = new SteamGameRandompicker();
                return fetcher.fetchOwnedGames();
            }
        };

        loadGamesTask.setOnSucceeded(event -> {
            List<SteamGameRandompicker.Game> games = loadGamesTask.getValue();
            libraryGames.setAll(games);

            // ### NEU: Wenn Steam fertig geladen hat, versuchen wir unser Profil zu laden
            // ###
            loadProfile();

            randomButton.setDisable(false);
            excludeButton.setDisable(false);
            includeButton.setDisable(false);
            resultLabel.setText("Bibliothek & Profil geladen!");
        });

        loadGamesTask.setOnFailed(event -> {
            resultLabel.setText("Fehler beim Laden der Spiele!");
        });

        randomButton.setDisable(true);
        excludeButton.setDisable(true);
        includeButton.setDisable(true);
        resultLabel.setText("Lade Spiele von der Steam API...");

        new Thread(loadGamesTask).start();
    }

    // ==========================================
    // NEUE METHODEN FÜR SPEICHERN UND LADEN
    // ==========================================

    @FXML
    private void onSaveProfileClick() {
        
        // NEU: Stelle sicher, dass der Ordner existiert, bevor wir speichern
        File dir = new File(SAVE_DIR);
        if (!dir.exists()) {
            dir.mkdirs(); // Erstellt den Ordner .steamrandomizer, falls er fehlt
        }

        UserProfile profile = new UserProfile();

        // 1. Alle Custom Games sichern (sowohl aus Bibliothek als auch aus Pool)
        for (SteamGameRandompicker.Game g : libraryGames) {
            if (g.appid < 0)
                profile.customGames.add(g);
        }
        for (SteamGameRandompicker.Game g : poolGames) {
            if (g.appid < 0)
                profile.customGames.add(g);

            // 2. Alle IDs der Pool-Spiele sichern (Steam + Custom)
            profile.poolAppIds.add(g.appid);
        }

        // 3. In Datei schreiben
        try (FileWriter writer = new FileWriter(PROFILE_FILE)) {
            Gson gson = new Gson();
            gson.toJson(profile, writer);
            resultLabel.setText("✅ Profil erfolgreich gespeichert!");
        } catch (Exception e) {
            System.err.println("Fehler beim Speichern:");
            e.printStackTrace();
            resultLabel.setText("❌ Fehler beim Speichern!");
        }
    }

    private void loadProfile() {
        File file = new File(PROFILE_FILE);
        if (!file.exists()) {
            return; // Kein Profil vorhanden, wir starten leer. Alles gut!
        }

        try (FileReader reader = new FileReader(file)) {
            Gson gson = new Gson();
            UserProfile profile = gson.fromJson(reader, UserProfile.class);

            if (profile != null) {
                // 1. Custom Games der Bibliothek hinzufügen
                if (profile.customGames != null) {
                    libraryGames.addAll(0, profile.customGames); // Oben anfügen

                    // Den ID-Counter aktualisieren, damit neue Custom Games keine ID-Kollisionen
                    // verurshen
                    for (SteamGameRandompicker.Game cg : profile.customGames) {
                        if (cg.appid <= customAppIdCounter) {
                            customAppIdCounter = cg.appid - 1;
                        }
                    }
                }

                // 2. Spiele in den Pool verschieben
                if (profile.poolAppIds != null) {
                    List<SteamGameRandompicker.Game> gamesToMove = new ArrayList<>();

                    // Wir suchen die passenden Spiele in der Bibliothek
                    for (SteamGameRandompicker.Game g : libraryGames) {
                        if (profile.poolAppIds.contains(g.appid)) {
                            gamesToMove.add(g);
                        }
                    }

                    // Verschieben
                    poolGames.addAll(gamesToMove);
                    libraryGames.removeAll(gamesToMove);
                }
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Laden des Profils:");
            e.printStackTrace();
        }
    }

    /**
     * NEU: Diese Klasse definiert, wie unsere JSON-Datei aufgebaut ist.
     */
    public static class UserProfile {
        public List<SteamGameRandompicker.Game> customGames = new ArrayList<>();
        public List<Integer> poolAppIds = new ArrayList<>();
    }

    // ==========================================
    // AB HIER WIEDER DIE ALTEN BUTTON-METHODEN
    // ==========================================

    @FXML
    private void onAddCustomGameClick() {
        String gameName = customGameField.getText().trim();
        if (gameName.isEmpty())
            return;

        SteamGameRandompicker.Game customGame = new SteamGameRandompicker.Game();
        customGame.appid = customAppIdCounter--;
        customGame.name = gameName;
        customGame.playtime_forever = 0;

        libraryGames.add(0, customGame);
        customGameField.clear();
    }

    @FXML
    private void onExcludeClick() {
        List<SteamGameRandompicker.Game> selected = new ArrayList<>(
                gamePoolListView.getSelectionModel().getSelectedItems());
        if (selected.isEmpty())
            return;
        libraryGames.addAll(selected);
        poolGames.removeAll(selected);
    }

    @FXML
    private void onIncludeClick() {
        List<SteamGameRandompicker.Game> selected = new ArrayList<>(
                gameLibraryListView.getSelectionModel().getSelectedItems());
        if (selected.isEmpty())
            return;
        poolGames.addAll(selected);
        libraryGames.removeAll(selected);
    }

    @FXML
    private void onRandomButtonClick() {
        if (poolGames.isEmpty()) {
            resultLabel.setText("Keine Spiele im Pool!");
            resultImageView.setImage(null);
            return;
        }

        int randomIndex = random.nextInt(poolGames.size());
        SteamGameRandompicker.Game randomGame = poolGames.get(randomIndex);

        resultLabel.setText(randomGame.name);

        if (randomGame.appid > 0) {
            resultImageView.setImage(imageCache.get(randomGame.appid));
        } else {
            resultImageView.setImage(null);
        }

        gamePoolListView.scrollTo(randomGame);
    }
}
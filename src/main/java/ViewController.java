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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.io.File;

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
    @FXML
    private ComboBox<String> profileComboBox;
    @FXML
    private TextField newProfileField;

    private ObservableList<SteamGameRandompicker.Game> poolGames;
    private ObservableList<SteamGameRandompicker.Game> libraryGames;
    private FilteredList<SteamGameRandompicker.Game> filteredLibraryGames;

    private Map<Integer, Image> imageCache = new HashMap<>();
    private Random random = new Random();
    private int customAppIdCounter = -1;

    // Die alte Konstante PROFILE_FILE kommt weg.
    // Dafür merken wir uns den aktuellen Profilnamen.
    private String currentProfile = "Default";
    private boolean isSwitchingProfile = false; // NEU: Flag, um Profilwechsel zu erkennen

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

        // Listener für die ComboBox (Reagiert, wenn du ein Profil im Dropdown wählst)
        profileComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (!isSwitchingProfile && newVal != null) {
                switchProfile(newVal);
            }
        });

        // Auswahl löschen, wenn in der anderen Liste geklickt wird
        gameLibraryListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null)
                gamePoolListView.getSelectionModel().clearSelection();
        });
        gamePoolListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null)
                gameLibraryListView.getSelectionModel().clearSelection();
        });
    }

    private void setCustomCellFactory(ListView<SteamGameRandompicker.Game> listView) {
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
                // GANZ WICHTIG: Das verhindert die Scroll-Bugs!
                super.updateItem(game, empty);

                if (empty || game == null) {
                    setGraphic(null);
                } else {
                    label.setText(game.name);

                    // NEU
                    File activeFile = CoverManager.getCoverFile(game.appid);

                    if (activeFile != null) {
                        if (!imageCache.containsKey(game.appid)) {
                            Image img = new Image(activeFile.toURI().toString(), false);
                            imageCache.put(game.appid, img);
                        }
                        Image img = imageCache.get(game.appid);
                        imageView.setImage(img);
                        CoverManager.applyProportionalSize(imageView, img, 120, 45); // Nutzt jetzt den Manager!

                    } else if (game.appid > 0) {
                        // 2. Steam-Spiel -> Hier ist dein echter Steam-Lade-Code wieder!
                        if (!imageCache.containsKey(game.appid)) {
                            String imageUrl = "http://media.steampowered.com/steam/apps/" + game.appid
                                    + "/capsule_184x69.jpg";
                            Image img = new Image(imageUrl, true); // true = asynchron im Hintergrund laden
                            imageCache.put(game.appid, img);
                        }
                        // Bei Steam-Bildern müssen wir nichts zuschneiden, die sind schon perfekt
                        imageView.setViewport(null);
                        imageView.setFitWidth(120);
                        imageView.setFitHeight(45);
                        imageView.setImage(imageCache.get(game.appid));
                    } else {
                        // Custom Game ohne zugewiesenes Cover -> leer lassen
                        imageView.setViewport(null);
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

            loadAvailableProfiles(); // Zuerst die verfügbaren Profile in die ComboBox laden
            loadProfile(); // Dann das aktuelle Profil laden (Default oder das zuletzt gewählte)

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
        UserProfile profile = new UserProfile();

        // Spiele zusammenkratzen (das ist und bleibt Aufgabe des Controllers)
        for (SteamGameRandompicker.Game g : libraryGames) {
            if (g.appid < 0)
                profile.customGames.add(g);
        }
        for (SteamGameRandompicker.Game g : poolGames) {
            if (g.appid < 0)
                profile.customGames.add(g);
            profile.poolAppIds.add(g.appid);
        }

        // NEU: Manager das Speichern überlassen
        try {
            ProfileManager.saveProfile(currentProfile, profile);
            resultLabel.setText("✅ Profil '" + currentProfile + "' gespeichert!");
        } catch (Exception e) {
            e.printStackTrace();
            resultLabel.setText("❌ Fehler beim Speichern!");
        }
    }

    private void loadProfile() {
        try {
            // NEU: Manager das Laden überlassen
            UserProfile profile = ProfileManager.loadProfile(currentProfile);

            if (profile != null) {
                if (profile.customGames != null) {
                    libraryGames.addAll(0, profile.customGames);
                    for (SteamGameRandompicker.Game cg : profile.customGames) {
                        if (cg.appid <= customAppIdCounter)
                            customAppIdCounter = cg.appid - 1;
                    }
                }
                if (profile.poolAppIds != null) {
                    List<SteamGameRandompicker.Game> gamesToMove = new ArrayList<>();
                    for (SteamGameRandompicker.Game g : libraryGames) {
                        if (profile.poolAppIds.contains(g.appid))
                            gamesToMove.add(g);
                    }
                    poolGames.addAll(gamesToMove);
                    libraryGames.removeAll(gamesToMove);
                }
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Laden des Profils:");
            e.printStackTrace();
        }
    }

    private void loadAvailableProfiles() {
        isSwitchingProfile = true;
        // NEU: Einfach den Manager fragen!
        List<String> profiles = ProfileManager.getAvailableProfiles();
        profileComboBox.getItems().setAll(profiles);
        profileComboBox.setValue(currentProfile);
        isSwitchingProfile = false;
    }

    // ==========================================
    // PROFIL-WECHSEL & NEUES PROFIL
    // ==========================================

    private void switchProfile(String newProfileName) {
        if (newProfileName == null || newProfileName.equals(currentProfile))
            return;

        currentProfile = newProfileName;
        resultLabel.setText("Wechsel zu Profil: " + currentProfile);

        // 1. Erstmal alles aufräumen: Spiele zurück in die Bibliothek schieben
        libraryGames.addAll(poolGames);
        poolGames.clear();

        // 2. Custom Games des alten Profils aus der Bibliothek entfernen
        libraryGames.removeIf(game -> game.appid < 0);

        // 3. Counter für Custom Games zurücksetzen
        customAppIdCounter = -1;

        // 4. Neues Profil über den Manager laden lassen
        loadProfile();
    }

    @FXML
    private void onAddProfileClick() {
        String newName = newProfileField.getText().trim();
        if (newName.isEmpty() || profileComboBox.getItems().contains(newName))
            return;

        // Das neue Profil der Liste hinzufügen und direkt anwählen
        profileComboBox.getItems().add(newName);
        profileComboBox.setValue(newName);
        newProfileField.clear();
    }

    @FXML
    private void onDeleteProfileClick() {
        if (currentProfile.equals("Default")) {
            resultLabel.setText("Das Default-Profil kann nicht gelöscht werden!");
            return;
        }

        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Profil löschen");
        alert.setHeaderText("Bist du sicher?");
        alert.setContentText("Das Profil '" + currentProfile + "' wird unwiderruflich gelöscht.");

        if (alert.showAndWait().get() == ButtonType.OK) {
            // NEU: Den Manager löschen lassen
            ProfileManager.deleteProfile(currentProfile);

            resultLabel.setText("Profil '" + currentProfile + "' wurde gelöscht.");
            currentProfile = "Default";
            loadAvailableProfiles();
            switchProfile("Default");
        }
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

        // Prüfen, ob für das gezogene Spiel ein lokales Cover existiert
        File activeFile = CoverManager.getCoverFile(randomGame.appid);

        if (activeFile != null) {
            // Lokales Bild laden und zuschneiden
            Image img = new Image(activeFile.toURI().toString());
            resultImageView.setImage(img);
            CoverManager.applyProportionalSize(resultImageView, img, 184, 69);
        } else if (randomGame.appid > 0) {
            // Normales Steam-Bild anzeigen
            Image img = imageCache.get(randomGame.appid);
            resultImageView.setImage(img);
            resultImageView.setViewport(null); // Steam-Originale nicht zuschneiden
            resultImageView.setFitWidth(184);
            resultImageView.setFitHeight(69);
        } else {
            resultImageView.setImage(null);
        }

        gamePoolListView.scrollTo(randomGame);
    }

    // Methode zum Auswählen und Speichern eines Covers für ein Custom Game
    @FXML
    private void onEditCoverClick() {
        SteamGameRandompicker.Game selected = gameLibraryListView.getSelectionModel().getSelectedItem();
        if (selected == null)
            selected = gamePoolListView.getSelectionModel().getSelectedItem();

        if (selected == null) {
            resultLabel.setText("Bitte zuerst ein Spiel auswählen!");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Bild für " + selected.name + " auswählen");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Bilder", "*.png", "*.jpg", "*.jpeg"));

        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            try {
                // NEU: Nur noch ein magischer Aufruf!
                CoverManager.saveCover(selectedFile, selected.appid);

                imageCache.remove(selected.appid);
                gameLibraryListView.refresh();
                gamePoolListView.refresh();
                resultLabel.setText("✅ Cover für " + selected.name + " aktualisiert!");
            } catch (Exception e) {
                e.printStackTrace();
                resultLabel.setText("❌ Fehler beim Kopieren des Bildes.");
            }
        }
    }

}
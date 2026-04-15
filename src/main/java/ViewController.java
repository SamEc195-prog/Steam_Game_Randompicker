import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode; // Importieren für Multi-Selektion

import java.util.List;
import java.util.Random;

public class ViewController {

    // --- FXML Elemente ---
    // Die 'fx:id' aus der FXML-Datei werden hier verknüpft

    // Linke Liste (Pool)
    @FXML
    private ListView<SteamGameRandompicker.Game> gamePoolListView;

    // NEU: Rechte Liste (Bibliothek)
    // (ersetzt gameIgnoredListView)
    @FXML
    private ListView<SteamGameRandompicker.Game> gameLibraryListView;

    // Buttons
    @FXML
    private Button excludeButton;
    @FXML
    private Button includeButton;
    @FXML
    private Button randomButton;

    // Ergebnis-Label
    @FXML
    private Label resultLabel;

    // --- Datenlisten ---
    
    // Die Datenquelle für die linke Liste (Pool)
    private ObservableList<SteamGameRandompicker.Game> poolGames;

    // NEU: Die Datenquelle für die rechte Liste (Bibliothek)
    // (ersetzt ignoredGames)
    private ObservableList<SteamGameRandompicker.Game> libraryGames;

    // --- Logik ---
    private Random random = new Random();

    /**
     * Diese Methode wird automatisch aufgerufen, 
     * NACHDEM die FXML-Datei geladen wurde.
     */
    @FXML
    public void initialize() {
        System.out.println("ViewController wird initialisiert...");
        
        // 1. Listen initialisieren
        poolGames = FXCollections.observableArrayList();
        libraryGames = FXCollections.observableArrayList();

        // 2. Listen mit den ListView-Elementen verknüpfen
        gamePoolListView.setItems(poolGames);
        gameLibraryListView.setItems(libraryGames);
        
        // 3. (Bonus) Erlaube Multi-Selektion in beiden Listen
        // Damit kann man mehrere Spiele auf einmal verschieben
        gamePoolListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        gameLibraryListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // 4. Spiele von der API laden
        loadGamesAsync();
    }

    /**
     * Lädt die Spiele in einem Hintergrund-Thread, 
     * damit die GUI nicht einfriert.
     */
    private void loadGamesAsync() {
        Task<List<SteamGameRandompicker.Game>> loadGamesTask = new Task<>() {
            @Override
protected List<SteamGameRandompicker.Game> call() throws Exception {
                System.out.println("Lade Spiele im Hintergrund-Thread...");
                SteamGameRandompicker fetcher = new SteamGameRandompicker();
                return fetcher.fetchOwnedGames();
            }
        };

        loadGamesTask.setOnSucceeded(event -> {
            List<SteamGameRandompicker.Game> games = loadGamesTask.getValue();
            // Wir fügen alle geladenen Spiele ZUERST dem Pool hinzu
            libraryGames.setAll(games);
            
            System.out.println("Spiele erfolgreich in die Bibliothek geladen.");
            // Alle Buttons freischalten
            randomButton.setDisable(false);
            excludeButton.setDisable(false);
            includeButton.setDisable(false);
            resultLabel.setText("Bibliothek geladen! Wähle Spiele aus dem Pool oder der Bibliothek aus.");
        });

        loadGamesTask.setOnFailed(event -> {
            System.err.println("Fehler beim Laden der Spiele:");
            loadGamesTask.getException().printStackTrace();
            resultLabel.setText("Fehler beim Laden der Spiele!");
        });

        // Buttons sperren, solange geladen wird
        randomButton.setDisable(true);
        excludeButton.setDisable(true);
        includeButton.setDisable(true);
        resultLabel.setText("Lade Spiele von der Steam API...");
        
        new Thread(loadGamesTask).start();
    }

    /**
     * Wird aufgerufen, wenn der ">" (Exclude) Button geklickt wird.
     * Verschiebt markierte Spiele von Pool -> Ignoriert.
     */
    @FXML
    private void onExcludeClick() {
        // Hole alle markierten Spiele aus der Pool-Liste
        List<SteamGameRandompicker.Game> selected = gamePoolListView.getSelectionModel().getSelectedItems();
        
        if (selected.isEmpty()) {
            return; // Nichts zu tun
        }

        // Füge sie der Ignoriert-Liste hinzu
        libraryGames.addAll(selected);
        // Entferne sie aus der Pool-Liste
        poolGames.removeAll(selected);
    }

    /**
     * Wird aufgerufen, wenn der "<" (Include) Button geklickt wird.
     * Verschiebt markierte Spiele von Bibliothek -> Pool.
     * (Das ist jetzt unsere Haupt-Aktion)
     */
    @FXML
    private void onIncludeClick() {
        // Hole alle markierten Spiele aus der Bibliothek-Liste
        List<SteamGameRandompicker.Game> selected = gameLibraryListView.getSelectionModel().getSelectedItems();

        if (selected.isEmpty()) {
            return; // Nichts zu tun
        }

        // Füge sie der Pool-Liste hinzu
        poolGames.addAll(selected);
        // Entferne sie aus der Bibliothek-Liste
        libraryGames.removeAll(selected);
    }


    /**
     * Wird aufgerufen, wenn der "Zufall"-Button geklickt wird.
     * Wählt nur noch aus der POOL-Liste aus.
     */
    @FXML
    private void onRandomButtonClick() {
        // WICHTIG: Wir prüfen jetzt die 'poolGames'-Liste
        if (poolGames.isEmpty()) {
            resultLabel.setText("Keine Spiele im Pool zum Auswählen!");
            return;
        }
        
        // Wähle einen zufälligen Index aus der Pool-Liste
        int randomIndex = random.nextInt(poolGames.size());
        
        // Hole das Spiel
        SteamGameRandompicker.Game randomGame = poolGames.get(randomIndex);

        // Zeige das Ergebnis
        resultLabel.setText(randomGame.name);
        
        // (Bonus) Scrolle zur Auswahl in der Pool-Liste
        gamePoolListView.getSelectionModel().select(randomGame);
        gamePoolListView.scrollTo(randomGame);
    }
}
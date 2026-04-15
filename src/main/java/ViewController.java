import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList; // NEU
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField; // NEU

import java.util.ArrayList; // NEU
import java.util.List;
import java.util.Random;

public class ViewController {

    // --- FXML Elemente ---
    @FXML private ListView<SteamGameRandompicker.Game> gamePoolListView;
    @FXML private ListView<SteamGameRandompicker.Game> gameLibraryListView;
    
    @FXML private TextField searchField; // NEU: Das Suchfeld aus der FXML

    @FXML private Button excludeButton; 
    @FXML private Button includeButton; 
    @FXML private Button randomButton;
    @FXML private Label resultLabel;

    // --- Datenlisten ---
    private ObservableList<SteamGameRandompicker.Game> poolGames;
    private ObservableList<SteamGameRandompicker.Game> libraryGames;
    
    // NEU: Die gefilterte Liste für die Anzeige
    private FilteredList<SteamGameRandompicker.Game> filteredLibraryGames; 

    private Random random = new Random();

    @FXML
    public void initialize() {
        poolGames = FXCollections.observableArrayList(); 
        libraryGames = FXCollections.observableArrayList(); 

        gamePoolListView.setItems(poolGames);
        
        // NEU: Wir verknüpfen die Bibliothek-Liste jetzt mit der FilteredList!
        // Am Anfang zeigt der Filter einfach alles an (p -> true)
        filteredLibraryGames = new FilteredList<>(libraryGames, p -> true);
        gameLibraryListView.setItems(filteredLibraryGames); 
        
        gamePoolListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        gameLibraryListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // NEU: Wir hören zu, was im Suchfeld getippt wird
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredLibraryGames.setPredicate(game -> {
                // Wenn das Suchfeld leer ist, zeige alle Spiele
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                
                // Text in Kleinbuchstaben umwandeln, damit Groß-/Kleinschreibung egal ist
                String lowerCaseFilter = newValue.toLowerCase();
                
                // Prüfen, ob der Spielname den Suchtext enthält
                return game.name.toLowerCase().contains(lowerCaseFilter);
            });
        });

        loadGamesAsync();
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
            libraryGames.setAll(games); // Füllt die Basis-Liste, der Filter aktualisiert sich automatisch
            
            randomButton.setDisable(false);
            excludeButton.setDisable(false);
            includeButton.setDisable(false);
            resultLabel.setText("Bibliothek geladen. Wähle Spiele für den Pool!");
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

    @FXML
    private void onExcludeClick() {
        List<SteamGameRandompicker.Game> selected = new ArrayList<>(gamePoolListView.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) return;

        libraryGames.addAll(selected);
        poolGames.removeAll(selected);
    }

    @FXML
    private void onIncludeClick() {
        // WICHTIG (NEU): Wir müssen die Auswahl in eine neue ArrayList kopieren.
        // Da wir nun eine gefilterte Liste haben, würde es sonst beim Verschieben 
        // zu Fehlern kommen (ConcurrentModificationException).
        List<SteamGameRandompicker.Game> selected = new ArrayList<>(gameLibraryListView.getSelectionModel().getSelectedItems());

        if (selected.isEmpty()) return;

        poolGames.addAll(selected);
        libraryGames.removeAll(selected); // Entfernt sie aus der Basis-Liste
    }

    @FXML
    private void onRandomButtonClick() {
        if (poolGames.isEmpty()) {
            resultLabel.setText("Keine Spiele im Pool zum Auswählen!");
            return;
        }
        
        int randomIndex = random.nextInt(poolGames.size());
        SteamGameRandompicker.Game randomGame = poolGames.get(randomIndex);

        resultLabel.setText(randomGame.name);
        
        // (Bugfix ist hier aktiv: Kein select() mehr, nur noch scrollen)
        gamePoolListView.scrollTo(randomGame);
    }
}
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
import javafx.scene.control.ListCell; // NEU
import javafx.scene.image.Image; // NEU
import javafx.scene.image.ImageView; // NEU
import javafx.scene.layout.HBox; // NEU

import java.util.ArrayList;
import java.util.HashMap; // NEU
import java.util.List;
import java.util.Map; // NEU
import java.util.Random;

public class ViewController {

    @FXML private ListView<SteamGameRandompicker.Game> gamePoolListView;
    @FXML private ListView<SteamGameRandompicker.Game> gameLibraryListView;
    @FXML private TextField searchField;
    @FXML private Button excludeButton; 
    @FXML private Button includeButton; 
    @FXML private Button randomButton;
    @FXML private Label resultLabel;
    
    @FXML private ImageView resultImageView; // NEU: Für das Gewinner-Bild

    private ObservableList<SteamGameRandompicker.Game> poolGames;
    private ObservableList<SteamGameRandompicker.Game> libraryGames;
    private FilteredList<SteamGameRandompicker.Game> filteredLibraryGames; 

    // NEU: Unser Bilder-Cache. Verhindert, dass Bilder beim Scrollen ständig neu geladen werden.
    private Map<Integer, Image> imageCache = new HashMap<>();

    private Random random = new Random();

    @FXML
    public void initialize() {
        poolGames = FXCollections.observableArrayList(); 
        libraryGames = FXCollections.observableArrayList(); 

        gamePoolListView.setItems(poolGames);
        filteredLibraryGames = new FilteredList<>(libraryGames, p -> true);
        gameLibraryListView.setItems(filteredLibraryGames); 
        
        gamePoolListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        gameLibraryListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // NEU: Wir wenden unsere eigene Design-Schablone auf BEIDE Listen an
        setCustomCellFactory(gamePoolListView);
        setCustomCellFactory(gameLibraryListView);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredLibraryGames.setPredicate(game -> {
                if (newValue == null || newValue.isEmpty()) return true;
                return game.name.toLowerCase().contains(newValue.toLowerCase());
            });
        });

        loadGamesAsync();
    }

    /**
     * NEU: Diese Methode verändert das Aussehen der Listenzeilen.
     * Statt nur Text, zeigen wir jetzt ein Bild und daneben den Text.
     */
    private void setCustomCellFactory(ListView<SteamGameRandompicker.Game> listView) {
        listView.setCellFactory(param -> new ListCell<>() {
            private HBox hbox = new HBox(10); // 10 Pixel Abstand
            private ImageView imageView = new ImageView();
            private Label label = new Label();

            {
                // Einmaliges Setup für jede Listenzeile
                imageView.setFitWidth(120); // Die Breite des Banners
                imageView.setPreserveRatio(true);
                // Text vertikal zentrieren
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

                    // Prüfen, ob wir das Bild schon heruntergeladen haben
                    if (!imageCache.containsKey(game.appid)) {
                        // Bild URL von den offiziellen Steam-Servern zusammenbauen
                        String imageUrl = "https://cdn.akamai.steamstatic.com/steam/apps/" + game.appid + "/capsule_184x69.jpg";
                        
                        // "true" am Ende bedeutet: Asynchron im Hintergrund laden (GUI friert nicht ein!)
                        Image img = new Image(imageUrl, true);
                        imageCache.put(game.appid, img);
                    }
                    
                    // Bild aus dem Cache setzen
                    imageView.setImage(imageCache.get(game.appid));
                    
                    // Der Liste sagen, dass sie unsere HBox als Zeile anzeigen soll
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
        List<SteamGameRandompicker.Game> selected = new ArrayList<>(gameLibraryListView.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) return;
        poolGames.addAll(selected);
        libraryGames.removeAll(selected); 
    }

    @FXML
    private void onRandomButtonClick() {
        if (poolGames.isEmpty()) {
            resultLabel.setText("Keine Spiele im Pool!");
            resultImageView.setImage(null); // Bild leeren bei Fehler
            return;
        }
        
        int randomIndex = random.nextInt(poolGames.size());
        SteamGameRandompicker.Game randomGame = poolGames.get(randomIndex);

        // Name unten anzeigen
        resultLabel.setText(randomGame.name);
        
        // NEU: Das Gewinner-Bild unten anzeigen (aus dem Cache holen)
        resultImageView.setImage(imageCache.get(randomGame.appid));
        
        gamePoolListView.scrollTo(randomGame);
    }
}
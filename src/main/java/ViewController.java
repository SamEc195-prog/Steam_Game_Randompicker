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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ViewController {

    @FXML private ListView<SteamGameRandompicker.Game> gamePoolListView;
    @FXML private ListView<SteamGameRandompicker.Game> gameLibraryListView;
    @FXML private TextField searchField;
    
    // NEU: Textfeld für eigene Spiele
    @FXML private TextField customGameField; 

    @FXML private Button excludeButton; 
    @FXML private Button includeButton; 
    @FXML private Button randomButton;
    @FXML private Label resultLabel;
    @FXML private ImageView resultImageView;

    private ObservableList<SteamGameRandompicker.Game> poolGames;
    private ObservableList<SteamGameRandompicker.Game> libraryGames;
    private FilteredList<SteamGameRandompicker.Game> filteredLibraryGames; 

    private Map<Integer, Image> imageCache = new HashMap<>();
    private Random random = new Random();
    
    // NEU: Ein Zähler für unsere eigenen Spiele, damit jedes eine einzigartige (negative) ID bekommt
    private int customAppIdCounter = -1;

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
                if (newValue == null || newValue.isEmpty()) return true;
                return game.name.toLowerCase().contains(newValue.toLowerCase());
            });
        });

        loadGamesAsync();
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
                super.updateItem(game, empty);

                if (empty || game == null) {
                    setGraphic(null);
                } else {
                    label.setText(game.name);

                    // NEU: Nur Steam-Bilder laden, wenn die appid > 0 ist
                    if (game.appid > 0) {
                        if (!imageCache.containsKey(game.appid)) {
                            String imageUrl = "https://cdn.akamai.steamstatic.com/steam/apps/" + game.appid + "/capsule_184x69.jpg";
                            Image img = new Image(imageUrl, true);
                            imageCache.put(game.appid, img);
                        }
                        imageView.setImage(imageCache.get(game.appid));
                    } else {
                        // Bei eigenen Spielen (appid < 0) sicherstellen, dass kein altes Bild angezeigt wird
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

    // NEU: Methode zum Hinzufügen von eigenen Spielen
    @FXML
    private void onAddCustomGameClick() {
        String gameName = customGameField.getText().trim();
        
        // Nichts tun, wenn das Feld leer ist
        if (gameName.isEmpty()) {
            return;
        }

        // Ein neues "Game"-Objekt erstellen
        SteamGameRandompicker.Game customGame = new SteamGameRandompicker.Game();
        customGame.appid = customAppIdCounter--; // Weist -1 zu, dann -2, dann -3...
        customGame.name = gameName;
        customGame.playtime_forever = 0;

        // Fügen wir es GANZ OBEN der Bibliothek hinzu, damit man es direkt sieht
        libraryGames.add(0, customGame);

        // Textfeld nach dem Hinzufügen leeren
        customGameField.clear();
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
            resultImageView.setImage(null);
            return;
        }
        
        int randomIndex = random.nextInt(poolGames.size());
        SteamGameRandompicker.Game randomGame = poolGames.get(randomIndex);

        resultLabel.setText(randomGame.name);
        
        // NEU: Auch hier beim Gewinner-Bild prüfen, ob es ein offizielles Spiel ist
        if (randomGame.appid > 0) {
            resultImageView.setImage(imageCache.get(randomGame.appid));
        } else {
            resultImageView.setImage(null);
        }
        
        gamePoolListView.scrollTo(randomGame);
    }
}
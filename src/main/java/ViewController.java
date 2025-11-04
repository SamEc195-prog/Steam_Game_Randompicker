import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import java.util.List;
import java.util.Random;

public class ViewController {

    // Diese @FXML-Variablen werden automatisch mit den Elementen
    // aus der FXML-Datei (anhand der 'fx:id') verknüpft
    @FXML
    private ListView<SteamGameRandompicker.Game> gameListView;

    @FXML
    private Button randomButton;

    @FXML
    private Label resultLabel;

    // Eine Liste, die Änderungen an die ListView meldet
    private ObservableList<SteamGameRandompicker.Game> observableGameList;

    private Random random = new Random();

    /**
     * Diese Methode wird automatisch aufgerufen,
     * NACHDEM die FXML-Datei geladen wurde.
     * Perfekt, um die Liste zu füllen.
     */

    @FXML
    public void initialize() {
        System.out.println("ViewController wird initialisiert...");
        // Wir erstellen die "beobachtbare" Liste und binden sie in die ListView
        observableGameList = FXCollections.observableArrayList();
        gameListView.setItems(observableGameList);

        // Wir rufen die Lade-Logik auf
        loadGamesAsync();
    }

    /**
     * Startet den Ladevorgang in einem Hintergrund-Thread,
     * damit die GUI beim Laden nicht einfriert.
     */
    private void loadGamesAsync() {
        // Ein JavaFX-Task ist ideal für Hintergrundarbeit
        Task<List<SteamGameRandompicker.Game>> loadGamesTask = new Task<>() {
            @Override
            protected List<SteamGameRandompicker.Game> call() throws Exception {
                // Das hier läuft im Hintergrund-Thread
                System.out.println("Lade Spiele im Hintergrund-Thread...");
                SteamGameRandompicker picker = new SteamGameRandompicker();
                return picker.fetchOwnedGames();
            }
        };

        // Diese Methoden werden im GUI-Thread ausgeführt,
        // wenn der Task fertig ist.

        // Wenn erfolgreich:
        loadGamesTask.setOnSucceeded(event -> {
            List<SteamGameRandompicker.Game> games = loadGamesTask.getValue();
            // Wir fügen alle geladene Spiele zur Liste hinzu
            observableGameList.setAll(games);
            System.out.println("Spiele erfolgreich in die GUI geladen.");
            randomButton.setDisable(false); // Button freischalten
            resultLabel.setText("Liste geladen. Bereit!");
        });

        // Wenn fehlgeschlagen:
        loadGamesTask.setOnFailed(event -> {
            System.err.println("Fehler beim Laden der Spiele:");
            loadGamesTask.getException().printStackTrace();
            resultLabel.setText("Fehler beim Laden der Spiele!");
        });

        // Bevor der Task startet, sperren wir den Button.
        randomButton.setDisable(true);
        resultLabel.setText("Lade Spiele von der Steam-API...");

        // Den Task in einem neuen Thread starten
        new Thread(loadGamesTask).start();
    }

    /**
     * Diese Methode wird aufgerufen, wenn der Button
     * (definiert in FXML 'onAction ="#onRandomButtonClick"')
     * geklickt wird.
     */
    @FXML
    private void onRandomButtonClick() {
        if (observableGameList.isEmpty()) {
            resultLabel.setText("Keine Spiele in der Liste zum Auswählen!");
            return;
        }

        // 1. Wähle einen zufälligen Index
        int randomIndex = random.nextInt(observableGameList.size());

        // 2. Hole das Spiel von diesem Index
        SteamGameRandompicker.Game randomGame = observableGameList.get(randomIndex);

        // 3. Zeige das Ergebnis im Label an
        resultLabel.setText(randomGame.name);

        // 4. Bonus: Scrolle zur Auswahl und selektiere sie
        gameListView.getSelectionModel().select(randomGame);
        gameListView.scrollTo(randomGame);
    }
}

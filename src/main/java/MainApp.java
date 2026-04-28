import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // Lädt die FXML-Datei aus dem 'resources'-Ordner
        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("/RandomizerView.fxml"));

        // Erstellt die Szene mit dem geladnen Layout
        Scene scene = new Scene(fxmlLoader.load());

        stage.setTitle("Steam Game Randomizer Picker");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

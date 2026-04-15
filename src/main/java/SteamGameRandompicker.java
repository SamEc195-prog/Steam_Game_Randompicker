import com.google.gson.Gson; // Importieren wir Gson
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;

public class SteamGameRandompicker {

    private static final String API_KEY = "9ED1938C227ADAA8BB64B95733B5930B";
    private static final String STEAM_ID = "76561198122751717";

    public List<Game> fetchOwnedGames() {
        System.out.println("Starte Abruf der Spieleliste...");
        try {
            // 1. Den API-Call vorbereiten
            HttpClient client = HttpClient.newHttpClient();

            /* Die URL, die wir aufrufen. Wir fügen unsere Infos ein
            und ganz wichtig: "?include_appinfo=true", damit wir die Namen bekommen!
            */
            String url = String.format(
                    "http://api.steampowered.com/IPlayerService/GetOwnedGames/v0001/?key=%s&steamid=%s&format=json&include_appinfo=true",
                    API_KEY,
                    STEAM_ID
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .build();

            // 2. Anfrage senden und Antwort als Text (String) empfangen
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // 3. Antwort verarbeiten (JSON-Parsing)
                String jsonBody = response.body();

                // Wir nutzen Gson, um den JSON-Text in unsere Java-Klassen zu füllen
                Gson gson = new Gson();
                SteamResponse steamResponse = gson.fromJson(jsonBody, SteamResponse.class);

                if (steamResponse != null && steamResponse.response != null && steamResponse.response.games != null) {
                    System.out.println("Erfolgreich " + steamResponse.response.game_count + " Spiele gefunden!");
                    return steamResponse.response.games;
            } else {
                System.err.println("Antwort erhalten, aber keine Spieldaten gefunden.");
            }
        } else {
                System.out.println("Fehler bei der API-Anfrage. Status Code: " + response.statusCode());
            }
    } catch (Exception e) {
            // Falls etwas schiefgeht (z.B. keine Internetverbindung)
            System.err.println("Ein Fehler ist aufgetreten:");
            e.printStackTrace();
        }

        // Im Fehlerfall geben wir eine leere Liste zurück
        return Collections.emptyList();
}

/*
    Diese inneren Klassen definieren die Struktur der JSON-Antwort von Steam.
    Gson verwendet sie, um die Daten automatisch zuzuordnen.
    Die Variablennamen MÜSSEN exakt so heißen wie die "Keys" im JSON.
 */

// Die oberste Ebene der JSON-Antwort
public static class SteamResponse {
    GameList response;
}

// Die "response" enthält die Liste der Spiele
public static class GameList {
    int game_count;
    List<Game> games;
}

// Das eigentliche Spiel-Objekt
public static class Game {
    int appid;
    String name;
    int playtime_forever; // Spielzeit in Minuten
    // Es gäbe hier noch mehr Felder (z.B. img_icon_url),
    // aber wir brauchen sie erstmal nicht.

    @Override
    public String toString() {
    return this.name;
    }
}
}
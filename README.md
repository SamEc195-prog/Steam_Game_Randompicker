# Steam Game Randompicker

Eine moderne JavaFX-Desktop-Anwendung, die hilft, die ewige Frage zu beantworten: *"Was soll ich heute spielen?"* Dieses Tool lädt automatisch die Steam-Bibliothek, erlaubt das Hinzufügen eigener (Non-Steam) Spiele und lässt einen "Pool" aus Spielen zusammenstellen.
Mit einem Klick auf den Random-Button wählt das Programm dann zufällig ein Spiel aus diesem Pool aus.

</br>

<img width="752" height="632" alt="sgrp-v1-screenshot" src="https://github.com/user-attachments/assets/b6549aa7-6786-46af-ac01-5225b35ff9dc" />

</br>

## Features

* **Automatische Steam-Integration:** Lädt asynchron alle gekauften Spiele über die Steam Web API.
* **Eigene Spiele (Custom Games):** Auch Spiele außerhalb von Steam (z.B. Brettspiele, Emulatoren oder andere Launcher) können der Bibliothek hinzugefügt werden.
* **Eigene Cover-Bilder:** Custom Games können eigene Bilder von der Festplatte zugewiesen werden. Die Bilder werden automatisch proportional zugeschnitten, sodass sie perfekt ins Steam-Banner-Format (184x69) passen.
* **Profil-Verwaltung:** Verschiedene Profile sind erstellbar (z. B. "Horror-Spiele", "Koop mit Freunden"), man kann sie abspeichern und nahtlos zwischen ihnen hin und her wechseln. Alle Speicherstände werden lokal und sicher im JSON-Format abgelegt.
* **Echtzeit-Suche:** Spiele können in großen Bibliotheken über das Suchfeld gefunden werden.
* **Bild-Caching:** Hohe Performance durch das lokale Zwischenspeichern von Steam-Bannern und eigenen Covern.

## Tech-Stack

* **Sprache:** Java 21
* **UI-Framework:** JavaFX (via FXML)
* **Build-Tool:** Maven
* **JSON-Parsing:** Gson (Google)
* **API:** Steam Web API (`GetOwnedGames`)

## Software-Architektur

Das Projekt wurde strukturiert, um UI-Logik von Dateiverwaltung und API-Calls zu trennen:

* `ViewController.java`: Steuert ausschließlich die Benutzeroberfläche, fängt Button-Klicks ab und aktualisiert die JavaFX-Listen.
* `SteamGameRandompicker.java`: Der API-Client. Handhabt den HTTP-Request zu Steam und das asynchrone Parsing der JSON-Antwort.
* `ProfileManager.java`: Übernimmt das Speichern, Laden und Löschen der `.json`-Profil-Dateien im Hintergrund.
* `CoverManager.java`: Verwaltet den lokalen Bild-Import, das sichere Kopieren der Dateien und den proportionalen Zuschnitt (Verhinderung von Letterboxing).
* `UserProfile.java`: Ein reines Daten-Objekt (POJO), das als Vorlage für den Gson-Parser dient.

## Für Entwickler: Projekt lokal starten

Voraussetzung: Java 21 (oder neuer) und Maven müssen auf dem System installiert sein.

1. Repository klonen:
   ```bash
   git clone [https://github.com/DEIN_USERNAME/Steam_Game_Randompicker.git](https://github.com/DEIN_USERNAME/Steam_Game_Randompicker.git)
   cd Steam_Game_Randompicker
   ```
2. Über das Maven JavaFX-Plugin starten:
   ```bash
   mvn clean javafx:run
   ```

## Wo speichert das Programm seine Daten?
Um Systemordner sauber zu halten, erstellt das Programm einen eigenen versteckten Ordner im Benutzerverzeichnis des aktuellen Betriebssystems:
* **Windows:** `C:\Users\DeinName\.steamrandomizer\`
* **Mac/Linux:** `~/.steamrandomizer/`

Dort liegen die `Profilname.json` Dateien sowie ein `covers/` Unterordner für die selbst importierten Bilder.

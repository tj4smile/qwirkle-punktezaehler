# Qwirkle Punktezähler

Ein einfacher, werbefreier Punktezähler für das Legespiel **Qwirkle** (Android).
Alle Daten bleiben auf deinem Gerät: kein Konto, keine Internetverbindung nötig,
keine Datenweitergabe.

*English summary below.*

> **Hinweis:** Dies ist eine unabhängige Zählhilfe von einem Spieler für Spieler.
> Sie steht in keiner Verbindung zu den Rechteinhabern von Qwirkle. „Qwirkle" ist
> eine Marke ihrer jeweiligen Inhaber.

## Funktionen

- Spieler verwalten (2 bis 7 Spieler pro Spiel, eigene Farben)
- Reihenfolge und Farben vor dem Spielstart festlegen
- Punkte pro Runde schnell erfassen (Zahlenrad 0–20, Tastatur für größere Werte),
  Qwirkles zählen, Bonus wird automatisch addiert
- Spielverlauf als Tabelle, einzelne Runden nachträglich korrigieren
- Endspielbonus, Endauswertung mit Platzierungen
- Statistik: gespielte Partien, Rekorde, Spielerstatistik (Siege, Siegquote,
  Durchschnitt, Bestwert, Qwirkles)
- Testspiele, die nicht in die Statistik zählen
- Sicherung und Wiederherstellung (JSON), Export für Excel (CSV)
- Erinnerung an eine Sicherung, Info-Seite mit Spielregeln-Kurzfassung
- Deutsch und Englisch (nach Systemsprache), helles und dunkles Design

## Installation

1. Lade die neueste `.apk` unter **Releases** herunter.
2. Öffne die Datei auf deinem Android-Gerät (Android 8.0 oder neuer).
3. Erlaube bei Nachfrage die Installation aus dieser Quelle
   („Installation aus unbekannten Quellen").
4. Updates: einfach die neue APK über die alte installieren. Das klappt, weil
   alle Versionen mit demselben Schlüssel signiert sind. Deine Daten bleiben
   erhalten.

**Prüfsumme:** Zu jedem Release steht der SHA-256-Wert der APK dabei. Unter
Windows prüfst du ihn mit
`certutil -hashfile Qwirkle-Punktezaehler-<Version>.apk SHA256`.

**Sicherung:** Lege vor einer Neuinstallation oder einem Gerätewechsel unter
„Daten sichern & wiederherstellen" eine Sicherung an. Ohne Sicherung gehen die
Daten beim Deinstallieren verloren.

## Screenshots

*(Screenshots hier einfügen, z. B. im Ordner `docs/screenshots/`.)*

## Selbst bauen

Voraussetzungen: Android Studio (aktuell), JDK passend zum Android Gradle Plugin.

```
git clone <dieses Repository>
cd <Ordner>
./gradlew assembleDebug        # Windows: gradlew.bat assembleDebug
```

Die Debug-APK liegt danach unter `app/build/outputs/apk/debug/`. Für eine
signierte Release-APK brauchst du einen eigenen Schlüssel (Keystore). Dieser
gehört **nicht** ins Repository.

Technik: Kotlin, Jetpack Compose, Material 3, Room, Navigation Compose,
MVVM. Die ausführliche technische Dokumentation steht in
[`docs/Qwirkle_Projektdokumentation.txt`](docs/Qwirkle_Projektdokumentation.txt).

## Mitmachen

Fehler und Wünsche gern als Issue melden. Bitte nenne Gerät, Android-Version und
App-Version (steht auf der Startseite unten).

## Lizenz

Der Quellcode steht unter der [MIT-Lizenz](LICENSE). Die verwendeten Bibliotheken
(Android Jetpack, Material Design) stehen unter der Apache License 2.0.

---

## English summary

A simple, ad-free score keeper for the tile game **Qwirkle** (Android 8.0+).
All data stays on your device: no account, no internet access needed, no data
sharing. Features: player management, playing order and colors, quick score entry,
round history with corrections, end bonus, statistics, backup/restore (JSON),
CSV export, German and English UI.

Install: download the latest `.apk` from **Releases**, open it on your device and
allow installation from this source. Updates install over the old version
(same signing key). Create a backup before reinstalling.

This is an independent tool and is not affiliated with the rights holders of
Qwirkle. "Qwirkle" is a trademark of its respective owners.

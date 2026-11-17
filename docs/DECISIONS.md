# Entscheidungen

Festgehaltene Produkt- und Designentscheidungen. Neue Entscheidungen werden unten ergänzt,
damit nachvollziehbar bleibt, warum die App so aussieht und funktioniert.

## Grundlagen

| Thema | Entscheidung |
|---|---|
| Name | **Ordo** |
| Paket-ID | `io.github.tombolinero.ordo` (darf nie geändert werden, sonst keine Updates) |
| Plattform | Native Android, Kotlin + Jetpack Compose, ab Android 8 |
| Sprachen | Deutsch und Englisch |
| Design | Nur dunkel, nach der Referenz in [DESIGN.md](DESIGN.md) |
| Lizenz | GPL-3.0 |
| Verteilung | GitHub Releases + Obtainium, siehe [RELEASING.md](RELEASING.md) |

## Bedienung (Konzept, E1–E7)

| # | Frage | Entscheidung |
|---|---|---|
| E1 | Verschieben ohne Versehen | **Sortier-Modus**: Verschieben nur nach Tippen auf „Sortieren", dann erscheinen Griffe |
| E2 | Darstellung geteilter Platz | **Gemeinsame Karte**, Einträge untereinander, gelbe Verbindungslinie |
| E3 | Zählweise nach Gleichstand | **Weiterzählen**: 3, 3, 4 |
| E4 | Startseite | **Große Karten untereinander** mit Platz 1 und Vorschaubildern |
| E5 | Top 3 | **Podium**: Platz 1 groß in Listenfarbe, 2 und 3 größer, danach kompakt |
| E6 | Antippen eines Eintrags | **Detailseite mit Übergang**, Rang-Rad aus der Vorlage |
| E7 | Untere Leiste | **Listen · Neu (gelbe Punkt-Pille) · Einstellungen** |

## Funktionen

| Funktion | Version |
|---|---|
| Listen, Einträge mit Titel/Bild/Notiz, Sortieren, Gleichstand, Backup | v1 |
| Rückgängig, Suche, „Noch nicht eingeordnet", Bild zuschneiden, Listen gestalten, Papierkorb | v1 |
| Duell-Modus „A oder B?", Als Bild teilen, Homescreen-Widget | v1 |
| Optionale Abschnitte, Rang-Verlauf, Eigene Felder, Mehrere Bilder pro Eintrag | später |
| Bewertung 0–100, Automatisches Backup, App-Sperre | nein |

## Feinschliff (F1–F5)

| # | Thema | Entscheidung |
|---|---|---|
| F1 | Punkt-Zahlen | **Pixel-Raster 5 × 7**: nur benötigte Punkte leuchten, gleicher Abstand überall |
| F2 | Schein nach außen | **Kein Schein**, nur der dunkle Schatten |
| F3 | Listenfarben | Graphit, Magenta, Violett, **Rubin, Kobalt, Petrol**. Gelb/Gold bleibt reine Akzentfarbe |
| F4 | Einstellungs-Symbol | **Zahnrad** |
| F5 | Logo | **„Wendeltreppe"**: drei nach außen wachsende, gedrehte Bögen (stilisierte Wendeltreppe von oben) |

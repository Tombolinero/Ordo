# Backup-Format

Ein Ordo-Backup ist **eine einzelne Datei** mit der Endung `.ordobackup`. Intern ist es ein
ZIP-Archiv, man kann es also mit jedem ZIP-Programm öffnen und hineinschauen.

```
Ordo-Backup-2026-11-17.ordobackup
├── manifest.json     Formatkennung, Format-Version, App-Version, Anzahl, Prüfsummen
├── data.json         alle Listen mit ihren Einträgen, Plätzen, Notizen, Papierkorb, Einstellungen
└── images/
    ├── 3f9c…e1.webp  Bilder der Einträge, benannt nach ihrer ID
    └── …
```

## manifest.json

```json
{
  "format": "ordo-backup",
  "formatVersion": 1,
  "appVersionName": "1.0.0",
  "appVersionCode": 10000,
  "createdAt": "2026-11-17T12:00:00Z",
  "counts": { "lists": 2, "items": 7, "images": 2 },
  "files": {
    "data.json": "<sha256>",
    "images/img-jesko.webp": "<sha256>"
  }
}
```

Beim Einlesen wird jede Datei mit ihrer Prüfsumme verglichen. Ein beschädigtes Backup wird
erkannt und **nicht** eingespielt.

## data.json (Format-Version 1)

```json
{
  "lists": [
    {
      "id": "…uuid…", "title": "Beste Autos", "description": "", "color": "magenta",
      "pinned": true, "sortOrder": 0, "createdAt": 1700000000000, "updatedAt": 1700000500000,
      "deletedAt": null,
      "items": [
        { "id": "…", "title": "Koenigsegg Jesko", "note": "Über 1.600 PS", "image": "….webp",
          "place": 0, "placeOrder": 0, "createdAt": …, "updatedAt": …, "deletedAt": null }
      ]
    }
  ],
  "settings": { }
}
```

- `place`: Platz ab 0. Einträge mit gleichem `place` teilen sich den Platz (Gleichstand).
  `null` bedeutet „noch nicht eingeordnet".
- `placeOrder`: Reihenfolge innerhalb eines geteilten Platzes bzw. der Ablage.
- `deletedAt`: gesetzt, wenn der Eintrag oder die Liste im Papierkorb liegt.
- Zeitstempel sind Millisekunden seit 1970 (UTC). IDs sind UUIDs.

## Regeln für Erweiterungen

Damit **alte Backups in neuen App-Versionen** und **neue Backups in alten App-Versionen**
funktionieren, gelten diese Regeln:

1. **Neue Funktion = neue optionale Felder mit Standardwert.** Die Format-Version bleibt gleich.
   - Neue App + altes Backup: fehlende Felder bekommen den Standardwert.
   - Alte App + neues Backup: unbekannte Felder und Dateien werden ignoriert. Die App
     warnt, dass das Backup aus einer neueren Version stammt und neue Inhalte eventuell
     fehlen. Listen, Einträge, Bilder und Notizen kommen trotzdem vollständig an.
2. **Nur echte Strukturänderungen** (umbenennen, verschieben, Bedeutung ändern) erhöhen
   `formatVersion`. Dann kommt ein Umwandlungsschritt in `BackupMigrations` dazu
   (v1 → v2 → v3 …). Alte Backups werden beim Einlesen Schritt für Schritt angehoben.
3. Ein Backup mit **höherer Format-Version, als die App kennt**, wird abgelehnt, und die App
   bittet darum, zuerst zu aktualisieren. Es wird nichts halb eingespielt.
4. Für **jede jemals veröffentlichte Format-Version** liegt eine echte Backup-Datei unter
   `core/backup/src/test/resources/golden/`. Jede neue App-Version muss alle davon
   fehlerfrei und vollständig einlesen, sonst schlägt der Build fehl.

## Einspielen

Beim Wiederherstellen hat man die Wahl:

- **Ersetzen**: Der aktuelle Stand wird vollständig durch das Backup ersetzt.
- **Zusammenführen**: Listen und Einträge aus dem Backup kommen dazu. Gibt es denselben
  Eintrag (gleiche ID) schon, gewinnt die neuere Version.

Vor jedem Wiederherstellen legt die App automatisch ein **Sicherheits-Backup** des aktuellen
Stands an, damit sich ein versehentliches Ersetzen rückgängig machen lässt.

## Sicherheit

Backups sind nicht verschlüsselt. Bewahre sie so auf, wie du die Inhalte auch sonst
aufbewahren würdest. Beim Einlesen gelten Grenzen für Dateigröße und Dateinamen, damit
manipulierte Dateien nichts außerhalb des Backup-Ordners schreiben können.

<div align="center">

# Ordo

**Ranglisten für alles, was dir wichtig ist.**
*Rank everything you care about.*

Native Android app · Kotlin + Jetpack Compose · GPL-3.0

</div>

---

## Deutsch

Ordo ist eine App für lange, persönliche Ranglisten: die besten Autos, Babynamen, Filme,
Urlaubsziele, was auch immer. Statt starrer S/A/B-Stufen sortierst du deine Einträge
Platz für Platz.

- **Beliebig viele Listen**, jede mit eigener Kartenfarbe
- **Einträge mit Titel, Bild und Notiz**
- **Sortier-Modus**: Verschieben nur, wenn du es willst, damit beim Scrollen nichts verrutscht
- **Geteilte Plätze**: zwei Einträge können sich Platz 3 teilen
- **Duell-Modus**: „A oder B?", die App findet den richtigen Platz für neue Einträge
- **Backups**: eine Datei mit allen Listen und Bildern, auch über Versionen hinweg ladbar
- **Updates über [Obtainium](https://github.com/ImranR98/Obtainium)** direkt aus den GitHub-Releases

### Installieren

1. [Obtainium](https://github.com/ImranR98/Obtainium) installieren.
2. App hinzufügen mit der URL dieses Repositorys.
3. Fertig. Obtainium meldet neue Versionen und installiert sie über die alte, deine Listen bleiben erhalten.

## English

Ordo is an app for long, personal rankings. Instead of fixed S/A/B tiers you rank entries
place by place, with images, notes, shared places (ties), a pairwise "duel" mode and
forward-compatible backups. Install and update it with Obtainium from this repository's releases.

---

## Für Entwickler · For developers

| Pfad | Inhalt |
|---|---|
| `app/` | Einstieg, Navigation, Widget |
| `core/model/` | Reine Kotlin-Logik: Rangfolge, Gleichstände, Duell (ohne Android) |
| `core/designsystem/` | Farben, Urbanist, Punktmatrix-Zahlen, Kissen-Karten, Feder-Animationen |
| `core/…` | Datenbank, Backup-Format, gemeinsame UI |
| `feature/…` | Die einzelnen Screens |
| `docs/` | [Design](docs/DESIGN.md), [Releases & Obtainium](docs/RELEASING.md) |

```bash
./gradlew testDebugUnitTest          # alle Tests (JVM + Robolectric)
./gradlew recordRoborazziDebug       # Screenshots neu aufnehmen
./gradlew verifyRoborazziDebug       # Screenshots vergleichen
./gradlew :app:assembleDebug         # Debug-APK
```

Voraussetzungen: JDK 21 und ein Android SDK (`ANDROID_HOME`). Android Studio ist optional.

## Lizenz · License

Ordo ist freie Software unter der [GNU General Public License v3.0](LICENSE).
Die Schrift [Urbanist](https://github.com/coreyhu/Urbanist) steht unter der
[SIL Open Font License 1.1](docs/licenses/Urbanist-OFL.txt).

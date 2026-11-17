# Veröffentlichen & Updates (Obtainium)

Diese Anleitung erklärt, wie Ordo-Versionen signiert, veröffentlicht und über
[Obtainium](https://github.com/ImranR98/Obtainium) auf dem Handy aktualisiert werden.
Sie ist für **Fedora Linux** geschrieben und braucht **kein Android Studio**.

---

## Wie das Ganze zusammenhängt

```
Code auf main ──► GitHub Actions „Release" (du startest es) ──► testet, baut,
signiert mit deinem Schlüssel ──► GitHub Release „v1.2.0" mit Ordo-v1.2.0.apk
──► Obtainium sieht das neue Release ──► installiert es über die alte Version
```

Android installiert ein Update nur über die bestehende App, wenn

1. die **Paket-ID** gleich ist (`io.github.tombolinero.ordo`, nie ändern), und
2. die APK mit **demselben Signatur-Schlüssel** signiert ist, und
3. der interne `versionCode` **größer** ist als der installierte.

Punkt 3 erledigt der Release-Workflow automatisch. Punkt 2 ist dein Schlüssel:
Er liegt verschlüsselt in GitHub Secrets und **niemals** im Repository.

> **Deine Listen bleiben bei jedem Update erhalten.** Ein Update ersetzt nur den
> App-Code. Die Datenbank wird bei Bedarf automatisch auf das neue Format migriert,
> und jede Migration ist durch Tests abgesichert.

---

## Einmalig: Signatur-Schlüssel erzeugen

### 1. Java installieren (enthält das Werkzeug `keytool`)

```bash
sudo dnf install java-21-openjdk-headless
```

### 2. Schlüssel erzeugen

Lege dafür einen Ordner **außerhalb** des Repositorys an, zum Beispiel `~/ordo-signing`:

```bash
mkdir -p ~/ordo-signing && cd ~/ordo-signing
```

```bash
keytool -genkeypair -v -keystore ordo-release.jks -storetype PKCS12 -alias ordo -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Ordo, O=tombolinero"
```

`keytool` fragt zweimal nach einem Passwort. Nimm ein **langes, zufälliges Passwort**
(z. B. aus deinem Passwortmanager generiert, mindestens 20 Zeichen).

### 3. Schlüssel sichern (wichtig!)

Speichere **die Datei `ordo-release.jks` und das Passwort** an mindestens zwei Orten,
zum Beispiel:

- im Passwortmanager (Datei als Anhang + Passwort)
- auf einem USB-Stick, der in der Schublade liegt

**Geht der Schlüssel verloren, kann nie wieder ein Update über die installierte App
gespielt werden.** Man müsste die App deinstallieren und neu installieren. Die Daten
ließen sich dann nur noch über ein Backup retten.

---

## Einmalig: Schlüssel in GitHub hinterlegen

### 4. Schlüssel in Text umwandeln

```bash
base64 -w0 ~/ordo-signing/ordo-release.jks > ~/ordo-signing/ordo-release.jks.b64
```

### 5. Vier Secrets anlegen

Im Browser: Repository auf GitHub öffnen → **Settings** → **Secrets and variables**
→ **Actions** → **New repository secret**. Lege diese vier an:

| Name | Inhalt |
|---|---|
| `ORDO_KEYSTORE_BASE64` | kompletter Inhalt von `ordo-release.jks.b64` (`cat ~/ordo-signing/ordo-release.jks.b64`, alles markieren, kopieren) |
| `ORDO_KEYSTORE_PASSWORD` | dein Passwort aus Schritt 2 |
| `ORDO_KEY_ALIAS` | `ordo` |
| `ORDO_KEY_PASSWORD` | dasselbe Passwort wie oben (bei PKCS12 identisch) |

Alternativ per Terminal mit der GitHub-CLI (`sudo dnf install gh`, dann `gh auth login`):

```bash
gh secret set ORDO_KEYSTORE_BASE64 --repo tombolinero/Tier-List-App < ~/ordo-signing/ordo-release.jks.b64
```

```bash
gh secret set ORDO_KEYSTORE_PASSWORD --repo tombolinero/Tier-List-App
```

```bash
gh secret set ORDO_KEY_ALIAS --repo tombolinero/Tier-List-App --body ordo
```

```bash
gh secret set ORDO_KEY_PASSWORD --repo tombolinero/Tier-List-App
```

(`gh` fragt bei den Passwort-Befehlen nach dem Wert, er landet so nicht in deiner Shell-History.)

### 6. Die Textdatei wieder löschen

```bash
shred -u ~/ordo-signing/ordo-release.jks.b64
```

Secrets sind auch in einem **öffentlichen** Repository sicher: Niemand kann sie
auslesen, auch du nicht. Workflows aus Forks oder fremden Pull Requests bekommen sie nicht.

---

## Eine neue Version veröffentlichen

1. Sicherstellen, dass `main` den gewünschten Stand hat (alle Prüfungen grün).
2. Auf GitHub: **Actions** → **Release** → **Run workflow**.
3. Version eintragen, z. B. `1.2.0` (Schema: `HAUPT.NEBEN.FIX`), und starten.

Der Workflow

- prüft, dass die Version neuer ist als die letzte,
- führt alle Tests aus,
- baut die APK, signiert sie mit deinem Schlüssel und prüft die Signatur,
- erstellt den Git-Tag `v1.2.0` und ein GitHub Release mit `Ordo-v1.2.0.apk`
  und der zugehörigen SHA-256-Prüfsumme.

Der interne `versionCode` wird aus der Version berechnet:
`HAUPT × 10000 + NEBEN × 100 + FIX`, z. B. `1.2.0` → `10200`.

---

## Obtainium auf dem Handy einrichten

1. Obtainium installieren (z. B. über F-Droid oder die GitHub-Releases von Obtainium).
2. **App hinzufügen** → Quell-URL: `https://github.com/tombolinero/Tier-List-App`
3. Optional unter „APK-Filter": `Ordo-v.*\.apk`
4. **Hinzufügen** → **Installieren**.

Obtainium prüft dann regelmäßig auf neue Releases und bietet Updates an.

### Fingerabdruck des Schlüssels (optional, zur Kontrolle)

```bash
keytool -list -v -keystore ~/ordo-signing/ordo-release.jks -alias ordo | grep SHA256
```

Diesen Fingerabdruck kann man in die README eintragen. Damit lässt sich prüfen, dass eine
heruntergeladene APK wirklich von dir signiert wurde.

---

## Häufige Fragen

**Kann ich die Debug-Version und die echte App gleichzeitig installieren?**
Ja. Debug-Builds heißen „Ordo Debug" und haben die Paket-ID
`io.github.tombolinero.ordo.debug`.

**Was passiert, wenn der Release-Workflow fehlschlägt?**
Dann wird nichts veröffentlicht. Der Fehler steht im Log des Workflows.

**Ich habe den Schlüssel verloren.** Neue Schlüssel erzeugen (Schritte 1–6), in der App
ein Backup machen, App deinstallieren, neue Version installieren, Backup einspielen.

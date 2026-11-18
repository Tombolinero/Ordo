# Design-Spezifikation (Referenz-Design)

Abgeleitet aus 5 Referenzbildern (Analytics-Screen, Karten-Grid, Gesamtansicht,
„Hiking Trip"-Screen, Style-Guide). Maße sind geschätzt und auf einen
**390 × 844 pt** Viewport (iPhone 14/15) normiert. Farben teils per Pixel-Sampling
aus den Bildern verifiziert.

> Hinweis: Das „RON DESIGN"-Logo im Style-Guide ist die Signatur des Designers,
> **kein** Bestandteil der App.

---

## 1. Design-Grundlagen

### 1.1 Stil-Charakter
- **Ultra-dark UI**: reines Schwarz als Fläche, Inhalte „leuchten" heraus.
- **Dot-Matrix-Zahlen**: alle Kennzahlen (48 %, 74 %, 32 km …) bestehen aus
  einzelnen runden Punkten – Signatur-Element des Designs.
- **Soft-3D / „Pillow"-Karten**: sehr stark abgerundete Flächen mit weichem
  Innenlicht (oben links heller), dezentem Rand-Glanz und Schlagschatten –
  wirken wie leicht aufgeblasene Kissen bzw. mattes Glas.
- **Ein Akzent**: Gelb `#F6D818` markiert *aktive Werte* (Gauge-Marker,
  Slider-Füllung, Ring-Fortschritt, Primär-Button). Sonst sparsam!
- **Farbige Karten** nur für einzelne Module (Magenta/Lila, Navy/Violett).
- **Feines Punkt-Raster** im Hintergrund (kaum sichtbar, ~4–6 % Weiß).
- Viel Leerraum, wenig Text, keine Trennlinien.

### 1.2 Farbpalette (Style-Guide + Sampling)

| Token | Hex | Verwendung |
|---|---|---|
| `--bg` | `#000000` | App-Hintergrund (gemessen: reines Schwarz) |
| `--bg-deep-navy` | `#000B1A` | Style-Guide-Farbe; Kern der „Sync"-Karte (gemessen `#03041A`) |
| `--surface` | `#161616` → `#2F2F2F` | Dunkle Karte (Rand → Highlight-Zentrum) |
| `--button` | `#080808`–`#0A0A0A` | Runde Icon-Buttons (Mitte), Rand heller `#2A2A2A` |
| `--magenta` | `#BB2C90` | Primär-Gradient (Strain-Karte) |
| `--purple` | `#933193` | Strain-Karte Mitte/Grundton |
| `--purple-light` | `#B671B4` | Highlight oben links auf Strain-Karte (gemessen) |
| `--purple-dark` | `#642B7E` / `#652374` | Strain-Karte unten rechts (gemessen) |
| `--violet` | `#6730DC` | Rand-Glow der Sync-Karte |
| `--violet-muted` | `#252753` | Sync-Karte Randbereich (gemessen) |
| `--yellow` | `#F6D818` | Akzent (gemessen `#EACF19` im Schatten) |
| `--text` | `#FFFFFF` | Primärtext |
| `--text-2` | `rgba(255,255,255,.55)` | Sekundärtext („~14 km range", „Min/Max") |
| `--text-3` | `rgba(255,255,255,.35)` | Tertiär (Nachbarwerte 47 % / 49 %, Labels) |
| `--tick` | `rgba(255,255,255,.22)` | Inaktive Gauge-Striche |
| `--tick-active` | `rgba(255,255,255,.85)` | Aktive Gauge-Striche |
| `--toggle-track` | `#262A3B` | Toggle-Schiene (gemessen) |

### 1.3 Typografie
- **Schrift: Urbanist** (Google Fonts) – geometrisch, runde Formen, für alle Texte.
  - Gewichte: 300 (Light – Labels wie „Cells temp:"), 400 (Body), 500 (Titel, Karten-Titel).
- **Zahlen: Dot-Matrix** (eigene Darstellung, siehe 1.4).

| Rolle | Beispiel | Font | Größe | Gewicht | Farbe |
|---|---|---|---|---|---|
| Screen-Titel | „Analytics", „Hiking Trip" | Urbanist | 24 px | 500 | `--text` |
| Sektion-Titel | „Energy Status" | Urbanist | 20 px | 500 | `--text` |
| Karten-Titel | „Assist Limit", „Strain", „Sync" | Urbanist | 19 px | 500 | `--text` |
| Sublabel | „~14 km range", „Distance covered" | Urbanist | 16 px | 400 | `--text-2` |
| Meta | „Cells temp:" / „34°C" | Urbanist | 12 px | 300 / 500 | `--text-2` / `--text` |
| Status | „System stable" | Urbanist | 13 px | 400 | `--text-2` |
| Achse | „Min", „Max" | Urbanist | 12 px | 300 | `--text-3` |
| Status-Bar | „11:30" | Urbanist/SF | 15 px | 600 | `--text` |
| Hero-Zahl | 48 % | Dot-Matrix | ~64 px Höhe | – | `--text` |
| Großwert | 32 km 200 m | Dot-Matrix | ~72 px | – | `--text` |
| Kartenwert | 74 % | Dot-Matrix | ~40 px | – | `--text` |
| Mini-Wert | 32 % im Ring | Dot-Matrix | ~18 px | – | `--text` |
| Nachbarwerte | 47 % / 49 %, 73 % / 75 % | Dot-Matrix | ~14 px | – | `--text-3` |
| Einheiten | km, m | Dot-Matrix | ~22 px | – | `--text-3` |

### 1.4 Dot-Matrix-Zahlen (Signatur)
- Ziffern sind **Strichzeichnungen aus runden Punkten**, die entlang der
  Mittellinie der Ziffer laufen (nicht starres 5×7-Raster): Kurven bei 2/3/8/0
  sind rund, Geraden (Oberkante der „3", Diagonale der „4") haben dicht
  aneinandergereihte Punkte.
- Punktdurchmesser ≈ 9–10 % der Ziffernhöhe; Abstand ≈ 1,6 × Durchmesser.
- „%" = Schrägstrich aus Punkten + je ein kleines 2×2-Punktcluster oben links / unten rechts.
- Einheiten (km, m) und Nebenwerte in derselben Technik, nur kleiner und grau.
- **Umsetzung (empfohlen):** eigene SVG-Pfade je Zeichen (0–9, %, ., k, m, °),
  gerendert mit
  `stroke-dasharray="0 <abstand>"` + `stroke-linecap="round"` + `stroke-width=<punkt>`
  → erzeugt exakt Punkte entlang der Linie, beliebig skalierbar.
  Fallback: Google Font **„Doto"** (Raster-Dot-Font, optisch nahe).

### 1.5 Formen, Radien, Abstände
- Bildschirmrand (horizontal): **20 px**
- Grid-Gap zwischen Karten: **12 px**
- Karten-Radius: **36 px** (sehr rund, quasi „Squircle"; Strain/Sync wirken fast pillenförmig)
- Runde Buttons: **48 px** (Header), **56 px** (Bottom-Nav)
- Primär-Pill (gelb): **~120 × 64 px**, Radius 32 px
- Vertikaler Rhythmus: 8er-System (8 / 16 / 24 / 32)

### 1.6 Effekte / Tiefe
- **Hintergrund-Punktraster**: `radial-gradient(rgba(255,255,255,.05) 1px, transparent 1.5px)`,
  Raster **16 px**.
- **Dunkle Karte** (Assist Limit):
  `radial-gradient(120% 90% at 30% 25%, #2F2F2F 0%, #1E1E1E 45%, #121212 100%)`
  + Innenlicht `inset 1px 1px 0 rgba(255,255,255,.06)`
  + Schatten `0 20px 40px rgba(0,0,0,.6)`.
- **Strain-Karte**:
  `radial-gradient(130% 120% at 15% 10%, #D79BD0 0%, #B671B4 18%, #933193 55%, #642B7E 100%)`
  + leichter Glanz oben links, Schatten wie oben.
- **Sync-Karte**: dunkler Kern mit violettem Rand-Leuchten:
  `radial-gradient(90% 80% at 50% 45%, #03041A 35%, #1A1D4A 70%, #4B37B0 92%, #6730DC 100%)`.
- **Runde Icon-Buttons**: dunkle Mitte, hellerer weicher Rand (Glas-Ring):
  `radial-gradient(circle, #080808 55%, #1C1C1C 85%, #2A2A2A 100%)`
  + `0 8px 20px rgba(0,0,0,.6)`; Icon weiß, ~20 px, Linienstärke 1.75.
- **Gelbe Pill**: `#F6D818` mit Punkt-Raster (dunklere Punkte
  `rgba(120,100,0,.35)`, Ø 3 px, Raster 8 px, ~4 Reihen × 10 Spalten, zu den
  Rändern ausblendend) + Glanz oben `inset 0 2px 6px rgba(255,255,255,.35)`.

---

## 2. Komponenten

### 2.1 Status-Bar
Links Uhrzeit „11:30", rechts Signal-Balken, WLAN, Akku (gefüllt, weiß).
Kamera-Punkt mittig (Dynamic Island / Punch-hole). Höhe ~54 px.

### 2.2 Header
`[◯ ←]   Titel (zentriert)   [◯ Aktion]`
- Links: Zurück-Button (Pfeil ←).
- Rechts je Screen: **Teilen/Upload** (Pfeil nach oben aus Ablage) auf Analytics,
  **Mehr** (⋮ vertikal) auf Hiking Trip.
- Titel optisch auf Buttonmitte ausgerichtet.

### 2.3 Radial-Gauge („Energy Status")
- Durchmesser ~**300 px**, horizontal zentriert.
- **120 Striche** (alle 3°), radial nach innen zeigend.
  - Inaktiv: Länge 8 px, Breite 1.5 px, `--tick`.
  - Aktiver Bereich (von 12 Uhr im Uhrzeigersinn bis Wert, z. B. 48 % ≈ 173°):
    Striche heller; **jeder 5. Strich = Major** (Länge ~20 px, weiß, 2 px).
  - **Gelber Marker-Strich** an der aktuellen Position (Länge ~22 px, 2.5 px).
- Mitte: Hero-Zahl „48%" (Dot-Matrix), links „47%" und rechts „49%"
  klein & grau, leicht unterhalb der Mittelachse (wie ein Wertewähler/Odometer).
- Darunter „~14 km range" (Sublabel).
- Oben rechts außerhalb: „Cells temp: **34°C**".
- Unten links außerhalb: „System stable".

### 2.4 Karte „Assist Limit" (dunkel, hoch)
- Breite ≈ 50 % minus halbe Gap, Höhe = 2 Kartenzeilen + Gap (~290 px).
- Titel oben links (Padding 24 px).
- Mitte: „74%" Dot-Matrix, flankiert von „73%" / „75%" (grau, Grundlinie unten).
- Unten: **Slider**
  - Gefüllter Teil: gelbe Linie 2 px von Min bis Thumb.
  - Thumb: gelbes, nach unten zeigendes Dreieck/Tropfen ~12 px auf der Linie.
  - Restteil: gepunktete graue Linie (Punkte 1.5 px, Abstand 4 px).
  - „Min" unten links, „Max" unten rechts.

### 2.5 Karte „Strain" (Magenta-Gradient)
- Rechte Spalte, oben, Höhe ~140 px.
- „Strain" links vertikal mittig.
- Rechts **Ring** Ø ~84 px:
  - Spur: dünner gepunkteter Kreis, `rgba(255,255,255,.35)`.
  - Fortschritt: gelber Bogen 3 px, runde Kappen, startet 12 Uhr, im Uhrzeigersinn (32 %).
  - Mitte: „32%" Dot-Matrix klein.

### 2.6 Karte „Sync" (Navy/Violett-Glow)
- Rechte Spalte, unten, Höhe ~140 px.
- „Sync" links, rechts **Toggle**:
  - Schiene 64 × 34 px, `#262A3B`, Radius voll.
  - Knopf 30 px weiß `#F6F6F6`, weicher Schatten; Aus = links, An = rechts
    (An-Zustand: Schiene gelb oder violett – im Bild nicht sichtbar, Vorschlag: `--yellow`).

### 2.7 Bottom-Navigation (schwebend)
`[◯ Ordner]     [ ▒▒ gelbe Punkt-Pill ▒▒ ]     [◯ Globus]`
- ~24 px über Safe-Area-Unterkante, horizontal zentriert verteilt.
- Links: Ordner/Dateien-Icon, rechts: Globus-Icon (runde Glas-Buttons 56 px).
- Mitte: gelbe Pill = Primär-Aktion/Home (Punktmuster wie Lautsprechergitter).

---

## 3. Screen-Aufbau

### 3.1 Screen „Analytics" (von oben nach unten)
1. Status-Bar
2. Header: ← · „Analytics" · Teilen
3. Zeile: „Energy Status" (links) – „Cells temp: 34°C" (rechts, etwas tiefer)
4. Radial-Gauge mit 48 % / ~14 km range
5. „System stable" (links unter der Gauge)
6. Karten-Grid (2 Spalten):
   ```
   ┌────────────┐ ┌────────────┐
   │Assist Limit│ │ Strain  ◔  │
   │            │ └────────────┘
   │ 73 74% 75  │ ┌────────────┐
   │ ━━━▼┄┄┄┄┄  │ │ Sync   (●○)│
   │ Min    Max │ └────────────┘
   └────────────┘
   ```
7. Bottom-Navigation (schwebend, über dem Inhalt, Inhalt scrollt darunter)

### 3.2 Screen „Hiking Trip"
1. Status-Bar
2. Header: ← · „Hiking Trip" · ⋮
3. „Distance covered" (zentriert, `--text-2`, 18 px)
4. Großwert: **32** `km` **200** `m` (Dot-Matrix, Einheiten klein & grau auf Grundlinie)
5. Weiterer Inhalt nicht sichtbar; unten Ansatz der Bottom-Nav (gelbes Element mit Icon).

---

## 4. Bewegung / Interaktion (abgeleitet, Vorschlag)
- Zahlenwechsel: Nebenwerte (47/49) deuten **vertikales/horizontales Rollen** an –
  Wert-Änderung als Slide + Fade der Dot-Matrix-Ziffern.
- Gauge-Striche füllen sich beim Laden im Uhrzeigersinn (600 ms, ease-out).
- Ring „Strain" animiert Bogen von 0 → Wert.
- Slider & Toggle: Feder-Animation (≈ 250 ms), Haptik bei Rasterpunkten.
- Buttons: beim Drücken scale 0.96 + Schatten kleiner.

## 5. Offene Punkte
- Ziel-Plattform/Tech-Stack (Web/PWA, React Native/Expo, Flutter, SwiftUI).
- Inhalt der App: geklärt, Ranglisten-App „Ordo“ (siehe [DECISIONS.md](DECISIONS.md)).
- Weitere Screens (Inhalt unter „Hiking Trip", Ziele der Bottom-Nav).
- Toggle-An-Zustand und Light-Mode sind in den Referenzen nicht definiert.

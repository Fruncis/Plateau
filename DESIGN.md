# Plateau: design system

Riferimento visivo: https://claude.ai/artifact/4g9EZL7qwzBYrToc7Ge5uV (tavole Logo, Brand, Home e Sessione, in tema scuro e chiaro).
Valori numerici: `design/tokens.json`. Implementazione: `ui/theme/` e `ui/components/`.
Se cambia un valore, aggiorna `tokens.json`, il codice Kotlin e questo file nello stesso commit.

## Principi
- **Il masso sale.** Una sola azione primaria per schermata, in Ember. Tutto il resto resta calmo.
- **I numeri sono i protagonisti.** Carichi e ripetizioni sono grandi, in Space Grotesk, a cifre tabulari.
- **Si usa in palestra:** contrasto alto, bersagli touch da almeno 44 dp, inserimento con pochi tap.
- **Tono:** diretto e calmo, frasi in minuscolo dopo la prima lettera. Le azioni dicono cosa succede ("Completa serie 3", non "Invia").

## Colori (usa sempre i ruoli, mai gli esadecimali)
| Ruolo (codice) | Scuro | Chiaro | Uso |
|---|---|---|---|
| `colorScheme.background` | #111318 | #F7F5F0 | sfondo schermata |
| `colorScheme.surface` | #1C1F26 | #FFFFFF | card, campi, tab bar |
| `colorScheme.surfaceVariant` / `outline` | #2A2E37 | #E4E0D6 | chip, bordi, tracce |
| `colorScheme.onBackground` / `onSurface` | #F4F1EA | #111318 | testo principale |
| `colorScheme.onSurfaceVariant` | #A3AAB8 | #5B6270 | testo secondario |
| `colorScheme.primary` / `onPrimary` | #FF7A3D / #111318 | stessi | azione principale, masso |
| `PlateauTheme.extras.progress` | #7DD3A8 | #7DD3A8 | riempimenti positivi (check, pallini) |
| `PlateauTheme.extras.progressText` | #7DD3A8 | #17744D | numeri/testo positivi (record, +kg) |
| `PlateauTheme.extras.accentText` | #FF7A3D | #B93F08 | arancione usato come testo |

Regole: l'arancione `primary` non va mai usato come testo su sfondo chiaro (usa `accentText`); il verde come testo usa sempre `progressText`.
Il tema segue il sistema (`isSystemInDarkTheme()`).

## Tipografia
Space Grotesk (titoli, numeri, bottoni) e Inter (testo). Usa solo `MaterialTheme.typography.*`, più `NumberLarge` e `NumberDisplay` per carichi e ripetizioni.
Titolo schermata: `headlineLarge`. Nome esercizio: `titleMedium`. Dati secondari: `bodySmall` in `onSurfaceVariant`.

## Forme e spaziatura
Card principali `shapes.large` (24), bottoni `shapes.medium` (18), campi e righe serie `shapes.small` (14), chip `PillShape`.
Spaziatura da `Spacing.*` (4/8 dp), margine laterale `Spacing.xl` (20). Bottone alto `Size.buttonHeight` (56), tab bar `Size.tabBarHeight` (84).
Grafici di Progressi: anello `Size.ringSize` (120) con tratto `Size.ringStroke` (12), grafico e1RM `Size.chartHeight` (200), volume `Size.chartHeightCompact` (160); linee `Size.chartLine` (2), punti `Size.chartPoint` (raggio 4), griglia `Size.chartGrid` (1), trattino target `Size.chartDash` (6).
Icone a tratto da 2 dp con estremi arrotondati (Material Symbols "rounded", peso 400 o il più vicino).

## Componenti
- `PlateauMark`: logo, per header e splash. Colori sovrascrivibili.
- `PlateauButton`: azione principale.
- `MuscleChip`: gruppo muscolare (`emphasized` per il primario).
Prima di creare un componente nuovo, controlla se uno esistente basta. I nuovi componenti vanno in `ui/components/` con un `@Preview` scuro e uno chiaro.

## Schermate di riferimento
- **Home (Oggi):** intestazione con data e titolo del giorno, striscia della settimana, card dell'allenamento (righe con nome, serie×reps e `MuscleChip`), `PlateauButton` "Inizia allenamento", card dell'ultimo record, tab bar a 5 voci (Oggi, Schede, Scopri, Progressi, Foto).
- **Sessione:** titolo esercizio con chip dei gruppi, riga "Ultima volta", righe serie (#, kg, reps, stato), timer di recupero, e1RM stimato, bottone fisso in basso "Completa serie N". La serie in corso ha bordo `primary` e numeri in `NumberLarge`.

## Da non fare
- Colori, dimensioni di testo o raggi scritti a mano nelle schermate.
- Più di un bottone Ember per schermata.
- Rosso/verde come unica differenza tra due stati: aggiungi sempre icona o testo.
- Gradienti, ombre pesanti ed emoji nell'interfaccia.

## Icona dell'app
`mipmap-anydpi-v26/ic_launcher*.xml` (adattiva, sfondo Basalt, masso Ember, con livello monocromatico per le icone tematiche).
L'icona da 512 px per il Play Store va esportata dalla tavola "Logo e icona".

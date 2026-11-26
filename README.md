# Plateau — Android App (Kotlin + Jetpack Compose)

App per il tracking dell'allenamento con focus su log serie, import schede, generazione programmi, analisi progressi e foto.

## Stack
- **Lingua**: Kotlin 2.0
- **UI**: Jetpack Compose + Material3
- **Architettura**: MVVM + Clean Architecture (Domain/Data/Presentation)
- **DI**: Hilt
- **Database**: Room (SQLite)
- **Preferenze**: DataStore
- **Background**: WorkManager
- **Fotocamera/ML**: CameraX + ML Kit (Pose Detection, Subject Segmentation)
- **Networking**: OkHttp + Retrofit + Kotlinx Serialization (per LLM proxy opzionale)
- **Immagini**: Coil

## Struttura moduli (single-module per ora)
```
app/
├── data/
│   ├── local/          # Room: entities, DAOs, database, seed
│   └── repository/     # Repository implementations
├── domain/
│   ├── model/          # Domain models (ExerciseDetail, PlanDetail, ProgressMetrics, PhotoMetrics)
│   └── usecase/        # Use cases (GetActivePlanUseCase, ...)
├── di/                 # Hilt modules (DatabaseModule, RepositoryModule, DataStoreModule)
├── ui/
│   ├── components/     # PlateauButton, PlateauMark, MuscleChip, BottomNavBar
│   ├── theme/          # Color, Type, Dimens, Theme (PlateauTheme)
│   ├── home/           # HomeScreen, HomeViewModel
│   ├── session/        # SessionScreen, SessionViewModel
│   ├── plans/          # PlansScreen, PlansViewModel
│   ├── discover/       # DiscoverScreen, DiscoverViewModel
│   ├── progress/       # ProgressScreen, ProgressViewModel
│   ├── photo/          # PhotoScreen, PhotoViewModel
│   └── navigation/     # Destination sealed class
└── PlateauApplication, MainActivity
```

## Design System
Definito in `DESIGN.md` e `design/tokens.json`:
- **Colori**: ruoli semantici (primary=Ember #FF7A3D, progress=Moss #7DD3A8, accentText per arancione come testo)
- **Tipografia**: Space Grotesk (display/numeri) + Inter (testo), cifre tabulari per carichi
- **Forme**: card large (24dp), bottoni medium (18dp), campi small (14dp), chip pill
- **Spaziatura**: griglia 4/8dp, margine laterale 20dp
- **Componenti riutilizzabili**: `PlateauButton` (una sola azione primaria per schermata), `MuscleChip`, `PlateauMark`

## Schermate (Bottom Nav a 5 tab + Sessione)
1. **Oggi (Home)**: giorno della scheda corrispondente al weekday, card allenamento attivo, ultimo PR, bottone "Inizia allenamento"
2. **Sessione**: esercizio corrente con chip gruppi, righe serie precompilate, timer recupero, e1RM, bottone fisso "Completa serie N"
3. **Schede**: scheda attiva + archivio, creazione manuale/import/generatore (Fase 2+)
4. **Scopri**: catalogo esercizi con filtri, sezione "Consigliati per te" basata su buchi volume/movimento
5. **Progressi**: e1RM trend (Epley), volume load settimanale, PR, doppia progressione
6. **Foto/Report** (Fase 4): cattura guidata (ghost overlay), confronto con metriche per regione, zone carenti, kcal target (Mifflin-St Jeur + TDEE da log reali)

## Modello dati (Room)
- `MuscleGroup`, `Exercise` (con alias IT/EN, pattern, attrezzatura, difficoltà)
- `Plan`, `PlanDay` (weekday o label), `PlanExercise` (sets, reps range, rest, note)
- `WorkoutSession`, `SetLog` (reps, weight, rpe, timestamp)
- `BodyMetric` (peso), `BodyPhoto` (pose, qualityScore, goalAtCapture), `PhotoAnalysis`
- `Goal` (BULK/CUT/MAINTENANCE con storico), `UserProfile`

## Build
```bash
./gradlew assembleDebug
```

## Note sviluppo
- Font: scaricare Space Grotesk e Inter (OFL) in `app/src/main/res/font/` come `.ttf`, poi aggiornare `Type.kt` per usare `Font(R.font.xxx)`
- Icona app: già configurata in `mipmap-anydpi-v26/` (adaptive, sfondo Basalt, masso Ember)
- Tema: segue sistema (`isSystemInDarkTheme()`), avvolgere tutto in `PlateauTheme { MaterialTheme { ... } }`
- Non usare mai colori/dimensioni hardcoded: solo `MaterialTheme.colorScheme`, `MaterialTheme.typography`, `MaterialTheme.shapes`, `PlateauTheme.extras`, `Spacing`, `Size`

## Roadmap (da SPEC)
- [x] **Fase 1**: catalogo, schede manuali, Home "oggi", logging, progressi, e1RM
- [ ] **Fase 2**: import .md/.txt con revisione, Scopri con filtri
- [ ] **Fase 3**: generatore da zero, rotazione bimestrale, toggle bulk/cut
- [ ] **Fase 4**: foto guidate, report overlay, kcal adattivi
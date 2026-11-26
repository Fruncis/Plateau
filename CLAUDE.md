# Plateau (app Android, Kotlin + Jetpack Compose)

## UI
- Prima di creare o modificare una schermata leggi `DESIGN.md` (regole) e `design/tokens.json` (valori).
- Avvolgi l'app in `PlateauTheme`. Usa solo `MaterialTheme.colorScheme`, `MaterialTheme.typography`, `MaterialTheme.shapes`, `PlateauTheme.extras`, `Spacing` e `Size`: mai colori, dimensioni o raggi hardcoded.
- Riusa i componenti in `ui/components/`; ogni schermata nuova ha un `@Preview` scuro e uno chiaro.
- Una sola azione primaria (`PlateauButton`) per schermata.

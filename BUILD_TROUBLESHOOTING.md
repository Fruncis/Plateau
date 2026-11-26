# Plateau App - Build Troubleshooting Log

**Date:** 2026-11-26  
**Branch:** phase2/import-discover-progress  
**Project:** Plateau (Android, Kotlin + Jetpack Compose)

---

## Problem Summary

The Android app build was failing at the `kaptDebugKotlin` task with annotation processing errors related to Hilt DI modules.

---

## Initial State

### Build Errors (First Run)
```
C:\Users\Francesco\Documents\Plateau\app\build\tmp\kapt3\stubs\debug\com\plateau\app\di\DatabaseModule.java:4: error: incompatible types: NonExistentClass cannot be converted to Annotation
@error.NonExistentClass()
      ^
... (similar errors for DataStoreModule, RepositoryModule)
```

### Root Cause Identified
The KAPT configuration had `correctErrorTypes = false` which caused annotation processor to generate invalid stubs with `@error.NonExistentClass()` placeholders instead of proper error handling.

---

## Attempted Fixes (Chronological)

### 1. Fixed KAPT `correctErrorTypes` Setting
**File:** `app/build.gradle.kts`  
**Change:** `correctErrorTypes = false` → `correctErrorTypes = true`

**Result:** Error changed from `NonExistentClass` to Hilt-specific errors about missing `@InstallIn` annotations and unresolved `Preferences` type.

---

### 2. Updated Hilt Version
**Files:** 
- `build.gradle.kts` (root): `2.51` → `2.51.1`
- `app/build.gradle.kts`: `2.51` → `2.51.1` (plugin + hilt_version variable)

**Result:** No change - same errors persisted.

---

### 3. Disabled Hilt InstallIn Check via KAPT Arguments
**File:** `app/build.gradle.kts`  
**Change:**
```kotlin
kapt {
    correctErrorTypes = true
    arguments {
        arg("hilt.disableInstallInCheck", "true")
    }
}
```

**Result:** Warning appeared: `The following options were not recognized by any processor: '[hilt.disableInstallInCheck, kapt.kotlin.generated]'`. The Hilt processor didn't recognize this flag in KAPT context.

---

## Current Errors (After All Fixes)

### Error 1: @InstallIn Annotation Not Found in Generated Stubs
```
C:\Users\Francesco\Documents\Plateau\app\build\tmp\kapt3\stubs\debug\com\plateau\app\di\DatabaseModule.java:15: error: cannot find symbol
@InstallIn()
 ^  symbol: class InstallIn
```

**Analysis:** The `@InstallIn` annotation from `dagger.hilt.android.scopes.InstallIn` is present in source code but not being resolved in KAPT-generated Java stubs.

### Error 2: Preferences Type Not Resolved
```
error: ModuleProcessingStep was unable to process 'com.plateau.app.di.DataStoreModule' because 'Preferences' could not be resolved.
```

**Analysis:** `androidx.datastore.preferences.Preferences` is imported in source but not available during KAPT processing.

---

## Source Code Analysis (DI Modules)

All three modules have correct annotations in source:

### DatabaseModule.kt
```kotlin
@Module
@InstallIn(SingletonComponent::class.java)
object DatabaseModule { ... }
```

### DataStoreModule.kt
```kotlin
@Module
@InstallIn(SingletonComponent::class.java)
object DataStoreModule {
    @Provides @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> { ... }
}
```

### RepositoryModule.kt
```kotlin
@Module
@InstallIn(SingletonComponent::class.java)
object RepositoryModule { ... }
```

---

## Dependency Analysis

### Hilt Dependencies (from `./gradlew :app:dependencies --configuration kapt`)
```
com.google.dagger:hilt-compiler:2.51.1
  +-- com.google.dagger:dagger:2.51.1
  +-- com.google.dagger:dagger-compiler:2.51.1
  +-- com.google.dagger:dagger-spi:2.51.1
  \-- org.jetbrains.kotlin:kotlin-stdlib:1.9.20
```

### Kotlin Version
- Root: `1.9.25`
- KAPT resolves to: `1.9.20` (from dagger-spi)

### DataStore Dependency
```kotlin
implementation("androidx.datastore:datastore-preferences:1.1.2")
kapt("androidx.datastore:datastore-preferences:1.1.2")  // <-- This might be wrong
```

**Issue:** `kapt("androidx.datastore:datastore-preferences:1.1.2")` - DataStore preferences doesn't have a KAPT processor; this dependency should only be `implementation`.

---

## Hypotheses for Root Cause

1. **Kotlin/KAPT Version Mismatch**: Kotlin 1.9.25 with KAPT using 1.9.20 stdlib from Hilt may cause annotation processing issues.

2. **Incorrect KAPT Dependency**: `kapt("androidx.datastore:datastore-preferences:1.1.2")` is incorrect - DataStore doesn't use KAPT for preferences.

3. **Hilt Compiler Version Incompatibility**: Hilt 2.51.1 may have issues with Kotlin 1.9.25 / AGP 8.7.3.

4. **Missing KAPT Plugin Configuration**: The `kapt` plugin might need explicit annotation processor options for Hilt.

---

## Recommended Next Steps

### Option 1: Remove Incorrect KAPT Dependency (Most Likely Fix)
```kotlin
// In app/build.gradle.kts - REMOVE this line:
kapt("androidx.datastore:datastore-preferences:$datastore_version")
```

### Option 2: Align Kotlin Versions
Update root `build.gradle.kts` to use Kotlin 1.9.20 to match Hilt's transitive dependency:
```kotlin
id("org.jetbrains.kotlin.android") version "1.9.20" apply false
id("org.jetbrains.kotlin.plugin.serialization") version "1.9.20" apply false
```

### Option 3: Upgrade Hilt to Latest Compatible Version
Try Hilt 2.52+ which has better Kotlin 1.9+ support.

### Option 4: Add Explicit Hilt Annotation Processor
```kotlin
kapt("com.google.dagger:hilt-compiler:2.51.1")  // Already present
kapt("androidx.hilt:hilt-compiler:1.1.0")        // May be needed for AndroidX Hilt
```

---

## Environment Info

- **Android CLI:** 1.1.16919734
- **SDK:** C:\Users\Francesco\AppData\Local\Android\Sdk
- **Gradle:** 8.11.1 (wrapper)
- **AGP:** 8.7.3
- **Kotlin:** 1.9.25 (configured) / 1.9.20 (resolved via Hilt)
- **Java:** 17 (compile/target)
- **Emulator:** Medium_Phone_API_37.0 (emulator-5554)

---

## Files Modified During Troubleshooting

| File | Changes |
|------|---------|
| `app/build.gradle.kts` | `correctErrorTypes = true`, added `hilt.disableInstallInCheck` argument, Hilt version 2.51.1 |
| `build.gradle.kts` (root) | Hilt plugin version 2.51.1 |

---

## Commands to Test Next Fix

```bash
# 1. Remove incorrect KAPT dependency from app/build.gradle.kts
# 2. Clean and rebuild
export PATH="$PATH:/c/Users/Francesco/AppData/Local/Android/Sdk/platform-tools"
./gradlew clean assembleDebug
```
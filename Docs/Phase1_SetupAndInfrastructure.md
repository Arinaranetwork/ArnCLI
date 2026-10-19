# Phase 1 - SetupAndInfrastructure
## Goal
Establish the foundational Android application architecture, Gradle build system, automated UI rule verification task, Electric Slate design tokens, Elms Sans fonts, preset component imports, and configuration import/export scaffolding for ArnTCLI.

## Scope
- Initialize standard Android Gradle modular project (`settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, Gradle wrapper 9.1.0).
- Integrate automated `VerifyAndroidRulesTask` into Gradle lifecycle (`preBuild`, `testDebugUnitTest`, `check`).
- Import and configure the Elms Sans font family (`elms_sans_bold.ttf`, `elms_sans_light.ttf`, `elms_sans_medium.ttf`).
- Import Electric Slate theme tokens, universal spring physics, and standardized component presets from Fotara.
- Implement baseline application classes (`ArnTCLIApp`, `MainActivity`).
- Setup configuration/profile import & export interfaces and contracts (`ProfileExporter`, `ProfileImporter`).
- Verify project compilation and automated Android UI rules enforcement.

## Out Of Scope
- Implementation of Linux PTY terminal emulation engine, shell fork/exec, or virtual terminal screen (VTE).
- Terminal package manager repositories (apt/pkg).
- Complex multi-window terminal sessions.

## Features
### Baseline Gradle & Android Application Setup
Standard Gradle 9.1 / AGP 9.0 configuration with Compose BOM, Kotlin 2.3, Java 17 toolchain, AndroidX dependencies, and clean package structure under `com.arinara.arntcli`.

### Automated Android UI Rules Verification
Custom Gradle task `VerifyAndroidRulesTask` scanning UI source code for strict token adherence (AR-001), mandatory component presets (AR-003), standardized shape hierarchy (AR-004), max 3 active colors (AR-005), safe bottom clearance (AR-006), outline widths (AR-008), modal presets (AR-009), typography scale (AR-010), elevation/shadows (AR-011), prohibition of white borders (AR-012), single border color (AR-013), and hardware UI clearance (AR-015).

### UI System, Fonts & Presets
Full integration of the Elms Sans font family, Electric Slate dark color palette (#0D1117, #161C26, #7A5AF8), universal spring physics (JigglePress, ViscousGooey, DismissFade), and centralized component presets (`AppScreen`, `AppTopBar`, `AppButton`, `AppCard`, `AppFloatingBar`, `AppListItem`, `AppBottomSheet`, `AppNoticeBanner`, `AppPillChip`, `AppSlider`, `ConnectedInspector`).

### Import & Export Scaffolding
Centralized contracts and serializers for exporting and importing terminal environment profiles, color palettes, and workspace configurations cleanly to and from JSON/file descriptors.

### Master Roadmap Architecture Specification
Formulation and documentation of the comprehensive v0.1 to v10.0 master roadmap covering the 3 Core Pillars (Space, Vault, Pocket), 8 functional modules (Connect, NetTools, DevLab, Tunnel, Monitor, Automation, Copilot, Knowledge), Android rootless platform engineering mitigations, zero-server offline design, and competitive developer ergonomics.

## UI Mockup
```
+------------------------------------------+
| ArnTCLI                     [Settings]   |
+------------------------------------------+
|  Terminal Workspace Ready                |
|  Elms Sans UI | Monospace CLI Output     |
|  Electric Slate (#0D1117)                |
|                                          |
|  [ Preset System Verified ]              |
|  [ VerifyAndroidRulesTask Active ]       |
|                                          |
|                                          |
+------------------------------------------+
| [Terminal]    [Sessions]    [Settings]   |
+------------------------------------------+
```

## Logic Notes
- Gradle build lifecycle binds `verifyAndroidRules` before compilation to enforce compliance.
- Presets enforce default Electric Slate tokens; no raw Composable styles permitted.
- Import/Export abstraction defines decoupled interfaces for session and profile data serialization.

## Risks
- Tooling version mismatches with local Android SDK -> Mitigation: Pin compileSdk/targetSdk to 36 and java toolchain to 17 matching local environment.

## Dependencies
- Android Gradle Plugin 9.0.1
- Kotlin 2.3.20
- Jetpack Compose BOM 2026.03.01

## Acceptance Criteria
- `VerifyAndroidRulesTask` compiles and executes in Gradle without error.
- All Elms Sans font files copied and accessible in `res/font`.
- All 11 component presets imported with package `com.arinara.arntcli.ui.components.presets`.
- Design tokens, spring physics, and strings configured cleanly.
- `app/src/main/AndroidManifest.xml` and `MainActivity` properly set up.
- Project compiles cleanly and passes `verifyAndroidRules`.
- `Docs/Roadmap.md` established with complete v0.1–v10.0 specifications, zero-cloud architecture, and Android platform workarounds.

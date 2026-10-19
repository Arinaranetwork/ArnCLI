# Rules - ArnTCLI
Last Updated: 2026-10-19

## Engineering
### R-001 - Strict Layered Architecture
Business logic, terminal environment handling, data access, and presentation must remain strictly separated. UI composables and activities must only observe ViewModels or state holders, and must never interact directly with low-level PTY/TTY descriptors, SQLite/Room databases, or raw disk filesystems. Reason: Guarantees testability, isolation, and long-term maintainability. Example violation: Executing shell commands or writing configuration files directly inside a Composable function.

### R-002 - Android Framework Naming and Placement Conventions
Framework-mandated files and directories (`app/`, `gradle/`, `build.gradle.kts`, `settings.gradle.kts`, `AndroidManifest.xml`, Kotlin packages under `com.arinara.arntcli`) are exempt from root PascalCase naming where build tooling requires standard Gradle conventions. Project-level folders created outside the Android module (`/Docs`, `/Changelog`, `/Assets`, `/Output`, `/Font`) must strictly use PascalCase. Reason: Prevents Gradle and Android build tooling breakages while preserving Arinara repository discipline. Example violation: Renaming `build.gradle.kts` to `BuildGradle.kts` or `app` to `App` causing build failures.

### R-003 - Offline-First Local Shell Operations
All terminal sessions, shell commands, scripts, local file indexing, and session management must function completely offline with zero mandatory network connectivity. Any failure in terminal process spawning, I/O streaming, or configuration parsing must be caught, surfaced with user-friendly error states, and recorded in logs without swallowing exceptions. Reason: Developers and system operators require reliable terminal tooling regardless of network availability. Example violation: Requiring a cloud ping before starting a terminal session or wrapping process spawning in an empty `catch (e: Exception) {}`.

### R-009 - Automated Android UI Rules Verification in Gradle
All Android.md design system rules (AR-001 through AR-016) must be enforced via the automated Gradle verification task `VerifyAndroidRulesTask`, bound directly to build and test lifecycles (`preBuild`, `testDebugUnitTest`, `check`). Builds must fail immediately if any component violates token adherence, border color restrictions, aspect ratios, hardware UI insets, or header spacing. Reason: Guarantees zero regression and continuous automated compliance with Arinara design standards across all developer and CI environments. Example violation: Bypassing Gradle verification or disabling automated rule checks.

## Design
### R-004 - Terminal & Workspace Visual System Realization
The terminal workspace and surrounding UI must strictly reproduce the Arinara Electric Slate brand aesthetic: near-black canvas background (`#0D1117`), dark rounded rectangle cards and panels (`#161C26`, ~24dp radius), subtle structural borders (`#262F3E`, 1dp), electric violet primary accents (`#7A5AF8`), and clean typography hierarchy. Reason: Preserves distinctive brand identity, visual calm, high legibility, and contrast harmony across dense CLI environments. Example violation: Using arbitrary high-contrast pure white borders or rainbow toolbars.

### R-005 - Zero Placeholder Shipped Work
All shipped screens and components must provide complete empty, loading, error, and populated states. No dummy mock data, TODOs, or fake stubs in production source code (`app/src/main/`). Reason: Ensures production-grade readiness at every milestone. Example violation: Leaving `// TODO: implement terminal` or hardcoding fake command outputs in production viewmodels.

## Release
### R-006 - Release Artifact Discipline
Shippable release artifacts must be packaged and copied into `/Output/Release/` adhering to the standard naming template `ArnTCLI_<x.y.z>[_Beta][_<Platform>_<Arch>].apk`. Intermediate build outputs in `build/` or `app/build/` must remain gitignored. Reason: Ensures clean, reproducible release tracking. Example violation: Copying unversioned `app-debug.apk` directly to repository root.

### R-007 - Autonomous GitHub Release Workflow
GitHub releases, tags, asset attachments, and repository operations are executed autonomously using pre-configured machine credentials (`GH_TOKEN` / Windows Keyring `gh auth`) via GitHub CLI (`gh`). Releases must bundle shippable artifacts from `/Output/Release/` and notes from `/Changelog/`. Reason: Guarantees reliable, unprompted release deployment across AI sessions and tools. Example violation: Requesting manual user credentials or skipping APK asset upload.

### R-008 - Desktop Workstation Directory Discipline
Any future desktop companion or workstation application lives in a dedicated PascalCase root folder (e.g., `/ArnTCLIDesktop`). Desktop release binaries are packaged into `/Output/Release/ArnTCLI_<x.y.z>[_Beta]_Windows_x64.exe`. Reason: Isolates desktop web and native shell code while preserving root PascalCase topic folder discipline. Example violation: Dropping desktop files directly at project root.

# Phase 10 - PublicLaunchAndHardening

## Goal
Deliver the flagship 1.0.0 Public Launch release of ArnTCLI: a tactile, offline-first mobile terminal workstation for sysadmins, DevOps, and network engineers with an interactive macro key bar, dynamic snippet parameter interpolation, cross-session automation workflows, zero-GC ring buffer session multiplexing, and hardened production release packaging.

## Scope
- Interactive Modifier & Macro Key Bar in `TerminalScreen` (Ctrl/Alt sticky latches, Esc `\u001b`, Tab `\t`, ANSI directional arrows, F1-F12 VT100 sequences).
- Snippet Parameter Interpolation Engine (`SnippetInterpolationEngine`) evaluating `{{host}}`, `{{user}}`, `{{port}}`, `{{vault_secret:key}}`.
- Terminal Automation Workflow Engine (`AutomationWorkflowEngine`) executing sequential runbooks with regex assertions, timeouts, step delays, and system notifications.
- Memory-bounded Terminal Session Manager (`TerminalSessionManager`) utilizing fixed 5,000-line ring buffers (`CircularLogBuffer`) for zero-leak high-throughput streaming.
- Production R8/ProGuard rules and release packaging verification.

## Out Of Scope
- Third-party cloud sync services, analytics SDKs, and telemetry (preserves 100% offline-first architecture).
- Desktop GUI packaging outside Android.

## Features
### Macro Key Bar & Interactive Modifiers
- Sticky latches for `CTRL` and `ALT` sending ASCII control bytes (`Ctrl+C` = `\u0003`, `Ctrl+D` = `\u0004`, `Ctrl+Z` = `\u001A`) and intercepting typed characters (`Ctrl+C`, `Ctrl+L`, `Ctrl+U`, `Ctrl+A`, `Ctrl+E`, `Ctrl+W`).
- Single-tap `ESC` (`\u001b`), `TAB` (`\t` path/command completion), and VT100 directional arrows (`\u001b[A`/`\u001b[B` for command history navigation, `\u001b[C`/`\u001b[D` for cursor movement).
- Quick macro pills and stateful built-in commands (`cd`, `pwd`, `export`, `unset`, `env`, `history`, `help`, `clear`, `ll`, `la`).
- Per-session writable sandbox home directory, persistent working directory (`cwd`), environment variables, and active process cancellation (`SIGKILL`/`SIGINT`).

### Snippet Parameter Interpolation
- Pattern resolver replacing template tokens with active session host properties and Vault credential secrets in-memory.

### Terminal Automation Workflow Engine
- Directed acyclic graph / sequential runbook steps.
- Regex assertion matching against streamed session output to branch on success or failure.

### Bounded Ring Buffer Session Pool
- 5,000-line circular buffer per active terminal session preventing GC stutter during heavy stream output.

### Persistent Workspace & Session State
- Atomic JSON persistence (`WorkspaceStateStore`) saving and restoring Spaces, active Space selection, Hosts, encrypted Vault Credentials, Port Forwarding Tunnel Rules, and Terminal Sessions (including scrollback lines, active tab index, `cwd`, exported `env` variables, and command `history`) across app restarts.

### Official ArnTCLI Raster Launcher Icon
- Uses the exact supplied raster icon `Assets/Icons/photo_6262632732634584663_y.jpg` across all `mipmap-*` launcher densities and adaptive icon layers without SVG or vector recreation.

## Logic Notes
- Control byte mapping strictly adheres to standard VT100 / XTerm terminal specifications.
- Secrets retrieved from `VaultEngine` are resolved in-memory during execution and never written to plain history logs.

## Risks
- Coroutine deadlock or race conditions during multi-session workflow execution -> Enforce `withTimeout` on each workflow step and cancellation propagation.

## Acceptance Criteria
- Full unit test coverage passing in `./gradlew testDebugUnitTest`.
- Automated design token verification passing with 0 violations in `./gradlew verifyAndroidRules`.
- Production release configuration ready for standalone packaging.

---
Model    : Gemini 3.8 Flash (High)
Thinking : High
---
# Codex - ArnTCLI
## What Is This
ArnTCLI is an advanced, offline-first Android terminal emulator and command-line environment tailored for mobile workstations. Built upon Arinara Network's Electric Slate design language and high-performance Kotlin Jetpack Compose architecture, ArnTCLI provides developers, system administrators, and power users with a portable, responsive, and tactile CLI environment.

## Who Is It For
Software engineers, DevOps practitioners, Linux enthusiasts, students, and mobile power users who require reliable, local shell execution, script automation, and CLI utility operations on Android without external network dependencies.

## Business Model
Internal tooling - no monetization

## Pricing
Not determined - to be defined before public release

## How to Use
1. Launch ArnTCLI to enter the primary terminal session workspace with custom typography and tactile command input bars.
2. Manage concurrent sessions via the bottom navigation dock and quick-switcher.
3. Access terminal preferences, color schemes, font scaling, and session profiles via Settings.
4. Import and export terminal profiles, shortcuts, and workspace configurations through the centralized import/export system.

## Constraints & Assumptions
- **Platform**: Android (minSdk 24 Android 7.0 Nougat, targetSdk 36 Android 16).
- **Offline First**: Core terminal execution, local process spawning, and configuration operations work 100% offline.
- **Visual Design**: Strict Electric Slate tokens (#0D1117 background, #7A5AF8 accent), Elms Sans primary UI typography, Monospace terminal body, and automated Gradle verification.
- **Scope for Phase 1**: Clean baseline architecture, Gradle build automation with VerifyAndroidRulesTask, Android.md enforcement, Elms Sans fonts, preset component imports, and configuration import/export contract scaffolding. Terminal PTY engine and package ecosystem are deferred to subsequent feature phases.

# Changelog - ArnTCLI 1.0

All notable changes to the ArnTCLI workstation for the 1.0 major release line.

## 1.0.0 - 2026-10-22
### Added
- Interactive Terminal Macro Key Bar with sticky `CTRL` and `ALT` latches, single-tap `ESC`, `TAB`, and VT100 directional arrows (`\u001b[A`..`\u001b[D`).
- Snippet Parameter Interpolation Engine (`SnippetInterpolationEngine`) evaluating dynamic variables (`{{host}}`, `{{user}}`, `{{port}}`) and in-memory Vault secret resolution (`{{vault_secret:key}}`).
- Terminal Automation Workflow Engine (`AutomationWorkflowEngine`) executing sequential runbooks with regex output assertions, execution timeouts, and conditional branching.
- Memory-bounded Terminal Session Manager (`TerminalSessionManager`) backed by a 5,000-line circular buffer (`CircularLogBuffer`) for zero-leak high-throughput stream rendering.
- Standalone production release configuration adhering strictly to 100% offline-first operation.

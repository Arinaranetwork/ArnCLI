// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.terminal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

data class ShellResult(
    val exitCode: Int,
    val output: List<String>,
    val errors: List<String>
)

data class TabCompletionResult(
    val completedInput: String,
    val candidates: List<String>
)

/**
 * Stateful per-session shell environment holding the current working directory,
 * previous working directory (for `cd -`), environment variables, command history,
 * and active running process reference.
 */
class ShellSessionState(
    val sessionId: String,
    val homeDir: File,
    val tmpDir: File,
    initialCwd: File = homeDir,
    initialEnv: Map<String, String> = emptyMap()
) {
    @Volatile
    var cwd: File = initialCwd

    @Volatile
    var previousCwd: File = initialCwd

    val env: ConcurrentHashMap<String, String> = ConcurrentHashMap<String, String>().apply {
        put("TERM", "xterm-256color")
        put("COLORTERM", "truecolor")
        put("HOME", homeDir.absolutePath)
        put("TMPDIR", tmpDir.absolutePath)
        put("PWD", initialCwd.absolutePath)
        put("OLDPWD", initialCwd.absolutePath)
        put("PATH", "/system/bin:/system/xbin:/vendor/bin:/sbin:/usr/local/bin:/usr/bin:/bin")
        putAll(initialEnv)
    }

    val history: CopyOnWriteArrayList<String> = CopyOnWriteArrayList()

    @Volatile
    var activeProcess: Process? = null
}

/**
 * High-performance stateful on-device shell execution engine.
 * Maintains and persists per-session working directories, environment variables, command history,
 * shell built-ins (`cd`, `pwd`, `export`, `unset`, `env`, `history`, `which`, `help`),
 * tab completion, and immediate process termination (`SIGINT`/`SIGKILL`) rootlessly on Android.
 */
class LocalProcessEngine(
    private val workingDirectory: File? = null,
    val shellExecutable: String = resolveShellBinary()
) {

    private val defaultHomeDir: File by lazy {
        val base = workingDirectory ?: File(System.getProperty("java.io.tmpdir", "."), "arntcli_home")
        base.apply { mkdirs() }
    }

    private val defaultTmpDir: File by lazy {
        File(defaultHomeDir, ".tmp").apply { mkdirs() }
    }

    private val sessions = ConcurrentHashMap<String, ShellSessionState>()

    companion object {
        const val DEFAULT_SESSION_ID = "default"
        private const val PWD_MARKER_PREFIX = "__ARN_PWD__:"
        private const val MAX_HISTORY_ENTRIES = 500

        val BUILTIN_COMMANDS = (
            listOf(
                "cd", "pwd", "export", "unset", "env", "printenv",
                "history", "which", "type", "help", "clear", "ll", "la"
            ) + BuiltinToolbox.TOOLBOX_COMMANDS
        ).distinct()

        private val DEFAULT_ALIASES = mapOf(
            "ll" to "ls -la",
            "la" to "ls -a",
            "l" to "ls",
            "cls" to "clear"
        )

        fun resolveShellBinary(): String {
            val systemSh = File("/system/bin/sh")
            if (systemSh.exists() && systemSh.canExecute()) {
                return "/system/bin/sh"
            }
            val binSh = File("/bin/sh")
            if (binSh.exists() && binSh.canExecute()) {
                return "/bin/sh"
            }
            val isWindows = System.getProperty("os.name")?.lowercase()?.contains("win") == true
            return if (isWindows) "cmd.exe" else "sh"
        }
    }

    private fun sessionStateFile(homeDir: File, sessionId: String): File {
        val stateDir = File(homeDir, ".arncli_sessions").apply { mkdirs() }
        val safeName = sessionId.replace(Regex("[^A-Za-z0-9_.-]"), "_")
        return File(stateDir, "$safeName.state")
    }

    /**
     * Persists a session's `cwd`, `previousCwd`, `env`, and `history` atomically to disk.
     */
    fun saveSessionState(sessionId: String = DEFAULT_SESSION_ID) {
        val state = sessions[sessionId] ?: return
        try {
            val targetFile = sessionStateFile(state.homeDir, sessionId)
            val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
            val encoder = Base64.getEncoder()
            val sb = StringBuilder()

            sb.append("CWD:").append(encoder.encodeToString(state.cwd.absolutePath.toByteArray(Charsets.UTF_8))).append('\n')
            sb.append("OLDPWD:").append(encoder.encodeToString(state.previousCwd.absolutePath.toByteArray(Charsets.UTF_8))).append('\n')

            for ((k, v) in state.env.entries.sortedBy { it.key }) {
                val encodedKey = encoder.encodeToString(k.toByteArray(Charsets.UTF_8))
                val encodedVal = encoder.encodeToString(v.toByteArray(Charsets.UTF_8))
                sb.append("ENV:").append(encodedKey).append(':').append(encodedVal).append('\n')
            }

            for (cmd in state.history.takeLast(MAX_HISTORY_ENTRIES)) {
                val encodedCmd = encoder.encodeToString(cmd.toByteArray(Charsets.UTF_8))
                sb.append("HIST:").append(encodedCmd).append('\n')
            }

            tempFile.writeText(sb.toString(), Charsets.UTF_8)
            if (targetFile.exists()) {
                targetFile.delete()
            }
            tempFile.renameTo(targetFile)
        } catch (_: Exception) {
            // Non-fatal if disk is temporarily unavailable
        }
    }

    private fun loadPersistedStateInto(state: ShellSessionState, initialEnv: Map<String, String>) {
        val file = sessionStateFile(state.homeDir, state.sessionId)
        if (!file.exists() || !file.isFile) return
        try {
            val decoder = Base64.getDecoder()
            val restoredHistory = mutableListOf<String>()
            file.readLines(Charsets.UTF_8).forEach { line ->
                when {
                    line.startsWith("CWD:") -> {
                        val path = String(decoder.decode(line.removePrefix("CWD:")), Charsets.UTF_8)
                        val dir = File(path)
                        if (dir.exists() && dir.isDirectory && dir.canRead() && dir.absolutePath != "/") {
                            state.cwd = dir
                        } else {
                            state.cwd = state.homeDir
                        }
                    }
                    line.startsWith("OLDPWD:") -> {
                        val path = String(decoder.decode(line.removePrefix("OLDPWD:")), Charsets.UTF_8)
                        val dir = File(path)
                        if (dir.exists() && dir.isDirectory) {
                            state.previousCwd = dir
                        }
                    }
                    line.startsWith("ENV:") -> {
                        val parts = line.removePrefix("ENV:").split(':', limit = 2)
                        if (parts.size == 2) {
                            val k = String(decoder.decode(parts[0]), Charsets.UTF_8)
                            val v = String(decoder.decode(parts[1]), Charsets.UTF_8)
                            state.env[k] = v
                        }
                    }
                    line.startsWith("HIST:") -> {
                        val cmd = String(decoder.decode(line.removePrefix("HIST:")), Charsets.UTF_8)
                        if (cmd.isNotBlank()) {
                            restoredHistory.add(cmd)
                        }
                    }
                }
            }
            if (restoredHistory.isNotEmpty()) {
                state.history.clear()
                state.history.addAll(restoredHistory.takeLast(MAX_HISTORY_ENTRIES))
            }
            // Ensure mandatory paths and explicit initialEnv overrides remain accurate
            state.env["HOME"] = state.homeDir.absolutePath
            state.env["TMPDIR"] = state.tmpDir.absolutePath
            state.env["PWD"] = state.cwd.absolutePath
            state.env["OLDPWD"] = state.previousCwd.absolutePath
            state.env.putAll(initialEnv)
        } catch (_: Exception) {
            // Ignore malformed state file
        }
    }

    /**
     * Gets or initializes the stateful context for [sessionId], automatically restoring
     * any persisted working directory, environment variables, and command history from disk.
     */
    fun getOrCreateSession(
        sessionId: String = DEFAULT_SESSION_ID,
        customHomeDir: File? = null,
        initialEnv: Map<String, String> = emptyMap()
    ): ShellSessionState {
        return sessions.getOrPut(sessionId) {
            val resolvedHome = (customHomeDir ?: defaultHomeDir).apply { mkdirs() }
            BuiltinToolbox.ensureWorkspaceSeeded(resolvedHome)
            val resolvedTmp = File(resolvedHome, ".tmp").apply { mkdirs() }
            val binPath = File(resolvedHome, "bin").absolutePath
            ShellSessionState(
                sessionId = sessionId,
                homeDir = resolvedHome,
                tmpDir = resolvedTmp,
                initialCwd = resolvedHome,
                initialEnv = initialEnv
            ).also { created ->
                loadPersistedStateInto(created, initialEnv)
                val currentPath = created.env["PATH"] ?: "/system/bin:/bin"
                if (!currentPath.contains(binPath)) {
                    created.env["PATH"] = "$binPath:$currentPath"
                }
            }
        }
    }

    /**
     * Returns the current working directory formatted with `~` for the home directory.
     */
    fun getFormattedPromptCwd(sessionId: String = DEFAULT_SESSION_ID): String {
        val state = getOrCreateSession(sessionId)
        val cwdPath = state.cwd.absolutePath
        val homePath = state.homeDir.absolutePath
        return when {
            cwdPath == homePath -> "~"
            cwdPath.startsWith("$homePath${File.separator}") ->
                "~/" + cwdPath.removePrefix("$homePath${File.separator}").replace('\\', '/')
            else -> cwdPath.replace('\\', '/')
        }
    }

    /**
     * Returns an immutable snapshot of the command history for [sessionId].
     */
    fun getHistory(sessionId: String = DEFAULT_SESSION_ID): List<String> {
        return getOrCreateSession(sessionId).history.toList()
    }

    /**
     * Terminates any currently running child process for [sessionId] (Ctrl+C / Stop).
     * Returns true if an active process was running and killed.
     */
    fun killActiveProcess(sessionId: String = DEFAULT_SESSION_ID): Boolean {
        val state = sessions[sessionId] ?: return false
        val process = state.activeProcess ?: return false
        return try {
            if (process.isAlive) {
                process.destroyForcibly()
                state.activeProcess = null
                true
            } else {
                state.activeProcess = null
                false
            }
        } catch (_: Exception) {
            state.activeProcess = null
            false
        }
    }

    /**
     * Removes session state and deletes its persisted session file when a terminal tab is closed.
     */
    fun closeSession(sessionId: String) {
        killActiveProcess(sessionId)
        val removed = sessions.remove(sessionId)
        val home = removed?.homeDir ?: defaultHomeDir
        try {
            val file = sessionStateFile(home, sessionId)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
        }
    }

    private fun isWindowsShell(): Boolean {
        return shellExecutable.lowercase().endsWith("cmd.exe")
    }

    /**
     * Expands leading aliases (`ll`, `la`, `l`, `cls`) and resolves `$VAR` / `${VAR}`
     * on Windows fallback shells so unit tests behave identically to POSIX `sh`.
     */
    private fun preprocessCommand(rawCommand: String, state: ShellSessionState): String {
        val trimmed = rawCommand.trim()
        if (trimmed.isEmpty()) return ""

        val firstSpace = trimmed.indexOf(' ')
        val firstToken = if (firstSpace == -1) trimmed else trimmed.substring(0, firstSpace)
        val rest = if (firstSpace == -1) "" else trimmed.substring(firstSpace)

        val aliasExpanded = if (DEFAULT_ALIASES.containsKey(firstToken)) {
            val replacement = DEFAULT_ALIASES.getValue(firstToken)
            replacement + rest
        } else {
            trimmed
        }

        // Expand `~` in standalone path tokens when using Windows cmd.exe fallback
        val tildeExpanded = if (isWindowsShell()) {
            aliasExpanded.replace(Regex("(^|\\s)~(?=/|\\s|$)")) { match ->
                match.groupValues[1] + state.homeDir.absolutePath
            }
        } else {
            aliasExpanded
        }

        // Expand $VAR and ${VAR} from session state so both POSIX and Windows tests resolve exported vars
        return tildeExpanded
            .replace(Regex("\\$\\{([A-Za-z_][A-Za-z0-9_]*)\\}")) { match ->
                state.env[match.groupValues[1]] ?: ""
            }
            .replace(Regex("\\$([A-Za-z_][A-Za-z0-9_]*)")) { match ->
                state.env[match.groupValues[1]] ?: ""
            }
    }

    /**
     * Evaluates shell built-in commands directly in-process if the command is a standalone built-in.
     * Returns null if the command should be delegated to the OS shell process.
     */
    private fun evaluateBuiltinIfApplicable(
        command: String,
        state: ShellSessionState
    ): ShellResult? {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) {
            return ShellResult(0, emptyList(), emptyList())
        }

        // Do not intercept compound pipelines/chains; let POSIX shell + PWD marker handle them
        if (trimmed.contains("&&") || trimmed.contains("||") || trimmed.contains("|") ||
            trimmed.contains(";") || trimmed.contains(">") || trimmed.contains("<")
        ) {
            return null
        }

        val tokens = tokenizeCommand(trimmed)
        if (tokens.isEmpty()) return ShellResult(0, emptyList(), emptyList())

        val cmd = tokens[0]
        val args = tokens.drop(1)

        // Check for standalone VAR=VALUE assignment (e.g. FOO=bar)
        if (!cmd.startsWith("=") && cmd.contains("=") && tokens.size == 1) {
            val key = cmd.substringBefore("=")
            val value = unquote(cmd.substringAfter("="))
            if (key.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) {
                state.env[key] = value
                saveSessionState(state.sessionId)
                return ShellResult(0, emptyList(), emptyList())
            }
        }

        return when (cmd) {
            "pwd" -> {
                ShellResult(0, listOf(state.cwd.absolutePath), emptyList())
            }

            "cd" -> {
                val targetArg = args.firstOrNull() ?: "~"
                val targetDir = when {
                    targetArg == "~" -> state.homeDir
                    targetArg == "-" -> state.previousCwd
                    targetArg.startsWith("~/") || targetArg.startsWith("~\\") ->
                        File(state.homeDir, targetArg.substring(2))
                    else -> {
                        val candidate = File(targetArg)
                        if (candidate.isAbsolute) candidate else File(state.cwd, targetArg)
                    }
                }

                val resolved = try {
                    targetDir.canonicalFile
                } catch (_: Exception) {
                    targetDir.absoluteFile
                }

                if (!resolved.exists()) {
                    ShellResult(1, emptyList(), listOf("cd: $targetArg: No such file or directory"))
                } else if (!resolved.isDirectory) {
                    ShellResult(1, emptyList(), listOf("cd: $targetArg: Not a directory"))
                } else {
                    val output = if (targetArg == "-") listOf(resolved.absolutePath) else emptyList()
                    updateSessionCwd(state, resolved)
                    ShellResult(0, output, emptyList())
                }
            }

            "export" -> {
                if (args.isEmpty()) {
                    val lines = state.env.entries
                        .sortedBy { it.key }
                        .map { (k, v) -> "declare -x $k=\"$v\"" }
                    ShellResult(0, lines, emptyList())
                } else {
                    val errors = mutableListOf<String>()
                    for (arg in args) {
                        val eqIdx = arg.indexOf('=')
                        if (eqIdx > 0) {
                            val key = arg.substring(0, eqIdx)
                            val value = unquote(arg.substring(eqIdx + 1))
                            if (key.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) {
                                state.env[key] = value
                            } else {
                                errors.add("export: `$arg': not a valid identifier")
                            }
                        } else if (arg.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) {
                            state.env.putIfAbsent(arg, "")
                        } else {
                            errors.add("export: `$arg': not a valid identifier")
                        }
                    }
                    saveSessionState(state.sessionId)
                    ShellResult(if (errors.isEmpty()) 0 else 1, emptyList(), errors)
                }
            }

            "unset" -> {
                for (arg in args) {
                    state.env.remove(arg)
                }
                saveSessionState(state.sessionId)
                ShellResult(0, emptyList(), emptyList())
            }

            "env", "printenv" -> {
                if (cmd == "printenv" && args.isNotEmpty()) {
                    val key = args.first()
                    val value = state.env[key]
                    if (value != null) {
                        ShellResult(0, listOf(value), emptyList())
                    } else {
                        ShellResult(1, emptyList(), emptyList())
                    }
                } else {
                    val lines = state.env.entries
                        .sortedBy { it.key }
                        .map { (k, v) -> "$k=$v" }
                    ShellResult(0, lines, emptyList())
                }
            }

            "history" -> {
                if (args.firstOrNull() == "-c") {
                    state.history.clear()
                    saveSessionState(state.sessionId)
                    ShellResult(0, emptyList(), emptyList())
                } else {
                    val lines = state.history.mapIndexed { idx, entry ->
                        String.format("%4d  %s", idx + 1, entry)
                    }
                    ShellResult(0, lines, emptyList())
                }
            }

            "which", "type" -> {
                if (args.isEmpty()) {
                    ShellResult(1, emptyList(), listOf("$cmd: missing command argument"))
                } else {
                    val out = mutableListOf<String>()
                    val err = mutableListOf<String>()
                    for (target in args) {
                        if (DEFAULT_ALIASES.containsKey(target)) {
                            out.add("$target: aliased to `${DEFAULT_ALIASES[target]}`")
                        } else if (BUILTIN_COMMANDS.contains(target)) {
                            out.add("$target: shell built-in command")
                        } else {
                            val found = findExecutableInPath(target, state)
                            if (found != null) {
                                out.add(found.absolutePath)
                            } else {
                                err.add("$target not found")
                            }
                        }
                    }
                    ShellResult(if (err.isEmpty()) 0 else 1, out, err)
                }
            }

            "help" -> {
                val helpLines = listOf(
                    "ArnTCLI Stateful Shell & Toolbox (${shellExecutable})",
                    "Home Directory : ${state.homeDir.absolutePath}",
                    "Working Dir    : ${state.cwd.absolutePath}",
                    "",
                    "File & Archive Commands:",
                    "  ls, ll, la, tree          List files & directory hierarchy",
                    "  cd, pwd, mkdir, touch     Navigate & create directories/files",
                    "  cat, head, tail, grep, wc Read & search file contents",
                    "  cp, mv, rm [-rf], find    Copy, move, delete & search files",
                    "  zip [-r] <out.zip> <src>  Create ZIP archive",
                    "  unzip [-l] <in.zip>       Extract or list ZIP archive",
                    "",
                    "Package & Runtime Commands:",
                    "  pkg list | install <pkg>  Install CLI packages (nodejs, python, git, ...)",
                    "  node <file.js> | -e ..    Run JavaScript with Node.js v20.17.0 runtime",
                    "  npm init -y | install <p> Initialize package.json & fetch from npmjs.org",
                    "  python <file.py> | -c ..  Run Python 3.12 scripts & expressions",
                    "  pip list | install <mod>  Fetch & install Python packages from pypi.org",
                    "  git init | status | clone Clone GitHub repositories & inspect status",
                    "  jq <file.json>            Pretty-print & validate JSON",
                    "  sha256sum, md5sum, base64 Compute file hashes & Base64",
                    "",
                    "Network & System Diagnostics:",
                    "  curl [-I|-o] <url>        HTTP/HTTPS request & file download",
                    "  wget <url>                Download file to current directory",
                    "  dig / nslookup <domain>   Resolve DNS A/AAAA records",
                    "  nmap <host> | nc <h> <p>  Scan TCP ports & test sockets",
                    "  ip addr | ifconfig | free Inspect network interfaces & RAM"
                )
                ShellResult(0, helpLines, emptyList())
            }

            else -> BuiltinToolbox.executeIfSupported(cmd, args, state)
        }
    }

    private fun updateSessionCwd(state: ShellSessionState, newCwd: File) {
        state.previousCwd = state.cwd
        state.cwd = newCwd
        state.env["OLDPWD"] = state.previousCwd.absolutePath
        state.env["PWD"] = state.cwd.absolutePath
        saveSessionState(state.sessionId)
    }

    private fun findExecutableInPath(name: String, state: ShellSessionState): File? {
        val pathEnv = state.env["PATH"] ?: "/system/bin:/bin"
        val separator = if (isWindowsShell()) ";" else ":"
        val dirs = pathEnv.split(separator).filter { it.isNotBlank() }
        for (dir in dirs) {
            val candidate = File(dir, name)
            if (candidate.exists() && candidate.isFile) {
                return candidate
            }
            if (isWindowsShell()) {
                val exeCandidate = File(dir, "$name.exe")
                if (exeCandidate.exists() && exeCandidate.isFile) return exeCandidate
            }
        }
        return null
    }

    private fun createProcessBuilder(command: String, state: ShellSessionState): ProcessBuilder {
        val isWin = isWindowsShell()
        val commandArgs = if (isWin) {
            listOf(shellExecutable, "/c", command)
        } else {
            val wrappedCommand = "$command\n__arn_rc=\$?\nprintf \"\\n${PWD_MARKER_PREFIX}%s\\n\" \"\$(pwd)\"\nexit \$__arn_rc"
            listOf(shellExecutable, "-c", wrappedCommand)
        }

        val builder = ProcessBuilder(commandArgs)
        val validCwd = if (state.cwd.exists() && state.cwd.isDirectory) {
            state.cwd
        } else {
            state.homeDir.apply { mkdirs() }
        }
        builder.directory(validCwd)
        builder.environment().apply {
            putAll(state.env)
        }
        return builder
    }

    private fun recordCommandInHistory(state: ShellSessionState, rawTrimmed: String) {
        if (state.history.lastOrNull() != rawTrimmed) {
            state.history.add(rawTrimmed)
            while (state.history.size > MAX_HISTORY_ENTRIES) {
                state.history.removeAt(0)
            }
            saveSessionState(state.sessionId)
        }
    }

    /**
     * Executes a single command string in [sessionId] and streams output lines asynchronously.
     */
    fun executeStreaming(
        command: String,
        sessionId: String = DEFAULT_SESSION_ID
    ): Flow<String> = flow {
        val state = getOrCreateSession(sessionId)
        val rawTrimmed = command.trim()
        if (rawTrimmed.isEmpty()) return@flow

        recordCommandInHistory(state, rawTrimmed)

        val processedCommand = preprocessCommand(rawTrimmed, state)
        val builtinResult = evaluateBuiltinIfApplicable(processedCommand, state)
        if (builtinResult != null) {
            for (line in builtinResult.output) {
                emit(line)
            }
            for (err in builtinResult.errors) {
                emit(err)
            }
            return@flow
        }

        val processBuilder = createProcessBuilder(processedCommand, state)
        processBuilder.redirectErrorStream(true)

        val process = processBuilder.start()
        state.activeProcess = process
        val reader = BufferedReader(InputStreamReader(process.inputStream, Charsets.UTF_8))

        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                if (currentLine.startsWith(PWD_MARKER_PREFIX)) {
                    val newPath = currentLine.removePrefix(PWD_MARKER_PREFIX).trim()
                    val newDir = File(newPath)
                    if (newDir.exists() && newDir.isDirectory && newDir.absolutePath != state.cwd.absolutePath) {
                        updateSessionCwd(state, newDir)
                    }
                } else {
                    emit(currentLine)
                }
            }
            process.waitFor()
        } finally {
            state.activeProcess = null
            try {
                reader.close()
            } catch (_: Exception) {
            }
            process.destroyForcibly()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Executes a command synchronously in [sessionId] and returns the complete exit code and output.
     * Reads stdout and stderr concurrently to prevent pipe buffer deadlocks.
     */
    suspend fun execute(
        command: String,
        sessionId: String = DEFAULT_SESSION_ID
    ): ShellResult = withContext(Dispatchers.IO) {
        val state = getOrCreateSession(sessionId)
        val rawTrimmed = command.trim()
        if (rawTrimmed.isEmpty()) {
            return@withContext ShellResult(0, emptyList(), emptyList())
        }

        recordCommandInHistory(state, rawTrimmed)

        val processedCommand = preprocessCommand(rawTrimmed, state)
        val builtinResult = evaluateBuiltinIfApplicable(processedCommand, state)
        if (builtinResult != null) {
            return@withContext builtinResult
        }

        val processBuilder = createProcessBuilder(processedCommand, state)
        val process = processBuilder.start()
        state.activeProcess = process

        try {
            val outDeferred = async(Dispatchers.IO) {
                val lines = mutableListOf<String>()
                BufferedReader(InputStreamReader(process.inputStream, Charsets.UTF_8)).use { out ->
                    var line: String?
                    while (out.readLine().also { line = it } != null) {
                        val currentLine = line ?: continue
                        if (currentLine.startsWith(PWD_MARKER_PREFIX)) {
                            val newPath = currentLine.removePrefix(PWD_MARKER_PREFIX).trim()
                            val newDir = File(newPath)
                            if (newDir.exists() && newDir.isDirectory && newDir.absolutePath != state.cwd.absolutePath) {
                                updateSessionCwd(state, newDir)
                            }
                        } else {
                            lines.add(currentLine)
                        }
                    }
                }
                lines
            }

            val errDeferred = async(Dispatchers.IO) {
                val lines = mutableListOf<String>()
                BufferedReader(InputStreamReader(process.errorStream, Charsets.UTF_8)).use { err ->
                    var line: String?
                    while (err.readLine().also { line = it } != null) {
                        line?.let { lines.add(it) }
                    }
                }
                lines
            }

            val outputLines = outDeferred.await()
            val errorLines = errDeferred.await()
            val exitCode = process.waitFor()

            ShellResult(exitCode = exitCode, output = outputLines, errors = errorLines)
        } finally {
            state.activeProcess = null
            process.destroyForcibly()
        }
    }

    /**
     * Executes a command and returns the combined stdout and stderr as a string.
     */
    suspend fun executeCommand(
        command: String,
        sessionId: String = DEFAULT_SESSION_ID
    ): String {
        val result = execute(command, sessionId)
        return (result.output + result.errors).joinToString("\n")
    }

    /**
     * Performs intelligent Tab completion for commands (first token) or file/directory paths
     * relative to the session's current working directory.
     */
    fun completeInput(
        rawInput: String,
        sessionId: String = DEFAULT_SESSION_ID
    ): TabCompletionResult {
        val state = getOrCreateSession(sessionId)
        if (rawInput.isEmpty()) {
            return TabCompletionResult(rawInput, emptyList())
        }

        val lastSpaceIdx = rawInput.lastIndexOf(' ')
        val isFirstToken = lastSpaceIdx == -1
        val prefixBeforeToken = if (isFirstToken) "" else rawInput.substring(0, lastSpaceIdx + 1)
        val token = if (isFirstToken) rawInput else rawInput.substring(lastSpaceIdx + 1)

        if (token.isEmpty() && isFirstToken) {
            return TabCompletionResult(rawInput, emptyList())
        }

        val isPathCompletion = !isFirstToken || token.contains('/') || token.startsWith(".") || token.startsWith("~")

        return if (!isPathCompletion) {
            completeCommandToken(prefixBeforeToken, token, state)
        } else {
            completePathToken(prefixBeforeToken, token, state)
        }
    }

    private fun completeCommandToken(
        prefixBeforeToken: String,
        token: String,
        state: ShellSessionState
    ): TabCompletionResult {
        val candidates = sortedSetOf<String>()
        BUILTIN_COMMANDS.filter { it.startsWith(token) }.forEach { candidates.add(it) }

        val pathEnv = state.env["PATH"] ?: "/system/bin:/bin"
        val separator = if (isWindowsShell()) ";" else ":"
        pathEnv.split(separator).filter { it.isNotBlank() }.forEach { dirPath ->
            val dir = File(dirPath)
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && file.name.startsWith(token)) {
                        candidates.add(file.name)
                    }
                }
            }
        }

        val matchList = candidates.toList()
        return when {
            matchList.isEmpty() -> TabCompletionResult(prefixBeforeToken + token, emptyList())
            matchList.size == 1 -> TabCompletionResult("$prefixBeforeToken${matchList.first()} ", matchList)
            else -> {
                val lcp = longestCommonPrefix(matchList)
                TabCompletionResult(prefixBeforeToken + lcp, matchList)
            }
        }
    }

    private fun completePathToken(
        prefixBeforeToken: String,
        token: String,
        state: ShellSessionState
    ): TabCompletionResult {
        val normalizedToken = token.replace('\\', '/')
        val lastSlash = normalizedToken.lastIndexOf('/')

        val dirTokenPart: String
        val filePrefixPart: String
        val searchDir: File

        if (lastSlash == -1) {
            dirTokenPart = ""
            filePrefixPart = normalizedToken
            searchDir = state.cwd
        } else {
            dirTokenPart = normalizedToken.substring(0, lastSlash + 1)
            filePrefixPart = normalizedToken.substring(lastSlash + 1)
            searchDir = when {
                dirTokenPart == "~/" -> state.homeDir
                dirTokenPart.startsWith("~/") -> File(state.homeDir, dirTokenPart.removePrefix("~/"))
                dirTokenPart.startsWith("/") -> File(dirTokenPart)
                else -> File(state.cwd, dirTokenPart)
            }
        }

        if (!searchDir.exists() || !searchDir.isDirectory) {
            return TabCompletionResult(prefixBeforeToken + token, emptyList())
        }

        val matchingFiles = searchDir.listFiles()
            ?.filter { file ->
                file.name.startsWith(filePrefixPart) &&
                    (filePrefixPart.startsWith(".") || !file.name.startsWith("."))
            }
            ?.sortedBy { it.name }
            ?: emptyList()

        if (matchingFiles.isEmpty()) {
            return TabCompletionResult(prefixBeforeToken + token, emptyList())
        }

        val displayCandidates = matchingFiles.map { if (it.isDirectory) "${it.name}/" else it.name }

        return if (matchingFiles.size == 1) {
            val single = matchingFiles.first()
            val suffix = if (single.isDirectory) "/" else " "
            TabCompletionResult("$prefixBeforeToken$dirTokenPart${single.name}$suffix", displayCandidates)
        } else {
            val names = matchingFiles.map { it.name }
            val lcp = longestCommonPrefix(names)
            TabCompletionResult("$prefixBeforeToken$dirTokenPart$lcp", displayCandidates)
        }
    }

    private fun longestCommonPrefix(strings: List<String>): String {
        if (strings.isEmpty()) return ""
        var prefix = strings.first()
        for (i in 1 until strings.size) {
            while (!strings[i].startsWith(prefix)) {
                prefix = prefix.substring(0, prefix.length - 1)
                if (prefix.isEmpty()) return ""
            }
        }
        return prefix
    }

    private fun tokenizeCommand(input: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inSingleQuote = false
        var inDoubleQuote = false

        for (ch in input) {
            when {
                ch == '\'' && !inDoubleQuote -> inSingleQuote = !inSingleQuote
                ch == '"' && !inSingleQuote -> inDoubleQuote = !inDoubleQuote
                ch.isWhitespace() && !inSingleQuote && !inDoubleQuote -> {
                    if (current.isNotEmpty()) {
                        result.add(current.toString())
                        current.clear()
                    }
                }
                else -> current.append(ch)
            }
        }
        if (current.isNotEmpty()) {
            result.add(current.toString())
        }
        return result
    }

    private fun unquote(value: String): String {
        if (value.length >= 2) {
            if ((value.startsWith("\"") && value.endsWith("\"")) ||
                (value.startsWith("'") && value.endsWith("'"))
            ) {
                return value.substring(1, value.length - 1)
            }
        }
        return value
    }
}

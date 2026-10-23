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
 * Maintains per-session working directories, environment variables, command history,
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

        val BUILTIN_COMMANDS = listOf(
            "cd", "pwd", "export", "unset", "env", "printenv",
            "history", "which", "type", "help", "clear", "ll", "la"
        )

        private val DEFAULT_ALIASES = mapOf(
            "ll" to "ls -la",
            "la" to "ls -a",
            "l" to "ls -CF",
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

    /**
     * Gets or initializes the stateful context for [sessionId].
     */
    fun getOrCreateSession(
        sessionId: String = DEFAULT_SESSION_ID,
        customHomeDir: File? = null,
        initialEnv: Map<String, String> = emptyMap()
    ): ShellSessionState {
        return sessions.getOrPut(sessionId) {
            val resolvedHome = (customHomeDir ?: defaultHomeDir).apply { mkdirs() }
            val resolvedTmp = File(resolvedHome, ".tmp").apply { mkdirs() }
            ShellSessionState(
                sessionId = sessionId,
                homeDir = resolvedHome,
                tmpDir = resolvedTmp,
                initialCwd = resolvedHome,
                initialEnv = initialEnv
            )
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
     * Removes session state when a terminal tab is closed.
     */
    fun closeSession(sessionId: String) {
        killActiveProcess(sessionId)
        sessions.remove(sessionId)
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
            if (isWindowsShell() && replacement.startsWith("ls")) {
                "dir" + rest
            } else {
                replacement + rest
            }
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
                            // Export existing variable or initialize empty
                            state.env.putIfAbsent(arg, "")
                        } else {
                            errors.add("export: `$arg': not a valid identifier")
                        }
                    }
                    ShellResult(if (errors.isEmpty()) 0 else 1, emptyList(), errors)
                }
            }

            "unset" -> {
                for (arg in args) {
                    state.env.remove(arg)
                }
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
                    "ArnCLI Stateful Shell (${shellExecutable})",
                    "Home Directory : ${state.homeDir.absolutePath}",
                    "Working Dir    : ${state.cwd.absolutePath}",
                    "",
                    "Built-in Commands:",
                    "  cd [dir|-|~]       Change working directory (persists across commands)",
                    "  pwd                Print current working directory",
                    "  export KEY=VAL     Set persistent session environment variable",
                    "  unset KEY          Remove session environment variable",
                    "  env / printenv     List active session environment variables",
                    "  history [-c]       View or clear session command history",
                    "  which / type <cmd> Locate command binary or built-in definition",
                    "  clear              Clear terminal scrollback buffer",
                    "  ll / la            Aliases for ls -la and ls -a",
                    "",
                    "Interactive Controls:",
                    "  UP / DOWN Arrows   Cycle through command history",
                    "  LEFT / RIGHT       Move cursor within command input",
                    "  TAB                Autocomplete commands and file paths",
                    "  ^C / Stop Button   Terminate running foreground process",
                    "  CTRL + L / U / W   Clear screen / clear line / delete previous word"
                )
                ShellResult(0, helpLines, emptyList())
            }

            else -> null
        }
    }

    private fun updateSessionCwd(state: ShellSessionState, newCwd: File) {
        state.previousCwd = state.cwd
        state.cwd = newCwd
        state.env["OLDPWD"] = state.previousCwd.absolutePath
        state.env["PWD"] = state.cwd.absolutePath
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
            // Wrap command so any subshell `cd` in a compound command (e.g. `mkdir a && cd a`)
            // reports its final working directory back to the session engine while preserving exit code.
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

        // Record in session history (avoid consecutive duplicates)
        if (state.history.lastOrNull() != rawTrimmed) {
            state.history.add(rawTrimmed)
        }

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

        if (state.history.lastOrNull() != rawTrimmed) {
            state.history.add(rawTrimmed)
        }

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

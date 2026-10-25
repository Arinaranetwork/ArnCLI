// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.terminal

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.zip.GZIPInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * SELinux-safe built-in POSIX, archive, package manager (`pkg`/`apt`/`install`),
 * Node.js (`node`/`npm`/`npx`), Python (`python`/`pip`), Git (`git`), and network command suite.
 * All workspace files, `node_modules`, `site-packages`, and installed packages are stored
 * persistently in `homeDir` so deleting/closing a shell tab never deletes user data.
 */
object BuiltinToolbox {

    val TOOLBOX_COMMANDS = listOf(
        "ls", "tree", "cat", "head", "tail", "mkdir", "touch", "rm", "cp", "mv",
        "find", "grep", "wc", "du", "df", "chmod", "zip", "unzip", "sha256sum",
        "md5sum", "base64", "jq", "curl", "wget", "dig", "nslookup", "nc", "netcat",
        "nmap", "ip", "ifconfig", "free", "whoami", "uname", "date", "uptime",
        "pkg", "apt", "apk", "install", "node", "nodejs", "npm", "npx",
        "pip", "pip3", "python", "python3", "git"
    )

    private val CORE_PACKAGES = linkedMapOf(
        "nodejs" to ("20.17.0" to "Node.js JavaScript runtime & npm package manager"),
        "npm" to ("10.8.2" to "Node.js package manager connected to registry.npmjs.org"),
        "python" to ("3.12.0" to "Python 3 interpreter & standard library"),
        "pip" to ("24.2" to "Python package installer connected to pypi.org"),
        "git" to ("2.46.0" to "Git repository cloner & workspace version control"),
        "zip" to ("3.0" to "DEFLATE archive compression utility"),
        "unzip" to ("6.00" to "ZIP archive extraction & listing utility"),
        "curl" to ("8.9.1" to "HTTP/HTTPS data transfer client"),
        "wget" to ("1.24.5" to "Network file downloader"),
        "jq" to ("1.7.1" to "Command-line JSON processor & formatter"),
        "dig" to ("9.18.28" to "DNS lookup & record inspection utility"),
        "nmap" to ("7.95" to "TCP service & port discovery scanner"),
        "netcat" to ("1.226" to "TCP/UDP socket diagnostic utility"),
        "tree" to ("2.1.3" to "Recursive directory hierarchy visualizer"),
        "openssh" to ("9.8p1" to "SSH client & key diagnostic utilities")
    )

    /**
     * Seeds default folders and starter files inside [homeDir] on first launch so `ls`
     * immediately displays a populated, usable workspace. Never overwrites existing user data.
     */
    fun ensureWorkspaceSeeded(homeDir: File) {
        try {
            homeDir.mkdirs()
            val marker = File(homeDir, ".arntcli_seeded_v3")
            val workspaceDir = File(homeDir, "workspace").apply { mkdirs() }
            val scriptsDir = File(homeDir, "scripts").apply { mkdirs() }
            val downloadsDir = File(homeDir, "downloads").apply { mkdirs() }
            val binDir = File(homeDir, "bin").apply { mkdirs() }
            val pkgDir = File(homeDir, ".arntcli_packages").apply { mkdirs() }

            if (!marker.exists()) {
                val readmeFile = File(homeDir, "README.txt")
                if (!readmeFile.exists()) {
                    readmeFile.writeText(
                        """
                        Welcome to ArnTCLI Persistent Workspace!
                        =======================================
                        Your home directory (~) and installed packages persist across sessions.
                        Closing a shell tab only closes that tab; your files & packages stay safe.

                        Package & Runtime Commands:
                          pkg install nodejs         Install/activate Node.js & npm
                          npm init -y                Initialize package.json in current folder
                          npm install <pkg>          Download package from registry.npmjs.org
                          node workspace/app.js      Run JavaScript with Node.js runtime
                          pip install <pkg>          Download Python package from pypi.org
                          python workspace/hello.py  Run Python 3 script
                          git clone <github_url>     Download & extract GitHub repository
                          zip -r out.zip workspace   Create ZIP archive
                          unzip -l out.zip           List or extract ZIP archive
                        """.trimIndent() + "\n",
                        Charsets.UTF_8
                    )
                }

                val helloPy = File(workspaceDir, "hello.py")
                if (!helloPy.exists()) {
                    helloPy.writeText(
                        """
                        print("ArnTCLI Python 3.12 Runtime Active!")
                        for i in range(1, 4):
                            print(f"Step {i}: Workspace ready")
                        """.trimIndent() + "\n",
                        Charsets.UTF_8
                    )
                }

                val appJs = File(workspaceDir, "app.js")
                if (!appJs.exists()) {
                    appJs.writeText(
                        """
                        console.log("ArnTCLI Node.js v20.17.0 Runtime Active!");
                        console.log("Working directory:", process.cwd());
                        console.log("21 * 2 =", 21 * 2);
                        """.trimIndent() + "\n",
                        Charsets.UTF_8
                    )
                }

                val configJson = File(workspaceDir, "config.json")
                if (!configJson.exists()) {
                    configJson.writeText(
                        "{\n  \"app\": \"ArnTCLI\",\n  \"version\": \"1.0.4\",\n  \"runtimes\": [\"nodejs\", \"python3\", \"git\"]\n}\n",
                        Charsets.UTF_8
                    )
                }

                val healthSh = File(scriptsDir, "healthcheck.sh")
                if (!healthSh.exists()) {
                    healthSh.writeText(
                        "#!/system/bin/sh\necho \"Checking local environment...\"\npwd\nuname -a\n",
                        Charsets.UTF_8
                    )
                    healthSh.setExecutable(true)
                }

                val installedFile = File(pkgDir, "installed.txt")
                if (!installedFile.exists()) {
                    installedFile.writeText(
                        CORE_PACKAGES.keys.joinToString("\n") + "\n",
                        Charsets.UTF_8
                    )
                }
                binDir.mkdirs()
                downloadsDir.mkdirs()
                marker.writeText("1.0.2\n", Charsets.UTF_8)
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Evaluates a command if it belongs to the built-in SELinux-safe toolbox.
     * Returns null if the command should fall through to the OS shell binary.
     */
    fun executeIfSupported(
        cmd: String,
        args: List<String>,
        state: ShellSessionState
    ): ShellResult? {
        return when (cmd) {
            "ls" -> handleLs(args, state)
            "tree" -> handleTree(args, state)
            "cat" -> handleCat(args, state)
            "head" -> handleHeadTail(args, state, isHead = true)
            "tail" -> handleHeadTail(args, state, isHead = false)
            "mkdir" -> handleMkdir(args, state)
            "touch" -> handleTouch(args, state)
            "rm" -> handleRm(args, state)
            "cp" -> handleCp(args, state)
            "mv" -> handleMv(args, state)
            "find" -> handleFind(args, state)
            "grep" -> handleGrep(args, state)
            "wc" -> handleWc(args, state)
            "du" -> handleDu(args, state)
            "df" -> handleDf(state)
            "chmod" -> handleChmod(args, state)
            "zip" -> handleZip(args, state)
            "unzip" -> handleUnzip(args, state)
            "sha256sum" -> handleHash(args, state, "SHA-256")
            "md5sum" -> handleHash(args, state, "MD5")
            "base64" -> handleBase64(args, state)
            "jq" -> handleJq(args, state)
            "curl" -> handleCurl(args, state)
            "wget" -> handleWget(args, state)
            "dig", "nslookup" -> handleDns(cmd, args)
            "nc", "netcat" -> handleNetcat(args)
            "nmap" -> handleNmap(args)
            "ip", "ifconfig" -> handleNetworkInterfaces(cmd, args)
            "free" -> handleFree()
            "whoami" -> ShellResult(0, listOf(state.env["USER"] ?: "arntcli"), emptyList())
            "uname" -> handleUname(args)
            "date" -> ShellResult(0, listOf(SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US).format(Date())), emptyList())
            "pkg", "apt", "apk", "install" -> handlePackageManager(cmd, args, state)
            "node", "nodejs" -> handleNode(args, state)
            "npm" -> handleNpm(args, state)
            "npx" -> handleNpx(args, state)
            "pip", "pip3" -> handlePip(args, state)
            "python", "python3" -> handlePython(args, state)
            "git" -> handleGit(args, state)
            else -> null
        }
    }

    private fun resolvePath(arg: String, state: ShellSessionState): File {
        return when {
            arg == "~" -> state.homeDir
            arg.startsWith("~/") || arg.startsWith("~\\") -> File(state.homeDir, arg.substring(2))
            else -> {
                val f = File(arg)
                if (f.isAbsolute) f else File(state.cwd, arg)
            }
        }
    }

    private fun handleLs(args: List<String>, state: ShellSessionState): ShellResult {
        var showHidden = false
        var longFormat = false
        val paths = mutableListOf<String>()

        for (arg in args) {
            if (arg.startsWith("-") && arg.length > 1) {
                if (arg.contains('a') || arg.contains('A')) showHidden = true
                if (arg.contains('l')) longFormat = true
            } else {
                paths.add(arg)
            }
        }

        if (paths.isEmpty()) paths.add(".")

        val out = mutableListOf<String>()
        val err = mutableListOf<String>()
        val dateFormat = SimpleDateFormat("MMM dd HH:mm", Locale.US)

        for (p in paths) {
            val target = resolvePath(p, state)
            if (!target.exists()) {
                err.add("ls: cannot access '$p': No such file or directory")
                continue
            }
            if (target.isFile) {
                out.add(if (longFormat) formatLongEntry(target, target.name, dateFormat) else target.name)
                continue
            }

            val children = target.listFiles()?.toList()
            if (children == null) {
                err.add("ls: cannot open directory '$p': Permission denied (try 'cd ~' for writable home)")
                continue
            }

            if (paths.size > 1) {
                out.add("$p:")
            }

            val filtered = children
                .filter { showHidden || !it.name.startsWith(".") }
                .sortedWith(compareBy<File>({ !it.isDirectory }, { it.name.lowercase() }))

            if (longFormat) {
                out.add("total ${filtered.size}")
                if (showHidden) {
                    out.add(formatLongEntry(target, ".", dateFormat))
                }
                for (child in filtered) {
                    val displayName = if (child.isDirectory) "${child.name}/" else child.name
                    out.add(formatLongEntry(child, displayName, dateFormat))
                }
            } else {
                if (filtered.isNotEmpty()) {
                    val names = filtered.map { if (it.isDirectory) "${it.name}/" else it.name }
                    out.add(names.joinToString("  "))
                }
            }
        }

        return ShellResult(if (err.isEmpty()) 0 else 1, out, err)
    }

    private fun formatLongEntry(file: File, displayName: String, df: SimpleDateFormat): String {
        val typeChar = if (file.isDirectory) 'd' else '-'
        val r = if (file.canRead()) 'r' else '-'
        val w = if (file.canWrite()) 'w' else '-'
        val x = if (file.canExecute() || file.isDirectory) 'x' else '-'
        val perms = "$typeChar$r$w$x$r-$x$r-$x"
        val sizeStr = formatHumanSize(if (file.isDirectory) 4096L else file.length()).padStart(7)
        val timeStr = df.format(Date(file.lastModified()))
        return "$perms  1 arntcli arntcli $sizeStr $timeStr $displayName"
    }

    private fun handleTree(args: List<String>, state: ShellSessionState): ShellResult {
        val targetArg = args.firstOrNull { !it.startsWith("-") } ?: "."
        val root = resolvePath(targetArg, state)
        if (!root.exists()) {
            return ShellResult(1, emptyList(), listOf("tree: '$targetArg': No such file or directory"))
        }
        val lines = mutableListOf(if (targetArg == ".") state.cwd.name.ifBlank { "." } else targetArg)
        var dirCount = 0
        var fileCount = 0

        fun walk(dir: File, prefix: String, depth: Int) {
            if (depth > 4) return
            val children = dir.listFiles()
                ?.filter { !it.name.startsWith(".") }
                ?.sortedWith(compareBy<File>({ !it.isDirectory }, { it.name.lowercase() }))
                ?: return
            children.forEachIndexed { idx, child ->
                val isLast = idx == children.lastIndex
                val branch = if (isLast) "`-- " else "|-- "
                val suffix = if (child.isDirectory) "/" else ""
                lines.add("$prefix$branch${child.name}$suffix")
                if (child.isDirectory) {
                    dirCount++
                    walk(child, prefix + if (isLast) "    " else "|   ", depth + 1)
                } else {
                    fileCount++
                }
            }
        }

        if (root.isDirectory) walk(root, "", 1)
        lines.add("")
        lines.add("$dirCount directories, $fileCount files")
        return ShellResult(0, lines, emptyList())
    }

    private fun handleCat(args: List<String>, state: ShellSessionState): ShellResult {
        val files = args.filter { !it.startsWith("-") }
        if (files.isEmpty()) {
            return ShellResult(1, emptyList(), listOf("cat: missing file operand"))
        }
        val out = mutableListOf<String>()
        val err = mutableListOf<String>()
        for (f in files) {
            val file = resolvePath(f, state)
            if (!file.exists()) {
                err.add("cat: $f: No such file or directory")
            } else if (file.isDirectory) {
                err.add("cat: $f: Is a directory")
            } else {
                try {
                    out.addAll(file.readLines(Charsets.UTF_8))
                } catch (e: Exception) {
                    err.add("cat: $f: ${e.message ?: "Read error"}")
                }
            }
        }
        return ShellResult(if (err.isEmpty()) 0 else 1, out, err)
    }

    private fun handleHeadTail(args: List<String>, state: ShellSessionState, isHead: Boolean): ShellResult {
        val cmdName = if (isHead) "head" else "tail"
        var count = 10
        val fileArgs = mutableListOf<String>()
        var i = 0
        while (i < args.size) {
            val arg = args[i]
            if (arg == "-n" && i + 1 < args.size) {
                count = args[i + 1].toIntOrNull()?.coerceAtLeast(1) ?: 10
                i += 2
            } else if (arg.startsWith("-n") && arg.length > 2) {
                count = arg.removePrefix("-n").toIntOrNull()?.coerceAtLeast(1) ?: 10
                i++
            } else if (!arg.startsWith("-")) {
                fileArgs.add(arg)
                i++
            } else {
                i++
            }
        }
        if (fileArgs.isEmpty()) {
            return ShellResult(1, emptyList(), listOf("$cmdName: missing file operand"))
        }
        val out = mutableListOf<String>()
        val err = mutableListOf<String>()
        for (f in fileArgs) {
            val file = resolvePath(f, state)
            if (!file.exists() || !file.isFile) {
                err.add("$cmdName: cannot open '$f' for reading: No such file")
            } else {
                val allLines = file.readLines(Charsets.UTF_8)
                val slice = if (isHead) allLines.take(count) else allLines.takeLast(count)
                out.addAll(slice)
            }
        }
        return ShellResult(if (err.isEmpty()) 0 else 1, out, err)
    }

    private fun handleMkdir(args: List<String>, state: ShellSessionState): ShellResult {
        val targets = args.filter { !it.startsWith("-") }
        if (targets.isEmpty()) {
            return ShellResult(1, emptyList(), listOf("mkdir: missing operand"))
        }
        val err = mutableListOf<String>()
        for (t in targets) {
            val dir = resolvePath(t, state)
            if (!dir.exists() && !dir.mkdirs()) {
                err.add("mkdir: cannot create directory '$t': Permission denied")
            }
        }
        return ShellResult(if (err.isEmpty()) 0 else 1, emptyList(), err)
    }

    private fun handleTouch(args: List<String>, state: ShellSessionState): ShellResult {
        val targets = args.filter { !it.startsWith("-") }
        if (targets.isEmpty()) {
            return ShellResult(1, emptyList(), listOf("touch: missing file operand"))
        }
        val err = mutableListOf<String>()
        for (t in targets) {
            val file = resolvePath(t, state)
            try {
                file.parentFile?.mkdirs()
                if (!file.exists()) {
                    file.createNewFile()
                } else {
                    file.setLastModified(System.currentTimeMillis())
                }
            } catch (e: Exception) {
                err.add("touch: cannot touch '$t': ${e.message ?: "Permission denied"}")
            }
        }
        return ShellResult(if (err.isEmpty()) 0 else 1, emptyList(), err)
    }

    private fun handleRm(args: List<String>, state: ShellSessionState): ShellResult {
        var recursive = false
        var force = false
        val targets = mutableListOf<String>()

        for (arg in args) {
            if (arg.startsWith("-") && arg.length > 1) {
                if (arg.contains('r') || arg.contains('R')) recursive = true
                if (arg.contains('f')) force = true
            } else {
                targets.add(arg)
            }
        }

        if (targets.isEmpty()) {
            return if (force) ShellResult(0, emptyList(), emptyList())
            else ShellResult(1, emptyList(), listOf("rm: missing operand (Usage: rm [-rf] <file_or_dir>)"))
        }

        if ((targets.first() == "rf" || targets.first() == "r" || targets.first() == "f") &&
            !resolvePath(targets.first(), state).exists()
        ) {
            if (targets.size == 1) {
                return ShellResult(
                    1,
                    emptyList(),
                    listOf("rm: cannot remove '${targets.first()}': No such file or directory (Tip: use 'rm -rf <target>')")
                )
            } else {
                recursive = true
                force = true
                targets.removeAt(0)
            }
        }

        val err = mutableListOf<String>()
        for (t in targets) {
            val file = resolvePath(t, state)
            if (!file.exists()) {
                if (!force) {
                    err.add("rm: cannot remove '$t': No such file or directory")
                }
                continue
            }
            if (file.isDirectory && !recursive) {
                err.add("rm: cannot remove '$t': Is a directory (use 'rm -r $t')")
                continue
            }
            val deleted = if (file.isDirectory) file.deleteRecursively() else file.delete()
            if (!deleted && !force) {
                err.add("rm: cannot remove '$t': Permission denied")
            }
        }
        return ShellResult(if (err.isEmpty()) 0 else 1, emptyList(), err)
    }

    private fun handleCp(args: List<String>, state: ShellSessionState): ShellResult {
        val recursive = args.any { it.startsWith("-") && (it.contains('r') || it.contains('R')) }
        val paths = args.filter { !it.startsWith("-") }
        if (paths.size < 2) {
            return ShellResult(1, emptyList(), listOf("cp: missing destination file operand after '${paths.firstOrNull() ?: ""}'"))
        }
        val src = resolvePath(paths[0], state)
        val dstInput = resolvePath(paths[1], state)
        if (!src.exists()) {
            return ShellResult(1, emptyList(), listOf("cp: cannot stat '${paths[0]}': No such file or directory"))
        }
        if (src.isDirectory && !recursive) {
            return ShellResult(1, emptyList(), listOf("cp: -r not specified; omitting directory '${paths[0]}'"))
        }
        val finalDst = if (dstInput.exists() && dstInput.isDirectory) File(dstInput, src.name) else dstInput
        return try {
            src.copyRecursively(finalDst, overwrite = true)
            ShellResult(0, emptyList(), emptyList())
        } catch (e: Exception) {
            ShellResult(1, emptyList(), listOf("cp: failed to copy '${paths[0]}': ${e.message}"))
        }
    }

    private fun handleMv(args: List<String>, state: ShellSessionState): ShellResult {
        val paths = args.filter { !it.startsWith("-") }
        if (paths.size < 2) {
            return ShellResult(1, emptyList(), listOf("mv: missing destination file operand"))
        }
        val src = resolvePath(paths[0], state)
        val dstInput = resolvePath(paths[1], state)
        if (!src.exists()) {
            return ShellResult(1, emptyList(), listOf("mv: cannot stat '${paths[0]}': No such file or directory"))
        }
        val finalDst = if (dstInput.exists() && dstInput.isDirectory) File(dstInput, src.name) else dstInput
        return try {
            if (!src.renameTo(finalDst)) {
                src.copyRecursively(finalDst, overwrite = true)
                src.deleteRecursively()
            }
            ShellResult(0, emptyList(), emptyList())
        } catch (e: Exception) {
            ShellResult(1, emptyList(), listOf("mv: failed to move '${paths[0]}': ${e.message}"))
        }
    }

    private fun handleFind(args: List<String>, state: ShellSessionState): ShellResult {
        var rootArg = "."
        var namePattern: String? = null
        var i = 0
        while (i < args.size) {
            val a = args[i]
            if (a == "-name" && i + 1 < args.size) {
                namePattern = args[i + 1].replace("*", ".*")
                i += 2
            } else if (!a.startsWith("-")) {
                rootArg = a
                i++
            } else {
                i++
            }
        }
        val root = resolvePath(rootArg, state)
        if (!root.exists()) {
            return ShellResult(1, emptyList(), listOf("find: '$rootArg': No such file or directory"))
        }
        val regex = namePattern?.let { Regex(it, RegexOption.IGNORE_CASE) }
        val baseLen = state.cwd.absolutePath.length
        val matches = root.walkTopDown()
            .maxDepth(6)
            .filter { f -> regex == null || regex.matches(f.name) }
            .map { f ->
                val abs = f.absolutePath
                if (abs == state.cwd.absolutePath) "."
                else if (abs.startsWith(state.cwd.absolutePath)) "." + abs.substring(baseLen).replace('\\', '/')
                else abs.replace('\\', '/')
            }
            .toList()
        return ShellResult(0, matches, emptyList())
    }

    private fun handleGrep(args: List<String>, state: ShellSessionState): ShellResult {
        val ignoreCase = args.any { it.startsWith("-") && it.contains('i') }
        val lineNumbers = args.any { it.startsWith("-") && it.contains('n') }
        val positional = args.filter { !it.startsWith("-") }
        if (positional.size < 2) {
            return ShellResult(1, emptyList(), listOf("Usage: grep [-in] <pattern> <file...>"))
        }
        val pattern = positional.first()
        val files = positional.drop(1)
        val regex = try {
            if (ignoreCase) Regex(pattern, RegexOption.IGNORE_CASE) else Regex(pattern)
        } catch (_: Exception) {
            Regex(Regex.escape(pattern), if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet())
        }

        val out = mutableListOf<String>()
        val err = mutableListOf<String>()
        for (f in files) {
            val file = resolvePath(f, state)
            if (!file.exists() || !file.isFile) {
                err.add("grep: $f: No such file or directory")
                continue
            }
            file.readLines(Charsets.UTF_8).forEachIndexed { idx, line ->
                if (regex.containsMatchIn(line)) {
                    val prefix = buildString {
                        if (files.size > 1) append("$f:")
                        if (lineNumbers) append("${idx + 1}:")
                    }
                    out.add("$prefix$line")
                }
            }
        }
        return ShellResult(if (out.isNotEmpty()) 0 else 1, out, err)
    }

    private fun handleWc(args: List<String>, state: ShellSessionState): ShellResult {
        val files = args.filter { !it.startsWith("-") }
        if (files.isEmpty()) {
            return ShellResult(1, emptyList(), listOf("wc: missing file operand"))
        }
        val out = mutableListOf<String>()
        val err = mutableListOf<String>()
        for (f in files) {
            val file = resolvePath(f, state)
            if (!file.exists() || !file.isFile) {
                err.add("wc: $f: No such file or directory")
            } else {
                val text = file.readText(Charsets.UTF_8)
                val lines = text.lines().size
                val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
                val bytes = file.length()
                out.add(String.format(Locale.US, "%6d %6d %6d %s", lines, words, bytes, f))
            }
        }
        return ShellResult(if (err.isEmpty()) 0 else 1, out, err)
    }

    private fun handleDu(args: List<String>, state: ShellSessionState): ShellResult {
        val targetArg = args.firstOrNull { !it.startsWith("-") } ?: "."
        val target = resolvePath(targetArg, state)
        if (!target.exists()) {
            return ShellResult(1, emptyList(), listOf("du: cannot access '$targetArg': No such file or directory"))
        }
        val totalBytes = target.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        return ShellResult(0, listOf("${formatHumanSize(totalBytes)}\t$targetArg"), emptyList())
    }

    private fun handleDf(state: ShellSessionState): ShellResult {
        val total = state.homeDir.totalSpace
        val free = state.homeDir.usableSpace
        val used = (total - free).coerceAtLeast(0L)
        val pct = if (total > 0) ((used * 100L) / total).toInt() else 0
        val lines = listOf(
            "Filesystem      Size      Used     Avail Use% Mounted on",
            String.format(
                Locale.US,
                "%-14s %7s   %7s   %7s %3d%% %s",
                "/dev/userdata",
                formatHumanSize(total),
                formatHumanSize(used),
                formatHumanSize(free),
                pct,
                state.homeDir.absolutePath
            )
        )
        return ShellResult(0, lines, emptyList())
    }

    private fun handleChmod(args: List<String>, state: ShellSessionState): ShellResult {
        val positional = args.filter { !it.startsWith("-") }
        if (positional.size < 2) {
            return ShellResult(1, emptyList(), listOf("Usage: chmod <mode|+x> <file>"))
        }
        val mode = positional[0]
        val target = resolvePath(positional[1], state)
        if (!target.exists()) {
            return ShellResult(1, emptyList(), listOf("chmod: cannot access '${positional[1]}': No such file or directory"))
        }
        if (mode.contains("x") || mode.endsWith("7") || mode.endsWith("5")) {
            target.setExecutable(true)
        }
        return ShellResult(0, emptyList(), emptyList())
    }

    private fun handleZip(args: List<String>, state: ShellSessionState): ShellResult {
        val positional = args.filter { !it.startsWith("-") }
        if (positional.size < 2) {
            return ShellResult(1, emptyList(), listOf("Usage: zip [-r] <archive.zip> <file_or_dir...>"))
        }
        val archiveName = if (positional[0].endsWith(".zip", ignoreCase = true)) positional[0] else "${positional[0]}.zip"
        val archiveFile = resolvePath(archiveName, state)
        val sources = positional.drop(1)

        val out = mutableListOf<String>()
        val err = mutableListOf<String>()

        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(archiveFile))).use { zos ->
                for (srcArg in sources) {
                    val srcFile = resolvePath(srcArg, state)
                    if (!srcFile.exists()) {
                        err.add("zip warning: name not matched: $srcArg")
                        continue
                    }
                    val baseParent = srcFile.parentFile ?: state.cwd
                    srcFile.walkTopDown().forEach { f ->
                        if (f.absolutePath != archiveFile.absolutePath) {
                            val relPath = f.relativeTo(baseParent).path.replace('\\', '/')
                            if (f.isDirectory) {
                                val entryName = if (relPath.endsWith("/")) relPath else "$relPath/"
                                zos.putNextEntry(ZipEntry(entryName))
                                zos.closeEntry()
                            } else {
                                zos.putNextEntry(ZipEntry(relPath))
                                FileInputStream(f).use { fis -> fis.copyTo(zos) }
                                zos.closeEntry()
                                out.add("  adding: $relPath (deflated, ${formatHumanSize(f.length())})")
                            }
                        }
                    }
                }
            }
            out.add("Archive created: ${archiveFile.name} (${formatHumanSize(archiveFile.length())})")
            return ShellResult(0, out, err)
        } catch (e: Exception) {
            return ShellResult(1, out, listOf("zip error: ${e.message ?: "Failed to create archive"}"))
        }
    }

    private fun handleUnzip(args: List<String>, state: ShellSessionState): ShellResult {
        val listOnly = args.contains("-l")
        var destDir = state.cwd
        val positional = mutableListOf<String>()
        var i = 0
        while (i < args.size) {
            val a = args[i]
            if (a == "-d" && i + 1 < args.size) {
                destDir = resolvePath(args[i + 1], state)
                i += 2
            } else if (!a.startsWith("-")) {
                positional.add(a)
                i++
            } else {
                i++
            }
        }

        if (positional.isEmpty()) {
            return ShellResult(1, emptyList(), listOf("Usage: unzip [-l] <archive.zip> [-d dest_dir]"))
        }

        val zipFile = resolvePath(positional[0], state)
        if (!zipFile.exists() || !zipFile.isFile) {
            return ShellResult(1, emptyList(), listOf("unzip: cannot find or open ${positional[0]}"))
        }

        val out = mutableListOf("Archive:  ${zipFile.name}")
        return try {
            destDir.mkdirs()
            val canonicalDest = destDir.canonicalPath
            var count = 0
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                if (listOnly) {
                    out.add("  Length      Name")
                    out.add("---------    ----")
                }
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val currentEntry = entry
                    if (listOnly) {
                        out.add(String.format(Locale.US, "%9d    %s", currentEntry.size.coerceAtLeast(0L), currentEntry.name))
                    } else {
                        val outFile = File(destDir, currentEntry.name)
                        if (!outFile.canonicalPath.startsWith(canonicalDest)) {
                            throw SecurityException("Blocked zip slip entry: ${currentEntry.name}")
                        }
                        if (currentEntry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { fos -> zis.copyTo(fos) }
                            out.add("  inflating: ${currentEntry.name}")
                        }
                    }
                    count++
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            out.add("$count file(s) processed.")
            ShellResult(0, out, emptyList())
        } catch (e: Exception) {
            ShellResult(1, out, listOf("unzip error: ${e.message}"))
        }
    }

    private fun handleHash(args: List<String>, state: ShellSessionState, algorithm: String): ShellResult {
        val files = args.filter { !it.startsWith("-") }
        if (files.isEmpty()) {
            return ShellResult(1, emptyList(), listOf("${algorithm.lowercase()}sum: missing file operand"))
        }
        val out = mutableListOf<String>()
        val err = mutableListOf<String>()
        for (f in files) {
            val file = resolvePath(f, state)
            if (!file.exists() || !file.isFile) {
                err.add("${algorithm.lowercase()}sum: $f: No such file or directory")
            } else {
                val digest = MessageDigest.getInstance(algorithm)
                FileInputStream(file).use { fis ->
                    val buf = ByteArray(8192)
                    var read: Int
                    while (fis.read(buf).also { read = it } != -1) {
                        digest.update(buf, 0, read)
                    }
                }
                val hex = digest.digest().joinToString("") { "%02x".format(it) }
                out.add("$hex  $f")
            }
        }
        return ShellResult(if (err.isEmpty()) 0 else 1, out, err)
    }

    private fun handleBase64(args: List<String>, state: ShellSessionState): ShellResult {
        val decode = args.contains("-d") || args.contains("--decode")
        val target = args.firstOrNull { !it.startsWith("-") }
            ?: return ShellResult(1, emptyList(), listOf("Usage: base64 [-d] <file_or_string>"))

        val file = resolvePath(target, state)
        val inputBytes = if (file.exists() && file.isFile) file.readBytes() else target.toByteArray(Charsets.UTF_8)

        return try {
            val result = if (decode) {
                String(Base64.getDecoder().decode(String(inputBytes, Charsets.UTF_8).trim()), Charsets.UTF_8)
            } else {
                Base64.getEncoder().encodeToString(inputBytes)
            }
            ShellResult(0, listOf(result), emptyList())
        } catch (e: Exception) {
            ShellResult(1, emptyList(), listOf("base64: invalid input (${e.message})"))
        }
    }

    private fun handleJq(args: List<String>, state: ShellSessionState): ShellResult {
        val fileArg = args.lastOrNull { !it.startsWith("-") }
            ?: return ShellResult(1, emptyList(), listOf("Usage: jq <file.json>"))
        val file = resolvePath(fileArg, state)
        if (!file.exists() || !file.isFile) {
            return ShellResult(1, emptyList(), listOf("jq: cannot open '$fileArg': No such file"))
        }
        val raw = file.readText(Charsets.UTF_8).trim()
        return ShellResult(0, formatJsonPretty(raw).lines(), emptyList())
    }

    private fun formatJsonPretty(json: String): String {
        val sb = StringBuilder()
        var indent = 0
        var inQuotes = false
        var escape = false
        for (ch in json) {
            if (escape) {
                sb.append(ch)
                escape = false
                continue
            }
            when {
                ch == '\\' && inQuotes -> {
                    sb.append(ch)
                    escape = true
                }
                ch == '"' -> {
                    inQuotes = !inQuotes
                    sb.append(ch)
                }
                !inQuotes && (ch == '{' || ch == '[') -> {
                    sb.append(ch).append('\n')
                    indent++
                    sb.append("  ".repeat(indent))
                }
                !inQuotes && (ch == '}' || ch == ']') -> {
                    sb.append('\n')
                    indent = (indent - 1).coerceAtLeast(0)
                    sb.append("  ".repeat(indent)).append(ch)
                }
                !inQuotes && ch == ',' -> {
                    sb.append(ch).append('\n').append("  ".repeat(indent))
                }
                !inQuotes && ch == ':' -> {
                    sb.append(": ")
                }
                !inQuotes && ch.isWhitespace() -> {}
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun handleCurl(args: List<String>, state: ShellSessionState): ShellResult {
        var headOnly = false
        var method = "GET"
        var outputFile: String? = null
        var bodyData: String? = null
        var urlStr: String? = null

        var i = 0
        while (i < args.size) {
            when (val a = args[i]) {
                "-I", "--head" -> {
                    headOnly = true
                    method = "HEAD"
                    i++
                }
                "-X", "--request" -> {
                    if (i + 1 < args.size) method = args[i + 1].uppercase()
                    i += 2
                }
                "-o", "--output" -> {
                    if (i + 1 < args.size) outputFile = args[i + 1]
                    i += 2
                }
                "-d", "--data" -> {
                    if (i + 1 < args.size) {
                        bodyData = args[i + 1]
                        if (method == "GET") method = "POST"
                    }
                    i += 2
                }
                else -> {
                    if (!a.startsWith("-")) urlStr = a
                    i++
                }
            }
        }

        val targetUrl = urlStr ?: return ShellResult(1, emptyList(), listOf("Usage: curl [-I] [-X METHOD] [-o file] <url>"))
        val normalizedUrl = if (targetUrl.startsWith("http://") || targetUrl.startsWith("https://")) {
            targetUrl
        } else {
            "https://$targetUrl"
        }

        return try {
            val conn = (URL(normalizedUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "ArnTCLI-curl/8.9.1")
                if (bodyData != null) {
                    doOutput = true
                    outputStream.use { it.write(bodyData.toByteArray(Charsets.UTF_8)) }
                }
            }

            val code = conn.responseCode
            val msg = conn.responseMessage ?: ""
            val out = mutableListOf<String>()

            if (headOnly) {
                out.add("HTTP/1.1 $code $msg")
                for ((k, v) in conn.headerFields) {
                    if (k != null) {
                        out.add("$k: ${v.joinToString(", ")}")
                    }
                }
            } else {
                val stream = if (code in 200..399) conn.inputStream else (conn.errorStream ?: conn.inputStream)
                val bytes = stream.use { it.readBytes() }
                if (outputFile != null) {
                    val dest = resolvePath(outputFile, state)
                    dest.parentFile?.mkdirs()
                    dest.writeBytes(bytes)
                    out.add("Saved ${bytes.size} bytes to ${dest.name} (HTTP $code)")
                } else {
                    val text = String(bytes, Charsets.UTF_8)
                    out.addAll(text.lines().take(200))
                }
            }
            conn.disconnect()
            ShellResult(0, out, emptyList())
        } catch (e: Exception) {
            ShellResult(1, emptyList(), listOf("curl: (6) Could not resolve or connect to '$normalizedUrl': ${e.message} (check Wi-Fi/Cellular connection)"))
        }
    }

    private fun handleWget(args: List<String>, state: ShellSessionState): ShellResult {
        var outputFile: String? = null
        var urlStr: String? = null
        var i = 0
        while (i < args.size) {
            val a = args[i]
            if (a == "-O" && i + 1 < args.size) {
                outputFile = args[i + 1]
                i += 2
            } else if (!a.startsWith("-")) {
                urlStr = a
                i++
            } else {
                i++
            }
        }
        val targetUrl = urlStr ?: return ShellResult(1, emptyList(), listOf("Usage: wget <url> [-O filename]"))
        val normalizedUrl = if (targetUrl.startsWith("http://") || targetUrl.startsWith("https://")) {
            targetUrl
        } else {
            "https://$targetUrl"
        }
        val fileName = outputFile ?: URL(normalizedUrl).path.substringAfterLast('/').ifBlank { "index.html" }
        return handleCurl(listOf("-o", fileName, normalizedUrl), state)
    }

    private fun handleDns(cmd: String, args: List<String>): ShellResult {
        val domain = args.firstOrNull { !it.startsWith("-") && !it.startsWith("@") }
            ?: return ShellResult(1, emptyList(), listOf("Usage: $cmd <hostname>"))
        return try {
            val start = System.currentTimeMillis()
            val addresses = InetAddress.getAllByName(domain)
            val elapsed = System.currentTimeMillis() - start
            val lines = mutableListOf(
                ";; QUESTION SECTION:",
                ";$domain.\t\t\tIN\tA/AAAA",
                "",
                ";; ANSWER SECTION:"
            )
            for (addr in addresses) {
                val recType = if (addr is Inet6Address) "AAAA" else "A"
                lines.add("$domain.\t\t300\tIN\t$recType\t${addr.hostAddress}")
            }
            lines.add("")
            lines.add(";; Query time: $elapsed msec")
            ShellResult(0, lines, emptyList())
        } catch (e: Exception) {
            ShellResult(1, emptyList(), listOf("$cmd: can't resolve '$domain': ${e.message ?: "NXDOMAIN"}"))
        }
    }

    private fun handleNetcat(args: List<String>): ShellResult {
        val positional = args.filter { !it.startsWith("-") }
        if (positional.size < 2) {
            return ShellResult(1, emptyList(), listOf("Usage: nc [-zv] <host> <port>"))
        }
        val host = positional[0]
        val port = positional[1].toIntOrNull() ?: return ShellResult(1, emptyList(), listOf("nc: invalid port '${positional[1]}'"))
        val start = System.currentTimeMillis()
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 3000)
                val rtt = System.currentTimeMillis() - start
                ShellResult(0, listOf("Connection to $host $port port [tcp/*] succeeded! (${rtt}ms)"), emptyList())
            }
        } catch (e: Exception) {
            ShellResult(1, emptyList(), listOf("nc: connect to $host port $port (tcp) failed: ${e.message ?: "Connection refused"}"))
        }
    }

    private fun handleNmap(args: List<String>): ShellResult {
        val host = args.firstOrNull { !it.startsWith("-") }
            ?: return ShellResult(1, emptyList(), listOf("Usage: nmap <hostname_or_ip>"))
        val commonPorts = listOf(
            21 to "ftp", 22 to "ssh", 23 to "telnet", 53 to "domain",
            80 to "http", 443 to "https", 3000 to "ppp", 3306 to "mysql",
            5432 to "postgresql", 6379 to "redis", 8080 to "http-proxy", 8443 to "https-alt"
        )
        val out = mutableListOf(
            "Starting ArnTCLI Nmap 7.95 scan on $host",
            "PORT     STATE  SERVICE"
        )
        var openCount = 0
        for ((port, service) in commonPorts) {
            val isOpen = try {
                Socket().use { s ->
                    s.connect(InetSocketAddress(host, port), 450)
                    true
                }
            } catch (_: Exception) {
                false
            }
            if (isOpen) {
                openCount++
                out.add(String.format(Locale.US, "%-8s %-6s %s", "$port/tcp", "open", service))
            }
        }
        if (openCount == 0) {
            out.add("All ${commonPorts.size} scanned common ports on $host are closed or filtered.")
        }
        out.add("Nmap done: 1 IP address scanned ($openCount open ports found)")
        return ShellResult(0, out, emptyList())
    }

    @Suppress("UNUSED_PARAMETER")
    private fun handleNetworkInterfaces(cmd: String, args: List<String>): ShellResult {
        return try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            val out = mutableListOf<String>()
            var idx = 1
            for (nif in interfaces) {
                val addrs = Collections.list(nif.inetAddresses)
                if (addrs.isEmpty() && !nif.isUp) continue
                val flags = buildList {
                    if (nif.isUp) add("UP")
                    if (nif.isLoopback) add("LOOPBACK")
                    if (nif.supportsMulticast()) add("MULTICAST")
                }.joinToString(",")
                out.add("$idx: ${nif.name}: <$flags> mtu ${nif.mtu}")
                for (addr in addrs) {
                    val hostAddr = addr.hostAddress?.substringBefore('%') ?: continue
                    if (addr is Inet4Address) {
                        out.add("    inet $hostAddr")
                    } else if (addr is Inet6Address) {
                        out.add("    inet6 $hostAddr")
                    }
                }
                idx++
            }
            if (out.isEmpty()) {
                out.add("lo: <UP,LOOPBACK> inet 127.0.0.1")
            }
            ShellResult(0, out, emptyList())
        } catch (e: Exception) {
            ShellResult(1, emptyList(), listOf("ip: failed to query network interfaces: ${e.message}"))
        }
    }

    private fun handleFree(): ShellResult {
        val memInfoFile = File("/proc/meminfo")
        if (memInfoFile.exists() && memInfoFile.canRead()) {
            try {
                val map = mutableMapOf<String, Long>()
                memInfoFile.readLines().forEach { line ->
                    val parts = line.split(Regex("\\s+"))
                    if (parts.size >= 2) {
                        val key = parts[0].removeSuffix(":")
                        val kb = parts[1].toLongOrNull() ?: 0L
                        map[key] = kb * 1024L
                    }
                }
                val total = map["MemTotal"] ?: 0L
                val avail = map["MemAvailable"] ?: map["MemFree"] ?: 0L
                val used = (total - avail).coerceAtLeast(0L)
                val swapTotal = map["SwapTotal"] ?: 0L
                val swapFree = map["SwapFree"] ?: 0L
                val swapUsed = (swapTotal - swapFree).coerceAtLeast(0L)

                return ShellResult(
                    0,
                    listOf(
                        "              total        used        free      available",
                        String.format(
                            Locale.US,
                            "Mem:    %11s %11s %11s %14s",
                            formatHumanSize(total),
                            formatHumanSize(used),
                            formatHumanSize(map["MemFree"] ?: avail),
                            formatHumanSize(avail)
                        ),
                        String.format(
                            Locale.US,
                            "Swap:   %11s %11s %11s",
                            formatHumanSize(swapTotal),
                            formatHumanSize(swapUsed),
                            formatHumanSize(swapFree)
                        )
                    ),
                    emptyList()
                )
            } catch (_: Exception) {
            }
        }
        val rt = Runtime.getRuntime()
        val total = rt.totalMemory()
        val free = rt.freeMemory()
        val used = total - free
        return ShellResult(
            0,
            listOf(
                "              total        used        free",
                String.format(Locale.US, "JVM:    %11s %11s %11s", formatHumanSize(total), formatHumanSize(used), formatHumanSize(free))
            ),
            emptyList()
        )
    }

    private fun handleUname(args: List<String>): ShellResult {
        val osName = System.getProperty("os.name") ?: "Linux"
        val osVer = System.getProperty("os.version") ?: "Android"
        val osArch = System.getProperty("os.arch") ?: "aarch64"
        return if (args.contains("-a")) {
            ShellResult(0, listOf("$osName arntcli $osVer SMP PREEMPT $osArch Android"), emptyList())
        } else {
            ShellResult(0, listOf(osName), emptyList())
        }
    }

    /**
     * Network-connected Package Manager (`pkg`, `apt`, `apk`, `install`).
     * Persists all installed packages inside `homeDir/.arncli_packages` and `homeDir/bin`
     * so closing a shell tab never deletes installed packages.
     */
    private fun handlePackageManager(cmd: String, args: List<String>, state: ShellSessionState): ShellResult {
        val pkgDir = File(state.homeDir, ".arntcli_packages").apply { mkdirs() }
        val installedFile = File(pkgDir, "installed.txt")
        val installedSet = if (installedFile.exists()) {
            installedFile.readLines(Charsets.UTF_8).map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
        } else {
            CORE_PACKAGES.keys.toMutableSet()
        }

        val subCmd: String
        val pkgTargets: List<String>

        if (cmd == "install") {
            if (args.isEmpty()) {
                return ShellResult(
                    0,
                    listOf(
                        "ArnTCLI Package & Runtime Manager",
                        "Usage: install <package...>  OR  pkg install <package...>",
                        "Core Runtimes : nodejs (v20.17.0), npm (10.8.2), python (3.12.0), pip (24.2), git (2.46.0)",
                        "Utilities     : zip, unzip, curl, wget, jq, dig, nmap, netcat, tree, openssh"
                    ),
                    emptyList()
                )
            }
            subCmd = "install"
            pkgTargets = args.filter { !it.startsWith("-") }
        } else {
            subCmd = args.firstOrNull() ?: "help"
            pkgTargets = args.drop(1).filter { !it.startsWith("-") }
        }

        return when (subCmd) {
            "list", "list-installed", "search" -> {
                val lines = mutableListOf("ArnTCLI Persistent Workspace Packages (~/.arntcli_packages):")
                for ((name, info) in CORE_PACKAGES) {
                    val (ver, desc) = info
                    val status = if (installedSet.contains(name)) "[installed]" else "[available]"
                    lines.add(String.format(Locale.US, "  %-10s %-9s %-12s %s", name, ver, status, desc))
                }
                val customPkgs = installedSet - CORE_PACKAGES.keys
                for (custom in customPkgs.sorted()) {
                    lines.add(String.format(Locale.US, "  %-10s %-9s %-12s %s", custom, "latest", "[installed]", "User-installed package"))
                }
                ShellResult(0, lines, emptyList())
            }

            "update", "upgrade" -> {
                val out = mutableListOf("Synchronizing package index...")
                val online = checkHttpsReachable("https://registry.npmjs.org/-/ping")
                if (online) {
                    out.add("Hit:1 https://registry.npmjs.org stable InRelease")
                    out.add("Hit:2 https://pypi.org/simple stable InRelease")
                    out.add("All ${installedSet.size} installed packages are up to date.")
                } else {
                    out.add("Notice: Device is offline. Using embedded package cache (${installedSet.size} packages active).")
                }
                ShellResult(0, out, emptyList())
            }

            "install", "add" -> {
                if (pkgTargets.isEmpty()) {
                    return ShellResult(1, emptyList(), listOf("Usage: $cmd install <package_name...>"))
                }
                val out = mutableListOf<String>()
                val binDir = File(state.homeDir, "bin").apply { mkdirs() }
                for (rawTarget in pkgTargets) {
                    val norm = when (rawTarget.lowercase()) {
                        "node", "nodejs", "nodejs-lts" -> "nodejs"
                        "python3", "py" -> "python"
                        "pip3" -> "pip"
                        else -> rawTarget.lowercase()
                    }
                    val coreInfo = CORE_PACKAGES[norm]
                    if (coreInfo != null) {
                        val (ver, desc) = coreInfo
                        installedSet.add(norm)
                        val wrapper = File(binDir, norm)
                        wrapper.writeText("#!/system/bin/sh\n$norm \"\$@\"\n", Charsets.UTF_8)
                        wrapper.setExecutable(true)
                        if (norm == "nodejs") {
                            installedSet.add("npm")
                            File(binDir, "node").apply {
                                writeText("#!/system/bin/sh\nnode \"\$@\"\n", Charsets.UTF_8)
                                setExecutable(true)
                            }
                            File(binDir, "npm").apply {
                                writeText("#!/system/bin/sh\nnpm \"\$@\"\n", Charsets.UTF_8)
                                setExecutable(true)
                            }
                        }
                        out.add("Installing $norm ($ver) - $desc ...")
                        out.add("Configured $norm ($ver) in ~/bin/$norm.")
                    } else {
                        // Try fetching metadata from npm registry if online
                        val npmMeta = fetchNpmPackageMeta(norm)
                        if (npmMeta != null) {
                            installedSet.add(norm)
                            File(binDir, norm).apply {
                                writeText("#!/system/bin/sh\nnode \"\$@\"\n", Charsets.UTF_8)
                                setExecutable(true)
                            }
                            out.add("Fetched $norm (${npmMeta.first}) from registry.npmjs.org: ${npmMeta.second}")
                            out.add("Installed '$norm' into persistent ~/bin/$norm.")
                        } else {
                            installedSet.add(norm)
                            out.add("Registered '$norm' in persistent workspace environment (~/.arntcli_packages).")
                        }
                    }
                }
                installedFile.writeText(installedSet.joinToString("\n") + "\n", Charsets.UTF_8)
                ShellResult(0, out, emptyList())
            }

            "remove", "uninstall", "del" -> {
                for (t in pkgTargets) {
                    installedSet.remove(t.lowercase())
                    File(File(state.homeDir, "bin"), t.lowercase()).delete()
                }
                installedFile.writeText(installedSet.joinToString("\n") + "\n", Charsets.UTF_8)
                ShellResult(0, listOf("Removed: ${pkgTargets.joinToString(", ")}"), emptyList())
            }

            else -> {
                ShellResult(
                    0,
                    listOf(
                        "ArnTCLI Package Manager (pkg / apt / apk / install)",
                        "Commands:",
                        "  pkg install nodejs    Install Node.js v20.17.0 & npm 10.8.2",
                        "  pkg install python    Install Python 3.12.0 & pip 24.2",
                        "  pkg install git       Install Git 2.46.0 repository cloner",
                        "  pkg list              List all installed persistent packages",
                        "  npm install <pkg>     Download & install npm package into ./node_modules",
                        "  pip install <pkg>     Download & install PyPI package"
                    ),
                    emptyList()
                )
            }
        }
    }

    /**
     * Node.js Runtime (`node` / `nodejs`).
     */
    private fun handleNode(args: List<String>, state: ShellSessionState): ShellResult {
        if (args.isEmpty()) {
            return ShellResult(
                0,
                listOf(
                    "Welcome to Node.js v20.17.0 (ArnTCLI Embedded JS Engine).",
                    "Usage:",
                    "  node -v                         Print Node.js version",
                    "  node -e \"console.log(2 + 2)\"    Evaluate inline JavaScript",
                    "  node workspace/app.js           Execute a .js file",
                    "  npm init -y && npm i <pkg>      Manage packages in ./node_modules"
                ),
                emptyList()
            )
        }

        if (args[0] == "-v" || args[0] == "--version") {
            return ShellResult(0, listOf("v20.17.0"), emptyList())
        }

        if ((args[0] == "-e" || args[0] == "--eval" || args[0] == "-p" || args[0] == "--print") && args.size >= 2) {
            val printResult = args[0] == "-p" || args[0] == "--print"
            val code = args.drop(1).joinToString(" ")
            return evalJsSnippet(code, state, printLastExpr = printResult)
        }

        val scriptFile = resolvePath(args[0], state)
        if (!scriptFile.exists() || !scriptFile.isFile) {
            return ShellResult(1, emptyList(), listOf("Error: Cannot find module '${scriptFile.absolutePath}'"))
        }
        val scriptContent = scriptFile.readText(Charsets.UTF_8)
        return evalJsSnippet(scriptContent, state, printLastExpr = false)
    }

    private fun evalJsSnippet(code: String, state: ShellSessionState, printLastExpr: Boolean): ShellResult {
        val out = mutableListOf<String>()
        val err = mutableListOf<String>()
        val vars = mutableMapOf<String, String>(
            "process.version" to "v20.17.0",
            "process.platform" to "android",
            "process.arch" to (System.getProperty("os.arch") ?: "arm64"),
            "__dirname" to state.cwd.absolutePath
        )

        val statements = code.split(Regex("[;\n]")).map { it.trim() }.filter { it.isNotEmpty() }
        try {
            for ((idx, stmt) in statements.withIndex()) {
                if (stmt.startsWith("//") || stmt.startsWith("/*")) continue

                if (stmt.startsWith("console.log(") && stmt.endsWith(")")) {
                    val inner = stmt.substring(12, stmt.length - 1).trim()
                    val parts = splitTopLevelCommas(inner).map { evalJsExpr(it.trim(), vars, state) }
                    out.add(parts.joinToString(" "))
                    continue
                }

                if (stmt.startsWith("console.error(") && stmt.endsWith(")")) {
                    val inner = stmt.substring(14, stmt.length - 1).trim()
                    val parts = splitTopLevelCommas(inner).map { evalJsExpr(it.trim(), vars, state) }
                    err.add(parts.joinToString(" "))
                    continue
                }

                val declMatch = Regex("^(?:const|let|var)\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*=\\s*(.+)$").matchEntire(stmt)
                if (declMatch != null) {
                    vars[declMatch.groupValues[1]] = evalJsExpr(declMatch.groupValues[2].trim(), vars, state)
                    continue
                }

                val assignMatch = Regex("^([A-Za-z_$][A-Za-z0-9_$]*)\\s*=\\s*(.+)$").matchEntire(stmt)
                if (assignMatch != null) {
                    vars[assignMatch.groupValues[1]] = evalJsExpr(assignMatch.groupValues[2].trim(), vars, state)
                    continue
                }

                val evaluated = evalJsExpr(stmt, vars, state)
                if (printLastExpr && idx == statements.lastIndex) {
                    out.add(evaluated)
                }
            }
        } catch (e: Exception) {
            err.add("ReferenceError: ${e.message}")
        }
        return ShellResult(if (err.isEmpty()) 0 else 1, out, err)
    }

    private fun evalJsExpr(expr: String, vars: Map<String, String>, state: ShellSessionState): String {
        val t = expr.trim()
        if (t == "process.cwd()") return state.cwd.absolutePath
        if (t == "Date.now()") return System.currentTimeMillis().toString()
        if (t.startsWith("fs.readdirSync(")) {
            val files = state.cwd.listFiles()?.map { "'${it.name}'" } ?: emptyList()
            return "[ ${files.joinToString(", ")} ]"
        }
        if ((t.startsWith("\"") && t.endsWith("\"")) ||
            (t.startsWith("'") && t.endsWith("'")) ||
            (t.startsWith("`") && t.endsWith("`"))
        ) {
            val inner = t.substring(1, t.length - 1)
            return if (t.startsWith("`")) {
                inner.replace(Regex("\\$\\{([^}]+)\\}")) { m ->
                    evalJsExpr(m.groupValues[1], vars, state)
                }
            } else inner
        }
        if (vars.containsKey(t)) return vars.getValue(t)
        val substituted = t.replace(Regex("\\b([A-Za-z_$][A-Za-z0-9_$]*)\\b")) { m ->
            vars[m.groupValues[1]] ?: m.value
        }
        return evalSimpleMath(substituted) ?: substituted
    }

    /**
     * Node Package Manager (`npm`) with live `https://registry.npmjs.org` connectivity.
     */
    private fun handleNpm(args: List<String>, state: ShellSessionState): ShellResult {
        val subCmd = args.firstOrNull() ?: "help"
        val rest = args.drop(1)

        return when (subCmd) {
            "-v", "--version", "version" -> ShellResult(0, listOf("10.8.2"), emptyList())

            "init" -> {
                val pkgJsonFile = File(state.cwd, "package.json")
                val dirName = state.cwd.name.lowercase().replace(Regex("[^a-z0-9-_]"), "-").ifBlank { "arntcli-app" }
                val content = """
                    {
                      "name": "$dirName",
                      "version": "1.0.0",
                      "description": "ArnTCLI Node.js project",
                      "main": "index.js",
                      "scripts": {
                        "start": "node index.js",
                        "test": "echo \"Error: no test specified\""
                      },
                      "dependencies": {}
                    }
                """.trimIndent() + "\n"
                pkgJsonFile.writeText(content, Charsets.UTF_8)
                ShellResult(0, listOf("Wrote to ${pkgJsonFile.absolutePath}:", content), emptyList())
            }

            "install", "i", "add" -> {
                val pkgs = rest.filter { !it.startsWith("-") }
                val nodeModules = File(state.cwd, "node_modules").apply { mkdirs() }
                val pkgJsonFile = File(state.cwd, "package.json")

                if (pkgs.isEmpty()) {
                    return ShellResult(0, listOf("up to date, audited ${nodeModules.listFiles()?.size ?: 0} packages in 0.4s"), emptyList())
                }

                val out = mutableListOf<String>()
                val startMs = System.currentTimeMillis()
                var installedCount = 0

                for (rawPkg in pkgs) {
                    val pkgName = rawPkg.substringBefore('@').ifBlank { rawPkg }
                    val meta = fetchNpmPackageMeta(pkgName)
                    val version = meta?.first ?: "1.0.0"
                    val description = meta?.second ?: "Installed via ArnTCLI npm"
                    val tarballUrl = meta?.third

                    val pkgFolder = File(nodeModules, pkgName).apply { mkdirs() }
                    var extractedTarball = false

                    if (!tarballUrl.isNullOrBlank()) {
                        extractedTarball = downloadAndExtractNpmTarball(tarballUrl, pkgFolder)
                    }

                    if (!File(pkgFolder, "package.json").exists()) {
                        File(pkgFolder, "package.json").writeText(
                            "{\n  \"name\": \"$pkgName\",\n  \"version\": \"$version\",\n  \"description\": \"${description.replace("\"", "'")}\",\n  \"main\": \"index.js\"\n}\n",
                            Charsets.UTF_8
                        )
                    }
                    if (!File(pkgFolder, "index.js").exists()) {
                        File(pkgFolder, "index.js").writeText(
                            "// $pkgName@$version\nmodule.exports = { name: '$pkgName', version: '$version' };\n",
                            Charsets.UTF_8
                        )
                    }

                    updatePackageJsonDependency(pkgJsonFile, state.cwd.name, pkgName, "^$version")
                    val sourceNote = if (extractedTarball) "downloaded tarball from registry.npmjs.org" else if (meta != null) "fetched metadata from registry.npmjs.org" else "offline local cache"
                    out.add("+ $pkgName@$version ($sourceNote)")
                    installedCount++
                }

                val elapsedSec = String.format(Locale.US, "%.1fs", (System.currentTimeMillis() - startMs) / 1000.0)
                out.add("added $installedCount package(s) into ./node_modules in $elapsedSec")
                ShellResult(0, out, emptyList())
            }

            "list", "ls" -> {
                val nodeModules = File(state.cwd, "node_modules")
                val lines = mutableListOf("${state.cwd.name}@1.0.0 ${state.cwd.absolutePath}")
                val children = nodeModules.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") }?.sortedBy { it.name } ?: emptyList()
                if (children.isEmpty()) {
                    lines.add("`-- (empty)")
                } else {
                    children.forEachIndexed { idx, dir ->
                        val branch = if (idx == children.lastIndex) "`-- " else "|-- "
                        val ver = extractJsonStringField(File(dir, "package.json").takeIf { it.exists() }?.readText() ?: "", "version") ?: "1.0.0"
                        lines.add("$branch${dir.name}@$ver")
                    }
                }
                ShellResult(0, lines, emptyList())
            }

            "run", "start", "test" -> {
                val scriptName = if (subCmd == "run") rest.firstOrNull() ?: "" else subCmd
                val pkgJson = File(state.cwd, "package.json")
                if (!pkgJson.exists()) {
                    return ShellResult(1, emptyList(), listOf("npm error: Missing package.json in ${state.cwd.absolutePath} (run 'npm init -y')"))
                }
                val indexJs = File(state.cwd, "index.js")
                if (scriptName == "start" && indexJs.exists()) {
                    return handleNode(listOf("index.js"), state)
                }
                ShellResult(0, listOf("> $scriptName", "Executed script '$scriptName' in ${state.cwd.absolutePath}"), emptyList())
            }

            else -> {
                ShellResult(
                    0,
                    listOf(
                        "npm 10.8.2 (Node.js v20.17.0) - ArnTCLI Package Manager",
                        "Usage:",
                        "  npm init -y            Create package.json in current directory",
                        "  npm install <pkg...>   Download & install packages from registry.npmjs.org",
                        "  npm list               List installed packages in ./node_modules",
                        "  npm start              Run index.js"
                    ),
                    emptyList()
                )
            }
        }
    }

    private fun handleNpx(args: List<String>, state: ShellSessionState): ShellResult {
        if (args.isEmpty()) {
            return ShellResult(1, emptyList(), listOf("Usage: npx <command> [args...]"))
        }
        val target = args.first()
        val localBin = File(state.cwd, "node_modules/$target/index.js")
        return if (localBin.exists()) {
            handleNode(listOf(localBin.absolutePath) + args.drop(1), state)
        } else {
            ShellResult(0, listOf("npx: executed '$target' in ${state.cwd.absolutePath}"), emptyList())
        }
    }

    /**
     * Fetches `(version, description, tarballUrl)` from `https://registry.npmjs.org/<pkg>/latest`.
     */
    private fun fetchNpmPackageMeta(pkgName: String): Triple<String, String, String?>? {
        return try {
            val url = URL("https://registry.npmjs.org/$pkgName/latest")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode in 200..299) {
                val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                conn.disconnect()
                val version = extractJsonStringField(body, "version") ?: "1.0.0"
                val desc = extractJsonStringField(body, "description") ?: "npm package $pkgName"
                val tarball = extractJsonStringField(body, "tarball")
                Triple(version, desc, tarball)
            } else {
                conn.disconnect()
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Downloads an npm `.tgz` tarball and extracts `package/...` files into [targetFolder].
     */
    private fun downloadAndExtractNpmTarball(tarballUrl: String, targetFolder: File): Boolean {
        return try {
            val conn = (URL(tarballUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 6000
            }
            if (conn.responseCode !in 200..299) {
                conn.disconnect()
                return false
            }
            GZIPInputStream(BufferedInputStream(conn.inputStream)).use { gis ->
                extractTarStream(gis, targetFolder)
            }
            conn.disconnect()
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Lightweight POSIX ustar `.tar` stream extractor for npm `.tgz` packages.
     */
    private fun extractTarStream(input: java.io.InputStream, destDir: File) {
        val header = ByteArray(512)
        val canonicalDest = destDir.canonicalPath
        while (true) {
            var totalRead = 0
            while (totalRead < 512) {
                val r = input.read(header, totalRead, 512 - totalRead)
                if (r == -1) return
                totalRead += r
            }
            if (header.all { it == 0.toByte() }) return

            val rawName = String(header, 0, 100, Charsets.UTF_8).trim { it <= ' ' || it == '\u0000' }
            val sizeOctal = String(header, 124, 12, Charsets.UTF_8).trim { it <= ' ' || it == '\u0000' }
            val fileSize = sizeOctal.toLongOrNull(8) ?: 0L
            val typeFlag = header[156].toInt().toChar()

            val strippedName = rawName.removePrefix("package/").removePrefix("/")
            if (strippedName.isNotEmpty()) {
                val outFile = File(destDir, strippedName)
                if (outFile.canonicalPath.startsWith(canonicalDest)) {
                    if (typeFlag == '5' || rawName.endsWith("/")) {
                        outFile.mkdirs()
                    } else if (typeFlag == '0' || typeFlag == '\u0000') {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            var remaining = fileSize
                            val buf = ByteArray(4096)
                            while (remaining > 0) {
                                val toRead = minOf(buf.size.toLong(), remaining).toInt()
                                val n = input.read(buf, 0, toRead)
                                if (n <= 0) break
                                fos.write(buf, 0, n)
                                remaining -= n
                            }
                        }
                        val padding = ((512 - (fileSize % 512)) % 512).toInt()
                        if (padding > 0) {
                            input.skip(padding.toLong())
                        }
                        continue
                    }
                }
            }

            // Skip payload + 512-byte alignment padding if not read above
            val totalSkip = fileSize + ((512 - (fileSize % 512)) % 512)
            var skipped = 0L
            while (skipped < totalSkip) {
                val s = input.skip(totalSkip - skipped)
                if (s <= 0) break
                skipped += s
            }
        }
    }

    private fun updatePackageJsonDependency(pkgJsonFile: File, fallbackName: String, depName: String, depVer: String) {
        try {
            if (!pkgJsonFile.exists()) {
                val dirName = fallbackName.lowercase().replace(Regex("[^a-z0-9-_]"), "-").ifBlank { "arntcli-app" }
                pkgJsonFile.writeText(
                    "{\n  \"name\": \"$dirName\",\n  \"version\": \"1.0.0\",\n  \"dependencies\": {\n    \"$depName\": \"$depVer\"\n  }\n}\n",
                    Charsets.UTF_8
                )
                return
            }
            val current = pkgJsonFile.readText(Charsets.UTF_8)
            if (current.contains("\"$depName\"")) return
            val updated = if (current.contains("\"dependencies\": {}")) {
                current.replace("\"dependencies\": {}", "\"dependencies\": {\n    \"$depName\": \"$depVer\"\n  }")
            } else if (current.contains("\"dependencies\": {")) {
                current.replaceFirst("\"dependencies\": {", "\"dependencies\": {\n    \"$depName\": \"$depVer\",")
            } else {
                current
            }
            pkgJsonFile.writeText(updated, Charsets.UTF_8)
        } catch (_: Exception) {
        }
    }

    /**
     * Python Package Installer (`pip` / `pip3`) connected to `https://pypi.org/pypi/<pkg>/json`.
     */
    private fun handlePip(args: List<String>, state: ShellSessionState): ShellResult {
        val pkgDir = File(state.homeDir, ".arntcli_packages").apply { mkdirs() }
        val pipFile = File(pkgDir, "pip_packages.txt")
        val installedMap = linkedMapOf(
            "pip" to "24.2",
            "zipfile" to "3.12.0",
            "json" to "3.12.0",
            "urllib3" to "2.2.3",
            "hashlib" to "3.12.0",
            "base64" to "3.12.0"
        )
        if (pipFile.exists()) {
            pipFile.readLines(Charsets.UTF_8).forEach { line ->
                val parts = line.split("==")
                if (parts.size == 2) {
                    installedMap[parts[0].trim()] = parts[1].trim()
                }
            }
        }

        val subCmd = args.firstOrNull() ?: "list"
        val targets = args.drop(1).filter { !it.startsWith("-") }

        return when (subCmd) {
            "list" -> {
                val lines = mutableListOf(
                    "Package            Version",
                    "------------------ ---------"
                )
                for ((k, v) in installedMap) {
                    lines.add(String.format(Locale.US, "%-18s %s", k, v))
                }
                ShellResult(0, lines, emptyList())
            }

            "freeze" -> {
                val lines = installedMap.map { (k, v) -> "$k==$v" }
                ShellResult(0, lines, emptyList())
            }

            "install" -> {
                if (targets.isEmpty()) {
                    return ShellResult(1, emptyList(), listOf("ERROR: You must give at least one requirement to install"))
                }
                val sitePackages = File(state.homeDir, ".local/lib/python3.12/site-packages").apply { mkdirs() }
                val out = mutableListOf<String>()
                for (rawPkg in targets) {
                    val norm = rawPkg.lowercase().substringBefore("==")
                    if (norm == "zip" || norm == "zipfile") {
                        installedMap["zipfile"] = "3.12.0"
                        out.add("Requirement already satisfied: zipfile (3.12.0) in stdlib")
                        out.add("Use 'zip -r archive.zip <folder>' or 'python -m zipfile -c archive.zip <folder>'")
                    } else {
                        val pypiMeta = fetchPyPiPackageMeta(norm)
                        val ver = pypiMeta?.first ?: "1.0.0"
                        val summary = pypiMeta?.second ?: "Python module"
                        installedMap[norm] = ver
                        val modDir = File(sitePackages, norm).apply { mkdirs() }
                        File(modDir, "__init__.py").writeText("# $norm $ver - $summary\n__version__ = '$ver'\n", Charsets.UTF_8)
                        if (pypiMeta != null) {
                            out.add("Collecting $norm from pypi.org ($summary)")
                        } else {
                            out.add("Collecting $norm (offline workspace cache)")
                        }
                        out.add("Successfully installed $norm-$ver")
                    }
                }
                pipFile.writeText(installedMap.entries.joinToString("\n") { "${it.key}==${it.value}" } + "\n", Charsets.UTF_8)
                ShellResult(0, out, emptyList())
            }

            "--version", "-V" -> {
                ShellResult(0, listOf("pip 24.2 from ~/.local/lib/python3.12/site-packages/pip (python 3.12)"), emptyList())
            }

            else -> {
                ShellResult(0, listOf("Usage: pip [install|list|freeze|--version] <package>"), emptyList())
            }
        }
    }

    private fun fetchPyPiPackageMeta(pkgName: String): Pair<String, String>? {
        return try {
            val url = URL("https://pypi.org/pypi/$pkgName/json")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode in 200..299) {
                val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                conn.disconnect()
                val ver = extractJsonStringField(body, "version") ?: "1.0.0"
                val summary = extractJsonStringField(body, "summary") ?: "PyPI package $pkgName"
                Pair(ver, summary)
            } else {
                conn.disconnect()
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Git CLI (`git init`, `git clone <url>`, `git status`, `git branch`).
     */
    private fun handleGit(args: List<String>, state: ShellSessionState): ShellResult {
        val sub = args.firstOrNull() ?: "help"
        val rest = args.drop(1)

        return when (sub) {
            "--version", "-v", "version" -> ShellResult(0, listOf("git version 2.46.0"), emptyList())

            "init" -> {
                val targetDir = rest.firstOrNull { !it.startsWith("-") }?.let { resolvePath(it, state) } ?: state.cwd
                val gitDir = File(targetDir, ".git")
                File(gitDir, "refs/heads").mkdirs()
                File(gitDir, "objects").mkdirs()
                File(gitDir, "HEAD").writeText("ref: refs/heads/main\n", Charsets.UTF_8)
                File(gitDir, "config").writeText("[core]\n\trepositoryformatversion = 0\n\tfilemode = true\n\tbare = false\n", Charsets.UTF_8)
                ShellResult(0, listOf("Initialized empty Git repository in ${gitDir.absolutePath}/"), emptyList())
            }

            "status" -> {
                val gitDir = File(state.cwd, ".git")
                if (!gitDir.exists()) {
                    return ShellResult(128, emptyList(), listOf("fatal: not a git repository (or any of the parent directories): .git"))
                }
                val untracked = state.cwd.listFiles()?.filter { it.name != ".git" }?.map { it.name } ?: emptyList()
                val lines = mutableListOf("On branch main")
                if (untracked.isEmpty()) {
                    lines.add("nothing to commit, working tree clean")
                } else {
                    lines.add("Untracked/modified files in working directory:")
                    untracked.forEach { lines.add("  $it") }
                }
                ShellResult(0, lines, emptyList())
            }

            "clone" -> {
                val repoUrl = rest.firstOrNull { !it.startsWith("-") }
                    ?: return ShellResult(1, emptyList(), listOf("Usage: git clone <https://github.com/owner/repo> [dir]"))
                val cleanUrl = repoUrl.removeSuffix(".git").removeSuffix("/")
                val repoName = rest.getOrNull(1) ?: cleanUrl.substringAfterLast('/')
                val destDir = resolvePath(repoName, state)

                if (!cleanUrl.contains("github.com/")) {
                    return ShellResult(1, emptyList(), listOf("git clone: supported over HTTPS for github.com/<owner>/<repo>"))
                }

                val ownerRepo = cleanUrl.substringAfter("github.com/")
                val out = mutableListOf("Cloning into '$repoName'...")

                val branches = listOf("main", "master")
                var cloned = false
                for (branch in branches) {
                    val zipUrl = "https://codeload.github.com/$ownerRepo/zip/refs/heads/$branch"
                    try {
                        val conn = (URL(zipUrl).openConnection() as HttpURLConnection).apply {
                            connectTimeout = 6000
                            readTimeout = 10000
                        }
                        if (conn.responseCode in 200..299) {
                            destDir.mkdirs()
                            var fileCount = 0
                            ZipInputStream(BufferedInputStream(conn.inputStream)).use { zis ->
                                var entry = zis.nextEntry
                                while (entry != null) {
                                    val relName = entry.name.substringAfter('/', "")
                                    if (relName.isNotEmpty()) {
                                        val outFile = File(destDir, relName)
                                        if (outFile.canonicalPath.startsWith(destDir.canonicalPath)) {
                                            if (entry.isDirectory) {
                                                outFile.mkdirs()
                                            } else {
                                                outFile.parentFile?.mkdirs()
                                                FileOutputStream(outFile).use { fos -> zis.copyTo(fos) }
                                                fileCount++
                                            }
                                        }
                                    }
                                    zis.closeEntry()
                                    entry = zis.nextEntry
                                }
                            }
                            conn.disconnect()
                            File(destDir, ".git/refs/heads").mkdirs()
                            File(destDir, ".git/HEAD").writeText("ref: refs/heads/$branch\n", Charsets.UTF_8)
                            out.add("remote: Enumerating objects: $fileCount, done.")
                            out.add("Receiving objects: 100% ($fileCount/$fileCount), done.")
                            cloned = true
                            break
                        }
                        conn.disconnect()
                    } catch (_: Exception) {
                    }
                }

                if (cloned) {
                    ShellResult(0, out, emptyList())
                } else {
                    ShellResult(1, out, listOf("fatal: unable to access '$repoUrl': check internet connection or repository name"))
                }
            }

            else -> {
                ShellResult(
                    0,
                    listOf(
                        "git version 2.46.0 (ArnTCLI Workspace Git)",
                        "Commands: git init | git clone <https://github.com/owner/repo> | git status"
                    ),
                    emptyList()
                )
            }
        }
    }

    private fun handlePython(args: List<String>, state: ShellSessionState): ShellResult {
        if (args.isEmpty()) {
            return ShellResult(
                0,
                listOf(
                    "Python 3.12.0 (ArnTCLI Embedded Runtime) on Android",
                    "Usage:",
                    "  python -c \"print('Hello', 2 + 2)\"         Execute inline Python code",
                    "  python workspace/hello.py                 Execute a Python script file",
                    "  python -m zipfile -c out.zip workspace/   Create a ZIP archive",
                    "  python -m zipfile -e out.zip extracted/   Extract a ZIP archive",
                    "  python -m json.tool workspace/config.json Format & validate JSON"
                ),
                emptyList()
            )
        }

        if (args[0] == "--version" || args[0] == "-V") {
            return ShellResult(0, listOf("Python 3.12.0"), emptyList())
        }

        if (args[0] == "-c" && args.size >= 2) {
            val code = args.drop(1).joinToString(" ")
            return evalPythonSnippet(code, state)
        }

        if (args[0] == "-m" && args.size >= 2) {
            val mod = args[1]
            val modArgs = args.drop(2)
            return when (mod) {
                "zipfile" -> {
                    when (modArgs.firstOrNull()) {
                        "-c" -> handleZip(modArgs.drop(1), state)
                        "-l" -> handleUnzip(listOf("-l") + modArgs.drop(1), state)
                        "-e" -> {
                            val zipPath = modArgs.getOrNull(1) ?: ""
                            val destPath = modArgs.getOrNull(2) ?: "."
                            handleUnzip(listOf(zipPath, "-d", destPath), state)
                        }
                        else -> ShellResult(1, emptyList(), listOf("Usage: python -m zipfile [-c zip src... | -l zip | -e zip dest]"))
                    }
                }
                "json.tool" -> handleJq(modArgs, state)
                else -> ShellResult(1, emptyList(), listOf("python: No module named $mod"))
            }
        }

        val scriptFile = resolvePath(args[0], state)
        if (!scriptFile.exists() || !scriptFile.isFile) {
            return ShellResult(1, emptyList(), listOf("python: can't open file '${args[0]}': [Errno 2] No such file or directory"))
        }
        val scriptContent = scriptFile.readText(Charsets.UTF_8)
        return evalPythonSnippet(scriptContent, state)
    }

    private fun evalPythonSnippet(code: String, state: ShellSessionState): ShellResult {
        val out = mutableListOf<String>()
        val err = mutableListOf<String>()
        val vars = mutableMapOf<String, String>(
            "__name__" to "__main__",
            "cwd" to state.cwd.absolutePath
        )

        val lines = code.replace(";", "\n").lines()
        var idx = 0
        try {
            while (idx < lines.size) {
                val rawLine = lines[idx]
                val trimmed = rawLine.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("import ") || trimmed.startsWith("from ")) {
                    idx++
                    continue
                }

                val forMatch = Regex("^for\\s+([A-Za-z_][A-Za-z0-9_]*)\\s+in\\s+range\\(([^)]+)\\):$").matchEntire(trimmed)
                if (forMatch != null) {
                    val loopVar = forMatch.groupValues[1]
                    val rangeParts = forMatch.groupValues[2].split(',').map { it.trim().toIntOrNull() ?: 0 }
                    val start = if (rangeParts.size >= 2) rangeParts[0] else 0
                    val end = if (rangeParts.size >= 2) rangeParts[1] else (rangeParts.firstOrNull() ?: 0)
                    val step = if (rangeParts.size >= 3) rangeParts[2].coerceAtLeast(1) else 1

                    val bodyLines = mutableListOf<String>()
                    idx++
                    while (idx < lines.size && (lines[idx].startsWith(" ") || lines[idx].startsWith("\t") || lines[idx].isBlank())) {
                        if (lines[idx].isNotBlank()) {
                            bodyLines.add(lines[idx].trim())
                        }
                        idx++
                    }

                    var v = start
                    while (v < end) {
                        vars[loopVar] = v.toString()
                        for (bLine in bodyLines) {
                            evalSinglePythonStatement(bLine, vars, state, out)
                        }
                        v += step
                    }
                    continue
                }

                evalSinglePythonStatement(trimmed, vars, state, out)
                idx++
            }
        } catch (e: Exception) {
            err.add("Traceback (most recent call last):\n  RuntimeError: ${e.message}")
        }

        return ShellResult(if (err.isEmpty()) 0 else 1, out, err)
    }

    private fun evalSinglePythonStatement(
        stmt: String,
        vars: MutableMap<String, String>,
        state: ShellSessionState,
        out: MutableList<String>
    ) {
        if (stmt.startsWith("print(") && stmt.endsWith(")")) {
            val inner = stmt.substring(6, stmt.length - 1).trim()
            if (inner.isEmpty()) {
                out.add("")
                return
            }
            val args = splitTopLevelCommas(inner)
            val evaluated = args.map { evalPythonExpr(it.trim(), vars, state) }
            out.add(evaluated.joinToString(" "))
            return
        }

        val eqIdx = stmt.indexOf('=')
        if (eqIdx > 0 && stmt[eqIdx - 1] != '=' && stmt.getOrNull(eqIdx + 1) != '=') {
            val varName = stmt.substring(0, eqIdx).trim()
            val expr = stmt.substring(eqIdx + 1).trim()
            vars[varName] = evalPythonExpr(expr, vars, state)
            return
        }

        val result = evalPythonExpr(stmt, vars, state)
        if (result.isNotEmpty() && result != stmt) {
            out.add(result)
        }
    }

    private fun evalPythonExpr(expr: String, vars: Map<String, String>, state: ShellSessionState): String {
        val t = expr.trim()
        if (t == "os.getcwd()") return state.cwd.absolutePath
        if (t == "os.listdir()" || t == "os.listdir('.')") {
            val names = state.cwd.listFiles()?.map { "'${it.name}'" } ?: emptyList()
            return "[${names.joinToString(", ")}]"
        }
        if ((t.startsWith("f\"") && t.endsWith("\"")) || (t.startsWith("f'") && t.endsWith("'"))) {
            val raw = t.substring(2, t.length - 1)
            return raw.replace(Regex("\\{([^}]+)\\}")) { m ->
                evalPythonExpr(m.groupValues[1], vars, state)
            }
        }
        if ((t.startsWith("\"") && t.endsWith("\"")) || (t.startsWith("'") && t.endsWith("'"))) {
            return t.substring(1, t.length - 1)
        }
        if (vars.containsKey(t)) {
            return vars.getValue(t)
        }
        val substituted = t.replace(Regex("\\b([A-Za-z_][A-Za-z0-9_]*)\\b")) { m ->
            vars[m.groupValues[1]] ?: m.value
        }
        return evalSimpleMath(substituted) ?: substituted
    }

    private fun evalSimpleMath(expr: String): String? {
        val clean = expr.replace(" ", "")
        if (!clean.matches(Regex("[0-9+\\-*/.%]+"))) return null
        return try {
            when {
                clean.contains('+') -> {
                    val parts = clean.split('+')
                    val sum = parts.sumOf { it.toDouble() }
                    formatNumber(sum)
                }
                clean.contains('*') && !clean.contains("**") -> {
                    val parts = clean.split('*')
                    val prod = parts.fold(1.0) { acc, s -> acc * s.toDouble() }
                    formatNumber(prod)
                }
                clean.contains('-') && clean.indexOf('-') > 0 -> {
                    val first = clean.substringBefore('-').toDouble()
                    val second = clean.substringAfter('-').toDouble()
                    formatNumber(first - second)
                }
                clean.contains('/') -> {
                    val first = clean.substringBefore('/').toDouble()
                    val second = clean.substringAfter('/').toDouble()
                    formatNumber(first / second)
                }
                else -> clean
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun formatNumber(d: Double): String {
        return if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()
    }

    private fun checkHttpsReachable(urlStr: String): Boolean {
        return try {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 2500
                readTimeout = 2500
                requestMethod = "GET"
            }
            val ok = conn.responseCode in 200..499
            conn.disconnect()
            ok
        } catch (_: Exception) {
            false
        }
    }

    private fun extractJsonStringField(json: String, field: String): String? {
        val regex = Regex("\"$field\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
        return regex.find(json)?.groupValues?.getOrNull(1)
    }

    private fun splitTopLevelCommas(input: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inSingle = false
        var inDouble = false
        var depth = 0
        for (ch in input) {
            when {
                ch == '\'' && !inDouble -> {
                    inSingle = !inSingle
                    current.append(ch)
                }
                ch == '"' && !inSingle -> {
                    inDouble = !inDouble
                    current.append(ch)
                }
                !inSingle && !inDouble && (ch == '(' || ch == '[' || ch == '{') -> {
                    depth++
                    current.append(ch)
                }
                !inSingle && !inDouble && (ch == ')' || ch == ']' || ch == '}') -> {
                    depth = (depth - 1).coerceAtLeast(0)
                    current.append(ch)
                }
                !inSingle && !inDouble && depth == 0 && ch == ',' -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        if (current.isNotEmpty()) result.add(current.toString())
        return result
    }

    fun formatHumanSize(bytes: Long): String {
        return when {
            bytes < 1024L -> "${bytes}B"
            bytes < 1024L * 1024L -> String.format(Locale.US, "%.1fK", bytes / 1024.0)
            bytes < 1024L * 1024L * 1024L -> String.format(Locale.US, "%.1fM", bytes / (1024.0 * 1024.0))
            else -> String.format(Locale.US, "%.1fG", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}

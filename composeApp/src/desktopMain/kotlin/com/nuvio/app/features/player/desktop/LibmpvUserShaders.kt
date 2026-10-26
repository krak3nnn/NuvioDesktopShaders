package com.nuvio.app.features.player.desktop

import co.touchlab.kermit.Logger
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readText
import kotlin.io.path.writeText

// Fork-only: shaders the user drops into [root] (%APPDATA%\Nuvio\shaders). Every .glsl/.hook with a
// //!HOOK is a preset, in that folder or one level of subfolders. Texture-only files aren't listed;
// they are loaded by the shaders that bind them.
internal class LibmpvUserShaders(
    private val root: Path,
    // Where rgb8 textures are rewritten as rgba8 copies.
    private val convertedDir: Path,
    // Used when nothing in the folder defines a shader's bound textures.
    private val fallbackTextures: LibmpvShaderTextures,
) {
    fun scan(): List<LibmpvShaderPreset> = runCatching {
        Files.createDirectories(root)
        val subfolders = entries(root).filter { it.isDirectory() }
        scanFolder(root, folder = null) + subfolders.flatMap { scanFolder(it, folder = it.name) }
    }.getOrElse { error ->
        log.w(error) { "scanning user shaders failed root=$root" }
        emptyList()
    }

    fun scanFolder(dir: Path, folder: String?): List<LibmpvShaderPreset> = runCatching {
        val files = entries(dir).filter { it.isRegularFile() }
        val textures = files.filter { it.extension.lowercase() in SHADER_EXTENSIONS }.mapNotNull { file ->
            val names = LibmpvShaderFacts.of(file)?.textures.orEmpty()
            if (names.isEmpty()) null else LibmpvShaderTextures(names) { file }
        }
        files.mapNotNull { preset(it, folder, textures) }
    }.getOrDefault(emptyList())

    private fun entries(dir: Path): List<Path> = dir.listDirectoryEntries().sortedBy { it.name.lowercase() }

    private fun preset(file: Path, folder: String?, available: List<LibmpvShaderTextures>): LibmpvShaderPreset? {
        val extension = file.extension.lowercase()
        val id = ID_PREFIX + (folder?.let { "$it/" }.orEmpty()) + file.name
        val name = file.nameWithoutExtension + (folder?.let { " ($it)" }.orEmpty())
        if (extension in RETROARCH_PRESETS) {
            return LibmpvShaderPreset(id, name, user = true, LibmpvShaderRenderer.GpuNext, problem = "RetroArch preset, not an mpv shader")
        }
        if (extension !in SHADER_EXTENSIONS) return null
        val facts = LibmpvShaderFacts.of(file) ?: return null
        if (!facts.hasHook) {
            if (facts.hasTexture) return null
            if (facts.retroArch) {
                return LibmpvShaderPreset(id, name, user = true, LibmpvShaderRenderer.GpuNext, problem = "RetroArch shader, not an mpv shader")
            }
        }
        val renderer = if (facts.gpuNext) LibmpvShaderRenderer.GpuNext else LibmpvShaderRenderer.Gpu
        val missing = facts.binds - facts.textures - facts.saves - MPV_BUILTIN_TEXTURES
        // mpv skips passes whose bound textures don't exist, so files that define them load first:
        // X-textures.glsl, then any other file in the folder, then the fallback.
        val companion = file.resolveSibling(file.nameWithoutExtension + TEXTURES_SUFFIX + "." + file.extension)
        val candidates = available.sortedByDescending { it.path() == companion } + fallbackTextures
        val needed = mutableListOf<LibmpvShaderTextures>()
        var unresolved = missing
        for (textures in candidates) {
            if (unresolved.isEmpty()) break
            if (textures.path() == file || textures.names.none { it in unresolved }) continue
            needed += textures
            unresolved = unresolved - textures.names
        }
        if (unresolved.isNotEmpty()) {
            val problem = "Missing textures: ${unresolved.sorted().joinToString(", ")}"
            return LibmpvShaderPreset(id, name, user = true, renderer, problem = problem)
        }
        return LibmpvShaderPreset(id, name, user = true, renderer) { (needed.map { it.path() } + listOf(file)).map(::prepare) }
    }

    // Direct3D 11 can't load rgb8 textures, so such files are swapped for converted copies.
    private fun prepare(file: Path): Path {
        if (LibmpvShaderFacts.of(file)?.rgb8 != true) return file
        val converted = LibmpvShaderTextureConverter.rgb8ToRgba8(file.readText()) ?: return file
        val relative = root.toAbsolutePath().relativize(file.toAbsolutePath())
        val target = convertedDir.resolve(if (relative.startsWith("..")) file.fileName else relative)
        Files.createDirectories(target.parent)
        target.writeText(converted)
        return target
    }

    private companion object {
        val log = Logger.withTag("LibmpvUserShaders")
        const val ID_PREFIX = "user:"
        const val TEXTURES_SUFFIX = "-textures"
        val SHADER_EXTENSIONS = setOf("glsl", "hook")
        val RETROARCH_PRESETS = setOf("glslp", "slangp", "slang", "cgp")

        // Textures mpv provides to every user shader.
        val MPV_BUILTIN_TEXTURES = setOf(
            "HOOKED", "MAIN", "NATIVE", "RGB", "LUMA", "CHROMA", "ALPHA", "XYZ", "CHROMA_SCALED", "ALPHA_SCALED",
            "LINEAR", "SIGMOID", "PREKERNEL", "POSTKERNEL", "SCALED", "PREOUTPUT", "OUTPUT",
        )
    }
}

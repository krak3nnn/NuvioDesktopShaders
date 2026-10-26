package com.nuvio.app.features.player.desktop

import co.touchlab.kermit.Logger
import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.features.player.LibmpvShaderSettings
import com.nuvio.app.isWindows
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.nio.file.Files
import java.nio.file.Path

// Fork-only: wires mpv user shaders into the Windows player. Called from three upstream hooks:
// before the player attaches (options for the next mpv instance), on controls-page events (the shader
// menu) and when controls state is sent to the page (menu contents).
internal object DesktopLibmpvShaders {
    private const val TOGGLE_EVENT = "shadersToggle"
    private const val SELECT_EVENT = "shadersSelect"
    private val log = Logger.withTag("DesktopLibmpvShaders")

    // mpv's glsl-shaders option is a path list; on Windows the separator is ';'.
    private const val MPV_PATH_LIST_SEPARATOR = ";"

    private val userDir: Path get() = DesktopStorage.rootDir.resolve(LIBMPV_SHADERS_DIR)
    private val cacheDir: Path get() = DesktopStorage.cacheDir.resolve(LIBMPV_SHADERS_DIR)
    private val logFile: Path get() = cacheDir.resolve("mpv-log.txt")

    private val userShaders by lazy { LibmpvUserShaders(userDir, cacheDir.resolve("user"), LibmpvBundledShaders.guestTextures) }

    // Both hooks are called on UI threads (Compose main dispatcher, Swing event thread), and preparing
    // shaders touches the disk, so the work runs here. One worker keeps live switches in order.
    private val workScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

    @Volatile
    private var presets: List<LibmpvShaderPreset> = LibmpvBundledShaders.presets

    // Renderer of the running player, so live switches only reinitialize mpv's video output when needed.
    @Volatile
    private var liveRenderer = LibmpvShaderRenderer.GpuNext

    suspend fun applyBeforeAttach() {
        if (!isWindows) return
        withContext(workScope.coroutineContext) { configureNextPlayer() }
    }

    private fun configureNextPlayer() {
        val enabled = LibmpvShaderSettings.enabled.value
        if (enabled) presets = LibmpvBundledShaders.presets + userShaders.scan()
        val preset = activePreset()
        liveRenderer = preset?.renderer ?: LibmpvShaderRenderer.GpuNext
        val logPath = if (enabled) runCatching { Files.createDirectories(cacheDir); logFile.toString() }.getOrDefault("") else ""
        runCatching {
            NativePlayerBridge.ensureLoaded()
            // Empty vo keeps upstream's renderer choice.
            NativeShaderBridge.configure(preset?.renderer?.vo.orEmpty(), shaderList(preset), logPath)
            log.d { "configured next player enabled=$enabled preset=${preset?.id} renderer=${preset?.renderer} presets=${presets.size}" }
        }.onFailure { error ->
            log.w(error) { "configure failed preset=${preset?.id}" }
        }
    }

    // Returns true when the controls page event belongs to the shader menu.
    fun handleControlEvent(handle: Long, type: String, value: Double): Boolean {
        if (type != TOGGLE_EVENT && type != SELECT_EVENT) return false
        if (!isWindows || !LibmpvShaderSettings.enabled.value) return true
        when (type) {
            TOGGLE_EVENT -> LibmpvShaderSettings.setActive(value != 0.0)
            SELECT_EVENT -> {
                val preset = presets.getOrNull(value.toInt())?.takeIf { it.usable } ?: return true
                LibmpvShaderSettings.setSelectedId(preset.id)
                LibmpvShaderSettings.setActive(true)
            }
        }
        if (handle != 0L) workScope.launch { applyLive(handle) }
        return true
    }

    // Adds the shader menu state to the controls JSON; unchanged when shader support is off.
    fun decorateControlsJson(json: String): String {
        if (!isWindows || !LibmpvShaderSettings.enabled.value) return json
        val end = json.lastIndexOf('}')
        if (end < 0) return json
        val current = presets
        val state = buildJsonObject {
            put("active", LibmpvShaderSettings.active.value)
            put("selected", current.indexOf(selectedPreset()))
            put("userFolder", userDir.toString())
            put("logFile", logFile.toString())
            putJsonArray("presets") {
                current.forEach { preset ->
                    addJsonObject {
                        put("name", preset.name)
                        put("user", preset.user)
                        preset.problem?.let { put("problem", it) }
                    }
                }
            }
        }
        val separator = if (json.substring(0, end).trimEnd().endsWith('{')) "" else ","
        return json.substring(0, end) + separator + "\"shaders\":" + state + json.substring(end)
    }

    private fun applyLive(handle: Long) {
        val preset = activePreset()
        // Turning shaders off keeps the current renderer; picking one switches to the renderer it needs.
        val renderer = preset?.renderer?.takeIf { it != liveRenderer }
        runCatching { NativeShaderBridge.setLiveShaders(handle, renderer?.vo.orEmpty(), shaderList(preset)) }
            .onSuccess {
                if (renderer != null) liveRenderer = renderer
                log.d { "live switch handle=$handle preset=${preset?.id} renderer=$liveRenderer" }
            }
            .onFailure { error -> log.w(error) { "live switch failed handle=$handle preset=${preset?.id}" } }
    }

    private fun activePreset(): LibmpvShaderPreset? =
        if (LibmpvShaderSettings.enabled.value && LibmpvShaderSettings.active.value) selectedPreset() else null

    private fun selectedPreset(): LibmpvShaderPreset {
        val id = LibmpvShaderSettings.selectedId.value
        return presets.firstOrNull { it.id == id && it.usable } ?: LibmpvBundledShaders.default
    }

    // Value for mpv's glsl-shaders option; empty disables shaders.
    private fun shaderList(preset: LibmpvShaderPreset?): String {
        if (preset == null) return ""
        return runCatching { preset.files.joinToString(MPV_PATH_LIST_SEPARATOR) { it.toAbsolutePath().toString() } }
            .getOrElse { error ->
                log.w(error) { "preparing shader files failed preset=${preset.id}" }
                ""
            }
    }
}

internal object NativeShaderBridge {
    // Implemented in native/windows/player_shaders.h: applied when the next player starts.
    // Empty vo keeps upstream's renderer; empty shaders disables them; empty logFile disables the mpv log.
    external fun configure(vo: String, shaders: String, logFile: String)

    // Implemented in native/windows/player_shaders_live.inc: switches shaders on a running player,
    // changing mpv's video output first when vo is not empty.
    external fun setLiveShaders(handle: Long, vo: String, shaders: String)
}

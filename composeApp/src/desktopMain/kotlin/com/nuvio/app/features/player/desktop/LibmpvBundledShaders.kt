package com.nuvio.app.features.player.desktop

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.features.player.desktop.LibmpvShaderRenderer.Gpu
import com.nuvio.app.features.player.desktop.LibmpvShaderRenderer.GpuNext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

// Fork-only: the shaders shipped in resources/shaders (licenses in resources/licenses/mpv-retro-shaders.txt).
internal object LibmpvBundledShaders {
    private const val GUEST_TEXTURES = "crt-guest-advanced-ntsc-textures.glsl"

    private val installDir: Path get() = DesktopStorage.cacheDir.resolve(LIBMPV_SHADERS_DIR)
    private val installed = mutableSetOf<String>()

    // The first entry is the default. Load order matters for multi-file presets: textures before the
    // shader that binds them.
    val presets: List<LibmpvShaderPreset> = listOf(
        preset("mattias", "CRT Mattias", GpuNext, "crt-mattias.glsl"),
        preset("guest-ntsc", "CRT Guest Advanced NTSC", Gpu, GUEST_TEXTURES, "crt-guest-advanced-ntsc.glsl"),
        preset("lottes", "CRT Lottes", Gpu, "crt-lottes.glsl"),
        preset("hyllian", "CRT Hyllian", GpuNext, "crt-hyllian.glsl"),
        preset("aperture", "CRT Aperture", GpuNext, "crt-aperture.glsl"),
        preset("royale", "CRT Royale Kurozumi", GpuNext, "crt-royale-kurozumi.glsl"),
    )

    val default: LibmpvShaderPreset get() = presets.first()

    // Guest NTSC's LUTs, for users who copy only the main crt-guest-advanced-ntsc.glsl into their folder.
    val guestTextures: LibmpvShaderTextures by lazy {
        LibmpvShaderTextures(LibmpvShaderFacts.read(install(GUEST_TEXTURES)).textures) { install(GUEST_TEXTURES) }
    }

    private fun preset(id: String, name: String, renderer: LibmpvShaderRenderer, vararg resources: String) =
        LibmpvShaderPreset(id, name, user = false, renderer) { resources.map(::install) }

    // mpv needs real file paths, so resources are extracted once per app run.
    private fun install(name: String): Path {
        val target = installDir.resolve(name)
        synchronized(installed) {
            if (name !in installed || !Files.exists(target)) {
                Files.createDirectories(target.parent)
                val stream = LibmpvBundledShaders::class.java.getResourceAsStream("/$LIBMPV_SHADERS_DIR/$name")
                    ?: error("Missing shader resource: $name")
                stream.use { Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING) }
                installed += name
            }
        }
        return target
    }
}

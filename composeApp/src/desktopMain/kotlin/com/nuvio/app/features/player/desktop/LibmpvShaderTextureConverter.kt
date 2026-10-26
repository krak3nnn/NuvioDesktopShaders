package com.nuvio.app.features.player.desktop

// Fork-only: Direct3D 11 (mpv's gpu renderer on Windows) has no 3-channel 8-bit texture format, so
// //!TEXTURE blocks declared as rgb8 are rewritten as rgba8 with alpha = ff. Pixel values are unchanged.
internal object LibmpvShaderTextureConverter {
    private const val RGB8_FORMAT = "//!FORMAT rgb8"

    // Returns null if a texture's data isn't whole rgb8 pixels.
    fun rgb8ToRgba8(text: String): String? {
        val out = StringBuilder(text.length + text.length / 3)
        val hex = StringBuilder()
        var inTexture = false
        var rgb8 = false
        var failed = false
        fun flushHex() {
            if (hex.isEmpty()) return
            if (hex.length % 6 != 0) failed = true
            for (index in 0 until hex.length step 6) out.append(hex, index, minOf(index + 6, hex.length)).append("ff")
            out.append('\n')
            hex.clear()
        }
        for (line in text.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("//!")) {
                flushHex()
                if (trimmed.startsWith("//!TEXTURE")) {
                    inTexture = true
                    rgb8 = false
                } else if (!inTexture || trimmed.startsWith("//!HOOK") || trimmed.startsWith("//!PARAM") || trimmed.startsWith("//!BUFFER")) {
                    inTexture = false
                    rgb8 = false
                }
                if (inTexture && trimmed == RGB8_FORMAT) {
                    rgb8 = true
                    out.append("//!FORMAT rgba8\n")
                    continue
                }
                out.append(line).append('\n')
            } else if (inTexture && rgb8 && trimmed.isNotEmpty() && trimmed.all { it.isLetterOrDigit() }) {
                hex.append(trimmed)
            } else {
                flushHex()
                out.append(line).append('\n')
            }
        }
        flushHex()
        return if (failed) null else out.toString()
    }
}

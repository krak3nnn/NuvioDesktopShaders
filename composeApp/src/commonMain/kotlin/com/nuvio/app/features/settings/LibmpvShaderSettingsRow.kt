package com.nuvio.app.features.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.features.player.LibmpvShaderSettings

// Fork-only: strings are inline to avoid touching upstream strings.xml.
@Composable
internal fun LibmpvShaderSettingsRow(isTablet: Boolean) {
    val shadersEnabled by LibmpvShaderSettings.enabled.collectAsStateWithLifecycle()
    SettingsGroupDivider(isTablet = isTablet)
    SettingsSwitchRow(
        title = "Shader support",
        description = "Adds a shader button to the player to toggle and pick retro CRT shaders. Each shader switches the renderer to the one it needs.",
        checked = shadersEnabled,
        isTablet = isTablet,
        onCheckedChange = LibmpvShaderSettings::setEnabled,
    )
}

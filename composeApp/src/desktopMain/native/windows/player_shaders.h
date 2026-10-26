// mpv user shader support for the Windows player bridge. Kept out of player_bridge.cpp so upstream
// syncs only touch a few hook lines. Live switching on a running player lives in player_shaders_live.inc.
#pragma once

#include <windows.h>
#include <jni.h>

#include <mutex>
#include <string>

namespace player_shaders {

struct Config {
    std::string videoOutput; // mpv vo list for the selected shader, or empty for upstream's choice
    std::string glslShaders; // ';'-separated paths for mpv's glsl-shaders option, or empty
    std::string logFile;     // mpv log path while shader support is on, or empty
};

inline std::mutex &configMutex() {
    static std::mutex mutex;
    return mutex;
}

inline Config &configValue() {
    static Config value;
    return value;
}

inline Config config() {
    std::lock_guard<std::mutex> lock(configMutex());
    return configValue();
}

// Called after the player's own options and before mpv_initialize, so a vo set here overrides the
// default. Each shader needs a specific renderer (gpu or gpu-next), chosen by DesktopLibmpvShaders.
template <typename SetOption>
inline void applyMpvOptions(SetOption setOption) {
    Config current = config();
    if (!current.videoOutput.empty()) setOption("vo", current.videoOutput.c_str());
    if (!current.glslShaders.empty()) setOption("glsl-shaders", current.glslShaders.c_str());
    if (!current.logFile.empty()) setOption("log-file", current.logFile.c_str());
}

inline std::string jstringToUtf8(JNIEnv *env, jstring value) {
    if (!value) return std::string();
    const jchar *chars = env->GetStringChars(value, nullptr);
    jsize length = env->GetStringLength(value);
    std::string result;
    if (chars && length > 0) {
        const auto *wide = reinterpret_cast<const wchar_t *>(chars);
        int size = WideCharToMultiByte(CP_UTF8, 0, wide, length, nullptr, 0, nullptr, nullptr);
        if (size > 0) {
            result.assign(size, '\0');
            WideCharToMultiByte(CP_UTF8, 0, wide, length, result.data(), size, nullptr, nullptr);
        }
    }
    if (chars) env->ReleaseStringChars(value, chars);
    return result;
}

} // namespace player_shaders

extern "C" JNIEXPORT void JNICALL
Java_com_nuvio_app_features_player_desktop_NativeShaderBridge_configure(
    JNIEnv *env, jobject, jstring videoOutput, jstring shaders, jstring logFile
) {
    player_shaders::Config value;
    value.videoOutput = player_shaders::jstringToUtf8(env, videoOutput);
    value.glslShaders = player_shaders::jstringToUtf8(env, shaders);
    value.logFile = player_shaders::jstringToUtf8(env, logFile);
    std::lock_guard<std::mutex> lock(player_shaders::configMutex());
    player_shaders::configValue() = value;
}

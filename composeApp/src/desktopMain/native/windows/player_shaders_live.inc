// Switches mpv user shaders on a running player. Included at the end of player_bridge.cpp, where
// WindowsMpvWebPlayer and playerFromHandle are visible.

extern "C" JNIEXPORT void JNICALL
Java_com_nuvio_app_features_player_desktop_NativeShaderBridge_setLiveShaders(
    JNIEnv *env, jobject, jlong handle, jstring videoOutput, jstring shaders
) {
    auto player = playerFromHandle(handle);
    if (!player) return;
    std::string vo = player_shaders::jstringToUtf8(env, videoOutput);
    if (!vo.empty()) {
        // Clear first so the old renderer never compiles shaders meant for the new one.
        player->setShaderProperty("glsl-shaders", "");
        player->setShaderProperty("vo", vo);
    }
    player->setShaderProperty("glsl-shaders", player_shaders::jstringToUtf8(env, shaders));
}

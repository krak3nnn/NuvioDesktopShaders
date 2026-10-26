<div align="center">

  <img src="https://nuvio.tv/assets/nuvio-app-logo-wordmark.webp" alt="Nuvio" width="320" />

  <p>
    A free, open-source media app for Windows, macOS, and Linux.
    <br />
    Bring your own sources. Nuvio turns them into a library with artwork, ratings, subtitles, and your place saved on every screen.
  </p>

  [Website](https://nuvio.tv) · [GitHub releases](https://github.com/NuvioMedia/NuvioDesktop/releases/latest) · [Support Nuvio](https://nuvio.tv/support)

</div>

## The Nuvio Desktop app you're familiar with but with GLSL shader support.

### How to Enable Shaders

Turn on **Shader support** under Settings → Playback and a shader button appears in the player, where you can switch shaders on and off and pick one while the video plays.

### Custom Shaders

Drop your own `.glsl` into `%APPDATA%\Nuvio\shaders` and they show up in player's shader drop-down menu. RetroArch `.slangp` and `.glslp` presets are not supported.

The following shaders are packaged with the app:
- **CRT Mattias** (default) – Mattias' "CRT Emulation" from [Shadertoy](https://www.shadertoy.com/view/lsB3DV),
  ported to a single mpv pass from the libretro `crt-mattias` preset. CC BY-NC-SA 3.0.
- **CRT Guest Advanced NTSC** – guest(r) and Dr. Venom's shader from
  [libretro slang-shaders](https://github.com/libretro/slang-shaders), mpv port from
  [mpv-retro-shaders](https://github.com/hhirtz/mpv-retro-shaders). GPL-2.0-or-later.
- **CRT Lottes** – Timothy Lottes' public-domain CRT shader, via mpv-retro-shaders.
- **CRT Hyllian** – Hyllian's shader from libretro slang-shaders, via mpv-retro-shaders. MIT.
- **CRT Aperture** – EasyMode's shader from libretro slang-shaders, via mpv-retro-shaders. GPL.
- **CRT Royale Kurozumi** – TroggleMonkey's CRT Royale with Kurozumi's preset, from libretro
  slang-shaders, via mpv-retro-shaders. GPL-2.0-or-later.

Full license texts are in `composeApp/src/desktopMain/resources/licenses/mpv-retro-shaders.txt`

## GPU Support Tested

| GPU Vendor | Tested |
|:----------:|:------:|
| Nvidia     |        |
| AMD        |        |
| Intel      |        |

## ⚠️ Alpha Software - Slow Development - Testers Only

Nuvio Desktop is currently in alpha and is intended only for testers. It is under development and is not suitable for daily use.

Expect breaking changes with every update. Features, settings, stored data, and compatibility may change or stop working without notice. Do not rely on this build as your primary media app, and report any issues you encounter during testing.

## Get Nuvio Desktop

Download the latest build from [GitHub Releases](https://github.com/NuvioMedia/NuvioDesktop/releases/latest).

- Windows: MSI installer

## License

[GNU General Public License v3.0](./LICENSE)

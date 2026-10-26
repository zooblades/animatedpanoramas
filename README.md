# Animated Panorama
Fabric client mod by **nazarbatrider**. Lets a resource pack animate the title screen overlay (`panorama_overlay.png`) with a GIF or PNG frames.

## Which jar for which Minecraft version
| Minecraft | Jar | Java |
|---|---|---|
| 26.1, 26.2, 26.3 (one jar) | `animated-panorama-<ver>+mc26.1-26.3.jar` | 25 |
| 1.21.11 | `animated-panorama-<ver>+mc1.21.11.jar` | 21 |
| 1.21.2 - 1.21.10 (one jar) | `animated-panorama-<ver>+mc1.21.2-1.21.10.jar` | 21 |
| 1.21, 1.21.1 (one jar) | `animated-panorama-mc1.21.1-<ver>.jar` | 21 |

Requires Fabric Loader and Fabric API. Jars are built by GitHub Actions (Actions tab → latest run → Artifacts).
Jars are split where Minecraft or Fabric API changed in a way one jar cannot cover: 26.x is unobfuscated and needs Java 25; 1.21.11 removed the old Fabric resource reload API; 1.21.1 -> 1.21.2 changed how the overlay is drawn.

## Resource pack: GIF (easiest)
```
MyPack/assets/minecraft/textures/gui/title/background/panorama_overlay.gif
```
Frame timing comes from the GIF. Transparency is supported. To force a fixed speed, add
`panorama_overlay_animation.json` next to it: `{ "fps": 12 }`.

Size limit: width x height x frames <= ~64 million pixels (1920x1080 up to ~30 frames, 960x540 up to ~120).
A GIF that is too large is skipped and the reason is written to `latest.log`.

## Resource pack: one PNG with frames stacked vertically
Use the normal overlay file and tell the mod how many frames it contains:
```
assets/minecraft/textures/gui/title/background/
  panorama_overlay.png                    (frames stacked top to bottom, all the same height)
  panorama_overlay_animation.json
```
```json
{ "frames": 20, "fps": 12 }
```
- `frames`: number of frames. Or give `frame_height` (in pixels) instead and the mod counts the frames itself.
- Without `frames` / `frame_height` the PNG is treated as a normal static overlay.
- Image height should divide evenly by the frame count (leftover rows are ignored, with a warning in the log).
- Priority: GIF, then `panorama_overlay_0.png ...` frames, then the vertical strip.

## Resource pack: PNG frames
```
MyPack/assets/minecraft/textures/gui/title/background/
  panorama_overlay_0.png, panorama_overlay_1.png, ...   (consecutive from 0, no gaps)
  panorama_overlay_animation.json                       (optional, { "fps": 12 })
```
Default speed is 10 fps. If both a GIF and PNG frames exist, the GIF is used.
Animation length in seconds = frames / fps.

## Repo layout
- `common/` shared overlay code (Mojang names) used by `mc1_21_11` and `mc26`
- `mc26/` Minecraft 26.1 - 26.3
- `mc1_21_11/` Minecraft 1.21.11
- `mc1_21_2/` Minecraft 1.21.2 - 1.21.10
- `common_modern/` texture helper for 1.21.11 and 26.x
- `mc1_21_1/` Minecraft 1.21.1 (Yarn mappings)

Build one: `cd mc26 && gradle build` (Gradle 9.7.1, run on Java 25; `mc1_21_11` also needs Java 25 to run Loom but produces Java 21 bytecode).

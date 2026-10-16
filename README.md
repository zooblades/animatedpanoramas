# Animated Panorama
Fabric 1.21.1 client mod by **nazarbatrider**. Lets a resource pack animate `panorama_overlay.png` on the title screen.

## Resource pack layout
```
MyPack/
├─ pack.mcmeta                      (pack_format 34 for 1.21.1)
└─ assets/minecraft/textures/gui/title/background/
   ├─ panorama_overlay_0.png
   ├─ panorama_overlay_1.png
   ├─ panorama_overlay_2.png
   ├─ ...                           (consecutive numbers, starting at 0, no gaps)
   └─ panorama_overlay_animation.json   (optional)
```
`panorama_overlay_animation.json`:
```json
{ "fps": 12 }
```
- Default fps is 10 (range 0.1–120).
- Frames can be any resolution, all the same size is best. They are stretched over the screen, keep transparency (PNG alpha).
- Fewer than 2 frames, or no mod installed → vanilla `panorama_overlay.png` is used.
- Make frames from a GIF: `ffmpeg -i overlay.gif -start_number 0 panorama_overlay_%d.png`

## Build
`gradle build` (Gradle 8.10, Java 21) or push to GitHub, Actions builds the jar. Output: `build/libs/animated-panorama-1.0.0.jar`.

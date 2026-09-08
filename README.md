# JUST MX
Reproductor de video Android ultra-mínimo. Media3/ExoPlayer + FFmpeg (NextLib) +
buffer grande configurable + OpenSubtitles. Se abre como reproductor externo de Stremio
y reproduce URLs directas de Real-Debrid. Video en HW, audio universal por FFmpeg,
todos los audios/subtítulos desde el segundo 0.

## Build
CI (GitHub Actions) genera el APK en cada push a `main` (artifact) y en cada tag `vX.Y.Z`
(Release). Local: `./gradlew assembleDebug`.

## Licencia
GPL-3.0.
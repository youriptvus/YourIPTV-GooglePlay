# YourIPTV store graphics

The store icon, feature graphic and Android TV store banner are completed local
exports. They retain the exact silver/orange-gold YourIPTV wordmark from the
approved source artwork and the established dark styling. The surrounding
third-party poster collage is excluded from all newly prepared public graphics
and from the derived Play application icons/banners. Original source artwork is
preserved unchanged.

| Upload asset | Verified dimensions | Format | Purpose |
|---|---:|---|---|
| [App-Icon.png](App-Icon.png) | 512x512 | 32-bit PNG, alpha channel | Main store listing icon |
| [Feature-Graphic.png](Feature-Graphic.png) | 1024x500 | 24-bit PNG, no alpha | Feature graphic |
| [Android-TV-Banner.png](Android-TV-Banner.png) | 1280x720 | 24-bit PNG, no alpha | Android TV store banner |

These are store-upload formats from Google's [preview asset requirements](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en).
The banner above is separate from the application's packaged 320x180 launcher
banner. No Google Play badge, ranking, pricing claim or unlicensed channel art is
included. The feature graphic's display/play symbol is an illustration, not an
application screenshot.

## Real application screenshots

Only actual app captures belong in `Phone-Screenshots` and
`Android-TV-Screenshots`. Their device/build/capture provenance and hashes are in
`Screenshots-Provenance.json` once the current safe device run supplies them.

Android TV captures retain their complete original 1920x1080 UI without framing,
cropping, overlays or scaling. If a source PNG has an opaque alpha channel, only
its encoding is normalized to RGB24; every RGB pixel is checked unchanged.

The Samsung's native long-screen capture can exceed Google's maximum 2:1 ratio.
For these phone captures, the exporter adds only a narrow opaque dark canvas at
the short edges. Every original UI pixel remains at 1:1 scale and is verified
byte-for-byte in the framed region; no UI is cropped, stretched or invented.
Unchanged raw captures are retained under `Sources/Raw-Captures` as evidence, not
as upload-ready images. Minimum screenshot count, actual appearance and all
resulting dimensions/formats must pass the final asset validation.

There is no actual tablet or installed tablet emulator in the current environment.
[Tablet-Screenshots/README.md](Tablet-Screenshots/README.md) records this limitation;
no phone/TV capture is mislabeled as a tablet image.

## Provenance and reproduction

[Artwork-Provenance.json](Artwork-Provenance.json) records the unchanged source
SHA-256, precise central-wordmark extraction rectangle, output hashes and all 17
derived app-resource exports. Essential adaptive-icon lettering fits the central
66dp safe area. No image-generation model or reconstructed application UI is used.

Store artwork can be reproduced with installed Chrome/Node:

```powershell
node GooglePlay-Release-Ready/Graphics/Sources/export-store-assets.mjs
```

The optional `--app-resources` flag also regenerates the approved derived launcher
PNGs and packaged TV banners; use it only during a deliberate application rebuild.
It does not change the original source JPEG, Android XML or application code.

`Sources/export-screenshots.mjs` accepts a JSON manifest of visually reviewed,
public/synthetic captures. It performs faithful framing/format export and records
exact pixel-preservation checks. `Sources/png-format.mjs` performs lossless PNG
color-format encoding; it does not modify RGB pixel values.

Upload only the named store assets and selected device screenshots. `Sources`
contains reproduction material/raw evidence, not extra Play Console graphics.

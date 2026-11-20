> November 19 owner correction: use the original full collage app icon, not the cropped wordmark redesign. Original phone launcher PNGs are restored byte for byte, and TV fallback icons use those same official files. `Sources/Original-App-Icons` holds the independent restoration baseline. The 512x512 store icon preserves all original RGB pixels, with PNG32 serialization for Play. Supporting feature/banner graphics remain separate assets. This supersedes earlier icon-only claims below about excluding the collage or using a wordmark-only adaptive foreground. Asset validation passes 32 static and six screenshot checks. No rights clearance or Google approval is implied by an asset-format check.

> Current screenshots completed: three actual TV and three actual Samsung captures are present. Asset-Validation.json records32 static checks and6 screenshot checks passed. TV captures are1920x1080; Samsung captures are2400x1350 with added edge canvas only and original UI pixels preserved1:1. Exact source/capture hashes and QA scope are in Screenshots-Provenance.json. No physical tablet screenshot is supplied.

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
For these phone captures, the exporter adds an opaque dark presentation canvas
to produce 16:9 landscape or 9:16 portrait dimensions. Every original UI pixel
remains at 1:1 scale and is verified
byte-for-byte in the framed region; no UI is cropped, stretched or invented.
Unchanged raw captures are retained under `Sources/Raw-Captures` as evidence, not
as upload-ready images. Minimum screenshot count, actual appearance and all
resulting dimensions/formats must pass the final asset validation.

There is no actual tablet or installed tablet emulator in the current environment.
[Tablet-Screenshots/README.md](Tablet-Screenshots/README.md) records this limitation;
no phone/TV capture is mislabeled as a tablet image.

## Provenance and reproduction

[Artwork-Provenance.json](Artwork-Provenance.json) records the unchanged source
SHA-256, precise central-wordmark extraction rectangle, output hashes and all 27
derived app PNGs plus two TV adaptive XML aliases. Essential adaptive-icon lettering
fits the central 66dp safe area. No image-generation model or reconstructed
application UI is used.

The TV-qualified fallback icons use 80/120/160/240/320 pixels across the five
densities, meeting Android's [TV icon design minimums](https://developer.android.com/design/ui/tv/guides/system/tv-app-icon-guidelines).
Phone fallback dimensions remain standard, and TV API26+ retains the same adaptive
foreground/background definitions through television-qualified aliases.

Store artwork can be reproduced with installed Chrome/Node:

```powershell
node GooglePlay-Release-Ready/Graphics/Sources/export-store-assets.mjs
```

The optional `--app-resources` flag regenerates the main launcher PNGs and packaged
TV banners. `--tv-icons` regenerates TV-qualified fallback PNGs and copies the
existing adaptive definitions into the TV qualifier. Use these only during a
deliberate application rebuild. They do not change the original source JPEG,
theme or player/application code.

`Sources/export-screenshots.mjs` accepts a JSON manifest of visually reviewed,
public/synthetic captures. It performs faithful framing/format export and records
exact pixel-preservation checks. `Sources/png-format.mjs` performs lossless PNG
color-format encoding; it does not modify RGB pixel values.

Run `py GooglePlay-Release-Ready/Graphics/Sources/validate-assets.py` to verify
actual dimensions, PNG color format, checksums, adaptive aliases and available
current screenshot metadata. `Asset-Validation.json` contains the result.

Upload only the named store assets and selected device screenshots. `Sources`
contains reproduction material/raw evidence, not extra Play Console graphics.

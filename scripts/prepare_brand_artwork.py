"""Create proportional Android/Play exports from the owner's unchanged PNG masters."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def contain(image, size, *, transparent=False):
    scale = min(size[0] / image.width, size[1] / image.height)
    fitted = image.resize((max(1, int(image.width * scale)), max(1, int(image.height * scale))), Image.Resampling.LANCZOS)
    canvas = Image.new('RGBA' if transparent else 'RGB', size, (0, 0, 0, 0) if transparent else '#05080d')
    canvas.paste(fitted, ((size[0] - fitted.width) // 2, (size[1] - fitted.height) // 2))
    return canvas


def prepare(source, root=ROOT):
    names = ['YourIPTV_App_Icon_1024.png', 'YourIPTV_App_Icon_512.png',
             'YourIPTV_Wallpaper_Background.png', 'YourIPTV_Extracted_Banner.png']
    images = {}
    for name in names:
        with Image.open(source / name) as loaded:
            images[name] = loaded.convert('RGBA')
    icon = images[names[0]]
    wallpaper = images[names[2]]
    banner = images[names[3]]
    assert icon.size == (1024, 1024) and images[names[1]].size == (512, 512)
    res = root / 'app/src/main/res'
    files = []
    for density, size in [('mdpi', 48), ('hdpi', 72), ('xhdpi', 96), ('xxhdpi', 144), ('xxxhdpi', 192)]:
        square = contain(icon, (size, size), transparent=True)
        round_icon = square.copy()
        mask = Image.new('L', (size, size), 0)
        ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
        round_icon.putalpha(mask)
        for prefix in ('mipmap-', 'mipmap-television-'):
            directory = res / (prefix + density)
            directory.mkdir(parents=True, exist_ok=True)
            for name, image in [('ic_launcher.png', square), ('ic_launcher_round.png', round_icon)]:
                path = directory / name
                image.save(path)
                files.append(path)
        # Android clips a 72dp viewport from a 108dp foreground. Keep the complete
        # square master within the central 66dp safe area so all title letters survive masks.
        side = round(size * 108 / 48)
        safe = round(side * 66 / 108)
        foreground = Image.new('RGBA', (side, side), (0, 0, 0, 0))
        fitted = contain(icon, (safe, safe), transparent=True)
        foreground.paste(fitted, ((side - safe) // 2, (side - safe) // 2))
        path = res / ('mipmap-' + density) / 'ic_launcher_foreground.png'
        foreground.save(path)
        files.append(path)
    for density, size in [('xhdpi', (320, 180)), ('xxxhdpi', (640, 360))]:
        path = res / ('drawable-' + density) / 'banner.png'
        contain(wallpaper, size).save(path)
        files.append(path)
    graphics = root / 'GooglePlay-Release-Ready/Graphics'
    graphics.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source / names[1], graphics / 'App-Icon.png')
    contain(wallpaper, (1280, 720)).save(graphics / 'Android-TV-Banner.png')
    contain(banner, (1024, 500)).save(graphics / 'Feature-Graphic.png')
    files += [graphics / x for x in ('App-Icon.png', 'Android-TV-Banner.png', 'Feature-Graphic.png')]
    provenance = {'ownerSupplied': True, 'aiUsed': True,
                  'method': 'Uniform proportional resampling and centered padding; no stretching or redraw.',
                  'androidAdaptiveSafeAreaDp': 66,
                  'masters': [{'file': name, 'sha256': sha(source / name), 'size': list(images[name].size)} for name in names],
                  'exports': [{'file': p.relative_to(root).as_posix(), 'sha256': sha(p),
                               'size': list(Image.open(p).size)} for p in files]}
    (graphics / 'Artwork-Provenance.json').write_text(json.dumps(provenance, indent=2) + '\n', encoding='utf-8')
    return provenance


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, default=ROOT / 'branding/current')
    args = parser.parse_args()
    result = prepare(args.source)
    print(json.dumps({'exportedFiles': len(result['exports']), 'proportionalFit': True, 'adaptiveSafeAreaDp': 66}))

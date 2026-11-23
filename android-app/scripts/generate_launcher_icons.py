#!/usr/bin/env python3
"""Generate legacy Android launcher PNGs from the approved Sakeena artwork."""
from pathlib import Path
from PIL import Image, ImageOps

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/res/drawable-nodpi/ic_launcher_sakina.webp"
if not SOURCE.is_file():
    raise SystemExit(f"Missing launcher artwork: {SOURCE}")

sizes = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

with Image.open(SOURCE) as source:
    artwork = ImageOps.exif_transpose(source).convert("RGBA")
    if artwork.width < 96 or artwork.height < 96:
        raise SystemExit(f"Launcher artwork is unexpectedly small: {artwork.size}")
    for density, size in sizes.items():
        directory = ROOT / f"app/src/main/res/mipmap-{density}"
        directory.mkdir(parents=True, exist_ok=True)
        # Fit to a square without changing the artwork's aspect ratio.
        icon = ImageOps.contain(artwork, (size, size), method=Image.Resampling.LANCZOS)
        canvas = Image.new("RGBA", (size, size), (6, 61, 50, 255))
        canvas.alpha_composite(icon, ((size - icon.width) // 2, (size - icon.height) // 2))
        destination = directory / "ic_launcher.png"
        canvas.save(destination, format="PNG", optimize=True)
        print(f"Generated {destination.relative_to(ROOT)} ({size}x{size})")

print("Density-specific launcher PNGs generated successfully.")

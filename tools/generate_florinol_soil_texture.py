#!/usr/bin/env python3
"""Recolor the dry-soil grayscale pattern with Florinol Soil's purple hue."""

from __future__ import annotations

import argparse
import colorsys
from collections import Counter
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SOURCE = ROOT / "tools" / "florinol_soil_dry_source.png"
DEFAULT_PALETTE = ROOT / "tools" / "florinol_soil_palette_source.png"
DEFAULT_OUTPUT = ROOT / "src" / "main" / "resources" / "assets" / "createsandustry" / "textures" / "block" / "florinol_soil.png"
DEFAULT_PREVIEW_DIR = ROOT / "output" / "florinol_soil"


def pixels(image: Image.Image) -> list[tuple[int, int, int, int]]:
    return list(image.convert("RGBA").get_flattened_data())


def dominant_color(image: Image.Image) -> tuple[int, int, int]:
    colors = Counter(pixel[:3] for pixel in pixels(image) if pixel[3] > 0)
    if not colors:
        raise ValueError("Palette source has no opaque pixels.")
    return colors.most_common(1)[0][0]


def grayscale_levels(image: Image.Image) -> list[int]:
    levels: set[int] = set()
    for red, green, blue, alpha in pixels(image):
        if alpha == 0:
            continue
        if max(red, green, blue) - min(red, green, blue) > 1:
            raise ValueError("Dry-soil source must be grayscale.")
        levels.add(round((red + green + blue) / 3))
    if len(levels) < 2:
        raise ValueError("Dry-soil source needs at least two grayscale levels.")
    return sorted(levels)


def purple_ramp(base: tuple[int, int, int], levels: list[int], mode: str,
                mean_level: float) -> dict[int, tuple[int, int, int, int]]:
    hue, saturation, value = colorsys.rgb_to_hsv(
            base[0] / 255.0, base[1] / 255.0, base[2] / 255.0)
    ramp: dict[int, tuple[int, int, int, int]] = {}
    for index, level in enumerate(levels):
        if mode == "rank":
            rank = index / (len(levels) - 1)
            shade_value = max(0.0, min(1.0, value + (rank - 0.5) * 0.30))
            shade_saturation = max(0.0, min(1.0, saturation + (0.5 - rank) * 0.06))
        else:
            shade_value = max(0.0, min(1.0, value * level / mean_level))
            shade_saturation = saturation
        red, green, blue = colorsys.hsv_to_rgb(hue, shade_saturation, shade_value)
        ramp[level] = (round(red * 255), round(green * 255), round(blue * 255), 255)
    return ramp


def recolor(source: Image.Image, ramp: dict[int, tuple[int, int, int, int]]) -> Image.Image:
    output = Image.new("RGBA", source.size, (0, 0, 0, 0))
    result: list[tuple[int, int, int, int]] = []
    for red, green, blue, alpha in pixels(source):
        if alpha == 0:
            result.append((0, 0, 0, 0))
            continue
        level = round((red + green + blue) / 3)
        color = ramp[level]
        result.append((color[0], color[1], color[2], alpha))
    output.putdata(result)
    return output


def checkerboard(size: tuple[int, int], tile: int = 8) -> Image.Image:
    image = Image.new("RGBA", size, (58, 58, 64, 255))
    draw = ImageDraw.Draw(image)
    for y in range(0, size[1], tile):
        for x in range(0, size[0], tile):
            if (x // tile + y // tile) % 2 == 0:
                draw.rectangle((x, y, x + tile - 1, y + tile - 1), fill=(76, 76, 84, 255))
    return image


def save_preview(source: Image.Image, output: Image.Image, path: Path,
                 source_label: str, output_label: str, scale: int = 384) -> None:
    canvas = checkerboard((scale * 2 + 24, scale + 32), 8)
    draw = ImageDraw.Draw(canvas)
    draw.text((8, 8), source_label, fill=(235, 235, 240, 255))
    draw.text((scale + 24, 8), output_label, fill=(235, 235, 240, 255))
    canvas.alpha_composite(source.resize((scale, scale), Image.Resampling.NEAREST), (8, 24))
    canvas.alpha_composite(output.resize((scale, scale), Image.Resampling.NEAREST), (scale + 24, 24))
    canvas.save(path)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE)
    parser.add_argument("--palette", type=Path, default=DEFAULT_PALETTE)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--preview-dir", type=Path, default=DEFAULT_PREVIEW_DIR)
    parser.add_argument("--mapping", choices=("rank", "luminance"), default="rank",
                        help="rank spreads grayscale layers evenly; luminance preserves the source contrast.")
    args = parser.parse_args()

    source_path = args.source.resolve()
    palette_path = args.palette.resolve()
    output_path = args.output.resolve()
    preview_dir = args.preview_dir.resolve()
    if not source_path.is_file() or not palette_path.is_file():
        raise SystemExit("Missing dry-soil source or Florinol Soil palette source.")
    if source_path == output_path:
        raise SystemExit("Input source and output texture must be different files.")

    source_image = Image.open(source_path).convert("RGBA")
    palette_image = Image.open(palette_path).convert("RGBA")
    base = dominant_color(palette_image)
    levels = grayscale_levels(source_image)
    opaque_lumas = [
        (red + green + blue) / 3.0
        for red, green, blue, alpha in pixels(source_image)
        if alpha > 0
    ]
    mean_level = sum(opaque_lumas) / max(1, len(opaque_lumas))
    ramp = purple_ramp(base, levels, args.mapping, mean_level)
    output = recolor(source_image, ramp)

    preview_dir.mkdir(parents=True, exist_ok=True)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output.save(output_path, optimize=True)
    save_preview(
        source_image,
        output,
        preview_dir / "florinol_soil_preview.png",
        source_path.stem,
        output_path.stem,
    )

    output_colors = Counter(pixel for pixel in pixels(output) if pixel[3] > 0)
    distinct_shades = len(set(ramp.values()))
    report = [
        "Florinol Soil texture verification",
        "=" * 36,
        f"dry-soil source: {source_image.width}x{source_image.height}",
        f"palette source: {palette_image.width}x{palette_image.height}",
        f"base Florinol color: #{base[0]:02X}{base[1]:02X}{base[2]:02X}",
        f"grayscale levels: {', '.join(str(level) for level in levels)}",
        f"mapping: {args.mapping}",
        f"distinct mapped shades: {distinct_shades}/{len(levels)}",
        f"output size: {output.width}x{output.height}",
        f"output opaque colors: {len(output_colors)}",
        f"alpha preserved: {all(a == b for a, b in zip([p[3] for p in pixels(source_image)], [p[3] for p in pixels(output)]))}",
    ]
    for level in levels:
        red, green, blue, _ = ramp[level]
        report.append(f"  gray {level} -> #{red:02X}{green:02X}{blue:02X}")
    report_path = preview_dir / "verification_report.txt"
    report_path.write_text("\n".join(report) + "\n", encoding="utf-8")
    print("\n".join(report))
    print(f"output: {output_path}")
    print(f"preview: {preview_dir / 'florinol_soil_preview.png'}")


if __name__ == "__main__":
    main()

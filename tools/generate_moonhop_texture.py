#!/usr/bin/env python3
"""Generate Moonhop's native Minecraft pink/lilac animation texture.

The source may contain multiple equally sized 16x16 frames stacked vertically.
Only exact grayscale foreground pixels are recolored. All other colors and the
Alpha channel are preserved unchanged while a periodic color phase animates over
the requested output frame count.
"""

from __future__ import annotations

import argparse
import colorsys
import json
import math
import sys
from collections import Counter
from pathlib import Path

try:
    from PIL import Image, ImageDraw
except ImportError as exc:
    raise SystemExit("Pillow is required. Use the bundled Python runtime or install Pillow.") from exc


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SOURCE = ROOT / "tools" / "moonhop_source_sheet.png"
DEFAULT_REFERENCE = ROOT / "tools" / "moonhop_color_reference.png"
DEFAULT_OUTPUT = ROOT / "src" / "main" / "resources" / "assets" / "createsandustry" / "textures" / "item" / "moonhop.png"
DEFAULT_PREVIEW_DIR = ROOT / "output" / "moonhop"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE, help="Source animation strip.")
    parser.add_argument("--reference", type=Path, default=DEFAULT_REFERENCE, help="Pink/lilac color reference.")
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT, help="Output sprite PNG.")
    parser.add_argument("--mcmeta", type=Path, default=None, help="Output .png.mcmeta; defaults beside output.")
    parser.add_argument("--preview-dir", type=Path, default=DEFAULT_PREVIEW_DIR)
    parser.add_argument("--report", type=Path, default=None)
    parser.add_argument("--frames", type=int, default=64)
    parser.add_argument("--frametime", type=int, default=1)
    parser.add_argument("--interpolate", action="store_true")
    parser.add_argument("--contact-columns", type=int, default=8)
    parser.add_argument("--preview-tile", type=int, default=72)
    return parser.parse_args()


def rgb_hex(color: tuple[int, int, int]) -> str:
    return "#%02X%02X%02X" % color


def rgba_hex(color: tuple[int, int, int, int]) -> str:
    return "#%02X%02X%02X%02X" % color


def pixel_data(image: Image.Image) -> list[tuple[int, ...]]:
    flattened = getattr(image, "get_flattened_data", None)
    return list(flattened() if flattened is not None else image.getdata())


def image_summary(name: str, image: Image.Image) -> list[str]:
    rgba = image.convert("RGBA")
    pixels = pixel_data(rgba)
    alpha_counts = Counter(pixel[3] for pixel in pixels)
    partial = sum(count for alpha, count in alpha_counts.items() if 0 < alpha < 255)
    return [
        f"{name}: size={rgba.width}x{rgba.height}, mode={image.mode}, format={image.format}",
        f"{name}: alpha transparent={alpha_counts.get(0, 0)}, opaque={alpha_counts.get(255, 0)}, partial={partial}",
        f"{name}: unique RGBA={len(set(pixels))}",
    ]


def extract_square_frames(image: Image.Image) -> list[Image.Image]:
    if image.width <= 0 or image.height <= 0:
        raise ValueError("Source sheet has invalid dimensions.")
    if image.height % image.width != 0:
        raise ValueError(
            f"Source sheet must be a vertical strip of square frames; got {image.width}x{image.height}."
        )
    frame_size = image.width
    count = image.height // frame_size
    return [
        image.crop((0, index * frame_size, frame_size, (index + 1) * frame_size))
        for index in range(count)
    ]


def is_grayscale(red: int, green: int, blue: int) -> bool:
    return max(red, green, blue) - min(red, green, blue) <= 1


def grayscale_levels(frames: list[Image.Image]) -> list[int]:
    levels: set[int] = set()
    for frame in frames:
        for red, green, blue, alpha in pixel_data(frame):
            if alpha == 0:
                continue
            if not is_grayscale(red, green, blue):
                continue
            levels.add(round((red + green + blue) / 3))
    if len(levels) < 2:
        raise ValueError("Source sheet needs at least two grayscale foreground brightness levels.")
    return sorted(levels)


def reference_anchors(reference: Image.Image) -> tuple[tuple[float, float, float], tuple[float, float, float], str]:
    rgba = reference.convert("RGBA")
    candidates: list[tuple[int, int, int, float, float, float]] = []
    for red, green, blue, alpha in pixel_data(rgba):
        if alpha == 0:
            continue
        hue, saturation, value = colorsys.rgb_to_hsv(red / 255, green / 255, blue / 255)
        if saturation < 0.05 or value < 0.3:
            continue
        candidates.append((red, green, blue, hue, saturation, value))
    if not candidates:
        raise ValueError("Color reference has no usable opaque colors.")

    pink = max(candidates, key=lambda item: item[3])
    lilac = min(candidates, key=lambda item: item[3])
    pink_hsv = (pink[3], pink[4], pink[5])
    lilac_hsv = (lilac[3], lilac[4], lilac[5])
    summary = (
        f"Reference anchors: pink={rgb_hex(pink[:3])} (HSV {pink[3] * 360:.1f}, "
        f"{pink[4]:.3f}, {pink[5]:.3f}), lilac={rgb_hex(lilac[:3])} "
        f"(HSV {lilac[3] * 360:.1f}, {lilac[4]:.3f}, {lilac[5]:.3f})"
    )
    return pink_hsv, lilac_hsv, summary


def periodic_phase(index: int, frame_count: int) -> float:
    phase = index / frame_count
    return (1.0 - math.cos(2.0 * math.pi * phase)) / 2.0


def base_hsv(pink: tuple[float, float, float], lilac: tuple[float, float, float], t: float) -> tuple[float, float, float]:
    hue_delta = ((lilac[0] - pink[0] + 0.5) % 1.0) - 0.5
    hue = (pink[0] + hue_delta * t) % 1.0
    saturation = pink[1] + (lilac[1] - pink[1]) * t
    return hue, saturation, 1.0


def palette_for_phase(
    levels: list[int],
    pink: tuple[float, float, float],
    lilac: tuple[float, float, float],
    t: float,
) -> dict[int, tuple[int, int, int, int]]:
    hue, saturation, _ = base_hsv(pink, lilac, t)
    low = levels[0] / 255.0
    high = levels[-1] / 255.0
    scale = max(high - low, 1.0 / 255.0)
    palette: dict[int, tuple[int, int, int, int]] = {}
    for level in levels:
        normalized = max(0.0, min(1.0, (level / 255.0 - low) / scale))
        shadow_factor = 1.0 - normalized
        shade_hue = (hue - (2.5 * shadow_factor) / 360.0) % 1.0
        shade_saturation = min(0.62, saturation + 0.12 * shadow_factor)
        shade_value = 0.56 + 0.44 * normalized
        red, green, blue = colorsys.hsv_to_rgb(shade_hue, shade_saturation, shade_value)
        palette[level] = (
            round(red * 255),
            round(green * 255),
            round(blue * 255),
            255,
        )
    return palette


def recolor_frame(source: Image.Image, palette: dict[int, tuple[int, int, int, int]]) -> Image.Image:
    output = Image.new("RGBA", source.size, (0, 0, 0, 0))
    pixels: list[tuple[int, int, int, int]] = []
    for red, green, blue, alpha in pixel_data(source):
        if alpha == 0:
            pixels.append((0, 0, 0, 0))
            continue
        if is_grayscale(red, green, blue):
            level = round((red + green + blue) / 3)
            color = palette[level]
            pixels.append((color[0], color[1], color[2], alpha))
        else:
            pixels.append((red, green, blue, alpha))
    output.putdata(pixels)
    return output


def checkerboard(size: tuple[int, int], tile: int = 8) -> Image.Image:
    image = Image.new("RGBA", size, (58, 58, 64, 255))
    draw = ImageDraw.Draw(image)
    for y in range(0, size[1], tile):
        for x in range(0, size[0], tile):
            if (x // tile + y // tile) % 2 == 0:
                draw.rectangle((x, y, x + tile - 1, y + tile - 1), fill=(76, 76, 84, 255))
    return image


def save_single_preview(frame: Image.Image, path: Path, scale: int = 512) -> None:
    background = checkerboard((scale, scale), max(8, scale // 32))
    enlarged = frame.resize((scale, scale), Image.Resampling.NEAREST)
    background.alpha_composite(enlarged)
    background.save(path)


def save_contact_sheet(frames: list[Image.Image], path: Path, columns: int, tile: int) -> None:
    columns = max(1, columns)
    rows = math.ceil(len(frames) / columns)
    padding = 8
    label_height = 14
    cell_width = tile + padding
    cell_height = tile + label_height + padding
    sheet = checkerboard((columns * cell_width + padding, rows * cell_height + padding), 8)
    draw = ImageDraw.Draw(sheet)
    for index, frame in enumerate(frames):
        column = index % columns
        row = index // columns
        x = padding + column * cell_width
        y = padding + row * cell_height
        enlarged = frame.resize((tile, tile), Image.Resampling.NEAREST)
        sheet.alpha_composite(enlarged, (x, y + label_height))
        draw.text((x, y), f"{index:02d}", fill=(235, 235, 240, 255))
    sheet.save(path)


def alpha_mask(frame: Image.Image) -> tuple[int, ...]:
    return tuple(pixel[3] for pixel in pixel_data(frame))


def foreground_mean(frame: Image.Image) -> tuple[float, float, float]:
    totals = [0.0, 0.0, 0.0]
    count = 0
    for red, green, blue, alpha in pixel_data(frame):
        if alpha == 0:
            continue
        totals[0] += red
        totals[1] += green
        totals[2] += blue
        count += 1
    return tuple(component / max(1, count) for component in totals)


def color_distance(a: tuple[float, float, float], b: tuple[float, float, float]) -> float:
    return math.sqrt(sum((left - right) ** 2 for left, right in zip(a, b)))


def write_mcmeta(path: Path, width: int, height: int, frametime: int, interpolate: bool) -> None:
    data = {
        "animation": {
            "frametime": frametime,
            "interpolate": interpolate,
            "width": width,
            "height": height,
        }
    }
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def main() -> int:
    args = parse_args()
    if args.frames < 2:
        raise SystemExit("--frames must be at least 2")
    if args.frametime < 1:
        raise SystemExit("--frametime must be at least 1")

    source_path = args.source.resolve()
    reference_path = args.reference.resolve()
    output_path = args.output.resolve()
    mcmeta_path = (args.mcmeta or output_path.with_name(output_path.name + ".mcmeta")).resolve()
    preview_dir = args.preview_dir.resolve()
    report_path = (args.report or preview_dir / "verification_report.txt").resolve()
    if source_path == output_path:
        raise SystemExit("Input source sheet and output sprite must be different files.")
    if not source_path.is_file():
        raise SystemExit(f"Missing source sheet: {source_path}")
    if not reference_path.is_file():
        raise SystemExit(f"Missing color reference: {reference_path}")

    source_image = Image.open(source_path)
    reference_image = Image.open(reference_path)
    source_frames = extract_square_frames(source_image.convert("RGBA"))
    if args.frames % len(source_frames) != 0:
        raise SystemExit(
            f"--frames ({args.frames}) must be a multiple of the source jump frame count "
            f"({len(source_frames)})."
        )
    levels = grayscale_levels(source_frames)
    pink, lilac, anchor_summary = reference_anchors(reference_image)

    output_frames: list[Image.Image] = []
    palettes: list[dict[int, tuple[int, int, int, int]]] = []
    for index in range(args.frames):
        t = periodic_phase(index, args.frames)
        palette = palette_for_phase(levels, pink, lilac, t)
        palettes.append(palette)
        source_frame = source_frames[index % len(source_frames)]
        output_frames.append(recolor_frame(source_frame, palette))

    frame_width, frame_height = output_frames[0].size
    sprite = Image.new("RGBA", (frame_width, frame_height * len(output_frames)), (0, 0, 0, 0))
    for index, frame in enumerate(output_frames):
        sprite.alpha_composite(frame, (0, index * frame_height))

    output_path.parent.mkdir(parents=True, exist_ok=True)
    preview_dir.mkdir(parents=True, exist_ok=True)
    mcmeta_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.parent.mkdir(parents=True, exist_ok=True)

    sprite.save(output_path, optimize=True)
    write_mcmeta(mcmeta_path, frame_width, frame_height, args.frametime, args.interpolate)
    preview_stem = output_path.stem
    single_preview_path = preview_dir / f"{preview_stem}_preview_frame.png"
    contact_sheet_path = preview_dir / f"{preview_stem}_contact_sheet.png"
    save_single_preview(output_frames[0], single_preview_path)
    save_contact_sheet(output_frames, contact_sheet_path, args.contact_columns, args.preview_tile)

    alpha_mismatches = 0
    preserved_color_mismatches = 0
    recolored_pixels = 0
    preserved_color_pixels = 0
    for index, frame in enumerate(output_frames):
        source_frame = source_frames[index % len(source_frames)]
        if alpha_mask(frame) != alpha_mask(source_frame):
            alpha_mismatches += 1
        for output_pixel, source_pixel in zip(pixel_data(frame), pixel_data(source_frame)):
            if source_pixel[3] == 0:
                continue
            if is_grayscale(*source_pixel[:3]):
                recolored_pixels += 1
            else:
                preserved_color_pixels += 1
                if output_pixel != source_pixel:
                    preserved_color_mismatches += 1

    means = [foreground_mean(frame) for frame in output_frames]
    adjacent_deltas = [color_distance(means[i], means[(i + 1) % len(means)]) for i in range(len(means))]
    foreground_loop_seam = color_distance(means[-1], means[0])
    palette_start = palette_for_phase(levels, pink, lilac, periodic_phase(0, args.frames))
    palette_end = palette_for_phase(levels, pink, lilac, periodic_phase(args.frames - 1, args.frames))
    palette_loop_seam = sum(
        color_distance(palette_start[level][:3], palette_end[level][:3])
        for level in levels
    ) / len(levels)
    unique_counts = [len({pixel for pixel in pixel_data(frame) if pixel[3] > 0}) for frame in output_frames]
    distinct_shade_counts = [len(set(palette.values())) for palette in palettes]
    duration_seconds = args.frames * args.frametime / 20.0

    lines = [
        "Moonhop texture verification",
        "=" * 34,
        *image_summary("source sheet", source_image),
        f"source structural frames: {len(source_frames)} ({frame_width}x{frame_height} each)",
        f"grayscale levels recolored: {', '.join(str(level) for level in levels)}",
        *image_summary("color reference", reference_image),
        anchor_summary,
        "",
        f"output sprite: {output_path}",
        f"output mcmeta: {mcmeta_path}",
        f"frames: {args.frames}",
        f"jump cycles in loop: {args.frames // len(source_frames)}",
        f"frame size: {frame_width}x{frame_height}",
        f"sprite size: {sprite.width}x{sprite.height}",
        f"frametime: {args.frametime} tick(s)",
        f"loop duration: {args.frames * args.frametime} ticks = {duration_seconds:.1f} seconds",
        f"interpolate: {str(args.interpolate).lower()}",
        f"source-frame cycling: output frame i uses structural frame i % {len(source_frames)}",
        f"alpha mismatches vs source: {alpha_mismatches}",
        f"non-grayscale color mismatches: {preserved_color_mismatches}",
        f"recolored grayscale pixels across output: {recolored_pixels}",
        f"preserved non-grayscale pixels across output: {preserved_color_pixels}",
        f"unique foreground colors per frame: min={min(unique_counts)}, max={max(unique_counts)}",
        f"distinct mapped grayscale shades: min={min(distinct_shade_counts)}, max={max(distinct_shade_counts)}, expected={len(levels)}",
        f"maximum adjacent-frame mean RGB delta: {max(adjacent_deltas):.4f}",
        f"palette loop seam mean RGB delta: {palette_loop_seam:.4f}",
        f"foreground loop seam mean RGB delta (includes source frame layout): {foreground_loop_seam:.4f}",
        "status: PASS"
        if alpha_mismatches == 0
        and preserved_color_mismatches == 0
        and min(distinct_shade_counts) == len(levels)
        and sprite.size == (frame_width, frame_height * args.frames)
        else "status: FAIL",
    ]
    report_path.write_text("\n".join(lines) + "\n", encoding="utf-8")

    print("\n".join(lines))
    print(f"preview: {single_preview_path}")
    print(f"contact sheet: {contact_sheet_path}")
    return 0


if __name__ == "__main__":
    sys.exit(main())

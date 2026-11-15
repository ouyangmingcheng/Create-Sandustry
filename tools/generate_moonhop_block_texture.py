#!/usr/bin/env python3
"""Generate the animated pink/lilac texture for the Moonhop Block."""

from __future__ import annotations

import colorsys
import json
import math
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "tools" / "moonhop_block_source.png"
SLIME_REFERENCE = ROOT / "tools" / "slime_block_source.png"
REFERENCE = ROOT / "tools" / "moonhop_color_reference.png"
OUTPUT = ROOT / "src" / "main" / "resources" / "assets" / "createsandustry" / "textures" / "block" / "moonhop_block.png"
PREVIEW_DIR = ROOT / "output" / "moonhop_block"
FRAME_COUNT = 32
FRAMETIME = 2
ALPHA = 180
OUTPUT_VALUE_LOW = 0.82
OUTPUT_VALUE_HIGH = 1.00
DARK_SATURATION = 0.48
HUE_SHIFT_DEGREES = 1.5


def pixels(image: Image.Image) -> list[tuple[int, int, int, int]]:
    return list(image.convert("RGBA").get_flattened_data())


def luma(red: int, green: int, blue: int) -> float:
    return red * 0.299 + green * 0.587 + blue * 0.114


def is_gray(red: int, green: int, blue: int) -> bool:
    return max(red, green, blue) - min(red, green, blue) <= 1


def reference_anchors() -> tuple[tuple[float, float, float], tuple[float, float, float]]:
    candidates: list[tuple[float, float, float]] = []
    for red, green, blue, alpha in pixels(Image.open(REFERENCE)):
        if alpha == 0:
            continue
        hue, saturation, value = colorsys.rgb_to_hsv(red / 255.0, green / 255.0, blue / 255.0)
        if saturation >= 0.05 and value >= 0.3:
            candidates.append((hue, saturation, value))
    pink = max(candidates, key=lambda color: color[0])
    lilac = min(candidates, key=lambda color: color[0])
    return pink, lilac


def phase_color(index: int, pink: tuple[float, float, float], lilac: tuple[float, float, float]) -> tuple[float, float]:
    phase = index / FRAME_COUNT
    amount = (1.0 - math.cos(2.0 * math.pi * phase)) / 2.0
    delta = ((lilac[0] - pink[0] + 0.5) % 1.0) - 0.5
    return (pink[0] + delta * amount) % 1.0, pink[1] + (lilac[1] - pink[1]) * amount


def brightness_value(pixel: tuple[int, int, int, int]) -> float:
    return colorsys.rgb_to_hsv(
            pixel[0] / 255.0, pixel[1] / 255.0, pixel[2] / 255.0)[2]


def recolor(source: Image.Image, slime: Image.Image, hue: float, saturation: float,
            low: float, high: float) -> Image.Image:
    output = Image.new("RGBA", source.size, (0, 0, 0, 0))
    result: list[tuple[int, int, int, int]] = []
    # 保留上一版柔和效果：粘液块亮度差小于 1 时按 1.0 归一化，压低明暗对比。
    span = max(1.0, high - low)
    for source_pixel, slime_pixel in zip(pixels(source), pixels(slime)):
        red, green, blue, _ = source_pixel
        if not is_gray(red, green, blue):
            result.append((red, green, blue, ALPHA))
            continue
        normalized = max(0.0, min(1.0, (brightness_value(slime_pixel) - low) / span))
        shadow = 1.0 - normalized
        shade_hue = (hue - (HUE_SHIFT_DEGREES * shadow) / 360.0) % 1.0
        shade_saturation = DARK_SATURATION + (saturation - DARK_SATURATION) * normalized
        shade_value = OUTPUT_VALUE_LOW + (OUTPUT_VALUE_HIGH - OUTPUT_VALUE_LOW) * normalized
        out_red, out_green, out_blue = colorsys.hsv_to_rgb(shade_hue, shade_saturation, shade_value)
        result.append((round(out_red * 255), round(out_green * 255), round(out_blue * 255), ALPHA))
    output.putdata(result)
    return output


def checkerboard(size: tuple[int, int], tile: int = 8) -> Image.Image:
    image = Image.new("RGBA", size, (38, 38, 44, 255))
    draw = ImageDraw.Draw(image)
    for y in range(0, size[1], tile):
        for x in range(0, size[0], tile):
            if (x // tile + y // tile) % 2 == 0:
                draw.rectangle((x, y, x + tile - 1, y + tile - 1), fill=(56, 56, 64, 255))
    return image


def save_contact_sheet(frames: list[Image.Image], path: Path) -> None:
    tile = 64
    columns = 8
    rows = math.ceil(len(frames) / columns)
    sheet = checkerboard((columns * (tile + 8) + 8, rows * (tile + 24) + 8), 8)
    draw = ImageDraw.Draw(sheet)
    for index, frame in enumerate(frames):
        column = index % columns
        row = index // columns
        x = 8 + column * (tile + 8)
        y = 8 + row * (tile + 24)
        sheet.alpha_composite(frame.resize((tile, tile), Image.Resampling.NEAREST), (x, y + 14))
        draw.text((x, y), f"{index:02d}", fill=(240, 240, 245, 255))
    sheet.save(path)


def save_gif(frames: list[Image.Image], path: Path) -> None:
    scale = 128
    gif_frames: list[Image.Image] = []
    for frame in frames:
        background = checkerboard((scale, scale), 8).convert("RGBA")
        background.alpha_composite(frame.resize((scale, scale), Image.Resampling.NEAREST))
        gif_frames.append(background.convert("RGB"))
    gif_frames[0].save(path, save_all=True, append_images=gif_frames[1:],
                       duration=FRAMETIME * 50, loop=0, disposal=2, optimize=False)


def main() -> None:
    source = Image.open(SOURCE).convert("RGBA")
    slime = Image.open(SLIME_REFERENCE).convert("RGBA")
    if source.size != slime.size:
        raise SystemExit("Moonhop Block source and slime reference must have the same dimensions.")
    pink, lilac = reference_anchors()
    source_pixels = pixels(source)
    slime_values = [brightness_value(pixel) for pixel in pixels(slime)]
    if not slime_values:
        raise SystemExit("Moonhop Block source contains no foreground grayscale pixels.")
    low, high = min(slime_values), max(slime_values)

    frames = [recolor(source, slime, *phase_color(index, pink, lilac), low, high)
              for index in range(FRAME_COUNT)]
    sprite = Image.new("RGBA", (source.width, source.height * FRAME_COUNT), (0, 0, 0, 0))
    for index, frame in enumerate(frames):
        sprite.alpha_composite(frame, (0, index * source.height))

    PREVIEW_DIR.mkdir(parents=True, exist_ok=True)
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    sprite.save(OUTPUT, optimize=True)
    frames[0].save(PREVIEW_DIR / "moonhop_block_preview_frame.png")
    save_contact_sheet(frames, PREVIEW_DIR / "moonhop_block_contact_sheet.png")
    save_gif(frames, PREVIEW_DIR / "moonhop_block_preview.gif")

    mcmeta = {"animation": {"frametime": FRAMETIME, "interpolate": False, "width": source.width, "height": source.height}}
    Path(str(OUTPUT) + ".mcmeta").write_text(json.dumps(mcmeta, indent=2) + "\n", encoding="utf-8")

    alpha_values = {pixel[3] for pixel in pixels(sprite)}
    colored_positions = [
        index
        for index, (red, green, blue, alpha) in enumerate(source_pixels)
        if alpha > 0 and not is_gray(red, green, blue)
    ]
    recolored_positions = [
        index
        for index, (red, green, blue, alpha) in enumerate(source_pixels)
        if alpha > 0 and is_gray(red, green, blue)
    ]
    colored_mismatches = sum(
        1
        for frame in frames
        for index in colored_positions
        if pixels(frame)[index][:3] != source_pixels[index][:3]
    )
    recolored_values = [
        brightness_value(pixels(frame)[index])
        for frame in frames
        for index in recolored_positions
    ]
    report = [
        "Moonhop Block texture verification",
        "=" * 37,
        f"source size: {source.width}x{source.height}",
        f"slime value range: {low:.3f}-{high:.3f}",
        f"output grayscale value range: {min(recolored_values):.3f}-{max(recolored_values):.3f}",
        f"dark-side saturation: {DARK_SATURATION:.2f}",
        f"frames: {FRAME_COUNT}",
        f"frametime: {FRAMETIME} tick(s)",
        f"loop duration: {FRAME_COUNT * FRAMETIME} ticks = {FRAME_COUNT * FRAMETIME / 20.0:.1f} seconds",
        f"sprite size: {sprite.width}x{sprite.height}",
        f"alpha values: {sorted(alpha_values)}",
        f"recolored grayscale pixels per frame: {len(recolored_positions)}",
        f"preserved colored pixels per frame: {len(colored_positions)}",
        f"colored RGB mismatches: {colored_mismatches}",
        f"unique RGBA colors: {len(set(pixels(sprite)))}",
    ]
    (PREVIEW_DIR / "verification_report.txt").write_text("\n".join(report) + "\n", encoding="utf-8")
    print("\n".join(report))
    print(f"output: {OUTPUT}")


if __name__ == "__main__":
    main()

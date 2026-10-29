#!/usr/bin/env python3
"""Render the broken sword's complete blade as a fading spectral blade."""

from __future__ import annotations

import colorsys
import json
import math
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
ITEM_DIR = ROOT / "src" / "main" / "resources" / "assets" / "createsandustry" / "textures" / "item"
BROKEN = ITEM_DIR / "sand_master_sword_broken.png"
SOURCE = ROOT / "tools" / "sand_master_sword_broken_source.png"
FULL = ITEM_DIR / "sand_master_sword.png"
OUTPUT_DIR = ROOT / "output" / "sand_master_sword_broken_ethereal"
FRAME_COUNT = 32
FRAMETIME = 2
MIN_VISIBILITY = 0.08


def pixels(image: Image.Image) -> list[tuple[int, int, int, int]]:
    return list(image.convert("RGBA").get_flattened_data())


def mix(first: tuple[int, int, int], second: tuple[int, int, int], amount: float) -> tuple[int, int, int]:
    return tuple(round(first[index] * (1.0 - amount) + second[index] * amount) for index in range(3))


def is_blade(x: int, y: int, full: tuple[int, int, int, int]) -> bool:
    if full[3] == 0 or x < 6 or y > 9:
        return False
    hue, saturation, value = colorsys.rgb_to_hsv(
            full[0] / 255.0, full[1] / 255.0, full[2] / 255.0)
    return 20.0 / 360.0 <= hue <= 70.0 / 360.0 and saturation >= 0.05 and value >= 0.50


def blade_pixel(full: tuple[int, int, int, int], x: int, y: int) -> tuple[int, int, int, int]:
    progress = max(0.0, min(1.0, ((x - 6) + (9 - y)) / 18.0))
    base = full[:3]
    warm = mix(base, (255, 248, 225), 0.30)
    color = mix(warm, (221, 205, 255), 0.20 + 0.50 * progress)
    color = tuple(min(255, round(channel * 0.75 + 255 * 0.25)) for channel in color)
    alpha = round(240 - 145 * progress)
    return (*color, alpha)


def checkerboard(size: tuple[int, int], tile: int = 8) -> Image.Image:
    image = Image.new("RGBA", size, (38, 38, 44, 255))
    draw = ImageDraw.Draw(image)
    for y in range(0, size[1], tile):
        for x in range(0, size[0], tile):
            if (x // tile + y // tile) % 2 == 0:
                draw.rectangle((x, y, x + tile - 1, y + tile - 1), fill=(56, 56, 64, 255))
    return image


def make_animation_frame(base: Image.Image, blade_positions: list[tuple[int, int]], phase: float) -> Image.Image:
    frame = base.copy()
    pixels_out = pixels(frame)
    visibility = MIN_VISIBILITY + (1.0 - MIN_VISIBILITY) * (0.5 + 0.5 * math.cos(2.0 * math.pi * phase))
    for x, y in blade_positions:
        index = y * frame.width + x
        red, green, blue, alpha = base.getpixel((x, y))
        color = mix((red, green, blue), (225, 213, 255), (1.0 - visibility) * 0.45)
        pixels_out[index] = (*color, max(1, round(alpha * visibility)))
    frame.putdata(pixels_out)
    return frame


def write_mcmeta(path: Path) -> None:
    data = {
        "animation": {
            "frametime": FRAMETIME,
            "interpolate": False,
            "width": 16,
            "height": 16,
        }
    }
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def save_animation_sheet(frames: list[Image.Image], path: Path) -> None:
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
        enlarged = frame.resize((scale, scale), Image.Resampling.NEAREST)
        background.alpha_composite(enlarged)
        gif_frames.append(background.convert("RGB"))
    gif_frames[0].save(
        path,
        save_all=True,
        append_images=gif_frames[1:],
        duration=FRAMETIME * 50,
        loop=0,
        disposal=2,
        optimize=False,
    )


def main() -> None:
    broken = Image.open(SOURCE).convert("RGBA")
    full = Image.open(FULL).convert("RGBA")
    if broken.size != full.size:
        raise SystemExit("Broken and full sword textures must have the same dimensions.")

    output = broken.copy()
    output_pixels = pixels(output)
    changed = 0
    alpha_values: list[int] = []
    for y in range(output.height):
        for x in range(output.width):
            index = y * output.width + x
            full_pixel = full.getpixel((x, y))
            if is_blade(x, y, full_pixel):
                spectral = blade_pixel(full_pixel, x, y)
                output_pixels[index] = spectral
                alpha_values.append(spectral[3])
                changed += 1
    output.putdata(output_pixels)

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    blade_positions = [
        (x, y)
        for y in range(output.height)
        for x in range(output.width)
        if is_blade(x, y, full.getpixel((x, y)))
    ]
    frames = [
        make_animation_frame(output, blade_positions, index / FRAME_COUNT)
        for index in range(FRAME_COUNT)
    ]
    sprite = Image.new("RGBA", (output.width, output.height * FRAME_COUNT), (0, 0, 0, 0))
    for index, frame in enumerate(frames):
        sprite.alpha_composite(frame, (0, index * output.height))
    sprite.save(BROKEN, optimize=True)
    output.save(OUTPUT_DIR / "sand_master_sword_broken_ethereal.png")
    write_mcmeta(Path(str(BROKEN) + ".mcmeta"))
    save_animation_sheet(frames, OUTPUT_DIR / "animation_contact_sheet.png")
    save_gif(frames, OUTPUT_DIR / "animation_preview.gif")

    scale = 384
    preview = checkerboard((scale * 2 + 24, scale + 32), 8)
    draw = ImageDraw.Draw(preview)
    draw.text((8, 8), "before", fill=(240, 240, 245, 255))
    draw.text((scale + 24, 8), "ethereal blade", fill=(240, 240, 245, 255))
    preview.alpha_composite(broken.resize((scale, scale), Image.Resampling.NEAREST), (8, 24))
    preview.alpha_composite(output.resize((scale, scale), Image.Resampling.NEAREST), (scale + 24, 24))
    preview_path = OUTPUT_DIR / "preview.png"
    preview.save(preview_path)

    outside_changes = 0
    for y in range(output.height):
        for x in range(output.width):
            if is_blade(x, y, full.getpixel((x, y))):
                continue
            if output.getpixel((x, y)) != broken.getpixel((x, y)):
                outside_changes += 1
    report = [
        "Ethereal broken blade verification",
        "=" * 36,
        f"texture size: {output.width}x{output.height}",
        f"changed blade pixels: {changed}",
        f"alpha range: {min(alpha_values)}-{max(alpha_values)}",
        f"animation frames: {FRAME_COUNT}",
        f"frametime: {FRAMETIME} tick(s)",
        f"loop duration: {FRAME_COUNT * FRAMETIME} ticks = {FRAME_COUNT * FRAMETIME / 20.0:.1f} seconds",
        f"changes outside blade mask: {outside_changes}",
        f"output RGBA colors: {len(set(pixels(output)))}",
    ]
    (OUTPUT_DIR / "verification_report.txt").write_text("\n".join(report) + "\n", encoding="utf-8")
    print("\n".join(report))
    print(f"output: {BROKEN}")
    print(f"preview: {preview_path}")


if __name__ == "__main__":
    main()

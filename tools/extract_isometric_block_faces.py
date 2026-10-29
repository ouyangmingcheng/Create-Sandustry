#!/usr/bin/env python3
"""Extract the three visible face textures from an isometric 128x128 block image."""

from __future__ import annotations

import argparse
from collections import Counter
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SOURCE = ROOT / "tools" / "468648_block_render.png"
DEFAULT_OUTPUT_DIR = ROOT / "output" / "468648_faces"

# Source-space parallelograms: origin, U edge, V edge.
FACES = {
    "top": ((7.0, 30.0), (57.0, -29.0), (57.0, 29.0)),
    "left": ((7.0, 30.0), (57.0, 29.0), (0.0, 69.0)),
    "right": ((64.0, 59.0), (56.0, -29.0), (0.0, 68.0)),
}


def pixel_data(image: Image.Image) -> list[tuple[int, int, int, int]]:
    return list(image.convert("RGBA").get_flattened_data())


def luma(color: tuple[int, int, int, int]) -> float:
    return color[0] * 0.299 + color[1] * 0.587 + color[2] * 0.114


def hottest_luma(face: Image.Image) -> float:
    return max(luma(pixel) for pixel in pixel_data(face) if pixel[3] > 0)


def normalize_face(face: Image.Image, gain: float) -> Image.Image:
    output = Image.new("RGBA", face.size, (0, 0, 0, 0))
    result: list[tuple[int, int, int, int]] = []
    for red, green, blue, alpha in pixel_data(face):
        if alpha == 0:
            result.append((0, 0, 0, 0))
            continue
        result.append((
            min(255, round(red * gain)),
            min(255, round(green * gain)),
            min(255, round(blue * gain)),
            alpha,
        ))
    output.putdata(result)
    return output


def nearest_face(source: np.ndarray, face: tuple[tuple[float, float], tuple[float, float], tuple[float, float]], size: int) -> Image.Image:
    origin = np.array(face[0], dtype=float)
    edge_u = np.array(face[1], dtype=float)
    edge_v = np.array(face[2], dtype=float)
    output = np.zeros((size, size, 4), dtype=np.uint8)
    height, width = source.shape[:2]
    for y in range(size):
        v = (y + 0.5) / size
        for x in range(size):
            u = (x + 0.5) / size
            sample_x, sample_y = np.rint(origin + edge_u * u + edge_v * v).astype(int)
            sample_x = max(0, min(width - 1, sample_x))
            sample_y = max(0, min(height - 1, sample_y))
            output[y, x] = source[sample_y, sample_x]
    return Image.fromarray(output, "RGBA")


def modal_face(
    source: np.ndarray,
    face: tuple[tuple[float, float], tuple[float, float], tuple[float, float]],
    size: int = 16,
    samples: int = 7,
) -> Image.Image:
    origin = np.array(face[0], dtype=float)
    edge_u = np.array(face[1], dtype=float)
    edge_v = np.array(face[2], dtype=float)
    output = np.zeros((size, size, 4), dtype=np.uint8)
    height, width = source.shape[:2]

    for y in range(size):
        for x in range(size):
            sampled: list[tuple[int, int, int, int]] = []
            for sy in range(samples):
                v = (y + (sy + 0.5) / samples) / size
                for sx in range(samples):
                    u = (x + (sx + 0.5) / samples) / size
                    sample_x, sample_y = np.rint(origin + edge_u * u + edge_v * v).astype(int)
                    if 0 <= sample_x < width and 0 <= sample_y < height:
                        color = tuple(int(channel) for channel in source[sample_y, sample_x])
                        if color[3] > 0:
                            sampled.append(color)
            if not sampled:
                continue

            counts = Counter(sampled)
            maximum = max(counts.values())
            candidates = [color for color, count in counts.items() if count == maximum]
            center_u = (x + 0.5) / size
            center_v = (y + 0.5) / size
            center_x, center_y = np.rint(origin + edge_u * center_u + edge_v * center_v).astype(int)
            center = tuple(int(channel) for channel in source[center_y, center_x])
            output[y, x] = min(
                candidates,
                key=lambda color: sum((color[index] - center[index]) ** 2 for index in range(4)),
            )
    return Image.fromarray(output, "RGBA")


def checkerboard(size: tuple[int, int], tile: int = 8) -> Image.Image:
    image = Image.new("RGBA", size, (42, 42, 48, 255))
    draw = ImageDraw.Draw(image)
    for y in range(0, size[1], tile):
        for x in range(0, size[0], tile):
            if (x // tile + y // tile) % 2 == 0:
                draw.rectangle((x, y, x + tile - 1, y + tile - 1), fill=(58, 58, 66, 255))
    return image


def save_sheet(faces: dict[str, Image.Image], path: Path, scale: int) -> None:
    tile = 16 * scale
    sheet = checkerboard((tile * len(faces) + 20, tile + 30), 8)
    draw = ImageDraw.Draw(sheet)
    for index, (name, face) in enumerate(faces.items()):
        x = 10 + index * tile
        enlarged = face.resize((tile - 8, tile - 8), Image.Resampling.NEAREST)
        sheet.alpha_composite(enlarged, (x + 4, 24))
        draw.text((x, 6), name, fill=(240, 240, 245, 255))
    sheet.save(path)


def atlas(faces: dict[str, Image.Image], size: int) -> Image.Image:
    image = Image.new("RGBA", (size * len(faces), size), (0, 0, 0, 0))
    for index, face in enumerate(faces.values()):
        image.alpha_composite(face, (index * size, 0))
    return image


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE)
    parser.add_argument("--output-dir", type=Path, default=DEFAULT_OUTPUT_DIR)
    args = parser.parse_args()

    source_path = args.source.resolve()
    output_dir = args.output_dir.resolve()
    if not source_path.is_file():
        raise SystemExit(f"Missing source image: {source_path}")
    source_image = Image.open(source_path).convert("RGBA")
    source = np.array(source_image)
    output_dir.mkdir(parents=True, exist_ok=True)

    faces_16 = {name: modal_face(source, face, 16, 7) for name, face in FACES.items()}
    faces_64 = {name: nearest_face(source, face, 64) for name, face in FACES.items()}
    top_luma = hottest_luma(faces_16["top"])
    gains = {
        name: top_luma / hottest_luma(face)
        for name, face in faces_16.items()
    }
    normalized_16 = {
        name: normalize_face(face, gains[name])
        for name, face in faces_16.items()
    }
    for name, face in faces_16.items():
        face.save(output_dir / f"{name}_16.png")
        normalized_16[name].save(output_dir / f"{name}_16_normalized.png")
    for name, face in faces_64.items():
        face.save(output_dir / f"{name}_64.png")

    atlas(faces_16, 16).save(output_dir / "faces_16_atlas.png")
    atlas(normalized_16, 16).save(output_dir / "faces_16_normalized_atlas.png")
    atlas(faces_64, 64).save(output_dir / "faces_64_atlas.png")
    save_sheet(faces_16, output_dir / "faces_16_preview.png", 16)
    save_sheet(normalized_16, output_dir / "faces_16_normalized_preview.png", 16)
    save_sheet(faces_64, output_dir / "faces_64_preview.png", 4)

    lines = [
        "Isometric block face extraction",
        "=" * 32,
        f"source: {source_path}",
        f"source size: {source_image.width}x{source_image.height}",
        "sampling: 16x16 modal area sampling; 64x64 nearest-neighbor affine sampling",
        "raw faces retain the directional shading visible in the isometric render",
        "normalized 16x16 faces use the top face's brightest tone as a shared exposure reference",
        "faces: top, left, right",
    ]
    for name, face in faces_16.items():
        colors = Counter(pixel for pixel in pixel_data(face) if pixel[3] > 0)
        lines.append(
            f"{name}: 16x16, opaque colors={len(colors)}, "
            f"transparent pixels={face.width * face.height - sum(colors.values())}, "
            f"normalization gain={gains[name]:.4f}"
        )
    (output_dir / "verification_report.txt").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("\n".join(lines))
    print(f"output: {output_dir}")


if __name__ == "__main__":
    main()

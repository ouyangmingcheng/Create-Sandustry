#!/usr/bin/env python3
"""Generate sand-dominant ores with original, hand-placed mineral grains.

Small shaded clusters occupy less than 10% of each face. The host texture is
copied exactly everywhere else; source ores are visual references only.
"""
from __future__ import annotations

import argparse
from pathlib import Path

from sandified_ore_manifest import iter_source_pairs
from sandified_png import (
    PngImage,
    blit_scaled,
    draw_rect,
    fill_rect,
    make_image,
    read_png,
    save_png,
    write_png,
)
from sandified_resources import (
    ResourceArchives,
    locate_create_jar,
    locate_minecraft_jar,
    model_texture_all,
    project_model_texture_all,
    repo_root,
    resolve_texture_bytes,
    sha256_bytes,
    write_json,
)


WHITE = (245, 245, 245, 255)
GRAY = (90, 90, 90, 255)
DARK = (30, 30, 30, 255)
ACCENT = (220, 180, 70, 255)


FONT = {
    "A": ("01110", "10001", "10001", "11111", "10001", "10001", "10001"),
    "B": ("11110", "10001", "10001", "11110", "10001", "10001", "11110"),
    "C": ("01111", "10000", "10000", "10000", "10000", "10000", "01111"),
    "D": ("11110", "10001", "10001", "10001", "10001", "10001", "11110"),
    "E": ("11111", "10000", "10000", "11110", "10000", "10000", "11111"),
    "F": ("11111", "10000", "10000", "11110", "10000", "10000", "10000"),
    "G": ("01111", "10000", "10000", "10111", "10001", "10001", "01111"),
    "H": ("10001", "10001", "10001", "11111", "10001", "10001", "10001"),
    "I": ("11111", "00100", "00100", "00100", "00100", "00100", "11111"),
    "J": ("00111", "00010", "00010", "00010", "00010", "10010", "01100"),
    "K": ("10001", "10010", "10100", "11000", "10100", "10010", "10001"),
    "L": ("10000", "10000", "10000", "10000", "10000", "10000", "11111"),
    "M": ("10001", "11011", "10101", "10101", "10001", "10001", "10001"),
    "N": ("10001", "11001", "10101", "10011", "10001", "10001", "10001"),
    "O": ("01110", "10001", "10001", "10001", "10001", "10001", "01110"),
    "P": ("11110", "10001", "10001", "11110", "10000", "10000", "10000"),
    "Q": ("01110", "10001", "10001", "10001", "10101", "10010", "01101"),
    "R": ("11110", "10001", "10001", "11110", "10100", "10010", "10001"),
    "S": ("01111", "10000", "10000", "01110", "00001", "00001", "11110"),
    "T": ("11111", "00100", "00100", "00100", "00100", "00100", "00100"),
    "U": ("10001", "10001", "10001", "10001", "10001", "10001", "01110"),
    "V": ("10001", "10001", "10001", "10001", "10001", "01010", "00100"),
    "W": ("10001", "10001", "10001", "10101", "10101", "10101", "01010"),
    "X": ("10001", "10001", "01010", "00100", "01010", "10001", "10001"),
    "Y": ("10001", "10001", "01010", "00100", "00100", "00100", "00100"),
    "Z": ("11111", "00001", "00010", "00100", "01000", "10000", "11111"),
    "0": ("01110", "10001", "10011", "10101", "11001", "10001", "01110"),
    "1": ("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
    "2": ("01110", "10001", "00001", "00010", "00100", "01000", "11111"),
    "3": ("11110", "00001", "00001", "01110", "00001", "00001", "11110"),
    "4": ("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
    "5": ("11111", "10000", "10000", "11110", "00001", "00001", "11110"),
    "6": ("01110", "10000", "10000", "11110", "10001", "10001", "01110"),
    "7": ("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
    "8": ("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
    "9": ("01110", "10001", "10001", "01111", "00001", "00001", "01110"),
    "_": ("00000", "00000", "00000", "00000", "00000", "00000", "11111"),
    "-": ("00000", "00000", "00000", "11111", "00000", "00000", "00000"),
    ":": ("00000", "00100", "00100", "00000", "00100", "00100", "00000"),
    " ": ("00000", "00000", "00000", "00000", "00000", "00000", "00000"),
}


def draw_text(image: PngImage, text: str, x: int, y: int, scale: int, color) -> None:
    cursor = x
    for char in text.upper():
        glyph = FONT.get(char)
        if glyph is None:
            cursor += 6 * scale
            continue
        for gy, row in enumerate(glyph):
            for gx, bit in enumerate(row):
                if bit == "1":
                    fill_rect(image, cursor + gx * scale, y + gy * scale, scale, scale, color)
        cursor += 6 * scale


def ensure_same_size(entry_name: str, source: PngImage, base: PngImage) -> None:
    if (source.width, source.height) != (base.width, base.height):
        raise ValueError(
            f"{entry_name}: source ore is {source.width}x{source.height}, "
            f"base is {base.width}x{base.height}"
        )
    if source.width != 16 or source.height != 16:
        raise ValueError(
            f"{entry_name}: standard block texture must be 16x16, "
            f"got {source.width}x{source.height}"
        )


# Three shades per mineral, lit from upper left. Warm shadows keep grains
# embedded in sand; no stone/deepslate outlines or full mineral veins.
GRAIN_PALETTES = {
    "coal": ("70675a", "494740", "373a35"),
    "iron": ("e1b492", "bb8564", "906447"),
    "copper": ("e1a075", "be7751", "925b40"),
    "gold": ("f8dc79", "e3af35", "b38229"),
    "redstone": ("e27661", "bd433c", "873b32"),
    "lapis": ("7798cd", "416caf", "365184"),
    "diamond": ("abebe0", "58bdb8", "398d91"),
    "emerald": ("9cda98", "4ba966", "347850"),
    "zinc": ("e0e2c7", "acbeb1", "7d968b"),
}
OXIDIZED_COPPER = ("a4c5a0", "6b9e87", "4e7f70")

# Six irregular deposits per face, with broad gaps of uninterrupted sand.
# Each digit selects light/base/shadow; dots leave the original host visible.
# Offsets and silhouettes are original and intentionally differ by mineral.
GRAIN_LAYOUTS = {
    "coal": ((2, 2, "01/.2"), (10, 1, "01"), (7, 6, "01/12"),
             (1, 10, "01/.2"), (12, 9, "01/.2"), (7, 13, "01")),
    "iron": ((1, 3, "011"), (10, 2, "01/.2"), (6, 7, "011"),
             (12, 10, "01"), (2, 12, "011"), (8, 14, "01")),
    "copper": ((2, 1, "01/.2"), (11, 3, "01/2."), (6, 6, "01/.2"),
               (1, 10, "01"), (11, 11, "01/.2"), (6, 14, "01")),
    "gold": ((3, 2, "01/.2"), (11, 1, "01"), (7, 6, "01/12"),
             (1, 10, "01"), (12, 10, "01/.2"), (6, 13, "01/.2")),
    "redstone": ((1, 2, "011"), (10, 3, "01/.2"), (5, 6, "01/.2"),
                 (12, 9, "011"), (2, 11, "01"), (7, 14, "011")),
    "lapis": ((2, 1, "01/12"), (11, 3, "01"), (6, 6, "01/.2"),
              (1, 10, "01/.2"), (12, 11, "01/12"), (6, 14, "01")),
    "diamond": ((2, 2, "0./12"), (11, 1, "01"), (7, 6, "0./12"),
                (1, 10, "01"), (12, 10, "0./12"), (6, 13, "0./12")),
    "emerald": ((3, 1, "0/1/2"), (11, 3, "01"), (7, 6, "0/1/2"),
                (1, 10, "0/1/2"), (12, 10, "01"), (7, 13, "0/1/2")),
    "zinc": ((1, 3, "01/.2"), (10, 1, "01/2."), (6, 7, "01"),
             (12, 10, "01/2."), (2, 12, "01"), (8, 14, "01")),
}


def rgba(hex_color):
    return tuple(bytes.fromhex(hex_color)) + (255,)


def generate_pair_textures(
    hardened_entry, petrified_entry, regular_ore, regular_base,
    deepslate_ore, deepslate_base, hardened_base, petrified_base,
):
    results = []
    for entry, source, target_base in (
        (hardened_entry, regular_ore, hardened_base),
        (petrified_entry, deepslate_ore, petrified_base),
    ):
        ensure_same_size(entry.id, source, target_base)
        pixels = list(target_base.pixels)
        grains = []
        for number, (x, y, pattern) in enumerate(GRAIN_LAYOUTS[entry.ore.key]):
            palette = GRAIN_PALETTES[entry.ore.key]
            if entry.ore.key == "copper" and number in (1, 4):
                palette = OXIDIZED_COPPER
            colors = tuple(rgba(color) for color in palette)
            for dy, row in enumerate(pattern.split("/")):
                for dx, shade in enumerate(row):
                    if shade == ".":
                        continue
                    index = (y + dy) * 16 + x + dx
                    pixels[index] = colors[int(shade)]
                    grains.append([x + dx, y + dy])
        results.append({
            "entry": entry, "source": source, "target_base": target_base,
            "result": PngImage(16, 16, pixels),
            "mineral_pixels": len(grains), "grain_coordinates": grains,
            "base_palette_size": len(target_base.palette()),
        })
    return results


def render_preview(pairs_data, out_path: Path) -> None:
    scale = 6
    tile = 16 * scale
    gap = 7
    row_label_width = 72
    header_height = 34
    row_height = tile + 25
    columns = 8
    width = row_label_width + columns * (tile + gap) + gap
    height = header_height + len(pairs_data) * row_height + gap
    image = make_image(width, height, DARK)
    fill_rect(image, 0, 0, width, header_height, (20, 20, 20, 255))
    draw_text(image, "SANDIFIED ORE TEXTURE REVIEW", gap, 8, 2, ACCENT)
    headers = ("SRC", "BASE", "SAND", "HARD", "DEEP", "BASE", "PETR", "PETR")
    for col, label in enumerate(headers):
        x = row_label_width + gap + col * (tile + gap)
        draw_text(image, label, x, 23, 1, WHITE)

    for row, data in enumerate(pairs_data):
        ore = data["ore"]
        y = header_height + row * row_height
        draw_text(image, ore.key.upper(), gap, y + 10, 2, WHITE)
        images = (
            data["regular_ore"],
            data["regular_base"],
            data["hardened_base"],
            data["hardened_result"],
            data["deepslate_ore"],
            data["deepslate_base"],
            data["petrified_base"],
            data["petrified_result"],
        )
        for col, tile_image in enumerate(images):
            x = row_label_width + gap + col * (tile + gap)
            fill_rect(image, x - 2, y - 2, tile + 4, tile + 4, (5, 5, 5, 255))
            fill_rect(image, x - 1, y - 1, tile + 2, tile + 2, (170, 170, 170, 255))
            blit_scaled(image, tile_image, x, y, scale)
        draw_text(
            image,
            f"MINERAL {data['mineral_pixels']}/256  SAND {256 - data['mineral_pixels']}/256",
            row_label_width + gap,
            y + tile + 5,
            1,
            GRAY,
        )
        draw_rect(image, 1, y - 2, width - 2, row_height - 2, (55, 55, 55, 255))
    save_png(out_path, image)


def render_grain_review(pairs_data, out_path: Path) -> None:
    """Nearest-neighbour 1x/8x swatches and 3x3 walls for both hosts."""
    image = make_image(1304, 628, DARK)
    draw_text(image, "SPARSE MINERALS IN SAND", 12, 10, 2, ACCENT)
    for col, data in enumerate(pairs_data):
        x = 12 + col * 144
        draw_text(image, data["ore"].key, x, 34, 1, WHITE)
        for row, key in enumerate(("hardened_result", "petrified_result")):
            texture = data[key]
            y = 50 + row * 164
            blit_scaled(image, texture, x, y, 8)
            blit_scaled(image, texture, x, y + 134, 1)
            draw_text(image, "1X", x + 24, y + 138, 1, GRAY)
    draw_text(image, "3X3 TILE - HARDENED", 12, 382, 1, ACCENT)
    draw_text(image, "3X3 TILE - PETRIFIED", 12, 504, 1, ACCENT)
    for col, data in enumerate(pairs_data):
        for row, key in enumerate(("hardened_result", "petrified_result")):
            for ty in range(3):
                for tx in range(3):
                    blit_scaled(image, data[key], 12 + col * 144 + tx * 32,
                                398 + row * 122 + ty * 32, 2)
    save_png(out_path, image)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--minecraft-jar")
    parser.add_argument("--create-jar")
    parser.add_argument("--check", action="store_true", help="fail if a generated texture differs")
    args = parser.parse_args()

    root = repo_root()
    minecraft_jar = locate_minecraft_jar(root, args.minecraft_jar)
    create_jar = locate_create_jar(root, args.create_jar)
    output_dir = root / "src/main/resources/assets/createsandustry/textures/block"
    blockstate_dir = root / "src/main/resources/assets/createsandustry/blockstates"
    model_dir = root / "src/main/resources/assets/createsandustry/models/block"
    item_model_dir = root / "src/main/resources/assets/createsandustry/models/item"
    docs_dir = root / "docs/sandified_ores"
    output_dir.mkdir(parents=True, exist_ok=True)
    docs_dir.mkdir(parents=True, exist_ok=True)

    report_entries: list[dict] = []
    mapping_entries: list[dict] = []
    preview_pairs: list[dict] = []
    changed = False

    with ResourceArchives(minecraft_jar, create_jar) as archives:
        hardened_base_texture_id = project_model_texture_all(
            root, "createsandustry:hardened_sand"
        )
        petrified_base_texture_id = project_model_texture_all(
            root, "createsandustry:petrified_sand"
        )
        hardened_base = read_png(resolve_texture_bytes(root, archives, hardened_base_texture_id))
        petrified_base = read_png(resolve_texture_bytes(root, archives, petrified_base_texture_id))

        for ore, hardened_entry, petrified_entry in iter_source_pairs():
            regular_ore_texture = model_texture_all(archives, ore.source_block)
            deepslate_ore_texture = model_texture_all(archives, ore.deepslate_source_block)
            regular_base_texture = model_texture_all(archives, ore.source_base_block)
            deepslate_base_texture = model_texture_all(archives, ore.deepslate_source_base_block)
            regular_ore = read_png(resolve_texture_bytes(root, archives, regular_ore_texture))
            deepslate_ore = read_png(resolve_texture_bytes(root, archives, deepslate_ore_texture))
            regular_base = read_png(resolve_texture_bytes(root, archives, regular_base_texture))
            deepslate_base = read_png(
                resolve_texture_bytes(root, archives, deepslate_base_texture)
            )

            results = generate_pair_textures(
                hardened_entry,
                petrified_entry,
                regular_ore,
                regular_base,
                deepslate_ore,
                deepslate_base,
                hardened_base,
                petrified_base,
            )

            for result_data in results:
                entry = result_data["entry"]
                source_texture = (
                    regular_ore_texture
                    if entry.variant.key == "hardened"
                    else deepslate_ore_texture
                )
                source_base_texture = (
                    regular_base_texture
                    if entry.variant.key == "hardened"
                    else deepslate_base_texture
                )
                target_base_texture = (
                    hardened_base_texture_id
                    if entry.variant.key == "hardened"
                    else petrified_base_texture_id
                )
                output_id = f"createsandustry:block/{entry.id}"
                output_path = output_dir / f"{entry.id}.png"
                output_bytes = write_png(result_data["result"])

                if args.check:
                    previous = output_path.read_bytes() if output_path.exists() else None
                    if previous != output_bytes:
                        raise SystemExit(f"texture drift detected: {output_path}")
                else:
                    previous = output_path.read_bytes() if output_path.exists() else None
                    if previous != output_bytes:
                        output_path.write_bytes(output_bytes)
                        changed = True

                blockstate = {
                    "variants": {
                        ("lit=false" if ore.redstone_behavior else ""): {
                            "model": f"createsandustry:block/{entry.id}"
                        }
                    }
                }
                if ore.redstone_behavior:
                    blockstate["variants"]["lit=true"] = {
                        "model": f"createsandustry:block/{entry.id}"
                    }
                block_model = {
                    "parent": "minecraft:block/cube_all",
                    "textures": {"all": output_id},
                }
                item_model = {"parent": f"createsandustry:block/{entry.id}"}
                if not args.check:
                    write_json(blockstate_dir / f"{entry.id}.json", blockstate)
                    write_json(model_dir / f"{entry.id}.json", block_model)
                    write_json(item_model_dir / f"{entry.id}.json", item_model)

                report_entries.append({
                    "target_block": entry.target_block,
                    "target_texture": output_id,
                    "variant": entry.variant.key,
                    "ore": ore.key,
                    "source_block": entry.source_block,
                    "source_texture": source_texture,
                    "source_base_block": entry.source_base_block,
                    "source_base_texture": source_base_texture,
                    "target_base_block": entry.variant.target_base_block,
                    "target_base_texture": target_base_texture,
                    "output_texture_path": str(output_path.relative_to(root)).replace("\\", "/"),
                    "dimensions": [result_data["result"].width, result_data["result"].height],
                    "source_texture_sha256": sha256_bytes(
                        resolve_texture_bytes(root, archives, source_texture)
                    ),
                    "output_texture_sha256": sha256_bytes(output_bytes),
                    "base_palette_size": result_data["base_palette_size"],
                    "mineral_pixels": result_data["mineral_pixels"],
                    "mineral_coverage_percent": round(result_data["mineral_pixels"] / 256 * 100, 2),
                    "unchanged_host_pixels": 256 - result_data["mineral_pixels"],
                    "grain_coordinates": result_data["grain_coordinates"],
                })
                mapping_entries.append({
                    "source_block": entry.source_block,
                    "source_texture": source_texture,
                    "source_base_block": entry.source_base_block,
                    "source_base_texture": source_base_texture,
                    "target_block": entry.target_block,
                    "target_texture": output_id,
                    "target_base_block": entry.variant.target_base_block,
                    "target_base_texture": target_base_texture,
                    "variant": entry.variant.key,
                })
            preview_pairs.append({
                "ore": ore,
                "regular_ore": regular_ore,
                "regular_base": regular_base,
                "hardened_base": hardened_base,
                "hardened_result": results[0]["result"],
                "deepslate_ore": deepslate_ore,
                "deepslate_base": deepslate_base,
                "petrified_base": petrified_base,
                "petrified_result": results[1]["result"],
                "mineral_pixels": results[0]["mineral_pixels"],
            })

    preview_path = docs_dir / "preview.png"
    render_preview(preview_pairs, preview_path)
    render_grain_review(preview_pairs, docs_dir / "grain_review.png")

    report = {
        "schema_version": 2,
        "tool": "tools/generate_sandified_ore_textures.py",
        "method": {
            "design": "original sparse mineral grains, six clusters per face",
            "coverage": "less than 10 percent; remaining host pixels unchanged",
            "shading": "three mineral shades, upper-left light, no stone outlines",
            "interpolation": "none",
            "encoding": "RGBA8, filter 0",
        },
        "minecraft_jar": minecraft_jar.name,
        "create_jar": create_jar.name,
        "preview_path": str(preview_path.relative_to(root)).replace("\\", "/"),
        "entries": report_entries,
    }
    if not args.check:
        write_json(docs_dir / "texture_mapping.json", {
            "schema_version": 1,
            "entries": mapping_entries,
        })
        write_json(docs_dir / "generation_report.json", report)

        lines = [
            "# 沙化矿石纹理审核", "",
            "本版改为原创的稀疏矿物颗粒，不再移植原版矿脉轮廓。",
            "每面六处小颗粒，矿物覆盖率低于 10%；其余像素精确保留沙子基底。",
            "三阶明暗、左上受光；铜矿保留铜色与氧化绿，锌矿为低饱和灰绿，宝石保持色相差异。",
            "硬化沙与石化沙共享矿物布局，依靠原有基底区分。", "",
            "| 方块 | 矿物像素 | 占比 | 保留沙子像素 |",
            "|---|---:|---:|---:|",
        ]
        for item in report_entries:
            lines.append(
                f"| {item['target_block']} | {item['mineral_pixels']} | "
                f"{item['mineral_coverage_percent']}% | {item['unchanged_host_pixels']} |"
            )
        lines.extend([
            "", "## 预览", "",
            "preview.png：源矿石、源基底、沙子基底与新版纹理并排。",
            "grain_review.png：全系列 1 倍/8 倍预览及两种基底的 3×3 连续平铺。",
            "自动检查覆盖率、完全不透明、16×16 尺寸与基底像素保留；视觉检查关注辨识度、颗粒明暗与平铺。",
        ])
        (docs_dir / "manual_review.md").write_text(
            "\n".join(lines) + "\n", encoding="utf-8", newline="\n"
        )

    print(
        f"textures={len(report_entries)} changed={changed} "
        f"preview={preview_path.relative_to(root)} check={args.check}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

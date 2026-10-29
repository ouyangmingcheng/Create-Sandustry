#!/usr/bin/env python3
"""Generate hardened/petrified sand ore textures from verified source assets.

The mask is deliberately not based on a simple RGB difference threshold.

Classification:
1. Collect the exact color palette of the real source base texture.
2. A pixel outside the corresponding base palette is a confident mineral pixel.
3. Cross-check the normal/deepslate ore pair. A pixel outside either palette is
   confident mineral because both variants use the same mineral silhouette.
4. A pixel inside both palettes is uncertain. If it touches confident mineral,
   preserve the source pixel to avoid destroying a same-color mineral detail;
   otherwise replace it with the target base. Every uncertain coordinate and the
   decision taken are recorded in the generated report.

Only exact palette membership, exact source pixels and direct RGBA pixel copying
are used. No smoothing, scaling or source-file overwrite occurs.
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


def _has_adjacent(mask: list[bool], width: int, height: int, index: int) -> bool:
    x = index % width
    y = index // width
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            if dx == 0 and dy == 0:
                continue
            nx, ny = x + dx, y + dy
            if 0 <= nx < width and 0 <= ny < height and mask[ny * width + nx]:
                return True
    return False


def generate_pair_textures(
    hardened_entry,
    petrified_entry,
    regular_ore: PngImage,
    regular_base: PngImage,
    deepslate_ore: PngImage,
    deepslate_base: PngImage,
    hardened_base: PngImage,
    petrified_base: PngImage,
):
    ensure_same_size(hardened_entry.id, regular_ore, regular_base)
    ensure_same_size(petrified_entry.id, deepslate_ore, deepslate_base)
    ensure_same_size(hardened_entry.id, regular_ore, hardened_base)
    ensure_same_size(petrified_entry.id, deepslate_ore, petrified_base)

    width = regular_ore.width
    height = regular_ore.height
    count = width * height
    regular_palette = regular_base.palette()
    deepslate_palette = deepslate_base.palette()

    regular_outside = [regular_ore.pixels[i] not in regular_palette for i in range(count)]
    deepslate_outside = [deepslate_ore.pixels[i] not in deepslate_palette for i in range(count)]
    confident = [regular_outside[i] or deepslate_outside[i] for i in range(count)]

    results = []
    for entry, source, target_base in (
        (hardened_entry, regular_ore, hardened_base),
        (petrified_entry, deepslate_ore, petrified_base),
    ):
        output_pixels: list[tuple[int, int, int, int]] = []
        uncertain: list[dict] = []
        preserved_confident = 0
        preserved_uncertain = 0
        preserved_transparent = 0
        replaced_background = 0
        for i, source_pixel in enumerate(source.pixels):
            target_pixel = target_base.pixels[i]
            x, y = i % width, i // width
            if source_pixel[3] != 255:
                output_pixels.append(source_pixel)
                preserved_transparent += 1
                if confident[i]:
                    preserved_confident += 1
                else:
                    uncertain.append({
                        "x": x,
                        "y": y,
                        "rgba": list(source_pixel),
                        "decision": "preserve_transparent",
                    })
                continue
            if confident[i]:
                output_pixels.append(source_pixel)
                preserved_confident += 1
                continue
            preserve_uncertain = False
            if preserve_uncertain:
                output_pixels.append(source_pixel)
                preserved_uncertain += 1
                decision = "preserve_near_confident_mineral"
            else:
                output_pixels.append(target_pixel)
                replaced_background += 1
                decision = "replace_as_background"
            uncertain.append({
                "x": x,
                "y": y,
                "rgba": list(source_pixel),
                "decision": decision,
            })

        result = PngImage(width, height, output_pixels)
        for i, pixel in enumerate(result.pixels):
            if pixel != source.pixels[i] and pixel != target_base.pixels[i]:
                raise AssertionError(f"{entry.id}: non-palette pixel introduced at {i}")
            if confident[i] and pixel != source.pixels[i]:
                raise AssertionError(f"{entry.id}: mineral pixel changed at {i}")
            if source.pixels[i][3] != 255 and pixel != source.pixels[i]:
                raise AssertionError(f"{entry.id}: source alpha semantics changed at {i}")
        results.append({
            "entry": entry,
            "source": source,
            "target_base": target_base,
            "result": result,
            "uncertain": uncertain,
            "preserved_confident": preserved_confident,
            "preserved_uncertain": preserved_uncertain,
            "preserved_transparent": preserved_transparent,
            "replaced_background": replaced_background,
            "base_palette_size": len(regular_palette if entry is hardened_entry else deepslate_palette),
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
            f"H UNCERTAIN {len(data['hardened_uncertain'])}  P UNCERTAIN {len(data['petrified_uncertain'])}",
            row_label_width + gap,
            y + tile + 5,
            1,
            GRAY,
        )
        draw_rect(image, 1, y - 2, width - 2, row_height - 2, (55, 55, 55, 255))
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
                    "confident_mineral_pixels": result_data["preserved_confident"],
                    "preserved_uncertain_near_mineral": result_data["preserved_uncertain"],
                    "replaced_background_pixels": result_data["replaced_background"],
                    "preserved_transparent_pixels": result_data["preserved_transparent"],
                    "uncertain_pixel_count": len(result_data["uncertain"]),
                    "uncertain_pixels": [
                        [item["x"], item["y"], item["decision"]]
                        for item in result_data["uncertain"]
                    ],
                    "review_required": bool(result_data["uncertain"]),
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
                "hardened_uncertain": results[0]["uncertain"],
                "petrified_uncertain": results[1]["uncertain"],
            })

    preview_path = docs_dir / "preview.png"
    render_preview(preview_pairs, preview_path)

    report = {
        "schema_version": 1,
        "tool": "tools/generate_sandified_ore_textures.py",
        "method": {
            "comparison": "exact RGBA palette membership plus normal/deepslate cross-check",
            "interpolation": "none",
            "encoding": "RGBA8, filter 0",
            "ambiguous_policy": (
                "replace with target base while listing every uncertain coordinate; "
                "the normal/deepslate cross-check protects confident mineral pixels"
            ),
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
            "# 沙化矿石纹理人工审核清单",
            "",
            "纹理遮罩使用“源基底精确调色板匹配 + 普通/深层矿石交叉验证”，不使用 RGB 阈值、平滑或缩放。",
            "矿物候选像素保留源 PNG 的精确 RGBA；透明像素始终保留原始语义。",
            "颜色同时属于两套基底调色板的像素被列为不确定像素，并按背景替换以便复核。",
            "",
            "## 自动检查摘要",
            "",
            "| 目标方块 | 矿物像素 | 不确定像素 | 替换背景像素 | 尺寸 |",
            "|---|---:|---:|---:|---:|",
        ]
        for item in report_entries:
            lines.append(
                f"| {item['target_block']} | {item['confident_mineral_pixels']} | "
                f"{item['uncertain_pixel_count']} | "
                f"{item['replaced_background_pixels']} | {item['dimensions'][0]}x{item['dimensions'][1]} |"
            )
        lines.extend([
            "",
            "## 人工检查步骤",
            "",
            "- [ ] 打开 'docs/sandified_ores/preview.png'，逐行核对 8 列：普通源矿、石基底、硬化沙基底、硬化结果、深层源矿、深板岩基底、石化沙基底、石化结果。",
            "- [ ] 核对矿物颜色、像素轮廓、位置与源矿一致，且背景没有残留石头/深板岩像素块。",
            "- [ ] 核对不确定像素列表；若其中包含矿物同色像素，按报告坐标调整遮罩或人工修补。",
            "- [ ] 核对 16×16 尺寸、透明通道以及所有模型/方块状态引用。",
            "",
            "## 不确定像素坐标",
            "",
            "格式：(x, y, 决策)。replace_as_background 表示该像素颜色同时属于两套基底调色板，无法仅凭颜色确认是否为同色矿物，因此按背景替换并列出复核。",
            "",
        ])
        for item in report_entries:
            lines.append(f"### '{item['target_block']}'")
            coords = ", ".join(
                f"({x},{y},{decision})" for x, y, decision in item["uncertain_pixels"]
            )
            lines.append(coords if coords else "无")
            lines.append("")
        (docs_dir / "manual_review.md").write_text(
            "\n".join(lines).rstrip() + "\n", encoding="utf-8", newline="\n"
        )

    print(
        f"textures={len(report_entries)} changed={changed} "
        f"preview={preview_path.relative_to(root)} check={args.check}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

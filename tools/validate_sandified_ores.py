#!/usr/bin/env python3
"""Static validation for sandified ore registration and generated resources."""
from __future__ import annotations

import argparse
import copy
import json
from pathlib import Path
import re

from generate_sandified_ore_data import replace_silk_touch_reference, source_loot_path
from sandified_ore_manifest import MOD_ID, entries
from sandified_png import load_png, read_png
from sandified_resources import (
    ResourceArchives,
    locate_create_jar,
    locate_minecraft_jar,
    read_json,
    repo_root,
    sha256_bytes,
    split_resource_id,
    write_json,
)


def fail(errors: list[str], message: str) -> None:
    errors.append(message)


def walk_names(value):
    if isinstance(value, dict):
        if isinstance(value.get("name"), str):
            yield value["name"]
        for child in value.values():
            yield from walk_names(child)
    elif isinstance(value, list):
        for child in value:
            yield from walk_names(child)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--minecraft-jar")
    parser.add_argument("--create-jar")
    args = parser.parse_args()

    root = repo_root()
    minecraft_jar = locate_minecraft_jar(root, args.minecraft_jar)
    create_jar = locate_create_jar(root, args.create_jar)
    errors: list[str] = []
    checks: list[str] = []
    all_entries = entries()
    expected_ids = [entry.id for entry in all_entries]

    java_path = root / "src/main/java/com/minechai/create_sandustry/content/block/SandifiedOreDefinitions.java"
    java = java_path.read_text(encoding="utf-8")
    ore_pattern = re.compile(
        r'new Ore\("([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*ToolTier\.(WOOD|STONE|IRON),\s*(\d+),\s*(\d+),\s*(true|false)\)'
    )
    java_ores = ore_pattern.findall(java)
    expected_ore_tuples = [
        (
            entry.ore.key,
            entry.ore.source_block,
            entry.ore.deepslate_source_block,
            entry.ore.tier.key.upper(),
            str(entry.ore.xp_min),
            str(entry.ore.xp_max),
            "true" if entry.ore.redstone_behavior else "false",
        )
        for entry in (e for e in all_entries if e.variant.key == "hardened")
    ]
    if java_ores != expected_ore_tuples:
        fail(errors, "Java ore definitions drift from tools/sandified_ore_manifest.py")
    else:
        checks.append("Java ore definitions match the Python manifest")

    variant_pattern = re.compile(
        r'new Variant\(\s*"([^"]+)",\s*"([^"]+)",\s*(true|false),'
    )
    variants = variant_pattern.findall(java)
    if len(variants) != 2:
        fail(errors, f"expected 2 Java variants, found {len(variants)}")
    else:
        checks.append("Java defines exactly two target base variants")

    mod_blocks = (root / "src/main/java/com/minechai/create_sandustry/registry/ModBlocks.java").read_text(
        encoding="utf-8"
    )
    mod_items = (root / "src/main/java/com/minechai/create_sandustry/registry/ModItems.java").read_text(
        encoding="utf-8"
    )
    if "SandifiedOreDefinitions.ENTRIES" not in mod_blocks or "BLOCKS.register(entry.id()" not in mod_blocks:
        fail(errors, "ModBlocks does not register every SandifiedOreDefinitions entry")
    else:
        checks.append("ModBlocks registers the manifest entries")
    if "ModBlocks.SANDIFIED_ORES.forEach" not in mod_items or "registerSimpleBlockItem(id, block)" not in mod_items:
        fail(errors, "ModItems does not create a BlockItem for every sandified ore")
    else:
        checks.append("ModItems creates a BlockItem for every sandified ore")

    if len(expected_ids) != len(set(expected_ids)):
        fail(errors, "duplicate sandified ore target IDs")
    else:
        checks.append(f"{len(expected_ids)} unique target IDs asserted")

    lang_zh = read_json(root / "src/main/resources/assets/createsandustry/lang/zh_cn.json")
    lang_en = read_json(root / "src/main/resources/assets/createsandustry/lang/en_us.json")
    report = read_json(root / "docs/sandified_ores/generation_report.json")
    report_by_target = {item["target_block"]: item for item in report["entries"]}
    loot_mapping = read_json(root / "docs/sandified_ores/loot_mapping.json")
    loot_by_target = {item["target_block"]: item for item in loot_mapping["entries"]}

    textures_dir = root / "src/main/resources/assets/createsandustry/textures/block"
    textures_checked = 0
    models_checked = 0
    loot_checked = 0
    tags_checked = 0

    shovel_values = read_json(
        root / "src/main/resources/data/minecraft/tags/block/mineable/shovel.json"
    ).get("values", [])
    wrong_pickaxe = {f"{MOD_ID}:{target}" for target in expected_ids}
    pickaxe_values = read_json(
        root / "src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json"
    ).get("values", [])
    if wrong_pickaxe & {v for v in pickaxe_values if isinstance(v, str)}:
        fail(errors, "sandified ore appears in mineable/pickaxe")
    needs_tags = {
        "minecraft:needs_stone_tool": set(),
        "minecraft:needs_iron_tool": set(),
    }
    for tag, target_set in needs_tags.items():
        path = root / f"src/main/resources/data/minecraft/tags/block/{tag.split(':', 1)[1]}.json"
        target_set.update(v for v in read_json(path).get("values", []) if isinstance(v, str))

    with ResourceArchives(minecraft_jar, create_jar) as archives:
        for expected_index, entry in enumerate(all_entries):
            target = entry.target_block
            target_id = entry.id
            target_path = f"{MOD_ID}:{target_id}"
            if target_path != target:
                fail(errors, f"target ID mismatch: {target_path} != {target}")
            if target_path not in shovel_values:
                fail(errors, f"{target_path} missing from mineable/shovel")
            else:
                tags_checked += 1
            for tag, values in needs_tags.items():
                should_have = entry.ore.tier.needs_tag == tag
                if should_have and target_path not in values:
                    fail(errors, f"{target_path} missing from {tag}")
                if not should_have and target_path in values:
                    fail(errors, f"{target_path} incorrectly present in {tag}")
            if entry.ore.tier.needs_tag is None and any(
                target_path in values for values in needs_tags.values()
            ):
                fail(errors, f"{target_path} should be wood tier but has a needs tag")

            blockstate_path = root / f"src/main/resources/assets/{MOD_ID}/blockstates/{target_id}.json"
            model_path = root / f"src/main/resources/assets/{MOD_ID}/models/block/{target_id}.json"
            item_model_path = root / f"src/main/resources/assets/{MOD_ID}/models/item/{target_id}.json"
            texture_path = textures_dir / f"{target_id}.png"
            for path in (blockstate_path, model_path, item_model_path, texture_path):
                if not path.is_file():
                    fail(errors, f"missing generated asset: {path.relative_to(root)}")
            if not all(path.is_file() for path in (blockstate_path, model_path, item_model_path, texture_path)):
                continue

            blockstate = read_json(blockstate_path)
            expected_models = {f"{MOD_ID}:block/{target_id}"}
            if entry.ore.redstone_behavior:
                if set(blockstate.get("variants", {})) != {"lit=false", "lit=true"}:
                    fail(errors, f"{target_path} redstone blockstate must define lit=false/true")
                for variant in blockstate.get("variants", {}).values():
                    if variant.get("model") not in expected_models:
                        fail(errors, f"{target_path} blockstate references wrong model")
            else:
                if set(blockstate.get("variants", {})) != {""}:
                    fail(errors, f"{target_path} should have one unconditional blockstate variant")
                if blockstate["variants"][""].get("model") not in expected_models:
                    fail(errors, f"{target_path} blockstate references wrong model")
            model = read_json(model_path)
            if model.get("textures", {}).get("all") != f"{MOD_ID}:block/{target_id}":
                fail(errors, f"{target_path} model has wrong all texture")
            item_model = read_json(item_model_path)
            if item_model.get("parent") != f"{MOD_ID}:block/{target_id}":
                fail(errors, f"{target_path} item model has wrong parent")
            image = load_png(texture_path)
            if (image.width, image.height) != (16, 16):
                fail(errors, f"{target_path} texture is not 16x16")
            report_item = report_by_target.get(target)
            if not report_item:
                fail(errors, f"{target_path} absent from texture generation report")
            else:
                if report_item["output_texture_sha256"] != sha256_bytes(texture_path.read_bytes()):
                    fail(errors, f"{target_path} texture hash differs from report")
                if report_item["dimensions"] != [16, 16]:
                    fail(errors, f"{target_path} report dimension mismatch")
            models_checked += 1
            textures_checked += 1

            lang_key = f"block.{MOD_ID}.{target_id}"
            if not lang_zh.get(lang_key):
                fail(errors, f"missing zh_cn language entry {lang_key}")
            if not lang_en.get(lang_key):
                fail(errors, f"missing en_us language entry {lang_key}")

            namespace, loot_relative = source_loot_path(entry.source_block)
            source_table = archives.data_json(namespace, loot_relative)
            target_loot_path = root / f"src/main/resources/data/{MOD_ID}/loot_table/blocks/{target_id}.json"
            if not target_loot_path.is_file():
                fail(errors, f"missing loot table {target_loot_path.relative_to(root)}")
                continue
            target_table = read_json(target_loot_path)
            expected = copy.deepcopy(source_table)
            replaced = replace_silk_touch_reference(expected, entry.source_block, target)
            if replaced < 1:
                fail(errors, f"{entry.source_block} has no silk touch block reference")
            expected["random_sequence"] = f"{MOD_ID}:blocks/{target_id}"
            if target_table != expected:
                fail(errors, f"{target_path} loot table differs from adapted source table")
            names = list(walk_names(target_table))
            if entry.source_block in names:
                fail(errors, f"{target_path} loot table still drops source block {entry.source_block}")
            if target not in names:
                fail(errors, f"{target_path} loot table has no silk-touch self drop")
            mapped = loot_by_target.get(target)
            if not mapped:
                fail(errors, f"{target_path} absent from loot mapping")
            else:
                if mapped["source_block"] != entry.source_block:
                    fail(errors, f"{target_path} loot mapping source mismatch")
                if mapped["preserved_resource_dependencies"] is None:
                    fail(errors, f"{target_path} dependency report missing")
            loot_checked += 1

    checks.extend([
        f"parsed {loot_checked} loot tables and verified exact source adaptation",
        f"validated {models_checked} blockstates/models/item models",
        f"validated {textures_checked} 16x16 RGBA textures and report hashes",
        f"validated {tags_checked} shovel tag entries and actual needs_* tier tags",
    ])

    result = {
        "schema_version": 1,
        "passed": not errors,
        "checks": checks,
        "errors": errors,
        "counts": {
            "entries": len(all_entries),
            "unique_ids": len(set(expected_ids)),
            "loot_tables": loot_checked,
            "models": models_checked,
            "textures": textures_checked,
            "shovel_tag_entries": tags_checked,
        },
    }
    write_json(root / "docs/sandified_ores/static_validation.json", result)
    lines = [
        "# 沙化矿石静态验证报告",
        "",
        f"- 结果：{'通过' if not errors else '失败'}",
        f"- 目标方块：{len(all_entries)}",
        f"- 唯一 ID：{len(set(expected_ids))}",
        f"- 战利品表：{loot_checked}",
        f"- 模型/方块状态：{models_checked}",
        f"- 纹理：{textures_checked}",
        f"- 锹标签条目：{tags_checked}",
        "",
        "## 检查项",
        "",
    ]
    lines.extend(f"- {'PASS' if not errors else 'CHECK'} {check}" for check in checks)
    if errors:
        lines.extend(["", "## 错误", ""])
        lines.extend(f"- {error}" for error in errors)
    (root / "docs/sandified_ores/static_validation.md").write_text(
        "\n".join(lines) + "\n", encoding="utf-8", newline="\n"
    )
    for check in checks:
        print(f"PASS {check}")
    for error in errors:
        print(f"FAIL {error}")
    return 0 if not errors else 1


if __name__ == "__main__":
    raise SystemExit(main())

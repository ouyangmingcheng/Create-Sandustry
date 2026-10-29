#!/usr/bin/env python3
"""Generate loot tables, block tags and language keys for sandified ores.

Loot tables are adapted from the exact source tables shipped by the installed
Minecraft/Create JARs. Only the silk-touch block-item reference and the loot
table random sequence are changed. Mineral items, counts, conditions,
enchantment functions and explosion behavior are preserved verbatim.
"""
from __future__ import annotations

import argparse
import copy
from pathlib import Path

from sandified_ore_manifest import entries, lang_name
from sandified_resources import (
    ResourceArchives,
    locate_create_jar,
    locate_minecraft_jar,
    read_json,
    repo_root,
    split_resource_id,
    write_json,
)


MINEABLE_SHOVEL = "minecraft:mineable/shovel"
MINEABLE_PICKAXE = "minecraft:mineable/pickaxe"


def source_loot_path(source_block: str) -> tuple[str, str]:
    namespace, path = split_resource_id(source_block)
    return namespace, f"loot_table/blocks/{path}.json"


def collect_dependencies(value, dependencies: set[str], key: str | None = None) -> None:
    if isinstance(value, dict):
        for child_key, child in value.items():
            if child_key in {
                "type", "condition", "function", "enchantment", "formula"
            } and isinstance(child, str):
                dependencies.add(child)
            collect_dependencies(child, dependencies, child_key)
    elif isinstance(value, list):
        for child in value:
            collect_dependencies(child, dependencies, key)


def replace_silk_touch_reference(value, source_block: str, target_block: str) -> int:
    replaced = 0
    if isinstance(value, dict):
        if value.get("name") == source_block:
            value["name"] = target_block
            replaced += 1
        for child in value.values():
            replaced += replace_silk_touch_reference(child, source_block, target_block)
    elif isinstance(value, list):
        for child in value:
            replaced += replace_silk_touch_reference(child, source_block, target_block)
    return replaced


def merged_tag_values(path: Path, additions: list[str]) -> list[str]:
    if path.exists():
        data = read_json(path)
        values = data.get("values", [])
    else:
        values = []
    normalized = []
    seen = set()
    for value in values:
        key = value if isinstance(value, str) else repr(value)
        if key not in seen:
            normalized.append(value)
            seen.add(key)
    for value in additions:
        if value not in seen:
            normalized.append(value)
            seen.add(value)
    return normalized


def write_tag(path: Path, additions: list[str]) -> None:
    write_json(path, {
        "replace": False,
        "values": merged_tag_values(path, additions),
    })


def update_lang(path: Path, additions: dict[str, str]) -> None:
    data = read_json(path) if path.exists() else {}
    for key, value in additions.items():
        data[key] = value
    write_json(path, data)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--minecraft-jar")
    parser.add_argument("--create-jar")
    args = parser.parse_args()

    root = repo_root()
    minecraft_jar = locate_minecraft_jar(root, args.minecraft_jar)
    create_jar = locate_create_jar(root, args.create_jar)
    loot_dir = root / "src/main/resources/data/createsandustry/loot_table/blocks"
    minecraft_block_tags = root / "src/main/resources/data/minecraft/tags/block"
    lang_dir = root / "src/main/resources/assets/createsandustry/lang"
    docs_dir = root / "docs/sandified_ores"
    loot_dir.mkdir(parents=True, exist_ok=True)
    minecraft_block_tags.mkdir(parents=True, exist_ok=True)
    docs_dir.mkdir(parents=True, exist_ok=True)

    all_entries = entries()
    shovel_blocks = [entry.target_block for entry in all_entries]
    stone_blocks = [entry.target_block for entry in all_entries if entry.ore.tier.needs_tag == "minecraft:needs_stone_tool"]
    iron_blocks = [entry.target_block for entry in all_entries if entry.ore.tier.needs_tag == "minecraft:needs_iron_tool"]
    write_tag(minecraft_block_tags / "mineable/shovel.json", shovel_blocks)
    write_tag(minecraft_block_tags / "needs_stone_tool.json", stone_blocks)
    write_tag(minecraft_block_tags / "needs_iron_tool.json", iron_blocks)

    pickaxe_path = minecraft_block_tags / "mineable/pickaxe.json"
    if pickaxe_path.exists():
        pickaxe_values = merged_tag_values(pickaxe_path, [])
        accidental = sorted(set(shovel_blocks) & {v for v in pickaxe_values if isinstance(v, str)})
        if accidental:
            raise SystemExit(f"sandified ores incorrectly present in pickaxe tag: {accidental}")

    mapping = []
    with ResourceArchives(minecraft_jar, create_jar) as archives:
        for entry in all_entries:
            namespace, loot_path = source_loot_path(entry.source_block)
            source_table = archives.data_json(namespace, loot_path)
            target_table = copy.deepcopy(source_table)
            replaced = replace_silk_touch_reference(
                target_table, entry.source_block, entry.target_block
            )
            if replaced < 1:
                raise SystemExit(
                    f"{entry.source_block}: source loot table has no block item reference"
                )
            target_table["random_sequence"] = f"createsandustry:blocks/{entry.id}"
            dependencies: set[str] = set()
            collect_dependencies(source_table, dependencies)
            write_json(loot_dir / f"{entry.id}.json", target_table)
            mapping.append({
                "target_block": entry.target_block,
                "source_block": entry.source_block,
                "source_loot_table": f"{namespace}:{loot_path.removesuffix('.json')}",
                "target_loot_table": f"createsandustry:loot_table/blocks/{entry.id}",
                "replaced_silk_touch_block_references": replaced,
                "preserved_resource_dependencies": sorted(dependencies),
                "xp_min": entry.ore.xp_min,
                "xp_max": entry.ore.xp_max,
                "redstone_behavior_preserved": entry.ore.redstone_behavior,
            })

    zh = {f"block.createsandustry.{entry.id}": lang_name(entry, "zh_cn") for entry in all_entries}
    en = {f"block.createsandustry.{entry.id}": lang_name(entry, "en_us") for entry in all_entries}
    update_lang(lang_dir / "zh_cn.json", zh)
    update_lang(lang_dir / "en_us.json", en)

    write_json(docs_dir / "loot_mapping.json", {
        "schema_version": 1,
        "policy": (
            "copy exact source table; replace only the source block item name in "
            "the silk-touch branch; preserve all other entries/functions/conditions; "
            "set target random sequence"
        ),
        "entries": mapping,
        "tags": {
            MINEABLE_SHOVEL: shovel_blocks,
            "minecraft:needs_stone_tool": stone_blocks,
            "minecraft:needs_iron_tool": iron_blocks,
            MINEABLE_PICKAXE: "validated absent",
        },
    })
    print(f"loot_tables={len(all_entries)} tags=3 languages=2")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

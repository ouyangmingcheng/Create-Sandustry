#!/usr/bin/env python3
"""Shared, verified manifest for the sandified ore feature.

The source block IDs and tool tiers below were checked against:
- Minecraft 1.21.1 client resources/data
- Create 1.21.1 6.0.10-280 slim JAR

Keep this manifest in sync with
src/main/java/com/minechai/create_candustry/block/SandifiedOreDefinitions.java.
The validator checks the generated assets and Java IDs for drift.
"""
from __future__ import annotations

from dataclasses import dataclass
from typing import Iterable, Optional

MOD_ID = "createsandustry"


@dataclass(frozen=True)
class ToolTier:
    key: str
    needs_tag: Optional[str]


WOOD = ToolTier("wood", None)
STONE = ToolTier("stone", "minecraft:needs_stone_tool")
IRON = ToolTier("iron", "minecraft:needs_iron_tool")


@dataclass(frozen=True)
class Ore:
    key: str
    source_block: str
    deepslate_source_block: str
    source_base_block: str
    deepslate_source_base_block: str
    tier: ToolTier
    xp_min: int
    xp_max: int
    redstone_behavior: bool = False


@dataclass(frozen=True)
class Variant:
    key: str
    prefix: str
    source_attribute: str
    target_base_block: str


@dataclass(frozen=True)
class Entry:
    ore: Ore
    variant: Variant

    @property
    def id(self) -> str:
        return f"{self.variant.prefix}_{self.ore.key}_ore"

    @property
    def source_block(self) -> str:
        return getattr(self.ore, self.variant.source_attribute)

    @property
    def target_block(self) -> str:
        return f"{MOD_ID}:{self.id}"

    @property
    def source_base_block(self) -> str:
        if self.variant.source_attribute == "deepslate_source_block":
            return self.ore.deepslate_source_base_block
        return self.ore.source_base_block


ORES: tuple[Ore, ...] = (
    Ore("coal", "minecraft:coal_ore", "minecraft:deepslate_coal_ore",
        "minecraft:stone", "minecraft:deepslate", WOOD, 0, 2),
    Ore("iron", "minecraft:iron_ore", "minecraft:deepslate_iron_ore",
        "minecraft:stone", "minecraft:deepslate", STONE, 0, 0),
    Ore("copper", "minecraft:copper_ore", "minecraft:deepslate_copper_ore",
        "minecraft:stone", "minecraft:deepslate", STONE, 0, 0),
    Ore("gold", "minecraft:gold_ore", "minecraft:deepslate_gold_ore",
        "minecraft:stone", "minecraft:deepslate", IRON, 0, 0),
    Ore("redstone", "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore",
        "minecraft:stone", "minecraft:deepslate", IRON, 1, 5, True),
    Ore("lapis", "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore",
        "minecraft:stone", "minecraft:deepslate", STONE, 2, 5),
    Ore("diamond", "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore",
        "minecraft:stone", "minecraft:deepslate", IRON, 3, 7),
    Ore("emerald", "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore",
        "minecraft:stone", "minecraft:deepslate", IRON, 3, 7),
    Ore("zinc", "create:zinc_ore", "create:deepslate_zinc_ore",
        "minecraft:stone", "minecraft:deepslate", IRON, 0, 0),
)

HARDENED = Variant(
    "hardened", "hardened_sand", "source_block", f"{MOD_ID}:hardened_sand"
)
PETRIFIED = Variant(
    "petrified", "petrified_sand", "deepslate_source_block", f"{MOD_ID}:petrified_sand"
)
VARIANTS: tuple[Variant, ...] = (HARDENED, PETRIFIED)


def entries() -> tuple[Entry, ...]:
    return tuple(Entry(ore, variant) for variant in VARIANTS for ore in ORES)


def entry_for(ore_key: str, variant_key: str) -> Entry:
    for entry in entries():
        if entry.ore.key == ore_key and entry.variant.key == variant_key:
            return entry
    raise KeyError((ore_key, variant_key))


def target_ids() -> tuple[str, ...]:
    return tuple(entry.id for entry in entries())


def lang_name(entry: Entry, language: str) -> str:
    mineral_zh = {
        "coal": "煤",
        "iron": "铁",
        "copper": "铜",
        "gold": "金",
        "redstone": "红石",
        "lapis": "青金石",
        "diamond": "钻石",
        "emerald": "绿宝石",
        "zinc": "锌",
    }[entry.ore.key]
    mineral_en = {
        "coal": "Coal",
        "iron": "Iron",
        "copper": "Copper",
        "gold": "Gold",
        "redstone": "Redstone",
        "lapis": "Lapis",
        "diamond": "Diamond",
        "emerald": "Emerald",
        "zinc": "Zinc",
    }[entry.ore.key]
    if language == "zh_cn":
        base = "硬化沙子" if entry.variant.key == "hardened" else "石化沙子"
        return f"{base}{mineral_zh}矿石"
    if language == "en_us":
        base = "Hardened Sand" if entry.variant.key == "hardened" else "Petrified Sand"
        return f"{base} {mineral_en} Ore"
    raise ValueError(language)


def iter_source_pairs() -> Iterable[tuple[Ore, Entry, Entry]]:
    for ore in ORES:
        yield ore, entry_for(ore.key, "hardened"), entry_for(ore.key, "petrified")

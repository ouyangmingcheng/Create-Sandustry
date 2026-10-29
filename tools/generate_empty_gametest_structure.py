#!/usr/bin/env python3
"""Write the minimal 1x1x1 air structure used by the NeoForge game test."""
from __future__ import annotations

import gzip
from pathlib import Path
import struct


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "src/main/resources/data/createsandustry/structure/empty.nbt"
DATA_VERSION = 3955


def tag_int(value: int) -> bytes:
    return struct.pack(">i", value)


def tag_int_list(values: tuple[int, ...]) -> bytes:
    return struct.pack(">i", len(values)) + b"".join(tag_int(value) for value in values)


def named(tag_type: int, name: str, payload: bytes) -> bytes:
    encoded = name.encode("utf-8")
    return bytes([tag_type]) + struct.pack(">H", len(encoded)) + encoded + payload


def compound(entries: list[tuple[int, str, bytes]]) -> bytes:
    return b"".join(named(tag_type, name, payload) for tag_type, name, payload in entries) + b"\x00"


def main() -> int:
    palette_entry = compound([
        (8, "Name", struct.pack(">H", len(b"minecraft:air")) + b"minecraft:air"),
    ])
    block_entry = compound([
        (9, "pos", b"\x03" + tag_int_list((0, 0, 0))),
        (3, "state", tag_int(0)),
    ])
    root_payload = compound([
        (3, "DataVersion", tag_int(DATA_VERSION)),
        (9, "size", b"\x03" + tag_int_list((1, 1, 1))),
        (9, "palette", b"\x0a" + struct.pack(">i", 1) + palette_entry),
        (9, "blocks", b"\x0a" + struct.pack(">i", 1) + block_entry),
        (9, "entities", b"\x0a" + struct.pack(">i", 0)),
    ])
    root = b"\x0a\x00\x00" + root_payload
    OUT.parent.mkdir(parents=True, exist_ok=True)
    with OUT.open("wb") as output:
        with gzip.GzipFile(fileobj=output, mode="wb", compresslevel=9, mtime=0) as handle:
            handle.write(root)
    print(OUT.relative_to(ROOT))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

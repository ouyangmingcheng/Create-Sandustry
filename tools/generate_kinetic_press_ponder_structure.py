#!/usr/bin/env python3
"""Generate the compact 5x8x5 Kinetic Press Ponder stage."""
from pathlib import Path
import gzip
import struct

from generate_empty_gametest_structure import DATA_VERSION, compound, tag_int, tag_int_list


def main() -> None:
    size = (5, 8, 5)
    palette = b"".join(compound([
        (8, "Name", struct.pack(">H", len(name)) + name),
    ]) for name in (b"minecraft:smooth_stone", b"minecraft:air"))
    blocks = []
    for x in range(size[0]):
        for y in range(size[1]):
            for z in range(size[2]):
                blocks.append(compound([
                    (9, "pos", b"\x03" + tag_int_list((x, y, z))),
                    (3, "state", tag_int(0 if y == 0 else 1)),
                ]))
    root = b"\x0a\x00\x00" + compound([
        (3, "DataVersion", tag_int(DATA_VERSION)),
        (9, "size", b"\x03" + tag_int_list(size)),
        (9, "palette", b"\x0a" + tag_int(2) + palette),
        (9, "blocks", b"\x0a" + tag_int(len(blocks)) + b"".join(blocks)),
        (9, "entities", b"\x0a" + tag_int(0)),
    ])
    output = Path(__file__).resolve().parents[1] / (
        "src/main/resources/assets/createsandustry/ponder/kinetic_press/basic.nbt")
    output.write_bytes(gzip.compress(root, mtime=0))
    print(output)


if __name__ == "__main__":
    main()

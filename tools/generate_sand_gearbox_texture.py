"""Draw the sand-cased shaft bearing face at its native 16x16 resolution."""
from pathlib import Path

from sandified_png import PngImage, write_png


# Warm metal shares the casing palette; the recessed bearing uses dark steel.
PALETTE = {
    "a": (158, 105, 71, 255),  # outer frame
    "b": (114, 71, 49, 255),  # frame shadow
    "h": (255, 235, 140, 255),  # gold highlight
    "l": (247, 203, 108, 255),  # gold light
    "g": (211, 175, 109, 255),  # sand gold
    "m": (181, 139, 82, 255),  # plate shadow
    "d": (98, 67, 41, 255),  # recessed plate
    "s": (71, 48, 31, 255),  # lower bevel
    "i": (81, 81, 81, 255),  # bearing highlight
    "j": (48, 47, 45, 255),  # bearing shadow
    "o": (26, 25, 25, 255),  # shaft opening
}

ROWS = (
    "baaaaaaaaaaaaaab",
    "alhhhhhhhhhhhhlb",
    "ahgggggggggggglb",
    "ahggmddddddmgglb",
    "ahgmddiiiiddmglb",
    "ahgddijjjjiidmgb",
    "ahgdiioooojjdmgb",
    "ahgdioooooojdmgb",
    "algdioooooojdmgb",
    "algdiioooojjdmmb",
    "algddjjjjjjddmmb",
    "aggmddjjjjddmmmb",
    "aggmmddddddmmmmb",
    "aggmmmmmmmmmmmmb",
    "algggggmmmmmmmmb",
    "abbbbbbbbbbbbbbb",
)


def main():
    assert len(ROWS) == 16 and all(len(row) == 16 for row in ROWS)
    image = PngImage(16, 16, [PALETTE[p] for row in ROWS for p in row])
    target = Path(__file__).resolve().parents[1] / (
        "src/main/resources/assets/createsandustry/textures/block/sand_gearbox.png"
    )
    target.write_bytes(write_png(image))
    print(target)


if __name__ == "__main__":
    main()

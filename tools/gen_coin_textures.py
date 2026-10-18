"""
Sandustry coin texture generator (single + stack).

Generates both coin texture sets from the two greyscale masters and archives
both sets plus the masters into ./coin_textures/.

  master (single) : src/main/resources/assets/createsandustry/textures/item/icon.png
  master (stack)  : src/main/resources/assets/createsandustry/textures/item/icon_stack.png

  game resources  : src/main/resources/assets/createsandustry/textures/item/*_coin.png
                    src/main/resources/assets/createsandustry/textures/item/*_coin_stack.png
  dev archive     : coin_textures/single/**, coin_textures/stack/**, README.md

This file is a development-time tool.  Nothing here is loaded by Minecraft and
no Minecraft runtime dependency is introduced.

Colour model
------------
Both masters are pure greyscale images with a small set of discrete tones, so the
recolour is a rank specification ("palette swap") instead of a tint: the tones of
a master are normalised onto t in [0,1] and a per-material colour profile maps
t -> colour.  The profile is 9 knots of (hue, saturation) plus a luma window; the
lightness is then solved so the colour's Rec.709 luma lands on the window value.
Sampling the very same profile with the stack master's own tones is what makes a
stack texture and its single-coin texture share one colour system: the darkest
pixel of both gets the profile's darkest colour and the brightest pixel of both
gets the profile's brightest colour.

Safety
------
Writes go through write_once(): an existing file is only replaced when the new
bytes are identical, so re-running never recolours or damages anything already
generated.  Both masters are opened read-only.
"""

import colorsys
import filecmp
import json
import os
import shutil
import struct
import sys

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSET_DIR = os.path.join(ROOT, "src", "main", "resources", "assets",
                         "createsandustry", "textures", "item")
ARCHIVE = os.path.join(ROOT, "coin_textures")
MASTER_SINGLE = os.path.join(ASSET_DIR, "icon.png")
MASTER_STACK = os.path.join(ASSET_DIR, "icon_stack.png")

NAMES = ["sand", "copper", "iron", "zinc", "gold", "brass", "diamond",
         "emerald", "netherite", "obsidian", "rainbow"]
LABELS = {
    "sand": u"\u6c99\u5b50\u5e01", "copper": u"\u94dc\u5e01", "iron": u"\u94c1\u5e01",
    "zinc": u"\u950c\u5e01", "gold": u"\u91d1\u5e01", "brass": u"\u9ec4\u94dc\u5e01",
    "diamond": u"\u94bb\u77f3\u5e01", "emerald": u"\u7eff\u5b9d\u77f3\u5e01",
    "netherite": u"\u4e0b\u754c\u5408\u91d1\u5e01", "obsidian": u"\u9ed1\u66dc\u77f3\u5e01",
    "rainbow": u"\u5f69\u8679\u5e01",
}

RAINBOW = dict(sat=0.78, lmin=60, lmax=236, frames=48, frametime=2)

# ---------------------------------------------------------------------------
# Create 6.0.10-280 reference colours, read straight out of the mod jar
# (create-1.21.1-6.0.10-280-slim.jar).  Only genuine Create pixel colours are
# used.  zinc_ingot.png / brass_ingot.png / brass_nugget.png / brass_sheet.png /
# zinc_block.png / brass_block.png were all inspected; see README.md.
# ---------------------------------------------------------------------------
CREATE_ZINC = ["#213B3F", "#31544E", "#506D63", "#788273", "#929B87",
               "#A5C0A0", "#B5D1BA", "#B9E9C1", "#D6FFDC"]
CREATE_BRASS = ["#602F24", "#76462D", "#995A3D", "#B57849", "#BF8E55",
                "#D3A155", "#E4B763", "#F7CB6C", "#FFEB94"]

# designed materials: hues[9], sats[9], luma_min, luma_max, gamma
DESIGNED = {
    # dry, matte sand: shallow luma window (no polished specular), warm hue
    "sand":      ([46, 46, 45, 44, 43, 42, 41, 40, 38],
                  [.40, .41, .42, .44, .46, .49, .52, .55, .58], 118, 228, 0.90),
    "copper":    ([16, 17, 18, 20, 22, 24, 26, 28, 30],
                  [.50, .52, .54, .57, .60, .66, .72, .78, .82],  50, 202, 1.00),
    "iron":      ([210, 210, 210, 210, 210, 210, 210, 210, 210],
                  [.05, .05, .05, .05, .05, .05, .055, .06, .06], 62, 236, 1.00),
    "gold":      ([38, 39, 40, 41, 42, 43, 45, 46, 47],
                  [.56, .58, .60, .64, .68, .74, .80, .86, .92],  58, 234, 1.00),
    "diamond":   ([198, 197, 196, 194, 192, 190, 188, 186, 184],
                  [.60, .61, .62, .64, .66, .70, .74, .80, .86],  60, 216, 1.00),
    "emerald":   ([152, 152, 151, 150, 149, 148, 147, 146, 146],
                  [.58, .59, .60, .62, .64, .68, .72, .78, .85],  56, 218, 1.00),
    # heavy, rare, dark: warm charcoal metal, highlight stays low
    "netherite": ([16, 17, 18, 20, 21, 22, 24, 26, 28],
                  [.22, .21, .20, .19, .18, .17, .16, .15, .14],  30, 112, 1.05),
    # dark purple-black body, low saturation blue-purple highlight
    "obsidian":  ([270, 271, 272, 273, 275, 276, 277, 279, 282],
                  [.40, .39, .38, .37, .36, .34, .32, .30, .28],  14,  78, 1.00),
}


# ---------------------------------------------------------------------------
# colour helpers
# ---------------------------------------------------------------------------
def luma(rgb):
    r, g, b = rgb[:3]
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def hsl_to_rgb255(hue_deg, sat, light):
    r, g, b = colorsys.hls_to_rgb(hue_deg / 360.0, light, sat)
    return (int(round(r * 255)), int(round(g * 255)), int(round(b * 255)))


def solve_lightness(hue_deg, sat, target_luma):
    """HSL lightness giving exactly target_luma for a fixed hue/saturation."""
    lo, hi = 0.0, 1.0
    for _ in range(48):
        mid = (lo + hi) / 2.0
        if luma(hsl_to_rgb255(hue_deg, sat, mid)) < target_luma:
            lo = mid
        else:
            hi = mid
    return (lo + hi) / 2.0


def design_color(hue_deg, sat, target_luma):
    return hsl_to_rgb255(hue_deg, sat, solve_lightness(hue_deg, sat, target_luma))


def hex_to_rgb(h):
    return (int(h[1:3], 16), int(h[3:5], 16), int(h[5:7], 16))


def rgb_to_hsl(rgb):
    r, g, b = [v / 255.0 for v in rgb]
    h, l, s = colorsys.rgb_to_hls(r, g, b)
    return h * 360.0, s


def hexs(rgb):
    return "#%02X%02X%02X" % tuple(rgb)


def interp(knot_t, knot_v, t):
    """Piecewise linear interpolation; returns knot values exactly at knots."""
    if t <= knot_t[0]:
        return knot_v[0]
    if t >= knot_t[-1]:
        return knot_v[-1]
    for i in range(len(knot_t) - 1):
        if knot_t[i] <= t <= knot_t[i + 1]:
            span = knot_t[i + 1] - knot_t[i]
            if span <= 0.0:
                return knot_v[i]
            return knot_v[i] + (knot_v[i + 1] - knot_v[i]) * ((t - knot_t[i]) / span)
    return knot_v[-1]


# ---------------------------------------------------------------------------
# material profiles
# ---------------------------------------------------------------------------
class Profile(object):
    """t in [0,1] -> RGB.

    Two kinds of profile:
      * "designed"   : 9 knots of (hue, saturation) + a luma window; the lightness
                       is solved so the colour's Rec.709 luma hits the window.
      * "create-ref" : 9 knots that are real Create pixel colours; they are
                       returned verbatim at the knots and linearly blended in RGB
                       in between, so no Create colour is ever re-derived.
    """

    def __init__(self, name, knot_t, tag, hues=None, sats=None, luma_fn=None,
                 rgb_knots=None):
        self.name = name
        self.knot_t = knot_t
        self.tag = tag
        self.hues = hues
        self.sats = sats
        self.luma_fn = luma_fn
        self.rgb_knots = rgb_knots

    def color(self, t):
        if self.rgb_knots is not None:
            return tuple(int(round(interp(self.knot_t, [c[i] for c in self.rgb_knots], t)))
                         for i in range(3))
        hue = interp(self.knot_t, self.hues, t)
        sat = interp(self.knot_t, self.sats, t)
        return design_color(hue, sat, self.luma_fn(t))

    def ramp(self, greys):
        gmin, gmax = greys[0], greys[-1]
        return {g: self.color((g - gmin) / float(gmax - gmin)) for g in greys}

    def lumas(self, greys):
        return [luma(c) for c in self.ramp(greys).values()]


def build_profiles(knot_t):
    """knot_t = normalised tone positions of the single-coin master's tones."""
    profiles = {}
    for name, (hues, sats, lmin, lmax, gamma) in DESIGNED.items():
        def luma_fn(t, lmin=lmin, lmax=lmax, gamma=gamma):
            return lmin + (t ** gamma) * (lmax - lmin)
        profiles[name] = Profile(name, knot_t, "designed", hues=hues, sats=sats,
                                 luma_fn=luma_fn)
    for name, palette in (("zinc", CREATE_ZINC), ("brass", CREATE_BRASS)):
        profiles[name] = Profile(name, knot_t, "create-ref",
                                 rgb_knots=[hex_to_rgb(h) for h in palette])
    return profiles


# ---------------------------------------------------------------------------
# rendering
# ---------------------------------------------------------------------------
def load_master(path):
    im = Image.open(path)
    mode = im.mode
    rgba = im.convert("RGBA")
    w, h = rgba.size
    px = rgba.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a > 0 and not (r == g == b):
                raise SystemExit("master %s is not greyscale at (%d,%d)" % (path, x, y))
    greys = sorted({px[x, y][0] for y in range(h) for x in range(w) if px[x, y][3] > 0})
    return dict(path=path, image=rgba, px=px, w=w, h=h, greys=greys, mode=mode)


def render_static(master, profile):
    w, h, px = master["w"], master["h"], master["px"]
    lut = profile.ramp(master["greys"])
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    op = out.load()
    for y in range(h):
        for x in range(w):
            r, _g, _b, a = px[x, y]
            op[x, y] = (0, 0, 0, 0) if a == 0 else lut[r] + (255,)
    return out, lut


def render_rainbow(master, cfg):
    w, h, px = master["w"], master["h"], master["px"]
    greys = master["greys"]
    gmin, gmax = greys[0], greys[-1]
    span = float(w + h - 2)
    n, sat = cfg["frames"], cfg["sat"]
    lmin, lmax = cfg["lmin"], cfg["lmax"]
    luma_of = {g: lmin + ((g - gmin) / float(gmax - gmin)) * (lmax - lmin) for g in greys}

    strip = Image.new("RGBA", (w, h * n), (0, 0, 0, 0))
    sp = strip.load()
    for f in range(n):
        offset = 360.0 * f / n
        frame_lut = {}
        for band in range(int(span) + 1):
            hue = ((band / span) * 360.0 + offset) % 360.0
            for g in greys:
                frame_lut[(band, g)] = design_color(hue, sat, luma_of[g])
        for y in range(h):
            for x in range(w):
                r, _g, _b, a = px[x, y]
                sp[x, y + f * h] = ((0, 0, 0, 0) if a == 0
                                    else frame_lut[(x + y, r)] + (255,))
    return strip


def mcmeta_bytes(cfg, w, h):
    meta = {"animation": {"frametime": cfg["frametime"], "width": w, "height": h,
                          "frames": list(range(cfg["frames"]))}}
    return (json.dumps(meta, indent=2) + "\n").encode("utf-8")


def png_bytes(image):
    import io
    buf = io.BytesIO()
    image.save(buf, format="PNG")
    return buf.getvalue()


# ---------------------------------------------------------------------------
# safe writing
# ---------------------------------------------------------------------------
report_lines = []
skipped = []
written = []
unchanged = []


def write_once(path, data, protect=True):
    """Write bytes.

    protect=True  -> existing files are never replaced unless the bytes match
                     (used for the single-coin set, which must not be recoloured).
    protect=False -> the file is this task's own output and may be refreshed.
    """
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if os.path.exists(path):
        with open(path, "rb") as fh:
            if fh.read() == data:
                unchanged.append(path)
                return "unchanged"
        if protect:
            skipped.append(path)
            return "skipped"
        with open(path, "wb") as fh:
            fh.write(data)
        written.append(path)
        return "updated"
    with open(path, "wb") as fh:
        fh.write(data)
    written.append(path)
    return "created"


def copy_to(src, dst):
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    shutil.copyfile(src, dst)
    return filecmp.cmp(src, dst, shallow=False)


# ---------------------------------------------------------------------------
# preview
# ---------------------------------------------------------------------------
def checkerboard(size, cell=4):
    img = Image.new("RGBA", size, (150, 150, 150, 255))
    px = img.load()
    for y in range(size[1]):
        for x in range(size[0]):
            if ((x // cell) + (y // cell)) % 2:
                px[x, y] = (205, 205, 205, 255)
    return img


def load_font(size):
    from PIL import ImageFont
    for p in (r"C:\Windows\Fonts\msyh.ttc", r"C:\Windows\Fonts\simhei.ttf"):
        if os.path.exists(p):
            try:
                return ImageFont.truetype(p, size), True
            except Exception:
                pass
    return ImageFont.load_default(), False


def build_preview(singles, stacks, out_path, scale=8):
    """Two rows of 11 coins: single coins on top, the stack coins underneath."""
    font, cjk = load_font(14)
    sample = singles["sand"]
    cellw, cellh = sample.width * scale, sample.height * scale
    pad, label_h = 8, 22
    cols = len(NAMES)
    size = (cols * (cellw + pad) + pad, 2 * (cellh + label_h + pad) + pad)
    sheet = checkerboard(size)
    draw = ImageDraw.Draw(sheet)
    for row, group in enumerate((singles, stacks)):
        for i, name in enumerate(NAMES):
            src = group[name]
            tile = src.crop((0, 0, src.width, src.height))
            big = tile.resize((cellw, cellh), Image.NEAREST)
            cx = pad + i * (cellw + pad)
            cy = pad + row * (cellh + label_h + pad)
            sheet.paste(big, (cx, cy), big)
            if cjk:
                label = LABELS[name] + (u" \u5806\u53e0" if row else u" \u5355\u679a")
            else:
                label = name + ("_stack" if row else "")
            draw.text((cx + 2, cy + cellh + 3), label, fill=(20, 20, 20, 255), font=font)
    sheet.save(out_path)
    return out_path


# ---------------------------------------------------------------------------# README
# ---------------------------------------------------------------------------
README = u"""# coin_textures — \u786c\u5e01\u7eb9\u7406\u5f52\u6863\u76ee\u5f55

\u672c\u76ee\u5f55\u662f**\u5f00\u53d1\u8d44\u6e90\u5f52\u6863**\uff0c\u4e0d\u662f Minecraft \u8fd0\u884c\u65f6\u8d44\u6e90\u76ee\u5f55\uff0c
\u4e0d\u8981\u76f4\u63a5\u5f53\u4f5c\u8d44\u6e90\u5305\u52a0\u8f7d\u3002\u6b63\u5f0f\u6e38\u620f\u8d44\u6e90\u4ecd\u4f4d\u4e8e
`src/main/resources/assets/createsandustry/textures/item/`\u3002

## \u76ee\u5f55\u7ed3\u6784

```
coin_textures/
\u251c\u2500 README.md                   \u672c\u6587\u4ef6
\u251c\u2500 preview_coins.png           \u5f00\u53d1\u9884\u89c8\u62fc\u56fe\uff08\u4e0a\u6392\u5355\u679a\u3001\u4e0b\u6392\u5806\u53e0\uff09
\u251c\u2500 single/
\u2502   \u251c\u2500 icon.png                \u5355\u679a\u7070\u5ea6\u6bcd\u7248\uff0816\u00d716, LA, 9 \u7ea7\u7070\u9636\uff09
\u2502   \u251c\u2500 sand_coin.png           ...
\u2502   \u251c\u2500 rainbow_coin.png        \u5f69\u8679\u5e01 48 \u5e27\u7eb5\u5411\u52a8\u753b\uff0816\u00d7768\uff09
\u2502   \u2514\u2500 rainbow_coin.png.mcmeta
\u2514\u2500 stack/
    \u251c\u2500 icon_stack.png          \u5806\u53e0\u7070\u5ea6\u6bcd\u7248\uff0816\u00d716, LA, \u5168\u4e0d\u900f\u660e, 8 \u7ea7\u7070\u9636\uff09
    \u251c\u2500 sand_coin_stack.png     ...
    \u251c\u2500 rainbow_coin_stack.png  \u5806\u53e0\u5f69\u8679\u5e01 48 \u5e27\uff0816\u00d7768\uff09
    \u2514\u2500 rainbow_coin_stack.png.mcmeta
```

## \u547d\u540d\u5bf9\u5e94\u5173\u7cfb

| \u6750\u8d28 | \u5355\u679a | \u5806\u53e0 |
|---|---|---|
| \u6c99\u5b50\u5e01 | `sand_coin.png` | `sand_coin_stack.png` |
| \u94dc\u5e01 | `copper_coin.png` | `copper_coin_stack.png` |
| \u94c1\u5e01 | `iron_coin.png` | `iron_coin_stack.png` |
| \u950c\u5e01 | `zinc_coin.png` | `zinc_coin_stack.png` |
| \u91d1\u5e01 | `gold_coin.png` | `gold_coin_stack.png` |
| \u9ec4\u94dc\u5e01 | `brass_coin.png` | `brass_coin_stack.png` |
| \u94bb\u77f3\u5e01 | `diamond_coin.png` | `diamond_coin_stack.png` |
| \u7eff\u5b9d\u77f3\u5e01 | `emerald_coin.png` | `emerald_coin_stack.png` |
| \u4e0b\u754c\u5408\u91d1\u5e01 | `netherite_coin.png` | `netherite_coin_stack.png` |
| \u9ed1\u66dc\u77f3\u5e01 | `obsidian_coin.png` | `obsidian_coin_stack.png` |
| \u5f69\u8679\u5e01 | `rainbow_coin.png` (+`.mcmeta`) | `rainbow_coin_stack.png` (+`.mcmeta`) |

## \u52a8\u753b\u89c4\u683c

| \u9879 | \u5355\u679a\u5f69\u8679\u5e01 | \u5806\u53e0\u5f69\u8679\u5e01 |
|---|---|---|
| \u5e27\u6570 | 48 | 48 |
| \u5355\u5e27\u5c3a\u5bf8 | 16\u00d716 | 16\u00d716 |
| \u96ea\u78a7\u56fe\u5c3a\u5bf8 | 16\u00d7768 | 16\u00d7768 |
| frametime | 2 tick | 2 tick |
| \u5faa\u73af\u5468\u671f | 96 tick = 4.8s | 96 tick = 4.8s |
| \u8272\u76f8\u65cb\u8f6c | 7.5\u00b0/\u5e27\uff0c\u5faa\u73af\u65e0\u7f1d | 7.5\u00b0/\u5e27\uff0c\u5faa\u73af\u65e0\u7f1d |

`.mcmeta` \u663e\u5f0f\u58f0\u660e `frametime` / `width` / `height` / `frames`\uff0c
\u4e0e Minecraft 1.21.1 `AnimationMetadataSectionSerializer` \u7684\u5408\u6cd5\u952e\u5b8c\u5168\u4e00\u81f4\u3002

## \u6bcd\u7248\u5c3a\u5bf8

| \u6bcd\u7248 | \u5c3a\u5bf8 | \u6a21\u5f0f | \u900f\u660e\u5ea6 | \u7070\u9636\u7ea7\u6570 |
|---|---|---|---|---|
| `single/icon.png` | 16\u00d716 | LA | 156/256 \u4e0d\u900f\u660e | 9 |
| `stack/icon_stack.png` | 16\u00d716 | LA | 256/256 \u4e0d\u900f\u660e\uff08\u65e0\u900f\u660e\u533a\uff09 | 8 |

\u4e24\u5f20\u6bcd\u7248\u5747\u4e3a\u539f\u59cb\u6587\u4ef6\u7684**\u9010\u5b57\u8282\u526f\u672c**\uff08\u5df2\u54c8\u5e0c\u6bd4\u5bf9\uff09\uff0c\u672a\u88ab\u4fee\u6539\u3002

## \u751f\u6210\u65b9\u6cd5

\u811a\u672c\uff1a`tools/gen_coin_textures.py`\uff08\u6821\u9a8c\uff1a`tools/verify_coin_textures.py`\uff09

1. \u8bfb\u53d6\u6bcd\u7248\uff08\u53ea\u8bfb\uff09\uff0c\u7edf\u8ba1\u5176\u79bb\u6563\u7070\u9636\uff0c\u5f52\u4e00\u5316\u4e3a t\u2208[0,1]\u3002
2. \u6bcf\u79cd\u6750\u8d28\u4e00\u4efd\u989c\u8272\u8f6c\u6362\u6863\u4f4d\uff1a9 \u4e2a\u8282\u70b9\u7684 (\u8272\u76f8, \u9971\u548c\u5ea6) + \u4eae\u5ea6\u7a97\u53e3\uff08\u542b gamma\uff09\uff1b\u950c\u3001\u9ec4\u94dc\u7684 9 \u4e2a\u8282\u70b9\u76f4\u63a5\u53d6 Create \u771f\u5b9e\u50cf\u7d20\u8272\uff0c\u8282\u70b9\u4e4b\u95f4\u5728 RGB \u7a7a\u95f4\u7ebf\u6027\u8fc7\u6e21\u3002
   \u4eae\u5ea6\u7531\u4e8c\u5206\u6cd5\u53cd\u89e3 HSL lightness\uff0c\u4f7f\u989c\u8272\u7684 Rec.709 \u4eae\u5ea6\u6070\u597d\u843d\u5728\u7a97\u53e3\u503c\u4e0a\u3002
3. \u4e24\u5f20\u6bcd\u7248\u4f7f\u7528**\u540c\u4e00\u5957\u6863\u4f4d**\uff1a\u6bcf\u5f20\u6bcd\u7248\u6309\u81ea\u8eab\u7070\u9636\u8303\u56f4\u5f52\u4e00\u5316\u540e\u53d6\u6837\uff0c
   \u56e0\u6b64\u5355\u679a\u4e0e\u5806\u53e0\u7684\u6700\u6697/\u6700\u4eae\u50cf\u7d20\u53d6\u5230\u540c\u4e00\u4e2a\u9634\u5f71\u8272\u548c\u540c\u4e00\u4e2a\u9ad8\u5149\u8272\u3002
4. \u5f69\u8679\u5e01\uff1a\u8272\u76f8\u6cbf (x+y) \u5bf9\u89d2\u7ebf\u94fa\u6ee1\u5e27\u9762\uff0c\u6bcf\u5e27\u6574\u4f53\u65cb\u8f6c 360\u00b0/48\uff1b\u4eae\u5ea6\u4ecd\u7531\u6bcd\u7248\u7070\u9636\u51b3\u5b9a\uff0c
   \u6bcf\u5e27\u72ec\u7acb\u4ece\u6bcd\u7248\u91cd\u7b97\uff0c\u4e0d\u62f7\u8d1d\u53e6\u4e00\u5957\u56fe\u50cf\u3002
5. \u5199\u5165\u4fdd\u62a4\uff1a\u5355\u679a\u96c6\u5408\u53d7\u4fdd\u62a4\uff08\u5df2\u5b58\u5728\u4e14\u5b57\u8282\u4e0d\u540c\u5219\u4e0d\u8986\u76d6\uff0c\u91cd\u8dd1\u53ea\u80fd\u5f97\u5230\u9010\u5b57\u8282\u76f8\u540c\u7684\u7ed3\u679c\uff09\uff1b\u5806\u53e0\u96c6\u5408\u662f\u672c\u6b21\u4efb\u52a1\u7684\u8f93\u51fa\uff0c\u53ef\u91cd\u590d\u751f\u6210\uff08\u7ed3\u679c\u786e\u5b9a\uff0c\u91cd\u8dd1\u9010\u5b57\u8282\u4e00\u81f4\uff09\u3002\n
\u950c\u4e0e\u9ec4\u94dc\u7684\u53c2\u8003\u6765\u6e90\uff1a\u673a\u68b0\u52a8\u529b `create-1.21.1-6.0.10-280-slim.jar` \u4e2d\u7684
`textures/item/zinc_ingot.png`\u3001`zinc_nugget.png`\u3001`brass_ingot.png`\u3001`brass_nugget.png`\u3001
`brass_sheet.png`\u3001`textures/block/zinc_block.png`\u3001`brass_block.png`\u3001`brass_casing.png`\u3002
\u8272\u9636\u5168\u90e8\u53d6\u81ea\u8fd9\u4e9b\u771f\u5b9e\u50cf\u7d20\uff08\u8be6\u89c1\u811a\u672c\u6ce8\u91ca\uff09\u3002
"""


def main():
    if not os.path.isfile(MASTER_STACK):
        raise SystemExit("missing master: " + MASTER_STACK)

    single = load_master(MASTER_SINGLE)
    stack = load_master(MASTER_STACK)
    print("single master: %s  %dx%d mode=%s grey levels %s"
          % (MASTER_SINGLE, single["w"], single["h"], single["mode"], single["greys"]))
    print("stack  master: %s  %dx%d mode=%s grey levels %s"
          % (MASTER_STACK, stack["w"], stack["h"], stack["mode"], stack["greys"]))

    gmin, gmax = single["greys"][0], single["greys"][-1]
    knot_t = [(g - gmin) / float(gmax - gmin) for g in single["greys"]]
    profiles = build_profiles(knot_t)

    singles, stacks, ramps_single, ramps_stack = {}, {}, {}, {}
    for name in [n for n in NAMES if n != "rainbow"]:
        prof = profiles[name]
        img, lut = render_static(single, prof)
        write_once(os.path.join(ASSET_DIR, name + "_coin.png"), png_bytes(img))
        singles[name] = img
        ramps_single[name] = lut

        simg, slut = render_static(stack, prof)
        write_once(os.path.join(ASSET_DIR, name + "_coin_stack.png"), png_bytes(simg),
                   protect=False)
        stacks[name] = simg
        ramps_stack[name] = slut

    # animated rainbow coins
    for name, master, tag in (("rainbow_coin", single, "single"),
                              ("rainbow_coin_stack", stack, "stack")):
        strip = render_rainbow(master, RAINBOW)
        protect = (tag == "single")
        write_once(os.path.join(ASSET_DIR, name + ".png"), png_bytes(strip), protect)
        write_once(os.path.join(ASSET_DIR, name + ".png.mcmeta"),
                   mcmeta_bytes(RAINBOW, master["w"], master["h"]), protect)
        if tag == "single":
            singles["rainbow"] = strip
        else:
            stacks["rainbow"] = strip

    # ---- archive ---------------------------------------------------------
    arch_single = os.path.join(ARCHIVE, "single")
    arch_stack = os.path.join(ARCHIVE, "stack")
    for d in (ARCHIVE, arch_single, arch_stack):
        os.makedirs(d, exist_ok=True)

    master_ok = {}
    master_ok["icon.png"] = copy_to(MASTER_SINGLE, os.path.join(arch_single, "icon.png"))
    master_ok["icon_stack.png"] = copy_to(MASTER_STACK, os.path.join(arch_stack, "icon_stack.png"))

    archive_mismatch = []
    for name in NAMES:
        for suffix, dest in (("_coin", arch_single), ("_coin_stack", arch_stack)):
            fn = name + suffix + ".png"
            if not copy_to(os.path.join(ASSET_DIR, fn), os.path.join(dest, fn)):
                archive_mismatch.append(fn)
    for fn, dest in (("rainbow_coin.png.mcmeta", arch_single),
                     ("rainbow_coin_stack.png.mcmeta", arch_stack)):
        if not copy_to(os.path.join(ASSET_DIR, fn), os.path.join(dest, fn)):
            archive_mismatch.append(fn)

    with open(os.path.join(ARCHIVE, "README.md"), "w", encoding="utf-8", newline="\n") as fh:
        fh.write(README)

    build_preview(singles, stacks, os.path.join(ARCHIVE, "preview_coins.png"))

    # ---- report ----------------------------------------------------------
    print()
    names = [n for n in NAMES if n != "rainbow"]
    print("single coins (grey -> colour),  grey levels %s" % single["greys"])
    for name in names:
        lut = ramps_single[name]
        print("  %-10s %-10s %s" % (name, profiles[name].tag,
                                    " ".join(hexs(lut[g]) for g in single["greys"])))
    print()
    print("stack coins (grey -> colour),  grey levels %s" % stack["greys"])
    for name in names:
        lut = ramps_stack[name]
        print("  %-10s %-10s %s" % (name, profiles[name].tag,
                                    " ".join(hexs(lut[g]) for g in stack["greys"])))
    print()
    for name in names:
        a = ramps_single[name]
        b = ramps_stack[name]
        sa, sb = single["greys"], stack["greys"]
        print("  %-10s endpoints  shadow %s/%s  highlight %s/%s"
              % (name, hexs(a[sa[0]]), hexs(b[sb[0]]), hexs(a[sa[-1]]), hexs(b[sb[-1]])))
    print()
    print("written   : %d files" % len(written))
    print("unchanged : %d files" % len(unchanged))
    if skipped:
        print("SKIPPED (differ from existing, left untouched):")
        for p in skipped:
            print("   " + p)
    print("masters archived byte-identical: %s" % master_ok)
    print("archive mismatches: %s" % (archive_mismatch or "none"))
    print("rainbow: %d frames, frame %dx%d, single strip %dx%d, stack strip %dx%d, frametime %d"
          % (RAINBOW["frames"], single["w"], single["h"],
             single["w"], single["h"] * RAINBOW["frames"],
             stack["w"], stack["h"] * RAINBOW["frames"], RAINBOW["frametime"]))
    print("archive : %s" % ARCHIVE)
    return 1 if skipped or archive_mismatch or not all(master_ok.values()) else 0


if __name__ == "__main__":
    sys.exit(main())


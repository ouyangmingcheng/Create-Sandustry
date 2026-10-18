"""
Verification tool for the sandustry coin textures (single + stack + archive).

Run after tools/gen_coin_textures.py.  Read-only: it never writes into the game
resource directory.  Its report goes to coin_textures/verify_report.txt and the
exit code is non-zero when any check fails.
"""

import json
import os
import struct
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSET_DIR = os.path.join(ROOT, "src", "main", "resources", "assets",
                         "createsandustry", "textures", "item")
ARCHIVE = os.path.join(ROOT, "coin_textures")
MASTER_SINGLE = os.path.join(ASSET_DIR, "icon.png")
MASTER_STACK = os.path.join(ASSET_DIR, "icon_stack.png")

NAMES = ["sand", "copper", "iron", "zinc", "gold", "brass", "diamond",
         "emerald", "netherite", "obsidian"]
ORDER = NAMES + ["rainbow"]

# MD5 of the two masters as they were received at the start of this task, used
# only to prove the masters were not rewritten.
BASELINE_MASTER_MD5 = {
    "icon.png": "0F89C8EB19B39CA7539D873DC5649757",
    "icon_stack.png": "CE4917715BCA4D2CCFD5D3B554A1CF7A",
}

fails = []
report = []


def check(ok, msg):
    line = ("  [OK]   " if ok else "  [FAIL] ") + msg
    print(line)
    report.append(line)
    if not ok:
        fails.append(msg)


def say(msg):
    print(msg)
    report.append(msg)


def luma(rgb):
    r, g, b = rgb[:3]
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def hue_of(rgb):
    r, g, b = [v / 255.0 for v in rgb[:3]]
    mx, mn = max(r, g, b), min(r, g, b)
    if mx == mn:
        return None
    d = mx - mn
    if mx == r:
        hh = ((g - b) / d) % 6
    elif mx == g:
        hh = (b - r) / d + 2
    else:
        hh = (r - g) / d + 4
    return hh * 60.0


def hue_dist(a, b):
    d = abs(a - b) % 360.0
    return min(d, 360.0 - d)


def corr(xs, ys):
    n = len(xs)
    mx, my = sum(xs) / n, sum(ys) / n
    sx = sum((v - mx) ** 2 for v in xs) ** 0.5
    sy = sum((v - my) ** 2 for v in ys) ** 0.5
    if sx == 0 or sy == 0:
        return 0.0
    return sum((xs[i] - mx) * (ys[i] - my) for i in range(n)) / (sx * sy)


def md5(path):
    import hashlib
    h = hashlib.md5()
    with open(path, "rb") as fh:
        for chunk in iter(lambda: fh.read(65536), b""):
            h.update(chunk)
    return h.hexdigest().upper()


CT = {0: "grey", 2: "truecolour", 3: "palette", 4: "grey+alpha", 6: "truecolour+alpha"}


def png_header(path):
    with open(path, "rb") as fh:
        head = fh.read(29)
    assert head[:8] == b"\x89PNG\r\n\x1a\n", "not a PNG: " + path
    return struct.unpack(">IIBBBBB", head[16:29])


class Master(object):
    def __init__(self, path):
        self.path = path
        im = Image.open(path)
        self.mode = im.mode
        self.image = im.convert("RGBA")
        self.w, self.h = self.image.size
        self.px = self.image.load()
        self.alpha = [[self.px[x, y][3] for x in range(self.w)] for y in range(self.h)]
        self.greys = sorted({self.px[x, y][0] for y in range(self.h)
                             for x in range(self.w) if self.px[x, y][3] > 0})
        self.luma = [luma(self.px[x, y]) for y in range(self.h) for x in range(self.w)
                     if self.px[x, y][3] > 0]

    def tmap(self):
        lo, hi = self.greys[0], self.greys[-1]
        return {g: (g - lo) / float(hi - lo) for g in self.greys}


def analyse_static(name, master):
    """Return (image, mapping, tones, opaque) for a *_coin[_stack].png file."""
    path = os.path.join(ASSET_DIR, name + ".png")
    im = Image.open(path).convert("RGBA")
    px = im.load()
    mapping, consistent = {}, True
    for y in range(master.h):
        for x in range(master.w):
            if px[x, y][3] == 0:
                continue
            g = master.px[x, y][0]
            if g in mapping and mapping[g] != px[x, y][:3]:
                consistent = False
            mapping[g] = px[x, y][:3]
    tones = {px[x, y][:3] for y in range(master.h) for x in range(master.w)
             if px[x, y][3] > 0}
    opaque = sum(1 for y in range(master.h) for x in range(master.w) if px[x, y][3] > 0)
    return im, mapping, tones, opaque, consistent


def mean_hue(im, master):
    px = im.load()
    hs = []
    for y in range(master.h):
        for x in range(master.w):
            if px[x, y][3] > 0:
                h = hue_of(px[x, y])
                if h is not None:
                    hs.append(h)
    if not hs:
        return None
    # circular mean
    import math
    sx = sum(math.cos(math.radians(h)) for h in hs)
    sy = sum(math.sin(math.radians(h)) for h in hs)
    return math.degrees(math.atan2(sy, sx)) % 360.0


def check_rainbow(tag, master, frames_expected, frametime_expected):
    fn = "rainbow_coin%s" % ("_stack" if tag == "stack" else "")
    png = os.path.join(ASSET_DIR, fn + ".png")
    meta_p = png + ".mcmeta"
    im = Image.open(png).convert("RGBA")
    sw, sh = im.size
    frames = sh // master.h
    check(sw == master.w, "%s: strip width %d == master width %d" % (fn, sw, master.w))
    check(sh % master.h == 0,
          "%s: strip height %d is a whole multiple of frame height %d" % (fn, sh, master.h))
    check(frames == frames_expected,
          "%s: %d frames == %d (same as the other rainbow coin)" % (fn, frames, frames_expected))
    px = im.load()
    alpha_ok, sigs, hues, corrs = True, [], [], []
    for f in range(frames):
        sig = []
        for y in range(master.h):
            for x in range(master.w):
                p = px[x, y + f * master.h]
                sig.append(p)
                if p[3] != master.alpha[y][x]:
                    alpha_ok = False
        sigs.append(tuple(sig))
        lum = [luma(p) for p in sig if p[3] > 0]
        hs = [hue_of(p) for p in sig if p[3] > 0 and hue_of(p) is not None]
        corrs.append(corr(master.luma, lum))
        import math
        sx = sum(math.cos(math.radians(h)) for h in hs)
        sy = sum(math.sin(math.radians(h)) for h in hs)
        hues.append(math.degrees(math.atan2(sy, sx)) % 360.0)
    check(alpha_ok, "%s: every frame keeps the master alpha channel" % fn)
    check(len(set(sigs)) == frames, "%s: all %d frames have distinct colours" % (fn, frames))
    check(min(corrs) > 0.95,
          "%s: every frame keeps the master relief (min luma correlation %.3f)"
          % (fn, min(corrs)))
    steps = [hue_dist(hues[i], hues[i + 1]) for i in range(frames - 1)]
    steps.append(hue_dist(hues[-1], hues[0]))
    check(min(steps) > 1.0,
          "%s: hue advances smoothly between frames (min step %.1f deg)" % (fn, min(steps)))
    buckets = []
    for f in range(frames):
        buckets.append(len({int(hue_of(px[x, y + f * master.h]) / 12.0)
                            for y in range(master.h) for x in range(master.w)
                            if px[x, y + f * master.h][3] > 0
                            and hue_of(px[x, y + f * master.h]) is not None}))
    check(min(buckets) >= 5,
          "%s: each frame sweeps several hues across the texture (min %d buckets)"
          % (fn, min(buckets)))
    w0, h0, depth, ctype, comp, filt, inter = png_header(png)
    check((w0, h0, depth, ctype, inter) == (sw, sh, 8, 6, 0),
          "%s: PNG %dx%d bitdepth=%d type=%s interlace=%d"
          % (fn, w0, h0, depth, CT[ctype], inter))
    try:
        with open(meta_p, "r", encoding="utf-8") as fh:
            raw = fh.read()
        meta = json.loads(raw)
        check(True, "%s.png.mcmeta parses as JSON" % fn)
        anim = meta.get("animation")
        check(isinstance(anim, dict), "%s: has an 'animation' object" % fn)
        check(set(anim) <= {"frametime", "width", "height", "frames", "interpolate"},
              "%s: animation keys %s are valid 1.21.1 keys" % (fn, sorted(anim)))
        check(anim.get("width") == master.w and anim.get("height") == master.h,
              "%s: declares frame size %rx%r" % (fn, anim.get("width"), anim.get("height")))
        check(anim.get("frametime") == frametime_expected,
              "%s: frametime = %r (matches the other rainbow coin)"
              % (fn, anim.get("frametime")))
        check(anim.get("frames") == list(range(frames)),
              "%s: 'frames' lists all %d frames in order (looping)" % (fn, frames))
    except Exception as exc:  # noqa: BLE001
        check(False, "%s: mcmeta JSON error: %r" % (fn, exc))
    return frames, sw, sh


def main():
    say("=" * 78)
    say("Sandustry coin texture verification (single + stack + archive)")
    say("=" * 78)

    single = Master(MASTER_SINGLE)
    stack = Master(MASTER_STACK)
    say("")
    say("masters")
    say("  icon.png       %dx%d mode=%-3s greys=%d  %s"
        % (single.w, single.h, single.mode, len(single.greys), single.greys))
    say("  icon_stack.png %dx%d mode=%-3s greys=%d  %s"
        % (stack.w, stack.h, stack.mode, len(stack.greys), stack.greys))
    for fn, m in (("icon.png", single), ("icon_stack.png", stack)):
        got = md5(os.path.join(ASSET_DIR, fn))
        check(got == BASELINE_MASTER_MD5[fn],
              "master %s unchanged since task start (md5 %s)" % (fn, got))

    # 1) existence ---------------------------------------------------------
    say("")
    say("1) resource inventory")
    for n in ORDER:
        check(os.path.isfile(os.path.join(ASSET_DIR, n + "_coin.png")),
              "single  textures/item/%s_coin.png" % n)
        check(os.path.isfile(os.path.join(ASSET_DIR, n + "_coin_stack.png")),
              "stack   textures/item/%s_coin_stack.png" % n)
    check(os.path.isfile(os.path.join(ASSET_DIR, "rainbow_coin.png.mcmeta")),
          "single  textures/item/rainbow_coin.png.mcmeta")
    check(os.path.isfile(os.path.join(ASSET_DIR, "rainbow_coin_stack.png.mcmeta")),
          "stack   textures/item/rainbow_coin_stack.png.mcmeta")
    check(len(ORDER) == 11, "11 materials per set")

    # 2) static sets -------------------------------------------------------
    say("")
    say("2) static single coins (master icon.png %dx%d, alpha %s)"
        % (single.w, single.h, "partial" if any(a == 0 for r in single.alpha for a in r) else "none"))
    sig_single = {}
    map_single = {}
    for n in NAMES:
        im, mapping, tones, opaque, consistent = analyse_static(n + "_coin", single)
        map_single[n] = mapping
        sig_single[n] = im.tobytes()
        check(im.size == (single.w, single.h),
              "%-10s size %s == master %s" % (n, im.size, (single.w, single.h)))
        bad = sum(1 for y in range(single.h) for x in range(single.w)
                  if im.load()[x, y][3] != single.alpha[y][x])
        check(bad == 0, "%-10s alpha identical to master (%d mismatches)" % (n, bad))
        check(consistent and len(mapping) == len(single.greys),
              "%-10s pure grey->colour map, %d opaque px, %d distinct tones"
              % (n, opaque, len(tones)))
        lm = [luma(mapping[g]) for g in single.greys]
        check(all(lm[i] < lm[i + 1] for i in range(len(lm) - 1)),
              "%-10s ramp luma strictly increasing (%.0f -> %.0f)" % (n, lm[0], lm[-1]))

    say("")
    say("3) static stack coins (master icon_stack.png %dx%d, alpha %s)"
        % (stack.w, stack.h, "partial" if any(a == 0 for r in stack.alpha for a in r) else "fully opaque"))
    sig_stack = {}
    map_stack = {}
    for n in NAMES:
        im, mapping, tones, opaque, consistent = analyse_static(n + "_coin_stack", stack)
        map_stack[n] = mapping
        sig_stack[n] = im.tobytes()
        check(im.size == (stack.w, stack.h),
              "%-10s size %s == icon_stack.png %s" % (n, im.size, (stack.w, stack.h)))
        bad = sum(1 for y in range(stack.h) for x in range(stack.w)
                  if im.load()[x, y][3] != stack.alpha[y][x])
        check(bad == 0, "%-10s alpha identical to master (%d mismatches)" % (n, bad))
        check(consistent and len(mapping) == len(stack.greys),
              "%-10s pure grey->colour map, %d opaque px, %d distinct tones"
              % (n, opaque, len(tones)))
        lm = [luma(mapping[g]) for g in stack.greys]
        check(all(lm[i] < lm[i + 1] for i in range(len(lm) - 1)),
              "%-10s ramp luma strictly increasing (%.0f -> %.0f)" % (n, lm[0], lm[-1]))

    # 4) cross-set colour consistency --------------------------------------
    say("")
    say("4) single vs stack colour-system consistency")
    for n in NAMES:
        a, b = map_single[n], map_stack[n]
        sa, sb = single.greys, stack.greys
        same_dark = a[sa[0]] == b[sb[0]]
        same_light = a[sa[-1]] == b[sb[-1]]
        check(same_dark and same_light,
              "%-10s shadow %s / %s , highlight %s / %s"
              % (n, "#%02X%02X%02X" % a[sa[0]], "#%02X%02X%02X" % b[sb[0]],
                 "#%02X%02X%02X" % a[sa[-1]], "#%02X%02X%02X" % b[sb[-1]]))
        h1 = mean_hue(Image.open(os.path.join(ASSET_DIR, n + "_coin.png")).convert("RGBA"), single)
        h2 = mean_hue(Image.open(os.path.join(ASSET_DIR, n + "_coin_stack.png")).convert("RGBA"), stack)
        check(h1 is not None and h2 is not None and hue_dist(h1, h2) < 12.0,
              "%-10s mean hue single %.0f deg vs stack %.0f deg (delta %.1f)"
              % (n, h1, h2, hue_dist(h1, h2)))
    dup = [(a, b) for i, a in enumerate(NAMES) for b in NAMES[i + 1:]
           if (a == b) or False]
    same_pixels = [n for n in NAMES if sig_single[n] == sig_stack[n]]
    check(not same_pixels,
          "no stack texture is a copy of its single texture (%s)" % (same_pixels or "none"))
    check(len(set(sig_stack.values())) == len(NAMES),
          "all %d stack textures are pairwise different" % len(NAMES))
    check(len(set(sig_single.values())) == len(NAMES),
          "all %d single textures are pairwise different" % len(NAMES))

    # 5) rainbows ----------------------------------------------------------
    say("")
    say("5) rainbow animations")
    fs1, sw1, sh1 = check_rainbow("single", single, 48, 2)
    fs2, sw2, sh2 = check_rainbow("stack", stack, fs1, 2)

    # 6) PNG headers -------------------------------------------------------
    say("")
    say("6) PNG headers")
    for n in ORDER:
        for tag, master, frames in (("", single, fs1), ("_stack", stack, fs2)):
            p = os.path.join(ASSET_DIR, n + "_coin" + tag + ".png")
            exp_h = master.h * frames if n == "rainbow" else master.h
            w0, h0, depth, ctype, comp, filt, inter = png_header(p)
            check((w0, h0, depth, ctype, inter) == (master.w, exp_h, 8, 6, 0),
                  "%-24s PNG %dx%d depth=%d type=%s interlace=%d (expected %dx%d)"
                  % (os.path.basename(p), w0, h0, depth, CT[ctype], inter, master.w, exp_h))
    check(sw1 == single.w and sw2 == stack.w, "rainbow strips keep the master width")

    # 7) archive -----------------------------------------------------------
    say("")
    say("7) archive coin_textures/")
    arch_single = os.path.join(ARCHIVE, "single")
    arch_stack = os.path.join(ARCHIVE, "stack")
    check(os.path.isfile(os.path.join(ARCHIVE, "README.md")), "README.md present")
    check(os.path.isfile(os.path.join(ARCHIVE, "preview_coins.png")), "preview_coins.png present")
    mismatches = []
    for fn, dest in [("icon.png", arch_single), ("icon_stack.png", arch_stack)]:
        src = os.path.join(ASSET_DIR, fn)
        dst = os.path.join(dest, fn)
        if not (os.path.isfile(dst) and md5(src) == md5(dst)):
            mismatches.append(fn)
    check(not [m for m in mismatches if m in ("icon.png", "icon_stack.png")],
          "both masters archived and byte-identical to the game-resource masters")
    for n in ORDER:
        for suffix, dest in (("_coin", arch_single), ("_coin_stack", arch_stack)):
            fn = n + suffix + ".png"
            src, dst = os.path.join(ASSET_DIR, fn), os.path.join(dest, fn)
            if not (os.path.isfile(dst) and md5(src) == md5(dst)):
                mismatches.append(fn)
    for fn, dest in (("rainbow_coin.png.mcmeta", arch_single),
                     ("rainbow_coin_stack.png.mcmeta", arch_stack)):
        src, dst = os.path.join(ASSET_DIR, fn), os.path.join(dest, fn)
        if not (os.path.isfile(dst) and md5(src) == md5(dst)):
            mismatches.append(fn)
    check(not mismatches, "every archived file is byte-identical to its game-resource twin (%s)"
          % (mismatches or "0 mismatches"))
    n_single = len([f for f in os.listdir(arch_single)])
    n_stack = len([f for f in os.listdir(arch_stack)])
    check(n_single == 13, "single/ holds 13 files (master + 11 + mcmeta), found %d" % n_single)
    check(n_stack == 13, "stack/ holds 13 files (master + 11 + mcmeta), found %d" % n_stack)
    pack_markers = [p for p in ("pack.mcmeta", "assets", "data")
                    if os.path.exists(os.path.join(ARCHIVE, p))]
    check(not pack_markers,
          "coin_textures/ is not a loadable resource pack (no %s)" % (pack_markers or "pack markers"))

    # 8) archive dir is not a build resource root --------------------------
    say("")
    say("8) archive is not wired into the build")
    with open(os.path.join(ROOT, "build.gradle"), "r", encoding="utf-8") as fh:
        bg = fh.read()
    check("coin_textures" not in bg, "build.gradle does not reference coin_textures/")

    say("")
    say("=" * 78)
    if fails:
        say("RESULT: %d CHECK(S) FAILED" % len(fails))
        for f in fails:
            say("   - " + f)
    else:
        say("RESULT: all checks passed")
    with open(os.path.join(ARCHIVE, "verify_report.txt"), "w", encoding="utf-8") as fh:
        fh.write("\n".join(report) + "\n")
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main())

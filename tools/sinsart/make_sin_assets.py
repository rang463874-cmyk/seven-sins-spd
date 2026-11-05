#!/usr/bin/env python3
"""Generate placeholder art for the Seven Sins classes by recoloring the Cleric's art.

Run from the project root:   python3 tools/sinsart/make_sin_assets.py
Safe to re-run: every output is rebuilt from the untouched Cleric sources, and sheets
are only enlarged (never shrunk). Requires Pillow (pip install pillow).

Sheet layout reserved for the sins (k = sin index in SINS below, 0..6):
  sprites/<name>.png, splashes/<name>.jpg     recolored cleric hero sheet / splash
  sprites/avatars.png      frame at the class ordinal (24x32 frames)
  interfaces/talent_icons  row 8+k  (icon index 256 + 32k + i)
  interfaces/hero_icons    row 16+k (icon index 128 + 8k + i): sub1, sub2, abil1..3
  sprites/items.png        row 33 (index 512 + 2k): signature item, class armor
These indices must match SinArt.java.
"""
import colorsys
import os
from collections import Counter

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")
A = os.path.join(ROOT, "core", "src", "main", "assets")

# cleric source indices
CLERIC_TALENT_ROW = 160          # 26 talents + heroic energy at 186
CLERIC_SUBCLASS_ICONS = [10, 11]  # PRIEST, PALADIN
CLERIC_ABILITY_ICONS = [31, 32, 33]  # ASCENDED_FORM, TRINITY, POWER_OF_MANY
CLERIC_ORDINAL = 5
ITEM_ARMOR_CLERIC = 16 * 11 + 10  # ARMOR row 12, +10  (xy(1,12)+10)
ITEM_TOME = 16 * 15 + 23        # ARTIFACTS row 16, +23 (ARTIFACT_TOME)

FIRST_SIN_ORDINAL = 6

# hand-drawn 16x16 icons usable as signature items
MAW = [
    "................",
    "....OOOOOOOO....",
    "..OOHHPPPPPPOO..",
    ".OHHPPPPPPPPPPO.",
    "OHPWPWPWPWPWPPPO",
    "OPWDWDWDWDWDWPPO",
    "OPDDDDDDDDDDDDPO",
    "OPDDDRRRRRRDDDPO",
    "OPDDRRLRRRRRDDPO",
    "OPDDDDDDDDDDDDPO",
    "OPWDWDWDWDWDWPPO",
    "OPPWPWPWPWPWPPSO",
    ".OPPPPPPPPPPPSO.",
    "..OOPPPPPPPSOO..",
    "....OOOOOOOO....",
    "................",
]
MAW_PAL = {
    "O": (26, 10, 34, 255), "P": (107, 45, 138, 255), "H": (154, 79, 192, 255), "S": (70, 26, 92, 255),
    "W": (240, 236, 224, 255), "D": (42, 6, 18, 255), "R": (192, 48, 74, 255), "L": (232, 110, 130, 255),
}

# One entry per sin, in HeroClass order. hue/accent in degrees.
#   talents: optional list of source talent icons (vanilla indices) to recolor instead of the
#            cleric row, one per talent in Talent order, last entry = heroic energy.
#   item: "maw" or None (None -> recolored cleric tome)
SINS = [
    dict(name="gluttony", hue=285, accent=95, item="maw",
         talents=[0, 72, 75, 3, 96, 44, 10, 146, 86, 144, 11, 100, 143, 71, 53, 54, 165,
                  76, 77, 13, 35, 99, 64, 49, 50, 51, 186]),
]


# ------------------------------------------------------------------ color helpers

def hsv(p):
    r, g, b, a = p
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    return h * 360, s, v, a


def rgb(h, s, v, a):
    r, g, b = colorsys.hsv_to_rgb((h % 360) / 360, max(0, min(1, s)), max(0, min(1, v)))
    return int(r * 255), int(g * 255), int(b * 255), a


def hero_px(hue, accent):
    """Cleric hero sheet: hair -> hue, cloth/metal -> hue tint, coloured trims -> accent, skin kept."""
    def f(p):
        if p[3] == 0:
            return p
        h, s, v, a = hsv(p)
        if 12 <= h <= 45 and s >= 0.7:            # brown hair (incl. dark outline)
            return rgb(hue, s * 0.8, v * 0.85, a)
        if 12 <= h <= 40 and s < 0.7 and v > 0.4:  # skin tones
            return p
        if s < 0.14 and v > 0.35:                 # whites / greys
            return rgb(hue, 0.22, v * 0.9, a)
        if s >= 0.35 and v > 0.25:                # red / blue / gold trims
            return rgb(accent, s, v, a)
        return p
    return f


def splash_px(hue, accent):
    def f(p):
        h, s, v, a = hsv(p)
        if s < 0.15 and v > 0.55:                 # white tabard / highlights
            return rgb(hue, 0.18, v * 0.92, a)
        if 38 <= h <= 60 and s > 0.4:             # gold
            return rgb(accent, s * 0.9, v, a)
        if (h >= 280 or h <= 8) and s > 0.25:     # pink stained glass / light
            return rgb(hue, s, v * 0.9, a)
        return p
    return f


def recolor(im, fn):
    im = im.convert("RGBA")
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            px[x, y] = fn(px[x, y])
    return im


def rotate_to(icon, target_hue):
    """Rotate hues so the icon's dominant saturated hue lands on target_hue."""
    hues = Counter()
    for p in icon.getdata():
        if p[3] == 0:
            continue
        h, s, v, _ = hsv(p)
        if s > 0.25 and v > 0.2:
            hues[int(h / 10)] += 1
    if not hues:
        return icon
    delta = target_hue - (hues.most_common(1)[0][0] * 10 + 5)
    out = icon.copy()
    po = out.load()
    for y in range(out.height):
        for x in range(out.width):
            p = po[x, y]
            if p[3] == 0:
                continue
            h, s, v, a = hsv(p)
            if s > 0.12:
                po[x, y] = rgb(h + delta, s, v, a)
    return out


def draw(rows, pal):
    ic = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                ic.putpixel((x, y), pal[ch])
    return ic


def cell(idx, cols, w=16, h=16):
    r, c = divmod(idx, cols)
    return (c * w, r * h, c * w + w, r * h + h)


def grow(im, w, h):
    if im.width >= w and im.height >= h:
        return im
    out = Image.new("RGBA", (max(w, im.width), max(h, im.height)), (0, 0, 0, 0))
    out.paste(im, (0, 0))
    return out


def save_indexed_safe(im, path):
    im.save(path)


# ------------------------------------------------------------------ main

def main():
    n = len(SINS)
    p = lambda rel: os.path.join(A, rel)

    cleric_sheet = Image.open(p("sprites/cleric.png")).convert("RGBA")
    cleric_splash = Image.open(p("splashes/cleric.jpg")).convert("RGBA")

    avatars = Image.open(p("sprites/avatars.png")).convert("RGBA")
    avatars = grow(avatars, 24 * (FIRST_SIN_ORDINAL + 7), 32)
    cleric_avatar = avatars.crop(cell(CLERIC_ORDINAL, 1000, 24, 32))

    talents = Image.open(p("interfaces/talent_icons.png")).convert("RGBA")
    talents = grow(talents, 512, 16 * (8 + 7))
    hero_icons = Image.open(p("interfaces/hero_icons.png")).convert("RGBA")
    hero_icons = grow(hero_icons, 128, 16 * (16 + 7))
    items = Image.open(p("sprites/items.png")).convert("RGBA")
    items = grow(items, 256, 16 * 33)

    for k, sin in enumerate(SINS):
        hue, acc, name = sin["hue"], sin["accent"], sin["name"]

        sheet = recolor(cleric_sheet, hero_px(hue, acc))
        # keep the death/skull frames (x 96..168) untouched
        sheet.paste(cleric_sheet.crop((96, 0, 168, cleric_sheet.height)), (96, 0))
        sheet.save(p("sprites/%s.png" % name))
        recolor(cleric_splash, splash_px(hue, acc)).convert("RGB").save(p("splashes/%s.jpg" % name), quality=92)
        avatars.paste(recolor(cleric_avatar, hero_px(hue, acc)), cell(FIRST_SIN_ORDINAL + k, 1000, 24, 32)[:2])

        srcs = sin.get("talents") or list(range(CLERIC_TALENT_ROW, CLERIC_TALENT_ROW + 27))
        for i, src in enumerate(srcs):
            ic = rotate_to(talents.crop(cell(src, 32)), hue)
            talents.paste(ic, cell(256 + 32 * k + i, 32)[:2])

        for i, src in enumerate(CLERIC_SUBCLASS_ICONS + CLERIC_ABILITY_ICONS):
            ic = rotate_to(hero_icons.crop(cell(src, 8)), hue)
            hero_icons.paste(ic, cell(128 + 8 * k + i, 8)[:2])

        sig = draw(MAW, MAW_PAL) if sin.get("item") == "maw" else rotate_to(items.crop(cell(ITEM_TOME, 16)), hue)
        items.paste(sig, cell(512 + 2 * k, 16)[:2])
        items.paste(recolor(items.crop(cell(ITEM_ARMOR_CLERIC, 16)), hero_px(hue, acc)), cell(512 + 2 * k + 1, 16)[:2])
        if sin.get("item") == "maw":
            # the maw doubles as the first armor ability icon
            hero_icons.paste(sig, cell(128 + 8 * k + 2, 8)[:2])
        print("generated art for", name)

    avatars.save(p("sprites/avatars.png"))
    talents.save(p("interfaces/talent_icons.png"))
    hero_icons.save(p("interfaces/hero_icons.png"))
    items.save(p("sprites/items.png"))
    print("done: %d sin(s)" % n)


if __name__ == "__main__":
    main()

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

# 15x11 golden card (top-left aligned)
CARD = [
    ".OOOOOOOOOOOOO.",
    "OYYYYYYYYYYYYHO",
    "OYHHYYYYYYYYYYO",
    "OKKKKKKKKKKKKKO",
    "OYYYYYYYYYYYYYO",
    "OYCCCYYYYYYYYYO",
    "OYCDCYYYYYYYYYO",
    "OYCCCYYYYYYYYYO",
    "OYYYYYYDDYDDYYO",
    "OSYYYYYYYYYYYSO",
    ".OOOOOOOOOOOOO.",
]
CARD_PAL = {
    "O": (92, 58, 10, 255), "Y": (232, 184, 48, 255), "H": (255, 236, 150, 255), "S": (176, 128, 24, 255),
    "K": (60, 40, 12, 255), "C": (200, 200, 210, 255), "D": (120, 84, 20, 255),
}

# 14x14 war drum (top-left aligned)
DRUM = [
    "....S....S....",
    ".....S..S.....",
    "..OOOOOOOOOO..",
    ".OHHHHHHHHHHO.",
    "OHLLLLLLLLLLHO",
    "ORRWRRRRWRRRRO",
    "ORRRWRRWRRRRRO",
    "ORRRRWWRRRRRRO",
    "ORRRWRRWRRRRRO",
    "ORRWRRRRWRRRDO",
    "OGGGGGGGGGGGGO",
    ".ODDRRRRRRDDO.",
    "..OOOOOOOOOO..",
    "..............",
]
DRUM_PAL = {
    "O": (40, 12, 8, 255), "R": (168, 32, 24, 255), "D": (104, 18, 14, 255), "H": (230, 214, 180, 255),
    "L": (250, 240, 214, 255), "W": (236, 200, 120, 255), "G": (200, 150, 40, 255), "S": (120, 80, 40, 255),
}

# 14x11 crown (top-left aligned)
CROWN = [
    "O.....OO.....O",
    "OHO..OHHO..OHO",
    "OYYO.OYYO.OYYO",
    "OYYYOYYYYOYYYO",
    "OYYYYYRRYYYYYO",
    "OYYBYYRRYYBYYO",
    "OYYYYYYYYYYYYO",
    "OSSSSSSSSSSSSO",
    "OYPYYPYYPYYPYO",
    "OSSSSSSSSSSSSO",
    ".OOOOOOOOOOOO.",
]
CROWN_PAL = {
    "O": (54, 30, 70, 255), "Y": (236, 196, 64, 255), "H": (255, 240, 170, 255), "S": (176, 132, 30, 255),
    "R": (200, 60, 220, 255), "B": (90, 160, 255, 255), "P": (240, 240, 255, 255),
}

# 10x14 perfume bottle (top-left aligned)
PERFUME = [
    "...OOOO...",
    "...OGGO...",
    "....OO....",
    "...OHHO...",
    "..OPPPPO..",
    ".OPLLPPPO.",
    "OPLLPPPPPO",
    "OPLPPPPPPO",
    "OPPPPPPPDO",
    "OPPPPPPPDO",
    "OPPPPPPDDO",
    ".OPPPPDDO.",
    "..OOOOOO..",
    "..........",
]
PERFUME_PAL = {
    "O": (70, 16, 40, 255), "P": (236, 92, 160, 255), "L": (255, 200, 228, 255), "D": (170, 40, 104, 255),
    "H": (240, 230, 240, 255), "G": (230, 190, 70, 255),
}

# 12x15 hand mirror (top-left aligned)
MIRROR = [
    "...OOOOOO...",
    "..OGGGGGGO..",
    ".OGLLSSSSGO.",
    "OGLLSSSSSSGO",
    "OGLSSSSSSSGO",
    "OGSSSSSSSSGO",
    "OGSSSSSSSDGO",
    "OGSSSSSSDDGO",
    ".OGSSSSDDGO.",
    "..OGGGGGGO..",
    "...OOGGOO...",
    ".....GG.....",
    "....OGGO....",
    "....OGGO....",
    ".....OO.....",
]
MIRROR_PAL = {
    "O": (14, 40, 18, 255), "G": (60, 170, 70, 255), "L": (220, 255, 220, 255), "S": (150, 210, 170, 255),
    "D": (90, 140, 110, 255),
}

# 15x11 pillow (top-left aligned)
PILLOW = [
    ".OO.........OO.",
    "OLLOOOOOOOOOLLO",
    "OLWWWWWWWWWWWLO",
    ".OWWWBBBBWWWWO.",
    ".OWWBWWWWBWWWO.",
    ".OWWWWZZWWWWWO.",
    ".OWWWWWWZZWWSO.",
    ".OWWWWWZZWWWSO.",
    "OLWWWWWWWWWSSLO",
    "OLLOOOOOOOOOLLO",
    ".OO.........OO.",
]
PILLOW_PAL = {
    "O": (30, 36, 70, 255), "W": (200, 214, 248, 255), "L": (150, 170, 230, 255), "S": (140, 156, 210, 255),
    "B": (90, 110, 190, 255), "Z": (60, 80, 160, 255),
}

# item sprite indices of the base weapons (see ItemSpriteSheet: WEP_TIER1 = 96, WEP_TIER3 = 112, WEP_TIER5 = 128)
BASE_T1 = {"gluttony": 96, "greed": 97, "wrath": 98, "pride": 99, "lust": 117, "envy": 100, "sloth": 97}
BASE_T5 = {"gluttony": 134, "greed": 131, "wrath": 128, "pride": 132, "lust": 130, "envy": 133, "sloth": 129}
SIN_WEAPON_ROW = 528  # row 34 of items.png: T1/T5 pairs, then the sigil at +14

# 14x14 sin sigil
SIGIL = [
    "....OOOOOO....",
    "..OORRRRRROO..",
    ".ORRRRYYRRRRO.",
    ".ORRRYRRYRRRO.",
    "ORRYYYRRYYYRRO",
    "ORRYRRRRRRYRRO",
    "ORRRYRRRRYRRRO",
    "ORRRYRRRRYRRRO",
    "ORRYRRYYRRYRRO",
    "ORRYYYRRYYYRRO",
    ".ORRRRRRRRRRO.",
    ".ODRRRRRRRRDO.",
    "..OODDDDDDOO..",
    "....OOOOOO....",
]
SIGIL_PAL = {"O": (50, 8, 12, 255), "R": (170, 24, 36, 255), "D": (110, 14, 22, 255), "Y": (240, 200, 90, 255)}


def weapon_px(hue):
    """sin weapons: grey metal and wood pick up the sin's colour"""
    def f(p):
        if p[3] == 0:
            return p
        h, s, v, a = hsv(p)
        if v < 0.12:
            return p
        return rgb(hue, max(s, 0.38), min(1, v * 1.05), a)
    return f

MIRROR_ROW = 544  # row 35 of items.png: the vault mirror's reflections, one per sin

# how each reflection is recoloured from the signature item: (target hue, saturation mult, value mult)
RELIC_TONE = {
    "gluttony": (45, 1.1, 1.15),   # golden maw
    "greed":    (230, 0.25, 0.45), # black card
    "wrath":    (350, 1.2, 0.7),   # blood drum
    "pride":    (275, 0.35, 1.25), # eternal crown, pale and radiant
    "lust":     (285, 1.0, 0.75),  # forbidden perfume
    "envy":     (150, 0.8, 0.8),   # shattered mirror
    "sloth":    (240, 0.9, 0.6),   # eternal pillow, night blue
}


def relic_px(hue, smul, vmul):
    def f(p):
        if p[3] == 0:
            return p
        h, s, v, a = hsv(p)
        return rgb(hue, min(1, s * smul + 0.05), min(1, v * vmul), a)
    return f

# One entry per sin, in HeroClass order. hue/accent in degrees.
#   talents: optional list of source talent icons (vanilla indices) to recolor instead of the
#            cleric row, one per talent in Talent order, last entry = heroic energy.
#   item: "maw" or None (None -> recolored cleric tome)
SINS = [
    dict(name="gluttony", hue=285, accent=95, item="maw",
         talents=[0, 72, 75, 3, 96, 44, 10, 146, 86, 144, 11, 100, 143, 71, 53, 54, 165,
                  76, 77, 13, 35, 99, 64, 49, 50, 51, 186]),
    dict(name="greed", hue=46, accent=130, item="card", hair_v=1.7),
    dict(name="wrath", hue=4, accent=28, item="drum", hair_v=1.15),
    dict(name="pride", hue=265, accent=48, item="crown", hair_v=2.4, hair_s=0.18),
    dict(name="lust", hue=332, accent=300, item="perfume", hair_v=1.6, hair_s=0.65),
    dict(name="envy", hue=125, accent=160, item="mirror", hair_v=1.2, hair_s=0.7),
    dict(name="sloth", hue=215, accent=195, item="pillow", hair_v=1.3, hair_s=0.6),
]


# ------------------------------------------------------------------ color helpers

def hsv(p):
    r, g, b, a = p
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    return h * 360, s, v, a


def rgb(h, s, v, a):
    r, g, b = colorsys.hsv_to_rgb((h % 360) / 360, max(0, min(1, s)), max(0, min(1, v)))
    return int(r * 255), int(g * 255), int(b * 255), a


def hero_px(hue, accent, hair_v=0.85, hair_s=0.8):
    """Cleric hero sheet: hair -> hue, cloth/metal -> hue tint, coloured trims -> accent, skin kept."""
    def f(p):
        if p[3] == 0:
            return p
        h, s, v, a = hsv(p)
        if 12 <= h <= 45 and s >= 0.7:            # brown hair (incl. dark outline)
            return rgb(hue, s * hair_s, v * hair_v, a)
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
    items = grow(items, 256, 16 * 35)

    for k, sin in enumerate(SINS):
        hue, acc, name = sin["hue"], sin["accent"], sin["name"]

        sheet = recolor(cleric_sheet, hero_px(hue, acc, sin.get("hair_v", 0.85), sin.get("hair_s", 0.8)))
        # keep the death/skull frames (frames 8..12, x 96..156) untouched
        sheet.paste(cleric_sheet.crop((96, 0, 156, cleric_sheet.height)), (96, 0))
        sheet.save(p("sprites/%s.png" % name))
        recolor(cleric_splash, splash_px(hue, acc)).convert("RGB").save(p("splashes/%s.jpg" % name), quality=92)
        avatars.paste(recolor(cleric_avatar, hero_px(hue, acc, sin.get("hair_v", 0.85), sin.get("hair_s", 0.8))), cell(FIRST_SIN_ORDINAL + k, 1000, 24, 32)[:2])

        srcs = sin.get("talents") or list(range(CLERIC_TALENT_ROW, CLERIC_TALENT_ROW + 27))
        for i, src in enumerate(srcs):
            ic = rotate_to(talents.crop(cell(src, 32)), hue)
            talents.paste(ic, cell(256 + 32 * k + i, 32)[:2])

        for i, src in enumerate(CLERIC_SUBCLASS_ICONS + CLERIC_ABILITY_ICONS):
            ic = rotate_to(hero_icons.crop(cell(src, 8)), hue)
            hero_icons.paste(ic, cell(128 + 8 * k + i, 8)[:2])

        if sin.get("item") == "maw":
            sig = draw(MAW, MAW_PAL)
        elif sin.get("item") == "card":
            sig = draw(CARD, CARD_PAL)
        elif sin.get("item") == "drum":
            sig = draw(DRUM, DRUM_PAL)
        elif sin.get("item") == "crown":
            sig = draw(CROWN, CROWN_PAL)
        elif sin.get("item") == "perfume":
            sig = draw(PERFUME, PERFUME_PAL)
        elif sin.get("item") == "mirror":
            sig = draw(MIRROR, MIRROR_PAL)
        elif sin.get("item") == "pillow":
            sig = draw(PILLOW, PILLOW_PAL)
        else:
            sig = rotate_to(items.crop(cell(ITEM_TOME, 16)), hue)
        items.paste(sig, cell(512 + 2 * k, 16)[:2])
        items.paste(recolor(items.crop(cell(ITEM_ARMOR_CLERIC, 16)), hero_px(hue, acc, sin.get("hair_v", 0.85), sin.get("hair_s", 0.8))), cell(512 + 2 * k + 1, 16)[:2])
        if sin.get("item") == "maw":
            # the maw doubles as the first armor ability icon
            hero_icons.paste(sig, cell(128 + 8 * k + 2, 8)[:2])
        tone = RELIC_TONE[name]
        relic = recolor(sig, relic_px(*tone))
        if name == "envy":
            # crack the glass
            for x, y in [(4, 3), (5, 4), (6, 5), (6, 6), (7, 7), (5, 6), (4, 7), (8, 6)]:
                relic.putpixel((x, y), (20, 30, 24, 255))
        items.paste(relic, cell(MIRROR_ROW + k, 16)[:2])
        for j, base in enumerate((BASE_T1[name], BASE_T5[name])):
            w = recolor(items.crop(cell(base, 16)), weapon_px(hue))
            items.paste(w, cell(SIN_WEAPON_ROW + 2 * k + j, 16)[:2])
        print("generated art for", name)

    items.paste(draw(SIGIL, SIGIL_PAL), cell(SIN_WEAPON_ROW + 14, 16)[:2])
    avatars.save(p("sprites/avatars.png"))
    talents.save(p("interfaces/talent_icons.png"))
    hero_icons.save(p("interfaces/hero_icons.png"))
    items.save(p("sprites/items.png"))
    print("done: %d sin(s)" % n)


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Draws the demon lords' sprites.

- sprites/demon_lords.png: one row per lord in DemonLord order (lucifer, mammon, asmodeus, leviathan,
  beelzebub, satan, belphegor), 12 frames of 48x49 (the art is 48x48, one spare row on top for the bob):
  idle (2), run (4), attack (3: wind up, lunge with the lord's colour, back), die (3: sinking and fading).
  The bosses are drawn over 3x3 tiles (sprites/DemonSprites.LordSprite).
- sprites/demon_leviathan.png: Leviathan's wall-form pieces, 48x49 frames: body (2 idle + fading),
  tail (2 idle + fading), blank.
- sprites/demon_altar.png: the altar, 48x49, 2 frames (braziers flaring).

The owner's own 48x48 art in tools/sinsart/demon_art/png/<name>.png (lords, leviathan_coil, leviathan_tail,
altar) is used when present; the text art below is the fallback.

The lords' pixel art lives in tools/sinsart/demon_art/<lord>.py as text (left halves are mirrored, then
patched). Re-run after editing: python3 tools/sinsart/make_demon_sprites.py
"""
import os
import sys
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from demon_art import artlib, lucifer, mammon, asmodeus, leviathan, beelzebub, satan, belphegor

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "core", "src", "main", "assets", "sprites")
S = 48          # art size
H = S + 1       # frame height
FRAMES = 12
K = artlib.K

LORDS = [
    (lucifer, (255, 240, 170, 255)),
    (mammon, (255, 220, 60, 255)),
    (asmodeus, (255, 110, 210, 255)),
    (leviathan, (130, 220, 255, 255)),
    (beelzebub, (170, 255, 70, 255)),
    (satan, (255, 130, 30, 255)),
    (belphegor, (180, 140, 255, 255)),
]


PNG = os.path.join(os.path.dirname(os.path.abspath(__file__)), "demon_art", "png")
#hand-made 48x48 art that faces left, mirrored so every sprite faces right like the game expects
MIRRORED = {"leviathan"}


def load_png(name):
    """the owner's 48x48 art: faint alpha noise dropped, cropped and set on the bottom centre of a 48x48 cell"""
    path = os.path.join(PNG, name + ".png")
    if not os.path.exists(path):
        return None
    img = Image.open(path).convert("RGBA")
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            if px[x, y][3] < 24:
                px[x, y] = (0, 0, 0, 0)
    if name in MIRRORED:
        img = img.transpose(Image.FLIP_LEFT_RIGHT)
    box = img.getbbox()
    img = img.crop(box)
    out = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    out.paste(img, ((S - img.width) // 2, S - img.height), img)
    return out


def draw(mod):
    own = load_png(mod.__name__.split(".")[-1])
    if own is not None:
        return own
    return artlib.build(half=getattr(mod, "HALF", None), full=getattr(mod, "FULL", None),
                        patches=getattr(mod, "PATCHES", []), pal=mod.PAL)


def shifted(img, dx, dy, h=H):
    out = Image.new("RGBA", (img.width, h), (0, 0, 0, 0))
    out.paste(img, (dx, h - img.height + dy), img)
    return out


def faded(img, alpha):
    out = img.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            px[x, y] = (r, g, b, int(a * alpha))
    return out


def squashed(img, hfrac, alpha, h=H):
    nh = max(1, int(img.height * hfrac))
    small = img.resize((img.width, nh), Image.NEAREST)
    out = Image.new("RGBA", (img.width, h), (0, 0, 0, 0))
    out.paste(small, (0, h - nh), small)
    return faded(out, alpha)


def with_fx(img, color, strong):
    """attack frame: a burst of the lord's colour in front of it (right side, mid height)"""
    out = img.copy()
    px = out.load()
    cx, cy = S - 4, 24
    r = 3 if strong else 1
    for y in range(cy - r, cy + r + 1):
        for x in range(cx - r, cx + r + 1):
            if abs(x - cx) + abs(y - cy) <= r and 0 <= x < S:
                px[x, y] = color
    if strong:
        for x, y in ((cx - 5, cy), (cx, cy - 5), (cx, cy + 5)):
            if 0 <= x < S:
                px[x, y] = color
    return out


def frames(mod, fx):
    base = draw(mod)
    rest = shifted(base, 0, 0)
    up = shifted(base, 0, -1)
    return [
        rest, up,
        rest, up, rest, up,
        shifted(base, -2, 0), with_fx(shifted(base, 2, 0), fx, True), with_fx(rest, fx, False),
        squashed(base, 0.8, 0.8), squashed(base, 0.55, 0.55), squashed(base, 0.3, 0.3),
    ]


# ---------------------------------------------------------------- Leviathan's coils (wall form)
CW, CH = 16, 33
COIL_PAL = leviathan.PAL


def coil(variant):
    """a 16-wide piece of serpent body; ends sit at the same height so pieces join side by side"""
    img = Image.new("RGBA", (CW, CH - 1), (0, 0, 0, 0))
    px = img.load()
    cells = {}
    import math
    for x in range(CW):
        if variant == 0:    # hump arching up, a fin on top
            top = 23 - round(9 * math.sin(math.pi * (x + 0.5) / CW))
            th = 9
        elif variant == 1:  # low coil half in the water
            top = 23 - round(2 * math.sin(math.pi * (x + 0.5) / CW))
            th = 9
        else:               # tail tip, thinning to the left
            th = max(2, round(9 * (x + 1) / CW))
            top = 23 + (9 - th) // 2 - round(2 * math.sin(math.pi * (x + 0.5) / CW))
        for y in range(top, min(CH - 1, top + th)):
            cells[(x, y)] = "N"
    for (x, y) in list(cells):
        if (x, y - 1) not in cells:
            cells[(x, y)] = "B" if variant != 2 or x > 4 else "N"
        elif (x, y + 1) not in cells:
            cells[(x, y)] = "n"
        elif (x + y) % 5 == 0:
            cells[(x, y)] = "a"
    if variant == 0:
        for i, h in enumerate((2, 4, 5, 4, 2)):
            x = 5 + i
            topy = min(y for (xx, y) in cells if xx == x)
            for y in range(topy - h, topy):
                cells[(x, y)] = "c" if y == topy - h else "A"
    for (x, y), c in cells.items():
        px[x, y] = COIL_PAL[c]
    # outline (not at the joining edges)
    for y in range(CH - 1):
        for x in range(CW):
            if (x, y) in cells:
                continue
            if any((x + dx, y + dy) in cells for dx, dy in ((0, 1), (0, -1), (1, 0), (-1, 0))):
                px[x, y] = K
    # water around the base
    for x in range(CW):
        for y in (29, 30, 31):
            if px[x, y][3] == 0 and (x + y) % 3:
                px[x, y] = COIL_PAL["W" if (x // 2 + y) % 2 else "w"]
    return img


def coil_frames():
    """Leviathan's wall pieces, 48x49 frames: body (rest, up, fading), tail (rest, up, fading), blank.
    Uses the owner's art (leviathan_coil.png, leviathan_tail.png); falls back to the drawn 16px pieces."""
    out = []
    for name, v in (("leviathan_coil", 0), ("leviathan_tail", 2)):
        base = load_png(name)
        if base is None:
            small = coil(v)
            base = Image.new("RGBA", (S, S), (0, 0, 0, 0))
            base.paste(small, ((S - CW) // 2, S - small.height), small)
        out += [shifted(base, 0, 0), shifted(base, 0, -1), faded(shifted(base, 0, 0), 0.4)]
    out.append(Image.new("RGBA", (S, H), (0, 0, 0, 0)))
    return out


# ---------------------------------------------------------------- the altar (16x16, 2 idle frames)
ALTAR = [
    "................",
    "...K........K...",
    "..KFK......KFK..",
    "..KfK......KfK..",
    "..KBK......KBK..",
    ".KKKKKKKKKKKKKK.",
    ".KRrRrRrRrRrRrK.",
    "KBBBBBBBBBBBBBBK",
    "KBbBSBbSBbSBbBBK",
    "KBbBBBbBBbBBbBBK",
    "KBBbSBBbSBBbSBBK",
    "KBbBBBbBBbBBbBBK",
    ".KBBBBBBBBBBBBK.",
    ".KbbbbbbbbbbbbK.",
    "KBBBBBBBBBBBBBBK",
    "KKKKKKKKKKKKKKKK",
]
ALTAR_PAL = {"K": K, "B": (70, 50, 60, 255), "b": (44, 30, 38, 255), "R": (180, 20, 40, 255),
             "r": (110, 10, 24, 255), "F": (255, 120, 40, 255), "f": (255, 220, 90, 255),
             "S": (255, 60, 90, 255)}
SIGILS = [(255, 224, 112), (255, 176, 0), (255, 90, 170), (64, 160, 255), (144, 192, 48), (255, 48, 32), (144, 112, 224)]


def altar_frames():
    out = []
    for flick in range(2):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        k = 0
        for y, row in enumerate(ALTAR):
            assert len(row) == 16, (y, len(row))
            for x, ch in enumerate(row):
                if ch == ".":
                    continue
                c = ALTAR_PAL[ch]
                if ch == "S":  # the seven sigils, one colour each
                    c = SIGILS[k % 7] + (255,)
                    k += 1
                if flick and ch in "Ff":
                    c = ALTAR_PAL["f" if ch == "F" else "F"]
                img.putpixel((x, y), c)
        out.append(img)
    return out


def main():
    sheet = Image.new("RGBA", (S * FRAMES, H * len(LORDS)), (0, 0, 0, 0))
    for r, (mod, fx) in enumerate(LORDS):
        for i, f in enumerate(frames(mod, fx)):
            sheet.paste(f, (i * S, r * H), f)
    sheet.save(os.path.join(OUT, "demon_lords.png"))

    cf = coil_frames()
    csheet = Image.new("RGBA", (S * len(cf), H), (0, 0, 0, 0))
    for i, f in enumerate(cf):
        csheet.paste(f, (i * S, 0), f)
    csheet.save(os.path.join(OUT, "demon_leviathan.png"))

    own = load_png("altar")
    if own is not None:
        #48x49 frames: as drawn, then with the braziers flaring
        flare = own.copy()
        fp = flare.load()
        for y in range(S):
            for x in range(S):
                r, g, b, a = fp[x, y]
                if a and r > 180 and r > b + 60:
                    fp[x, y] = (255, min(255, g + 50), min(255, b + 20), a)
        alt = [shifted(own, 0, 0), shifted(flare, 0, 0)]
        asheet = Image.new("RGBA", (S * 2, H), (0, 0, 0, 0))
        for i, f in enumerate(alt):
            asheet.paste(f, (i * S, 0), f)
    else:
        alt = altar_frames()
        asheet = Image.new("RGBA", (32, 16), (0, 0, 0, 0))
        for i, f in enumerate(alt):
            asheet.paste(f, (i * 16, 0), f)
    asheet.save(os.path.join(OUT, "demon_altar.png"))
    print("demon sprites written")


if __name__ == "__main__":
    main()

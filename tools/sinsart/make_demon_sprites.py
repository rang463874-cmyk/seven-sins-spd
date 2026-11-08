#!/usr/bin/env python3
"""Draws the demon lords' sprite sheet (sprites/demon_lords.png) and the altar (sprites/demon_altar.png).

Each lord is one 24x24 pixel-art pose (frames are 24x25) (facing right) written as text below. The animation frames are
derived from it: idle (2: bob), run (4: bob/sway), attack (3: wind up, lunge with the lord's effect, back)
and die (3: sinking and fading). Sheet layout (must match sprites/DemonSprites.java): one row per lord in
DemonLord order (lucifer, mammon, asmodeus, leviathan, beelzebub, satan, belphegor), 12 frames per row.

Re-run after editing: python3 tools/sinsart/make_demon_sprites.py
"""
import os
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "core", "src", "main", "assets", "sprites")
S = 24          # art size
H = S + 1       # frame height: one spare row on top so the bob never clips
FRAMES = 12

K = (28, 18, 22, 255)  # shared outline

LORDS = []

# ---------------------------------------------------------------- Lucifer: the fallen morning star
LORDS.append(dict(
    name="lucifer",
    fx=(255, 240, 170, 255),
    pal={"K": K, "W": (245, 244, 236, 255), "w": (176, 178, 196, 255), "G": (244, 200, 64, 255),
         "g": (168, 116, 30, 255), "S": (252, 222, 196, 255), "s": (214, 170, 140, 255),
         "H": (255, 230, 120, 255), "E": (220, 30, 40, 255), "B": (60, 52, 70, 255)},
    art=[
        "........................",
        "..........G.G.G.........",
        ".KK.......GgGgG......KK.",
        "KwWK.....KHHHHHK....KWwK",
        "KwWWK...KHHHHHHHK..KWWwK",
        ".KwWWK..KHSSSSSHK.KWWwK.",
        ".KwWWWK.KHESSESHKKWWWwK.",
        "..KwWWWKKHSSSSSHKWWWwK..",
        "..KwWWWWKKHSssHKKWWWwK..",
        "...KwWWWKGKSSSKGKWWwK...",
        "...KwWWKWWGGGGGWWKWwK...",
        "....KwKSWWWWGWWWWSKwK...",
        "....KwKsKWWWGWWWKsKK....",
        ".....KK.KWWWGWWWK.......",
        "........KWWWGWWWK.......",
        "........KGGGgGGGK.......",
        ".......KWWWWGWWWWK......",
        ".......KwWWWGWWWwK......",
        "......KwWWWWGWWWWwK.....",
        "......KwWWWWGWWWWwK.....",
        ".....KwWWWWWGWWWWWwK....",
        ".....KGGGGGGGGGGGGGK....",
        ".....KKKKKKKKKKKKKKK....",
        "........KBK....KBK......",
    ]))

# ---------------------------------------------------------------- Mammon: a golem of molten gold
LORDS.append(dict(
    name="mammon",
    fx=(255, 220, 60, 255),
    pal={"K": K, "G": (232, 176, 40, 255), "g": (150, 96, 20, 255), "Y": (255, 236, 120, 255),
         "E": (230, 40, 40, 255), "R": (200, 30, 60, 255), "D": (90, 56, 14, 255)},
    art=[
        "........................",
        "..........Y.R.Y.........",
        ".........KYYYYYK........",
        "........KGGGGGGGK.......",
        ".......KGGEGGGEGGK......",
        ".......KGGGGGGGGGK......",
        ".......KGgDDDDDgGK......",
        "....KK.KGGGGGGGGGK.KK...",
        "...KGGKKKgGGGGGgKKKGGK..",
        "..KGGGGGGGGGGGGGGGGGGGK.",
        "..KGgGGGGGYYYYYGGGGGgGK.",
        "..KGgGKGGYYgggYYGGKGgGK.",
        "..KGgGKGGYYgYYYYGGKGgGK.",
        "..KGgGKGGYYgggYYGGKGgGK.",
        ".KGGGGKGGGYYYYYGGGKGGGGK",
        ".KYGYGKGGGGGGGGGGGKGYGYK",
        ".KKKKKKGgGGGGGGGgGKKKKKK",
        "......KGGgGGGGGgGGK.....",
        "......KGGGKKKKKGGGK.....",
        ".....KGGGGK...KGGGGK....",
        ".....KGgGGK...KGGgGK....",
        ".....KGGGGK...KGGGGK....",
        "....KYGYGYK...KYGYGYK...",
        "....KKKKKKK...KKKKKKK...",
    ]))

# ---------------------------------------------------------------- Asmodeus: the crimson dancer
LORDS.append(dict(
    name="asmodeus",
    fx=(255, 110, 190, 255),
    pal={"K": K, "P": (255, 120, 190, 255), "p": (190, 60, 130, 255), "S": (250, 214, 210, 255),
         "s": (214, 160, 160, 255), "E": (120, 255, 200, 255), "h": (70, 30, 80, 255),
         "R": (200, 20, 50, 255), "r": (120, 10, 30, 255), "b": (110, 30, 90, 255), "m": (230, 40, 80, 255)},
    art=[
        "........................",
        "........K.....K.........",
        ".......KhK...KhK........",
        ".......KhKKKKKhK........",
        "......KPPPPPPPPPK.......",
        ".....KPPSSSSSSPPpK......",
        ".....KPPSESSESSPpK......",
        ".....KPPSSSSSSSPPpK.....",
        "..KK.KPPPSSmSSPPPpK.KK..",
        ".KbbKKPPPKSSSKPPPpKKbbK.",
        ".KbbbKKPKRRRRRKPPpKbbbK.",
        "KbbbbbKKSRRrRRSKpKbbbbbK",
        "KbbKbbKSsKRRRKsSKKbbKbbK",
        ".KK.KK.KKRRrRRKK.KK.KK..",
        "........KRRRRRK.........",
        "........KRRrRRRK........",
        ".......KRRRRrRRK........",
        ".......KRRRRrRRRK.......",
        "......KRRRRRRrRRK.......",
        "......KRRrRRRRrRRK......",
        ".....KRRRRrRRRRrRK......",
        ".....KrRrRrRrRrRrRK.....",
        "......KKKKKKKKKKKK......",
        "........KsK...KsK.......",
    ]))

# ---------------------------------------------------------------- Leviathan: the serpent of the deep
LORDS.append(dict(
    name="leviathan",
    fx=(120, 210, 255, 255),
    pal={"K": K, "B": (60, 140, 230, 255), "b": (30, 70, 150, 255), "C": (150, 220, 255, 255),
         "E": (255, 230, 60, 255), "W": (240, 240, 240, 255), "F": (40, 200, 190, 255), "f": (20, 120, 130, 255)},
    art=[
        "........................",
        "..............K.K.K.....",
        ".............KFKFKFK....",
        "............KFfFfFfFK...",
        "...........KBBBBBBBBBK..",
        "..........KBBBBBBBEBBBK.",
        "..........KBbBBBBBBBBBBK",
        "..........KBbBBBBKWKWKWK",
        "...........KBbBBBK......",
        "...K........KBbBBBK.....",
        "..KFK........KBbCBBK....",
        "..KFfK........KBbCBBK...",
        "..KFBBK.......KBbCCBK...",
        "...KBBBK.....KBBbCCBK...",
        "...KBbBBKKKKKBBbCCBBK...",
        "....KBbBBBBBBBBbCCBBK...",
        "....KBBbbBBBBBbCCBBK....",
        ".....KBBBbbbbbCCBBK.....",
        "...KKKBBBBBBBBBBBKKK....",
        "..KBBBBBbbbbbbbBBBBBK...",
        ".KBbbBBBBBBBBBBBBBbbBK..",
        ".KBBBbbbbbbbbbbbbbBBBK..",
        "..KKBBBBBBBBBBBBBBBKK...",
        "....KKKKKKKKKKKKKKK.....",
    ]))

# ---------------------------------------------------------------- Beelzebub: the lord of flies
LORDS.append(dict(
    name="beelzebub",
    fx=(170, 220, 70, 255),
    pal={"K": K, "D": (52, 66, 36, 255), "d": (30, 38, 22, 255), "G": (110, 140, 50, 255),
         "E": (210, 30, 30, 255), "e": (255, 120, 90, 255), "w": (200, 230, 230, 150), "v": (150, 190, 200, 150),
         "Y": (240, 200, 60, 255), "L": (20, 20, 20, 255)},
    art=[
        "..........Y.Y.Y.........",
        "..KK......YYYYY......KK.",
        ".KwwK....KDDDDDK....KwwK",
        ".KwvwK..KEEDDDEEK..KwvwK",
        "..KwvwK.KEeEDEeEK.KwvwK.",
        "..KwwvwKKEEEDEEEKKwvwwK.",
        "...KwwvwKKDDDDDKKwvwwK..",
        "....KwwvwKDdLdDKwvwwK...",
        ".....KKwKDGDDDGDKwKK....",
        ".......KDGDDDDDGDK......",
        "......KDDGGGGGGGDDK.....",
        ".....LKDDDDDDDDDDDKL....",
        "....L.KDGGGGGGGGGDK.L...",
        "...L..KDDDDDDDDDDDK..L..",
        "......KDGGGGGGGGGDK.....",
        ".....LKDDDDDDDDDDDKL....",
        "....L..KDGGGGGGGDK..L...",
        "...L...KDDDDDDDDDK...L..",
        ".......KDdGGGGGdDK......",
        "........KDDDDDDDK.......",
        "........KDdGGGdDK.......",
        ".........KDDDDDK........",
        "..........KdddK.........",
        "...........KKK..........",
    ]))

# ---------------------------------------------------------------- Satan: horned lord of wrath
LORDS.append(dict(
    name="satan",
    fx=(255, 120, 30, 255),
    pal={"K": K, "R": (210, 40, 30, 255), "r": (130, 20, 20, 255), "H": (60, 50, 50, 255),
         "h": (120, 110, 100, 255), "E": (255, 230, 60, 255), "B": (40, 30, 36, 255), "b": (80, 60, 70, 255),
         "F": (255, 150, 30, 255), "f": (255, 230, 100, 255), "W": (240, 240, 240, 255)},
    art=[
        "...K..............K.....",
        "..KhK............KhK....",
        "..KhHK..........KHhK....",
        "...KhHKK......KKHhK.....",
        "....KKHHKKKKKKHHKK......",
        "......KRRRRRRRRK........",
        ".....KRRRRRRRRRRK.......",
        ".....KRREErrREERK.......",
        ".....KRRRRRRRRRRK.......",
        ".....KRrKWKWKWrRK.......",
        "......KRRRrrRRRK........",
        "..f...KKKRRRRKKK...f....",
        ".fFf.KBBBKRRKBBBK.fFf...",
        ".FfFKBBbBBKKBBbBBKFfF...",
        "..FKRBBbBBBBBBbBBRKF....",
        "..KRRKBBbBBBBBbBBKRRK...",
        "...KKKBBbBBBBBbBBKKK....",
        "......KBBbBBBBbBBK......",
        "......KBBbBBBBbBBK......",
        ".....KBBBbBBBBbBBBK.....",
        ".....KBBBbBBBBbBBBK.....",
        "....KbBbBbBbBbBbBbBK....",
        "....KKKKKKKKKKKKKKKK....",
        ".......KrRK...KRrK......",
    ]))

# ---------------------------------------------------------------- Belphegor: the slouching brute
LORDS.append(dict(
    name="belphegor",
    fx=(170, 140, 255, 255),
    pal={"K": K, "P": (150, 110, 210, 255), "p": (96, 66, 150, 255), "H": (230, 220, 190, 255),
         "h": (160, 150, 120, 255), "E": (255, 255, 255, 255), "e": (40, 20, 60, 255), "T": (250, 250, 240, 255),
         "C": (190, 40, 60, 255), "c": (120, 20, 40, 255), "Y": (240, 200, 70, 255), "Z": (220, 230, 255, 255)},
    art=[
        "...................ZZZ..",
        "...KK.........KK....Z...",
        "..KHhK.......KhHK..ZZZ..",
        "...KHhK.....KhHK........",
        "....KKHKKKKKHKK.........",
        "......KPPPPPPPK.........",
        ".....KPPPPPPPPPK........",
        ".....KPPeeePeeePK.......",
        ".....KPPEEePEEePK.......",
        ".....KPPPPPPPPPPK.......",
        ".....KPpKTKKTKpPK.......",
        "...KKKKPPPPPPPPKKKK.....",
        "..KPPPPKKPPPPKKPPPPK....",
        ".KPPpPPPPPPPPPPPPpPPK...",
        ".KPPpPKPPPPPPPPPKPpPPK..",
        ".KPPPKPPPPpppPPPPKPPPK..",
        "..KKKKPPPPPPPPPPPKKKK...",
        "....KCKPPPPPPPPPKCK.....",
        "...KCCCKKPPPPPKKCCCK....",
        "..KCCYCCCKKKKKCCCYCCK...",
        "..KCCCCCCCCCCCCCCCCCK...",
        "..KcCcCcCcCcCcCcCcCcK...",
        "..KKKKKKKKKKKKKKKKKKK...",
        "...KhK...........KhK....",
    ]))


def draw(lord):
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    for y, row in enumerate(lord["art"]):
        if len(row) != S:
            raise SystemExit("%s row %d has %d chars" % (lord["name"], y, len(row)))
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), lord["pal"][ch])
    if len(lord["art"]) != S:
        raise SystemExit("%s has %d rows" % (lord["name"], len(lord["art"])))
    return img


def shifted(img, dx, dy):
    """places the art in a frame (resting position = one row down), moved by dx/dy (dy <= 0)"""
    out = Image.new("RGBA", (S, H), (0, 0, 0, 0))
    out.paste(img, (dx, 1 + dy), img)
    return out


def faded(img, alpha):
    out = img.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            px[x, y] = (r, g, b, int(a * alpha))
    return out


def squashed(img, hfrac, alpha):
    h = max(1, int(S * hfrac))
    small = img.resize((S, h), Image.NEAREST)
    out = Image.new("RGBA", (S, H), (0, 0, 0, 0))
    out.paste(small, (0, H - h), small)
    return faded(out, alpha)


def with_fx(img, color, strong):
    """attack frame: a burst of the lord's colour in front of it"""
    out = img.copy()
    px = out.load()
    pts = [(21, 10), (22, 11), (21, 12), (23, 11), (20, 11)] if strong else [(21, 11)]
    if strong:
        pts += [(22, 9), (22, 13), (23, 10), (23, 12)]
    for x, y in pts:
        px[x, y] = color
    return out


def frames(lord):
    base = draw(lord)
    fx = lord["fx"]
    rest = shifted(base, 0, 0)
    up = shifted(base, 0, -1)
    return [
        rest, up,                                                                 # idle
        rest, up, rest, up,                                                       # run
        shifted(base, -1, 0), with_fx(shifted(base, 1, 0), fx, True), with_fx(rest, fx, False),  # attack
        squashed(base, 0.8, 0.8), squashed(base, 0.55, 0.55), squashed(base, 0.3, 0.3),          # die
    ]


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
    for r, lord in enumerate(LORDS):
        for i, f in enumerate(frames(lord)):
            sheet.paste(f, (i * S, r * H), f)
    sheet.save(os.path.join(OUT, "demon_lords.png"))

    alt = altar_frames()
    asheet = Image.new("RGBA", (32, 16), (0, 0, 0, 0))
    for i, f in enumerate(alt):
        asheet.paste(f, (i * 16, 0), f)
    asheet.save(os.path.join(OUT, "demon_altar.png"))
    print("demon sprites written")


if __name__ == "__main__":
    main()

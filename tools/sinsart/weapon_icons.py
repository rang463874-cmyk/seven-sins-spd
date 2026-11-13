"""Seven Sins: 16x16 item icons for the new weapons (shifting halberd x2 forms, thunder flail, chakram, parrying sabre),
drawn procedurally and outlined. Used by make_sin_assets.py (items.png row 38, +5..+9); run alone to patch items.png:
python3 tools/sinsart/weapon_icons.py"""
import math
import os

from PIL import Image

K = (0, 0, 0, 255)
WOOD, WOOD_L = (128, 82, 44, 255), (176, 122, 70, 255)
STEEL, STEEL_L, STEEL_D = (186, 196, 210, 255), (240, 245, 250, 255), (120, 128, 146, 255)
GOLD, GOLD_D = (240, 200, 70, 255), (180, 130, 30, 255)
IRON, IRON_L = (78, 88, 120, 255), (130, 144, 186, 255)
SPARK = (255, 240, 120, 255)
TEAL = (70, 170, 170, 255)
LEATHER = (90, 50, 36, 255)


def canvas():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def put(im, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        im.putpixel((x, y), c)


def line(im, x0, y0, x1, y1, c):
    dx, dy = abs(x1 - x0), -abs(y1 - y0)
    sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
    err = dx + dy
    while True:
        put(im, x0, y0, c)
        if x0 == x1 and y0 == y1:
            break
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy


def outline(im):
    px = im.load()
    out = im.copy()
    o = out.load()
    for y in range(16):
        for x in range(16):
            if px[x, y][3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < 16 and 0 <= ny < 16 and px[nx, ny][3] and px[nx, ny] != K:
                    o[x, y] = K
                    break
    return out


def halberd(axe):
    im = canvas()
    line(im, 1, 14, 10, 5, WOOD)
    line(im, 2, 14, 10, 6, WOOD_L)
    put(im, 9, 7, GOLD); put(im, 8, 6, GOLD); put(im, 10, 8, GOLD_D)
    if not axe:                                    # long slim spear point
        line(im, 11, 4, 14, 1, STEEL)
        line(im, 11, 5, 14, 2, STEEL_L)
        line(im, 10, 4, 13, 1, STEEL_D)
        put(im, 12, 6, STEEL_D); put(im, 13, 7, STEEL_D)  # small back hook
    else:                                          # broad axe blade off the pole's side + short spike
        line(im, 11, 4, 13, 2, STEEL_L)
        for y in range(16):
            for x in range(16):
                along, across = x - y, x + y - 15     # along the pole (towards its tip) / out to its side
                if -2 <= along <= 6 and 1 <= across:
                    edge = 3.0 + 3.2 * math.cos((along - 2.0) / 4.6 * math.pi / 2)
                    if across <= edge:
                        put(im, x, y, STEEL_L if across > edge - 1 else STEEL)
    return outline(im)


def flail():
    im = canvas()
    line(im, 1, 14, 5, 10, WOOD)
    line(im, 2, 14, 5, 11, WOOD_L)
    put(im, 5, 10, GOLD)
    for (x, y) in ((6, 9), (7, 8), (8, 8)):
        put(im, x, y, STEEL_D)
    cx, cy = 11, 5
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d <= 2.8:
                put(im, x, y, IRON_L if (x - cx) + (y - cy) < -1 else IRON)
    for (x, y) in ((11, 1), (15, 5), (11, 9), (8, 3), (14, 2), (14, 8)):
        put(im, x, y, STEEL)
    put(im, 10, 4, SPARK); put(im, 12, 6, SPARK)
    return outline(im)


def chakram():
    im = canvas()
    cx, cy = 7, 7
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if 3.0 <= d <= 5.6:
                put(im, x, y, STEEL_L if (x - cx) - (y - cy) > 2 else (STEEL if d < 4.8 else STEEL_D))
            elif 2.0 <= d < 3.0:
                put(im, x, y, TEAL)
    for a in range(8):
        ang = a * math.pi / 4 + math.pi / 8
        put(im, round(cx + 6.4 * math.cos(ang)), round(cy + 6.4 * math.sin(ang)), STEEL_L)
    return outline(im)


def sabre():
    im = canvas()
    pts = []
    for i in range(12):                              # gently curved blade from the guard to the tip
        t = i / 11
        x = 4 + t * 10
        y = 11 - t * 10 + math.sin(t * math.pi) * 1.6
        pts.append((round(x), round(y)))
    for a, b in zip(pts, pts[1:]):
        line(im, a[0], a[1], b[0], b[1], STEEL)
    for x, y in pts[2:]:
        put(im, x - 1, y, STEEL_L)
    line(im, 1, 14, 3, 12, LEATHER)
    put(im, 0, 15, GOLD)
    for (x, y) in ((2, 10), (3, 10), (4, 10), (5, 11), (5, 12), (6, 13), (5, 14), (4, 14)):
        put(im, x, y, GOLD)
    put(im, 3, 11, GOLD_D)
    return outline(im)


def tint(im, hue, sat, vmul):
    """recolour the mid tones of a vanilla icon (keeps outlines and bright highlights)"""
    import colorsys
    out = im.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if not a:
                continue
            h, s_, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if v < 0.12 or v > 0.88:
                continue
            nr, ng, nb = colorsys.hsv_to_rgb((hue % 360) / 360, sat, min(1, v * vmul))
            px[x, y] = (int(nr * 255), int(ng * 255), int(nb * 255), a)
    return out


def sling():
    im = canvas()
    line(im, 5, 14, 7, 9, WOOD)                     # handle
    line(im, 6, 14, 8, 9, WOOD_L)
    line(im, 7, 9, 3, 4, WOOD)                      # left prong
    line(im, 8, 9, 12, 3, WOOD)                     # right prong
    line(im, 9, 9, 13, 4, WOOD_L)
    line(im, 3, 4, 7, 6, LEATHER)                   # band and pouch
    line(im, 12, 3, 8, 6, LEATHER)
    put(im, 7, 6, STEEL_D); put(im, 8, 6, STEEL_D)
    return outline(im)


def coin_sack():
    im = canvas()
    sack, sack_l = (150, 104, 60, 255), (196, 150, 96, 255)
    for y in range(16):
        for x in range(16):
            d = ((x - 8) / 5.5) ** 2 + ((y - 10.5) / 4.6) ** 2
            if d <= 1:
                put(im, x, y, sack_l if x < 7 and y < 11 else sack)
    for x in range(6, 11):
        put(im, x, 5, LEATHER)                       # tied neck
    for (x, y) in ((6, 3), (8, 2), (10, 3), (7, 4), (9, 4), (8, 4)):
        put(im, x, y, GOLD)                          # coins spilling out of the top
    put(im, 8, 3, (255, 240, 150, 255))
    put(im, 4, 14, GOLD); put(im, 12, 14, GOLD_D)
    return outline(im)


# owner's weapon list: (row offset from row 38, source vanilla icon or None, tint) in ItemSpriteSheet order
VANILLA = {"DAGGER": 100, "SPEAR": 106, "DIRK": 108, "SICKLE": 109, "SWORD": 112, "MACE": 113, "LONGSWORD": 120,
           "BATTLE_AXE": 121, "GREATSWORD": 128, "WAR_HAMMER": 129, "GLAIVE": 130, "WAR_SCYTHE": 134, "PICKAXE": 468}
MOD_LIST = [
    ("KUKRI", "DIRK", (30, 0.35, 1.0)), ("MORNINGSTAR", "MACE", (0, 0.35, 0.9)), ("SLING", None, None),
    ("FLAMBERGE", "SWORD", (18, 0.55, 1.05)), ("WAR_PICK", "PICKAXE", (210, 0.3, 1.0)), ("COIN_SACK", None, None),
    ("BATTLE_SCYTHE", "SICKLE", (95, 0.35, 1.0)), ("EXECUTIONER_SWORD", "LONGSWORD", (0, 0.25, 0.7)),
    ("CRESCENT_AXE", "BATTLE_AXE", (190, 0.4, 1.05)), ("CRUSADER_MACE", "MACE", (48, 0.6, 1.2)),
    ("RUNE_SPEAR", "SPEAR", (220, 0.6, 1.2)), ("GIANT_SLAYER", "GREATSWORD", (30, 0.22, 1.1)),
    ("ABYSS_SCYTHE", "WAR_SCYTHE", (270, 0.6, 0.85)), ("HEAVENLY_SPEAR", "GLAIVE", (50, 0.5, 1.3)),
    ("RUIN_HAMMER", "WAR_HAMMER", (10, 0.6, 0.85)), ("DOOM_BLADE", "GREATSWORD", (350, 0.85, 0.7)),
    ("VOID_SCYTHE", "WAR_SCYTHE", (280, 0.85, 0.6)), ("SUN_JUDGMENT", "GLAIVE", (45, 0.9, 1.5)),
    ("WORLDS_END", "WAR_HAMMER", (20, 0.9, 1.05)),
]


def mod_icons(items):
    """(absolute icon index, image): row 38 +10..15, then row 39"""
    out = []
    for i, (name, src, t) in enumerate(MOD_LIST):
        idx = ROW38 + 10 + i if i < 6 else ROW38 + 16 + (i - 6)
        if src is None:
            im = sling() if name == "SLING" else coin_sack()
        else:
            r, c = divmod(VANILLA[src], 16)
            im = tint(items.crop((c * 16, r * 16, c * 16 + 16, r * 16 + 16)), *t)
        out.append((idx, im))
    return out


def icons():
    """(row-38 offset, image)"""
    return [(5, halberd(False)), (6, halberd(True)), (7, flail()), (8, chakram()), (9, sabre())]


ROW38 = 37 * 16  # "row 38" counts from 1 (ItemSpriteSheet.xy), the 38th row of 16px icons


def paste_into(items):
    for idx, im in mod_icons(items):
        r, c = divmod(idx, 16)
        items.paste(Image.new("RGBA", (16, 16), (0, 0, 0, 0)), (c * 16, r * 16))
        items.paste(im, (c * 16, r * 16))
    for off, im in icons():
        idx = ROW38 + off
        r, c = divmod(idx, 16)
        items.paste(Image.new("RGBA", (16, 16), (0, 0, 0, 0)), (c * 16, r * 16))
        items.paste(im, (c * 16, r * 16))


if __name__ == "__main__":
    p = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "core", "src", "main", "assets",
                     "sprites", "items.png")
    items = Image.open(p).convert("RGBA")
    if items.height < 39 * 16:                    # row 39 for the owner's weapon list
        grown = Image.new("RGBA", (items.width, 39 * 16), (0, 0, 0, 0))
        grown.paste(items, (0, 0))
        items = grown
    paste_into(items)
    items.save(p)
    print("patched", p)

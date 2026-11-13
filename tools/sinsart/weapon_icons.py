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


def icons():
    """(row-38 offset, image)"""
    return [(5, halberd(False)), (6, halberd(True)), (7, flail()), (8, chakram()), (9, sabre())]


ROW38 = 37 * 16  # "row 38" counts from 1 (ItemSpriteSheet.xy), the 38th row of 16px icons


def paste_into(items):
    for off, im in icons():
        idx = ROW38 + off
        r, c = divmod(idx, 16)
        items.paste(Image.new("RGBA", (16, 16), (0, 0, 0, 0)), (c * 16, r * 16))
        items.paste(im, (c * 16, r * 16))


if __name__ == "__main__":
    p = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "core", "src", "main", "assets",
                     "sprites", "items.png")
    items = Image.open(p).convert("RGBA")
    paste_into(items)
    items.save(p)
    print("patched", p)

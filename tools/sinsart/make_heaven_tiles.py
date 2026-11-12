#!/usr/bin/env python3
"""The heavens' tileset (floors 27-31) from the owner's mock-up map (heaven_art/heaven_map.png):
white-blue marble floors, cream marble walls with gold tops, gold doors, a starry chasm and glowing teal water.

The city tile sheet keeps the layout (every wall/edge variant the game expects); its colours are remapped
(beige stone -> cream marble, wood -> gold, blue-grey floor -> pale marble) and the plain floor, decorated
floor, special floor and chasm tiles are replaced by pieces cut from the owner's picture.
Writes environment/tiles_heaven.png and environment/water_heaven.png.
Re-run: python3 tools/sinsart/make_heaven_tiles.py
"""
import colorsys
import os
import random

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "..")
ENV = os.path.join(ROOT, "core", "src", "main", "assets", "environment")
SRC = os.path.join(HERE, "heaven_art", "heaven_map.png")

# one ~31px tile of the picture each (x0, y0, x1, y1)
FLOORS = [(104, 1085, 134, 1115), (104, 1115, 134, 1145), (135, 1115, 165, 1145)]
SPECIAL = (550, 905, 650, 1005)          # the golden sigil floor, squeezed into one tile
CHASM = (262, 1035, 412, 1130)           # starry sky over clouds


def hsv(p):
    r, g, b = p[:3]
    return colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)


def rgb(h, s, v, a=255):
    r, g, b = colorsys.hsv_to_rgb(h % 1, max(0, min(1, s)), max(0, min(1, v)))
    return int(r * 255), int(g * 255), int(b * 255), a


def remap(p):
    r, g, b, a = p
    if a == 0:
        return p
    h, s, v = hsv(p)
    deg = h * 360
    if v < 0.06:                                   # the black void between walls stays black
        return p
    if s < 0.12:                                   # greys / blacks: lift a little, cool tint
        return rgb(0.6, 0.06, 0.2 + v * 0.85, a)
    if deg >= 330 or deg <= 50:
        if s < 0.4 and v > 0.35:                   # beige stone -> cream marble
            return rgb(0.12, 0.10, min(1, 0.45 + v * 0.6), a)
        return rgb(0.125, 0.62, min(1, 0.3 + v * 0.85), a)   # wood / dark trims -> gold
    if 190 <= deg <= 260:                          # blue-grey floor -> pale marble
        return rgb(0.62, 0.12, min(1, 0.5 + v * 0.55), a)
    return rgb(h, s, min(1, v * 1.15), a)         # grass, flowers, crystal: keep, a touch brighter


def tile(src, box):
    return src.crop(box).resize((16, 16), Image.BOX).convert("RGBA")


def put(sheet, idx, img):
    sheet.paste(img, ((idx % 16) * 16, (idx // 16) * 16))


def sprinkle(img, n, rng, color):
    px = img.load()
    for _ in range(n):
        x, y = rng.randrange(16), rng.randrange(16)
        px[x, y] = color
    return img


def main():
    rng = random.Random(27)
    city = Image.open(os.path.join(ENV, "tiles_city.png")).convert("RGBA")
    src = Image.open(SRC).convert("RGB")
    sheet = city.copy()
    px = sheet.load()
    for y in range(sheet.height):
        for x in range(sheet.width):
            px[x, y] = remap(px[x, y])

    floors = [tile(src, b) for b in FLOORS]
    # FLOOR 0, FLOOR_ALT_1 6, FLOOR_ALT_2 12
    put(sheet, 0, floors[0])
    put(sheet, 6, floors[1])
    put(sheet, 12, floors[2])
    # FLOOR_DECO 1 / 7: marble with little green sprouts like the picture's
    for idx, base in ((1, floors[0]), (7, floors[1])):
        put(sheet, idx, sprinkle(base.copy(), 7, rng, (120, 200, 110, 255)))
    # FLOOR_SP 4 / 10: the golden sigil
    sp = tile(src, SPECIAL)
    put(sheet, 4, sp)
    put(sheet, 10, sp)
    # CHASM 24: a starry piece of the night sky (HeavenSky draws the whole picture over chasms; this is the fallback)
    skyfile = os.path.join(ENV, "custom_tiles", "heaven_sky.png")
    if os.path.exists(skyfile):
        put(sheet, 24, Image.open(skyfile).convert("RGBA").crop((16, 0, 32, 16)))
    else:
        sky = src.crop(CHASM).resize((32, 20), Image.BOX).crop((0, 0, 16, 16)).convert("RGBA")
        put(sheet, 24, sprinkle(sky, 3, rng, (255, 255, 240, 255)))
    sheet.save(os.path.join(ENV, "tiles_heaven.png"))

    # water: glowing teal with drifting sparkles (32x32, tiles seamlessly)
    water = Image.new("RGBA", (32, 32))
    wp = water.load()
    for y in range(32):
        for x in range(32):
            k = 0.5 + 0.12 * ((x * 7 + y * 3) % 5) / 4
            wp[x, y] = rgb(0.47, 0.55, 0.55 + k * 0.35, 255)
    for _ in range(14):
        x, y = rng.randrange(32), rng.randrange(32)
        wp[x, y] = (230, 255, 250, 255)
    water.save(os.path.join(ENV, "water_heaven.png"))
    print("heaven tiles written")


if __name__ == "__main__":
    main()

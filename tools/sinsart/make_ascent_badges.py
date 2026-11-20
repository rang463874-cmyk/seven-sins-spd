#!/usr/bin/env python3
"""Badge icons for the ascent's five endings and 'all roads' (Nov 20 2026, ascent/AscentHooks, Badges ASCENT_*).
Tints of existing badges into the free slots 112..117 of interfaces/badges.png (16x16 cells, 8 per row):
the happy end badge (99) for each ending, the pacifist ascent badge (120) for all roads.
Re-run: python3 tools/sinsart/make_ascent_badges.py"""
import colorsys
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
PATH = os.path.join(HERE, "..", "..", "core", "src", "main", "assets", "interfaces", "badges.png")
COLS, S = 8, 16
# slot -> (source badge, hue in degrees or None for grey, saturation factor, brightness factor)
TINTS = {112: (99, 355, 1.0, 0.95),   # god of sin: crimson
         113: (99, 45, 1.0, 1.1),     # god of dawn: gold
         114: (99, 275, 0.9, 0.7),    # heir of the demon lords: dark violet
         115: (99, 200, 0.35, 1.15),  # the penitent: pale silver blue
         116: (99, None, 0.0, 0.9),   # the grey wanderer
         117: (120, 300, 0.8, 1.0)}   # walker of all roads


def cell(i):
    r, c = divmod(i, COLS)
    return (c * S, r * S, c * S + S, r * S + S)


def main():
    sheet = Image.open(PATH).convert("RGBA")
    for slot, (src, hue, sat, val) in TINTS.items():
        tile = sheet.crop(cell(src))
        px = tile.load()
        for y in range(S):
            for x in range(S):
                r, g, b, a = px[x, y]
                if not a:
                    continue
                h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
                if hue is not None and s > 0.08:
                    h = hue / 360
                s = min(1, s * sat)
                v = min(1, v * val)
                nr, ng, nb = colorsys.hsv_to_rgb(h, s, v)
                px[x, y] = (int(nr * 255), int(ng * 255), int(nb * 255), a)
        sheet.paste(tile, cell(slot)[:2])
    sheet.save(PATH)
    print("ascent badges written")


if __name__ == "__main__":
    main()

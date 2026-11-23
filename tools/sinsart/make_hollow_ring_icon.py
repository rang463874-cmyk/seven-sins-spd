#!/usr/bin/env python3
"""Icons of the hollow ring and the Ring of the Twelve Kings (Nov 23 2026, items/rings/HollowRing) in items.png:
the vanilla garnet ring (RINGS+0, row 14 col 0) recoloured. Hollow: a pale silver band with the gem cut out (dark hole).
Complete: a gold band holding a twelve-colour gem. Written to SINS3+8 / +9 (row 34, cols 8 and 9).
Re-run: python3 tools/sinsart/make_hollow_ring_icon.py"""
import colorsys
import os

from PIL import Image

PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "core", "src", "main", "assets", "sprites", "items.png")
S = 16


def cell(col, row):
    return (col * S, row * S, col * S + S, row * S + S)


def main():
    sheet = Image.open(PATH).convert("RGBA")
    src = sheet.crop(cell(0, 14))
    for col, complete in ((8, False), (9, True)):
        t = src.copy()
        px = t.load()
        gem = []
        for y in range(S):
            for x in range(S):
                r, g, b, a = px[x, y]
                if not a:
                    continue
                h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
                is_gem = s > 0.45 and (h < 0.08 or h > 0.9)   # the garnet's red
                if is_gem:
                    gem.append((x, y))
                    continue
                if v < 0.18:
                    continue    # outline stays
                if complete:
                    nr, ng, nb = colorsys.hsv_to_rgb(0.13, 0.75, min(1, v * 1.15))
                else:
                    nr, ng, nb = colorsys.hsv_to_rgb(0.62, 0.12, v * 0.72)
                px[x, y] = (int(nr * 255), int(ng * 255), int(nb * 255), a)
        if gem:
            xs = [p[0] for p in gem]
            x0, x1 = min(xs), max(xs)
            for x, y in gem:
                a = px[x, y][3]
                if complete:
                    hue = (x - x0) / max(1, x1 - x0 + 1)
                    nr, ng, nb = colorsys.hsv_to_rgb(hue, 0.75, 1.0)
                    px[x, y] = (int(nr * 255), int(ng * 255), int(nb * 255), a)
                else:
                    px[x, y] = (24, 20, 30, a)
        sheet.paste(t, cell(col, 34)[:2])
    sheet.save(PATH)
    print("hollow ring icons written")


if __name__ == "__main__":
    main()

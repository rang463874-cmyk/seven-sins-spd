#!/usr/bin/env python3
"""The ascent's bosses (Nov 20 2026): the owner's pictures tools/sinsart/ascent_art/<name>.png (made from PROMPTS.md, each on
a flat background colour) -> sprites/ascent_bosses.png, 64x65 frames, 12 per row (idle 0-1, run 2-5, attack 6-8, die 9-11),
one row per boss in ORDER (the row index is AscentSprites' `row`). A boss without a picture leaves its row empty; its Java
sprite stays the tinted placeholder until AscentSprites points it at its row.
Background: flood fill from the border by distance to the border's median colour, plus any strongly saturated pixel of the
background's hue anywhere (light leaking between wings and weapons). Shrinking, the outline and the frames come from
make_virtue_sprites (the archangels' 64px pipeline).
Re-run: python3 tools/sinsart/make_ascent_sprites.py"""
import colorsys
import os
from collections import deque

import numpy as np
from PIL import Image

import make_virtue_sprites as mvs

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "ascent_art")
OUT = os.path.join(HERE, "..", "..", "core", "src", "main", "assets", "sprites", "ascent_bosses.png")

ORDER = ["moloch", "lilith", "azazel", "abaddon", "cherubim", "ophanim", "azrael", "sandalphon", "behemoth", "metatron"]
# the attack spark's colour per boss
FX = {"moloch": (255, 150, 40, 255), "lilith": (200, 90, 255, 255), "azazel": (200, 200, 210, 255),
      "abaddon": (150, 200, 60, 255), "cherubim": (255, 170, 60, 255), "ophanim": (255, 230, 120, 255),
      "azrael": (220, 220, 230, 255), "sandalphon": (150, 210, 255, 255), "behemoth": (200, 140, 80, 255),
      "metatron": (255, 250, 220, 255)}
SIZE = 64
TOL = 70        # colour distance still counted as background when flood filling


def cut(path):
    img = Image.open(path).convert("RGBA")
    a = np.array(img).astype(int)
    h, w, _ = a.shape
    border = np.concatenate([a[0, :, :3], a[-1, :, :3], a[:, 0, :3], a[:, -1, :3]])
    bg = np.median(border, 0)
    near = np.abs(a[..., :3] - bg).sum(2) < TOL
    seen = np.zeros((h, w), bool)
    q = deque([(y, x) for x in range(w) for y in (0, h - 1)] + [(y, x) for y in range(h) for x in (0, w - 1)])
    while q:
        y, x = q.popleft()
        if y < 0 or x < 0 or y >= h or x >= w or seen[y, x] or not near[y, x]:
            continue
        seen[y, x] = True
        q.extend(((y + 1, x), (y - 1, x), (y, x + 1), (y, x - 1)))
    # the background's hue, strongly saturated, anywhere (light between the wings, behind weapons)
    bh, bs, bv = colorsys.rgb_to_hsv(*(bg / 255))
    rgb = a[..., :3] / 255.0
    mx, mn = rgb.max(2), rgb.min(2)
    sat = np.where(mx > 0, (mx - mn) / np.maximum(mx, 1e-6), 0)
    hue = np.zeros((h, w))
    d = np.maximum(mx - mn, 1e-6)
    r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    hue = np.where(mx == r, ((g - b) / d) % 6, np.where(mx == g, (b - r) / d + 2, (r - g) / d + 4)) / 6
    dh = np.minimum(np.abs(hue - bh), 1 - np.abs(hue - bh))
    leak = (dh < 0.09) & (sat > 0.28) & (mx > 0.3)
    a[..., 3] = np.where(seen | leak, 0, 255)
    return Image.fromarray(a.astype(np.uint8), "RGBA")


def main():
    mvs.S, mvs.H = SIZE, SIZE + 1
    sheet = Image.new("RGBA", (SIZE * 12, (SIZE + 1) * len(ORDER)), (0, 0, 0, 0))
    done = []
    for row, name in enumerate(ORDER):
        path = os.path.join(SRC, name + ".png")
        if not os.path.exists(path):
            continue
        base = mvs.shrink(mvs.cutout(cut(path), dark=-1))
        for i, f in enumerate(mvs.frames(base, FX[name])):
            sheet.paste(f, (i * SIZE, row * (SIZE + 1)), f)
        done.append(name)
    sheet.save(OUT)
    print("ascent bosses:", ", ".join(done))


if __name__ == "__main__":
    main()

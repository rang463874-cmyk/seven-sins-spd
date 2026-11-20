#!/usr/bin/env python3
"""The ascent's bosses (Nov 20 2026): the owner's pictures tools/sinsart/ascent_art/<name>.png (made from PROMPTS.md, each on
a flat background colour) -> sprites/ascent_bosses.png, 96x80 frames, 12 per row (idle 0-1, run 2-5, attack 6-8, die 9-11),
one row per boss in ORDER (the row index is AscentSprites' `row`). A boss without a picture leaves its row empty; its Java
sprite stays the tinted placeholder until AscentSprites points it at its row.
Background: flood fill from the border by distance to the border's median colour, plus any strongly saturated pixel of the
background's hue anywhere (light leaking between wings and weapons). Shrinking, the outline and the frames follow make_virtue_sprites
(the archangels' pipeline), on wider 96x80 frames.
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
# pictures drawn facing left: mirrored so every sprite faces right like the game expects
MIRROR = {"behemoth"}
ISLAND = 0.02   # pieces smaller than this share of the biggest are dropped (swarms, flying rubble, the Gemini sparkle)
FW, FH = 96, 80   # frame size: wide enough for wingspans, tall enough for the giants
TOL = 50        # colour distance still counted as background when flood filling


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
    leak = (dh < 0.09) & (sat > 0.28) & (mx > 0.3) & (bs > 0.3)  # only for a coloured background
    keep = ~(seen | leak)
    # drop small islands
    lab = np.zeros((h, w), int)
    sizes = []
    for y0 in range(h):
        for x0 in range(w):
            if not keep[y0, x0] or lab[y0, x0]:
                continue
            n = len(sizes) + 1
            st, cnt = [(y0, x0)], 0
            lab[y0, x0] = n
            while st:
                y, x = st.pop()
                cnt += 1
                for ny, nx in ((y + 1, x), (y - 1, x), (y, x + 1), (y, x - 1)):
                    if 0 <= ny < h and 0 <= nx < w and keep[ny, nx] and not lab[ny, nx]:
                        lab[ny, nx] = n
                        st.append((ny, nx))
            sizes.append(cnt)
    if sizes:
        big = max(sizes)
        small = [i + 1 for i, c in enumerate(sizes) if c < big * ISLAND]
        keep &= ~np.isin(lab, small)
    a[..., 3] = np.where(keep, 255, 0)
    return Image.fromarray(a.astype(np.uint8), "RGBA")


OUTLINE = mvs.OUTLINE


def shrink(img):
    """crop, scale to fit FW-2 x FH-2 keeping the aspect (premultiplied box filter), threshold alpha, outline, bottom centre"""
    img = img.crop(img.getbbox())
    scale = min((FW - 2) / img.width, (FH - 2) / img.height)
    nw, nh = max(1, round(img.width * scale)), max(1, round(img.height * scale))
    a = np.array(img).astype(float)
    a[..., :3] *= a[..., 3:4] / 255
    pre = Image.fromarray(a.astype(np.uint8), "RGBA")
    small = np.array(pre.resize((nw, nh), Image.BOX)).astype(float)
    alpha = small[..., 3]
    rgb = np.where(alpha[..., None] > 0, small[..., :3] * 255 / np.maximum(alpha[..., None], 1), 0)
    out_small = np.zeros((nh, nw, 4), np.uint8)
    on = alpha >= 110
    out_small[on, :3] = np.clip(rgb[on], 0, 255)
    out_small[on, 3] = 255
    out = np.zeros((FH, FW, 4), np.uint8)
    x0, y0 = (FW - nw) // 2, FH - 1 - nh
    out[y0:y0 + nh, x0:x0 + nw] = out_small
    solid = out[..., 3] == 255
    edge = np.zeros_like(solid)
    edge[1:] |= solid[:-1]; edge[:-1] |= solid[1:]; edge[:, 1:] |= solid[:, :-1]; edge[:, :-1] |= solid[:, 1:]
    edge &= ~solid
    out[edge] = OUTLINE
    return Image.fromarray(out, "RGBA")


def shifted(img, dx, dy):
    out = Image.new("RGBA", (FW, FH), (0, 0, 0, 0))
    out.paste(img, (dx, dy), img)
    return out


def squashed(img, hfrac, alpha):
    nh = max(1, int(FH * hfrac))
    small = img.resize((FW, nh), Image.NEAREST)
    out = Image.new("RGBA", (FW, FH), (0, 0, 0, 0))
    out.paste(small, (0, FH - nh), small)
    return mvs.faded(out, alpha)


def glow(img, color, strong):
    """attack frame: a spark in front (right of the figure, mid height)"""
    out = img.copy()
    px = out.load()
    box = img.getbbox() or (0, 0, FW, FH)
    cx, cy = min(FW - 3, box[2] + 1), (box[1] + box[3]) // 2
    r = 2 if strong else 1
    for y in range(cy - r, cy + r + 1):
        for x in range(cx - r, cx + r + 1):
            if abs(x - cx) + abs(y - cy) <= r and 0 <= x < FW and 0 <= y < FH:
                px[x, y] = color
    if strong:
        for x, y in ((cx - 4, cy), (cx, cy - 4), (cx, cy + 4)):
            if 0 <= x < FW and 0 <= y < FH:
                px[x, y] = color
    return out


def frames(base, fx):
    rest, up = shifted(base, 0, 0), shifted(base, 0, -1)
    return [rest, up, rest, up, rest, up,
            shifted(base, -1, 0), glow(shifted(base, 1, 0), fx, True), glow(rest, fx, False),
            squashed(base, 0.8, 0.8), squashed(base, 0.55, 0.55), squashed(base, 0.3, 0.3)]


def main():
    sheet = Image.new("RGBA", (FW * 12, FH * len(ORDER)), (0, 0, 0, 0))
    done = []
    for row, name in enumerate(ORDER):
        path = os.path.join(SRC, name + ".png")
        if not os.path.exists(path):
            continue
        img = cut(path)
        if name in MIRROR:
            img = img.transpose(Image.FLIP_LEFT_RIGHT)
        base = shrink(img)
        for i, f in enumerate(frames(base, FX[name])):
            sheet.paste(f, (i * FW, row * FH), f)
        done.append(name)
    sheet.save(OUT)
    print("ascent bosses:", ", ".join(done))


if __name__ == "__main__":
    main()

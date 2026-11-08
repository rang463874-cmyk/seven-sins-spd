#!/usr/bin/env python3
"""Draws the virtue champions' sprites from the owner's art.

Source art (big pixel-art pictures on a black background) lives in tools/sinsart/virtue_art/src/:
- paladin.png: the virtues' paladins (patience, temperance, diligence)
- saint.png:   the virtues' saints (humility, charity, chastity, kindness)

- angel_<name>.png: the seven archangels (sprites/virtue_angels.png, 64x65 frames, one row each in Virtue order)

Output: core/src/main/assets/sprites/virtue_champions.png, two rows (paladin, saint) of 12 frames,
S x (S+1) each (one spare row on top for the bob): idle (2), run (4), attack (3), die (3) — the same
layout as the demon lords. sprites/VirtueSprites draws them centred on the champion's cell.

Steps: the black background is flood-filled away from the edges, stray specks (e.g. a watermark) are
dropped by keeping only big connected pieces, the art is cropped, shrunk to fit S x S (box filter, then
alpha thresholded), given a dark outline and set bottom centre. Re-run after changing the art:
python3 tools/sinsart/make_virtue_sprites.py
"""
import os
from collections import deque
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
SRC = os.path.join(HERE, "virtue_art", "src")
OUT = os.path.join(ROOT, "core", "src", "main", "assets", "sprites")

S = 48          # art size (same as the demon lords; drawn over the 3x3 tiles around the cell)
H = S + 1
FRAMES = 12
OUTLINE = (20, 16, 24, 255)

ROWS = [
    ("paladin", (255, 230, 150, 255)),
    ("saint", (255, 245, 200, 255)),
]

#the archangels, one row each in Virtue order (humility, charity, chastity, patience, temperance,
#kindness, diligence); drawn bigger (64px, over ~4x4 tiles) as the final boss
ANGEL_SIZE = 64
ANGELS = [
    ("angel_michael", (255, 230, 120, 255)),
    ("angel_raguel", (140, 200, 255, 255)),
    ("angel_gabriel", (255, 255, 255, 255)),
    ("angel_uriel", (255, 120, 80, 255)),
    ("angel_raphael", (140, 255, 170, 255)),
    ("angel_jeremiel", (210, 150, 255, 255)),
    ("angel_sariel", (200, 220, 255, 255)),
]


def cutout(img, dark=40):
    """alpha mask: everything not reachable from the border through near-black pixels"""
    img = img.convert("RGBA")
    w, h = img.size
    px = img.load()
    bg = bytearray(w * h)
    q = deque()
    for x in range(w):
        q.append((x, 0)); q.append((x, h - 1))
    for y in range(h):
        q.append((0, y)); q.append((w - 1, y))
    while q:
        x, y = q.popleft()
        i = y * w + x
        if bg[i]:
            continue
        r, g, b, a = px[x, y]
        if max(r, g, b) > dark:
            continue
        bg[i] = 1
        if x > 0: q.append((x - 1, y))
        if x < w - 1: q.append((x + 1, y))
        if y > 0: q.append((x, y - 1))
        if y < h - 1: q.append((x, y + 1))

    # keep only big pieces of foreground (drops watermarks and specks)
    seen = bytearray(w * h)
    keep = bytearray(w * h)
    total = w * h - sum(bg)
    for start in range(w * h):
        if bg[start] or seen[start]:
            continue
        comp = []
        q.append(start)
        seen[start] = 1
        while q:
            i = q.popleft()
            comp.append(i)
            x, y = i % w, i // w
            for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
                if 0 <= nx < w and 0 <= ny < h:
                    j = ny * w + nx
                    if not bg[j] and not seen[j]:
                        seen[j] = 1
                        q.append(j)
        if len(comp) > total * 0.02:
            for i in comp:
                keep[i] = 1
    for y in range(h):
        for x in range(w):
            if not keep[y * w + x]:
                px[x, y] = (0, 0, 0, 0)
    return img


def shrink(img):
    """crop, scale to fit S x S keeping the aspect, threshold alpha, outline, bottom centre"""
    img = img.crop(img.getbbox())
    scale = min((S - 2) / img.width, (S - 1) / img.height)
    nw, nh = max(1, round(img.width * scale)), max(1, round(img.height * scale))
    # premultiplied box filter so the black background doesn't bleed into the edges
    r, g, b, a = img.split()
    pre = Image.merge("RGBA", (
        Image.composite(r, Image.new("L", img.size, 0), a),
        Image.composite(g, Image.new("L", img.size, 0), a),
        Image.composite(b, Image.new("L", img.size, 0), a), a))
    small = pre.resize((nw, nh), Image.BOX)
    px = small.load()
    for y in range(nh):
        for x in range(nw):
            cr, cg, cb, ca = px[x, y]
            if ca < 110:
                px[x, y] = (0, 0, 0, 0)
            else:
                f = 255 / ca
                px[x, y] = (min(255, int(cr * f)), min(255, int(cg * f)), min(255, int(cb * f)), 255)
    out = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    out.paste(small, ((S - nw) // 2, S - nh), small)
    op = out.load()
    edge = []
    for y in range(S):
        for x in range(S):
            if op[x, y][3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < S and 0 <= ny < S and op[nx, ny][3] == 255 and op[nx, ny] != OUTLINE:
                    edge.append((x, y))
                    break
    for x, y in edge:
        op[x, y] = OUTLINE
    return out


def shifted(img, dx, dy):
    out = Image.new("RGBA", (img.width, H), (0, 0, 0, 0))
    out.paste(img, (dx, H - img.height + dy), img)
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
    nh = max(1, int(img.height * hfrac))
    small = img.resize((img.width, nh), Image.NEAREST)
    out = Image.new("RGBA", (img.width, H), (0, 0, 0, 0))
    out.paste(small, (0, H - nh), small)
    return faded(out, alpha)


def glow(img, color, strong):
    """attack frame: a spark of holy light in front (right side, mid height)"""
    out = img.copy()
    px = out.load()
    cx, cy = S - 3, H // 2
    r = 2 if strong else 1
    for y in range(cy - r, cy + r + 1):
        for x in range(cx - r, cx + r + 1):
            if abs(x - cx) + abs(y - cy) <= r and 0 <= x < S:
                px[x, y] = color
    if strong:
        for x, y in ((cx - 4, cy), (cx, cy - 4), (cx, cy + 4)):
            if 0 <= x < S:
                px[x, y] = color
    return out


def frames(base, fx):
    rest = shifted(base, 0, 0)
    up = shifted(base, 0, -1)
    return [
        rest, up,
        rest, up, rest, up,
        shifted(base, -1, 0), glow(shifted(base, 1, 0), fx, True), glow(rest, fx, False),
        squashed(base, 0.8, 0.8), squashed(base, 0.55, 0.55), squashed(base, 0.3, 0.3),
    ]


def sheet(rows, size, out, blank=False):
    """blank=True adds an empty 13th frame (used by the seraph's invisible body parts)"""
    global S, H
    S, H = size, size + 1
    img = Image.new("RGBA", (S * (FRAMES + (1 if blank else 0)), H * len(rows)), (0, 0, 0, 0))
    for r, (name, fx) in enumerate(rows):
        base = shrink(cutout(Image.open(os.path.join(SRC, name + ".png"))))
        for i, f in enumerate(frames(base, fx)):
            img.paste(f, (i * S, r * H), f)
    img.save(os.path.join(OUT, out))


def main():
    sheet(ROWS, 48, "virtue_champions.png")
    sheet(ANGELS, ANGEL_SIZE, "virtue_angels.png")
    # the heavens (floors 27-30): the owner's believer, priest, lesser angel and seraph
    sheet([("heaven_believer", (255, 255, 255, 255))], 24, "heaven_believer.png")
    sheet([("heaven_priest", (255, 230, 150, 255))], 24, "heaven_priest.png")
    sheet([("heaven_lesser_angel", (255, 80, 80, 255))], 24, "heaven_lesser_angel.png")
    sheet([("heaven_seraph", (255, 200, 80, 255))], 32, "heaven_seraph.png", blank=True)
    print("virtue sprites written")


if __name__ == "__main__":
    main()

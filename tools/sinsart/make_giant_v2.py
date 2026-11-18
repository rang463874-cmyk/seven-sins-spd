#!/usr/bin/env python3
"""The holy giant, second design (Nov 18 2026, owner's five pictures in tools/sinsart/giant_art/v2/):
- body.png (armless upper body, cut at the waist) -> sprites/heaven_giant_body.png at one sheet pixel per art pixel;
  drawn x BODY_ZOOM in game over the void above the arena, its lower part fading into the void
- gold_open/gold_fist.png (the gold bracelet arm, on the screen's left) and bolt_open/bolt_fist.png (the lightning arm,
  on the right; owner, Nov 18 2026) -> sprites/heaven_giant_arms.png, one frame each (left open, left fist, right open,
  right fist)
- the frame sizes and the points the game needs (each arm's shoulder end and hand, the body's shoulders) are written
  to core/.../sprites/GiantArt.java

Each picture is sampled at its own art-pixel pitch (one colour per art pixel), the blue background is flood-filled
away from the border (plus the Gemini sparkle), small islands dropped. Also writes giant_art/v2/preview.png: the
arena with the giant at rest and slamming, laid out like the game does it.
Re-run: python3 tools/sinsart/make_giant_v2.py
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "giant_art", "v2")
OUT = os.path.join(HERE, "..", "..", "core", "src", "main", "assets", "sprites")
JAVA = os.path.join(HERE, "..", "..", "core", "src", "main", "java", "com", "shatteredpixel",
                    "shatteredpixeldungeon", "sprites", "GiantArt.java")

BODY_ZOOM = 4
# pitch search ranges (source px per art px), from the autocorrelation of each picture
PITCH = {"body": (9.6, 11.4), "bolt_open": (10.5, 12.5), "bolt_fist": (11.0, 13.0),
         "gold_open": (13.0, 15.0), "gold_fist": (16.0, 18.0)}
# by eye, in source pixels: each arm's cut end (the shoulder side) and the middle of its hand
ARM_POINTS = {"bolt_open": ((1010, 40), (500, 590)), "bolt_fist": ((1000, 40), (480, 600)),
              "gold_open": ((110, 70), (760, 560)), "gold_fist": ((130, 90), (880, 800))}
# the body's shoulder joints (where the arms come out), source pixels
SHOULDERS = ((300, 470), (1090, 470))
FADE = 0.22   # share of the body's height that fades into the void at the bottom


def sample(path, prange):
    a = np.array(Image.open(path).convert("RGB").filter(ImageFilter.MedianFilter(5))).astype(float)
    h, w, _ = a.shape
    profs = [np.abs(np.diff(a, axis=ax)).sum(2).sum(1 - ax) for ax in (1, 0)]
    best = None
    for pp in np.arange(prange[0], prange[1] + 0.001, 0.05):
        tot, oo = 0, []
        for pr in profs:
            n = len(pr)
            bo = max(((pr[np.clip(np.round(np.arange(o, n, pp)).astype(int), 0, n - 1)].sum()
                       - pr[np.clip(np.round(np.arange(o + pp / 2, n, pp)).astype(int), 0, n - 1)].sum()) / (n / pp), o)
                     for o in np.arange(0, pp, 0.5))
            tot += bo[0]
            oo.append(bo[1])
        if best is None or tot > best[0]:
            best = (tot, pp, oo)
    _, pp, (ox, oy) = best
    cols, rows = int((w - ox) // pp), int((h - oy) // pp)
    out = np.zeros((rows, cols, 3), np.uint8)
    for r in range(rows):
        for c in range(cols):
            x0, y0 = ox + c * pp, oy + r * pp
            cell = a[int(y0 + pp * 0.3):int(y0 + pp * 0.7) + 1, int(x0 + pp * 0.3):int(x0 + pp * 0.7) + 1].reshape(-1, 3)
            out[r, c] = np.median(cell, 0)
    return out, pp, ox, oy


def is_bg(c, bg):
    c = c.astype(int)
    if np.abs(c - bg).sum() < 90:
        return True
    r, g, b = c
    # saturated blue with little red/green: the backdrop (the giant's blues are greyer, the lightning paler)
    return b > 170 and r < 70 and g < 110


def cut(img, island=0.02):
    rows, cols, _ = img.shape
    border = np.concatenate([img[0], img[-1], img[:, 0], img[:, -1]]).astype(int)
    bg = np.median(border, 0)
    seen = np.zeros((rows, cols), bool)
    stack = [(r, c) for r in range(rows) for c in (0, cols - 1)] + [(r, c) for c in range(cols) for r in (0, rows - 1)]
    while stack:
        r, c = stack.pop()
        if r < 0 or c < 0 or r >= rows or c >= cols or seen[r, c]:
            continue
        if not is_bg(img[r, c], bg):
            continue
        seen[r, c] = True
        stack += [(r + 1, c), (r - 1, c), (r, c + 1), (r, c - 1)]
    keep = ~seen
    # the backdrop blue left inside lightning loops and blended at the edges: pure saturated blue anywhere
    keep &= ~((img[..., 0].astype(int) < 45) & (img[..., 2].astype(int) > 190))
    lab = np.zeros((rows, cols), int)
    sizes = {}
    n = 0
    for r in range(rows):
        for c in range(cols):
            if keep[r, c] and not lab[r, c]:
                n += 1
                st, cnt = [(r, c)], 0
                while st:
                    y, x = st.pop()
                    if y < 0 or x < 0 or y >= rows or x >= cols or not keep[y, x] or lab[y, x]:
                        continue
                    lab[y, x] = n
                    cnt += 1
                    st += [(y + 1, x), (y - 1, x), (y, x + 1), (y, x - 1)]
                sizes[n] = cnt
    big = max(sizes.values())
    for k, sz in sizes.items():
        if sz < big * island:
            keep[lab == k] = False
    out = np.zeros((rows, cols, 4), np.uint8)
    out[..., :3] = img
    out[..., 3] = keep * 255
    return out


def load(name):
    img, pp, ox, oy = sample(os.path.join(SRC, name + ".png"), PITCH[name])
    rgba = cut(img)
    ys, xs = np.where(rgba[..., 3] > 0)
    x0, y0 = xs.min(), ys.min()
    rgba = rgba[y0:ys.max() + 1, x0:xs.max() + 1]

    def to_art(p):
        return ((p[0] - ox) / pp - x0, (p[1] - oy) / pp - y0)
    print("%s: pitch %.2f -> %dx%d" % (name, pp, rgba.shape[1], rgba.shape[0]))
    return Image.fromarray(np.ascontiguousarray(rgba), "RGBA").copy(), to_art


def body():
    img, to_art = load("body")
    px = img.load()
    h = img.height
    start = int(h * (1 - FADE))
    for y in range(start, h):
        f = 1 - (y - start + 1) / (h - start + 1)
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a:
                px[x, y] = (r, g, b, int(a * f))
    img.save(os.path.join(OUT, "heaven_giant_body.png"))
    return img, [to_art(s) for s in SHOULDERS]


def arms():
    frames, points = [], []
    for name in ("gold_open", "gold_fist", "bolt_open", "bolt_fist"):
        img, to_art = load(name)
        frames.append(img)
        points.append([to_art(p) for p in ARM_POINTS[name]])
    fw = max(f.width for f in frames)
    fh = max(f.height for f in frames)
    sheet = Image.new("RGBA", (fw * len(frames), fh), (0, 0, 0, 0))
    for i, f in enumerate(frames):
        sheet.paste(f, (i * fw, 0), f)
    sheet.save(os.path.join(OUT, "heaven_giant_arms.png"))
    return frames, points, fw, fh


def write_java(bimg, shoulders, fw, fh, points):
    def f(v):
        return "%.1ff" % v
    with open(JAVA, "w") as o:
        o.write("package com.shatteredpixel.shatteredpixeldungeon.sprites;\n\n")
        o.write("/** generated by tools/sinsart/make_giant_v2.py: sizes and points of the holy giant's pictures (art pixels) */\n")
        o.write("public class GiantArt {\n")
        o.write("\tpublic static final int BODY_W = %d, BODY_H = %d, BODY_ZOOM = %d;\n" % (bimg.width, bimg.height, BODY_ZOOM))
        o.write("\t/** left and right shoulder joints on the body picture */\n")
        o.write("\tpublic static final float[][] SHOULDERS = {{%s, %s}, {%s, %s}};\n" % (
            f(shoulders[0][0]), f(shoulders[0][1]), f(shoulders[1][0]), f(shoulders[1][1])))
        o.write("\tpublic static final int ARM_W = %d, ARM_H = %d;\n" % (fw, fh))
        o.write("\t/** frames: left open, left fist, right open, right fist; {shoulder end x, y, hand x, y} */\n")
        o.write("\tpublic static final float[][] ARMS = {\n")
        for (a, h) in points:
            o.write("\t\t\t{%s, %s, %s, %s},\n" % (f(a[0]), f(a[1]), f(h[0]), f(h[1])))
        o.write("\t};\n}\n")


# ------------------------------------------------------------------ preview (same layout as the game)

W, LEDGE, BOTTOM, T = 29, 18, 34, 16


def place_arm(canvas, frame, pts, shoulder, target, zoom_min=2.2, zoom_max=3.8):
    (ax, ay), (hx, hy) = pts
    dx, dy = target[0] - shoulder[0], target[1] - shoulder[1]
    dist = math.hypot(dx, dy)
    ilen = math.hypot(hx - ax, hy - ay)
    s = max(zoom_min, min(zoom_max, dist / ilen))
    ang = math.degrees(math.atan2(dy, dx) - math.atan2(hy - ay, hx - ax))
    big = frame.resize((int(frame.width * s), int(frame.height * s)), Image.NEAREST)
    # rotate around the hand point: pad so the hand is the centre
    hxs, hys = hx * s, hy * s
    R = int(math.hypot(big.width, big.height)) + 4
    pad = Image.new("RGBA", (2 * R, 2 * R), (0, 0, 0, 0))
    pad.paste(big, (int(R - hxs), int(R - hys)), big)
    rot = pad.rotate(-ang, resample=Image.NEAREST)
    canvas.alpha_composite(rot, (int(target[0] - R), int(target[1] - R)))


def preview(bimg, shoulders, frames, points):
    gx = W // 2
    out = []
    for pose in ("rest", "slam"):
        c = Image.new("RGBA", (W * T, (BOTTOM + 2) * T), (12, 14, 40, 255))
        for y in range(LEDGE, BOTTOM + 1):
            for x in range(2, 27):
                shade = 205 if (x + y) % 2 else 190
                c.paste((shade, shade, 215, 255), (x * T, y * T, (x + 1) * T, (y + 1) * T))
        bw, bh = bimg.width * BODY_ZOOM, bimg.height * BODY_ZOOM
        bx, by = (gx + 0.5) * T - bw / 2, LEDGE * T - bh
        sh = [(bx + s[0] * BODY_ZOOM, by + s[1] * BODY_ZOOM) for s in shoulders]
        targets = [((gx - 6.5) * T, LEDGE * T + 4), ((gx + 7.5) * T, LEDGE * T + 4)]
        fr = [0, 2]
        if pose == "slam":
            targets[0] = ((gx - 8 + 0.5) * T, (LEDGE + 9 + 0.5) * T)
            fr[0] = 1
        for side in (0, 1):
            place_arm(c, frames[fr[side]], points[fr[side]], sh[side], targets[side])
        c.alpha_composite(bimg.resize((bw, bh), Image.NEAREST), (int(bx), int(by)))
        out.append(c)
    p = Image.new("RGBA", (out[0].width * 2 + 8, out[0].height), (0, 0, 0, 255))
    p.paste(out[0], (0, 0))
    p.paste(out[1], (out[0].width + 8, 0))
    p.save(os.path.join(SRC, "preview.png"))


def main():
    bimg, shoulders = body()
    frames, points, fw, fh = arms()
    write_java(bimg, shoulders, fw, fh, points)
    preview(bimg, shoulders, frames, points)
    print("giant v2 written")


if __name__ == "__main__":
    main()

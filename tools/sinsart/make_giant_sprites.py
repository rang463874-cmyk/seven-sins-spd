#!/usr/bin/env python3
"""The holy giant's sprites from the owner's art (tools/sinsart/giant_art/):
- holy_giant.png (giant rising from dark water; cropped at the waterline) -> sprites/heaven_giant.png,
  GW x (GH+1) frames: idle 2, attack 3 (fist/bolt), die 3
- fists.png (two panels: right fist | left fist, labels below) -> sprites/heaven_fists.png,
  FS x (FS+1) frames, row 0 = left fist, row 1 = right fist: idle 2, die 3
Uses the cut-out/shrink helpers of make_virtue_sprites.py. Re-run: python3 tools/sinsart/make_giant_sprites.py
"""
import os
import sys
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import make_virtue_sprites as mv  # noqa: E402

SRC = os.path.join(HERE, "giant_art")
OUT = mv.OUT

GW, GH = 112, 62   # the giant: 7 tiles wide, rising ~4 tiles above its ledge
FS = 32            # a fist: 2x2 tiles over the tile it lies on
WATERLINE = 592


def fit(img, w, h):
    """crop to content and shrink into w x h, bottom centre, outlined (like mv.shrink but any box)"""
    img = img.crop(img.getbbox())
    scale = min((w - 2) / img.width, (h - 1) / img.height)
    nw, nh = max(1, round(img.width * scale)), max(1, round(img.height * scale))
    r, g, b, a = img.split()
    z = Image.new("L", img.size, 0)
    pre = Image.merge("RGBA", (Image.composite(r, z, a), Image.composite(g, z, a), Image.composite(b, z, a), a))
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
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    out.paste(small, ((w - nw) // 2, h - nh), small)
    op = out.load()
    edge = []
    for y in range(h):
        for x in range(w):
            if op[x, y][3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and op[nx, ny][3] == 255 and op[nx, ny] != mv.OUTLINE:
                    edge.append((x, y))
                    break
    for x, y in edge:
        op[x, y] = mv.OUTLINE
    return out


def framed(base, w, h, dy=0, alpha=1.0, hfrac=1.0):
    fr = Image.new("RGBA", (w, h + 1), (0, 0, 0, 0))
    img = base
    if hfrac < 1.0:
        nh = max(1, int(base.height * hfrac))
        img = base.resize((base.width, nh), Image.NEAREST)
    if alpha < 1.0:
        img = img.copy()
        px = img.load()
        for y in range(img.height):
            for x in range(img.width):
                r, g, b, a = px[x, y]
                px[x, y] = (r, g, b, int(a * alpha))
    fr.paste(img, (0, h + 1 - img.height + dy), img)
    return fr


def lightning(img, strong):
    """an attack frame: brighten the art"""
    out = img.copy()
    px = out.load()
    k = 1.35 if strong else 1.15
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a:
                px[x, y] = (min(255, int(r * k)), min(255, int(g * k)), min(255, int(b * k + 20)), a)
    return out


def main():
    giant = Image.open(os.path.join(SRC, "holy_giant.png")).convert("RGBA")
    giant = giant.crop((0, 0, giant.width, WATERLINE))
    g = fit(mv.cutout(giant, dark=30), GW, GH)
    frames = [framed(g, GW, GH), framed(g, GW, GH, dy=-1),
              framed(lightning(g, False), GW, GH), framed(lightning(g, True), GW, GH, dy=-1), framed(g, GW, GH),
              framed(g, GW, GH, alpha=0.8, hfrac=0.85), framed(g, GW, GH, alpha=0.55, hfrac=0.6),
              framed(g, GW, GH, alpha=0.3, hfrac=0.35)]
    sheet = Image.new("RGBA", (GW * len(frames), GH + 1), (0, 0, 0, 0))
    for i, f in enumerate(frames):
        sheet.paste(f, (i * GW, 0), f)
    sheet.save(os.path.join(OUT, "heaven_giant.png"))

    fists = Image.open(os.path.join(SRC, "fists.png")).convert("RGBA")
    w = fists.width
    right = fists.crop((0, 0, 700, 680))
    left = fists.crop((708, 0, w, 680))
    sheet = Image.new("RGBA", (FS * 5, (FS + 1) * 2), (0, 0, 0, 0))
    for row, art in enumerate((left, right)):
        f = fit(mv.cutout(art, dark=30), FS, FS)
        fr = [framed(f, FS, FS), framed(f, FS, FS, dy=-1),
              framed(f, FS, FS, alpha=0.75, hfrac=0.8), framed(f, FS, FS, alpha=0.5, hfrac=0.55),
              framed(f, FS, FS, alpha=0.25, hfrac=0.3)]
        for i, x in enumerate(fr):
            sheet.paste(x, (i * FS, row * (FS + 1)), x)
    sheet.save(os.path.join(OUT, "heaven_fists.png"))
    print("giant sprites written")


if __name__ == "__main__":
    main()

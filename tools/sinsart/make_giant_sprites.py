#!/usr/bin/env python3
"""The holy giant's sprites from the owner's art (tools/sinsart/giant_art/):
- holy_giant.png (giant rising from dark water; cropped at the waterline) -> sprites/heaven_giant.png,
  GW x (GH+1) frames: idle 2, attack 3 (fist/bolt), die 3
- fists.png (two panels: right fist | left fist, labels below) -> sprites/heaven_fists.png,
  FS x (FS+1) frames, row 0 = left fist, row 1 = right fist: idle 2, die 3
- fists.png again -> sprites/heaven_arms.png: the whole arm slammed down (AW x (AH+1), row 0 left / row 1 right):
  the fist turned knuckles-down at the bottom, its forearm band repeated up to the shoulder; idle 2, die 3
- lightning.png (the owner's branching bolt) -> sprites/heaven_lightning.png (LC x LR tiles, alpha from brightness)
  and the struck tiles in core/.../actors/mobs/heaven/GiantLightningPattern.java (cells with >= LT bright pixels)
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
AW, AH = 48, 128   # an arm: 3 x 8 tiles
LC, LR, LT = 21, 17, 0.16
LBOX = (404, 0, 1039, 768)   # the bolt inside lightning.png (the watermark star bottom right is left out)
JAVA = os.path.join(HERE, "..", "..", "core", "src", "main", "java", "com", "shatteredpixel",
                    "shatteredpixeldungeon", "actors", "mobs", "heaven", "GiantLightningPattern.java")


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
    arms(left, right)
    bolt()
    print("giant sprites written")


def arm_art(art, angle, flip, band):
    """knuckles down, forearm up; the forearm band (rows of the turned art) is repeated up to the top"""
    img = mv.cutout(art, dark=30)
    if flip:
        img = img.transpose(Image.FLIP_TOP_BOTTOM)
    img = img.rotate(angle, resample=Image.BICUBIC, expand=True)
    img = img.crop(img.getbbox())
    tall = int(img.width * AH / AW)
    canvas = Image.new("RGBA", (img.width, max(tall, img.height)), (0, 0, 0, 0))
    top = canvas.height - img.height
    strip = img.crop((0, band[0], img.width, band[1]))
    strip = strip.resize((strip.width, strip.height * 2), Image.NEAREST)
    flipped = strip.transpose(Image.FLIP_TOP_BOTTOM)
    y = top + band[0]
    k = 0
    while y > 0:
        y -= strip.height
        piece = flipped if k % 2 == 0 else strip
        canvas.paste(piece, (0, y), piece)
        k += 1
    canvas.paste(img, (0, top), img)
    return canvas


def arms(left, right):
    sheet = Image.new("RGBA", (AW * 5, (AH + 1) * 2), (0, 0, 0, 0))
    for row, (art, angle, flip, band) in enumerate(((left, 0, True, (10, 110)), (right, 38, False, (130, 260)))):
        a = fit(arm_art(art, angle, flip, band), AW, AH)
        fr = [framed(a, AW, AH), framed(a, AW, AH, dy=-1),
              framed(a, AW, AH, alpha=0.75), framed(a, AW, AH, alpha=0.45), framed(a, AW, AH, alpha=0.2)]
        for i, x in enumerate(fr):
            sheet.paste(x, (i * AW, row * (AH + 1)), x)
    sheet.save(os.path.join(OUT, "heaven_arms.png"))


def bolt():
    src = Image.open(os.path.join(SRC, "lightning.png")).convert("RGB").crop(LBOX)
    px = src.load()
    rows = []
    for j in range(LR):
        line = ""
        for i in range(LC):
            ax, bx = src.width * i // LC, src.width * (i + 1) // LC
            ay, by = src.height * j // LR, src.height * (j + 1) // LR
            cnt = sum(1 for y in range(ay, by) for x in range(ax, bx) if sum(px[x, y]) > 200)
            line += "#" if cnt / ((bx - ax) * (by - ay)) > LT else "."
        rows.append(line)
    big = src.resize((LC * 16, LR * 16), Image.BOX)
    out = Image.new("RGBA", big.size, (0, 0, 0, 0))
    bp, op = big.load(), out.load()
    for y in range(big.height):
        for x in range(big.width):
            r, g, b = bp[x, y]
            a = min(255, int(max(r, g, b) * 1.6))
            if a > 24:
                f = 255 / max(1, max(r, g, b))
                op[x, y] = (min(255, int(r * f)), min(255, int(g * f)), min(255, int(b * f)), a)
    out.save(os.path.join(OUT, "heaven_lightning.png"))
    with open(JAVA, "w") as f:
        f.write("package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.heaven;\n\n")
        f.write("/** generated by tools/sinsart/make_giant_sprites.py from giant_art/lightning.png: '#' = struck tile */\n")
        f.write("public class GiantLightningPattern {\n")
        f.write("\tpublic static final int W = %d, H = %d;\n" % (LC, LR))
        f.write("\tpublic static final String[] ROWS = {\n")
        for r in rows:
            f.write('\t\t\t"%s",\n' % r)
        f.write("\t};\n}\n")


if __name__ == "__main__":
    main()

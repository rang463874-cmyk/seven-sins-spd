"""Leviathan's boss art (owner's pictures, Nov 14 2026) -> game assets.

demon_art/leviathan/head_closed.png, head_open.png  -> sprites/demon_leviathan_head.png
    (FW x FH+1 frames in one row: closed, closed 1px lower, open, open, three fading frames, blank)
demon_art/leviathan/beam.png  -> effects/leviathan_beam.png (a horizontal strip, stretched along the shot)
                                 effects/leviathan_ball.png (the glow at the far end)
demon_art/leviathan/wave.jpg  -> effects/leviathan_wave.png (the tidal wave, its crest facing right)
effects/leviathan_drop.png is drawn here (a falling drop).

Run: python3 tools/sinsart/make_leviathan_art.py
"""
import os
from collections import deque

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
SRC = os.path.join(HERE, "demon_art", "leviathan")
ASSETS = os.path.join(ROOT, "core", "src", "main", "assets")

FW, FH = 56, 70          # the head in the game: 3.5 tiles wide, rising over the void behind its ledge tile
WAVE_H = 160             # ten floor rows
BEAM_LEN, BEAM_THICK = 64, 14


def cutout(im, tol, keep_largest=True):
    """RGBA copy with the border-connected background (the colour of the top-left corner) made transparent."""
    a = np.array(im.convert("RGB")).astype(int)
    h, w = a.shape[:2]
    bg = a[2, 2]
    near = np.abs(a - bg).sum(axis=2) <= tol
    seen = np.zeros((h, w), bool)
    q = deque()
    for x in range(w):
        for y in (0, h - 1):
            if near[y, x] and not seen[y, x]:
                seen[y, x] = True
                q.append((y, x))
    for y in range(h):
        for x in (0, w - 1):
            if near[y, x] and not seen[y, x]:
                seen[y, x] = True
                q.append((y, x))
    while q:
        y, x = q.popleft()
        for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            ny, nx = y + dy, x + dx
            if 0 <= ny < h and 0 <= nx < w and near[ny, nx] and not seen[ny, nx]:
                seen[ny, nx] = True
                q.append((ny, nx))
    solid = ~seen
    if keep_largest:
        solid = largest(solid)
    out = np.dstack([a, np.where(solid, 255, 0)]).astype(np.uint8)
    return Image.fromarray(out, "RGBA")


def largest(mask):
    """only the biggest connected blob (drops the Gemini sparkle and stray specks)"""
    h, w = mask.shape
    label = np.zeros((h, w), int)
    best, best_n, cur = 0, 0, 0
    for sy in range(h):
        for sx in range(w):
            if not mask[sy, sx] or label[sy, sx]:
                continue
            cur += 1
            n = 0
            q = deque([(sy, sx)])
            label[sy, sx] = cur
            while q:
                y, x = q.popleft()
                n += 1
                for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    ny, nx = y + dy, x + dx
                    if 0 <= ny < h and 0 <= nx < w and mask[ny, nx] and not label[ny, nx]:
                        label[ny, nx] = cur
                        q.append((ny, nx))
            if n > best_n:
                best, best_n = cur, n
    return label == best


def crop(im):
    return im.crop(im.getbbox()) if im.getbbox() else im


def shrink(im, size):
    """smooth downscale with a hard alpha edge, so the sprite keeps a crisp outline"""
    im = im.resize(size, Image.LANCZOS)
    a = np.array(im)
    a[..., 3] = np.where(a[..., 3] >= 128, 255, 0)
    a[a[..., 3] == 0] = 0
    return Image.fromarray(a, "RGBA")


def fade(im, alpha):
    a = np.array(im).astype(float)
    a[..., 3] *= alpha
    return Image.fromarray(a.astype(np.uint8), "RGBA")


def head():
    closed = cutout(Image.open(os.path.join(SRC, "head_closed.png")), 60)
    opened = cutout(Image.open(os.path.join(SRC, "head_open.png")), 60)
    #both pictures share one box, so the head doesn't jump when the mouth opens
    box = [min(closed.getbbox()[i], opened.getbbox()[i]) for i in (0, 1)] + \
          [max(closed.getbbox()[i], opened.getbbox()[i]) for i in (2, 3)]
    frames = []
    for im in (closed, opened):
        im = im.crop(box)
        scale = min(FW / im.width, FH / im.height)
        size = (max(1, round(im.width * scale)), max(1, round(im.height * scale)))
        small = shrink(im, size)
        f = Image.new("RGBA", (FW, FH + 1), (0, 0, 0, 0))
        f.paste(small, ((FW - size[0]) // 2, FH - size[1]))
        frames.append(f)
    c, o = frames
    bob = Image.new("RGBA", c.size, (0, 0, 0, 0))
    bob.paste(c.crop((0, 0, FW, FH)), (0, 1))
    row = [c, bob, o, o, fade(c, 0.66), fade(c, 0.4), fade(c, 0.15), Image.new("RGBA", c.size, (0, 0, 0, 0))]
    sheet = Image.new("RGBA", (FW * len(row), FH + 1), (0, 0, 0, 0))
    for i, f in enumerate(row):
        sheet.paste(f, (i * FW, 0))
    sheet.save(os.path.join(ASSETS, "sprites", "demon_leviathan_head.png"))
    return sheet


def beam():
    im = cutout(Image.open(os.path.join(SRC, "beam.png")), 60)
    w, h = im.size
    #the shaft: the upper three quarters; the ball: the glow at the bottom
    shaft = crop(im.crop((0, int(h * 0.05), w, int(h * 0.70))))
    ball = crop(im.crop((0, int(h * 0.885), w, h)))
    shaft = shaft.rotate(90, expand=True)           # vertical -> horizontal
    strip = shrink(shaft, (BEAM_LEN, BEAM_THICK))
    strip.save(os.path.join(ASSETS, "effects", "leviathan_beam.png"))
    side = 22
    scale = side / max(ball.size)
    b = shrink(ball, (max(1, round(ball.width * scale)), max(1, round(ball.height * scale))))
    out = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    out.paste(b, ((side - b.width) // 2, (side - b.height) // 2))
    out.save(os.path.join(ASSETS, "effects", "leviathan_ball.png"))
    return strip, out


def wave():
    im = cutout(Image.open(os.path.join(SRC, "wave.jpg")), 26, keep_largest=False)
    width = round(im.width * WAVE_H / im.height)
    small = shrink(im, (width, WAVE_H))
    small.save(os.path.join(ASSETS, "effects", "leviathan_wave.png"))
    return small


DROP = [
    "...X...",
    "...X...",
    "..XLX..",
    "..XLX..",
    ".XLLBX.",
    ".XLBBX.",
    "XLLBBBX",
    "XLBBBBX",
    "XBBBBDX",
    ".XBBDX.",
    "..XXX..",
]
DROP_PAL = {"X": (20, 60, 120, 255), "L": (190, 235, 255, 255), "B": (70, 160, 235, 255), "D": (40, 110, 200, 255)}


def drop():
    im = Image.new("RGBA", (len(DROP[0]), len(DROP)), (0, 0, 0, 0))
    for y, line in enumerate(DROP):
        for x, ch in enumerate(line):
            if ch in DROP_PAL:
                im.putpixel((x, y), DROP_PAL[ch])
    im.save(os.path.join(ASSETS, "effects", "leviathan_drop.png"))
    return im


def main():
    os.makedirs(os.path.join(ASSETS, "effects"), exist_ok=True)
    h, (s, b), w, d = head(), beam(), wave(), drop()
    #a preview of everything on the arena's dark floor, x4
    prev = Image.new("RGBA", (h.width, h.height + w.height + 40), (25, 20, 30, 255))
    prev.alpha_composite(h, (0, 0))
    prev.alpha_composite(w, (0, h.height + 4))
    prev.alpha_composite(s, (w.width + 10, h.height + 10))
    prev.alpha_composite(b, (w.width + 10 + s.width, h.height + 6))
    prev.alpha_composite(d, (w.width + 10, h.height + 40))
    prev.resize((prev.width * 3, prev.height * 3), Image.NEAREST).save(os.path.join(SRC, "preview.png"))
    print("head sheet", h.size, "beam", s.size, "ball", b.size, "wave", w.size, "drop", d.size)


if __name__ == "__main__":
    main()

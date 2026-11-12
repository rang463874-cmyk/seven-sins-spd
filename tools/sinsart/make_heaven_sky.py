#!/usr/bin/env python3
"""The heavens' chasm: the owner's night-sky picture (heaven_art/night_sky.jpg) as one continuous sky drawn over every
chasm tile of floors 27-31 (tiles/custom/HeavenSky). The picture is pixel art at ~3px per pixel: it is cut to
624x336, shrunk 3x to its own pixels (208x112 = 13x7 tiles), mirrored side by side so it repeats seamlessly
(26x7 tiles), its top and bottom edges blended so it also repeats downward, and its colours reduced to 24.
The lower half of the sheet is the same sky with a shadow along the top, used under a floor's edge.
Writes environment/custom_tiles/heaven_sky.png (416x224). Re-run: python3 tools/sinsart/make_heaven_sky.py
"""
import os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "heaven_art", "night_sky.jpg")
OUT = os.path.join(HERE, "..", "..", "core", "src", "main", "assets", "environment", "custom_tiles", "heaven_sky.png")


def main():
    src = Image.open(SRC).convert("RGB")
    w, h = src.size
    x0, y0 = (w - 624) // 2, (h - 336) // 2
    img = src.crop((x0, y0, x0 + 624, y0 + 336)).resize((208, 112), Image.BOX)
    sky = Image.new("RGB", (416, 112))
    sky.paste(img, (0, 0))
    sky.paste(img.transpose(Image.FLIP_LEFT_RIGHT), (208, 0))
    # blend the bottom rows into the top row's colour so the sky repeats downward
    px = sky.load()
    row = sorted(px[x, 0] for x in range(416))
    top = row[len(row) // 2]                      # the plain sky colour (not a star)
    for x in range(416):
        for k in range(6):
            y = 111 - k
            t = (6 - k) / 7
            r, g, b = px[x, y]
            px[x, y] = (int(r + (top[0] - r) * t), int(g + (top[1] - g) * t), int(b + (top[2] - b) * t))
    sky = sky.quantize(colors=24, method=Image.MEDIANCUT, dither=Image.NONE).convert("RGBA")

    shadow = sky.copy()
    sp = shadow.load()
    for x in range(416):
        for y in range(112):
            k = (y % 16)
            if k < 6:
                f = 0.25 + 0.75 * k / 6
                r, g, b, a = sp[x, y]
                sp[x, y] = (int(r * f), int(g * f), int(b * f), a)
    sheet = Image.new("RGBA", (416, 224))
    sheet.paste(sky, (0, 0))
    sheet.paste(shadow, (0, 112))
    sheet.save(OUT)
    print("heaven sky written", sheet.size)


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""The launcher icon from the owner's picture (icon/app_icon.png), for the main and debug builds.

- ic_launcher.png (legacy, 48dp): the whole picture
- adaptive icon (108dp layers): background = the picture's dark edge colour, foreground = the picture at
  FG_DP centred (launchers show roughly the middle 72dp, so little more than the picture's dark frame is cut),
  monochrome = the bright parts of the picture as a white silhouette (Android 13 themed icons)
Re-run: python3 tools/sinsart/make_app_icon.py
"""
import os

from PIL import Image, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
RES = [os.path.join(HERE, "..", "..", "android", "src", s, "res") for s in ("main", "debug")]
SRC = os.path.join(HERE, "icon", "app_icon.png")
DENSITY = {"ldpi": 0.75, "mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
FG_DP = 80


def edge_colour(img):
    px = img.load()
    w, h = img.size
    pts = [px[x, y] for x in range(0, w, 7) for y in (0, 1, h - 2, h - 1)]
    pts += [px[x, y] for y in range(0, h, 7) for x in (0, 1, w - 2, w - 1)]
    n = len(pts)
    return tuple(sum(p[i] for p in pts) // n for i in range(3)) + (255,)


def main():
    src = Image.open(SRC).convert("RGBA")
    bg = edge_colour(src)
    # monochrome mask from brightness (gold chest, halo, wings)
    lum = src.convert("L")
    mono_full = Image.new("RGBA", src.size, (255, 255, 255, 0))
    mono_full.putalpha(lum.point(lambda v: 255 if v > 150 else 0).filter(ImageFilter.MedianFilter(5)))
    for res in RES:
        for dens, f in DENSITY.items():
            d = os.path.join(res, "mipmap-" + dens)
            os.makedirs(d, exist_ok=True)
            size = round(48 * f)
            src.resize((size, size), Image.LANCZOS).save(os.path.join(d, "ic_launcher.png"))
            if dens == "ldpi":
                continue
            layer = round(108 * f)
            fg = round(FG_DP * f)
            off = (layer - fg) // 2
            Image.new("RGBA", (layer, layer), bg).save(os.path.join(d, "ic_launcher_background.png"))
            front = Image.new("RGBA", (layer, layer), (0, 0, 0, 0))
            front.paste(src.resize((fg, fg), Image.LANCZOS), (off, off))
            front.save(os.path.join(d, "ic_launcher_foreground.png"))
            mono = Image.new("RGBA", (layer, layer), (0, 0, 0, 0))
            m = mono_full.resize((round(64 * f), round(64 * f)), Image.LANCZOS)
            mono.paste(m, ((layer - m.width) // 2, (layer - m.height) // 2), m)
            mono.save(os.path.join(d, "ic_launcher_monochrome.png"))
    print("app icon written")


if __name__ == "__main__":
    main()

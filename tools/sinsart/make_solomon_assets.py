#!/usr/bin/env python3
"""Placeholder art for Solomon, the hidden eighth class (Nov 17 2026), like make_sin_assets.py does for the sins
(recoloured cleric art), without touching the sins' art:

  sprites/solomon.png, splashes/solomon.jpg   recoloured cleric sheet / splash (skipped once hero_art/solomon exists)
  sprites/avatars.png                          frame 13 (HeroClass ordinal)
  sprites/items.png                            row 33 +14 ring of Solomon, +15 class armor
  interfaces/hero_icons.png                    row 23 (184..188): subclasses, then armor abilities
Run: python3 tools/sinsart/make_solomon_assets.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import make_sin_assets as m  # noqa: E402

HUE, ACCENT = 272, 48
ORDINAL = 13

RING = [
    "................",
    "................",
    "......RRRR......",
    ".....RWWWWR.....",
    "......RRRR......",
    ".....GGGGGG.....",
    "....GG....GG....",
    "...GH......GG...",
    "...GH......DG...",
    "...GH......DG...",
    "...GG......DG...",
    "....GG....DG....",
    ".....GGDDDG.....",
    "......GGGG......",
    "................",
    "................",
]
RING_PAL = {"R": (120, 40, 170, 255), "W": (220, 170, 255, 255), "G": (220, 175, 60, 255),
            "H": (255, 235, 150, 255), "D": (150, 105, 30, 255)}


def main():
    p = lambda rel: os.path.join(m.A, rel)
    here = os.path.dirname(os.path.abspath(__file__))
    px = m.hero_px(HUE, ACCENT, 1.1, 0.7)

    cleric_sheet = m.Image.open(p("sprites/cleric.png")).convert("RGBA")
    if not os.path.isdir(os.path.join(here, "hero_art", "solomon")):
        sheet = m.recolor(cleric_sheet, px)
        sheet.paste(cleric_sheet.crop((96, 0, 156, cleric_sheet.height)), (96, 0))
        sheet.save(p("sprites/solomon.png"))
        splash = m.Image.open(p("splashes/cleric.jpg")).convert("RGBA")
        m.recolor(splash, m.splash_px(HUE, ACCENT)).convert("RGB").save(p("splashes/solomon.jpg"), quality=92)

    avatars = m.Image.open(p("sprites/avatars.png")).convert("RGBA")
    avatars = m.grow(avatars, 24 * (ORDINAL + 1), 32)
    cleric_avatar = avatars.crop(m.cell(m.CLERIC_ORDINAL, 1000, 24, 32))
    avatars.paste(m.recolor(cleric_avatar, px), m.cell(ORDINAL, 1000, 24, 32)[:2])
    avatars.save(p("sprites/avatars.png"))

    items = m.Image.open(p("sprites/items.png")).convert("RGBA")
    blank = m.Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    items.paste(blank, m.cell(512 + 14, 16)[:2])
    items.paste(m.draw(RING, RING_PAL), m.cell(512 + 14, 16)[:2])
    items.paste(m.recolor(items.crop(m.cell(m.ITEM_ARMOR_CLERIC, 16)), px), m.cell(512 + 15, 16)[:2])
    items.save(p("sprites/items.png"))

    icons = m.Image.open(p("interfaces/hero_icons.png")).convert("RGBA")
    icons = m.grow(icons, 128, 16 * 24)
    for i, src in enumerate(m.CLERIC_SUBCLASS_ICONS + m.CLERIC_ABILITY_ICONS):
        icons.paste(m.rotate_to(icons.crop(m.cell(src, 8)), HUE), m.cell(184 + i, 8)[:2])
    icons.save(p("interfaces/hero_icons.png"))
    print("solomon art done")


if __name__ == "__main__":
    main()

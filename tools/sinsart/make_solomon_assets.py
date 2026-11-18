#!/usr/bin/env python3
"""Placeholder art for Solomon, the hidden eighth class (Nov 17 2026), like make_sin_assets.py does for the sins
(recoloured cleric art), without touching the sins' art:

  sprites/solomon.png, splashes/solomon.jpg   recoloured cleric sheet / splash (skipped once hero_art/solomon exists)
  sprites/avatars.png                          frame 13 (HeroClass ordinal)
  sprites/items.png                            row 33 +14 ring of Solomon (the owner's solomon_art/ring.png), +15 class armor
  interfaces/hero_icons.png                    row 23 (184..188): subclasses, then armor abilities
  interfaces/talent_icons.png                  row 15 (480..505 + heroic energy 506)
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


def ring_icon(path, width=16):
    """the owner's ring picture (Nov 18 2026) -> a 16 px icon: background flood-filled away, premultiplied box
    downscale, alpha cut, black rim, contrast stretched back up (averaging flattens the shading)"""
    from PIL import ImageDraw
    import numpy as np
    im = m.Image.open(path).convert("RGB")
    w, h = im.size
    ff = im.copy()
    for c in [(0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)]:
        ImageDraw.floodfill(ff, c, (255, 0, 255), thresh=30)
    a = np.array(ff)
    bgm = (a[..., 0] == 255) & (a[..., 1] == 0) & (a[..., 2] == 255)
    rgba = np.dstack([np.array(im), np.where(bgm, 0, 255).astype("uint8")])
    ys, xs = np.where(~bgm)
    img = m.Image.fromarray(rgba).crop((xs.min(), ys.min(), xs.max() + 1, ys.max() + 1))
    W, H = width, round(img.height * width / img.width)
    arr = np.array(img).astype(float)
    al = arr[..., 3:4] / 255
    pm = m.Image.fromarray(np.dstack([arr[..., :3] * al, arr[..., 3:4]]).astype("uint8"))
    sm = np.array(pm.resize((W, H), m.Image.BOX)).astype(float)
    alpha = sm[..., 3]
    rgb = np.where(alpha[..., None] > 0, sm[..., :3] / np.maximum(alpha[..., None], 1) * 255, 0)
    opaque = alpha > 100
    out = np.zeros((H, W, 4))
    out[..., :3] = np.clip(rgb, 0, 255)
    out[..., 3] = np.where(opaque, 255, 0)
    rim = np.zeros((H, W), bool)
    for y in range(H):
        for x in range(W):
            if opaque[y, x] and any(not (0 <= y + dy < H and 0 <= x + dx < W and opaque[y + dy, x + dx])
                                    for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                rim[y, x] = True
    inner = opaque & ~rim
    mean = out[..., :3][inner].mean()
    out[..., :3] = np.where(inner[..., None], np.clip((out[..., :3] - mean) * 1.7 + mean + 8, 25, 255), out[..., :3])
    out[rim, :3] = 0
    return m.Image.fromarray(out.astype("uint8"))


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

    if not os.path.isdir(os.path.join(here, "hero_art", "solomon")):
        #with the owner's pictures, make_hero_sprites.py makes the avatar too
        avatars = m.Image.open(p("sprites/avatars.png")).convert("RGBA")
        avatars = m.grow(avatars, 24 * (ORDINAL + 1), 32)
        cleric_avatar = avatars.crop(m.cell(m.CLERIC_ORDINAL, 1000, 24, 32))
        avatars.paste(m.recolor(cleric_avatar, px), m.cell(ORDINAL, 1000, 24, 32)[:2])
        avatars.save(p("sprites/avatars.png"))

    items = m.Image.open(p("sprites/items.png")).convert("RGBA")
    blank = m.Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    items.paste(blank, m.cell(512 + 14, 16)[:2])
    ring_src = os.path.join(here, "solomon_art", "ring.png")
    if os.path.exists(ring_src):
        icon = ring_icon(ring_src)
        items.paste(icon, m.cell(512 + 14, 16)[:2])
        print("ring icon %dx%d (set assignItemRect(SOLOMON_RING) to match)" % icon.size)
    else:
        items.paste(m.draw(RING, RING_PAL), m.cell(512 + 14, 16)[:2])
    items.paste(m.recolor(items.crop(m.cell(m.ITEM_ARMOR_CLERIC, 16)), px), m.cell(512 + 15, 16)[:2])
    items.save(p("sprites/items.png"))

    icons = m.Image.open(p("interfaces/hero_icons.png")).convert("RGBA")
    icons = m.grow(icons, 128, 16 * 24)
    for i, src in enumerate(m.CLERIC_SUBCLASS_ICONS + m.CLERIC_ABILITY_ICONS):
        icons.paste(m.rotate_to(icons.crop(m.cell(src, 8)), HUE), m.cell(184 + i, 8)[:2])
    icons.save(p("interfaces/hero_icons.png"))

    # talents (Nov 18 2026): row 15 = 480 + i, heroic energy at +26, recoloured cleric talents
    talents = m.Image.open(p("interfaces/talent_icons.png")).convert("RGBA")
    talents = m.grow(talents, 512, 16 * 16)
    for i in range(26):
        talents.paste(m.rotate_to(talents.crop(m.cell(m.CLERIC_TALENT_ROW + i, 32)), HUE), m.cell(480 + i, 32)[:2])
    talents.paste(m.rotate_to(talents.crop(m.cell(m.CLERIC_TALENT_ROW + 26, 32)), HUE), m.cell(480 + 26, 32)[:2])
    talents.save(p("interfaces/talent_icons.png"))
    print("solomon art done")


if __name__ == "__main__":
    main()

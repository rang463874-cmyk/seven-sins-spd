#!/usr/bin/env python3
"""Hero sheets from the owner's pixel-art pictures (Gemini output).

Input:  tools/sinsart/hero_art/<sin>/{front,walk,attack,die}.png
        Big pictures of pixel art drawn on a visible grid (each art pixel ~23px), on a flat background.
        front = idle (facing the viewer), walk = side view facing right, attack = side view (either way,
        mirrored to face right if the weapon is on the left), die = lying on the ground.
Output: core/src/main/assets/sprites/<sin>.png   (21 frames of 12x15 per row, one row per armor tier 0..6,
                                                  same layout as the vanilla hero sheets, see HeroSprite)
        core/src/main/assets/sprites/avatars.png (24x32 frame at the class ordinal)
        tools/sinsart/hero_art/<sin>/preview.png (sheet x6 on grey, for checking)

Steps: find the art-pixel grid (pitch/offset from edge strength), sample one colour per art pixel, cut the
background (+ the blood splashes and the Gemini sparkle) by flood fill from the border, shrink to 12x15 by
area majority, redraw a 1px black outline, then derive the animation frames and recolour the clothes per
armor tier. Re-run after adding or changing pictures: python3 tools/sinsart/make_hero_sprites.py
make_sin_assets.py leaves the sheet and avatar of a sin with pictures here alone.
"""
import colorsys
import os
import sys
from collections import Counter

import numpy as np
from PIL import Image, ImageFilter

ROOT = os.path.dirname(os.path.abspath(__file__))
ART = os.path.join(ROOT, "hero_art")
ASSETS = os.path.join(ROOT, "..", "..", "core", "src", "main", "assets")

SINS = ["gluttony", "greed", "wrath", "pride", "lust", "envy", "sloth"]
FIRST_SIN_ORDINAL = 6
FW, FH = 12, 15
FRAMES = 21
TIERS = 7

# clothes colour per armor tier (hue deg, saturation, value multiplier); None = the picture's own colours.
# 0 no armor, 1 cloth, 2 leather, 3 mail, 4 scale, 5 plate, 6 class armor (sin colour)
TIER_CLOTHES = {
    "default": [None, (40, 0.18, 1.35), (28, 0.55, 0.95), (225, 0.10, 1.25), (8, 0.60, 1.15),
                (250, 0.12, 1.55), None],
}
# pictures without a visible grid (soft Gemini "pixel art"): shrunk straight from the picture instead of sampled per cell
NO_GRID = {"greed"}
NO_GRID_STEP = 12          # source px per intermediate px for those
# drop islands smaller than this share of the biggest one (flying coins, motion swooshes, sparkles)
ISLAND_SHARE = {"default": 0.05, "greed": 0.15}

# which pixels are "clothes" for the tier recolour: hue range (0..1, may wrap), min saturation, min value,
# and only rows at or below this share of the figure's height (keeps hoods/hair/eyes as drawn)
CLOTHES = {
    "default": dict(hue=(0.2, 0.48), smin=0.3, vmin=0.0, below=0.0),     # gluttony's green tunic
    "greed": dict(hue=(0.94, 0.04), smin=0.5, vmin=0.35, below=0.45),   # the red skirt (not the hood)
}

SIN_HUE = {"gluttony": 285, "greed": 46, "wrath": 4, "pride": 265, "lust": 332, "envy": 125, "sloth": 215}


# ------------------------------------------------------------------ picture -> native pixel art

def fit_axis(a, axis):
    prof = np.abs(np.diff(a, axis=axis)).sum(2).sum(1 - axis)
    n = len(prof)
    best = None
    for p in np.arange(18.0, 30.0, 0.02):
        for o in np.arange(0, p, 0.25):
            edges = np.arange(o, n, p)
            e = prof[np.clip(np.round(edges).astype(int), 0, n - 1)].sum()
            m = prof[np.clip(np.round(edges + p / 2).astype(int), 0, n - 1)].sum()
            s = (e - m) / len(edges)
            if best is None or s > best[0]:
                best = (s, p, o)
    return best[1], best[2]


def native(path):
    """One RGB colour per art pixel (median of the cell's middle), grid lines filtered out."""
    a = np.array(Image.open(path).convert("RGB").filter(ImageFilter.MedianFilter(5))).astype(float)
    h, w, _ = a.shape
    px, ox = fit_axis(a, 1)
    py, oy = fit_axis(a, 0)
    cols, rows = int((w - ox) // px), int((h - oy) // py)
    out = np.zeros((rows, cols, 3), np.uint8)
    for r in range(rows):
        for c in range(cols):
            x0, y0 = ox + c * px, oy + r * py
            cell = a[int(y0 + py * 0.3):int(y0 + py * 0.7), int(x0 + px * 0.3):int(x0 + px * 0.7)].reshape(-1, 3)
            out[r, c] = np.median(cell, 0)
    return out


def native_plain(path, step=NO_GRID_STEP):
    """Pictures without a grid: median-filter away soft edges, box-shrink, merge near shades."""
    im = Image.open(path).convert("RGB").filter(ImageFilter.MedianFilter(7))
    w, h = im.size
    small = im.resize((max(1, round(w / step)), max(1, round(h / step))), Image.BOX)
    small = small.quantize(colors=64, method=Image.MEDIANCUT, dither=Image.Dither.NONE).convert("RGB")
    return np.array(small)


def hsv(c):
    return colorsys.rgb_to_hsv(*(np.asarray(c[:3], float) / 255))


def is_blood(c):
    h, s, v = hsv(c)
    return (h < 0.03 or h > 0.97) and s > 0.45 and 0.3 < v < 0.65


def cut_background_(img, island=0.05):
    """RGBA with the background, blood and stray marks cleared (flood fill from the border)."""
    rows, cols, _ = img.shape
    border = np.concatenate([img[0], img[-1], img[:, 0], img[:, -1]]).astype(int)
    bg = np.median(border, 0)

    def bgish(c):
        c = c.astype(int)
        if np.abs(c - bg).sum() < 60:
            return True
        h, s, v = hsv(c)
        return s < 0.12 and v > 0.6          # grid lines, the sparkle mark

    seen = np.zeros((rows, cols), bool)
    stack = [(r, c) for r in range(rows) for c in (0, cols - 1)] + [(r, c) for c in range(cols) for r in (0, rows - 1)]
    while stack:
        r, c = stack.pop()
        if r < 0 or c < 0 or r >= rows or c >= cols or seen[r, c]:
            continue
        if not (bgish(img[r, c]) or is_blood(img[r, c])):
            continue
        seen[r, c] = True
        stack += [(r + 1, c), (r - 1, c), (r, c + 1), (r, c - 1)]
    keep = ~seen
    # drop small islands (sparkle, specks)
    lab = np.zeros((rows, cols), int)
    sizes = {}
    n = 0
    for r in range(rows):
        for c in range(cols):
            if keep[r, c] and not lab[r, c]:
                n += 1
                st = [(r, c)]
                cnt = 0
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
    ys, xs = np.where(keep)
    return out[ys.min():ys.max() + 1, xs.min():xs.max() + 1]


def is_black(c):
    return int(c[0]) + int(c[1]) + int(c[2]) < 120


# ------------------------------------------------------------------ shrinking

def shrink(art, scale):
    """Area-majority downscale of RGBA pixel art by `scale` (target px per source px), then a 1px outline."""
    sh, sw = art.shape[:2]
    th, tw = max(1, round(sh * scale)), max(1, round(sw * scale))
    feat = small_dark_blobs(art)
    out = np.zeros((th, tw, 4), np.uint8)
    share = np.zeros((th, tw))            # black share of each target pixel's footprint
    featured = {}                         # (y, x) -> colour under a kept eye/mouth pixel
    for ty in range(th):
        for tx in range(tw):
            y0, y1 = ty / scale, (ty + 1) / scale
            x0, x1 = tx / scale, (tx + 1) / scale
            weights = Counter()
            opaque = total = fshare = 0.0
            for sy in range(int(y0), min(sh, int(np.ceil(y1)))):
                for sx in range(int(x0), min(sw, int(np.ceil(x1)))):
                    wgt = (min(y1, sy + 1) - max(y0, sy)) * (min(x1, sx + 1) - max(x0, sx))
                    total += wgt
                    p = art[sy, sx]
                    if p[3]:
                        opaque += wgt
                        if feat[sy, sx]:
                            fshare += wgt
                        weights[tuple(int(v) for v in p[:3])] += wgt
            if total == 0 or opaque / total < 0.5:
                continue
            blk = sum(w for c, w in weights.items() if is_black(c))
            share[ty, tx] = blk / opaque
            if blk / opaque >= 0.62:
                out[ty, tx] = (0, 0, 0, 255)
                continue
            # majority colour among non-black, merging near-identical shades
            groups = Counter()
            reps = {}
            for c, w in weights.items():
                if is_black(c):
                    continue
                key = tuple(v // 24 for v in c)
                groups[key] += w
                if key not in reps or weights[reps[key]] < w:
                    reps[key] = c
            key = groups.most_common(1)[0][0]
            col = reps[key]
            # small dark features (eyes, mouth) on skin survive as black
            if fshare / opaque >= 0.2 and is_skin(col):
                out[ty, tx] = (0, 0, 0, 255)
                featured[(ty, tx)] = col
                continue
            out[ty, tx] = (*col, 255)
    # features stay one pixel tall at this size: a feature right under another goes back to skin
    for (y, x), col in sorted(featured.items()):
        if (y - 1, x) in featured:
            out[y, x] = (*col, 255)
    return outline(out, share)


def is_skin(c):
    h, s, v = hsv(c)
    return 0.03 < h < 0.12 and 0.2 < s < 0.6 and v > 0.7


def small_dark_blobs(art, limit=6):
    """Black pixels in small 4-connected groups that touch skin: eyes, mouths (outlines are long lines)."""
    h, w = art.shape[:2]
    blk = np.array([[art[y, x, 3] and is_black(art[y, x]) for x in range(w)] for y in range(h)])
    out = np.zeros((h, w), bool)
    seen = np.zeros((h, w), bool)
    for y in range(h):
        for x in range(w):
            if not blk[y, x] or seen[y, x]:
                continue
            comp, st = [], [(y, x)]
            while st:
                cy, cx = st.pop()
                if cy < 0 or cx < 0 or cy >= h or cx >= w or seen[cy, cx] or not blk[cy, cx]:
                    continue
                seen[cy, cx] = True
                comp.append((cy, cx))
                st += [(cy + 1, cx), (cy - 1, cx), (cy, cx + 1), (cy, cx - 1)]
            if len(comp) > limit:
                continue
            touches_skin = any(0 <= cy + dy < h and 0 <= cx + dx < w and art[cy + dy, cx + dx, 3]
                               and is_skin(art[cy + dy, cx + dx])
                               for cy, cx in comp for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if touches_skin:
                for c in comp:
                    out[c] = True
    return out


def outline(a, share=None):
    """Black on the silhouette's edge; with `share`, only where the picture had dark outline there."""
    h, w = a.shape[:2]
    out = a.copy()
    for y in range(h):
        for x in range(w):
            if not a[y, x, 3] or (share is not None and share[y, x] < 0.2):
                continue
            for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                ny, nx = y + dy, x + dx
                if ny < 0 or nx < 0 or ny >= h or nx >= w or not a[ny, nx, 3]:
                    out[y, x] = (0, 0, 0, 255)
                    break
    return out


# ------------------------------------------------------------------ frames

def head_center(a):
    """x centre of the top 40% of the opaque pixels (the head), used to line poses up."""
    ys, xs = np.where(a[..., 3] > 0)
    top = ys.min() + (ys.max() - ys.min()) * 0.4
    sel = xs[ys <= top]
    return (sel.min() + sel.max()) / 2


def place(a, cx=None, dy=0):
    """Put art into a 12x15 frame, bottom-aligned, head centred (cropping overflow)."""
    f = np.zeros((FH, FW, 4), np.uint8)
    h, w = a.shape[:2]
    if cx is None:
        cx = head_center(a)
    ox = int(round(FW / 2 - cx - 0.5))
    oy = FH - h + dy
    for y in range(h):
        for x in range(w):
            ty, tx = y + oy, x + ox
            if 0 <= ty < FH and 0 <= tx < FW and a[y, x, 3]:
                f[ty, tx] = a[y, x]
    return f


def faces_left(art):
    """True if most of the width sticks out to the left of the head (weapon thrust to the left)."""
    w = art.shape[1]
    hc = head_center(art)
    return hc > w / 2


def mirror(a):
    return a[:, ::-1].copy()


def swap_legs(a):
    """Mirror the bottom third around the body's centre: the other stride for walk cycles."""
    out = a.copy()
    h, w = a.shape[:2]
    top = h - max(3, h // 3)
    cx = head_center(a)
    leg = a[top:].copy()
    out[top:] = 0
    for y in range(leg.shape[0]):
        for x in range(w):
            if leg[y, x, 3]:
                nx = int(round(2 * cx - x))
                if 0 <= nx < w:
                    out[top + y, nx] = leg[y, x]
    return out


def squash(a, rows):
    """Remove `rows` rows from the middle (crouch/breathe), keeping head and feet."""
    h = a.shape[0]
    mid = h // 2
    keep = [y for y in range(h) if not (mid - rows // 2 <= y < mid - rows // 2 + rows)]
    return a[keep]


def build_frames(pics, name="default"):
    isl = ISLAND_SHARE.get(name, ISLAND_SHARE["default"])

    def cut_background(img):
        return cut_background_(img, isl)

    front_n = cut_background(pics["front"])
    scale = min(FH / front_n.shape[0], FW / front_n.shape[1])
    front = shrink(front_n, scale)
    walk = shrink(cut_background(pics["walk"]), scale)
    atk_n = cut_background(pics["attack"])
    if faces_left(atk_n):
        atk_n = mirror(atk_n)
    attack = shrink(atk_n, scale)
    die_n = cut_background(pics["die"])
    # lying down is much wider than a frame: shrink a bit more and let the feet/weapon tip run off the edge
    die = shrink(die_n, min(scale, (FW + 4) / die_n.shape[1]))

    fr = [None] * FRAMES
    fr[0] = place(front)
    fr[1] = place(squash(front, 1))
    walk2 = swap_legs(walk)
    for i, (img, dy) in enumerate([(walk, 0), (walk, -1), (walk, 0), (walk2, 0), (walk2, -1), (walk2, 0)]):
        fr[2 + i] = place(img, dy=dy)
    fr[8] = place(squash(front, 2))
    fr[9] = place(squash(front, 5))
    dcx = head_center(die) - 1          # keep the head and the torso in the frame
    for i in (10, 11, 12):
        fr[i] = place(die, cx=dcx)
    hc = head_center(attack)
    fr[13] = place(walk)
    fr[14] = place(attack, cx=hc)
    fr[15] = place(attack, cx=hc + 1)
    fr[16] = place(squash(front, 1))
    fr[17] = place(squash(front, 2))
    fr[18] = place(walk, dy=-1)
    fr[19] = place(front)
    fr[20] = place(squash(front, 1))
    return fr, front_n


def load_poses(path):
    """Hand-pixelled poses (hero_art/<sin>/poses.txt): a palette block, then front/walk/attack/die grids."""
    pal, poses, cur = {}, {}, None
    for line in open(path):
        line = line.rstrip("\n")
        if not line.strip() or line.startswith("#"):
            continue
        word = line.strip()
        if word in ("palette", "front", "walk", "attack", "die"):
            cur = word
            if cur != "palette":
                poses[cur] = []
            continue
        if cur == "palette":
            k, rgb = word.split()
            pal[k] = tuple(int(v) for v in rgb.split(","))
        else:
            poses[cur].append(line)
    out = {}
    for k, rows in poses.items():
        a = np.zeros((len(rows), max(len(r) for r in rows), 4), np.uint8)
        for y, r in enumerate(rows):
            for x, ch in enumerate(r):
                if ch != ".":
                    a[y, x] = (*pal[ch], 255)
        out[k] = a
    return out


def frames_from_poses(p):
    """The same frame layout as build_frames, from ready-made 12x15 poses."""
    front, walk, attack, die = p["front"], p["walk"], p["attack"], p["die"]
    fr = [None] * FRAMES
    fr[0] = place(front)
    fr[1] = place(squash(front, 1))
    walk2 = swap_legs(walk)
    for i, (img, dy) in enumerate([(walk, 0), (walk, -1), (walk, 0), (walk2, 0), (walk2, -1), (walk2, 0)]):
        fr[2 + i] = place(img, dy=dy)
    fr[8] = place(squash(front, 2))
    fr[9] = place(squash(front, 5))
    for i in (10, 11, 12):
        fr[i] = place(die, cx=die.shape[1] / 2 - 0.5)
    fr[13] = place(walk)
    fr[14] = place(attack, cx=head_center(attack))
    fr[15] = place(attack, cx=head_center(attack) + 1)
    fr[16] = place(squash(front, 1))
    fr[17] = place(squash(front, 2))
    fr[18] = place(walk, dy=-1)
    fr[19] = place(front)
    fr[20] = place(squash(front, 1))
    return fr


def in_hue(h, rng):
    lo, hi = rng
    return lo <= h <= hi if lo <= hi else (h >= lo or h <= hi)


def recolor_clothes(frame, spec, name="default"):
    """Shift the clothes (see CLOTHES) to a tier colour."""
    if spec is None:
        return frame
    hue, sat, vmul = spec
    cl = CLOTHES.get(name, CLOTHES["default"])
    out = frame.copy()
    ys = np.where(frame[..., 3] > 0)[0]
    if not len(ys):
        return out
    first = ys.min() + (ys.max() - ys.min()) * cl["below"]
    for y in range(FH):
        if y < first:
            continue
        for x in range(FW):
            p = frame[y, x]
            if not p[3]:
                continue
            h, s, v = hsv(p)
            if in_hue(h, cl["hue"]) and s > cl["smin"] and v > cl["vmin"]:
                r, g, b = colorsys.hsv_to_rgb(hue / 360, sat, min(1, v * vmul))
                out[y, x, :3] = (int(r * 255), int(g * 255), int(b * 255))
    return out


def build_sheet(name, frames):
    sheet = Image.new("RGBA", (256, 128), (0, 0, 0, 0))
    tiers = TIER_CLOTHES.get(name, TIER_CLOTHES["default"])
    for t in range(TIERS):
        spec = tiers[t]
        if t == 6:
            spec = (SIN_HUE[name], 0.55, 1.0)
        for i, f in enumerate(frames):
            sheet.paste(Image.fromarray(recolor_clothes(f, spec, name)), (i * FW, t * FH))
    return sheet


def avatar(front_n):
    """24x32 avatar from the full-size front picture."""
    h, w = front_n.shape[:2]
    scale = min(1.0, 31 / h, 23 / w)
    a = shrink(front_n, scale) if scale < 1 else outline(front_n)
    img = Image.new("RGBA", (24, 32), (0, 0, 0, 0))
    ah, aw = a.shape[:2]
    img.paste(Image.fromarray(a), ((24 - aw) // 2, 32 - ah))
    return img


# where the character's body is in each splash picture (share of the width), centred in the 800x450 splash
SPLASH_FOCUS = {"default": 0.5, "greed": 0.52}


def splash(name, src):
    """Hero select background: every splash must be 800x450 (the scene scales by the first texture's height).
    The owner's picture is fitted to the height on black, its character in the middle (portrait phones only
    show the middle)."""
    im = Image.open(src).convert("RGB")
    s = 450 / im.height
    im = im.resize((round(im.width * s), 450), Image.LANCZOS)
    out = Image.new("RGB", (800, 450), (0, 0, 0))
    fx = SPLASH_FOCUS.get(name, SPLASH_FOCUS["default"]) * im.width
    out.paste(im, (int(400 - fx), 0))
    out.save(os.path.join(ASSETS, "splashes", name + ".jpg"), quality=92)


def main():
    names = sys.argv[1:] or [s for s in SINS if os.path.isdir(os.path.join(ART, s))]
    avatars_path = os.path.join(ASSETS, "sprites", "avatars.png")
    avatars = Image.open(avatars_path).convert("RGBA")
    for name in names:
        d = os.path.join(ART, name)
        load = native_plain if name in NO_GRID else native
        pics = {k: load(os.path.join(d, k + ".png")) for k in ("front", "walk", "attack", "die")}
        frames, front_n = build_frames(pics, name)
        poses = os.path.join(d, "poses.txt")
        if os.path.exists(poses):          # too detailed to shrink well: hand-pixelled poses win
            pz = load_poses(poses)
            frames = frames_from_poses(pz)
            front_n = None
            pose_avatar = Image.fromarray(pz["front"]).resize((pz["front"].shape[1] * 2, pz["front"].shape[0] * 2),
                                                             Image.NEAREST)
        sheet = build_sheet(name, frames)
        sheet.save(os.path.join(ASSETS, "sprites", name + ".png"))
        k = SINS.index(name)
        x = 24 * (FIRST_SIN_ORDINAL + k)
        avatars.paste(Image.new("RGBA", (24, 32), (0, 0, 0, 0)), (x, 0))
        if front_n is None:                # hand poses: the avatar is the front pose at 2x
            av = Image.new("RGBA", (24, 32), (0, 0, 0, 0))
            av.paste(pose_avatar, ((24 - pose_avatar.width) // 2, 32 - pose_avatar.height))
            avatars.paste(av, (x, 0))
        else:
            avatars.paste(avatar(front_n), (x, 0))
        prev = Image.new("RGBA", sheet.size, (90, 90, 90, 255))
        prev.alpha_composite(sheet)
        prev.resize((sheet.width * 6, sheet.height * 6), Image.NEAREST).save(os.path.join(d, "preview.png"))
        sp = os.path.join(d, "splash_src.png")
        if os.path.exists(sp):
            splash(name, sp)
        print("built", name)
    avatars.save(avatars_path)


if __name__ == "__main__":
    main()

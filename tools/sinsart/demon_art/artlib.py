from PIL import Image
K = (24, 14, 22, 255)

def build(half=None, full=None, patches=(), pal=None, width=48, height=48):
    """half: left halves (mirrored to the right). full: complete rows. patches: (x, y, text) overlays ('.' = keep)."""
    rows = []
    if half is not None:
        for r in half:
            assert len(r) == width // 2, (len(r), r)
            rows.append(r + r[::-1])
    else:
        for r in full:
            assert len(r) <= width, (len(r), r)
            rows.append(r.ljust(width, '.'))
    assert len(rows) == height, len(rows)
    rows = [list(r) for r in rows]
    for x, y, text in patches:
        for i, ch in enumerate(text):
            if ch != '.' and 0 <= x + i < width:
                rows[y][x + i] = ' ' if ch == '_' else ch
    img = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch in '. ':
                continue
            img.putpixel((x, y), pal[ch])
    return img

def _preview(imgs, path, z=5, bg=(60, 56, 70, 255)):
    w = sum(i.width for i in imgs) + 4 * len(imgs)
    h = max(i.height for i in imgs)
    out = Image.new('RGBA', (w, h), bg)
    x = 0
    for i in imgs:
        out.paste(i, (x, h - i.height), i); x += i.width + 4
    out.resize((w * z, h * z), Image.NEAREST).save(path)

from .artlib import K
PAL = {"K": K, "A": (64, 142, 240, 255), "a": (32, 82, 176, 255), "c": (140, 206, 255, 255),
       "G": (246, 196, 60, 255), "g": (176, 120, 30, 255), "R": (110, 240, 255, 255),
       "D": (40, 40, 84, 255), "E": (232, 250, 255, 255), "T": (196, 228, 255, 255), "t": (122, 170, 232, 255),
       "S": (214, 232, 255, 255), "N": (52, 112, 212, 255), "n": (26, 62, 144, 255), "B": (146, 212, 255, 255),
       "W": (200, 246, 255, 255), "w": (90, 190, 240, 255)}
HALF = [
 "......................KK",  # 0
 ".....................KGG",
 ".....................KGG",
 "................K...KGGG",
 "...............KGK..KGGG",
 "...............KGGK.KGGG",  # 5
 "...............KGGGKGGRR",
 "...............KGGGGGGRR",
 "............KKKKgGgGgGgG",
 "......KK...KAAAKKKKKKKKK",
 ".....KcAK.KAcAAAAAAAAAAA",  # 10
 "....KccAAKKAcAAAAAAAAAAA",
 "....KcAAAAKcAAAAAKAAAAKA",
 ".....KcAAKAcAAAAKDKAAKDK",
 "......KKKAcAAAAKDDDKKDDD",
 ".......KAcAAAAKDDDDDDDDD",  # 15
 "......KAcAAAAAKDDEEDDDDD",
 "......KAcAAAAAKDDEEDDDDD",
 "......KAcAAaAAKDDDDDDDDD",
 ".....KAcAAAaAAAKDDDDDDDD",
 ".....KAcAAaAAAAAKKDDDDDD",  # 20
 ".....KAcAAaAAAAAAKKKKSSS",
 "....KAcAAaAAAKKKKTTTTKKS",
 "....KAcAAaAAKTTTTTTTTTTK",
 "....KAcAaAAKTTTTTttTTTTT",
 "....KAcAaAAKTTTTKKtTTTTT",  # 25
 "...KAcAaAAKKTTTKSSKtTTTT",
 "...KAcAaAAK.KKKSSKKKTTTT",
 "...KAcAaAAK....KKKNNNNNN",
 "...KAAaAAK.......KNNNNNN",
 "....KAaAK.........KNNNNN",  # 30
 ".....KKK...........KNNNN",
 "........................",
 "........................",
 "........................",
 "........................",  # 35
 "........................",
 "........................",
 "........................",
 "........................",
 "........................",  # 40
 "........................",
 "........................",
 "........................",
 "........................",
 "........................",  # 45
 "........................",
 "........................",
]
# the serpent tail: a thick body sweeping down into a coil (drawn by code)
def _tail():
    W, H = 48, 48
    grid = [['.'] * W for _ in range(H)]
    cells = {}
    # waist down into the coil
    for y in range(28, 39):
        half = 6 + (y - 28) // 2
        cx = 24 + (y - 28) // 3
        for x in range(cx - half, cx + half):
            cells[(x, y)] = 'N'
    # the coil: an elliptic ring
    cx, cy, rx, ry, th = 23.5, 41.0, 19, 6.2, 6.5
    for y in range(34, 48):
        for x in range(W):
            d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2
            din = ((x - cx) / (rx - th)) ** 2 + ((y - cy) / (ry - 3.0)) ** 2
            if d <= 1 and din >= 1:
                cells[(x, y)] = 'N'
            elif d <= 1 and y >= cy:
                cells[(x, y)] = 'X'
    # shading: belly stripe on the upper inner edge, dark underside
    for (x, y) in list(cells):
        if cells[(x, y)] == 'X':
            continue
        below = (x, y + 1) in cells and cells[(x, y + 1)] != 'X'
        above = (x, y - 1) in cells
        if not below:
            cells[(x, y)] = 'n'
        elif not above and y > 36:
            cells[(x, y)] = 'B'
    for y in range(29, 39):
        for x in range(W):
            if cells.get((x, y)) == 'N' and (x + 1, y) in cells and (x - 1, y) in cells and x == 22 + (y - 28) // 3:
                cells[(x, y)] = 'B'
    for k, c in list(cells.items()):
        if c == 'X':
            cells[k] = 'n'
    for (x, y), c in cells.items():
        if 0 <= x < W and 0 <= y < H:
            grid[y][x] = c
    for y in range(H):
        for x in range(W):
            if grid[y][x] == '.' and any((x + dx, y + dy) in cells for dx, dy in ((1,0),(-1,0),(0,1),(0,-1))) and y >= 30:
                grid[y][x] = 'K'
    # water splashing around the coil
    for x in range(W):
        if grid[47][x] == '.':
            grid[47][x] = 'w' if (x // 3) % 2 else 'W'
    return [(0, y, ''.join(grid[y])) for y in range(30, 48)]

PATCHES = _tail()

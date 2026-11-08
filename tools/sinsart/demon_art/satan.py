from .artlib import K
PAL = {"K": K, "B": (46, 38, 46, 255), "b": (80, 68, 82, 255), "h": (124, 110, 128, 255), "H": (64, 54, 60, 255),
       "R": (226, 36, 30, 255), "E": (255, 70, 40, 255), "F": (255, 136, 30, 255), "f": (255, 222, 92, 255),
       "C": (30, 20, 28, 255), "c": (150, 22, 34, 255), "s": (196, 196, 208, 255), "z": (132, 132, 144, 255)}
HALF = [
 "....KK..................",  # 0
 "...KhK..................",
 "...KhHK.................",
 "...KhHK.................",
 "...KHhHK................",
 "....KHhHK...............",  # 5
 "....KHHhHK..............",
 ".....KHHhHKK......KKKKKK",
 "......KHHHHHKK...KbbbbbR",
 ".......KKHHHHHKKKBbbbbRR",
 ".........KKHHHHBBBbbbbbR",  # 10
 "...........KKKBBBbbbbbbb",
 "............KBBBKKKKKKKK",
 "............KBBKCCCCCCCC",
 "............KBBKCEECCCCC",
 "............KBBBKCCCCCCC",  # 15
 "........KKKKKBBBBKKKKKKK",
 "......KKcCCCKKBBBBBBBRBB",
 ".....KcCCCCKbbKKBBBBBBBB",
 "....KcCCCCKbhhbbKBBBBBBB",
 "....KcCCCKbhhbbbbKBBBBBB",  # 20
 "...KcCCCCKbhbbbbbKBBbBBB",
 "...KcCCCKBbbbbbbKBBbbBBB",
 "...KcCCCKBBbbbbKBBBbbBBB",
 "..KcCCCCKBBBKKKBBBbbBBBB",
 "..KcCCCKKBBKBBBBBBbBBBBB",  # 25
 "..KcCCCKBBKBBBBBBbbBBBBB",
 "..KcCCCKBKKKKKKKKKKKKKKK",
 ".KcCCCCKBKsssssssssssssR",
 ".KcCCCCKBKKKKKKKKKKKKKKK",
 ".KcCCCCKKBBBBBBbBBBBBBBB",  # 30
 ".KcCCCCCKBBBBBBbBBBBBBBB",
 ".KcCCCCCKBBBBBbbBBBBBBBB",
 "KcCCCCCCKBBBBBbBBBBBBKBB",
 "KcCCCCCCKBBBBBbBBBBBBKBB",
 "KcCCCCCCKBBBBbbBBBBBKBBB",  # 35
 "KcCCCCCCCKBBBbBBBBBBKBBB",
 "KcCCCCCCCKBBBbBBBBBBKBBB",
 "KcCcCCcCCKBBbBBBBBBKBBBB",
 "KKcKcCKcCKBBbBBBBBBKBBBB",
 ".KK.KcKKCCKBbBBBBBKbBBBB",  # 40
 "....KK.KKKKBBBBBBBKbBBBB",
 "..........KBBBBBBBKbbBBB",
 "...F......KBBBBBBKKKbBBB",
 "..FfF....KBBBBBBK..KKBBK",
 ".FffFF..KBBBBBBBK...KBBK",  # 45
 "FFfffFFKKKKKKKKKK...KKKK",
 "FFFFFFFFFF.FFFFFFFFF.FFF",
]
PATCHES = [
 # chain hanging from the left gauntlet
 (6, 24, "z"), (5, 25, "z"), (6, 26, "z"), (5, 27, "z"), (6, 28, "z"), (5, 29, "z"), (6, 30, "z"),
 # flames licking the shoulders
 (2, 13, "..F"), (1, 14, ".FfF"), (1, 15, "FffF"), (2, 16, "FF"),
 (43, 13, "F.."), (43, 14, "FfF."), (43, 15, "FffF"), (44, 16, "FF"),
 (21, 47, "FfFfFf"), (11, 47, "f"), (36, 47, "f"),
]

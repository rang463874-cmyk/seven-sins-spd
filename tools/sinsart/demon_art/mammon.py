from .artlib import K
PAL = {"K": K, "G": (232, 176, 40, 255), "Y": (255, 228, 112, 255), "g": (164, 106, 22, 255), "o": (100, 60, 14, 255),
       "D": (34, 22, 26, 255), "E": (255, 250, 228, 255), "B": (22, 16, 22, 255), "b": (110, 110, 130, 255),
       "s": (255, 210, 80, 255)}
HALF = [
 "........................",  # 0
 "..............K.........",
 ".............KYK........",
 ".............KYGK.......",
 ".............KGGK....KKK",
 "..............KGGK..KYYG",  # 5
 "..............KGgGKKYGGG",
 "...............KKKGGGGGG",
 ".............KKYYGGGGGGG",
 "............KYYGGGGGGGGG",
 "...........KYGGGGGGGGGGG",  # 10
 "...........KYGGKKKKKKKKK",
 "...........KGGKDDDDDDDDD",
 "...........KGGKDDEEDDDDD",
 "...........KGgKDDEEDDDDD",
 "...........KGgGKDDDDDDDD",  # 15
 "....KKKK...KgGGGKKKKKKKK",
 "..KKYYYYKK..KgggGGGGGGGG",
 ".KYYGGGGGgK..KKgggggGGGG",
 "KYGGGGGGGgoK.KGGKKKKKgGG",
 "KYGGGGGGGgoKKGGGGGGGGKKK",  # 20
 "KGGGGGGGggoKGYGGGGGGKBBB",
 "KGgGGGGgggoKGYGGGGGKBbBB",
 ".KggggggooKKGYGGGGGKBBBB",
 "..KoooooKKGGGGGGGGGKBBBB",
 "..KYGGgKKGgGGGGGGGGGKBBB",  # 25
 "..KYGGgK.KGgGGGGGGGGGKKK",
 "..KGGGgK.KGggGGGGGGGGGGG",
 "..KGGGgK..KGggGGGGGGGGGG",
 "..KKKKKK..KKKKKKKKKKKKKK",
 "..KYGGgK..KYGGGGGGGGGGGG",  # 30
 "..KGGGgK..KgGgGgGgGgGgGg",
 ".KYYGGggK.KKKKKKKKKKKKKK",
 "KYGGGGGgoK.KYGGGGGGgK...",
 "KGGGGGGgoK.KGGGGGGGgK...",
 "KgGGGGggoK.KKKKKKKKKK...",  # 35
 ".KgggggoK..KYGGGGGGgK...",
 "..KKKKKK...KGGGGGGGgK...",
 "...........KGGGGGGggK...",
 "...........KKKKKKKKKK...",
 "...........KYGGGGGGgK...",  # 40
 "...........KGGGGGGGgK...",
 "..........KYGGGGGGGggK..",
 ".........KYGGGGGGGGGgoK.",
 ".........KGGGGGGGGGggoK.",
 ".........KgggggggggoooK.",  # 45
 ".........KKKKKKKKKKKKKK.",
 "........................",
]
# the right arm is raised, fist clenched over the shoulder
PATCHES = [(38, y, "_" * 10) for y in range(24, 38)] + [
 (33, 0,  "...KKKKKKKK."),
 (33, 1,  "..KYYYYYYGgK"),
 (33, 2,  ".KYGGKGGKGgoK"),
 (33, 3,  ".KYGGKGGKGgoK"),
 (33, 4,  ".KGGGKGGKGgoK"),
 (33, 5,  ".KGGGGGGGGgoK"),
 (33, 6,  ".KKYYGGGGggoK"),
 (33, 7,  "KYYGGKgGGggoK"),
 (33, 8,  "KGGGGKgggggoK"),
 (33, 9,  ".KggKKoooooK"),
 (33, 10, "..KK.KGGGgK"),
 (33, 11, ".....KYGGgK"),
 (33, 12, ".....KYGGgK"),
 (33, 13, ".....KGGGgK"),
 (33, 14, ".....KGGggK"),
 (33, 15, ".....KGGggK"),
 # round chest orb
 (20, 20, "KKKKKKKK"),
 (19, 21, "KBBBBBBBBK"),
 (19, 22, "KBbbBBBBBK"),
 (19, 23, "KBbBBBBBBK"),
 (19, 24, "KBBBBBBBBK"),
 (20, 25, "KKKKKKKK"),
]

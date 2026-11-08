from .artlib import K
PAL = {"K": K, "P": (255, 112, 182, 255), "p": (196, 54, 128, 255), "Q": (255, 176, 216, 255),
       "G": (246, 196, 60, 255), "g": (176, 120, 30, 255), "R": (236, 30, 70, 255),
       "D": (66, 32, 48, 255), "E": (255, 150, 226, 255), "B": (38, 24, 38, 255), "b": (78, 48, 76, 255),
       "M": (236, 56, 150, 255), "L": (226, 84, 156, 255), "l": (150, 40, 100, 255), "S": (250, 214, 214, 255),
       "O": (255, 80, 220, 255), "o": (255, 236, 252, 255), "h": (60, 30, 60, 255)}
HALF = [
 "........................",  # 0
 "........................",
 "........................",
 "........................",
 "......................KK",
 ".....................KGG",  # 5
 "...........K.......KKGRR",
 "..........KhK.....KGKGRR",
 "..........KhhK....KGGGGG",
 "..........KhhKKKKKKgggggg"[:24],
 "..........KKPPPPPPPPPPPP",  # 10
 ".........KPPQQPPPPPPPPPP",
 "........KPPQPPPPPPPPPPPP",
 "........KPQPPPPPKPPPPKPP",
 ".......KPPQPPPPKDKPPKDKP",
 ".......KPQPPPPKDDDKKDDDD",  # 15
 ".......KPQPPPPKDDDDDDDDD",
 "......KPPQPPPKDDEEDDDDDD",
 "......KPQPPPPKDDEEDDDDDD",
 "......KPQPPPPKDDDDDDDDDD",
 "......KPQPPPPPKDDDDDDDDD",  # 20
 ".....KPPQPPPPPPKKDDDDDDD",
 ".....KPQPPPpPPPPPKKKKSSS",
 ".....KPQPPPpPPPPKBBBBKSS",
 ".....KPQPPPpPPPKBBMBBBKK",
 "....KPPQPPpPPPKSKBBMBBBB",  # 25
 "....KPQPPPpPPKSSKBBBMBBB",
 "....KPQPPPpPPKSSKBBBBMBB",
 "....KPQPPpPPPPKKBBBBBBMB",
 "...KPPQPPpPPPPKBBBBBBBBB",
 "...KPQPPPpPPPKMBMBBMBBMB",  # 30
 "...KPQPPpPPPKBBBBBBBBBBB",
 "...KPQPPpPPPKBbBBBbBBBbB",
 "..KPPQPPpPPKBBbBBBbBBBbB",
 "..KPQPPpPPPKBbBBBbBBBbBB",
 "..KPQPPpPPKMMMMMMMMMMMMM",  # 35
 "..KPQPPpPPPKKKKKKKKSSKKK",
 "..KPQPpPPPPPK....KSSK...",
 ".KPPQPpPPPPPK....KSSK...",
 ".KPQPPpPPPPPK....KSSK...",
 ".KPQPpPPPPPK.....KLLK...",  # 40
 ".KPQPpPPPPPK.....KLLK...",
 ".KPQPpPPPPK......KLLK...",
 ".KPPPpPPPK.......KLLK...",
 "..KPPpPPK.......KLLlK...",
 "...KPpPK........KLllK...",  # 45
 "....KKK.........KKKKK...",
 "........................",
]
PATCHES = [
 (38, 17, "....KKKK"),
 (38, 18, "..KKOOOOKK"),
 (38, 19, ".KOOOooOOOK"),
 (38, 20, ".KOOooooOOK"),
 (38, 21, "KOOoooooOOOK"),
 (38, 22, "KOOooooooOOK"),
 (38, 23, "KOOooooooOOK"),
 (38, 24, "KOOOooooOOOK"),
 (38, 25, ".KOOOooOOOK"),
 (38, 26, ".KKOOOOOOKK"),
 (36, 27, "KSSKKOOKK"),
 (35, 28, "KSSK.KK"),
]

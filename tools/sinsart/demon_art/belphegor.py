from .artlib import K
PAL = {"K": K, "L": (204, 186, 242, 255), "l": (150, 128, 204, 255), "W": (238, 230, 255, 255),
       "S": (214, 214, 228, 255), "s": (140, 140, 162, 255), "G": (176, 96, 255, 255),
       "D": (52, 38, 72, 255), "E": (224, 206, 255, 255), "P": (112, 72, 172, 255), "p": (70, 42, 122, 255),
       "M": (164, 116, 224, 255), "R": (176, 34, 70, 255), "B": (84, 52, 96, 255), "Q": (244, 236, 214, 255),
       "U": (236, 232, 244, 255), "u": (168, 162, 186, 255), "F": (176, 112, 255, 255), "f": (120, 70, 220, 255)}
HALF = [
 "........................",  # 0
 "......................KK",
 ".....................KSS",
 ".....................KSS",
 "................K...KSSS",
 "...............KSK..KSSS",  # 5
 "...............KSSK.KSGG",
 "...............KSSSKSSGG",
 "...........KKKKKsSsSsSsS",
 ".........KKLLLLLKKKKKKKK",
 "........KLLWWLLLLLLLLLLL",  # 10
 ".......KLLWLLLLLLLLLLLLL",
 "......KLLWLLLLLKLLLLLKLL",
 "......KLWLLLLLKDKLLLKDKL",
 ".....KLLWLLLLKDDDKKKDDDD",
 ".....KLWLLLLLKDDDDDDDDDD",  # 15
 "....KLLWLLLLKDDDDDDDDDDD",
 "....KLWLLLLLKDDKKKDDDDDD",
 "....KLWLLLlLKDDDEEDDDDDD",
 "...KLLWLLLlLKDDDDDDDDDDD",
 "...KLWLLLlLLLKDDDDDDDDDD",  # 20
 "...KLWLLLlLLLLKKDDDDDDDD",
 "..KLLWLLlLLLLLLLKKKKKKKK",
 "..KLWLLLlLLLLLKPPPPPPKRR",
 "..KLWLLlLLLLLKPPPMPPPPKR",
 ".KLLWLLlLLLLKPPPMPPPPPPK",  # 25
 ".KLWLLlLLLLKPPPMPPPPPPPP",
 ".KLWLLlLLLKPPPMPPPPPPPPP",
 ".KLWLLlLLLKPPMPPPPPPPPPP",
 "KLLWLlLLLKPPPPPPPPPPPPPP",
 "KLWLLlLLLKRRRRRRRRRRRRRR",  # 30
 "KLWLLlLLKPPMPPPPMPPPPMPP",
 "KLWLlLLLKPPMPPPPMPPPPMPP",
 "KLWLlLLKPPMPPPPMPPPPMPPP",
 "KLLLlLLKPPMPPPPMPPPPMPPP",
 ".KLLlLKpPPPPPPPPPPPPPPPP",  # 35
 ".KLLlLKRRRRRRRRRRRRRRRRR",
 "..KLlLKpPMPPPPMPPPPMPPPP",
 "..KLlKpPMPPPPMPPPPMPPPPP",
 "...KLKpPMPPPPMPPPPMPPPPP",
 "...KKpPMPPPPMPPPPMPPPPPP",  # 40
 "....KpPPPPPPPPPPPPPPPPPP",
 "....KpppppppppppppppppPP",
 ".....KKKKKKKKKKKKKKKKKKK",
 "..................KDDK..",
 "..................KDDK..",  # 45
 ".................KDDDK..",
 ".................KKKKK..",
]
PATCHES = [
 # the open book of sloth, held on the left
 (3, 25, "KKKKKKKKKK"),
 (2, 26, "KQQQQKQQQQK"),
 (2, 27, "KQssQKQssQK"),
 (2, 28, "KQQQQKQQQQK"),
 (2, 29, "KQssQKQssQK"),
 (2, 30, "KBBBBKBBBBK"),
 (3, 31, "KKKKKKKKKK"),
 # floating skulls
 (37, 22, ".KKKKK."),
 (37, 23, "KUUUUUK"),
 (37, 24, "KUKUKUK"),
 (37, 25, "KUUUUUK"),
 (37, 26, ".KuKuK."),
 (37, 27, "..KKK.."),
 (0, 8,  ".KKKK."),
 (0, 9,  "KUUUUK"),
 (0, 10, "KKUKUK"),
 (0, 11, "KUUUUK"),
 (0, 12, ".KuuK."),
 (0, 13, "..KK.."),
 # ghostly flames
 (41, 8, ".F."), (41, 9, "FfF"), (42, 10, "f"),
 (44, 40, ".F"), (44, 41, "FfF"), (1, 44, "F"), (0, 45, "FfF"),
]

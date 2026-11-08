from .artlib import K
PAL = {"K": K, "V": (66, 96, 44, 255), "v": (40, 60, 28, 255), "L": (110, 150, 60, 255), "l": (160, 200, 90, 255),
       "E": (255, 50, 40, 255), "e": (255, 160, 120, 255), "D": (24, 34, 20, 255), "S": (70, 70, 80, 255),
       "s": (120, 120, 130, 255), "T": (180, 255, 60, 255), "t": (110, 220, 40, 255), "Y": (220, 255, 120, 255)}
HALF = [
 "........................",  # 0
 "........................",
 "........................",
 "........................",
 "........................",
 "........................",  # 5
 "..................KKKKKK",
 ".................KVLLLLL",
 "................KVLlLLLL",
 "................KVLLLLLL",
 "...............KVLLKKKLL",  # 10
 "...............KVLKDEEKL",
 "...............KVLKDEeKL",
 "...............KvVLKKKLL",
 "......KKKKK....KvVLLLLLL",
 "....KKLLLLLKK...KvVVVVVV",  # 15
 "...KLlLLLLLLLKKKKKKvvvvv",
 "..KLlLLLLLLLLLLKVVKKKKKK",
 ".KLlLLLLLLLLLLLKVLLLLKVV",
 ".KLlLLLLLLLLLLKVLLLLLLKV",
 "KLlLLLLLLLLLLLKVLLLLLLLK",  # 20
 "KLlLLLLLLLLLLKVLLLKKKKLL",
 "KLLLLLLLLLLLLKVLLKVVVVKL",
 "KVLLLLLLLLLLKVLLKVLLLLVK",
 "KVLLLLLLLLLLKVLLKVLLLLLK",
 "KVVLLLLLLLLKVLLLKKKKKKKK",  # 25
 ".KVVLLLLLLKVLLLKVVVVVVVV",
 ".KVVVLLLLKVVLLKVLLLLLLLL",
 "..KVVVLLKVVLLLKVLLKKKKLL",
 "..KVKKKKKVLLLLKVLKVVVVKL",
 "..KVK..KVVLLLLKVLKVLLLVK",  # 30
 "..KVK..KVLLLLLKKKKKKKKKK",
 "..KLK..KVLLLLKVVVVVVVVVV",
 ".KLlLK.KVLLLLKVLLLLLLLLL",
 ".KLLLK.KVVLLLKVLLKKKKKKL",
 ".KVVVK.KVVLLLLKVKVVVVVVK",  # 35
 "..KKK..KVVVLLLKVKVLLLLLK",
 ".......KVVVVLLKKKKKKKKKK",
 "........KVVVVLLLKKVVVVVK",
 "........KKVVVVLLLKKVLLVK",
 ".........KVVVVLLLLKKVVKK",  # 40
 ".........KVVVVVLLLLKKKK.",
 "........KSKVVVVVLLLLK...",
 ".......KSsSKVVVVVVLLK...",
 "......KSsSSKKVVVVVVVK...",
 ".....KSsSSSKSKKKKKKKKK..",  # 45
 "....KSSSSSKSsSK.KSsSK...",
 "........................",
]
PATCHES = [
 # the green horn-spear rising over the left shoulder
 (8, 0,  "..KK"),
 (8, 1,  ".KTTK"),
 (8, 2,  ".KTtK"),
 (8, 3,  "KTTtK"),
 (8, 4,  "KTtK"),
 (8, 5,  "KTtK"),
 (8, 6,  "KTtK"),
 (8, 7,  "KTtK"),
 (8, 8,  "KTtK"),
 (8, 9,  "KTtK"),
 (8, 10, "KTtK"),
 (8, 11, "KTtK"),
 (8, 12, "KTtK"),
 (8, 13, "KTtK"),
 # toxic sparks
 (2, 9, "Y"), (44, 12, "Y"), (45, 30, "T"), (1, 38, "T"), (40, 4, "t"),
]

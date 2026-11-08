from .artlib import K
PAL = {"K": K, "W": (248, 248, 244, 255), "w": (206, 206, 222, 255), "q": (150, 140, 196, 255),
       "G": (246, 196, 60, 255), "g": (176, 120, 30, 255), "Y": (255, 240, 150, 255), "R": (90, 200, 255, 255),
       "D": (70, 50, 46, 255), "d": (40, 28, 30, 255), "E": (255, 250, 210, 255),
       "V": (64, 56, 92, 255), "v": (104, 96, 140, 255), "u": (160, 152, 196, 255), "s": (255, 230, 120, 255), "S": (252, 226, 206, 255)}
HALF = [
 "........................",  # 0
 "......................KK",
 ".....................KYY",
 ".............KK......KYG",
 "............KYK.....KYGG",
 "...s........KYGK....KGGG",
 "..sYs.......KGGK.KK.KGGG",  # 6
 "...s........KGGGKGGKGGGG",
 "............KGGGGGGGGGRR",
 "....KK......KgGgGgGgGgRR",
 "...KuK.....KWKgggggggggg",  # 10
 "..KuvK....KWWWKKKKKKKKKK",
 ".KuuvK....KWWWWWWWWWWWWW",
 ".KuuvVK..KWWWwWWWWWWWWWW",
 "KuuuvVK.KWWWwWWWWKWWWWKW",
 "KuuuvVVKKWWwWWWWKDKWWKDK",  # 15
 "KuuuvvVKWWwWWWWKDDDKKDDD",
 "KuuvvVVKWWwWWWKDDDDDDDDD",
 "KuuvvVKKWwWWWWKDDEEDDDDD",
 "KuvvVVKWWwWWWKDDDEEDDDDD",
 "KuvvVKKWWwWWWKDDDDDDDDDD",  # 20
 "KuvvVKuKWwWWWWKDDDDDDDDD",
 "KvvVKuuKWwKWWWWKKDDDDDDD",
 "KvVKuuvKwwKKWWWWWKKKKDDD",
 "KVKuuvvVKKwwKKKKKWWWWKKK",
 "KKuuvvVVVKWWWWWWWWWWWWWW",  # 25
 ".KuuvvVVKWWWwWWWWKWWWWWW",
 ".KuvvVVKWWWwWKKKKwKWWWWW",
 "..KuvVKWWWwWKwwwwKKWWWWW",
 "..KvVKKWWwWKWWWWWKGGGGGG",
 "...KKKWWwKKWWWWWKgGgGgGg",  # 30
 ".....KSSKKKWWWWWWKWWWWWG",
 ".....KSSK.KwWWWWWKWWWWWG",
 "......KK..KwWWWWWKWWWWWG",
 "..........KwwWWWWWKWWWWG",
 ".........KqwwWWWWWKWWWWG",  # 35
 ".........KqwwWWWWWKWWWWG",
 "........KqqwwWWWWWWKWWWG",
 "........KqqwwWWWWWWKWWWG",
 ".......KqqqwwWWWWWWWKWWG",
 ".......KGGGGGGGGGGGGGGGG",  # 40
 ".......KgGgGgGgGgGgGgGgG",
 "........KKKqqwwKKwwWWWWW",
 "...........KKKK..KKwWWWW",
 "..................KWWWWw",
 "...................KwWWw",  # 45
 "...................KKKKK",
 "........................",
]
PATCHES = []

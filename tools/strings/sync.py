#!/usr/bin/env python3
"""Copies every key in tools/strings/*.properties into the game's message files.

Existing keys are updated in place. A new key is inserted right after the key that
precedes it in its source file, so it lands in the right Seven Sins section.
Keys that are only in the game files are left alone (delete them by hand).

Usage: python3 tools/strings/sync.py
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SRC = os.path.join(ROOT, "tools", "strings")
MSG = os.path.join(ROOT, "core", "src", "main", "assets", "messages")


def target(fname):
    m = re.match(r"(.+)_(en|ko)\.properties$", fname)
    if not m:
        return None
    stem, lang = m.groups()
    if stem.endswith("_items"):
        cat = "items"
    elif stem.endswith("_windows"):
        cat = "windows"
    else:
        cat = "actors"
    suffix = "" if lang == "en" else "_" + lang
    return os.path.join(MSG, cat, cat + suffix + ".properties")


def parse(lines):
    out = []
    for ln in lines:
        if "=" in ln and not ln.lstrip().startswith("#"):
            k, v = ln.rstrip("\n").split("=", 1)
            out.append((k.strip(), v))
    return out


def main():
    changed = 0
    for fname in sorted(os.listdir(SRC)):
        dst = target(fname)
        if dst is None:
            continue
        with open(os.path.join(SRC, fname), encoding="utf-8") as f:
            src = parse(f.readlines())
        with open(dst, encoding="utf-8") as f:
            lines = f.readlines()
        index = {}
        for i, ln in enumerate(lines):
            if "=" in ln and not ln.lstrip().startswith("#"):
                index[ln.split("=", 1)[0].strip()] = i
        prev = None
        for k, v in src:
            new = k + "=" + v + "\n"
            if k in index:
                if lines[index[k]] != new:
                    lines[index[k]] = new
                    changed += 1
            else:
                if prev is None or prev not in index:
                    print("cannot place new key %s from %s" % (k, fname), file=sys.stderr)
                    sys.exit(1)
                at = index[prev] + 1
                lines.insert(at, new)
                index = {kk: (ii + 1 if ii >= at else ii) for kk, ii in index.items()}
                index[k] = at
                changed += 1
            prev = k
        with open(dst, "w", encoding="utf-8") as f:
            f.writelines(lines)
    print("updated %d lines" % changed)


if __name__ == "__main__":
    main()

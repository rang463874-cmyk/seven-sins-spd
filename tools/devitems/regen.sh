#!/bin/bash
# Regenerates core/.../devtools/DevItemList.java from the compiled game classes.
# Run from the project root after adding new items:
#   ./gradlew :core:compileJava && bash tools/devitems/regen.sh
# CLASSES / CP can be overridden to point at other build outputs.
set -e
cd "$(dirname "$0")/../.."
CLASSES=${CLASSES:-core/build/classes/java/main}
CP=${CP:-SPD-classes/build/classes/java/main}
TMP=$(mktemp -d)
javac -d "$TMP" tools/devitems/ListItems.java
java -cp "$TMP:$CLASSES:$CP" ListItems "$CLASSES" | python3 tools/devitems/gen_dev_items.py
rm -rf "$TMP"
